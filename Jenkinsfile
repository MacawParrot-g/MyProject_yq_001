pipeline {
    agent any

    environment {
        DOCKERHUB_USER = 'macawparrot'
        BACKEND_IMAGE = "${DOCKERHUB_USER}/myapp-backend:latest"
        FRONTEND_IMAGE = "${DOCKERHUB_USER}/myapp-frontend:latest"
        DEPLOY_DIR = '/opt/cicd-sandbox'
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
                    // 【修复1】增加调试命令，确认 JAR 包是否生成及名称是否正确
                    sh 'mvn clean package -DskipTests'
                    sh 'ls -la target/*.jar'
                    // 【修复2】增加容错判断，避免 JAR 包不存在时继续执行 docker build
                    sh '''
                        if [ ! -f target/Automatic_test_script-1.0-SNAPSHOT.jar ]; then
                            echo "❌ JAR包不存在，请检查 pom.xml 的 finalName 配置"
                            exit 1
                        fi
                        docker build -t ${BACKEND_IMAGE} .
                    '''
                }
            }
        }

        stage('3. 构建前端/Nginx镜像') {
            steps {
                echo '正在构建前端 (Nginx)...'
            }
        }

        // stage('4. 推送镜像到 Docker Hub') {
        //     steps {
        //         echo '正在推送镜像到 Docker Hub...'
        //         withCredentials([usernamePassword(credentialsId: 'docker-hub-credentials', usernameVariable: 'DOCKERHUB_USER', passwordVariable: 'DOCKERHUB_PASSWORD')]) {
        //             sh "docker login -u ${DOCKERHUB_USER} -p '${DOCKERHUB_PASSWORD}'"
        //             sh "docker push ${BACKEND_IMAGE}"
        //             sh "docker push ${FRONTEND_IMAGE}"
        //         }
        //     }
        // }

        stage('4. 推送镜像到 Docker Hub') {
    steps {
        echo '正在推送镜像到 Docker Hub...'
        // 建议使用不同的变量名，避免与环境变量混淆
        withCredentials([usernamePassword(credentialsId: 'docker-hub-credentials', usernameVariable: 'DH_USER', passwordVariable: 'DH_PASS')]) {
            // 使用 --password-stdin 更安全，防止密码在进程列表中泄露
            sh "echo ${DH_PASS} | docker login -u ${DH_USER} --password-stdin"
            sh "docker push ${BACKEND_IMAGE}"
            sh "docker push ${FRONTEND_IMAGE}"
        }
    }
}

        stage('5. 本地部署（沙箱环境）') {
            steps {
                sh """
                    mkdir -p ${DEPLOY_DIR}
                    cp docker-compose.cicd.yml ${DEPLOY_DIR}/docker-compose.yml
                    cd ${DEPLOY_DIR}
                    # 【修复3】修正语法错误：移除嵌套 sh，修正文件名拼写
                    docker compose -f docker-compose.cicd.yml -p cicd-test up -d --build
                    echo '部署完成！服务已在沙箱环境启动，未暴露端口。'
                """
            }
        }
    }

    post {
        always {
            echo '清理工作区...'
            cleanWs()
        }
        success {
            echo 'CI/CD 流水线执行成功！'
        }
        failure {
            echo '部署失败，请检查日志。'
        }
    }
}
