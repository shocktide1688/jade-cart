package com.jade.cart.app.storefront;

import com.jade.cart.app.storefront.i18n.StoreMessages;
import com.jade.cart.app.storefront.i18n.UiText;
import com.jade.cart.catalog.dto.ProductView;
import com.jade.cart.catalog.entity.Category;
import com.jade.cart.catalog.entity.Product;
import com.jade.cart.catalog.entity.ProductImage;
import com.jade.cart.catalog.service.CatalogService;
import com.jade.cart.trade.dto.CartView;
import com.jade.cart.trade.entity.Order;
import com.jade.cart.trade.service.CartService;
import com.jade.cart.trade.service.OrderService;
import com.jade.common.exception.BizException;
import io.quarkus.logging.Log;
import io.quarkus.qute.Location;
import io.quarkus.qute.Template;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import java.net.URI;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/** 店面 SSR 页面 (Qute, 英文, 无 JS 可用 — kartwo 理念): 首页 / 商品列表 / 详情 / 购物车 / Mock 收银台 / 订单状态 */
@Path("/")
@Produces(MediaType.TEXT_HTML)
@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
public class StorefrontPages {

    @Inject
    @ConfigProperty(name = "jade.cart.store-name", defaultValue = "Jade Cart Store")
    String storeName;

    /** 未配置时的默认值 (等于此值视为未配置, 走请求头推导) */
    private static final String DEFAULT_BASE_URL = "http://localhost:8090";

    @Inject
    @ConfigProperty(name = "jade.cart.store-base-url", defaultValue = DEFAULT_BASE_URL)
    String configuredBaseUrl;

