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
import com.lmax.disruptor.EventFactory;
import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.dsl.Disruptor;
import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Util;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Computer;
import hudson.model.HealthReport;
import hudson.model.JDK;
import hudson.model.Node;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.ArgumentListBuilder;
import hudson.util.ReflectionUtils;
import jenkins.model.Jenkins;
import jenkins.security.MasterToSlaveCallable;
import jenkins.tasks.SimpleBuildStep;
import org.HdrHistogram.Histogram;
import org.HdrHistogram.Recorder;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.commons.lang.text.StrSubstitutor;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.customizers.ImportCustomizer;
import org.eclipse.jetty.load.generator.CollectorInformations;
import org.eclipse.jetty.load.generator.LoadGenerator;
import org.eclipse.jetty.load.generator.ValueListener;
import org.eclipse.jetty.load.generator.profile.ResourceProfile;
import org.eclipse.jetty.load.generator.report.DetailledResponseTimeReport;
import org.eclipse.jetty.load.generator.report.DetailledResponseTimeReportListener;
import org.eclipse.jetty.load.generator.report.GlobalSummaryReportListener;
import org.eclipse.jetty.load.generator.report.SummaryReport;
import org.eclipse.jetty.load.generator.responsetime.ResponseNumberPerPath;
import org.eclipse.jetty.load.generator.responsetime.ResponseTimeListener;
import org.eclipse.jetty.load.generator.responsetime.ResponseTimePerPathListener;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 *
 */
