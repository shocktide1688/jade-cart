import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'package:jade_cart_mobile/app_providers.dart';
import 'package:jade_cart_mobile/core/catalog_models.dart';

/// 商品详情 provider: 按 slug 取详情
final productDetailProvider = FutureProvider.family<ProductDetail, String>((
  ref,
  slug,
) {
  return ref.watch(catalogRepositoryProvider).product(slug);
});

/// 变体选择状态 (单一真源: PDP 的已选变体组合)
final class VariantSelection {
  const VariantSelection({this.variantId, this.spec = ''});

  /// null = 未选择 (多变体商品必须先选)
  final int? variantId;
  final String spec;

  bool get hasSelection => variantId != null;

  VariantSelection copyWith({int? variantId, String? spec}) {
    return VariantSelection(
      variantId: variantId ?? this.variantId,
      spec: spec ?? this.spec,
    );
  }
}

/// PDP 变体选择控制器: 记录用户已选的变体 id 与规格文本
class VariantSelectionController extends StateNotifier<VariantSelection> {
  VariantSelectionController() : super(const VariantSelection());

  void select(ProductVariant variant) {
    state = VariantSelection(variantId: variant.id, spec: variant.spec);
  }

  void clear() {
    state = const VariantSelection();
  }
}

final variantSelectionProvider =
    StateNotifierProvider<VariantSelectionController, VariantSelection>((ref) {
      return VariantSelectionController();
    });
