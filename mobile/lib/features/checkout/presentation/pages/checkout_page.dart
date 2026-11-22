import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import 'package:jade_cart_mobile/features/cart/presentation/controllers/cart_controllers.dart';
import 'package:jade_cart_mobile/features/checkout/presentation/controllers/checkout_controllers.dart';
import 'package:jade_cart_mobile/ui/common.dart';

/// 结算页: 邮箱 / 收件人 / 国家 → 建单并拉起支付
class CheckoutPage extends ConsumerStatefulWidget {
  const CheckoutPage({super.key});

  @override
  ConsumerState<CheckoutPage> createState() => _CheckoutPageState();
}

class _CheckoutPageState extends ConsumerState<CheckoutPage> {
  final _formKey = GlobalKey<FormState>();
  final _emailController = TextEditingController();
  final _nameController = TextEditingController();
  String _country = 'US';

  /// 常用国家 (店面结算只需国家码, 运费由后端按国家算)
  static const _countries = ['US', 'GB', 'DE', 'FR', 'CA', 'AU', 'JP', 'SG'];

  @override
  void dispose() {
    _emailController.dispose();
    _nameController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final state = ref.watch(checkoutControllerProvider);
    final cartState = ref.watch(cartControllerProvider);
    final theme = Theme.of(context);

    return Scaffold(
      appBar: AppBar(title: const Text('Checkout')),
      body: Form(
        key: _formKey,
        child: ListView(
          padding: const EdgeInsets.fromLTRB(16, 16, 16, 24),
          children: [
            if (cartState.items.isNotEmpty)
              Card(
                child: Padding(
                  padding: const EdgeInsets.all(14),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        'Order summary',
                        style: theme.textTheme.titleSmall?.copyWith(
                          fontWeight: FontWeight.w700,
                        ),
                      ),
                      const SizedBox(height: 8),
                      for (final line in cartState.items)
                        Padding(
                          padding: const EdgeInsets.symmetric(vertical: 3),
                          child: Row(
                            children: [
                              Expanded(
                                child: Text(
                                  '${line.quantity} × ${line.productTitle}',
                                  style: theme.textTheme.bodyMedium,
                                  overflow: TextOverflow.ellipsis,
                                ),
                              ),
                              Text(
                                formatMoney(
                                  line.lineTotalCents,
                                  currency: cartState.currency,
                                ),
                              ),
                            ],
                          ),
                        ),
                      const Divider(height: 20),
                      Row(
                        children: [
                          Text(
                            'Subtotal',
                            style: theme.textTheme.bodyMedium?.copyWith(
                              fontWeight: FontWeight.w600,
                            ),
                          ),
                          const Spacer(),
                          Text(
                            formatMoney(
                              cartState.subtotalCents,
                              currency: cartState.currency,
                            ),
                            style: theme.textTheme.titleMedium?.copyWith(
                              fontWeight: FontWeight.w700,
                            ),
                          ),
                        ],
                      ),
                    ],
                  ),
                ),
              ),
            const SizedBox(height: 16),
            TextFormField(
              controller: _emailController,
              keyboardType: TextInputType.emailAddress,
              autofillHints: const [AutofillHints.email],
              decoration: const InputDecoration(
                labelText: 'Email',
                helperText: 'Order confirmation is sent to this address',
              ),
              validator: (value) {
                final text = value?.trim() ?? '';
                if (text.isEmpty) return 'Email is required';
                if (!text.contains('@')) return 'Enter a valid email address';
                return null;
              },
            ),
            const SizedBox(height: 12),
            TextFormField(
              controller: _nameController,
              textCapitalization: TextCapitalization.words,
              autofillHints: const [AutofillHints.name],
              decoration: const InputDecoration(
                labelText: 'Recipient name (optional)',
              ),
            ),
            const SizedBox(height: 12),
            DropdownButtonFormField<String>(
              initialValue: _country,
              decoration: const InputDecoration(labelText: 'Shipping country'),
              items: [
                for (final code in _countries)
                  DropdownMenuItem(value: code, child: Text(code)),
              ],
              onChanged: (value) {
                if (value != null) setState(() => _country = value);
              },
            ),
            if (state.error.isNotEmpty) ...[
              const SizedBox(height: 16),
              Text(
                state.error,
                style: theme.textTheme.bodyMedium?.copyWith(
                  color: theme.colorScheme.error,
                ),
              ),
            ],
            const SizedBox(height: 24),
            FilledButton(
              onPressed: state.submitting ? null : _submit,
              child: state.submitting
                  ? const SizedBox(
                      height: 20,
                      width: 20,
                      child: CircularProgressIndicator(strokeWidth: 2),
                    )
                  : const Text('Place order'),
            ),
          ],
        ),
      ),
    );
  }

  void _submit() {
    if (!(_formKey.currentState?.validate() ?? false)) return;
    ref
        .read(checkoutControllerProvider.notifier)
        .submit(
          email: _emailController.text.trim(),
          shipName: _nameController.text.trim(),
          country: _country,
          onSuccess: (result) {
            if (!mounted) return;
            context.pushReplacement('/orders/${result.orderPublicId}');
          },
        );
  }
}
