package com.jade.cart.content.rest;

import com.jade.cart.content.entity.ContentPage;
import com.jade.cart.content.service.ContentService;
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
 * 内容页管理 (管理端)
 */
@Path("/api/v1/content-pages")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Authenticated
@Tag(name = "内容管理")
public class AdminContentController {

    @Inject
    ContentService contentService;

    public record PageReq(String slug, String title, String bodyMarkdown, String status) {}

    @GET
    @RolesAllowed("admin")
    @Operation(summary = "内容页列表")
    public R<List<ContentPage>> list() {
        return R.ok(contentService.pages());
    }

    @POST
    @RolesAllowed("admin")
    @Transactional
    @Operation(summary = "创建内容页")
    public R<ContentPage> create(PageReq req) {
        return R.ok(contentService.create(req.slug(), req.title(), req.bodyMarkdown()));
    }

    @PUT
    @Path("/{id}")
    @RolesAllowed("admin")
    @Transactional
    @Operation(summary = "更新内容页")
    public R<ContentPage> update(@PathParam("id") Long id, PageReq req) {
        return R.ok(contentService.update(id, req.title(), req.bodyMarkdown(), req.status()));
    }

    @DELETE
    @Path("/{id}")
    @RolesAllowed("admin")
    @Transactional
    @Operation(summary = "删除内容页")
    public R<Void> delete(@PathParam("id") Long id) {
        contentService.delete(id);
        return R.ok();
    }

    @POST
    @Path("/generate-policies")
    @RolesAllowed("admin")
    @Transactional
    @Operation(summary = "幂等生成 7 个页脚政策页 (按店铺资料)")
    public R<Integer> generatePolicies() {
        return R.ok(contentService.generatePolicies());
    }
}
