//
//  ========================================================================
//  Copyright (c) 1995-2018 Webtide LLC, Olivier Lamy
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

import org.mortbay.jetty.load.generator.Resource;

import java.io.Serializable;

/**
 *
 */
public class Values
    implements Serializable
{

    /**
     * the timestamp in nano seconds
     */
    private long eventTimestamp;

    private String path;

    /**
     * the value in nano seconds
     */
    private long responseTime;

    /**
     * the value in nano seconds
     */
    private long latencyTime;

    private String method;

    private long size;

    private int status;

    public Values()
    {
        // no op
    }

    public String getPath()
    {
        return path;
    }

    public void setPath( String path )
    {
        this.path = path;
    }

    public Values path( String path )
    {
        this.path = path;
        return this;
    }

    public long getResponseTime()
    {
        return responseTime;
    }

    public void setResponseTime( long time )
    {
        this.responseTime = time;
    }

    public Values responseTime( long time )
    {
        this.responseTime = time;
        return this;
    }

    public long getLatencyTime()
    {
        return latencyTime;
    }

    public void setLatencyTime( long latencyTime )
    {
        this.latencyTime = latencyTime;
    }

    public Values latencyTime( long latencyTime )
    {
        this.latencyTime = latencyTime;
        return this;
    }

    public String getMethod()
    {
        return method;
    }

    public void setMethod( String method )
    {
        this.method = method;
    }

    public Values method( String method )
    {
        this.method = method;
        return this;
    }

    public long getSize()
    {
        return size;
    }

    public void setSize( long size )
    {
        this.size = size;
    }

    public Values size( long size )
    {
        this.size = size;
        return this;
    }

    public int getStatus()
    {
        return status;
    }

    public void setStatus( int status )
    {
        this.status = status;
    }

    public Values status( int status )
    {
        this.status = status;
        return this;
    }

    public long getEventTimestamp()
    {
        return eventTimestamp;
    }

    public void setEventTimestamp( long eventTimestamp )
    {
        this.eventTimestamp = eventTimestamp;
    }

    public Values eventTimestamp( long eventTimestamp )
    {
        this.eventTimestamp = eventTimestamp;
        return this;
    }

    @Override
    public String toString()
    {
        return "Values{" + "eventTimestamp=" + eventTimestamp + ", path='" + path + '\'' + ", responseTime="
            + responseTime + ", latencyTime=" + latencyTime + ", method='" + method + '\'' + ", size=" + size
            + ", status=" + status + '}';
    }

    public Resource.Info getInfo()
    {
        Resource resource = new Resource( this.path ).method( this.method );
        Resource.Info info = resource.newInfo();
        info.setStatus( this.status );
        info.setLatencyTime( this.latencyTime );
        info.setRequestTime( this.eventTimestamp );
        info.setResponseTime( this.responseTime );
        return info;
    }

}
