package com.webtide.jetty.load.generator.jenkins;

import hudson.AbortException;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Proc;
import hudson.Util;
import hudson.model.Computer;
import hudson.model.JDK;
import hudson.model.TaskListener;
import hudson.remoting.Channel;
import hudson.remoting.Pipe;
import hudson.remoting.RemoteInputStream;
import hudson.remoting.RemoteOutputStream;
import hudson.remoting.SocketInputStream;
import hudson.remoting.SocketOutputStream;
import hudson.remoting.Which;
import hudson.slaves.Channels;
import hudson.util.ArgumentListBuilder;
import hudson.util.DelegatingOutputStream;
import hudson.util.StreamCopyThread;
import jenkins.security.MasterToSlaveCallable;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Zip;
import org.eclipse.jetty.load.generator.starter.JenkinsRemoteStarter;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

/**
 *
 */
public class LoadGeneratorProcessFactory
{


    public Channel buildChannel( TaskListener listener, JDK jdk, FilePath slaveRoot, Launcher launcher )
        throws Exception
    {

        // FIXME extraJvmArgs and check version for alpn jar

        ArgumentListBuilder args = new ArgumentListBuilder();
        if ( jdk == null )
        {
            args.add( "java" );
        }
        else
        {
            args.add( jdk.getHome() + "/bin/java" ); // use JDK.getExecutable() here ?
        }

        if ( DEBUG_PORT > 0 )
        {
            args.add( "-Xrunjdwp:transport=dt_socket,server=y,address=" + DEBUG_PORT );
        }

        String remotingJar = launcher.getChannel().call( new GetRemotingJar() );

        args.add( "-cp" );

        String cp = classPathEntry( slaveRoot, //
                                    JenkinsRemoteStarter.class, //
                                    "jetty-load-generator-starter", //
                                    listener ) //
            + ( launcher.isUnix() ? ":" : ";" ) //
            + remotingJar;

        for (LoadGeneratorProcessClasspathDecorator decorator : LoadGeneratorProcessClasspathDecorator.all())
        {
            cp = decorator.decorateClasspath( cp, listener, slaveRoot, launcher );
        }


        args.add( cp );

        args.add( "org.eclipse.jetty.load.generator.starter.JenkinsRemoteStarter" );

        final Acceptor acceptor = launcher.getChannel().call( new SocketHandler() );

        InetAddress host = InetAddress.getLocalHost();
        String hostName = null;// host.getHostName();

        final String socket =
            hostName != null ? hostName + ":" + acceptor.getPort() : String.valueOf( acceptor.getPort() );
        listener.getLogger().println( "Established TCP socket on " + socket );

        args.add( socket );

        final Proc proc = launcher.launch().cmds( args ).stdout( listener ).stderr( listener.getLogger() ).start();

        Connection con;
        try
        {
            con = acceptor.accept();
        }
        catch ( SocketTimeoutException e )
        {
            // failed to connect. Is the process dead?
            // if so, the error should have been provided by the launcher already.
            // so abort gracefully without a stack trace.
            if ( !proc.isAlive() )
            {
                throw new AbortException( "Failed to launch LoadGenerator. Exit code = " + proc.join() );
            }
            throw e;
        }

        Channel ch;
        try
        {
            ch = Channels.forProcess( "Channel to LoadGenerator " + Arrays.toString( args.toCommandArray() ), //
                                      Computer.threadPoolForRemoting, //
                                      new BufferedInputStream( con.in ), //
                                      new BufferedOutputStream( con.out ), //
                                      listener.getLogger(), //
                                      proc );


            return ch;
        }
        catch ( IOException e )
        {
            throw e;
        }

    }


    private static class RedirectableOutputStream
        extends DelegatingOutputStream
    {
        public RedirectableOutputStream( OutputStream out )
        {
            super( out );
        }

        public void set( OutputStream os )
        {
            super.out = os;
        }
    }

    /**
     * Represents a bi-directional connection.
     * <p>
     * <p>
     * This implementation is remoting aware, so it can be safely sent to the remote callable object.
     * <p>
     * <p>
     * When LoadGenerator runs on the master, Connection.in/.out just points to SocketInput/OutputStream,  and
     * a channel will be built on top of it. No complication.
     * <p>
     * <p>
     * When we run LoadGenertor on a slave, the master may not have a direct TCP/IP connectivity to the slave.
     * That means the {@link Channel} between the master and the LoadGenerator needs to be tunneled through
     * the channel between master and the slave, then go to TCP socket to the LoadGenerator process.
     * <p>
     * In this case, we'll have a thread running on the slave to read the socket as fast as we can
     * and buffer data on the master. What we need to avoid here is to have channel input stream be a
     * {@link RemoteInputStream}, as it'll turn every read into a remote read that has a large latency.
     */
    static final class Connection
        implements Serializable
    {
        // these two fields are non-null when Connection is in memory
        public InputStream in;

        public OutputStream out;

        // this field is used to capture the #in field in the serialized transport format
        private Pipe pipe;

        // only used on the sender side that was Socket object locally
        private transient Socket socket;

        Connection( Socket socket )
            throws IOException
        {
            this.in = new SocketInputStream( socket );
            this.out = new SocketOutputStream( socket );
            this.socket = socket;
        }

        private Connection( Pipe in, OutputStream out )
        {
            this.pipe = in;
            this.out = out;
        }

        private Object writeReplace()
        {
            Pipe p = Pipe.createLocalToRemote();
            // the thread will terminate when there's nothing more to read.
            new StreamCopyThread( "Stream reader: LoadGenerator process at " + socket, in, p.getOut(), true ).start();

            return new Connection( p, new RemoteOutputStream( out ) );
        }

        private Object readResolve()
        {
            assert in == null;
            this.in = pipe.getIn();
            assert out != null;
            assert socket == null;
            return this;
        }

        private static final long serialVersionUID = 1L;
    }


