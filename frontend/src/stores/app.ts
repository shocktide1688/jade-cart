import { defineStore } from 'pinia'
import { ref } from 'vue'
import request from '@/utils/request'

/** 兜底值 (sys_app_config 未配置/加载失败时使用) */
export const DEFAULT_APP_TITLE = 'Jade Platform'
export const DEFAULT_LOGIN_WELCOME = '欢迎登录'

export const useAppStore = defineStore('app', () => {
  const sidebarCollapsed = ref(false)
  const device = ref<'desktop' | 'mobile'>('desktop')
  const title = ref(DEFAULT_APP_TITLE)
  const loginWelcome = ref(DEFAULT_LOGIN_WELCOME)

  function toggleSidebar() {
    sidebarCollapsed.value = !sidebarCollapsed.value
  }

  /** 从系统配置拉取公开配置 (应用标题 + 登录欢迎语, 失败静默回落默认值) */
  async function loadConfig() {
    try {
      const res: any = await request.get('/api/v1/sys-config')
      title.value = res.data.data['app.title'] || DEFAULT_APP_TITLE
      loginWelcome.value = res.data.data['login.welcome'] || DEFAULT_LOGIN_WELCOME
    } catch {
      title.value = DEFAULT_APP_TITLE
      loginWelcome.value = DEFAULT_LOGIN_WELCOME
    }
  }

  return {
    sidebarCollapsed,
    device,
    title,
    loginWelcome,
    toggleSidebar,
    loadConfig,
  }
})
