package com.jade.cart.catalog.service;

import com.jade.cart.catalog.dto.ProductView;
import com.jade.cart.catalog.entity.Category;
import com.jade.cart.catalog.entity.Inventory;
import com.jade.cart.catalog.entity.Product;
import com.jade.cart.catalog.entity.ProductCategoryLink;
import com.jade.cart.catalog.entity.ProductOption;
import com.jade.cart.catalog.entity.ProductOptionValue;
import com.jade.cart.catalog.entity.Variant;
import com.jade.cart.catalog.entity.VariantOptionValue;
import com.jade.common.exception.BizException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/** 商品目录: 商品 CRUD / 变体轴 / 变体矩阵生成 / 库存设置 变体组合唯一性用规范化签名 (排序后 optionKey=value 对) 保证 */
@ApplicationScoped
public class CatalogService {

    // ---------- 分类 ----------

    public List<Category> categories() {
        return Category.listAllLive();
    }

    @Transactional
    public Category createCategory(String name, String slug, Long parentId, Integer sortOrder) {
        Category category = new Category();
        category.name = name;
        category.slug = uniqueSlug(slug, name, s -> Category.findBySlug(s) != null);
        category.parentId = parentId;
        category.sortOrder = sortOrder == null ? 0 : sortOrder;
        category.persist();
        return category;
    }

    @Transactional
    public Category updateCategory(
            Long id, String name, String slug, Long parentId, Integer sortOrder, Integer status) {
        Category category = Category.findById(id);
        if (category == null || category.deleted) {
            throw new BizException("分类不存在");
        }
        if (name != null && !name.isBlank()) {
            category.name = name;
        }
        if (slug != null && !slug.isBlank() && !slug.equals(category.slug)) {
            category.slug = uniqueSlug(slug, name, s -> Category.findBySlug(s) != null);
        }
        if (parentId != null) {
            if (parentId.equals(id)) {
                throw new BizException("父分类不能是自身");
            }
            category.parentId = parentId;
        }
        if (sortOrder != null) {
            category.sortOrder = sortOrder;
        }
        if (status != null) {
            category.status = status;
        }
        category.persist();
        return category;
    }

    @Transactional
    public void deleteCategory(Long id) {
        Category category = Category.findById(id);
        if (category == null || category.deleted) {
            throw new BizException("分类不存在");
        }
        category.deleted = true;
        category.persist();
    }

    // ---------- 商品 ----------

    /** 商品分页 (keyword 匹配标题/SKU, status 过滤) */
    public List<Product> pageProducts(String keyword, String status, int page, int size) {
        StringBuilder where = new StringBuilder("deleted = false");
        Map<String, Object> params = new LinkedHashMap<>();
        if (keyword != null && !keyword.isBlank()) {
            where.append(" and lower(title) like :kw");
            params.put("kw", "%" + keyword.toLowerCase(Locale.ROOT) + "%");
        }
        if (status != null && !status.isBlank()) {
            where.append(" and status = :st");
            params.put("st", status);
        }
        return Product.find(where.toString() + " order by createdAt desc", params)
                .page(Math.max(page - 1, 0), size)
                .list();
    }

    public long countProducts(String keyword, String status) {
        StringBuilder where = new StringBuilder("deleted = false");
        Map<String, Object> params = new LinkedHashMap<>();
        if (keyword != null && !keyword.isBlank()) {
            where.append(" and lower(title) like :kw");
            params.put("kw", "%" + keyword.toLowerCase(Locale.ROOT) + "%");
        }
        if (status != null && !status.isBlank()) {
            where.append(" and status = :st");
            params.put("st", status);
        }
        return Product.count(where.toString(), params);
    }

