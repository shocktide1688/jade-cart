package com.jade.cart.sysconfig.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * 系统配置 KV (明文存储, 仅非敏感配置; 密钥类请走 StoreSetting, AES-GCM 加密)
 *
 * <p>键: app.title (管理端左上角标题)
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Entity
@Table(name = "sys_app_config")
public class AppConfig extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(nullable = false, unique = true, length = 100)
    public String key;

    @Column(columnDefinition = "TEXT")
    public String value;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    public LocalDateTime updatedAt;

    public static Optional<String> getValue(String key) {
        return AppConfig.<AppConfig>find("key", key).firstResultOptional().map(c -> c.value);
    }

    public static void put(String key, String value) {
        AppConfig config = find("key", key).firstResult();
        if (config == null) {
            config = new AppConfig();
            config.key = key;
        }
        config.value = value == null ? "" : value;
        config.persist();
    }

    public static Map<String, String> allAsMap() {
        Map<String, String> map = new HashMap<>();
        List<AppConfig> all = listAll();
        for (AppConfig c : all) {
            map.put(c.key, c.value == null ? "" : c.value);
        }
        return map;
    }
}
