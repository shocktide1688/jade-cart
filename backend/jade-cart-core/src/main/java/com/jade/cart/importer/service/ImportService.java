package com.jade.cart.importer.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jade.cart.catalog.entity.Inventory;
import com.jade.cart.catalog.entity.Product;
import com.jade.cart.catalog.entity.ProductImage;
import com.jade.cart.catalog.entity.Variant;
import com.jade.cart.importer.entity.ImportJob;
import com.jade.cart.importer.entity.Redirect;
import com.jade.cart.media.service.MediaService;
import com.jade.common.exception.BizException;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;

/**
 * CSV 商品导入 (kartwo 0012~0014 对齐): 干跑预览 (行级错误, 零写入) → 确认执行 (source_sha256 幂等); Shopify 格式含远程图片下载与
 * 301 重定向
 *
 * <p>generic 列: title,slug,description,sku,price,stock,category (一行一商品) shopify 列:
 * Handle,Title,Body (HTML),Variant SKU,Variant Price,Variant Inventory Qty,Image Src (Handle 分组,
 * 一行一变体)
 */
@ApplicationScoped
public class ImportService {

    @Inject MediaService mediaService;

    private final HttpClient http =
            HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build();
    private final ObjectMapper mapper = new ObjectMapper();

    // ---------- 预览 ----------

    @Transactional
    public ImportJob preview(String filename, String format, byte[] csv) {
        String sha256 = sha256(csv);
        ImportJob existing = ImportJob.bySha256(sha256).orElse(null);
        if (existing != null) {
            return existing; // 同文件幂等: 直接返回既有任务
        }

        List<Map<String, Object>> products = new ArrayList<>();
        List<Map<String, Object>> errors = new ArrayList<>();
        int totalRows = 0;

        try {
            List<CSVRecord> all =
                    CSVFormat.DEFAULT
                            .builder()
                            .setIgnoreEmptyLines(true)
                            .build()
                            .parse(
                                    new java.io.StringReader(
                                            new String(csv, StandardCharsets.UTF_8)))
                            .getRecords();
            if (all.isEmpty()) {
                throw new BizException("CSV 为空");
            }
            // 小写表头索引 (大小写不敏感列匹配)
            Map<String, Integer> header = new LinkedHashMap<>();
            CSVRecord headerRow = all.get(0);
            for (int i = 0; i < headerRow.size(); i++) {
                header.put(headerRow.get(i).trim().toLowerCase(Locale.ROOT), i);
            }
            List<CSVRecord> records = all.subList(1, all.size());

            var parsed =
                    "shopify".equalsIgnoreCase(format)
                            ? parseShopify(header, records)
                            : parseGeneric(header, records);
            products = parsed.products();
            errors = parsed.errors();
            totalRows = records.size();
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            BizException ex = new BizException("CSV 解析失败: " + e.getMessage());
            ex.initCause(e); // PMD PreserveStackTrace: 保留原始异常链
            throw ex;
        }

        ImportJob job = new ImportJob();
        job.filename = filename;
        job.sourceFormat = "shopify".equalsIgnoreCase(format) ? "shopify" : "generic";
        job.sourceSha256 = sha256;
        job.totalRows = totalRows;
        job.errorRows = errors.size();
        job.productCount = products.size();

        Map<String, Object> preview = new LinkedHashMap<>();
        preview.put("products", products);
        preview.put("errors", errors);
        try {
            job.previewJson = mapper.writeValueAsString(preview);
        } catch (Exception e) {
            BizException ex = new BizException("预览序列化失败");
            ex.initCause(e);
            throw ex;
        }
        job.persist();
        return job;
    }

    // ---------- 确认执行 ----------

    @Transactional
    public ImportJob confirm(String publicId) {
        ImportJob job = ImportJob.find("publicId", publicId).firstResult();
        if (job == null) {
            throw new BizException("导入任务不存在");
        }
        if (ImportJob.STATUS_CONFIRMED.equals(job.status)) {
            throw new BizException("该文件已导入过 (幂等保护), 不可重复执行");
        }
        if (job.previewJson == null) {
            throw new BizException("任务缺少预览数据, 请重新上传");
        }

        int imported = 0;
        int failed = 0;
        try {
            JsonNode products = mapper.readTree(job.previewJson).path("products");
            for (JsonNode p : products) {
                try {
                    importOne(p, "shopify".equals(job.sourceFormat));
                    imported++;
                } catch (Exception e) {
                    failed++;
                    Log.errorf(e, "import row failed: %s", p.path("title").asText());
                }
            }
        } catch (Exception e) {
            job.status = ImportJob.STATUS_FAILED;
            job.persist();
            BizException ex = new BizException("导入执行失败: " + e.getMessage());
            ex.initCause(e);
            throw ex;
        }

        job.status = ImportJob.STATUS_CONFIRMED;
        job.importedCount = imported;
        job.confirmedAt = LocalDateTime.now();
        job.persist();
        Log.infof("import confirmed: job=%s imported=%d failed=%d", job.publicId, imported, failed);
        return job;
    }

