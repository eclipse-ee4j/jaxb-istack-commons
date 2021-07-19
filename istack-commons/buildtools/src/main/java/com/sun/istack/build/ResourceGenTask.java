/*
 * Copyright (c) 1997, 2021 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.sun.istack.build;

import com.sun.codemodel.CodeWriter;
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
import com.sun.codemodel.writer.FileCodeWriter;
import com.sun.codemodel.writer.FilterCodeWriter;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.FileSet;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Generate source files from resource bundles,
 * so that code can refer to resources as methods,
 * instead of hard-coding string constants, which is
 * much harder to search.
 *
 * @author Lukas Jungmann
 * @author Kohsuke Kawaguchi
 */
public class ResourceGenTask extends Task {
    /**
     * Resource files to be compiled
     */
    private FileSet resources;

    private File destDir;

    /**
     * @since 2.12
     */
    private File license;

    /**
     * @since 2.12
     */
    private String localizationUtilitiesPkgName;

    /**
     * @since 2.12
     */
    private String encoding;

    public void addConfiguredResource( FileSet fs ) {
        resources = fs;
    }

    public void setDestDir(File dir) {
        this.destDir = dir;
    }

    /**
     * @param license
     * @since 2.12
     */
    public void setLicense(File license) {
        this.license = license;
    }

    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }

    /**
     * @param localizationUtilitiesPkgName
     * @since 2.12
     */
    public void setLocalizationUtilitiesPkgName(String localizationUtilitiesPkgName) {
        this.localizationUtilitiesPkgName = localizationUtilitiesPkgName;
    }

    @Override
    public void execute() throws BuildException {
        if(resources==null)
            throw new BuildException("No resource file is specified");
        if(destDir==null)
            throw new BuildException("No destdir attribute is specified");

        if(localizationUtilitiesPkgName == null) {
            localizationUtilitiesPkgName = "com.sun.istack.localization";
        }

        if (!destDir.exists() && !destDir.mkdirs()) {
                throw new BuildException("Cannot create destdir");
        }

        if (!destDir.canWrite()) {
            throw new BuildException("Cannot write to destdir");
        }

        if (encoding == null || encoding.trim().length() == 0) {
            encoding =  System.getProperty("file.encoding");
            log("File encoding has not been set, using platform encoding "
                    + encoding + ", i.e. build is platform dependent!",
                    Project.MSG_WARN);
        }

        JCodeModel cm = new JCodeModel();

        DirectoryScanner ds = resources.getDirectoryScanner(getProject());
        String[] includedFiles = ds.getIncludedFiles();
        File baseDir = ds.getBasedir();

        for (String value : includedFiles) {
            File res = new File(baseDir, value);

            if(res.getName().contains("_"))
                continue;   // this is a localized bundle, so ignore.

            String className = getClassName(res);

            String bundleName = value.substring(0, value.lastIndexOf('.')).replace('/', '.').replace('\\', '.');// cut off '.properties'
            String dirName = bundleName.substring(0, bundleName.lastIndexOf('.'));

            File destFile = new File(new File(destDir,dirName.replace('.','/')),className+".java");
            if(destFile.lastModified() >= res.lastModified()) {
                log("Skipping "+res,Project.MSG_INFO);
                continue;
            }

            log("Processing "+res,Project.MSG_INFO);
            JPackage pkg = cm._package(dirName);

            Properties props = new Properties();
            FileInputStream in = null;
            try {
                in = new FileInputStream(res);
                props.load(in);
            } catch (IOException e) {
                throw new BuildException(e.getMessage(), e);
            } finally {
                if (in != null) {
                    try {
                        in.close();
                    } catch (IOException ioe) {
                        throw new BuildException(ioe.getMessage(), ioe);
                    }
                }
            }

            JDefinedClass clazz;
            try {
                clazz = pkg._class(JMod.PUBLIC | JMod.FINAL, className);
            } catch (JClassAlreadyExistsException e) {
                throw new BuildException("Name conflict "+className);
            }

            clazz.javadoc().add(
                "Defines string formatting method for each constant in the resource file"
            );

            /*
              [RESULT]

                LocalizableMessageFactory messageFactory =
                    new LocalizableMessageFactory("com.sun.xml.ws.resources.client");
                Localizer localizer = new Localizer();
            */

            JClass lmf_class;
            JClass l_class;
            JClass lable_class;
            try {
                lmf_class = cm.parseType(addLocalizationUtilityPackageName("LocalizableMessageFactory")).boxify();
                l_class = cm.parseType(addLocalizationUtilityPackageName("Localizer")).boxify();
                lable_class = cm.parseType(addLocalizationUtilityPackageName("Localizable")).boxify();
            } catch (ClassNotFoundException e) {
                throw new BuildException(e); // impossible -- but why parseType throwing ClassNotFoundExceptoin!?
            }

            JFieldVar $msgFactory = clazz.field(JMod.PRIVATE|JMod.STATIC|JMod.FINAL,
                lmf_class, "messageFactory", JExpr._new(lmf_class).arg(JExpr.lit(bundleName)));

            JFieldVar $localizer = clazz.field(JMod.PRIVATE|JMod.STATIC|JMod.FINAL,
                l_class, "localizer", JExpr._new(l_class));

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
                method2.javadoc().add(e.getValue());

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
            throw new BuildException("Failed to generate code",e);
        }
    }

    private String addLocalizationUtilityPackageName(final String className) {
        return String.format("%s.%s", localizationUtilitiesPkgName, className);
    }

    /**
     * Counts the number of arguments.
     */
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

    /**
     * Writes all the source files under the specified file folder and
     * inserts a license file each java source file.
     *
     * @author Jitendra Kotamraju
     *
     */
    public static class LicenseCodeWriter extends FilterCodeWriter {
        private final File license;

        /**
         * @param core
         *      This CodeWriter will be used to actually create a storage for files.
         *      LicenseCodeWriter simply decorates this underlying CodeWriter by
         *      adding prolog comments.
         * @param license license File
         * @param encoding
         */
        public LicenseCodeWriter(CodeWriter core, File license, String encoding) {
            super(core);
            this.license = license;
            this.encoding = encoding;
        }

        @Override
        public Writer openSource(JPackage pkg, String fileName) throws IOException {
            Writer w = super.openSource(pkg,fileName);

            PrintWriter out = new PrintWriter(w);
            FileInputStream fin = null;
            try {
                fin = new FileInputStream(license);
                byte[] buf = new byte[8192];
                int len;
                while ((len=fin.read(buf)) != -1) {
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
