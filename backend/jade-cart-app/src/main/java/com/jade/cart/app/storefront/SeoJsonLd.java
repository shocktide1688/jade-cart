package com.jade.cart.app.storefront;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jade.cart.blog.entity.BlogPost;
import com.jade.cart.catalog.entity.Product;
import com.jade.cart.catalog.entity.Variant;
import com.jade.cart.catalog.service.CatalogService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 结构化数据 (JSON-LD) 构建器: Product / BreadcrumbList / WebSite+SearchAction / ItemList / BlogPosting 输出经
 * Jackson 序列化, 模板以 {jsonLd.raw} 原样输出
 */
@ApplicationScoped
public class SeoJsonLd {

    private static final ObjectMapper M = new ObjectMapper();

    @Inject CatalogService catalogService;

    /** WebSite + Sitelinks SearchAction (首页) */
    public String website(String baseUrl, String storeName) {
        Map<String, Object> g = base("@type", "WebSite");
        g.put("name", storeName);
        g.put("url", baseUrl + "/");
        Map<String, Object> sa = new LinkedHashMap<>();
        sa.put("@type", "SearchAction");
        sa.put(
                "target",
                Map.of(
                        "@type",
                        "EntryPoint",
                        "urlTemplate",
                        baseUrl + "/products?q={search_term_string}"));
        sa.put("query-input", "required name=search_term_string");
        g.put("potentialAction", sa);
        return write(g);
    }

    /** Product 完整结构化数据: 图片/描述/品牌/SKU/价格区间/库存状态 */
    public String product(Product p, String baseUrl) {
        List<Variant> variants = Variant.listByProduct(p.id);
        long low = variants.stream().mapToLong(v -> v.priceCents).min().orElse(0);
        long high = variants.stream().mapToLong(v -> v.priceCents).max().orElse(0);
        int totalAvailable =
                variants.stream()
                        .mapToInt(
                                v ->
                                        com.jade.cart.catalog.entity.Inventory.byVariant(v.id)
                                                .map(
                                                        com.jade.cart.catalog.entity.Inventory
                                                                ::available)
                                                .orElse(0))
                        .sum();

        Map<String, Object> g = base("@type", "Product");
        g.put("name", p.title);
        g.put("url", baseUrl + "/products/" + p.slug);
        if (p.description != null && !p.description.isBlank()) {
            g.put(
                    "description",
                    clip(
                            p.seoDescription != null && !p.seoDescription.isBlank()
                                    ? p.seoDescription
                                    : p.description,
                            300));
        }
        if (p.mainImagePath != null) {
            g.put("image", baseUrl + "/media/" + p.mainImagePath);
        }
        g.put("brand", Map.of("@type", "Brand", "name", "Jade Cart"));
        if (!variants.isEmpty()) {
            g.put("sku", variants.get(0).sku);
        }

        Map<String, Object> offers = base("@type", "AggregateOffer");
        offers.put("url", baseUrl + "/products/" + p.slug);
        offers.put("priceCurrency", "USD");
        offers.put("lowPrice", low / 100.0);
        offers.put("highPrice", high / 100.0);
        offers.put("offerCount", variants.size());
        offers.put(
                "availability",
                totalAvailable > 0
                        ? "https://schema.org/InStock"
                        : "https://schema.org/OutOfStock");
        g.put("offers", offers);
        return write(g);
    }

    /** BreadcrumbList: 首页 > 商品 > 当前商品 */
    public String breadcrumb(String baseUrl, String... namesAndPaths) {
        List<Map<String, Object>> items = new ArrayList<>();
        int pos = 1;
        for (int i = 0; i < namesAndPaths.length; i += 2) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("@type", "ListItem");
            item.put("position", pos++);
            item.put("name", namesAndPaths[i]);
            item.put("item", baseUrl + namesAndPaths[i + 1]);
            items.add(item);
        }
        Map<String, Object> g = base("@type", "BreadcrumbList");
        g.put("itemListElement", items);
        return write(g);
    }

    /** ItemList (商品列表页): 位置/名称/链接 */
    public String itemList(String baseUrl, List<Product> products) {
        List<Map<String, Object>> items = new ArrayList<>();
        int pos = 1;
        for (Product p : products) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("@type", "ListItem");
            item.put("position", pos++);
            item.put("name", p.title);
            item.put("url", baseUrl + "/products/" + p.slug);
            items.add(item);
        }
        Map<String, Object> g = base("@type", "ItemList");
        g.put("itemListElement", items);
        return write(g);
    }

    /** 博客列表 ItemList (博客列表页) */
    public String blogList(String baseUrl, List<BlogPost> posts) {
        List<Map<String, Object>> items = new ArrayList<>();
        int pos = 1;
        for (BlogPost p : posts) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("@type", "ListItem");
            item.put("position", pos++);
            item.put("name", p.title);
            item.put("url", baseUrl + "/blog/" + p.slug);
            items.add(item);
        }
        Map<String, Object> g = base("@type", "ItemList");
        g.put("itemListElement", items);
        return write(g);
    }

    /** BlogPosting (博客文章页) */
    public String blogPosting(BlogPost post, String baseUrl, String storeName) {
        Map<String, Object> g = base("@type", "BlogPosting");
        g.put("headline", post.title);
        g.put("url", baseUrl + "/blog/" + post.slug);
        g.put("mainEntityOfPage", baseUrl + "/blog/" + post.slug);
        if (post.coverPath != null) {
            g.put("image", baseUrl + "/media/" + post.coverPath);
        }
        if (post.seoDescription != null && !post.seoDescription.isBlank()) {
            g.put("description", post.seoDescription);
        } else if (post.excerpt != null && !post.excerpt.isBlank()) {
            g.put("description", post.excerpt);
        }
        if (post.publishedAt != null) {
            g.put("datePublished", post.publishedAt.toString());
        }
        g.put("dateModified", post.updatedAt == null ? null : post.updatedAt.toString());
        g.put("author", Map.of("@type", "Organization", "name", storeName));
        g.put("publisher", Map.of("@type", "Organization", "name", storeName));
        return write(g);
    }

    // ---------- 内部 ----------

    private Map<String, Object> base(String key, Object type) {
        Map<String, Object> g = new LinkedHashMap<>();
        g.put("@context", "https://schema.org");
        g.put(key, type);
        return g;
    }

    private String clip(String s, int max) {
        if (s == null) {
            return null;
        }
        String flat = s.replaceAll("\\s+", " ").trim();
        return flat.length() <= max ? flat : flat.substring(0, max - 1) + "…";
    }

    private String write(Map<String, Object> json) {
        try {
            return M.writeValueAsString(json);
        } catch (Exception e) {
            return "{}";
        }
    }
}
