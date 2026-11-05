package com.jade.cart.app.storefront.i18n;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.Optional;
import org.jboss.resteasy.reactive.server.ServerRequestFilter;

/**
 * `?lang=xx` → 写 store_lang cookie 并 303 回原地址 (剥掉 lang 参数): 页脚语言切换用纯链接即可, 符合店面 "无 JS 可完成购物"。只拦店面
 * GET (api/media/q 不动); 保留 lang 之外的查询参数。
 */
@ApplicationScoped
public class LangParamFilter {

    @Inject StoreMessages messages;

    /** 非店面路径 (管理 API / dev 工具 / 媒体) 不处理 */
    private static boolean isStorefrontPath(String p) {
        return !(p.startsWith("/api/") || p.startsWith("/q/") || p.startsWith("/media/"));
    }

    @ServerRequestFilter
    public Optional<Response> langParam(ContainerRequestContext ctx) {
        UriInfo uri = ctx.getUriInfo();
        String path = uri.getPath();
        String normalized = path.startsWith("/") ? path : "/" + path;
        if (!"GET".equals(ctx.getMethod()) || !isStorefrontPath(normalized)) {
            return Optional.empty();
        }
        String lang = uri.getQueryParameters().getFirst("lang");
        if (lang == null) {
            return Optional.empty();
        }
        // 非法值不落 cookie, 只剥参数 (防手指误改); 合法值才切换
        NewCookie cookie = null;
        if (messages.supported(lang)) {
            cookie =
                    new NewCookie.Builder(StoreMessages.LANG_COOKIE)
                            .value(lang)
                            .path("/")
                            .maxAge(60 * 60 * 24 * 365)
                            .sameSite(NewCookie.SameSite.LAX)
                            .httpOnly(true)
                            .build();
        }
        // 重建跳转地址: 原地址剥掉 lang 参数、保留其余参数 (?category= / ?img=), UriBuilder 负责转义
        URI target = uri.getRequestUriBuilder().replaceQueryParam("lang").build();
        Response.ResponseBuilder builder =
                Response.status(Response.Status.SEE_OTHER).location(target);
        if (cookie != null) {
            builder.cookie(cookie);
        }
        return Optional.of(builder.build());
    }
}
