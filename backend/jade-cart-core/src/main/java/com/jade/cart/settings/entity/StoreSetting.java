package com.jade.cart.settings.entity;

import com.jade.security.annotation.Encrypted;
import com.jade.security.crypto.EncryptedStringConverter;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
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
 * 商城设置 KV (值 AES-256-GCM 加密存储, 与 jade-mail/LDAP 密码同机制)
 *
 * <p>P0 键: store_name / currency / support_email / announcement / mock_payment_enabled P1 键:
 * stripe_secret_key / paypal_client_secret 等 (加密, 界面掩码)
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Entity
@Table(name = "mall_store_setting")
public class StoreSetting extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(nullable = false, unique = true, length = 100)
    public String key;

    /** 值加密存储 (@Encrypted + AES-GCM converter) */
    @Encrypted
    @Convert(converter = EncryptedStringConverter.class)
    @Column(columnDefinition = "TEXT")
    public String value;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    public LocalDateTime updatedAt;

    public static Optional<String> getValue(String key) {
        return StoreSetting.<StoreSetting>find("key", key).firstResultOptional().map(s -> s.value);
    }

    public static void put(String key, String value) {
        StoreSetting setting = find("key", key).firstResult();
        if (setting == null) {
            setting = new StoreSetting();
            setting.key = key;
        }
        setting.value = value == null ? "" : value;
        setting.persist();
    }

    public static Map<String, String> allAsMap() {
        Map<String, String> map = new HashMap<>();
        List<StoreSetting> all = listAll();
        for (StoreSetting s : all) {
            map.put(s.key, s.value == null ? "" : s.value);
        }
        return map;
    }
}
