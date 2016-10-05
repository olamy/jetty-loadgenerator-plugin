package com.webtide.jetty.load.generator.jenkins.steps;

import com.google.inject.Inject;
import com.webtide.jetty.load.generator.jenkins.LoadGeneratorBuilder;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Run;
import hudson.model.TaskListener;
import org.jenkinsci.plugins.workflow.steps.AbstractSynchronousStepExecution;
import org.jenkinsci.plugins.workflow.steps.StepContextParameter;

/**
 * Created by olamy on 4/10/16.
 */
public class LoadGeneratorBuilderStepExecution
    extends AbstractSynchronousStepExecution<Void>
{

    @StepContextParameter
    private Run<?, ?> run;

    @StepContextParameter
    private FilePath filePath;

    @StepContextParameter
    private Launcher launcher;

    @StepContextParameter
    private TaskListener taskListener;

    @Inject
    private transient LoadGeneratorBuilderStep loadGeneratorBuilderStep;

    @Override
    protected Void run()
        throws Exception
    {
        LoadGeneratorBuilder builder = new LoadGeneratorBuilder( loadGeneratorBuilderStep.getResourceProfile(), //
                                                                 loadGeneratorBuilderStep.getHost(), //
                                                                 loadGeneratorBuilderStep.getPort(), //
                                                                 loadGeneratorBuilderStep.getUsers(), //
                                                                 loadGeneratorBuilderStep.getProfileXmlFromFile(), //
                                                                 loadGeneratorBuilderStep.getRunningTime(), //
                                                                 loadGeneratorBuilderStep.getRunningTimeUnit(), //
                                                                 loadGeneratorBuilderStep.getRunIteration(), //
                                                                 loadGeneratorBuilderStep.getTransactionRate(), //
                                                                 loadGeneratorBuilderStep.getTransport(), //
                                                                 loadGeneratorBuilderStep.isSecureProtocol() );

        builder.perform( run, filePath, launcher, taskListener );

        return null;
    }


}
