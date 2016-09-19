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
import net.sf.json.JSONObject;
import org.eclipse.jetty.load.generator.LoadGenerator;
import org.eclipse.jetty.load.generator.profile.ResourceProfile;
import org.eclipse.jetty.load.generator.responsetime.ResponseTimeListener;
import org.eclipse.jetty.xml.XmlConfiguration;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.servlet.ServletException;
import java.io.IOException;

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

    // Fields in config.jelly must match the parameter names in the "DataBoundConstructor"
    @DataBoundConstructor
    public JettyLoadGeneratorBuilder( String profileXml, String host, int port, int users )
    {
        this.profileXml = Util.fixEmptyAndTrim( profileXml);
        this.host = host;
        this.port = port;
        this.users = users;
    }

    /**
     * We'll use this from the <tt>config.jelly</tt>.
     */
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

    @Override
    public boolean perform( AbstractBuild build, Launcher launcher, BuildListener listener )
    {
        try
        {
            run();
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
            run();
        }
        catch ( Exception e )
        {
            throw new RuntimeException( e.getMessage(), e );
        }
    }


    protected void run()
        throws Exception
    {
        LOGGER.info( "host: {}, port: {}", getHost(), getPort());
        LOGGER.info( "profileXml: {}", this.getProfileXml() );
        ResourceProfile resourceProfile = (ResourceProfile) new XmlConfiguration( this.getProfileXml() ).configure();
        LOGGER.info( "resourceProfile: {}", resourceProfile );

        /*
        LoadGenerator loadGenerator = new LoadGenerator.Builder() //
            .host( "localhost" ) //
            .port( connector.getLocalPort() ) //
            .users( this.usersNumber ) //
            .transactionRate( 1 ) //
            .transport( this.transport ) //
            .httpClientTransport( this.httpClientTransport() ) //
            .sslContextFactory( sslContextFactory ) //
            .loadProfile( profile ) //
            .responseTimeListeners( responseTimeListeners.toArray( new ResponseTimeListener[responseTimeListeners.size()]) ) //
            .requestListeners( testRequestListener ) //
            //.executor( new QueuedThreadPool() )
            .build();
        */
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
}

