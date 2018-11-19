package org.mortbay.jetty.load.generator.jenkins;

import org.mortbay.jetty.load.generator.listeners.CollectorInformations;
import org.mortbay.jetty.load.generator.listeners.ServerInfo;

import java.text.SimpleDateFormat;
import java.util.Date;

public class RunInformations
    extends CollectorInformations
{
    private String buildId;

    private String jettyVersion;

    private String timestampStr;

    private int estimatedQps;

    private String transport;

    private ServerInfo serverInfo;

    public RunInformations( String buildId, CollectorInformations collectorInformations, String transport )
    {
        this.buildId = buildId;
        totalCount( collectorInformations.getTotalCount() );
        minValue( collectorInformations.getMinValue() );
        maxValue( collectorInformations.getMaxValue() );
        value50( collectorInformations.getValue50() );
        value90( collectorInformations.getValue90() );
        mean( collectorInformations.getMean() );
        stdDeviation( collectorInformations.getStdDeviation() );
        startTimeStamp( collectorInformations.getStartTimeStamp() );
        endTimeStamp( collectorInformations.getEndTimeStamp() );
        timestampStr = new SimpleDateFormat( "dd/MM/yyyy HH:mm:ss" ) //
            .format( new Date( collectorInformations.getStartTimeStamp() ) );
        this.transport = transport;
    }

    public String getTimestampStr()
    {
        return timestampStr;
    }

    public String getBuildId()
    {
        return buildId;
    }

    public void setBuildId( String buildId )
    {
        this.buildId = buildId;
    }

    public String getJettyVersion()
    {
        return jettyVersion;
    }

    public void setJettyVersion( String jettyVersion )
    {
        this.jettyVersion = jettyVersion;
    }

    public RunInformations jettyVersion( String jettyVersion )
    {
        this.jettyVersion = jettyVersion;
        return this;
    }

    public int getEstimatedQps()
    {
        return estimatedQps;
    }

    public void setEstimatedQps( int estimatedQps )
    {
        this.estimatedQps = estimatedQps;
    }

    public RunInformations estimatedQps( int estimatedQps )
    {
        this.estimatedQps = estimatedQps;
        return this;
    }

    public ServerInfo getServerInfo()
    {
        return serverInfo;
    }

    public void setServerInfo( ServerInfo serverInfo )
    {
        this.serverInfo = serverInfo;
    }

    public RunInformations serverInfo( ServerInfo serverInfo )
    {
        this.serverInfo = serverInfo;
        return this;
    }

    public String getTransport()
    {
        return transport;
    }

    public void setTransport( String transport )
    {
        this.transport = transport;
    }
}
