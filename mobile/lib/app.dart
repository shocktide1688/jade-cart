import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import 'package:jade_cart_mobile/core/brand.dart';
import 'package:jade_cart_mobile/features/account/presentation/pages/account_page.dart';
import 'package:jade_cart_mobile/features/cart/presentation/pages/cart_page.dart';
import 'package:jade_cart_mobile/features/catalog/presentation/pages/home_page.dart';
import 'package:jade_cart_mobile/features/catalog/presentation/pages/product_detail_page.dart';
import 'package:jade_cart_mobile/features/catalog/presentation/pages/shop_page.dart';
import 'package:jade_cart_mobile/features/catalog/presentation/controllers/catalog_controllers.dart';
import 'package:jade_cart_mobile/features/checkout/presentation/pages/checkout_page.dart';
import 'package:jade_cart_mobile/features/orders/presentation/pages/order_detail_page.dart';
import 'package:jade_cart_mobile/ui/app_theme.dart';
import 'package:jade_cart_mobile/ui/glass_tab_bar.dart';

/// 路由: 底部四页签 (StatefulShellRoute 各分支独立栈) + 详情页
///
/// 深链: jadecart:///shop/jade-tee → /shop/jade-tee (三斜杠, path 完整对齐路由);
/// 需在 iOS Info.plist 开 FlutterDeepLinkingEnabled, Android Manifest 开 flutter_deeplinking_enabled。
/// 后端地址写死在 Brand (构建期 STORE_KEY 选店), 不存在未配置/切服状态, 无需守卫。
final routerProvider = Provider<GoRouter>((ref) {
  return GoRouter(
    initialLocation: '/home',
    routes: [
      StatefulShellRoute.indexedStack(
        builder: (context, state, shell) => HomeShell(shell: shell),
        branches: [
          StatefulShellBranch(
            routes: [
              GoRoute(
                path: '/home',
                name: 'home',
                builder: (context, state) => const HomePage(),
              ),
            ],
          ),
          StatefulShellBranch(
            routes: [
              GoRoute(
                path: '/shop',
                name: 'shop',
                builder: (context, state) => const ShopPage(),
                routes: [
                  GoRoute(
                    path: ':slug',
                    name: 'product',
                    builder: (context, state) =>
                        ProductDetailPage(slug: state.pathParameters['slug']!),
                  ),
                ],
              ),
            ],
          ),
          StatefulShellBranch(
            routes: [
              GoRoute(
                path: '/cart',
                name: 'cart',
                builder: (context, state) => const CartPage(),
              ),
            ],
          ),
          StatefulShellBranch(
            routes: [
              GoRoute(
                path: '/account',
                name: 'account',
                builder: (context, state) => const AccountPage(),
              ),
            ],
          ),
        ],
      ),
      GoRoute(
        path: '/checkout',
        name: 'checkout',
        builder: (context, state) => const CheckoutPage(),
      ),
      GoRoute(
        path: '/orders/:publicId',
        name: 'order',
        builder: (context, state) =>
            OrderDetailPage(publicId: state.pathParameters['publicId']!),
      ),
    ],
  );
});

class JadeCartApp extends ConsumerWidget {
  const JadeCartApp({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    return MaterialApp.router(
      title: brand.name,
      theme: AppTheme.light(),
      darkTheme: AppTheme.dark(),
      themeMode: ThemeMode.system,
      routerConfig: ref.watch(routerProvider),
    );
  }
}

/// 底部四页签壳: 首页 / 商店 / 购物车 / 我的 + 液态玻璃底栏 (搜索胶囊)
///
/// extendBody: 让内容滚到玻璃底栏下方产生折射; 各分支页底部已留玻璃条高度的边距。
class HomeShell extends ConsumerWidget {
  const HomeShell({required this.shell, super.key});

  final StatefulNavigationShell shell;

  /// 商店分支在 branches 里的序号
  static const _shopBranchIndex = 1;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    return Scaffold(
      extendBody: true,
      body: shell,
      bottomNavigationBar: GlassTabBar(
        selectedIndex: shell.currentIndex,
        onDestinationSelected: (index) =>
            shell.goBranch(index, initialLocation: index == shell.currentIndex),
        onSearchSubmitted: (query) => _search(ref, query),
      ),
    );
  }

  /// 底栏搜索提交 → 切到商店分支并按关键词刷新列表
  void _search(WidgetRef ref, String query) {
    ref.read(productListControllerProvider.notifier).search(query);
    shell.goBranch(_shopBranchIndex);
  }
}
