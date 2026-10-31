package com.jade.cart.app.storefront.sections;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jade.cart.content.service.ContentService;
import io.quarkus.qute.Location;
import io.quarkus.qute.Template;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.Map;

/**
 * rich_text: 富文本区块 (受限 Markdown)
 * config: {title?, markdown}
 */
@ApplicationScoped
public class RichTextSection implements SectionRenderer {

    @Inject
    ContentService contentService;

    @Inject
    @Location("sections/rich-text")
    Template tpl;

    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public String type() {
        return "rich_text";
    }

    @Override
    public String render(JsonNode cfg) throws Exception {
        String html = contentService.renderMarkdown(cfg.path("markdown").asText(""));
        Map<String, Object> c = mapper.readValue(cfg.toString(), Map.class);
        c.putIfAbsent("title", "");
        c.putIfAbsent("markdown", "");
        return tpl.data("cfg", c).data("html", html).render();
    }
}
