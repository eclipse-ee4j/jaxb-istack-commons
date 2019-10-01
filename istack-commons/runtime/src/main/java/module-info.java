/*
 * Copyright (c) 2017, 2019 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

/**
 * istack-commons runtime utilities.
 */
module com.sun.istack.runtime {
    requires transitive java.logging;
    requires transitive java.xml;
    requires static transitive jakarta.activation;

    exports com.sun.istack;
    exports com.sun.istack.localization;
    exports com.sun.istack.logging;
}
