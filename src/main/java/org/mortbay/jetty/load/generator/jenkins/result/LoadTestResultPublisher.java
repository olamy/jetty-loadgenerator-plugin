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

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractProject;
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
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 *
 */
public class LoadTestResultPublisher
    extends Recorder
    implements SimpleBuildStep
{

    private static final Logger LOGGER = LoggerFactory.getLogger( LoadTestResultPublisher.class );

    private final String resultFilePath;

    private String elasticHostName;

    private String idPrefix;

    private transient ResultStore resultStore;

    public static final ObjectMapper OBJECT_MAPPER =
        new ObjectMapper().configure( DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES, false );

    @DataBoundConstructor
    public LoadTestResultPublisher( String resultFilePath, String elasticHostName, String idPrefix )
    {
        this.resultFilePath = resultFilePath;
        this.elasticHostName = elasticHostName;
        this.idPrefix = idPrefix;
    }

    public ResultStore getResultStore( ElasticHost elasticHost )
    {
        if ( this.resultStore != null )
        {
            return this.resultStore;
        }
        initializeResultStore( elasticHost );
        return this.resultStore;

    }

    private void initializeResultStore( ElasticHost elasticHost )
    {
        if ( elasticHost != null && StringUtils.isNotEmpty( elasticHost.getElasticHost() ) )
        {
            this.resultStore = elasticHost.buildElasticResultStore();
        }
        else
        {
            this.resultStore = Empty.INSTANCE;
            LOGGER.info( "No elastic host defined so result will not be stored " );
        }
    }

    public String getElasticHostName()
    {
        return elasticHostName;
    }

    public void setElasticHostName( String elasticHostName )
    {
        this.elasticHostName = elasticHostName;
    }

    public String getResultFilePath()
    {
        return resultFilePath;
    }

    public String getIdPrefix()
    {
        return idPrefix;
    }

    public void setIdPrefix( String idPrefix )
    {
        this.idPrefix = idPrefix;
    }

    @Override
    public void perform( @Nonnull Run<?, ?> run, @Nonnull FilePath filePath, @Nonnull Launcher launcher,
                         @Nonnull TaskListener taskListener )
        throws InterruptedException, IOException
    {
//        ElasticHost elasticHost = ElasticHost.get( elasticHostName );
//
//        FilePath resultFile = filePath.child( getResultFilePath() );
//        if ( !resultFile.exists() )
//        {
//            taskListener.getLogger().println( "Cannot find load result file" );
//            return;
//        }
//        try (InputStream inputStream = resultFile.read())
//        {
//            LoadResult loadResult = new ObjectMapper() //
//                .configure( DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false ) //
//                .readValue( inputStream, LoadResult.class );
//
//            // FIXME calculate score from previous build
//            HealthReport healthReport = new HealthReport( 30, "text" );
//
//            String uuid = StringUtils.isEmpty( idPrefix ) ? "jenkins-" + run.getParent().getName() : idPrefix;
//            uuid += "-" + run.getId();
//
//            loadResult.uuid( uuid ).uuidPrefix( idPrefix );
//
//            this.getResultStore( elasticHost ).save( loadResult );
//
//            taskListener.getLogger().println( "Load result stored with id: " + loadResult.getUuid() );
//
//            run.addAction(
//                new LoadTestdResultBuildAction( healthReport, loadResult.getUuid(), run, elasticHostName ) );
//        }

        // FIXME use a json file to calculate some HealthReport
        LOGGER.info( "publish result" );
        if ( StringUtils.isEmpty( elasticHostName ) )
        {
            // we use the first one
            Optional<ElasticHost> elasticHost =
                ElasticHostProjectProperty.DESCRIPTOR.getElasticHosts().stream().findFirst();
            elasticHostName = elasticHost.isPresent() ? elasticHost.get().getElasticHostName() : "";
        }

        ElasticHost elasticHost = ElasticHostProjectProperty.DESCRIPTOR.getElasticHostByName( elasticHostName );
        ElasticResultStore elasticResultStore = elasticHost.buildElasticResultStore();
        List<LoadResult> loadResults = elasticResultStore.searchResultsByExternalId( run.getId());//"547" );// run.getId() );

        try
        {
            run.addAction( new LoadTestResultBuildAction( null, run, elasticHostName,
                                                           OBJECT_MAPPER.writeValueAsString( loadResults ) ) );
            LoadResult loadResult = loadResults.get( 0 );
            run.setDescription( "Jetty Version " + loadResult.getServerInfo().getJettyVersion()
                                    + ", transport " + loadResult.getTransport()
                                    + ", qps " + LoadTestResultBuildAction.estimatedQps(
                    LoadTestResultBuildAction.getLoaderConfig( loadResult ) ));
        }
        catch ( Exception e )
        {
            throw new IOException( e.getMessage(), e );
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

        public List<ElasticHost> getElasticHosts()
        {
            return ElasticHostProjectProperty.DESCRIPTOR.getElasticHosts();
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
        public void save( LoadResult loadResult )
        {
            LOGGER.info( "No elastic host defined so results are not stored " );
        }

        @Override
        public void remove( LoadResult loadResult )
        {
            // no op
        }

        @Override
        public List<LoadResult> get( List<String> loadResultId )
        {
            return null;
        }

        @Override
        public List<LoadResult> find( QueryFilter queryFilter )
        {
            return null;
        }

        @Override
        public List<LoadResult> findAll()
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
        public void close()
            throws IOException
        {
            //no op
        }

        @Override
        public LoadResult get( String loadResultId )
        {
            return null;
        }
    }
}
