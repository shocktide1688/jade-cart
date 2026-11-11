import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import { setupGuards } from './guards'

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/pages/login/index.vue'),
    meta: { layout: 'blank', title: '登录' },
  },
  {
    path: '/',
    component: () => import('@/layouts/DefaultLayout.vue'),
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/pages/dashboard/index.vue'),
        meta: { title: '首页', icon: 'House' },
      },
      // ===== 商城管理 (对应 sys_menu V18: parent path=mall) =====
      {
        path: 'mall/product',
        name: 'MallProduct',
        component: () => import('@/pages/mall/product/index.vue'),
        meta: { title: '商品管理', icon: 'Goods' },
      },
      {
        path: 'mall/category',
        name: 'MallCategory',
        component: () => import('@/pages/mall/category/index.vue'),
        meta: { title: '分类管理', icon: 'Files' },
      },
      {
        path: 'mall/order',
        name: 'MallOrder',
        component: () => import('@/pages/mall/order/index.vue'),
        meta: { title: '订单管理', icon: 'List' },
      },
      {
        path: 'mall/shipping',
        name: 'MallShipping',
        component: () => import('@/pages/mall/shipping/index.vue'),
        meta: { title: '运费设置', icon: 'Van' },
      },
      {
        path: 'mall/content',
        name: 'MallContent',
        component: () => import('@/pages/mall/content/index.vue'),
        meta: { title: '内容管理', icon: 'Document' },
      },
      {
        path: 'mall/contact',
        name: 'MallContact',
        component: () => import('@/pages/mall/contact/index.vue'),
        meta: { title: '客服留言', icon: 'Chat' },
      },
      {
        path: 'mall/import',
        name: 'MallImport',
        component: () => import('@/pages/mall/import/index.vue'),
        meta: { title: '数据导入', icon: 'Upload' },
      },
      {
        path: 'mall/backup',
        name: 'MallBackup',
        component: () => import('@/pages/mall/backup/index.vue'),
        meta: { title: '备份管理', icon: 'Box' },
      },
      {
        path: 'mall/storefront',
        name: 'MallStorefront',
        component: () => import('@/pages/mall/storefront/index.vue'),
        meta: { title: '店面装修', icon: 'Brush' },
      },
      {
        path: 'mall/blog',
        name: 'MallBlog',
        component: () => import('@/pages/mall/blog/index.vue'),
        meta: { title: '博客管理', icon: 'Reading' },
      },
      {
        path: 'mall/setting',
        name: 'MallSetting',
        component: () => import('@/pages/mall/setting/index.vue'),
        meta: { title: '商城设置', icon: 'Setting' },
      },
      // ===== 系统管理 =====
      {
        path: 'system/user',
        name: 'SystemUser',
        component: () => import('@/pages/system/user.vue'),
        meta: { title: '用户管理', icon: 'User' },
      },
      {
        path: 'system/role',
        name: 'SystemRole',
        component: () => import('@/pages/system/role.vue'),
        meta: { title: '角色管理', icon: 'UserFilled' },
      },
      {
        path: 'system/menu',
        name: 'SystemMenu',
        component: () => import('@/pages/system/menu.vue'),
        meta: { title: '菜单管理', icon: 'Menu' },
      },
      {
        path: 'system/dept',
        name: 'SystemDept',
        component: () => import('@/pages/system/dept.vue'),
        meta: { title: '部门管理', icon: 'OfficeBuilding' },
      },
      {
        path: 'system/dict',
        name: 'SystemDict',
        component: () => import('@/pages/system/dict.vue'),
        meta: { title: '字典管理', icon: 'Reading' },
      },
      {
        path: 'system/notice',
        name: 'SystemNotice',
        component: () => import('@/pages/system/notice.vue'),
        meta: { title: '通知公告', icon: 'Bell' },
      },
      // ===== 系统管理-配置 =====
      {
        path: 'system/mail',
        name: 'SystemMail',
        component: () => import('@/pages/system/mail/index.vue'),
        meta: { title: '邮件配置', icon: 'Message' },
      },
      {
        path: 'system/ldap',
        name: 'SystemLdap',
        component: () => import('@/pages/system/ldap/index.vue'),
        meta: { title: 'LDAP 配置', icon: 'Key' },
      },
      {
        path: 'system/config',
        name: 'SystemConfig',
        component: () => import('@/pages/system/config/index.vue'),
        meta: { title: '系统配置', icon: 'Setting' },
      },
      // ===== 系统监控 =====
      {
        path: 'monitor/operlog',
        name: 'MonitorOperLog',
        component: () => import('@/pages/monitor/operlog.vue'),
        meta: { title: '操作日志', icon: 'Document' },
      },
      {
        path: 'monitor/loginlog',
        name: 'MonitorLoginLog',
        component: () => import('@/pages/monitor/loginlog.vue'),
        meta: { title: '登录日志', icon: 'Key' },
      },
      // ===== 个人中心 =====
      {
        path: 'profile',
        name: 'Profile',
        component: () => import('@/pages/profile/index.vue'),
        meta: { title: '个人中心', icon: 'UserFilled', hidden: true },
      },
      // ===== 平台演示 =====
      {
        path: 'demo/platform',
        name: 'DemoPlatform',
        component: () => import('@/pages/demo/platform.vue'),
        meta: { title: '平台能力展示', icon: 'MagicStick' },
      },
    ],
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('@/pages/error/404.vue'),
    meta: { layout: 'blank', title: '404' },
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

setupGuards(router)

/** 收集所有已注册路由的完整路径（用于过滤后端菜单里没实现的） */
export const REGISTERED_PATHS = new Set<string>(['/dashboard', '/login'])
function collectPaths(rs: RouteRecordRaw[], prefix = '') {
  for (const r of rs) {
    const p = (prefix + '/' + (r.path || '')).replace('//', '/')
    if (p && p !== '/') REGISTERED_PATHS.add(p)
    if (r.children?.length) collectPaths(r.children, p)
  }
}
collectPaths(routes)

export default router
