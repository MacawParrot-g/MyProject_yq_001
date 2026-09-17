# nginx/Dockerfile
FROM nginx:1.21-alpine

# 删除默认的 Nginx 配置文件
RUN rm /etc/nginx/conf.d/default.conf

# 复制你本地的自定义配置（假设 conf.d 目录在 nginx 文件夹下）
COPY conf.d/ /etc/nginx/conf.d/

# 复制前端 Vite 构建好的 dist 产物（假设前端构建后 dist 在 nginx/html 下）
COPY html/dist/ /usr/share/nginx/html/
