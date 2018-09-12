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

import java.util.List;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.resolution.DependencyResolutionException;
import org.eclipse.aether.resolution.DependencyResult;
import org.eclipse.aether.artifact.DefaultArtifact;

/**
 *
 * @author lukas
 */
final class DependencyResolver {

    public static DependencyResult resolve(CollectRequest collectRequest, List<RemoteRepository> remoteRepos,
            RepositorySystem repoSystem, RepositorySystemSession repoSession) throws DependencyResolutionException {
        DependencyRequest dependencyRequest = new DependencyRequest(collectRequest, null);
        return repoSystem.resolveDependencies(repoSession, dependencyRequest);
    }

    public static DependencyResult resolve(org.apache.maven.model.Dependency dependency, List<RemoteRepository> remoteRepos,
            RepositorySystem repoSystem, RepositorySystemSession repoSession) throws DependencyResolutionException {
        CollectRequest collectRequest = new CollectRequest(createDependency(dependency), remoteRepos);
        return resolve(collectRequest, remoteRepos, repoSystem, repoSession);
    }

    private static Dependency createDependency(org.apache.maven.model.Dependency d) {
        Artifact artifact = new DefaultArtifact(d.getGroupId(), d.getArtifactId(), "pom", d.getVersion());
        return new Dependency(artifact, d.getScope(), d.isOptional(), null);
    }

}
