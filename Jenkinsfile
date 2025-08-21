pipeline {
  agent {
    docker {
      image 'maven:3.9-eclipse-temurin-21'
      args '-v $HOME/.m2:/root/.m2'
    }
  }
  environment {
    NO_COLOR = '1'
    CLICOLOR = '0'
    TERM = 'dumb'
  }
  options {
    timestamps()
    buildDiscarder(logRotator(numToKeepStr: '20'))
  }
  stages {
    stage('Checkout') {
      steps { checkout scm }
    }
    stage('Test') {
      steps {
        sh '''
          mvn -DskipTests=false -Dmaven.test.failure.ignore=true \
              -Dpicocli.ansi=false -Dorg.fusesource.jansi.Ansi.disable=true \
              test
        '''
      }
    }
  }
  post {
    always {
      junit allowEmptyResults: true, testResults: 'target/surefire-reports/*.xml'

      archiveArtifacts allowEmptyArchive: true, fingerprint: true,
        artifacts: 'target/e2e/**/diff.patch, target/e2e/**/actual.log, target/e2e/**/expected.log'
    }
  }
}
