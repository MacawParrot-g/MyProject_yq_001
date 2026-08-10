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

        stage('3. 标记前端镜像') {
    steps {
        echo '前端使用官方 Nginx 基础镜像，无需本地构建...'
        // 拉取一个干净的 nginx 镜像并打上你的标签，供 compose 使用
        sh 'docker pull nginx:alpine && docker tag nginx:alpine macawparrot/myapp-frontend:latest'
    }
}

        // stage('4. 推送镜像到 Docker Hub') {
        //     steps {
        //         echo '正在推送镜像...'
        //         withCredentials([usernamePassword(credentialsId: 'docker-hub-credentials', usernameVariable: 'DOCKER_USER', passwordVariable: 'DOCKER_PASS')]) {
        //             sh """
        //                 echo "$DOCKER_PASS" | docker login -u "$DOCKER_USER" --password-stdin
        //                 docker push ${BACKEND_IMAGE}
        //                 docker push ${FRONTEND_IMAGE}
        //                 docker logout
        //             """
        //         }
        //     }
        // }

        stage('5. 本地部署（沙箱环境）') {
    steps {
        echo "正在部署到 ${DEPLOY_DIR}..."
        sh """
            # 1. 创建部署目录
            mkdir -p ${DEPLOY_DIR}
            
            # 2. 【修改点】使用 cp 替代 rsync
            # -r: 递归复制目录
            # --exclude: 排除 .git 文件夹
            # 注意：cp 的 exclude 语法和 rsync 不同，这里用 find + cp 或者简单的 cp -r 后删除
            # 最简单的做法是直接复制，因为 .git 在容器里也不大，或者用 tar 过滤
            
            # 方法 A：直接复制（如果 .git 不大，推荐这个，最快）
            cp -r ./ ${DEPLOY_DIR}/
            
            # 方法 B：如果你非常介意 .git 目录，可以用 tar 管道过滤（更专业）
            # tar --exclude='.git' -c . | tar -x -C ${DEPLOY_DIR}/

            # 3. 进入部署目录并启动
            cd ${DEPLOY_DIR}
            
            # 停止旧容器
            docker compose -f docker-compose.cicd.yml down || true
            
            # 启动新容器
            docker compose -f docker-compose.cicd.yml up -d
            
            echo "部署完成！"
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
