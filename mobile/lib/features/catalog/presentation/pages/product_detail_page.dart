import 'package:flutter/material.dart';
import 'package:flutter_html/flutter_html.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:liquid_glass_widgets/liquid_glass_widgets.dart';

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
  final _pageController = PageController();
  int _page = 0;
  int _quantity = 1;
  bool _adding = false;

  @override
  void dispose() {
    _pageController.dispose();
    super.dispose();
  }

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
      // 底部留出吸底"Add to cart"按钮 + 数量步进器的高度, 否则内容被遮住
      padding: const EdgeInsets.only(bottom: 160),
      children: [
        // 4:5 + contain: 与 SSR 端 PDP 一致, 不裁切商家素材 (竖版营销图顶部常带文字)
        AspectRatio(
          aspectRatio: 4 / 5,
          child: Stack(
            fit: StackFit.expand,
            children: [
              PageView.builder(
                controller: _pageController,
                onPageChanged: (index) => setState(() => _page = index),
                itemCount: images.length,
                itemBuilder: (context, index) {
                  return ColoredBox(
                    color: Theme.of(
                      context,
                    ).colorScheme.surfaceContainerHighest,
                    child: StoreImage(
                      url: mediaUrl(baseUrl, images[index], width: 1600),
                      fit: BoxFit.contain,
                    ),
                  );
                },
              ),
              if (images.length > 1)
                Positioned(
                  right: 12,
                  bottom: 12,
                  child: _PageIndicator(current: _page, total: images.length),
                ),
            ],
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
              // 富文本描述 (后端 HtmlSanitizer 消毒过): 与 SSR 一致, 空则整块不渲染
              if (product.description.isNotEmpty) ...[
                const SizedBox(height: 24),
                Text(
                  'Description',
                  style: Theme.of(context).textTheme.titleMedium?.copyWith(
                    fontWeight: FontWeight.w700,
                  ),
                ),
                Html(
                  data: product.description,
                  style: {
                    'body': Style(
                      margin: Margins.zero,
                      padding: HtmlPaddings.zero,
                      fontSize: FontSize(14),
                      lineHeight: const LineHeight(1.6),
                    ),
                  },
                ),
              ],
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
    final enabled = !_adding && !needsSelection && product.totalAvailable > 0;
    // 悬浮玻璃加购栏, 与玻璃底栏同风格
    return SafeArea(
      child: Padding(
        padding: const EdgeInsets.fromLTRB(16, 8, 16, 12),
        child: GlassContainer(
          padding: const EdgeInsets.all(8),
          shape: const LiquidRoundedSuperellipse(borderRadius: 18),
          child: GlassButton.custom(
            onTap: enabled ? () => _addToCart(product) : () {},
            enabled: enabled,
            width: double.infinity,
            height: 48,
            style: GlassButtonStyle.prominent,
            shape: const LiquidRoundedSuperellipse(borderRadius: 12),
            child: _adding
                ? const SizedBox(
                    height: 20,
                    width: 20,
                    child: CircularProgressIndicator(strokeWidth: 2),
                  )
                : Text(
                    needsSelection ? 'Select an option' : 'Add to cart',
                    style: glassProminentTextStyle(Theme.of(context)),
                  ),
          ),
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

/// 图集页码角标 ("2 / 5")
class _PageIndicator extends StatelessWidget {
  const _PageIndicator({required this.current, required this.total});

  final int current;
  final int total;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
      decoration: BoxDecoration(
        color: theme.colorScheme.surface.withValues(alpha: 0.85),
        borderRadius: BorderRadius.circular(999),
        border: Border.all(color: theme.colorScheme.outlineVariant),
      ),
      child: Text(
        '${current + 1} / $total',
        style: theme.textTheme.labelSmall?.copyWith(
          fontWeight: FontWeight.w600,
        ),
      ),
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
