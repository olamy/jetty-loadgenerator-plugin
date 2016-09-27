//
//  ========================================================================
//  Copyright (c) 1995-2016 Webtide LLC
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

package com.webtide.jetty.load.generator.plugin;

import hudson.model.HealthReport;
import hudson.model.HealthReportingAction;

/**
 * Created by olamy on 21/09/2016.
 */
public class JettyLoadGeneratorBuildAction implements HealthReportingAction
{

    //private final AbstractBuild<?, ?> owner;

    private HealthReport health = null;

    @Override
    public HealthReport getBuildHealth()
    {
        if (health != null) {
            return health;
        }
        return null;
    }

    @Override
    public String getIconFileName()
    {
        // TODO Jetty icon
        return null;
    }

    @Override
    public String getDisplayName()
    {
        // TODO i18n
        return "jetty-load-generator";
    }

    @Override
    public String getUrlName()
    {
        return "jetty-loadgenerator";
    }
}