    /** 站点对外域名: 优先显式配置 (JADE_CART_STORE_BASE_URL), 否则按请求头推导 (反代需保留 Host) */
    private String baseUrlOf(HttpHeaders headers) {
        if (configuredBaseUrl != null
                && !configuredBaseUrl.isBlank()
                && !DEFAULT_BASE_URL.equals(configuredBaseUrl.trim())) {
            return trimSlash(configuredBaseUrl);
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

    private static String trimSlash(String s) {
        String v = s.trim();
        while (v.endsWith("/")) {
            v = v.substring(0, v.length() - 1);
        }
        return v;
    }

    /** 启动自检: 站点域名仍是 localhost → canonical/sitemap 会指向本机, 搜索引擎索引失效 */
    @PostConstruct
    void warnIfBaseUrlUnset() {
        if (configuredBaseUrl == null
                || configuredBaseUrl.isBlank()
                || DEFAULT_BASE_URL.equals(configuredBaseUrl.trim())) {
            Log.warnf(
                    "jade.cart.store-base-url 未配置: canonical/og:url/sitemap 按请求头推导, "
                            + "若反代未保留 Host 头会回落到 localhost —— 生产请显式设置 "
                            + "JADE_CART_STORE_BASE_URL");
            return;
        }
        if (configuredBaseUrl.contains("localhost")) {
            Log.errorf(
                    "jade.cart.store-base-url 指向 localhost (%s): canonical/sitemap 无效, "
                            + "搜索引擎无法索引该站",
                    configuredBaseUrl);
        }
    }

    @Inject CatalogService catalogService;

    @Inject CartService cartService;

    @Inject OrderService orderService;

    @Inject
    @Location("store/home")
    Template home;

    @Inject
    @Location("store/products")
    Template products;

    @Inject
    @Location("store/product")
    Template product;

    @Inject
    @Location("store/cart")
    Template cart;

    @Inject
    @Location("store/pay-mock")
    Template payMock;

    @Inject
    @Location("store/pay-paypal")
    Template payPaypal;

    @Inject
    @Location("store/page")
    Template contentTpl;

    @Inject
    @Location("store/notfound")
    Template notFoundTpl;

    @Inject
    @Location("store/contact")
    Template contactTpl;

    @Inject com.jade.cart.contact.service.ContactService contactService;

    @Inject com.jade.cart.blog.service.BlogService blogService;

    @Inject SeoJsonLd seoJsonLd;

    @Inject com.jade.cart.settings.service.ThemeService themeService;

    @Inject StorefrontService storefrontService;

    @Inject StoreMessages messages;

    @Inject
    @Location("store/blog")
    Template blogTpl;

    @Inject
    @Location("store/blog-post")
    Template blogPostTpl;

    @Inject
    @Location("store/order")
    Template orderTpl;

    /**
     * 页面响应: 统一带 Cache-Control: no-cache。
     *
     * <p>店面页面 HTML 内联了整套 CSS (主题变量运行时注入), HTML 变了样式就变了; 不设缓存头时浏览器按启发式复用旧 HTML, 用户看到旧样式却查不出原因。
     * 媒体/图片走 PublicMediaController 的 immutable 长缓存, 不受影响。
     */
    private Response page(String html) {
        return Response.ok(html).header("Cache-Control", "no-cache").build();
    }

    // ---------- 页面 ----------

    @GET
    public Response index(@Context HttpHeaders headers) {
        List<ProductView.Summary> featured =
                catalogService.featuredProducts(8).stream().map(catalogService::toSummary).toList();
        List<ProductView.Summary> latest =
                catalogService.activeProducts(null, null, 12).stream()
                        .map(catalogService::toSummary)
                        .toList();

        // og:image (社交分享预览图): 取首张精选商品主图, 没有则用最新
        String homeImage = null;
        for (List<ProductView.Summary> pool : List.of(featured, latest)) {
            for (ProductView.Summary cand : pool) {
                if (cand.mainImagePath() != null && !cand.mainImagePath().isBlank()) {
                    homeImage = baseUrlOf(headers) + "/media/" + cand.mainImagePath();
                    break;
                }
            }
            if (homeImage != null) {
                break;
            }
        }
        String homeDesc =
                com.jade.cart.settings.entity.StoreSetting.getValue("announcement")
                        .filter(v -> !v.isBlank())
                        .orElseGet(() -> fmt(t(headers).seoHome(), storeName));
        return page(
                home.data("sections", storefrontService.renderHome())
                        .data("featured", featured)
                        .data("latest", latest)
                        .data("storeName", storeName)
                        .data("baseUrl", baseUrlOf(headers))
                        .data("navCats", navTree())
                        .data("theme", themeService.tokens())
                        .data("t", t(headers))
                        .data("langs", langs(headers))
                        .data("seo", seo(homeDesc, homeImage, false, "website", "/"))
                        .data("jsonLd", ld(seoJsonLd.website(baseUrlOf(headers), storeName)))
                        .render());
    }

    @GET
    @Path("products")
    public Response productList(
            @Context HttpHeaders headers,
            @QueryParam("q") String q,
            @QueryParam("category") String categorySlug) {
        Long categoryId = null;
        String categoryName = null;
        if (categorySlug != null && !categorySlug.isBlank()) {
            Category category = Category.findBySlug(categorySlug);
            if (category != null) {
                categoryId = category.id;
                categoryName = category.name;
            }
        }
        List<ProductView.Summary> items =
                catalogService.activeProducts(q, categoryId, 60).stream()
                        .map(catalogService::toSummary)
                        .toList();
        String desc =
                categoryName != null
                        ? fmt(t(headers).seoCategory(), categoryName, storeName)
                        : q != null && !q.isBlank()
                                ? fmt(t(headers).seoSearch(), q, storeName)
                                : fmt(t(headers).seoAllProducts(), storeName);
        // SEO: 页面标题 (此前未传 title → 回落 homeTitle, 与首页标题重复) + og:image 取首图
        String pageTitle = categoryName != null ? categoryName : t(headers).footAllProducts();
        String ogImage =
                items.isEmpty() || items.get(0).mainImagePath() == null
                        ? null
                        : baseUrlOf(headers) + "/media/" + items.get(0).mainImagePath();
        return page(
                products.data("items", items)
                        .data("pageTitle", pageTitle)
                        .data("q", q == null ? "" : q)
                        .data("categoryName", categoryName)
                        .data("currentSlug", categorySlug)
                        .data("categories", Category.listActive())
                        .data("storeName", storeName)
                        .data("baseUrl", baseUrlOf(headers))
                        .data("navCats", navTree())
                        .data("theme", themeService.tokens())
                        .data("t", t(headers))
                        .data("langs", langs(headers))
                        .data("seo", seo(desc, ogImage, false, "website", "/products"))
                        .data(
                                "jsonLd",
                                ld(
                                        seoJsonLd.itemList(
                                                baseUrlOf(headers),
                                                catalogService.activeProducts(q, categoryId, 20))))
                        .render());
    }

    @GET
    @Path("products/{slug}")
    public Response productDetail(
            @Context HttpHeaders headers,
            @PathParam("slug") String slug,
            @QueryParam("img") Integer img) {
        Product entity = Product.findBySlug(slug);
        if (entity == null || !entity.isActive()) {
            return Response.status(Response.Status.NOT_FOUND).entity("Product not found").build();
        }
        ProductView view = catalogService.toView(entity);
        // 相册 (主图 + 补充图); ?img=N 切换主展示图 (无 JS 画廊)
        List<Map<String, Object>> gallery = new ArrayList<>();
        if (entity.mainImagePath != null && !entity.mainImagePath.isBlank()) {
            gallery.add(Map.of("idx", 0, "path", entity.mainImagePath));
        }
        List<ProductImage> extras =
                ProductImage.find("productId = ?1 order by position asc, id asc", entity.id).list();
        int extraPos = gallery.isEmpty() ? 0 : 1;
        for (ProductImage pi : extras) {
            gallery.add(Map.of("idx", extraPos, "path", pi.path));
            extraPos++;
        }
        int curIdx = (img == null || img < 0 || img >= gallery.size()) ? 0 : img;
        String mainImg = gallery.isEmpty() ? null : (String) gallery.get(curIdx).get("path");
        List<ProductView.Summary> related =
                catalogService.activeProducts(null, null, 4).stream()
                        .filter(pr -> !pr.id.equals(entity.id))
                        .map(catalogService::toSummary)
                        .limit(3)
                        .toList();
        String desc =
                SeoExtensions.teaser(
                        entity.seoDescription != null && !entity.seoDescription.isBlank()
                                ? entity.seoDescription
                                : entity.description,
                        160);
        String ogImage =
                entity.mainImagePath == null
                        ? null
                        : baseUrlOf(headers) + "/media/" + entity.mainImagePath;
        String jsonLd =
                ld(seoJsonLd.product(entity, baseUrlOf(headers)))
                        + "\n"
                        + ld(
                                seoJsonLd.breadcrumb(
                                        baseUrlOf(headers),
                                        t(headers).crumbHome(),
                                        "/",
                                        t(headers).crumbProducts(),
                                        "/products",
                                        entity.title,
                                        "/products/" + entity.slug));
        // 面包屑 (首个分类) + 购买区数据
        String crumbName = null;
        String crumbSlug = null;
        if (view.categoryIds() != null && !view.categoryIds().isEmpty()) {
            Category c = Category.findById(view.categoryIds().get(0));
            if (c != null) {
                crumbName = c.name;
                crumbSlug = c.slug;
            }
        }
        long priceCents = view.variants().isEmpty() ? 0 : view.variants().get(0).priceCents();
        int totalAvail =
                view.variants().stream()
                        .mapToInt(v -> v.available() == null ? 0 : v.available())
                        .sum();
        boolean lowStock = totalAvail > 0 && totalAvail <= 5;
        return page(
                product.data("p", view)
                        .data("related", related)
                        .data("crumbName", crumbName)
                        .data("crumbSlug", crumbSlug)
                        .data("priceCents", priceCents)
                        .data("totalAvail", totalAvail)
                        .data("lowStock", lowStock)
                        .data("gallery", gallery)
                        .data("mainImg", mainImg)
                        .data("curIdx", curIdx)
                        .data("desc", desc)
                        .data("storeName", storeName)
                        .data("baseUrl", baseUrlOf(headers))
                        .data("navCats", navTree())
                        .data("theme", themeService.tokens())
                        .data("t", t(headers))
                        .data("langs", langs(headers))
                        .data(
                                "seo",
                                seo(
                                        desc,
                                        ogImage,
                                        false,
                                        "product",
                                        "/products/" + entity.slug,
                                        entity.seoKeywords))
                        .data("jsonLd", jsonLd)
                        .render());
    }

    @GET
    @Path("cart")
    public Response cartPage(@Context HttpHeaders headers, @QueryParam("error") String error) {
        CartView view =
                Optional.ofNullable(CartService.readToken(headers).orElse(null))
                        .map(token -> cartService.view(cartService.getOrCreate(token)))
                        .orElseGet(() -> new CartView(null, "USD", List.of(), 0L));
        // Cross-Sell: 空购物车时展示在售商品 (对标 Shopify 空车推荐位)
        List<ProductView.Summary> cross =
                catalogService.activeProducts(null, null, 4).stream()
                        .map(catalogService::toSummary)
                        .toList();
        return page(
                cart.data("cart", view)
                        .data("cross", cross)
                        .data("error", error)
                        .data("storeName", storeName)
                        .data("baseUrl", baseUrlOf(headers))
                        .data("navCats", navTree())
                        .data("theme", themeService.tokens())
                        .data("t", t(headers))
                        .data("langs", langs(headers))
                        .data("seo", seo(fmt(t(headers).seoCart(), storeName), null, true))
                        .render());
    }

    @GET
    @Path("pay/mock/{publicId}")
    public Response mockPayPage(
            @Context HttpHeaders headers, @PathParam("publicId") String publicId) {
        Order order = Order.byPublicId(publicId).orElse(null);
        if (order == null) {
            return Response.status(Response.Status.NOT_FOUND).entity("Order not found").build();
        }
        return page(
                payMock.data("o", order)
                        .data("storeName", storeName)
                        .data("baseUrl", baseUrlOf(headers))
                        .data("navCats", navTree())
                        .data("theme", themeService.tokens())
                        .data("t", t(headers))
                        .data("langs", langs(headers))
                        .data("seo", seo(t(headers).seoPay(), null, true))
                        .render());
    }

    @GET
    @Path("pay/paypal/{publicId}")
    public Response paypalReturn(
            @Context HttpHeaders headers,
            @PathParam("publicId") String publicId,
            @QueryParam("token") String paypalOrderId) {
        Order order = Order.byPublicId(publicId).orElse(null);
        if (order == null) {
            return Response.status(Response.Status.NOT_FOUND).entity("Order not found").build();
        }
        return page(
                payPaypal
                        .data("o", order)
                        .data("paypalOrderId", paypalOrderId == null ? "" : paypalOrderId)
                        .data("navCats", navTree())
                        .data("theme", themeService.tokens())
                        .data("t", t(headers))
                        .data("langs", langs(headers))
                        .data("seo", seo(t(headers).seoPaypal(), null, true))
                        .render());
    }

    /** PayPal 回跳页"完成支付": capture → markPaid 幂等路径 */
    @POST
    @Path("pay/paypal/{publicId}/capture")
    @Transactional
    public Response paypalCapture(
            @PathParam("publicId") String publicId,
            @FormParam("paypalOrderId") String paypalOrderId) {
        Order order = Order.byPublicId(publicId).orElse(null);
        if (order == null) {
            return Response.status(Response.Status.NOT_FOUND).entity("Order not found").build();
        }
        try {
            var provider =
                    (com.jade.cart.trade.payment.PaypalProvider)
                            orderService.provider("paypal").orElseThrow();
            var result = provider.capture(paypalOrderId);
            String status = result.path("status").asText();
            if ("COMPLETED".equals(status)) {
                var capture =
                        result.path("purchase_units")
                                .path(0)
                                .path("payments")
                                .path("captures")
                                .path(0);
                String captureId = capture.path("id").asText();
                long amountCents =
                        Math.round(capture.path("amount").path("value").asDouble(0) * 100);
                String currency = capture.path("amount").path("currency_code").asText("USD");
                orderService.markPaid(
                        "paypal", "cap_" + captureId, publicId, amountCents, currency, captureId);
            }
        } catch (Exception e) {
            Log.errorf(e, "paypal capture failed: order=%s", publicId);
        }
        return Response.seeOther(URI.create("/orders/" + publicId)).build();
    }

    /** Shopify 旧链接 301: mall_redirect 命中则永久跳转, 否则 404 */
    @GET
    @Path("{handle}")
    public Response legacyHandle(@Context HttpHeaders headers, @PathParam("handle") String handle) {
        if (handle.contains(".") || handle.isBlank()) {
            return notFoundPage(headers);
        }
        return com.jade.cart.importer.entity.Redirect.byHandle(handle)
                .map(
                        r ->
                                Response.status(Response.Status.MOVED_PERMANENTLY)
                                        .location(URI.create(r.target))
                                        .build())
                .orElseGet(() -> notFoundPage(headers));
    }

    @GET
    @Path("blog")
    public Response blogList(@Context HttpHeaders headers) {
        var posts = com.jade.cart.blog.entity.BlogPost.listActive();
        java.time.format.DateTimeFormatter fmt =
                java.time.format.DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.ENGLISH);
        List<Map<String, Object>> view = new ArrayList<>();
        for (var p : posts) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("slug", p.slug);
            m.put("title", p.title);
            m.put("excerpt", p.excerpt);
            m.put("coverPath", p.coverPath);
            var when = p.publishedAt != null ? p.publishedAt : p.updatedAt;
            m.put("date", when == null ? "" : when.format(fmt));
            view.add(m);
        }
        // SEO: 标题 + og:image (首篇封面) + 文章列表 ItemList 结构化数据
        String blogTitle = t(headers).navBlog();
        String blogImage =
                posts.isEmpty() || posts.get(0).coverPath == null
                        ? null
                        : baseUrlOf(headers) + "/media/" + posts.get(0).coverPath;
        String blogJsonLd = seoJsonLd.blogList(baseUrlOf(headers), posts);
        return page(
                blogTpl.data("posts", view)
                        .data("title", blogTitle)
                        .data("storeName", storeName)
                        .data("baseUrl", baseUrlOf(headers))
                        .data("navCats", navTree())
                        .data("theme", themeService.tokens())
                        .data("t", t(headers))
                        .data("langs", langs(headers))
                        .data("jsonLd", blogJsonLd)
                        .data(
                                "seo",
                                seo(
                                        fmt(t(headers).seoBlog(), storeName),
                                        blogImage,
                                        false,
                                        "website",
                                        "/blog"))
                        .render());
    }

