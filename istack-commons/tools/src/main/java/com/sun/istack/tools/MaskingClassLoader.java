/*
 * Copyright (c) 1997, 2023 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.sun.istack.tools;

import java.util.Collection;

/**
 * {@link ClassLoader} that masks a specified set of classes
 * from its parent class loader.
 *
 * <p>
 * This code is used to create an isolated environment.
 *
 * @author Kohsuke Kawaguchi
 */
public class MaskingClassLoader extends ClassLoader {

    private final String[] masks;

    public MaskingClassLoader(String... masks) {
        this.masks = masks;
    }

    public MaskingClassLoader(Collection<String> masks) {
        this(masks.toArray(new String[0]));
    }

    public MaskingClassLoader(ClassLoader parent, String... masks) {
        super(parent);
        this.masks = masks;
    }

    public MaskingClassLoader(ClassLoader parent, Collection<String> masks) {
        this(parent, masks.toArray(new String[0]));
    }

    @Override
    protected synchronized Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        for (String mask : masks) {
            if(name.startsWith(mask))
                throw new ClassNotFoundException();
        }

        return super.loadClass(name, resolve);
    }
}
