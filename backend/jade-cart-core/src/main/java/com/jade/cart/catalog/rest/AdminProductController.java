package com.jade.cart.catalog.rest;

import com.jade.cart.catalog.dto.ProductView;
import com.jade.cart.catalog.service.CatalogService;
import com.jade.cart.catalog.service.ProductImageService;
import com.jade.common.api.PageResult;
import com.jade.common.api.R;
import io.quarkus.security.Authenticated;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import java.util.List;
import java.util.Map;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

/** 商品管理 (管理端) */
@Path("/api/v1/products")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Authenticated
@Tag(name = "商品管理")
public class AdminProductController {

    @Inject CatalogService catalogService;

    @Inject ProductImageService productImageService;

    public record CreateReq(
            String title,
            String slug,
            String description,
            String seoDescription,
            String status,
            Boolean featured,
            Long defaultPriceCents,
            String defaultSku,
            String mainImagePath,
            String seoKeywords) {}

    public record UpdateReq(
            String title,
            String slug,
            String description,
            String seoDescription,
            String status,
            Boolean featured,
            String mainImagePath,
            String seoKeywords) {}

    public record OptionReq(String optionKey, String displayName, List<String> values) {}

    public record VariantGenerateReq(Long defaultPriceCents) {}

    public record VariantCreateReq(String sku, Long priceCents) {}

    public record VariantUpdateReq(String sku, Long priceCents) {}

    public record InventoryReq(Integer quantity) {}

    @GET
    @RolesAllowed("admin")
    @Operation(summary = "商品分页")
    public R<PageResult<ProductView.Summary>> list(
            @QueryParam("page") Integer page,
            @QueryParam("size") Integer size,
            @QueryParam("keyword") String keyword,
            @QueryParam("status") String status) {
        int p = page == null ? 1 : page;
        int s = size == null ? 10 : size;
        List<ProductView.Summary> records =
                catalogService.pageProducts(keyword, status, p, s).stream()
                        .map(catalogService::toSummary)
                        .toList();
        return R.ok(PageResult.of(records, catalogService.countProducts(keyword, status), p, s));
    }

    @POST
    @RolesAllowed("admin")
    @Transactional
    @Operation(summary = "创建商品 (自动带默认变体)")
    public R<ProductView> create(CreateReq req) {
        if (req.title() == null || req.title().isBlank()) {
            return R.fail(400, "商品标题不能为空");
        }
        return R.ok(
                catalogService.toView(
                        catalogService.createProduct(
                                req.title(),
                                req.slug(),
                                req.description(),
                                req.seoDescription(),
                                req.status(),
                                req.featured(),
                                req.defaultPriceCents(),
                                req.defaultSku(),
                                req.mainImagePath(),
                                req.seoKeywords())));
    }

    @GET
    @Path("/{id}")
    @RolesAllowed("admin")
    @Operation(summary = "商品详情 (含变体轴/变体/库存)")
    public R<ProductView> get(@PathParam("id") Long id) {
        com.jade.cart.catalog.entity.Product product =
                com.jade.cart.catalog.entity.Product.findById(id);
        if (product == null || product.deleted) {
            return R.fail(404, "商品不存在");
        }
        return R.ok(catalogService.toView(product));
    }

    @PUT
    @Path("/{id}")
    @RolesAllowed("admin")
    @Transactional
    @Operation(summary = "更新商品")
    public R<ProductView> update(@PathParam("id") Long id, UpdateReq req) {
        return R.ok(
                catalogService.toView(
                        catalogService.updateProduct(
                                id,
                                req.title(),
                                req.slug(),
                                req.description(),
                                req.seoDescription(),
                                req.status(),
                                req.featured(),
                                req.mainImagePath(),
                                req.seoKeywords())));
    }

    @DELETE
    @Path("/{id}")
    @RolesAllowed("admin")
    @Transactional
    @Operation(summary = "归档商品 (软删除)")
    public R<Void> archive(@PathParam("id") Long id) {
        catalogService.archiveProduct(id);
        return R.ok();
    }

    @PUT
    @Path("/{id}/categories")
    @RolesAllowed("admin")
    @Transactional
    @Operation(summary = "设置商品所属分类 (覆盖式)")
    public R<Void> linkCategories(
            @PathParam("id") Long id, com.jade.cart.catalog.rest.req.CategoryLinkReq req) {
        catalogService.linkCategories(id, req.categoryIds());
        return R.ok();
    }

    // ---------- 商品图集 (相册) ----------

    @GET
    @Path("/{id}/images")
    @RolesAllowed("admin")
    @Operation(summary = "商品图集列表 (主图 + 补充图, 按展示顺序)")
    public R<List<Map<String, Object>>> images(@PathParam("id") Long id) {
        return R.ok(productImageService.gallery(id));
    }

