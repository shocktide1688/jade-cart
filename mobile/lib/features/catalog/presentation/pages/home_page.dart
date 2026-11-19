import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import 'package:jade_cart_mobile/app_providers.dart';
import 'package:jade_cart_mobile/features/catalog/presentation/controllers/catalog_controllers.dart';
import 'package:jade_cart_mobile/ui/common.dart';

/// 首页: 精选商品 + 全部商品入口
class HomePage extends ConsumerWidget {
  const HomePage({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final baseUrl = ref.watch(serverUrlProvider);
    final featured = ref.watch(featuredProductsProvider);

    return Scaffold(
      appBar: AppBar(
        title: const Text('Jade Cart'),
        actions: [
          IconButton(
            onPressed: () => context.go('/shop'),
            icon: const Icon(Icons.search),
            tooltip: 'Browse products',
          ),
        ],
      ),
      body: RefreshIndicator(
        onRefresh: () async => ref.invalidate(featuredProductsProvider),
        child: featured.when(
          loading: () => const Center(child: CircularProgressIndicator()),
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
                  padding: const EdgeInsets.fromLTRB(16, 8, 16, 16),
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
                        onTap: () => context.go('/shop?slug=${product.slug}'),
                      );
                    }, childCount: products.length),
                  ),
                ),
              SliverToBoxAdapter(
                child: Padding(
                  padding: const EdgeInsets.fromLTRB(16, 8, 16, 32),
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
