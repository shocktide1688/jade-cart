# jade_cart_mobile

jade-cart 独立站的跨平台移动客户端（Flutter / Dart），一套代码支持 iPhone 与 Android。

独立站网页（Qute SSR）与本 App 是两条独立渠道：网页继续承担 SEO 与广告落地页，App 承担浏览与下单。

## 已实现

- 首页精选商品、商店页（关键词搜索 + 分类筛选）、商品详情（图集 / 规格选择 / 数量 / 加购）
- 购物车（数量调整、删除、小计），会话由后端 `cart_token` cookie 维持，跨启动保留
- 结算（邮箱 / 收件人 / 国家）→ 建单 → 拉起支付
- 支付：mock 网关内联确认；Stripe / PayPal 走外部浏览器，订单页轮询感知支付结果
- 订单详情（状态流转、明细、物流链接）、订单本地历史（按服务器隔离存储）
- 我的页：服务器地址切换（连通性测试 + 持久化）、服务器切换后购物车与历史自动隔离
- 浅色/深色主题跟随系统

## 与后端的关系

App 只消费后端既有店面接口 + 本次新增的只读目录接口：

| 接口 | 说明 |
|---|---|
| `GET /api/store/products` | 商品列表（关键词 / 分类 slug / 精选）— **本次新增** |
| `GET /api/store/products/{slug}` | 商品详情（变体 / 库存 / 图集）— **本次新增** |
| `GET /api/store/categories` | 分类树 — **本次新增** |
| `GET/POST/PUT/DELETE /api/store/cart/**` | 购物车（既有，cookie 会话） |
| `POST /api/store/checkout` | 结算建单（既有） |
| `GET /api/store/orders/{publicId}` | 订单状态（既有，App 轮询它感知支付结果） |
| `POST /api/store/mock-pay/{publicId}/confirm` | mock 网关确认（既有，开发环境默认网关） |

支付结果没有 JSON 回调端点：Stripe / PayPal 由网关 webhook 通知后端，App 靠轮询订单状态感知。

## 本地运行

先启动后端（默认 8090 端口）：

```bash
cd ../backend
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn install -DskipTests
java -jar jade-cart-app/target/quarkus-app/quarkus-run.jar
```

iOS 模拟器默认连接 `http://localhost:8090`，Android 模拟器走宿主机 `http://10.0.2.2:8090`：

```bash
flutter run -d ios
flutter run -d android
```

运行时可在「我的 → API 服务」修改服务器地址（先点 Test 测连通性），无需重新打包。
构建参数仍可设置首次安装的默认地址：

```bash
flutter run --dart-define=API_BASE_URL=https://your-store.example.com
```

Android 模拟器访问宿主机必须用 `10.0.2.2`（`localhost` 指向模拟器自身）。

## 验证与构建

```bash
flutter analyze
dart format --set-exit-if-changed lib test
flutter test
flutter build ios --no-codesign
flutter build apk
```

Android 构建需要 JDK 17 或更新版本。

## 目录

```text
lib/
├── core/          # ApiClient (R 信封 + cookie)、服务器配置、领域模型、仓储
├── features/      # catalog / cart / checkout / orders / account
├── ui/            # 主题与通用组件 (商品卡、图片、空态、错误态)
├── app.dart       # 路由与四页签装配
├── app_providers.dart  # DI 链
└── main.dart      # 启动入口
```

## 架构约定

工程规范见 [AGENTS.md](AGENTS.md)。要点：

- feature-first 分层：`features/<feature>/{data,presentation}`，`core` 不依赖 `features`
- Riverpod DI 链：`serverUrlProvider → apiClientProvider → repositoryProvider → controllerProvider`
- 切换服务器 = 改一个 provider，整条链重建，cookie 与订单历史随之隔离
- 状态唯一真源：筛选/购物车/订单历史各有自己的 State + Controller，不在 widget 里散落状态

## 已知限制

- 无账号体系：订单历史靠本地存储的订单 publicId，换设备不可见
- 支付只实测过 mock 网关；Stripe / PayPal 需配置真实密钥后联调
- App 端文案固定英文；后端没有面向客户端的 i18n 接口
