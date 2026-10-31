import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { getToken, setToken, removeToken } from '@/utils/auth'
import request from '@/utils/request'

export interface UserInfo {
  id: number
  username: string
  nickname?: string
  email?: string
}

export interface LoginPayload {
  username: string
  password: string
}

export interface LoginResult {
  accessToken: string
  tokenType: string
  expiresIn: number
  user: UserInfo
}

export const useUserStore = defineStore(
  'user',
  () => {
    const accessToken = ref<string>(getToken() || '')
    const userInfo = ref<UserInfo | null>(null)

    const isLoggedIn = computed(() => !!accessToken.value)
    const username = computed(() => userInfo.value?.username || '')

    async function login(payload: LoginPayload) {
      const res = await request({
        url: '/api/v1/auth/login',
        method: 'POST',
        data: payload,
      })
      const data: LoginResult = res.data.data
      accessToken.value = data.accessToken
      setToken(data.accessToken)
      userInfo.value = data.user
      return data
    }

    function logout() {
      accessToken.value = ''
      userInfo.value = null
      removeToken()
    }

    function setUser(u: any) {
      userInfo.value = u
    }

    return {
      accessToken,
      userInfo,
      isLoggedIn,
      username,
      login,
      logout,
      setUser,
    }
  },
  {
    persist: {
      pick: ['accessToken', 'userInfo'],
    },
  }
)
