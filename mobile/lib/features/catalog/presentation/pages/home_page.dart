import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import 'package:jade_cart_mobile/app_providers.dart';
import 'package:jade_cart_mobile/core/brand.dart';
import 'package:jade_cart_mobile/core/catalog_models.dart';
import 'package:jade_cart_mobile/features/catalog/presentation/controllers/catalog_controllers.dart';
import 'package:jade_cart_mobile/ui/common.dart';
import 'package:jade_cart_mobile/ui/skeleton.dart';

/// 首页: 品牌横幅 + 分类入口 + 精选商品
class HomePage extends ConsumerWidget {
  const HomePage({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final baseUrl = ref.watch(serverUrlProvider);
    final featured = ref.watch(featuredProductsProvider);
    final categories = ref.watch(categoriesProvider);

    return Scaffold(
      appBar: AppBar(
        title: Text(brand.name),
        actions: [
          IconButton(
            onPressed: () => context.go('/shop'),
            icon: const Icon(Icons.search),
            tooltip: 'Browse products',
          ),
        ],
      ),
      body: RefreshIndicator(
        onRefresh: () async {
          ref.invalidate(featuredProductsProvider);
          ref.invalidate(categoriesProvider);
        },
        child: featured.when(
          loading: () => const ProductGridSkeleton(),
          error: (error, _) => ListView(
            children: [
              SizedBox(
                height: MediaQuery.sizeOf(context).height * 0.5,
                child: ErrorView(
                  message: '$error',
                  onRetry: () => ref.invalidate(featuredProductsProvider),
                ),
              ),
            ],
          ),
          data: (products) => CustomScrollView(
            slivers: [
              if (products.isNotEmpty)
                SliverToBoxAdapter(
                  child: _HeroBanner(product: products.first, baseUrl: baseUrl),
                ),
              SliverToBoxAdapter(
                child: categories.maybeWhen(
                  data: (items) => items.isEmpty
                      ? const SizedBox.shrink()
                      : _CategorySection(categories: items),
                  orElse: () => const SizedBox.shrink(),
                ),
              ),
              SliverToBoxAdapter(
                child: Padding(
                  padding: const EdgeInsets.fromLTRB(16, 16, 16, 4),
                  child: Text(
                    'Featured',
                    style: Theme.of(context).textTheme.titleLarge?.copyWith(
                      fontWeight: FontWeight.w700,
                    ),
                  ),
                ),
              ),
              if (products.isEmpty)
                const SliverToBoxAdapter(
                  child: EmptyView(
                    title: 'No featured products',
                    subtitle: 'Featured items will appear here.',
                  ),
                )
              else
                SliverPadding(
                  padding: const EdgeInsets.fromLTRB(16, 8, 16, 8),
                  sliver: SliverGrid(
                    gridDelegate:
                        const SliverGridDelegateWithFixedCrossAxisCount(
                          crossAxisCount: 2,
                          mainAxisSpacing: 14,
                          crossAxisSpacing: 14,
                          // 固定项高: 比宽高比更可控 (图片 4:5 + 两行标题 + 价格), 避免 RenderFlex 溢出
                          mainAxisExtent: 330,
                        ),
                    delegate: SliverChildBuilderDelegate((context, index) {
                      final product = products[index];
                      return ProductCard(
                        product: product,
                        baseUrl: baseUrl,
                        onTap: () => context.go('/shop/${product.slug}'),
                      );
                    }, childCount: products.length),
                  ),
                ),
              SliverToBoxAdapter(
                child: Padding(
                  // 底部留悬浮玻璃底栏高度, 避免按钮被遮住
                  padding: const EdgeInsets.fromLTRB(16, 8, 16, 128),
                  child: FilledButton.tonalIcon(
                    onPressed: () => context.go('/shop'),
                    icon: const Icon(Icons.storefront_outlined),
                    label: const Text('Browse all products'),
                  ),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

/// 品牌横幅: 用第一件精选商品的图做底, 压暗渐变 + 店名标语, 点进商品详情
class _HeroBanner extends StatelessWidget {
  const _HeroBanner({required this.product, required this.baseUrl});

  final ProductSummary product;
  final String baseUrl;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.fromLTRB(16, 12, 16, 4),
      child: ClipRRect(
        borderRadius: BorderRadius.circular(16),
        child: InkWell(
          onTap: () => context.go('/shop/${product.slug}'),
          child: SizedBox(
            height: 180,
            child: Stack(
              fit: StackFit.expand,
              children: [
                StoreImage(url: mediaUrl(baseUrl, product.mainImagePath)),
                const DecoratedBox(
                  decoration: BoxDecoration(
                    gradient: LinearGradient(
                      begin: Alignment.topCenter,
                      end: Alignment.bottomCenter,
                      colors: [Colors.transparent, Colors.black54],
                    ),
                  ),
                ),
                Positioned(
                  left: 16,
                  bottom: 14,
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        brand.name,
                        style: Theme.of(context).textTheme.titleLarge?.copyWith(
                          color: Colors.white,
                          fontWeight: FontWeight.w800,
                        ),
                      ),
                      Text(
                        brand.tagline,
                        style: Theme.of(
                          context,
                        ).textTheme.bodySmall?.copyWith(color: Colors.white70),
                      ),
                    ],
                  ),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}

/// 分类入口: 横滑胶囊条, 点了直接带筛选跳商店页
class _CategorySection extends ConsumerWidget {
  const _CategorySection({required this.categories});

  final List<CategoryNode> categories;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Padding(
          padding: const EdgeInsets.fromLTRB(16, 12, 16, 8),
          child: Text(
            'Categories',
            style: Theme.of(
              context,
            ).textTheme.titleMedium?.copyWith(fontWeight: FontWeight.w700),
          ),
        ),
        CategoryFilterBar(
          categories: categories,
          selectedSlug: null,
          onSelect: (slug) => _openCategory(context, ref, slug),
        ),
      ],
    );
  }

  void _openCategory(BuildContext context, WidgetRef ref, String? slug) {
    unawaited(
      ref.read(productListControllerProvider.notifier).selectCategory(slug),
    );
    context.go('/shop');
  }
}
