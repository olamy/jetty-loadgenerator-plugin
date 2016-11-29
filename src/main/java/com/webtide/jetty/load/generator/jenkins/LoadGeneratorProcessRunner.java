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

import hudson.FilePath;
import hudson.Launcher;
import hudson.model.JDK;
import hudson.model.Node;
import hudson.model.TaskListener;
import hudson.remoting.Channel;
import hudson.util.ArgumentListBuilder;
import jenkins.model.Jenkins;
import jenkins.security.MasterToSlaveCallable;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.load.generator.latency.LatencyTimeListener;
import org.eclipse.jetty.util.SocketAddressResolver;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 *
 */
public class LoadGeneratorProcessRunner
    implements Serializable
{

    public void runProcess( TaskListener taskListener, FilePath workspace, Launcher launcher, String jdkName, //
                            Node currentNode, List<?> responseTimeListeners,
                            List<LatencyTimeListener> latencyTimeListeners, //
                            List<String> args, String jvmExtraArgs, int dryRun, String monitorUrl )
        throws Exception
    {
        Channel channel = null;

        try
        {
            long start = System.nanoTime();

            JDK jdk =
                jdkName == null ? null : Jenkins.getInstance().getJDK( jdkName ).forNode( currentNode, taskListener );
            channel =
                new LoadGeneratorProcessFactory().buildChannel( taskListener, jdk, workspace, launcher, jvmExtraArgs );

            // first run as dry run if configured
            if (dryRun > 0)
            {
                ArgumentListBuilder cmdLine = new ArgumentListBuilder(args.toArray( new String[args.size()] )) //
                .add( "-ri" ).add( dryRun ) //
                .add( "-notint" );
                channel.call( new LoadCaller( cmdLine.toList(), responseTimeListeners, latencyTimeListeners ) );
                for ( Object listener : responseTimeListeners) {
                    if (listener instanceof ValuesFileWriter) {
                        ((ValuesFileWriter)listener).reset();
                    }
                }
                for ( Object listener : latencyTimeListeners) {
                    if (listener instanceof ValuesFileWriter) {
                        ((ValuesFileWriter)listener).reset();
                    }
                }
            }


            String response = startMonitor(monitorUrl);
            taskListener.getLogger().println( "start monitor call " + response );

            channel.call( new LoadCaller( args, responseTimeListeners, latencyTimeListeners ) );

            long end = System.nanoTime();

            taskListener.getLogger().println( "remote LoadGenerator execution done: " //
                                                  + TimeUnit.NANOSECONDS.toMillis( end - start ) //
                                                  + " ms " );
        }
        finally
        {
            if ( channel != null )
            {
                channel.close();
            }
        }

    }

    protected String startMonitor( String monitorUrl )
        throws Exception
    {
        HttpClient httpClient = new HttpClient();
        httpClient.setSocketAddressResolver( new SocketAddressResolver.Sync() );
        try
        {
            httpClient.start();
            String url = monitorUrl + "?start=true";
            ContentResponse contentResponse = httpClient.newRequest( url ).send();
            return contentResponse.getContentAsString();
        }
        catch ( Exception e )
        {
            return "";
        }
        finally
        {
            httpClient.stop();
        }

    }


    private static class LoadCaller
        extends MasterToSlaveCallable<Void, Exception>
        implements Serializable
    {
        private final List<String> args;

        private final List<?> responseTimeListeners;

        private final List<?> latencyTimeListeners;

        public LoadCaller( List<String> args, List<?> responseTimeListeners, List<?> latencyTimeListeners )
        {
            this.args = args;
            this.responseTimeListeners = responseTimeListeners;
            this.latencyTimeListeners = latencyTimeListeners;
        }

        @Override
        public Void call()
            throws Exception
        {
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

            Class jenkinsRemoteStarterClazz =
                classLoader.loadClass( "org.eclipse.jetty.load.generator.starter.JenkinsRemoteStarter" );

            Method setResponseTimeListeners =
                jenkinsRemoteStarterClazz.getMethod( "setResponseTimeListeners", List.class );
            setResponseTimeListeners.invoke( null, responseTimeListeners );

            Method setLatencyTimeListeners =
                jenkinsRemoteStarterClazz.getMethod( "setLatencyTimeListeners", List.class );
            setLatencyTimeListeners.invoke( null, latencyTimeListeners );

            Method launch = jenkinsRemoteStarterClazz.getMethod( "launch", List.class );

            launch.invoke( null, args );

            return null;
        }
    }


}
