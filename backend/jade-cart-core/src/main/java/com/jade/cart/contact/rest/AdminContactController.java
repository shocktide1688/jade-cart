package com.jade.cart.contact.rest;

import com.jade.cart.contact.entity.ContactMessage;
import com.jade.cart.contact.service.ContactService;
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

/** 客服留言管理 (管理端) */
@Path("/api/v1/contact-messages")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Authenticated
@Tag(name = "客服留言")
public class AdminContactController {

    @Inject ContactService contactService;

    @GET
    @RolesAllowed("admin")
    @Operation(summary = "留言分页")
    public R<PageResult<ContactMessage>> list(
            @QueryParam("page") Integer page,
            @QueryParam("size") Integer size,
            @QueryParam("status") String status) {
        int p = page == null ? 1 : page;
        int s = size == null ? 10 : size;
        List<ContactMessage> records = contactService.page(status, p, s);
        return R.ok(PageResult.of(records, contactService.count(status), p, s));
    }

    @POST
    @Path("/{publicId}/{action}")
    @RolesAllowed("admin")
    @Transactional
    @Operation(summary = "状态流转 (processing / closed / reopen)")
    public R<ContactMessage> transition(
            @PathParam("publicId") String publicId, @PathParam("action") String action) {
        String to =
                switch (action) {
                    case "processing" -> ContactMessage.STATUS_PROCESSING;
                    case "closed" -> ContactMessage.STATUS_CLOSED;
                    case "reopen" -> ContactMessage.STATUS_NEW;
                    default -> null;
                };
        if (to == null) {
            return R.fail(400, "非法操作");
        }
        return R.ok(contactService.transition(publicId, to));
    }
}
