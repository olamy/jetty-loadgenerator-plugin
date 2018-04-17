package com.webtide.jetty.load.generator.jenkins.result.pipeline;

import com.google.common.collect.ImmutableSet;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.TaskListener;
import org.jenkinsci.plugins.workflow.steps.AbstractStepDescriptorImpl;
import org.jenkinsci.plugins.workflow.steps.Step;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepDescriptor;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import org.kohsuke.stapler.DataBoundConstructor;

import javax.annotation.Nonnull;
import java.io.Serializable;
import java.util.Set;

public class LoadTestResultStep extends Step implements Serializable
{

    @DataBoundConstructor
    public LoadTestResultStep()
    {
    }

    @Override
    public StepExecution start( StepContext context )
        throws Exception
    {
        return new LoadTestResultStepExecution( context );
    }

    @Extension
    public static class DescriptorImpl extends StepDescriptor
    {

        public DescriptorImpl()
        {
            super();
        }

        @Override
        public String getFunctionName() {
            return "loadresult";
        }

        @Override
        @Nonnull
        public String getDisplayName() {
            return "Archive Load Result Test";
        }

        @Override
        public Set<? extends Class<?>> getRequiredContext() {
            return ImmutableSet.of( FilePath.class, TaskListener.class, Launcher.class);
        }
    }
}
