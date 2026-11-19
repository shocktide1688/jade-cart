import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:url_launcher/url_launcher.dart';

import 'package:jade_cart_mobile/app_providers.dart';
import 'package:jade_cart_mobile/core/repositories.dart';
import 'package:jade_cart_mobile/core/trade_models.dart';
import 'package:jade_cart_mobile/features/orders/presentation/controllers/order_history_controller.dart';

/// 结算状态
final class CheckoutState {
  const CheckoutState({this.submitting = false, this.error = '', this.result});

  final bool submitting;
  final String error;
  final CheckoutResult? result;

  CheckoutState copyWith({
    bool? submitting,
    String? error,
    CheckoutResult? result,
  }) {
    return CheckoutState(
      submitting: submitting ?? this.submitting,
      error: error ?? this.error,
      result: result ?? this.result,
    );
  }
}

/// 结算控制器: 建单 → 拉起支付 (mock 内联确认 / 真实网关外部浏览器)
class CheckoutController extends StateNotifier<CheckoutState> {
  CheckoutController(this._repository, this._baseUrl, this._onOrderCreated)
    : super(const CheckoutState());

  final CheckoutRepository _repository;
  final String _baseUrl;
  final void Function(String orderPublicId) _onOrderCreated;

  /// 提交结算: 返回后若为 mock 支付则直接确认, 否则打开外部浏览器
  ///
  /// [onSuccess] 在建单成功后回调 (页面据此跳转订单详情), 避免读取异步结果产生竞态。
  Future<void> submit({
    required String email,
    required String shipName,
    required String country,
    required void Function(CheckoutResult result) onSuccess,
  }) async {
    state = state.copyWith(submitting: true, error: '');
    try {
      final result = await _repository.checkout(
        email: email,
        shipName: shipName,
        country: country,
      );
      if (!mounted) return;
      state = state.copyWith(submitting: false, result: result);
      _onOrderCreated(result.orderPublicId);
      onSuccess(result);
      await _openPayment(result);
    } on Exception catch (error) {
      if (!mounted) return;
      state = state.copyWith(submitting: false, error: '$error');
    }
  }

  Future<void> _openPayment(CheckoutResult result) async {
    if (result.isMockPay) {
      await _repository.confirmMockPay(result.orderPublicId);
      return;
    }
    final url = result.payUrl.startsWith('http')
        ? result.payUrl
        : '$_baseUrl${result.payUrl}';
    final launched = await launchUrl(
      Uri.parse(url),
      mode: LaunchMode.externalApplication,
    );
    if (!launched) {
      state = state.copyWith(error: 'Could not open the payment page');
    }
  }
}

final checkoutControllerProvider =
    StateNotifierProvider.autoDispose<CheckoutController, CheckoutState>((ref) {
      return CheckoutController(
        ref.watch(checkoutRepositoryProvider),
        ref.watch(serverUrlProvider),
        (orderPublicId) => ref
            .read(orderHistoryControllerProvider.notifier)
            .record(orderPublicId),
      );
    });
