package com.jade.cart.app.storefront.sections;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jade.cart.blog.entity.BlogPost;
import io.quarkus.qute.Location;
import io.quarkus.qute.Template;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.List;
import java.util.Map;

/** blog_latest: 最新文章 config: {title, limit} */
@ApplicationScoped
public class BlogLatestSection implements SectionRenderer {

    @Inject
    @Location("sections/blog-latest")
    Template tpl;

    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public String type() {
        return "blog_latest";
    }

    @Override
    public String render(JsonNode cfg) throws Exception {
        int limit = Math.min(Math.max(cfg.path("limit").asInt(3), 1), 9);
        List<BlogPost> posts = BlogPost.listActive();
        posts = posts.subList(0, Math.min(limit, posts.size()));
        Map<String, Object> c = mapper.readValue(cfg.toString(), Map.class);
        c.putIfAbsent("title", "");
        return tpl.data("cfg", c).data("posts", posts).render();
    }
}
