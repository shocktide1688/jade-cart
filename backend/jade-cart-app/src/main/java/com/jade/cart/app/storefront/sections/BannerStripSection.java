package com.jade.cart.app.storefront.sections;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.qute.Location;
import io.quarkus.qute.Template;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.Map;
import java.util.regex.Pattern;

/**
 * banner_strip: 公告/促销条 (背景色白名单校验防 CSS 注入)
 * config: {text, bg(默认 --c-primary), href?}
 */
@ApplicationScoped
public class BannerStripSection implements SectionRenderer {

    private static final Pattern COLOR = Pattern.compile("^#[0-9a-fA-F]{3,8}$");

    @Inject
    @Location("sections/banner-strip")
    Template tpl;

    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public String type() {
        return "banner_strip";
    }

    @Override
    public String render(JsonNode cfg) throws Exception {
        Map<String, Object> c = mapper.readValue(cfg.toString(), Map.class);
        c.putIfAbsent("text", "");
        c.putIfAbsent("bg", "");
        c.putIfAbsent("href", "");
        String bg = cfg.path("bg").asText("");
        if (!COLOR.matcher(bg).matches()) {
            c.put("bg", ""); // 空 → 回退 CSS var(--c-primary)
        }
        return tpl.data("cfg", c).render();
    }
}
