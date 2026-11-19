import 'dart:convert';
import 'dart:typed_data';

import 'package:dio/dio.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:jade_cart_mobile/core/api_client.dart';
import 'package:jade_cart_mobile/core/repositories.dart';

/// 回放固定响应并记录请求的适配器
final class _StubAdapter implements HttpClientAdapter {
  _StubAdapter(this._body);

  final String _body;
  final List<RequestOptions> captured = <RequestOptions>[];

  @override
  Future<ResponseBody> fetch(
    RequestOptions options,
    Stream<Uint8List>? requestStream,
    Future<void>? cancelFuture,
  ) async {
    captured.add(options);
    return ResponseBody.fromString(
      _body,
      200,
      headers: {
        Headers.contentTypeHeader: [Headers.jsonContentType],
      },
    );
  }

  @override
  void close({bool force = false}) {}
}

final class _TradeStub {
  _TradeStub(this.repository, this.adapter);

  final CartRepository repository;
  final _StubAdapter adapter;
}

_TradeStub _cartStubWith(Object? data) {
  final adapter = _StubAdapter(
    jsonEncode({'code': 0, 'message': '', 'data': data, 'timestamp': 0}),
  );
  final dio = Dio()..httpClientAdapter = adapter;
  return _TradeStub(
    CartRepository(
      ApiClient(
        baseUrl: 'http://test.local',
        cookieDir: '/tmp/jadecart-trade-test',
        dio: dio,
      ),
    ),
    adapter,
  );
}

final class _CheckoutStub {
  _CheckoutStub(this.repository, this.adapter);

  final CheckoutRepository repository;
  final _StubAdapter adapter;
}

_CheckoutStub _checkoutStubWith(Object? data) {
  final adapter = _StubAdapter(
    jsonEncode({'code': 0, 'message': '', 'data': data, 'timestamp': 0}),
  );
  final dio = Dio()..httpClientAdapter = adapter;
  return _CheckoutStub(
    CheckoutRepository(
      ApiClient(
        baseUrl: 'http://test.local',
        cookieDir: '/tmp/jadecart-trade-test',
        dio: dio,
      ),
    ),
    adapter,
  );
}

const _cartJson = {
  'cartPublicId': 'c1',
  'currency': 'USD',
  'subtotalCents': 3800,
  'items': [
    {
      'variantId': 11,
      'productTitle': 'Jade Tee',
      'productSlug': 'jade-tee',
      'sku': 'TEE-RED',
      'spec': 'Color: Red',
      'unitPriceCents': 1900,
      'quantity': 2,
      'available': 5,
      'lineTotalCents': 3800,
      'mainImagePath': 'img/tee.jpg',
    },
  ],
};

void main() {
  group('购物车', () {
    test('应该解析购物车行与小计 当获取购物车', () async {
      final stub = _cartStubWith(_cartJson);

      final cart = await stub.repository.cart();

      expect(cart.items, hasLength(1));
      expect(cart.items.first.quantity, 2);
      expect(cart.items.first.spec, 'Color: Red');
      expect(cart.subtotalCents, 3800);
      expect(cart.currency, 'USD');
      expect(cart.items.first.mainImagePath, 'img/tee.jpg');
    });

    test('应该把数量 0 当作移除 当更新数量', () async {
      final stub = _cartStubWith(_cartJson);

      await stub.repository.updateItem(11, 0);

      final request = stub.adapter.captured.single;
      expect(request.method, 'PUT');
      expect(request.path, '/api/store/cart/items/11');
      expect(request.data, {'quantity': 0});
    });

    test('应该带变体与数量 当加入购物车', () async {
      final stub = _cartStubWith(_cartJson);

      await stub.repository.addItem(11, quantity: 3);

      final request = stub.adapter.captured.single;
      expect(request.method, 'POST');
      expect(request.path, '/api/store/cart/items');
      expect(request.data, {'variantId': 11, 'quantity': 3});
    });

    test('应该返回行数 当读取角标数量', () async {
      final stub = _cartStubWith(3);

      expect(await stub.repository.count(), 3);
    });
  });

  group('结算', () {
    test('应该解析结算结果并识别 mock 支付 当提交结算', () async {
      final stub = _checkoutStubWith({
        'orderPublicId': 'o1',
        'orderNumber': 'JC20260101000000',
        'totalCents': 4200,
        'currency': 'USD',
        'payUrl': '/pay/mock/o1',
      });

      final result = await stub.repository.checkout(
        email: 'a@b.com',
        shipName: 'Ann',
        country: 'US',
      );

      expect(result.orderPublicId, 'o1');
      expect(result.totalCents, 4200);
      expect(result.isMockPay, isTrue);
    });

    test('应该识别外部支付 当 payUrl 是 http 地址', () async {
      final stub = _checkoutStubWith({
        'orderPublicId': 'o2',
        'orderNumber': 'JC20260101000001',
        'totalCents': 4200,
        'currency': 'USD',
        'payUrl': 'https://checkout.stripe.com/pay/abc',
      });

      final result = await stub.repository.checkout(
        email: 'a@b.com',
        country: 'US',
      );

      expect(result.isMockPay, isFalse);
    });

    test('应该解析订单状态 当查询订单', () async {
      final stub = _checkoutStubWith({
        'publicId': 'o1',
        'orderNumber': 'JC20260101000000',
        'email': 'a@b.com',
        'shipName': 'Ann',
        'status': 'PAID',
        'subtotalCents': 3800,
        'shippingCents': 400,
        'totalCents': 4200,
        'currency': 'USD',
        'shippingCountry': 'US',
        'paymentProvider': 'mock',
        'items': [
          {
            'productTitle': 'Jade Tee',
            'variantSpec': 'Color: Red',
            'sku': 'TEE-RED',
            'unitPriceCents': 1900,
            'quantity': 2,
            'lineTotalCents': 3800,
          },
        ],
        'shipment': {
          'carrier': 'DHL',
          'trackingNumber': 'X1',
          'trackingUrl': 'https://x/1',
        },
      });

      final order = await stub.repository.order('o1');

      expect(order.status.name, 'paid');
      expect(order.items, hasLength(1));
      expect(order.trackingUrl, 'https://x/1');
      expect(order.totalCents, 4200);
    });

    test('应该把未识别状态归为 unknown 当订单状态异常', () async {
      final stub = _checkoutStubWith({
        'publicId': 'o1',
        'status': 'WEIRD_STATUS',
        'items': <Object?>[],
      });

      final order = await stub.repository.order('o1');

      expect(order.status.name, 'unknown');
    });
  });
}
