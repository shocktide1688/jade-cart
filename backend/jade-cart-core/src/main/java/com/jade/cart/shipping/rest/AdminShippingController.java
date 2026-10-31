package com.jade.cart.shipping.rest;

import com.jade.cart.shipping.entity.ShippingZone;
import com.jade.cart.shipping.service.ShippingService;
import com.jade.common.api.R;
import io.quarkus.security.Authenticated;
import jakarta.annotation.security.RolesAllowed;
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
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.List;

/**
 * 运费分区管理 (管理端)
 */
@Path("/api/v1/shipping-zones")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Authenticated
@Tag(name = "运费设置")
public class AdminShippingController {

    @Inject
    ShippingService shippingService;

    public record ZoneReq(String name, String countries, Long rateCents,
                          Long freeThresholdCents, Boolean active) {}

    @GET
    @RolesAllowed("admin")
    @Operation(summary = "运费分区列表")
    public R<List<ShippingZone>> list() {
        return R.ok(shippingService.zones());
    }

    @POST
    @RolesAllowed("admin")
    @Transactional
    @Operation(summary = "创建分区 (countries: 'US,CA' 或 '*' 兜底)")
    public R<ShippingZone> create(ZoneReq req) {
        return R.ok(shippingService.create(req.name(), req.countries(),
                req.rateCents(), req.freeThresholdCents(),
                req.active() == null || req.active()));
    }

    @PUT
    @Path("/{id}")
    @RolesAllowed("admin")
    @Transactional
    @Operation(summary = "更新分区")
    public R<ShippingZone> update(@PathParam("id") Long id, ZoneReq req) {
        return R.ok(shippingService.update(id, req.name(), req.countries(),
                req.rateCents(), req.freeThresholdCents(), req.active()));
    }

    @DELETE
    @Path("/{id}")
    @RolesAllowed("admin")
    @Transactional
    @Operation(summary = "删除分区")
    public R<Void> delete(@PathParam("id") Long id) {
        shippingService.delete(id);
        return R.ok();
    }
}
