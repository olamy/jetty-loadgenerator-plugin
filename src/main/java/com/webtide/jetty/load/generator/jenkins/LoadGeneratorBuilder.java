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

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
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
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.text.StrSubstitutor;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.customizers.ImportCustomizer;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.mortbay.jetty.load.generator.LoadGenerator;
import org.mortbay.jetty.load.generator.Resource;
import org.mortbay.jetty.load.generator.listeners.CollectorInformations;
import org.mortbay.jetty.load.generator.listeners.report.DetailledTimeReportListener;
import org.mortbay.jetty.load.generator.listeners.report.GlobalSummaryListener;
import org.mortbay.jetty.load.generator.listeners.report.SummaryReport;
import org.mortbay.jetty.load.generator.listeners.responsetime.ResponseNumberPerPath;
import org.mortbay.jetty.load.generator.listeners.responsetime.ResponsePerStatus;
import org.mortbay.jetty.load.generator.listeners.responsetime.TimePerPathListener;
import org.mortbay.jetty.load.generator.starter.LoadGeneratorStarterArgs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.Serializable;
import java.io.StringWriter;
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

    private List<Resource.NodeListener> nodeListeners = new ArrayList<>();

    private Resource loadResource;

    private LoadGeneratorStarterArgs.Transport transport;

    private boolean secureProtocol;

    private String jdkName;

    private String jvmExtraArgs;

    private String generatorNumber = "1";

    private String warmupNumber = "0";

    // Fields in config.jelly must match the parameter names in the "DataBoundConstructor"
    @DataBoundConstructor
    public LoadGeneratorBuilder( String profileGroovy, String host, String port, String users, String profileFromFile,
                                 String runningTime, TimeUnit runningTimeUnit, String runIteration, String transactionRate,
                                 LoadGeneratorStarterArgs.Transport transport, boolean secureProtocol )
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

    public LoadGeneratorBuilder( Resource resource, String host, String port, String users, //
                                 String profileFromFile, String runningTime, TimeUnit runningTimeUnit, String runIteration, //
                                 String transactionRate, LoadGeneratorStarterArgs.Transport transport, boolean secureProtocol, //
                                 String jvmExtraArgs, String generatorNumber)
    {

        this( null, host, port, users, profileFromFile, runningTime, runningTimeUnit, runIteration, transactionRate,
              transport, secureProtocol );
        this.loadResource = resource;
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

    public void addNodeListener( Resource.NodeListener nodeListener )
    {
        this.nodeListeners.add( nodeListener );
    }

    public String getTransactionRate()
    {
        return transactionRate;
    }

    public Resource getLoadResource()
    {
        return loadResource;
    }

    public void setLoadResource( Resource loadResource )
    {
        this.loadResource = loadResource;
    }

    public LoadGeneratorStarterArgs.Transport getTransport()
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

    public String getWarmupNumber()
    {
        return warmupNumber;
    }

    @DataBoundSetter
    public void setWarmupNumber( String warmupNumber )
    {
        this.warmupNumber = warmupNumber;
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

        Resource resource =
            this.loadResource == null ? loadResource( workspace ) : this.loadResource;

        if ( resource == null )
        {
            taskListener.getLogger().println( "resource profile must be set, Build ABORTED" );
            LOGGER.error( "resource profile must be set, Build ABORTED" );
            run.setResult( Result.ABORTED );
            return;
        }

        runProcess( taskListener, workspace, run, launcher, resource );

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
                               Resource resource )
        throws Exception
    {


        // -------------------------
        // listeners to get data files
        // -------------------------
        List<Resource.NodeListener> nodeListeners = new ArrayList<>();

        Path resultFilePath = Paths.get( launcher.getChannel() //
                                             .call( new LoadGeneratorProcessFactory.RemoteTmpFileCreate()) );

        ValuesFileWriter valuesFileWriter = new ValuesFileWriter( resultFilePath );
        nodeListeners.add( valuesFileWriter );

        List<LoadGenerator.Listener> loadGeneratorListeners = new ArrayList<>(  );
        loadGeneratorListeners.add( valuesFileWriter );

        Path statsResultFilePath = Paths.get( launcher.getChannel() //
                                                   .call( new LoadGeneratorProcessFactory.RemoteTmpFileCreate()) );

        List<String> args = getArgsProcess( resource, launcher.getComputer(), taskListener, //
                                            run, statsResultFilePath.toString()  );

        String monitorUrl = getMonitorUrl( taskListener, run );

        new LoadGeneratorProcessRunner().runProcess( taskListener, workspace, launcher, //
                                                     this.jdkName, getCurrentNode(launcher.getComputer()), //
                                                     nodeListeners, loadGeneratorListeners, //
                                                     args, getJvmExtraArgs(), //
                                                     getAl);

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

        nodeListeners.clear();
        if ( this.nodeListeners != null )
        {
            nodeListeners.addAll( this.nodeListeners );
        }
        nodeListeners.add( responseNumberPerPath );
        nodeListeners.add( timePerPathListener );
        nodeListeners.add( globalSummaryListener );
        nodeListeners.add( detailledTimeReportListener );
        nodeListeners.add( responsePerStatus );

        LOGGER.info( "LoadGenerator parsing response result files" );

        //-------------------------------------------------
        // time values
        //-------------------------------------------------
        parseTimeValues( workspace, resultFilePath, nodeListeners);


        //-------------------------------------------------
        // Monitor values
        //-------------------------------------------------
        String monitorJson = getMonitorValues(monitorUrl, taskListener);

        taskListener.getLogger().print( "monitorJson: " + monitorJson );

        Map<String, Object> monitoringResultMap = null;

        try
        {
            monitoringResultMap = new ObjectMapper() //
                .disable( DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES ) //
                .readValue( monitorJson, Map.class );
        }
        catch ( Exception e )
        {
            LOGGER.warn( "skip error parsing json monitoring result" );
        }
        // manage results

        SummaryReport summaryReport = new SummaryReport(run.getId());

        timePerPathListener.getResponseTimePerPath().entrySet().stream().forEach( entry ->  {
            String path = entry.getKey();
            Histogram histogram = entry.getValue();

            AtomicInteger number = responseNumberPerPath.getResponseNumberPerPath().get( path );
            LOGGER.debug( "responseTimePerPath: {} - mean: {}ms - number: {}", //
                          path, //
                          TimeUnit.NANOSECONDS.toMillis( Math.round( histogram.getMean() ) ), //
                          number.get() );
            summaryReport.addResponseTimeInformations( path, new CollectorInformations( histogram, //
                                                                                        TimeUnit.NANOSECONDS, TimeUnit.MILLISECONDS ) );
        } );

        timePerPathListener.getLatencyTimePerPath().entrySet().stream().forEach( entry -> {
            String path = entry.getKey();
            Histogram histogram = entry.getValue();

            AtomicInteger number = responseNumberPerPath.getResponseNumberPerPath().get( path );
            LOGGER.debug( "responseTimePerPath: {} - mean: {}ms - number: {}", //
                          path, //
                          TimeUnit.NANOSECONDS.toMillis( Math.round( histogram.getMean() ) ), //
                          number.get() );
            summaryReport.addLatencyTimeInformations( path, new CollectorInformations( histogram, //
                                                                                       TimeUnit.NANOSECONDS, TimeUnit.MILLISECONDS ) );
        } );

        // FIXME calculate score from previous build
        HealthReport healthReport = new HealthReport( 30, "text" );

        Map<String, List<ResponseTimeInfo>> allResponseInfoTimePerPath = new HashMap<>();

        detailledTimeReportListener.getDetailledLatencyTimeValuesReport().getEntries().stream().forEach( entry -> {
            List<ResponseTimeInfo> responseTimeInfos = allResponseInfoTimePerPath.get( entry.getPath() );
            if ( responseTimeInfos == null )
            {
                responseTimeInfos = new ArrayList<>();
                allResponseInfoTimePerPath.put( entry.getPath(), responseTimeInfos );
            }
            responseTimeInfos.add( new ResponseTimeInfo( entry.getTimeStamp(), //
                                                         TimeUnit.NANOSECONDS.toMillis( entry.getTime() ), //
                                                         entry.getHttpStatus() ) );
        } );


        run.addAction( new LoadGeneratorBuildAction( healthReport, //
                                                     summaryReport, //
                                                     new CollectorInformations(
                                                         globalSummaryListener.getResponseTimeHistogram().getIntervalHistogram(), //
                                                         TimeUnit.NANOSECONDS, TimeUnit.MILLISECONDS ), //
                                                     new CollectorInformations(
                                                         globalSummaryListener.getLatencyTimeHistogram().getIntervalHistogram(), //
                                                         TimeUnit.NANOSECONDS, TimeUnit.MILLISECONDS ), //
                                                     allResponseInfoTimePerPath, run, monitoringResultMap, stats ) );

        // cleanup

        getCurrentNode(launcher.getComputer()) //
            .getChannel() //
            .call( new LoadGeneratorProcessFactory.DeleteTmpFile( resultFilePath.toString() ) );



        LOGGER.info( "LoadGenerator end" );
    }

    protected void parseTimeValues( FilePath workspace, Path responseTimeResultFilePath,
                                    List<Resource.NodeListener> nodeListeners )
        throws Exception
    {
        Path responseTimeResultFile = Files.createTempFile( "loadgenerator_result_responsetime", ".csv" );

        workspace.child( responseTimeResultFilePath.toString() ).copyTo( Files.newOutputStream( responseTimeResultFile ) );

        CSVParser csvParser = new CSVParser( Files.newBufferedReader( responseTimeResultFile ), CSVFormat.newFormat( '|' ) );

        csvParser.forEach(
            strings ->
            {
                Values values = new Values() //
                    .eventTimestamp( Long.parseLong( strings.get( 0 ) )) //
                    .method( strings.get( 1 ) ) //
                    .path( strings.get( 2 ) ) //
                    .status( Integer.parseInt( strings.get( 3 ) ) ) //
                    .size( Long.parseLong( strings.get( 4 ) ) ) //
                    .responseTime( Long.parseLong( strings.get( 5 ) ) ) //
                    .latencyTime( Long.parseLong( strings.get( 6 ) ) );

                for (Resource.NodeListener listener : nodeListeners)
                {
                    listener.onResourceNode( values.getInfo() );
                }
            } );

        Files.deleteIfExists( responseTimeResultFile );
    }

    protected List<String> getArgsProcess( Resource resource, Computer computer,
                                           TaskListener taskListener, Run<?,?> run, String statsResultFilePath)
        throws Exception
    {

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure( SerializationFeature.FAIL_ON_EMPTY_BEANS, false );
        StringWriter stringWriter = new StringWriter(  );
        objectMapper.writeValue( stringWriter, resource );
        stringWriter.close();

        final String tmpFilePath = getCurrentNode(computer) //
            .getChannel().call( new CopyResource( stringWriter.toString() ) );

        ArgumentListBuilder cmdLine = new ArgumentListBuilder();

        cmdLine.add( "-pjp" ).add( tmpFilePath ) //
            .add( "-h" ).add( expandTokens( taskListener, host, run ) ) //
            .add( "-p" ).add( expandTokens( taskListener, port, run ) ) //
            .add( "--transport" ).add( StringUtils.lowerCase( this.getTransport().toString() ) ) //
            .add( "-u" ).add( expandTokens( taskListener, users, run ) ) //
            .add( "-tr" ).add( expandTokens( taskListener, transactionRate, run ) ) //
            .add( "-stf" ).add( statsResultFilePath ) //
            .add( "-css" ) //
            .add( "--scheme" ).add( isSecureProtocol()? "https" : "http" );

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

        int warmupNumber = StringUtils.isNotEmpty( getWarmupNumber() ) ? //
            Integer.parseInt( expandTokens( taskListener, this.getWarmupNumber(), run ) ) : -1;

        if (warmupNumber > 0)
        {
            cmdLine.add( "-wn" ).add( warmupNumber );
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
            ContentResponse contentResponse = httpClient.newRequest( monitorUrl + "?stats=true" ).send();
            return contentResponse == null ? "" : contentResponse.getContentAsString();
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


    static class CopyResource
        extends MasterToSlaveCallable<String, IOException>
        implements Serializable
    {
        private String resourceAsJson;

        public CopyResource( String resourceAsJson )
        {
            this.resourceAsJson = resourceAsJson;
        }

        @Override
        public String call()
            throws IOException
        {
            Path tmpPath = Files.createTempFile( "profile", ".tmp" );
            Files.write( tmpPath, resourceAsJson.getBytes() );
            return tmpPath.toString();
        }
    }


    protected Resource loadResource( FilePath workspace )
        throws Exception
    {

        Resource resource = null;

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
                new ImportCustomizer().addStarImports( "org.eclipse.jetty.load.generator" ) );

            GroovyShell interpreter = new GroovyShell( Resource.class.getClassLoader(), //
                                                       new Binding(), //
                                                       compilerConfiguration );

            resource = (Resource) interpreter.evaluate( groovy );
        }

        return resource;
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

        private static final List<LoadGeneratorStarterArgs.Transport> TRANSPORTS = Arrays.asList( LoadGeneratorStarterArgs.Transport.HTTP,//
                                                                                                  LoadGeneratorStarterArgs.Transport.HTTPS,//
                                                                                                  LoadGeneratorStarterArgs.Transport.H2,//
                                                                                                  LoadGeneratorStarterArgs.Transport.H2C );

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

        public List<LoadGeneratorStarterArgs.Transport> getTransports()
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

