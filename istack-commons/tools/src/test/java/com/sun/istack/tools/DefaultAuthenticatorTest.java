/*
 * Copyright (c) 2005, 2023 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.sun.istack.tools;

import java.io.File;
import java.lang.reflect.Field;
import java.net.Authenticator;
import java.net.MalformedURLException;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import static org.testng.Assert.*;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

/**
 * @author Rama Pulavarthi
 * @author Lukas Jungmann
 */
public class DefaultAuthenticatorTest {

    private static final Logger logger = Logger.getLogger(DefaultAuthenticatorTest.class.getName());

    public DefaultAuthenticatorTest() {
    }

    private static class MyAuthenticator extends DefaultAuthenticator {

        private String requestingURL;

        @Override
        protected URL getRequestingURL() {
            try {
                return new URL(requestingURL);
            } catch (MalformedURLException e) {
                logger.log(Level.SEVERE, null, e);
            }
            return null;
        }

        void setRequestingURL(String url) {
            requestingURL = url;
        }
    }

    @AfterMethod
    public void after() {
        clearTestAuthenticator();
    }

    @Test
    public void testDefaultAuth() throws Exception {
        URL url = getResourceAsUrl("com/sun/istack/tools/.auth");
        MyAuthenticator ma = createTestAuthenticator();
        ma.setRequestingURL("http://foo.com/myservice?wsdl");
        assertNull(DefaultAuthenticator.getCurrentAuthenticator());
        assertEquals(0, getCounter());
        try {
            DefaultAuthenticator da = DefaultAuthenticator.getAuthenticator();
            assertEquals(ma, da);
            assertEquals(1, getCounter());
            da.setAuth(new File(url.toURI()), null);
            PasswordAuthentication pa = da.getPasswordAuthentication();
            assertTrue(pa != null && pa.getUserName().equals("duke") && Arrays.equals(pa.getPassword(), "test".toCharArray()));
        } finally {
            DefaultAuthenticator.reset();
            assertNotEquals(ma, DefaultAuthenticator.getCurrentAuthenticator());
            assertEquals(0, getCounter());
        }
    }

    @Test
    public void testGetDefaultAuth() {
        Authenticator orig = DefaultAuthenticator.getCurrentAuthenticator();
        try {
            DefaultAuthenticator da = DefaultAuthenticator.getAuthenticator();
            assertNotEquals(orig, da);
            assertEquals(1, getCounter());
            Authenticator auth = DefaultAuthenticator.getCurrentAuthenticator();
            assertNotNull(auth);
            assertEquals(da, auth);
        } finally {
            DefaultAuthenticator.reset();
            assertEquals(orig, DefaultAuthenticator.getCurrentAuthenticator());
            assertEquals(0, getCounter());
        }
    }

    @Test
    public void testJaxWs_1101() throws Exception {
        URL url = getResourceAsUrl("com/sun/istack/tools/auth_test.resource");
        MyAuthenticator ma = createTestAuthenticator();

        try {
            DefaultAuthenticator da = DefaultAuthenticator.getAuthenticator();
            assertEquals(1, getCounter());
            assertEquals(ma, da);
            da.setAuth(new File(url.toURI()), null);

            ma.setRequestingURL("http://server1.myserver.com/MyService/Service.svc?wsdl");
            PasswordAuthentication pa = da.getPasswordAuthentication();
            assertEquals("user", pa.getUserName());
            assertEquals(")/_@B8M)gDw", new String(pa.getPassword()));

            ma.setRequestingURL("http://server1.myserver.com/MyService/Service.svc?xsd=xsd0");
            pa = da.getPasswordAuthentication();
            assertEquals("user", pa.getUserName());
            assertEquals(")/_@B8M)gDw", new String(pa.getPassword()));

            ma.setRequestingURL("http://server1.myserver.com/MyService/Service.svc");
            pa = da.getPasswordAuthentication();
            assertEquals("user", pa.getUserName());
            assertEquals(")/_@B8M)gDw", new String(pa.getPassword()));

            ma.setRequestingURL("http://server1.myserver.com/encoded/MyService/Service.svc?wsdl");
            pa = da.getPasswordAuthentication();
            assertEquals("user2", pa.getUserName());
            assertEquals(")/_@B8M)gDw", new String(pa.getPassword()));
        } finally {
            DefaultAuthenticator.reset();
            assertEquals(0, getCounter());
        }
    }

    private static URL getResourceAsUrl(String resourceName) throws RuntimeException {
        URL input = Thread.currentThread().getContextClassLoader().getResource(resourceName);
        if (input == null) {
            throw new RuntimeException("Failed to find resource \"" + resourceName + "\"");
        }
        return input;
    }

    private MyAuthenticator createTestAuthenticator() {
        Field f1 = null;
        try {
            f1 = DefaultAuthenticator.class.getDeclaredField("instance");
            f1.setAccessible(true);
            MyAuthenticator auth = new MyAuthenticator();
            f1.set(null, auth);
            return auth;
        } catch (Exception ex) {
            logger.log(Level.SEVERE, null, ex);
        } finally {
            if (f1 != null) {
                f1.setAccessible(false);
            }
        }
        return null;
    }

    private void clearTestAuthenticator() {
        Field f1, f2 = f1 = null;
        try {
            f1 = DefaultAuthenticator.class.getDeclaredField("instance");
            f1.setAccessible(true);
            MyAuthenticator auth = new MyAuthenticator();
            f1.set(null, null);
            f2 = DefaultAuthenticator.class.getDeclaredField("counter");
            f2.setAccessible(true);
            f2.setInt(null, 0);
        } catch (Exception ex) {
            logger.log(Level.SEVERE, null, ex);
        } finally {
            if (f1 != null) {
                f1.setAccessible(false);
            }
            if (f2 != null) {
                f2.setAccessible(false);
            }
        }
    }

    private int getCounter() {
        Field f = null;
        try {
            f = DefaultAuthenticator.class.getDeclaredField("counter");
            f.setAccessible(true);
            return f.getInt(null);
        } catch (Exception ex) {
            logger.log(Level.SEVERE, null, ex);
        } finally {
            if (f != null) {
                f.setAccessible(false);
            }
        }
        return -1;
    }
}
