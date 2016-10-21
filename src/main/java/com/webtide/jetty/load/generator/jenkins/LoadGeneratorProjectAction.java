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
import hudson.model.Hudson;
import hudson.model.Job;
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
import java.util.List;

/**
 *
 */
public class LoadGeneratorProjectAction
    extends Actionable
    implements ProminentProjectAction
{

    private static final Logger LOGGER = LoggerFactory.getLogger( LoadGeneratorProjectAction.class );

    private final Job<?, ?> project;

    public LoadGeneratorProjectAction( Job<?, ?> project )
    {
        this.project = project;
    }


    public String getGlobalData()
        throws IOException
    {

        ObjectMapper objectMapper = new ObjectMapper();

        List<RunInformations> datas = new ArrayList<>();

        for ( Run run : getCompleteRunList() )
        {
            LoadGeneratorBuildAction buildAction = run.getAction( LoadGeneratorBuildAction.class );
            if ( buildAction != null )
            {
                if ( buildAction.getGlobalCollectorInformations() != null )
                {
                    RunInformations runInformations =
                        new RunInformations( run.getId(), buildAction.getGlobalCollectorInformations() );
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

    protected RunList<?> getCompleteRunList()
    {
        try
        {
            return project.getBuilds();
        }
        catch ( NullPointerException e )
        {
            // olamy: really hackhish but Jenkins lazy loading generate that!!
            try
            {
                project.onLoad( Hudson.getActiveInstance(), project.getName() );
                return project.getBuilds();
            }
            catch ( Exception e1 )
            {
                // crappyyyyyyy :-)
            }
        }
        return new RunList<>();
    }

    public void doTrend( StaplerRequest req, StaplerResponse rsp )
        throws IOException, ServletException
    {
        LOGGER.debug( "doTrend" );
        String data = getGlobalData();
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
