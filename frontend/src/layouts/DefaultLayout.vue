<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { REGISTERED_PATHS } from '@/router'
import { useUserStore } from '@/stores/user'
import { useAppStore } from '@/stores/app'
import { useTagsViewStore } from '@/stores/tagsView'
import TagsView from '@/components/TagsView.vue'
import request from '@/utils/request'
import {
  House, Expand, Fold, UserFilled, User, Menu, OfficeBuilding, Reading,
  Document, Key, Refresh, SwitchButton, Bell,
} from '@element-plus/icons-vue'

// 菜单 icon 用到的组件 (显式导入, 避免 namespace import 触发依赖重优化)
import {
  Setting, Monitor, Tools, Promotion, Collection, MagicStick, AlarmClock,
  Lock, DataAnalysis, ShoppingCart, Goods, Tickets, Van, ChatDotRound,
  Upload, Box, Brush, Message, Files,
} from '@element-plus/icons-vue'

const userStore = useUserStore()
const appStore = useAppStore()
const router = useRouter()
const route = useRoute()
const menus = ref<any[]>([])

// keep-alive 缓存哪些页面：以打开的标签页路由 name 为准，关 tab 即释放缓存
const tagsStore = useTagsViewStore()
const cachedViews = computed(
  () => tagsStore.visitedViews.map((v) => v.name).filter(Boolean) as string[]
)

// 路径 -> 图标 的映射 (菜单未配 icon 时按名称回退)
const ICON_MAP: Record<string, any> = {
  首页: House, 用户管理: User, 角色管理: UserFilled, 菜单管理: Menu,
  部门管理: OfficeBuilding, 字典管理: Reading, 操作日志: Document, 登录日志: Key,
  通知公告: Bell, 个人中心: UserFilled, 邮件配置: Message, 首页概览: House,
}

// 菜单 icon 解析: 优先 sys_menu.icon (Element Plus 组件 PascalCase 名), 次按名称映射, 兜底 Menu
const MENU_ICONS: Record<string, any> = {
  Setting, Monitor, Tools, Promotion, Collection, MagicStick, AlarmClock,
  Lock, DataAnalysis, ShoppingCart, Goods, Tickets, Van, ChatDotRound,
  Upload, Box, Brush, Document, Key, Message, Files, User, UserFilled,
  Menu, House, OfficeBuilding, Reading,
}
function resolveIcon(icon: string | undefined | null, name: string): any {
  if (icon && MENU_ICONS[icon]) return MENU_ICONS[icon]
  return ICON_MAP[name] || Menu
}

async function loadMenus() {
  try {
    const res = await request({ url: '/api/v1/menus/router', method: 'GET' })
    // 1. 补全父路径 (path 是相对的, Element Plus router 模式需要完整路径)
    // 2. 过滤掉前端没实现路由的菜单 (不然点过去 404)
    const raw = (res.data.data || []) as any[]
    const withPaths = buildFullPaths(raw, '')
    menus.value = filterUnregistered(withPaths)
  } catch (e) {
    console.warn('菜单加载失败，用 fallback', e)
    menus.value = []
  }
}

function buildFullPaths(items: any[], parentPath: string): any[] {
  return items.map((it) => {
    const fullPath = parentPath ? `${parentPath}/${it.path}` : `/${it.path}`
    const children = it.children ? buildFullPaths(it.children, fullPath) : undefined
    return { ...it, path: fullPath, children }
  })
}

/** 递归剔除没注册路由的菜单 (含子菜单都过滤掉) */
function filterUnregistered(items: any[]): any[] {
  return items
    .map((it) => ({
      ...it,
      children: it.children ? filterUnregistered(it.children) : undefined,
    }))
    .filter((it) => {
      // 有子菜单的父菜单: 至少有一个子菜单留下来了
      if (it.children && it.children.length > 0) return true
      // 叶子菜单: 路径必须在路由表里
      return REGISTERED_PATHS.has(it.path)
    })
}

onMounted(loadMenus)

const activeMenu = computed(() => router.currentRoute.value.path)

// 面包屑：从当前路由的 matched 数组构造
interface Crumb { name: string; path: string }
const breadcrumbs = computed<Crumb[]>(() => {
  const crumbs: Crumb[] = []
  for (const m of route.matched) {
    const title = (m.meta?.title as string)
    if (title && !m.meta?.hidden) {
      crumbs.push({ name: title, path: m.path })
    }
  }
  return crumbs
})

