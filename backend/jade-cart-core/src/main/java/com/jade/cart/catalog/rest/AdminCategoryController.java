package com.jade.cart.catalog.rest;

import com.jade.cart.catalog.entity.Category;
import com.jade.cart.catalog.service.CatalogService;
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
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.List;

/**
 * 分类管理 (管理端)
 */
@Path("/api/v1/categories")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Authenticated
@Tag(name = "分类管理")
public class AdminCategoryController {

    @Inject
    CatalogService catalogService;

    public record CategoryReq(String name, String slug, Long parentId, Integer sortOrder, Integer status) {}

    @GET
    @RolesAllowed("admin")
    @Operation(summary = "分类列表 (平铺, 前端组树)")
    public R<List<Category>> list() {
        return R.ok(catalogService.categories());
    }

    @POST
    @RolesAllowed("admin")
    @Transactional
    @Operation(summary = "创建分类")
    public R<Category> create(CategoryReq req) {
        if (req.name() == null || req.name().isBlank()) {
            return R.fail(400, "分类名称不能为空");
        }
        return R.ok(catalogService.createCategory(
                req.name(), req.slug(), req.parentId(), req.sortOrder()));
    }

    @PUT
    @Path("/{id}")
    @RolesAllowed("admin")
    @Transactional
    @Operation(summary = "更新分类")
    public R<Category> update(@PathParam("id") Long id, CategoryReq req) {
        return R.ok(catalogService.updateCategory(
                id, req.name(), req.slug(), req.parentId(), req.sortOrder(), req.status()));
    }

    @DELETE
    @Path("/{id}")
    @RolesAllowed("admin")
    @Transactional
    @Operation(summary = "删除分类 (软删除)")
    public R<Void> delete(@PathParam("id") Long id) {
        catalogService.deleteCategory(id);
        return R.ok();
    }
}
