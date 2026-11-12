/**
 * 登录 token 滑动续期: 路由切换 (用户活跃信号) 时检查剩余寿命,
 * 低于 3 天则调 /api/v1/auth/renew 静默换新 —— 活跃使用永不过期, 闲置到期才掉线。
 * 阈值与后端 jade.jwt.renew.remaining-seconds 保持一致。
 */
import request from './request'
import { getToken } from './auth'
import { useUserStore } from '@/stores/user'

const RENEW_THRESHOLD_SECONDS = 3 * 86400
const MIN_INTERVAL_MS = 10 * 60 * 1000

let lastRenewAt = 0

export function maybeRenewToken() {
  const token = getToken()
  if (!token) return
  let exp: number
  try {
    const payload = token.split('.')[1]
    exp = JSON.parse(atob(payload.replace(/-/g, '+').replace(/_/g, '/'))).exp
  } catch {
    return
  }
  const left = exp - Date.now() / 1000
  if (left <= 0 || left > RENEW_THRESHOLD_SECONDS) return
  const now = Date.now()
  if (now - lastRenewAt < MIN_INTERVAL_MS) return
  lastRenewAt = now
  request
    .post('/api/v1/auth/renew')
    .then((res) => {
      const fresh: string | undefined = res.data?.data?.token
      if (fresh && fresh !== token) {
        useUserStore().renewToken(fresh)
      }
    })
    .catch(() => {})
}
