# 开发流程 (jade-cart 多店体系)

> 架构: jade-cart = 开源通用引擎; 各店铺 = 同一镜像的独立部署实例 (差异全在环境变量 + 数据)。
> 分工决策树: 通用功能/bugfix → 改 jade-cart 代码; 店铺内容(商品/博客/主题) → 直接生产管理后台操作;
> 店铺专属代码逻辑 → 私有品牌覆盖层仓库 (出现第一个品牌专属代码需求时再建)。
> 本文所有域名/IP/账号均为占位符, 真实值在各部署环境的服务器 .env / 运维文档中, **不入库**。

## 一、三套环境

| 环境 | 位置 | 数据库 | 用途 |
|---|---|---|---|
| 本地开发 | 开发机, localhost:8090 (后端) + :5173 (前端) | PG (Docker 容器:5432, 库 jade_cart) + Redis (:6380) | 写代码调试 |
| 店铺 A 生产 | https://shop.example.com + admin 域名 | 服务器 SQLite `/opt/<shop-a>/data/jade_cart.db` | 正式运营 |
| 店铺 B 生产 | 按 brand B 域名 | 服务器 SQLite `/opt/<shop-b>/data/jade_cart.db` | 正式运营 |

⚠️ 本地 PG 与生产 SQLite 各自演化, 不要假设数据一致。
⚠️ 本地后端用**默认 profile 构建** (PG); 服务器镜像用 **sqlite profile 构建** (`-Dquarkus.profile=sqlite`, db-kind 构建期固化)。
⚠️ 本机 launchd 托管的后端 (`~/Library/LaunchAgents/com.jade.cart`) 指向 `jade-cart-app/launchd-app/` (独立目录, 发行构建不会覆盖)。本地更新后端:
```bash
cd backend && mvn clean package -DskipTests          # 默认 PG profile
rsync -a --delete jade-cart-app/target/quarkus-app/ jade-cart-app/launchd-app/
launchctl bootout gui/$(id -u)/com.jade.cart; launchctl bootstrap gui/$(id -u) ~/Library/LaunchAgents/com.jade.cart.plist
```
⚠️ 生产域名/服务器 IP/凭证通过 `JADE_CORS_ORIGINS` 等**环境变量**注入, 代码默认值仅 localhost/内网正则。

## 二、本地环境启动

```bash
# 前置容器 (通常已在跑)
docker start <pg-container> <redis-container>

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
2. 测试: cd backend && mvn test            # 需要 PG + Redis 服务
3. 前端检查: cd frontend && npm run lint && npm run type-check
4. git commit (Conventional Commits 中文)
5. 构建部署 (各店同镜像, 都要更新, 见 §五)
6. 生产验证 (各店域名各走一遍)
```

新增数据库迁移注意: **PG 和 SQLite 双方言都要写**
- PG 版: `backend/jade-cart-core/.../db/migration/postgresql/V1xx__xxx.sql`
- SQLite 版: 同目录 `sqlite/V1xx__xxx.sql` (BIGSERIAL→INTEGER, TIMESTAMPTZ→TIMESTAMP,
  NOW()→CURRENT_TIMESTAMP, 无 COMMENT ON/DROP CASCADE/setval/ADD COLUMN IF NOT EXISTS)
- 时间列必须 TIMESTAMP (NUMERIC 亲和), 禁止 TEXT (xerial epoch 毫秒读写不对称)
- 迁移与配套代码同一 commit (flyway-at-start, 自动部署时不允许孤儿迁移)

## 四、场景 B: 店铺内容运营 (零代码)

直接在对应管理后台操作, 数据落各店自己的 SQLite:

| 内容 | 位置 |
|---|---|
| 商品/分类/库存 | 管理后台 → 商城管理 |
| 博客/内容页 | 管理后台 → 内容管理/博客管理 |
| 主题 (预设+覆盖) | 管理后台 → 店面装修 (预设: minimal/classic/bold/warm/emerald) |
| 语言 | 店面右上角切换器 (语言注册表: `jade.cart.locales`, 翻译包 i18n/store_<code>.properties) |
| LDAP 用户目录 | LDAP 管理后台 (统一目录, 登录时本地账号优先) |

## 五、构建部署 (手动阶段, 未来 Drone 自动化)

```bash
# ① 后端镜像 (sqlite profile 固化)
cd backend && mvn package -DskipTests -Dquarkus.profile=sqlite -pl jade-cart-app -am
cd jade-cart-app && docker buildx build --platform linux/amd64 -t jade-cart:sqlite --load .
docker save jade-cart:sqlite | gzip > /tmp/img.tar.gz
scp /tmp/img.tar.gz root@<SERVER_IP>:/tmp/

# ② 服务器更新各店 (容器 IP 已固定, 网关上游不受影响)
ssh root@<SERVER_IP> '
  docker load -i /tmp/img.tar.gz && rm /tmp/img.tar.gz
  cd /opt/<shop-a> && docker compose up -d --force-recreate app
  docker rm -f <shop-b>-app && docker run -d --name <shop-b>-app \
    --restart unless-stopped --network jade-cart_default --ip <固定IP> \
    --env-file /opt/<shop-b>/<shop-b>.env -p <端口>:8090 \
    -v /opt/<shop-b>/data:/app/data jade-cart:sqlite'

# ③ 管理端前端 (各店品牌版: VITE_APP_TITLE=<品牌> npm run build)
cd frontend && npm run build && tar czf /tmp/d.tar.gz -C dist .
scp /tmp/d.tar.gz root@<SERVER_IP>:/tmp/
ssh root@<SERVER_IP> 'rm -rf /opt/<shop>/admin-dist/* && tar xzf /tmp/d.tar.gz -C /opt/<shop>/admin-dist'

# ④ 验证
curl -s -o /dev/null -w "%{http_code}\n" https://<shop-domain>/
```

容器 IP 固定 (网关上游依赖): 每容器一个 static IP, **重建容器必须保持同 IP**;
生产域名走环境变量 (`JADE_CORS_ORIGINS` 等), 代码/镜像不含任何部署专属地址。

## 六、未来 CI/CD (已定案: Gitea + Drone + 华为云 SWR)

```
git push → Drone: 内核流水线 (test/构建基础镜像 → push SWR)
         → 品牌流水线 (FROM 基础镜像 + 品牌层 → push SWR)
         → 部署 job (SSH: 备份 DB → 拉新 tag → compose up → health 探活)
```
前置条件已完成: 容器 IP 固定 + compose 支持 `APP_IMAGE` 变量 (Drone 改 tag 即部署)。

## 七、服务器目录速查

```
/opt/<shop>/    compose/.env(APP_IMAGE/密钥/CORS) + data/(SQLite+media) + admin-dist + admin-conf
/opt/jade-ldap/ data/(SQLite+ldif) + web-dist + web-conf
/opt/lucky/conf 网关配置 (v3.7.1 配置在容器 /app/conf, 必须挂载持久化!)
```
