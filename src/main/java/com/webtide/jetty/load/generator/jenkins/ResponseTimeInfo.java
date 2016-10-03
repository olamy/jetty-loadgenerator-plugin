package com.webtide.jetty.load.generator.jenkins;

/**
 *
 */
public class ResponseTimeInfo
{

    // in ms
    private long timeStamp;

    // in ms
    private long time;

    public ResponseTimeInfo( long timeStamp, long time )
    {
        this.timeStamp = timeStamp;
        this.time = time;
    }

    public long getTimeStamp()
    {
        return timeStamp;
    }

    public long getTime()
    {
        return time;
    }
}
