/*
 * Copyright (c) 2012, 2023 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.sun.istack.maven;

import java.io.File;

import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author Lukas Jungmann
 */
public class ResourceGenMojoITCase {

    private static final File PROJECTS_DIR = new File(System.getProperty("it.projects.dir"));

    public ResourceGenMojoITCase() {
    }

    @Test
    public void testGeneration() {
        File project = new File(PROJECTS_DIR, "sample");
        File f = new File(project, "target/generated-sources/resources/org/aaa/ApropMessages.java");
        Assert.assertTrue("Not found " + f.getAbsolutePath(), f.exists());
        f = new File(project, "target/classes/org/aaa/ApropMessages.class");
        Assert.assertTrue("Not found " + f.getAbsolutePath(), f.exists());
    }
}
