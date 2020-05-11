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

import java.net.Authenticator;

/**
 *
 * @author lukas
 */
final class Utils {

    private Utils() {}

    static Authenticator getCurrentAuthenticator() {
        return Authenticator.getDefault();
    }
}
