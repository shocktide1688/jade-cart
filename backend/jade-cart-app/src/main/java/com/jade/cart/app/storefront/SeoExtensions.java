package com.jade.cart.app.storefront;

import io.quarkus.qute.TemplateExtension;

/** SEO 文案扩展: {value.teaser(160)} 截断 + 去换行 (meta description 用) */
public final class SeoExtensions {

    private SeoExtensions() {}

    @TemplateExtension(matchName = "teaser")
    public static String teaser(String s, int max) {
        if (s == null) {
            return "";
        }
        String flat =
                s.replaceAll("\\s+", " ")
                        .replaceAll("^#+\\s*", "") // markdown 标题符号
                        .replace("[", "")
                        .replace("]", "")
                        .replace("(", " ")
                        .replace(")", "")
                        .trim();
        return flat.length() <= max ? flat : flat.substring(0, max - 1) + "…";
    }
}
