/*
 * Copyright (c) 1997, 2023 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.sun.istack.maven;

import com.sun.codemodel.CodeWriter;
import com.sun.codemodel.JArray;
import com.sun.codemodel.JClass;
import com.sun.codemodel.JClassAlreadyExistsException;
import com.sun.codemodel.JCodeModel;
import com.sun.codemodel.JDefinedClass;
import com.sun.codemodel.JExpr;
import com.sun.codemodel.JFieldVar;
import com.sun.codemodel.JMethod;
import com.sun.codemodel.JMod;
import com.sun.codemodel.JPackage;
import com.sun.codemodel.writer.FileCodeWriter;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Pattern;

/**
 * Generates implementations of {@code org.glassfish.jaxb.runtime.v2.model.annotation.Quick} classes
 * for XML Binding annotations. Used exclusively by jaxb-ri project.
 *
 */
@Mojo(name = "quick-gen",
      defaultPhase = LifecyclePhase.GENERATE_SOURCES,
      requiresDependencyResolution = ResolutionScope.COMPILE,
      threadSafe = true)
public class QuickGenMojo extends AbstractMojo {

    private static final String LOCATABLE = "org.glassfish.jaxb.core.v2.model.annotation.Locatable";
    private static final String QUICK = "org.glassfish.jaxb.runtime.v2.model.annotation.Quick";

    @Parameter(defaultValue = "false")
    private boolean skip;

    /**
     * Location of the destination directory.
     */
    @Parameter(defaultValue = "${project.build.directory}/generated-sources/quick-sources")
    private File destDir;

    /**
     * File encoding for generated sources.
     * @since 2.12
     */
    @Parameter(defaultValue = "${project.build.sourceEncoding}")
    private String encoding;

    /**
     * Annotations to generate implementations of {@code org.glassfish.jaxb.runtime.v2.model.annotation.Quick} for.
     */
    @Parameter(required = true)
    private List<String> classes;

    /**
     * License file to use in generated sources.
     */
    @Parameter
    private File license;

    /**
     * Package for generated implementations.
     */
    @Parameter(required = true)
    private String packageName;

    /**
     * Attach generated sources to project sources.
     */
    @Parameter(defaultValue = "true")
    private boolean attach;

    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    private MavenProject project;

    private final List<Pattern> patterns = new ArrayList<>();

    /**
     * Used during the build to load annotation classes.
     */
    private ClassLoader userLoader;

    /**
     * Generated interfaces go into this codeModel.
     */
    private JCodeModel codeModel = new JCodeModel();

    /**
     * Avoid compile-time dependency on {@code org.glassfish.jaxb.core.v2.model.annotation.Locatable}
     */
    private final JClass locatable = codeModel.ref(LOCATABLE);

    /**
     * Avoid compile-time dependency on {@code org.glassfish.jaxb.core.v2.model.annotation.Quick}
     */
    private final JClass quick = codeModel.ref(QUICK);

    /**
     * The writers will be generated into this package.
     */
    private JPackage pkg = codeModel.rootPackage();

    /**
     * Map from annotation classes to their writers.
     */
    private final Map<Class<? extends Annotation>, JDefinedClass> queue = new TreeMap<>(Comparator.comparing(Class::getName));

    public QuickGenMojo() {
    }

