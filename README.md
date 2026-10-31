# Jade Cart

基于 [Jade Platform (jade-boot)](../jade-boot) 底座的电商系统, 领域模型对标 kartwo。
管理端开箱接入 [jade-ldap](../jade-ldap) LDAP 认证源。

## 架构

```
jade-cart/
├── backend/
│   ├── jade-cart-core   电商业务模块 (jade-mail 式自包含: 实体+服务+REST+jar 内 Flyway 迁移)
│   └── jade-cart-app    可运行 Quarkus 应用 (聚合 jade-admin + jade-cart-core + Qute 店面)
└── frontend/            Vue3 + Element Plus 管理端 (复制 jade-boot/frontend 改造)
```

- **底座能力 (零开发复用)**: 登录/JWT(RS256)/RBAC/菜单/租户/操作日志 (jade-admin)、幂等/限流/分布式锁 (jade-redis)、字段加密 (jade-security @Encrypted)、SMTP 配置存库 (jade-mail)、**LDAP 认证源 (jade-ldap-client: 本地优先, LDAP 兜底, 自动建号)**
- **业务模块 (jade-cart-core)**: 商品/双轴变体/库存(quantity+reserved)/分类树、匿名购物车、结算单事务防超卖、订单状态机 + 30min 预留过期清扫、PaymentProvider 接口 + Mock 收银台 + webhook 幂等表、商城设置 KV (AES-GCM 加密)
- **店面**: Qute SSR 英文店面 (无 JS 可完成购物), 与管理 API 同进程

## 迁移版本序

jade-admin V4~V8(自带) + mail V14 + ldap-client V15 + **cart V16(商品) V17(交易) V18(菜单+设置) V19(P1 运费/发货/邮件/媒体) V20(P2 内容页/客服)**
多模块合并迁移, `out-of-order: true` 必开 (见 jade-cart-app/application.yml 的 flyway locations)。

## 已交付能力 (P0+P1+P2 部分)

**P0**: 商品/双轴变体/库存(防超卖)/分类树、匿名购物车、结算单事务、订单状态机+预留过期清扫、Mock 收银台+webhook 幂等、Qute 店面、管理端商城四页

**P1**: Stripe/PayPal 直连 (Checkout Session / Orders API, 无 SDK) + webhook 验签 + 渠道自动选择 (stripe→paypal→mock) + 网关退款、运费分区(国家/满额免邮)+运费快照+发货追踪、邮件 outbox (与业务同事务入队, 指数退避, SMTP 复用 jade-mail 存库配置)、媒体库 (magic bytes 校验/内容哈希去重/多尺寸派生/磁盘护栏)、管理端: 支付设置/运费设置/履约弹窗/主图上传

**P2 部分**: SEO 全量重做 (每页 meta description/OG 完整含 og:image/og:url/Twitter Card/canonical 控制器统一构建/购物车结算订单页 noindex/robots 排除交易路径/sitemap 含 lastmod+图片扩展+博客)、JSON-LD (WebSite+SearchAction/完整 Product 含 brand·sku·AggregateOffer·availability/BreadcrumbList/ItemList/BlogPosting, 服务端 Jackson 构建防注入)、博客 (V106: 受限 Markdown+封面+SEO 描述+发布时间, 店面 /blog 列表+文章页, 管理端博客管理页, sitemap 收录)、内容页 (受限 Markdown) + 7 页脚政策页生成器、客服留言 (蜜罐+IP 哈希+限频+到期匿名化)、管理端: 内容管理/博客管理/客服留言

**P2 完成**: CSV 导入 (generic + Shopify, 干跑预览/行级错误/sha256 幂等/远程图片下载/旧 handle 301)、备份 (manifest+数据 JSON+媒体 ZIP, 每日 04:00 自动, 保留 7 份, 管理页可手动/下载/删除)、数据导入页 + 备份管理页

**P3 部分**: LDAP 组→角色映射 (jade-boot V16: sys_ldap_config.role_mappings, 格式 `[{"group":"cn","role":"roleCode"}]`, 登录时按 roleOccupant/member 匹配只增不减, 空则零行为)、部署文件 (Dockerfile + docker-compose.jade-cart.yml)

**P3 完成**: 测试 (OrderFlowTest 5 例: 并发防超卖/状态机/webhook 幂等/预留过期/迟到款, jade_test 库隔离, `mvn test -pl jade-cart-app`)、多市场框架 (`JADE_CART_MARKETS=US:USD,EU:EUR` 注册表 + 设置 KV `market` 运行时切换, 结账币种跟随)、LDAP 组映射管理界面字段 (jade-boot 前端)

**全部计划完成** — 详见 docs/ROADMAP.md

### 迁移版本号约定 (重要)
底座 jade-boot 占用 V1~V16 (ldap-client V16 起), 业务应用迁移从 **V100** 开始 (V100~V105), 避免底座演进撞号。

## 店面主题 + 装修

- **主题层**: 商城设置 → 店面主题 — 4 套预设 (minimal/classic/bold/warm) + 主色/背景/圆角/字体覆盖 + Logo/Favicon 上传; CSS 变量注入, 保存即时生效
- **装修层**: 店面装修 — 首页区块编排 (Hero 首屏 / 商品网格 / 分类导航 / 卖点条 / 富文本 / 最新文章 / 公告条), 排序显隐 + 手机桌面预览; 空编排回退内置布局
- REST: `/api/v1/theme` (preset + overrides), `/api/v1/storefront` (区块整体保存)

## 云原生镜像 (GHCR)

CI (`release.yml`) 每次 push main / 打 tag 自动: 核心测试 → 双架构构建 (amd64/arm64 原生 runner) → GHCR 多架构 manifest。

