# 用户门户镜像：npm 构建 -> Nginx 托管
FROM node:22-alpine AS build
WORKDIR /app

COPY portal/package.json ./
RUN npm install --no-audit --no-fund

COPY portal/ ./
RUN npm run build

FROM nginx:1.27-alpine
COPY deploy/nginx/portal.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html

EXPOSE 80
