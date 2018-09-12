/*
 * Copyright (c) 1997, 2018 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.sun.istack.logging;

import junit.framework.TestCase;

/**
 *
 * @author Marek Potociar <marek.potociar at sun.com>
 */
public class LoggerTest extends TestCase {
    
    public LoggerTest(String testName) {
        super(testName);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * Test of getLogger method, of class Logger.
     */
    public void testGetLogger() {
        Logger result = Logger.getLogger(LoggerTest.class);
        assertNotNull(result);
    }

    /**
     * Test of getSystemLoggerName method, of class Logger.
     */
    public void testGetSubsystemName() {
        String result = Logger.getSystemLoggerName(LoggerTest.class);
        assertEquals("com.sun.istack.logging", result);
    }

}
