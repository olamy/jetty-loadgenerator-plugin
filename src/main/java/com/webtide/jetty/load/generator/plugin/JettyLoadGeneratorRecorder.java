package com.webtide.jetty.load.generator.plugin;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;
import org.kohsuke.stapler.DataBoundConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Created by olamy on 21/09/2016.
 */
public class JettyLoadGeneratorRecorder extends Recorder
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
        LOGGER.info( "perform " );
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
