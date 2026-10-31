package com.jade.cart.settings.rest;

import com.jade.cart.settings.entity.StoreSetting;
import com.jade.common.api.R;
import io.quarkus.security.Authenticated;
import jakarta.annotation.security.RolesAllowed;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.Map;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

/** 商城设置 (管理端 KV, 值 AES-GCM 加密存储) */
@Path("/api/v1/store-settings")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Authenticated
@Tag(name = "商城设置")
public class AdminStoreSettingController {

    @GET
    @RolesAllowed("admin")
    @Operation(summary = "读取全部设置")
    public R<Map<String, String>> get() {
        return R.ok(StoreSetting.allAsMap());
    }

    @PUT
    @RolesAllowed("admin")
    @Transactional
    @Operation(summary = "批量保存设置 (整Map覆盖式写入)")
    public R<Map<String, String>> save(Map<String, String> settings) {
        if (settings != null) {
            settings.forEach(StoreSetting::put);
        }
        return R.ok(StoreSetting.allAsMap());
    }
}
