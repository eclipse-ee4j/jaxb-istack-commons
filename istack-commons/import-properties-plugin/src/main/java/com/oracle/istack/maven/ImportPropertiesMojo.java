/*
 * Copyright (c) 2013, 2023 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.oracle.istack.maven;

import java.io.IOException;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.repository.RemoteRepository;

/**
 * The import-properties goal imports all properties from scope-import poms
 * (boms) and sets them as project properties.
 *
 * @author Martin Grebac
 */
@Mojo(name = "import-pom-properties", threadSafe = true,
        defaultPhase = LifecyclePhase.COMPILE, requiresDependencyResolution = ResolutionScope.NONE)
public class ImportPropertiesMojo extends AbstractMojo {

    /**
     * The Maven Project Object.
     */
    @Parameter(defaultValue = "${project}", readonly = true)
    protected MavenProject project;

    /**
     * The entry point to Aether, i.e. the component doing all the work.
     *
     * @since 2.3.1
     */
    @Component
    private RepositorySystem repoSystem;

    /**
     * The current repository/network configuration of Maven.
     *
     * @since 2.3.1
     */
    @Parameter(defaultValue = "${repositorySystemSession}", readonly = true)
    private RepositorySystemSession repoSession;

    /**
     * The project's remote repositories to use for the resolution of project
     * and its dependencies.
     *
     * @since 2.3.1
     */
    @Parameter(defaultValue = "${project.remoteProjectRepositories}", readonly = true)
    private List<RemoteRepository> projectRepos;

    private Properties projectProperties = null;

    @Override
    public void execute() throws MojoExecutionException {
        try {
            projectProperties = project.getProperties();

            MavenProject bomProject = project;
            while (bomProject != null && !bomProject.getArtifactId().endsWith("-bom")) {
                bomProject = bomProject.getParent();
            }

            if (bomProject == null || !bomProject.getArtifactId().endsWith("-bom")) {
                getLog().warn("No '*-bom' project found in project hierarchy, using this project's pom for import search.");
                bomProject = project;
            }

            getLog().warn("Searching project: " + bomProject.getArtifactId());

            PropertyResolver resolver = new PropertyResolver(new CommonLogger(getLog()), projectProperties, repoSession, repoSystem, projectRepos);
            resolver.resolveProperties(bomProject);

        } catch (XmlPullParserException | IOException ex) {
            Logger.getLogger(ImportPropertiesMojo.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

}
