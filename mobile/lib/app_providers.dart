import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'core/api_client.dart';
import 'core/app_config.dart';
import 'core/repositories.dart';
import 'core/server_config.dart';

/// main() 启动前注入的初始服务器地址 (持久化值; null → 平台默认)
final initialServerUrlProvider = Provider<String?>((ref) => null);

/// cookie 持久化目录 (main() 用 path_provider 算好后 override)
final cookieDirProvider = Provider<String>((ref) {
  throw UnimplementedError('cookieDirProvider 必须在 main() 中 override');
});

/// 服务器地址持久化 store (测试注入内存实现)
final serverConfigStoreProvider = Provider<ServerConfigStore>((ref) {
  return SecureServerConfigStore();
});

/// 当前服务器地址: 唯一写入点 = ServerUrlController.update (写持久化 + 切状态)
///
/// 状态变化 → apiClientProvider 及其下游全部重建, cookie jar 随实例隔离, 不会串用。
final serverUrlProvider = StateNotifierProvider<ServerUrlController, String>((
  ref,
) {
  final initial = ref.watch(initialServerUrlProvider) ?? AppConfig.apiBaseUrl;
  return ServerUrlController(ref.watch(serverConfigStoreProvider), initial);
});

class ServerUrlController extends StateNotifier<String> {
  ServerUrlController(this._store, String initial) : super(initial);

  final ServerConfigStore _store;

  Future<void> update(String url) async {
    final normalized = normalizeServerUrl(url);
    await _store.writeUrl(normalized);
    state = normalized;
  }
}

/// DI 链: serverUrl → apiClient → repositories
final apiClientProvider = Provider<ApiClient>((ref) {
  final client = ApiClient(
    baseUrl: ref.watch(serverUrlProvider),
    cookieDir: ref.watch(cookieDirProvider),
  );
  ref.onDispose(client.close);
  return client;
});

final catalogRepositoryProvider = Provider<CatalogRepository>((ref) {
  return CatalogRepository(ref.watch(apiClientProvider));
});

final cartRepositoryProvider = Provider<CartRepository>((ref) {
  return CartRepository(ref.watch(apiClientProvider));
});

final checkoutRepositoryProvider = Provider<CheckoutRepository>((ref) {
  return CheckoutRepository(ref.watch(apiClientProvider));
});
