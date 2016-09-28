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

import com.fasterxml.jackson.databind.ObjectMapper;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;
import org.eclipse.jetty.load.generator.report.SummaryReport;
import org.kohsuke.stapler.DataBoundConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

/**
 * Created by olamy on 21/09/2016.
 */
public class JettyLoadGeneratorRecorder extends Recorder // implements HealthReportingAction
{
    private static final Logger LOGGER = LoggerFactory.getLogger( JettyLoadGeneratorRecorder.class );

    @DataBoundConstructor
    public JettyLoadGeneratorRecorder()
    {
    }

    @Override
    public boolean perform( AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener )
        throws InterruptedException, IOException
    {
        File reportDirectory = new File(build.getRootDir(), JettyLoadGeneratorBuilder.REPORT_DIRECTORY_NAME);
        File summaryReportFile = new File( reportDirectory, JettyLoadGeneratorBuilder.SUMMARY_REPORT_FILE );

        ObjectMapper objectMapper = new ObjectMapper(  );

        SummaryReport summaryReport = objectMapper.readValue( summaryReportFile, SummaryReport.class );

        return true;
    }





    @Override
    public BuildStepMonitor getRequiredMonitorService()
    {
        return BuildStepMonitor.NONE;
    }


    /**
     * Descriptor should be singleton.
     */
    @Extension
    public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();


    public static final class DescriptorImpl extends BuildStepDescriptor<Publisher>
    {

        /**
         * Constructs a new DescriptorImpl.
         */
        DescriptorImpl()
        {
            super( JettyLoadGeneratorRecorder.class );
        }

        @Override
        public boolean isApplicable( Class<? extends AbstractProject> aClass )
        {
            return true;
        }
    }

}
