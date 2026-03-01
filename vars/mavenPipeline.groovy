def call(Map config) {

    pipeline {
        agent any

        tools {
            maven 'Maven-3'
        }

        stages {

            stage('Checkout') {
                steps {
                    git branch: config.branch,
                        url: config.gitUrl
                }
            }

            stage('Build') {
                steps {
                    sh 'mvn clean package'
                }
            }

            stage('Test') {
                steps {
                    sh 'mvn test'
                }
            }

            stage('Archive') {
                steps {
                    archiveArtifacts artifacts: 'target/*.jar', fingerprint: true
                }
            }
        }

        post {
            success {
                echo "Build Successful"
            }
            failure {
                echo "Build Failed"
            }
        }
    }
}
