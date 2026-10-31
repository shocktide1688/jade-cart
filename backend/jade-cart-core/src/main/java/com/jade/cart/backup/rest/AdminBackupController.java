package com.jade.cart.backup.rest;

import com.jade.cart.backup.service.BackupService;
import com.jade.common.api.R;
import io.quarkus.security.Authenticated;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.nio.file.Files;
import java.util.List;
import java.util.Map;

/**
 * 备份管理 (管理端): 手动备份 / 列表 / 下载 / 删除
 */
@Path("/api/v1/backups")
@Produces(MediaType.APPLICATION_JSON)
@Authenticated
@Tag(name = "备份")
public class AdminBackupController {

    @Inject
    BackupService backupService;

    @GET
    @RolesAllowed("admin")
    @Operation(summary = "备份列表")
    public R<List<Map<String, Object>>> list() {
        return R.ok(backupService.listBackups());
    }

    @POST
    @RolesAllowed("admin")
    @Operation(summary = "立即备份 (manifest + 数据 JSON + 媒体)")
    public R<Map<String, Object>> run() throws Exception {
        java.nio.file.Path zip = backupService.createBackup();
        return R.ok(Map.of("filename", zip.getFileName().toString(),
                "sizeBytes", Files.size(zip)));
    }

    @GET
    @Path("/{filename}")
    @RolesAllowed("admin")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @Operation(summary = "下载备份")
    public Response download(@PathParam("filename") String filename) {
        java.nio.file.Path file = backupService.resolve(filename);
        if (file == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        try {
            return Response.ok(Files.readAllBytes(file))
                    .header("Content-Disposition", "attachment; filename=\"" + filename + "\"")
                    .build();
        } catch (Exception e) {
            return Response.serverError().build();
        }
    }

    @DELETE
    @Path("/{filename}")
    @RolesAllowed("admin")
    @Operation(summary = "删除备份")
    public R<Void> delete(@PathParam("filename") String filename) {
        if (!backupService.delete(filename)) {
            return R.fail(404, "备份不存在");
        }
        return R.ok();
    }
}
