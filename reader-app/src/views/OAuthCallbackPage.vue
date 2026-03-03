<template>
  <div class="oauth-callback">
    <div v-if="error" class="callback-error">
      <p>登录失败：{{ error }}</p>
      <router-link to="/login">返回登录</router-link>
    </div>
    <div v-else class="callback-loading">
      <div class="spinner" aria-hidden="true"></div>
      <p>正在登录，请稍候...</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { getProfileApi } from '../api/auth'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const error = ref('')

onMounted(async () => {
  const token = route.query.token as string
  const errorMsg = route.query.error as string

  if (errorMsg) {
    error.value = decodeURIComponent(errorMsg)
    return
  }

  if (!token) {
    error.value = '未获取到登录凭证'
    return
  }

  try {
    authStore.setToken(token)
    // Fetch user profile to populate store
    const res = await getProfileApi()
    authStore.setUser(res.data.data)
    router.push('/')
  } catch {
    authStore.clearAuth()
    error.value = '登录失败，请重试'
  }
})
</script>

<style scoped>
.oauth-callback {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  background: var(--color-bg);
}

.callback-loading,
.callback-error {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--spacing-lg);
  color: var(--color-text-secondary);
  font-size: var(--font-size-base);
}

.callback-error a {
  color: var(--color-primary);
  text-decoration: underline;
}

.spinner {
  width: 40px;
  height: 40px;
  border: 3px solid var(--color-border);
  border-top-color: var(--color-primary);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}
</style>
