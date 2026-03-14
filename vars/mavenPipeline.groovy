def call(Map config) {

    pipeline {
        agent any

        environment {
            AWS_REGION = "eu-west-1"
            ECR_REPO   = "725889403091.dkr.ecr.eu-west-1.amazonaws.com/nginx-ecr-repo-20251213"
            IMAGE_TAG  = "${env.BUILD_NUMBER}"
            EC2_USER   = "ubuntu"
            EC2_IP     = "127.0.0.1" // local EC2 (Jenkins on same machine)
        }

        tools {
            jdk 'JDK-17'
            maven 'Maven-3'
        }

        stages {

            stage('Checkout') {
                steps {
                    git branch: config.branch,
                        url: 'https://github.com/kishorpatil2107/hello-app.git',
                        credentialsId: 'github-2026'
                }
            }

            stage('Build JAR') {
                steps {
                    sh 'mvn clean package'
                    sh 'ls -l target'
                    sh 'which java'
                    sh 'java -version'
                    sh 'echo $JAVA_HOME'
                    sh 'mvn -version'
                }
            }

            stage('Debug Dockerfile') {
                steps {
                    sh 'cat Dockerfile'
                    sh 'pwd'
                    sh 'ls -l'
                    sh 'ls -l Dockerfile'
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
                    | docker login --username AWS --password-stdin ${ECR_REPO}
                    """
                }
            }

            stage('Push Image') {
                steps {
                    sh "docker push ${ECR_REPO}:${IMAGE_TAG}"
                }
            }

            stage('Cleanup Old Images') {
                steps {
                    sh """
                    aws ecr describe-images --repository-name nginx-ecr-repo-20251213 --region ${AWS_REGION} \
                    --query 'sort_by(imageDetails,& imagePushedAt)[:-3].imageTags[0]' --output text | \
                    xargs -r -n 1 -I {} aws ecr batch-delete-image \
                    --repository-name nginx-ecr-repo-20251213 --region ${AWS_REGION} --image-ids imageTag={}
                    """
                }
            }

            stage('Deploy to EC2 (Local)') {
                steps {
                    sh """
                    ssh -o StrictHostKeyChecking=no ${EC2_USER}@${EC2_IP} '
                        docker pull ${ECR_REPO}:${IMAGE_TAG} &&
                        docker stop hello-app || true &&
                        docker rm hello-app || true &&
                        docker run -d --name hello-app -p 8000:8000 ${ECR_REPO}:${IMAGE_TAG}
                    '
                    """
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
