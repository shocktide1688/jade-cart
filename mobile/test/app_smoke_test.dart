import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:jade_cart_mobile/app.dart';
import 'package:jade_cart_mobile/app_providers.dart';
import 'package:jade_cart_mobile/core/server_config.dart';

/// 内存版服务器地址 store (测试不碰 Keychain/Keystore)
final class _InMemoryServerConfigStore implements ServerConfigStore {
  String? _url;

  @override
  Future<String?> readUrl() async => _url;

  @override
  Future<void> writeUrl(String url) async {
    _url = url;
  }
}

void main() {
  testWidgets('应该渲染四个页签 当应用启动', (tester) async {
    await tester.pumpWidget(
      ProviderScope(
        overrides: [
          serverConfigStoreProvider.overrideWithValue(
            _InMemoryServerConfigStore(),
          ),
          initialServerUrlProvider.overrideWithValue('http://localhost:8090'),
          // 固定路径: testWidgets 内不可做真实文件 I/O (fake async 会挂死)
          cookieDirProvider.overrideWithValue('/tmp/jadecart-test-cookies'),
        ],
        child: const JadeCartApp(),
      ),
    );
    await tester.pump();
    await tester.pump(const Duration(seconds: 1));

    expect(find.text('Home'), findsWidgets);
    expect(find.text('Shop'), findsWidgets);
    expect(find.text('Cart'), findsWidgets);
    expect(find.text('Account'), findsWidgets);
    // 占位首页带 AppBar 标题
    expect(find.byType(AppBar), findsOneWidget);
  });
}
