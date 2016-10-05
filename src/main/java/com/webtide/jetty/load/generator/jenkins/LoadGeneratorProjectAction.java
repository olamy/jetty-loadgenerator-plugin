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
import hudson.model.HealthReport;
import hudson.model.Job;
import hudson.model.ProminentProjectAction;
import hudson.model.Run;
import org.eclipse.jetty.load.generator.CollectorInformations;
import org.eclipse.jetty.load.generator.report.SummaryReport;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class LoadGeneratorProjectAction
    extends Actionable
    implements ProminentProjectAction
{

    private static final Logger LOGGER = LoggerFactory.getLogger( LoadGeneratorProjectAction.class );

    private HealthReport health = null;

    private SummaryReport summaryReport;

    private CollectorInformations globalCollectorInformations;

    private final Job<?, ?> project;

    public LoadGeneratorProjectAction( Job<?, ?> project )
    {
        this.project = project;
        // well that's weird but can happen especially with pipeline...
        if (project != null)
        {
            Run lastBuild = project.getLastBuild();
            if ( lastBuild != null )
            {
                LoadGeneratorBuildAction loadGeneratorBuildAction = lastBuild.getAction( LoadGeneratorBuildAction.class );
                if ( loadGeneratorBuildAction != null )
                {
                    this.health = loadGeneratorBuildAction.getBuildHealth();
                    this.summaryReport = loadGeneratorBuildAction.getSummaryReport();
                    this.globalCollectorInformations = loadGeneratorBuildAction.getGlobalCollectorInformations();
                }
            }
        }
    }

    public String getGlobalData()
        throws IOException
    {

        ObjectMapper objectMapper = new ObjectMapper();

        List<CollectorInformations> datas = new ArrayList<>();

        for ( Run run : project.getBuilds() )
        {
            LoadGeneratorBuildAction buildAction = run.getAction( LoadGeneratorBuildAction.class );
            if ( buildAction != null )
            {
                CollectorInformations collectorInformations = buildAction.getGlobalCollectorInformations();
                if ( collectorInformations != null )
                {
                    datas.add( collectorInformations );
                }
            }
        }

        StringWriter stringWriter = new StringWriter();

        objectMapper.writeValue( stringWriter, datas );

        return stringWriter.toString();

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