    @GET
    @Path("blog/{slug}")
    public Response blogPostPage(@Context HttpHeaders headers, @PathParam("slug") String slug) {
        return com.jade.cart.blog.entity.BlogPost.activeBySlug(slug)
                .map(
                        post -> {
                            // 前后篇 (listActive 按 publishedAt desc: i-1 更新, i+1 更旧)
                            var all = com.jade.cart.blog.entity.BlogPost.listActive();
                            com.jade.cart.blog.entity.BlogPost prevPost = null;
                            com.jade.cart.blog.entity.BlogPost nextPost = null;
                            for (int i = 0; i < all.size(); i++) {
                                if (all.get(i).id.equals(post.id)) {
                                    if (i > 0) {
                                        prevPost = all.get(i - 1);
                                    }
                                    if (i < all.size() - 1) {
                                        nextPost = all.get(i + 1);
                                    }
                                    break;
                                }
                            }
                            String desc =
                                    post.seoDescription != null && !post.seoDescription.isBlank()
                                            ? post.seoDescription
                                            : post.excerpt != null && !post.excerpt.isBlank()
                                                    ? post.excerpt
                                                    : SeoExtensions.teaser(post.bodyMarkdown, 160);
                            String ogImage =
                                    post.coverPath == null
                                            ? null
                                            : baseUrlOf(headers) + "/media/" + post.coverPath;
                            var when = post.publishedAt != null ? post.publishedAt : post.updatedAt;
                            String dateStr =
                                    when == null
                                            ? ""
                                            : when.format(
                                                    java.time.format.DateTimeFormatter.ofPattern(
                                                            "MMMM d, yyyy", Locale.ENGLISH));
                            return page(
                                    blogPostTpl
                                            .data("post", post)
                                            .data("dateStr", dateStr)
                                            .data("prevPost", prevPost)
                                            .data("nextPost", nextPost)
                                            .data("storeName", storeName)
                                            .data("baseUrl", baseUrlOf(headers))
                                            .data("navCats", navTree())
                                            .data("theme", themeService.tokens())
                                            .data("t", t(headers))
                                            .data("langs", langs(headers))
                                            .data(
                                                    "seo",
                                                    seo(
                                                            desc,
                                                            ogImage,
                                                            false,
                                                            "article",
                                                            "/blog/" + post.slug,
                                                            post.seoKeywords))
                                            .data(
                                                    "jsonLd",
                                                    ld(
                                                            seoJsonLd.blogPosting(
                                                                    post,
                                                                    baseUrlOf(headers),
                                                                    storeName)))
                                            .render());
                        })
                .orElseGet(() -> notFoundPage(headers));
    }

