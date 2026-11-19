package com.jade.cart.catalog.dto;

import java.util.List;

/** 店面分类树节点 (两级, 与 SSR mega 菜单同形状) */
public record CategoryNode(String slug, String name, List<CategoryNode> children) {}
