# 开发流程 (jade-cart 多店体系)

> 架构: jade-cart = 开源通用引擎; teawone / sinivora = 同一镜像的两个独立部署实例 (差异全在环境变量 + 数据)。
> 分工决策树: 通用功能/bugfix → 改 jade-cart 代码; 店铺内容(商品/博客/主题) → 直接生产管理后台操作;
> 店铺专属代码逻辑 → 未来建 jade-brands 私有覆盖层 (出现第一个品牌专属代码需求时再建)。

## 一、三套环境

| 环境 | 位置 | 数据库 | 用途 |
|---|---|---|---|
| 本地开发 | Mac, localhost:8090 (后端) + :5173 (前端) | PG (Docker `jade-postgres`:5432, 库 jade_cart) + Redis (`jade-redis`:6380) | 写代码调试 |
| teawone 生产 | https://shop.teawone.com + shop-admin.teawone.com | 服务器 SQLite `/opt/teawone/data/jade_cart.db` | 正式运营 |
| sinivora 生产 | https://sinivora.teawone.com + sinivora-admin.store.teawone.com | 服务器 SQLite `/opt/sinivora/data/jade_cart.db` | 正式运营 |

⚠️ 本地 PG 与生产 SQLite 已分家 (11-05 同步后各自演化), 不要假设数据一致。
⚠️ 本地后端用**默认 profile 构建** (PG); 服务器镜像用 **sqlite profile 构建** (`-Dquarkus.profile=sqlite`, db-kind 构建期固化)。

## 二、本地环境启动

```bash
# 前置容器 (通常已在跑)
docker start jade-postgres jade-redis

# 后端 (默认 PG profile; jar 若是 sqlite 构建版需先重构建)
cd backend
mvn package -DskipTests -pl jade-cart-app -am        # 不带 quarkus.profile = PG 版
java -jar jade-cart-app/target/quarkus-app/quarkus-run.jar   # 监听 8090, 工作目录须在 backend/

# 前端
cd frontend && npm run dev    # localhost:5173, 代理 /api /media /q → 8090
```

## 三、场景 A: 通用功能开发 (开源核心)

```
1. 本地写代码 + localhost:5173 调试 (PG 数据)
2. 测试: cd backend && mvn test            # 需要 PG(5432) + Redis(6380)
3. 前端检查: cd frontend && npm run lint && npm run type-check
4. git commit (Conventional Commits 中文)
5. 构建部署 (两店同镜像, 都要更新, 见 §五)
6. 生产验证 (两店域名各走一遍)
```

新增数据库迁移注意: **PG 和 SQLite 双方言都要写**
- PG 版: `backend/jade-cart-core/.../db/migration/postgresql/V1xx__xxx.sql`
- SQLite 版: 同目录 `sqlite/V1xx__xxx.sql` (BIGSERIAL→INTEGER, TIMESTAMPTZ→TIMESTAMP,
  NOW()→CURRENT_TIMESTAMP, 无 COMMENT ON/DROP CASCADE/setval/ADD COLUMN IF NOT EXISTS)
- 时间列必须 TIMESTAMP (NUMERIC 亲和), 禁止 TEXT (xerial epoch 毫秒读写不对称)
- 迁移与配套代码同一 commit (flyway-at-start, 自动部署时不允许孤儿迁移)

## 四、场景 B: 店铺内容运营 (零代码)

直接在对应管理后台操作, 数据落各店自己的 SQLite:

| 内容 | teawone | sinivora |
|---|---|---|
| 商品/分类/库存 | shop-admin.teawone.com → 商城管理 | sinivora-admin.store.teawone.com → 商城管理 |
| 博客/内容页 | 同上 → 内容管理/博客管理 | 同上 |
| 主题 (预设+覆盖) | 商城管理 → 店面装修 | 同上 |
| 市场设置 | 商城设置 (KV, AES 加密) | 同上 |
| 语言 | 店面右上角 EN/DE/FR | 同上 |
| LDAP 用户 | 统一目录: jade-ldap (ldap.store.teawone.com 管理) | 同一目录, 用户两店通用 |

## 五、构建部署 (手动阶段, 未来 Drone 自动化)

```bash
# ① 后端镜像 (sqlite profile 固化)
cd backend && mvn package -DskipTests -Dquarkus.profile=sqlite -pl jade-cart-app -am
cd jade-cart-app && docker buildx build --platform linux/amd64 -t jade-cart:sqlite --load .
docker save jade-cart:sqlite | gzip > /tmp/img.tar.gz
scp /tmp/img.tar.gz root@45.32.83.6:/tmp/

# ② 服务器更新两店 (IP 已固定, Lucky 上游不受影响)
ssh root@45.32.83.6 '
  docker load -i /tmp/img.tar.gz && rm /tmp/img.tar.gz
  cd /opt/teawone && docker compose up -d --force-recreate app      # .env: APP_IMAGE
  docker rm -f sinivora-app && docker run -d --name sinivora-app \
    --restart unless-stopped --network jade-cart_default --ip 172.18.0.8 \
    --env-file /opt/sinivora/sinivora.env -p 8091:8090 \
    -v /opt/sinivora/data:/app/data jade-cart:sqlite'

# ③ 管理端前端 (teawone 通用版 / sinivora 品牌版分开构建)
cd frontend && npm run build && tar czf /tmp/d.tar.gz -C dist .
scp /tmp/d.tar.gz root@45.32.83.6:/tmp/
ssh root@45.32.83.6 'rm -rf /opt/teawone/admin-dist/* && tar xzf /tmp/d.tar.gz -C /opt/teawone/admin-dist && rm /tmp/d.tar.gz'
# sinivora 版: VITE_APP_TITLE="Sinivora Admin" npm run build → 同法更新 /opt/sinivora/admin-dist

# ④ 验证
curl -s -o /dev/null -w "%{http_code}\n" https://shop.teawone.com/
curl -s -o /dev/null -w "%{http_code}\n" https://sinivora.teawone.com/
```

容器 IP 已固定 (Lucky 上游依赖): redis=.2 app=.3 lucky=.4 admin-web=.5 ldap-web=.6
jade-ldap=.7 sinivora-app=.8 sinivora-admin-web=.9 — **重建容器必须保持这些 IP**。

## 六、未来 CI/CD (已定案: Gitea + Drone + 华为云 SWR)

```
git push → Drone: 内核流水线 (test/构建基础镜像 → push SWR)
         → 品牌流水线 (FROM 基础镜像 + 品牌层 → push SWR)
         → 部署 job (SSH: 备份 DB → 拉新 tag → compose up → health 探活)
```
前置条件已完成: 容器 IP 固定 + compose 支持 `APP_IMAGE` 变量 (Drone 改 tag 即部署)。

## 七、服务器目录速查

```
/opt/teawone/    docker-compose.yml + .env(APP_IMAGE/密钥) + data/(SQLite+media) + admin-dist + admin-conf
/opt/sinivora/   同结构 + sinivora.env (docker run --env-file 用)
/opt/jade-ldap/  data/(SQLite+ldif) + web-dist + web-conf
/opt/lucky/conf  Lucky 配置 (v3.7.1 配置在容器 /app/conf, 已挂载持久化!)
```