    /** 404 页面 (带 h1/说明/返回入口, 不再是裸文本) */
    private Response notFoundPage(@Context HttpHeaders headers) {
        return Response.status(Response.Status.NOT_FOUND)
                .entity(
                        notFoundTpl
                                .data("storeName", storeName)
                                .data("baseUrl", baseUrlOf(headers))
                                .data("navCats", navTree())
                                .data("theme", themeService.tokens())
                                .data("t", t(headers))
                                .data("langs", langs(headers))
                                .data("seo", seo(null, null, true, "website", "/"))
                                .render())
                .build();
    }

    @GET
    @Path("pages/{slug}")
    public Response contentPage(@Context HttpHeaders headers, @PathParam("slug") String slug) {
        return com.jade.cart.content.entity.ContentPage.activeBySlug(slug)
                .map(
                        page ->
                                page(
                                        contentTpl
                                                .data("page", page)
                                                .data("storeName", storeName)
                                                .data("baseUrl", baseUrlOf(headers))
                                                .data("navCats", navTree())
                                                .data("theme", themeService.tokens())
                                                .data("t", t(headers))
                                                .data("langs", langs(headers))
                                                .data(
                                                        "seo",
                                                        seo(
                                                                SeoExtensions.teaser(
                                                                        page.bodyMarkdown, 160),
                                                                null,
                                                                false,
                                                                "website",
                                                                "/pages/" + page.slug))
                                                .data(
                                                        "jsonLd",
                                                        ld(
                                                                seoJsonLd.webPage(
                                                                        baseUrlOf(headers),
                                                                        page.title,
                                                                        SeoExtensions.teaser(
                                                                                page.bodyMarkdown,
                                                                                160),
                                                                        "/pages/" + page.slug)))
                                                .render()))
                .orElseGet(() -> notFoundPage(headers));
    }

