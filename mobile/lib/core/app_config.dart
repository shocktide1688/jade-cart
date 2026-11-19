import 'dart:io';

/// 编译期 (--dart-define) 与平台默认服务器地址, 外加 URL 规范化
abstract final class AppConfig {
  /// 构建期注入: --dart-define=API_BASE_URL=https://your-store.example.com
  static const String configuredApiBaseUrl = String.fromEnvironment(
    'API_BASE_URL',
  );

  /// 默认 API 基址: Android 模拟器访问宿主机必须走 10.0.2.2, iOS 模拟器/桌面用 localhost
  static String get apiBaseUrl {
    if (configuredApiBaseUrl.isNotEmpty) {
      return normalizeServerUrl(configuredApiBaseUrl);
    }
    if (Platform.isAndroid) return 'http://10.0.2.2:8090';
    return 'http://localhost:8090';
  }
}

/// 校验并规范化服务器地址: 仅接受 http(s)://host[:port], 拒绝 path/query/fragment/userInfo, 去掉尾斜杠
///
/// 运行时可切换服务器 (我的 → API 服务), 所有入口 (输入框/持久化回读) 统一走这里兜底。
String normalizeServerUrl(String raw) {
  final trimmed = raw.trim();
  Uri uri;
  try {
    uri = Uri.parse(trimmed);
  } on FormatException {
    throw const FormatException('服务器地址无效');
  }
  final isHttp = uri.scheme == 'http' || uri.scheme == 'https';
  final hasBarePath = uri.path.isNotEmpty && uri.path != '/';
  if (!isHttp ||
      uri.host.isEmpty ||
      uri.userInfo.isNotEmpty ||
      uri.hasQuery ||
      uri.hasFragment ||
      hasBarePath) {
    throw const FormatException('服务器地址需形如 http(s)://host:port');
  }
  final port = uri.hasPort ? ':${uri.port}' : '';
  return '${uri.scheme}://${uri.host}$port';
}
