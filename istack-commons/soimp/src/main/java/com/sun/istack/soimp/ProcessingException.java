/*
 * Copyright (c) 2012, 2022 Oracle and/or its affiliates. All rights reserved.
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
public class ProcessingException extends Exception {

    private static final long serialVersionUID = -6214429602282488762L;

    public ProcessingException(String message) {
        super(message);
    }

    public ProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}