    @Transactional
    public Product createProduct(
            String title,
            String slug,
            String description,
            String seoDescription,
            String status,
            Boolean featured,
            Long defaultPriceCents,
            String defaultSku,
            String mainImagePath,
            String seoKeywords) {
        Product product = new Product();
        product.title = title;
        product.slug = uniqueSlug(slug, title, s -> Product.findBySlug(s) != null);
        product.description = description;
        product.seoDescription = seoDescription;
        product.status = validStatus(status, Product.STATUS_DRAFT);
        product.featured = Boolean.TRUE.equals(featured);
        product.mainImagePath =
                (mainImagePath == null || mainImagePath.isBlank()) ? null : mainImagePath;
        product.seoKeywords = seoKeywords;
        product.description = HtmlSanitizer.sanitize(product.description);
        product.persist();

        // 无变体商品自动带一条默认变体 (kartwo 语义: 商品必有 >=1 变体)
        createVariant(
                product.id,
                defaultSku,
                defaultPriceCents == null ? 0L : defaultPriceCents,
                List.of());
        return product;
    }

    @Transactional
    public Product updateProduct(
            Long id,
            String title,
            String slug,
            String description,
            String seoDescription,
            String status,
            Boolean featured,
            String mainImagePath,
            String seoKeywords) {
        Product product = Product.findById(id);
        if (product == null || product.deleted) {
            throw new BizException("商品不存在");
        }
        if (title != null && !title.isBlank()) {
            product.title = title;
        }
        if (slug != null && !slug.isBlank() && !slug.equals(product.slug)) {
            product.slug = uniqueSlug(slug, title, s -> Product.findBySlug(s) != null);
        }
        if (description != null) {
            product.description = HtmlSanitizer.sanitize(description);
        }
        if (seoDescription != null) {
            product.seoDescription = seoDescription;
        }
        if (status != null) {
            product.status = validStatus(status, product.status);
        }
        if (featured != null) {
            product.featured = featured;
        }
        if (mainImagePath != null) {
            product.mainImagePath = mainImagePath.isBlank() ? null : mainImagePath;
        }
        if (seoKeywords != null) {
            product.seoKeywords = seoKeywords.isBlank() ? null : seoKeywords;
        }
        product.persist();
        return product;
    }

    /** 归档 (软删除): 店面不再展示, 历史订单快照不受影响 */
    @Transactional
    public void archiveProduct(Long id) {
        Product product = Product.findById(id);
        if (product == null || product.deleted) {
            throw new BizException("商品不存在");
        }
        product.status = Product.STATUS_ARCHIVED;
        product.deleted = true;
        product.persist();
    }

    // ---------- 变体轴 ----------

    @Transactional
    public ProductOption addOption(
            Long productId, String optionKey, String displayName, List<String> values) {
        Product product = Product.findById(productId);
        if (product == null || product.deleted) {
            throw new BizException("商品不存在");
        }
        ProductOption option = new ProductOption();
        option.productId = productId;
        option.optionKey = normalizeKey(optionKey);
        option.displayName =
                displayName == null || displayName.isBlank() ? option.optionKey : displayName;
        option.sortOrder = ProductOption.listByProduct(productId).size();
        option.persist();
        if (values != null) {
            for (String value : values) {
                if (value == null || value.isBlank()) {
                    continue;
                }
                ProductOptionValue v = new ProductOptionValue();
                v.optionId = option.id;
                v.value = value.trim();
                v.sortOrder = ProductOptionValue.listByOption(option.id).size();
                v.persist();
            }
        }
        return option;
    }

    @Transactional
    public void deleteOption(Long optionId) {
        ProductOption option = ProductOption.findById(optionId);
        if (option == null) {
            throw new BizException("变体轴不存在");
        }
        option.delete();
        ProductOptionValue.<ProductOptionValue>find("optionId", optionId)
                .list()
                .forEach(ProductOptionValue::delete);
    }

    @Transactional
    public void addOptionValue(Long optionId, String value) {
        ProductOption option = ProductOption.findById(optionId);
        if (option == null) {
            throw new BizException("变体轴不存在");
        }
        ProductOptionValue v = new ProductOptionValue();
        v.optionId = optionId;
        v.value = value.trim();
        v.sortOrder = ProductOptionValue.listByOption(optionId).size();
        v.persist();
    }

    // ---------- 变体 ----------