    // ---------- 解析 ----------

    private record Parsed(List<Map<String, Object>> products, List<Map<String, Object>> errors) {}

    /** generic: title,slug,description,sku,price,stock,category — 一行一商品 */
    private Parsed parseGeneric(Map<String, Integer> header, List<CSVRecord> records) {
        List<Map<String, Object>> products = new ArrayList<>();
        List<Map<String, Object>> errors = new ArrayList<>();
        int rowNo = 1;
        for (CSVRecord r : records) {
            rowNo++;
            String title = cell(header, r, "title");
            String sku = cell(header, r, "sku");
            if (title.isBlank()) {
                errors.add(error(rowNo, "title 不能为空"));
                continue;
            }
            long price = parsePrice(cell(header, r, "price"), rowNo, errors);
            if (price < 0) {
                continue;
            }
            Map<String, Object> p = new LinkedHashMap<>();
            p.put("title", title);
            p.put("slug", cell(header, r, "slug"));
            p.put("description", cell(header, r, "description"));
            p.put("sku", sku);
            p.put("priceCents", price);
            p.put("stock", parseInt(cell(header, r, "stock"), 0));
            p.put("category", cell(header, r, "category"));
            p.put("images", List.of());
            products.add(p);
        }
        return new Parsed(products, errors);
    }

    /** shopify: Handle 分组, 一行一变体; 首行取 Title/Body; Image Src 首图 */
    private Parsed parseShopify(Map<String, Integer> header, List<CSVRecord> records) {
        List<Map<String, Object>> products = new ArrayList<>();
        List<Map<String, Object>> errors = new ArrayList<>();
        Map<String, Map<String, Object>> byHandle = new LinkedHashMap<>();
        int rowNo = 1;
        for (CSVRecord r : records) {
            rowNo++;
            String handle = cell(header, r, "handle");
            if (handle.isBlank()) {
                errors.add(error(rowNo, "Handle 不能为空"));
                continue;
            }
            Map<String, Object> p = byHandle.get(handle);
            if (p == null) {
                String title = cell(header, r, "title");
                if (title.isBlank()) {
                    errors.add(error(rowNo, "Title 不能为空 (handle=" + handle + ")"));
                    continue;
                }
                p = new LinkedHashMap<>();
                p.put("title", title);
                p.put("slug", handle); // shopify handle 直接做 slug
                p.put("description", cell(header, r, "body (html)"));
                p.put("handle", handle);
                p.put("variants", new ArrayList<Map<String, Object>>());
                String image = cell(header, r, "image src");
                p.put("images", new ArrayList<String>());
                if (!image.isBlank()) {
                    ((List<String>) p.get("images")).add(image);
                }
                byHandle.put(handle, p);
                products.add(p);
            }
            String sku = cell(header, r, "variant sku");
            String priceStr = cell(header, r, "variant price");
            if (sku.isBlank() && priceStr.isBlank()) {
                // 纯图片/补充行: 收集相册图 (Shopify 多图导出格式)
                String extra = cell(header, r, "image src");
                if (!extra.isBlank()) {
                    ((List<String>) p.get("images")).add(extra);
                }
                continue;
            }
            long price = parsePrice(priceStr, rowNo, errors);
            if (price < 0) {
                continue;
            }
            Map<String, Object> v = new LinkedHashMap<>();
            v.put("sku", sku);
            v.put("priceCents", price);
            v.put("stock", parseInt(cell(header, r, "variant inventory qty"), 0));
            ((List<Map<String, Object>>) p.get("variants")).add(v);
        }
        return new Parsed(products, errors);
    }

    // ---------- 执行单条 ----------

