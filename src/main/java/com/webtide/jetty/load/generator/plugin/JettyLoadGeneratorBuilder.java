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

import com.fasterxml.jackson.databind.ObjectMapper;
import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Util;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;
import jenkins.tasks.SimpleBuildStep;
import org.HdrHistogram.Recorder;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.customizers.ImportCustomizer;
import org.eclipse.jetty.load.generator.HttpTransportBuilder;
import org.eclipse.jetty.load.generator.LoadGenerator;
import org.eclipse.jetty.load.generator.profile.ResourceProfile;
import org.eclipse.jetty.load.generator.responsetime.ResponseNumberPerPath;
import org.eclipse.jetty.load.generator.responsetime.ResponseTimeListener;
import org.eclipse.jetty.load.generator.responsetime.ResponseTimePerPath;
import org.eclipse.jetty.xml.XmlConfiguration;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.servlet.ServletException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 *
 */
public class JettyLoadGeneratorBuilder
    extends Builder
    implements SimpleBuildStep
{

    private static final Logger LOGGER = LoggerFactory.getLogger( JettyLoadGeneratorBuilder.class );

    private final String profileGroovy;

    private final String host;

    private final int port;

    private final int users;

    private final String profileXmlFromFile;

    private final int runningTime;

    private final TimeUnit runningTimeUnit;

    private final int runIteration;

    private final int transactionRate;

    private List<ResponseTimeListener> responseTimeListeners = new ArrayList<>();

    private ResourceProfile loadProfile;

    // Fields in config.jelly must match the parameter names in the "DataBoundConstructor"
    @DataBoundConstructor
    public JettyLoadGeneratorBuilder( String profileGroovy, String host, int port, int users, String profileXmlFromFile,
                                      int runningTime, TimeUnit runningTimeUnit, int runIteration, int transactionRate )
    {
        this.profileGroovy = Util.fixEmptyAndTrim( profileGroovy );
        this.host = host;
        this.port = port;
        this.users = users;
        this.profileXmlFromFile = profileXmlFromFile;
        this.runningTime = runningTime < 1 ? 30 : runningTime;
        this.runningTimeUnit = runningTimeUnit == null ? TimeUnit.SECONDS : runningTimeUnit;
        this.runIteration = runIteration;
        this.transactionRate = transactionRate == 0 ? 1 : transactionRate;
    }

    public String getProfileGroovy()
    {
        return profileGroovy;
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

    public String getProfileXmlFromFile()
    {
        return profileXmlFromFile;
    }

    public int getRunningTime()
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

    public void addResponseTimeListener( ResponseTimeListener responseTimeListener )
    {
        this.responseTimeListeners.add( responseTimeListener );
    }

    public int getTransactionRate()
    {
        return transactionRate;
    }

    public ResourceProfile getLoadProfile()
    {
        return loadProfile;
    }

    public void setLoadProfile( ResourceProfile loadProfile )
    {
        this.loadProfile = loadProfile;
    }

    @Override
    public boolean perform( AbstractBuild build, Launcher launcher, BuildListener listener )
    {
        try
        {
            run( listener, build.getWorkspace(), build.getRootBuild() );
        }
        catch ( Exception e )
        {
            throw new RuntimeException( e.getMessage(), e );
        }
        return true;
    }


    @Override
    public void perform( @Nonnull Run<?, ?> run, @Nonnull FilePath filePath, @Nonnull Launcher launcher,
                         @Nonnull TaskListener taskListener )
        throws InterruptedException, IOException
    {
        LOGGER.debug( "simpleBuildStep perform" );
        try
        {
            run( taskListener, filePath, run );
        }
        catch ( Exception e )
        {
            throw new RuntimeException( e.getMessage(), e );
        }
    }


    protected void run( TaskListener taskListener, FilePath workspace, Run<?, ?> run )
        throws Exception
    {

        LOGGER.info( "host: {}, port: {}", getHost(), getPort() );

        ResourceProfile resourceProfile =
            this.loadProfile == null ? loadResourceProfile( workspace ) : this.loadProfile;

        if ( resourceProfile == null )
        {
            LOGGER.error( "resource profile must be set, Build ABORTED" );
            run.setResult( Result.ABORTED );
            return;
        }

        ResponseTimePerPath responseTimePerPath = new ResponseTimePerPath();
        ResponseNumberPerPath responseNumberPerPath = new ResponseNumberPerPath();

        List<ResponseTimeListener> listeners = new ArrayList<>();
        if ( this.responseTimeListeners != null )
        {
            listeners.addAll( this.responseTimeListeners );
        }
        listeners.add( responseTimePerPath );
        listeners.add( responseNumberPerPath );

        // TODO remove that one which is for debug purpose
        if ( LOGGER.isDebugEnabled() )
        {
            listeners.add( new ResponseTimeListener()
            {
                @Override
                public void onResponseTimeValue( Values values )
                {
                    LOGGER.debug( "response time {} ms for path: {}", //
                                  TimeUnit.NANOSECONDS.toMillis( values.getTime() ), //
                                  values.getPath() );
                }

                @Override
                public void onLoadGeneratorStop()
                {
                    LOGGER.debug( "stop loadGenerator" );
                }
            } );

        }
        //ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool( 1);

        LoadGenerator loadGenerator = new LoadGenerator.Builder() //
            .host( getHost() ) //
            .port( getPort() ) //
            .users( getUsers() ) //
            .transactionRate( getTransactionRate() ) //
            .transport( LoadGenerator.Transport.HTTP ) //
            .httpClientTransport( new HttpTransportBuilder().build() ) //
            //.sslContextFactory( sslContextFactory ) //
            .loadProfile( resourceProfile ) //
            .responseTimeListeners( listeners.toArray( new ResponseTimeListener[listeners.size()] ) ) //
            //.requestListeners( testRequestListener ) //
            //.executor( new QueuedThreadPool() )
            .build();

        if ( runIteration > 0 )
        {
            loadGenerator.run( runIteration );
        }
        else
        {

            loadGenerator.run( runningTime, runningTimeUnit );
        }

        for ( Map.Entry<String, Recorder> entry : responseTimePerPath.getRecorderPerPath().entrySet() )
        {
            AtomicInteger number = responseNumberPerPath.getResponseNumberPerPath().get( entry.getKey() );
            LOGGER.info( "responseTimePerPath: {} - {}ms - number: {}", //
                         entry.getKey(), //
                         TimeUnit.NANOSECONDS.toMillis(
                             Math.round( entry.getValue().getIntervalHistogram().getMean() ) ), //
                         number.get() );
        }

        ObjectMapper objectMapper = new ObjectMapper();

        //filePath.

    }


    protected ResourceProfile loadResourceProfile( FilePath workspace )
        throws Exception
    {

        ResourceProfile resourceProfile = null;

        String groovy = StringUtils.trim( this.getProfileGroovy() );

        if ( StringUtils.isNotBlank( groovy ) )
        {
            CompilerConfiguration compilerConfiguration = new CompilerConfiguration( CompilerConfiguration.DEFAULT );
            compilerConfiguration.setDebug( true );
            compilerConfiguration.setVerbose( true );

            compilerConfiguration.addCompilationCustomizers(
                new ImportCustomizer().addStarImports( "org.eclipse.jetty.load.generator.profile" ) );

            GroovyShell interpreter = new GroovyShell( ResourceProfile.class.getClassLoader(), //
                                                       new Binding(), //
                                                       compilerConfiguration );

            resourceProfile = (ResourceProfile) interpreter.evaluate( groovy );
        }
        else
        {

            String profileXmlPath = getProfileXmlFromFile();

            if ( StringUtils.isNotBlank( profileXmlPath ) )
            {
                FilePath profileXmlFilePath = workspace.child( profileXmlPath );
                String xml = IOUtils.toString( profileXmlFilePath.read() );
                LOGGER.debug( "profileXml: {}", xml );
                resourceProfile = (ResourceProfile) new XmlConfiguration( xml ).configure();
            }
        }

        return resourceProfile;
    }


    @Override
    public DescriptorImpl getDescriptor()
    {
        return (DescriptorImpl) super.getDescriptor();
    }

    /**
     * Descriptor for {@link JettyLoadGeneratorBuilder}. Used as a singleton.
     * See <tt>views/hudson/plugins/hello_world/JettyLoadGeneratorBuilder/*.jelly</tt>
     * for the actual HTML fragment for the configuration screen.
     */
    @Extension
    public static final class DescriptorImpl
        extends BuildStepDescriptor<Builder>
    {

        private static final List<TimeUnit> TIME_UNITS = Arrays.asList( TimeUnit.DAYS, //
                                                                        TimeUnit.HOURS, //
                                                                        TimeUnit.MINUTES, //
                                                                        TimeUnit.SECONDS, //
                                                                        TimeUnit.MILLISECONDS );

        /**
         * This human readable name is used in the configuration screen.
         */
        public String getDisplayName()
        {
            return "Jetty LoadGenerator";
        }

        public List<TimeUnit> getTimeUnits()
        {
            return TIME_UNITS;
        }

        public FormValidation doCheckPort( @QueryParameter String value )
            throws IOException, ServletException
        {
            try
            {
                int port = Integer.parseInt( value );
                if ( port < 1 )
                {
                    return FormValidation.error( "port must be a positive number" );
                }
            }
            catch ( NumberFormatException e )
            {
                return FormValidation.error( "port must be number" );
            }

            return FormValidation.ok();
        }

        public FormValidation doCheckUsers( @QueryParameter String value )
            throws IOException, ServletException
        {
            try
            {
                int port = Integer.parseInt( value );
                if ( port < 1 )
                {
                    return FormValidation.error( "users must be a positive number" );
                }
            }
            catch ( NumberFormatException e )
            {
                return FormValidation.error( "users must be number" );
            }

            return FormValidation.ok();
        }

        public FormValidation doCheckRunningTime( @QueryParameter String value )
            throws IOException, ServletException
        {
            try
            {
                int runningTime = Integer.parseInt( value );
                if ( runningTime < 1 )
                {
                    return FormValidation.error( "running time must be a positive number" );
                }
            }
            catch ( NumberFormatException e )
            {
                return FormValidation.error( "running time must be number" );
            }

            return FormValidation.ok();
        }

        public FormValidation doCheckTransactionRate( @QueryParameter String value )
            throws IOException, ServletException
        {
            try
            {
                int transactionRate = Integer.parseInt( value );
                if ( transactionRate <= 0 )
                {
                    return FormValidation.error( "transactionRate must be a positive number" );
                }
            }
            catch ( NumberFormatException e )
            {
                return FormValidation.error( "transactionRate time must be number" );
            }

            return FormValidation.ok();
        }

        public boolean isApplicable( Class<? extends AbstractProject> aClass )
        {
            // indicates that this builder can be used with all kinds of project types 
            return true;
        }

        /*
        @Override
        public boolean configure( StaplerRequest req, JSONObject formData )
            throws FormException
        {
            save();
            return super.configure( req, formData );
        }*/

    }

}

