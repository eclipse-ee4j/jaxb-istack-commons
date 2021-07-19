/*
 * Copyright (c) 1997, 2021 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.sun.istack.test;

import java.util.Locale;
import java.util.StringTokenizer;

/**
 * Immutable representation of a dot-separated digits (such as "1.0.1").
 *
 * {@link VersionNumber}s are {@link Comparable}.
 *
 * <p>
 * We allow a component to be not just a number, but also "ea", "ea1", "ea2".
 * "ea" is treated as "ea0", and {@code eaN < M for any M > 0}.
 *
 * <p>
 * '*' is also allowed as a component, and {@code '*' > M} for any {@code M > 0}.
 *
 * <pre>
 * {@code 2.0.* > 2.0.1 > 2.0.0 > 2.0.ea > 2.0 }
 * </pre>
 *
 * @author
 *     Kohsuke Kawaguchi (kohsuke.kawaguchi@sun.com)
 */
public class VersionNumber implements Comparable<VersionNumber> {

    public static final VersionNumber v1_0   = new VersionNumber("1.0");
    public static final VersionNumber v1_0_1 = new VersionNumber("1.0.1");
    public static final VersionNumber v1_0_2 = new VersionNumber("1.0.2");
    public static final VersionNumber v1_0_3 = new VersionNumber("1.0.3");
    public static final VersionNumber v2_0   = new VersionNumber("2.0");
    public static final VersionNumber v2_1   = new VersionNumber("2.1");

    private final int[] digits;

    /**
     * Parses a string like "1.0.2" into the version number.
     *
     * @param num string to parse
     * @throws IllegalArgumentException
     *      if the parsing fails.
     */
    public VersionNumber( String num ) {
        StringTokenizer tokens = new StringTokenizer(num,".");
        digits = new int[tokens.countTokens()];
        if(digits.length<2)
            throw new IllegalArgumentException();

        int i=0;
        while( tokens.hasMoreTokens() ) {
            String token = tokens.nextToken().toLowerCase(Locale.ENGLISH);
            if(token.equals("*")) {
                digits[i++] = 1000;
            } else
            if(token.startsWith("ea")) {
                if(token.length()==2)
                    digits[i++] = -1000;    // just "ea"
                else
                    digits[i++] = -1000 + Integer.parseInt(token.substring(2)); // "eaNNN"
            } else {
                digits[i++] = Integer.parseInt(token);
            }
        }
    }

    @Override
    public String toString() {
        StringBuffer buf = new StringBuffer();
        for( int i=0; i<digits.length; i++ ) {
            if(i!=0)    buf.append('.');
            buf.append( Integer.toString(digits[i]) );
        }
        return buf.toString();
    }

    public boolean isOlderThan( VersionNumber rhs ) {
        return compareTo(rhs)<0;
    }

    public boolean isNewerThan( VersionNumber rhs ) {
        return compareTo(rhs)>0;
    }


    @Override
    public boolean equals( Object o ) {
        if (o instanceof VersionNumber) {
            return compareTo((VersionNumber)o)==0;
        }
        return false;
    }

    @Override
    public int hashCode() {
        int x=0;
        for (int i : digits)
            x = (x << 1) | i;
        return x;
    }

    @Override
    public int compareTo(VersionNumber rhs) {
        for( int i=0; ; i++ ) {
            if( i==this.digits.length && i==rhs.digits.length )
                return 0;   // equals
            if( i==this.digits.length )
                return -1;  // rhs is larger
            if( i==rhs.digits.length )
                return 1;

            int r = this.digits[i] - rhs.digits[i];
            if(r!=0)    return r;
        }
    }
}
