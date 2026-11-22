# 上架清单与商店元数据

> jade_cart_mobile 上架 App Store / Google Play 所需的全部文案、配置与步骤。
> 商店后台填写时直接复制本文对应段落。

## 1. 通用信息

| 项 | 值 |
|---|---|
| App 名称 | Jade Cart |
| 副标题（iOS ≤30 字符） | 独立站购物 · 随时随地 |
| Bundle ID / Package | `com.jadecart.jadeCartMobile` / `com.jadecart.jade_cart_mobile` |
| 版本 | `pubspec.yaml` 的 `version`（1.0.0+1）—— **唯一真源** |
| 分类 | Shopping（购物） |
| 年龄分级 | 4+（无不当内容、无用户生成内容、无广告 SDK） |
| 版权 | © 2026 店名（与店面设置一致） |
| 支持网址 | 店面首页 |
| 隐私政策 URL | 店面 `/pages/privacy-policy`（见 §5） |
| 支持邮箱 | 店面 `/contact` 的联系邮箱 |

## 2. 描述文案

### App Store 描述（上限 4000 字符）

```
在手机上轻松逛你的独立店。

Jade Cart 是我们独立站的官方 App —— 浏览商品、查看详情、加入购物车、下单支付，
全流程在手机上完成，商品和价格与网页端实时一致。

· 精选好物：首页直达当季推荐
· 分类与搜索：按分类筛选，或用关键词找到想要的那件
· 商品详情：高清图集、规格选择、实时库存
· 购物车：随时调整数量，购物车跨设备保留
· 快速下单：填写邮箱与收货国家即可下单，支持主流支付方式
· 订单追踪：随时查看订单状态与物流信息

无广告，无账号也能下单 —— 邮箱即你的订单凭证。

我们做的是一间小店，所以每一件都想让你满意。有问题随时找我们。
```

### 简短描述（iOS 副标题 / Play 简短说明 ≤80 字符）

```
浏览、下单、追踪订单 —— 你的独立店随身版
```

### 关键词（iOS ≤100 字符，逗号分隔，不含品牌名）

```
shopping,store,ecommerce,independent,fashion,home,goods,buy,deals,gifts
```

### Google Play 简短说明（≤80 字符）

```
独立店的随身 App：浏览商品、下单支付、追踪订单
```

### Google Play 完整说明（≤4000 字符）

```
Jade Cart 是我们独立站的官方 App。

用手机完成从浏览到下单的全流程：

• 首页精选 + 分类筛选 + 关键词搜索
• 商品详情：高清图集、规格选择、实时库存
• 购物车跨启动保留，随时调整数量
• 填写邮箱与收货国家即可下单
• 订单状态与物流信息随时可查

我们不收集多余数据，也不做广告追踪 —— 邮箱只用于发送订单确认邮件。

独立小店，认真做货。欢迎来看看。
```

## 3. 审核备注（Review Notes）

```
本 App 是自营独立站的官方客户端，仅用于购买本店商品。

登录：无需登录/注册（匿名购物 + 邮箱识别订单）。
测试账号：不需要。

支付：走店面既有的 Stripe / PayPal 收银台，App 只跳转到网关页面完成支付，
不接触也不存储任何卡号信息。

后端地址：按店写死在构建配置里（无任何运行时入口）。
teawone 包连 https://shop.teawone.com，sinivora 包连 https://sinivora.teawone.com，
两个包用 --flavor 区分（见 README「双店构建」）。

功能受限说明：若店铺未配置 Stripe / PayPal 密钥，下单流程会自动走测试收银台。
```

## 4. 隐私标签（Privacy Labels）

App 内 `ios/Runner/PrivacyInfo.xcprivacy` 已声明 required reason API；
商店后台的隐私问卷与之对应：

| 采集项 | 是否采集 | 用途 | 关联用户 | 用于追踪 |
|---|---|---|---|---|
| 联系邮箱 | 是 | App 功能（订单确认） | 是 | 否 |
| 姓名 | 是 | App 功能（收件人） | 是 | 否 |
| 购买记录 | 是 | App 功能（订单查询） | 是 | 否 |
| 收货地址 | 是（国家 + 收件人） | App 功能（配送） | 是 | 否 |

**不采集**：支付卡信息（由 Stripe/PayPal 收银台处理）、位置、通讯录、标识符、广告数据。

## 5. 隐私政策与账号删除

- **隐私政策 URL**：在店面管理后台创建功能页，slug 设为 `privacy-policy`，
  访问地址 `https://<店铺域名>/pages/privacy-policy`。内容需覆盖：
  - 收集哪些数据（邮箱、姓名、收货国家、订单记录）及用途
  - 数据与谁共享（仅配送与支付服务商）
  - 数据保留期限与删除方式（发邮件到店铺邮箱即可删除）
  - 联系我们
- **账号删除**：本 App 无账号体系（匿名购物），App Store 若询问账号删除，
  填「本 App 无用户账号，不适用」即可；订单数据删除请邮件联系店铺。

## 6. 签名与构建

### iOS
```bash
# Xcode 里配置 Team 与 Bundle ID 后
flutter build ipa --release
```
- 需 Apple Developer 账号与 provisioning profile
- `ios/Runner/Runner.entitlements` 已含 Keychain 访问组，**不要删**

### Android
```bash
cp android/key.properties.example android/key.properties   # 填写 keystore 信息
keytool -genkey -v -keystore ~/keystores/jade-cart-release.jks \
        -keyalg RSA -keysize 2048 -validity 10000 -alias jade-cart
flutter build appbundle --release                            # 出 .aab 上传 Play
```
- `key.properties` 与 keystore 不入库（`.gitignore` 已含）
- 未配置 `key.properties` 时自动退回 debug 签名（仅本地验证，不可上架）

### 隐私清单与签名自检
```bash
# iOS: 确认隐私清单已打进 bundle
ls build/ios/iphoneos/Runner.app/PrivacyInfo.xcprivacy
# Android: 确认 release 用了混淆且能装
flutter build apk --release && adb install app-release.apk
```

## 7. 提交前检查

- [ ] `flutter analyze` 无问题
- [ ] `flutter test` 全绿
- [ ] `dart format --set-exit-if-changed lib test` 无改动
- [ ] 后端 `mvn test` 全绿（新增接口不影响既有 SSR 页面）
- [ ] 两端 release 构建成功，App 能连上正式店铺
- [ ] 真机走通：浏览 → 加购 → 下单 → 支付 → 订单状态
- [ ] 隐私政策 URL 可公开访问
- [ ] 截图按 §8 生成
- [ ] 版本号已递增（pubspec.yaml 的 version）

## 8. 商店截图

App Store 需要 6.9"/6.7" iPhone 截图，Play 需要手机截图。
用 `tool/generate_app_screenshots.sh` 生成（需 iOS 模拟器 + 后端在跑）。
