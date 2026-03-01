def call(Map config) {

    pipeline {
        agent any

        environment {
            AWS_REGION = "eu-west-1"
            ECR_REPO   = "725889403091.dkr.ecr.eu-west-1.amazonaws.com/nginx-ecr-repo-20251213"
            IMAGE_TAG  = "${env.BUILD_NUMBER}"
        }

        tools {
            jdk 'JDK-17'
            maven 'Maven-3'
        }

        stages {

            stage('Checkout') {
                steps {
                    git branch: config.branch,
                        url: 'git@github.com:kishorpatil2107/hello-app.git',
                        credentialsId: 'ken-ec2'
                }
            }

            stage('Build JAR') {
                steps {
                    //sh 'java -version'
                    //sh 'mvn clean package'
                    sh 'which java'
                    sh 'java -version'
                    sh 'echo $JAVA_HOME'
                    sh 'mvn -version'
                    sh 'mvn clean package'
                }
            }

            stage('Docker Build') {
                steps {
                    sh "docker build -t ${ECR_REPO}:${IMAGE_TAG} ."
                }
            }

            stage('ECR Login') {
                steps {
                    sh """
                    aws ecr get-login-password --region ${AWS_REGION} \
                    | docker login --username AWS --password-stdin 725889403091.dkr.ecr.eu-west-1.amazonaws.com
                    """
                }
            }

            stage('Push Image') {
                steps {
                    sh "docker push ${ECR_REPO}:${IMAGE_TAG}"
                }
            }
        }

        post {
            success {
                echo "Docker image pushed successfully 🚀"
                echo "Image: ${ECR_REPO}:${IMAGE_TAG}"
            }
            failure {
                echo "Pipeline Failed ❌"
            }
        }
    }
}
