package com.jade.cart.shipping.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 运费分区: 国家列表(逗号分隔 ISO2, * = 兜底) + 固定运费 + 满额免邮
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Entity
@Table(name = "mall_shipping_zone")
public class ShippingZone extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(nullable = false, length = 100)
    public String name;

    /** "US,CA" 或 "*" (兜底) */
    @Column(nullable = false, length = 500)
    public String countries = "*";

    @Column(name = "rate_cents", nullable = false)
    public Long rateCents = 0L;

    /** 满额免邮门槛, 0 = 不启用 */
    @Column(name = "free_threshold_cents", nullable = false)
    public Long freeThresholdCents = 0L;

    @Column(nullable = false)
    public boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    public LocalDateTime createdAt;

    public static List<ShippingZone> listActive() {
        return list("active = true order by id asc");
    }

    public boolean matches(String country) {
        if ("*".equals(countries)) {
            return true;
        }
        for (String c : countries.split(",")) {
            if (c.trim().equalsIgnoreCase(country)) {
                return true;
            }
        }
        return false;
    }
}