    interface Acceptor
    {
        Connection accept()
            throws IOException;

        int getPort()
            throws UnknownHostException;
    }

    private static final class SocketHandler
        extends MasterToSlaveCallable<Acceptor, IOException>
    {
        private static final long serialVersionUID = 1L;

        public Acceptor call()
            throws IOException
        {
            return new AcceptorImpl();
        }

        static final class AcceptorImpl
            implements Acceptor, Serializable
        {
            private static final long serialVersionUID = -2226788819948521018L;

            private transient final ServerSocket serverSocket;

            private transient Socket socket;

            AcceptorImpl()
                throws IOException
            {
                // open a TCP socket to talk to the launched LoadGenerator process.
                // let the OS pick up a random open port
                this.serverSocket = new ServerSocket();
                serverSocket.bind( null ); // new InetSocketAddress(InetAddress.getLocalHost(),0));
                // prevent a hang at the accept method in case the forked process didn't start successfully
                serverSocket.setSoTimeout( SOCKET_TIMEOUT );
            }

            public Connection accept()
                throws IOException
            {
                socket = serverSocket.accept();
                // we'd only accept one connection
                serverSocket.close();

                return new Connection( socket );
            }

            @Override
            public int getPort()
            {
                return serverSocket.getLocalPort();
            }

            /**
             * When sent to the remote node, send a proxy.
             */
            private Object writeReplace()
            {
                return Channel.current().export( Acceptor.class, this );
            }
        }
    }

    protected static final class GetRemotingJar
        extends MasterToSlaveCallable<String, IOException>
    {
        private static final long serialVersionUID = 6022357183425911351L;

        public String call()
            throws IOException
        {
            return Which.jarFile( hudson.remoting.Launcher.class ).getPath();
        }
    }


    /**
     * If not 0, launch LoadGenerator with a debugger port.
     */
    public static final int DEBUG_PORT = Integer.getInteger( "loadgenerator.debugPort", -1 );

    public static final int SOCKET_TIMEOUT = Integer.getInteger( "loadgenerator.socketTimeOut", 30 * 1000 );


    static String classPathEntry( FilePath root, Class<?> representative, String seedName, TaskListener listener )
        throws IOException, InterruptedException
    {
        if ( root == null )
        { // master
            return Which.jarFile( representative ).getAbsolutePath();
        }
        else
        {
            return copyJar( listener.getLogger(), root, representative, seedName ).getRemote();
        }
    }

    /**
     * Copies a jar file from the master to slave.
     */
    static FilePath copyJar( PrintStream log, FilePath dst, Class<?> representative, String seedName )
        throws IOException, InterruptedException
    {
        // in normal execution environment, the master should be loading 'representative' from this jar, so
        // in that way we can find it.
        File jar = Which.jarFile( representative );
        FilePath copiedJar = dst.child( seedName + ".jar" );

        if ( jar.isDirectory() )
        {
            // but during the development and unit test environment, we may be picking the class up from the classes dir
            Zip zip = new Zip();
            zip.setBasedir( jar );
            File t = File.createTempFile( seedName, "jar" );
            t.delete();
            zip.setDestFile( t );
            zip.setProject( new Project() );
            zip.execute();
            jar = t;
        }
        else if ( copiedJar.exists() //
            && copiedJar.digest().equals( Util.getDigestOf( jar ) ) ) //
        // && copiedJar.lastModified() == jar.lastModified() )
        {
            log.println( seedName + ".jar already up to date" );
            return copiedJar;
        }

        // Theoretically could be a race condition on a multi-executor Windows slave; symptom would be an IOException during the build.
        // Could perhaps be solved by synchronizing on dst.getChannel() or similar.
        new FilePath( jar ).copyTo( copiedJar );
        log.println( "Copied " + seedName + ".jar" );
        return copiedJar;
    }

    static class RemoteTmpFileCreate extends MasterToSlaveCallable<String, IOException>
    {
        @Override
        public String call()
            throws IOException
        {
            return Files.createTempFile( "loadgenerator_result", ".csv" ).toString();
        }
    }



    static class DeleteTmpFile
        extends MasterToSlaveCallable<Void, IOException>
    {
        private String filePath;

        public DeleteTmpFile( String filePath )
        {
            this.filePath = filePath;
        }

        @Override
        public Void call()
            throws IOException
        {
            Path path = Paths.get( filePath );
            if ( Files.exists( path ) )
            {
                Files.delete( path );
            }

            return null;
        }
    }

}