    /** 变体矩阵生成: 对各轴取值做笛卡尔积, 已存在的组合跳过, 新组合建变体 + 库存行 */
    @Transactional
    public List<Variant> generateVariants(Long productId, Long defaultPriceCents) {
        Product product = Product.findById(productId);
        if (product == null || product.deleted) {
            throw new BizException("商品不存在");
        }
        List<ProductOption> options = ProductOption.listByProduct(productId);
        if (options.isEmpty()) {
            throw new BizException("请先添加变体轴");
        }

        // 现有变体签名 → 跳过重复组合
        Map<String, Variant> existing = signatureMap(productId);

        // 笛卡尔积 (回溯)
        List<Map<ProductOption, String>> combos = new ArrayList<>();
        Deque<Map.Entry<ProductOption, String>> current = new LinkedList<>();
        cartesian(options, 0, current, combos);

        List<Variant> created = new ArrayList<>();
        for (Map<ProductOption, String> combo : combos) {
            String signature = signatureOf(combo);
            if (existing.containsKey(signature)) {
                continue;
            }
            List<VariantOptionValue> links = new ArrayList<>();
            for (Map.Entry<ProductOption, String> e : combo.entrySet()) {
                VariantOptionValue link = new VariantOptionValue();
                link.optionId = e.getKey().id;
                link.value = e.getValue();
                links.add(link);
            }
            created.add(
                    createVariant(
                            productId,
                            null,
                            defaultPriceCents == null ? 0L : defaultPriceCents,
                            links));
        }
        return created;
    }

    @Transactional
    public Variant addVariant(Long productId, String sku, Long priceCents) {
        Product product = Product.findById(productId);
        if (product == null || product.deleted) {
            throw new BizException("商品不存在");
        }
        return createVariant(productId, sku, priceCents == null ? 0L : priceCents, List.of());
    }

    @Transactional
    public Variant updateVariant(Long variantId, String sku, Long priceCents) {
        Variant variant = Variant.findById(variantId);
        if (variant == null || variant.deleted) {
            throw new BizException("变体不存在");
        }
        if (sku != null && !sku.isBlank() && !sku.equals(variant.sku)) {
            if (Variant.findBySku(sku).isPresent()) {
                throw new BizException("SKU 已存在: " + sku);
            }
            variant.sku = sku;
        }
        if (priceCents != null) {
            if (priceCents < 0) {
                throw new BizException("价格不能为负");
            }
            variant.priceCents = priceCents;
        }
        variant.persist();
        return variant;
    }

    @Transactional
    public void deleteVariant(Long variantId) {
        Variant variant = Variant.findById(variantId);
        if (variant == null || variant.deleted) {
            throw new BizException("变体不存在");
        }
        variant.deleted = true;
        variant.persist();
    }

    @Transactional
    public Variant createVariant(
            Long productId, String sku, Long priceCents, List<VariantOptionValue> links) {
        Variant variant = new Variant();
        variant.productId = productId;
        variant.sku =
                sku == null || sku.isBlank()
                        ? "SKU-"
                                + UUID.randomUUID()
                                        .toString()
                                        .substring(0, 8)
                                        .toUpperCase(Locale.ROOT)
                        : sku;
        if (Variant.findBySku(variant.sku).isPresent()) {
            throw new BizException("SKU 已存在: " + variant.sku);
        }
        variant.priceCents = priceCents;
        variant.persist();

        for (VariantOptionValue link : links) {
            link.variantId = variant.id;
            link.persist();
        }

        Inventory inventory = new Inventory();
        inventory.variantId = variant.id;
        inventory.quantity = 0;
        inventory.reserved = 0;
        inventory.persist();
        return variant;
    }

    // ---------- 库存 ----------

    @Transactional
    public Inventory setInventory(Long variantId, int quantity) {
        Variant variant = Variant.findById(variantId);
        if (variant == null || variant.deleted) {
            throw new BizException("变体不存在");
        }
        if (quantity < 0) {
            throw new BizException("库存不能为负");
        }
        if (Inventory.byVariant(variantId).isEmpty()) {
            Inventory inventory = new Inventory();
            inventory.variantId = variantId;
            inventory.quantity = quantity;
            inventory.persist();
            return inventory;
        }
        Inventory.setQuantity(variantId, quantity);
        return Inventory.byVariant(variantId).orElseThrow();
    }

