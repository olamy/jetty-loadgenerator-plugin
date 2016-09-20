package com.webtide.jetty.load.generator.plugin;

/**
 *
 */
public class LoadGeneratorConfiguration
{

    private String profileXml;

    private String host;

    private int port;

    private int users;

    public String getHost()
    {
        return host;
    }

    public void setHost( String host )
    {
        this.host = host;
    }

    public int getPort()
    {
        return port;
    }

    public void setPort( int port )
    {
        this.port = port;
    }

    public String getProfileXml()
    {
        return profileXml;
    }

    public void setProfileXml( String profileXml )
    {
        this.profileXml = profileXml;
    }

    public int getUsers()
    {
        return users;
    }

    public void setUsers( int users )
    {
        this.users = users;
    }
}
