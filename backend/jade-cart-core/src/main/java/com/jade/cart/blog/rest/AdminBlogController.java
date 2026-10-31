package com.jade.cart.blog.rest;

import com.jade.cart.blog.entity.BlogPost;
import com.jade.cart.blog.service.BlogService;
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
 * 博客管理 (管理端)
 */
@Path("/api/v1/blog-posts")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Authenticated
@Tag(name = "博客管理")
public class AdminBlogController {

    @Inject
    BlogService blogService;

    public record PostReq(String slug, String title, String excerpt, String bodyMarkdown,
                          String coverPath, String seoDescription, String status) {}

    @GET
    @RolesAllowed("admin")
    @Operation(summary = "文章列表")
    public R<List<BlogPost>> list() {
        return R.ok(blogService.posts());
    }

    @POST
    @RolesAllowed("admin")
    @Transactional
    @Operation(summary = "创建文章 (status=ACTIVE 直接发布)")
    public R<BlogPost> create(PostReq req) {
        return R.ok(blogService.create(req.slug(), req.title(), req.excerpt(), req.bodyMarkdown(),
                req.coverPath(), req.seoDescription(), BlogPost.STATUS_ACTIVE.equals(req.status())));
    }

    @PUT
    @Path("/{id}")
    @RolesAllowed("admin")
    @Transactional
    @Operation(summary = "更新文章")
    public R<BlogPost> update(@PathParam("id") Long id, PostReq req) {
        return R.ok(blogService.update(id, req.title(), req.excerpt(), req.bodyMarkdown(),
                req.coverPath(), req.seoDescription(), req.status()));
    }

    @DELETE
    @Path("/{id}")
    @RolesAllowed("admin")
    @Transactional
    @Operation(summary = "删除文章")
    public R<Void> delete(@PathParam("id") Long id) {
        blogService.delete(id);
        return R.ok();
    }
}