    @GET
    @Path("contact")
    public Response contactPage(@Context HttpHeaders headers) {
        return page(
                contactTpl
                        .data("sent", false)
                        .data("error", (String) null)
                        .data("storeName", storeName)
                        .data("baseUrl", baseUrlOf(headers))
                        .data("navCats", navTree())
                        .data("theme", themeService.tokens())
                        .data("t", t(headers))
                        .data("langs", langs(headers))
                        .data(
                                "jsonLd",
                                ld(
                                        seoJsonLd.contactPage(
                                                baseUrlOf(headers),
                                                storeName,
                                                fmt(t(headers).seoContact(), storeName))))
                        .data(
                                "seo",
                                seo(
                                        fmt(t(headers).seoContact(), storeName),
                                        null,
                                        false,
                                        "website",
                                        "/contact"))
                        .render());
    }

    @POST
    @Path("contact")
    @Transactional
    public Response contactSubmit(
            @Context HttpHeaders headers,
            @FormParam("name") String name,
            @FormParam("email") String email,
            @FormParam("topic") String topic,
            @FormParam("message") String message,
            @FormParam("website") String website) {
        // 走与 API 相同的校验/限频 (直接复用服务层)
        try {
            if (email == null || !email.contains("@") || message == null || message.isBlank()) {
                return page(
                        contactTpl
                                .data("sent", false)
                                .data("error", t(headers).contactErrInvalid())
                                .data("storeName", storeName)
                                .data("baseUrl", baseUrlOf(headers))
                                .data("navCats", navTree())
                                .data("theme", themeService.tokens())
                                .data("t", t(headers))
                                .data("langs", langs(headers))
                                .data(
                                        "seo",
                                        seo(fmt(t(headers).seoContact(), storeName), null, true))
                                .render());
            }
            contactService.submit(name, email, topic == null ? "general" : topic, message, "web");
            return page(
                    contactTpl
                            .data("sent", true)
                            .data("storeName", storeName)
                            .data("baseUrl", baseUrlOf(headers))
                            .data("navCats", navTree())
                            .data("theme", themeService.tokens())
                            .data("t", t(headers))
                            .data("langs", langs(headers))
                            .data("seo", seo(fmt(t(headers).seoContact(), storeName), null, true))
                            .render());
        } catch (Exception e) {
            return page(
                    contactTpl
                            .data("sent", false)
                            .data("error", t(headers).contactErrFailed())
                            .data("storeName", storeName)
                            .data("baseUrl", baseUrlOf(headers))
                            .data("navCats", navTree())
                            .data("theme", themeService.tokens())
                            .data("t", t(headers))
                            .data("langs", langs(headers))
                            .data("seo", seo(fmt(t(headers).seoContact(), storeName), null, true))
                            .render());
        }
    }

