package com.jade.cart.trade.rest;

import com.jade.cart.trade.dto.CartView;
import com.jade.cart.trade.entity.Cart;
import com.jade.cart.trade.service.CartService;
import com.jade.common.api.R;
import jakarta.annotation.security.PermitAll;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

/** 店面购物车 API (公开, 匿名 cookie token 会话) */
@Path("/api/store/cart")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@PermitAll
@Tag(name = "店面-购物车")
public class StoreCartController {

    /** 30 天有效 */
    private static final int COOKIE_MAX_AGE = 60 * 60 * 24 * 30;

    @Inject CartService cartService;

    public record ItemReq(Long variantId, Integer quantity) {}

    public record UpdateReq(Integer quantity) {}

    @GET
    @Operation(summary = "查看购物车 (无 cookie 时建新会话)")
    public Response get(@Context HttpHeaders headers) {
        TokenBox box = resolve(headers);
        Cart cart = cartService.getOrCreate(box.token);
        return wrap(box, cartService.view(cart), cart);
    }

    @GET
    @Path("/count")
    @Operation(summary = "购物车件数 (店面导航徽章; 无会话 = 0, 不建会话)")
    public R<Integer> count(@Context HttpHeaders headers) {
        return R.ok(
                CartService.readToken(headers)
                        .flatMap(Cart::activeByToken)
                        .map(c -> c.items().size())
                        .orElse(0));
    }

    @POST
    @Path("/items")
    @Transactional
    @Operation(summary = "加购")
    public Response add(@Context HttpHeaders headers, ItemReq req) {
        TokenBox box = resolve(headers);
        if (req.variantId() == null) {
            return Response.ok(R.fail(400, "variantId 不能为空")).build();
        }
        int qty = req.quantity() == null ? 1 : req.quantity();
        Cart cart = cartService.getOrCreate(box.token);
        return wrap(box, cartService.addItem(cart.token, req.variantId(), qty), cart);
    }

    @PUT
    @Path("/items/{variantId}")
    @Transactional
    @Operation(summary = "改数量 (0 = 移除)")
    public Response update(
            @Context HttpHeaders headers, @PathParam("variantId") Long variantId, UpdateReq req) {
        TokenBox box = resolve(headers);
        int qty = req.quantity() == null ? 1 : req.quantity();
        Cart cart = cartService.getOrCreate(box.token);
        return wrap(box, cartService.updateItem(cart.token, variantId, qty), cart);
    }

    @DELETE
    @Path("/items/{variantId}")
    @Transactional
    @Operation(summary = "移除")
    public Response remove(@Context HttpHeaders headers, @PathParam("variantId") Long variantId) {
        TokenBox box = resolve(headers);
        Cart cart = cartService.getOrCreate(box.token);
        return wrap(box, cartService.removeItem(cart.token, variantId), cart);
    }

    // ---------- 内部 ----------

    private record TokenBox(String token, boolean isNew) {}

    private TokenBox resolve(HttpHeaders headers) {
        return CartService.readToken(headers)
                .map(t -> new TokenBox(t, false))
                .orElseGet(() -> new TokenBox(CartService.newToken(), true));
    }

    /**
     * 写回 cookie
     *
     * <p>两种情况需要回写: 新会话 (isNew); 或 token 冲突时服务端换了新 token (cart.token 与请求 token 不同)。
     */
    private Response wrap(TokenBox box, CartView view, Cart cart) {
        Response.ResponseBuilder builder = Response.ok(R.ok(view));
        String effectiveToken = cart == null ? box.token() : cart.token;
        if (box.isNew() || !effectiveToken.equals(box.token())) {
            builder.cookie(
                    new NewCookie.Builder(CartService.CART_COOKIE)
                            .value(effectiveToken)
                            .path("/")
                            .maxAge(COOKIE_MAX_AGE)
                            .httpOnly(true)
                            .build());
        }
        return builder.build();
    }
}
