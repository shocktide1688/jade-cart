import 'package:dio/dio.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';

import 'api_client.dart';
import 'app_config.dart';

/// 服务器地址持久化 (运行时可切换, 单 key; 购物车 cookie 随 ApiClient 重建天然隔离)
abstract interface class ServerConfigStore {
  Future<String?> readUrl();
  Future<void> writeUrl(String url);
}

final class SecureServerConfigStore implements ServerConfigStore {
  SecureServerConfigStore({FlutterSecureStorage? storage})
    : _storage = storage ?? const FlutterSecureStorage();

  final FlutterSecureStorage _storage;

  static const _key = 'jadecart.server_url';

  /// 内存兜底: Keychain/Keystore 不可用时 (如 iOS 缺 entitlement、无签名构建) 至少让 App 能跑起来
  String? _memoryFallback;

  @override
  Future<String?> readUrl() async {
    try {
      final raw = await _storage.read(key: _key);
      if (raw == null || raw.isEmpty) return _memoryFallback;
      try {
        return normalizeServerUrl(raw);
      } on FormatException {
        // 存的是非法值 (旧版本/手工改动) → 清掉走平台默认
        await _storage.delete(key: _key);
        return null;
      }
    } on Exception {
      // 安全存储不可用 → 降级到内存
      return _memoryFallback;
    }
  }

  @override
  Future<void> writeUrl(String url) async {
    final normalized = normalizeServerUrl(url);
    _memoryFallback = normalized;
    try {
      await _storage.write(key: _key, value: normalized);
    } on Exception {
      // 落盘失败不阻断切换 (内存里已生效)
    }
  }
}

/// 测试连接: GET /q/health 返回耗时; 网络失败抛 [ApiFailure], 非 200 视为不可用
Future<Duration> testServerConnection(String serverUrl, {Dio? dio}) async {
  final client = dio ?? Dio();
  final stopwatch = Stopwatch()..start();
  try {
    final response = await client.get<String>(
      '$serverUrl/q/health',
      options: Options(
        responseType: ResponseType.plain,
        receiveTimeout: const Duration(seconds: 8),
      ),
    );
    if (response.statusCode != 200) {
      throw ApiFailure('Health check returned HTTP ${response.statusCode}');
    }
    return stopwatch.elapsed;
  } on DioException catch (error) {
    throw ApiFailure.fromDio(error);
  } finally {
    if (dio == null) client.close();
  }
}
