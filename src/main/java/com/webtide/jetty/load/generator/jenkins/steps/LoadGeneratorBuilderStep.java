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

package com.webtide.jetty.load.generator.jenkins.steps;

import hudson.Extension;
import org.jenkinsci.plugins.scriptsecurity.sandbox.Whitelist;
import org.jenkinsci.plugins.scriptsecurity.sandbox.whitelists.StaticWhitelist;
import org.jenkinsci.plugins.workflow.steps.AbstractStepDescriptorImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractStepImpl;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.mortbay.jetty.load.generator.Resource;
import org.mortbay.jetty.load.generator.starter.LoadGeneratorStarterArgs;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

/**
 *
 */
public class LoadGeneratorBuilderStep
    extends AbstractStepImpl
{

    private Resource resource;

    private String host;

    private int port;

    private int users;

    private String resourceFromFile;

    private String runningTime;

    private TimeUnit runningTimeUnit;

    private int runIteration;

    private int resourceRate;

    private LoadGeneratorStarterArgs.Transport transport;

    private boolean secureProtocol;

    private String jdkName;

    private String jvmExtraArgs;

    private int generatorNumber = 1;

    private int warmupNumber = 0;

    private String alpnVersion;

    private int threadsNumber = 1;

    @DataBoundConstructor
    public LoadGeneratorBuilderStep( Resource resource, String host, int port, int users, //
                                     String resourceFromFile, String runningTime, TimeUnit runningTimeUnit, //
                                     int runIteration, int resourceRate,
                                     LoadGeneratorStarterArgs.Transport transport, //
                                     boolean secureProtocol, String jvmExtraArgs, int generatorNumber, int warmupNumber,
                                     int threadsNumber )
    {
        this.resource = resource;
        this.host = host;
        this.port = port;
        this.users = users;
        this.resourceFromFile = resourceFromFile;
        this.runningTime = runningTime;
        this.runningTimeUnit = runningTimeUnit;
        this.runIteration = runIteration;
        this.resourceRate = resourceRate;
        this.transport = transport;
        this.secureProtocol = secureProtocol;
        this.jvmExtraArgs = jvmExtraArgs;
        this.generatorNumber = generatorNumber;
        this.warmupNumber = warmupNumber;
        this.threadsNumber = threadsNumber;
    }

    public Resource getResource()
    {
        return resource;
    }

    public String getHost()
    {
        return host;
    }

    public int getPort()
    {
        return port;
    }

    public int getUsers()
    {
        return users;
    }

    public String getResourceFromFile()
    {
        return resourceFromFile;
    }

    public String getRunningTime()
    {
        return runningTime;
    }

    public TimeUnit getRunningTimeUnit()
    {
        return runningTimeUnit;
    }

    public int getRunIteration()
    {
        return runIteration;
    }

    public int getResourceRate()
    {
        return resourceRate;
    }

    public LoadGeneratorStarterArgs.Transport getTransport()
    {
        return transport;
    }

    public boolean isSecureProtocol()
    {
        return secureProtocol;
    }

    public String getJvmExtraArgs()
    {
        return jvmExtraArgs;
    }

    @DataBoundSetter
    public void setJdkName( String jdkName )
    {
        this.jdkName = jdkName;
    }

    public String getJdkName()
    {
        return jdkName;
    }

    public int getGeneratorNumber()
    {
        return generatorNumber;
    }

    public int getWarmupNumber()
    {
        return warmupNumber;
    }

    public String getAlpnVersion()
    {
        return alpnVersion;
    }

    @DataBoundSetter
    public void setAlpnVersion( String alpnVersion )
    {
        this.alpnVersion = alpnVersion;
    }

    public int getThreadsNumber()
    {
        return threadsNumber;
    }

    @Extension
    public static class DescriptorImpl
        extends AbstractStepDescriptorImpl
    {
        public DescriptorImpl()
        {
            super( LoadGeneratorBuilderStepExecution.class );
        }

        public DescriptorImpl( Class<? extends StepExecution> executionType )
        {
            super( executionType );
        }

        @Override
        public String getFunctionName()
        {
            return "loadgenerator";
        }

        @Nonnull
        @Override
        public String getDisplayName()
        {
            return "HTTP Load Generator by Jetty";
        }
    }


    @Extension
    public static class LoadGeneratorWhileList
        extends Whitelist
    {
        private StaticWhitelist staticWhitelist;

        public LoadGeneratorWhileList()
        {
            try
            {
                try (InputStream inputStream = LoadGeneratorBuilderStep.class.getResourceAsStream(
                    "/com/webtide/jetty/load/generator/jenkins/steps/LoadGeneratorBuilderStep/loadgenerator-whilelist" ))
                {
                    try (InputStreamReader inputStreamReader = new InputStreamReader( inputStream ))
                    {
                        staticWhitelist = new StaticWhitelist( inputStreamReader );
                    }
                }
            }
            catch ( Exception e )
            {
                throw new RuntimeException( e.getMessage(), e );
            }
        }

        @Override
        public boolean permitsMethod( @Nonnull Method method, @Nonnull Object o, @Nonnull Object[] objects )
        {
            return staticWhitelist.permitsMethod( method, o, objects );
        }

        @Override
        public boolean permitsConstructor( @Nonnull Constructor<?> constructor, @Nonnull Object[] objects )
        {
            return staticWhitelist.permitsConstructor( constructor, objects );
        }

        @Override
        public boolean permitsStaticMethod( @Nonnull Method method, @Nonnull Object[] objects )
        {
            return staticWhitelist.permitsStaticMethod( method, objects );
        }

        @Override
        public boolean permitsFieldGet( @Nonnull Field field, @Nonnull Object o )
        {
            return staticWhitelist.permitsFieldGet( field, o );
        }

        @Override
        public boolean permitsFieldSet( @Nonnull Field field, @Nonnull Object o, @CheckForNull Object o1 )
        {
            return staticWhitelist.permitsFieldSet( field, o, o1 );
        }

        @Override
        public boolean permitsStaticFieldGet( @Nonnull Field field )
        {
            if ( field.getType().equals( LoadGeneratorStarterArgs.Transport.class ) || field.getType().equals(
                TimeUnit.class ) )
            {
                return true;
            }
            return staticWhitelist.permitsStaticFieldGet( field );
        }

        @Override
        public boolean permitsStaticFieldSet( @Nonnull Field field, @CheckForNull Object o )
        {
            return staticWhitelist.permitsStaticFieldSet( field, o );
        }
    }

}
