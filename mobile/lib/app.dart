import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import 'package:jade_cart_mobile/features/account/presentation/pages/account_page.dart';
import 'package:jade_cart_mobile/features/cart/presentation/pages/cart_page.dart';
import 'package:jade_cart_mobile/features/catalog/presentation/pages/home_page.dart';
import 'package:jade_cart_mobile/features/catalog/presentation/pages/product_detail_page.dart';
import 'package:jade_cart_mobile/features/catalog/presentation/pages/shop_page.dart';
import 'package:jade_cart_mobile/features/checkout/presentation/pages/checkout_page.dart';
import 'package:jade_cart_mobile/features/orders/presentation/pages/order_detail_page.dart';
import 'package:jade_cart_mobile/ui/app_theme.dart';

/// 路由: 底部四页签 (StatefulShellRoute 各分支独立栈) + 详情页
///
/// 深链: jadecart:///shop/jade-tee → /shop/jade-tee (三斜杠, path 完整对齐路由);
/// 需在 iOS Info.plist 开 FlutterDeepLinkingEnabled, Android Manifest 开 flutter_deeplinking_enabled。
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
      title: 'Jade Cart',
      theme: AppTheme.light(),
      darkTheme: AppTheme.dark(),
      themeMode: ThemeMode.system,
      routerConfig: ref.watch(routerProvider),
    );
  }
}

/// 底部四页签壳: 首页 / 商店 / 购物车 / 我的
class HomeShell extends StatelessWidget {
  const HomeShell({required this.shell, super.key});

  final StatefulNavigationShell shell;

  static const _tabs = <(String, IconData)>[
    ('Home', Icons.home_outlined),
    ('Shop', Icons.storefront_outlined),
    ('Cart', Icons.shopping_cart_outlined),
    ('Account', Icons.person_outline),
  ];

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: shell,
      bottomNavigationBar: NavigationBar(
        selectedIndex: shell.currentIndex,
        onDestinationSelected: (index) =>
            shell.goBranch(index, initialLocation: index == shell.currentIndex),
        destinations: [
          for (final (label, icon) in _tabs)
            NavigationDestination(icon: Icon(icon), label: label),
        ],
      ),
    );
  }
}
