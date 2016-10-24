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

package com.webtide.jetty.load.generator.jenkins.cometd;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.webtide.jetty.load.generator.jenkins.JenkinsUtils;
import com.webtide.jetty.load.generator.jenkins.PluginConstants;
import com.webtide.jetty.load.generator.jenkins.cometd.beans.LoadResults;
import hudson.model.Actionable;
import hudson.model.Job;
import hudson.model.ProminentProjectAction;
import hudson.model.Run;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 */
public class CometdProjectAction
    extends Actionable
    implements ProminentProjectAction
{


    private static final Logger LOGGER = LoggerFactory.getLogger( CometdProjectAction.class );

    private final Job<?, ?> project;

    public CometdProjectAction( Job<?, ?> project )
    {
        this.project = project;
    }

    public void doTrend( StaplerRequest req, StaplerResponse rsp )
        throws IOException, ServletException
    {
        ObjectMapper objectMapper = new ObjectMapper();

        List<BuildLoadResults> datas = new ArrayList<>();

        for ( Run run : JenkinsUtils.getCompleteRunList( project ) )
        {
            CometdResultBuildAction buildAction = run.getAction( CometdResultBuildAction.class );
            if ( buildAction != null )
            {
                if ( buildAction.getLoadResults() != null )
                {
                    BuildLoadResults buildLoadResults =
                        new BuildLoadResults( run.getId(), buildAction.getLoadResults() );
                    datas.add( buildLoadResults );
                }
            }
        }

        // order by buildId

        Collections.sort( datas, ( o1, o2 ) -> Long.valueOf( o1.buildId ).compareTo( Long.valueOf( o2.buildId ) ) );

        //StringWriter stringWriter = new StringWriter();

        rsp.setHeader( "Content-Type", "application/json" );

        objectMapper.writeValue( rsp.getOutputStream(), datas );

        //rsp.getWriter().write( data );

    }



    public void doTitles( StaplerRequest req, StaplerResponse rsp )
        throws IOException, ServletException
    {
        Map<String, String> titles = new HashMap<>();

        Run run = JenkinsUtils.getLastRun( project );
        if ( run != null )
        {
            CometdResultBuildAction buildAction = run.getAction( CometdResultBuildAction.class );
            if ( buildAction != null && buildAction.getLoadResults() != null )
            {
                LoadResults loadResults = buildAction.getLoadResults();

                titles.put( "latency", "Latency Trend " + loadResults.getResults().getLatency().getMax().getUnit() );
                titles.put( "cpu", "Cpu Trend " + loadResults.getResults().getCpu().getUnit() + ", " //
                    + " " + loadResults.getConfig().getCores() + " cores " );
                titles.put( "garbage", "Garbage Trend " + loadResults.getResults().getGc().getOldGarbage().getUnit() );
            }
        }
        else
        {
            titles.put( "latency", "No Data" );
            titles.put( "cpu", "No Data" );
            titles.put( "garbage", "No Data" );
        }

        rsp.addHeader( "Content-Type", "application/json; charset=utf-8" );

        new ObjectMapper().writeValue( rsp.getOutputStream(), titles );

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

    public static class BuildLoadResults
        extends LoadResults
    {
        private String buildId;

        private LoadResults loadResults;

        public BuildLoadResults( String buildId, LoadResults loadResults )
        {
            this.buildId = buildId;
            this.loadResults = loadResults;
        }

        public String getBuildId()
        {
            return buildId;
        }

        public void setBuildId( String buildId )
        {
            this.buildId = buildId;
        }

        public LoadResults getLoadResults()
        {
            return loadResults;
        }

        public void setLoadResults( LoadResults loadResults )
        {
            this.loadResults = loadResults;
        }
    }
}
