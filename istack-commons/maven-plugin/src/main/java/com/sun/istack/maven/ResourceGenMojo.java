/*
 * Copyright (c) 1997, 2021 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.sun.istack.maven;

import com.sun.codemodel.CodeWriter;
import com.sun.codemodel.JAnnotationUse;
import com.sun.codemodel.JClass;
import com.sun.codemodel.JClassAlreadyExistsException;
import com.sun.codemodel.JCodeModel;
import com.sun.codemodel.JDefinedClass;
import com.sun.codemodel.JExpr;
import com.sun.codemodel.JFieldVar;
import com.sun.codemodel.JInvocation;
import com.sun.codemodel.JMethod;
import com.sun.codemodel.JMod;
import com.sun.codemodel.JPackage;
import com.sun.codemodel.JVar;
import com.sun.codemodel.writer.FileCodeWriter;
import com.sun.codemodel.writer.FilterCodeWriter;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;


/**
 * Goal which generates source files from resource bundles.
 * You can then refer to resources as methods rather than hard-coding string constants.
 *
 * @author Lukas Jungmann
 * @author Jakub Podlesak
 * @author Kohsuke Kawaguchi
 *
 * Goal: rs-gen
 * RequiresProject: false
 * Phase: process-sources
 */
@Mojo(name = "rs-gen", requiresProject = false, threadSafe = true,
        defaultPhase = LifecyclePhase.PROCESS_SOURCES)
public class ResourceGenMojo extends AbstractMojo {

    /**
     * Location of the destination directory.
     */
    @Parameter(required = true, property = "destDir", defaultValue = "${project.build.directory}/generated-sources/resources")
    private File destDir;

    /**
     * File set of the properties files to be processed.
     */
    @Parameter
    private RSFileSet resources;

    /**
     * Directory with properties files to be processed from the command line.
     * @since 2.12
     */
    @Parameter(property = "resources")
    private String cliResource;

    /**
     * Package to be used for the localization utility classes.
     */
    @Parameter(property = "localizationUtilitiesPkgName", defaultValue = "com.sun.istack.localization")
    private String localizationUtilitiesPkgName;

    /**
     * License file to use in generated sources.
     * @since 2.12
     */
    @Parameter(property = "license")
    private File license;

    /**
     * Mark generated sources with {@code @jakarta.annotation.Generated}.
     * @since 3.0.5
     */
    @Parameter(property = "atGenerated", defaultValue = "false")
    private boolean atGenerated;

    /**
     * Generate javadoc comments.
     * @since 4.0.1
     */
    @Parameter(property = "rs.javadoc", defaultValue = "true")
    private boolean javadoc;

    /**
     * File encoding for generated sources.
     * @since 2.12
     */
    @Parameter(property = "encoding", defaultValue = "${project.build.sourceEncoding}")
    private String encoding;

    /**
     * @since 2.12
     */
    @Parameter(property = "project", readonly = true)
    private MavenProject project;

