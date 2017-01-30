import org.webtide.jetty.load.generator.profile.*
node ('master') {

  def profile = new ResourceProfile(new Resource( "/jenkins",
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
  )
  );

  def transport = org.eclipse.jetty.load.generator.LoadGenerator.Transport.HTTP;

  def timeUnit = java.util.concurrent.TimeUnit.SECONDS;

  loadgenerator host: 'localhost', port: ${port}, resourceProfile: profile, users: 1, transactionRate: 1, transport: transport, runIteration: ${iteration}, runningTime: '20', runningTimeUnit: timeUnit

}