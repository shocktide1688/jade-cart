package com.jade.cart.integration.rest;

import com.jade.cart.integration.IntegrationService;
import com.jade.common.api.R;
import jakarta.annotation.security.PermitAll;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * ERP 集成端点 (机器对机器, Bearer API-Key 认证见 IntegrationAuthFilter)。
 *
 * <p>下行: POST /products/upsert (ERP 推商品 upsert, 按 SKU 幂等)。 上行: GET /orders 与 /products 增量 (ERP
 * 定时拉取, updated_after 游标 + 分页)。
 */
@Path("/api/v1/integration")
@PermitAll // 认证由 IntegrationAuthFilter 的 API-Key 负责 (不走用户 JWT)
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class IntegrationResource {

    @Inject IntegrationService integrationService;

    /** 订单增量: updated_after 为上次拉取的最大 updatedAt (ISO-8601), 首次不传 = 全量 */
    @GET
    @Path("/orders")
    public R<Map<String, Object>> orders(
            @QueryParam("updated_after") String updatedAfter,
            @QueryParam("page") @DefaultValue("1") int page,
            @QueryParam("size") @DefaultValue("100") int size) {
        return R.ok(
                integrationService.listOrdersAfter(
                        parseSince(updatedAfter), normPage(page), normSize(size)));
    }

    /** 商品增量 (变体粒度): 含 SKU/价格/库存, 供 ERP 对账 */
    @GET
    @Path("/products")
    public R<Map<String, Object>> products(
            @QueryParam("updated_after") String updatedAfter,
            @QueryParam("page") @DefaultValue("1") int page,
            @QueryParam("size") @DefaultValue("100") int size) {
        return R.ok(
                integrationService.listProductsAfter(
                        parseSince(updatedAfter), normPage(page), normSize(size)));
    }

    /** 商品推送 upsert (按 SKU 幂等); 非法值返回明确错误码由 ERP 重试/修正 */
    @POST
    @Path("/products/upsert")
    public R<Map<String, Object>> upsertProduct(ProductUpsertReq req) {
        return R.ok(
                integrationService.upsertProduct(
                        req.sku,
                        req.title,
                        req.description,
                        req.priceCents,
                        req.stock,
                        req.status,
                        req.categoryName,
                        req.imageUrls));
    }

    /** ERP 商品推送载荷 (JSON body; 字段缺失按需校验); imageUrls 首图为主图, null = 不动图片 */
    public static class ProductUpsertReq {
        public String sku;
        public String title;
        public String description;
        public Long priceCents;
        public Integer stock;
        public String status;
        public String categoryName;
        public java.util.List<String> imageUrls;
    }

    private LocalDateTime parseSince(String updatedAfter) {
        if (updatedAfter == null || updatedAfter.isBlank()) {
            return LocalDateTime.MIN;
        }
        return LocalDateTime.parse(updatedAfter, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }

    private int normPage(int page) {
        return Math.max(page, 1);
    }

    private int normSize(int size) {
        return Math.min(Math.max(size, 1), 200);
    }
}
