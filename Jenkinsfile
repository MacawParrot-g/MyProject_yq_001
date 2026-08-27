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
                echo '>>> 【强制清洗】彻底清空 Jenkins 工作区，防止旧代码污染...'
                // 删除当前工作区下的所有文件和隐藏文件（除了 .git 目录，保留 git 配置以加快后续拉取）
                sh 'rm -rf * .[!.]* ..?*'
                
                echo '>>> 正在从 GitHub 强制拉取最新代码...'
                checkout scm
                
                echo '>>> 【关键验证】打印当前拉取的代码版本，确认是否为最新...'
                sh 'git log -1'
            }
        }
        

               stage('2. 后端构建与镜像打包') {
            steps {
                dir('backend') {
                    echo '>>> 【强制清理】删除旧的构建产物...'
                    // 彻底清空 target 目录，确保没有任何旧文件残留
                    sh 'rm -rf target/*'
                    
                    echo '>>> 开始清理并构建后端...'
                    sh 'mvn clean package -DskipTests -U'

                    echo '>>> 验证 JAR 包...'
                    sh "ls -lh target/${JAR_NAME}"

                    echo '>>> 【清理旧镜像】发现旧镜像立刻销毁...'
                    sh "docker rmi -f ${BACKEND_IMAGE} || true"

                    echo '>>> 【重新构建】强制不使用缓存，从头构建新镜像...'
                    sh "docker build --no-cache -t ${BACKEND_IMAGE} ."
                }
            }
        }
        stage('3. 前端构建 (Vite)') {
            steps {
                dir('frontend') {
                    echo '>>> 开始安装前端依赖并构建...'
                    sh 'npm install'
                    sh 'npm run build'
                    echo '>>> 验证构建产物是否存在...'
                    sh 'ls -lh ../nginx/html/dist/'
                }
            }
        }

                stage('4. 本地部署 (沙箱环境)') {
            steps {
                echo '>>> 准备部署目录...'
                sh """
                    mkdir -p ${DEPLOY_DIR}/cicd-data/{mysql,redis,rabbitmq,export,nginx-logs}
                    mkdir -p ${DEPLOY_DIR}/mysql/initsql
                    
                    # 只复制配置文件
                    cp -f docker-compose.cicd.yml ${DEPLOY_DIR}/
                    cp -f .env ${DEPLOY_DIR}/ 2>/dev/null || true
                    cp -rf nginx/conf.d ${DEPLOY_DIR}/nginx/ 2>/dev/null || true
                    cp -f nginx/nginx.conf ${DEPLOY_DIR}/nginx/ 2>/dev/null || true
                    cp -rf mysql ${DEPLOY_DIR}/ 2>/dev/null || true
                    cp -rf redis ${DEPLOY_DIR}/ 2>/dev/null || true
                    cp -f data/* ${DEPLOY_DIR}/data/ 2>/dev/null || true
                """

                echo '>>> 启动/重建所有服务...'
                dir("${DEPLOY_DIR}") {
                    sh """
                        rm -rf cicd-data/nginx-logs/*
                        # 后端镜像已在 Stage 2 强制重建，这里直接拉起新容器
                        docker compose -f ${COMPOSE_FILE} up -d --force-recreate
                    """
                }

                echo '>>> 将前端配置注入到 Nginx 容器中...'
                sh """
                    sleep 3
                    docker cp nginx/html/dist/. cicd-nginx:/usr/share/nginx/html/dist/
                    docker cp nginx/conf.d/. cicd-nginx:/etc/nginx/conf.d/
                    docker exec cicd-nginx chown -R 101:101 /usr/share/nginx/html
                    docker exec cicd-nginx nginx -s reload
                """
                
                echo '✅ 前后端部署完成！'
            }
        }
    }

    post {
        always {
            echo '>>> 【构建后清理】清理本地构建产物，避免磁盘膨胀...'
            // 删除本次构建生成的镜像，释放空间（保留基础镜像）
            sh "docker rmi -f ${BACKEND_IMAGE} || true"
            
            echo '清理 Jenkins 工作区...'
            cleanWs()
        }
        failure {
            echo '❌ 流水线执行失败，请检查上方日志！'
        }
        success {
            echo '✅ 部署成功！前端已更新，后端已重建。'
        }
    }
}
