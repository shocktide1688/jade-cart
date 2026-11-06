package com.jade.cart.integration;

import com.jade.cart.catalog.entity.Category;
import com.jade.cart.catalog.entity.Inventory;
import com.jade.cart.catalog.entity.Product;
import com.jade.cart.catalog.entity.Variant;
import com.jade.cart.catalog.service.CatalogService;
import com.jade.cart.trade.entity.Order;
import com.jade.cart.trade.entity.OrderItem;
import com.jade.common.exception.BizException;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * ERP 集成服务: 订单增量查询 / 商品增量查询 / 商品 upsert。
 *
 * <p>游标语义: ERP 传 updated_after (ISO-8601, 无时区, 服务器本地时间), 返回 updatedAt >= since 的 记录并附 nextCursor
 * (本批最大 updatedAt); ERP 消费幂等 (按订单号/SKU upsert), 重复拉取无副作用。
 */
@ApplicationScoped
public class IntegrationService {

    private static final DateTimeFormatter CURSOR_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS");

    @Inject CatalogService catalogService;

    /**
     * 订单增量 (含行项目), 按 updatedAt 升序分页。updatedAt 过滤在 Java 内存层做: SQLite 时间列为 TEXT 亲和, 与绑定参数的类型比较不可靠
     * (见事故记录); 数据量为小店铺量级, 内存过滤足够。
     */
    @Transactional
    public Map<String, Object> listOrdersAfter(LocalDateTime since, int page, int size) {
        List<Order> matchedOrders =
                Order.<Order>listAll().stream()
                        .filter(o -> o.updatedAt != null && !o.updatedAt.isBefore(since))
                        .sorted(java.util.Comparator.comparing(o -> o.updatedAt))
                        .toList();
        List<Order> orders = pageSlice(matchedOrders, page, size);
        List<Map<String, Object>> records = new ArrayList<>();
        LocalDateTime max = since;
        for (Order o : orders) {
            if (o.updatedAt.isAfter(max)) {
                max = o.updatedAt;
            }
            List<Map<String, Object>> items = new ArrayList<>();
            for (OrderItem it : OrderItem.listByOrder(o.id)) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("sku", it.sku);
                item.put("title", it.productTitle);
                item.put("variantSpec", it.variantSpec);
                item.put("quantity", it.quantity);
                item.put("unitPriceCents", it.unitPriceCents);
                item.put("lineTotalCents", it.lineTotalCents);
                items.add(item);
            }
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("orderNumber", o.orderNumber);
            row.put("publicId", o.publicId);
            row.put("status", o.status);
            row.put("email", o.email);
            row.put("shipName", o.shipName);
            row.put("shippingCountry", o.shippingCountry);
            row.put("currency", o.currency);
            row.put("subtotalCents", o.subtotalCents);
            row.put("shippingCents", o.shippingCents);
            row.put("totalCents", o.totalCents);
            row.put("placedAt", String.valueOf(o.createdAt));
            row.put("updatedAt", String.valueOf(o.updatedAt));
            row.put("items", items);
            records.add(row);
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("orders", records);
        out.put("nextCursor", max.format(CURSOR_FMT));
        out.put("hasMore", page * size < matchedOrders.size());
        return out;
    }

