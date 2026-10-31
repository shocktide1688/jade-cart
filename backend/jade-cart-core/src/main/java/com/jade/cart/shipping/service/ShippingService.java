package com.jade.cart.shipping.service;

import com.jade.cart.shipping.entity.ShippingZone;
import com.jade.common.exception.BizException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Locale;

/** 运费分区: 结账时按收货国家匹配 (先配置的具体国家优先, * 兜底最后) */
@ApplicationScoped
public class ShippingService {

    public List<ShippingZone> zones() {
        return ShippingZone.list("1=1 order by id asc");
    }

    @Transactional
    public ShippingZone create(
            String name,
            String countries,
            Long rateCents,
            Long freeThresholdCents,
            boolean active) {
        if (name == null || name.isBlank()) {
            throw new BizException("分区名称不能为空");
        }
        ShippingZone zone = new ShippingZone();
        zone.name = name;
        zone.countries = normalize(countries);
        zone.rateCents = rateCents == null ? 0 : rateCents;
        zone.freeThresholdCents = freeThresholdCents == null ? 0 : freeThresholdCents;
        zone.active = active;
        zone.persist();
        return zone;
    }

    @Transactional
    public ShippingZone update(
            Long id,
            String name,
            String countries,
            Long rateCents,
            Long freeThresholdCents,
            Boolean active) {
        ShippingZone zone = ShippingZone.findById(id);
        if (zone == null) {
            throw new BizException("运费分区不存在");
        }
        if (name != null && !name.isBlank()) {
            zone.name = name;
        }
        if (countries != null && !countries.isBlank()) {
            zone.countries = normalize(countries);
        }
        if (rateCents != null) {
            zone.rateCents = rateCents;
        }
        if (freeThresholdCents != null) {
            zone.freeThresholdCents = freeThresholdCents;
        }
        if (active != null) {
            zone.active = active;
        }
        zone.persist();
        return zone;
    }

    @Transactional
    public void delete(Long id) {
        if (!ShippingZone.deleteById(id)) {
            throw new BizException("运费分区不存在");
        }
    }

    /** 匹配运费: 具体国家命中优先; 仅 * 兜底; 无任何命中 = 免运费 (0) 满额免邮: 门槛 > 0 且小计 >= 门槛时免运费 */
    public long shippingFor(String country, long subtotalCents) {
        if (country == null || country.isBlank()) {
            country = "US";
        }
        ShippingZone wildcard = null;
        for (ShippingZone zone : ShippingZone.listActive()) {
            if ("*".equals(zone.countries)) {
                wildcard = wildcard == null ? zone : wildcard;
                continue;
            }
            if (zone.matches(country)) {
                return applyThreshold(zone, subtotalCents);
            }
        }
        if (wildcard != null) {
            return applyThreshold(wildcard, subtotalCents);
        }
        return 0;
    }

    /** 运费快照信息 (店面结账前预览) */
    public record ShippingQuote(long shippingCents, String zoneName) {}

    public ShippingQuote quote(String country, long subtotalCents) {
        if (country == null || country.isBlank()) {
            country = "US";
        }
        ShippingZone wildcard = null;
        for (ShippingZone zone : ShippingZone.listActive()) {
            if ("*".equals(zone.countries)) {
                wildcard = wildcard == null ? zone : wildcard;
                continue;
            }
            if (zone.matches(country)) {
                return new ShippingQuote(applyThreshold(zone, subtotalCents), zone.name);
            }
        }
        if (wildcard != null) {
            return new ShippingQuote(applyThreshold(wildcard, subtotalCents), wildcard.name);
        }
        return new ShippingQuote(0, null);
    }

    private long applyThreshold(ShippingZone zone, long subtotalCents) {
        if (zone.freeThresholdCents > 0 && subtotalCents >= zone.freeThresholdCents) {
            return 0;
        }
        return zone.rateCents;
    }

    private String normalize(String countries) {
        return countries == null
                ? "*"
                : countries.trim().toUpperCase(Locale.ROOT).replaceAll("\\s+", "");
    }
}
