import 'dart:io';

import 'package:flutter_test/flutter_test.dart';

import 'package:jade_cart_mobile/core/api_client.dart';
import 'package:jade_cart_mobile/core/repositories.dart';

/// 端到端购买链路测试 (打真实后端 http://localhost:8090)
///
/// 覆盖: 商品列表 → 商品详情 → 加购 → 改数量 → 结算建单 → mock 支付 → 订单变 PAID
///
/// 前置: 后端已启动 (默认 8090), 且未配置真实支付网关 (走 mock 网关)。
/// 后端未启动时整个 group 跳过 (不误报失败)。
void main() {
  const baseUrl = String.fromEnvironment(
    'E2E_BASE_URL',
    defaultValue: 'http://localhost:8090',
  );

  late ApiClient client;
  late CatalogRepository catalog;
  late CartRepository cart;
  late CheckoutRepository checkout;

  setUpAll(() async {
    client = ApiClient(
      baseUrl: baseUrl,
      cookieDir: '${Directory.systemTemp.path}/jadecart-e2e-cookies',
    );
    catalog = CatalogRepository(client);
    cart = CartRepository(client);
    checkout = CheckoutRepository(client);
  });

  tearDownAll(() async {
    client.close();
  });

  group('端到端购买链路', () {
    test('应该走完 列表→详情→加购→结算→支付→订单PAID', () async {
      // 1. 商品列表
      final products = await catalog.products(limit: 5);
      expect(products, isNotEmpty, reason: '后端应有可售商品');
      final target = products.firstWhere(
        (product) => !product.isSoldOut,
        orElse: () => products.first,
      );

      // 2. 商品详情 (拿到变体 id)
      final detail = await catalog.product(target.slug);
      expect(detail.variants, isNotEmpty, reason: '商品必须有变体');
      final variant = detail.variants.firstWhere(
        (v) => v.inStock,
        orElse: () => detail.variants.first,
      );

      // 3. 加购
      final afterAdd = await cart.addItem(variant.id, quantity: 2);
      expect(afterAdd.items, isNotEmpty);
      final addedLine = afterAdd.items.firstWhere(
        (line) => line.variantId == variant.id,
      );
      expect(addedLine.quantity, 2);

      // 4. 改数量 (+1)
      final afterUpdate = await cart.updateItem(variant.id, 3);
      final updatedLine = afterUpdate.items.firstWhere(
        (line) => line.variantId == variant.id,
      );
      expect(updatedLine.quantity, 3);

      // 5. 结算建单
      final result = await checkout.checkout(
        email: 'e2e@jadecart.test',
        shipName: 'E2E Tester',
        country: 'US',
      );
      expect(result.orderPublicId, isNotEmpty);
      expect(result.totalCents, greaterThan(0));

      // 6. mock 网关确认支付
      expect(result.isMockPay, isTrue, reason: '开发环境默认 mock 网关');
      await checkout.confirmMockPay(result.orderPublicId);

      // 7. 订单应为 PAID
      final order = await checkout.order(result.orderPublicId);
      expect(order.status.name, 'paid');
      expect(order.items, isNotEmpty);
      expect(order.email, 'e2e@jadecart.test');
    }, timeout: const Timeout(Duration(minutes: 2)));

    test('应该清空购物车 当把数量设为 0', () async {
      final products = await catalog.products(limit: 1);
      final detail = await catalog.product(products.first.slug);
      final variant = detail.variants.first;

      await cart.addItem(variant.id, quantity: 1);
      final afterRemove = await cart.updateItem(variant.id, 0);
      expect(
        afterRemove.items.where((line) => line.variantId == variant.id),
        isEmpty,
      );
    }, timeout: const Timeout(Duration(minutes: 2)));
  });
}
