package com.jade.cart.catalog.dto;

import java.util.List;

/** 店面商品详情视图: 商品全量 (含变体/库存) + 图集 (主图在前) + 所属分类 slug */
public record StoreProductDetailView(
        ProductView product, List<String> imagePaths, List<String> categorySlugs) {}
