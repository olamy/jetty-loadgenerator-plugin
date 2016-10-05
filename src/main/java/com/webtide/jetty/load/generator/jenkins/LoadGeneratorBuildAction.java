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
import hudson.model.HealthReport;
import hudson.model.HealthReportingAction;
import org.eclipse.jetty.load.generator.CollectorInformations;
import org.eclipse.jetty.load.generator.report.SummaryReport;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 *
 */
public class LoadGeneratorBuildAction
    implements HealthReportingAction  // , SimpleBuildStep
{

    private static final Logger LOGGER = LoggerFactory.getLogger( LoadGeneratorBuildAction.class );

    private HealthReport health = null;

    private final SummaryReport summaryReport;

    private final CollectorInformations globalCollectorInformations;

    private final Map<String, CollectorInformations> perPath;

    private final Map<String, List<ResponseTimeInfo>> allResponseInfoTimePerPath;

    public LoadGeneratorBuildAction( HealthReport health, SummaryReport summaryReport,
                                     CollectorInformations globalCollectorInformations,
                                     Map<String, CollectorInformations> perPath,
                                     Map<String, List<ResponseTimeInfo>> allResponseInfoTimePerPath )
    {
        this.health = health;
        this.summaryReport = summaryReport;
        this.globalCollectorInformations = globalCollectorInformations;
        this.perPath = perPath;
        this.allResponseInfoTimePerPath = allResponseInfoTimePerPath;
    }

    public SummaryReport getSummaryReport()
    {
        return summaryReport;
    }

    public CollectorInformations getGlobalCollectorInformations()
    {
        return globalCollectorInformations;
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

    /*
    @Override
    public Collection<? extends Action> getProjectActions()
    {
        return null;
    }*/

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
        return perPath;
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
