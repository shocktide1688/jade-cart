package com.jade.cart.app.storefront.sections;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.qute.Location;
import io.quarkus.qute.Template;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.Map;

/**
 * hero: 首屏大图 + 标题 + 副标题 + CTA config: {headline, sub, image(媒体相对路径), ctaText, ctaHref, align:
 * center|left}
 */
@ApplicationScoped
public class HeroSection implements SectionRenderer {

    @Inject
    @Location("sections/hero")
    Template tpl;

    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public String type() {
        return "hero";
    }

    @Override
    public String render(JsonNode cfg) throws Exception {
        Map<String, Object> c = mapper.readValue(cfg.toString(), Map.class);
        c.putIfAbsent("headline", "");
        c.putIfAbsent("sub", "");
        c.putIfAbsent("image", "");
        c.putIfAbsent("ctaText", "");
        c.putIfAbsent("ctaHref", "");
        c.putIfAbsent("align", "");
        // 强调词: *word* → 衬线斜体 + 粉紫下划线 (仅允许词内安全字符, 输出用 .raw)
        String headline = String.valueOf(c.get("headline"));
        String html =
                headline.replaceAll("\\*([A-Za-z0-9 ,.'\\u2019-]+)\\*", "<em class=\"hl\">$1</em>");
        c.put("headlineHtml", html);
        return tpl.data("cfg", c).render();
    }
}
