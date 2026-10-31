package com.jade.cart.app.storefront.sections;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jade.cart.catalog.entity.Category;
import io.quarkus.qute.Location;
import io.quarkus.qute.Template;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.Map;

/** category_grid: 分类导航卡 config: {title, limit} */
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
        Map<String, Object> c = mapper.readValue(cfg.toString(), Map.class);
        c.putIfAbsent("title", "");
        return tpl.data("cfg", c).data("categories", Category.listActive()).render();
    }
}