    @Override
    public void execute() throws MojoExecutionException {

        if(resources == null && cliResource == null) {
            throw new MojoExecutionException("No resource file is specified");
        }
        if(destDir == null) {
            throw new MojoExecutionException("No destdir attribute is specified");
        }

        if(localizationUtilitiesPkgName == null) {
            localizationUtilitiesPkgName = "com.sun.istack.localization";
        }

        if (!destDir.exists() && !destDir.mkdirs()) {
                throw new MojoExecutionException("Cannot create destdir");
        }

        if (!destDir.canWrite()) {
            throw new MojoExecutionException("Cannot write to destdir");
        }

        if (encoding == null || encoding.trim().isEmpty()) {
            encoding =  System.getProperty("file.encoding");
            getLog().warn("File encoding has not been set, using platform encoding "
                    + encoding + ", i.e. build is platform dependent!");
        }

        if (resources == null && cliResource != null) {
            RSFileSet fs = new RSFileSet();
            fs.setDirectory(System.getProperty("user.dir"));
            List<String> l = new ArrayList<>();
            l.add(cliResource);
            fs.setIncludes(l);
            resources = fs;
        }

        if (resources == null || !resources.exists()) {
            getLog().info("No resources specified.");
            return;
        }

        List<Path> includedFiles = resources.getIncludedFiles();

        getLog().info("Resources:");
        for(Path s : includedFiles) {
            getLog().info(s.toString());
        }

        JCodeModel cm = new JCodeModel();

        for (Path p : includedFiles) {
            File res = resources.resolve(p).toFile();
            String value = p.toString();

            String className = getClassName(res);

            String bundleName = value.substring(0, value.lastIndexOf('.')).replace('/', '.').replace('\\', '.');// cut off '.properties'
            String dirName = bundleName.substring(0, bundleName.lastIndexOf('.'));

            File destFile = destDir.toPath().resolve(dirName.replace('.', '/')).resolve(className+".java").toFile();
            if(destFile.exists() && (destFile.lastModified() >= res.lastModified())) {
                getLog().info("Skipping " + res);
                continue;
            }

            getLog().info("Processing "+res);

            if (!destFile.getParentFile().mkdirs()) {
                if (getLog().isDebugEnabled()) {
                    getLog().debug("Reusing existing " + destFile.getParentFile().getAbsolutePath() + " directory.");
                }
            }
            JPackage pkg = cm._package(dirName);

            Properties props = new Properties();
            FileInputStream in = null;
            try {
                in = new FileInputStream(res);
                props.load(in);
            } catch (IOException e) {
                throw new MojoExecutionException(e.getMessage(), e);
            } finally {
                if (in != null) {
                    try {
                        in.close();
                    } catch (IOException ioe) {
                        throw new MojoExecutionException(ioe.getMessage(), ioe);
                    }
                }
            }

            JDefinedClass clazz;
            try {
                clazz = pkg._class(JMod.PUBLIC | JMod.FINAL, className);
            } catch (JClassAlreadyExistsException e) {
                throw new MojoExecutionException("Name conflict "+className);
            }

            if (javadoc) {
                clazz.javadoc().add(
                        "Defines string formatting method for each constant in the resource file"
                );
            }

            if (atGenerated) {
                // no direct dependency on Jakarta annotations API
                JClass annotation = cm.ref("jakarta.annotation.Generated");
                JAnnotationUse generated = clazz.annotate(annotation);
                generated.param("value", ResourceGenMojo.class.getName());
            }

            /*
              [RESULT]

            String BUNDLE_NAME = "com.sun.xml.ws.resources.client";
            LocalizableMessageFactory MESSAGE_FACTORY =
                    new LocalizableMessageFactory(BUNDLE_NAME, new BundleSupplier());
            Localizer LOCALIZER = new Localizer();

            class BundleSupplier implements ResourceBundleSupplier {

                public ResourceBundle getResourceBundle(Locale locale) {
                    return ResourceBundle.getBundle(BUNDLE_NAME, locale);
                }
            }
            */

            JClass lmf_class, l_class, lable_class;
            JClass supplier_class, rbundle_class, locale_class;
            try {
                lmf_class = cm.parseType(addLocalizationUtilityPackageName("LocalizableMessageFactory")).boxify();
                l_class = cm.parseType(addLocalizationUtilityPackageName("Localizer")).boxify();
                lable_class = cm.parseType(addLocalizationUtilityPackageName("Localizable")).boxify();
                supplier_class = cm.parseType(addLocalizationUtilityPackageName("LocalizableMessageFactory.ResourceBundleSupplier")).boxify();
                rbundle_class = cm.parseType("java.util.ResourceBundle").boxify();
                locale_class = cm.parseType("java.util.Locale").boxify();
            } catch (ClassNotFoundException e) {
                throw new MojoExecutionException(e.getMessage(), e); // impossible -- but why parseType throwing ClassNotFoundExceptoin!?
            }

            JFieldVar $bundle = clazz.field(JMod.PRIVATE | JMod.STATIC | JMod.FINAL,
                    String.class, "BUNDLE_NAME", JExpr.lit(bundleName));

            JDefinedClass supplier;
            try {
                supplier = clazz._class(JMod.PRIVATE | JMod.STATIC, "BundleSupplier");
                supplier._implements(supplier_class);
                JMethod rb = supplier.method(JMod.PUBLIC, rbundle_class, "getResourceBundle");
                JVar ploc = rb.param(locale_class, "locale");
                rb.body()._return(rbundle_class.staticInvoke("getBundle").arg($bundle).arg(ploc));
            } catch (JClassAlreadyExistsException e) {
                throw new MojoExecutionException(e.getMessage(), e);
            }

            JFieldVar $msgFactory = clazz.field(JMod.PRIVATE | JMod.STATIC | JMod.FINAL,
                lmf_class, "MESSAGE_FACTORY", JExpr._new(lmf_class).arg($bundle).arg(JExpr._new(supplier)));
            JFieldVar $localizer = clazz.field(JMod.PRIVATE | JMod.STATIC | JMod.FINAL,
                l_class, "LOCALIZER", JExpr._new(l_class));

            for (Map.Entry<Object,Object> e : props.entrySet()) {
                // [RESULT]
                // Localizable METHOD_localizable(Object arg1, Object arg2, ...) {
                //   return messageFactory.getMessage("servlet.html.notFound", message));
                // }
                // String METHOD(Object arg1, Object arg2, ...) {
                //   return localizer.localize(METHOD_localizable(arg1,arg2,...));
                // }
                String methodBaseName = NameConverter.smart.toConstantName(e.getKey().toString());

                JMethod method = clazz.method(JMod.PUBLIC | JMod.STATIC, lable_class, "localizable"+methodBaseName);

                int countArgs = countArgs(e.getValue().toString());

                JInvocation format = $msgFactory.invoke("getMessage").arg(
                    JExpr.lit(e.getKey().toString()));

                for( int i=0; i<countArgs; i++ ) {
                    format.arg( method.param(Object.class,"arg"+i));
                }
                method.body()._return(format);

                JMethod method2 = clazz.method(JMod.PUBLIC|JMod.STATIC, String.class, methodBaseName);
                if (javadoc) {
                    method2.javadoc().add(escape(e.getValue().toString()));
                }

                JInvocation localize = JExpr.invoke(method);
                for( int i=0; i<countArgs; i++ ) {
                    localize.arg( method2.param(Object.class,"arg"+i));
                }

                method2.body()._return($localizer.invoke("localize").arg(localize));
            }
        }

        try {
            CodeWriter core = new FileCodeWriter(destDir, encoding);
            if (license != null) {
                core = new LicenseCodeWriter(core, license, encoding);
            }
            cm.build(core);
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to generate code",e);
        }

        if (project != null) {
            project.addCompileSourceRoot(destDir.getAbsolutePath());
        }
    }

