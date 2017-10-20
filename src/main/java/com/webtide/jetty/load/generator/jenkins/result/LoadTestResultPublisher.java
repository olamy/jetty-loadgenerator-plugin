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
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractProject;
import hudson.model.HealthReport;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;
import jenkins.tasks.SimpleBuildStep;
import org.apache.commons.lang3.StringUtils;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.mortbay.jetty.load.generator.listeners.LoadResult;
import org.mortbay.jetty.load.generator.store.ElasticResultStore;
import org.mortbay.jetty.load.generator.store.ResultStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 */
public class LoadTestResultPublisher
    extends Recorder
    implements SimpleBuildStep
{

    private static final Logger LOGGER = LoggerFactory.getLogger( LoadTestResultPublisher.class );

    private final String resultFilePath;

    private String elasticHost, elasticScheme = "http", elasticUsername, elasticPassword;

    private int elasticPort;

    private transient ResultStore resultStore;

    @DataBoundConstructor
    public LoadTestResultPublisher( String resultFilePath, String elasticHost, String elasticScheme,
                                    String elasticUsername, String elasticPassword, int elasticPort )
    {
        this.resultFilePath = resultFilePath;
        this.elasticHost = elasticHost;
        this.elasticScheme = elasticScheme;
        this.elasticUsername = elasticUsername;
        this.elasticPassword = elasticPassword;
        this.elasticPort = elasticPort;

        initializeResultStore();

    }

    public ResultStore getResultStore()
    {
        if ( this.resultStore != null )
        {
            return this.resultStore;
        }
        initializeResultStore();
        return this.resultStore;

    }

    private void initializeResultStore()
    {
        if ( StringUtils.isNotEmpty( this.elasticHost ) )
        {
            this.resultStore = new ElasticResultStore();
            Map<String, String> setupData = new HashMap<>();
            setupData.put( ElasticResultStore.HOST_KEY, this.elasticHost );
            setupData.put( ElasticResultStore.PORT_KEY, Integer.toString( this.elasticPort ) );
            setupData.put( ElasticResultStore.SCHEME_KEY, this.elasticScheme );
            setupData.put( ElasticResultStore.USER_KEY, this.elasticUsername );
            setupData.put( ElasticResultStore.PWD_KEY, this.elasticPassword );
            this.resultStore.initialize( setupData );
        }
        else
        {
            this.resultStore = Empty.INSTANCE;
            LOGGER.info( "No elastic host defined so result will not be stored " );
        }
    }

    public String getResultFilePath()
    {
        return resultFilePath;
    }

    public String getElasticHost()
    {
        return elasticHost;
    }

    public void setElasticHost( String elasticHost )
    {
        this.elasticHost = elasticHost;
    }

    public String getElasticScheme()
    {
        return elasticScheme;
    }

    public void setElasticScheme( String elasticScheme )
    {
        this.elasticScheme = elasticScheme;
    }

    public String getElasticUsername()
    {
        return elasticUsername;
    }

    public void setElasticUsername( String elasticUsername )
    {
        this.elasticUsername = elasticUsername;
    }

    public String getElasticPassword()
    {
        return elasticPassword;
    }

    public void setElasticPassword( String elasticPassword )
    {
        this.elasticPassword = elasticPassword;
    }

    public int getElasticPort()
    {
        return elasticPort;
    }

    public void setElasticPort( int elasticPort )
    {
        this.elasticPort = elasticPort;
    }

    @Override
    public void perform( @Nonnull Run<?, ?> run, @Nonnull FilePath filePath, @Nonnull Launcher launcher,
                         @Nonnull TaskListener taskListener )
        throws InterruptedException, IOException
    {
        FilePath resultFile = filePath.child( getResultFilePath() );
        if ( !resultFile.exists() )
        {
            taskListener.getLogger().println( "Cannot find load result file" );
            return;
        }
        try (InputStream inputStream = resultFile.read())
        {
            LoadResult loadResult = new ObjectMapper().readValue( inputStream, LoadResult.class );

            // FIXME calculate score from previous build
            HealthReport healthReport = new HealthReport( 30, "text" );

            ResultStore.ExtendedLoadResult extendedLoadResult = this.getResultStore().save( loadResult );

            run.addAction( new LoadTestdResultBuildAction( healthReport, extendedLoadResult.getUuid(), run ) );
        }
    }

    @Override
    public BuildStepMonitor getRequiredMonitorService()
    {
        return BuildStepMonitor.BUILD;
    }

    @Override
    public DescriptorImpl getDescriptor()
    {
        return (DescriptorImpl) super.getDescriptor();
    }

    @Extension
    @Symbol( "loadtestresult" )
    public static class DescriptorImpl
        extends BuildStepDescriptor<Publisher>
    {

        @Override
        public boolean isApplicable( Class<? extends AbstractProject> aClass )
        {
            return true;
        }

        @Override
        public String getDisplayName()
        {
            return "Load Test Result Store";
        }

        public List<String> getSchemes()
        {
            return Arrays.asList( "http", "https" );
        }

    }


    private static class Empty
        implements ResultStore
    {
        static final Empty INSTANCE = new Empty();

        @Override
        public void initialize( Map<String, String> setupData )
        {

        }

        @Override
        public ExtendedLoadResult save( LoadResult loadResult )
        {
            LOGGER.info( "No elastic host defined so results are not stored " );
            return null;
        }

        @Override
        public void remove( ExtendedLoadResult loadResult )
        {

        }

        @Override
        public List<ExtendedLoadResult> find( QueryFiler queryFiler )
        {
            return null;
        }

        @Override
        public List<ExtendedLoadResult> findAll()
        {
            return null;
        }

        @Override
        public String getProviderId()
        {
            return null;
        }

        @Override
        public boolean isActive( Map<String, String> setupData )
        {
            return false;
        }

        @Override
        public ExtendedLoadResult get( String loadResultId )
        {
            return null;
        }
    }
}
