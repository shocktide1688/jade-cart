package com.jade.cart.app.storefront;

import com.jade.cart.blog.entity.BlogPost;
import com.jade.cart.catalog.entity.Category;
import com.jade.cart.catalog.entity.Product;
import com.jade.cart.content.entity.ContentPage;
import jakarta.annotation.security.PermitAll;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.time.LocalDate;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/** SEO: robots.txt (交易页 noindex) + sitemap.xml (含图片扩展与博客) */
@Path("/")
@PermitAll
public class SeoController {

    @Inject
    @ConfigProperty(name = "jade.cart.store-base-url", defaultValue = "http://localhost:8090")
    String baseUrl;

    @GET
    @Path("robots.txt")
    @Produces(MediaType.TEXT_PLAIN)
    public Response robots() {
        // 购物车/收银台/订单页是薄内容, 明确禁止索引
        String body =
                """
                User-agent: *
                Allow: /
                Disallow: /admin
                Disallow: /api/
                Disallow: /cart
                Disallow: /orders/
                Disallow: /pay/
                Disallow: /checkout

                Sitemap: %s/sitemap.xml
                """
                        .formatted(baseUrl);
        return Response.ok(body).build();
    }

    @GET
    @Path("sitemap.xml")
    @Produces(MediaType.APPLICATION_XML)
    public Response sitemap() {
        String today = LocalDate.now().toString();
        StringBuilder xml =
                new StringBuilder(
                        """
                <?xml version="1.0" encoding="UTF-8"?>
                <urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9"
                        xmlns:image="http://www.google.com/schemas/sitemap-image/1.1">
                """);

        // 首页/列表/博客/联系
        xml.append(url("", today, "daily", "1.0", null));
        xml.append(url("/products", today, "daily", "0.9", null));
        xml.append(url("/blog", today, "daily", "0.8", null));
        xml.append(url("/contact", today, "monthly", "0.4", null));

        // 商品: 带 lastmod + 图片扩展
        for (Product p :
                Product.<Product>find("status = ?1 and deleted = false", Product.STATUS_ACTIVE)
                        .list()) {
            String mod = p.updatedAt == null ? today : p.updatedAt.toLocalDate().toString();
            String image = p.mainImagePath == null ? null : baseUrl + "/media/" + p.mainImagePath;
            xml.append(url("/products/" + p.slug, mod, "weekly", "0.8", image));
        }

        // 分类聚合页
        for (Category c : Category.listActive()) {
            xml.append(url("/products?category=" + c.slug, today, "weekly", "0.6", null));
        }

        // 博客文章: 带 lastmod + 封面图
        for (BlogPost post : BlogPost.listActive()) {
            String mod = post.updatedAt == null ? today : post.updatedAt.toLocalDate().toString();
            String image = post.coverPath == null ? null : baseUrl + "/media/" + post.coverPath;
            xml.append(url("/blog/" + post.slug, mod, "weekly", "0.7", image));
        }

        // 内容页 (政策等)
        for (ContentPage page : ContentPage.listActive()) {
            xml.append(
                    url(
                            "/pages/" + page.slug,
                            page.updatedAt.toLocalDate().toString(),
                            "monthly",
                            "0.4",
                            null));
        }

        xml.append("</urlset>");
        return Response.ok(xml.toString()).build();
    }

    private String url(String path, String lastmod, String freq, String priority, String imageUrl) {
        StringBuilder sb =
                new StringBuilder("  <url><loc>")
                        .append(esc(baseUrl + path))
                        .append("</loc><lastmod>")
                        .append(lastmod)
                        .append("</lastmod><changefreq>")
                        .append(freq)
                        .append("</changefreq><priority>")
                        .append(priority)
                        .append("</priority>");
        if (imageUrl != null) {
            sb.append("<image:image><image:loc>")
                    .append(esc(imageUrl))
                    .append("</image:loc></image:image>");
        }
        sb.append("</url>\n");
        return sb.toString();
    }

    private String esc(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
