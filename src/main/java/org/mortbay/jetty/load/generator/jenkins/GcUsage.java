package org.mortbay.jetty.load.generator.jenkins;

public class GcUsage
{
    // values ms or MiB
    protected String youngCount, youngTime, oldCount, oldTime, youngGarbage, oldGarbage;

    protected String buildId;

    public String getYoungCount()
    {
        return youngCount == null ? "" : youngCount;
    }

    public String getYoungTime()
    {
        return youngTime == null ? "" : youngTime;
    }

    public String getOldCount()
    {
        return oldCount == null ? "" : oldCount;
    }

    public String getOldTime()
    {
        return oldTime == null ? "" : oldTime;
    }

    public String getYoungGarbage()
    {
        return youngGarbage == null ? "" : youngGarbage;
    }

    public String getOldGarbage()
    {
        return oldGarbage == null ? "" : oldGarbage;
    }

    public String getBuildId()
    {
        return buildId == null ? "" : buildId;
    }
}
