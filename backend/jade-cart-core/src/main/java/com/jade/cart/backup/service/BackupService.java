package com.jade.cart.backup.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.hibernate.Session;

/**
 * 全量备份 (kartwo backup 对齐): ZIP = manifest + mall_* 表 JSON 快照 + 媒体文件 本地定时 (每日 04:00) + 保留 N 份; 恢复用
 * restore 脚本/未来导入 (P3)
 */
@ApplicationScoped
public class BackupService {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
    private static final List<String> TABLES =
            List.of(
                    "mall_category",
                    "mall_product",
                    "mall_product_option",
                    "mall_product_option_value",
                    "mall_variant",
                    "mall_variant_option_value",
                    "mall_inventory",
                    "mall_product_category",
                    "mall_customer",
                    "mall_cart",
                    "mall_cart_item",
                    "mall_order",
                    "mall_order_item",
                    "mall_webhook_event",
                    "mall_store_setting",
                    "mall_shipping_zone",
                    "mall_shipment",
                    "mall_email_outbox",
                    "mall_media_asset",
                    "mall_content_page",
                    "mall_contact_message",
                    "mall_import_job",
                    "mall_redirect");

    @Inject EntityManager em;

    private final ObjectMapper mapper =
            new ObjectMapper()
                    .enable(SerializationFeature.INDENT_OUTPUT)
                    .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule())
                    .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @ConfigProperty(name = "jade.cart.backup-dir", defaultValue = "./data/backups")
    String backupDir;

    @ConfigProperty(name = "jade.cart.media-dir", defaultValue = "./data/media")
    String mediaDir;

    @ConfigProperty(name = "jade.cart.backup-keep", defaultValue = "7")
    int keep;

    public Path backupRoot() {
        return Path.of(backupDir).toAbsolutePath().normalize();
    }

    @Transactional
    public Path createBackup() throws Exception {
        Path root = backupRoot();
        Files.createDirectories(root);
        String name = "jade-cart-backup-" + LocalDateTime.now().format(FMT) + ".zip";
        Path zipPath = root.resolve(name);

        try (OutputStream fos = Files.newOutputStream(zipPath);
                ZipOutputStream zip = new ZipOutputStream(fos, StandardCharsets.UTF_8)) {

            // ① manifest
            Map<String, Object> manifest = new LinkedHashMap<>();
            manifest.put("app", "jade-cart");
            manifest.put("created_at", LocalDateTime.now().toString());
            manifest.put("tables", TABLES);
            long products = countSafe("mall_product");
            manifest.put("product_count", products);
            putZipEntry(zip, "manifest.json", mapper.writeValueAsBytes(manifest));

            // ② 各表 JSON 快照 (JDBC 通用导出 → 独立于实体, 兼容 PostgreSQL/SQLite)
            for (String table : TABLES) {
                try {
                    putZipEntry(
                            zip,
                            "data/" + table + ".json",
                            mapper.writeValueAsBytes(exportTable(table)));
                } catch (Exception e) {
                    Log.warnf("backup table skipped: %s (%s)", table, e.getMessage());
                }
            }

            // ③ 媒体文件
            Path media = Path.of(mediaDir).toAbsolutePath().normalize();
            if (Files.isDirectory(media)) {
                try (var walk = Files.walk(media)) {
                    walk.filter(Files::isRegularFile)
                            .forEach(
                                    file -> {
                                        try {
                                            zip.putNextEntry(
                                                    new ZipEntry(
                                                            "media/" + media.relativize(file)));
                                            zip.write(Files.readAllBytes(file));
                                            zip.closeEntry();
                                        } catch (Exception e) {
                                            Log.warnf("backup media skipped: %s", file);
                                        }
                                    });
                }
            }
        }

        enforceRetention(root);
        Log.infof("backup created: %s", zipPath);
        return zipPath;
    }

    public List<Map<String, Object>> listBackups() {
        List<Map<String, Object>> out = new ArrayList<>();
        Path root = backupRoot();
        if (!Files.isDirectory(root)) {
            return out;
        }
        try (var stream = Files.list(root)) {
            stream.filter(f -> f.getFileName().toString().endsWith(".zip"))
                    .sorted((a, b) -> b.compareTo(a))
                    .forEach(
                            f -> {
                                Map<String, Object> m = new LinkedHashMap<>();
                                m.put("filename", f.getFileName().toString());
                                try {
                                    m.put("sizeBytes", Files.size(f));
                                } catch (Exception e) {
                                    m.put("sizeBytes", 0);
                                }
                                out.add(m);
                            });
        } catch (Exception ignored) {
        }
        return out;
    }

    public Path resolve(String filename) {
        if (!filename.endsWith(".zip") || filename.contains("/") || filename.contains("\\")) {
            return null;
        }
        Path target = backupRoot().resolve(filename).normalize();
        if (!target.startsWith(backupRoot()) || !Files.isRegularFile(target)) {
            return null;
        }
        return target;
    }

    public boolean delete(String filename) {
        Path target = resolve(filename);
        if (target == null) {
            return false;
        }
        try {
            Files.deleteIfExists(target);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /** 保留最近 keep 份 */
    private void enforceRetention(Path root) {
        try (var stream = Files.list(root)) {
            List<Path> zips =
                    stream.filter(f -> f.getFileName().toString().endsWith(".zip"))
                            .sorted((a, b) -> b.compareTo(a))
                            .toList();
            for (int i = keep; i < zips.size(); i++) {
                Files.deleteIfExists(zips.get(i));
                Log.infof("backup rotated out: %s", zips.get(i).getFileName());
            }
        } catch (Exception e) {
            Log.warnf("backup retention failed: %s", e.getMessage());
        }
    }

    private long countSafe(String table) {
        try {
            return ((Number)
                            em.createNativeQuery("SELECT count(*) FROM " + table).getSingleResult())
                    .longValue();
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * 通用表导出: SELECT * + ResultSetMetaData 拼行对象。不依赖 PostgreSQL 专有 to_jsonb 函数, SQLite
     * (QUARKUS_PROFILE=sqlite) 下同样可用; 产出干净 JSON 数组 (替代原先 jsonb 字符串再编码的双层形式)
     */
    @SuppressWarnings(
            "PMD.CloseResource") // Session 由 CDI 管理的 EntityManager unwrap 而来, 生命周期归容器, 不可关闭
    private List<Map<String, Object>> exportTable(String table) {
        Session session = em.unwrap(Session.class);
        return session.doReturningWork(
                connection -> {
                    List<Map<String, Object>> rows = new ArrayList<>();
                    try (Statement st = connection.createStatement();
                            ResultSet rs = st.executeQuery("SELECT * FROM " + table)) {
                        int cols = rs.getMetaData().getColumnCount();
                        while (rs.next()) {
                            Map<String, Object> row = new LinkedHashMap<>();
                            for (int i = 1; i <= cols; i++) {
                                row.put(rs.getMetaData().getColumnLabel(i), rs.getObject(i));
                            }
                            rows.add(row);
                        }
                    }
                    return rows;
                });
    }

    private void putZipEntry(ZipOutputStream zip, String name, byte[] bytes) throws Exception {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(bytes);
        zip.closeEntry();
    }
}
