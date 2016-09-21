package com.webtide.jetty.load.generator.plugin;

import hudson.model.AbstractBuild;
import hudson.model.HealthReport;
import hudson.model.HealthReportingAction;

/**
 * Created by olamy on 21/09/2016.
 */
public class JettyLoadGeneratorBuildAction implements HealthReportingAction
{

    //private final AbstractBuild<?, ?> owner;

    private HealthReport health = null;

    @Override
    public HealthReport getBuildHealth()
    {
        if (health != null) {
            return health;
        }
        return null;
    }

    @Override
    public String getIconFileName()
    {
        // TODO Jetty icon
        return null;
    }

    @Override
    public String getDisplayName()
    {
        // TODO i18n
        return "jetty-load-generator";
    }

    @Override
    public String getUrlName()
    {
        return "jetty-loadgenerator";
    }
}
