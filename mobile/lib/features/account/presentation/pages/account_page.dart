import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import 'package:jade_cart_mobile/core/brand.dart';
import 'package:jade_cart_mobile/features/orders/presentation/controllers/order_history_controller.dart';
import 'package:jade_cart_mobile/ui/common.dart';

/// 我的页: 订单历史 / 店铺信息 / 关于
///
/// 后端地址写死在构建配置里 (StoreKey), 页面不提供任何服务器入口。
class AccountPage extends ConsumerStatefulWidget {
  const AccountPage({super.key});

  @override
  ConsumerState<AccountPage> createState() => _AccountPageState();
}

class _AccountPageState extends ConsumerState<AccountPage> {
  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      ref.read(orderHistoryControllerProvider.notifier).load();
    });
  }

  @override
  Widget build(BuildContext context) {
    final history = ref.watch(orderHistoryControllerProvider);

    return Scaffold(
      appBar: AppBar(title: const Text('Account')),
      body: ListView(
        // 底部留悬浮玻璃底栏高度
        padding: const EdgeInsets.only(bottom: 128),
        children: [
          _sectionTitle(context, 'Orders'),
          if (history.orderIds.isEmpty)
            const Padding(
              padding: EdgeInsets.symmetric(vertical: 24),
              child: EmptyView(
                title: 'No orders yet',
                subtitle: 'Your placed orders will appear here.',
                icon: Icons.receipt_long_outlined,
              ),
            )
          else
            for (final orderId in history.orderIds)
              ListTile(
                leading: const Icon(Icons.receipt_outlined),
                title: Text(orderId),
                trailing: const Icon(Icons.chevron_right),
                onTap: () => context.push('/orders/$orderId'),
              ),
          const Divider(height: 32),
          _sectionTitle(context, 'Store'),
          ListTile(
            leading: const Icon(Icons.storefront_outlined),
            title: Text(brand.name),
            subtitle: Text(Uri.parse(brand.apiUrl).host),
          ),
          const Divider(height: 32),
          _sectionTitle(context, 'About'),
          ListTile(
            leading: const Icon(Icons.info_outline),
            title: Text(brand.name),
            subtitle: const Text('Independent store mobile client'),
          ),
        ],
      ),
    );
  }

  Widget _sectionTitle(BuildContext context, String title) {
    return Padding(
      padding: const EdgeInsets.fromLTRB(16, 16, 16, 8),
      child: Text(
        title,
        style: Theme.of(context).textTheme.titleSmall?.copyWith(
          fontWeight: FontWeight.w700,
          color: Theme.of(context).colorScheme.onSurfaceVariant,
        ),
      ),
    );
  }
}
