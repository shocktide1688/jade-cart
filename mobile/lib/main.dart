import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:liquid_glass_widgets/liquid_glass_widgets.dart';
import 'package:path_provider/path_provider.dart';

import 'app.dart';
import 'app_providers.dart';

/// 启动序列: 预热玻璃着色器 + 算 cookie 目录 → ProviderScope.overrides 注入
///
/// 后端地址写死在 Brand 配置里 (构建期 STORE_KEY 选店), 启动时无需读任何持久化地址。
Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();
  await LiquidGlassWidgets.initialize();
  final supportDir = await getApplicationSupportDirectory();
  runApp(
    LiquidGlassWidgets.wrap(
      brightnessResolver: Theme.maybeBrightnessOf,
      child: ProviderScope(
        overrides: [
          cookieDirProvider.overrideWithValue('${supportDir.path}/cookies'),
        ],
        child: const JadeCartApp(),
      ),
    ),
  );
}
