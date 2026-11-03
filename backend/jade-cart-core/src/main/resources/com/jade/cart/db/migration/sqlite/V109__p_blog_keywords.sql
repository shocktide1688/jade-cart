-- =====================================================
-- V109: 博客 SEO 关键词 (Jade Cart) (SQLite 方言)
-- (SQLite 的 ALTER TABLE ADD COLUMN 不支持 IF NOT EXISTS, Flyway 版本号保证只执行一次)
-- =====================================================
ALTER TABLE mall_blog_post ADD COLUMN seo_keywords VARCHAR(300);
