package com.jade.cart.content.service;

import com.jade.cart.content.entity.ContentPage;
import com.jade.cart.settings.entity.StoreSetting;
import com.jade.common.exception.BizException;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.util.List;

/**
 * 内容页: 受限 Markdown 渲染 (先转义原始 HTML 再解析白名单语法) + 政策生成器
 */
@ApplicationScoped
public class ContentService {

    /** 7 个英文页脚政策页 (kartwo 对齐) */
    private static final String[][] POLICY_PAGES = {
            {"privacy-policy", "Privacy Policy",
                    "## Privacy Policy\n\nWe collect only the information needed to fulfill your order: "
                    + "your email, name and shipping details. We never sell your data.\n\n"
                    + "Contact us at {support_email} for any privacy request."},
            {"refund-policy", "Refund Policy",
                    "## Refund Policy\n\nUnopened items can be refunded within 30 days of delivery. "
                    + "Refunds are issued to the original payment method within 5-10 business days.\n\n"
                    + "Questions: {support_email}"},
            {"terms-of-service", "Terms of Service",
                    "## Terms of Service\n\nBy placing an order you agree to these terms. "
                    + "Prices are shown in USD and include applicable taxes at checkout.\n\n"
                    + "Questions: {support_email}"},
            {"shipping-policy", "Shipping Policy",
                    "## Shipping Policy\n\nOrders ship within 2-3 business days. "
                    + "Shipping rates are calculated at checkout by destination.\n\n"
                    + "Questions: {support_email}"},
            {"contact-us", "Contact Us",
                    "## Contact Us\n\nReach us at {support_email} or use the contact form. "
                    + "We reply within one business day."},
            {"about-us", "About Us",
                    "## About {store_name}\n\n{store_name} is an independent store built on Jade Cart. "
                    + "Questions: {support_email}"},
            {"cookie-policy", "Cookie Policy",
                    "## Cookie Policy\n\nWe use a single anonymous cookie to keep your shopping cart. "
                    + "No tracking cookies are used.\n\nQuestions: {support_email}"},
    };

    // ---------- CRUD ----------

    public List<ContentPage> pages() {
        return ContentPage.list("1=1 order by id asc");
    }

    @Transactional
    public ContentPage create(String slug, String title, String bodyMarkdown) {
        if (slug == null || slug.isBlank()) {
            throw new BizException("slug 不能为空");
        }
        String normalized = slug.trim().toLowerCase().replaceAll("[^a-z0-9-]", "-");
        if (ContentPage.count("slug", normalized) > 0) {
            throw new BizException("slug 已存在: " + normalized);
        }
        ContentPage page = new ContentPage();
        page.slug = normalized;
        page.title = title == null || title.isBlank() ? normalized : title;
        setBody(page, bodyMarkdown);
        page.persist();
        return page;
    }

    @Transactional
    public ContentPage update(Long id, String title, String bodyMarkdown, String status) {
        ContentPage page = ContentPage.findById(id);
        if (page == null) {
            throw new BizException("内容页不存在");
        }
        if (title != null && !title.isBlank()) {
            page.title = title;
        }
        if (bodyMarkdown != null) {
            setBody(page, bodyMarkdown);
        }
        if (status != null) {
            if (!ContentPage.STATUS_ACTIVE.equals(status) && !ContentPage.STATUS_DRAFT.equals(status)) {
                throw new BizException("非法状态");
            }
            page.status = status;
        }
        page.persist();
        return page;
    }

    @Transactional
    public void delete(Long id) {
        if (!ContentPage.deleteById(id)) {
            throw new BizException("内容页不存在");
        }
    }

    /** 幂等生成 7 个政策页 (仅覆盖 policy=true 的页面, 不动人工页面) */
    @Transactional
    public int generatePolicies() {
        String storeName = StoreSetting.getValue("store_name").orElse("Jade Cart Store");
        String supportEmail = StoreSetting.getValue("support_email").orElse("support@example.com");
        for (String[] def : POLICY_PAGES) {
            String body = def[2]
                    .replace("{store_name}", esc(storeName))
                    .replace("{support_email}", esc(supportEmail));
            ContentPage page = ContentPage.find("slug", def[0]).firstResult();
            if (page == null) {
                page = new ContentPage();
                page.slug = def[0];
                page.title = def[1];
                page.bodyMarkdown = body;
                page.bodyHtml = renderMarkdown(body);
                page.policy = true;
                page.persist();
            } else {
                page.title = def[1];
                page.bodyMarkdown = body;
                page.bodyHtml = renderMarkdown(body);
                page.policy = true;
                page.persist();
            }
        }
        Log.infof("policy pages generated: %d", POLICY_PAGES.length);
        return POLICY_PAGES.length;
    }

