-- 商品标签 (ERP 发布时整体同步, 逗号分隔)
ALTER TABLE mall_product ADD COLUMN IF NOT EXISTS tags VARCHAR(500);
