import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'core/api_client.dart';
import 'core/brand.dart';
import 'core/repositories.dart';
import 'features/orders/data/order_history_store.dart';
import 'features/orders/presentation/controllers/order_history_controller.dart';

/// cookie 持久化目录 (main() 用 path_provider 算好后 override)
final cookieDirProvider = Provider<String>((ref) {
  throw UnimplementedError('cookieDirProvider 必须在 main() 中 override');
});

/// 当前店的后端地址: 写死在 Brand 配置里 (构建期 STORE_KEY 选店), 无运行时入口
final serverUrlProvider = Provider<String>((ref) => brand.apiUrl);

/// DI 链: serverUrl → apiClient → repositories
///
/// 地址是编译期写死的常量, 不会中途变化; ApiClient 的 cookie jar 仍持久化
/// cart_token (购物车跨启动保留), 订单历史按地址隔离 (per-server key)。
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

/// 订单历史存储: per-server key (同机多店各看各的订单)
final orderHistoryStoreProvider = Provider<OrderHistoryStore>((ref) {
  return SecureOrderHistoryStore(serverUrl: ref.watch(serverUrlProvider));
});
