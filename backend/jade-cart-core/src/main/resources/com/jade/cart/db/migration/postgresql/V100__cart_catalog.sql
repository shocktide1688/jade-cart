-- =====================================================
-- V16: 商品目录 (Jade Cart) — 分类/商品/变体轴/变体/库存
-- 对标 kartwo 0002_catalog: 通用双轴变体 + 两级库存
-- 金额一律整数分 (price_cents); 对外 ID 用 public_id (UUID), 内部自增不外泄
-- =====================================================

-- 商品分类 (父子层级)
CREATE TABLE mall_category (
    id          BIGSERIAL    PRIMARY KEY,
    public_id   VARCHAR(36)  NOT NULL UNIQUE,
    name        VARCHAR(100) NOT NULL,
    slug        VARCHAR(140) NOT NULL UNIQUE,
    parent_id   BIGINT,
    sort_order  INTEGER      NOT NULL DEFAULT 0,
    status      INTEGER      NOT NULL DEFAULT 1,        -- 1 启用 0 停用
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    deleted     BOOLEAN      NOT NULL DEFAULT FALSE
);
CREATE INDEX idx_mall_category_parent ON mall_category (parent_id);

-- 商品
CREATE TABLE mall_product (
    id               BIGSERIAL    PRIMARY KEY,
    public_id        VARCHAR(36)  NOT NULL UNIQUE,
    title            VARCHAR(200) NOT NULL,
    slug             VARCHAR(200) NOT NULL UNIQUE,
    description      TEXT,
    seo_description  VARCHAR(500),
    status           VARCHAR(20)  NOT NULL DEFAULT 'DRAFT',  -- DRAFT / ACTIVE / ARCHIVED
    featured         BOOLEAN      NOT NULL DEFAULT FALSE,    -- 首页精选
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    deleted          BOOLEAN      NOT NULL DEFAULT FALSE
);
CREATE INDEX idx_mall_product_status ON mall_product (status, deleted);
CREATE INDEX idx_mall_product_featured ON mall_product (featured) WHERE featured;

-- 商品变体轴 (如 color / size)
CREATE TABLE mall_product_option (
    id           BIGSERIAL    PRIMARY KEY,
    product_id   BIGINT       NOT NULL REFERENCES mall_product (id),
    option_key   VARCHAR(50)  NOT NULL,                   -- 规范化轴名, 如 color
    display_name VARCHAR(100) NOT NULL,
    sort_order   INTEGER      NOT NULL DEFAULT 0
);
CREATE INDEX idx_mall_option_product ON mall_product_option (product_id);

-- 变体轴取值
CREATE TABLE mall_product_option_value (
    id          BIGSERIAL    PRIMARY KEY,
    option_id   BIGINT       NOT NULL REFERENCES mall_product_option (id),
    value       VARCHAR(100) NOT NULL,
    sort_order  INTEGER      NOT NULL DEFAULT 0
);
CREATE INDEX idx_mall_option_value_option ON mall_product_option_value (option_id);

-- 变体 (SKU): 价格挂在变体上 (kartwo 同款); 无变体商品自动带一条默认变体
CREATE TABLE mall_variant (
    id          BIGSERIAL    PRIMARY KEY,
    public_id   VARCHAR(36)  NOT NULL UNIQUE,
    product_id  BIGINT       NOT NULL REFERENCES mall_product (id),
    sku         VARCHAR(120) NOT NULL UNIQUE,
    price_cents BIGINT       NOT NULL DEFAULT 0,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    deleted     BOOLEAN      NOT NULL DEFAULT FALSE
);
CREATE INDEX idx_mall_variant_product ON mall_variant (product_id, deleted);

-- 变体 × 轴取值 关联 (组合唯一由应用层签名保证)
CREATE TABLE mall_variant_option_value (
    id          BIGSERIAL    PRIMARY KEY,
    variant_id  BIGINT       NOT NULL REFERENCES mall_variant (id),
    option_id   BIGINT       NOT NULL REFERENCES mall_product_option (id),
    value       VARCHAR(100) NOT NULL
);
CREATE INDEX idx_mall_vov_variant ON mall_variant_option_value (variant_id);

-- 库存: quantity 实物在库 + reserved 未付款占用; 可售 = quantity - reserved
CREATE TABLE mall_inventory (
    id          BIGSERIAL   PRIMARY KEY,
    variant_id  BIGINT      NOT NULL UNIQUE REFERENCES mall_variant (id),
    quantity    INTEGER     NOT NULL DEFAULT 0,
    reserved    INTEGER     NOT NULL DEFAULT 0,
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- 商品 × 分类 多对多
CREATE TABLE mall_product_category (
    id          BIGSERIAL PRIMARY KEY,
    product_id  BIGINT    NOT NULL REFERENCES mall_product (id),
    category_id BIGINT    NOT NULL REFERENCES mall_category (id),
    UNIQUE (product_id, category_id)
);
CREATE INDEX idx_mall_pc_category ON mall_product_category (category_id);
