import axios, { type AxiosInstance, type InternalAxiosRequestConfig, type AxiosResponse } from 'axios'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'
import router from '@/router'

/**
 * 统一响应格式
 */
export interface ApiResponse<T = unknown> {
  code: number
  message: string
  data: T
  timestamp: number
}

const service: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE ?? '',
  timeout: 15000,
})

// 请求拦截器
service.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const userStore = useUserStore()
    // 仅登录接口剥凭据: 过期旧 token 会在 HTTP 层被 401 拒绝而阻断登录;
    // /auth/me /profile 等其余 auth 接口仍需携带 token
    const isPublicAuth = config.url === '/api/v1/auth/login'
    if (userStore.accessToken && !isPublicAuth) {
      config.headers.Authorization = `Bearer ${userStore.accessToken}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

// 响应拦截器
service.interceptors.response.use(
  (response: AxiosResponse<ApiResponse>) => {
    const res = response.data

    // 兼容非包装响应（如文件下载）
    if (typeof res !== 'object' || res === null || !('code' in res)) {
      return response
    }

    if (res.code === 0) {
      return response
    }

    // 1001/1002 未登录
    if (res.code === 1001 || res.code === 1002) {
      ElMessageBox.confirm('登录状态已过期，请重新登录', '提示', {
        confirmButtonText: '重新登录',
        cancelButtonText: '取消',
        type: 'warning',
      }).then(() => {
        const userStore = useUserStore()
        userStore.logout()
        router.push('/login')
      }).catch(() => {})
      return Promise.reject(new Error(res.message))
    }

    // 1003 无权限
    if (res.code === 1003) {
      ElMessage.error('您没有权限执行此操作')
      return Promise.reject(new Error(res.message))
    }

    // 业务错误
    ElMessage.error(res.message || '请求失败')
    return Promise.reject(new Error(res.message))
  },
  async (error) => {
    const status = error.response?.status
    const bizCode = error.response?.data?.code

    // 会话失效: HTTP 401 或业务码 1001/1002 → 统一引导重新登录 (加锁去重)
    if (status === 401 || bizCode === 1001 || bizCode === 1002) {
      if (!(window as any).__reloginPrompting) {
        ;(window as any).__reloginPrompting = true
        ElMessageBox.confirm('登录已过期, 请重新登录', '提示', {
          confirmButtonText: '重新登录',
          cancelButtonText: '取消',
          type: 'warning',
        })
          .then(() => {
            const userStore = useUserStore()
            userStore.logout()
            router.push('/login')
          })
          .finally(() => {
            ;(window as any).__reloginPrompting = false
          })
      }
      return Promise.reject(new Error('登录已过期'))
    }

    if (status === 403 || bizCode === 1003) {
      ElMessage.error('您没有权限执行此操作')
      return Promise.reject(error)
    }

    const msg = error.response?.data?.message || error.message || '网络异常'
    ElMessage.error(msg)
    return Promise.reject(error)
  }
)

export default service
