package com.webtide.jetty.load.generator.jenkins;

public class StatusResult
{
    protected String buildId;

    protected long _1xx, _2xx, _3xx, _4xx, _5xx;


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
