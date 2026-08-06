pipeline {
    agent any

    environment {
        // Docker Hub 用户名
        DOCKERHUB_USER = 'macawparrot'
        
        // 镜像名称定义
        BACKEND_IMAGE = "${DOCKERHUB_USER}/myapp-backend:latest"
        FRONTEND_IMAGE = "${DOCKERHUB_USER}/myapp-frontend:latest" // 对应 nginx 目录的镜像
        
        // 凭据 ID (你刚才截图里的那个)
        DOCKER_CREDENTIALS_ID = 'docker-hub-credentials'
    }

    stages {
        stage('1. 拉取代码') {
            steps {
                echo '📥 正在从 GitHub 拉取最新代码...'
                checkout scm
            }
        }

        stage('2. 构建后端镜像') {
            steps {
                echo '🔨 正在构建后端 (Backend)...'
                dir('backend') {
                    script {
                        // 假设 backend/Dockerfile 存在且能处理 Maven 构建
                        // 如果需要在 Jenkins 宿主机先 mvn package，请修改此处逻辑
                        def image = docker.build("${BACKEND_IMAGE}", ".")
                        image.inside {
                            echo "后端镜像构建完成: ${BACKEND_IMAGE}"
                        }
                    }
                }
            }
        }

        stage('3. 构建前端/Nginx镜像') {
            steps {
                echo '🎨 正在构建前端 (Nginx)...'
                dir('nginx') {
                    script {
                        def image = docker.build("${FRONTEND_IMAGE}", ".")
                        image.inside {
                            echo "前端镜像构建完成: ${FRONTEND_IMAGE}"
                        }
                    }
                }
            }
        }

        stage('4. 推送镜像到 Docker Hub') {
            steps {
                echo '🚀 正在推送镜像到 Docker Hub...'
                script {
                    docker.withRegistry('https://registry.hub.docker.com', DOCKER_CREDENTIALS_ID) {
                        docker.image("${BACKEND_IMAGE}").push()
                        docker.image("${FRONTEND_IMAGE}").push()
                    }
                }
            }
        }

        stage('5. 本地部署（沙箱环境）') {
    steps {
        sh """
            mkdir -p ${DEPLOY_DIR}
            cp docker-compose.cicd.yml ${DEPLOY_DIR}/docker-compose.yml
            cd ${DEPLOY_DIR}
            docker-compose up -d
            echo '✅ 部署完成！服务已在沙箱环境启动，未暴露端口。'
        """
    }
}
    }

    post {
        always {
            echo '🏁 流水线执行结束。'
            // 可选：清理工作空间
            // cleanWs() 
        }
        success {
            echo '✅ 部署成功！'
        }
        failure {
            echo '❌ 部署失败，请检查日志。'
        }
    }
}
