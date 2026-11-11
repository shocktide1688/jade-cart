-- =====================================================
-- V113: 系统配置 KV + 系统配置菜单 (Jade Cart) (SQLite 方言)
-- 菜单 id 170 (底座 1~60, cart 140~1601 已占用)
-- (sqlite 方言目录已有 V112 底座表亲和修复, 顺延 V113)
-- =====================================================

-- 系统配置 KV (明文存储, 非密钥类配置)
CREATE TABLE sys_app_config (
    id          INTEGER      PRIMARY KEY,
    key         VARCHAR(100) NOT NULL UNIQUE,
    value       TEXT,
    updated_at  TIMESTAMP  NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 种子: 管理端左上角标题 + 登录页欢迎语
INSERT INTO sys_app_config (key, value) VALUES ('app.title', 'Jade Platform')
ON CONFLICT (key) DO NOTHING;
INSERT INTO sys_app_config (key, value) VALUES ('login.welcome', '欢迎登录')
ON CONFLICT (key) DO NOTHING;

-- 系统管理 -> 系统配置
INSERT INTO sys_menu (id, parent_id, menu_name, menu_type, path, component, icon, perms, sort_order, visible, status, is_cache)
VALUES (170, 1, '系统配置', 'C', 'config', 'system/config/index', 'setting', 'system:config:config', 9, 1, 1, 0)
ON CONFLICT (id) DO NOTHING;

-- 授权 admin 角色 (role_id=1)
INSERT INTO sys_role_menu (role_id, menu_id) VALUES (1, 170)
ON CONFLICT DO NOTHING;
