package com.jade.cart.media.service;

import com.jade.cart.media.entity.MediaAsset;
import com.jade.common.exception.BizException;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import javax.imageio.ImageIO;
import net.coobird.thumbnailator.Thumbnails;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * 媒体库: 上传校验 (magic bytes/大小/磁盘护栏) → 内容哈希去重 → 原图落盘 → 多尺寸派生图
 *
 * <p>说明: 派生图保持原格式 (JPEG/PNG); WebP 无 JDK 编码器, 原样存储不派生。
 */
@ApplicationScoped
public class MediaService {

    public static final long MAX_SIZE = 10L * 1024 * 1024; // 10 MiB
    public static final long MIN_USABLE_DISK = 200L * 1024 * 1024; // 200 MiB 护栏

    /** 派生图尺寸档位 (上传时生成, 最长边缩到该值) */
    private static final int[] DERIVED_SIZES = {200, 800, 1600};

    @ConfigProperty(name = "jade.cart.media-dir", defaultValue = "./data/media")
    String mediaDir;

    public Path root() {
        return Path.of(mediaDir).toAbsolutePath().normalize();
    }

    @Transactional
    public MediaAsset upload(InputStream in, String filename, String declaredType) {
        byte[] bytes;
        try {
            bytes = in.readAllBytes();
        } catch (Exception e) {
            BizException ex = new BizException("读取上传内容失败");
            ex.initCause(e);
            throw ex;
        }
        if (bytes.length == 0) {
            throw new BizException("文件为空");
        }
        if (bytes.length > MAX_SIZE) {
            throw new BizException("文件超过 10MiB 上限");
        }
        try {
            Files.createDirectories(root());
        } catch (Exception e) {
            BizException ex = new BizException("媒体目录创建失败: " + e.getMessage());
            ex.initCause(e);
            throw ex;
        }
        // 目录不存在时 getUsableSpace() 返回 0 → 先建目录, 仍为 0 才是真没空间
        long usable = root().toFile().getUsableSpace();
        if (usable > 0 && usable < MIN_USABLE_DISK) {
            throw new BizException("磁盘空间不足 (可用 < 200MiB), 暂停上传");
        }

        String type = sniffType(bytes);
        if (type == null) {
            throw new BizException("仅支持 JPEG / PNG / WebP 图片");
        }

        String hash = sha256Hex(bytes);
        MediaAsset existing = MediaAsset.byHash(hash).orElse(null);
        if (existing != null) {
            return existing; // 内容去重
        }

        String ext =
                switch (type) {
                    case "image/jpeg" -> ".jpg";
                    case "image/png" -> ".png";
                    default -> ".webp";
                };
        String safeName =
                (filename == null || filename.isBlank() ? "image" : filename)
                        .replaceAll("[^a-zA-Z0-9._-]", "_");
        try {
            Path original = root().resolve(hash + ext);
            Files.createDirectories(original.getParent());
            Files.write(original, bytes);

            int width = 0;
            int height = 0;
            if (!"image/webp".equals(type)) {
                // 尽力而为: 尺寸解析/派生图失败不阻断原图存储
                try {
                    BufferedImage img = ImageIO.read(new ByteArrayInputStream(bytes));
                    if (img != null) {
                        width = img.getWidth();
                        height = img.getHeight();
                        derive(original, img, type, 200);
                        derive(original, img, type, 800);
                        if (Math.max(width, height) > 1600) {
                            derive(original, img, type, 1600);
                        }
                    }
                } catch (Exception e) {
                    Log.warnf(e, "media derive failed (original stored): %s", hash);
                }
            }

            MediaAsset asset = new MediaAsset();
            asset.filename = safeName;
            asset.contentHash = hash;
            asset.contentType = type;
            asset.sizeBytes = (long) bytes.length;
            asset.width = width;
            asset.height = height;
            asset.path = root().relativize(original).toString().replace('\\', '/');
            asset.persist();
            Log.infof(
                    "media uploaded: %s (%d bytes, %dx%d)",
                    asset.path, bytes.length, width, height);
            return asset;
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            BizException ex = new BizException("保存媒体失败: " + e.getMessage());
            ex.initCause(e);
            throw ex;
        }
    }

    /** 读取媒体文件 (相对路径, 防目录穿越) */
    public Path resolve(String relativePath) {
        Path root = root();
        Path target = root.resolve(relativePath).normalize();
        if (!target.startsWith(root) || !Files.isRegularFile(target)) {
            return null;
        }
        return target;
    }

    /**
     * 按目标宽度读取媒体: 优先命中已生成的派生图 (200/800/1600), 否则回落原图。
     *
     * <p>上传时已按 {hash}_{size}.jpg 生成多尺寸派生图; 页面用 ?w=200 取小图, 避免为 76px 缩略图下载 896×1200 原图 (图集 10 张可省
     * ~90% 带宽)。
     */
    public Path resolveWithWidth(String relativePath, Integer width) {
        Path original = resolve(relativePath);
        if (original == null || width == null || width <= 0) {
            return original;
        }
        int size = pickDerivedSize(width);
        Path derived = Path.of(original.toString().replaceFirst("(\\.[a-z]+)$", "_" + size + "$1"));
        // 派生图未生成 (webp / 小图 / 旧数据) → 回落原图
        return Files.isRegularFile(derived) ? derived : original;
    }

    /** 取不小于目标宽度的最小派生档位 (无更大档则取最大档) */
    private int pickDerivedSize(int width) {
        if (width <= DERIVED_SIZES[0]) {
            return DERIVED_SIZES[0];
        }
        if (width <= DERIVED_SIZES[1]) {
            return DERIVED_SIZES[1];
        }
        if (width <= DERIVED_SIZES[2]) {
            return DERIVED_SIZES[2];
        }
        return DERIVED_SIZES[2];
    }

    // ---------- 内部 ----------

    /** 派生图: 最长边缩到 size (只缩不放), 命名 {hash}_{size}.jpg */
    private void derive(Path original, BufferedImage img, String type, int size) throws Exception {
        if (Math.max(img.getWidth(), img.getHeight()) <= size) {
            return;
        }
        Path target = Path.of(original.toString().replaceFirst("(\\.[a-z]+)$", "_" + size + "$1"));
        if (Files.exists(target)) {
            return;
        }
        Thumbnails.of(img)
                .size(size, size)
                .keepAspectRatio(true)
                .outputFormat("jpg".equals(extOf(type)) ? "jpg" : "png")
                .toFile(target.toFile());
    }

    private String extOf(String type) {
        return "image/png".equals(type) ? "png" : "jpg";
    }

    /** magic bytes 嗅探, 非 null = 支持的类型 */
    private String sniffType(byte[] b) {
        if (b.length >= 12 && (b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G') {
            return "image/png";
        }
        if (b.length >= 3
                && (b[0] & 0xFF) == 0xFF
                && (b[1] & 0xFF) == 0xD8
                && (b[2] & 0xFF) == 0xFF) {
            return "image/jpeg";
        }
        if (b.length >= 12
                && b[0] == 'R'
                && b[1] == 'I'
                && b[2] == 'F'
                && b[3] == 'F'
                && b[8] == 'W'
                && b[9] == 'E'
                && b[10] == 'B'
                && b[11] == 'P') {
            return "image/webp";
        }
        return null;
    }

    private String sha256Hex(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
