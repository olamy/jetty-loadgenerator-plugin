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
import org.eclipse.jetty.util.SocketAddressResolver;
import org.mortbay.jetty.load.generator.LoadGenerator;
import org.mortbay.jetty.load.generator.Resource;

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
                            Node currentNode,
                            List<Resource.NodeListener> nodeListeners, //
                            List<LoadGenerator.Listener> loadGeneratorListeners, //
                            List<String> args, String jvmExtraArgs, String monitorUrl )
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

            String response = startMonitor( monitorUrl, taskListener );
            taskListener.getLogger().println( "start monitor call " + response );

            channel.call( new LoadCaller( args, nodeListeners, loadGeneratorListeners ) );

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

    protected String startMonitor( String monitorUrl, TaskListener taskListener )
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
            taskListener.getLogger().println( "error calling start monitorUrl:" + monitorUrl + "," + e.getMessage() );
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

        private final List<?> nodeListeners;

        private final List<?> loadGeneratorListeners;

        public LoadCaller( List<String> args, List<?> nodeListeners, List<?> loadGeneratorListeners)
        {
            this.args = args;
            this.nodeListeners = nodeListeners;
            this.loadGeneratorListeners = loadGeneratorListeners;
        }

        @Override
        public Void call()
            throws Exception
        {
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

            Class jenkinsRemoteStarterClazz =
                classLoader.loadClass( "org.mortbay.jetty.load.generator.starter.JenkinsRemoteStarter" );

            Method setResponseTimeListeners =
                jenkinsRemoteStarterClazz.getMethod( "setNodeListeners", List.class );
            setResponseTimeListeners.invoke( null, nodeListeners );


            Method setLoadGeneratorListeners =
                jenkinsRemoteStarterClazz.getMethod( "setLoadGeneratorListeners", List.class );
            setLoadGeneratorListeners.invoke( null, loadGeneratorListeners );

            Method launch = jenkinsRemoteStarterClazz.getMethod( "launch", List.class );

            launch.invoke( null, args );

            return null;
        }
    }


}
