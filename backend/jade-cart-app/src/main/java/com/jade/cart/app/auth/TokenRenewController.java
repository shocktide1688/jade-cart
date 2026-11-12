package com.jade.cart.app.auth;

import com.jade.common.api.R;
import com.jade.security.jwt.MpJwtUtil;
import com.jade.security.repository.SysUserRepository;
import io.quarkus.security.Authenticated;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.Map;
import java.util.Set;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

/**
 * 登录 token 滑动续期端点: 前端在路由切换时调用 (剩余寿命 < 3 天才调, 10 分钟节流), 按当前身份从数据库取最新角色/租户签发全新 7 天 token —— 活跃使用永不过期,
 * 闲置到期才掉线。
 *
 * <p>为什么不用响应过滤器 / 解析旧 token: RESTEasy Reactive 3.39 的 filter API 参数白名单 拿不到请求头, JsonWebToken 裸
 * getClaim 又有 parsson JsonNumber 强转坑; 直接以 SecurityIdentity 身份 + 数据库为准更稳, 且角色/租户天然与库内最新状态一致。
 * 手动退出/改密等场景不受影响 (旧 token 签名仍有效至自然过期, RS256 密钥固定)。
 */
@Path("/api/v1/auth")
@Produces(MediaType.APPLICATION_JSON)
@ApplicationScoped
@Tag(name = "认证")
public class TokenRenewController {

    @Inject SecurityIdentity identity;

    @Inject MpJwtUtil mpJwtUtil;

    @Inject SysUserRepository sysUserRepository;

    @POST
    @Path("/renew")
    @Authenticated
    @Operation(summary = "滑动续期 (活跃用户换发新 token)")
    public R<Map<String, String>> renew() {
        String username = identity.getPrincipal().getName();
        Set<String> roles = identity.getRoles();
        return sysUserRepository
                .findByUsername(username)
                .map(
                        user -> {
                            String fresh =
                                    mpJwtUtil.generate(user.id, username, roles, user.tenantId);
                            return R.ok(Map.of("token", fresh, "renewed", "true"));
                        })
                .orElseGet(() -> R.fail(401, "账号不存在, 请重新登录"));
    }
}
