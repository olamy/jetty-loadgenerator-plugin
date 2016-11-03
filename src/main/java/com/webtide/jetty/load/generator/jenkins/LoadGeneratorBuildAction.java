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
import hudson.model.Action;
import hudson.model.HealthReport;
import hudson.model.HealthReportingAction;
import hudson.model.Run;
import hudson.util.RunList;
import jenkins.model.RunAction2;
import jenkins.tasks.SimpleBuildStep;
import org.eclipse.jetty.load.generator.CollectorInformations;
import org.eclipse.jetty.load.generator.report.SummaryReport;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 *
 */
public class LoadGeneratorBuildAction
    implements HealthReportingAction, SimpleBuildStep.LastBuildAction, RunAction2
{

    private static final Logger LOGGER = LoggerFactory.getLogger( LoadGeneratorBuildAction.class );

    private HealthReport health = null;

    private final SummaryReport summaryReport;

    private final CollectorInformations globalResponseTimeInformations;

    private final CollectorInformations globalLatencyTimeInformations;

    private final Map<String, List<ResponseTimeInfo>> allResponseInfoTimePerPath;

    private String jobName;

    private String buildId;

    private transient RunList<?> builds;

    public LoadGeneratorBuildAction( HealthReport health, SummaryReport summaryReport,
                                     CollectorInformations globalResponseTimeInformations,
                                     CollectorInformations globalLatencyTimeInformations,
                                     Map<String, List<ResponseTimeInfo>> allResponseInfoTimePerPath,
                                     Run<?,?> run )
    {
        this.health = health;
        this.summaryReport = summaryReport;
        this.globalResponseTimeInformations = globalResponseTimeInformations;
        this.globalLatencyTimeInformations = globalLatencyTimeInformations;
        this.jobName = run.getParent().getName();
        this.allResponseInfoTimePerPath = allResponseInfoTimePerPath;
        this.buildId = run.getId();
    }

    public SummaryReport getSummaryReport()
    {
        return summaryReport;
    }

    public CollectorInformations getGlobalResponseTimeInformations()
    {
        return globalResponseTimeInformations;
    }

    public CollectorInformations getGlobalLatencyTimeInformations()
    {
        return globalLatencyTimeInformations;
    }

    public Map<String, List<ResponseTimeInfo>> getAllResponseInfoTimePerPath()
    {
        return allResponseInfoTimePerPath;
    }

    public void doTimeSeries( StaplerRequest req, StaplerResponse rsp )
        throws IOException, ServletException
    {
        LOGGER.debug( "doTimeSeries" );

        ObjectMapper objectMapper = new ObjectMapper();

        objectMapper.writeValue( rsp.getWriter(), allResponseInfoTimePerPath.get( req.getParameter( "path" ) ) );

    }

    @Override
    public Collection<? extends Action> getProjectActions()
    {
        return Arrays.asList( new LoadGeneratorProjectAction( this.builds ));
    }

    @Override
    public void onAttached( Run<?, ?> r )
    {
        onLoad( r );
    }

    @Override
    public void onLoad( Run<?, ?> r )
    {
        this.builds = r.getParent().getBuilds();
    }

    @Override
    public HealthReport getBuildHealth()
    {
        if ( health != null )
        {
            return health;
        }
        return null;
    }

    public Map<String, CollectorInformations> getPerPath()
    {
        return summaryReport.getResponseTimeInformationsPerPath();
    }

    @Override
    public String getIconFileName()
    {
        return PluginConstants.ICON_URL;
    }

    @Override
    public String getDisplayName()
    {
        return PluginConstants.DISPLAY_NAME;
    }

    @Override
    public String getUrlName()
    {
        return PluginConstants.URL_NAME;
    }


}
