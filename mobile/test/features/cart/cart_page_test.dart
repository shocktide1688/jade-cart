import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:go_router/go_router.dart';

import 'package:jade_cart_mobile/app_providers.dart';
import 'package:jade_cart_mobile/features/cart/presentation/controllers/cart_controllers.dart';
import 'package:jade_cart_mobile/features/cart/presentation/pages/cart_page.dart';

/// 空购物车假控制器: 不碰仓库, 直接给空态
class _EmptyCartController extends StateNotifier<CartState>
    implements CartController {
  _EmptyCartController() : super(const CartState());

  @override
  Future<void> load() async {}

  @override
  Future<void> addItem(int variantId, int quantity) async {}

  @override
  Future<void> setQuantity(int variantId, int quantity) async {}

  @override
  Future<void> removeItem(int variantId) async {}
}

void main() {
  Widget buildApp(GoRouter router) => ProviderScope(
    overrides: [
      serverUrlProvider.overrideWithValue('http://test.local'),
      cartControllerProvider.overrideWith((ref) => _EmptyCartController()),
    ],
    child: MaterialApp.router(routerConfig: router),
  );

  testWidgets('应该展示去逛逛按钮并跳转商店 当购物车为空', (tester) async {
    tester.view.physicalSize = const Size(1200, 2400);
    tester.view.devicePixelRatio = 3;
    addTearDown(tester.view.reset);
    final router = GoRouter(
      initialLocation: '/cart',
      routes: [
        GoRoute(path: '/cart', builder: (_, _) => const CartPage()),
        GoRoute(
          path: '/shop',
          builder: (_, _) => const Scaffold(body: Text('SHOP_PAGE')),
        ),
      ],
    );
    addTearDown(router.dispose);
    await tester.pumpWidget(buildApp(router));
    await tester.pumpAndSettle();

    // 空态文案 + CTA
    expect(find.text('Your cart is empty'), findsOneWidget);
    final cta = find.text('Browse products');
    expect(cta, findsOneWidget);

    await tester.tap(cta);
    await tester.pumpAndSettle();
    expect(find.text('SHOP_PAGE'), findsOneWidget);
  });
}
