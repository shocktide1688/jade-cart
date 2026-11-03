-- =====================================================
-- V21: P2 数据导入 (Jade Cart) — 导入任务 + Shopify 旧链接 301 重定向 (SQLite 方言)
-- =====================================================

-- 导入任务: source_sha256 唯一 = 同文件幂等; 干跑预览 → 确认执行
CREATE TABLE mall_import_job (
    id              INTEGER      PRIMARY KEY,
    public_id       VARCHAR(36)  NOT NULL UNIQUE,
    filename        VARCHAR(200) NOT NULL,
    source_format   VARCHAR(20)  NOT NULL DEFAULT 'generic', -- generic / shopify
    source_sha256   VARCHAR(64)  NOT NULL UNIQUE,
    status          VARCHAR(20)  NOT NULL DEFAULT 'PREVIEW', -- PREVIEW / CONFIRMED / FAILED
    total_rows      INTEGER      NOT NULL DEFAULT 0,
    error_rows      INTEGER      NOT NULL DEFAULT 0,
    product_count   INTEGER      NOT NULL DEFAULT 0,
    imported_count  INTEGER      NOT NULL DEFAULT 0,
    preview_json    TEXT,                                     -- 解析结果 (产品+行级错误)
    created_at      TIMESTAMP  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    confirmed_at    TIMESTAMP
);

-- Shopify 旧 handle → 新商品页 301 (仅导入执行时写入)
CREATE TABLE mall_redirect (
    id         INTEGER      PRIMARY KEY,
    handle     VARCHAR(200) NOT NULL UNIQUE,
    target     VARCHAR(300) NOT NULL,                      -- /products/{slug}
    created_at TIMESTAMP  NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 菜单: 数据导入 (158)
INSERT INTO sys_menu (id, parent_id, menu_name, menu_type, path, component, icon, perms, sort_order, visible, status, is_cache)
VALUES (158, 150, '数据导入', 'C', 'import', 'mall/import/index', 'upload', 'mall:import:list', 8, 1, 1, 0)
ON CONFLICT (id) DO NOTHING;
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, id FROM sys_menu WHERE id = 158
ON CONFLICT DO NOTHING;