    @Override
    public void execute() throws MojoExecutionException {
        if (skip) {
            getLog().info("Skipping execution.");
            return;
        }

        for (String s : classes) {
            patterns.add(Pattern.compile(convertToRegex(s)));
        }
        pkg = codeModel._package(packageName);
        try {
            List<String> classpath = project.getCompileClasspathElements();
            Set<File> files = new HashSet<>();
            List<URL> cp = new ArrayList<>();
            for (String s : classpath) {
                File f = new File(s);
                files.add(f);
                cp.add(f.toURI().toURL());
            }
            userLoader = new URLClassLoader(cp.toArray(new URL[] {}));

            // find classes to be bound
            for (File f : files) {
                if (f.exists()) {
                    if (f.isDirectory()) {
                        processDir(f, "");
                    } else {
                        processJar(f);
                    }
                }
            }

            // [RESULT]
            // class Init {
            //    static Quick[] getAll() {
            //       ... return all Quick classes ...
            //    }
            // }
            try {
                JArray all = JExpr.newArray(quick);
                JDefinedClass init = pkg._class(0, "Init");
                init.method(JMod.STATIC, quick.array(), "getAll").body()._return(all);

                for (Map.Entry<Class<? extends Annotation>, JDefinedClass> e : queue.entrySet()) {
                    process(e.getKey(), e.getValue());
                    all.add(JExpr._new(e.getValue()).arg(JExpr._null()).arg(JExpr._null()));
                }
            } catch (JClassAlreadyExistsException e) {
                throw new MojoExecutionException(e);
            }

            if (!destDir.mkdirs()) {
                getLog().warn(destDir + " failed to be created.");
            }
            CodeWriter core = new FileCodeWriter(destDir, encoding);
            if (license != null) {
                core = new LicenseCodeWriter(core, license, encoding);
            }

            codeModel.build(core);

            if (attach) {
                project.addCompileSourceRoot(destDir.getAbsolutePath());
            }
        } catch (IOException | DependencyResolutionRequiredException e) {
            throw new MojoExecutionException(e);
        } finally {
            userLoader = null;
        }
    }

    /**
     * Generate a {@code org.glassfish.jaxb.runtime.v2.model.annotation.Quick} implementation
     * of the specified annotation in the specified package.
     *
     * @param ann
     *      annotation type to
     */
    private void process(Class<? extends Annotation> ann, JDefinedClass c) {
        // XXX - adding javax.annotation.processing.Generated implies dep on java.compiler,
        //adding jakarta.annotation.Generated implies dep on jakarta.annotation
        // => we don't want any of them
        // [RESULT]
        // public final class XmlAttributeQuick extends Quick implements XmlAttribute {
        c.javadoc().add("<p><b>Auto-generated, do not edit.</b></p>");
        c._extends(quick);
        c._implements(ann);

        // [RESULT]
        //     private final XmlAttribute core;
        JFieldVar $core = c.field(JMod.PRIVATE | JMod.FINAL, ann, "core");

        // [RESULT]
        // public XmlAttributeQuick(Locatable upstream, XmlAttribute core) {
        //     super(upstream);
        //     this.core = core;
        // }
        {
            JMethod m = c.constructor(JMod.PUBLIC);
            m.body().invoke("super").arg(m.param(locatable, "upstream"));
            m.body().assign(JExpr._this().ref($core), m.param(ann, "core"));
        }

        // [RESULT]
        // @Override
        // protected Annotation getAnnotation() {
        //     return core;
        // }
        {
            JMethod m = c.method(JMod.PROTECTED, Annotation.class, "getAnnotation");
            m.annotate(Override.class);
            m.body()._return($core);
        }

        // [RESULT]
        // @Override
        // protected Quick newInstance(Locatable upstream,Annotation core) {
        //     return new XmlAttributeQuick(upstream,(XmlAttribute)core);
        // }
        {
            JMethod m = c.method(JMod.PROTECTED, quick, "newInstance");
            m.annotate(Override.class);
            m.body()._return(JExpr._new(c).arg(m.param(locatable, "upstream")).arg(JExpr.cast(codeModel.ref(ann), m.param(Annotation.class, "core"))));
        }

        // [RESULT]
        // @Override
        // public Class<XmlAttribute> annotationType() {
        //     return XmlAttribute.class;
        // }
        {
            JMethod m = c.method(JMod.PUBLIC,
                    codeModel.ref(Class.class).narrow(ann), "annotationType");
            m.annotate(Override.class);
            m.body()._return(codeModel.ref(ann).dotclass());
        }


        // then for each annotation parameter just generate a delegation method
        Method[] methods = ann.getDeclaredMethods();
        Arrays.sort(methods, Comparator.comparing(Method::getName));
        for (Method method : methods) {
            // [RESULT]
            // @Override
            // public String name() {
            //    return core.name();
            //}
            JMethod m = c.method(JMod.PUBLIC, method.getReturnType(), method.getName());
            m.annotate(Override.class);
            m.body()._return($core.invoke(method.getName()));
        }
    }

    /**
     * Gets the short name from a fully-qualified name.
     */
    private static String getShortName(String className) {
        int idx = className.lastIndexOf('.');
        return className.substring(idx + 1);
    }

