<template>
  <div class="login-page">
    <div class="login-card">
      <h1 class="login-title">{{ isLoginMode ? '登录' : '注册' }}</h1>
      <p class="login-subtitle">
        {{ isLoginMode ? '欢迎回来，继续你的阅读之旅' : '创建账号，开启精彩阅读体验' }}
      </p>

      <form class="login-form" @submit.prevent="handleSubmit">
        <!-- Email -->
        <div class="form-group">
          <label class="form-label" for="email">邮箱</label>
          <input
            id="email"
            v-model="form.email"
            type="email"
            class="form-input"
            placeholder="请输入邮箱地址"
            autocomplete="email"
            required
          />
        </div>

        <!-- Nickname (register only) -->
        <div v-if="!isLoginMode" class="form-group">
          <label class="form-label" for="nickname">昵称</label>
          <input
            id="nickname"
            v-model="form.nickname"
            type="text"
            class="form-input"
            placeholder="请输入昵称"
            autocomplete="nickname"
            required
          />
        </div>

        <!-- Password -->
        <div class="form-group">
          <label class="form-label" for="password">密码</label>
          <input
            id="password"
            v-model="form.password"
            type="password"
            class="form-input"
            placeholder="请输入密码（至少6位）"
            autocomplete="current-password"
            minlength="6"
            required
          />
        </div>

        <!-- Confirm Password (register only) -->
        <div v-if="!isLoginMode" class="form-group">
          <label class="form-label" for="confirmPassword">确认密码</label>
          <input
            id="confirmPassword"
            v-model="form.confirmPassword"
            type="password"
            class="form-input"
            placeholder="请再次输入密码"
            autocomplete="new-password"
            minlength="6"
            required
          />
        </div>

        <!-- Error message -->
        <p v-if="errorMsg" class="form-error" role="alert">{{ errorMsg }}</p>

        <!-- Submit -->
        <button type="submit" class="form-submit" :disabled="loading">
          <span v-if="loading" class="spinner" aria-hidden="true"></span>
          {{ loading ? '处理中...' : isLoginMode ? '登录' : '注册' }}
        </button>
      </form>

      <!-- Toggle mode -->
      <p class="toggle-text">
        {{ isLoginMode ? '还没有账号？' : '已有账号？' }}
        <button class="toggle-btn" type="button" @click="toggleMode">
          {{ isLoginMode ? '立即注册' : '去登录' }}
        </button>
      </p>

      <!-- OAuth divider -->
      <div class="oauth-divider">
        <span>或使用第三方账号登录</span>
      </div>

      <!-- OAuth buttons -->
      <div class="oauth-buttons">
        <button class="oauth-btn oauth-btn--github" type="button" @click="loginWithOAuth('github')">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="currentColor">
            <path d="M12 0C5.37 0 0 5.37 0 12c0 5.31 3.435 9.795 8.205 11.385.6.105.825-.255.825-.57 0-.285-.015-1.23-.015-2.235-3.015.555-3.795-.735-4.035-1.41-.135-.345-.72-1.41-1.23-1.695-.42-.225-1.02-.78-.015-.795.945-.015 1.62.87 1.845 1.23 1.08 1.815 2.805 1.305 3.495.99.105-.78.42-1.305.765-1.605-2.67-.3-5.46-1.335-5.46-5.925 0-1.305.465-2.385 1.23-3.225-.12-.3-.54-1.53.12-3.18 0 0 1.005-.315 3.3 1.23.96-.27 1.98-.405 3-.405s2.04.135 3 .405c2.295-1.56 3.3-1.23 3.3-1.23.66 1.65.24 2.88.12 3.18.765.84 1.23 1.905 1.23 3.225 0 4.605-2.805 5.625-5.475 5.925.435.375.81 1.095.81 2.22 0 1.605-.015 2.895-.015 3.3 0 .315.225.69.825.57A12.02 12.02 0 0 0 24 12c0-6.63-5.37-12-12-12z"/>
          </svg>
          GitHub 登录
        </button>
        <button class="oauth-btn oauth-btn--google" type="button" @click="loginWithOAuth('google')">
          <svg width="18" height="18" viewBox="0 0 24 24">
            <path fill="#4285F4" d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z"/>
            <path fill="#34A853" d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z"/>
            <path fill="#FBBC05" d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z"/>
            <path fill="#EA4335" d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z"/>
          </svg>
          Google 登录
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import request from '../api/request'
import type { ApiResponse } from '../api/request'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

const isLoginMode = ref(true)
const loading = ref(false)
const errorMsg = ref('')

const form = reactive({
  email: '',
  password: '',
  confirmPassword: '',
  nickname: '',
})

function toggleMode() {
  isLoginMode.value = !isLoginMode.value
  errorMsg.value = ''
}

function resetForm() {
  form.email = ''
  form.password = ''
  form.confirmPassword = ''
  form.nickname = ''
  errorMsg.value = ''
}

