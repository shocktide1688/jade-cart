package com.jade.cart.trade.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单视图 (管理端 + 店面订单查询通用形状)
 */
public record OrderView(
        String publicId,
        String orderNumber,
        String email,
        String shipName,
        String status,
        Long subtotalCents,
        Long shippingCents,
        Long totalCents,
        String currency,
        String shippingCountry,
        String paymentProvider,
        String paymentRef,
        LocalDateTime reservationExpiresAt,
        LocalDateTime paidAt,
        LocalDateTime fulfilledAt,
        LocalDateTime cancelledAt,
        LocalDateTime refundedAt,
        LocalDateTime createdAt,
        List<ItemView> items,
        ShipmentView shipment) {

    public record ShipmentView(String carrier, String trackingNumber, String trackingUrl) {}

    public record ItemView(
            Long variantId,
            String productTitle,
            String variantSpec,
            String sku,
            Long unitPriceCents,
            Integer quantity,
            Long lineTotalCents) {}
}
