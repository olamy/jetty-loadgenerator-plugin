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

import com.fasterxml.jackson.databind.ObjectMapper;
import hudson.model.Actionable;
import hudson.model.ProminentProjectAction;
import hudson.model.Run;
import hudson.util.RunList;
import org.eclipse.jetty.load.generator.CollectorInformations;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

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

        Collections.sort( datas, Comparator.comparing( RunInformations::getBuildId));

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

        Collections.sort( datas, ( o1, o2 ) -> Long.valueOf( o1.buildId ).compareTo( Long.valueOf( o2.buildId ) ) );

        StringWriter stringWriter = new StringWriter();

        objectMapper.writeValue( stringWriter, datas );

        return stringWriter.toString();

    }


    public static class RunInformations
        extends CollectorInformations
    {
        private String buildId;

        public RunInformations( String buildId, CollectorInformations collectorInformations )
        {
            this.buildId = buildId;
            setInformationType( collectorInformations.getInformationType() );
            totalCount( collectorInformations.getTotalCount() );
            minValue( collectorInformations.getMinValue() );
            maxValue( collectorInformations.getMaxValue() );
            value50( collectorInformations.getValue50() );
            value90( collectorInformations.getValue90() );
            mean( collectorInformations.getMean() );
            stdDeviation( collectorInformations.getStdDeviation() );
            startTimeStamp( collectorInformations.getStartTimeStamp() );
            endTimeStamp( collectorInformations.getEndTimeStamp() );
        }

        public String getBuildId()
        {
            return buildId;
        }

        public void setBuildId( String buildId )
        {
            this.buildId = buildId;
        }
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
