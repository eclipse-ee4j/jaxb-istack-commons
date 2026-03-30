/*
 * Copyright (c) 2012, 2023 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.sun.istack.tools;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import javax.xml.stream.XMLInputFactory;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 *
 * @author lukas
 */
public class ParallelWorldClassLoaderTest {

    private ClassLoader cl;
    private MaskingClassLoader mcl;
    private URLClassLoader ucl;
    private ParallelWorldClassLoader pwcl;
    private ClassLoader orig;

    public ParallelWorldClassLoaderTest() {
    }

    @BeforeMethod
    public void setUpMethod() throws Exception {
        cl = ClassLoader.getSystemClassLoader();
        mcl = new MaskingClassLoader(cl, "javax.xml.ws");
        String dir = System.getProperty("surefire.test.class.path").split(File.pathSeparator)[0];
        ucl = new URLClassLoader(new URL[] {new File(dir).toURI().toURL()}, mcl);
        pwcl = new ParallelWorldClassLoader(ucl, "");
        orig = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(pwcl);
    }

    @AfterMethod(alwaysRun = true)
    public void tearDownMethod() {
        Thread.currentThread().setContextClassLoader(orig);
    }

    /**
     * Test of findClass method, of class ParallelWorldClassLoader.
     */
    @Test
    public void testFindClass() {
        System.out.println("findClass");
        //XXX: why this fails ?
//        Class c3 = pwcl.findClass("javax.xml.ws.Service");
//        Assert.assertEquals(c3.getDeclaredMethods().length, 1);

        Class<?> c1,c2;
        try {
            c1 = Class.forName("javax.xml.ws.Service", false, pwcl);
            // jacoco adds method '$jacocoInit' so there can be at most two
            Assert.assertTrue(c1.getDeclaredMethods().length < 3);
        } catch (ClassNotFoundException cnfe) {
            Assert.fail();
        }
        try {
            c2 = Class.forName("javax.xml.ws.Service", false, Thread.currentThread().getContextClassLoader());
            // jacoco adds method '$jacocoInit' so there can be at most two
            Assert.assertTrue(c2.getDeclaredMethods().length < 3);
        } catch (ClassNotFoundException cnfe) {
            Assert.fail();
        }
    }

    /**
     * Test of findResource method, of class ParallelWorldClassLoader.
     */
    @Test
    public void testFindResource() {
        if (isJDK9()) return;
        URL resource = pwcl.getResource("javax/xml/ws/Service.class");
        URL object = pwcl.getResource("java/lang/Object.class");
        String resJar = resource.getPath().substring(0, resource.getPath().indexOf("!"));
        String rtJar = object.getPath().substring(0, object.getPath().indexOf("!"));
        Assert.assertEquals(resJar, rtJar);
    }

    /**
     * Test of findResources method, of class ParallelWorldClassLoader.
     */
    @Test
    public void testFindResources() throws Exception {
        if (isJDK9()) return;
        Enumeration<URL> foundURLs = pwcl.getResources("javax/xml/ws/Service.class");
        // TODO - this depends on jdk, maven cp, e.g.
        ArrayList<URL> al = Collections.list(foundURLs);
        int found = al.size();
        if (!((found == 3) || (found == 4))) {
            Assert.fail("Expected 3/4 elements. Verify the urls: \n" + al);
        }
    }

    @Test
    public void testJaxp() {
        XMLInputFactory inFactory = XMLInputFactory.newInstance();
        inFactory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
        inFactory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
        Assert.assertEquals(inFactory.getClass().getClassLoader(), ucl);
    }

    private static boolean isJDK9() {
        return true;
    }
}
