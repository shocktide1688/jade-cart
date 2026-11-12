import type { Router } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { maybeRenewToken } from '@/utils/renew'

const WHITE_LIST = ['/login', '/404', '/403']

export function setupGuards(router: Router) {
  router.beforeEach((to, _from, next) => {
    const userStore = useUserStore()

    // document.title 由 App.vue 统一同步 (路由 + 应用标题双响应)

    if (WHITE_LIST.includes(to.path)) {
      return next()
    }

    if (!userStore.isLoggedIn) {
      return next({ path: '/login', query: { redirect: to.fullPath } })
    }

    next()
  })

  // 用户活跃信号 = 路由切换: token 剩余 < 3 天时静默滑动续期
  router.afterEach(() => {
    maybeRenewToken()
  })
}
