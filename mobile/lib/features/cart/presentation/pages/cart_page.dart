import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import 'package:jade_cart_mobile/app_providers.dart';
import 'package:jade_cart_mobile/core/trade_models.dart';
import 'package:jade_cart_mobile/features/cart/presentation/controllers/cart_controllers.dart';
import 'package:jade_cart_mobile/ui/common.dart';

/// 购物车页: 行列表 + 数量调整 + 去结算
class CartPage extends ConsumerStatefulWidget {
  const CartPage({super.key});

  @override
  ConsumerState<CartPage> createState() => _CartPageState();
}

class _CartPageState extends ConsumerState<CartPage> {
  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      ref.read(cartControllerProvider.notifier).load();
    });
  }

  @override
  Widget build(BuildContext context) {
    final state = ref.watch(cartControllerProvider);
    final baseUrl = ref.watch(serverUrlProvider);

    return Scaffold(
      appBar: AppBar(
        title: const Text('Cart'),
        actions: [
          IconButton(
            onPressed: () => ref.read(cartControllerProvider.notifier).load(),
            icon: const Icon(Icons.refresh),
            tooltip: 'Refresh',
          ),
        ],
      ),
      body: _body(context, state, baseUrl),
      bottomNavigationBar: state.isEmpty ? null : _checkoutBar(context, state),
    );
  }

  Widget _body(BuildContext context, CartState state, String baseUrl) {
    if (state.loading && state.items.isEmpty) {
      return const Center(child: CircularProgressIndicator());
    }
    if (state.error.isNotEmpty && state.items.isEmpty) {
      return ErrorView(
        message: state.error,
        onRetry: () => ref.read(cartControllerProvider.notifier).load(),
      );
    }
    if (state.isEmpty) {
      return const EmptyView(
        title: 'Your cart is empty',
        subtitle: 'Browse the shop to add products.',
        icon: Icons.shopping_cart_outlined,
      );
    }
    return ListView.separated(
      // 底部留出结算栏高度, 否则最后一行被遮住
      padding: const EdgeInsets.fromLTRB(16, 12, 16, 100),
      itemCount: state.items.length,
      separatorBuilder: (_, _) => const SizedBox(height: 12),
      itemBuilder: (context, index) {
        final line = state.items[index];
        return _CartLineCard(
          line: line,
          currency: state.currency,
          baseUrl: baseUrl,
          busy: state.mutating,
          onQuantity: (quantity) => ref
              .read(cartControllerProvider.notifier)
              .setQuantity(line.variantId, quantity),
          onRemove: () => ref
              .read(cartControllerProvider.notifier)
              .removeItem(line.variantId),
        );
      },
    );
  }

  Widget _checkoutBar(BuildContext context, CartState state) {
    final theme = Theme.of(context);
    return SafeArea(
      child: Container(
        padding: const EdgeInsets.fromLTRB(16, 12, 16, 12),
        decoration: BoxDecoration(
          color: theme.colorScheme.surface,
          border: Border(
            top: BorderSide(color: theme.colorScheme.outlineVariant),
          ),
        ),
        child: Row(
          children: [
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                mainAxisSize: MainAxisSize.min,
                children: [
                  Text(
                    'Subtotal',
                    style: theme.textTheme.bodySmall?.copyWith(
                      color: theme.colorScheme.onSurfaceVariant,
                    ),
                  ),
                  Text(
                    formatMoney(state.subtotalCents),
                    style: theme.textTheme.titleMedium?.copyWith(
                      fontWeight: FontWeight.w700,
                    ),
                  ),
                ],
              ),
            ),
            FilledButton(
              onPressed: () => context.push('/checkout'),
              child: const Text('Checkout'),
            ),
          ],
        ),
      ),
    );
  }
}

class _CartLineCard extends StatelessWidget {
  const _CartLineCard({
    required this.line,
    required this.currency,
    required this.baseUrl,
    required this.busy,
    required this.onQuantity,
    required this.onRemove,
  });

  final CartLine line;
  final String currency;
  final String baseUrl;
  final bool busy;
  final ValueChanged<int> onQuantity;
  final VoidCallback onRemove;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(12),
        child: Row(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            SizedBox(
              width: 72,
              height: 90,
              child: StoreImage(
                url: mediaUrl(baseUrl, line.mainImagePath, width: 200),
              ),
            ),
            const SizedBox(width: 12),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    line.productTitle,
                    maxLines: 2,
                    overflow: TextOverflow.ellipsis,
                    style: theme.textTheme.titleSmall?.copyWith(
                      fontWeight: FontWeight.w600,
                    ),
                  ),
                  if (line.spec.isNotEmpty) ...[
                    const SizedBox(height: 2),
                    Text(
                      line.spec,
                      style: theme.textTheme.bodySmall?.copyWith(
                        color: theme.colorScheme.onSurfaceVariant,
                      ),
                    ),
                  ],
                  const SizedBox(height: 8),
                  Row(
                    children: [
                      IconButton.outlined(
                        onPressed: busy || line.quantity <= 1
                            ? null
                            : () => onQuantity(line.quantity - 1),
                        icon: const Icon(Icons.remove),
                        visualDensity: VisualDensity.compact,
                      ),
                      Padding(
                        padding: const EdgeInsets.symmetric(horizontal: 8),
                        child: Text('${line.quantity}'),
                      ),
                      IconButton.outlined(
                        onPressed: busy
                            ? null
                            : () => onQuantity(line.quantity + 1),
                        icon: const Icon(Icons.add),
                        visualDensity: VisualDensity.compact,
                      ),
                      const Spacer(),
                      Text(
                        formatMoney(line.lineTotalCents),
                        style: theme.textTheme.titleSmall?.copyWith(
                          fontWeight: FontWeight.w700,
                        ),
                      ),
                    ],
                  ),
                  Align(
                    alignment: Alignment.centerRight,
                    child: TextButton.icon(
                      onPressed: busy ? null : onRemove,
                      icon: const Icon(Icons.delete_outline, size: 18),
                      label: const Text('Remove'),
                    ),
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }
}
