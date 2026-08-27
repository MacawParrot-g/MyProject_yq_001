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
            # 1. 确保目录存在
            mkdir -p ${DEPLOY_DIR}
            mkdir -p ${DEPLOY_DIR}/cicd-data/{mysql,redis,rabbitmq,export,nginx-logs}
            mkdir -p ${DEPLOY_DIR}/mysql/initsql
            
            # 2. 清理旧代码（保留数据目录）
            # 保留 cicd-data 目录，删除其他所有内容
            cd ${DEPLOY_DIR}
            find . -maxdepth 1 ! -name 'cicd-data' ! -name '.' -exec rm -rf {} + 2>/dev/null || true
            cd -
            
            # 3. 复制必要的文件
            # 复制 docker-compose 文件
            cp -f docker-compose.cicd.yml ${DEPLOY_DIR}/
            cp -f .env ${DEPLOY_DIR}/ 2>/dev/null || true
            
            # 复制后端
            cp -rf backend ${DEPLOY_DIR}/
            
             # 复制前端构建产物
            mkdir -p ${DEPLOY_DIR}/nginx/html/dist
            # 尝试清理旧文件（即使没权限也没关系，Nginx 容器会兜底清理）
            rm -rf ${DEPLOY_DIR}/nginx/html/dist/* 2>/dev/null || true
            cp -rf nginx/html/dist/* ${DEPLOY_DIR}/nginx/html/dist/
            cp -rf nginx/conf.d ${DEPLOY_DIR}/nginx/ 2>/dev/null || true
            cp -f nginx/nginx.conf ${DEPLOY_DIR}/nginx/ 2>/dev/null || true
            
            # 复制 MySQL 初始化脚本
            cp -rf mysql ${DEPLOY_DIR}/ 2>/dev/null || true
            
            # 复制 Redis 配置
            cp -rf redis ${DEPLOY_DIR}/ 2>/dev/null || true
            
            # 复制其他必要的文件
            cp -f data/* ${DEPLOY_DIR}/data/ 2>/dev/null || true
            
            # 4. 验证产物
            ls -lh ${DEPLOY_DIR}/nginx/html/dist/
        """

        echo '>>> 启动服务...'
        dir("${DEPLOY_DIR}") {
           sh """
           rm -rf cicd-data/nginx-logs/*
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
