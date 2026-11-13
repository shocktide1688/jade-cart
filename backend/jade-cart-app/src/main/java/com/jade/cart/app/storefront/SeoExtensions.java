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
                s.replaceAll("<[^>]+>", " ") // HTML 标签 (描述存的是 HTML, meta/摘要都要纯文本)
                        .replace("&nbsp;", " ")
                        .replace("&amp;", "&")
                        .replace("&lt;", "<")
                        .replace("&gt;", ">")
                        .replaceAll("\\s+", " ")
                        .replaceAll("^#+\\s*", "") // markdown 标题符号
                        .replace("[", "")
                        .replace("]", "")
                        .replace("(", " ")
                        .replace(")", "")
                        .trim();
        if (flat.length() <= max) {
            return flat;
        }
        // 截断到句子边界 (句号/问号/叹号), 避免半句 + 省略号看起来像内容损坏
        String cut = flat.substring(0, max);
        int stop =
                Math.max(
                        cut.lastIndexOf('.'), Math.max(cut.lastIndexOf('!'), cut.lastIndexOf('?')));
        if (stop > max / 2) {
            return cut.substring(0, stop + 1);
        }
        // 无句号可用 → 退到最后一个词边界
        int word = cut.lastIndexOf(' ');
        return (word > max / 2 ? cut.substring(0, word) : cut) + "…";
    }
}
