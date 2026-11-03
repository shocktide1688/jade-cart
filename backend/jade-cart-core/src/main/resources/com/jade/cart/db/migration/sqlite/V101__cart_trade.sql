-- =====================================================
-- V17: 交易 (Jade Cart) — 客户/购物车/订单/支付回调幂等 (SQLite 方言)
-- 对标 kartwo 0005_cart + 0006_order + 0008_payment + 0009_refund
-- 订单行落快照 (标题/规格/SKU/单价), 结算单事务防超卖
-- =====================================================

-- 客户: 访客下单按邮箱 upsert, 无密码 (kartwo 同款)
CREATE TABLE mall_customer (
    id          INTEGER      PRIMARY KEY,
    public_id   VARCHAR(36)  NOT NULL UNIQUE,
    email       VARCHAR(200) NOT NULL UNIQUE,
    name        VARCHAR(100),
    created_at  TIMESTAMP  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP  NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 购物车: 匿名 cookie token 标识
CREATE TABLE mall_cart (
    id          INTEGER      PRIMARY KEY,
    public_id   VARCHAR(36)  NOT NULL UNIQUE,
    token       VARCHAR(64)  NOT NULL UNIQUE,
    status      VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE', -- ACTIVE / CONVERTED / ABANDONED
    created_at  TIMESTAMP  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP  NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_mall_cart_token ON mall_cart (token, status);

CREATE TABLE mall_cart_item (
    id          INTEGER PRIMARY KEY,
    cart_id     BIGINT  NOT NULL REFERENCES mall_cart (id),
    variant_id  BIGINT  NOT NULL REFERENCES mall_variant (id),
    quantity    INTEGER NOT NULL DEFAULT 1,
    created_at  TIMESTAMP  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (cart_id, variant_id)
);

-- 订单 (表名用 orders 避开 SQL 保留字)
CREATE TABLE mall_order (
    id                     INTEGER      PRIMARY KEY,
    public_id              VARCHAR(36)  NOT NULL UNIQUE,
    order_number           VARCHAR(40)  NOT NULL UNIQUE,
    customer_id            BIGINT       REFERENCES mall_customer (id),
    email                  VARCHAR(200) NOT NULL,
    ship_name              VARCHAR(100),
    status                 VARCHAR(30)  NOT NULL DEFAULT 'PENDING',
    -- PENDING → PAID → FULFILLED; 分支: CANCELLED / REFUNDED / PAYMENT_REVIEW
    subtotal_cents         BIGINT       NOT NULL DEFAULT 0,
    shipping_cents         BIGINT       NOT NULL DEFAULT 0,   -- P1 运费分区, 先留 0
    total_cents            BIGINT       NOT NULL DEFAULT 0,
    currency               VARCHAR(10)  NOT NULL DEFAULT 'USD',
    payment_provider       VARCHAR(30),
    payment_ref            VARCHAR(120),
    reservation_expires_at TIMESTAMP,                              -- 30 分钟预留期限
    paid_at                TIMESTAMP,
    fulfilled_at           TIMESTAMP,
    cancelled_at           TIMESTAMP,
    refunded_at            TIMESTAMP,
    created_at             TIMESTAMP  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at             TIMESTAMP  NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_mall_order_created ON mall_order (created_at);
CREATE INDEX idx_mall_order_status ON mall_order (status);
CREATE INDEX idx_mall_order_reservation ON mall_order (status, reservation_expires_at);

-- 订单行 (快照)
CREATE TABLE mall_order_item (
    id               INTEGER      PRIMARY KEY,
    order_id         BIGINT       NOT NULL REFERENCES mall_order (id),
    variant_id       BIGINT,
    product_title    VARCHAR(200) NOT NULL,
    variant_spec     VARCHAR(300),                             -- "Color: Red / Size: M"
    sku              VARCHAR(120),
    unit_price_cents BIGINT       NOT NULL,
    quantity         INTEGER      NOT NULL,
    line_total_cents BIGINT       NOT NULL
);
CREATE INDEX idx_mall_order_item_order ON mall_order_item (order_id);

-- 支付回调幂等表: (provider, event_id) 唯一, 冲突即已处理 (kartwo 0008 同款)
CREATE TABLE mall_webhook_event (
    id           INTEGER      PRIMARY KEY,
    provider     VARCHAR(30)  NOT NULL,
    event_id     VARCHAR(120) NOT NULL,
    order_public_id VARCHAR(36),
    payload      TEXT,
    processed_at TIMESTAMP  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (provider, event_id)
);
