package org.mortbay.jetty.load.generator.jenkins.result.pipeline;

import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.SynchronousStepExecution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class LoadTestResultStepExecution extends SynchronousStepExecution
{
    private static final transient Logger LOGGER = LoggerFactory.getLogger( LoadTestResultStepExecution.class.getName());

    public LoadTestResultStepExecution( StepContext stepContext )
    {
        super(stepContext);
    }

    @Override
    protected Object run()
        throws Exception
    {
        LOGGER.info( "run" );
        return null;
    }
}
