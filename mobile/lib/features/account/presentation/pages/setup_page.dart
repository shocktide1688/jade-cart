import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import 'package:jade_cart_mobile/app_providers.dart';
import 'package:jade_cart_mobile/core/server_config.dart';

/// 配置引导页: release 包未内置店铺地址时展示, 让用户填自己的店铺地址
class SetupPage extends ConsumerStatefulWidget {
  const SetupPage({super.key});

  @override
  ConsumerState<SetupPage> createState() => _SetupPageState();
}

class _SetupPageState extends ConsumerState<SetupPage> {
  final _controller = TextEditingController();
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
    return Scaffold(
      body: SafeArea(
        child: Center(
          child: SingleChildScrollView(
            padding: const EdgeInsets.all(24),
            child: ConstrainedBox(
              constraints: const BoxConstraints(maxWidth: 420),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  Icon(
                    Icons.storefront_outlined,
                    size: 56,
                    color: theme.colorScheme.primary,
                  ),
                  const SizedBox(height: 20),
                  Text(
                    'Connect your store',
                    textAlign: TextAlign.center,
                    style: theme.textTheme.headlineSmall?.copyWith(
                      fontWeight: FontWeight.w700,
                    ),
                  ),
                  const SizedBox(height: 8),
                  Text(
                    'Enter your store address to load products and orders.',
                    textAlign: TextAlign.center,
                    style: theme.textTheme.bodyMedium?.copyWith(
                      color: theme.colorScheme.onSurfaceVariant,
                    ),
                  ),
                  const SizedBox(height: 28),
                  TextField(
                    controller: _controller,
                    keyboardType: TextInputType.url,
                    autocorrect: false,
                    decoration: const InputDecoration(
                      labelText: 'Store address',
                      hintText: 'https://your-store.example.com',
                    ),
                    onSubmitted: (_) => _save(),
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
                  const SizedBox(height: 20),
                  Row(
                    children: [
                      Expanded(
                        child: OutlinedButton(
                          onPressed: _testing ? null : _test,
                          child: _testing
                              ? const SizedBox(
                                  height: 18,
                                  width: 18,
                                  child: CircularProgressIndicator(
                                    strokeWidth: 2,
                                  ),
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
            ),
          ),
        ),
      ),
    );
  }

  Future<void> _test() async {
    final url = _controller.text.trim();
    if (url.isEmpty) {
      setState(() => _status = 'Enter your store address first');
      return;
    }
    setState(() {
      _testing = true;
      _status = '';
    });
    try {
      final elapsed = await testServerConnection(url);
      if (!mounted) return;
      setState(() => _status = 'Connected · ${elapsed.inMilliseconds} ms');
    } on Exception catch (error) {
      if (!mounted) return;
      setState(() => _status = '$error');
    } finally {
      if (mounted) setState(() => _testing = false);
    }
  }

  Future<void> _save() async {
    final url = _controller.text.trim();
    if (url.isEmpty) {
      setState(() => _status = 'Enter your store address first');
      return;
    }
    setState(() {
      _saving = true;
      _status = '';
    });
    try {
      await ref.read(serverUrlProvider.notifier).update(url);
      if (!mounted) return;
      if (ref.read(serverUrlProvider).isNotEmpty) {
        context.go('/home');
      }
    } on Exception catch (error) {
      if (!mounted) return;
      setState(() => _status = '$error');
    } finally {
      if (mounted) setState(() => _saving = false);
    }
  }
}
