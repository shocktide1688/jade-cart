package com.jade.cart.app.storefront.sections;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jade.cart.catalog.dto.ProductView;
import com.jade.cart.catalog.entity.Product;
import com.jade.cart.catalog.service.CatalogService;
import io.quarkus.qute.Location;
import io.quarkus.qute.Template;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.List;
import java.util.Map;

/** featured: 商品网格 config: {title, limit, source: "featured"|"latest"|"category:<slug>"} */
@ApplicationScoped
public class FeaturedSection implements SectionRenderer {

    @Inject CatalogService catalogService;

    @Inject
    @Location("sections/featured")
    Template tpl;

    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public String type() {
        return "featured";
    }

    @Override
    public String render(JsonNode cfg) throws Exception {
        String source = cfg.path("source").asText("featured");
        int limit = Math.min(Math.max(cfg.path("limit").asInt(8), 1), 24);
        String categorySlug =
                source.startsWith("category:") ? source.substring("category:".length()) : null;

        List<Product> products;
        if ("latest".equals(source)) {
            products = catalogService.activeProducts(null, null, limit);
        } else if (categorySlug != null && !categorySlug.isBlank()) {
            com.jade.cart.catalog.entity.Category category =
                    com.jade.cart.catalog.entity.Category.findBySlug(categorySlug);
            products =
                    catalogService.activeProducts(
                            null, category == null ? null : category.id, limit);
        } else {
            products = catalogService.featuredProducts(limit);
            if (products.isEmpty()) {
                products = catalogService.activeProducts(null, null, limit);
            }
        }
        List<ProductView.Summary> summaries =
                products.stream().map(catalogService::toSummary).toList();
        // 视图组装: 附带首个变体 (快速加购用); 变体数>1 时按钮跳详情页选规格
        List<Map<String, Object>> items = new java.util.ArrayList<>();
        for (ProductView.Summary s : summaries) {
            Map<String, Object> m = new java.util.LinkedHashMap<>();
            m.put("title", s.title());
            m.put("slug", s.slug());
            m.put("mainImagePath", s.mainImagePath());
            m.put("minPriceCents", s.minPriceCents());
            m.put("totalAvailable", s.totalAvailable());
            com.jade.cart.catalog.entity.Variant first =
                    com.jade.cart.catalog.entity.Variant.find(
                                    "productId = ?1 and deleted = false order by id", s.id())
                            .firstResult();
            long variantCount =
                    com.jade.cart.catalog.entity.Variant.count(
                            "productId = ?1 and deleted = false", s.id());
            m.put("variantId", first == null ? null : first.id);
            m.put("multi", variantCount > 1);
            items.add(m);
        }

        Map<String, Object> c = mapper.readValue(cfg.toString(), Map.class);
        c.putIfAbsent("title", "");
        return tpl.data("cfg", c).data("items", items).render();
    }
}
