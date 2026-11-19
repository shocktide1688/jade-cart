package com.jade.cart.catalog.rest;

import com.jade.cart.catalog.dto.CategoryNode;
import com.jade.cart.catalog.dto.ProductView;
import com.jade.cart.catalog.dto.StoreProductDetailView;
import com.jade.cart.catalog.entity.Category;
import com.jade.cart.catalog.entity.Product;
import com.jade.cart.catalog.service.CatalogService;
import com.jade.cart.catalog.service.ProductImageService;
import com.jade.common.api.R;
import jakarta.annotation.security.PermitAll;
import jakarta.inject.Inject;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import java.util.ArrayList;
import java.util.List;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

/**
 * 店面商品目录 (公开, 只读): 供移动端 App 与其他 JSON 消费方使用
 *
 * <p>与 SSR 店面共用 CatalogService 数据源, 返回 R 信封 JSON; 列表上限与 SSR PLP (60) 对齐。
 */
@Path("/api/store")
@Produces(MediaType.APPLICATION_JSON)
@PermitAll
@Tag(name = "店面-商品")
public class StoreCatalogController {

    /** 店面列表上限 (与 SSR PLP limit 60 对齐) */
    static final int MAX_LIMIT = 60;

    @Inject CatalogService catalogService;

    @Inject ProductImageService productImageService;

    @GET
    @Path("/products")
    @Operation(summary = "店面商品列表: 关键词/分类 slug/精选过滤")
    public R<List<ProductView.Summary>> products(
            @QueryParam("q") String q,
            @QueryParam("category") String categorySlug,
            @QueryParam("featured") Boolean featured,
            @QueryParam("limit") @DefaultValue("60") int limit) {
        List<Product> items;
        if (Boolean.TRUE.equals(featured)) {
            items = catalogService.featuredProducts(clamp(limit));
        } else {
            Long categoryId = null;
            if (categorySlug != null && !categorySlug.isBlank()) {
                Category category = Category.findBySlug(categorySlug);
                if (category == null) {
                    // 未知分类按空结果 (客户端从 /categories 取合法 slug, 不当错误处理)
                    return R.ok(List.of());
                }
                categoryId = category.id;
            }
            items = catalogService.activeProducts(q, categoryId, clamp(limit));
        }
        return R.ok(items.stream().map(catalogService::toSummary).toList());
    }

    @GET
    @Path("/products/{slug}")
    @Operation(summary = "店面商品详情: 变体/库存/图集 (主图在前)")
    public R<StoreProductDetailView> product(@PathParam("slug") String slug) {
        Product product = Product.findBySlug(slug);
        if (product == null || !product.isActive()) {
            return R.fail(404, "商品不存在");
        }
        List<String> images = new ArrayList<>();
        if (product.mainImagePath != null && !product.mainImagePath.isBlank()) {
            images.add(product.mainImagePath);
        }
        productImageService.listImages(product.id).forEach(image -> images.add(image.path));
        // findById 泛型推断: 循环内赋值目标类型明确 (流式 lambda 会退化成 Object)
        List<String> categorySlugs = new ArrayList<>();
        for (Long categoryId : catalogService.categoryIdsOf(product.id)) {
            Category category = Category.findById(categoryId);
            if (category != null && !category.deleted) {
                categorySlugs.add(category.slug);
            }
        }
        return R.ok(
                new StoreProductDetailView(catalogService.toView(product), images, categorySlugs));
    }

    @GET
    @Path("/categories")
    @Operation(summary = "店面分类树 (启用分类, 父子两级)")
    public R<List<CategoryNode>> categories() {
        List<Category> all = Category.listActive();
        List<CategoryNode> roots = new ArrayList<>();
        for (Category category : all) {
            if (category.parentId != null) {
                continue;
            }
            List<CategoryNode> children = new ArrayList<>();
            for (Category child : all) {
                if (category.id.equals(child.parentId)) {
                    children.add(new CategoryNode(child.slug, child.name, List.of()));
                }
            }
            roots.add(new CategoryNode(category.slug, category.name, children));
        }
        return R.ok(roots);
    }

    private int clamp(int limit) {
        if (limit <= 0) {
            return MAX_LIMIT;
        }
        return Math.min(limit, MAX_LIMIT);
    }
}
