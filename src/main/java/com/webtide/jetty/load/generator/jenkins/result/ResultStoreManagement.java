package com.webtide.jetty.load.generator.jenkins.result;

import hudson.Extension;
import hudson.model.Item;
import hudson.model.ManagementLink;
import hudson.security.Permission;
import jenkins.model.Jenkins;
import org.kohsuke.stapler.StaplerProxy;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Level;
import java.util.logging.Logger;

@Extension
public class ResultStoreManagement
    extends ManagementLink
    implements StaplerProxy
{

    private final static Logger LOGGER = Logger.getLogger( ResultStoreManagement.class.getName() );

    private static Path STORE_DIRECTORY;

    static
    {
        STORE_DIRECTORY = new File( Jenkins.getInstance().getRootDir(), "loadresults" ).toPath();
        // create directory structure if not existing
        if ( !Files.exists( STORE_DIRECTORY ) )
        {
            try
            {
                STORE_DIRECTORY = Files.createDirectories( STORE_DIRECTORY );
            }
            catch ( IOException e )
            {
                String msg = "Cannot create directory structure:" + STORE_DIRECTORY;
                LOGGER.log( Level.SEVERE, msg );
                //throw new RuntimeException( msg );
            }
        }
    }

    public ResultStoreManagement()
    {
        super();

    }

    public ResultStore getResultStore()
    {
        // FIXME dynamic
        return Jenkins.getInstance().getExtensionList( CsvResultStore.class ).get( 0 );
    }

    @Override
    public String getIconFileName()
    {
        return "TODO create an icon";
    }


    @Override
    public String getUrlName()
    {
        return "loadresults";
    }

    @Override
    public String getDisplayName()
    {
        return "Load Result Management";
    }

    @Override
    public Permission getRequiredPermission()
    {
        return super.getRequiredPermission();
    }

    @Override
    public boolean getRequiresConfirmation()
    {
        return super.getRequiresConfirmation();
    }

    private void checkPermission( Permission permission )
    {
        Jenkins.getInstance().checkPermission( permission );
    }

    @Override
    public Object getTarget()
    {
        checkPermission( Item.EXTENDED_READ );
        return this;
    }


    public static Path getStoreDirectory()
    {
        return STORE_DIRECTORY;
    }

}
