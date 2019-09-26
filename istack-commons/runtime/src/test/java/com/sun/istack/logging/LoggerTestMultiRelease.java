/*
 * Copyright (c) 2019 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.sun.istack.logging;

import org.junit.Assume;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.StreamHandler;

import static org.junit.Assert.assertEquals;

/**
 * Test cases for multi-release are executed as integration tests, those needs packaged jar to work properly
 *
 * @author Daniel Kec
 */
public class LoggerTestMultiRelease {

    /**
     * Test source method name resolution
     */
    @Test
    public void testGetCallerMethodName() throws UnsupportedEncodingException {
        Assume.assumeTrue("Skipping, test is applicable since java 9 and higher.",
                //Runtime#version is available since java 9
                Arrays.stream(Runtime.class.getMethods()).anyMatch(m -> "version".equals(m.getName())));
        Logger istackLogger = Logger.getLogger(LoggerTestMultiRelease.class);
        java.util.logging.Logger utilLogger =
                java.util.logging.Logger.getLogger(Logger.getSystemLoggerName(LoggerTestMultiRelease.class));
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
        assertEquals("testGetCallerMethodName", logText);
    }


}
