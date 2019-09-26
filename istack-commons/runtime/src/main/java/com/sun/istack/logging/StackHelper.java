/*
 * Copyright (c) 1997, 2019 Oracle and/or its affiliates. All rights reserved.
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
 * @author Marek Potociar
 * @author Fabian Ritzmann
 */
class StackHelper {
    
    /**
     * Function returns the name of the caller method for the method executing this
     * function.
     *
     * @return caller method name from the call stack of the current {@link Thread}.
     */
    static String getCallerMethodName() {
        return getStackMethodName(5);
    }

    /**
     * Method returns the name of the method that is on the {@code methodIndexInStack}
     * position in the call stack of the current {@link Thread}.
     *
     * @param methodIndexInStack index to the call stack to get the method name for.
     * @return the name of the method that is on the {@code methodIndexInStack}
     *         position in the call stack of the current {@link Thread}.
     */
    static String getStackMethodName(final int methodIndexInStack) {
        final String methodName;

        final StackTraceElement[] stack = Thread.currentThread().getStackTrace();
        if (stack.length > methodIndexInStack + 1) {
            methodName = stack[methodIndexInStack].getMethodName();
        } else {
            methodName = "UNKNOWN METHOD";
        }

        return methodName;
    }
}
