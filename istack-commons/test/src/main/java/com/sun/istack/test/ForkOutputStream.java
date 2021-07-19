/*
 * Copyright (c) 1997, 2021 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.sun.istack.test;

import java.io.OutputStream;
import java.io.IOException;

/**
 * {@link OutputStream} splitter.
 *
 * @author Kohsuke Kawaguchi
 */
public final class ForkOutputStream extends OutputStream {
    final OutputStream out1,out2;

    public ForkOutputStream(OutputStream out1,OutputStream out2) {
        this.out1 = out1;
        this.out2 = out2;
    }

    @Override
    public void write(int b) throws IOException {
        out1.write(b);
        out2.write(b);
    }

    @Override
    public void close() throws IOException {
        out1.close();
        out2.close();
    }

    @Override
    public void flush() throws IOException {
        out1.flush();
        out2.flush();
    }

    @Override
    public void write(byte b[]) throws IOException {
        out1.write(b);
        out2.write(b);
    }

    @Override
    public void write(byte b[], int off, int len) throws IOException {
        out1.write(b,off,len);
        out2.write(b,off,len);
    }
}
