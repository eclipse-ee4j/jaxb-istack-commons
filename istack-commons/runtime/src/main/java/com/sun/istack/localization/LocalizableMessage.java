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

import com.sun.istack.localization.LocalizableMessageFactory.ResourceBundleSupplier;

import java.util.Arrays;
import java.util.Locale;
import java.util.ResourceBundle;


/**
 * @author WS Development Team
 */
public final class LocalizableMessage implements Localizable {

    private final String _bundlename;
    private final ResourceBundleSupplier _rbSupplier;

    private final String _key;
    private final Object[] _args;

    @Deprecated
    public LocalizableMessage(String bundlename, String key, Object... args) {
        this(bundlename, null, key, args);
    }

    public LocalizableMessage(String bundlename, ResourceBundleSupplier rbSupplier,
                              String key, Object... args) {
        _bundlename = bundlename;
        _rbSupplier = rbSupplier;
        _key = key;
        if(args==null)
            args = new Object[0];
        _args = args;
    }

    @Override
    public String getKey() {
        return _key;
    }

    @Override
    public Object[] getArguments() {
        return Arrays.copyOf(_args, _args.length);
    }

    @Override
    public String getResourceBundleName() {
        return _bundlename;
    }

    @Override
    public ResourceBundle getResourceBundle(Locale locale) {
        if (_rbSupplier == null)
            return null;

        return _rbSupplier.getResourceBundle(locale);
    }
}
