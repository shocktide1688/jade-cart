-- =====================================================
-- V19: P1 增强 (Jade Cart) — 运费分区/发货追踪/邮件 outbox/媒体库
-- =====================================================

-- 运费分区: 国家列表(逗号分隔 ISO2, * = 其余) + 固定运费 + 满额免邮
CREATE TABLE mall_shipping_zone (
    id                   BIGSERIAL    PRIMARY KEY,
    name                 VARCHAR(100) NOT NULL,
    countries            VARCHAR(500) NOT NULL DEFAULT '*',  -- "US,CA" 或 "*"
    rate_cents           BIGINT       NOT NULL DEFAULT 0,
    free_threshold_cents BIGINT       NOT NULL DEFAULT 0,    -- 0 = 不启用满额免邮
    active               BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at           TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- 订单加收货国家
ALTER TABLE mall_order ADD COLUMN IF NOT EXISTS shipping_country VARCHAR(2) NOT NULL DEFAULT 'US';

-- 发货追踪 (单次履约模型)
CREATE TABLE mall_shipment (
    id              BIGSERIAL    PRIMARY KEY,
    order_id        BIGINT       NOT NULL UNIQUE REFERENCES mall_order (id),
    carrier         VARCHAR(100),
    tracking_number VARCHAR(120),
    tracking_url    VARCHAR(300),
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- 邮件 outbox: 与业务同事务入队, worker 异步投递; (order_id, kind) 唯一 = 天然幂等
CREATE TABLE mall_email_outbox (
    id              BIGSERIAL    PRIMARY KEY,
    order_id        BIGINT,
    kind            VARCHAR(40)  NOT NULL,   -- order_confirm / payment_confirm / shipped
    recipient       VARCHAR(200) NOT NULL,
    subject         VARCHAR(300) NOT NULL,
    body_html       TEXT         NOT NULL,
    status          VARCHAR(20)  NOT NULL DEFAULT 'PENDING', -- PENDING/SENT/FAILED/DEAD
    attempts        INTEGER      NOT NULL DEFAULT 0,
    next_attempt_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    last_error      TEXT,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    sent_at         TIMESTAMPTZ,
    UNIQUE (order_id, kind)
);
CREATE INDEX idx_mall_outbox_due ON mall_email_outbox (status, next_attempt_at);

-- 媒体资产: 磁盘存储内容哈希命名, DB 只存相对路径
CREATE TABLE mall_media_asset (
    id            BIGSERIAL    PRIMARY KEY,
    public_id     VARCHAR(36)  NOT NULL UNIQUE,
    filename      VARCHAR(200) NOT NULL,
    content_hash  VARCHAR(64)  NOT NULL UNIQUE,
    content_type  VARCHAR(100) NOT NULL,
    size_bytes    BIGINT       NOT NULL,
    width         INTEGER      NOT NULL DEFAULT 0,
    height        INTEGER      NOT NULL DEFAULT 0,
    path          VARCHAR(300) NOT NULL,   -- 相对 media 根目录
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- 商品主图
ALTER TABLE mall_product ADD COLUMN IF NOT EXISTS main_image_path VARCHAR(300);

-- 菜单: 运费设置 (155) + 编辑权限
INSERT INTO sys_menu (id, parent_id, menu_name, menu_type, path, component, icon, perms, sort_order, visible, status, is_cache)
VALUES (155, 150, '运费设置', 'C', 'shipping', 'mall/shipping/index', 'van', 'mall:shipping:list', 5, 1, 1, 0)
ON CONFLICT (id) DO NOTHING;
INSERT INTO sys_menu (id, parent_id, menu_name, menu_type, path, component, icon, perms, sort_order, visible, status, is_cache)
VALUES (1551, 155, '运费编辑', 'F', '', NULL, '', 'mall:shipping:edit', 1, 1, 1, 0)
ON CONFLICT (id) DO NOTHING;
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, id FROM sys_menu WHERE id IN (155, 1551)
ON CONFLICT DO NOTHING;
