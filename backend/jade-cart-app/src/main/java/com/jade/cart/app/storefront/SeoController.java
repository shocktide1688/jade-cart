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
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.time.LocalDate;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/** SEO: robots.txt (交易页 noindex) + sitemap.xml (含图片扩展与博客) */
@Path("/")
@PermitAll
public class SeoController {

    private static final String DEFAULT_BASE_URL = "http://localhost:8090";

    @Inject
    @ConfigProperty(name = "jade.cart.store-base-url", defaultValue = DEFAULT_BASE_URL)
    String configuredBaseUrl;

    /** 站点对外域名: 优先显式配置, 否则按请求头推导 (反代需保留 Host) */
    private String baseUrlOf(HttpHeaders headers) {
        if (configuredBaseUrl != null
                && !configuredBaseUrl.isBlank()
                && !DEFAULT_BASE_URL.equals(configuredBaseUrl.trim())) {
            String v = configuredBaseUrl.trim();
            while (v.endsWith("/")) {
                v = v.substring(0, v.length() - 1);
            }
            return v;
        }
        String host = headers == null ? null : headers.getHeaderString("Host");
        if (host != null && !host.isBlank() && !host.contains(":")) {
            String proto = headers.getHeaderString("X-Forwarded-Proto");
            if (proto == null || proto.isBlank()) {
                proto = "https";
            }
            return proto + "://" + host;
        }
        return "http://localhost:8090";
    }

    @GET
    @Path("robots.txt")
    @Produces(MediaType.TEXT_PLAIN)
    public Response robots(@Context HttpHeaders headers) {
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
                        .formatted(baseUrlOf(headers));
        return Response.ok(body).build();
    }

    @GET
    @Path("sitemap.xml")
    @Produces(MediaType.APPLICATION_XML)
    public Response sitemap(@Context HttpHeaders headers) {
        String today = LocalDate.now().toString();
        StringBuilder xml =
                new StringBuilder(
                        """
                <?xml version="1.0" encoding="UTF-8"?>
                <urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9"
                        xmlns:image="http://www.google.com/schemas/sitemap-image/1.1">
                """);

        // 首页/列表/博客/联系
        xml.append(url(baseUrlOf(headers), "", today, "daily", "1.0", null));
        xml.append(url(baseUrlOf(headers), "/products", today, "daily", "0.9", null));
        xml.append(url(baseUrlOf(headers), "/blog", today, "daily", "0.8", null));
        xml.append(url(baseUrlOf(headers), "/contact", today, "monthly", "0.4", null));

        // 商品: 带 lastmod + 图片扩展
        for (Product p :
                Product.<Product>find("status = ?1 and deleted = false", Product.STATUS_ACTIVE)
                        .list()) {
            String mod = p.updatedAt == null ? today : p.updatedAt.toLocalDate().toString();
            String image =
                    p.mainImagePath == null
                            ? null
                            : baseUrlOf(headers) + "/media/" + p.mainImagePath;
            xml.append(url(baseUrlOf(headers), "/products/" + p.slug, mod, "weekly", "0.8", image));
        }

        // 分类聚合页
        for (Category c : Category.listActive()) {
            xml.append(
                    url(
                            baseUrlOf(headers),
                            "/products?category=" + c.slug,
                            today,
                            "weekly",
                            "0.6",
                            null));
        }

        // 博客文章: 带 lastmod + 封面图
        for (BlogPost post : BlogPost.listActive()) {
            String mod = post.updatedAt == null ? today : post.updatedAt.toLocalDate().toString();
            String image =
                    post.coverPath == null ? null : baseUrlOf(headers) + "/media/" + post.coverPath;
            xml.append(url(baseUrlOf(headers), "/blog/" + post.slug, mod, "weekly", "0.7", image));
        }

        // 内容页 (政策等)
        for (ContentPage page : ContentPage.listActive()) {
            xml.append(
                    url(
                            baseUrlOf(headers),
                            "/pages/" + page.slug,
                            page.updatedAt.toLocalDate().toString(),
                            "monthly",
                            "0.4",
                            null));
        }

        xml.append("</urlset>");
        return Response.ok(xml.toString()).build();
    }

    private String url(
            String baseUrl,
            String path,
            String lastmod,
            String freq,
            String priority,
            String imageUrl) {
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