    @GET
    @Path("orders/{publicId}")
    public Response orderPage(
            @Context HttpHeaders headers, @PathParam("publicId") String publicId) {
        return Order.byPublicId(publicId)
                .map(
                        o ->
                                page(
                                        orderTpl.data("o", orderService.toView(o))
                                                .data("storeName", storeName)
                                                .data("baseUrl", baseUrlOf(headers))
                                                .data("navCats", navTree())
                                                .data("theme", themeService.tokens())
                                                .data("t", t(headers))
                                                .data("langs", langs(headers))
                                                .data(
                                                        "seo",
                                                        seo(
                                                                fmt(
                                                                        t(headers).seoOrder(),
                                                                        o.orderNumber),
                                                                null,
                                                                true))
                                                .render()))
                .orElseGet(
                        () ->
                                Response.status(Response.Status.NOT_FOUND)
                                        .entity("Order not found")
                                        .build());
    }

    // ---------- 表单动作 (无 JS 购物流程) ----------

    @POST
    @Path("cart/add")
    @Transactional
    public Response addToCart(
            @Context HttpHeaders headers,
            @FormParam("variantId") Long variantId,
            @FormParam("quantity") Integer quantity,
            @Context UriInfo uriInfo) {
        TokenBox box = tokenBox(headers);
        try {
            cartService.addItem(box.token, variantId, quantity == null ? 1 : quantity);
        } catch (BizException e) {
            Log.warnf("add to cart rejected: %s", e.getMessage());
            return seeOther("/cart", box);
        }
        return seeOther("/cart", box);
    }

