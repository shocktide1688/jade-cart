package com.jade.cart.app.web;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.container.ContainerResponseContext;
import org.jboss.resteasy.reactive.server.ServerResponseFilter;

/**
 * 页面 HTML 缓存策略: no-cache (每次回源校验)。
 *
 * <p>背景: 店面页面 HTML 里内联了整套 CSS (主题变量运行时注入), HTML 变了样式就变了。 之前页面响应没有任何 Cache-Control, 浏览器按启发式缓存复用旧
 * HTML —— 用户看到的 是旧样式却查不出原因 (媒体/图片已有 immutable 长缓存, 不受影响)。
 */
@ApplicationScoped
public class PageCacheControlFilter {

    private static final String HTML = "text/html";

    @ServerResponseFilter
    public void setNoCacheOnHtml(ContainerResponseContext responseContext) {
        String contentType =
                responseContext.getMediaType() == null
                        ? null
                        : responseContext.getMediaType().toString();
        if (contentType != null && contentType.startsWith(HTML)) {
            responseContext.getHeaders().putSingle("Cache-Control", "no-cache");
        }
    }
}
