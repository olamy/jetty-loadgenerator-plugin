package com.webtide.jetty.load.generator.jenkins;

import org.mortbay.jetty.load.generator.listeners.CollectorInformations;

public class RunInformations
    extends CollectorInformations
{
    private String buildId;

    private String jettyVersion;

    public RunInformations( String buildId, CollectorInformations collectorInformations )
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

}
