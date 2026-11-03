-- =====================================================
-- V111: 商品多图画廊 (Jade Cart)
-- 主图仍在 mall_product.main_image_path; 本表存全部图
-- (position 0 通常与主图相同, 画廊主图用 ?img=N 切换)
-- =====================================================

CREATE TABLE IF NOT EXISTS mall_product_image (
    id         BIGSERIAL PRIMARY KEY,
    product_id BIGINT      NOT NULL,
    path       VARCHAR(300) NOT NULL,
    position   INT          NOT NULL DEFAULT 0,
    created_at TIMESTAMP    NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_pimg_product FOREIGN KEY (product_id) REFERENCES mall_product (id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_pimg_product ON mall_product_image (product_id, position);
