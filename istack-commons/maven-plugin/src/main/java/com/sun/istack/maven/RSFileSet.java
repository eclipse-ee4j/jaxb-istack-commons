/*
 * Copyright (c) 2018 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.sun.istack.maven;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import static java.nio.file.FileVisitResult.CONTINUE;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author lukas
 */
public class RSFileSet {

    private Path root;
    private List<String> inc;
    private final PathMatcher exMatcher;

    public RSFileSet() {
        inc = new ArrayList<>();
        exMatcher = FileSystems.getDefault().getPathMatcher("glob:*_*.properties");
    }

    public void setDirectory(String dir) {
        root = Paths.get(dir);
    }

    public String getDirectory() {
        return root.toString();
    }

    public void addInclude(String include) {
        inc.add(include);
    }

    public List<String> getIncludes() {
        return inc;
    }

    public void setIncludes(List<String> inc) {
        this.inc = inc;
    }

    public Path resolve(Path path) {
        return root.resolve(path);
    }

    public List<Path> getIncludedFiles() {
        final List<Path> result = new ArrayList<>();
        if (inc.isEmpty()) {
            inc.add("**");
        }
        for (final String i : inc) {
            int idx = i.indexOf('/');
            final PathMatcher matcher;
            final PathMatcher dirMatcher;
            if (idx < 0 ) {
                matcher = FileSystems.getDefault().getPathMatcher("glob:" + i);
                dirMatcher = null;
            } else {
                matcher = FileSystems.getDefault().getPathMatcher("glob:" + i.substring(idx + 1));
                dirMatcher = FileSystems.getDefault().getPathMatcher("glob:" + i.substring(0, idx));
            }
            try {
                Files.walkFileTree(root, new SimpleFileVisitor<Path>() {

                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                        if (dirMatcher == null || (dirMatcher.matches(file.getParent()))) {
                        Path name = file.getFileName();
                        if (name != null && matcher.matches(name) && !exMatcher.matches(name)) {
                            result.add(root.relativize(file));
                        }
                        }
                        return CONTINUE;
                    }
                });
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }
        return result;
    }
}
