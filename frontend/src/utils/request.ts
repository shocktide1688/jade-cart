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

/**
 * 携带业务码的错误（登录页等需按 code 分支处理；保留 response 供调用方读取）
 */
export class BizError extends Error {
  code: number
  response?: AxiosResponse
  constructor(code: number, message: string, response?: AxiosResponse) {
    super(message)
    this.code = code
    this.response = response
  }
}

const service: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE ?? '',
  timeout: 15000,
})

/** 无需携带 token 的公开端点 */
const PUBLIC_AUTH_URLS = ['/api/v1/auth/login', '/api/v1/auth/captcha']

// 请求拦截器
service.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const userStore = useUserStore()
    // 公开端点剥凭据: 残留/过期的旧 token 会触发 Quarkus 安全层 401 (无 CORS 头,
    // 浏览器直接拦截变 Network Error), 且验证码/登录本就不需要身份;
    // /auth/me /profile 等其余 auth 接口仍需携带 token
    const isPublicAuth = PUBLIC_AUTH_URLS.includes(config.url || '')
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

    // 1001/1002 未登录（登录接口自身返回这些码只按普通业务错误提示，不弹过期框）
    if ((res.code === 1001 || res.code === 1002) && response.config.url !== '/api/v1/auth/login') {
      ElMessageBox.confirm('登录状态已过期，请重新登录', '提示', {
        confirmButtonText: '重新登录',
        cancelButtonText: '取消',
        type: 'warning',
      }).then(() => {
        const userStore = useUserStore()
        userStore.logout()
        router.push('/login')
      }).catch(() => {})
      return Promise.reject(new BizError(res.code, res.message))
    }

    // 1003 无权限
    if (res.code === 1003) {
      ElMessage.error('您没有权限执行此操作')
      return Promise.reject(new BizError(res.code, res.message))
    }

    // 业务错误
    ElMessage.error(res.message || '请求失败')
    return Promise.reject(new BizError(res.code, res.message))
  },
  async (error) => {
    const status = error.response?.status
    const bizCode = error.response?.data?.code
    const url = error.config?.url

    // 会话失效: HTTP 401 或业务码 1001/1002 → 统一引导重新登录 (加锁去重, 避免并发请求弹多个框)
    // 登录接口自身失效只按普通错误提示（不该弹"登录已过期"误导）
    if ((status === 401 || bizCode === 1001 || bizCode === 1002) && url !== '/api/v1/auth/login') {
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
      return Promise.reject(new BizError(bizCode ?? status ?? 0, '登录已过期', error.response))
    }

    if (status === 403 || bizCode === 1003) {
      ElMessage.error('您没有权限执行此操作')
      return Promise.reject(new BizError(bizCode ?? status ?? 0, '您没有权限执行此操作', error.response))
    }

    const msg = error.response?.data?.message || error.message || '网络异常'
    ElMessage.error(msg)
    return Promise.reject(new BizError(bizCode ?? status ?? 0, msg, error.response))
  }
)

export default service
