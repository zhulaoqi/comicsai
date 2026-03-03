import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import {
  loginApi,
  registerApi,
  logoutApi,
  getProfileApi,
  type LoginParams,
  type RegisterParams,
  type UserInfo,
} from '../api/auth'

export const useAuthStore = defineStore('auth', () => {
  const token = ref<string | null>(localStorage.getItem('token'))
  const user = ref<UserInfo | null>(null)

  const isLoggedIn = computed(() => !!token.value)

  function setToken(newToken: string) {
    token.value = newToken
    localStorage.setItem('token', newToken)
  }

  function clearAuth() {
    token.value = null
    user.value = null
    localStorage.removeItem('token')
  }

  function setUser(userData: UserInfo) {
    user.value = userData
  }

  async function login(params: LoginParams) {
    const res = await loginApi(params)
    const { token: newToken, user: userData } = res.data.data
    setToken(newToken)
    setUser(userData)
  }

  async function register(params: RegisterParams) {
    const res = await registerApi(params)
    return res.data.data
  }

  async function logout() {
    try {
      await logoutApi()
    } catch {
      // Ignore API errors — always clear local auth state
    } finally {
      clearAuth()
    }
  }

  async function fetchProfile() {
    if (!token.value) return
    try {
      const res = await getProfileApi()
      setUser(res.data.data)
    } catch {
      clearAuth()
    }
  }

  return {
    token,
    user,
    isLoggedIn,
    setToken,
    clearAuth,
    setUser,
    login,
    register,
    logout,
    fetchProfile,
  }
})
