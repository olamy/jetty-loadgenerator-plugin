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
    private transient Run<?, ?> run;

    @StepContextParameter
    private transient FilePath filePath;

    @StepContextParameter
    private transient Launcher launcher;

    @StepContextParameter
    private transient TaskListener taskListener;

    @Inject
    private transient LoadGeneratorBuilderStep loadGeneratorBuilderStep;

    @Override
    protected Void run()
        throws Exception
    {
        LoadGeneratorBuilder builder = new LoadGeneratorBuilder( loadGeneratorBuilderStep.getResource(), //
                                                                 loadGeneratorBuilderStep.getHost(), //
                                                                 Integer.toString( loadGeneratorBuilderStep.getPort() ),//
                                                                 Integer.toString( loadGeneratorBuilderStep.getUsers() ),//
                                                                 loadGeneratorBuilderStep.getProfileFromFile(), //
                                                                 loadGeneratorBuilderStep.getRunningTime(), //
                                                                 loadGeneratorBuilderStep.getRunningTimeUnit(), //
                                                                 Integer.toString( loadGeneratorBuilderStep.getRunIteration() ),//
                                                                 Integer.toString( loadGeneratorBuilderStep.getTransactionRate() ),//
                                                                 loadGeneratorBuilderStep.getTransport(), //
                                                                 loadGeneratorBuilderStep.isSecureProtocol(), //
                                                                 loadGeneratorBuilderStep.getJvmExtraArgs(), //
                                                                 Integer.toString( loadGeneratorBuilderStep.getGeneratorNumber() ) );
        builder.setJdkName( loadGeneratorBuilderStep.getJdkName() );
        builder.setWarmupNumber( Integer.toString( loadGeneratorBuilderStep.getWarmupNumber() ) );

        builder.doRun( taskListener, filePath, run, launcher );

        return null;
    }


}
