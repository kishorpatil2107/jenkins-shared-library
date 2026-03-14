def call(Map config) {

    pipeline {
        agent any

        environment {
            AWS_REGION = "eu-west-1"
            ECR_REPO   = "725889403091.dkr.ecr.eu-west-1.amazonaws.com/nginx-ecr-repo-20251213"
            IMAGE_TAG  = "${env.BUILD_NUMBER}"
            EC2_USER   = "ubuntu"               // Replace with your EC2 username
            EC2_IP     = "34.244.71.229"        // Replace with your EC2 public IP
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
                    --query 'sort_by(imageDetails,& imagePushedAt)[:-3].imageTags[0]' --output text \
                    | xargs -r -n 1 aws ecr batch-delete-image --repository-name nginx-ecr-repo-20251213 --region ${AWS_REGION} --image-ids imageTag=
                    """
                }
            }

            stage('Deploy to EC2') {
                steps {
                    sh """
                    ssh -o StrictHostKeyChecking=no ${EC2_USER}@${EC2_IP} \\
                    "docker pull ${ECR_REPO}:${IMAGE_TAG} &&
                     docker stop hello-app || true &&
                     docker rm hello-app || true &&
                     docker run -d --name hello-app -p 8000:8000 ${ECR_REPO}:${IMAGE_TAG}"
                    """
                }
            }
        }

        post {
            success {
                echo "Docker image pushed and deployed successfully 🚀"
                echo "Image: ${ECR_REPO}:${IMAGE_TAG}"
                echo "EC2: ${EC2_USER}@${EC2_IP}"
            }
            failure {
                echo "Pipeline Failed ❌"
            }
        }
    }
}


// def call(Map config) {

//     pipeline {
//         agent any

//         environment {
//             AWS_REGION = "eu-west-1"
//             ECR_REPO   = "725889403091.dkr.ecr.eu-west-1.amazonaws.com/nginx-ecr-repo-20251213"
//             IMAGE_TAG  = "${env.BUILD_NUMBER}"
//         }

//         tools {
//             jdk 'JDK-17'
//             maven 'Maven-3'
//         }

//         stages {

//             stage('Checkout') {
//                 steps {
//                     git branch: config.branch,
//                         url: 'https://github.com/kishorpatil2107/hello-app.git',
//                         credentialsId: 'github-2026'
//                 }
//             }

//             stage('Build JAR') {
//                 steps {
//                     //sh 'java -version'
//                     //sh 'mvn clean package'
//                     sh 'mvn clean package'
//                     sh 'ls -l target'
//                     sh 'which java'
//                     sh 'java -version'
//                     sh 'echo $JAVA_HOME'
//                     sh 'mvn -version'
//                     //sh 'mvn clean package'
//                 }
//             }

//             stage('Debug Dockerfile') {
//                 steps {
//                     sh 'cat Dockerfile'
//                     sh 'pwd'
//                     sh 'ls -l'
//                     sh 'ls -l Dockerfile'
//                 }
//             }

//             stage('Docker Build') {
//                 steps {
//                     sh "docker build -t ${ECR_REPO}:${IMAGE_TAG} ."
//                 }
//             }

//             stage('ECR Login') {
//                 steps {
//                     sh """
//                     aws ecr get-login-password --region ${AWS_REGION} \
//                     | docker login --username AWS --password-stdin 725889403091.dkr.ecr.eu-west-1.amazonaws.com
//                     """
//                 }
//             }

//             stage('Push Image') {
//                 steps {
//                     sh "docker push ${ECR_REPO}:${IMAGE_TAG}"
//                 }
//             }
//             stage('Cleanup Old Images') {
//                 steps {
//                     sh """
//                     aws ecr describe-images --repository-name nginx-ecr-repo-20251213 --region eu-west-1 \
//                     --query 'sort_by(imageDetails,& imagePushedAt)[:-3].imageTags[0]' --output text | \
//                     xargs -r -n 1 aws ecr batch-delete-image --repository-name nginx-ecr-repo-20251213 --region eu-west-1 --image-ids imageTag=
//                     """
//                 }
//             }
//         }

//         post {
//             success {
//                 echo "Docker image pushed successfully 🚀"
//                 echo "Image: ${ECR_REPO}:${IMAGE_TAG}"
//             }
//             failure {
//                 echo "Pipeline Failed ❌"
//             }
//         }
//     }
// }

// def call(Map config) {

//     pipeline {
//         agent any

//         // Prompt user for EC2 username and IP at runtime
//         parameters {
//             string(name: 'EC2_USER', defaultValue: 'ubuntu', description: 'EC2 SSH username')
//             string(name: 'EC2_IP', defaultValue: '', description: 'EC2 public IP or DNS')
//         }

//         environment {
//             AWS_REGION = "eu-west-1"
//             ECR_REPO   = "725889403091.dkr.ecr.eu-west-1.amazonaws.com/nginx-ecr-repo-20251213"
//             IMAGE_TAG  = "${env.BUILD_NUMBER}"
//             APP_PORT   = "8000"
//         }

//         tools {
//             jdk 'JDK-17'
//             maven 'Maven-3'
//         }

//         stages {

//             stage('Checkout') {
//                 steps {
//                     git branch: config.branch,
//                         url: 'https://github.com/kishorpatil2107/hello-app.git',
//                         credentialsId: 'github-2026'
//                 }
//             }

//             stage('Build JAR') {
//                 steps {
//                     sh 'mvn clean package'
//                     sh 'ls -l target'
//                     sh 'which java'
//                     sh 'java -version'
//                     sh 'echo $JAVA_HOME'
//                     sh 'mvn -version'
//                 }
//             }

//             stage('Debug Dockerfile') {
//                 steps {
//                     sh 'cat Dockerfile'
//                     sh 'pwd'
//                     sh 'ls -l'
//                     sh 'ls -l Dockerfile'
//                 }
//             }

//             stage('Docker Build') {
//                 steps {
//                     sh "docker build -t ${ECR_REPO}:${IMAGE_TAG} ."
//                 }
//             }

//             stage('ECR Login') {
//                 steps {
//                     sh """
//                     aws ecr get-login-password --region ${AWS_REGION} \
//                     | docker login --username AWS --password-stdin 725889403091.dkr.ecr.eu-west-1.amazonaws.com
//                     """
//                 }
//             }

//             stage('Push Image') {
//                 steps {
//                     sh "docker push ${ECR_REPO}:${IMAGE_TAG}"
//                 }
//             }

//             stage('Cleanup Old Images') {
//                 steps {
//                     sh """
//                     # Keep only last 3 images in ECR
//                     aws ecr describe-images --repository-name nginx-ecr-repo-20251213 --region ${AWS_REGION} \
//                     --query 'sort_by(imageDetails,& imagePushedAt)[:-3].imageTags[0]' --output text | \
//                     xargs -r -n 1 aws ecr batch-delete-image --repository-name nginx-ecr-repo-20251213 --region ${AWS_REGION} --image-ids imageTag=
//                     """
//                 }
//             }

//             stage('Deploy to EC2') {
//                 steps {
//                     sh """
//                     ssh -o StrictHostKeyChecking=no ${params.EC2_USER}@${params.EC2_IP} '
//                         docker pull ${ECR_REPO}:${IMAGE_TAG} &&
//                         docker stop hello-app || true &&
//                         docker rm hello-app || true &&
//                         docker run -d --name hello-app -p ${APP_PORT}:${APP_PORT} ${ECR_REPO}:${IMAGE_TAG}
//                     '
//                     """
//                 }
//             }

//         }

//         post {
//             success {
//                 echo "Docker image pushed successfully 🚀"
//                 echo "Image: ${ECR_REPO}:${IMAGE_TAG}"
//                 echo "Deployed to EC2: ${params.EC2_USER}@${params.EC2_IP}:${APP_PORT}"
//             }
//             failure {
//                 echo "Pipeline Failed ❌"
//             }
//         }
//     }
}
