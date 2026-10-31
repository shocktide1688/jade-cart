package com.jade.cart.trade.rest;

import com.jade.cart.trade.dto.OrderView;
import com.jade.cart.trade.service.OrderService;
import com.jade.common.api.PageResult;
import com.jade.common.api.R;
import io.quarkus.security.Authenticated;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import java.util.List;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

/** 订单管理 (管理端): 列表/详情/履约/取消/退款 */
@Path("/api/v1/orders")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Authenticated
@Tag(name = "订单管理")
public class AdminOrderController {

    @Inject OrderService orderService;

    @GET
    @RolesAllowed("admin")
    @Operation(summary = "订单分页")
    public R<PageResult<OrderView>> list(
            @QueryParam("page") Integer page,
            @QueryParam("size") Integer size,
            @QueryParam("status") String status,
            @QueryParam("keyword") String keyword) {
        int p = page == null ? 1 : page;
        int s = size == null ? 10 : size;
        List<OrderView> records =
                orderService.pageOrders(status, keyword, p, s).stream()
                        .map(orderService::toView)
                        .toList();
        return R.ok(PageResult.of(records, orderService.countOrders(status, keyword), p, s));
    }

    @GET
    @Path("/{publicId}")
    @RolesAllowed("admin")
    @Operation(summary = "订单详情")
    public R<OrderView> get(@PathParam("publicId") String publicId) {
        com.jade.cart.trade.entity.Order order =
                com.jade.cart.trade.entity.Order.byPublicId(publicId).orElse(null);
        if (order == null) {
            return R.fail(404, "订单不存在");
        }
        return R.ok(orderService.toView(order));
    }

    public record FulfillReq(String carrier, String trackingNumber, String trackingUrl) {}

    @POST
    @Path("/{publicId}/fulfill")
    @RolesAllowed("admin")
    @Transactional
    @Operation(summary = "履约 (PAID → FULFILLED + 发货追踪 + 邮件入队)")
    public R<OrderView> fulfill(@PathParam("publicId") String publicId, FulfillReq req) {
        return R.ok(
                orderService.toView(
                        orderService.fulfill(
                                publicId,
                                req == null ? null : req.carrier(),
                                req == null ? null : req.trackingNumber(),
                                req == null ? null : req.trackingUrl())));
    }

    @POST
    @Path("/{publicId}/cancel")
    @RolesAllowed("admin")
    @Transactional
    @Operation(summary = "取消 (PENDING → CANCELLED, 释放库存预留)")
    public R<OrderView> cancel(@PathParam("publicId") String publicId) {
        return R.ok(orderService.toView(orderService.cancel(publicId)));
    }

    @POST
    @Path("/{publicId}/refund")
    @RolesAllowed("admin")
    @Transactional
    @Operation(summary = "退款 (PAID/REVIEW → REFUNDED, P0 Mock 直转)")
    public R<OrderView> refund(@PathParam("publicId") String publicId) {
        return R.ok(orderService.toView(orderService.refund(publicId)));
    }
}
