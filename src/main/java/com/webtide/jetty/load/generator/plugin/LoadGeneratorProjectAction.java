package com.webtide.jetty.load.generator.plugin;

import hudson.model.AbstractProject;
import hudson.model.Actionable;
import hudson.model.ProminentProjectAction;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Created by olamy on 29/9/16.
 */
public class LoadGeneratorProjectAction
    extends Actionable
    implements ProminentProjectAction
{

    private static final Logger LOGGER = LoggerFactory.getLogger( LoadGeneratorProjectAction.class );

    private final AbstractProject<?, ?> project;

    public LoadGeneratorProjectAction( AbstractProject<?, ?> project )
    {
        this.project = project;
    }

    public void doGraph( StaplerRequest req, StaplerResponse rsp) throws IOException
    {
        LOGGER.debug( "doGraph" );
    }

    public void doIndex(StaplerRequest req, StaplerResponse rsp) throws IOException {
        LOGGER.debug( "doIndex" );
    }


    @Override
    public String getIconFileName()
    {
        return "jetty.gif";
    }

    @Override
    public String getUrlName()
    {
        return "loadgenerator";
    }

    @Override
    public String getDisplayName()
    {
        return "loadgenerator by Jetty";
    }

    @Override
    public String getSearchUrl()
    {
        return getUrlName();
    }
}
