package com.jade.cart.app.storefront.sections;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jade.cart.blog.entity.BlogPost;
import io.quarkus.qute.Location;
import io.quarkus.qute.Template;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** blog_latest: 最新文章 (日期格式化 MMMM d, yyyy) config: {title, limit} */
@ApplicationScoped
public class BlogLatestSection implements SectionRenderer {

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.ENGLISH);

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
        List<Map<String, Object>> view = new ArrayList<>();
        for (BlogPost post : posts) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("slug", post.slug);
            m.put("title", post.title);
            m.put("coverPath", post.coverPath);
            LocalDateTime when = post.publishedAt != null ? post.publishedAt : post.updatedAt;
            m.put("date", when == null ? "" : when.format(DATE_FMT));
            view.add(m);
        }
        Map<String, Object> c = mapper.readValue(cfg.toString(), Map.class);
        c.putIfAbsent("title", "");
        return tpl.data("cfg", c).data("posts", view).render();
    }
}
