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

package com.webtide.jetty.load.generator.jenkins;

import com.fasterxml.jackson.databind.ObjectMapper;
import hudson.model.Actionable;
import hudson.model.ProminentProjectAction;
import hudson.model.Run;
import hudson.util.RunList;
import org.eclipse.jetty.load.generator.CollectorInformations;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.LongAdder;

/**
 *
 */
public class LoadGeneratorProjectAction
    extends Actionable
    implements ProminentProjectAction
{

    private static final Logger LOGGER = LoggerFactory.getLogger( LoadGeneratorProjectAction.class );

    private final transient RunList<?> builds;

    public LoadGeneratorProjectAction( RunList<?> builds )
    {
        this.builds = builds == null ? new RunList<>() : builds;
    }


    public String getAllResponseTimeInformations()
        throws IOException
    {

        ObjectMapper objectMapper = new ObjectMapper();

        List<RunInformations> datas = new ArrayList<>();

        for ( Run run : this.builds )
        {
            LoadGeneratorBuildAction buildAction = run.getAction( LoadGeneratorBuildAction.class );
            if ( buildAction != null )
            {
                if ( buildAction.getGlobalResponseTimeInformations() != null )
                {
                    RunInformations runInformations =
                        new RunInformations( run.getId(), buildAction.getGlobalResponseTimeInformations() );
                    datas.add( runInformations );
                }
            }
        }

        // order by buildId

        Collections.sort( datas, Comparator.comparing( RunInformations::getBuildId));

        StringWriter stringWriter = new StringWriter();

        objectMapper.writeValue( stringWriter, datas );

        return stringWriter.toString();

    }

    public String getAllLatencyInformations()
        throws IOException
    {

        ObjectMapper objectMapper = new ObjectMapper();

        List<RunInformations> datas = new ArrayList<>();

        for ( Run run : this.builds )
        {
            LoadGeneratorBuildAction buildAction = run.getAction( LoadGeneratorBuildAction.class );
            if ( buildAction != null )
            {
                if ( buildAction.getGlobalLatencyTimeInformations() != null )
                {
                    RunInformations runInformations =
                        new RunInformations( run.getId(), buildAction.getGlobalLatencyTimeInformations() );
                    datas.add( runInformations );
                }
            }
        }

        // order by buildId

        Collections.sort( datas, Comparator.comparing( RunInformations::getBuildId ));

        StringWriter stringWriter = new StringWriter();

        objectMapper.writeValue( stringWriter, datas );

        return stringWriter.toString();

    }


    public static class RunInformations
        extends CollectorInformations
    {
        private String buildId;

        public RunInformations( String buildId, CollectorInformations collectorInformations )
        {
            this.buildId = buildId;
            setInformationType( collectorInformations.getInformationType() );
            totalCount( collectorInformations.getTotalCount() );
            minValue( collectorInformations.getMinValue() );
            maxValue( collectorInformations.getMaxValue() );
            value50( collectorInformations.getValue50() );
            value90( collectorInformations.getValue90() );
            mean( collectorInformations.getMean() );
            stdDeviation( collectorInformations.getStdDeviation() );
            startTimeStamp( collectorInformations.getStartTimeStamp() );
            endTimeStamp( collectorInformations.getEndTimeStamp() );
        }

        public String getBuildId()
        {
            return buildId;
        }

        public void setBuildId( String buildId )
        {
            this.buildId = buildId;
        }
    }

    public static class StatusResult {
        private String buildId;

        private long _1xx, _2xx, _3xx, _4xx, _5xx;


        public String getBuildId()
        {
            return buildId;
        }

        public long get1xx()
        {
            return _1xx;
        }

        public long get2xx()
        {
            return _2xx;
        }

        public long get3xx()
        {
            return _3xx;
        }

        public long get4xx()
        {
            return _4xx;
        }

        public long get5xx()
        {
            return _5xx;
        }
    }


    public void doResponseTimeTrend( StaplerRequest req, StaplerResponse rsp )
        throws IOException, ServletException
    {
        LOGGER.debug( "doResponseTimeTrend" );
        String data = getAllResponseTimeInformations();
        rsp.getWriter().write( data );
    }

    public void doLatencyTrend( StaplerRequest req, StaplerResponse rsp )
        throws IOException, ServletException
    {
        LOGGER.debug( "doLatencyTrend" );
        String data = getAllLatencyInformations();
        rsp.getWriter().write( data );
    }

    public void doPerFamilyStatusNumber( StaplerRequest req, StaplerResponse rsp )
        throws IOException, ServletException
    {
        LOGGER.debug( "doPerFamilyStatusNumber" );

        List<StatusResult> statusResults = new ArrayList<>(  );

        for ( Run run : this.builds )
        {
            LoadGeneratorBuildAction buildAction = run.getAction( LoadGeneratorBuildAction.class );
            if ( buildAction != null )
            {
                Map<Integer, LongAdder> re = buildAction.getResponseNumberPerStatusFamily();
                if ( re != null )
                {
                    StatusResult statusResult = new StatusResult();
                    statusResult.buildId = run.getId();
                    if (re.get( 1 ) != null)
                    {
                        statusResult._1xx = re.get( 1 ).longValue();
                    }
                    if (re.get( 2 ) != null)
                    {
                        statusResult._2xx = re.get( 2 ).longValue();
                    }
                    if (re.get( 3 ) != null)
                    {
                        statusResult._3xx = re.get( 3 ).longValue();
                    }
                    if (re.get( 4 ) != null)
                    {
                        statusResult._4xx = re.get( 4 ).longValue();
                    }
                    if (re.get( 5 ) != null)
                    {
                        statusResult._5xx = re.get( 5 ).longValue();
                    }
                    statusResults.add( statusResult );
                }
            }
        }

        Collections.sort( statusResults, Comparator.comparing( StatusResult::getBuildId));


        StringWriter stringWriter = new StringWriter(  );

        new ObjectMapper().writeValue( stringWriter, statusResults );

        rsp.getWriter().write( stringWriter.toString() );
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
