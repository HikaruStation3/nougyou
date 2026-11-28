# 后端服务镜像：多阶段构建（Maven 打包 -> JRE 运行）
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

# 国内镜像源，避免拉取依赖超时
COPY deploy/maven/settings-docker.xml /root/.m2/settings.xml

# 先拷 pom 以复用依赖下载层，再拷源码
COPY backend/pom.xml ./

COPY backend/src ./src
# 使用 BuildKit 缓存挂载复用本地仓库，网络中断后重跑可断点续传
RUN --mount=type=cache,target=/root/.m2/repository \
    mvn -B -DskipTests \
    -Dmaven.wagon.http.retryHandler.count=5 \
    -Dmaven.wagon.httpconnectionManager.ttlSeconds=60 \
    package

FROM eclipse-temurin:21-jre
WORKDIR /app
ENV TZ=Asia/Shanghai \
    JAVA_OPTS="-Xms256m -Xmx512m"

COPY --from=build /build/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
