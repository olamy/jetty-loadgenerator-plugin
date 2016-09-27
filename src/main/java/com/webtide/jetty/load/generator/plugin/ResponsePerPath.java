//
//  ========================================================================
//  Copyright (c) 1995-2016 Webtide LLC
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

package com.webtide.jetty.load.generator.plugin;

import org.HdrHistogram.Recorder;
import org.eclipse.jetty.load.generator.responsetime.ResponseTimeListener;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by olamy on 27/9/16.
 */
public class ResponsePerPath implements ResponseTimeListener
{

    private final Map<String, Recorder> recorderPerPath = new ConcurrentHashMap<>();

    private final Map<String, AtomicInteger> responseNumberPerPath = new ConcurrentHashMap<>();

    @Override
    public void onResponseTimeValue( Values values )
    {
        String path = values.getPath();

        // histogram record
        {
            Recorder recorder = recorderPerPath.get( path );
            if ( recorder == null )
            {
                recorder = new Recorder( TimeUnit.MICROSECONDS.toNanos( 1 ), //
                                         TimeUnit.MINUTES.toNanos( 1 ), //
                                         3 );
                recorderPerPath.put( path, recorder );
            }
            recorder.recordValue( values.getTime() );
        }

        // response number record
        {

            AtomicInteger number = responseNumberPerPath.get( path );
            if ( number == null )
            {
                number = new AtomicInteger( 1 );
                responseNumberPerPath.put( path, number );
            }
            else
            {
                number.incrementAndGet();
            }

        }
    }


    @Override
    public void onLoadGeneratorStop()
    {

    }

    public Map<String, Recorder> getRecorderPerPath()
    {
        return recorderPerPath;
    }

    public Map<String, AtomicInteger> getResponseNumberPerPath()
    {
        return responseNumberPerPath;
    }
}
