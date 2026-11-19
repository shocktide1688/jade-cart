# release 构建混淆规则 (isMinifyEnabled = true)

# Flutter 框架由 Gradle 插件自动处理, 这里只补第三方库

# dio: 依赖反射解析请求/响应? dio 5.x 用代码生成, 无需 keep;
# 但 cookie_jar / dio_cookie_manager 涉及泛型与序列化, 保留签名
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes InnerClasses

# cookie_jar: 内部用 dart 侧, Java 侧仅接口
-keep class io.github.littlesun.** { *; }

# 网络日志: release 关闭
-dontwarn okhttp3.**
-dontwarn okio.**
