package com.jade.cart.catalog.service;

import com.jade.cart.catalog.entity.Product;
import com.jade.cart.catalog.entity.ProductImage;
import com.jade.common.exception.BizException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 商品图集管理 (mall_product_image): 店面 PDP 的相册 (主图 + 补充图, ?img=N 切换)
 *
 * <p>写入通道此前只有 Shopify CSV 导入与 ERP 推送, 商家无法在后台人工整理图集; 这里补齐人工通道: 列表 / 追加 / 删除 / 排序 / 设为主图。 position 从
 * 0 连续编号, 越小越靠前。
 */
@ApplicationScoped
public class ProductImageService {

    /** 图集列表 (含主图, 主图固定 idx=0) */
    public List<Map<String, Object>> gallery(Long productId) {
        Product product = requireProduct(productId);
        List<Map<String, Object>> out = new ArrayList<>();
        if (product.mainImagePath != null && !product.mainImagePath.isBlank()) {
            out.add(Map.of("idx", 0, "id", 0L, "path", product.mainImagePath, "main", true));
        }
        int idx = out.size();
        for (ProductImage pi : listImages(productId)) {
            Map<String, Object> m = new HashMap<>();
            m.put("idx", idx);
            m.put("id", pi.id);
            m.put("path", pi.path);
            m.put("position", pi.position);
            m.put("main", false);
            out.add(m);
            idx++;
        }
        return out;
    }

    /** 补充图列表 (不含主图, 按 position 排序) */
    public List<ProductImage> listImages(Long productId) {
        return ProductImage.find("productId = ?1 order by position asc, id asc", productId).list();
    }

    /** 追加补充图 (path 为媒体库相对路径) */
    @Transactional
    public ProductImage add(Long productId, String path) {
        requireProduct(productId);
        if (path == null || path.isBlank()) {
            throw new BizException("图片路径不能为空");
        }
        List<ProductImage> existing = listImages(productId);
        ProductImage pi = new ProductImage();
        pi.productId = productId;
        pi.path = path;
        pi.position = existing.isEmpty() ? 0 : existing.get(existing.size() - 1).position + 1;
        pi.persist();
        return pi;
    }

    /** 删除补充图 */
    @Transactional
    public void remove(Long productId, Long imageId) {
        ProductImage pi = ProductImage.findById(imageId);
        if (pi == null || !pi.productId.equals(productId)) {
            throw new BizException("图片不存在");
        }
        pi.delete();
    }

    /** 批量排序 (imageIds 按目标顺序排列; 主图不在此列) */
    @Transactional
    public void reorder(Long productId, List<Long> imageIds) {
        requireProduct(productId);
        if (imageIds == null) {
            return;
        }
        int pos = 0;
        for (Long imageId : imageIds) {
            ProductImage pi = ProductImage.findById(imageId);
            if (pi == null || !pi.productId.equals(productId)) {
                continue;
            }
            pi.position = pos;
            pos++;
        }
    }

    /** 把某张补充图提升为主图 (原主图进图集首位) */
    @Transactional
    public void promoteToMain(Long productId, Long imageId) {
        Product product = requireProduct(productId);
        ProductImage pi = ProductImage.findById(imageId);
        if (pi == null || !pi.productId.equals(productId)) {
            throw new BizException("图片不存在");
        }
        String oldMain = product.mainImagePath;
        product.mainImagePath = pi.path;
        pi.delete();
        if (oldMain != null && !oldMain.isBlank()) {
            ProductImage former = new ProductImage();
            former.productId = productId;
            former.path = oldMain;
            former.position = 0;
            former.persist();
            // 其余图整体后移一位
            for (ProductImage other : listImages(productId)) {
                if (!former.id.equals(other.id)) {
                    other.position = other.position + 1;
                }
            }
        }
    }

    private Product requireProduct(Long productId) {
        Product product = Product.findById(productId);
        if (product == null || product.deleted) {
            throw new BizException("商品不存在");
        }
        return product;
    }
}
