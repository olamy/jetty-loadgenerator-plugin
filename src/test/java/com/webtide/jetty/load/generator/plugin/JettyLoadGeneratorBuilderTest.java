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

import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import org.apache.commons.io.IOUtils;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.StatisticsHandler;
import org.eclipse.jetty.server.session.HashSessionIdManager;
import org.eclipse.jetty.server.session.SessionHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.servlet.StatisticsServlet;
import org.eclipse.jetty.util.IO;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by olamy on 27/9/16.
 */
public class JettyLoadGeneratorBuilderTest
{
    @Rule
    public JenkinsRule j = new JenkinsRule();

    Server server;

    ServerConnector connector;

    StatisticsHandler statisticsHandler = new StatisticsHandler();

    @Before
    public void startJetty()
        throws Exception
    {
        QueuedThreadPool serverThreads = new QueuedThreadPool();
        serverThreads.setName( "server" );
        server = new Server( serverThreads );
        server.setSessionIdManager( new HashSessionIdManager() );
        connector = new ServerConnector( server, new HttpConnectionFactory( new HttpConfiguration() ) );
        server.addConnector( connector );

        server.setHandler( statisticsHandler );

        ServletContextHandler statsContext = new ServletContextHandler( statisticsHandler, "/" );

        statsContext.addServlet( new ServletHolder( new StatisticsServlet() ), "/stats" );

        statsContext.addServlet( new ServletHolder( new TestHandler() ), "/" );

        statsContext.setSessionHandler( new SessionHandler() );

        server.start();
    }

    @Test
    public void testWithgroovyScript()
        throws Exception
    {
        FreeStyleProject project = j.createFreeStyleProject();

        InputStream inputStream =
            Thread.currentThread().getContextClassLoader().getResourceAsStream( "website_profile.groovy" );

        int iteration = 2;

        JettyLoadGeneratorBuilder jettyLoadGeneratorBuilder =
            new JettyLoadGeneratorBuilder( IOUtils.toString( inputStream ), "localhost", connector.getLocalPort(), //
                                           1, "", 20, TimeUnit.SECONDS, //
                                           iteration, 1 );

        ResponseTimePerPath responseTimePerPath = new ResponseTimePerPath();

        jettyLoadGeneratorBuilder.addResponseTimeListener( responseTimePerPath );

        project.getBuildersList().add( jettyLoadGeneratorBuilder );

        FreeStyleBuild build = project.scheduleBuild2( 0 ).get();

        Assert.assertEquals( 12, responseTimePerPath.getResponseNumberPerPath().size() );

        for ( Map.Entry<String, AtomicInteger> entry : responseTimePerPath.getResponseNumberPerPath().entrySet() )
        {
            Assert.assertEquals( "not " + iteration + " but " + entry.getValue().get() + " for path " + entry.getKey(),
                                 //
                                 entry.getValue().get(), iteration );
        }

    }


    static class TestHandler
        extends HttpServlet
    {

        @Override
        protected void service( HttpServletRequest request, HttpServletResponse response )
            throws ServletException, IOException
        {

            String method = request.getMethod().toUpperCase( Locale.ENGLISH );

            HttpSession httpSession = request.getSession();

            switch ( method )
            {
                case "GET":
                {
                    response.getOutputStream().write( "Jetty rocks!!".getBytes() );
                    response.flushBuffer();
                    break;
                }
                case "POST":
                {
                    IO.copy( request.getInputStream(), response.getOutputStream() );
                    break;
                }
            }

        }
    }

}
