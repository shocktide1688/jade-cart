package com.jade.cart.app.storefront;

import com.jade.cart.catalog.dto.ProductView;
import com.jade.cart.catalog.entity.Category;
import com.jade.cart.catalog.entity.Product;
import com.jade.cart.catalog.service.CatalogService;
import com.jade.cart.trade.dto.CartView;
import com.jade.cart.trade.entity.Order;
import com.jade.cart.trade.service.CartService;
import com.jade.cart.trade.service.OrderService;
import com.jade.common.exception.BizException;
import io.quarkus.logging.Log;
import io.quarkus.qute.Location;
import io.quarkus.qute.Template;
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
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 店面 SSR 页面 (Qute, 英文, 无 JS 可用 — kartwo 理念):
 * 首页 / 商品列表 / 详情 / 购物车 / Mock 收银台 / 订单状态
 */
@Path("/")
@Produces(MediaType.TEXT_HTML)
@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
public class StorefrontPages {

    @Inject
    @ConfigProperty(name = "jade.cart.store-name", defaultValue = "Jade Cart Store")
    String storeName;

    @Inject
    @ConfigProperty(name = "jade.cart.store-base-url", defaultValue = "http://localhost:8090")
    String baseUrl;

    @Inject
    CatalogService catalogService;

    @Inject
    CartService cartService;

    @Inject
    OrderService orderService;

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
    @Location("store/contact")
    Template contactTpl;

    @Inject
    com.jade.cart.contact.service.ContactService contactService;

    @Inject
    com.jade.cart.blog.service.BlogService blogService;

    @Inject
    SeoJsonLd seoJsonLd;

    @Inject
    com.jade.cart.settings.service.ThemeService themeService;

    @Inject
    StorefrontService storefrontService;

    @Inject
    @Location("store/blog")
    Template blogTpl;

    @Inject
    @Location("store/blog-post")
    Template blogPostTpl;

    @Inject
    @Location("store/order")
    Template orderTpl;

    // ---------- 页面 ----------

    @GET
    public Response index() {
        List<ProductView.Summary> featured = catalogService.featuredProducts(8).stream()
                .map(catalogService::toSummary).toList();
        List<ProductView.Summary> latest = catalogService.activeProducts(null, null, 12).stream()
                .map(catalogService::toSummary).toList();
        String homeDesc = com.jade.cart.settings.entity.StoreSetting.getValue("announcement")
                .filter(v -> !v.isBlank())
                .orElseGet(() -> "Shop " + storeName
                        + " — curated products, fast worldwide shipping and easy returns.");
        return Response.ok(home
                .data("sections", storefrontService.renderHome())
                .data("featured", featured)
                .data("latest", latest)
                .data("storeName", storeName).data("baseUrl", baseUrl)
                .data("theme", themeService.tokens()).data("seo", seo(homeDesc, null, false, "website", "/"))
                .data("jsonLd", ld(seoJsonLd.website(baseUrl, storeName)))
                .render()).build();
    }

    @GET
    @Path("products")
    public Response productList(@QueryParam("q") String q, @QueryParam("category") String categorySlug) {
        Long categoryId = null;
        String categoryName = null;
        if (categorySlug != null && !categorySlug.isBlank()) {
            Category category = Category.findBySlug(categorySlug);
            if (category != null) {
                categoryId = category.id;
                categoryName = category.name;
            }
        }
        List<ProductView.Summary> items = catalogService.activeProducts(q, categoryId, 60).stream()
                .map(catalogService::toSummary).toList();
        String desc = categoryName != null
                ? "Browse " + categoryName + " products at " + storeName + ". Worldwide shipping."
                : (q != null && !q.isBlank()
                        ? "Search results for \u201C" + q + "\u201D at " + storeName + "."
                        : "All products at " + storeName + ". Curated quality, fast shipping.");
        return Response.ok(products
                .data("items", items)
                .data("q", q == null ? "" : q)
                .data("categoryName", categoryName)
                .data("categories", Category.listActive())
                .data("storeName", storeName).data("baseUrl", baseUrl)
                .data("theme", themeService.tokens()).data("seo", seo(desc, null, false, "website", "/products"))
                .data("jsonLd", ld(seoJsonLd.itemList(baseUrl,
                        catalogService.activeProducts(q, categoryId, 20))))
                .render()).build();
    }