async function handleCommand(cmd: string) {
  if (cmd === 'logout') {
    await userStore.logout()
    router.push('/login')
  } else if (cmd === 'profile') {
    router.push('/profile')
  }
}
</script>

<template>
  <el-container class="layout">
    <el-aside
      :width="appStore.sidebarCollapsed ? '64px' : '220px'"
      class="aside"
    >
      <div
        class="logo"
        @click="router.push('/dashboard')"
      >
        <span v-if="!appStore.sidebarCollapsed">Jade Platform</span>
        <span v-else>玉</span>
      </div>
      <el-menu
        :default-active="activeMenu"
        :collapse="appStore.sidebarCollapsed"
        router
        background-color="#001529"
        text-color="#fff"
        active-text-color="#00a86b"
      >
        <template
          v-for="m in menus"
          :key="m.path || m.name"
        >
          <el-sub-menu
            v-if="m.children && m.children.length"
            :index="m.path || m.name"
          >
            <template #title>
              <el-icon><component :is="resolveIcon(m.icon, m.name)" /></el-icon>
              <span>{{ m.name }}</span>
            </template>
            <el-menu-item
              v-for="c in m.children"
              :key="c.path"
              :index="c.path"
            >
              <el-icon><component :is="resolveIcon(c.icon, c.name)" /></el-icon>
              <template #title>
                {{ c.name }}
              </template>
            </el-menu-item>
          </el-sub-menu>
          <el-menu-item
            v-else-if="m.path"
            :index="m.path"
          >
            <el-icon><component :is="resolveIcon(m.icon, m.name)" /></el-icon>
            <template #title>
              {{ m.name }}
            </template>
          </el-menu-item>
        </template>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="header">
        <el-button
          text
          @click="appStore.toggleSidebar"
        >
          <el-icon><Expand v-if="appStore.sidebarCollapsed" /><Fold v-else /></el-icon>
        </el-button>
        <div class="header-right">
          <el-tooltip
            content="刷新菜单"
            placement="bottom"
          >
            <el-button
              text
              @click="loadMenus"
            >
              <el-icon><Refresh /></el-icon>
            </el-button>
          </el-tooltip>
          <el-dropdown
            trigger="click"
            @command="handleCommand"
          >
            <span class="user-name">
              <el-icon><UserFilled /></el-icon>
              {{ userStore.username || '未登录' }}
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">
                  <el-icon><User /></el-icon> 个人中心
                </el-dropdown-item>
                <el-dropdown-item
                  command="logout"
                  divided
                >
                  <el-icon><SwitchButton /></el-icon> 退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <TagsView />

      <el-main class="main">
        <el-breadcrumb
          v-if="breadcrumbs.length > 1"
          separator="/"
          class="breadcrumb"
        >
          <el-breadcrumb-item
            v-for="(c, i) in breadcrumbs"
            :key="c.path"
          >
            <router-link
              v-if="i < breadcrumbs.length - 1"
              :to="c.path"
            >{{ c.name }}</router-link>
            <span v-else>{{ c.name }}</span>
          </el-breadcrumb-item>
        </el-breadcrumb>
        <router-view v-slot="{ Component }">
          <keep-alive :include="cachedViews">
            <component :is="Component" />
          </keep-alive>
        </router-view>
      </el-main>
    </el-container>
  </el-container>
</template>

<style scoped>
.layout { height: 100vh; }
.aside { background: #001529; transition: width 0.2s; overflow-x: hidden; }
.logo {
  height: 60px; line-height: 60px; color: #fff; text-align: center;
  font-size: 18px; font-weight: bold; letter-spacing: 2px; cursor: pointer;
}
.logo span { color: #00a86b; }
.aside :deep(.el-menu) { border-right: none; }
.aside :deep(.el-sub-menu__title:hover),
.aside :deep(.el-menu-item:hover) { background: #002140 !important; }
.header {
  display: flex; align-items: center; justify-content: space-between;
  background: #fff; border-bottom: 1px solid #e6e6e6; padding: 0 16px;
  height: 56px;
}
.header-right { display: flex; align-items: center; gap: 8px; }
.user-name {
  display: flex; align-items: center; gap: 6px; cursor: pointer;
  color: #303133; padding: 4px 8px; border-radius: 4px;
}
.user-name:hover { background: #f5f7fa; }
.main { background: #f5f7fa; padding: 16px; }
.breadcrumb { padding: 0 0 12px 0; font-size: 13px; }
</style>
