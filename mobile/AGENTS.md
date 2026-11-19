# AGENTS.md — jade_cart_mobile 编码 Agent 硬性规范

> 本文件是所有编码 Agent（包括人类开发者）在本目录（Flutter / Dart）工作的**强制约束**。
> 每次开始任何编码任务前必须完整阅读。违反规范的代码将被拒绝合入。
> 规范体系对标 ReqPilotMobile（`../ReqPilotMobile`），冲突时以本文件为准。

---

## 0. 最高原则（不可违反）

1. **小步提交**：单次改动不超过 10 个文件（脚手架/批量重命名除外）
2. **单文件 ≤ 300 行**，**单函数 ≤ 50 行**，超限必须拆分——没有例外
3. **单圈复杂度 ≤ 10**：嵌套超过 3 层（if/for/try）必须提前返回或拆函数
4. **强类型优先**：禁止 `dynamic`，能用 `Object?` 就别用 `dynamic`；确需绕过时用 `Object?` + 类型守卫，并写注释说明原因
5. **状态机只有唯一真源**：业务状态枚举与流转规则集中在唯一模块，禁止散落到各 feature 重复定义
6. **不改别人的域**：改动落在任务指定的 feature 目录内；跨域改动必须先在 PR 描述中说明理由
7. **禁区文件不得修改**：`pubspec.lock`、`build/`、`.dart_tool/`、原生工程 `ios/` / `android/` 内由 IDE 生成的文件

---

## 1. 仓库结构纪律

```
lib/
├── main.dart              # 启动入口: 读持久化状态 → ProviderScope.overrides
├── app.dart               # 路由 (GoRouter) + 应用壳 (四页签)
├── app_providers.dart     # DI 链 + 全局 provider
├── core/                  # API/配置/模型/仓储/错误 (唯一真源)
├── features/<feature>/    # 一个特性 = 一个目录
│   ├── data/              # datasources / models / repositories / 本地存储
│   └── presentation/      # controllers / pages / widgets
├── shared/                # 复用 widget (本项目暂用 ui/common.dart)
└── ui/                    # 主题与通用组件
```

- **新增业务 = 新增 feature 目录**，禁止往已有文件里堆新功能
- **禁止循环依赖**：`core` / `ui` 不依赖 `features`；`features` 之间通过 `app_providers.dart` 汇合，禁止互相 import
- 跨包引用统一走包名（`package:jade_cart_mobile/...`），禁止相对路径穿越 `../../..`

## 2. 每个 feature 的标准文件结构

```
lib/features/<feature>/
├── data/
│   ├── models/            # 或 repositories/ (本地存储实现)
└── presentation/
    ├── pages/
    ├── widgets/
    └── controllers/       # Riverpod StateNotifier
```

- presentation 层禁止直接 import datasource；跨层访问走 repository
- data 层禁止 import presentation
- domain 逻辑（状态枚举、金额/数量计算）不依赖 Flutter SDK 之外的任何具体实现

## 3. Dart / Flutter 代码规范

### 3.1 函数与方法

- 函数**只做一件事**；超过 50 行必须拆
- 参数超过 3 个 → 改用命名参数 options 对象
- 提前返回（early return）替代深层嵌套
- 异步统一 `async/await`，禁止裸 `.then()` 链超过 2 层
- 禁止回调地狱；禁止在循环里 `await` 串行无关请求（用 `Future.wait`）

### 3.2 错误处理

- 网络/业务异常统一抛 `ApiFailure`（`lib/core/api_client.dart`），格式 `{ code, message }`
- 禁止 `try { ... } catch (_) {}` 空吞异常；catch 必须记录日志或继续抛出
- 后端错误码/消息不得原样透传到 UI，需转成人话
- 所有 Future 必须有 rejection 处理，禁止 floating futures

### 3.3 命名

- 文件 / 目录：snake_case；类 / 组件：PascalCase；函数 / 变量：camelCase；常量：UPPER_SNAKE
- 布尔变量用 `is/has/can/should` 前缀；函数名必须是动词开头
- 私有成员前缀下划线

### 3.4 类型

- API 模型手写 `fromJson` + 守卫型 helper（`lib/core/models.dart` 的 `asJsonMap` / `stringOf` / `intOf`）
- 模型类型从 data/core 层导出，禁止在 presentation 层重新定义
- 枚举一律用常量对象 + 联合类型，禁止散落魔法字符串（订单状态见 `OrderStatus`）
- 金额一律用「分」为单位的整数传输，展示时才除 100

### 3.5 Widget 规范

- 不可变 widget 私有字段全部 `final`；能编译期常量的全部 `const`
- 单 widget 的 `build` 方法 ≤ 50 行；禁止在 build 中做副作用
- 长列表必须 `ListView.builder` / `SliverGrid.builder`，禁止 `Column(children: [...])` 装长列表

### 3.6 登录态与存储

- **本 App 无用户账号体系**（店面为匿名 cookie 购物车），不要引入登录页
- 购物车身份 = 后端 `cart_token` httpOnly cookie，由 `ApiClient` 的 `PersistCookieJar` 托管，**不要自己存 token**
- 需要持久化的业务数据用 per-server key 存储（`jadecart.<name>.<base64(serverUrl)>`），切服务器后历史隔离
- 密钥只从 `--dart-define` / 环境变量读取；禁止写进代码、注释、测试

## 4. 测试规范

- 框架：flutter_test；测试与源码同目录就近放置（`test/` 下镜像 `lib/` 结构）
- **TDD 节奏（红→绿→重构）**：先写失败测试，再写最少实现使其通过，最后重构
- **验收标准用 BDD 格式**（Given/When/Then），测试名写成 `应该<预期行为> 当<条件>`
- HTTP 层测试用自定义 `HttpClientAdapter` 捕获请求并回放固定响应（见 `test/core/api_client_test.dart`）
- 测试禁连真实数据库/真实外部服务：用内存替身或 mock
- **widget 测试里禁止真实文件/网络 I/O**（flutter_test 的 fake async 会挂死 10 分钟），用固定路径或 `tester.runAsync`
- 提交前 `flutter analyze && flutter test` 必须全绿

## 4.1 质量门禁

| 门禁 | 工具 | 说明 |
|---|---|---|
| 类型检查 | `flutter analyze` | 残留 `dynamic` / `Object?` 必须注释原因 |
| 格式化 | `dart format` | 提交前强制 |
| Lint | `package:flutter_lints` + `analysis_options.yaml` | 集中配置 |
| 单测 | `flutter test` | 新增 feature 必须带仓储层测试 |

## 5. 提交与 PR

- Conventional Commits：`feat: / fix: / refactor: / test: / docs: / chore:`，subject ≤ 50 字，中文描述
- PR 描述必须包含：改了什么、为什么、如何验证（测试证据）
- 重构和功能改动禁止混在一个 PR
- **非平凡改动必须附决策记录**：重大架构决策写 `docs/adr/`

## 6. 编码 Agent 任务执行特别条款

1. 开工前：先读本文件 + 相关源码
2. 先写/改测试，再写实现（Bugfix 必须先有能复现问题的失败测试）
3. 完成后必须自报：改动文件清单、测试结果、**未覆盖的风险点**——禁止隐瞒已知缺陷
4. 不确定需求含义时：**停下来提问，禁止自行脑补**扩大改动范围
5. 遇到规范与任务冲突：遵守规范，并在报告中指出冲突点

---

*本文件的修改必须走人工审批，编码 Agent 无权改动 AGENTS.md 自身。*
