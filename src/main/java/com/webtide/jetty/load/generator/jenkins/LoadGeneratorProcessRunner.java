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
import jenkins.model.Jenkins;
import jenkins.security.MasterToSlaveCallable;

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

    public void runProcess( TaskListener taskListener, FilePath workspace, Launcher launcher, String jdkName,
                            Node currentNode, List<?> responseTimeListeners, List<String> args )
        throws Exception
    {
        Channel channel = null;

        try
        {
            long start = System.nanoTime();

            JDK jdk = jdkName == null ? null : Jenkins.getInstance().getJDK( jdkName ).forNode( currentNode, taskListener );
            channel = new LoadGeneratorProcessFactory().buildChannel( taskListener, jdk, workspace, launcher );

            channel.call( new LoadCaller( args, responseTimeListeners ) );

            long end = System.nanoTime();

            taskListener.getLogger().println( "remote LoadGenerator execution done: " //
                                                  + TimeUnit.NANOSECONDS.toMillis( end - start ) //
                                                  + " ms ");
        }
        finally
        {
            if (channel != null)
            {
                channel.close();
            }
        }

    }

    private static class LoadCaller
        extends MasterToSlaveCallable<Void, Exception>
        implements Serializable
    {
        private final List<String> args;

        private final List<?> responseTimeListeners;

        public LoadCaller( List<String> args, List<?> responseTimeListeners )
        {
            this.args = args;
            this.responseTimeListeners = responseTimeListeners;
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

            Method launch = jenkinsRemoteStarterClazz.getMethod( "launch", List.class );

            launch.invoke( null, args );

            return null;
        }
    }


}
