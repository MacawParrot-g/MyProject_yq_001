pipeline {
    agent any

    environment {
        DOCKERHUB_USER = 'macawparrot'
        // 建议加上版本号或构建号，避免一直用 latest 导致缓存问题
        BACKEND_IMAGE = "${DOCKERHUB_USER}/myapp-backend:latest"
        FRONTEND_IMAGE = "${DOCKERHUB_USER}/myapp-frontend:latest"
        DEPLOY_DIR = '/opt/cicd-sandbox'
        JAR_NAME = 'Automatic_test_script-1.0-SNAPSHOT.jar' // 根据你的 pom.xml 确认这个名字
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
                        # 构建后端镜像
                        docker build -t ${BACKEND_IMAGE} .
                    """
                }
            }
        }

        stage('3. 构建前端/Nginx镜像') {
            steps {
                echo '正在构建前端 (Nginx)...'
                dir('nginx') { // 假设你的前端代码或 Dockerfile 在 nginx 目录下
                    // 【重要】这里需要补充前端的构建逻辑
                    // 如果前端需要 npm build，请在这里执行，例如：
                    // sh 'npm install && npm run build' 
                    
                    // 构建前端镜像
                    sh "docker build -t ${FRONTEND_IMAGE} ."
                }
            }
        }

        stage('4. 推送镜像到 Docker Hub') {
            steps {
                echo '正在推送镜像到 Docker Hub...'
                withCredentials([usernamePassword(credentialsId: 'docker-hub-credentials', usernameVariable: 'DH_USER', passwordVariable: 'DH_PASS')]) {
                    // 使用 --password-stdin 更安全
                    sh "echo \${DH_PASS} | docker login -u \${DH_USER} --password-stdin"
                    sh "docker push ${BACKEND_IMAGE}"
                    sh "docker push ${FRONTEND_IMAGE}"
                }
            }
        }

        stage('5. 本地部署（沙箱环境）') {
            steps {
                sh """
                    # 1. 创建部署目录
                    mkdir -p ${DEPLOY_DIR}
                    
                    # 2. 【核心修复】将整个工作区内容复制到部署目录
                    # 这样 docker-compose.yml 里的 build: ./backend 才能找到对应的文件夹
                    # 注意：这会覆盖部署目录下的同名文件
                    cp -r ./* ${DEPLOY_DIR}/
                    
                    cd ${DEPLOY_DIR}
                    
                    # 3. 停止并删除旧容器（避免端口冲突或配置未更新）
                    docker compose -f docker-compose.cicd.yml -p cicd-test down || true
                    
                    # 4. 启动服务
                    # 因为镜像已经在上面构建并推送了，这里可以直接 up，不需要 --build
                    # 如果你想用本地刚构建的镜像而不是拉取的，可以去掉 --pull always (如果有配置的话)
                    docker compose -f docker-compose.cicd.yml -p cicd-test up -d
                    
                    echo '✅ 部署完成！服务已在沙箱环境启动。'
                """
            }
        }
    }

    post {
        always {
            echo '清理工作区...'
            // 注意：cleanWs() 会清空 Jenkins 的工作区，但不会影响 /opt/cicd-sandbox
            cleanWs()
        }
        success {
            echo '🎉 CI/CD 流水线执行成功！'
        }
        failure {
            echo '❌ 部署失败，请检查上方日志。'
        }
    }
}
