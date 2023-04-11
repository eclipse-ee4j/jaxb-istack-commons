/*
 * Copyright (c) 1997, 2023 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.sun.istack.logging;

import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.StreamHandler;

/**
 *
 * @author Marek Potociar
 */
public class LoggerTest {

    public LoggerTest() {
    }

    /**
     * Test of getLogger method, of class Logger.
     */
    @Test
    public void testGetLogger() {
        Logger result = Logger.getLogger(LoggerTest.class);
        Assert.assertNotNull(result);
    }

    /**
     * Test of getSystemLoggerName method, of class Logger.
     */
    @Test
    public void testGetSubsystemName() {
        String result = Logger.getSystemLoggerName(LoggerTest.class);
        Assert.assertEquals("com.sun.istack.logging", result);
    }

    /**
     * Test source method name resolution
     */
    @Test
    public void testGetCallerMethodName() throws UnsupportedEncodingException {
        Logger istackLogger = Logger.getLogger(LoggerTest.class);
        java.util.logging.Logger utilLogger =
                java.util.logging.Logger.getLogger(Logger.getSystemLoggerName(LoggerTest.class));
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        StreamHandler streamHandler = new StreamHandler(outputStream, new Formatter() {
            @Override
            public String format(LogRecord record) {
                return record.getSourceMethodName();
            }
        });
        utilLogger.addHandler(streamHandler);

        istackLogger.logException(new Exception("This LOG entry is part of the test"), Level.INFO);

        streamHandler.flush();

        String logText = outputStream.toString("UTF-8");
        Assert.assertEquals("testGetCallerMethodName", logText);
    }

}
