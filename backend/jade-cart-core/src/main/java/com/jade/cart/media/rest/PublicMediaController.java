package com.jade.cart.media.rest;

import com.jade.cart.media.service.MediaService;
import jakarta.annotation.security.PermitAll;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Response;

import java.nio.file.Files;

/**
 * 媒体公开访问: GET /media/{...相对路径} (内容哈希命名, 长缓存)
 */
@Path("/media")
@PermitAll
public class PublicMediaController {

    @Inject
    MediaService mediaService;

    @GET
    @Path("/{resource:.*}")
    public Response get(@PathParam("resource") String resource) {
        java.nio.file.Path file = mediaService.resolve(resource);
        if (file == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        String name = file.getFileName().toString().toLowerCase();
        String type = name.endsWith(".png") ? "image/png"
                : name.endsWith(".webp") ? "image/webp" : "image/jpeg";
        try {
            return Response.ok(Files.readAllBytes(file))
                    .type(type)
                    .header("Cache-Control", "public, max-age=31536000, immutable")
                    .build();
        } catch (Exception e) {
            return Response.serverError().build();
        }
    }
}
