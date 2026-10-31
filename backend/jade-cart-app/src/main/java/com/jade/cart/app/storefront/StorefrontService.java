package com.jade.cart.app.storefront;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jade.cart.app.storefront.sections.SectionRenderer;
import com.jade.cart.storefront.entity.StorefrontSection;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import java.util.ArrayList;
import java.util.List;

/**
 * 店面装修渲染管线: 首页 = 有序区块 → SectionRenderer 注册表逐块渲染 → HTML 列表
 * 空列表时店面首页回退内置布局; 单块渲染失败跳过不阻断
 */
@ApplicationScoped
public class StorefrontService {

    @Inject
    Instance<SectionRenderer> renderers;

    private final ObjectMapper mapper = new ObjectMapper();

    public List<String> renderHome() {
        List<String> out = new ArrayList<>();
        for (StorefrontSection section : StorefrontSection.listVisible("home")) {
            SectionRenderer renderer = renderers.stream()
                    .filter(r -> r.type().equals(section.type))
                    .findFirst().orElse(null);
            if (renderer == null) {
                Log.warnf("unknown section type skipped: %s", section.type);
                continue;
            }
            try {
                JsonNode cfg = mapper.readTree(section.config == null || section.config.isBlank()
                        ? "{}" : section.config);
                out.add(renderer.render(cfg));
            } catch (Exception e) {
                Log.errorf(e, "section render failed, skipped: type=%s id=%s", section.type, section.id);
            }
        }
        return out;
    }
}
