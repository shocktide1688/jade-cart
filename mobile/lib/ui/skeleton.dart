import 'package:flutter/material.dart';

/// 商品网格骨架屏: 加载中展示脉冲占位卡, 替代裸菊花
class ProductGridSkeleton extends StatefulWidget {
  const ProductGridSkeleton({super.key, this.count = 6});

  final int count;

  @override
  State<ProductGridSkeleton> createState() => _ProductGridSkeletonState();
}

class _ProductGridSkeletonState extends State<ProductGridSkeleton>
    with SingleTickerProviderStateMixin {
  late final AnimationController _pulse = AnimationController(
    vsync: this,
    duration: const Duration(milliseconds: 1100),
  )..repeat(reverse: true);

  @override
  void dispose() {
    _pulse.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final base = Theme.of(context).colorScheme.surfaceContainerHighest;
    return GridView.builder(
      padding: const EdgeInsets.fromLTRB(16, 12, 16, 128),
      gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
        crossAxisCount: 2,
        mainAxisSpacing: 14,
        crossAxisSpacing: 14,
        mainAxisExtent: 330, // 与 ProductCard 行高一致, 加载完不跳变
      ),
      itemCount: widget.count,
      itemBuilder: (context, index) => AnimatedBuilder(
        animation: _pulse,
        builder: (context, child) => Opacity(
          opacity: 0.45 + 0.55 * _pulse.value,
          child: _SkeletonCard(color: base),
        ),
      ),
    );
  }
}

class _SkeletonCard extends StatelessWidget {
  const _SkeletonCard({required this.color});

  final Color color;

  @override
  Widget build(BuildContext context) {
    return Card(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          // 图区吃剩余高度, 不用固定宽高比: 宽屏(测试面/平板)下不会撑爆卡片
          Expanded(child: ColoredBox(color: color)),
          Padding(
            padding: const EdgeInsets.fromLTRB(12, 12, 12, 0),
            child: _bar(width: double.infinity),
          ),
          Padding(
            padding: const EdgeInsets.fromLTRB(12, 8, 12, 0),
            child: _bar(width: 64),
          ),
        ],
      ),
    );
  }

  Widget _bar({required double width}) => Container(
    width: width,
    height: 12,
    decoration: BoxDecoration(
      color: color,
      borderRadius: BorderRadius.circular(6),
    ),
  );
}
