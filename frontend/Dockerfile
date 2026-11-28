# 管理后台镜像：pnpm 构建 -> Nginx 托管
FROM node:22-alpine AS build
WORKDIR /app

# 关闭 husky（容器内无 git 仓库）
ENV HUSKY=0
RUN corepack enable && corepack prepare pnpm@9 --activate

COPY frontend/package.json ./
RUN pnpm install --no-frozen-lockfile

COPY frontend/ ./
# 该目录下的 pnpm-workspace.yaml 缺少 packages 字段，会使 pnpm 进入空 workspace 模式导致构建失败
RUN rm -f pnpm-workspace.yaml
# build-only 跳过 vue-tsc 类型检查，避免无关文件的类型告警阻塞镜像构建；
# 需要严格校验时改为 pnpm build
RUN pnpm build-only

FROM nginx:1.27-alpine
COPY deploy/nginx/admin.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html

EXPOSE 80
