import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'package:jade_cart_mobile/app_providers.dart';

/// 订单本地历史 (匿名购物无账号, 订单 publicId 是唯一可追溯凭据)
final class OrderHistoryState {
  const OrderHistoryState({this.orderIds = const []});

  final List<String> orderIds;

  OrderHistoryState copyWith({List<String>? orderIds}) {
    return OrderHistoryState(orderIds: orderIds ?? this.orderIds);
  }
}

/// 订单历史控制器 (内存态; 写入时同时落 secure storage)
class OrderHistoryController extends StateNotifier<OrderHistoryState> {
  OrderHistoryController(this._store) : super(const OrderHistoryState());

  final OrderHistoryStore _store;

  Future<void> load() async {
    final ids = await _store.read();
    if (!mounted) return;
    state = state.copyWith(orderIds: ids);
  }

  /// 记录一笔订单 (去重, 新订单排最前)
  Future<void> record(String orderPublicId) async {
    if (orderPublicId.isEmpty) return;
    final updated = [
      orderPublicId,
      ...state.orderIds.where((id) => id != orderPublicId),
    ].take(50).toList(growable: false);
    state = state.copyWith(orderIds: updated);
    await _store.write(updated);
  }
}

/// 订单历史存储接口 (per-server key, 切服务器历史隔离)
abstract interface class OrderHistoryStore {
  Future<List<String>> read();
  Future<void> write(List<String> orderIds);
}

final orderHistoryControllerProvider =
    StateNotifierProvider<OrderHistoryController, OrderHistoryState>((ref) {
      return OrderHistoryController(ref.watch(orderHistoryStoreProvider));
    });
