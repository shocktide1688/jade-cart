# Jade Cart 后端: 预构建 quarkus-run.jar 运行镜像
# 构建: cd backend && mvn clean install -DskipTests && docker build -f Dockerfile -t jade-cart-app:local .
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY jade-cart-app/target/quarkus-app/ /app/
EXPOSE 8090
ENV JADE_CART_HTTP_PORT=8090
ENTRYPOINT ["java", "-jar", "/app/quarkus-run.jar"]
