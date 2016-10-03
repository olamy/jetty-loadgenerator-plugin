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
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
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

    public void doGraph( StaplerRequest req, StaplerResponse rsp )
        throws IOException
    {
        LOGGER.debug( "doGraph" );
    }

    public void doIndex( StaplerRequest req, StaplerResponse rsp )
        throws IOException
    {
        LOGGER.debug( "doIndex" );
    }

    public void doGetData( StaplerRequest req, StaplerResponse rsp )
        throws IOException
    {
        LOGGER.debug( "doGetData" );
    }

    public String getData()
        throws IOException
    {

        ObjectMapper objectMapper = new ObjectMapper();

        /*

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat( "yyyy-MM-dd HH:mm:ss" );

        List<JsonData> datas = new ArrayList<>();

        for ( Run run : project.getBuilds() )
        {
            LoadGeneratorBuildAction buildAction = run.getAction( LoadGeneratorBuildAction.class );
            if ( buildAction != null )
            {
                CollectorInformations collectorInformations = buildAction.getGlobalCollectorInformations();
                String date = simpleDateFormat.format( new Date( collectorInformations.getEndTimeStamp() ) );
                datas.add( new JsonData() //
                               .date( date ) //
                               .mean( Double.toString( collectorInformations.getMean() ) ) //
                               .percentile50( Long.toString( collectorInformations.getValue50() ) ) //
                               .percentile90( Long.toString( collectorInformations.getValue90() ) ) //
                );
            }
        }



        StringWriter stringWriter = new StringWriter();

        objectMapper.writeValue( stringWriter, datas );

        return stringWriter.toString();
        */

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
        String data = getData();
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


    public static final class JsonData
    {
        private String date;

        private String mean;

        private String percentile90;

        private String percentile50;

        public String getDate()
        {
            return date;
        }

        public String getMean()
        {
            return mean;
        }

        public String getPercentile90()
        {
            return percentile90;
        }

        public JsonData percentile90( String percentile90 )
        {
            this.percentile90 = percentile90;
            return this;
        }

        public JsonData date( String date )
        {
            this.date = date;
            return this;
        }

        public JsonData mean( String mean )
        {
            this.mean = mean;
            return this;
        }

        public String getPercentile50()
        {
            return percentile50;
        }

        public JsonData percentile50( String percentile50 )
        {
            this.percentile50 = percentile50;
            return this;
        }
    }
}
