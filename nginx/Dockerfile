FROM nginx:1.21-alpine

# 删除默认的 Nginx 配置文件，避免冲突
RUN rm /etc/nginx/conf.d/default.conf

# 注意这里加了 nginx/ 前缀，对应 Jenkins 工作区的根目录
COPY nginx/conf.d/ /etc/nginx/conf.d/

# 同样加了 nginx/ 前缀，确保 dist 目录被正确复制
COPY nginx/html/dist/ /usr/share/nginx/html/
