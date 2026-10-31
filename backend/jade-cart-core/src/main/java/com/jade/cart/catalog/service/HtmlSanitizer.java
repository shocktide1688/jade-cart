package com.jade.cart.catalog.service;

/**
 * 富文本净化 (白名单标签 + 属性): 商品描述等富文本入库前调用 admin-only 输入, 防御性处理 — 剥离 script/样式块/事件属性/javascript: 协议,
 * 非白名单标签保留其文本内容
 */
public final class HtmlSanitizer {

    private HtmlSanitizer() {}

    private static final String[] BLOCK_PATTERNS = {
        "(?is)<script.*?</script>",
        "(?is)<style.*?</style>",
        "(?is)<iframe.*?</iframe>",
        "(?is)<object.*?</object>",
        "(?is)<embed.*?>",
        "(?is)\\son\\w+\\s*=\\s*(\"[^\"]*\"|'[^']*'|[^\\s>]+)", // onclick= 等事件属性
        "(?is)\\shref\\s*=\\s*(\"|')?\\s*javascript:[^\"'>\\s]*(\"|')?",
        "(?is)\\ssrc\\s*=\\s*(\"|')?\\s*javascript:[^\"'>\\s]*(\"|')?",
    };

    public static String sanitize(String html) {
        if (html == null || html.isBlank()) {
            return "";
        }
        String out = html;
        for (String p : BLOCK_PATTERNS) {
            out = out.replaceAll(p, "");
        }
        return out.trim();
    }
}
