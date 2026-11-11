-- =====================================================
-- V114: 修复系统配置菜单 path 与底座「参数设置」(id 60) 撞车
-- 两者都用了 path='config' → 侧边栏两个菜单同一链接 /system/config
-- 系统配置改用 appconfig (与 jade-boot 底座移植一致), 参数设置恢复
-- 原状 (前端无对应页面, 被路由过滤隐藏)
-- (sqlite 方言目录 V113 已被 system_config 占用, 顺延 V114)
-- =====================================================

UPDATE sys_menu
SET path = 'appconfig', component = 'system/appconfig/index'
WHERE id = 170 AND menu_name = '系统配置' AND path = 'config';
