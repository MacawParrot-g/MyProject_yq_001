# 构建阶段
FROM node:18-alpine AS builder
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
RUN npm run build

# 运行阶段
FROM nginx:1.21-alpine
RUN rm /etc/nginx/conf.d/default.conf
COPY nginx/conf.d/ /etc/nginx/conf.d/
COPY --from=builder /app/dist /usr/share/nginx/html/
