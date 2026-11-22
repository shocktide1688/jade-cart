import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:jade_cart_mobile/app.dart';
import 'package:jade_cart_mobile/app_providers.dart';

void main() {
  testWidgets('应该渲染四个页签 当应用启动', (tester) async {
    await tester.pumpWidget(
      ProviderScope(
        overrides: [
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
  });

  testWidgets('应该展示写死的店铺信息 且无任何服务器入口 当打开我的页', (tester) async {
    await tester.pumpWidget(
      ProviderScope(
        overrides: [
          cookieDirProvider.overrideWithValue('/tmp/jadecart-test-cookies'),
        ],
        child: const JadeCartApp(),
      ),
    );
    await tester.pump();
    await tester.pump(const Duration(seconds: 1));

    // 切到 Account 页签
    await tester.tap(find.text('Account').last);
    await tester.pump();
    await tester.pump(const Duration(seconds: 1));

    // 店铺名来自 Brand 配置 (debug 无 STORE_KEY → Dev Store)
    expect(find.text('Dev Store'), findsWidgets);
    // 旧版的 API service 切换入口必须不存在 (地址写死, 用户不可改)
    expect(find.text('API service'), findsNothing);
    expect(find.text('Server URL'), findsNothing);
  });
}
