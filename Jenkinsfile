pipeline {
    agent any

    environment {
        // === 后端配置 ===
        BACKEND_IMAGE = 'myapp-backend:latest'
        JAR_NAME = 'Automatic_test_script-1.0-SNAPSHOT.jar'
        
        // === 部署配置 ===
        DEPLOY_DIR = '/opt/traffic-data-system-YQ5287476'
        COMPOSE_FILE = 'docker-compose.cicd.yml'
    }

    stages {
        stage('1. 拉取代码') {
            steps {
                echo '>>> 强制清理 Jenkins 工作区的所有改动...'
                
                // 1. 强制删除所有未被 Git 追踪的文件和目录
                sh 'git clean -fdx'
                
                // 2. 强制重置所有已被 Git 追踪的文件到上一次提交的状态，放弃所有本地修改
                sh 'git reset --hard HEAD'
                
                echo '>>> 正在从 GitHub 强制拉取最新代码...'
                // 3. 再次执行拉取，此时工作区是绝对干净的，Git 会被迫更新到最新版本
                checkout scm
                
                echo '>>> 打印当前拉取的代码版本！'
                // 4. 打印最新一次提交的哈希值和日志，这是验证拉取是否成功的铁证
                sh '''
                    echo "=== 最新提交信息 ==="
                    git log -1 --format=fuller
                    echo "===================="
                '''
            }
        }

        stage('2. 后端构建与镜像打包') {
            steps {
                dir('backend') {
                    echo '>>> 【强制清理】删除旧的构建产物...'
                    sh 'rm -rf target'
                    
                    echo '>>> 开始构建后端...'
                    sh 'mvn clean package -DskipTests -U'

                    echo '>>> 检查 JAR 包是否真的生成成功了！'
                    sh "ls -lh target/${JAR_NAME}"

                    echo '>>> 强制删除旧镜像...'
                    sh "docker rmi -f ${BACKEND_IMAGE} || true"

                    echo '>>> 强制从头构建...'
                    sh "docker build --no-cache --pull -t ${BACKEND_IMAGE} ."

                }
            }
        }

        stage('3. 前端构建 (Vite)') {
    tools {
        nodejs 'nodejs' 
    }
    steps {
        dir('frontend') {
            echo '>>> 删除旧的前端构建产物，防止文件堆积...'
            sh 'rm -rf ../nginx/html/dist/*'
            
            echo '>>> 开始安装前端依赖并构建...'
            sh 'npm install'
            sh 'npm run build'
            
            echo '>>> 检查 dist 目录是否存在...'
            sh '''
                if [ ! -d "dist" ]; then
                    echo "错误：dist 目录不存在！"
                    ls -la
                    exit 1
                fi
                ls -la dist/
            '''
            
            echo '>>> 将构建产物复制到 Nginx 挂载目录...'
            sh 'mkdir -p ../nginx/html/dist'
            sh 'cp -rf dist/* ../nginx/html/dist/'
            
            echo '>>> 验证构建产物是否存在...'
            sh 'ls -lh ../nginx/html/dist/'
        }
    }
}

        stage('4. 部署') {
    steps {
        echo '>>> 准备部署目录...'
        sh """
            mkdir -p ${DEPLOY_DIR}/persistent-data/{mysql,redis,rabbitmq,export,nginx-logs}
            mkdir -p ${DEPLOY_DIR}/mysql/initsql
            
            cp -f docker-compose.cicd.yml ${DEPLOY_DIR}/
            # 只复制 Nginx 配置文件，不再复制 html 文件
            cp -rf nginx/conf.d ${DEPLOY_DIR}/nginx/ 2>/dev/null || true
            cp -f nginx/nginx.conf ${DEPLOY_DIR}/nginx/ 2>/dev/null || true
            
            cp -rf mysql ${DEPLOY_DIR}/ 2>/dev/null || true
            cp -rf redis ${DEPLOY_DIR}/ 2>/dev/null || true
        """

        echo '>>> 从 Jenkins 凭据生成 .env 文件...'
        withCredentials([
            string(credentialsId: 'tds-mysql-root-pwd',  variable: 'MYSQL_ROOT_PWD'),
            string(credentialsId: 'tds-mysql-user-pwd',  variable: 'MYSQL_USER_PWD'),
            string(credentialsId: 'tds-rabbitmq-user',   variable: 'RABBITMQ_USER_VAL'),
            string(credentialsId: 'tds-rabbitmq-pwd',    variable: 'RABBITMQ_PWD')
        ]) {
            sh """
                cat > ${DEPLOY_DIR}/.env << ENVEOF
MYSQL_ROOT_PASSWORD=${MYSQL_ROOT_PWD}
MYSQL_USER=remote_user
MYSQL_PASSWORD=${MYSQL_USER_PWD}
RABBITMQ_USER=${RABBITMQ_USER_VAL}
RABBITMQ_PASSWORD=${RABBITMQ_PWD}
ENVEOF
                chmod 600 ${DEPLOY_DIR}/.env
            """
        }

        echo '>>> 构建新的 Nginx 镜像...'
        sh "ls -lh ${WORKSPACE}/nginx/html/dist/"
        // 指定 Dockerfile 路径
        sh "docker build --no-cache -f nginx/Dockerfile -t tds-nginx:latest .""

        echo '>>> 启动/重建所有服务...'
        dir("${DEPLOY_DIR}") {
            sh """
                # 清理旧日志
                rm -rf persistent-data/nginx-logs/*
                
                # 停止并移除旧容器
                docker stop tds-backend tds-nginx || true
                docker rm tds-backend tds-nginx || true
                
                # 用新镜像重建并启动所有服务
                docker compose -f ${COMPOSE_FILE} up -d
            """
        }

        echo '>>> 清理悬空镜像...'
        sh 'docker image prune -f'

        echo '>>> 前后端部署完成！'
    }
}
    }
    

    post {
        always {
            echo '>>> 【构建后清理】清理本地构建产物，避免磁盘膨胀...'
            // 删除本次构建生成的镜像，释放空间（保留基础镜像）
            echo '清理 Jenkins 工作区...'
            cleanWs()
        }
        failure {
            echo '流水线执行失败，请检查上方日志！'
        }
        success {
            echo '部署成功！前端已更新，后端已重建。'
        }
    }
}

