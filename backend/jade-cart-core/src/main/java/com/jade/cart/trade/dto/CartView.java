package com.jade.cart.trade.dto;

import java.util.List;

/** 购物车视图 (匿名 cookie token 会话) */
public record CartView(
        String cartPublicId, String currency, List<LineView> items, Long subtotalCents) {

    public record LineView(
            Long variantId,
            String productTitle,
            String productSlug,
            String sku,
            String spec,
            Long unitPriceCents,
            Integer quantity,
            Integer available,
            Long lineTotalCents,
            /** 商品主图相对路径 (移动端购物车展示图, 前端拼 /media/{path}) */
            String mainImagePath) {}
}
