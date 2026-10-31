package com.jade.cart.storefront.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jade.cart.storefront.entity.StorefrontSection;
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
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 店面装修 (管理端): 区块列表 + 整体保存 (position = 数组顺序)
 */
@Path("/api/v1/storefront")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Authenticated
@Tag(name = "店面装修")
public class AdminStorefrontController {

    private static final List<String> TYPES = List.of(
            "hero", "featured", "category_grid", "usp", "rich_text", "blog_latest", "banner_strip");

    private final ObjectMapper mapper = new ObjectMapper();

    public record SectionReq(String type, Map<String, Object> config, Boolean visible) {}

    @GET
    @RolesAllowed("admin")
    @Operation(summary = "首页区块列表 (含合法类型)")
    public R<Map<String, Object>> list() {
        List<Map<String, Object>> out = new ArrayList<>();
        for (StorefrontSection s : StorefrontSection.listPage("home")) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", s.id);
            m.put("type", s.type);
            m.put("visible", s.visible);
            m.put("config", parse(s.config));
            out.add(m);
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("sections", out);
        body.put("types", TYPES);
        return R.ok(body);
    }

    /** 整体保存: 数组顺序即 position; 覆盖式 (删掉未出现的) */
    @PUT
    @RolesAllowed("admin")
    @Transactional
    @Operation(summary = "保存首页区块编排")
    public R<Void> save(List<SectionReq> sections) {
        if (sections == null) {
            sections = List.of();
        }
        List<StorefrontSection> keep = new ArrayList<>();
        int pos = 0;
        for (SectionReq req : sections) {
            if (req.type() == null || !TYPES.contains(req.type())) {
                continue; // 非法类型跳过
            }
            StorefrontSection s = new StorefrontSection();
            s.page = "home";
            s.position = pos++;
            s.type = req.type();
            s.visible = !Boolean.FALSE.equals(req.visible());
            s.config = toJson(req.config());
            keep.add(s);
        }
        StorefrontSection.delete("page", "home");
        keep.forEach(s -> s.persist());
        return R.ok();
    }

    private String toJson(Map<String, Object> config) {
        if (config == null || config.isEmpty()) {
            return "{}";
        }
        try {
            return mapper.writeValueAsString(config);
        } catch (Exception e) {
            return "{}";
        }
    }

    private Map<String, Object> parse(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            return mapper.readValue(json, Map.class);
        } catch (Exception e) {
            return Map.of();
        }
    }
}
