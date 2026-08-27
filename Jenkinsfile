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
                    # 1. 确保所有必要的目录存在（如果不存在就创建，存在就保留）
                    mkdir -p ${DEPLOY_DIR}/cicd-data/{mysql,redis,rabbitmq,export,nginx-logs}
                    mkdir -p ${DEPLOY_DIR}/mysql/initsql
                    mkdir -p ${DEPLOY_DIR}/nginx/html/dist
                    
                    # 2. 复制必要的文件（cp -rf 会自动覆盖同名文件，绝对安全）
                    cp -f docker-compose.cicd.yml ${DEPLOY_DIR}/
                    cp -f .env ${DEPLOY_DIR}/ 2>/dev/null || true
                    
                    # 复制后端
                    cp -rf backend ${DEPLOY_DIR}/
                    
                    # 复制前端构建产物
                    cp -rf nginx/html/dist/* ${DEPLOY_DIR}/nginx/html/dist/
                    
                    # 复制 Nginx 配置
                    cp -rf nginx/conf.d ${DEPLOY_DIR}/nginx/ 2>/dev/null || true
                    cp -f nginx/nginx.conf ${DEPLOY_DIR}/nginx/ 2>/dev/null || true
                    
                    # 复制数据库和缓存配置
                    cp -rf mysql ${DEPLOY_DIR}/ 2>/dev/null || true
                    cp -rf redis ${DEPLOY_DIR}/ 2>/dev/null || true
                    cp -f data/* ${DEPLOY_DIR}/data/ 2>/dev/null || true
                    
                    # 3. 验证前端产物是否真的存在
                    ls -lh ${DEPLOY_DIR}/nginx/html/dist/
                """
                
                echo '已经将前端部分打包进nginx'

                echo '>>> 启动服务...'
                dir("${DEPLOY_DIR}") {
                    sh """
                        # 清理旧的 Nginx 日志缓存
                        rm -rf cicd-data/nginx-logs/*
                        # 强制不使用缓存构建镜像，并重建容器
                        docker compose -f ${COMPOSE_FILE} build --no-cache
                        docker compose -f ${COMPOSE_FILE} up -d --force-recreate
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
