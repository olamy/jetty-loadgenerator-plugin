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
import org.apache.commons.lang.text.StrSubstitutor;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.customizers.ImportCustomizer;
import org.eclipse.jetty.client.HttpClient;
import org.mortbay.jetty.load.generator.CollectorInformations;
import org.mortbay.jetty.load.generator.LoadGenerator;
import org.mortbay.jetty.load.generator.ValueListener;
import org.mortbay.jetty.load.generator.latency.LatencyTimeListener;
import org.mortbay.jetty.load.generator.profile.ResourceProfile;
import org.mortbay.jetty.load.generator.report.DetailledTimeReportListener;
import org.mortbay.jetty.load.generator.report.DetailledTimeValuesReport;
import org.mortbay.jetty.load.generator.report.GlobalSummaryListener;
import org.mortbay.jetty.load.generator.report.SummaryReport;
import org.mortbay.jetty.load.generator.responsetime.ResponseNumberPerPath;
import org.mortbay.jetty.load.generator.responsetime.ResponsePerStatus;
import org.mortbay.jetty.load.generator.responsetime.ResponseTimeListener;
import org.mortbay.jetty.load.generator.responsetime.TimePerPathListener;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.lang.reflect.Method;
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

    private static final Logger LOGGER = LoggerFactory.getLogger( LoadGeneratorBuilder.class );

    private final String profileGroovy;

    private final String host;

    private final String port;

    private final String users;

    private final String profileFromFile;

    private final String runningTime;

    private final TimeUnit runningTimeUnit;

    private final String runIteration;

    private final String transactionRate;

    private List<ResponseTimeListener> responseTimeListeners = new ArrayList<>();

    private ResourceProfile loadProfile;

    private LoadGenerator.Transport transport;

    private boolean secureProtocol;

    private String jdkName;

    private String jvmExtraArgs;

    private String generatorNumber = "1";

    private String dryRun = "0";

    // Fields in config.jelly must match the parameter names in the "DataBoundConstructor"
    @DataBoundConstructor
    public LoadGeneratorBuilder( String profileGroovy, String host, String port, String users, String profileFromFile,
                                 String runningTime, TimeUnit runningTimeUnit, String runIteration, String transactionRate,
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
        this.transactionRate = StringUtils.isEmpty( transactionRate ) ? "1" : transactionRate;
        this.transport = transport;
        this.secureProtocol = secureProtocol;
    }

    public LoadGeneratorBuilder( ResourceProfile resourceProfile, String host, String port, String users,
                                 String profileFromFile, String runningTime, TimeUnit runningTimeUnit, String runIteration,
                                 String transactionRate, LoadGenerator.Transport transport, boolean secureProtocol, String jvmExtraArgs,
                                 String generatorNumber)
    {

        this( null, host, port, users, profileFromFile, runningTime, runningTimeUnit, runIteration, transactionRate,
              transport, secureProtocol );
        this.loadProfile = resourceProfile;
        this.jvmExtraArgs = jvmExtraArgs;
        this.generatorNumber = generatorNumber;
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

    public String getUsers()
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

    public String getTransactionRate()
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

    public String getJvmExtraArgs()
    {
        return jvmExtraArgs;
    }

    @DataBoundSetter
    public void setJvmExtraArgs( String jvmExtraArgs )
    {
        this.jvmExtraArgs = jvmExtraArgs;
    }


    public String getGeneratorNumber()
    {
        return generatorNumber;
    }

    @DataBoundSetter
    public void setGeneratorNumber( String generatorNumber )
    {
        this.generatorNumber = generatorNumber;
    }

    public String getDryRun()
    {
        return dryRun;
    }

    @DataBoundSetter
    public void setDryRun( String dryRun )
    {
        this.dryRun = dryRun;
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


        // -------------------------
        // listeners to get data files
        // -------------------------
        List<ResponseTimeListener> responseTimeListeners = new ArrayList<>();

        Path responseTimeResultFilePath = Paths.get( launcher.getChannel() //
                                             .call( new LoadGeneratorProcessFactory.RemoteTmpFileCreate()) );

        responseTimeListeners.add( new ValuesFileWriter( responseTimeResultFilePath ) );

        List<LatencyTimeListener> latencyTimeListeners = new ArrayList<>();

        Path latencyTimeResultFilePath = Paths.get( launcher.getChannel() //
                                                         .call( new LoadGeneratorProcessFactory.RemoteTmpFileCreate()) );

        latencyTimeListeners.add( new ValuesFileWriter( latencyTimeResultFilePath ) );

        Path statsResultFilePath = Paths.get( launcher.getChannel() //
                                                   .call( new LoadGeneratorProcessFactory.RemoteTmpFileCreate()) );

        List<String> args = getArgsProcess( resourceProfile, launcher.getComputer(), taskListener, //
                                            run, statsResultFilePath.toString()  );

        String monitorUrl = getMonitorUrl( taskListener, run );

        int dryRun = StringUtils.isNotEmpty( getDryRun() ) ? //
            Integer.parseInt( expandTokens( taskListener, this.getDryRun(), run ) ) : -1;

        new LoadGeneratorProcessRunner().runProcess( taskListener, workspace, launcher, //
                                                     this.jdkName, getCurrentNode(launcher.getComputer()), //
                                                     responseTimeListeners, latencyTimeListeners, args, getJvmExtraArgs(), //
                                                     dryRun, monitorUrl);

        String stats = workspace.child( statsResultFilePath.toString() ).readToString();


        TimePerPathListener timePerPathListener = new TimePerPathListener( false );
        GlobalSummaryListener globalSummaryListener = new GlobalSummaryListener();
        // this one will use some memory for a long load test!!
        // FIXME find a way to flush that somewhere!!
        DetailledTimeReportListener detailledTimeReportListener = new DetailledTimeReportListener();

        // -----------------------------
        // handle response time reports
        // -----------------------------

        ResponsePerStatus responsePerStatus = new ResponsePerStatus();

        ResponseNumberPerPath responseNumberPerPath = new ResponseNumberPerPath();

        responseTimeListeners.clear();
        if ( this.responseTimeListeners != null )
        {
            responseTimeListeners.addAll( this.responseTimeListeners );
        }
        responseTimeListeners.add( responseNumberPerPath );
        responseTimeListeners.add( timePerPathListener );
        responseTimeListeners.add( globalSummaryListener );
        responseTimeListeners.add( detailledTimeReportListener );
        responseTimeListeners.add( responsePerStatus );


        latencyTimeListeners.clear();
        latencyTimeListeners.add( timePerPathListener );
        latencyTimeListeners.add( globalSummaryListener );
        latencyTimeListeners.add( detailledTimeReportListener );

        LOGGER.info( "LoadGenerator parsing response result files" );

        //-------------------------------------------------
        // response time values
        //-------------------------------------------------
        parseResponseTimeValues(workspace, responseTimeResultFilePath, responseTimeListeners);

        //-------------------------------------------------
        // latency time values
        //-------------------------------------------------
        parseLatencyValues(workspace, latencyTimeResultFilePath, latencyTimeListeners);

        //-------------------------------------------------
        // Monitor values
        //-------------------------------------------------
        String monitorJson = getMonitorValues(monitorUrl, taskListener);

        taskListener.getLogger().print( "monitorJson: " + monitorJson );

        Map<String, Object> monitoringResultMap = null;

        try
        {
            monitoringResultMap = new ObjectMapper().readValue( monitorJson, Map.class );
        }
        catch ( Exception e )
        {
            LOGGER.warn( "skip error parsing json monitoring result" );
        }
        // manage results

        SummaryReport summaryReport = new SummaryReport(run.getId());


        for ( Map.Entry<String, Recorder> entry : timePerPathListener.getResponseTimePerPath().entrySet() )
        {
            String path = entry.getKey();
            Histogram histogram = entry.getValue().getIntervalHistogram();

            AtomicInteger number = responseNumberPerPath.getResponseNumberPerPath().get( path );
            LOGGER.debug( "responseTimePerPath: {} - mean: {}ms - number: {}", //
                          path, //
                          TimeUnit.NANOSECONDS.toMillis( Math.round( histogram.getMean() ) ), //
                          number.get() );
            summaryReport.addResponseTimeInformations( path, new CollectorInformations( histogram ) );
        }



        for ( Map.Entry<String, Recorder> entry : timePerPathListener.getLatencyTimePerPath().entrySet() )
        {
            String path = entry.getKey();
            Histogram histogram = entry.getValue().getIntervalHistogram();

            AtomicInteger number = responseNumberPerPath.getResponseNumberPerPath().get( path );
            LOGGER.debug( "responseTimePerPath: {} - mean: {}ms - number: {}", //
                          path, //
                          TimeUnit.NANOSECONDS.toMillis( Math.round( histogram.getMean() ) ), //
                          number.get() );
            summaryReport.addLatencyTimeInformations( path, new CollectorInformations( histogram ) );
        }


        // FIXME calculate score from previous build
        HealthReport healthReport = new HealthReport( 30, "text" );

        Map<String, List<ResponseTimeInfo>> allResponseInfoTimePerPath = new HashMap<>();

        for ( DetailledTimeValuesReport.Entry entry : detailledTimeReportListener.getDetailledResponseTimeValuesReport().getEntries() )
        {
            List<ResponseTimeInfo> responseTimeInfos = allResponseInfoTimePerPath.get( entry.getPath() );
            if ( responseTimeInfos == null )
            {
                responseTimeInfos = new ArrayList<>();
                allResponseInfoTimePerPath.put( entry.getPath(), responseTimeInfos );
            }
            responseTimeInfos.add( new ResponseTimeInfo( entry.getTimeStamp(), //
                                                         TimeUnit.NANOSECONDS.toMillis( entry.getTime() ), //
                                                         entry.getHttpStatus() ) );

        }

        run.addAction( new LoadGeneratorBuildAction( healthReport, //
                                                     summaryReport, //
                                                     new CollectorInformations(
                                                         globalSummaryListener.getResponseTimeHistogram() ), //
                                                     new CollectorInformations(
                                                         globalSummaryListener.getLatencyTimeHistogram() ), //
                                                     allResponseInfoTimePerPath, run, monitoringResultMap, stats ) );

        // cleanup

        getCurrentNode(launcher.getComputer()) //
            .getChannel() //
            .call( new LoadGeneratorProcessFactory.DeleteTmpFile( responseTimeResultFilePath.toString() ) );



        LOGGER.info( "LoadGenerator end" );
    }

    protected void parseResponseTimeValues( FilePath workspace, Path responseTimeResultFilePath,
                                       List<ResponseTimeListener> responseTimeListeners )
        throws Exception
    {
        Path responseTimeResultFile = Files.createTempFile( "loadgenerator_result_responsetime", ".csv" );

        workspace.child( responseTimeResultFilePath.toString() ).copyTo( Files.newOutputStream( responseTimeResultFile ) );

        CSVParser csvParser = new CSVParser( Files.newBufferedReader( responseTimeResultFile ), CSVFormat.newFormat( '|' ) );

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
                for (ResponseTimeListener listener : responseTimeListeners)
                {
                    listener.onResponseTimeValue( values );
                }
            } );

        Files.deleteIfExists( responseTimeResultFile );
    }

    protected void parseLatencyValues( FilePath workspace, Path latencyTimeResultFilePath,
                                       List<LatencyTimeListener> latencyTimeListeners )
        throws Exception
    {

        Path latencyTimeResultFile = Files.createTempFile( "loadgenerator_result_latency", ".csv" );

        workspace.child( latencyTimeResultFilePath.toString() ).copyTo(
            Files.newOutputStream( latencyTimeResultFile ) );

        CSVParser csvParser =
            new CSVParser( Files.newBufferedReader( latencyTimeResultFile ), CSVFormat.newFormat( '|' ) );

        csvParser.forEach( strings ->
                           {
                               try
                               {
                                   ValueListener.Values values = new ValueListener.Values() //
                                       .eventTimestamp( Long.parseLong( strings.get( 0 ) ) ) //
                                       .method( strings.get( 1 ) ) //
                                       .path( strings.get( 2 ) ) //
                                       .time( Long.parseLong( strings.get( 3 ) ) ) //
                                       .status( Integer.parseInt( strings.get( 4 ) ) ) //
                                       .size( Long.parseLong( strings.get( 5 ) ) );
                                   for ( LatencyTimeListener listener : latencyTimeListeners )
                                   {
                                       listener.onLatencyTimeValue( values );
                                   }
                               }
                               catch ( Exception e )
                               {
                                   e.printStackTrace();
                               }
                           } );

        Files.deleteIfExists( latencyTimeResultFile );
    }

    protected List<String> getArgsProcess( ResourceProfile resourceProfile, Computer computer,
                                           TaskListener taskListener, Run<?,?> run, String statsResultFilePath)
        throws Exception
    {

        final String tmpFilePath = getCurrentNode(computer).getChannel().call( new CopyResourceProfile( resourceProfile ) );

        ArgumentListBuilder cmdLine = new ArgumentListBuilder();

        cmdLine.add( "-pjp" ).add( tmpFilePath );
        cmdLine.add( "-h" ).add( expandTokens( taskListener, host, run ) );
        cmdLine.add( "-p" ).add( expandTokens( taskListener, port, run ) );
        cmdLine.add( "--transport" ).add( StringUtils.lowerCase( this.getTransport().toString() ) );
        cmdLine.add( "-u" ).add( expandTokens( taskListener, users, run ) );
        cmdLine.add( "-tr" ).add( expandTokens( taskListener, transactionRate, run ) );
        cmdLine.add( "-stf" ).add( statsResultFilePath );

        if ( StringUtils.isNotBlank( runIteration ) )
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

    protected String getMonitorUrl( TaskListener taskListener, Run run )
        throws Exception
    {
        String url = ( this.isSecureProtocol() ? "https" : "http" ) //
            + "://" + expandTokens( taskListener, this.host, run );

        if ( StringUtils.isNotEmpty( this.port ) )
        {
            url = url //
                + ":" + expandTokens( taskListener, this.port, run ) //
                + "/monitor";
        }
        return url;
    }

    protected String getMonitorValues( String monitorUrl, TaskListener taskListener )
        throws Exception
    {
        HttpClient httpClient = new HttpClient();
        try
        {
            httpClient.start();
            return httpClient.newRequest( monitorUrl + "?stats=true" ).send().getContentAsString();
        }
        catch ( Exception e )
        {
            taskListener.getLogger().println( "error calling stats monitorUrl:" + monitorUrl + "," + e.getMessage());
            e.printStackTrace();
            return "";
        }
        finally
        {
            httpClient.stop();
        }
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

