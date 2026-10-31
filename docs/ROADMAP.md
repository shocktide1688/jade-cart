# Jade Cart 路线图（P0 之后）

> P0 已交付并验证：商品/变体/库存/分类、匿名购物车、结算防超卖、订单状态机、Mock 支付+webhook 幂等、Qute 店面、管理端四页面、LDAP 认证源接入。
> 本文档是后续迭代的执行计划，按期组织，每项含验收标准与预估工作量（人日）。

---

## P1 · 真实支付与履约闭环（运营必需，约 12~15 人日）

### 1. Stripe / PayPal 真实网关（3~4d）
- `StripeProvider`：Checkout Session REST 直连（`api.stripe.com/v1/checkout/sessions`，无 SDK），`client_reference_id=订单 public_id`；webhook 验签（`Stripe-Signature` HMAC + 时间戳容差）→ 复用 `mall_webhook_event` 幂等表 → 金额/币种双校验 → 现有 `markPaid` 路径
- `PaypalProvider`：Orders API（sandbox/live 双 base），capture + webhook 双路确认
- 密钥双路：环境变量 or 后台加密存储（`mall_store_setting` 已有 AES-GCM）；管理端「支付设置」页 + 连接测试按钮
- 迟到款→`PAYMENT_REVIEW` 已具备；补 `charge.refunded` webhook 反向同步订单状态
- **验收**：sandbox 从店面下单→网关收银台→webhook→PAID→退款全链路；重放 webhook 幂等
- 改动：core 新增 `payment/stripe`、`payment/paypal`；app 新增 `/api/store/webhooks/{provider}`；管理页设置扩展

### 2. 运费分区 + 发货追踪（2d，kartwo 0019 对标）
- `mall_shipping_zone`（国家组/固定运费/满额免邮）+ checkout 时匹配写入 `shipping_cents` 快照
- `mall_shipment`：carrier / tracking_number / tracking_url，履约时录入（单次发货模型）
- 管理端：运费设置页；订单履约弹窗加物流表单
- **验收**：不同金额订单运费快照正确；履约后店面订单页显示物流号

### 3. 退款网关化（1~1.5d）
- `refund()` 先调网关退款 API（Stripe refund / PayPal capture refund），成功后事务内写 `mall_refund` + 状态流转；失败给人工提示
- **验收**：sandbox 退款后网关与订单状态一致，重复退款被拦截

### 4. 邮件 outbox（2d，复用 jade-mail）
- `mall_email_outbox`（`UNIQUE(order_id,kind)` 幂等）；与支付/发货同事务入队；worker 20s 轮询 + 指数退避（1m/5m/30m/2h/6h → 死信）
- 模板（英文）：下单确认 / 支付确认 / 发货通知；SMTP 直接用 jade-mail 存库配置
- **验收**：支付完成后收到邮件；停 SMTP 后消息不丢、恢复后补发、不重复

### 5. 媒体库（2.5~3d，kartwo 0004 对标）
- `mall_media_asset`（相对路径，内容哈希命名）+ 派生图 WebP（thumb 200 / medium 800 / large 1600）
- 上传校验：magic bytes 仅 JPEG/PNG/WebP、去 EXIF、单文件 ≤10MiB、磁盘护栏
- 商品主图/相册字段 + 管理端上传组件 + 店面展示（占位 ✦ 替换为真图）
- 存储走本地 `data/media/`，接口预留 `jade-oss`
- **验收**：上传→派生图生成→店面详情页展示；重复上传同图不产生双份

---

## P2 · 内容与获客（增长增强，约 8~10 人日）

### 6. CSV 商品导入（2d，kartwo 0012~0014）
- generic + Shopify 两种格式；干跑预览/行级错误报告/`source_sha256` 幂等执行；Shopify 远程图片下载、旧 handle → 301 重定向表
- **验收**：一份 Shopify 导出 CSV 完整导入且二次导入零副作用

### 7. 内容页 + 政策生成器（1d）
- `mall_content_page`（受限 Markdown）；店铺资料一键生成 7 个英文页脚政策页；首页精选管理已有 featured 字段，补管理界面

### 8. 博客（1.5d）
- `blog_post`：draft/active、受限 Markdown→安全 HTML（转义原始 HTML、仅本站图片）、封面、SEO 描述

### 9. 客服留言（1d）
- `mall_contact_message`：topic 枚举、new/processing/closed、提交限流、到期匿名化（IP 置空不删除）

### 10. SEO 强化（1d）
- 店面补 JSON-LD（Product/Offer）、canonical、OG 标签、sitemap.xml、robots.txt；商品中英辅助标题/描述字段

### 11. 数据导出与备份（1.5d）
- 订单 CSV 导出；全量备份 ZIP（PG dump + 媒体 + manifest）、本地定时保留 N 份、WebDAV 异地上传

---

## P3 · 平台化与工程化（约 8~10 人日）

### 12. LDAP 组→角色自动映射（1d）
- 扩展 `LdapAuthenticator.syncLocalUser`：读 `ou=groups`（roleOccupant）映射 `sys_role`；管理端可配「LDAP 组 ↔ 系统角色」映射表
- **验收**：目录用户登录即获得对应角色，无需手工授权

### 13. orval 类型安全客户端（1d）
- `npm run gen:api` 生成 mall API client，mall 四页面迁移到生成代码（底座已有 orval 配置，指向 `/q/openapi`）

### 14. 测试补齐（2d）
- core 单测：并发结算防超卖（多线程抢购同一变体）、状态机非法流转、webhook 幂等、预留过期
- app REST 测试：quarkus-junit5 + rest-assured 关键路径

### 15. 业务监控（1d）
- BusinessMetrics 模式：下单/支付/退款/库存事件打点；Grafana 面板（底座已有 Prometheus 栈）

### 16. 部署工程（2d）
- `docker-compose.jade-cart.yml`（app + frontend + 复用外部 PG/Redis）；Dockerfile（JVM 起步，native 可选）；Drone/GitHub Actions CI（构建+测试+镜像）

### 17. 多市场/多币种框架（2d，可选）
- market 注册表（kartwo 0018 思路）：单市场 US/USD 落地，EU 等占位；市场决定货币/语言/可用支付方式

---

## 建议执行顺序

```
P1:  1 支付 → 3 退款 → 2 运费/发货 → 4 邮件 → 5 媒体     （先打通钱，再打通货）
P2:  6 导入 → 10 SEO → 7 政策页 → 8 博客 → 9 客服 → 11 备份
P3:  12 LDAP 映射（独立，可随时插队）→ 14 测试 → 13 orval → 15 监控 → 16 部署 → 17 多市场
```

依赖关系：退款(3) 依赖真实网关(1)；邮件模板(4) 可与网关并行；媒体(5) 是导入(6) 图片下载的前置；多市场(17) 依赖运费(2) 与支付(1) 的市场化参数。

## 风险与注意

- webhook 验签需处理密钥轮换与重放时间窗；网关超时要有重试且不产生重复扣款（幂等表已就绪）
- 媒体磁盘护栏必须在导入(6)之前就位（远程图片下载量大）
- 邮件 worker 与预留清扫同进程并发，注意 PG 连接池水位（当前 max-size 20）
- LDAP 映射改动点在 jade-boot 仓库（jade-ldap-client），需要跨仓库发版
