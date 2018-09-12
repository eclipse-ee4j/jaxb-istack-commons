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

import org.xml.sax.SAXParseException;
import org.xml.sax.Locator;

/**
 * {@link SAXParseException} that handles exception chaining correctly.
 *
 * @author Kohsuke Kawaguchi
 * @since 2.0 FCS
 */
public class SAXParseException2 extends SAXParseException {
    public SAXParseException2(String message, Locator locator) {
        super(message, locator);
    }

    public SAXParseException2(String message, Locator locator, Exception e) {
        super(message, locator, e);
    }

    public SAXParseException2(String message, String publicId, String systemId, int lineNumber, int columnNumber) {
        super(message, publicId, systemId, lineNumber, columnNumber);
    }

    public SAXParseException2(String message, String publicId, String systemId, int lineNumber, int columnNumber, Exception e) {
        super(message, publicId, systemId, lineNumber, columnNumber, e);
    }

    public Throwable getCause() {
        return getException();
    }
}
