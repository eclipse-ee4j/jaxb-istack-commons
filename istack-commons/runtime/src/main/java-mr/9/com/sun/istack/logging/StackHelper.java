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

/**
 * Utils for stack trace analysis
 *
 * @author Daniel Kec
 */
class StackHelper {

    /**
     * Function returns the name of the caller method for the method executing this
     * function.
     *
     * @return caller method name from the call stack of the current {@link Thread}.
     */
    static String getCallerMethodName() {
        return StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE)
                .walk(frames -> frames
                        //Its a method of first declaring class after istack Logger class
                        .dropWhile(f -> !Logger.class.equals(f.getDeclaringClass()))
                        .dropWhile(f -> Logger.class.equals(f.getDeclaringClass()))
                        .findFirst()
                        .map(StackWalker.StackFrame::getMethodName)
                        .orElse("UNKNOWN METHOD"));
    }
}
