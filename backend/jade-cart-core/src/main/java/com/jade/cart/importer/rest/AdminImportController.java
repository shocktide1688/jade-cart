package com.jade.cart.importer.rest;

import com.jade.cart.importer.entity.ImportJob;
import com.jade.cart.importer.service.ImportService;
import com.jade.common.api.R;
import com.jade.common.exception.BizException;
import io.quarkus.security.Authenticated;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.multipart.FileUpload;

import java.nio.file.Files;
import java.util.List;

/**
 * CSV 商品导入 (管理端): 干跑预览 → 确认执行 (同文件 sha256 幂等)
 */
@Path("/api/v1/import")
@Produces(MediaType.APPLICATION_JSON)
@Authenticated
@Tag(name = "数据导入")
public class AdminImportController {

    @Inject
    ImportService importService;

    @POST
    @Path("/preview")
    @RolesAllowed("admin")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Transactional
    @Operation(summary = "上传 CSV 干跑预览 (零写入; 同文件重复上传返回既有任务)")
    public R<ImportJob> preview(@RestForm("file") FileUpload file,
                                @RestForm("format") String format) {
        if (file == null || file.uploadedFile() == null) {
            return R.fail(400, "请选择 CSV 文件");
        }
        try {
            byte[] csv = Files.readAllBytes(file.uploadedFile());
            return R.ok(importService.preview(file.fileName(), format == null ? "generic" : format, csv));
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            return R.fail(400, "读取文件失败: " + e.getMessage());
        }
    }

    @POST
    @Path("/{publicId}/confirm")
    @RolesAllowed("admin")
    @Transactional
    @Operation(summary = "确认执行导入 (已 CONFIRMED 的任务拒绝重复执行)")
    public R<ImportJob> confirm(@PathParam("publicId") String publicId) {
        return R.ok(importService.confirm(publicId));
    }

    @GET
    @Path("/jobs")
    @RolesAllowed("admin")
    @Operation(summary = "导入历史")
    public R<List<ImportJob>> jobs(@QueryParam("limit") Integer limit) {
        return R.ok(ImportJob.<ImportJob>find("1=1 order by id desc")
                .page(0, limit == null ? 20 : limit).list());
    }
}
