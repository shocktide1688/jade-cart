package com.jade.cart.app.storefront.sections;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.qute.Location;
import io.quarkus.qute.Template;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.List;
import java.util.Map;

/**
 * newsletter: 邮件订阅区 (无 JS 表单, 提交到店面 /contact, 落客服留言 general) config: {title, text, placeholder,
 * button}
 */
@ApplicationScoped
public class NewsletterSection implements SectionRenderer {

    @Inject
    @Location("sections/newsletter")
    Template tpl;

    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public String type() {
        return "newsletter";
    }

    @Override
    public String render(JsonNode cfg) throws Exception {
        Map<String, Object> c = mapper.readValue(cfg.toString(), Map.class);
        for (String k : List.of("title", "text", "placeholder", "button")) {
            c.putIfAbsent(k, "");
        }
        return tpl.data("cfg", c).render();
    }
}
