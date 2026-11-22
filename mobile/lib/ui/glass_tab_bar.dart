import 'package:flutter/material.dart';
import 'package:liquid_glass_widgets/liquid_glass_widgets.dart' as lgw;

/// 液态玻璃底栏: 四页签 + 右侧搜索胶囊 (liquid_glass_widgets 官方实现)
///
/// 选中胶囊滑动带水滴透镜放大 (liquid lens), 点搜索胶囊整根底栏变形
/// 展开成搜索框; 配合 Scaffold(extendBody: true) 让内容从玻璃下折射透过。
class GlassTabBar extends StatefulWidget {
  const GlassTabBar({
    required this.selectedIndex,
    required this.onDestinationSelected,
    required this.onSearchSubmitted,
    super.key,
  });

  final int selectedIndex;
  final ValueChanged<int> onDestinationSelected;
  final ValueChanged<String> onSearchSubmitted;

  /// (label, 未选中图标, 选中图标), 顺序与 HomeShell 分支一致
  static const tabs = <(String, IconData, IconData)>[
    ('Home', Icons.home_outlined, Icons.home_rounded),
    ('Shop', Icons.storefront_outlined, Icons.storefront_rounded),
    ('Cart', Icons.shopping_cart_outlined, Icons.shopping_cart_rounded),
    ('Account', Icons.person_outline, Icons.person_rounded),
  ];

  @override
  State<GlassTabBar> createState() => _GlassTabBarState();
}

class _GlassTabBarState extends State<GlassTabBar> {
  final _searchController = TextEditingController();

  @override
  void dispose() {
    _searchController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return lgw.GlassTabBar.searchable(
      tabs: [
        for (final (label, icon, activeIcon) in GlassTabBar.tabs)
          lgw.GlassTab(
            icon: Icon(icon),
            activeIcon: Icon(activeIcon),
            label: label,
          ),
      ],
      selectedIndex: widget.selectedIndex,
      onTabSelected: widget.onDestinationSelected,
      searchConfig: lgw.GlassSearchBarConfig(
        onSearchToggle: (_) {},
        hintText: 'Search products',
        controller: _searchController,
        autoFocusOnExpand: true,
        onSubmitted: widget.onSearchSubmitted,
      ),
    );
  }
}
