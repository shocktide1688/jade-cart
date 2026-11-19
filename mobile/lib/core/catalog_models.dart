import 'models.dart';

/// 店面分类树节点 (对应后端 CategoryNode)
final class CategoryNode {
  const CategoryNode({
    required this.slug,
    required this.name,
    required this.children,
  });

  final String slug;
  final String name;
  final List<CategoryNode> children;

  factory CategoryNode.fromJson(JsonMap json) {
    return CategoryNode(
      slug: stringOf(json, 'slug'),
      name: stringOf(json, 'name'),
      children: asJsonMapList(
        json['children'],
      ).map(CategoryNode.fromJson).toList(growable: false),
    );
  }
}

/// 商品列表摘要 (对应后端 ProductView.Summary)
final class ProductSummary {
  const ProductSummary({
    required this.publicId,
    required this.title,
    required this.slug,
    required this.featured,
    required this.mainImagePath,
    required this.minPriceCents,
    required this.variantCount,
    required this.totalAvailable,
  });

  final String publicId;
  final String title;
  final String slug;
  final bool featured;
  final String mainImagePath;
  final int minPriceCents;
  final int variantCount;
  final int totalAvailable;

  bool get isSoldOut => totalAvailable <= 0;

  factory ProductSummary.fromJson(JsonMap json) {
    return ProductSummary(
      publicId: stringOf(json, 'publicId'),
      title: stringOf(json, 'title'),
      slug: stringOf(json, 'slug'),
      featured: boolOf(json, 'featured'),
      mainImagePath: stringOf(json, 'mainImagePath'),
      minPriceCents: intOf(json, 'minPriceCents'),
      variantCount: intOf(json, 'variantCount'),
      totalAvailable: intOf(json, 'totalAvailable'),
    );
  }
}

/// 变体轴 (对应后端 ProductView.OptionView)
final class ProductOptionAxis {
  const ProductOptionAxis({
    required this.optionKey,
    required this.displayName,
    required this.values,
  });

  final String optionKey;
  final String displayName;
  final List<String> values;

  factory ProductOptionAxis.fromJson(JsonMap json) {
    return ProductOptionAxis(
      optionKey: stringOf(json, 'optionKey'),
      displayName: stringOf(json, 'displayName'),
      values: (json['values'] as List? ?? const <Object?>[])
          .map((value) => value.toString())
          .toList(growable: false),
    );
  }
}

/// 变体视图 (对应后端 ProductView.VariantView)
final class ProductVariant {
  const ProductVariant({
    required this.id,
    required this.sku,
    required this.priceCents,
    required this.spec,
    required this.available,
  });

  final int id;
  final String sku;
  final int priceCents;
  final String spec;
  final int available;

  bool get inStock => available > 0;

  factory ProductVariant.fromJson(JsonMap json) {
    return ProductVariant(
      id: intOf(json, 'id'),
      sku: stringOf(json, 'sku'),
      priceCents: intOf(json, 'priceCents'),
      spec: stringOf(json, 'spec'),
      available: intOf(json, 'available'),
    );
  }
}

/// 商品详情 (对应后端 StoreProductDetailView = ProductView + 图集 + 分类 slug)
final class ProductDetail {
  const ProductDetail({
    required this.publicId,
    required this.title,
    required this.slug,
    required this.description,
    required this.seoDescription,
    required this.mainImagePath,
    required this.options,
    required this.variants,
    required this.imagePaths,
    required this.categorySlugs,
  });

  final String publicId;
  final String title;
  final String slug;

  /// 富文本 HTML (后端 HtmlSanitizer 消毒过), PDP 用 flutter_html 渲染
  final String description;
  final String seoDescription;
  final String mainImagePath;
  final List<ProductOptionAxis> options;
  final List<ProductVariant> variants;
  final List<String> imagePaths;
  final List<String> categorySlugs;

  /// 最低价变体价 (无变体时 0)
  int get minPriceCents => variants.isEmpty
      ? 0
      : variants.map((v) => v.priceCents).reduce((a, b) => a < b ? a : b);

  int get totalAvailable => variants.fold(0, (sum, v) => sum + v.available);

  factory ProductDetail.fromJson(JsonMap json) {
    final product = asJsonMap(json['product']);
    return ProductDetail(
      publicId: stringOf(product, 'publicId'),
      title: stringOf(product, 'title'),
      slug: stringOf(product, 'slug'),
      description: stringOf(product, 'description'),
      seoDescription: stringOf(product, 'seoDescription'),
      mainImagePath: stringOf(product, 'mainImagePath'),
      options: asJsonMapList(
        product['options'],
      ).map(ProductOptionAxis.fromJson).toList(growable: false),
      variants: asJsonMapList(
        product['variants'],
      ).map(ProductVariant.fromJson).toList(growable: false),
      imagePaths: (json['imagePaths'] as List? ?? const <Object?>[])
          .map((path) => path.toString())
          .toList(growable: false),
      categorySlugs: (json['categorySlugs'] as List? ?? const <Object?>[])
          .map((slug) => slug.toString())
          .toList(growable: false),
    );
  }
}
