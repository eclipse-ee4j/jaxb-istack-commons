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

import java.security.AccessController;
import java.security.PrivilegedAction;

/**
 * Class defined for safe calls of getClassLoader methods of any kind (context/system/class
 * classloader. This MUST be package private and defined in every package which
 * uses such invocations.
 * @author snajper
 */
class SecureLoader {

    static ClassLoader getContextClassLoader() {
        if (System.getSecurityManager() == null) {
            return Thread.currentThread().getContextClassLoader();
        } else {
            return AccessController.doPrivileged(
                    new PrivilegedAction<ClassLoader>() {
                        @Override
                        public ClassLoader run() {
                            return Thread.currentThread().getContextClassLoader();
                        }
                    });
        }
    }

    static ClassLoader getClassClassLoader(final Class c) {
        if (System.getSecurityManager() == null) {
            return c.getClassLoader();
        } else {
            return AccessController.doPrivileged(
                    new PrivilegedAction<ClassLoader>() {
                        @Override
                        public ClassLoader run() {
                            return c.getClassLoader();
                        }
                    });
        }
    }

    static ClassLoader getSystemClassLoader() {
        if (System.getSecurityManager() == null) {
            return ClassLoader.getSystemClassLoader();
        } else {
            return AccessController.doPrivileged(
                    new PrivilegedAction<ClassLoader>() {
                        @Override
                        public ClassLoader run() {
                            return ClassLoader.getSystemClassLoader();
                        }
                    });
        }
    }

    static ClassLoader getParentClassLoader(final ClassLoader cl) {
        if (System.getSecurityManager() == null) {
            return cl.getParent();
        } else {
            return AccessController.doPrivileged(
                    new PrivilegedAction<ClassLoader>() {
                        @Override
                        public ClassLoader run() {
                            return cl.getParent();
                        }
                    });
        }
    }

    static void setContextClassLoader(final ClassLoader cl) {
        if (System.getSecurityManager() == null) {
            Thread.currentThread().setContextClassLoader(cl);
        } else {
            AccessController.doPrivileged(
                    new PrivilegedAction<ClassLoader>() {
                        @Override
                        public ClassLoader run() {
                            Thread.currentThread().setContextClassLoader(cl);
                            return null;
                        }
                    });
        }
    }

}
