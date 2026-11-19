import 'models.dart';

/// 购物车行 (对应后端 CartView.LineView)
final class CartLine {
  const CartLine({
    required this.variantId,
    required this.productTitle,
    required this.productSlug,
    required this.sku,
    required this.spec,
    required this.unitPriceCents,
    required this.quantity,
    required this.available,
    required this.lineTotalCents,
    required this.mainImagePath,
  });

  final int variantId;
  final String productTitle;
  final String productSlug;
  final String sku;
  final String spec;
  final int unitPriceCents;
  final int quantity;
  final int available;
  final int lineTotalCents;

  /// 商品主图相对路径 (后端补字段, 供购物车列表展示)
  final String mainImagePath;

  factory CartLine.fromJson(JsonMap json) {
    return CartLine(
      variantId: intOf(json, 'variantId'),
      productTitle: stringOf(json, 'productTitle'),
      productSlug: stringOf(json, 'productSlug'),
      sku: stringOf(json, 'sku'),
      spec: stringOf(json, 'spec'),
      unitPriceCents: intOf(json, 'unitPriceCents'),
      quantity: intOf(json, 'quantity'),
      available: intOf(json, 'available'),
      lineTotalCents: intOf(json, 'lineTotalCents'),
      mainImagePath: stringOf(json, 'mainImagePath'),
    );
  }
}

/// 购物车 (对应后端 CartView)
final class CartView {
  const CartView({
    required this.cartPublicId,
    required this.currency,
    required this.items,
    required this.subtotalCents,
  });

  final String cartPublicId;
  final String currency;
  final List<CartLine> items;
  final int subtotalCents;

  factory CartView.fromJson(JsonMap json) {
    return CartView(
      cartPublicId: stringOf(json, 'cartPublicId'),
      currency: stringOf(json, 'currency', 'USD'),
      items: asJsonMapList(
        json['items'],
      ).map(CartLine.fromJson).toList(growable: false),
      subtotalCents: intOf(json, 'subtotalCents'),
    );
  }
}

/// 结算结果 (对应后端 CheckoutResult)
final class CheckoutResult {
  const CheckoutResult({
    required this.orderPublicId,
    required this.orderNumber,
    required this.totalCents,
    required this.currency,
    required this.payUrl,
  });

  final String orderPublicId;
  final String orderNumber;
  final int totalCents;
  final String currency;

  /// 收银台地址: Stripe/PayPal 为外部链接, mock 为站内相对路径 (/pay/mock/{publicId})
  final String payUrl;

  /// mock 网关 (开发默认) → App 内直接调 JSON confirm; 真实网关 → 外部浏览器打开 payUrl
  bool get isMockPay => payUrl.startsWith('/pay/mock/');

  factory CheckoutResult.fromJson(JsonMap json) {
    return CheckoutResult(
      orderPublicId: stringOf(json, 'orderPublicId'),
      orderNumber: stringOf(json, 'orderNumber'),
      totalCents: intOf(json, 'totalCents'),
      currency: stringOf(json, 'currency', 'USD'),
      payUrl: stringOf(json, 'payUrl'),
    );
  }
}

/// 订单状态 (后端 Order 状态机: PENDING → PAID → FULFILLED, 侧向 CANCELLED/REFUNDED/PAYMENT_REVIEW)
enum OrderStatus {
  pending,
  paid,
  fulfilled,
  cancelled,
  refunded,
  paymentReview,
  unknown;

  static OrderStatus of(String? raw) {
    return switch (raw) {
      'PENDING' => pending,
      'PAID' => paid,
      'FULFILLED' => fulfilled,
      'CANCELLED' => cancelled,
      'REFUNDED' => refunded,
      'PAYMENT_REVIEW' => paymentReview,
      _ => unknown,
    };
  }

  /// 终态/可停轮询的状态
  bool get isSettled =>
      this == paid ||
      this == fulfilled ||
      this == cancelled ||
      this == refunded;
}

/// 订单行 (对应后端 OrderView.ItemView)
final class OrderItemView {
  const OrderItemView({
    required this.productTitle,
    required this.variantSpec,
    required this.sku,
    required this.unitPriceCents,
    required this.quantity,
    required this.lineTotalCents,
  });

  final String productTitle;
  final String variantSpec;
  final String sku;
  final int unitPriceCents;
  final int quantity;
  final int lineTotalCents;

  factory OrderItemView.fromJson(JsonMap json) {
    return OrderItemView(
      productTitle: stringOf(json, 'productTitle'),
      variantSpec: stringOf(json, 'variantSpec'),
      sku: stringOf(json, 'sku'),
      unitPriceCents: intOf(json, 'unitPriceCents'),
      quantity: intOf(json, 'quantity'),
      lineTotalCents: intOf(json, 'lineTotalCents'),
    );
  }
}

/// 订单 (对应后端 OrderView)
final class OrderView {
  const OrderView({
    required this.publicId,
    required this.orderNumber,
    required this.email,
    required this.shipName,
    required this.status,
    required this.subtotalCents,
    required this.shippingCents,
    required this.totalCents,
    required this.currency,
    required this.shippingCountry,
    required this.paymentProvider,
    required this.items,
    required this.trackingUrl,
  });

  final String publicId;
  final String orderNumber;
  final String email;
  final String shipName;
  final OrderStatus status;
  final int subtotalCents;
  final int shippingCents;
  final int totalCents;
  final String currency;
  final String shippingCountry;
  final String paymentProvider;
  final List<OrderItemView> items;
  final String trackingUrl;

  factory OrderView.fromJson(JsonMap json) {
    final shipment = json['shipment'];
    String trackingUrl = '';
    if (shipment is Map) {
      trackingUrl = stringOf(asJsonMap(shipment), 'trackingUrl');
    }
    return OrderView(
      publicId: stringOf(json, 'publicId'),
      orderNumber: stringOf(json, 'orderNumber'),
      email: stringOf(json, 'email'),
      shipName: stringOf(json, 'shipName'),
      status: OrderStatus.of(json['status'] as String?),
      subtotalCents: intOf(json, 'subtotalCents'),
      shippingCents: intOf(json, 'shippingCents'),
      totalCents: intOf(json, 'totalCents'),
      currency: stringOf(json, 'currency', 'USD'),
      shippingCountry: stringOf(json, 'shippingCountry'),
      paymentProvider: stringOf(json, 'paymentProvider'),
      items: asJsonMapList(
        json['items'],
      ).map(OrderItemView.fromJson).toList(growable: false),
      trackingUrl: trackingUrl,
    );
  }
}
