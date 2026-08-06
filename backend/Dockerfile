# 1. 使用精简版的 Java 17 运行环境作为基础镜像
#FROM openjdk:17-jdk
FROM eclipse-temurin:17-jdk-alpine
# 2. 设置容器内的工作目录
WORKDIR /app

# 3. 将当前目录下的 jar 包复制到容器内，并改名为 app.jar
COPY target/Automatic_test_script-1.0-SNAPSHOT.jar app.jar
# 4. 声明应用监听的端口（文档说明作用）
EXPOSE 8080

# 5. 容器启动时执行的命令
ENTRYPOINT ["java", "-jar", "app.jar"]
