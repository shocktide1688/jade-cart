package com.jade.cart.settings.rest;

import com.jade.cart.settings.service.ThemeService;
import com.jade.common.api.R;
import io.quarkus.security.Authenticated;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
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

/** 店面主题 (管理端): 预设 + 覆盖, 保存即时生效 (SSR 每请求渲染) */
@Path("/api/v1/theme")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Authenticated
@Tag(name = "店面主题")
public class AdminThemeController {

    @Inject ThemeService themeService;

    @GET
    @RolesAllowed("admin")
    @Operation(summary = "读取主题 (当前预设/覆盖/预设列表/最终 token)")
    public R<Map<String, Object>> get() {
        return R.ok(themeService.view());
    }

    @PUT
    @RolesAllowed("admin")
    @Transactional
    @Operation(summary = "保存主题 (preset + 覆盖字段; 非法值静默忽略)")
    public R<Map<String, String>> save(Map<String, Object> body) {
        String preset = body == null ? null : (String) body.get("preset");
        @SuppressWarnings("unchecked")
        Map<String, String> overrides =
                body == null
                        ? Map.of()
                        : (Map<String, String>) body.getOrDefault("overrides", Map.of());
        return R.ok(themeService.save(preset, overrides));
    }
}
