import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:liquid_glass_widgets/liquid_glass_widgets.dart';

import 'package:jade_cart_mobile/app_providers.dart';
import 'package:jade_cart_mobile/core/catalog_models.dart';
import 'package:jade_cart_mobile/features/catalog/presentation/controllers/product_detail_controller.dart';
import 'package:jade_cart_mobile/features/catalog/presentation/pages/product_detail_page.dart';

ProductDetail _detail({String description = ''}) => ProductDetail(
  publicId: 'p1',
  title: 'Ice Smoke Teapot',
  slug: 'ice-smoke-teapot',
  description: description,
  seoDescription: '',
  mainImagePath: '', // 空路径走占位图, 测试不碰网络
  options: const [],
  variants: const [
    ProductVariant(
      id: 1,
      sku: 'SKU1',
      priceCents: 9900,
      spec: 'Default',
      available: 5,
    ),
  ],
  imagePaths: const [],
  categorySlugs: const [],
);

void main() {
  Widget buildPage(ProductDetail detail) => ProviderScope(
    overrides: [
      serverUrlProvider.overrideWithValue('http://test.local'),
      productDetailProvider(
        'ice-smoke-teapot',
      ).overrideWith((ref) async => detail),
    ],
    child: LiquidGlassWidgets.wrap(
      brightnessResolver: Theme.maybeBrightnessOf,
      child: const MaterialApp(
        home: ProductDetailPage(slug: 'ice-smoke-teapot'),
      ),
    ),
  );

  setUp(() {
    TestWidgetsFlutterBinding.ensureInitialized();
    LiquidGlassWidgets.initialize();
  });

  testWidgets('应该渲染富文本描述区块 当商品有描述', (tester) async {
    // 手机竖屏尺寸: 默认 800×600 测试面太宽
    tester.view.physicalSize = const Size(1200, 2400);
    tester.view.devicePixelRatio = 3;
    addTearDown(tester.view.reset);
    await tester.pumpWidget(
      buildPage(
        _detail(
          description: '<h2>Brewing</h2><p>Wood-fired handmade teapot.</p>',
        ),
      ),
    );
    await tester.pumpAndSettle();

    // 描述区块标题 + HTML 渲染出的正文 (flutter_html 输出 RichText)
    expect(find.text('Description'), findsOneWidget);
    expect(
      find.textContaining('Wood-fired handmade teapot.', findRichText: true),
      findsOneWidget,
    );
  });

  testWidgets('应该隐藏描述区块 当商品描述为空 (与 SSR 行为一致)', (tester) async {
    tester.view.physicalSize = const Size(1200, 2400);
    tester.view.devicePixelRatio = 3;
    addTearDown(tester.view.reset);
    await tester.pumpWidget(buildPage(_detail()));
    await tester.pumpAndSettle();

    expect(find.text('Description'), findsNothing);
  });
}
