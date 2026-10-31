package com.jade.cart.settings.service;

import com.jade.cart.settings.entity.StoreSetting;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 市场注册表 (kartwo market 对齐, P3-17 最小实现):
 * 配置驱动 `jade.cart.markets` (格式 "CODE:CURRENCY,CODE:CURRENCY"), 店铺当前经营市场存设置 KV `market`
 *
 * 当前: 单市场结账 (order.currency 按经营市场解析);
 * 扩展点: 多市场店面路由 / 按市场选支付方式时, 复用 registry 即可
 */
@ApplicationScoped
public class MarketService {

    public record Market(String code, String currency) {}

    @ConfigProperty(name = "jade.cart.markets", defaultValue = "US:USD")
    String marketsConfig;

    @ConfigProperty(name = "jade.cart.currency", defaultValue = "USD")
    String fallbackCurrency;

    /** 注册表 (有序, 首个为默认) */
    public List<Market> registry() {
        List<Market> out = new ArrayList<>();
        for (String entry : marketsConfig.split(",")) {
            String[] parts = entry.trim().split(":");
            if (parts.length >= 1 && !parts[0].isBlank()) {
                out.add(new Market(parts[0].trim().toUpperCase(),
                        (parts.length > 1 ? parts[1] : fallbackCurrency).trim().toUpperCase()));
            }
        }
        if (out.isEmpty()) {
            out.add(new Market("US", fallbackCurrency));
        }
        return out;
    }

    /** 店铺当前经营市场 (设置 KV `market`, 未配置 = 注册表首个) */
    public Market current() {
        Optional<String> configured = StoreSetting.getValue("market")
                .map(m -> m.trim().toUpperCase());
        if (configured.isPresent()) {
            Optional<Market> hit = registry().stream()
                    .filter(m -> m.code().equals(configured.get())).findFirst();
            if (hit.isPresent()) {
                return hit.get();
            }
        }
        return registry().get(0);
    }

    /** 结账币种 = 经营市场币种 */
    public String currency() {
        return current().currency();
    }

    /** 市场视图 (管理端/店面下拉) */
    public List<Map<String, Object>> view() {
        Market current = current();
        List<Map<String, Object>> out = new ArrayList<>();
        for (Market m : registry()) {
            Map<String, Object> v = new LinkedHashMap<>();
            v.put("code", m.code());
            v.put("currency", m.currency());
            v.put("current", m.code().equals(current.code()));
            out.add(v);
        }
        return out;
    }
}
