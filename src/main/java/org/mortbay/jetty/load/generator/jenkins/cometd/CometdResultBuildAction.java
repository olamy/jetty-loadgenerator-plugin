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

package org.mortbay.jetty.load.generator.jenkins.cometd;

import hudson.model.Action;
import hudson.model.HealthReport;
import hudson.model.HealthReportingAction;
import hudson.model.Job;
import hudson.model.Run;
import hudson.util.RunList;
import jenkins.model.RunAction2;
import jenkins.tasks.SimpleBuildStep;
import org.mortbay.jetty.load.generator.jenkins.cometd.beans.LoadResults;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

/**
 *
 */
public class CometdResultBuildAction
    implements HealthReportingAction, SimpleBuildStep.LastBuildAction, RunAction2
{

    private final LoadResults loadResults;

    private final HealthReport healthReport;

    private final String buildId;

    private final String jobName;

    private transient RunList<?> builds;

    public CometdResultBuildAction( HealthReport healthReport, LoadResults loadResults, Run<?, ?> run )
    {
        this.loadResults = loadResults;
        this.healthReport = healthReport;
        this.buildId = run.getId();
        this.jobName = run.getParent().getName();
    }

    @Override
    public HealthReport getBuildHealth()
    {
        return null;
    }

    public LoadResults getLoadResults()
    {
        return loadResults;
    }

    @Override
    public Collection<? extends Action> getProjectActions()
    {
        return this.builds != null ? //
            Arrays.asList( new CometdProjectAction( this.builds, this.builds.getLastBuild() ) ) : Collections.emptyList();
    }

    @Override
    public void onAttached( Run<?, ?> r )
    {
        onLoad( r );
    }

    @Override
    public void onLoad( Run<?, ?> r )
    {
        Job parent = r.getParent();
        if (parent != null)
        {
            this.builds = parent.getBuilds();
        }
    }

    @Override
    public String getIconFileName()
    {
        return null;
    }

    @Override
    public String getDisplayName()
    {
        return null;
    }

    @Override
    public String getUrlName()
    {
        return null;
    }
}
