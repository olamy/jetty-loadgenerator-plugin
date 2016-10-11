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

import com.fasterxml.jackson.databind.ObjectMapper;
import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Proc;
import hudson.Util;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Executor;
import hudson.model.HealthReport;
import hudson.model.JDK;
import hudson.model.Node;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.remoting.Which;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.ArgumentListBuilder;
import hudson.util.FormValidation;
import jenkins.model.Jenkins;
import jenkins.security.MasterToSlaveCallable;
import jenkins.tasks.SimpleBuildStep;
import org.HdrHistogram.Histogram;
import org.HdrHistogram.Recorder;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Zip;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.customizers.ImportCustomizer;
import org.eclipse.jetty.client.HttpClientTransport;
import org.eclipse.jetty.load.generator.CollectorInformations;
import org.eclipse.jetty.load.generator.Http2TransportBuilder;
import org.eclipse.jetty.load.generator.HttpFCGITransportBuilder;
import org.eclipse.jetty.load.generator.HttpTransportBuilder;
import org.eclipse.jetty.load.generator.LoadGenerator;
import org.eclipse.jetty.load.generator.profile.ResourceProfile;
import org.eclipse.jetty.load.generator.report.DetailledResponseTimeReport;
import org.eclipse.jetty.load.generator.report.DetailledResponseTimeReportListener;
import org.eclipse.jetty.load.generator.report.GlobalSummaryReportListener;
import org.eclipse.jetty.load.generator.report.SummaryReport;
import org.eclipse.jetty.load.generator.responsetime.ResponseNumberPerPath;
import org.eclipse.jetty.load.generator.responsetime.ResponseTimeListener;
import org.eclipse.jetty.load.generator.responsetime.ResponseTimePerPathListener;
import org.eclipse.jetty.load.generator.starter.LoadGeneratorStarter;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.xml.XmlConfiguration;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.servlet.ServletException;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 *
 */
