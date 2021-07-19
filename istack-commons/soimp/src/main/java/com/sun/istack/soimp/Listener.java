/*
 * Copyright (c) 2012, 2021 Oracle and/or its affiliates. All rights reserved.
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
public interface Listener {
    void info(String line);

    public static final Listener CONSOLE = new Listener() {
        @Override
        public void info(String line) {
            System.out.println(line);
        }
    };
}
