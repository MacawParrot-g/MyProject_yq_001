# 第一阶段：构建 Vue 项目
FROM node:18-alpine AS builder
WORKDIR /app
COPY package*.json ./
RUN npm install
COPY . .
# 执行构建命令，产物会生成在 /app/dist 目录
RUN npm run build

# 第二阶段：使用 Nginx 提供静态文件服务
FROM nginx:1.21-alpine
# 从 builder 阶段复制构建产物到 Nginx 的默认网页目录
COPY --from=builder /app/dist /usr/share/nginx/html
# 复制测试环境专用的 Nginx 配置文件
COPY ../nginx/conf.d/nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 19302
