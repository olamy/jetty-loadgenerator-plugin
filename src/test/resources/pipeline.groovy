import org.mortbay.jetty.load.generator.*
import org.mortbay.jetty.load.generator.starter.LoadGeneratorStarterArgs

node ('master') {

  def profile = new Resource( "/jenkins",
                                                  new Resource( "/jenkins/job/pipeline-test/",
                                                                new Resource( "/logo.gif" ),
                                                                new Resource( "/spacer.png" )
                                                  ),
                                                  new Resource( "jenkins/job/foo/" ),
                                                  new Resource( "/script.js",
                                                                new Resource( "/library.js" ),
                                                                new Resource( "/morestuff.js" )
                                                  ),
                                                  new Resource( "/anotherScript.js" ),
                                                  new Resource( "/iframeContents.html" ),
                                                  new Resource( "/moreIframeContents.html" ),
                                                  new Resource( "/favicon.ico" )
  );

  def transport = LoadGeneratorStarterArgs.Transport.HTTP;

  def timeUnit = java.util.concurrent.TimeUnit.SECONDS;

  loadgenerator host: 'localhost', port: ${port}, resourceProfile: profile, users: 1, transactionRate: 1, transport: transport, runIteration: ${iteration}, runningTime: '20', runningTimeUnit: timeUnit

}