pipeline {
    agent any

    environment {
        DOCKERHUB_USER  = 'macawparrot'
        BACKEND_IMAGE   = "${DOCKERHUB_USER}/myapp-backend:latest"
        FRONTEND_IMAGE  = "${DOCKERHUB_USER}/myapp-frontend:latest"
        DEPLOY_DIR      = '/opt/cicd-sandbox'
        JAR_NAME        = 'Automatic_test_script-1.0-SNAPSHOT.jar'
    }

    stages {
        stage('1. 拉取代码') {
            steps {
                echo '正在从 GitHub 拉取最新代码...'
                checkout scm
            }
        }

        stage('2. 构建后端镜像') {
            steps {
                echo '正在构建后端 (Backend)...'
                dir('backend') {
                    sh 'mvn clean package -DskipTests'
                    sh "ls -la target/${JAR_NAME}"
                    sh """
                        if [ ! -f target/${JAR_NAME} ]; then
                            echo "❌ JAR包不存在，请检查 pom.xml 的 finalName 配置"
                            exit 1
                        fi
                        docker build -t ${BACKEND_IMAGE} .
                    """
                }
            }
        }

        stage('3. 构建前端/Nginx镜像') {
            steps {
                echo '正在构建前端 (Nginx)...'
                // TODO: 补充前端构建逻辑
            }
        }

        // stage('4. 推送镜像到 Docker Hub') {
        //     steps {
        //         echo '正在推送镜像到 Docker Hub...'
        //         withCredentials([usernamePassword(
        //             credentialsId: 'docker-hub-credentials',
        //             usernameVariable: 'DH_USER',
        //             passwordVariable: 'DH_PASS'
        //         )]) {
        //             sh 'echo "${DH_PASS}" | docker login -u "${DH_USER}" --password-stdin'
        //             sh "docker push ${BACKEND_IMAGE}"
        //             sh "docker push ${FRONTEND_IMAGE}"
        //         }
        //     }
        // }

        stage('5. 本地部署（沙箱环境）') {
            steps {
                sh """
                    mkdir -p ${DEPLOY_DIR}
                    cp docker-compose.cicd.yml ${DEPLOY_DIR}/docker-compose.yml
                    cd ${DEPLOY_DIR}
                    docker compose -f docker-compose.yml -p cicd-test up -d --build
                    echo '部署完成！服务已在沙箱环境启动。'
                """
            }
        }
    }

    post {
        always {
            echo '清理工作区...'
            cleanWs()
        }
        success {
            echo 'CI/CD 流水线执行成功！'
        }
        failure {
            echo '部署失败，请检查日志。'
        }
    }
}
