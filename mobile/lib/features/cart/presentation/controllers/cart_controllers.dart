import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'package:jade_cart_mobile/app_providers.dart';
import 'package:jade_cart_mobile/core/repositories.dart';
import 'package:jade_cart_mobile/core/trade_models.dart';

/// 购物车状态 (单一真源: 购物车页数据与页签角标共用)
final class CartState {
  const CartState({
    this.cart,
    this.count = 0,
    this.loading = false,
    this.mutating = false,
    this.error = '',
  });

  final CartView? cart;
  final int count;
  final bool loading;
  final bool mutating;
  final String error;

  List<CartLine> get items => cart?.items ?? const [];

  bool get isEmpty => items.isEmpty;

  int get subtotalCents => cart?.subtotalCents ?? 0;

  String get currency => cart?.currency ?? 'USD';

  CartState copyWith({
    CartView? cart,
    int? count,
    bool? loading,
    bool? mutating,
    String? error,
  }) {
    return CartState(
      cart: cart ?? this.cart,
      count: count ?? this.count,
      loading: loading ?? this.loading,
      mutating: mutating ?? this.mutating,
      error: error ?? this.error,
    );
  }
}

/// 购物车控制器: 加载 / 加购 / 改数量 / 删除
class CartController extends StateNotifier<CartState> {
  CartController(this._repository) : super(const CartState());

  final CartRepository _repository;

  Future<void> load() async {
    state = state.copyWith(loading: true, error: '');
    try {
      final cart = await _repository.cart();
      if (!mounted) return;
      state = state.copyWith(
        cart: cart,
        count: cart.items.length,
        loading: false,
      );
    } on Exception catch (error) {
      if (!mounted) return;
      state = state.copyWith(loading: false, error: '$error');
    }
  }

  Future<void> addItem(int variantId, int quantity) async {
    state = state.copyWith(mutating: true, error: '');
    try {
      final cart = await _repository.addItem(variantId, quantity: quantity);
      if (!mounted) return;
      state = state.copyWith(
        cart: cart,
        count: cart.items.length,
        mutating: false,
      );
    } on Exception catch (error) {
      if (!mounted) return;
      state = state.copyWith(mutating: false, error: '$error');
      rethrow;
    }
  }

  /// quantity 0 → 后端移除该行
  Future<void> setQuantity(int variantId, int quantity) async {
    state = state.copyWith(mutating: true, error: '');
    try {
      final cart = await _repository.updateItem(variantId, quantity);
      if (!mounted) return;
      state = state.copyWith(
        cart: cart,
        count: cart.items.length,
        mutating: false,
      );
    } on Exception catch (error) {
      if (!mounted) return;
      state = state.copyWith(mutating: false, error: '$error');
      rethrow;
    }
  }

  Future<void> removeItem(int variantId) async {
    await setQuantity(variantId, 0);
  }
}

final cartControllerProvider = StateNotifierProvider<CartController, CartState>(
  (ref) {
    return CartController(ref.watch(cartRepositoryProvider));
  },
);

/// 页签角标 (独立读取, 不打扰购物车页状态)
final cartCountProvider = FutureProvider<int>((ref) {
  return ref.watch(cartRepositoryProvider).count();
});
