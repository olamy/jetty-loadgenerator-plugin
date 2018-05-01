node('linux') {
  checkout scm
  stage('Compile') {
      timeout(time: 15, unit: 'MINUTES') {
        withMaven(
                maven: 'maven3.5',
                jdk: "jdk8",
                mavenLocalRepo: "${env.JENKINS_HOME}/${env.EXECUTOR_NUMBER}") {
          sh "mvn -V -B clean install"
        }
    }
  }

}
