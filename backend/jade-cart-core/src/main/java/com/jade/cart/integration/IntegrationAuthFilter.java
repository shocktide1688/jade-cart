package com.jade.cart.integration;

import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Response;
import java.util.Optional;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.resteasy.reactive.server.ServerRequestFilter;

/**
 * ERP 集成端点认证: Bearer API-Key 静态凭证 (机器对机器, 非用户 JWT)。
 *
 * <p>仅拦截 /api/v1/integration/** 前缀; 密钥未配置 (空) 时端点整体关闭 (503), 避免裸奔。 密钥按店注入环境变量
 * JADE_INTEGRATION_API_KEY, 泄露可单独吊销 (换 key 重启即生效)。
 */
@ApplicationScoped
public class IntegrationAuthFilter {

    private static final String PATH_PREFIX = "/api/v1/integration";

    @Inject
    @ConfigProperty(name = "jade.integration.api-key", defaultValue = "")
    String apiKey;

    @ServerRequestFilter
    public Optional<Response> check(ContainerRequestContext ctx) {
        String path = ctx.getUriInfo().getPath();
        String normalized = path.startsWith("/") ? path : "/" + path;
        if (!normalized.startsWith(PATH_PREFIX)) {
            return Optional.empty();
        }
        if (apiKey.isBlank() || "disabled".equals(apiKey)) {
            Log.warn("集成端点未配置 API-Key, 已拒绝访问 (JADE_INTEGRATION_API_KEY)");
            return Optional.of(Response.status(Response.Status.SERVICE_UNAVAILABLE).build());
        }
        String auth = ctx.getHeaderString("X-API-Key");
        if (auth != null && auth.equals(apiKey)) {
            return Optional.empty();
        }
        return Optional.of(
                Response.status(Response.Status.UNAUTHORIZED)
                        .entity("{\"code\":401,\"message\":\"invalid api key\",\"success\":false}")
                        .build());
    }
}
