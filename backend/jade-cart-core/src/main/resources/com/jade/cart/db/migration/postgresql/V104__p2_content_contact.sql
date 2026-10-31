-- =====================================================
-- V20: P2 内容 (Jade Cart) — 内容页 + 客服留言
-- =====================================================

-- 内容页 (受限 Markdown → 安全 HTML; 页脚政策页由生成器幂等写入)
CREATE TABLE mall_content_page (
    id           BIGSERIAL    PRIMARY KEY,
    slug         VARCHAR(140) NOT NULL UNIQUE,
    title        VARCHAR(200) NOT NULL,
    body_markdown TEXT,
    body_html    TEXT,
    status       VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE', -- ACTIVE / DRAFT
    policy       BOOLEAN      NOT NULL DEFAULT FALSE,    -- 政策生成器产物
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- 客服留言 (私密工单; 到期匿名化不删除)
CREATE TABLE mall_contact_message (
    id            BIGSERIAL    PRIMARY KEY,
    public_id     VARCHAR(36)  NOT NULL UNIQUE,
    name          VARCHAR(100),
    email         VARCHAR(200) NOT NULL,
    topic         VARCHAR(40)  NOT NULL DEFAULT 'general', -- general/order/shipping/return/other
    message       TEXT         NOT NULL,
    status        VARCHAR(20)  NOT NULL DEFAULT 'NEW',     -- NEW / PROCESSING / CLOSED
    ip_hash       VARCHAR(64),
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    processed_at  TIMESTAMPTZ,
    anonymized_at TIMESTAMPTZ
);
CREATE INDEX idx_mall_contact_status ON mall_contact_message (status, created_at);

-- 菜单: 内容管理 (156) + 客服留言 (157)
INSERT INTO sys_menu (id, parent_id, menu_name, menu_type, path, component, icon, perms, sort_order, visible, status, is_cache)
VALUES (156, 150, '内容管理', 'C', 'content', 'mall/content/index', 'document', 'mall:content:list', 6, 1, 1, 0)
ON CONFLICT (id) DO NOTHING;
INSERT INTO sys_menu (id, parent_id, menu_name, menu_type, path, component, icon, perms, sort_order, visible, status, is_cache)
VALUES (1561, 156, '内容编辑', 'F', '', NULL, '', 'mall:content:edit', 1, 1, 1, 0)
ON CONFLICT (id) DO NOTHING;
INSERT INTO sys_menu (id, parent_id, menu_name, menu_type, path, component, icon, perms, sort_order, visible, status, is_cache)
VALUES (157, 150, '客服留言', 'C', 'contact', 'mall/contact/index', 'chat', 'mall:contact:list', 7, 1, 1, 0)
ON CONFLICT (id) DO NOTHING;
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, id FROM sys_menu WHERE id IN (156, 1561, 157)
ON CONFLICT DO NOTHING;