    private void importOne(JsonNode p, boolean shopify) {
        String slug = p.path("slug").asText("");
        // slug 冲突: 自动加后缀 (导入不中断)
        if (!slug.isBlank() && Product.findBySlug(slug) != null) {
            slug = slug + "-" + UUID.randomUUID().toString().substring(0, 6);
        }
        Product product = new Product();
        product.title = p.path("title").asText("Imported product");
        product.slug =
                slug.isBlank()
                        ? product.title
                                        .toLowerCase(Locale.ROOT)
                                        .replaceAll("[^a-z0-9]+", "-")
                                        .replaceAll("^-+|-+$", "")
                                + "-"
                                + UUID.randomUUID().toString().substring(0, 6)
                        : slug;
        product.description = p.path("description").asText(null);
        product.status = Product.STATUS_ACTIVE;
        product.persist();

        // 变体: shopify 多变体 / generic 单变体
        JsonNode variants = p.path("variants");
        if (variants.isArray() && variants.size() > 0) {
            for (JsonNode v : variants) {
                createVariant(
                        product.id,
                        v.path("sku").asText(null),
                        v.path("priceCents").asLong(0),
                        v.path("stock").asInt(0));
            }
        } else {
            createVariant(
                    product.id,
                    p.path("sku").asText(null),
                    p.path("priceCents").asLong(0),
                    p.path("stock").asInt(0));
        }

        // 主图: 远程图片下载进媒体库 (失败不阻断); 相册图同样入库
        JsonNode images = p.path("images");
        int imagePos = 0;
        if (images.isArray() && images.size() > 0) {
            for (JsonNode imgNode : images) {
                String imageUrl = imgNode.asText("");
                if (imageUrl.isBlank()) {
                    continue;
                }
                try {
                    HttpResponse<byte[]> resp =
                            http.send(
                                    HttpRequest.newBuilder(URI.create(imageUrl))
                                            .timeout(Duration.ofSeconds(30))
                                            .build(),
                                    HttpResponse.BodyHandlers.ofByteArray());
                    if (resp.statusCode() != 200) {
                        continue;
                    }
                    try (InputStream in = new ByteArrayInputStream(resp.body())) {
                        var asset =
                                mediaService.upload(
                                        in,
                                        imageUrl.substring(imageUrl.lastIndexOf('/') + 1),
                                        null);
                        if (imagePos == 0) {
                            product.mainImagePath = asset.path;
                        } else {
                            ProductImage pi = new ProductImage();
                            pi.productId = product.id;
                            pi.path = asset.path;
                            pi.position = imagePos;
                            pi.persist();
                        }
                        imagePos++;
                    }
                } catch (Exception e) {
                    Log.warnf("import image download failed: %s (%s)", imageUrl, e.getMessage());
                }
            }
            product.persist();
        }

        // Shopify 旧 handle → 301 (不覆盖)
        String handle = p.path("handle").asText("");
        if (shopify && !handle.isBlank() && Redirect.byHandle(handle).isEmpty()) {
            Redirect redirect = new Redirect();
            redirect.handle = handle;
            redirect.target = "/products/" + product.slug;
            redirect.persist();
        }
    }

    private void createVariant(Long productId, String sku, long priceCents, int stock) {
        Variant variant = new Variant();
        variant.productId = productId;
        variant.sku =
                (sku == null || sku.isBlank())
                        ? "IMP-"
                                + UUID.randomUUID()
                                        .toString()
                                        .substring(0, 8)
                                        .toUpperCase(Locale.ROOT)
                        : sku;
        if (Variant.findBySku(variant.sku).isPresent()) {
            variant.sku =
                    variant.sku
                            + "-"
                            + UUID.randomUUID().toString().substring(0, 6).toUpperCase(Locale.ROOT);
        }
        variant.priceCents = priceCents;
        variant.persist();

        Inventory inventory = new Inventory();
        inventory.variantId = variant.id;
        inventory.quantity = Math.max(stock, 0);
        inventory.persist();
    }

    // ---------- 内部 ----------

    private String cell(Map<String, Integer> header, CSVRecord r, String col) {
        Integer idx = header.get(col);
        if (idx == null || idx >= r.size()) {
            return "";
        }
        String v = r.get(idx);
        return v == null ? "" : v.trim();
    }

    private long parsePrice(String s, int rowNo, List<Map<String, Object>> errors) {
        if (s == null || s.isBlank()) {
            return 0;
        }
        try {
            String cleaned = s.replaceAll("[$,\\s]", "");
            if (cleaned.contains(".")) {
                return Math.round(Double.parseDouble(cleaned) * 100);
            }
            return Long.parseLong(cleaned); // 整数直接当分
        } catch (Exception e) {
            errors.add(error(rowNo, "价格格式非法: " + s));
            return -1;
        }
    }

    private int parseInt(String s, int def) {
        try {
            return s.isBlank() ? def : Integer.parseInt(s.trim());
        } catch (Exception e) {
            return def;
        }
    }

    private Map<String, Object> error(int row, String message) {
        Map<String, Object> e = new LinkedHashMap<>();
        e.put("row", row);
        e.put("message", message);
        return e;
    }

    private String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
