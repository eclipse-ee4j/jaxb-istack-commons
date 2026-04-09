/*
 * Copyright (c) 2026 Contributors to the Eclipse Foundation. All rights reserved.
 * Copyright (c) 2012, 2018 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.sun.istack.soimp;

/**
 * @author Kohsuke Kawaguchi
 */
enum Mode {
    NEW, DELETED, UPDATED;

    static Mode parse(char ch) {
        return switch (ch) {
            case 'M' -> UPDATED;
            case '!' -> DELETED;
            case '?' -> NEW;
            default -> null;
        };
    }
}
