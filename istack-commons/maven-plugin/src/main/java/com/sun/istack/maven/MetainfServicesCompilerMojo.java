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

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.impl.ArtifactResolver;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Goal which compiles META-INF/services files from various dependencies
 *
 * @author japod
 *
 * Goal: metainf-services
 *
 * Phase: generate-sources
 */
@Mojo(name = "metainf-services", defaultPhase = LifecyclePhase.GENERATE_SOURCES, threadSafe = true)
public class MetainfServicesCompilerMojo extends AbstractMojo {

    /**
     * Collection of ArtifactItems to take the META-INF/services from. (ArtifactItem contains groupId,
     * artifactId, version)
     *
     * parameter
     * required
     * @since 1.0
     */
    private List<ArtifactItem> artifactItems;
    /**
     * Collection of files from META-INF/services to compile
     *
     * parameter
     * required
     * @since 1.0
     */
    private List<String> providers;
    /**
     * Destination for the generated service registry files
     *
     * parameter
     * required
     * @since 1.0
     */
    private File destDir;

    /**
     * component role="org.apache.maven.artifact.resolver.ArtifactResolver"
     * required
     * readonly
     */
    protected ArtifactResolver artifactResolver;
    /**
     * parameter expression="${project.remoteArtifactRepositories}"
     * readonly
     * required
     */
    protected List<RemoteRepository> remoteRepositories;

    /**
     * The current repository/network configuration of Maven.
     *
     * parameter default-value="${repositorySystemSession}"
     * readonly
     */
    private RepositorySystemSession repoSession;

    @Override
    public void execute() throws MojoExecutionException {
        getLog().info("About to compile META-INF/services files");
        getLog().info("Artifact Items = " + artifactItems);
        getLog().info("SPIs = " + providers);
        getLog().info("dest dir = " + destDir);
        File msDir = new File(destDir, "META-INF/services");
        boolean created =msDir.mkdirs();
        if (!created) {
            Logger.getLogger(MetainfServicesCompilerMojo.class.getName()).log(Level.FINE, "Cannot create directory: {0}", msDir);
        }
        for (String spi : providers) {
            PrintWriter registryWriter = null;
            try {
                File spiRegistry = new File(msDir, spi);
                if (spiRegistry.exists()) {
                    boolean deleted = spiRegistry.delete();
                    if (!deleted) {
                        Logger.getLogger(MetainfServicesCompilerMojo.class.getName()).log(Level.FINE, "Cannot delete file: {0}", spiRegistry);
                    }
                }
                created = spiRegistry.createNewFile();
                if (!created) {
                    Logger.getLogger(MetainfServicesCompilerMojo.class.getName()).log(Level.FINE, "Cannot create file: {0}", spiRegistry);
                }
                registryWriter = new PrintWriter(spiRegistry);
                ZipFile zipFile = null;
                try {
                    BufferedReader reader = null;
                    InputStreamReader isReader = null;
                    for (ArtifactItem ai : artifactItems) {
                        Artifact artifact;
                        artifact = new DefaultArtifact(ai.getGroupId(), ai.getArtifactId(), null, ai.getVersion());
//                        artifact = artifactFactory.createExtensionArtifact(ai.getGroupId(), ai.getArtifactId(), VersionRange.createFromVersion(ai.getVersion()));

                        ArtifactRequest request = new ArtifactRequest();
                        request.setArtifact( artifact );
                        request.setRepositories(remoteRepositories);
                        artifactResolver.resolveArtifact(repoSession, request);
                        zipFile = new ZipFile(artifact.getFile());
                        final ZipEntry servicesEntry = zipFile.getEntry("META-INF/services/" + spi);
                        if (servicesEntry != null) {
                            try {
                                final InputStream inputStream = zipFile.getInputStream(servicesEntry);
                                isReader = new InputStreamReader(inputStream);
                                reader = new BufferedReader(isReader);
                                while (reader.ready()) {
                                    registryWriter.println(reader.readLine());
                                }
                            } finally {
                                if (reader != null) {
                                    reader.close();
                                }
                                if (isReader != null) {
                                    isReader.close();
                                }
                            }
                        }
                    }
                } catch (ArtifactResolutionException ex) {
                    Logger.getLogger(MetainfServicesCompilerMojo.class.getName()).log(Level.SEVERE, null, ex);
                    throw new MojoExecutionException("Can not resolve artifact!", ex);
                } finally {
                    if (zipFile != null) {
                        zipFile.close();
                    }
                }
            } catch (IOException ex) {
                    throw new MojoExecutionException("Can not create spi registry file!", ex);
            } finally {
                if (registryWriter != null) {
                    registryWriter.close();
                }
            }
        }
    }
}
