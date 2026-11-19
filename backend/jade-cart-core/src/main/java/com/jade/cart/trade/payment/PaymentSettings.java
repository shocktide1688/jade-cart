package com.jade.cart.trade.payment;

import com.jade.cart.settings.entity.StoreSetting;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** 支付/站点设置读取 (mall_store_setting, 敏感值已加密) */
@ApplicationScoped
public class PaymentSettings {

    public static final String K_STRIPE_SECRET = "stripe_secret_key";
    public static final String K_STRIPE_WEBHOOK = "stripe_webhook_secret";
    public static final String K_PAYPAL_ID = "paypal_client_id";
    public static final String K_PAYPAL_SECRET = "paypal_client_secret";
    public static final String K_PAYPAL_ENV = "paypal_env"; // sandbox | live
    public static final String K_PAYPAL_WEBHOOK_ID = "paypal_webhook_id";
    public static final String K_STORE_BASE_URL = "store_base_url"; // 结算回跳地址
    public static final String K_STRIPE_METHODS = "stripe_payment_methods"; // 按市场路由支付方式

    /** 仅 EUR 可用的 Stripe 方式 (非 EUR 订单自动剔除, 避免 Stripe 报错) */
    private static final Set<String> EUR_ONLY =
            Set.of("ideal", "bancontact", "sofort", "giropay", "eps", "p24", "sepa_debit");

    public Optional<String> get(String key) {
        return StoreSetting.getValue(key).filter(v -> !v.isBlank());
    }

    public Optional<String> stripeSecret() {
        return get(K_STRIPE_SECRET);
    }

    public Optional<String> stripeWebhookSecret() {
        return get(K_STRIPE_WEBHOOK);
    }

    public boolean stripeConfigured() {
        return stripeSecret().isPresent();
    }

    public boolean paypalConfigured() {
        return get(K_PAYPAL_ID).isPresent() && get(K_PAYPAL_SECRET).isPresent();
    }

    /** sandbox 起步, 显式配置 live 才走生产 */
    public boolean paypalLive() {
        return "live".equalsIgnoreCase(get(K_PAYPAL_ENV).orElse("sandbox"));
    }

    public String paypalApiBase() {
        return paypalLive() ? "https://api-m.paypal.com" : "https://api-m.sandbox.paypal.com";
    }

    /** 结算回跳基址: 店面与浏览器同源 */
    public String storeBaseUrl() {
        return get(K_STORE_BASE_URL).orElse("http://localhost:8090");
    }

    /**
     * Stripe 收银台支付方式路由 (设置 KV `stripe_payment_methods`): 格式
     * "US=card,klarna;EU=card,ideal,bancontact,klarna;DEFAULT=card" 未配置/市场未命中 → 仅 card (Apple Pay /
     * Google Pay 随 card 自动出现); EUR-only 方式在非 EUR 订单自动剔除
     */
    public List<String> stripeMethods(String marketCode, String currency) {
        Map<String, List<String>> byMarket = new LinkedHashMap<>();
        for (String segment : get(K_STRIPE_METHODS).orElse("").split(";")) {
            String[] kv = segment.split("=", 2);
            if (kv.length == 2 && !kv[0].isBlank()) {
                byMarket.put(kv[0].trim().toUpperCase(Locale.ROOT), parseList(kv[1]));
            }
        }
        String code = marketCode == null ? "" : marketCode.trim().toUpperCase(Locale.ROOT);
        List<String> methods =
                byMarket.getOrDefault(code, byMarket.getOrDefault("DEFAULT", List.of("card")));
        boolean eur = "EUR".equalsIgnoreCase(currency);
        List<String> out = new ArrayList<>();
        for (String method : methods) {
            if (!EUR_ONLY.contains(method) || eur) {
                out.add(method);
            }
        }
        if (out.isEmpty()) {
            out.add("card");
        }
        return out;
    }

    private static List<String> parseList(String raw) {
        List<String> out = new ArrayList<>();
        for (String item : raw.split(",")) {
            String v = item.trim().toLowerCase(Locale.ROOT);
            if (!v.isEmpty() && !out.contains(v)) {
                out.add(v);
            }
        }
        return out;
    }
}
