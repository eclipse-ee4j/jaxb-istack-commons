/*
 * Copyright (c) 2019 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

/**
 * Common test-related utility code for the istack components.
 */
module com.sun.istack.test {
    requires java.logging;
    requires transitive java.xml;

    exports com.sun.istack.test;
}
