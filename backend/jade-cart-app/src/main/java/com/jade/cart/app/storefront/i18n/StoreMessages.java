package com.jade.cart.app.storefront.i18n;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.HttpHeaders;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * 店面多语言服务: 语言注册表配置驱动 `jade.cart.locales` (格式 "code:label,code:label", 首个为默认), 翻译资产 = classpath
 * `i18n/store_&lt;code&gt;.properties` (英文默认值内置 {@link UiText}, 缺 key / 缺文件自动落英文)。
 *
 * <p>请求语言解析链: cookie store_lang → Accept-Language → 店铺默认; `?lang=` 由 {@link LangParamFilter} 转
 * cookie (无 JS 可切换)。
 */
@ApplicationScoped
public class StoreMessages {

    /** 语言选择 cookie (值 = 语言代码, 如 zh-CN) */
    public static final String LANG_COOKIE = "store_lang";

    /** 语言条目视图 (页脚切换器) */
    public record Lang(String code, String label) {}

    @Inject
    @ConfigProperty(name = "jade.cart.locales", defaultValue = "en:English,zh-CN:简体中文")
    String localesConfig;

    private final Map<String, Properties> overlays = new ConcurrentHashMap<>();

    /** 注册表 (有序, 首个为默认) */
    public List<Lang> registry() {
        List<Lang> out = new ArrayList<>();
        for (String entry : localesConfig.split(",")) {
            String[] parts = entry.trim().split(":");
            if (parts.length >= 1 && !parts[0].isBlank()) {
                out.add(new Lang(parts[0].trim(), parts.length > 1 ? parts[1].trim() : parts[0]));
            }
        }
        if (out.isEmpty()) {
            out.add(new Lang("en", "English"));
        }
        return out;
    }

    public boolean supported(String code) {
        return code != null && registry().stream().anyMatch(l -> l.code().equals(code));
    }

    /** 页脚切换器视图: code/label/current */
    public List<Map<String, Object>> languages(String current) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (Lang l : registry()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("code", l.code());
            m.put("label", l.label());
            m.put("current", l.code().equals(current));
            out.add(m);
        }
        return out;
    }

    /** 请求语言解析: cookie → Accept-Language (全码匹配再语言前缀匹配) → 默认 */
    public String resolveCode(HttpHeaders headers) {
        if (headers != null) {
            jakarta.ws.rs.core.Cookie cookieJar = headers.getCookies().get(LANG_COOKIE);
            String cookie = cookieJar == null ? null : cookieJar.getValue();
            if (cookie != null && supported(cookie)) {
                return cookie;
            }
            List<Locale> acceptable =
                    headers.getAcceptableLanguages().stream()
                            .filter(l -> l != null && !"*".equals(l.getLanguage()))
                            .toList();
            for (Locale l : acceptable) {
                String tag = l.toLanguageTag();
                if (supported(tag)) {
                    return tag;
                }
            }
            for (Lang cand : registry()) {
                for (Locale l : acceptable) {
                    String candLang = cand.code().split("-")[0];
                    if (candLang.equalsIgnoreCase(l.getLanguage())) {
                        return cand.code();
                    }
                }
            }
        }
        return registry().get(0).code();
    }

    /** 当前请求的文案集 (英文默认 + 语言覆盖) */
    public UiText forRequest(HttpHeaders headers) {
        return forCode(resolveCode(headers));
    }

    public UiText forCode(String code) {
        Properties p = overlay(code);
        // htmlLang 不进翻译文件: 由代码按语言代码注入 (模板 <html lang>)
        if (!p.containsKey("htmlLang")) {
            p.setProperty("htmlLang", code);
        }
        return UiText.from(p);
    }

    /** 语言覆盖包 (classpath i18n/store_&lt;code&gt;.properties, 缺文件 = 纯英文默认) */
    private Properties overlay(String code) {
        return overlays.computeIfAbsent(code, this::loadOverlay);
    }

    private Properties loadOverlay(String code) {
        Properties p = new Properties();
        String path = "i18n/store_" + code + ".properties";
        try (InputStream in =
                Thread.currentThread().getContextClassLoader().getResourceAsStream(path)) {
            if (in != null) {
                p.load(new InputStreamReader(in, StandardCharsets.UTF_8));
            }
        } catch (Exception e) {
            // 缺文件/坏文件不阻断: 落英文默认
            io.quarkus.logging.Log.warnf(
                    e, "store text bundle load failed, fallback to en defaults: %s", path);
        }
        return p;
    }
}
