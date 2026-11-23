-- =====================================================
-- V117: 商品标签 (ERP 发布时整体同步, 逗号分隔) (SQLite 方言)
-- (SQLite 的 ALTER TABLE ADD COLUMN 不支持 IF NOT EXISTS, Flyway 版本号保证只执行一次)
-- =====================================================
ALTER TABLE mall_product ADD COLUMN tags VARCHAR(500);
