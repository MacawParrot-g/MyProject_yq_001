FROM nginx:1.21-alpine

RUN rm /etc/nginx/conf.d/default.conf

# 修改前: COPY conf.d/ /etc/nginx/conf.d/
# 修改后:
COPY nginx/conf.d/ /etc/nginx/conf.d/

# 修改前: COPY html/dist/ /usr/share/nginx/html/
# 修改后:
COPY nginx/html/dist/ /usr/share/nginx/html/
