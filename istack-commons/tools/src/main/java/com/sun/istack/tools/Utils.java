/*
 * Copyright (c) 2020 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.sun.istack.tools;

import java.lang.reflect.Field;
import java.net.Authenticator;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author lukas
 */
final class Utils {

    private static final Logger LOGGER = Logger.getLogger(DefaultAuthenticator.class.getName());

    private Utils() {}

    static Authenticator getCurrentAuthenticator() {
        final Field f = getTheAuthenticator();
        if (f == null) {
            return null;
        }

        try {
            AccessController.doPrivileged((PrivilegedAction<Void>) () -> {
                f.setAccessible(true);
                return null;
            });
            return (Authenticator) f.get(null);
        } catch (IllegalAccessException | IllegalArgumentException ex) {
            LOGGER.log(Level.FINEST, "Cannot get Authenticator instance", ex);
            return null;
        } finally {
            AccessController.doPrivileged((PrivilegedAction<Void>) () -> {
                f.setAccessible(false);
                return null;
            });
        }
    }

    private static Field getTheAuthenticator() {
        try {
            return Authenticator.class.getDeclaredField("theAuthenticator");
        } catch (NoSuchFieldException | SecurityException ex) {
            LOGGER.log(Level.FINEST, "Cannot find Authenticator field", ex);
            return null;
        }
    }
}
