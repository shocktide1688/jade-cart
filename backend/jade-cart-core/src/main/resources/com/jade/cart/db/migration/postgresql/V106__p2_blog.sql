-- =====================================================
-- V106: 博客 (Jade Cart, 补齐 P2-8) — SEO 核心内容载体
-- =====================================================

CREATE TABLE mall_blog_post (
    id              BIGSERIAL    PRIMARY KEY,
    public_id       VARCHAR(36)  NOT NULL UNIQUE,
    slug            VARCHAR(200) NOT NULL UNIQUE,
    title           VARCHAR(200) NOT NULL,
    excerpt         VARCHAR(400),
    body_markdown   TEXT,
    body_html       TEXT,
    cover_path      VARCHAR(300),
    seo_description VARCHAR(500),
    status          VARCHAR(20)  NOT NULL DEFAULT 'DRAFT',  -- DRAFT / ACTIVE
    published_at    TIMESTAMPTZ,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_mall_blog_status ON mall_blog_post (status, published_at);

-- 菜单: 博客管理 (159)
INSERT INTO sys_menu (id, parent_id, menu_name, menu_type, path, component, icon, perms, sort_order, visible, status, is_cache)
VALUES (159, 150, '博客管理', 'C', 'blog', 'mall/blog/index', 'reading', 'mall:blog:list', 9, 1, 1, 0)
ON CONFLICT (id) DO NOTHING;
INSERT INTO sys_menu (id, parent_id, menu_name, menu_type, path, component, icon, perms, sort_order, visible, status, is_cache)
VALUES (1591, 159, '博客编辑', 'F', '', NULL, '', 'mall:blog:edit', 1, 1, 1, 0)
ON CONFLICT (id) DO NOTHING;
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, id FROM sys_menu WHERE id IN (159, 1591)
ON CONFLICT DO NOTHING;