| 镜像 | 说明 |
|---|---|
| `ghcr.io/shocktide1688/jade-cart:latest` | **all-in-one 单容器**（JVM 后端 + nginx SPA，端口 8080） |
| `ghcr.io/shocktide1688/jade-cart:latest-api` | 纯后端 JVM（8090） |
| `sha-<sha>-aio` / `sha-<sha>-api` | 每次提交的固定版本 |

一键自托管（数据库外置时）：
```bash
docker run -d -p 8080:8080 -v cart-data:/app/data \
  -e DB_URL=jdbc:postgresql://<host>:5432/jade_cart \
  -e REDIS_URL=redis://<host>:6379 \
  ghcr.io/shocktide1688/jade-cart:latest
```
JWT/Crypto 密钥未提供时自动生成并持久化到 `/app/data/.env`（重启不丢）。
完整栈（含数据库）用 `docker-compose.jade-cart.yml`。

## 测试

```bash
mvn test -pl jade-cart-app -Dtest=OrderFlowTest
```
`%test` profile 自动指向独立库 jade_test (需先建库), 与开发库 jade_cart 完全隔离。

### 支付接入
管理端 商城管理→商城设置→支付配置 填入 Stripe/PayPal 密钥即启用; webhook 地址:
- Stripe: `{store_base_url}/api/store/webhooks/stripe` (事件: checkout.session.completed)
- PayPal: `{store_base_url}/api/store/webhooks/paypal` (事件: PAYMENT.CAPTURE.COMPLETED)

### 邮件
SMTP 配置 (系统管理→邮件配置) 指向 localhost:1025 (Mailpit) 即可本地验证; outbox 见 mall_email_outbox 表。

## 本地运行

```bash
# 0) 依赖: jade-boot 先 install 进本地仓库
cd ../jade-boot/backend && ./mvnw install -DskipTests

# 1) 基础设施 (Postgres 5432 / Redis 6380 / jade-ldap PG 5433+Redis 6381)
docker compose -f ../jade-boot/docker-compose.yml up -d postgres redis
docker compose -f ../jade-ldap/docker-compose.ldap.yml up -d

# 2) jade-ldap 服务 (JVM 模式, 端口 1390 —— 1389 可能被已有实例占用)
cd ../jade-ldap/backend && mvn -pl jade-ldap -am package -DskipTests
LDAP_PORT=1390 LDAPS_PORT=1637 LDAP_ADMIN_PASSWORD='ldap-admin-2026' \
DB_URL='jdbc:postgresql://localhost:5433/jade_ldap?currentSchema=public' REDIS_URL='redis://localhost:6381' \
JADE_JWT_SECRET='...' JADE_JWT_EXPIRE_SECONDS=7200 JADE_JWT_ISSUER=jade-ldap JADE_CRYPTO_MASTER_KEY='...' \
java -jar jade-ldap/target/quarkus-app/quarkus-run.jar

# 3) jade-cart 后端 (8090, DB jade_cart)
cd ../../jade-cart/backend && mvn install -DskipTests
# 推荐 launchd 托管 (见下), 或直接:
java -jar jade-cart-app/target/quarkus-app/quarkus-run.jar

# 4) 管理端前端 (5174)
cd ../frontend && npm install && npm run dev -- --port 5174
```

### 端口约定 (避开常用端口冲突)

| 服务 | 端口 |
|---|---|
| jade-cart-app | 8090 |
| 管理端前端 dev | 5174 |
| 店面 (同 app) | http://localhost:8090/ |
| jade-ldap (本仓库用) | LDAP 1390 / LDAPS 1637 / HTTP 8080 |
| 底座 PG/Redis | 5432 / 6380 |
| jade-ldap PG/Redis | 5433 / 6381 |

### launchd 托管 (macOS, 防 shell 退出杀进程)

`~/Library/LaunchAgents/com.jade.{cart,ldap}.plist` 已配置 (KeepAlive):
```bash
launchctl bootstrap gui/$(id -u) ~/Library/LaunchAgents/com.jade.cart.plist
launchctl kickstart -k gui/$(id -u)/com.jade.cart   # 重新部署后重启
```

## LDAP 接入 (管理端)

1. jade-ldap 控制台 (localhost:8080, admin/admin123) 建目录用户
2. 管理端 系统管理→LDAP 配置: `ldap://localhost:1390`, Base DN `dc=jade,dc=local`,
   Bind DN `cn=admin,dc=jade,dc=local`, 密码即 LDAP_ADMIN_PASSWORD, 自动建号开启
3. LDAP 用户直接登录管理端 → 本地无此用户时转 LDAP bind → 自动建 `auth_source=ldap` 本地账号
4. 注意: 登录 URL 若写 `localhost`, 解析可能命中 127.0.0.1 上的其他 LDAP 实例 —— 本机多实例时用明确端口 (1390)

已知边界: LDAP 组不自动映射系统角色, 新用户需在用户管理手工分配角色。

## 管理端账号

- 本地: admin / admin123
- LDAP: 目录用户 (自动建号, 组→角色映射见下)

### 会话过期处理 (401)

token 2 小时过期。过期后任何请求返回 401 → 前端统一弹「重新登录」对话框 (去重), 确认后清凭据跳登录页; 登录等公开接口永不携带旧凭据。该修复位于底座: jade-common GlobalExceptionMapper (认证异常→R(1001)) + 前端 request.ts error 拦截器。

## API

- OpenAPI: http://localhost:8090/q/openapi · Swagger UI: /q/swagger-ui
- 店面 API: `/api/store/*` (购物车/结算/Mock webhook)
- Mock 支付: 店面下单 → /pay/mock/{publicId} → 确认即走 webhook 幂等路径

## 默认管理员

admin / admin123 (jade-admin V1 迁移内置)