    /** 商品增量 (变体粒度: 一行一个 SKU), 含可售库存。updatedAt 过滤在 Java 内存层 (同订单增量原因)。 */
    @Transactional
    public Map<String, Object> listProductsAfter(LocalDateTime since, int page, int size) {
        List<Product> matchedProducts =
                Product.<Product>listAll().stream()
                        .filter(pp -> pp.updatedAt != null && !pp.updatedAt.isBefore(since))
                        .sorted(java.util.Comparator.comparing(pp -> pp.updatedAt))
                        .toList();
        List<Product> products = pageSlice(matchedProducts, page, size);
        List<Map<String, Object>> records = new ArrayList<>();
        LocalDateTime max = since;
        for (Product p : products) {
            if (p.updatedAt != null && p.updatedAt.isAfter(max)) {
                max = p.updatedAt;
            }
            for (Variant v : Variant.listByProduct(p.id)) {
                if (v.deleted) {
                    continue;
                }
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("sku", v.sku);
                row.put("title", p.title);
                row.put("slug", p.slug);
                row.put("status", p.status);
                row.put("priceCents", v.priceCents);
                row.put("stock", Inventory.byVariant(v.id).map(i -> i.quantity).orElse(0));
                row.put("updatedAt", String.valueOf(p.updatedAt));
                records.add(row);
            }
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("products", records);
        out.put("nextCursor", max.format(CURSOR_FMT));
        out.put("hasMore", page * size < matchedProducts.size());
        return out;
    }

    /**
     * 商品 upsert (按 SKU 幂等): 存在则更新标题/价格/库存/状态, 不存在则创建商品 + 默认变体 + 库存。 分类按名称自动建; 下架语义 = status DRAFT
     * (不物理删除, 保护订单快照)。
     */
    @Transactional
    public Map<String, Object> upsertProduct(
            String sku,
            String title,
            String description,
            Long priceCents,
            Integer stock,
            String status,
            String categoryName) {
        if (sku == null || sku.isBlank()) {
            throw new BizException("sku 不能为空");
        }
        if (title == null || title.isBlank()) {
            throw new BizException("title 不能为空");
        }
        if (priceCents != null && priceCents < 0) {
            throw new BizException("priceCents 不能为负");
        }
        String normStatus =
                "ACTIVE".equalsIgnoreCase(status) ? Product.STATUS_ACTIVE : Product.STATUS_DRAFT;

        Variant variant = Variant.findBySku(sku).orElse(null);
        String action;
        Product product;
        if (variant != null && !variant.deleted) {
            action = "updated";
            product = Product.findById(variant.productId);
            if (product == null || product.deleted) {
                throw new BizException("变体所属商品不存在: " + sku);
            }
            if (!title.equals(product.title)) {
                product.title = title;
            }
            if (description != null) {
                product.description = description;
            }
            product.status = normStatus;
            product.persist();
        } else {
            action = "created";
            Long categoryId = resolveOrCreateCategory(categoryName);
            product =
                    catalogService.createProduct(
                            title,
                            null,
                            description,
                            null,
                            normStatus,
                            false,
                            priceCents,
                            sku,
                            null,
                            null);
            variant = Variant.findBySku(sku).orElseThrow();
            if (categoryId != null) {
                catalogService.linkCategories(product.id, List.of(categoryId));
            }
        }
        if (priceCents != null && variant != null && !priceCents.equals(variant.priceCents)) {
            variant.priceCents = priceCents;
            variant.persist();
        }
        if (stock != null) {
            Inventory inv = Inventory.byVariant(variant.id).orElseThrow();
            inv.quantity = stock;
            inv.persist();
        }
        Log.infof("集成商品 upsert: action=%s sku=%s", action, sku);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("action", action);
        out.put("sku", sku);
        out.put("productPublicId", product.publicId);
        out.put("slug", product.slug);
        out.put("status", product.status);
        return out;
    }

    private Long resolveOrCreateCategory(String categoryName) {
        if (categoryName == null || categoryName.isBlank()) {
            return null;
        }
        Category existing = Category.find("name", categoryName).firstResult();
        if (existing != null && !existing.deleted) {
            return existing.id;
        }
        String slug =
                "erp-"
                        + categoryName
                                .trim()
                                .toLowerCase(java.util.Locale.ROOT)
                                .replaceAll("[^a-z0-9\\u4e00-\\u9fa5]+", "-");
        Category created = catalogService.createCategory(categoryName, slug, null, 0);
        return created.id;
    }

    private <T> List<T> pageSlice(List<T> list, int page, int size) {
        int from = Math.min((page - 1) * size, list.size());
        int to = Math.min(from + size, list.size());
        return list.subList(from, to);
    }
}
