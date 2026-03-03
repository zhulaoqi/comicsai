import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export const useAuthStore = defineStore('auth', () => {
  const token = ref<string | null>(localStorage.getItem('admin_token'))
  const admin = ref<{ id: number; email: string; nickname: string } | null>(null)

  const isLoggedIn = computed(() => !!token.value)

  function setToken(newToken: string) {
    token.value = newToken
    localStorage.setItem('admin_token', newToken)
  }

  function clearAuth() {
    token.value = null
    admin.value = null
    localStorage.removeItem('admin_token')
  }

  function setAdmin(data: { id: number; email: string; nickname: string }) {
    admin.value = data
  }

  return { token, admin, isLoggedIn, setToken, clearAuth, setAdmin }
})
