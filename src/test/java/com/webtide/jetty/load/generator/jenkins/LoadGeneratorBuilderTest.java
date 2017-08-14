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

import com.beust.jcommander.JCommander;
import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.core.Versioned;
import com.fasterxml.jackson.databind.ObjectMapper;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.FreeStyleProject;
import hudson.model.Run;
import hudson.model.TaskListener;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.text.StrSubstitutor;
import org.eclipse.jetty.client.HttpClientTransport;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.io.ClientConnectionFactory;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.StatisticsHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.servlet.StatisticsServlet;
import org.eclipse.jetty.toolchain.perf.PlatformTimer;
import org.eclipse.jetty.util.IO;
import org.eclipse.jetty.util.Trie;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.mortbay.jetty.load.generator.starter.LoadGeneratorStarterArgs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.mortbay.jetty.load.generator.LoadGenerator;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 *
 */
public class LoadGeneratorBuilderTest
{
    private static final Logger LOGGER = LoggerFactory.getLogger( LoadGeneratorBuilderTest.class );

    @Rule
    public JenkinsRule j = new JenkinsRule();

    Server server;

    ServerConnector connector;

    StatisticsHandler statisticsHandler = new StatisticsHandler();

    TestHandler testHandler = new TestHandler();

    @Before
    public void startJetty()
        throws Exception
    {
        QueuedThreadPool serverThreads = new QueuedThreadPool();
        serverThreads.setName( "server" );
        server = new Server( serverThreads );
        connector = new ServerConnector( server, new HttpConnectionFactory( new HttpConfiguration() ) );
        server.addConnector( connector );

        server.setHandler( statisticsHandler );

        ServletContextHandler statsContext = new ServletContextHandler( statisticsHandler, "/" );

        statsContext.addServlet( new ServletHolder( new StatisticsServlet() ), "/stats" );

        statsContext.addServlet( new ServletHolder( testHandler ), "/" );

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

        LoadGeneratorBuilder loadGeneratorBuilder =
            new LoadGeneratorBuilder( IOUtils.toString( inputStream ), //
                                      "localhost", //
                                      Integer.toString( connector.getLocalPort()), //
                                      "1", //
                                      "",  //
                                      Integer.toString( 20 ), //
                                      TimeUnit.SECONDS, //
                                      Integer.toString(iteration), //
                                      "1", //
                                      LoadGeneratorStarterArgs.Transport.HTTP, //
                                      false, 1 );
        //loadGeneratorBuilder.setJvmExtraArgs( "-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=8000" );

        project.getBuildersList().add( loadGeneratorBuilder );
        Run run = j.assertBuildStatusSuccess( project.scheduleBuild2( 0 ).get() );
        LOGGER.info( "request received: {}", testHandler.requestCounter.get());
        doAssert( run, iteration );
    }


    @Test
    public void testWithWorkflow() throws Exception
    {
        WorkflowJob project = j.jenkins.createProject( WorkflowJob.class, "foo");

        InputStream inputStream =
            Thread.currentThread().getContextClassLoader().getResourceAsStream( "pipeline.groovy" );

        int iteration = 2;

        Map<String,String> values = new HashMap<>(  );
        values.put( "port" , Integer.toString( connector.getLocalPort()));
        values.put( "iteration", Integer.toString( iteration ) );

        String script = StrSubstitutor.replace( IOUtils.toString( inputStream ), values );

        project.setDefinition(new CpsFlowDefinition( script ));

        Run run = j.assertBuildStatusSuccess( project.scheduleBuild2( 0 ).get() );

        doAssert( run, iteration );

    }

    protected void doAssert(Run run, int iteration ) throws Exception
    {

        LoadGeneratorBuildAction action = run.getAction( LoadGeneratorBuildAction.class );

        Assert.assertEquals( 12, action.getAllResponseInfoTimePerPath().size() );

        for ( Map.Entry<String, List<ResponseTimeInfo>> entry : action.getAllResponseInfoTimePerPath().entrySet() ) // responseNumberPerPath.getResponseNumberPerPath().entrySet() )
        {
            Assert.assertEquals( "not " + iteration + " but " + entry.getValue().size() + " for path " + entry.getKey(),
                                 //
                                 entry.getValue().size(), iteration );
        }

    }


    static class TestHandler
        extends HttpServlet
    {

        protected AtomicInteger requestCounter = new AtomicInteger( 0 );

        @Override
        protected void service( HttpServletRequest request, HttpServletResponse response )
            throws ServletException, IOException
        {
            requestCounter.incrementAndGet();
            String method = request.getMethod().toUpperCase( Locale.ENGLISH );
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

    @Extension
    public static class UnitTestDecorator extends LoadGeneratorProcessClasspathDecorator
    {
        @Override
        public String decorateClasspath( String cp, TaskListener listener, FilePath slaveRoot,
                                         Launcher launcher )
            throws Exception
        {

            List<Class> classes =
                Arrays.asList( JCommander.class, LoadGenerator.class, ObjectMapper.class, Versioned.class, //
                               JsonView.class, HttpMethod.class, Trie.class, HttpClientTransport.class, //
                               ClientConnectionFactory.class, PlatformTimer.class );

            for (Class clazz : classes)
            {
                cp = cp + ( launcher.isUnix() ? ":" : ";" ) //
                    + LoadGeneratorProcessFactory.classPathEntry( slaveRoot, clazz, clazz.getSimpleName(), listener );
            }


            return cp;

        }
    }

}
