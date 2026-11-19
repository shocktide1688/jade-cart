import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import 'package:jade_cart_mobile/app_providers.dart';
import 'package:jade_cart_mobile/core/server_config.dart';
import 'package:jade_cart_mobile/features/orders/presentation/controllers/order_history_controller.dart';
import 'package:jade_cart_mobile/ui/common.dart';

/// 我的页: 订单历史 / 服务器切换 / 关于
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
    final serverUrl = ref.watch(serverUrlProvider);

    return Scaffold(
      appBar: AppBar(title: const Text('Account')),
      body: ListView(
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
          _sectionTitle(context, 'Server'),
          ListTile(
            leading: const Icon(Icons.dns_outlined),
            title: const Text('API service'),
            subtitle: Text(serverUrl),
            trailing: const Icon(Icons.chevron_right),
            onTap: () => _openServerSheet(context, serverUrl),
          ),
          const Divider(height: 32),
          _sectionTitle(context, 'About'),
          const ListTile(
            leading: Icon(Icons.info_outline),
            title: Text('Jade Cart'),
            subtitle: Text('Independent store mobile client'),
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

  Future<void> _openServerSheet(BuildContext context, String currentUrl) {
    return showModalBottomSheet<void>(
      context: context,
      isScrollControlled: true,
      builder: (context) => _ServerSheet(currentUrl: currentUrl),
    );
  }
}

/// 服务器切换面板: 输入地址 → 测试连接 → 保存
class _ServerSheet extends ConsumerStatefulWidget {
  const _ServerSheet({required this.currentUrl});

  final String currentUrl;

  @override
  ConsumerState<_ServerSheet> createState() => _ServerSheetState();
}

class _ServerSheetState extends ConsumerState<_ServerSheet> {
  late final TextEditingController _controller = TextEditingController(
    text: widget.currentUrl,
  );
  String _status = '';
  bool _testing = false;
  bool _saving = false;

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Padding(
      padding: EdgeInsets.only(
        left: 16,
        right: 16,
        top: 20,
        bottom: MediaQuery.viewInsetsOf(context).bottom + 20,
      ),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            'API service',
            style: theme.textTheme.titleLarge?.copyWith(
              fontWeight: FontWeight.w700,
            ),
          ),
          const SizedBox(height: 4),
          Text(
            'Switch to another store backend',
            style: theme.textTheme.bodySmall?.copyWith(
              color: theme.colorScheme.onSurfaceVariant,
            ),
          ),
          const SizedBox(height: 16),
          TextField(
            controller: _controller,
            keyboardType: TextInputType.url,
            autocorrect: false,
            decoration: const InputDecoration(
              labelText: 'Server URL',
              hintText: 'http://localhost:8090',
            ),
          ),
          if (_status.isNotEmpty) ...[
            const SizedBox(height: 10),
            Text(
              _status,
              style: theme.textTheme.bodySmall?.copyWith(
                color: _status.startsWith('OK')
                    ? theme.colorScheme.primary
                    : theme.colorScheme.error,
              ),
            ),
          ],
          const SizedBox(height: 16),
          Row(
            children: [
              Expanded(
                child: OutlinedButton(
                  onPressed: _testing ? null : _test,
                  child: _testing
                      ? const SizedBox(
                          height: 18,
                          width: 18,
                          child: CircularProgressIndicator(strokeWidth: 2),
                        )
                      : const Text('Test'),
                ),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: FilledButton(
                  onPressed: _saving ? null : _save,
                  child: const Text('Save'),
                ),
              ),
            ],
          ),
        ],
      ),
    );
  }

  Future<void> _test() async {
    final url = _controller.text.trim();
    setState(() {
      _testing = true;
      _status = '';
    });
    try {
      final elapsed = await testServerConnection(url);
      if (!mounted) return;
      setState(() => _status = 'OK · ${elapsed.inMilliseconds} ms');
    } on Exception catch (error) {
      if (!mounted) return;
      setState(() => _status = '$error');
    } finally {
      if (mounted) setState(() => _testing = false);
    }
  }

  Future<void> _save() async {
    setState(() {
      _saving = true;
      _status = '';
    });
    try {
      await ref
          .read(serverUrlProvider.notifier)
          .update(_controller.text.trim());
      if (!mounted) return;
      Navigator.of(context).pop();
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(const SnackBar(content: Text('Server updated')));
    } on Exception catch (error) {
      if (!mounted) return;
      setState(() => _status = '$error');
    } finally {
      if (mounted) setState(() => _saving = false);
    }
  }
}
