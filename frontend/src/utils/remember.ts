/**
 * 登录页「记住账号并自动登录」本地凭据存取
 *
 * 存 localStorage (密码 base64 编码 — 防肩窥, 非加密; 浏览器同源即可读, 公共电脑勿勾选):
 *   { username, password, auto }  auto=true 时登录页自动重登
 * 手动退出登录只清 auto (保留填充, 否则永远退不出去), 勾选框取消才整体清除。
 */
const KEY = 'jade_remember'

export interface Remembered {
  username: string
  password: string
  auto: boolean
}

function encode(s: string): string {
  return btoa(encodeURIComponent(s))
}

function decode(s: string): string {
  return decodeURIComponent(atob(s))
}

export function loadRemembered(): Remembered | null {
  try {
    const raw = localStorage.getItem(KEY)
    if (!raw) return null
    const o = JSON.parse(raw)
    if (!o.username || !o.password) return null
    return { username: decode(o.username), password: decode(o.password), auto: !!o.auto }
  } catch {
    return null
  }
}

export function saveRemembered(username: string, password: string) {
  localStorage.setItem(
    KEY,
    JSON.stringify({ username: encode(username), password: encode(password), auto: true }),
  )
}

/** 取消记住: 整体清除 */
export function clearRemembered() {
  localStorage.removeItem(KEY)
}

/** 手动登出: 停止自动登录, 保留账号密码填充 */
export function disableAutoLogin() {
  const r = loadRemembered()
  if (!r) return
  localStorage.setItem(
    KEY,
    JSON.stringify({ username: encode(r.username), password: encode(r.password), auto: false }),
  )
}

/** 自动登录失败(密码改了/账号禁用)时停用 auto, 防循环 */
export function stopAutoLogin() {
  const r = loadRemembered()
  if (r?.auto) disableAutoLogin()
}