    @POST
    @Path("cart/update")
    @Transactional
    public Response updateCart(
            @Context HttpHeaders headers,
            @FormParam("variantId") Long variantId,
            @FormParam("quantity") Integer quantity) {
        TokenBox box = tokenBox(headers);
        cartService.updateItem(box.token, variantId, quantity == null ? 1 : quantity);
        return seeOther("/cart", box);
    }

    @POST
    @Path("cart/remove")
    @Transactional
    public Response removeFromCart(
            @Context HttpHeaders headers, @FormParam("variantId") Long variantId) {
        TokenBox box = tokenBox(headers);
        cartService.removeItem(box.token, variantId);
        return seeOther("/cart", box);
    }

    @POST
    @Path("checkout")
    @Transactional
    public Response checkout(
            @Context HttpHeaders headers,
            @FormParam("email") String email,
            @FormParam("shipName") String shipName,
            @FormParam("country") String country) {
        String token = CartService.readToken(headers).orElse(null);
        if (token == null) {
            return Response.seeOther(URI.create("/cart")).build();
        }
        try {
            Order placed = orderService.checkout(token, email, shipName, country);
            String payUrl = orderService.providerOf(placed).createPayment(placed).redirectUrl();
            return Response.seeOther(URI.create(payUrl)).build();
        } catch (BizException e) {
            Log.warnf("checkout rejected: %s", e.getMessage());
            return Response.seeOther(
                            URI.create(
                                    "/cart?error="
                                            + java.net.URLEncoder.encode(
                                                    e.getMessage(),
                                                    java.nio.charset.StandardCharsets.UTF_8)))
                    .build();
        }
    }