public class LoadGeneratorBuilder
    extends Builder
    implements SimpleBuildStep
{

    public static final String REPORT_DIRECTORY_NAME = "load-generator-reports";

    public static final String SUMMARY_REPORT_FILE = "summaryReport.json";

    public static final String GLOBAL_SUMMARY_REPORT_FILE = "globalSummaryReport.json";

    private static final Logger LOGGER = LoggerFactory.getLogger( LoadGeneratorBuilder.class );

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

    private LoadGenerator.Transport transport;

    private boolean embeddedLoadGenerator = true;

    private boolean secureProtocol;

    private String jdkName;

    // Fields in config.jelly must match the parameter names in the "DataBoundConstructor"
    @DataBoundConstructor
    public LoadGeneratorBuilder( String profileGroovy, String host, int port, int users, String profileXmlFromFile,
                                 int runningTime, TimeUnit runningTimeUnit, int runIteration, int transactionRate,
                                 LoadGenerator.Transport transport, boolean secureProtocol,
                                 boolean embeddedLoadGenerator )
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
        this.transport = transport;
        this.secureProtocol = secureProtocol;
        this.embeddedLoadGenerator = embeddedLoadGenerator;
    }

    public LoadGeneratorBuilder( ResourceProfile resourceProfile, String host, int port, int users,
                                 String profileXmlFromFile, int runningTime, TimeUnit runningTimeUnit, int runIteration,
                                 int transactionRate, LoadGenerator.Transport transport, boolean secureProtocol,
                                 boolean embeddedLoadGenerator )
    {
        this.profileGroovy = null;
        this.loadProfile = resourceProfile;
        this.host = host;
        this.port = port;
        this.users = users;
        this.profileXmlFromFile = profileXmlFromFile;
        this.runningTime = runningTime < 1 ? 30 : runningTime;
        this.runningTimeUnit = runningTimeUnit == null ? TimeUnit.SECONDS : runningTimeUnit;
        this.runIteration = runIteration;
        this.transactionRate = transactionRate == 0 ? 1 : transactionRate;
        this.transport = transport;
        this.secureProtocol = secureProtocol;
        this.embeddedLoadGenerator = embeddedLoadGenerator;
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

    public LoadGenerator.Transport getTransport()
    {
        return transport;
    }

    public boolean isSecureProtocol()
    {
        return secureProtocol;
    }

    public boolean isEmbeddedLoadGenerator()
    {
        return embeddedLoadGenerator;
    }

    public String getJdkName()
    {
        return jdkName;
    }

    @DataBoundSetter
    public void setJdkName( String jdkName )
    {
        this.jdkName = jdkName;
    }

    @Override
    public boolean perform( AbstractBuild build, Launcher launcher, BuildListener listener )
    {
        try
        {
            doRun( listener, build.getWorkspace(), build.getRootBuild(), launcher );
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
            doRun( taskListener, filePath, run, launcher );
        }
        catch ( Exception e )
        {
            throw new RuntimeException( e.getMessage(), e );
        }
    }


    public void doRun( TaskListener taskListener, FilePath workspace, Run<?, ?> run, Launcher launcher )
        throws Exception
    {

        ResourceProfile resourceProfile =
            this.loadProfile == null ? loadResourceProfile( workspace ) : this.loadProfile;

        if ( resourceProfile == null )
        {
            taskListener.getLogger().println( "resource profile must be set, Build ABORTED" );
            LOGGER.error( "resource profile must be set, Build ABORTED" );
            run.setResult( Result.ABORTED );
            return;
        }

        if ( this.embeddedLoadGenerator )
        {
            runEmbeddedLoadGenerator( taskListener, workspace, run, resourceProfile );
        }
        else
        {
            runForked( taskListener, workspace, run, launcher, resourceProfile );
        }
    }

    protected void runForked( TaskListener taskListener, FilePath workspace, Run<?, ?> run, Launcher launcher,
                              ResourceProfile resourceProfile )
        throws Exception
    {
        boolean isMaster = getCurrentNode() == Jenkins.getInstance();
        FilePath slaveRoot = null;
        if ( !isMaster )
        {
            slaveRoot = getCurrentNode().getRootPath();
        }

        final String tmpFilePath = getCurrentNode().getChannel().call( new CopyResourceProfile( resourceProfile ) );

        ArgumentListBuilder cmdLine = new ArgumentListBuilder();

        if ( jdkName != null )
        {
            JDK jdk = Jenkins.getInstance().getJDK( this.jdkName ).forNode( getCurrentNode(), taskListener );
            String jdkHome = jdk.getHome();
            LOGGER.debug( "jdkHome: {}", jdkHome );
            cmdLine.add( jdk.getHome() + "/bin/java" ); // use JDK.getExecutable() here ?
        }
        else
        {
            cmdLine.add( "java" );
        }

        String starterPath =
            classPathEntry( slaveRoot, LoadGeneratorStarter.class, "jetty-load-generator-starter", taskListener );
        LOGGER.debug( "starterPath: {}", starterPath );

        cmdLine.add( "-jar" );

        cmdLine.add( starterPath );

        //cmdLine.add( "-pjp" ).add( tmpFilePath );
        cmdLine.add( "-h" ).add( host );
        cmdLine.add( "-p" ).add( port );

        if ( runIteration > 0 )
        {
            cmdLine.add( "-ri" ).add( runIteration );
        }
        else
        {
            cmdLine.add( "-rt" ).add( runningTime );
            cmdLine.add( "-rtu" );
            switch ( this.runningTimeUnit )
            {
                case HOURS:
                    cmdLine.add( "h" );
                    break;
                case MINUTES:
                    cmdLine.add( "m" );
                    break;
                case SECONDS:
                    cmdLine.add( "s" );
                    break;
                case MILLISECONDS:
                    cmdLine.add( "ms" );
                    break;
                default:
                    throw new IllegalArgumentException( runningTimeUnit + " is not recognized" );
            }
        }

        String[] cmds = cmdLine.toCommandArray();
        final Proc proc =  getCurrentNode().createLauncher( taskListener ).launch(  ).cmds( cmdLine ).start();
        // final Proc proc =  launcher.launch().cmds( cmds ).stdout( taskListener ).start();

        while ( proc.isAlive() )
        {
            Thread.sleep( 1 );
        }

        // deleting tmp file
        getCurrentNode().getChannel().call( new DeleteTmpFile(tmpFilePath) );
        LOGGER.debug( "finish" );




    }


    private static class CopyResourceProfile
        extends MasterToSlaveCallable<String, IOException>
    {
        private ResourceProfile resourceProfile;

        public CopyResourceProfile( ResourceProfile resourceProfile )
        {
            this.resourceProfile = resourceProfile;
        }

        @Override
        public String call()
            throws IOException
        {
            ObjectMapper objectMapper = new ObjectMapper();
            Path tmpPath = Files.createTempFile( "profile", ".tmp" );
            objectMapper.writeValue( tmpPath.toFile(), resourceProfile );
            return tmpPath.toString();
        }
    }

    private static class DeleteTmpFile
        extends MasterToSlaveCallable<Void, IOException>
    {
        private String filePath;

        public DeleteTmpFile( String filePath )
        {
            this.filePath = filePath;
        }

        @Override
        public Void call()
            throws IOException
        {
            Path path = Paths.get( filePath );
            if ( Files.exists( path ) )
            {
                Files.delete( path );
            }

            return null;
        }
    }

    protected void runEmbeddedLoadGenerator( TaskListener taskListener, FilePath workspace, Run<?, ?> run,
                                             ResourceProfile resourceProfile )
        throws Exception
    {

        ResponseTimePerPathListener responseTimePerPath = new ResponseTimePerPathListener( false );
        ResponseNumberPerPath responseNumberPerPath = new ResponseNumberPerPath();
        GlobalSummaryReportListener globalSummaryReportListener = new GlobalSummaryReportListener();
        // this one will use some memory for a long load test!!
        // FIXME find a way to flush that somewhere!!
        DetailledResponseTimeReportListener detailledResponseTimeReportListener =
            new DetailledResponseTimeReportListener();

        List<ResponseTimeListener> listeners = new ArrayList<>();
        if ( this.responseTimeListeners != null )
        {
            listeners.addAll( this.responseTimeListeners );
        }
        listeners.add( responseTimePerPath );
        listeners.add( responseNumberPerPath );
        listeners.add( globalSummaryReportListener );
        listeners.add( detailledResponseTimeReportListener );

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

        LoadGenerator.Builder builder = new LoadGenerator.Builder() //
            .host( getHost() ) //
            .port( getPort() ) //
            .users( getUsers() ) //
            .transactionRate( getTransactionRate() ) //
            .transport( getTransport() ) //
            .httpClientTransport( httpClientTransport() ) //
            .loadProfile( resourceProfile ) //
            .responseTimeListeners( listeners.toArray( new ResponseTimeListener[listeners.size()] ) ); //
        //.requestListeners( testRequestListener ) //
        //.executor( new QueuedThreadPool() )

        if ( secureProtocol )
        {
            // well that's an easy accept everything one...
            builder.sslContextFactory( new SslContextFactory( true ) );
        }

        LoadGenerator loadGenerator = builder.build();

        //ObjectMapper objectMapper = new ObjectMapper(  );

        //objectMapper.writeValue( new File( "text.out.json" ), loadGenerator );

        if ( runIteration > 0 )
        {
            taskListener.getLogger().println(
                "starting " + runIteration + " iterations, load generator to host " + host + " with port " + port );
            loadGenerator.run( runIteration );
            LOGGER.info( "host: {}, port: {}", getHost(), getPort() );
        }
        else
        {
            taskListener.getLogger().println( "starting for " + runningTime + " " + runningTimeUnit.toString()
                                                  + " iterations, load generator to host " + host + " with port "
                                                  + port );
            loadGenerator.run( runningTime, runningTimeUnit );
        }

        taskListener.getLogger().println( "load generator stopped, enjoy your results!!" );

        SummaryReport summaryReport = new SummaryReport();

        Map<String, CollectorInformations> perPath = new HashMap<>( responseTimePerPath.getRecorderPerPath().size() );

        for ( Map.Entry<String, Recorder> entry : responseTimePerPath.getRecorderPerPath().entrySet() )
        {
            String path = entry.getKey();
            Histogram histogram = entry.getValue().getIntervalHistogram();
            perPath.put( entry.getKey(), new CollectorInformations( histogram ) );
            AtomicInteger number = responseNumberPerPath.getResponseNumberPerPath().get( path );
            LOGGER.debug( "responseTimePerPath: {} - mean: {}ms - number: {}", //
                          path, //
                          TimeUnit.NANOSECONDS.toMillis( Math.round( histogram.getMean() ) ), //
                          number.get() );
            summaryReport.addCollectorInformations( path, new CollectorInformations( histogram ) );
        }

        // TODO calculate score from previous build
        HealthReport healthReport = new HealthReport( 30, "text" );

        Map<String, List<ResponseTimeInfo>> allResponseInfoTimePerPath = new HashMap<>();

        for ( DetailledResponseTimeReport.Entry entry : detailledResponseTimeReportListener.getDetailledResponseTimeReport().getEntries() )
        {
            List<ResponseTimeInfo> responseTimeInfos = allResponseInfoTimePerPath.get( entry.getPath() );
            if ( responseTimeInfos == null )
            {
                responseTimeInfos = new ArrayList<>();
                allResponseInfoTimePerPath.put( entry.getPath(), responseTimeInfos );
            }
            responseTimeInfos.add( new ResponseTimeInfo( entry.getTimeStamp(), //
                                                         TimeUnit.NANOSECONDS.toMillis( entry.getTime() ) ) );

        }

        run.addAction( new LoadGeneratorBuildAction( healthReport, //
                                                     summaryReport, //
                                                     new CollectorInformations(
                                                         globalSummaryReportListener.getHistogram() ), //
                                                     perPath, allResponseInfoTimePerPath, run ) );

        LOGGER.debug( "end" );

    }


    protected HttpClientTransport httpClientTransport()
    {
        switch ( getTransport() )
        {
            case HTTP:
            case HTTPS:
                return new HttpTransportBuilder().build();
            case H2:
            case H2C:
                return new Http2TransportBuilder().build();
            case FCGI:
                return new HttpFCGITransportBuilder().build();

        }
        throw new IllegalArgumentException( "unknown transport: " + getTransport() );
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

    @Extension
    @Symbol( "loadgenerator" )
    public static final class DescriptorImpl
        extends BuildStepDescriptor<Builder>
    {

        private static final List<TimeUnit> TIME_UNITS = Arrays.asList( TimeUnit.DAYS, //
                                                                        TimeUnit.HOURS, //
                                                                        TimeUnit.MINUTES, //
                                                                        TimeUnit.SECONDS, //
                                                                        TimeUnit.MILLISECONDS );

        private static final List<LoadGenerator.Transport> TRANSPORTS = Arrays.asList( LoadGenerator.Transport.HTTP, //
                                                                                       LoadGenerator.Transport.HTTPS, //
                                                                                       LoadGenerator.Transport.H2, //
                                                                                       LoadGenerator.Transport.H2C, //
                                                                                       LoadGenerator.Transport.FCGI );

        /**
         * This human readable name is used in the configuration screen.
         */
        public String getDisplayName()
        {
            return "HTTP LoadGenerator";
        }

        public List<TimeUnit> getTimeUnits()
        {
            return TIME_UNITS;
        }

        public List<JDK> getJdks()
        {
            return Jenkins.getInstance().getJDKs();
        }

        public List<LoadGenerator.Transport> getTransports()
        {
            return TRANSPORTS;
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


    protected final String classPathEntry( FilePath root, Class<?> representative, String seedName,
                                           TaskListener listener )
        throws IOException, InterruptedException
    {
        if ( root == null )
        { // master
            return Which.jarFile( representative ).getAbsolutePath();
        }
        else
        {
            return copyJar( listener.getLogger(), root, representative, seedName ).getRemote();
        }
    }

    /**
     * Copies a jar file from the master to slave.
     */
    static FilePath copyJar( PrintStream log, FilePath dst, Class<?> representative, String seedName )
        throws IOException, InterruptedException
    {
        // in normal execution environment, the master should be loading 'representative' from this jar, so
        // in that way we can find it.
        File jar = Which.jarFile( representative );
        FilePath copiedJar = dst.child( seedName + ".jar" );

        if ( jar.isDirectory() )
        {
            // but during the development and unit test environment, we may be picking the class up from the classes dir
            Zip zip = new Zip();
            zip.setBasedir( jar );
            File t = File.createTempFile( seedName, "jar" );
            t.delete();
            zip.setDestFile( t );
            zip.setProject( new Project() );
            zip.execute();
            jar = t;
        }
        else if ( copiedJar.exists() && copiedJar.digest().equals( Util.getDigestOf( jar ) ) )
        {
            log.println( seedName + ".jar already up to date" );
            return copiedJar;
        }

        // Theoretically could be a race condition on a multi-executor Windows slave; symptom would be an IOException during the build.
        // Could perhaps be solved by synchronizing on dst.getChannel() or similar.
        new FilePath( jar ).copyTo( copiedJar );
        log.println( "Copied " + seedName + ".jar" );
        return copiedJar;
    }

    protected Node getCurrentNode()
    {
        return Executor.currentExecutor().getOwner().getNode();
    }

}

