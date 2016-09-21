package com.webtide.jetty.load.generator.plugin;

import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Recorder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Created by olamy on 21/09/2016.
 */
public class JettyLoadGeneratorRecorder extends Recorder
{
    private static final Logger LOGGER = LoggerFactory.getLogger( JettyLoadGeneratorRecorder.class );

    @Override
    public boolean perform( AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener )
        throws InterruptedException, IOException
    {
        LOGGER.info( "perform " );
        return super.perform( build, launcher, listener );
    }

    @Override
    public BuildStepMonitor getRequiredMonitorService()
    {
        return BuildStepMonitor.NONE;
    }
}
