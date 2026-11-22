import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:liquid_glass_widgets/liquid_glass_widgets.dart'
    show LiquidGlassWidgets;

import 'package:jade_cart_mobile/ui/glass_tab_bar.dart';

Widget _host(GlassTabBar bar) => LiquidGlassWidgets.wrap(
  child: MaterialApp(
    home: Scaffold(extendBody: true, bottomNavigationBar: bar),
  ),
);

void main() {
  testWidgets('应该渲染四个页签与搜索胶囊 当底栏挂载', (tester) async {
    await tester.pumpWidget(
      _host(
        GlassTabBar(
          selectedIndex: 0,
          onDestinationSelected: (_) {},
          onSearchSubmitted: (_) {},
        ),
      ),
    );
    await tester.pump();

    expect(find.text('Home'), findsWidgets);
    expect(find.text('Shop'), findsWidgets);
    expect(find.text('Cart').first, findsWidgets);
    expect(find.text('Account'), findsWidgets);
  });

  testWidgets('应该回传页签序号 当点击页签', (tester) async {
    int? tapped;
    await tester.pumpWidget(
      _host(
        GlassTabBar(
          selectedIndex: 0,
          onDestinationSelected: (index) => tapped = index,
          onSearchSubmitted: (_) {},
        ),
      ),
    );
    await tester.pump();

    await tester.tap(find.text('Cart').first);
    await tester.pump();

    expect(tapped, 2);
  });

  testWidgets('应该回传搜索词 当胶囊展开后提交', (tester) async {
    String? submitted;
    await tester.pumpWidget(
      _host(
        GlassTabBar(
          selectedIndex: 0,
          onDestinationSelected: (_) {},
          onSearchSubmitted: (query) => submitted = query,
        ),
      ),
    );
    await tester.pump();

    await tester.tap(find.byIcon(CupertinoIcons.search).first);
    await tester.pumpAndSettle();
    await tester.enterText(find.byType(EditableText).first, 'fair cup');
    await tester.testTextInput.receiveAction(TextInputAction.search);
    await tester.pumpAndSettle();

    expect(submitted, 'fair cup');
  });
}
