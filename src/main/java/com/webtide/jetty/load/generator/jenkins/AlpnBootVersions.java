//
//  ========================================================================
//  Copyright (c) 1995-2016 Webtide LLC, Olivier Lamy
//  ------------------------------------------------------------------------
//  All rights reserved. This program and the accompanying materials
//  are made available under the terms of the Eclipse Public License v1.0
//  and Apache License v2.0 which accompanies this distribution.
//
//      The Eclipse Public License is available at
//      http://www.eclipse.org/legal/epl-v10.html
//
//      The Apache License v2.0 is available at
//      http://www.opensource.org/licenses/apache2.0.php
//
//  You may elect to redistribute this code under either of these licenses.
//  ========================================================================

package com.webtide.jetty.load.generator.jenkins;

import org.apache.commons.lang.StringUtils;
import org.dom4j.Document;
import org.dom4j.Node;
import org.dom4j.XPath;
import org.dom4j.io.SAXReader;
import org.dom4j.tree.DefaultElement;

import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Parse jetty pom to get versions mapping jdk vs alpn boot
 */
public class AlpnBootVersions
{
    private Map<String, String> jdkVersionAlpnBootVersion;

    private AlpnBootVersions()
    {
        Map<String, String> map = new HashMap<>();

        try
        {
            try (InputStream inputStream = Thread.currentThread() //
                .getContextClassLoader().getResourceAsStream( "jetty/jetty-project.pom" ))
            {

                SAXReader reader = new SAXReader();
                Document document = reader.read( inputStream );
                Map<String, String> namespaceMap = new HashMap<>();
                namespaceMap.put( "mvn", "http://maven.apache.org/POM/4.0.0" );
                XPath xpath = document.createXPath( "//mvn:profiles/mvn:profile" );
                xpath.setNamespaceURIs( namespaceMap );

                List<DefaultElement> nodes = xpath.selectNodes( document );
                for ( DefaultElement o : nodes )
                {
                    if ( StringUtils.startsWith( (String) o.element( "id" ).getData(), "8u" ) )
                    {
                        // olamy well a bit fragile way to parse if more than one property...
                        //"//mvn:properties/mvn:alpn.version"
                        // o.selectSingleNode( "//properties/alpn.version" );
                        Node version = o.element( "properties" ).element( "alpn.version" );
                        //"//mvn:activation/mvn:property/mvn:value"
                        //o.selectSingleNode( "//activation/property/value" );
                        Node javaVersion = o.element( "activation" ).element( "property" ).element( "value" );

                        map.put( javaVersion.getStringValue(), version.getStringValue() );
                    }
                }
            }
        }
        catch ( Exception e )
        {
            throw new RuntimeException( e.getMessage(), e );
        }

        jdkVersionAlpnBootVersion = Collections.unmodifiableMap( map );
    }

    private static class LazyHolder
    {
        static final AlpnBootVersions INSTANCE = new AlpnBootVersions();
    }

    public static AlpnBootVersions getInstance()
    {
        return LazyHolder.INSTANCE;
    }

    public Map<String, String> getJdkVersionAlpnBootVersion()
    {
        return jdkVersionAlpnBootVersion;
    }
}