async function handleSubmit() {
  errorMsg.value = ''

  if (!isLoginMode.value && form.password !== form.confirmPassword) {
    errorMsg.value = '两次输入的密码不一致'
    return
  }

  loading.value = true
  try {
    if (isLoginMode.value) {
      await authStore.login({ email: form.email, password: form.password })
      const redirect = (route.query.redirect as string) || '/'
      router.push(redirect)
    } else {
      await authStore.register({
        email: form.email,
        password: form.password,
        nickname: form.nickname,
      })
      resetForm()
      isLoginMode.value = true
      alert('注册成功，请登录')
    }
  } catch (err: unknown) {
    const axiosErr = err as { response?: { data?: { message?: string }; status?: number } }
    if (axiosErr.response?.status === 409) {
      errorMsg.value = '该邮箱已注册，请直接登录'
    } else if (axiosErr.response?.data?.message) {
      errorMsg.value = axiosErr.response.data.message
    } else {
      errorMsg.value = isLoginMode.value ? '登录失败，请检查邮箱和密码' : '注册失败，请稍后重试'
    }
  } finally {
    loading.value = false
  }
}

async function loginWithOAuth(provider: string) {
  // Redirect directly to Spring Security OAuth2 authorization endpoint
  window.location.href = `http://localhost:8080/oauth2/authorization/${provider}`
}
</script>

<style scoped>
.login-page {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  padding: var(--spacing-lg);
  background-color: var(--color-bg);
}

.login-card {
  width: 100%;
  max-width: 420px;
  padding: var(--spacing-3xl) var(--spacing-2xl);
  background: var(--color-bg-card);
  border-radius: var(--radius-xl);
  box-shadow: var(--shadow-lg);
}

.login-title {
  font-size: var(--font-size-3xl);
  font-weight: var(--font-weight-bold);
  color: var(--color-text-primary);
  text-align: center;
  margin-bottom: var(--spacing-sm);
}

.login-subtitle {
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
  text-align: center;
  margin-bottom: var(--spacing-2xl);
}

.login-form {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-lg);
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-xs);
}

.form-label {
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-medium);
  color: var(--color-text-primary);
}

.form-input {
  padding: var(--spacing-sm) var(--spacing-md);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  font-size: var(--font-size-base);
  color: var(--color-text-primary);
  background: var(--color-bg);
  transition: border-color var(--transition-fast), box-shadow var(--transition-fast);
  outline: none;
}

.form-input::placeholder {
  color: var(--color-text-muted);
}

.form-input:focus {
  border-color: var(--color-primary);
  box-shadow: 0 0 0 3px rgba(74, 108, 247, 0.1);
}

.form-error {
  font-size: var(--font-size-sm);
  color: var(--color-error);
  text-align: center;
  margin: 0;
}

.form-submit {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--spacing-sm);
  width: 100%;
  padding: var(--spacing-sm) var(--spacing-lg);
  background: var(--color-primary);
  color: var(--color-text-inverse);
  font-size: var(--font-size-base);
  font-weight: var(--font-weight-semibold);
  border-radius: var(--radius-md);
  transition: background var(--transition-fast);
  min-height: 44px;
}

.form-submit:hover:not(:disabled) {
  background: var(--color-primary-dark);
}

.form-submit:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.spinner {
  width: 16px;
  height: 16px;
  border: 2px solid rgba(255, 255, 255, 0.3);
  border-top-color: #fff;
  border-radius: 50%;
  animation: spin 0.6s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

.toggle-text {
  text-align: center;
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
  margin-top: var(--spacing-xl);
}

.toggle-btn {
  color: var(--color-primary);
  font-weight: var(--font-weight-medium);
  font-size: var(--font-size-sm);
  transition: color var(--transition-fast);
}

.toggle-btn:hover {
  color: var(--color-primary-dark);
}

/* Responsive */
@media (max-width: 480px) {
  .login-card {
    padding: var(--spacing-2xl) var(--spacing-lg);
  }

  .login-title {
    font-size: var(--font-size-2xl);
  }
}

/* OAuth */
.oauth-divider {
  display: flex;
  align-items: center;
  gap: var(--spacing-md);
  margin-top: var(--spacing-xl);
  color: var(--color-text-muted);
  font-size: var(--font-size-sm);
}

.oauth-divider::before,
.oauth-divider::after {
  content: '';
  flex: 1;
  height: 1px;
  background: var(--color-border);
}

.oauth-buttons {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-sm);
  margin-top: var(--spacing-md);
}

.oauth-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--spacing-sm);
  width: 100%;
  padding: var(--spacing-sm) var(--spacing-lg);
  border-radius: var(--radius-md);
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-medium);
  cursor: pointer;
  transition: background var(--transition-fast), opacity var(--transition-fast);
  min-height: 44px;
  border: 1px solid var(--color-border);
}

.oauth-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.oauth-btn--github {
  background: #24292e;
  color: #fff;
  border-color: #24292e;
}

.oauth-btn--github:hover:not(:disabled) {
  background: #1a1e22;
}

.oauth-btn--google {
  background: #fff;
  color: #3c4043;
}

.oauth-btn--google:hover:not(:disabled) {
  background: #f8f9fa;
}
</style>