public class LoadGeneratorBuilder
    extends Builder
    implements SimpleBuildStep
{

    private static final Logger LOGGER = LoggerFactory.getLogger( LoadGeneratorBuilder.class );

    private final String profileGroovy;

    private final String host;

    private final String port;

    private final int users;

    private final String profileFromFile;

    private final String runningTime;

    private final TimeUnit runningTimeUnit;

    private final String runIteration;

    private final int transactionRate;

    private List<ResponseTimeListener> responseTimeListeners = new ArrayList<>();

    private ResourceProfile loadProfile;

    private LoadGenerator.Transport transport;

    private boolean secureProtocol;

    private String jdkName;

    // Fields in config.jelly must match the parameter names in the "DataBoundConstructor"
    @DataBoundConstructor
    public LoadGeneratorBuilder( String profileGroovy, String host, String port, int users, String profileFromFile,
                                 String runningTime, TimeUnit runningTimeUnit, String runIteration, int transactionRate,
                                 LoadGenerator.Transport transport, boolean secureProtocol )
    {
        this.profileGroovy = Util.fixEmptyAndTrim( profileGroovy );
        this.host = host;
        this.port = port;
        this.users = users;
        this.profileFromFile = profileFromFile;
        this.runningTime = runningTime;
        this.runningTimeUnit = runningTimeUnit == null ? TimeUnit.SECONDS : runningTimeUnit;
        this.runIteration = runIteration;
        this.transactionRate = transactionRate == 0 ? 1 : transactionRate;
        this.transport = transport;
        this.secureProtocol = secureProtocol;
    }

    public LoadGeneratorBuilder( ResourceProfile resourceProfile, String host, String port, int users,
                                 String profileXmlFromFile, String runningTime, TimeUnit runningTimeUnit, String runIteration,
                                 int transactionRate, LoadGenerator.Transport transport, boolean secureProtocol )
    {
        this.profileGroovy = null;
        this.loadProfile = resourceProfile;
        this.host = host;
        this.port = port;
        this.users = users;
        this.profileFromFile = profileXmlFromFile;
        this.runningTime = runningTime;
        this.runningTimeUnit = runningTimeUnit == null ? TimeUnit.SECONDS : runningTimeUnit;
        this.runIteration = runIteration;
        this.transactionRate = transactionRate == 0 ? 1 : transactionRate;
        this.transport = transport;
        this.secureProtocol = secureProtocol;
    }

    public String getProfileGroovy()
    {
        return profileGroovy;
    }

    public String getHost()
    {
        return host;
    }

    public String getPort()
    {
        return port;
    }

    public int getUsers()
    {
        return users;
    }

    public String getProfileFromFile()
    {
        return profileFromFile;
    }

    public String getRunningTime()
    {
        return runningTime;
    }

    public TimeUnit getRunningTimeUnit()
    {
        return runningTimeUnit;
    }

    public String getRunIteration()
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

        runProcess( taskListener, workspace, run, launcher, resourceProfile );

    }

    /**
     * Expand tokens with token macro and build variables
     */
    protected static String expandTokens( TaskListener listener, String str, Run<?,?> run )
        throws Exception
    {
        if ( str == null )
        {
            return null;
        }
        try
        {
            Class<?> clazz = Class.forName( "org.jenkinsci.plugins.tokenmacro.TokenMacro" );
            Method expandMethod = ReflectionUtils.findMethod( clazz, "expand",
                                                              new Class[]{
                                                                  AbstractBuild.class, //
                                                                  TaskListener.class, //
                                                                  String.class } );
            return (String) expandMethod.invoke( null, run, listener, str );
            //opts = TokenMacro.expand(this, listener, opts);
        }
        catch ( Exception tokenException )
        {
            //Token plugin not present. Ignore, this is OK.
            LOGGER.trace( "Ignore problem in expanding tokens", tokenException );
        }
        catch ( LinkageError linkageError )
        {
            // Token plugin not present. Ignore, this is OK.
            LOGGER.trace( "Ignore problem in expanding tokens", linkageError );
        }


        str = StrSubstitutor.replace( str, run.getEnvironment(listener) );

        return str;
    }


    protected void runProcess( TaskListener taskListener, FilePath workspace, Run<?, ?> run, Launcher launcher,
                               ResourceProfile resourceProfile )
        throws Exception
    {

        List<ResponseTimeListener> listeners = new ArrayList<>();


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

        Path resultFilePath = Paths.get( launcher //
                                             .getChannel() //
                                             .call( new LoadGeneratorProcessFactory.RemoteTmpFileCreate()) );

        ResponseTimeFileWriter responseTimeFileWriter = new ResponseTimeFileWriter( resultFilePath );
        listeners.add( responseTimeFileWriter );

        List<String> args = getArgsProcess( resourceProfile, launcher.getComputer(), taskListener, run );

        new LoadGeneratorProcessRunner().runProcess( taskListener, workspace, launcher, //
                                                     this.jdkName, getCurrentNode(launcher.getComputer()), //
                                                     listeners, args );

        // handle reports

        ResponseTimePerPathListener responseTimePerPath = new ResponseTimePerPathListener( false );
        ResponseNumberPerPath responseNumberPerPath = new ResponseNumberPerPath();
        GlobalSummaryReportListener globalSummaryReportListener = new GlobalSummaryReportListener();
        // this one will use some memory for a long load test!!
        // FIXME find a way to flush that somewhere!!
        DetailledResponseTimeReportListener detailledResponseTimeReportListener =
            new DetailledResponseTimeReportListener();

        listeners.clear();
        if ( this.responseTimeListeners != null )
        {
            listeners.addAll( this.responseTimeListeners );
        }
        listeners.add( responseNumberPerPath );
        listeners.add( responseTimePerPath );
        listeners.add( globalSummaryReportListener );
        listeners.add( detailledResponseTimeReportListener );

        // get remote file

        LOGGER.info( "LoadGenerator parsing result file" );

        Path localResultFile = Files.createTempFile( "loadgenerator_result", ".csv" );

        workspace.child( resultFilePath.toString() ).copyTo( Files.newOutputStream( localResultFile ) );

        CSVParser csvParser = new CSVParser( Files.newBufferedReader( localResultFile ), CSVFormat.newFormat( '|' ) );

        csvParser.forEach(
            strings ->
            {
                ValueListener.Values values = new ValueListener.Values() //
                    .eventTimestamp( Long.parseLong( strings.get( 0 ) )) //
                    .method( strings.get( 1 ) ) //
                    .path( strings.get( 2 ) ) //
                    .time( Long.parseLong( strings.get( 3 ) ) ) //
                    .status( Integer.parseInt( strings.get( 4 ) ) ) //
                    .size( Long.parseLong( strings.get( 5 ) ) );
                for (ResponseTimeListener listener : listeners)
                {
                    listener.onResponseTimeValue( values );
                }
            } );

        //FilePath projectWorkspaceOnSlave = build.getProject().getWorkspace();

        // manage results

        SummaryReport summaryReport = new SummaryReport(run.getId());

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

        // FIXME calculate score from previous build
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

        // cleanup

        getCurrentNode(launcher.getComputer()) //
            .getChannel() //
            .call( new LoadGeneratorProcessFactory.DeleteTmpFile( resultFilePath.toString() ) );
        Files.deleteIfExists( localResultFile );

        LOGGER.info( "LoadGenerator end" );
    }


    protected List<String> getArgsProcess( ResourceProfile resourceProfile, Computer computer, TaskListener taskListener, Run<?,?> run)
        throws Exception
    {

        final String tmpFilePath = getCurrentNode(computer).getChannel().call( new CopyResourceProfile( resourceProfile ) );

        ArgumentListBuilder cmdLine = new ArgumentListBuilder();

        cmdLine.add( "-pjp" ).add( tmpFilePath );
        cmdLine.add( "-h" ).add( expandTokens( taskListener, host, run ) );
        cmdLine.add( "-p" ).add( expandTokens( taskListener, port, run ) );

        if ( NumberUtils.isNumber( runIteration ) && Integer.parseInt( runIteration ) > 0 )
        {
            cmdLine.add( "-ri" ).add( expandTokens( taskListener, runIteration, run) );
        }
        else
        {
            cmdLine.add( "-rt" ).add( expandTokens( taskListener, runningTime, run ) );
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

        // FIXME deleting tmp file
        // getCurrentNode().getChannel().call( new DeleteTmpFile( tmpFilePath ) );
        LOGGER.debug( "finish" );
        return cmdLine.toList();

    }


    static class CopyResourceProfile
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


    protected ResourceProfile loadResourceProfile( FilePath workspace )
        throws Exception
    {

        ResourceProfile resourceProfile = null;

        String groovy = StringUtils.trim( this.getProfileGroovy() );

        String profileFromPath = getProfileFromFile();

        if (StringUtils.isBlank( groovy ) && StringUtils.isNotBlank( profileFromPath ))
        {
            FilePath profileGroovyFilePath = workspace.child( profileFromPath );
            groovy = IOUtils.toString( profileGroovyFilePath.read() );
        }

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

        return resourceProfile;
    }




    protected Node getCurrentNode( Computer computer )
    {
        Node node = null;
        // well avoid NPE when running workflow testing
        //return Executor.currentExecutor().getOwner().getNode();
        if (Computer.currentComputer() != null)
        {
            node = Computer.currentComputer().getNode();
        }
        if (node == null) {
            node = computer.getNode();
        }
        return node;
    }


    static class ResponseTimeFileWriter
        implements ResponseTimeListener, Serializable, EventHandler<ValueListener.Values>,
        EventFactory<ValueListener.Values>
    {

        private final String filePath;

        private transient BufferedWriter bufferedWriter;

        private transient RingBuffer<Values> ringBuffer;

        public ResponseTimeFileWriter( Path path )
        {
            try
            {
                this.filePath = path.toAbsolutePath().toString();
                this.bufferedWriter = Files.newBufferedWriter( path );
            }
            catch ( IOException e )
            {
                throw new RuntimeException( e.getMessage(), e );
            }

        }

        @Override
        public void onResponseTimeValue( Values values )
        {
            this.ringBuffer.publishEvent( ( event, sequence ) -> event.eventTimestamp( values.getEventTimestamp() ) //
                .method( values.getMethod() ) //
                .path( values.getPath() ) //
                .time( values.getTime() ) //
                .status( values.getStatus() ) //
                .size( values.getSize() ) );
        }

        public Object readResolve()
        {
            try
            {
                this.bufferedWriter = Files.newBufferedWriter( Paths.get( this.filePath ) );

                // Executor that will be used to construct new threads for consumers
                ExecutorService executor = Executors.newCachedThreadPool();

                // Specify the size of the ring buffer, must be power of 2.
                int bufferSize = 1024;

                // Construct the Disruptor
                Disruptor<Values> disruptor = new Disruptor<>( this, bufferSize, executor );

                // Connect the handler
                disruptor.handleEventsWith( this );

                // Start the Disruptor, starts all threads running
                disruptor.start();

                this.ringBuffer = disruptor.getRingBuffer();

            }
            catch ( Exception e )
            {
                throw new RuntimeException( e.getMessage(), e );
            }
            return this;
        }

        @Override
        public void onEvent( Values values, long l, boolean b )
            throws Exception
        {
            try
            {
                StringBuilder sb = new StringBuilder( 128 ) //
                    .append( values.getEventTimestamp() ).append( '|' ) //
                    .append( values.getMethod() ).append( '|' ) //
                    .append( values.getPath() ).append( '|' ) //
                    .append( values.getTime() ).append( '|' ) //
                    .append( values.getStatus() ).append( '|' ) //
                    .append( values.getSize() );

                this.bufferedWriter.write( sb.toString() );
                this.bufferedWriter.newLine();//. write( sb.toString() + System.lineSeparator() );
            }
            catch ( IOException e )
            {
                e.printStackTrace();
            }
        }

        @Override
        public Values newInstance()
        {
            return new Values();
        }


        @Override
        public void onLoadGeneratorStop()
        {
            try
            {
                this.bufferedWriter.flush();
                this.bufferedWriter.close();
            }
            catch ( IOException e )
            {
                throw new RuntimeException( e.getMessage(), e );
            }
            System.out.println( "stop loadGenerator" );
        }

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

