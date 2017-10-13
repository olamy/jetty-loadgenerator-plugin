package com.webtide.jetty.load.generator.jenkins.result;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import hudson.ExtensionList;
import hudson.ExtensionPoint;
import jenkins.model.Jenkins;

import java.io.File;
import java.nio.file.Files;
import java.util.logging.Level;

public abstract class ResultStoreProvider
    implements ExtensionPoint
{

    /**
     * All registered {@link ResultStoreProvider}s.
     */
    public static ExtensionList<ResultStore> all()
    {
        return Jenkins.getInstance().getExtensionList( ResultStore.class );
    }

    /**
     * Lookup a {@link ResultStoreProvider} by its id.
     *
     * @param providerId id of the desired {@link ResultStoreProvider}
     * @return the {@link ResultStoreProvider} or {@code null} if not found
     */
    @CheckForNull
    public static ResultStore getByIdOrNull( @Nullable String providerId )
    {
        if ( providerId == null || providerId.isEmpty() )
        {
            return null;
        }

        for ( ResultStore provider : ResultStoreProvider.all() )
        {
            if ( providerId.equals( provider.getProviderId() ) )
            {
                return provider;
            }
        }
        return null;
    }

}
