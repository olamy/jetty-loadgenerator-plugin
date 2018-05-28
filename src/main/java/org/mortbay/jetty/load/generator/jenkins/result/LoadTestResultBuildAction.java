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

package org.mortbay.jetty.load.generator.jenkins.result;

import com.fasterxml.jackson.core.type.TypeReference;
import hudson.model.Action;
import hudson.model.HealthReport;
import hudson.model.HealthReportingAction;
import hudson.model.Job;
import hudson.model.Run;
import hudson.util.RunList;
import jenkins.model.RunAction2;
import jenkins.tasks.SimpleBuildStep;
import org.mortbay.jetty.load.generator.listeners.LoadConfig;
import org.mortbay.jetty.load.generator.listeners.LoadResult;
import org.mortbay.jetty.load.generator.store.ElasticResultStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 *
 */
public class LoadTestResultBuildAction
    implements HealthReportingAction, SimpleBuildStep.LastBuildAction, RunAction2
{

    private static final Logger LOGGER = LoggerFactory.getLogger( LoadTestResultBuildAction.class );

    private final HealthReport healthReport;

    private final String buildId;

    private final String jobName;

    private final String elasticHostName;

    private transient RunList<?> builds;

    // we store json because of  https://jenkins.io/redirect/class-filter/
    private String loadResultsJson;

    private transient List<LoadResult> loadResults;

    public LoadTestResultBuildAction( HealthReport healthReport, Run<?, ?> run, String elasticHostName,
                                      String loadResultsJson )
        throws Exception
    {
        this.healthReport = healthReport;
        this.buildId = run.getId();
        this.jobName = run.getParent().getName();
        this.elasticHostName = elasticHostName;
        this.loadResultsJson = loadResultsJson;
        this.loadResults =
            LoadTestResultPublisher.OBJECT_MAPPER.readValue( this.loadResultsJson, new TypeReference<List<LoadResult>>()
            {
            } );
    }

    public String getBuildId()
    {
        return buildId;
    }

    @Override
    public HealthReport getBuildHealth()
    {
        return null;
    }

    @Override
    public Collection<? extends Action> getProjectActions()
    {
        return this.builds != null ? //
            Arrays.asList(
                new LoadResultProjectAction( this.builds, this.builds.getLastBuild(), this.elasticHostName ) ) //
            : Collections.emptyList();
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
        if ( parent != null )
        {
            this.builds = parent.getBuilds();
        }
        if ( this.loadResults == null )
        {
            getLoadResults();
        }
    }

    public List<LoadResult> getLoadResults()
    {
        if ( loadResults == null )
        {
            ElasticHost elasticHost = ElasticHost.get( elasticHostName );
            try (ElasticResultStore elasticResultStore = elasticHost.buildElasticResultStore())
            {
                this.loadResults = elasticResultStore.searchResultsByExternalId( buildId );
                this.loadResultsJson = LoadTestResultPublisher.OBJECT_MAPPER.writeValueAsString( loadResults );
            }
            catch ( Exception e )
            {
                LOGGER.error( e.getMessage(), e );
                this.loadResults = Collections.emptyList();
            }
        }
        return loadResults;
    }

    public static LoadConfig getLoaderConfig( LoadResult loadResult )
    {
        Optional<LoadConfig> optionalLoadConfig = loadResult.getLoadConfigs().stream() //
            .filter( loadConfig -> loadConfig.getType() == LoadConfig.Type.LOADER ) //
            .findFirst();
        return optionalLoadConfig.isPresent() ? optionalLoadConfig.get() : null;
    }

    public List<LoadResult> getLoadResultsOrderByEstimatedQps()
    {
        return loadResults.stream() //
            .sorted(
                Comparator.comparingInt( value -> estimatedQps( getLoaderConfig( (LoadResult) value ) ) ) ).collect(
                Collectors.toList() );
    }

    public static int estimatedQps( LoadConfig loadConfig )
    {
        return loadConfig.getInstanceNumber() //
            * loadConfig.getResourceNumber() //
            * loadConfig.getResourceRate();
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
