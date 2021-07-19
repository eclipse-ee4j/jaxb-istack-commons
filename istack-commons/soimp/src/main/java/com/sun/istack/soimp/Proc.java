/*
 * Copyright (c) 2012, 2021 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.sun.istack.soimp;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public final class Proc {
    private final Process proc;
    private final Thread t1,t2;

    public Proc(String cmd,String[] env,OutputStream out, File workDir) throws IOException {
        this( Runtime.getRuntime().exec(cmd,env,workDir), null, out );
    }

    public Proc(String[] cmd,String[] env,OutputStream out, File workDir) throws IOException {
        this( Runtime.getRuntime().exec(cmd,env,workDir), null, out );
    }

    public Proc(String[] cmd,String[] env,InputStream in,OutputStream out) throws IOException {
        this( Runtime.getRuntime().exec(cmd,env), in, out );
    }

    private Proc( Process proc, InputStream in, OutputStream out ) throws IOException {
        this.proc = proc;
        t1 = new Copier(proc.getInputStream(), out);
        t1.start();
        t2 = new Copier(proc.getErrorStream(), out);
        t2.start();
        if(in!=null)
            new ByteCopier(in,proc.getOutputStream()).start();
        else
            proc.getOutputStream().close();
    }

    public int join() {
        try {
            t1.join();
            t2.join();
            return proc.waitFor();
        } catch (InterruptedException e) {
            // aborting. kill the process
            proc.destroy();
            return -1;
        }
    }

    private static class Copier extends Thread {
        private final InputStream in;
        private final OutputStream out;

        public Copier(InputStream in, OutputStream out) {
            this.in = in;
            this.out = out;
        }

        @Override
        public void run() {
            try {
                copyStream(in,out);
                in.close();
            } catch (IOException e) {
                // TODO: what to do?
            }
        }
    }

    private static class ByteCopier extends Thread {
        private final InputStream in;
        private final OutputStream out;

        public ByteCopier(InputStream in, OutputStream out) {
            this.in = in;
            this.out = out;
        }

        @Override
        public void run() {
            try {
                while(true) {
                    int ch = in.read();
                    if(ch==-1)  break;
                    out.write(ch);
                }
                in.close();
                out.close();
            } catch (IOException e) {
                // TODO: what to do?
            }
        }
    }

    public static void copyStream(InputStream in,OutputStream out) throws IOException {
        byte[] buf = new byte[256];
        int len;
        while((len=in.read(buf))>0)
            out.write(buf,0,len);
    }
}
