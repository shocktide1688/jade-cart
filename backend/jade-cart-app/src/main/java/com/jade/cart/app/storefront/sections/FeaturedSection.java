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

/**
 * featured: 商品网格
 * config: {title, limit, source: "featured"|"latest"|"category:<slug>"}
 */
@ApplicationScoped
public class FeaturedSection implements SectionRenderer {

    @Inject
    CatalogService catalogService;

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
        String categorySlug = source.startsWith("category:") ? source.substring("category:".length()) : null;

        List<Product> products;
        if ("latest".equals(source)) {
            products = catalogService.activeProducts(null, null, limit);
        } else if (categorySlug != null && !categorySlug.isBlank()) {
            com.jade.cart.catalog.entity.Category category =
                    com.jade.cart.catalog.entity.Category.findBySlug(categorySlug);
            products = catalogService.activeProducts(null,
                    category == null ? null : category.id, limit);
        } else {
            products = catalogService.featuredProducts(limit);
            if (products.isEmpty()) {
                products = catalogService.activeProducts(null, null, limit);
            }
        }
        List<ProductView.Summary> items = products.stream().map(catalogService::toSummary).toList();

        Map<String, Object> c = mapper.readValue(cfg.toString(), Map.class);
        c.putIfAbsent("title", "");
        return tpl.data("cfg", c).data("items", items).render();
    }
}