    // ---------- 商品分类关联 ----------

    /** 覆盖式设置商品所属分类 */
    @Transactional
    public void linkCategories(Long productId, List<Long> categoryIds) {
        Product product = Product.findById(productId);
        if (product == null || product.deleted) {
            throw new BizException("商品不存在");
        }
        ProductCategoryLink.unlinkProduct(productId);
        if (categoryIds != null) {
            for (Long categoryId : new HashSet<>(categoryIds)) {
                Category category = Category.findById(categoryId);
                if (category == null || category.deleted) {
                    continue;
                }
                ProductCategoryLink link = new ProductCategoryLink();
                link.productId = productId;
                link.categoryId = categoryId;
                link.persist();
            }
        }
    }

    public List<Long> categoryIdsOf(Long productId) {
        return ProductCategoryLink.listByProduct(productId).stream()
                .map(link -> link.categoryId)
                .toList();
    }

    /** 店面商品列表: ACTIVE + 可选关键词/分类过滤 */
    public List<Product> activeProducts(String keyword, Long categoryId, int limit) {
        StringBuilder where = new StringBuilder("status = 'ACTIVE' and deleted = false");
        Map<String, Object> params = new LinkedHashMap<>();
        if (keyword != null && !keyword.isBlank()) {
            where.append(" and lower(title) like :kw");
            params.put("kw", "%" + keyword.toLowerCase(Locale.ROOT) + "%");
        }
        if (categoryId != null) {
            where.append(
                    " and id in (select l.productId from ProductCategoryLink l where l.categoryId = :cid)");
            params.put("cid", categoryId);
        }
        return Product.find(where.toString() + " order by featured desc, createdAt desc", params)
                .page(0, limit)
                .list();
    }

    /** 首页精选 */
    public List<Product> featuredProducts(int limit) {
        return Product.find(
                        "status = 'ACTIVE' and deleted = false and featured = true "
                                + "order by createdAt desc")
                .page(0, limit)
                .list();
    }

    // ---------- 视图组装 ----------

    public ProductView toView(Product product) {
        List<ProductView.OptionView> options =
                ProductOption.listByProduct(product.id).stream()
                        .map(
                                option ->
                                        new ProductView.OptionView(
                                                option.id,
                                                option.optionKey,
                                                option.displayName,
                                                ProductOptionValue.listByOption(option.id).stream()
                                                        .map(v -> v.value)
                                                        .toList()))
                        .toList();

        List<ProductView.VariantView> variants =
                Variant.listByProduct(product.id).stream().map(this::toVariantView).toList();

        return new ProductView(
                product.id,
                product.publicId,
                product.title,
                product.slug,
                product.description,
                product.seoDescription,
                product.seoKeywords,
                product.status,
                product.featured,
                product.mainImagePath,
                product.createdAt,
                categoryIdsOf(product.id),
                options,
                variants);
    }

    public ProductView.VariantView toVariantView(Variant variant) {
        Inventory inventory = Inventory.byVariant(variant.id).orElse(null);
        int quantity = inventory == null ? 0 : inventory.quantity;
        int reserved = inventory == null ? 0 : inventory.reserved;
        return new ProductView.VariantView(
                variant.id,
                variant.publicId,
                variant.sku,
                variant.priceCents,
                specText(variant.id),
                quantity,
                reserved,
                Math.max(quantity - reserved, 0));
    }

    /** 商品列表摘要 (含最低价/总可售) */
    public ProductView.Summary toSummary(Product product) {
        List<Variant> variants = Variant.listByProduct(product.id);
        long minPrice = variants.stream().mapToLong(v -> v.priceCents).min().orElse(0L);
        int totalAvailable =
                variants.stream()
                        .mapToInt(
                                v -> Inventory.byVariant(v.id).map(Inventory::available).orElse(0))
                        .sum();
        return new ProductView.Summary(
                product.id,
                product.publicId,
                product.title,
                product.slug,
                product.status,
                product.featured,
                product.mainImagePath,
                minPrice,
                variants.size(),
                totalAvailable,
                product.createdAt);
    }