    @POST
    @Path("/{id}/images")
    @RolesAllowed("admin")
    @Transactional
    @Operation(summary = "追加图集图片 (path 为媒体库相对路径)")
    public R<Void> addImage(@PathParam("id") Long id, ImageReq req) {
        productImageService.add(id, req.path());
        return R.ok();
    }

    @DELETE
    @Path("/{id}/images/{imageId}")
    @RolesAllowed("admin")
    @Transactional
    @Operation(summary = "删除图集图片")
    public R<Void> deleteImage(@PathParam("id") Long id, @PathParam("imageId") Long imageId) {
        productImageService.remove(id, imageId);
        return R.ok();
    }

    @PUT
    @Path("/{id}/images/order")
    @RolesAllowed("admin")
    @Transactional
    @Operation(summary = "调整图集顺序 (imageIds 为目标顺序)")
    public R<Void> reorderImages(@PathParam("id") Long id, OrderReq req) {
        productImageService.reorder(id, req.imageIds());
        return R.ok();
    }

    @PUT
    @Path("/{id}/images/{imageId}/main")
    @RolesAllowed("admin")
    @Transactional
    @Operation(summary = "把图集图片设为主图 (原主图进图集首位)")
    public R<Void> promoteImage(@PathParam("id") Long id, @PathParam("imageId") Long imageId) {
        productImageService.promoteToMain(id, imageId);
        return R.ok();
    }

    public record ImageReq(String path) {}

    public record OrderReq(List<Long> imageIds) {}

    // ---------- 变体轴 ----------

    @POST
    @Path("/{id}/options")
    @RolesAllowed("admin")
    @Transactional
    @Operation(summary = "添加变体轴 (如 color: [Red, Blue])")
    public R<Void> addOption(@PathParam("id") Long id, OptionReq req) {
        catalogService.addOption(id, req.optionKey(), req.displayName(), req.values());
        return R.ok();
    }

    @DELETE
    @Path("/{id}/options/{optionId}")
    @RolesAllowed("admin")
    @Transactional
    @Operation(summary = "删除变体轴")
    public R<Void> deleteOption(@PathParam("id") Long id, @PathParam("optionId") Long optionId) {
        catalogService.deleteOption(optionId);
        return R.ok();
    }

    @POST
    @Path("/{id}/options/{optionId}/values")
    @RolesAllowed("admin")
    @Transactional
    @Operation(summary = "追加轴取值")
    public R<Void> addOptionValue(
            @PathParam("id") Long id, @PathParam("optionId") Long optionId, OptionReq req) {
        if (req.values() == null || req.values().isEmpty()) {
            return R.fail(400, "取值不能为空");
        }
        req.values().forEach(v -> catalogService.addOptionValue(optionId, v));
        return R.ok();
    }

    // ---------- 变体 ----------

    @POST
    @Path("/{id}/variants/generate")
    @RolesAllowed("admin")
    @Transactional
    @Operation(summary = "按轴取值笛卡尔积生成缺失变体组合")
    public R<Integer> generateVariants(@PathParam("id") Long id, VariantGenerateReq req) {
        return R.ok(catalogService.generateVariants(id, req.defaultPriceCents()).size());
    }

    @POST
    @Path("/{id}/variants")
    @RolesAllowed("admin")
    @Transactional
    @Operation(summary = "手工添加变体")
    public R<Void> addVariant(@PathParam("id") Long id, VariantCreateReq req) {
        catalogService.addVariant(id, req.sku(), req.priceCents());
        return R.ok();
    }

    @PUT
    @Path("/variants/{variantId}")
    @RolesAllowed("admin")
    @Transactional
    @Operation(summary = "更新变体 (SKU/价格)")
    public R<ProductView.VariantView> updateVariant(
            @PathParam("variantId") Long variantId, VariantUpdateReq req) {
        return R.ok(
                catalogService.toVariantView(
                        catalogService.updateVariant(variantId, req.sku(), req.priceCents())));
    }

    @DELETE
    @Path("/variants/{variantId}")
    @RolesAllowed("admin")
    @Transactional
    @Operation(summary = "删除变体 (软删除)")
    public R<Void> deleteVariant(@PathParam("variantId") Long variantId) {
        catalogService.deleteVariant(variantId);
        return R.ok();
    }

    // ---------- 库存 ----------

    @PUT
    @Path("/variants/{variantId}/inventory")
    @RolesAllowed("admin")
    @Transactional
    @Operation(summary = "设置在库数量 (reserved 不受影响)")
    public R<ProductView.VariantView> setInventory(
            @PathParam("variantId") Long variantId, InventoryReq req) {
        if (req.quantity() == null) {
            return R.fail(400, "数量不能为空");
        }
        catalogService.setInventory(variantId, req.quantity());
        com.jade.cart.catalog.entity.Variant variant =
                com.jade.cart.catalog.entity.Variant.findById(variantId);
        return R.ok(catalogService.toVariantView(variant));
    }
}
