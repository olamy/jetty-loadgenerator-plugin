//
//  ========================================================================
//  Copyright (c) 1995-2016 Webtide LLC
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

package com.webtide.jetty.load.generator.plugin;

import hudson.model.HealthReport;
import hudson.model.HealthReportingAction;
import hudson.model.RootAction;
import org.eclipse.jetty.load.generator.CollectorInformations;
import org.eclipse.jetty.load.generator.report.GlobalSummaryReportListener;
import org.eclipse.jetty.load.generator.report.SummaryReport;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Created by olamy on 21/09/2016.
 */
public class LoadGeneratorBuildAction
    implements HealthReportingAction
{

    private static final Logger LOGGER = LoggerFactory.getLogger( LoadGeneratorBuildAction.class );

    private HealthReport health = null;

    private final SummaryReport summaryReport;

    private final CollectorInformations globalCollectorInformations;

    public LoadGeneratorBuildAction( HealthReport health, SummaryReport summaryReport, CollectorInformations globalCollectorInformations )
    {
        this.health = health;
        this.summaryReport = summaryReport;
        this.globalCollectorInformations = globalCollectorInformations;
    }

    public SummaryReport getSummaryReport()
    {
        return summaryReport;
    }

    public CollectorInformations getGlobalCollectorInformations()
    {
        return globalCollectorInformations;
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

    public void doGraph( StaplerRequest req, StaplerResponse rsp) throws IOException
    {
        LOGGER.info( "doGraph" );

        //JFreeChart chart = new CoverageChart( this).createChart();
        //ChartUtil.generateGraph(req, rsp, chart, 500, 200);
    }
}
