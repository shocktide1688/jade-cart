-- =====================================================
-- V108: 商品 SEO 关键词 (Jade Cart)
-- =====================================================
ALTER TABLE mall_product ADD COLUMN IF NOT EXISTS seo_keywords VARCHAR(300);
