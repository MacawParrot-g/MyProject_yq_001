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
            
            # 2. 使用 rsync 增量同步（只更新变化的文件）
            # 注意：排除数据目录，避免覆盖已有数据
            rsync -av --exclude='cicd-data' --exclude='.git' ./* ${DEPLOY_DIR}/
            
            # 3. 验证产物
            ls -lh ${DEPLOY_DIR}/nginx/html/dist/
        """

        echo '>>> 重启服务...'
        dir("${DEPLOY_DIR}") {
            sh """
                docker compose -f ${COMPOSE_FILE} up -d --build --force-recreate
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
