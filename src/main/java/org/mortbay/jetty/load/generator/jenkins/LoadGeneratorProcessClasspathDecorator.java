//
//  ========================================================================
//  Copyright (c) 1995-2018 Webtide LLC, Olivier Lamy
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

package org.mortbay.jetty.load.generator.jenkins;

import hudson.ExtensionList;
import hudson.ExtensionPoint;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.TaskListener;

import javax.annotation.CheckForNull;
import java.io.IOException;
import java.util.List;

/**
 *
 */
public abstract class LoadGeneratorProcessClasspathDecorator
    implements ExtensionPoint
{
    /**
     * unit test or IDE need some classes which are available within the uber jar but not for ide
     * @return the decorated classpath entry
     * @throws IOException
     */
    @CheckForNull
    public abstract String decorateClasspath( String classpath, TaskListener listener, FilePath slaveRoot,
                                             Launcher launcher )
        throws Exception;

    public static List<LoadGeneratorProcessClasspathDecorator> all()
    {
        return ExtensionList.lookup( LoadGeneratorProcessClasspathDecorator.class );
    }
}
