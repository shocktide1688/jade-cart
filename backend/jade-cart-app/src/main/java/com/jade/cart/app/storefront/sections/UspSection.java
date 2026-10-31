package com.jade.cart.app.storefront.sections;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.qute.Location;
import io.quarkus.qute.Template;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * usp: 多列卖点条
 * config: {items: [{icon, title, text}]}
 */
@ApplicationScoped
public class UspSection implements SectionRenderer {

    @Inject
    @Location("sections/usp")
    Template tpl;

    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public String type() {
        return "usp";
    }

    @Override
    public String render(JsonNode cfg) throws Exception {
        List<Map<String, Object>> items = new ArrayList<>();
        for (JsonNode n : cfg.path("items")) {
            Map<String, Object> item = new java.util.LinkedHashMap<>();
            item.put("icon", n.path("icon").asText("✦"));
            item.put("title", n.path("title").asText(""));
            item.put("text", n.path("text").asText(""));
            items.add(item);
        }
        Map<String, Object> c = mapper.readValue(cfg.toString(), Map.class);
        return tpl.data("cfg", c).data("items", items).render();
    }
}
