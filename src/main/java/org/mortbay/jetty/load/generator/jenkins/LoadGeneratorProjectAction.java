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

package org.mortbay.jetty.load.generator.jenkins;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import hudson.model.Actionable;
import hudson.model.ProminentProjectAction;
import hudson.model.Run;
import hudson.util.RunList;
import org.apache.commons.lang.ObjectUtils;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.mortbay.jetty.load.generator.listeners.CollectorInformations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.LongAdder;

/**
 *
 */
public class LoadGeneratorProjectAction
    extends Actionable
    implements ProminentProjectAction
{

    private static final Logger LOGGER = LoggerFactory.getLogger( LoadGeneratorProjectAction.class );

    private final transient RunList<?> builds;

    public LoadGeneratorProjectAction( RunList<?> builds )
    {
        this.builds = builds == null ? new RunList<>() : builds;
    }


    public String getAllResponseTimeInformations()
        throws IOException
    {

        ObjectMapper objectMapper = new ObjectMapper();

        List<RunInformations> datas = new ArrayList<>();

        for ( Run run : this.builds )
        {
            LoadGeneratorBuildAction buildAction = run.getAction( LoadGeneratorBuildAction.class );
            if ( buildAction != null )
            {
                if ( buildAction.getGlobalResponseTimeInformations() != null )
                {
                    RunInformations runInformations =
                        new RunInformations( run.getId(), buildAction.getGlobalResponseTimeInformations() );
                    datas.add( runInformations );
                }
            }
        }

        // order by buildId

        Collections.sort( datas, Comparator.comparing( RunInformations::getBuildId ) );

        StringWriter stringWriter = new StringWriter();

        objectMapper.writeValue( stringWriter, datas );

        return stringWriter.toString();

    }


    public String getAllLatencyInformations()
        throws IOException
    {

        ObjectMapper objectMapper = new ObjectMapper();

        List<RunInformations> datas = new ArrayList<>();

        for ( Run run : this.builds )
        {
            LoadGeneratorBuildAction buildAction = run.getAction( LoadGeneratorBuildAction.class );
            if ( buildAction != null )
            {
                if ( buildAction.getGlobalLatencyTimeInformations() != null )
                {
                    RunInformations runInformations =
                        new RunInformations( run.getId(), buildAction.getGlobalLatencyTimeInformations() );
                    datas.add( runInformations );
                }
            }
        }

        // order by buildId

        Collections.sort( datas, Comparator.comparing( RunInformations::getBuildId ) );

        StringWriter stringWriter = new StringWriter();

        objectMapper.writeValue( stringWriter, datas );

        return stringWriter.toString();
    }


    public void doResponseTimeTrend( StaplerRequest req, StaplerResponse rsp )
        throws IOException, ServletException
    {
        LOGGER.debug( "doResponseTimeTrend" );
        String data = getAllResponseTimeInformations();
        rsp.getWriter().write( data );
    }

    public void doLatencyTrend( StaplerRequest req, StaplerResponse rsp )
        throws IOException, ServletException
    {
        LOGGER.debug( "doLatencyTrend" );
        String data = getAllLatencyInformations();
        rsp.getWriter().write( data );
    }

    public void doPerFamilyStatusNumber( StaplerRequest req, StaplerResponse rsp )
        throws IOException, ServletException
    {
        LOGGER.debug( "doPerFamilyStatusNumber" );

        List<StatusResult> statusResults = new ArrayList<>();

        for ( Run run : this.builds )
        {
            LoadGeneratorBuildAction buildAction = run.getAction( LoadGeneratorBuildAction.class );
            if ( buildAction != null )
            {
                Map<Integer, LongAdder> re = buildAction.getResponseNumberPerStatusFamily();
                if ( re != null )
                {
                    StatusResult statusResult = new StatusResult();
                    statusResult.buildId = run.getId();
                    if ( re.get( 1 ) != null )
                    {
                        statusResult._1xx = re.get( 1 ).longValue();
                    }
                    if ( re.get( 2 ) != null )
                    {
                        statusResult._2xx = re.get( 2 ).longValue();
                    }
                    if ( re.get( 3 ) != null )
                    {
                        statusResult._3xx = re.get( 3 ).longValue();
                    }
                    if ( re.get( 4 ) != null )
                    {
                        statusResult._4xx = re.get( 4 ).longValue();
                    }
                    if ( re.get( 5 ) != null )
                    {
                        statusResult._5xx = re.get( 5 ).longValue();
                    }
                    statusResults.add( statusResult );
                }
            }
        }

        Collections.sort( statusResults, Comparator.comparing( StatusResult::getBuildId ) );

        StringWriter stringWriter = new StringWriter();

        new ObjectMapper() //
            .disable( SerializationFeature.FAIL_ON_EMPTY_BEANS ) //
            .writeValue( stringWriter, statusResults );

        rsp.getWriter().write( stringWriter.toString() );
    }

    public void doGcUsage( StaplerRequest req, StaplerResponse rsp )
        throws IOException, ServletException
    {

        List<GcUsage> gcUsages = new ArrayList<>();

        for ( Run run : this.builds )
        {
            LoadGeneratorBuildAction buildAction = run.getAction( LoadGeneratorBuildAction.class );
            if ( buildAction != null )
            {
                Map<String, Object> monitoringResultMap = buildAction.getMonitoringResultMap();
                if ( monitoringResultMap == null )
                {
                    continue;
                }
                GcUsage gcUsage = new GcUsage();
                Map<String, Object> resultsMap = (Map) monitoringResultMap.get( "results" );

                if ( resultsMap == null )
                {
                    continue;
                }

                Map<String, Object> gcResult = (Map) resultsMap.get( "gc" );

                gcUsage.youngCount = ObjectUtils.toString( gcResult.get( "youngCount" ) );

                Map<String, Object> youngTime = (Map) gcResult.get( "youngTime" );
                if ( youngTime != null )
                {
                    gcUsage.youngTime = ObjectUtils.toString( youngTime.get( "value" ) );
                }
                gcUsage.oldCount = ObjectUtils.toString( gcResult.get( "oldCount" ) );
                Map<String, Object> oldTime = (Map) gcResult.get( "oldTime" );
                if ( oldTime != null )
                {
                    gcUsage.oldTime = ObjectUtils.toString( oldTime.get( "value" ) );
                }

                Map<String, Object> youngGarbage = (Map) gcResult.get( "youngGarbage" );
                if ( youngGarbage != null )
                {
                    gcUsage.youngGarbage = ObjectUtils.toString( youngGarbage.get( "value" ) );
                }

                Map<String, Object> oldGarbage = (Map) gcResult.get( "oldGarbage" );
                if ( oldGarbage != null )
                {
                    gcUsage.oldGarbage = ObjectUtils.toString( oldGarbage.get( "value" ) );
                }

                gcUsages.add( gcUsage );
            }
        }

        Collections.sort( gcUsages, Comparator.comparing( GcUsage::getBuildId ) );

        StringWriter stringWriter = new StringWriter();

        new ObjectMapper().writeValue( stringWriter, gcUsages );

        rsp.getWriter().write( stringWriter.toString() );
    }


    @Override
    public String getIconFileName()
    {
        return PluginConstants.ICON_URL;
    }

    @Override
    public String getUrlName()
    {
        return PluginConstants.URL_NAME;
    }

    @Override
    public String getDisplayName()
    {
        return PluginConstants.DISPLAY_NAME;
    }

    @Override
    public String getSearchUrl()
    {
        return getUrlName();
    }

}
