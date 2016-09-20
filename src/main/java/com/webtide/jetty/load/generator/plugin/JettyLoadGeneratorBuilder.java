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
import java.util.concurrent.atomic.AtomicLong;

/**
 * Sample {@link Builder}.
 * <p>
 * <p>
 * When the user configures the project and enables this builder,
 * {@link DescriptorImpl\#newInstance(StaplerRequest)} is invoked
 * and a new {@link JettyLoadGeneratorBuilder} is created. The created
 * instance is persisted to the project configuration XML by using
 * XStream, so this allows you to use instance fields (like {@link \#name})
 * to remember the configuration.
 * <p>
 * <p>
 * When a build is performed, the {@link \#perform(AbstractBuild, Launcher, BuildListener)} method
 * will be invoked.
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

    public List<TimeUnit> getTimeUnits() {
        return Arrays.asList(TimeUnit.DAYS, TimeUnit.HOURS, TimeUnit.MINUTES, TimeUnit.MILLISECONDS);
    }

    @Override
    public boolean perform( AbstractBuild build, Launcher launcher, BuildListener listener )
    {
        try
        {
            run( listener );
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
            run( taskListener );
        }
        catch ( Exception e )
        {
            throw new RuntimeException( e.getMessage(), e );
        }
    }


    protected void run( final TaskListener taskListener )
        throws Exception
    {
        LOGGER.info( "host: {}, port: {}", getHost(), getPort() );
        String xml = StringUtils.trim( this.getProfileXml() );
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

        for ( Map.Entry<String, AtomicLong> entry : responsePerPath.getRecorderPerPath().entrySet() )
        {
            LOGGER.info( "responsePerPath: {}", entry );
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

        /**
         * Performs on-the-fly validation of the form field 'name'.
         *
         * @param value This parameter receives the value that the user has typed.
         * @return Indicates the outcome of the validation. This is sent to the browser.
         */
        public FormValidation doCheckProfileXml( @QueryParameter String value )
            throws IOException, ServletException
        {
            // TODO some validation here
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

        private final Map<String, AtomicLong> recorderPerPath = new ConcurrentHashMap<>();

        @Override
        public void onResponseTimeValue( Values values )
        {
            String path = values.getPath();
            AtomicLong response = recorderPerPath.get( path );
            if ( response == null )
            {
                response = new AtomicLong( 1 );
                recorderPerPath.put( path, response );
            }
            else
            {
                response.incrementAndGet();
            }
        }


        @Override
        public void onLoadGeneratorStop()
        {

        }

        public Map<String, AtomicLong> getRecorderPerPath()
        {
            return recorderPerPath;
        }
    }
}

