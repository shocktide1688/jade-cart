-- =====================================================
-- V110: 菜单图标修正 (Jade Cart) (SQLite 方言, 语句与 PG 版一致)
-- icon 统一为 Element Plus 图标组件的 PascalCase 名
-- (前端 DefaultLayout 优先按 sys_menu.icon 渲染)
-- =====================================================

-- 底座目录/菜单
UPDATE sys_menu SET icon='Setting'       WHERE menu_name='系统管理';
UPDATE sys_menu SET icon='Monitor'       WHERE menu_name='系统监控';
UPDATE sys_menu SET icon='Tools'         WHERE menu_name='系统工具';
UPDATE sys_menu SET icon='Promotion'     WHERE menu_name='平台演示';
UPDATE sys_menu SET icon='UserFilled'    WHERE menu_name='我的';
UPDATE sys_menu SET icon='User'          WHERE menu_name='用户管理';
UPDATE sys_menu SET icon='UserFilled'    WHERE menu_name='角色管理';
UPDATE sys_menu SET icon='Menu'          WHERE menu_name='菜单管理';
UPDATE sys_menu SET icon='OfficeBuilding' WHERE menu_name='部门管理';
UPDATE sys_menu SET icon='Collection'    WHERE menu_name='字典管理';
UPDATE sys_menu SET icon='Setting'       WHERE menu_name='参数设置';
UPDATE sys_menu SET icon='Document'      WHERE menu_name='操作日志';
UPDATE sys_menu SET icon='Key'           WHERE menu_name='登录日志';
UPDATE sys_menu SET icon='MagicStick'    WHERE menu_name='代码生成';
UPDATE sys_menu SET icon='Files'         WHERE menu_name='文件存储';
UPDATE sys_menu SET icon='AlarmClock'    WHERE menu_name='定时任务';
UPDATE sys_menu SET icon='Lock'          WHERE menu_name='分布式锁';
UPDATE sys_menu SET icon='DataAnalysis'  WHERE menu_name='业务指标';
UPDATE sys_menu SET icon='UserFilled'    WHERE menu_name='个人中心';
UPDATE sys_menu SET icon='Message'       WHERE menu_name='邮件配置';
UPDATE sys_menu SET icon='Key'           WHERE menu_name='LDAP 配置';

-- 商城管理目录与子菜单
UPDATE sys_menu SET icon='ShoppingCart'  WHERE menu_name='商城管理';
UPDATE sys_menu SET icon='Goods'         WHERE menu_name='商品管理';
UPDATE sys_menu SET icon='Files'         WHERE menu_name='分类管理';
UPDATE sys_menu SET icon='Tickets'       WHERE menu_name='订单管理';
UPDATE sys_menu SET icon='Setting'       WHERE menu_name='商城设置';
UPDATE sys_menu SET icon='Van'           WHERE menu_name='运费设置';
UPDATE sys_menu SET icon='Document'      WHERE menu_name='内容管理';
UPDATE sys_menu SET icon='ChatDotRound'  WHERE menu_name='客服留言';
UPDATE sys_menu SET icon='Upload'        WHERE menu_name='数据导入';
UPDATE sys_menu SET icon='Box'           WHERE menu_name='备份管理';
UPDATE sys_menu SET icon='Reading'       WHERE menu_name='博客管理';
UPDATE sys_menu SET icon='Brush'         WHERE menu_name='店面装修';
