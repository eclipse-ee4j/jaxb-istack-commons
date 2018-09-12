/*
 * Copyright (c) 2013, 2018 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.oracle.istack.maven;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.DependencyResolutionException;
import org.eclipse.aether.resolution.DependencyResult;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Martin Grebac
 */
public class PropertyResolver {
    
    private final CommonLogger logger;
    
    private final MavenXpp3Reader mavenreader = new MavenXpp3Reader();

    private final Properties properties;

    private final RepositorySystemSession repoSession;
    
    private final RepositorySystem repoSystem;
    
    private final List<RemoteRepository> pluginRepos;
    
    PropertyResolver(CommonLogger logger, Properties properties, RepositorySystemSession session, 
            RepositorySystem repoSystem, List<RemoteRepository> pluginRepositories) {
        this.logger = logger;
        this.properties = properties;
        this.repoSession = session;
        this.repoSystem = repoSystem;
        this.pluginRepos = pluginRepositories;
    }
            
    /**
     *
     * @param project maven project
     * @throws FileNotFoundException properties not found
     * @throws IOException IO error
     * @throws XmlPullParserException error parsing xml
     */
    public void resolveProperties(MavenProject project) throws FileNotFoundException, IOException, XmlPullParserException {
        logger.info("Resolving properties for " + project.getGroupId() + ":" + project.getArtifactId());
 
        Model model = null;
        FileReader reader;
        try {
            reader = new FileReader(project.getFile());
            model = mavenreader.read(reader);
        } catch (FileNotFoundException ex) {
             Logger.getLogger(ImportPropertiesMojo.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(ImportPropertiesMojo.class.getName()).log(Level.SEVERE, null, ex);
        }
        MavenProject loadedProject = new MavenProject(model);

        DependencyManagement dm = loadedProject.getDependencyManagement();        
        if (dm == null) {
            logger.warn("No dependency management section found in: "  + loadedProject.getGroupId() + ":" + loadedProject.getArtifactId());
            return;
        }

        List<Dependency> depList = dm.getDependencies();

        DependencyResult result;
        for (Dependency d : depList) {
            if ("import".equals(d.getScope())) {
                try {
                    String version = d.getVersion();
                    logger.info("Imported via import-scope: " + d.getGroupId() + ":" + d.getArtifactId() + ":" + version);
                    if (version.contains("$")) {
                        version = properties.getProperty(version.substring(version.indexOf('{')+1, version.lastIndexOf('}')));
                        logger.info("Imported version resolved to: " + version);
                    }
                    d.setVersion(version);
                    d.setType("pom");
                    d.setClassifier("pom");
                    result = DependencyResolver.resolve(d, pluginRepos, repoSystem, repoSession);                                        
                    Artifact a = result.getArtifactResults().get(0).getArtifact();
                    reader = new FileReader(a.getFile());
                    Model m = mavenreader.read(reader);
                    MavenProject p = new MavenProject(m);
                    p.setFile(a.getFile());
                    for (Map.Entry<Object,Object> e : p.getProperties().entrySet()) {
                        logger.info("Setting property: " + (String)e.getKey() + ":" + (String)e.getValue());
                        properties.setProperty((String)e.getKey(), (String)e.getValue());
                    }

                    resolveProperties(p);
                } catch (DependencyResolutionException ex) {
                    Logger.getLogger(ImportPropertiesMojo.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }        
    }

    
}
