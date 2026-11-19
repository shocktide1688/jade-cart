import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import 'ui/app_theme.dart';

/// 底部页签路由 (StatefulShellRoute 各分支独立导航栈)
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

// ---------- 页签占位 (各 feature 提交逐个替换) ----------

class HomePage extends StatelessWidget {
  const HomePage({super.key});

  @override
  Widget build(BuildContext context) {
    return _StubPage('Home');
  }
}

class ShopPage extends StatelessWidget {
  const ShopPage({super.key});

  @override
  Widget build(BuildContext context) {
    return _StubPage('Shop');
  }
}

class CartPage extends StatelessWidget {
  const CartPage({super.key});

  @override
  Widget build(BuildContext context) {
    return _StubPage('Cart');
  }
}

class AccountPage extends StatelessWidget {
  const AccountPage({super.key});

  @override
  Widget build(BuildContext context) {
    return _StubPage('Account');
  }
}

class _StubPage extends StatelessWidget {
  const _StubPage(this.title);

  final String title;

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: Text(title)),
      body: Center(
        child: Text(title, style: Theme.of(context).textTheme.headlineMedium),
      ),
    );
  }
}
