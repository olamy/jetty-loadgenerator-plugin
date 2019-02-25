//
//  ========================================================================
//  Copyright (c) 1995-2018 Webtide LLC, Olivier Lamy
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

package org.mortbay.jetty.load.generator.jenkins.result;

import com.jayway.jsonpath.JsonPath;
import hudson.model.Actionable;
import hudson.model.ProminentProjectAction;
import hudson.model.Run;
import hudson.util.RunList;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.text.StrSubstitutor;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.eclipse.jetty.client.HttpContentResponse;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.mortbay.jetty.load.generator.jenkins.PluginConstants;
import org.mortbay.jetty.load.generator.jenkins.RunInformations;
import org.mortbay.jetty.load.generator.listeners.LoadResult;
import org.mortbay.jetty.load.generator.store.ElasticResultStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import java.io.IOException;
import java.io.InputStream;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 *
 */
public class LoadResultProjectAction
    extends Actionable
    implements ProminentProjectAction
{

    private static final Logger LOGGER = LoggerFactory.getLogger( LoadResultProjectAction.class );

    private final transient RunList<?> builds;

    private final String elasticHostName;

    private final transient Run<?, ?> lastRun;

    public LoadResultProjectAction( RunList<?> builds, Run<?, ?> lastRun, String elasticHostName )
    {
        this.builds = builds == null ? new RunList<>() : builds;
        this.lastRun = lastRun;
        this.elasticHostName = elasticHostName;
    }

    public void doTrend( StaplerRequest req, StaplerResponse rsp )
        throws IOException, ServletException
    {

    }


    public void doTitles( StaplerRequest req, StaplerResponse rsp )
        throws IOException, ServletException
    {

    }

    @Override
    public String getIconFileName()
    {
        return PluginConstants.ICON_URL;
    }

    @Override
    public String getUrlName()
    {
        return "loadtestresult";
    }


    /**
     * @return all jetty versions available in loadresult index key version value number of results
     */
    public Map<String, String> getJettyVersions()
        throws IOException
    {
        LOGGER.debug( "getJettyVersions" );

        ElasticHost elasticHost = ElasticHost.get( elasticHostName );
        return getJettyVersions( elasticHost );
    }

    public static Map<String, String> getJettyVersions( ElasticHost elasticHost )
        throws IOException
    {
        LOGGER.debug( "getJettyVersions" );

        try (ElasticResultStore elasticResultStore = elasticHost.buildElasticResultStore(); //
             InputStream inputStream = LoadResultProjectAction.class.getResourceAsStream( "/distinctJettyVersion.json" ))
        {
            String distinctSearchQuery = IOUtils.toString( inputStream );

            String distinctResult = elasticResultStore.search( distinctSearchQuery );

            List<Map<String, String>> versionsListMap =
                JsonPath.parse( distinctResult ).read( "$.aggregations.version.buckets" );

            Map<String, String> versions = versionsListMap.stream() //
                // filter out test values
                .filter( stringStringMap -> !( StringUtils.equalsIgnoreCase( stringStringMap.get( "key" ), "9.1" ) //
                    || StringUtils.equalsIgnoreCase( stringStringMap.get( "key" ), "9.2" ) ) ).collect(
                    Collectors.toMap( m -> m.get( "key" ), m -> String.valueOf( m.get( "doc_count" ) ) ) );
            return versions;
        }
    }

    public void doResponseTimeTrend( StaplerRequest req, StaplerResponse rsp )
        throws IOException, ServletException
    {

        LOGGER.debug( "doResponseTimeTrend" );

        String jettyVersion = req.getParameter( "jettyVersion" );
        String[] versions = StringUtils.split(jettyVersion, '|');
        ElasticHost elasticHost = ElasticHost.get( elasticHostName );

        try
        {
            List<RunInformations> runInformations = new ArrayList<>();
            for (String version:versions){
                runInformations.addAll(searchRunInformations( version, elasticHost ));
            }
            Collections.sort( runInformations, Comparator.comparing( o -> o.getStartTimeStamp() ) );
            rsp.addHeader( "Content-Type", "application/json" );
            LoadTestResultPublisher.OBJECT_MAPPER.writeValue( rsp.getWriter(), runInformations );
        }
        catch ( Exception e )
        {
            LOGGER.error( e.getMessage(), e );
        }
    }

    public static List<RunInformations> searchRunInformations( String jettyVersion, ElasticHost elasticHost )
        throws IOException
    {
        return searchRunInformations( jettyVersion, elasticHost, 100 );
    }

    public static List<RunInformations> searchRunInformations( String jettyVersion, ElasticHost elasticHost,
                                                               int maxResult )
        throws IOException
    {

        String originalJettyVersion = jettyVersion;

        // jettyVersion 9.4.9*
        //in case jettyVersion is 9.4.9.v20180320 we need to replace with 9.4.9*
        if ( StringUtils.contains( jettyVersion, 'v' ) )
        {
            jettyVersion = StringUtils.substringBeforeLast( jettyVersion, ".v" );
        }
        // FIXME investigate elastic but query such 9.4.10-SNAPSHOT doesn't work...
        // so using 9.4.10* then filter response back....
        if ( StringUtils.contains( jettyVersion, "-SNAPSHOT" ) )
        {
            jettyVersion = StringUtils.substringBeforeLast( jettyVersion, "-SNAPSHOT" );
        }

        // in case of 9.4.11-NO-LOGGER-SNAPSHOT still not working with elastic
        // here we must have only number or . so remove everything else

        StringBuilder versionQuery = new StringBuilder();
        CharacterIterator ci = new StringCharacterIterator( jettyVersion );
        for ( char c = ci.first(); c != CharacterIterator.DONE; c = ci.next() )
        {
            if ( NumberUtils.isCreatable( Character.toString( c ) ) || c == '.' )
            {
                versionQuery.append( c );
            }
        }

        jettyVersion = versionQuery.toString() + "*";

        try (ElasticResultStore elasticResultStore = elasticHost.buildElasticResultStore(); //
             InputStream inputStream = LoadResultProjectAction.class.getResourceAsStream( "/versionResult.json" ))
        {
            String versionResultQuery = IOUtils.toString( inputStream );
            Map<String, String> map = new HashMap<>( 1 );
            map.put( "jettyVersion", jettyVersion );
            map.put( "maxResult", Integer.toString( maxResult ) );
            versionResultQuery = StrSubstitutor.replace( versionResultQuery, map );

            String results = elasticResultStore.search( versionResultQuery );

            List<LoadResult> loadResults =
                ElasticResultStore.map( new HttpContentResponse( null, results.getBytes(), null, null ) );

            List<RunInformations> runInformations = //
                loadResults.stream() //
                    .filter( loadResult -> StringUtils.equalsIgnoreCase( originalJettyVersion, //
                                                                         loadResult.getServerInfo().getJettyVersion() ) ) //
                    .map( loadResult -> new RunInformations(
                        loadResult.getServerInfo().getJettyVersion() + ":" + loadResult.getServerInfo().getGitHash(), //
                        loadResult.getCollectorInformations(), StringUtils.lowerCase(loadResult.getTransport())) //
                        .jettyVersion( loadResult.getServerInfo().getJettyVersion() ) //
                        .estimatedQps( LoadTestResultBuildAction.estimatedQps(
                            LoadTestResultBuildAction.getLoaderConfig( loadResult ) ) ) //
                        .serverInfo( loadResult.getServerInfo() ) ) //
                    .collect( Collectors.toList() );

            Collections.sort( runInformations, Comparator.comparing( o -> o.getStartTimeStamp() ) );
            return runInformations;
        }

    }


    @Override
    public String getDisplayName()
    {
        return "Http Load Testing Result";
    }

    @Override
    public String getSearchUrl()
    {
        return getUrlName();
    }


}
