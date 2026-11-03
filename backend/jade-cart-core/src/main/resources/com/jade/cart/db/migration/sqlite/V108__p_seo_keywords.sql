-- =====================================================
-- V108: 商品 SEO 关键词 (Jade Cart) (SQLite 方言)
-- (SQLite 的 ALTER TABLE ADD COLUMN 不支持 IF NOT EXISTS, Flyway 版本号保证只执行一次)
-- =====================================================
ALTER TABLE mall_product ADD COLUMN seo_keywords VARCHAR(300);
