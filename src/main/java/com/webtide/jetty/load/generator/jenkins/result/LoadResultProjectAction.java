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

package com.webtide.jetty.load.generator.jenkins.result;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.webtide.jetty.load.generator.jenkins.PluginConstants;
import com.webtide.jetty.load.generator.jenkins.cometd.CometdResultBuildAction;
import com.webtide.jetty.load.generator.jenkins.cometd.beans.LoadResults;
import hudson.model.Actionable;
import hudson.model.ProminentProjectAction;
import hudson.model.Run;
import hudson.util.RunList;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.mortbay.jetty.load.generator.listeners.LoadResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 */
public class LoadResultProjectAction
    extends Actionable
    implements ProminentProjectAction
{


    private static final Logger LOGGER = LoggerFactory.getLogger( LoadResultProjectAction.class );

    private final transient RunList<?> builds;

    private final transient Run<?, ?> lastRun;

    public LoadResultProjectAction( RunList<?> builds, Run<?, ?> lastRun )
    {
        this.builds = builds == null ? new RunList<>() : builds;
        this.lastRun = lastRun;
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

    public static class BuildLoadResult
        extends LoadResult
    {
        private String buildId;

        private LoadResults loadResults;

        public BuildLoadResult( String buildId, LoadResult loadResult )
        {
            super(loadResult.getServerInfo(), loadResult.getCollectorInformations());
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
