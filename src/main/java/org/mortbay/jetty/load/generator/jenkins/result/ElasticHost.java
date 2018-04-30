package org.mortbay.jetty.load.generator.jenkins.result;

import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import hudson.util.FormValidation;
import org.apache.commons.lang3.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.mortbay.jetty.load.generator.store.ElasticResultStore;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ElasticHost
    extends AbstractDescribableImpl<ElasticHost>
{

    private String elasticHostName, elasticHost, elasticScheme = "http", elasticUsername, elasticPassword;

    private int elasticPort;

    @DataBoundConstructor
    public ElasticHost( String elasticHostName, String elasticHost, String elasticScheme, String elasticUsername,
                        String elasticPassword, int elasticPort )
    {
        this.elasticHostName = elasticHostName;
        this.elasticHost = elasticHost;
        this.elasticScheme = elasticScheme;
        this.elasticUsername = elasticUsername;
        this.elasticPassword = elasticPassword;
        this.elasticPort = elasticPort;
    }

    public String getElasticHostName()
    {
        return elasticHostName;
    }

    public void setElasticHostName( String elasticHostName )
    {
        this.elasticHostName = elasticHostName;
    }

    public String getElasticHost()
    {
        return elasticHost;
    }

    public void setElasticHost( String elasticHost )
    {
        this.elasticHost = elasticHost;
    }

    public String getElasticScheme()
    {
        return elasticScheme;
    }

    public void setElasticScheme( String elasticScheme )
    {
        this.elasticScheme = elasticScheme;
    }

    public String getElasticUsername()
    {
        return elasticUsername;
    }

    public void setElasticUsername( String elasticUsername )
    {
        this.elasticUsername = elasticUsername;
    }

    public String getElasticPassword()
    {
        return elasticPassword;
    }

    public void setElasticPassword( String elasticPassword )
    {
        this.elasticPassword = elasticPassword;
    }

    public int getElasticPort()
    {
        return elasticPort;
    }

    public void setElasticPort( int elasticPort )
    {
        this.elasticPort = elasticPort;
    }

    /**
     * Gets the effective {@link ElasticHost} associated with the given project.
     *
     * @return null
     * if no such was found.
     */
    public static ElasticHost get( String elasticHostName )
    {
        if ( StringUtils.isNotEmpty( elasticHostName ) )
        {
            // Looks in global configuration for the site configured

            Optional<ElasticHost> elasticHost = ElasticHostProjectProperty.DESCRIPTOR.getElasticHosts().stream() //
                .filter( elasticHost1 -> StringUtils.equals( elasticHost1.getElasticHostName(), elasticHostName ) ) //
                .findFirst();
            if ( elasticHost.isPresent() )
            {
                return elasticHost.get();
            }
        }

        // folder??

        // none is explicitly configured. try the default ---
        // if only one is configured, that must be it.
        List<ElasticHost> elasticHosts = ElasticHostProjectProperty.DESCRIPTOR.getElasticHosts();
        if ( !elasticHosts.isEmpty() )
        {
            return elasticHosts.get( 0 );
        }

        return null;
    }

    // FIXME definitely something better to do as we build httpclient everytime.....
    public ElasticResultStore buildElasticResultStore()
    {
        ElasticResultStore elasticResultStore = new ElasticResultStore();
        Map<String, String> setupData = new HashMap<>();
        setupData.put( ElasticResultStore.HOST_KEY, this.getElasticHost() );
        setupData.put( ElasticResultStore.PORT_KEY, Integer.toString( this.getElasticPort() ) );
        setupData.put( ElasticResultStore.SCHEME_KEY, this.getElasticScheme() );
        setupData.put( ElasticResultStore.USER_KEY, this.getElasticUsername() );
        setupData.put( ElasticResultStore.PWD_KEY, this.getElasticPassword() );
        elasticResultStore.initialize( setupData );
        return elasticResultStore;
    }


    @Extension
    public static class DescriptorImpl
        extends Descriptor<ElasticHost>
    {
        @Override
        public String getDisplayName()
        {
            return "Elastic Host";
        }

        public List<String> getSchemes()
        {
            return Arrays.asList( "http", "https" );
        }

        /**
         * Checks if parameters are ok
         */
        public FormValidation doValidate( @QueryParameter String elasticHostName, //
                                          @QueryParameter String elasticHost, //
                                          @QueryParameter String elasticScheme, //
                                          @QueryParameter String elasticUsername, //
                                          @QueryParameter String elasticPassword, //
                                          @QueryParameter Integer elasticPort )
            throws IOException
        {

            if ( StringUtils.isEmpty( elasticHostName ) )
            {
                return FormValidation.error( "elasticHostName cannot be empty" );
            }

            if ( StringUtils.isEmpty( elasticHost ) )
            {
                return FormValidation.error( "elasticHost cannot be empty" );
            }

            if ( StringUtils.isEmpty( elasticScheme ) )
            {
                return FormValidation.error( "elasticScheme cannot be empty" );
            }

            if ( elasticPort < 1 )
            {
                return FormValidation.error( "elasticPort must be a positive Integer" );
            }

            return FormValidation.ok( "Success" );

            // TODO try a connection to validate informations
        }
    }
}
