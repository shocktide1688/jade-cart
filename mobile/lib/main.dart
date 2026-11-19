import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:path_provider/path_provider.dart';

import 'app.dart';
import 'app_providers.dart';
import 'core/server_config.dart';

/// 启动序列: 读持久化服务器地址 + 算 cookie 目录 → ProviderScope.overrides 注入 (ReqPilot 同款)
Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();
  final configStore = SecureServerConfigStore();
  final initialUrl = await configStore.readUrl();
  final supportDir = await getApplicationSupportDirectory();
  runApp(
    ProviderScope(
      overrides: [
        serverConfigStoreProvider.overrideWithValue(configStore),
        initialServerUrlProvider.overrideWithValue(initialUrl),
        cookieDirProvider.overrideWithValue('${supportDir.path}/cookies'),
      ],
      child: const JadeCartApp(),
    ),
  );
}
