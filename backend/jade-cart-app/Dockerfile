# Jade Cart (SQLite 构建版, QUARKUS_PROFILE=sqlite 固化)
# 构建: mvn package -DskipTests -Dquarkus.profile=sqlite -pl jade-cart-app -am
#       docker buildx build --platform linux/amd64 -t jade-cart:sqlite --load .
FROM eclipse-temurin:21-jre-noble

WORKDIR /app

# Quarkus fast-jar 目录整体拷贝 (app/ lib/ quarkus/)
COPY target/quarkus-app /app/quarkus-app

# 数据目录: SQLite 库文件 + 媒体 + 备份 (挂卷持久化)
RUN mkdir -p /app/data && \
    chown -R 185:0 /app && chmod -R g+rwX /app
VOLUME /app/data

ENV JAVA_OPTS="-Xmx512m -Xms128m -XX:+UseSerialGC -XX:MaxMetaspaceSize=160m"

EXPOSE 8090
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar quarkus-app/quarkus-run.jar"]
