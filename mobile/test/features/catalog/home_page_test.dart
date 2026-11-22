import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:jade_cart_mobile/app_providers.dart';
import 'package:jade_cart_mobile/core/catalog_models.dart';
import 'package:jade_cart_mobile/features/catalog/presentation/controllers/catalog_controllers.dart';
import 'package:jade_cart_mobile/features/catalog/presentation/pages/home_page.dart';

const _product = ProductSummary(
  publicId: 'p1',
  title: 'Ice Smoke Teapot',
  slug: 'ice-smoke-teapot',
  featured: true,
  mainImagePath: '', // 空路径走占位图, 测试不碰网络
  minPriceCents: 9900,
  variantCount: 1,
  totalAvailable: 5,
);

const _category = CategoryNode(slug: 'teaware', name: 'Teaware', children: []);

void main() {
  Widget buildHome() => ProviderScope(
    overrides: [
      serverUrlProvider.overrideWithValue('http://test.local'),
      featuredProductsProvider.overrideWith((ref) async => [_product]),
      categoriesProvider.overrideWith((ref) async => [_category]),
    ],
    child: const MaterialApp(home: HomePage()),
  );

  testWidgets('应该展示品牌横幅与分类入口 当首页数据就绪', (tester) async {
    // 手机竖屏尺寸: 默认 800×600 测试面网格单元过宽会撑爆商品卡
    tester.view.physicalSize = const Size(1200, 2400);
    tester.view.devicePixelRatio = 3;
    addTearDown(tester.view.reset);
    await tester.pumpWidget(buildHome());
    await tester.pumpAndSettle();

    // 品牌横幅 (AppBar 与横幅都有店名)
    expect(
      find.text('Dev Store'),
      findsWidgets,
    ); // 品牌名来自 Brand (debug 无 STORE_KEY)
    expect(
      find.text('Local development'),
      findsOneWidget,
    ); // hero 文案来自 Brand.tagline
    // 分类入口
    expect(find.text('Teaware'), findsOneWidget);
    // 精选网格
    expect(find.text('Ice Smoke Teapot'), findsOneWidget);
  });
}