    /** Mock 收银台"确认支付": 模拟网关回调 markPaid 幂等路径 */
    @POST
    @Path("pay/mock/{publicId}/confirm")
    @Transactional
    public Response mockPayConfirm(@PathParam("publicId") String publicId) {
        Order order = Order.byPublicId(publicId).orElse(null);
        if (order == null) {
            return Response.status(Response.Status.NOT_FOUND).entity("Order not found").build();
        }
        String eventId =
                "evt_mock_"
                        + java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        orderService.markPaid(
                "mock", eventId, publicId, order.totalCents, order.currency, "mock_" + eventId);
        return Response.seeOther(URI.create("/orders/" + publicId)).build();
    }

    // ---------- 内部 ----------

    /** 当前请求语言文案集 (cookie store_lang / Accept-Language / 店铺默认) */
    private UiText t(HttpHeaders headers) {
        return messages.forRequest(headers);
    }

    /** 页脚语言切换器数据 */
    private List<Map<String, Object>> langs(HttpHeaders headers) {
        return messages.languages(messages.resolveCode(headers));
    }

    /** 翻译模板拼装 (UiText seo* 字段含 {0} 占位符; 模板值内禁用单引号) */
    private String fmt(String pattern, Object... args) {
        return MessageFormat.format(pattern, args);
    }

    /** 页面级 SEO 元数据 (desc/image/noindex/ogType), 经 seo=seo 传给布局标签 */
    /** 导航分类树 (父分类 + children, 供 Category mega 面板) */
    private List<Map<String, Object>> navTree() {
        List<Category> all =
                Category.listAllLive().stream()
                        .filter(c -> c.status == null || c.status == 1)
                        .sorted(
                                java.util.Comparator.comparing(
                                        c -> c.sortOrder == null ? 0 : c.sortOrder))
                        .toList();
        List<Map<String, Object>> roots = new ArrayList<>();
        for (Category c : all) {
            if (c.parentId != null) {
                continue;
            }
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("name", c.name);
            m.put("slug", c.slug);
            List<Map<String, Object>> children = new ArrayList<>();
            for (Category ch : all) {
                if (c.id.equals(ch.parentId)) {
                    Map<String, Object> cm = new LinkedHashMap<>();
                    cm.put("name", ch.name);
                    cm.put("slug", ch.slug);
                    children.add(cm);
                }
            }
            m.put("children", children);
            roots.add(m);
        }
        return roots;
    }

    private Map<String, Object> seo(String desc, String image, boolean noindex) {
        return seo(desc, image, noindex, "website", null, null);
    }

    private Map<String, Object> seo(
            String desc, String image, boolean noindex, String ogType, String canon) {
        return seo(desc, image, noindex, ogType, canon, null);
    }

    private Map<String, Object> seo(
            String desc,
            String image,
            boolean noindex,
            String ogType,
            String canon,
            String keywords) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("desc", desc == null ? "" : desc);
        m.put("image", (image == null || image.isBlank()) ? null : image);
        m.put("noindex", noindex);
        m.put("ogType", ogType);
        m.put("canon", canon);
        m.put("keywords", keywords == null ? "" : keywords);
        return m;
    }

    /** JSON-LD script 包裹 */
    private String ld(String json) {
        return "<script type=\"application/ld+json\">" + json + "</script>";
    }

    private record TokenBox(String token, boolean isNew) {}

    private TokenBox tokenBox(HttpHeaders headers) {
        return CartService.readToken(headers)
                .map(t -> new TokenBox(t, false))
                .orElseGet(() -> new TokenBox(CartService.newToken(), true));
    }

    /** 303 重定向 + (新会话时) 写 cart_token cookie */
    private Response seeOther(String location, TokenBox box) {
        Response.ResponseBuilder builder = Response.seeOther(URI.create(location));
        if (box.isNew()) {
            builder.cookie(
                    new NewCookie.Builder(CartService.CART_COOKIE)
                            .value(box.token())
                            .path("/")
                            .maxAge(60 * 60 * 24 * 30)
                            .httpOnly(true)
                            .build());
        }
        return builder.build();
    }
}
