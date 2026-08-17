pipeline {
    agent any

    environment {
        // 不再推送到 DockerHub，仅本地保存
        BACKEND_IMAGE = 'myapp-backend:latest'
        FRONTEND_IMAGE = 'myapp-frontend:latest'
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
                dir('backend') {
                    // 1. 强制清理本地仓库缓存中的当前项目快照
                    sh 'mvn dependency:purge-local-repository -DmanualInclude=org.example:Automatic_test_script || true'

                    // 2. 重新构建
                    sh 'mvn clean package -DskipTests -U'

                    // 3. 验证 JAR 包内容
                    sh '''
                        echo ">>> 检查 JAR 包大小 <<<"
                        ls -lh target/Automatic_test_script-1.0-SNAPSHOT.jar

                        echo ">>> 检查 Main 类是否存在 <<<"
                        jar tf target/Automatic_test_script-1.0-SNAPSHOT.jar | grep "org/example/Main.class" || echo "ERROR: Main class NOT found!"
                    '''

                    // 4. 构建新镜像前，先销毁旧的本地镜像
                    sh '''
                        echo ">>> 清理旧的后端镜像 <<<"
                        docker rmi -f myapp-backend:latest 2>/dev/null || true
                    '''

                    // 5. 构建新的本地镜像
                    sh "docker build -t ${BACKEND_IMAGE} ."
                }
            }
        }

        stage('3. 标记前端镜像') {
            steps {
                echo '前端使用官方 Nginx 基础镜像，无需本地构建...'
                sh '''
                    docker rmi -f myapp-frontend:latest 2>/dev/null || true
                    docker pull nginx:alpine && docker tag nginx:alpine myapp-frontend:latest
                '''
            }
        }

        stage('4. 本地部署（沙箱环境）') {
            steps {
                sh '''
                    # 1. 清理宿主机上的旧构建产物
                    rm -rf /opt/cicd-sandbox/backend/target
                    rm -rf /opt/cicd-sandbox/backend/Dockerfile

                    # 2. 复制新代码
                    cp -r ./ /opt/cicd-sandbox/

                    # 3. 创建 Docker Compose 中 Bind Mount 所需的宿主机目录
                    mkdir -p /opt/cicd-sandbox/cicd-data/mysql
                    mkdir -p /opt/cicd-sandbox/cicd-data/redis
                    mkdir -p /opt/cicd-sandbox/cicd-data/rabbitmq
                    mkdir -p /opt/cicd-sandbox/cicd-data/export
                    mkdir -p /opt/cicd-sandbox/cicd-data/nginx-logs
                    mkdir -p /opt/cicd-sandbox/mysql/initsql
                    mkdir -p /opt/cicd-sandbox/nginx/conf.d
                    mkdir -p /opt/cicd-sandbox/nginx/html

                    # 4. 进入目录并部署
                    cd /opt/cicd-sandbox
                    docker compose -f docker-compose.cicd.yml down
                    docker compose -f docker-compose.cicd.yml up -d --build
                '''
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
