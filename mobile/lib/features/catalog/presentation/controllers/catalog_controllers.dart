import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'package:jade_cart_mobile/core/catalog_models.dart';
import 'package:jade_cart_mobile/core/repositories.dart';
import 'package:jade_cart_mobile/app_providers.dart';

/// 商品列表页状态
final class ProductListState {
  const ProductListState({
    this.items = const [],
    this.categories = const [],
    this.selectedCategorySlug,
    this.keyword = '',
    this.loading = false,
    this.error = '',
  });

  final List<ProductSummary> items;
  final List<CategoryNode> categories;
  final String? selectedCategorySlug;
  final String keyword;
  final bool loading;
  final String error;

  ProductListState copyWith({
    List<ProductSummary>? items,
    List<CategoryNode>? categories,
    String? selectedCategorySlug,
    bool clearCategory = false,
    String? keyword,
    bool? loading,
    String? error,
  }) {
    return ProductListState(
      items: items ?? this.items,
      categories: categories ?? this.categories,
      selectedCategorySlug: clearCategory
          ? null
          : (selectedCategorySlug ?? this.selectedCategorySlug),
      keyword: keyword ?? this.keyword,
      loading: loading ?? this.loading,
      error: error ?? this.error,
    );
  }
}

/// 商品列表控制器: 负责搜索/分类筛选与加载 (单一真源: 商品列表页的筛选状态)
class ProductListController extends StateNotifier<ProductListState> {
  ProductListController(this._repository) : super(const ProductListState());

  final CatalogRepository _repository;

  /// 首次加载: 并行取分类树与商品列表
  Future<void> load() async {
    state = state.copyWith(loading: true, error: '');
    try {
      final categories = await _repository.categories();
      final products = await _repository.products(
        keyword: state.keyword,
        categorySlug: state.selectedCategorySlug,
      );
      if (!mounted) return;
      state = state.copyWith(
        items: products,
        categories: categories,
        loading: false,
      );
    } on Exception catch (error) {
      if (!mounted) return;
      state = state.copyWith(loading: false, error: '$error');
    }
  }

  Future<void> selectCategory(String? slug) async {
    state = state.copyWith(
      selectedCategorySlug: slug,
      clearCategory: slug == null,
      loading: true,
      error: '',
    );
    try {
      final products = await _repository.products(
        keyword: state.keyword,
        categorySlug: slug,
      );
      if (!mounted) return;
      state = state.copyWith(items: products, loading: false);
    } on Exception catch (error) {
      if (!mounted) return;
      state = state.copyWith(loading: false, error: '$error');
    }
  }

  Future<void> search(String keyword) async {
    state = state.copyWith(keyword: keyword, loading: true, error: '');
    try {
      final products = await _repository.products(
        keyword: keyword,
        categorySlug: state.selectedCategorySlug,
      );
      if (!mounted) return;
      state = state.copyWith(items: products, loading: false);
    } on Exception catch (error) {
      if (!mounted) return;
      state = state.copyWith(loading: false, error: '$error');
    }
  }
}

final productListControllerProvider =
    StateNotifierProvider<ProductListController, ProductListState>((ref) {
      return ProductListController(ref.watch(catalogRepositoryProvider));
    });

/// 首页商品 (优先精选; 商家没设精选时回落最新, 首页不留白)
final featuredProductsProvider = FutureProvider<List<ProductSummary>>((
  ref,
) async {
  final repository = ref.watch(catalogRepositoryProvider);
  final featured = await repository.products(featured: true, limit: 10);
  if (featured.isNotEmpty) return featured;
  return repository.products(limit: 10);
});

/// 分类树 (首页分类入口; 与商品列表筛选共用仓储)
final categoriesProvider = FutureProvider<List<CategoryNode>>((ref) {
  return ref.watch(catalogRepositoryProvider).categories();
});
