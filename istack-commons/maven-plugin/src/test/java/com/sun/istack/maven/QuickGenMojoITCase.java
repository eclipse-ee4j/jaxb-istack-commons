/*
 * Copyright (c) 2023 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.sun.istack.maven;

import org.junit.Assert;
import org.junit.Test;

import java.io.File;

public class QuickGenMojoITCase {

    private static final File PROJECTS_DIR = new File(System.getProperty("it.projects.dir"));

    public QuickGenMojoITCase() {
    }

    @Test
    public void testGeneration() {
        File project = new File(PROJECTS_DIR, "sample");
        File f = new File(project, "target/generated-sources/quick-sources/org/app/annotation/XmlEnumQuick.java");
        Assert.assertTrue("Not found " + f.getAbsolutePath(), f.exists());
        f = new File(project, "target/classes/org/app/annotation/Init.class");
        Assert.assertFalse("Found " + f.getAbsolutePath(), f.exists());
    }
}
