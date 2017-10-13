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
import hudson.model.Executor;
import hudson.model.HealthReport;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;
import jenkins.model.Jenkins;
import jenkins.tasks.SimpleBuildStep;
import org.kohsuke.stapler.DataBoundConstructor;
import org.mortbay.jetty.load.generator.listeners.LoadResult;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.InputStream;

/**
 *
 */
public class LoadTestResultPublisher
    extends Recorder
    implements SimpleBuildStep
{

    private final String resultFilePath;

    @DataBoundConstructor
    public LoadTestResultPublisher( String resultFilePath )
    {
        this.resultFilePath = resultFilePath;
    }

    public String getResultFilePath()
    {
        return resultFilePath;
    }

    @Override
    public void perform( @Nonnull Run<?, ?> run, @Nonnull FilePath filePath, @Nonnull Launcher launcher,
                         @Nonnull TaskListener taskListener )
        throws InterruptedException, IOException
    {
        ObjectMapper objectMapper = new ObjectMapper();
        FilePath resultFile = filePath.child( getResultFilePath() );
        if ( !resultFile.exists() )
        {
            taskListener.getLogger().println( "Cannot find load result file" );
            return;
        }
        try (InputStream inputStream = resultFile.read())
        {
            LoadResult loadResult = objectMapper.readValue( inputStream, LoadResult.class );

            // FIXME calculate score from previous build
            HealthReport healthReport = new HealthReport( 30, "text" );

            run.addAction( new LoadTestdResultBuildAction( healthReport, loadResult, run ) );

            boolean master = Executor.currentExecutor().getOwner().getNode() == Jenkins.getInstance();

            ResultStoreManagement resultStoreManagement = //
                Jenkins.getInstance().getExtensionList( ResultStoreManagement.class ).get( 0 );

            ResultStore resultStore = resultStoreManagement.getResultStore();
            resultStore.save( loadResult );

        }
    }

    @Override
    public BuildStepMonitor getRequiredMonitorService()
    {
        return BuildStepMonitor.BUILD;
    }

    @Extension
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
            return "Load Test Result parser";
        }

    }
}
