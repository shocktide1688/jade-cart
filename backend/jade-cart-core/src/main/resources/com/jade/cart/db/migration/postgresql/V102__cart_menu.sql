-- =====================================================
-- V18: 商城菜单 + 商城设置 KV (Jade Cart)
-- 菜单 id 从 150 起 (140=邮件配置, 141=LDAP 配置 已被占用)
-- =====================================================

-- 商城设置 KV (值由实体层 AES-GCM 加密)
CREATE TABLE mall_store_setting (
    id          BIGSERIAL    PRIMARY KEY,
    key         VARCHAR(100) NOT NULL UNIQUE,
    value       TEXT,                -- AES-256-GCM 密文
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- 一级目录: 商城管理
INSERT INTO sys_menu (id, parent_id, menu_name, menu_type, path, component, icon, perms, sort_order, visible, status, is_cache)
VALUES (150, 0, '商城管理', 'M', 'mall', NULL, 'shopping', '', 2, 1, 1, 0)
ON CONFLICT (id) DO NOTHING;

-- 商品管理
INSERT INTO sys_menu (id, parent_id, menu_name, menu_type, path, component, icon, perms, sort_order, visible, status, is_cache)
VALUES (151, 150, '商品管理', 'C', 'product', 'mall/product/index', 'goods', 'mall:product:list', 1, 1, 1, 0)
ON CONFLICT (id) DO NOTHING;
INSERT INTO sys_menu (id, parent_id, menu_name, menu_type, path, component, icon, perms, sort_order, visible, status, is_cache)
VALUES (1511, 151, '商品编辑', 'F', '', NULL, '', 'mall:product:edit', 1, 1, 1, 0)
ON CONFLICT (id) DO NOTHING;

-- 分类管理
INSERT INTO sys_menu (id, parent_id, menu_name, menu_type, path, component, icon, perms, sort_order, visible, status, is_cache)
VALUES (152, 150, '分类管理', 'C', 'category', 'mall/category/index', 'files', 'mall:category:list', 2, 1, 1, 0)
ON CONFLICT (id) DO NOTHING;
INSERT INTO sys_menu (id, parent_id, menu_name, menu_type, path, component, icon, perms, sort_order, visible, status, is_cache)
VALUES (1521, 152, '分类编辑', 'F', '', NULL, '', 'mall:category:edit', 1, 1, 1, 0)
ON CONFLICT (id) DO NOTHING;

-- 订单管理
INSERT INTO sys_menu (id, parent_id, menu_name, menu_type, path, component, icon, perms, sort_order, visible, status, is_cache)
VALUES (153, 150, '订单管理', 'C', 'order', 'mall/order/index', 'list', 'mall:order:list', 3, 1, 1, 0)
ON CONFLICT (id) DO NOTHING;
INSERT INTO sys_menu (id, parent_id, menu_name, menu_type, path, component, icon, perms, sort_order, visible, status, is_cache)
VALUES (1531, 153, '订单操作', 'F', '', NULL, '', 'mall:order:manage', 1, 1, 1, 0)
ON CONFLICT (id) DO NOTHING;

-- 商城设置
INSERT INTO sys_menu (id, parent_id, menu_name, menu_type, path, component, icon, perms, sort_order, visible, status, is_cache)
VALUES (154, 150, '商城设置', 'C', 'setting', 'mall/setting/index', 'setting', 'mall:setting:config', 4, 1, 1, 0)
ON CONFLICT (id) DO NOTHING;

-- 授权 admin 角色 (role_id=1)
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, id FROM sys_menu WHERE id IN (150, 151, 1511, 152, 1521, 153, 1531, 154)
ON CONFLICT DO NOTHING;
