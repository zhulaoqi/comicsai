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
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '../stores/auth'

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
      // After successful registration, switch to login mode
      resetForm()
      isLoginMode.value = true
      errorMsg.value = ''
      // Show a brief success hint via a temporary message
      errorMsg.value = ''
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
</style>
