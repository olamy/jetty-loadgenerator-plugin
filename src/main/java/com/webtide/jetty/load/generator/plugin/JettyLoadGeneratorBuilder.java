package com.webtide.jetty.load.generator.plugin;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Util;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;
import jenkins.tasks.SimpleBuildStep;
import org.HdrHistogram.Recorder;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.eclipse.jetty.load.generator.HttpTransportBuilder;
import org.eclipse.jetty.load.generator.LoadGenerator;
import org.eclipse.jetty.load.generator.profile.ResourceProfile;
import org.eclipse.jetty.load.generator.responsetime.ResponseTimeListener;
import org.eclipse.jetty.xml.XmlConfiguration;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.servlet.ServletException;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
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

    private final String profileXml;

    private final String host;

    private final int port;

    private final int users;

    private final String profileXmlFromFile;

    private final int runningTime;

    private final TimeUnit runningTimeUnit;

    // Fields in config.jelly must match the parameter names in the "DataBoundConstructor"
    @DataBoundConstructor
    public JettyLoadGeneratorBuilder( String profileXml, String host, int port, int users, String profileXmlFromFile,
                                      int runningTime, TimeUnit runningTimeUnit )
    {
        this.profileXml = Util.fixEmptyAndTrim( profileXml );
        this.host = host;
        this.port = port;
        this.users = users;
        this.profileXmlFromFile = profileXmlFromFile;
        this.runningTime = runningTime;
        this.runningTimeUnit = runningTimeUnit;

    }

    public String getProfileXml()
    {
        return profileXml;
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

    @Override
    public boolean perform( AbstractBuild build, Launcher launcher, BuildListener listener )
    {
        try
        {
            run( listener, build.getWorkspace() );
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
        LOGGER.debug( " simpleBuildStep perform" );
        try
        {
            run( taskListener, filePath );
        }
        catch ( Exception e )
        {
            throw new RuntimeException( e.getMessage(), e );
        }
    }


    protected void run( final TaskListener taskListener, final FilePath filePath )
        throws Exception
    {
        LOGGER.info( "host: {}, port: {}", getHost(), getPort() );

        String xml = StringUtils.trim( this.getProfileXml() );

        String profileXmlPath = getProfileXmlFromFile();

        if ( StringUtils.isNotBlank( profileXmlPath ) )
        {
            FilePath profileXmlFilePath = filePath.child( profileXmlPath );
            xml = IOUtils.toString( profileXmlFilePath.read() );
        }

        LOGGER.info( "profileXml: {}", xml );
        ResourceProfile resourceProfile = (ResourceProfile) new XmlConfiguration( xml ).configure();
        LOGGER.info( "resourceProfile: {}", resourceProfile );

        ResponsePerPath responsePerPath = new ResponsePerPath();

        LoadGenerator loadGenerator = new LoadGenerator.Builder() //
            .host( getHost() ) //
            .port( getPort() ) //
            .users( getUsers() ) //
            .transactionRate( 1 ) //
            .transport( LoadGenerator.Transport.HTTP ) //
            .httpClientTransport( new HttpTransportBuilder().build() ) //
            //.sslContextFactory( sslContextFactory ) //
            .loadProfile( resourceProfile ) //
            .responseTimeListeners( responsePerPath, new ResponseTimeListener()
            {
                @Override
                public void onResponseTimeValue( Values values )
                {
                    taskListener.getLogger().println(
                        "response time " + TimeUnit.NANOSECONDS.toMillis( values.getTime() ) + " ms for path: "
                            + values.getPath() );
                }

                @Override
                public void onLoadGeneratorStop()
                {
                    taskListener.getLogger().println( "stop loadGenerator" );
                }
            } )
            //.requestListeners( testRequestListener ) //
            //.executor( new QueuedThreadPool() )
            .build();

        loadGenerator.run( 1, TimeUnit.MINUTES );

        for ( Map.Entry<String, Recorder> entry : responsePerPath.getRecorderPerPath().entrySet() )
        {
            AtomicInteger number = responsePerPath.responseNumberPerPath.get( entry.getKey() );
            LOGGER.info( "responsePerPath: {} - {}ms - number: {}", //
                         entry.getKey(), //
                         TimeUnit.NANOSECONDS.toMillis(
                             Math.round( entry.getValue().getIntervalHistogram().getMean() ) ), //
                         number.get() );
        }

    }

    // overrided for better type safety.
    // if your plugin doesn't really define any property on Descriptor,
    // you don't have to do this.
    @Override
    public DescriptorImpl getDescriptor()
    {
        return (DescriptorImpl) super.getDescriptor();
    }

    /**
     * Descriptor for {@link JettyLoadGeneratorBuilder}. Used as a singleton.
     * The class is marked as public so that it can be accessed from views.
     * <p>
     * <p>
     * See <tt>views/hudson/plugins/hello_world/JettyLoadGeneratorBuilder/*.jelly</tt>
     * for the actual HTML fragment for the configuration screen.
     */
    @Extension // this marker indicates Hudson that this is an implementation of an extension point.
    public static final class DescriptorImpl
        extends BuildStepDescriptor<Builder>
    {

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
                int port = Integer.parseInt( value );
                if ( port < 1 )
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

        public boolean isApplicable( Class<? extends AbstractProject> aClass )
        {
            // indicates that this builder can be used with all kinds of project types 
            return true;
        }

        /**
         * This human readable name is used in the configuration screen.
         */
        public String getDisplayName()
        {
            return "Jetty LoadGenerator";
        }

        public List<TimeUnit> getTimeUnits()
        {
            return Arrays.asList( TimeUnit.DAYS, //
                                  TimeUnit.HOURS, //
                                  TimeUnit.MINUTES, //
                                  TimeUnit.SECONDS, //
                                  TimeUnit.MILLISECONDS );
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


    public static class ResponsePerPath
        implements ResponseTimeListener
    {

        private final Map<String, Recorder> recorderPerPath = new ConcurrentHashMap<>();

        private final Map<String, AtomicInteger> responseNumberPerPath = new ConcurrentHashMap<>();

        @Override
        public void onResponseTimeValue( Values values )
        {
            String path = values.getPath();
            Recorder recorder = recorderPerPath.get( path );
            if ( recorder == null )
            {
                recorder = new Recorder( TimeUnit.MICROSECONDS.toNanos( 1 ), //
                                         TimeUnit.MINUTES.toNanos( 1 ), //
                                         3 );
                recorderPerPath.put( path, recorder );
            }
            recorder.recordValue( values.getTime() );

            AtomicInteger number = responseNumberPerPath.get( path );
            if ( number == null )
            {
                number = new AtomicInteger( 1 );
                responseNumberPerPath.put( path, number );
            }
            else
            {
                number.incrementAndGet();
            }


        }


        @Override
        public void onLoadGeneratorStop()
        {

        }

        public Map<String, Recorder> getRecorderPerPath()
        {
            return recorderPerPath;
        }
    }
}

