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
 *
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

        builder.doRun( taskListener, filePath, run );

        return null;
    }


}
