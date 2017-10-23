package com.webtide.jetty.load.generator.jenkins.result;

import hudson.Extension;
import hudson.model.Job;
import hudson.model.JobProperty;
import hudson.model.JobPropertyDescriptor;
import hudson.util.CopyOnWriteList;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

import java.util.List;

public class ElasticHostProjectProperty
    extends JobProperty<Job<?, ?>>
{
    @DataBoundConstructor
    public ElasticHostProjectProperty()
    {
        // no op
    }

    @Extension
    public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();

    public static final class DescriptorImpl
        extends JobPropertyDescriptor
    {
        private final CopyOnWriteList<ElasticHost> elasticHosts = new CopyOnWriteList<>();

        public DescriptorImpl()
        {
            super( ElasticHostProjectProperty.class );
            load();
        }

        @Override
        @SuppressWarnings( "unchecked" )
        public boolean isApplicable( Class<? extends Job> jobType )
        {
            return Job.class.isAssignableFrom( jobType );
        }

        @Override
        public String getDisplayName()
        {
            return "Elastic Host";
        }

        public void setElasticHosts( ElasticHost elasticHost )
        {
            elasticHosts.add( elasticHost );
        }

        public List<ElasticHost> getElasticHosts()
        {
            return elasticHosts.getView();
        }

        @Override
        public boolean configure( StaplerRequest req, JSONObject formData )
        {
            elasticHosts.replaceBy( req.bindJSONToList( ElasticHost.class, formData.get( "elasticHosts" ) ) );
            save();
            return true;
        }
    }
}
