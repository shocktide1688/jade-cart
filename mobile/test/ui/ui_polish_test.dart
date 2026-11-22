import 'dart:math';

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:jade_cart_mobile/ui/app_theme.dart';
import 'package:jade_cart_mobile/ui/common.dart';
import 'package:jade_cart_mobile/ui/skeleton.dart';

void main() {
  group('formatMoney', () {
    test('应该带美元符号 当币种为 USD', () {
      expect(formatMoney(1990), r'$19.90');
      expect(formatMoney(9900, currency: 'USD'), r'$99.00');
    });

    test('应该带对应符号 当币种为 EUR/GBP/CNY', () {
      expect(formatMoney(100, currency: 'EUR'), '€1.00');
      expect(formatMoney(100, currency: 'GBP'), '£1.00');
      expect(formatMoney(100, currency: 'CNY'), '¥1.00');
    });

    test('应该用币种码前缀 当币种无符号映射', () {
      expect(formatMoney(100, currency: 'CHF'), 'CHF 1.00');
    });
  });

  group('glassProminentTextStyle', () {
    test('应该固定深玉绿 当浅色或深色主题 (玻璃药丸恒为浅色)', () {
      expect(
        glassProminentTextStyle(AppTheme.light()).color,
        AppTheme.seedColor,
      );
      expect(
        glassProminentTextStyle(AppTheme.dark()).color,
        AppTheme.seedColor,
      );
    });

    test('应该满足 WCAG AA 对比度 当文字落在浅色玻璃上', () {
      // 玻璃药丸近似白底: 深玉绿对白的相对亮度比需 >= 4.5
      final fg = AppTheme.seedColor;
      const bg = Colors.white;
      double lum(Color c) {
        double channel(double v) =>
            v <= 0.04045 ? v / 12.92 : pow((v + 0.055) / 1.055, 2.4).toDouble();
        return 0.2126 * channel(c.r) +
            0.7152 * channel(c.g) +
            0.0722 * channel(c.b);
      }

      final ratio = (lum(bg) + 0.05) / (lum(fg) + 0.05);
      expect(ratio, greaterThanOrEqualTo(4.5));
    });
  });

  group('ProductGridSkeleton', () {
    testWidgets('应该渲染指定数量的占位卡片 当加载中', (tester) async {
      // 手机竖屏尺寸: 默认 800×600 测试面太宽, 网格单元比真机宽得多会撑爆卡片
      tester.view.physicalSize = const Size(1200, 2400);
      tester.view.devicePixelRatio = 3;
      addTearDown(tester.view.reset);
      await tester.pumpWidget(
        const MaterialApp(home: Scaffold(body: ProductGridSkeleton(count: 4))),
      );
      await tester.pump();

      expect(find.byType(ProductGridSkeleton), findsOneWidget);
      // 每张占位卡 = 一张 Card (图区 + 标题条 + 价格条)
      expect(find.byType(Card), findsNWidgets(4));
    });
  });
}
