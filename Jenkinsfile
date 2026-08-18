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
                    # 1. 彻底清理旧的部署文件（保留数据卷目录）
                    rm -rf ${DEPLOY_DIR}
                    mkdir -p ${DEPLOY_DIR}

                    # 2. 复制当前最新代码（包含刚构建好的 nginx/html/dist）
                    # 注意：这里复制的是整个工作空间，所以包含了 backend, frontend, nginx 等所有文件夹
                    cp -r ./* ${DEPLOY_DIR}/
                    cp -r ./.??* ${DEPLOY_DIR}/ 2>/dev/null || true # 复制隐藏文件如 .env

                    # 3. 创建必要的数据持久化目录
                    mkdir -p ${DEPLOY_DIR}/cicd-data/{mysql,redis,rabbitmq,export,nginx-logs}
                    mkdir -p ${DEPLOY_DIR}/mysql/initsql
                    
                    # 4. 确保 nginx 挂载点存在且包含构建产物
                    # 虽然 cp -r 已经复制了，但为了保险再次确认
                    ls -lh ${DEPLOY_DIR}/nginx/html/dist/
                """

                echo '>>> 启动服务...'
                dir("${DEPLOY_DIR}") {
                    sh """
                        docker compose -f ${COMPOSE_FILE} down || true
                        docker compose -f ${COMPOSE_FILE} up -d --build
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
