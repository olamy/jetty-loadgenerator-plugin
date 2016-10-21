package com.webtide.jetty.load.generator.jenkins;

import hudson.model.Hudson;
import hudson.model.Job;
import hudson.util.RunList;

/**
 *
 */
public class JenkinsUtils
{

    public static RunList<?> getCompleteRunList( Job<?, ?> project)
    {
        try
        {
            return project.getBuilds();
        }
        catch ( NullPointerException e )
        {
            // olamy: really hackhish but Jenkins lazy loading generate that!!
            try
            {
                project.onLoad( Hudson.getActiveInstance(), project.getName() );
                return project.getBuilds();
            }
            catch ( Exception e1 )
            {
                // crappyyyyyyy :-)
            }
        }
        return new RunList<>();
    }

}