    @GET
    @Path("products/{slug}")
    public Response productDetail(@PathParam("slug") String slug) {
        Product entity = Product.findBySlug(slug);
        if (entity == null || !entity.isActive()) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("Product not found").build();
        }
        ProductView view = catalogService.toView(entity);
        List<ProductView.Summary> related = catalogService.activeProducts(null, null, 4).stream()
                .filter(pr -> !pr.id.equals(entity.id))
                .map(catalogService::toSummary).limit(3).toList();
        String desc = SeoExtensions.teaser(
                entity.seoDescription != null && !entity.seoDescription.isBlank()
                        ? entity.seoDescription : entity.description, 160);
        String ogImage = entity.mainImagePath == null ? null : baseUrl + "/media/" + entity.mainImagePath;
        String jsonLd = ld(seoJsonLd.product(entity, baseUrl)) + "\n"
                + ld(seoJsonLd.breadcrumb(baseUrl,
                        "Home", "/",
                        "Products", "/products",
                        entity.title, "/products/" + entity.slug));
        return Response.ok(product
                .data("p", view)
                .data("related", related)
                .data("storeName", storeName).data("baseUrl", baseUrl)
                .data("theme", themeService.tokens()).data("seo", seo(desc, ogImage, false, "product", "/products/" + entity.slug, entity.seoKeywords))
                .data("jsonLd", jsonLd)
                .render()).build();
    }

    @GET
    @Path("cart")
    public Response cartPage(@Context HttpHeaders headers,
                             @QueryParam("error") String error) {
        CartView view = Optional.ofNullable(CartService.readToken(headers).orElse(null))
                .map(token -> cartService.view(cartService.getOrCreate(token)))
                .orElseGet(() -> new CartView(null, "USD", List.of(), 0L));
        return Response.ok(cart
                .data("cart", view)
                .data("error", error)
                .data("storeName", storeName).data("baseUrl", baseUrl)
                .data("theme", themeService.tokens()).data("seo", seo("Your shopping cart at " + storeName + ".", null, true))
                .render()).build();
    }

    @GET
    @Path("pay/mock/{publicId}")
    public Response mockPayPage(@PathParam("publicId") String publicId) {
        Order order = Order.byPublicId(publicId).orElse(null);
        if (order == null) {
            return Response.status(Response.Status.NOT_FOUND).entity("Order not found").build();
        }
        return Response.ok(payMock.data("o", order).data("storeName", storeName).data("baseUrl", baseUrl)
                .data("theme", themeService.tokens()).data("seo", seo("Complete your payment.", null, true))
                .render()).build();
    }

    @GET
    @Path("pay/paypal/{publicId}")
    public Response paypalReturn(@PathParam("publicId") String publicId,
                                 @QueryParam("token") String paypalOrderId) {
        Order order = Order.byPublicId(publicId).orElse(null);
        if (order == null) {
            return Response.status(Response.Status.NOT_FOUND).entity("Order not found").build();
        }
        return Response.ok(payPaypal
                .data("o", order)
                .data("paypalOrderId", paypalOrderId == null ? "" : paypalOrderId)
                .data("theme", themeService.tokens()).data("seo", seo("Complete your PayPal payment.", null, true))
                .render()).build();
    }

    /** PayPal 回跳页"完成支付": capture → markPaid 幂等路径 */
    @POST
    @Path("pay/paypal/{publicId}/capture")
    @Transactional
    public Response paypalCapture(@PathParam("publicId") String publicId,
                                  @FormParam("paypalOrderId") String paypalOrderId) {
        Order order = Order.byPublicId(publicId).orElse(null);
        if (order == null) {
            return Response.status(Response.Status.NOT_FOUND).entity("Order not found").build();
        }
        try {
            var provider = (com.jade.cart.trade.payment.PaypalProvider)
                    orderService.provider("paypal").orElseThrow();
            var result = provider.capture(paypalOrderId);
            String status = result.path("status").asText();
            if ("COMPLETED".equals(status)) {
                var capture = result.path("purchase_units").path(0).path("payments").path("captures").path(0);
                String captureId = capture.path("id").asText();
                long amountCents = Math.round(capture.path("amount").path("value").asDouble(0) * 100);
                String currency = capture.path("amount").path("currency_code").asText("USD");
                orderService.markPaid("paypal", "cap_" + captureId, publicId, amountCents, currency, captureId);
            }
        } catch (Exception e) {
            Log.errorf(e, "paypal capture failed: order=%s", publicId);
        }
        return Response.seeOther(java.net.URI.create("/orders/" + publicId)).build();
    }

    /** Shopify 旧链接 301: mall_redirect 命中则永久跳转, 否则 404 */
    @GET
    @Path("{handle}")
    public Response legacyHandle(@PathParam("handle") String handle) {
        if (handle.contains(".") || handle.isBlank()) {
            return Response.status(Response.Status.NOT_FOUND).entity("Not found").build();
        }
        return com.jade.cart.importer.entity.Redirect.byHandle(handle)
                .map(r -> Response.status(Response.Status.MOVED_PERMANENTLY)
                        .location(URI.create(r.target)).build())
                .orElseGet(() -> Response.status(Response.Status.NOT_FOUND)
                        .entity("Not found").build());
    }

    @GET
    @Path("blog")
    public Response blogList() {
        var posts = com.jade.cart.blog.entity.BlogPost.listActive();
        return Response.ok(blogTpl
                .data("posts", posts)
                .data("storeName", storeName).data("baseUrl", baseUrl)
                .data("theme", themeService.tokens()).data("seo", seo("Blog — news, guides and stories from " + storeName + ".", null, false, "website", "/blog"))
                .render()).build();
    }

    @GET
    @Path("blog/{slug}")
    public Response blogPostPage(@PathParam("slug") String slug) {
        return com.jade.cart.blog.entity.BlogPost.activeBySlug(slug)
                .map(post -> {
                    String desc = post.seoDescription != null && !post.seoDescription.isBlank()
                            ? post.seoDescription
                            : (post.excerpt != null && !post.excerpt.isBlank()
                                    ? post.excerpt : SeoExtensions.teaser(post.bodyMarkdown, 160));
                    String ogImage = post.coverPath == null ? null : baseUrl + "/media/" + post.coverPath;
                    return Response.ok(blogPostTpl
                            .data("post", post)
                            .data("storeName", storeName).data("baseUrl", baseUrl)
                            .data("theme", themeService.tokens()).data("seo", seo(desc, ogImage, false, "article", "/blog/" + post.slug, post.seoKeywords))
                            .data("jsonLd", ld(seoJsonLd.blogPosting(post, baseUrl, storeName)))
                            .render()).build();
                })
                .orElseGet(() -> Response.status(Response.Status.NOT_FOUND)
                        .entity("Post not found").build());
    }

    @GET
    @Path("pages/{slug}")
    public Response contentPage(@PathParam("slug") String slug) {
        return com.jade.cart.content.entity.ContentPage.activeBySlug(slug)
                .map(page -> Response.ok(contentTpl
                        .data("page", page)
                        .data("storeName", storeName).data("baseUrl", baseUrl)
                        .data("theme", themeService.tokens()).data("seo", seo(SeoExtensions.teaser(page.bodyMarkdown, 160), null, false, "website", "/pages/" + page.slug))
                        .render()).build())
                .orElseGet(() -> Response.status(Response.Status.NOT_FOUND)
                        .entity("Page not found").build());
    }

    @GET
    @Path("contact")
    public Response contactPage() {
        return Response.ok(contactTpl
                .data("sent", false)
                .data("error", (String) null)
                .data("storeName", storeName).data("baseUrl", baseUrl)
                .data("theme", themeService.tokens()).data("seo", seo("Contact " + storeName + " — we reply within one business day.", null, false, "website", "/contact"))
                .render()).build();
    }

    @POST
    @Path("contact")
    @Transactional
    public Response contactSubmit(@FormParam("name") String name,
                                  @FormParam("email") String email,
                                  @FormParam("topic") String topic,
                                  @FormParam("message") String message,
                                  @FormParam("website") String website) {
        var req = new com.jade.cart.contact.rest.StoreContactController.ContactReq(
                name, email, topic, message, website);
        // 走与 API 相同的校验/限频 (直接复用服务层)
        try {
            if (email == null || !email.contains("@") || message == null || message.isBlank()) {
                return Response.ok(contactTpl.data("sent", false)
                        .data("error", "Please provide a valid email and message.")
                        .data("storeName", storeName).data("baseUrl", baseUrl)
                        .data("theme", themeService.tokens()).data("seo", seo("Contact " + storeName + ".", null, true))
                        .render()).build();
            }
            contactService.submit(name, email, topic == null ? "general" : topic, message, "web");
            return Response.ok(contactTpl.data("sent", true)
                    .data("storeName", storeName).data("baseUrl", baseUrl)
                    .data("theme", themeService.tokens()).data("seo", seo("Message sent — thank you!", null, true))
                    .render()).build();
        } catch (Exception e) {
            return Response.ok(contactTpl.data("sent", false)
                    .data("error", "Failed to send, please try later.")
                    .data("storeName", storeName).data("baseUrl", baseUrl)
                    .data("theme", themeService.tokens()).data("seo", seo("Contact " + storeName + ".", null, true))
                    .render()).build();
        }
    }

    @GET
    @Path("orders/{publicId}")
    public Response orderPage(@PathParam("publicId") String publicId) {
        return Order.byPublicId(publicId)
                .map(o -> Response.ok(orderTpl.data("o", orderService.toView(o)).data("storeName", storeName).data("baseUrl", baseUrl)
                        .data("theme", themeService.tokens()).data("seo", seo("Order " + o.orderNumber + " status.", null, true))
                        .render()).build())
                .orElseGet(() -> Response.status(Response.Status.NOT_FOUND)
                        .entity("Order not found").build());
    }

    // ---------- 表单动作 (无 JS 购物流程) ----------

    @POST
    @Path("cart/add")
    @Transactional
    public Response addToCart(@Context HttpHeaders headers,
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
    public Response updateCart(@Context HttpHeaders headers,
                               @FormParam("variantId") Long variantId,
                               @FormParam("quantity") Integer quantity) {
        TokenBox box = tokenBox(headers);
        cartService.updateItem(box.token, variantId, quantity == null ? 1 : quantity);
        return seeOther("/cart", box);
    }

    @POST
    @Path("cart/remove")
    @Transactional
    public Response removeFromCart(@Context HttpHeaders headers,
                                   @FormParam("variantId") Long variantId) {
        TokenBox box = tokenBox(headers);
        cartService.removeItem(box.token, variantId);
        return seeOther("/cart", box);
    }

    @POST
    @Path("checkout")
    @Transactional
    public Response checkout(@Context HttpHeaders headers,
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
            return Response.seeOther(URI.create("/cart?error=" +
                    java.net.URLEncoder.encode(e.getMessage(), java.nio.charset.StandardCharsets.UTF_8)))
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
        String eventId = "evt_mock_" + java.util.UUID.randomUUID().toString()
                .replace("-", "").substring(0, 16);
        orderService.markPaid("mock", eventId, publicId, order.totalCents, order.currency,
                "mock_" + eventId);
        return Response.seeOther(URI.create("/orders/" + publicId)).build();
    }

    // ---------- 内部 ----------

    /** 页面级 SEO 元数据 (desc/image/noindex/ogType), 经 seo=seo 传给布局标签 */
    private Map<String, Object> seo(String desc, String image, boolean noindex) {
        return seo(desc, image, noindex, "website", null, null);
    }

    private Map<String, Object> seo(String desc, String image, boolean noindex, String ogType) {
        return seo(desc, image, noindex, ogType, null, null);
    }

    private Map<String, Object> seo(String desc, String image, boolean noindex, String ogType, String canon) {
        return seo(desc, image, noindex, ogType, canon, null);
    }

    private Map<String, Object> seo(String desc, String image, boolean noindex, String ogType, String canon, String keywords) {
        Map<String, Object> m = new java.util.LinkedHashMap<>();
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

    private String teaser160(String s) {
        return SeoExtensions.teaser(s, 160);
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
            builder.cookie(new NewCookie.Builder(CartService.CART_COOKIE)
                    .value(box.token()).path("/").maxAge(60 * 60 * 24 * 30).httpOnly(true).build());
        }
        return builder.build();
    }
}