    // ---------- 受限 Markdown 渲染 ----------

    /**
     * 安全受限 Markdown: 先整体 HTML 转义, 再解析白名单语法
     * (##/### 标题, **bold**, *italic*, [text](同站/https 链接), - 列表, 段落)
     */
    public String renderMarkdown(String md) {
        if (md == null || md.isBlank()) {
            return "";
        }
        String[] lines = esc(md).split("\n", -1);
        StringBuilder html = new StringBuilder();
        boolean inList = false;
        StringBuilder para = new StringBuilder();
        for (String raw : lines) {
            String line = raw.trim();
            if (line.isEmpty()) {
                closeList(html, inListHolder(inList));
                inList = false;
                flushPara(html, para);
                continue;
            }
            if (line.startsWith("### ")) {
                flushPara(html, para);
                closeList(html, inListHolder(inList));
                inList = false;
                html.append("<h3>").append(inline(line.substring(4))).append("</h3>");
            } else if (line.startsWith("## ")) {
                flushPara(html, para);
                closeList(html, inListHolder(inList));
                inList = false;
                html.append("<h2>").append(inline(line.substring(3))).append("</h2>");
            } else if (line.startsWith("# ")) {
                flushPara(html, para);
                closeList(html, inListHolder(inList));
                inList = false;
                html.append("<h2>").append(inline(line.substring(2))).append("</h2>");
            } else if (line.startsWith("- ") || line.startsWith("* ")) {
                flushPara(html, para);
                if (!inList) {
                    html.append("<ul>");
                    inList = true;
                }
                html.append("<li>").append(inline(line.substring(2))).append("</li>");
            } else {
                closeList(html, inListHolder(inList));
                inList = false;
                if (para.length() > 0) {
                    para.append(' ');
                }
                para.append(inline(line));
            }
        }
        closeList(html, inListHolder(inList));
        flushPara(html, para);
        return html.toString();
    }

    private boolean[] inListHolder(boolean v) {
        return new boolean[]{v};
    }

    private void closeList(StringBuilder html, boolean[] inList) {
        if (inList[0]) {
            html.append("</ul>");
        }
    }

    private void flushPara(StringBuilder html, StringBuilder para) {
        if (para.length() > 0) {
            html.append("<p>").append(para).append("</p>");
            para.setLength(0);
        }
    }

    /** 行内语法: **bold** / *italic* / [text](https:// 或 / 开头链接) */
    private String inline(String s) {
        StringBuilder out = new StringBuilder();
        int i = 0;
        while (i < s.length()) {
            if (s.startsWith("**", i)) {
                int end = s.indexOf("**", i + 2);
                if (end > 0) {
                    out.append("<strong>").append(s, i + 2, end).append("</strong>");
                    i = end + 2;
                    continue;
                }
            }
            if (s.charAt(i) == '[') {
                int close = s.indexOf(']', i);
                if (close > 0 && close + 1 < s.length() && s.charAt(close + 1) == '(') {
                    int paren = s.indexOf(')', close + 2);
                    if (paren > 0) {
                        String href = s.substring(close + 2, paren);
                        if (href.startsWith("https://") || (href.startsWith("/") && !href.startsWith("//"))) {
                            out.append("<a href=\"").append(href).append("\">")
                                    .append(s, i + 1, close).append("</a>");
                            i = paren + 1;
                            continue;
                        }
                    }
                }
            }
            out.append(s.charAt(i));
            i++;
        }
        return out.toString();
    }

    private void setBody(ContentPage page, String bodyMarkdown) {
        page.bodyMarkdown = bodyMarkdown;
        page.bodyHtml = renderMarkdown(bodyMarkdown);
    }

    private String esc(String s) {
        return s == null ? "" : s.replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\"", "&quot;");
    }
}
