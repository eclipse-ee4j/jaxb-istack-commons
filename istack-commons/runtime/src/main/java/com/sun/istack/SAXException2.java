/*
 * Copyright (c) 1997, 2018 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.sun.istack;

import org.xml.sax.SAXException;

/**
 * {@link SAXException} that handles exception chaining correctly.
 *
 * @author Kohsuke Kawaguchi
 * @since 2.0 FCS
 */
public class SAXException2 extends SAXException {
    public SAXException2(String message) {
        super(message);
    }

    public SAXException2(Exception e) {
        super(e);
    }

    public SAXException2(String message, Exception e) {
        super(message, e);
    }

    public Throwable getCause() {
        return getException();
    }
}
