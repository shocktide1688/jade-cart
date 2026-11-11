package com.jade.cart.sysconfig.rest;

import com.jade.cart.sysconfig.entity.AppConfig;
import com.jade.common.api.R;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.HashMap;
import java.util.Map;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

/** 系统配置 (管理端 KV, 明文存储非敏感项; 密钥类配置请走 StoreSetting) */
@Path("/api/v1/sys-config")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "系统配置")
public class AdminSysConfigController {

    /** 公开可读的白名单键 (登录页未登录也要取欢迎语), 防止误存敏感值被匿名拉走 */
    private static final String READABLE_KEYS = "app.title,login.welcome";

    @GET
    @PermitAll
    @Operation(summary = "读取系统配置 (公开白名单键, 供布局/登录页取标题与欢迎语)")
    public R<Map<String, String>> get() {
        Map<String, String> all = AppConfig.allAsMap();
        Map<String, String> readable = new HashMap<>();
        for (String k : READABLE_KEYS.split(",")) {
            if (all.containsKey(k)) {
                readable.put(k, all.get(k));
            }
        }
        return R.ok(readable);
    }

    @PUT
    @RolesAllowed("admin")
    @Transactional
    @Operation(summary = "保存系统配置 (传入的键覆盖写入, 其余键保留)")
    public R<Map<String, String>> save(Map<String, String> config) {
        if (config != null) {
            config.forEach(AppConfig::put);
        }
        return R.ok(AppConfig.allAsMap());
    }
}