    private String convertToRegex(String pattern) {
        StringBuilder regex = new StringBuilder();
        char nc = ' ';
        if (pattern.length() > 0) {
            for (int i = 0; i < pattern.length(); i++) {
                char c = pattern.charAt(i);
                nc = ' ';
                if ((i + 1) != pattern.length()) {
                    nc = pattern.charAt(i + 1);
                }
                //escape single '.'
                if ((c == '.') && (nc != '.')) {
                    regex.append('\\');
                    regex.append('.');
                    //do not allow patterns like a..b
                } else if ((c == '.') && (nc == '.')) {
                    continue;
                    // "**" gets replaced by ".*"
                } else if ((c == '*') && (nc == '*')) {
                    regex.append(".*");
                    break;
                    //'*' replaced by anything but '.' i.e [^\\.]+
                } else if (c == '*') {
                    regex.append("[^\\.]+");
                    continue;
                    //'?' replaced by anything but '.' i.e [^\\.]
                } else if (c == '?') {
                    regex.append("[^\\.]");
                    //else leave the chars as they occur in the pattern
                } else {
                    regex.append(c);
                }
            }
        }
        return regex.toString();
    }


    /**
     * Visits a jar file and looks for classes that match the specified pattern.
     */
    private void processJar(File jarfile) throws MojoExecutionException {
        try (JarFile jar = new JarFile(jarfile)) {
            for (Enumeration<JarEntry> en = jar.entries(); en.hasMoreElements(); ) {
                JarEntry e = en.nextElement();
                process(e.getName(), e.getTime());
            }
        } catch (IOException e) {
            throw new MojoExecutionException("Unable to process " + jarfile, e);
        }
    }

    /**
     * Visits a directory and looks for classes that match the specified pattern.
     *
     * @param prefix
     *      the package name prefix like "" or "foo/bar/"
     */
    private void processDir(File dir, String prefix) throws MojoExecutionException {
        // look for class files
        String[] classes = dir.list((d, name) -> name.endsWith(".class"));
        if (classes != null) {
            for (String c : classes) {
                process(prefix + c, new File(dir, c).lastModified());
            }
        }

        // look for subdirectories
        File[] subdirs = dir.listFiles(File::isDirectory);
        if (subdirs != null) {
            for (File f : subdirs) {
                processDir(f, prefix + f.getName() + '/');
            }
        }
    }

    /**
     * Process a file.
     *
     * @param name such as "jakarta/xml/bind/Abc.class"
     */
    private void process(String name, long timestamp) throws MojoExecutionException {
        if (!name.endsWith(".class")) {
            return; // not a class
        }
        name = name.substring(0, name.length() - 6);
        name = name.replace('/', '.'); // make it a class naem
        // find a match
        for (Pattern p : patterns) {
            if (p.matcher(name).matches()) {
                queue(name, timestamp);
                return;
            }
        }
    }

    /**
     * Queues a file for generation.
     */
    @SuppressWarnings({"unchecked"})
    private void queue(String className, long timestamp) throws MojoExecutionException {
        getLog().debug("Processing " + className);
        Class<? extends Annotation> ann;
        try {
            ann = (Class<? extends Annotation>) userLoader.loadClass(className);
        } catch (ClassNotFoundException e) {
            throw new MojoExecutionException(e);
        }

        if (!Annotation.class.isAssignableFrom(ann)) {
            getLog().debug("Skipping " + className + ". Not an annotation");
            return;
        }

        JDefinedClass w;
        try {
            w = pkg._class(JMod.FINAL, getShortName(ann.getName()) + "Quick");
        } catch (JClassAlreadyExistsException e) {
            throw new MojoExecutionException("Class name collision on " + className, e);
        }

        // up to date check
        String name = pkg.name();
        if (name.length() == 0) {
            name = getShortName(className);
        } else {
            name += '.' + getShortName(className);
        }

        File dst = new File(destDir, name.replace('.', File.separatorChar) + "Quick.java");
        if (dst.exists() && dst.lastModified() > timestamp) {
            getLog().debug("Skipping " + className + ". Up to date.");
            w.hide();
        }

        queue.put(ann, w);
    }
}
