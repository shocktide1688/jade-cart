# =====================================================
# Jade Cart 云原生镜像 (jade-wiki 双 target 惯例)
#   --target api        纯后端 (JVM, quarkus-run)
#   --target all-in-one 单容器: JVM 后端 + nginx 前端 SPA (单端口 8080)
#
# 前置: 仓库根目录需有构建产物 (CI 或本地先产出):
#   backend/jade-cart-app/target/quarkus-app/   (mvn package)
#   frontend/dist/                              (npm run build)
#
# 构建:  docker build -f docker/Dockerfile --target all-in-one -t jade-cart:aio .
# 运行:  docker run -d -p 8080:8080 -v cart-data:/app/data \
#          -e DB_URL=jdbc:postgresql://host:5432/jade_cart \
#          -e REDIS_URL=redis://host:6379 jade-cart:aio
# 密钥:  JADE_JWT_SECRET / JADE_CRYPTO_MASTER_KEY 未提供时自动生成并持久化
#        到 /app/data/.env (重启不丢, 与 jade-wiki entrypoint 惯例一致)
# =====================================================

# ---------- 共享基础: JRE (alpine/musl, all-in-one 同容器跑 nginx) ----------
FROM eclipse-temurin:21-jre-alpine AS base
RUN apk add --no-cache nginx curl tini && mkdir -p /app /usr/share/nginx/html
WORKDIR /app

# ---------- api: 纯后端 ----------
FROM base AS api
COPY docker/entrypoint.sh /entrypoint.sh
RUN chmod +x /entrypoint.sh
COPY backend/jade-cart-app/target/quarkus-app/ /app/
EXPOSE 8090
ENV JADE_CART_HTTP_PORT=8090
ENTRYPOINT ["/sbin/tini", "--", "/entrypoint.sh", "api"]

# ---------- all-in-one: 后端 + 前端 SPA (单端口) ----------
FROM base AS all-in-one
COPY docker/entrypoint.sh /entrypoint.sh
RUN chmod +x /entrypoint.sh
COPY backend/jade-cart-app/target/quarkus-app/ /app/
COPY frontend/dist/ /usr/share/nginx/html/
COPY docker/nginx-all-in-one.conf /etc/nginx/http.d/default.conf
EXPOSE 8080
ENTRYPOINT ["/sbin/tini", "--", "/entrypoint.sh", "all-in-one"]
