package com.webtide.jetty.load.generator.jenkins;

import com.fasterxml.jackson.databind.ObjectMapper;
import hudson.model.AbstractProject;
import hudson.model.Actionable;
import hudson.model.HealthReport;
import hudson.model.ProminentProjectAction;
import hudson.model.Run;
import org.eclipse.jetty.load.generator.CollectorInformations;
import org.eclipse.jetty.load.generator.report.SummaryReport;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by olamy on 29/9/16.
 */
public class LoadGeneratorProjectAction
    extends Actionable
    implements ProminentProjectAction
{

    private static final Logger LOGGER = LoggerFactory.getLogger( LoadGeneratorProjectAction.class );

    private HealthReport health = null;

    private SummaryReport summaryReport;

    private CollectorInformations globalCollectorInformations;

    private final AbstractProject<?, ?> project;

    public LoadGeneratorProjectAction( AbstractProject<?, ?> project )
    {
        this.project = project;
        LoadGeneratorBuildAction loadGeneratorBuildAction = project.getLastBuild().getAction( LoadGeneratorBuildAction.class );
        if ( loadGeneratorBuildAction != null )
        {
            this.health = loadGeneratorBuildAction.getBuildHealth();
            this.summaryReport = loadGeneratorBuildAction.getSummaryReport();
            this.globalCollectorInformations = loadGeneratorBuildAction.getGlobalCollectorInformations();
        }
    }

    public String getGlobalData()
        throws IOException
    {

        ObjectMapper objectMapper = new ObjectMapper();

        List<CollectorInformations> datas = new ArrayList<>(  );

        for ( Run run : project.getBuilds() )
        {
            LoadGeneratorBuildAction buildAction = run.getAction( LoadGeneratorBuildAction.class );
            if ( buildAction != null )
            {
                CollectorInformations collectorInformations = buildAction.getGlobalCollectorInformations();
                if (collectorInformations != null)
                {
                    datas.add( collectorInformations );
                }
            }
        }

        StringWriter stringWriter = new StringWriter();

        objectMapper.writeValue( stringWriter, datas );

        return stringWriter.toString();

    }

    public void doTrend( StaplerRequest req, StaplerResponse rsp ) throws IOException, ServletException
    {
        LOGGER.debug( "doTrend" );
        String data = getGlobalData();
        rsp.getWriter().write( data );
    }

    @Override
    public String getIconFileName()
    {
        return PluginConstants.ICON_URL;
    }

    @Override
    public String getUrlName()
    {
        return PluginConstants.URL_NAME;
    }

    @Override
    public String getDisplayName()
    {
        return PluginConstants.DISPLAY_NAME;
    }

    @Override
    public String getSearchUrl()
    {
        return getUrlName();
    }

}
