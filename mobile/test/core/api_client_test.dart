import 'dart:convert';
import 'dart:typed_data';

import 'package:dio/dio.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:jade_cart_mobile/core/api_client.dart';

/// 捕获请求并回放固定响应的 Dio 适配器 (ReqPilotMobile 同款测试手法)
final class _CapturingAdapter implements HttpClientAdapter {
  _CapturingAdapter(this._body, {this.statusCode = 200});

  final String _body;
  final int statusCode;
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
      statusCode,
      headers: {
        Headers.contentTypeHeader: [Headers.jsonContentType],
      },
    );
  }

  @override
  void close({bool force = false}) {}
}

ApiClient _clientWith(String body, {int statusCode = 200}) {
  final dio = Dio()
    ..httpClientAdapter = _CapturingAdapter(body, statusCode: statusCode);
  return ApiClient(
    baseUrl: 'http://test.local',
    cookieDir: '/tmp/jadecart-adapter-test',
    dio: dio,
  );
}

String _envelope(Object? data, {int code = 0, String message = ''}) {
  return jsonEncode({
    'code': code,
    'message': message,
    'data': data,
    'timestamp': 0,
  });
}

void main() {
  test('应该返回 data 字段 当信封 code 为 0', () async {
    final client = _clientWith(_envelope({'title': 'Jade Tee'}));

    final data = await client.getData('/api/store/products/jade-tee');

    expect(data, isA<Map<String, Object?>>());
    expect((data! as Map<String, Object?>)['title'], 'Jade Tee');
  });

  test('应该抛出 ApiFailure 当信封 code 非 0', () async {
    final client = _clientWith(_envelope(null, code: 404, message: '商品不存在'));

    await expectLater(
      client.getData('/api/store/products/missing'),
      throwsA(
        isA<ApiFailure>()
            .having((failure) => failure.code, 'code', 404)
            .having((failure) => failure.message, 'message', '商品不存在'),
      ),
    );
  });

  test('应该抛出 ApiFailure 当响应体不是信封', () async {
    final client = _clientWith('not-json-envelope');

    await expectLater(
      client.getData('/api/store/products'),
      throwsA(isA<ApiFailure>()),
    );
  });

  test('应该抛出连接错误文案 当服务器不可达', () async {
    final dio = Dio()..httpClientAdapter = _ThrowingAdapter();
    final client = ApiClient(
      baseUrl: 'http://test.local',
      cookieDir: '/tmp/jadecart-adapter-test',
      dio: dio,
    );

    await expectLater(
      client.getData('/api/store/products'),
      throwsA(
        isA<ApiFailure>().having(
          (failure) => failure.message,
          'message',
          contains('Cannot reach the store'),
        ),
      ),
    );
  });

  test('应该把查询参数拼到请求上 当调用带 query 的方法', () async {
    final dio = Dio()
      ..httpClientAdapter = _CapturingAdapter(_envelope(<Object?>[]));
    final client = ApiClient(
      baseUrl: 'http://test.local',
      cookieDir: '/tmp/jadecart-adapter-test',
      dio: dio,
    );

    await client.getData(
      '/api/store/products',
      query: {'q': 'tee', 'limit': 20},
    );

    final options =
        (dio.httpClientAdapter as _CapturingAdapter).captured.single;
    expect(options.uri.path, '/api/store/products');
    expect(options.uri.queryParameters['q'], 'tee');
    expect(options.uri.queryParameters['limit'], '20');
  });
}

/// 模拟网络不可达
final class _ThrowingAdapter implements HttpClientAdapter {
  @override
  Future<ResponseBody> fetch(
    RequestOptions options,
    Stream<Uint8List>? requestStream,
    Future<void>? cancelFuture,
  ) {
    throw DioException.connectionError(
      requestOptions: options,
      reason: 'connection refused',
    );
  }

  @override
  void close({bool force = false}) {}
}
