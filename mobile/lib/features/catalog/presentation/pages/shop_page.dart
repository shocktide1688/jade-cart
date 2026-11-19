import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import 'package:jade_cart_mobile/app_providers.dart';
import 'package:jade_cart_mobile/features/catalog/presentation/controllers/catalog_controllers.dart';
import 'package:jade_cart_mobile/ui/common.dart';

/// 商品列表页: 搜索 + 分类筛选 + 商品网格
class ShopPage extends ConsumerStatefulWidget {
  const ShopPage({super.key});

  @override
  ConsumerState<ShopPage> createState() => _ShopPageState();
}

class _ShopPageState extends ConsumerState<ShopPage> {
  final _searchController = TextEditingController();

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      ref.read(productListControllerProvider.notifier).load();
    });
  }

  @override
  void dispose() {
    _searchController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final state = ref.watch(productListControllerProvider);
    final controller = ref.read(productListControllerProvider.notifier);
    final baseUrl = ref.watch(serverUrlProvider);

    return Scaffold(
      appBar: AppBar(
        title: const Text('Shop'),
        bottom: PreferredSize(
          preferredSize: const Size.fromHeight(104),
          child: Padding(
            padding: const EdgeInsets.fromLTRB(16, 0, 16, 12),
            child: Column(
              children: [
                TextField(
                  controller: _searchController,
                  textInputAction: TextInputAction.search,
                  onSubmitted: controller.search,
                  decoration: InputDecoration(
                    hintText: 'Search products',
                    prefixIcon: const Icon(Icons.search),
                    suffixIcon: IconButton(
                      onPressed: () {
                        _searchController.clear();
                        controller.search('');
                      },
                      icon: const Icon(Icons.clear),
                    ),
                  ),
                ),
                const SizedBox(height: 8),
                CategoryFilterBar(
                  categories: state.categories,
                  selectedSlug: state.selectedCategorySlug,
                  onSelect: controller.selectCategory,
                ),
              ],
            ),
          ),
        ),
      ),
      body: _body(context, state, controller, baseUrl),
    );
  }

  Widget _body(
    BuildContext context,
    ProductListState state,
    ProductListController controller,
    String baseUrl,
  ) {
    if (state.loading && state.items.isEmpty) {
      return const Center(child: CircularProgressIndicator());
    }
    if (state.error.isNotEmpty && state.items.isEmpty) {
      return ErrorView(message: state.error, onRetry: controller.load);
    }
    if (state.items.isEmpty) {
      return const EmptyView(
        title: 'No products found',
        subtitle: 'Try a different keyword or category.',
        icon: Icons.search_off_outlined,
      );
    }
    return GridView.builder(
      padding: const EdgeInsets.fromLTRB(16, 12, 16, 24),
      gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
        crossAxisCount: 2,
        mainAxisSpacing: 14,
        crossAxisSpacing: 14,
        childAspectRatio: 0.62,
      ),
      itemCount: state.items.length,
      itemBuilder: (context, index) {
        final product = state.items[index];
        return ProductCard(
          product: product,
          baseUrl: baseUrl,
          onTap: () => context.go('/shop/${product.slug}'),
        );
      },
    );
  }
}
