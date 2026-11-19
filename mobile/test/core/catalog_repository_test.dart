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

/// 仓储 + 适配器 组合 (记录请求用)
final class _CatalogStub {
  _CatalogStub(this.repository, this.adapter);

  final CatalogRepository repository;
  final _StubAdapter adapter;
}

_CatalogStub _repositoryWith(Object? data) {
  final adapter = _StubAdapter(
    jsonEncode({'code': 0, 'message': '', 'data': data, 'timestamp': 0}),
  );
  final dio = Dio()..httpClientAdapter = adapter;
  return _CatalogStub(
    CatalogRepository(
      ApiClient(
        baseUrl: 'http://test.local',
        cookieDir: '/tmp/jadecart-catalog-test',
        dio: dio,
      ),
    ),
    adapter,
  );
}

void main() {
  test('应该解析商品摘要列表 当获取商品列表', () async {
    final stub = _repositoryWith(<Object?>[
      {
        'publicId': 'p1',
        'title': 'Jade Tee',
        'slug': 'jade-tee',
        'featured': true,
        'mainImagePath': 'img/tee.jpg',
        'minPriceCents': 1900,
        'variantCount': 2,
        'totalAvailable': 7,
      },
    ]);

    final products = await stub.repository.products();

    expect(products, hasLength(1));
    expect(products.first.title, 'Jade Tee');
    expect(products.first.minPriceCents, 1900);
    expect(products.first.featured, isTrue);
    expect(products.first.isSoldOut, isFalse);
  });

  test('应该带上关键词与分类过滤 当查询商品列表', () async {
    final stub = _repositoryWith(<Object?>[]);

    await stub.repository.products(keyword: 'tee', categorySlug: 'apparel');

    final query = stub.adapter.captured.single.uri.queryParameters;
    expect(query['q'], 'tee');
    expect(query['category'], 'apparel');
  });

  test('应该解析商品详情含变体与图集 当获取单个商品', () async {
    final stub = _repositoryWith({
      'product': {
        'publicId': 'p1',
        'title': 'Jade Tee',
        'slug': 'jade-tee',
        'description': '<p>soft</p>',
        'seoDescription': 'seo',
        'mainImagePath': 'img/main.jpg',
        'options': [
          {
            'optionKey': 'color',
            'displayName': 'Color',
            'values': ['Red', 'Blue'],
          },
        ],
        'variants': [
          {
            'id': 11,
            'sku': 'TEE-RED',
            'priceCents': 1900,
            'spec': 'Color: Red',
            'available': 3,
          },
          {
            'id': 12,
            'sku': 'TEE-BLUE',
            'priceCents': 2100,
            'spec': 'Color: Blue',
            'available': 0,
          },
        ],
      },
      'imagePaths': ['img/main.jpg', 'img/extra.jpg'],
      'categorySlugs': ['apparel'],
    });

    final detail = await stub.repository.product('jade-tee');

    expect(detail.title, 'Jade Tee');
    expect(detail.variants, hasLength(2));
    expect(detail.options.single.values, ['Red', 'Blue']);
    expect(detail.imagePaths, hasLength(2));
    expect(detail.categorySlugs, ['apparel']);
    expect(detail.minPriceCents, 1900);
    expect(detail.totalAvailable, 3);
    expect(detail.variants.last.inStock, isFalse);
  });

  test('应该解析分类树 当获取分类', () async {
    final stub = _repositoryWith(<Object?>[
      {
        'slug': 'apparel',
        'name': 'Apparel',
        'children': [
          {'slug': 'tees', 'name': 'Tees', 'children': <Object?>[]},
        ],
      },
    ]);

    final categories = await stub.repository.categories();

    expect(categories, hasLength(1));
    expect(categories.first.name, 'Apparel');
    expect(categories.first.children.single.slug, 'tees');
  });
}
