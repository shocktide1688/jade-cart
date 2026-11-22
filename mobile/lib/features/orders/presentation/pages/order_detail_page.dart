import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:url_launcher/url_launcher.dart';

import 'package:jade_cart_mobile/core/trade_models.dart';
import 'package:jade_cart_mobile/features/orders/presentation/controllers/order_detail_controller.dart';
import 'package:jade_cart_mobile/ui/common.dart';

/// 订单详情页: 展示状态/明细; 未到终态时轮询直到 PAID
class OrderDetailPage extends ConsumerStatefulWidget {
  const OrderDetailPage({required this.publicId, super.key});

  final String publicId;

  @override
  ConsumerState<OrderDetailPage> createState() => _OrderDetailPageState();
}

class _OrderDetailPageState extends ConsumerState<OrderDetailPage> {
  Timer? _pollTimer;

  @override
  void initState() {
    super.initState();
    // 页面自持轮询: 支付网关回调是异步的, 前端靠轮询感知状态变化 (ReqPilot 同款惯例)
    WidgetsBinding.instance.addPostFrameCallback((_) => _startPolling());
  }

  @override
  void dispose() {
    _pollTimer?.cancel();
    super.dispose();
  }

  void _startPolling() {
    _pollTimer?.cancel();
    _pollTimer = Timer.periodic(const Duration(seconds: 3), (_) {
      if (!mounted) return;
      ref.invalidate(orderDetailProvider(widget.publicId));
    });
  }

  @override
  Widget build(BuildContext context) {
    final order = ref.watch(orderDetailProvider(widget.publicId));

    return Scaffold(
      appBar: AppBar(title: const Text('Order')),
      body: order.when(
        // 轮询刷新时保留旧数据, 不再整页闪菊花
        skipLoadingOnRefresh: true,
        loading: () => const Center(child: CircularProgressIndicator()),
        error: (error, _) => ErrorView(
          message: '$error',
          onRetry: () => ref.invalidate(orderDetailProvider(widget.publicId)),
        ),
        data: (view) {
          // 到终态就停轮询
          if (view.status.isSettled) _pollTimer?.cancel();
          return _content(context, view, refreshing: order.isRefreshing);
        },
      ),
    );
  }

  Widget _content(
    BuildContext context,
    OrderView view, {
    required bool refreshing,
  }) {
    final theme = Theme.of(context);
    return ListView(
      padding: const EdgeInsets.fromLTRB(16, 16, 16, 24),
      children: [
        // 轮询中的细进度条, 不打断内容
        if (refreshing)
          const Padding(
            padding: EdgeInsets.only(bottom: 8),
            child: LinearProgressIndicator(minHeight: 2),
          ),
        _StatusBanner(status: view.status),
        const SizedBox(height: 16),
        Card(
          child: Padding(
            padding: const EdgeInsets.all(14),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                _row(context, 'Order number', view.orderNumber),
                _row(context, 'Email', view.email),
                if (view.shipName.isNotEmpty)
                  _row(context, 'Recipient', view.shipName),
                _row(context, 'Country', view.shippingCountry),
                if (view.paymentProvider.isNotEmpty)
                  _row(context, 'Payment', view.paymentProvider),
                const Divider(height: 24),
                for (final item in view.items)
                  Padding(
                    padding: const EdgeInsets.symmetric(vertical: 3),
                    child: Row(
                      children: [
                        Expanded(
                          child: Text(
                            '${item.quantity} × ${item.productTitle}',
                            overflow: TextOverflow.ellipsis,
                            style: theme.textTheme.bodyMedium,
                          ),
                        ),
                        Text(
                          formatMoney(
                            item.lineTotalCents,
                            currency: view.currency,
                          ),
                        ),
                      ],
                    ),
                  ),
                const Divider(height: 24),
                _row(
                  context,
                  'Subtotal',
                  formatMoney(view.subtotalCents, currency: view.currency),
                ),
                _row(
                  context,
                  'Shipping',
                  formatMoney(view.shippingCents, currency: view.currency),
                ),
                _row(
                  context,
                  'Total',
                  formatMoney(view.totalCents, currency: view.currency),
                  emphasize: true,
                ),
              ],
            ),
          ),
        ),
        if (view.trackingUrl.isNotEmpty) ...[
          const SizedBox(height: 16),
          OutlinedButton.icon(
            onPressed: () => launchUrl(
              Uri.parse(view.trackingUrl),
              mode: LaunchMode.externalApplication,
            ),
            icon: const Icon(Icons.local_shipping_outlined),
            label: const Text('Track shipment'),
          ),
        ],
      ],
    );
  }

  Widget _row(
    BuildContext context,
    String label,
    String value, {
    bool emphasize = false,
  }) {
    final theme = Theme.of(context);
    final style = emphasize
        ? theme.textTheme.titleMedium?.copyWith(fontWeight: FontWeight.w700)
        : theme.textTheme.bodyMedium;
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 4),
      child: Row(
        children: [
          Expanded(
            child: Text(
              label,
              style: style?.copyWith(color: theme.colorScheme.onSurfaceVariant),
            ),
          ),
          Text(value, style: style),
        ],
      ),
    );
  }
}

class _StatusBanner extends StatelessWidget {
  const _StatusBanner({required this.status});

  final OrderStatus status;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final (label, color) = switch (status) {
      OrderStatus.paid => ('Paid', theme.colorScheme.primary),
      OrderStatus.fulfilled => ('Fulfilled', theme.colorScheme.primary),
      OrderStatus.pending => ('Awaiting payment', theme.colorScheme.tertiary),
      OrderStatus.cancelled => ('Cancelled', theme.colorScheme.error),
      OrderStatus.refunded => ('Refunded', theme.colorScheme.outline),
      OrderStatus.paymentReview => (
        'Payment under review',
        theme.colorScheme.error,
      ),
      OrderStatus.unknown => ('Unknown status', theme.colorScheme.outline),
    };
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 20),
      decoration: BoxDecoration(
        color: color.withValues(alpha: 0.12),
        borderRadius: BorderRadius.circular(12),
      ),
      child: Column(
        children: [
          Icon(
            status.isSettled ? Icons.check_circle_outline : Icons.schedule,
            size: 32,
            color: color,
          ),
          const SizedBox(height: 8),
          Text(
            label,
            style: theme.textTheme.titleMedium?.copyWith(
              color: color,
              fontWeight: FontWeight.w700,
            ),
          ),
        ],
      ),
    );
  }
}
