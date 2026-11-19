import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'package:jade_cart_mobile/app_providers.dart';
import 'package:jade_cart_mobile/core/trade_models.dart';

/// 订单详情 provider (按 publicId 取)
final orderDetailProvider = FutureProvider.family<OrderView, String>((
  ref,
  publicId,
) {
  return ref.watch(checkoutRepositoryProvider).order(publicId);
});
