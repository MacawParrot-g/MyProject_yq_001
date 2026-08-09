pipeline {
    agent any

    environment {
        DOCKERHUB_USER = 'macawparrot'
        // 建议加上 BUILD_NUMBER 避免缓存问题，或者坚持用 latest
        BACKEND_IMAGE = "${DOCKERHUB_USER}/myapp-backend:latest"
        FRONTEND_IMAGE = "${DOCKERHUB_USER}/myapp-frontend:latest"
        DEPLOY_DIR = '/opt/cicd-sandbox'
        JAR_NAME = 'Automatic_test_script-1.0-SNAPSHOT.jar' 
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
                    
                    // 检查 JAR 包是否存在
                    sh "ls -la target/${JAR_NAME}"
                    
                    sh """
                        docker build -t ${BACKEND_IMAGE} .
                        echo "后端镜像构建完成: ${BACKEND_IMAGE}"
                    """
                }
            }
        }

        stage('3. 准备前端/Nginx镜像') {
            steps {
                echo '检测到前端已预构建，准备构建 Nginx 镜像...'
                dir('nginx') {
                    // 验证 dist 目录是否存在
                    sh 'ls -la html/dist'
                    
                    sh """
                        # 基于 nginx/html/Dockerfile 构建镜像
                        # 确保 Dockerfile 里的 COPY 路径是相对于 nginx 目录的
                        docker build -t ${FRONTEND_IMAGE} .
                        echo "前端镜像构建完成: ${FRONTEND_IMAGE}"
                    """
                }
            }
        }

        stage('4. 推送镜像到 Docker Hub') {
            steps {
                echo '正在推送镜像...'
                withCredentials([usernamePassword(credentialsId: 'dockerhub-cred', usernameVariable: 'DOCKER_USER', passwordVariable: 'DOCKER_PASS')]) {
                    sh """
                        echo "$DOCKER_PASS" | docker login -u "$DOCKER_USER" --password-stdin
                        docker push ${BACKEND_IMAGE}
                        docker push ${FRONTEND_IMAGE}
                        docker logout
                    """
                }
            }
        }

        stage('5. 本地部署（沙箱环境）') {
            steps {
                echo "正在部署到 ${DEPLOY_DIR}..."
                sh """
                    # 1. 创建部署目录
                    mkdir -p ${DEPLOY_DIR}
                    
                    # 2. 【关键修复】同步整个项目结构到部署目录
                    # 这样 docker-compose.yml 才能找到 ./backend 和 ./nginx 目录
                    rsync -av --exclude='.git' ./ ${DEPLOY_DIR}/
                    
                    # 3. 进入部署目录并启动
                    cd ${DEPLOY_DIR}
                    
                    # 停止旧容器（防止端口冲突）
                    docker compose -f docker-compose.cicd.yml down || true
                    
                    # 启动新容器
                    # 如果 yml 里写了 build: ./backend，这里会自动使用刚才复制过来的代码构建
                    # 如果 yml 里写的是 image: ...，则会拉取刚才 push 的镜像
                    docker compose -f docker-compose.cicd.yml up -d
                    
                    echo "部署完成！请检查服务状态。"
                """
            }
        }
    }

    post {
        always {
            echo '清理工作区...'
            cleanWs()
        }
        failure {
            echo '部署失败，请检查日志。'
        }
    }
}
