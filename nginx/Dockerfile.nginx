FROM nginx:1.21-alpine

RUN rm /etc/nginx/conf.d/default.conf

COPY nginx/conf.d/ /etc/nginx/conf.d/
COPY nginx/html/dist/ /usr/share/nginx/html/
