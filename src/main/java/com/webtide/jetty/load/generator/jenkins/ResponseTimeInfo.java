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

/**
 *
 */
public class ResponseTimeInfo
{

    // in ms
    private long timeStamp;

    // in ms
    private long time;

    private int status;

    public ResponseTimeInfo( long timeStamp, long time, int status )
    {
        this.timeStamp = timeStamp;
        this.time = time;
        this.status = status;
    }

    public long getTimeStamp()
    {
        return timeStamp;
    }

    public long getTime()
    {
        return time;
    }

    public int getStatus()
    {
        return status;
    }
}
