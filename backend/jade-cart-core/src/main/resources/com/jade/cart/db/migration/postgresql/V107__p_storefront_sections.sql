-- =====================================================
-- V107: 店面装修 (P-主题装修二期) — 首页区块编排
-- 页面 = 有序 Section 列表; 类型由 SectionRenderer 注册表渲染
-- =====================================================

CREATE TABLE mall_storefront_section (
    id         BIGSERIAL    PRIMARY KEY,
    page       VARCHAR(50)  NOT NULL DEFAULT 'home',
    position   INTEGER      NOT NULL DEFAULT 0,
    type       VARCHAR(40)  NOT NULL,   -- hero/featured/category_grid/usp/rich_text/blog_latest/banner_strip
    config     TEXT,                    -- JSON (白名单字段, 渲染端校验)
    visible    BOOLEAN      NOT NULL DEFAULT TRUE,
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_mall_section_page ON mall_storefront_section (page, position);

-- 菜单: 店面装修 (160)
INSERT INTO sys_menu (id, parent_id, menu_name, menu_type, path, component, icon, perms, sort_order, visible, status, is_cache)
VALUES (160, 150, '店面装修', 'C', 'storefront', 'mall/storefront/index', 'brush', 'mall:storefront:list', 10, 1, 1, 0)
ON CONFLICT (id) DO NOTHING;
INSERT INTO sys_menu (id, parent_id, menu_name, menu_type, path, component, icon, perms, sort_order, visible, status, is_cache)
VALUES (1601, 160, '装修编辑', 'F', '', NULL, '', 'mall:storefront:edit', 1, 1, 1, 0)
ON CONFLICT (id) DO NOTHING;
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, id FROM sys_menu WHERE id IN (160, 1601)
ON CONFLICT DO NOTHING;
