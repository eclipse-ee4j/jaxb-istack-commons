/*
 * Copyright (c) 1997, 2020 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package com.sun.istack.tools;

import java.io.Closeable;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DynamicConfigurator;
import org.apache.tools.ant.IntrospectionHelper;
import org.apache.tools.ant.Task;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.apache.tools.ant.AntClassLoader;

/**
 * Executes a {@link Task} in a special class loader that allows us to control
 * where to load particular APIs.
 *
 * @author Kohsuke Kawaguchi
 * @author Bhakti Mehta
 */
public abstract class ProtectedTask extends Task implements DynamicConfigurator {

    private final AntElement root = new AntElement("root");

    @Override
    public void setDynamicAttribute(String name, String value) throws BuildException {
        root.setDynamicAttribute(name, value);
    }

    @Override
    public Object createDynamicElement(String name) throws BuildException {
        return root.createDynamicElement(name);
    }

    @Override
    public void execute() throws BuildException {
        //Leave XJC2 in the publicly visible place
        // and then isolate XJC1 in a child class loader,
        // then use a MaskingClassLoader
        // so that the XJC2 classes in the parent class loader
        //  won't interfere with loading XJC1 classes in a child class loader
        ClassLoader ccl = SecureLoader.getContextClassLoader();
        try {
            ClassLoader cl = createClassLoader();
            @SuppressWarnings("unchecked")
            Class<Task> driver = (Class<Task>) cl.loadClass(getCoreClassName());

            Task t = driver.getDeclaredConstructor().newInstance();
            t.setProject(getProject());
            t.setTaskName(getTaskName());
            root.configure(t);

            SecureLoader.setContextClassLoader(cl);
            try {
                t.execute();
            } finally {
                driver = null;
                t.setTaskName(null);
                t.setProject(null);
                t = null;
            }
        } catch (UnsupportedClassVersionError e) {
            throw new BuildException("Requires Java SE 8 or later. Please download it from https://www.oracle.com/java/technologies/javase-download.html");
        } catch (ReflectiveOperationException | IOException e) {
            throw new BuildException(e);
        } finally {
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            SecureLoader.setContextClassLoader(ccl);

            //close/cleanup all classloaders but the one which loaded this class
            while (cl != null && !ccl.equals(cl)) {
                try {
                    ((Closeable) cl).close();
                } catch (IOException ex) {
                    throw new BuildException(ex);
                }
                cl = getParentClassLoader(cl);
            }
            cl = null;
        }
    }

    /**
     * Returns the name of the class that extends {@link Task}.This class will
     * be loaded int the protected classloader.
     * @return Task class name
     */
    protected abstract String getCoreClassName();

    /**
     * Creates a protective class loader that will host the actual task.
     * @return ClassLoader use d for task execution
     * @throws java.lang.ClassNotFoundException if required APIs are not found
     * @throws java.io.IOException if error happens
     */
    protected abstract ClassLoader createClassLoader() throws ClassNotFoundException, IOException;

    private ClassLoader getParentClassLoader(final ClassLoader cl) {
        //Calling getParent() on AntClassLoader doesn't return the - expected -
        //actual parent classloader but always the SystemClassLoader.
        if (cl instanceof AntClassLoader) {
            //1.8 added getConfiguredParent() to get correct 'parent' classloader
            ClassLoader loader = ((AntClassLoader) cl).getConfiguredParent();
            // we may be called by Gradle, in such case do not close its classloader,
            // so Gradle can handle it itself and return null here;
            // in other cases return parent or null if not found
            return loader == null ? null
                    : loader.getClass().getName().startsWith("org.gradle.") ? null : loader;
        }
        return SecureLoader.getParentClassLoader(cl);
    }

    /**
     * Captures the elements and attributes.
     */
    private class AntElement implements DynamicConfigurator {

        private final String name;

        private final Map<String,String> attributes = new HashMap<>();

        private final List<AntElement> elements = new ArrayList<>();

        public AntElement(String name) {
            this.name = name;
        }

        @Override
        public void setDynamicAttribute(String name, String value) throws BuildException {
            attributes.put(name, value);
        }

        @Override
        public Object createDynamicElement(String name) throws BuildException {
            AntElement e = new AntElement(name);
            elements.add(e);
            return e;
        }

        /**
         * Copies the properties into the Ant task.
         */
        public void configure(Object antObject) {
            IntrospectionHelper ih = IntrospectionHelper.getHelper(antObject.getClass());

            // set attributes first
            for (Entry<String, String> att : attributes.entrySet()) {
                ih.setAttribute(getProject(), antObject, att.getKey(), att.getValue());
            }

            // then nested elements
            for (AntElement e : elements) {
                Object child = ih.getElementCreator(getProject(), "", antObject, e.name, null).create();
                e.configure(child);
                ih.storeElement(getProject(), antObject, child, e.name);
            }
        }
    }
}
