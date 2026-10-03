pipeline {
  agent any

  parameters {
    string(
      name: 'API_BASE_URL',
      defaultValue: 'http://localhost:8070',
      description: 'Target API. Default is the self-hosted toolshop-clone-api (docker compose up -d in ../ToolshopCloneApi). Point at https://api.practicesoftwaretesting.com for the public real Toolshop, or http://localhost:8091 for a self-hosted real Toolshop.'
    )
    choice(name: 'SUITE', choices: ['all', 'smoke'], description: 'Which tests to run')
  }

  options {
    timeout(time: 20, unit: 'MINUTES')
    timestamps()
    buildDiscarder(logRotator(numToKeepStr: '20'))
  }

  stages {
    stage('Checkout') { steps { checkout scm } }

    stage('Health check') {
      steps {
        bat "curl -s -o NUL -w \"API %%{http_code}\\n\" --max-time 30 %API_BASE_URL%/products"
      }
    }

    stage('API tests') {
      steps {
        script {
          def tagFilter = params.SUITE == 'smoke' ? '-Dcucumber.filter.tags=@smoke' : ''
          bat "mvn -B -Dapi.baseUrl=%API_BASE_URL% ${tagFilter} clean test"
        }
      }
    }
  }

  post {
    always {
      junit testResults: 'target/surefire-reports/*.xml', allowEmptyResults: true
      archiveArtifacts artifacts: 'target/cucumber-report.html, target/cucumber-results.xml, target/allure-results/**',
                        allowEmptyArchive: true
    }
  }
}
