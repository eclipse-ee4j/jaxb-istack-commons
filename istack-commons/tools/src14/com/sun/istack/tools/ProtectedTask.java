/*
 * Copyright (c) 1997, 2018 Oracle and/or its affiliates. All rights reserved.
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
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import java.net.URLClassLoader;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Iterator;
import java.util.Map.Entry;
import org.apache.tools.ant.AntClassLoader;

/**
 * Executes a {@link Task} in a special class loader that allows
 * us to control where to load 2.1 APIs, even if we run in Java 6.
 *
 * <p>
 * No JDK 1.5 code here, please. This allows us to detect "require JDK5" bug nicely.
 *
 * @author Kohsuke Kawaguchi
 * @author Bhakti Mehta
 */
public abstract class ProtectedTask extends Task implements DynamicConfigurator {

    private final AntElement root = new AntElement("root");

    public void setDynamicAttribute(String name, String value) throws BuildException {
        root.setDynamicAttribute(name,value);
    }

    public Object createDynamicElement(String name) throws BuildException {
        return root.createDynamicElement(name);
    }

    public void execute() throws BuildException {
        //Leave XJC2 in the publicly visible place
        // and then isolate XJC1 in a child class loader,
        // then use a MaskingClassLoader
        // so that the XJC2 classes in the parent class loader
        //  won't interfere with loading XJC1 classes in a child class loader
        ClassLoader ccl = SecureLoader.getContextClassLoader();
        try {
            ClassLoader cl = createClassLoader();
            Class driver = cl.loadClass(getCoreClassName());

            Task t = (Task)driver.newInstance();
            t.setProject(getProject());
            t.setTaskName(getTaskName());
            root.configure(t);

            SecureLoader.setContextClassLoader(cl);
            t.execute();
            
            driver = null;
            t.setTaskName(null);
            t.setProject(null);
            t = null;
        } catch (UnsupportedClassVersionError e) {
            throw new BuildException("Requires JDK 5.0 or later. Please download it from http://java.sun.com/j2se/1.5/");
        } catch (ClassNotFoundException e) {
            throw new BuildException(e);
        } catch (InstantiationException e) {
            throw new BuildException(e);
        } catch (IllegalAccessException e) {
            throw new BuildException(e);
        } catch (IOException e) {
            throw new BuildException(e);
        } finally {
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            SecureLoader.setContextClassLoader(ccl);
            
            //close/cleanup all classloaders but the one which loaded this class
            while (cl != null && !ccl.equals(cl)) {
                if (cl instanceof Closeable) {
                    //JDK7+, ParallelWorldClassLoader, Ant (AntClassLoader5)
                    try {
                        ((Closeable) cl).close();
                    } catch (IOException ex) {
                        throw new BuildException(ex);
                    }
                } else {
                    if (cl instanceof URLClassLoader) {
                        //JDK6 - API jars are loaded by instance of URLClassLoader
                        //so use proprietary API to release holded resources
                        try {
                            Class clUtil = ccl.loadClass("sun.misc.ClassLoaderUtil");
                            Method release = clUtil.getDeclaredMethod("releaseLoader", URLClassLoader.class);
                            release.invoke(null, cl);
                        } catch (ClassNotFoundException ex) {
                            //not Sun JDK 6, ignore
                        } catch (IllegalAccessException ex) {
                            throw new BuildException(ex);
                        } catch (IllegalArgumentException ex) {
                            throw new BuildException(ex);
                        } catch (InvocationTargetException ex) {
                            throw new BuildException(ex);
                        } catch (NoSuchMethodException ex) {
                            throw new BuildException(ex);
                        } catch (SecurityException ex) {
                            throw new BuildException(ex);
                        }
                    }
                }
                cl = getParentClassLoader(cl);
            }
            cl = null;
        }
    }

    /**
     * Returns the name of the class that extends {@link Task}.
     * This class will be loaded int the protected classloader.
     */
    protected abstract String getCoreClassName();

    /**
     * Creates a protective class loader that will host the actual task.
     */
    protected abstract ClassLoader createClassLoader() throws ClassNotFoundException, IOException;

    /* workaround for: https://issues.apache.org/bugzilla/show_bug.cgi?id=35436
       which is fixed in Ant 1.8 but 1.7 still needs to be supported */
    private ClassLoader getParentClassLoader(final ClassLoader cl) {
        //Calling getParent() on AntClassLoader doesn't return the - expected -
        //actual parent classloader but always the SystemClassLoader.
        if (cl instanceof AntClassLoader) {
            //1.8 added getConfiguredParent() to get correct 'parent' classloader
            if (System.getSecurityManager() == null) {
                return getPCL(cl);
            } else {
                return AccessController.doPrivileged(
                        new PrivilegedAction<ClassLoader>() {
                            public ClassLoader run() {
                                return getPCL(cl);
                            }
                        });
            }
        }
        return SecureLoader.getParentClassLoader(cl);
    }

    private ClassLoader getPCL(ClassLoader cl) {
        try {
            Method parentM = AntClassLoader.class.getDeclaredMethod("getConfiguredParent");
            return (ClassLoader) parentM.invoke(cl);
        } catch (IllegalAccessException ex) {
            throw new BuildException(ex);
        } catch (IllegalArgumentException ex) {
            throw new BuildException(ex);
        } catch (InvocationTargetException ex) {
            throw new BuildException(ex);
        } catch (NoSuchMethodException ex) {
            //Ant 1.7 try to get 'parent' field
            Field parentF = null;
            try {
                parentF = AntClassLoader.class.getDeclaredField("parent");
                parentF.setAccessible(true);
                return (ClassLoader) parentF.get(cl);
            } catch (IllegalAccessException ex1) {
                throw new BuildException(ex1);
            } catch (IllegalArgumentException ex1) {
                throw new BuildException(ex1);
            } catch (NoSuchFieldException ex1) {
                //not Ant 1.8 nor 1.7
                //should be some warning here?
            } catch (SecurityException ex1) {
                throw new BuildException(ex1);
            } finally {
                if (parentF != null) {
                    parentF.setAccessible(false);
                }
            }
        }
        return null;
    }

    /**
     * Captures the elements and attributes.
     */
    private class AntElement implements DynamicConfigurator {
        private final String name;

        private final Map/*<String,String>*/ attributes = new HashMap();

        private final List/*<AntElement>*/ elements = new ArrayList();

        public AntElement(String name) {
            this.name = name;
        }

        public void setDynamicAttribute(String name, String value) throws BuildException {
            attributes.put(name,value);
        }

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
            for (Iterator itr = attributes.entrySet().iterator(); itr.hasNext();) {
                Entry att = (Entry)itr.next();
                ih.setAttribute(getProject(), antObject, (String)att.getKey(), (String)att.getValue());
            }

            // then nested elements
            for (Iterator itr = elements.iterator(); itr.hasNext();) {
                AntElement e = (AntElement) itr.next();
                Object child = ih.createElement(getProject(), antObject, e.name);
                e.configure(child);
                ih.storeElement(getProject(), antObject, child, e.name);
            }
        }
    }
}

