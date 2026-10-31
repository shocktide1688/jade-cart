package com.jade.cart.catalog.rest.req;

import java.util.List;

/**
 * 商品分类关联请求
 */
public record CategoryLinkReq(List<Long> categoryIds) {}
