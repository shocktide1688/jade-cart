package com.jade.cart.mail.service;

import com.jade.cart.mail.entity.EmailOutbox;
import com.jade.cart.trade.entity.Order;
import com.jade.cart.trade.entity.OrderItem;
import com.jade.mail.service.MailService;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 邮件 outbox: 业务事务内 enqueue → worker 异步投递 (SMTP 用 jade-mail 存库配置) (order_id, kind) 唯一约束 =
 * 与业务同事务的天然幂等
 */
@ApplicationScoped
public class OutboxService {

    public static final String KIND_ORDER_CONFIRM = "order_confirm";
    public static final String KIND_PAYMENT_CONFIRM = "payment_confirm";
    public static final String KIND_SHIPPED = "shipped";

    @Inject MailService mailService;

    // ---------- 业务侧入队 (调用方需在事务内) ----------

    @Transactional
    public void enqueueOrderConfirm(Order order) {
        if (contains(order.id, KIND_ORDER_CONFIRM)) {
            return;
        }
        StringBuilder items = new StringBuilder();
        for (OrderItem line : order.items()) {
            items.append(
                    tr(
                            "Spec",
                            line.productTitle,
                            line.variantSpec,
                            line.sku,
                            money(line.unitPriceCents),
                            String.valueOf(line.quantity),
                            money(line.lineTotalCents)));
        }
        String html =
                layout(
                        "Order " + order.orderNumber + " confirmed",
                        "<p>Hi"
                                + (order.shipName == null ? "" : " " + esc(order.shipName))
                                + ",</p>"
                                + "<p>Thanks for your order <strong>"
                                + order.orderNumber
                                + "</strong>. "
                                + "Unpaid orders hold stock for 30 minutes.</p>"
                                + table(
                                        "Item",
                                        "Spec",
                                        "SKU",
                                        "Price",
                                        "Qty",
                                        "Subtotal",
                                        items.toString())
                                + "<p style=\"text-align:right\"><strong>Total: "
                                + money(order.totalCents)
                                + " "
                                + order.currency
                                + "</strong></p>"
                                + "<p><a href=\"/orders/"
                                + order.publicId
                                + "\">View order status</a></p>");
        enqueue(order, KIND_ORDER_CONFIRM, "Order " + order.orderNumber + " confirmed", html);
    }

    @Transactional
    public void enqueuePaymentConfirm(Order order) {
        if (contains(order.id, KIND_PAYMENT_CONFIRM)) {
            return;
        }
        String html =
                layout(
                        "Payment received — " + order.orderNumber,
                        "<p>Thanks! We received your payment of <strong>"
                                + money(order.totalCents)
                                + " "
                                + order.currency
                                + "</strong> for order <strong>"
                                + order.orderNumber
                                + "</strong>.</p>"
                                + "<p>We'll notify you when it ships.</p>");
        enqueue(order, KIND_PAYMENT_CONFIRM, "Payment received — " + order.orderNumber, html);
    }

    @Transactional
    public void enqueueShipped(
            Order order, String carrier, String trackingNumber, String trackingUrl) {
        if (contains(order.id, KIND_SHIPPED)) {
            return;
        }
        String track =
                trackingUrl == null || trackingUrl.isBlank()
                        ? (trackingNumber == null
                                ? ""
                                : "<p>Tracking number: <strong>"
                                        + esc(trackingNumber)
                                        + "</strong></p>")
                        : "<p><a href=\""
                                + esc(trackingUrl)
                                + "\">Track your package</a> ("
                                + esc(trackingNumber == null ? "" : trackingNumber)
                                + ")</p>";
        String html =
                layout(
                        "Your order " + order.orderNumber + " has shipped",
                        "<p>Good news! Order <strong>"
                                + order.orderNumber
                                + "</strong> is on its way"
                                + (carrier == null ? "." : " via " + esc(carrier) + ".")
                                + "</p>"
                                + track);
        enqueue(order, KIND_SHIPPED, "Your order " + order.orderNumber + " has shipped", html);
    }

    // ---------- worker 投递 ----------

