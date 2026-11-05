package com.jade.cart.app.storefront.sections;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jade.cart.catalog.entity.Category;
import com.jade.cart.catalog.entity.Product;
import com.jade.cart.catalog.entity.ProductCategoryLink;
import io.quarkus.qute.Location;
import io.quarkus.qute.Template;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** category_grid: 分类导航卡 (图卡样式, 封面取该分类下首个有主图的在售商品) config: {title, limit} */
@ApplicationScoped
public class CategoryGridSection implements SectionRenderer {

    @Inject
    @Location("sections/category-grid")
    Template tpl;

    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public String type() {
        return "category_grid";
    }

    @Override
    public String render(JsonNode cfg) throws Exception {
        int limit = Math.min(Math.max(cfg.path("limit").asInt(6), 1), 12);
        Map<String, Object> c = mapper.readValue(cfg.toString(), Map.class);
        c.putIfAbsent("title", "");
        List<Category> all = Category.listActive();
        List<Map<String, Object>> cats = new ArrayList<>();
        for (Category cat : all.subList(0, Math.min(limit, all.size()))) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("name", cat.name);
            m.put("slug", cat.slug);
            m.put("cover", firstProductImage(cat.id));
            cats.add(m);
        }
        return tpl.data("cfg", c).data("categories", cats).render();
    }

    /** 分类封面: 首个带主图的关联商品 (无则 null, 模板走占位) */
    private String firstProductImage(Long categoryId) {
        List<ProductCategoryLink> links =
                ProductCategoryLink.find("categoryId", categoryId).page(0, 8).list();
        for (ProductCategoryLink link : links) {
            Product p = Product.findById(link.productId);
            if (p != null && !p.deleted && p.mainImagePath != null && !p.mainImagePath.isBlank()) {
                return p.mainImagePath;
            }
        }
        return null;
    }
}
