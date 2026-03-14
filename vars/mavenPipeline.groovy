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
                    # Keep last 3 images, delete older
                    aws ecr describe-images --repository-name nginx-ecr-repo-20251213 --region ${AWS_REGION} \
                    --query 'sort_by(imageDetails,& imagePushedAt)[:-3].imageTags[0]' --output text | \
                    xargs -r -n 1 aws ecr batch-delete-image --repository-name nginx-ecr-repo-20251213 --region ${AWS_REGION} --image-ids imageTag=
                    """
                }
            }

            stage('Deploy to EC2 (Local)') {
                steps {
                    sh """
                    # Stop old container if exists
                    docker stop hello-app || true
                    docker rm hello-app || true

                    # Pull new image
                    docker pull ${ECR_REPO}:${IMAGE_TAG}

                    # Run new container
                    docker run -d --name hello-app -p 8000:8000 ${ECR_REPO}:${IMAGE_TAG}
                    """
                }
            }

        }

        post {
            success {
                echo "Pipeline completed successfully 🚀"
                echo "Deployed Image: ${ECR_REPO}:${IMAGE_TAG}"
                echo "App should be reachable at http://<EC2_PUBLIC_IP>:8000/hello"
            }
            failure {
                echo "Pipeline Failed ❌"
            }
        }
    }
}
