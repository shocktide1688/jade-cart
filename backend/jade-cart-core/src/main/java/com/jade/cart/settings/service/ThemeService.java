package com.jade.cart.settings.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jade.cart.settings.entity.StoreSetting;
import com.jade.common.exception.BizException;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 店面主题 (设计令牌): 预设 4 套 + 商家覆盖, 合并出最终 token 存储: mall_store_setting KV "theme_config" =
 * {"preset":"minimal","overrides":{...}} 校验: 颜色 #hex / radius 0~24 / font 白名单 (防 CSS 注入)
 */
@ApplicationScoped
public class ThemeService {

    public static final String KV_KEY = "theme_config";
    private static final Pattern COLOR = Pattern.compile("^#[0-9a-fA-F]{3,8}$");
    private static final List<String> FONTS = List.of("sans", "serif", "mono");

    /** 非 token 附加键: 媒体路径 (不可 blank 之外无格式校验) */
    private static final List<String> MEDIA_KEYS = List.of("logoPath", "faviconPath");

    /** token 键 → 预设值 (minimal 为基线) */
    private static final Map<String, String> BASE =
            Map.of(
                    "primary", "#1a1a1a",
                    "bg", "#ffffff",
                    "ink", "#1a1a1a",
                    "muted", "#767676",
                    "line", "#e6e6e6",
                    "radius", "8",
                    "font", "sans");

    /** 预设主题 (有序, minimal 首位) */
    private static final Map<String, Map<String, String>> PRESETS = new LinkedHashMap<>();

    static {
        PRESETS.put("minimal", BASE);
        PRESETS.put(
                "classic",
                Map.of(
                        "primary", "#7c2d12", "bg", "#faf6f0", "ink", "#292524", "muted", "#a8a29e",
                        "line", "#e7e0d8", "radius", "4", "font", "serif"));
        PRESETS.put(
                "bold",
                Map.of(
                        "primary", "#4f46e5", "bg", "#ffffff", "ink", "#111827", "muted", "#6b7280",
                        "line", "#e5e7eb", "radius", "16", "font", "sans"));
        PRESETS.put(
                "warm",
                Map.of(
                        "primary", "#b45309", "bg", "#fffbeb", "ink", "#451a03", "muted", "#a16207",
                        "line", "#fde68a", "radius", "12", "font", "sans"));
    }

    private final ObjectMapper mapper = new ObjectMapper();

    /** 预设列表 (管理端卡片) */
    public List<Map<String, Object>> presets() {
        List<Map<String, Object>> out = new ArrayList<>();
        PRESETS.forEach(
                (name, tokens) -> {
                    Map<String, Object> p = new LinkedHashMap<>(tokens);
                    p.put("name", name);
                    out.add(p);
                });
        return out;
    }

    /** 最终 token (预设 + 覆盖合并, 全部校验后返回; 供控制器注入模板) */
    public Map<String, String> tokens() {
        String presetName = "minimal";
        Map<String, String> overrides = new LinkedHashMap<>();
        String raw = StoreSetting.getValue(KV_KEY).orElse(null);
        if (raw != null && !raw.isBlank()) {
            try {
                JsonNode node = mapper.readTree(raw);
                String p = node.path("preset").asText("minimal");
                if (PRESETS.containsKey(p)) {
                    presetName = p;
                }
                JsonNode ov = node.path("overrides");
                if (ov.isObject()) {
                    ov.fields()
                            .forEachRemaining(
                                    e -> overrides.put(e.getKey(), e.getValue().asText()));
                }
            } catch (Exception e) {
                Log.warnf("theme_config 解析失败, 回退预设: %s", e.getMessage());
            }
        }
        Map<String, String> tokens = new LinkedHashMap<>(PRESETS.get(presetName));
        overrides.forEach(
                (k, v) -> {
                    if ((BASE.containsKey(k) || MEDIA_KEYS.contains(k)) && valid(k, v)) {
                        tokens.put(k, v);
                    }
                });
        // 派生: font 白名单 → 字体栈
        tokens.put(
                "fontFamily",
                switch (tokens.get("font")) {
                    case "serif" -> "Georgia,'Times New Roman',serif";
                    case "mono" -> "ui-monospace,'SF Mono',Menlo,monospace";
                    default ->
                            "-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,Helvetica,Arial,sans-serif";
                });
        // 媒体路径键: 模板 {#if} 需要显式键存在, 无值置 null
        for (String k : MEDIA_KEYS) {
            tokens.put(
                    k,
                    overrides.containsKey(k) && !overrides.get(k).isBlank()
                            ? overrides.get(k)
                            : null);
        }
        return tokens;
    }

    /** 当前配置视图 (管理端) */
    public Map<String, Object> view() {
        String preset = "minimal";
        Map<String, String> overrides = new LinkedHashMap<>();
        String raw = StoreSetting.getValue(KV_KEY).orElse(null);
        if (raw != null && !raw.isBlank()) {
            try {
                JsonNode node = mapper.readTree(raw);
                preset = node.path("preset").asText("minimal");
                JsonNode ov = node.path("overrides");
                if (ov.isObject()) {
                    ov.fields()
                            .forEachRemaining(
                                    e -> overrides.put(e.getKey(), e.getValue().asText()));
                }
            } catch (Exception ignored) {
            }
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("preset", preset);
        out.put("overrides", overrides);
        out.put("presets", presets());
        out.put("tokens", tokens());
        return out;
    }

    /** 保存: 校验后写 KV */
    public Map<String, String> save(String preset, Map<String, String> overrides) {
        if (preset != null && !PRESETS.containsKey(preset)) {
            throw new BizException("未知主题预设: " + preset);
        }
        Map<String, String> clean = new LinkedHashMap<>();
        if (overrides != null) {
            overrides.forEach(
                    (k, v) -> {
                        if ((BASE.containsKey(k) || MEDIA_KEYS.contains(k)) && valid(k, v)) {
                            clean.put(k, v);
                        }
                    });
        }
        try {
            String json =
                    mapper.writeValueAsString(
                            Map.of(
                                    "preset",
                                    preset == null ? "minimal" : preset,
                                    "overrides",
                                    clean));
            StoreSetting.put(KV_KEY, json);
        } catch (Exception e) {
            BizException ex = new BizException("主题保存失败: " + e.getMessage());
            ex.initCause(e);
            throw ex;
        }
        Log.infof("theme saved: preset=%s overrides=%d", preset, clean.size());
        return tokens();
    }

    private boolean valid(String key, String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        if (MEDIA_KEYS.contains(key)) {
            return value.length() <= 300;
        }
        return switch (key) {
            case "radius" -> {
                try {
                    int r = Integer.parseInt(value.trim());
                    yield r >= 0 && r <= 24;
                } catch (Exception e) {
                    yield false;
                }
            }
            case "font" -> FONTS.contains(value);
            default -> COLOR.matcher(value.trim()).matches(); // 颜色键
        };
    }
}