    private String addLocalizationUtilityPackageName(final String className) {
        return String.format("%s.%s", localizationUtilitiesPkgName, className);
    }

    private int countArgs(String value) {
        List<String> x = new ArrayList<>();

        while(true) {
            String r1 = MessageFormat.format(value, x.toArray());
            x.add("xxxx");
            String r2 = MessageFormat.format(value, x.toArray());

            if(r1.equals(r2))
                return x.size()-1;
        }
    }

    /**
     * Computes the class name from the resource bundle name.
     */
    private String getClassName(File res) {
        String name = res.getName();
        int suffixIndex = name.lastIndexOf('.');
        name = name.substring(0,suffixIndex);
        return NameConverter.smart.toClassName(name)+"Messages";
    }

    private String escape(String s) {
        return s.replaceAll("<", "{@code <}").replaceAll(">", "{@code >}");
    }

    /**
     * Writes all the source files under the specified file folder and inserts a
     * license file each java source file.
     *
     * @author Jitendra Kotamraju
     *
     */
    public static class LicenseCodeWriter extends FilterCodeWriter {

        private final File license;

        /**
         * @param core This CodeWriter will be used to actually create a storage
         * for files. LicenseCodeWriter simply decorates this underlying
         * CodeWriter by adding prolog comments.
         * @param license license File
         * @param encoding encoding
         */
        public LicenseCodeWriter(CodeWriter core, File license, String encoding) {
            super(core);
            this.license = license;
            this.encoding = encoding;
        }

        @Override
        public Writer openSource(JPackage pkg, String fileName) throws IOException {
            Writer w = super.openSource(pkg, fileName);

            PrintWriter out = new PrintWriter(w);
            FileInputStream fin = null;
            try {
                fin = new FileInputStream(license);
                byte[] buf = new byte[8192];
                int len;
                while ((len = fin.read(buf)) != -1) {
                    out.write(new String(buf, 0, len));
                }
            } finally {
                if (fin != null) {
                    fin.close();
                }
            }
            out.flush();    // we can't close the stream for that would close the undelying stream.

            return w;
        }
    }
}
