package com.jade.cart.trade.payment;

import com.jade.cart.settings.entity.StoreSetting;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Optional;

/**
 * 支付/站点设置读取 (mall_store_setting, 敏感值已加密)
 */
@ApplicationScoped
public class PaymentSettings {

    public static final String K_STRIPE_SECRET = "stripe_secret_key";
    public static final String K_STRIPE_WEBHOOK = "stripe_webhook_secret";
    public static final String K_PAYPAL_ID = "paypal_client_id";
    public static final String K_PAYPAL_SECRET = "paypal_client_secret";
    public static final String K_PAYPAL_ENV = "paypal_env";          // sandbox | live
    public static final String K_PAYPAL_WEBHOOK_ID = "paypal_webhook_id";
    public static final String K_STORE_BASE_URL = "store_base_url";  // 结算回跳地址

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
}
