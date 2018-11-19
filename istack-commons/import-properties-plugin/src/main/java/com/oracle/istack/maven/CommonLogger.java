/*
 * Copyright (c) 2013, 2018 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.oracle.istack.maven;

import org.apache.maven.plugin.logging.Log;

/**
 *
 * @author Martin Grebac
 */
public class CommonLogger {

    private final Log log;

    public CommonLogger(Log log) {
        this.log = log;
    }

    public void warn(String s) {
        if (log != null) {
            log.warn(s);
        }
    }

    public void info(String s) {
        if (log != null) {
            log.info(s);
        }
    }
}
