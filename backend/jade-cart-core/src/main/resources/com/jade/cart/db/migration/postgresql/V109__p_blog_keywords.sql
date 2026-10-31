-- =====================================================
-- V109: 博客 SEO 关键词 (Jade Cart)
-- =====================================================
ALTER TABLE mall_blog_post ADD COLUMN IF NOT EXISTS seo_keywords VARCHAR(300);