    /** 投递一批到期消息; 返回处理条数。调用方 (定时任务) 无需事务 — 每条消息独立事务。 */
    public int flushDue(int limit) {
        List<EmailOutbox> due = EmailOutbox.listDue(limit);
        for (EmailOutbox msg : due) {
            deliverOne(msg.id);
        }
        return due.size();
    }

    @Transactional
    public void deliverOne(Long id) {
        EmailOutbox msg = EmailOutbox.findById(id);
        if (msg == null
                || EmailOutbox.STATUS_SENT.equals(msg.status)
                || EmailOutbox.STATUS_DEAD.equals(msg.status)) {
            return;
        }
        try {
            mailService.sendHtml(msg.recipient, msg.subject, msg.bodyHtml);
            msg.status = EmailOutbox.STATUS_SENT;
            msg.sentAt = LocalDateTime.now();
            msg.lastError = null;
        } catch (Exception e) {
            msg.attempts += 1;
            msg.lastError = truncate(e.getMessage(), 500);
            if (msg.attempts >= EmailOutbox.MAX_ATTEMPTS) {
                msg.status = EmailOutbox.STATUS_DEAD;
                Log.errorf(
                        e,
                        "email dead-letter: id=%s kind=%s to=%s",
                        msg.id,
                        msg.kind,
                        msg.recipient);
            } else {
                msg.status = EmailOutbox.STATUS_FAILED;
                long backoff =
                        EmailOutbox.BACKOFF_MINUTES[
                                Math.min(msg.attempts - 1, EmailOutbox.BACKOFF_MINUTES.length - 1)];
                msg.nextAttemptAt = LocalDateTime.now().plusMinutes(backoff);
            }
        }
        msg.persist();
    }

    // ---------- 内部 ----------

    private boolean contains(Long orderId, String kind) {
        return orderId != null
                && EmailOutbox.count("orderId = ?1 and kind = ?2", orderId, kind) > 0;
    }

    private void enqueue(Order order, String kind, String subject, String html) {
        EmailOutbox msg = new EmailOutbox();
        msg.orderId = order.id;
        msg.kind = kind;
        msg.recipient = order.email;
        msg.subject = subject;
        msg.bodyHtml = html;
        msg.persist();
    }

    private String layout(String title, String content) {
        return """
                <div style="font-family:-apple-system,Segoe UI,Roboto,Arial,sans-serif;max-width:560px;margin:0 auto;color:#1a1a1a">
                  <h2 style="font-size:18px">%s</h2>
                  %s
                  <p style="color:#767676;font-size:12px;margin-top:24px">Jade Cart Store · this is an automated message</p>
                </div>
                """
                .formatted(esc(title), content);
    }

    private String table(String... headersAndRows) {
        StringBuilder sb = new StringBuilder();
        // headersAndRows: 6 个表头 + 已拼好的行 html
        sb.append("<table style=\"width:100%;border-collapse:collapse;font-size:13px\">");
        sb.append("<tr>");
        for (int i = 0; i < 6; i++) {
            sb.append("<th style=\"text-align:left;border-bottom:1px solid #e6e6e6;padding:6px\">")
                    .append(headersAndRows[i])
                    .append("</th>");
        }
        sb.append("</tr>").append(headersAndRows[6]).append("</table>");
        return sb.toString();
    }

    private String tr(String... cells) {
        StringBuilder sb = new StringBuilder("<tr>");
        for (String c : cells) {
            sb.append("<td style=\"border-bottom:1px solid #f0f0f0;padding:6px\">")
                    .append(esc(c))
                    .append("</td>");
        }
        sb.append("</tr>");
        return sb.toString();
    }

    private String money(long cents) {
        return "$" + (cents / 100) + "." + String.format("%02d", Math.abs(cents % 100));
    }

    private String esc(String s) {
        return s == null
                ? ""
                : s.replace("&", "&amp;")
                        .replace("<", "&lt;")
                        .replace(">", "&gt;")
                        .replace("\"", "&quot;");
    }

    private String truncate(String s, int max) {
        if (s == null) {
            return null;
        }
        return s.length() <= max ? s : s.substring(0, max);
    }
}
