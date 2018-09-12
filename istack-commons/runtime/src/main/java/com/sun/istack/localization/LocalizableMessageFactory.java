/*
 * Copyright (c) 1997, 2018 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.sun.istack.localization;

import java.util.Locale;
import java.util.ResourceBundle;

/**
 * @author WS Development Team
 */
public class LocalizableMessageFactory {

    private final String _bundlename;
    private final ResourceBundleSupplier _rbSupplier;

    @Deprecated
    public LocalizableMessageFactory(String bundlename) {
        _bundlename = bundlename;
        _rbSupplier = null;
    }

    public LocalizableMessageFactory(String bundlename, ResourceBundleSupplier rbSupplier) {
        _bundlename = bundlename;
        _rbSupplier = rbSupplier;
    }

    public Localizable getMessage(String key, Object... args) {
        return new LocalizableMessage(_bundlename, _rbSupplier, key, args);
    }

    public interface ResourceBundleSupplier {
        /**
         * Gets the ResourceBundle.
         * @param locale the requested bundle's locale
         * @return ResourceBundle
         */
        ResourceBundle getResourceBundle(Locale locale);
    }

}
