package com.webtide.jetty.load.generator.jenkins;

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
