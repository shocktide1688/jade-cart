import 'api_client.dart';
import 'catalog_models.dart';
import 'models.dart';
import 'trade_models.dart';

/// 店面商品目录 (GET /api/store/products|categories, 只读)
class CatalogRepository {
  const CatalogRepository(this._api);

  final ApiClient _api;

  Future<List<CategoryNode>> categories() async {
    final data = await _api.getData('/api/store/categories');
    return asJsonMapList(
      data,
    ).map(CategoryNode.fromJson).toList(growable: false);
  }

  /// 商品列表: 关键词 (标题) / 分类 slug / 精选 过滤
  Future<List<ProductSummary>> products({
    String? keyword,
    String? categorySlug,
    bool featured = false,
    int limit = 60,
  }) async {
    final data = await _api.getData(
      '/api/store/products',
      query: <String, Object?>{
        if (keyword != null && keyword.isNotEmpty) 'q': keyword,
        if (categorySlug != null && categorySlug.isNotEmpty)
          'category': categorySlug,
        if (featured) 'featured': 'true',
        'limit': limit,
      },
    );
    return asJsonMapList(
      data,
    ).map(ProductSummary.fromJson).toList(growable: false);
  }

  Future<ProductDetail> product(String slug) async {
    final data = await _api.getData('/api/store/products/$slug');
    return ProductDetail.fromJson(asJsonMap(data));
  }
}

/// 购物车 (cookie 会话由 ApiClient 的 PersistCookieJar 托管)
class CartRepository {
  const CartRepository(this._api);

  final ApiClient _api;

  Future<CartView> cart() async {
    final data = await _api.getData('/api/store/cart');
    return CartView.fromJson(asJsonMap(data));
  }

  /// 页签角标 (后端不建会话, count = 行数)
  Future<int> count() async {
    final data = await _api.getData('/api/store/cart/count');
    if (data is num) return data.toInt();
    if (data == null) return 0;
    throw const FormatException('Unexpected server response format');
  }

  Future<CartView> addItem(int variantId, {int quantity = 1}) async {
    final data = await _api.postData(
      '/api/store/cart/items',
      body: {'variantId': variantId, 'quantity': quantity},
    );
    return CartView.fromJson(asJsonMap(data));
  }

  /// quantity 0 = 移除该行 (后端语义)
  Future<CartView> updateItem(int variantId, int quantity) async {
    final data = await _api.putData(
      '/api/store/cart/items/$variantId',
      body: {'quantity': quantity},
    );
    return CartView.fromJson(asJsonMap(data));
  }

  Future<CartView> removeItem(int variantId) async {
    final data = await _api.deleteData('/api/store/cart/items/$variantId');
    return CartView.fromJson(asJsonMap(data));
  }
}

/// 结算 + 订单查询 (GET /api/store/orders/{publicId})
class CheckoutRepository {
  const CheckoutRepository(this._api);

  final ApiClient _api;

  Future<CheckoutResult> checkout({
    required String email,
    String? shipName,
    required String country,
  }) async {
    final data = await _api.postData(
      '/api/store/checkout',
      body: {
        'email': email,
        if (shipName != null && shipName.isNotEmpty) 'shipName': shipName,
        'country': country,
      },
    );
    return CheckoutResult.fromJson(asJsonMap(data));
  }

  Future<OrderView> order(String publicId) async {
    final data = await _api.getData('/api/store/orders/$publicId');
    return OrderView.fromJson(asJsonMap(data));
  }

  /// mock 网关 (开发默认): App 内直接确认支付, 走后端幂等 markPaid 路径
  Future<bool> confirmMockPay(String publicId) async {
    final data = await _api.postData('/api/store/mock-pay/$publicId/confirm');
    return data is bool ? data : boolOf(asJsonMap(data), 'value', true);
  }
}
