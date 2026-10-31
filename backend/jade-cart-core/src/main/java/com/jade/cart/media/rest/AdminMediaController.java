package com.jade.cart.media.rest;

import com.jade.cart.media.entity.MediaAsset;
import com.jade.cart.media.service.MediaService;
import com.jade.common.api.R;
import io.quarkus.security.Authenticated;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.multipart.FileUpload;

import java.io.InputStream;
import java.nio.file.Files;

/**
 * 媒体上传 (管理端): 校验/去重/派生图
 */
@Path("/api/v1/media")
@Produces(MediaType.APPLICATION_JSON)
@Authenticated
@Tag(name = "媒体库")
public class AdminMediaController {

    @Inject
    MediaService mediaService;

    public record MediaView(Long id, String path, String contentType, Long sizeBytes,
                            Integer width, Integer height, String filename) {}

    @POST
    @Path("/upload")
    @RolesAllowed("admin")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Transactional
    @Operation(summary = "上传图片 (JPEG/PNG/WebP, ≤10MiB)")
    public R<MediaView> upload(@RestForm("file") FileUpload file) {
        if (file == null || file.uploadedFile() == null) {
            return R.fail(400, "请选择文件");
        }
        try (InputStream in = Files.newInputStream(file.uploadedFile())) {
            MediaAsset asset = mediaService.upload(in,
                    file.fileName(), file.contentType());
            return R.ok(new MediaView(asset.id, asset.path, asset.contentType,
                    asset.sizeBytes, asset.width, asset.height, asset.filename));
        } catch (Exception e) {
            return R.fail(400, "读取上传文件失败: " + e.getMessage());
        }
    }
}
