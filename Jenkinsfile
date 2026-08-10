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
        dir('backend') {
            // 1. 强制清理本地仓库缓存中的当前项目快照
            sh 'mvn dependency:purge-local-repository -DmanualInclude=org.example:Automatic_test_script || true'

            // 2. 重新构建
            sh 'mvn clean package -DskipTests -U' // -U 强制更新快照

            // 3. 【关键】验证 JAR 包内容
            sh '''
                echo ">>> 检查 JAR 包大小 <<<"
                ls -lh target/Automatic_test_script-1.0-SNAPSHOT.jar

                echo ">>> 检查 Main 类是否存在 <<<"
                jar tf target/Automatic_test_script-1.0-SNAPSHOT.jar | grep "org/example/Main.class" || echo "ERROR: Main class NOT found!"
            '''

            sh "docker build -t ${BACKEND_IMAGE} ."
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
        sh '''
            # 1. 清理宿主机上的旧构建产物
            rm -rf /opt/cicd-sandbox/backend/target
            rm -rf /opt/cicd-sandbox/backend/Dockerfile # 可选，确保完全干净

            # 2. 复制新代码
            cp -r ./ /opt/cicd-sandbox/

            # 3. 进入目录并部署
            cd /opt/cicd-sandbox
            docker compose -f docker-compose.cicd.yml down
            docker compose -f docker-compose.cicd.yml up -d --build # 强制重新构建
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
