/*
 * Copyright (c) 2012, 2018 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

log.info("Checking proxy...")
def itsettings = new XmlParser().parse(project.build.testResources.directory[0] + "/it-settings.xml")
def itproxy = ""
if (settings?.proxies) {
    Node proxies = new Node(itsettings, "proxies")
    settings?.proxies?.each { proxy ->
        if (proxy.active) {
            if ("http".equals(proxy.protocol)) {
                itproxy =  "-Dhttp.proxyHost=" + proxy.host
                if (proxy.port) {
                    itproxy += " -Dhttp.proxyPort=" + proxy.port
                }
            }
            def p = new Node(proxies, "proxy")
            new Node(p, "protocol", proxy.protocol)
            new Node(p, "port", proxy.port)
            if (proxy.username) {new Node(p, "username", proxy.username)}
            if (proxy.password) {new Node(p, "password", proxy.password)}
            new Node(p, "host", proxy.host)
            new Node(p, "active", proxy.active)
            new Node(p, "nonProxyHosts", proxy.nonProxyHosts)
        }
    }
}

if (itproxy.trim().length() > 0) {
    log.info("Setting: " + itproxy)
} else {
    log.info("No proxy found")
}

def writer = new FileWriter(new File(project.build.directory, "it-settings.xml"))
XmlNodePrinter printer = new XmlNodePrinter(new PrintWriter(writer))
printer.setPreserveWhitespace(true);
printer.print(itsettings)

project.getModel().addProperty("ittest-proxy", itproxy)
