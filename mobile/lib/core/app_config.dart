import 'dart:io';

import 'package:flutter/foundation.dart';

/// 编译期 (--dart-define) 与平台默认服务器地址, 外加 URL 规范化
abstract final class AppConfig {
  /// 构建期注入: --dart-define=API_BASE_URL=https://your-store.example.com
  static const String configuredApiBaseUrl = String.fromEnvironment(
    'API_BASE_URL',
  );

  /// 默认 API 基址
  ///
  /// release 构建返回空串表示"未配置": 上架包若漏传 --dart-define, 必须让用户
  /// 在 App 里填店铺地址, 而不是静默连到 localhost / 10.0.2.2 (用户会拿到一个用不了的 App)。
  /// debug 构建用平台默认地址, 方便本地开发。
  static String get apiBaseUrl {
    if (configuredApiBaseUrl.isNotEmpty) {
      return normalizeServerUrl(configuredApiBaseUrl);
    }
    if (kReleaseMode) return '';
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
    throw const FormatException('Invalid server address');
  }
  final isHttp = uri.scheme == 'http' || uri.scheme == 'https';
  final hasBarePath = uri.path.isNotEmpty && uri.path != '/';
  if (!isHttp ||
      uri.host.isEmpty ||
      uri.userInfo.isNotEmpty ||
      uri.hasQuery ||
      uri.hasFragment ||
      hasBarePath) {
    throw const FormatException(
      'Server address must look like http(s)://host:port',
    );
  }
  final port = uri.hasPort ? ':${uri.port}' : '';
  return '${uri.scheme}://${uri.host}$port';
}
