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

import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.MalformedURLException;
import java.net.URLConnection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Load classes/resources from a side folder, so that
 * classes of the same package can live in a single jar file.
 *
 * <p>
 * For example, with the following jar file:
 * <pre>
 *  /
 *  +- foo
 *     +- X.class
 *  +- bar
 *     +- X.class
 * </pre>
 * <p>
 * {@link ParallelWorldClassLoader}("foo/") would load {@code X.class} from
 * {@code /foo/X.class} (note that X is defined in the root package, not
 * {@code foo.X}.
 *
 * <p>
 * This can be combined with  {@link MaskingClassLoader} to mask classes which are loaded by the parent
 * class loader so that the child class loader
 * classes living in different folders are loaded
 * before the parent class loader loads classes living the jar file publicly
 * visible
 * For example, with the following jar file:
 * <pre>
 *  /
 *  +- foo
 *     +- X.class
 *  +- bar
 *     +-foo
 *        +- X.class
 * </pre>
 * <p>
 * {@link ParallelWorldClassLoader}(MaskingClassLoader.class.getClassLoader())
 * would load {@code foo.X.class}  from
 * {@code /bar/foo.X.class} not the {@code foo.X.class}
 * in the publicly visible place in the jar file, thus
 * masking the parent classLoader from loading the class from {@code foo.X.class}
 * (note that X is defined in the  package foo, not
 * {@code bar.foo.X}.
 *
 * @author Kohsuke Kawaguchi
 */
public class ParallelWorldClassLoader extends ClassLoader implements Closeable {

    /**
     * Strings like "prefix/", "abc/", or "" to indicate
     * classes should be loaded normally.
     */
    private final String prefix;
    private final Set<JarFile> jars;

    public ParallelWorldClassLoader(ClassLoader parent,String prefix) {
        super(parent);
        this.prefix = prefix;
        jars = Collections.synchronizedSet(new HashSet<>());
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {

        StringBuilder sb = new StringBuilder(name.length()+prefix.length()+6);
        sb.append(prefix).append(name.replace('.','/')).append(".class");

        URL u  = getParent().getResource(sb.toString());
        if (u == null) {
            throw new ClassNotFoundException(name);
        }

        InputStream is = null;
        URLConnection con = null;

        try {
            con = u.openConnection();
            is = con.getInputStream();
        } catch (IOException ioe) {
            throw new ClassNotFoundException(name);
        }

        if (is==null)
            throw new ClassNotFoundException(name);

        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buf = new byte[1024];
            int len;
            while((len=is.read(buf))>=0)
                baos.write(buf,0,len);

            buf = baos.toByteArray();
            int packIndex = name.lastIndexOf('.');
            if (packIndex != -1) {
                String pkgname = name.substring(0, packIndex);
                // Check if package already loaded.
                Package pkg = getDefinedPackage(pkgname);
                if (pkg == null) {
                    definePackage(pkgname, null, null, null, null, null, null, null);
                }
            }
            return defineClass(name,buf,0,buf.length);
        } catch (IOException e) {
            throw new ClassNotFoundException(name,e);
        } finally {
            try {
                if (con instanceof JarURLConnection) {
                    jars.add(((JarURLConnection) con).getJarFile());
                }
            } catch (IOException ioe) {
                //ignore
            }
            try {
                is.close();
            } catch (IOException ioe) {
                //ignore
            }
        }
    }

    @Override
    protected URL findResource(String name) {
        URL u = getParent().getResource(prefix + name);
        if (u != null) {
            try {
                jars.add(new JarFile(new File(toJarUrl(u).toURI())));
            } catch (URISyntaxException | IOException ex) {
                Logger.getLogger(ParallelWorldClassLoader.class.getName()).log(Level.WARNING, null, ex);
            } catch (ClassNotFoundException ex) {
                //ignore - not a jar
            }
        }
        return u;
    }

    @Override
    protected Enumeration<URL> findResources(String name) throws IOException {
        Enumeration<URL> en = getParent().getResources(prefix + name);
        while (en.hasMoreElements()) {
            try {
                jars.add(new JarFile(new File(toJarUrl(en.nextElement()).toURI())));
            } catch (URISyntaxException | IOException ex) {
                //should not happen
                Logger.getLogger(ParallelWorldClassLoader.class.getName()).log(Level.WARNING, null, ex);
            } catch (ClassNotFoundException ex) {
                //ignore - not a jar
            }
        }
        return en;
    }

    @Override
    public synchronized void close() throws IOException {
        for (JarFile jar : jars) {
            jar.close();
        }
    }

    /**
     * Given the URL inside jar, returns the URL to the jar itself.
     * @param res Resource in a jar
     * @return URL to the conaining jar file
     * @throws java.lang.ClassNotFoundException if res does not denote jar URL
     * @throws java.net.MalformedURLException if computed URL is invalid
     */
    public static URL toJarUrl(URL res) throws ClassNotFoundException, MalformedURLException {
        String url = res.toExternalForm();
        if(!url.startsWith("jar:"))
            throw new ClassNotFoundException("Loaded outside a jar "+url);
        url = url.substring(4); // cut off jar:
        url = url.substring(0,url.lastIndexOf('!'));    // cut off everything after '!'
        url = url.replaceAll(" ", "%20"); // support white spaces in path
        return new URL(url);
    }
}
