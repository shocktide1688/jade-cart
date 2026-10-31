package com.jade.cart.catalog.dto;

import java.time.LocalDateTime;
import java.util.List;

/** 商品视图 (管理端/店面通用形状) */
public record ProductView(
        Long id,
        String publicId,
        String title,
        String slug,
        String description,
        String seoDescription,
        String seoKeywords,
        String status,
        boolean featured,
        String mainImagePath,
        LocalDateTime createdAt,
        List<Long> categoryIds,
        List<OptionView> options,
        List<VariantView> variants) {

    /** 变体轴视图 */
    public record OptionView(Long id, String optionKey, String displayName, List<String> values) {}

    /** 变体视图 (含库存) */
    public record VariantView(
            Long id,
            String publicId,
            String sku,
            Long priceCents,
            String spec,
            Integer quantity,
            Integer reserved,
            Integer available) {}

    /** 商品列表摘要 */
    public record Summary(
            Long id,
            String publicId,
            String title,
            String slug,
            String status,
            boolean featured,
            String mainImagePath,
            Long minPriceCents,
            Integer variantCount,
            Integer totalAvailable,
            LocalDateTime createdAt) {}
}
