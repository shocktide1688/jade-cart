import 'package:flutter/foundation.dart';

/// 品牌构建配置: 每个店一个专属包, 后端地址与店名**全部写死**, 不提供任何运行时入口
///
/// 选择方式: 构建命令携带
///   --flavor teawone --dart-define=STORE_KEY=teawone
///   --flavor sinivora --dart-define=STORE_KEY=sinivora
///
/// debug 构建不带 STORE_KEY 时用本地开发默认 (Android 模拟器 10.0.2.2 / 其他 localhost),
/// 方便本地开发; release 构建必须带 STORE_KEY, 缺失/未知直接启动失败 (宁可不起, 不可连错店)。
final class BrandConfig {
  const BrandConfig({
    required this.name,
    required this.apiUrl,
    required this.tagline,
  });

  /// 店名 (App 内标题 / 关于页展示)
  final String name;

  /// 店面后端地址 (写死, 用户不可见不可改)
  final String apiUrl;

  /// 首页 hero 品牌文案
  final String tagline;
}

/// 当前包的品牌 (编译期常量, 运行时只读)
const String storeKey = String.fromEnvironment('STORE_KEY');

/// 解析当前品牌; release 下未知 key 抛错阻断启动
BrandConfig resolveBrand() {
  final config = _all[storeKey];
  if (config != null) return config;
  if (kReleaseMode) {
    throw StateError(
      'STORE_KEY "$storeKey" 不匹配任何品牌配置, release 构建必须携带'
      ' --dart-define=STORE_KEY=teawone|sinivora',
    );
  }
  // 本地开发默认 (debug/profile 无 STORE_KEY 时)
  if (defaultTargetPlatform == TargetPlatform.android) {
    return const BrandConfig(
      name: 'Dev Store',
      apiUrl: 'http://10.0.2.2:8090',
      tagline: 'Local development',
    );
  }
  return const BrandConfig(
    name: 'Dev Store',
    apiUrl: 'http://localhost:8090',
    tagline: 'Local development',
  );
}

/// 两店配置 (写死; 新增店 = 加一个条目 + 对应原生 flavor)
const Map<String, BrandConfig> _all = {
  'teawone': BrandConfig(
    name: 'Tea One',
    apiUrl: 'https://shop.teawone.com',
    tagline: 'Wood-fired handmade tea wares',
  ),
  'sinivora': BrandConfig(
    name: 'Sinivora',
    apiUrl: 'https://sinivora.teawone.com',
    tagline: 'Objects for everyday living',
  ),
};

/// 当前包生效的品牌
final BrandConfig brand = resolveBrand();
