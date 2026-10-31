package com.jade.cart.app.storefront.sections;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * 店面装修区块渲染器注册表: type → 渲染 HTML
 * 每个实现注入自己的 Qute 模板 + 所需服务; 配置只读白名单字段 (Jackson path 默认值)
 */
public interface SectionRenderer {

    /** 区块类型标识 (对应 mall_storefront_section.type) */
    String type();

    /** 渲染区块 HTML (config 为该区块的 JSON 配置) */
    String render(JsonNode cfg) throws Exception;
}
