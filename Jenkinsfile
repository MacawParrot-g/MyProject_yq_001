pipeline {
    agent any

    environment {
        // === 后端配置 ===
        BACKEND_IMAGE = 'myapp-backend:latest'
        JAR_NAME = 'Automatic_test_script-1.0-SNAPSHOT.jar'
        
        // === 部署配置 ===
        DEPLOY_DIR = '/opt/cicd-sandbox'
        COMPOSE_FILE = 'docker-compose.cicd.yml'
    }

    tools {
        // 确保 Jenkins 全局工具配置里有名为 'nodejs' 的 NodeJS 版本
        nodejs 'nodejs' 
    }

    stages {
        stage('1. 拉取代码') {
            steps {
                echo '正在从 GitHub 拉取最新代码...'
                checkout scm
            }
        }

        stage('2. 后端构建与镜像打包') {
            steps {
                dir('backend') {
                    echo '>>> 开始清理并构建后端...'
                    // 强制更新快照依赖并打包
                    sh 'mvn clean package -DskipTests -U'

                    echo '>>> 验证 JAR 包...'
                    sh "ls -lh target/${JAR_NAME}"

                    echo '>>> 重建后端 Docker 镜像...'
                    sh """
                        docker rmi -f ${BACKEND_IMAGE} || true
                        docker build -t ${BACKEND_IMAGE} .
                    """
                }
            }
        }

        stage('3. 前端构建 (Vite)') {
            steps {
                dir('frontend') {
                    echo '>>> 开始安装前端依赖并构建...'
                    
                    // 1. 安装依赖
                    sh 'npm install'
                    
                    // 2. 执行构建
                    // 注意：这里不需要指定输出目录，因为 vite.config.js 里已经配好了 outDir: '../nginx/html/dist'
                    sh 'npm run build'

                    echo '>>> 验证构建产物是否存在...'
                    // 检查产物是否真的生成了
                    sh 'ls -lh ../nginx/html/dist/'
                }
            }
        }

         stage('4. 本地部署 (沙箱环境)') {
            steps {
                echo '>>> 准备部署目录...'
                sh """
                    # 1. 确保基础目录存在
                    mkdir -p ${DEPLOY_DIR}/cicd-data/{mysql,redis,rabbitmq,export,nginx-logs}
                    mkdir -p ${DEPLOY_DIR}/mysql/initsql
                    
                    # 2. 复制必要的配置文件（绝对不要往 backend/target 和 nginx/html/dist 里复制任何东西！）
                    cp -f docker-compose.cicd.yml ${DEPLOY_DIR}/
                    cp -f .env ${DEPLOY_DIR}/ 2>/dev/null || true
                    cp -rf nginx/conf.d ${DEPLOY_DIR}/nginx/ 2>/dev/null || true
                    cp -f nginx/nginx.conf ${DEPLOY_DIR}/nginx/ 2>/dev/null || true
                    cp -rf mysql ${DEPLOY_DIR}/ 2>/dev/null || true
                    cp -rf redis ${DEPLOY_DIR}/ 2>/dev/null || true
                    cp -f data/* ${DEPLOY_DIR}/data/ 2>/dev/null || true
                """

                echo '>>> 启动基础服务...'
                dir("${DEPLOY_DIR}") {
                    sh """
                        rm -rf cicd-data/nginx-logs/*
                        docker compose -f ${COMPOSE_FILE} up -d --force-recreate
                    """
                }

                echo '>>> 将前端代码注入到 Nginx 容器中...'
                sh """
                    sleep 3
                    docker cp nginx/html/dist/. cicd-nginx:/usr/share/nginx/html/dist/
                    docker cp nginx/conf.d/. cicd-nginx:/etc/nginx/conf.d/
                    docker exec cicd-nginx chown -R 101:101 /usr/share/nginx/html
                    docker exec cicd-nginx nginx -s reload
                """
                
                echo '已经将前端部分打包进nginx'

                echo '>>> 将后端代码注入到 Backend 容器中...'
                sh """
                    sleep 3
                    # 直接将后端产物复制到 Backend 容器内部！彻底绕过宿主机的挂载吞噬！
                    docker cp backend/. cicd-backend:/app/
                    
                    # 修复容器内的文件权限，防止后端启动报权限错误
                    docker exec cicd-backend chown -R root:root /app
                """
                
                echo '已经将后端部分打包进容器'

                echo '>>> 重启后端服务使新代码生效...'
                dir("${DEPLOY_DIR}") {
                    sh """
                        # 重启后端容器，让它加载刚注入的新 JAR 包
                        docker compose -f ${COMPOSE_FILE} restart backend
                    """
                }
            }
        }               
    }

    post {
        always {
            echo '清理 Jenkins 工作区...'
            cleanWs()
        }
        failure {
            echo '❌ 流水线执行失败，请检查上方日志！'
        }
        success {
            echo '✅ 部署成功！前端已更新，后端已重启。'
        }
    }
}
