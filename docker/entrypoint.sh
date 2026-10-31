#!/bin/sh
# Jade Cart 容器入口: 自动生成密钥并持久化 → 启动后端 (+ nginx)
set -e

ENV_FILE=/app/data/.env
mkdir -p /app/data

# 密钥自动生成 + 持久化 (重启不丢; 与 jade-wiki entrypoint 惯例一致)
if [ -f "$ENV_FILE" ]; then
  set -a; . "$ENV_FILE"; set +a
fi
if [ -z "$JADE_JWT_SECRET" ]; then
  JADE_JWT_SECRET="$(head -c 48 /dev/urandom | base64 | tr -d '\n')"
  echo "JADE_JWT_SECRET=$JADE_JWT_SECRET" >> "$ENV_FILE"
  echo "[entrypoint] generated JADE_JWT_SECRET -> $ENV_FILE"
fi
if [ -z "$JADE_CRYPTO_MASTER_KEY" ]; then
  # AES-256 需要 >=32 字节
  JADE_CRYPTO_MASTER_KEY="$(head -c 48 /dev/urandom | base64 | tr -d '\n')"
  echo "JADE_CRYPTO_MASTER_KEY=$JADE_CRYPTO_MASTER_KEY" >> "$ENV_FILE"
  echo "[entrypoint] generated JADE_CRYPTO_MASTER_KEY -> $ENV_FILE"
fi
export JADE_JWT_SECRET JADE_CRYPTO_MASTER_KEY

export JADE_JWT_EXPIRE_SECONDS="${JADE_JWT_EXPIRE_SECONDS:-7200}"
export JADE_JWT_ISSUER="${JADE_JWT_ISSUER:-jade-cart}"

MODE="${1:-all-in-one}"

case "$MODE" in
  api)
    exec java ${JAVA_OPTS:--Xmx512m} -jar /app/quarkus-run.jar
    ;;
  all-in-one)
    java ${JAVA_OPTS:--Xmx512m} -jar /app/quarkus-run.jar &
    # 等 /q/health 就绪再放行 nginx (最多 90s)
    for i in $(seq 1 90); do
      if curl -fsS http://127.0.0.1:8090/q/health >/dev/null 2>&1; then
        echo "[entrypoint] backend ready (${i}s)"
        break
      fi
      sleep 1
    done
    exec nginx -g 'daemon off;'
    ;;
  *)
    echo "usage: entrypoint.sh [api|all-in-one]"; exit 1
    ;;
esac