    /** 变体规格文本: "Color: Red / Size: M" */
    public String specText(Long variantId) {
        List<VariantOptionValue> links = VariantOptionValue.listByVariant(variantId);
        if (links.isEmpty()) {
            return null;
        }
        Map<Long, ProductOption> options = new LinkedHashMap<>();
        for (ProductOption option : ProductOption.<ProductOption>listAll()) {
            options.put(option.id, option);
        }
        return links.stream()
                .map(
                        link -> {
                            ProductOption option = options.get(link.optionId);
                            String name = option == null ? "?" : option.displayName;
                            return name + ": " + link.value;
                        })
                .reduce((a, b) -> a + " / " + b)
                .orElse(null);
    }

    // ---------- 内部 ----------

    private void cartesian(
            List<ProductOption> options,
            int index,
            Deque<Map.Entry<ProductOption, String>> current,
            List<Map<ProductOption, String>> out) {
        if (index == options.size()) {
            Map<ProductOption, String> copy = new LinkedHashMap<>();
            for (Map.Entry<ProductOption, String> entry : current) {
                copy.put(entry.getKey(), entry.getValue());
            }
            out.add(copy);
            return;
        }
        ProductOption option = options.get(index);
        for (ProductOptionValue value : ProductOptionValue.listByOption(option.id)) {
            current.addLast(Map.entry(option, value.value));
            cartesian(options, index + 1, current, out);
            current.removeLast();
        }
    }

    private Map<String, Variant> signatureMap(Long productId) {
        Map<String, Variant> map = new LinkedHashMap<>();
        for (Variant variant : Variant.listByProduct(productId)) {
            Map<Long, String> pairs = new LinkedHashMap<>();
            for (VariantOptionValue link : VariantOptionValue.listByVariant(variant.id)) {
                pairs.put(link.optionId, link.value);
            }
            map.put(normalizedSignature(pairs), variant);
        }
        return map;
    }

    private String signatureOf(Map<ProductOption, String> combo) {
        Map<Long, String> pairs = new LinkedHashMap<>();
        combo.forEach((option, value) -> pairs.put(option.id, value));
        return normalizedSignature(pairs);
    }

    /** 规范化签名: optionId 排序后 "id=value" 用 | 连接 */
    private String normalizedSignature(Map<Long, String> pairs) {
        return pairs.entrySet().stream()
                .sorted(Comparator.comparingLong(Map.Entry::getKey))
                .map(e -> e.getKey() + "=" + e.getValue())
                .reduce((a, b) -> a + "|" + b)
                .orElse("");
    }

    private String normalizeKey(String key) {
        String normalized =
                key == null
                        ? ""
                        : key.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_-]", "-");
        if (normalized.isBlank()) {
            throw new BizException("变体轴 key 不能为空");
        }
        return normalized;
    }

    private String validStatus(String status, String fallback) {
        if (status == null || status.isBlank()) {
            return fallback;
        }
        return switch (status) {
            case Product.STATUS_DRAFT, Product.STATUS_ACTIVE, Product.STATUS_ARCHIVED -> status;
            default -> throw new BizException("非法商品状态: " + status);
        };
    }

    /** slug 生成: 用户提供优先, 否则从 title 推导; 冲突加随机后缀 */
    private String uniqueSlug(
            String candidate, String title, java.util.function.Predicate<String> exists) {
        String base =
                candidate != null && !candidate.isBlank()
                        ? candidate
                                .trim()
                                .toLowerCase(Locale.ROOT)
                                .replaceAll("[^a-z0-9\\u4e00-\\u9fa5]+", "-")
                        : title == null
                                ? "item"
                                : title.trim()
                                        .toLowerCase(Locale.ROOT)
                                        .replaceAll("[^a-z0-9\\u4e00-\\u9fa5]+", "-");
        base = base.replaceAll("^-+|-+$", "");
        if (base.isBlank()) {
            base = "item";
        }
        String slug = base;
        int attempt = 0;
        while (exists.test(slug)) {
            slug = base + "-" + UUID.randomUUID().toString().substring(0, 6);
            attempt++;
            if (attempt > 5) {
                throw new BizException("slug 生成失败, 请手动指定");
            }
        }
        return slug;
    }
}
