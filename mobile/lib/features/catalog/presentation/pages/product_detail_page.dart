import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'package:jade_cart_mobile/app_providers.dart';
import 'package:jade_cart_mobile/core/catalog_models.dart';
import 'package:jade_cart_mobile/features/cart/presentation/controllers/cart_controllers.dart';
import 'package:jade_cart_mobile/features/catalog/presentation/controllers/product_detail_controller.dart';
import 'package:jade_cart_mobile/ui/common.dart';

/// 商品详情页: 图集 / 规格选择 / 数量 / 加购
class ProductDetailPage extends ConsumerStatefulWidget {
  const ProductDetailPage({required this.slug, super.key});

  final String slug;

  @override
  ConsumerState<ProductDetailPage> createState() => _ProductDetailPageState();
}

class _ProductDetailPageState extends ConsumerState<ProductDetailPage> {
  int _quantity = 1;
  bool _adding = false;

  @override
  Widget build(BuildContext context) {
    final detail = ref.watch(productDetailProvider(widget.slug));
    final baseUrl = ref.watch(serverUrlProvider);

    return Scaffold(
      appBar: AppBar(title: const Text('Product')),
      body: detail.when(
        loading: () => const Center(child: CircularProgressIndicator()),
        error: (error, _) => ErrorView(
          message: '$error',
          onRetry: () => ref.invalidate(productDetailProvider(widget.slug)),
        ),
        data: (product) => _content(context, product, baseUrl),
      ),
      bottomNavigationBar: detail.maybeWhen(
        data: (product) => _bottomBar(context, product),
        orElse: () => null,
      ),
    );
  }

  Widget _content(BuildContext context, ProductDetail product, String baseUrl) {
    final images = product.imagePaths.isEmpty
        ? [product.mainImagePath]
        : product.imagePaths;
    return ListView(
      padding: const EdgeInsets.only(bottom: 24),
      children: [
        AspectRatio(
          aspectRatio: 1,
          child: PageView.builder(
            itemCount: images.length,
            itemBuilder: (context, index) {
              return StoreImage(
                url: mediaUrl(baseUrl, images[index], width: 1600),
              );
            },
          ),
        ),
        Padding(
          padding: const EdgeInsets.fromLTRB(16, 16, 16, 0),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                product.title,
                style: Theme.of(
                  context,
                ).textTheme.titleLarge?.copyWith(fontWeight: FontWeight.w700),
              ),
              const SizedBox(height: 8),
              Text(
                formatMoney(product.minPriceCents),
                style: Theme.of(context).textTheme.titleMedium?.copyWith(
                  color: Theme.of(context).colorScheme.primary,
                  fontWeight: FontWeight.w700,
                ),
              ),
              const SizedBox(height: 4),
              Text(
                product.totalAvailable > 0
                    ? '${product.totalAvailable} in stock'
                    : 'Sold out',
                style: Theme.of(context).textTheme.bodySmall?.copyWith(
                  color: product.totalAvailable > 0
                      ? Theme.of(context).colorScheme.onSurfaceVariant
                      : Theme.of(context).colorScheme.error,
                ),
              ),
              if (product.options.isNotEmpty) ...[
                const SizedBox(height: 20),
                _VariantPicker(product: product),
              ],
              const SizedBox(height: 20),
              _QuantityRow(
                quantity: _quantity,
                onChanged: (value) => setState(() => _quantity = value),
              ),
            ],
          ),
        ),
      ],
    );
  }

  Widget _bottomBar(BuildContext context, ProductDetail product) {
    final selection = ref.watch(variantSelectionProvider);
    final needsSelection =
        product.variants.length > 1 && !selection.hasSelection;
    return SafeArea(
      child: Padding(
        padding: const EdgeInsets.fromLTRB(16, 8, 16, 12),
        child: FilledButton(
          onPressed: (_adding || needsSelection || product.totalAvailable <= 0)
              ? null
              : () => _addToCart(product),
          child: _adding
              ? const SizedBox(
                  height: 20,
                  width: 20,
                  child: CircularProgressIndicator(strokeWidth: 2),
                )
              : Text(needsSelection ? 'Select an option' : 'Add to cart'),
        ),
      ),
    );
  }

  Future<void> _addToCart(ProductDetail product) async {
    final selection = ref.read(variantSelectionProvider);
    // 无变体轴的商品取第一条变体 (后端保证商品必有 >=1 变体)
    final variantId = selection.variantId ?? product.variants.first.id;
    setState(() => _adding = true);
    try {
      await ref
          .read(cartControllerProvider.notifier)
          .addItem(variantId, _quantity);
      if (!mounted) return;
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(const SnackBar(content: Text('Added to cart')));
    } on Exception catch (error) {
      if (!mounted) return;
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(SnackBar(content: Text('$error')));
    } finally {
      if (mounted) setState(() => _adding = false);
    }
  }
}

/// 变体选择: 按规格文本列出可选变体
class _VariantPicker extends ConsumerWidget {
  const _VariantPicker({required this.product});

  final ProductDetail product;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final selected = ref.watch(variantSelectionProvider);
    final theme = Theme.of(context);
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          'Options',
          style: theme.textTheme.titleSmall?.copyWith(
            fontWeight: FontWeight.w700,
          ),
        ),
        const SizedBox(height: 8),
        Wrap(
          spacing: 8,
          runSpacing: 8,
          children: [
            for (final variant in product.variants)
              ChoiceChip(
                label: Text(variant.spec.isEmpty ? variant.sku : variant.spec),
                selected: selected.variantId == variant.id,
                onSelected: variant.inStock
                    ? (_) => ref
                          .read(variantSelectionProvider.notifier)
                          .select(variant)
                    : null,
              ),
          ],
        ),
      ],
    );
  }
}

/// 数量步进器
class _QuantityRow extends StatelessWidget {
  const _QuantityRow({required this.quantity, required this.onChanged});

  final int quantity;
  final ValueChanged<int> onChanged;

  @override
  Widget build(BuildContext context) {
    return Row(
      children: [
        Text(
          'Quantity',
          style: Theme.of(
            context,
          ).textTheme.titleSmall?.copyWith(fontWeight: FontWeight.w700),
        ),
        const SizedBox(width: 16),
        IconButton.outlined(
          onPressed: quantity > 1 ? () => onChanged(quantity - 1) : null,
          icon: const Icon(Icons.remove),
        ),
        Text('$quantity', style: Theme.of(context).textTheme.titleMedium),
        IconButton.outlined(
          onPressed: () => onChanged(quantity + 1),
          icon: const Icon(Icons.add),
        ),
      ],
    );
  }
}
