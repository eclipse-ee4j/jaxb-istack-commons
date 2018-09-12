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
 * {@link Localizable} that wraps a non-localizable string.
 *
 * @author WS Development Team
 */
public final class NullLocalizable implements Localizable {
    private final String msg;

    public NullLocalizable(String msg) {
        if(msg==null)
            throw new IllegalArgumentException();
        this.msg = msg;
    }

    @Override
    public String getKey() {
        return Localizable.NOT_LOCALIZABLE;
    }
    @Override
    public Object[] getArguments() {
        return new Object[]{msg};
    }
    @Override
    public String getResourceBundleName() {
        return "";
    }
    @Override
    public ResourceBundle getResourceBundle(Locale locale) {
        return null;
    }
}
