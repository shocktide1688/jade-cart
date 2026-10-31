# AGENTS.md — Jade Cart AI 编码约定

本文件是 AI 助手参与本项目的强制约定。规则来自现有代码的实际做法, 改动前先读这里。

## 项目是什么

基于 [Jade Platform (jade-boot)](../jade-boot) 底座的电商系统 (领域模型对标 kartwo), 管理端经 jade-ldap 接入 LDAP 认证。

```
backend/
├── jade-cart-core   电商业务 (按业务域分包: entity + service + rest + dto, jar 内 Flyway 迁移)
└── jade-cart-app    可运行 Quarkus 应用 (聚合 jade-admin + jade-cart-core + Qute 店面 + job)
frontend/            Vue 3 + Element Plus 管理端 (API 客户端由 orval 生成)
```

- 技术栈: Java 21 + Quarkus + Hibernate Panache + Lombok + PostgreSQL; Vue 3.5 + TS + Pinia + vue-query + Element Plus
- 底座能力 (登录/JWT/RBAC/菜单/租户/加密/邮件/LDAP) 来自 jade-boot, 本仓库不重复造

## 质量门 (必须过, 违规即构建失败)

| 门禁 | 触发 | 说明 |
|---|---|---|
| Spotless (google-java-format AOSP) | `mvn test` 的 validate 阶段 | 格式不符直接失败; 提交前跑 `mvn spotless:apply` |
| PMD 7 quickstart | 同上 | 静态分析; 阿里 P3C 停留在 PMD 6 不支持 Java 21 语法, 故用 PMD 7 内置集 |
| ESLint 9 (flat config) | `npm run lint` | `src/api/generated/` 不参与 lint |
| vue-tsc | `npm run type-check` | CI 的 test job 两条都跑 |
| OrderFlowTest | `mvn test` | 5 例: 并发防超卖/状态机/webhook 幂等/预留过期/迟到款 |

- 本机跑 Maven 需 JDK 21: `JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn ...`
- 仓库根 `.mvn/jvm.config` 提供 google-java-format 所需的 add-exports 参数, **不要删除**
- **不要绕过门禁**: 确需压制某条 PMD 规则时用 `@SuppressWarnings("PMD.规则名")` 并写注释说明理由, 一个都不能免
- 已知约定: catch 后重抛必须保留异常链 (`ex.initCause(e)`, PMD PreserveStackTrace 强制; BizException 无 cause 构造器, 它在底座仓库)

## 常用命令

```bash
# 后端 (backend/ 为 Maven 根)
mvn spotless:apply                 # 格式化 (提交前必跑)
mvn test                           # 质量门 + 测试 (%test 库: jade_test, 可用 TEST_DB_URL 覆盖)
mvn test -pl jade-cart-app -Dtest=OrderFlowTest

# 前端 (frontend/)
npm run lint          # ESLint 检查
npm run lint:fix      # 自动修复
npm run type-check    # vue-tsc
npm run gen:api       # 后端 OpenAPI 变更后重新生成 API 客户端
npm run build         # vue-tsc -b && vite build
```

## 后端约定

- **按业务域分包, 不按技术分层**: 每个域一个包 (catalog/trade/blog/shipping/media/…), 包内 `entity/ service/ rest/ dto/`; 新功能找到对应域, 没有就建新域包
- **Controller 命名**: `Admin*Controller` (管理端, 走 RBAC) / `Store*Controller` (买家, `@PermitAll`) / `Public*Controller`; 类上 `@Tag(name = "店面-客服")` 中文分组, 方法 `@Operation(summary = …)`
- **响应统一 `R<T>`**: `com.jade.common.api.R` (底座), `R.ok()` / `R.fail(400, "msg")`; 业务错误抛 `BizException("中文消息")`
- **Panache Active Record**: 实体字段包内直接访问 (`category.name = name`), 不写 getter/setter; Service 用 `@ApplicationScoped` + `@Transactional`
- **请求体**: Controller 内嵌 record (`record ContactReq(String name, …)`); 视图模型放 `dto/` 包, 同样多用 record
- **Flyway 迁移版本号**: 业务应用一律 **V100 起** (V100~V105 已用, 顺延), 底座占用 V1~V16+; 多模块合并迁移 `out-of-order: true` 必开 — **不要用底座号段**
- **Qute 店面** (`jade-cart-app/storefront/`): SSR 英文店面, 无 JS 可完成购物; `@TemplateExtension` 工具类 `final` + 私有构造器
- **定时任务** 放 `jade-cart-app/job/`

## 前端约定

- **`src/api/generated/` 由 orval 自动生成 — 禁止手改**, 后端 API 变了就跑 `npm run gen:api`
- auto-import 已开启 (vue / vue-router / pinia 的 API 和 Element Plus 组件无需显式 import, 见 `vite.config.ts`)
- `pages/` 按业务域分组 (system/mall/monitor/…), 新页面放进对应域
- 全局状态只放 `stores/` (app / user), 页面局部状态用组合式 API
- `vite dev` 代理 `/api` `/media` `/q` → `localhost:8090`

## 提交规范

Conventional Commits (中文描述): `feat: 商品描述富文本编辑器` / `fix: vite dev 监听 0.0.0.0`。一次提交一个主题, 格式化扫描不与功能改动混在一起。

## 迁移版本序参考

jade-admin V4~V8(自带) + mail V14 + ldap-client V15 + cart V16~V20 → **业务新迁移从 V106 起顺延** (详见 README)
