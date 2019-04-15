/*
 * Copyright (c) 1997, 2019 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.sun.istack.test;

import java.util.HashSet;
import java.util.Set;
import java.util.StringTokenizer;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Represents a range of versions.
 *
 * @author
 *     Kohsuke Kawaguchi (kohsuke.kawaguchi@sun.com)
 */
public final class VersionProcessor {
    /**
     * This test is only applicable to the RI of this version or later.
     * can be null.
     */
    private final VersionNumber since;

    /**
     * This test is only applicable to the RI of this version or younger.
     * can be null.
     */
    private final VersionNumber until;

    /**
     * This test shall be excluded from the RI of versions listed here.
     */
    private final Set<Object> excludeVersions;

    /**
     * Special version number constant to represent ALL in
     * {@link #excludeVersions}.
     */
    private static final Object ALL_VERSION = new Object();

    /**
     * Creates a default {@link VersionProcessor} that accepts
     * any version.
     */
    private VersionProcessor() {
        since = null;
        until = null;
        excludeVersions = null;
    }

    public VersionProcessor( String sinceValue, String untilValue, String excludeFromValue ) {
        if( sinceValue!=null && !sinceValue.trim().isEmpty())
            since = new VersionNumber( sinceValue );
        else
            since = null;

        if( untilValue!=null && !untilValue.trim().isEmpty())
            until = new VersionNumber( untilValue );
        else
            until = null;

        if( excludeFromValue!=null && !excludeFromValue.trim().isEmpty()) {
            excludeVersions = new HashSet<Object>();
            String v = excludeFromValue.trim();
            if(v.equals("all")) {
                excludeVersions.add(ALL_VERSION);
            } else {
                StringTokenizer tokens = new StringTokenizer( v );
                while(tokens.hasMoreTokens())
                    excludeVersions.add( new VersionNumber( tokens.nextToken() ) );
            }
        } else
            excludeVersions = null;
    }

    public VersionProcessor( Document testSpecMeta ) {
        this(testSpecMeta.getDocumentElement());
    }

    public VersionProcessor( Element e ) {
        this(e.getAttribute("since"), e.getAttribute("until"), e.getAttribute("excludeFrom"));
    }

    /**
     * Checks if the test is valid against the JAXB RI of
     * the specified version.
     */
    public boolean isApplicable(VersionNumber v) {
        if( excludeVersions!=null ) {
            if( excludeVersions.contains(ALL_VERSION)
            ||  excludeVersions.contains(v) )
                return false;
        }

        if(since!=null && since.isNewerThan(v))
            return false;

        if(until!=null && v.isNewerThan(until))
            return false;

        return true;
    }

    /**
     * Default {@link VersionProcessor} that accepts any version.
     */
    public static final VersionProcessor DEFAULT = new VersionProcessor();
}
