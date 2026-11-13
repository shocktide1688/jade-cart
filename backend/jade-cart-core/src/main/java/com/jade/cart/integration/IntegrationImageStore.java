package com.jade.cart.integration;

import com.jade.cart.catalog.entity.Product;
import com.jade.cart.catalog.entity.ProductImage;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

/**
 * ERP 集成图片写库 (每个方法独立小事务): 供 syncProductImages 无事务编排调用。
 *
 * <p>独立事务的原因: 图片属于辅助资产, 磁盘满/下载失败时只丢图片; 商品主数据已在 upsert 事务中提交, 绝不能被图片异常连带回滚 (2026-11-13 生产事故:
 * 图片异常污染主事务, 商品静默丢失)。
 */
@ApplicationScoped
public class IntegrationImageStore {

    /** 清空商品现有图片 (相册行 + 主图路径), 为整体替换做准备。 */
    @Transactional
    public void clearProductImages(Long productId) {
        ProductImage.delete("productId", productId);
        Product product = Product.findById(productId);
        if (product != null) {
            product.mainImagePath = null;
            product.persist();
        }
    }

    /** 设置主图。 */
    @Transactional
    public void setMainImage(Long productId, String path) {
        Product product = Product.findById(productId);
        if (product == null) {
            return;
        }
        product.mainImagePath = path;
        product.persist();
    }

    /** 追加一张相册图。 */
    @Transactional
    public void attachGalleryImage(Long productId, String path, int position) {
        ProductImage pi = new ProductImage();
        pi.productId = productId;
        pi.path = path;
        pi.position = position;
        pi.persist();
        Log.debugf(
                "integration gallery image attached: product=%s pos=%s",
                productId, Integer.valueOf(position));
    }
}
