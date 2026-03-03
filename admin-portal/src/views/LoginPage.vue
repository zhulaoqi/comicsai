<template>
  <div class="login-container">
    <el-card class="login-card">
      <div class="login-header">
        <h2>管理端登录</h2>
        <p class="login-hint">默认账号：admin@comicsai.com / admin123456</p>
      </div>
      <el-form :model="form" @submit.prevent="handleLogin">
        <el-form-item>
          <el-input v-model="form.email" placeholder="邮箱" prefix-icon="User" />
        </el-form-item>
        <el-form-item>
          <el-input v-model="form.password" type="password" placeholder="密码" prefix-icon="Lock" show-password />
        </el-form-item>
        <p v-if="errorMsg" class="error-msg">{{ errorMsg }}</p>
        <el-form-item>
          <el-button type="primary" native-type="submit" :loading="loading" style="width: 100%">登录</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import request from '@/api/request'
import type { ApiResponse } from '@/api/request'

interface AdminLoginVO {
  token: string
  id: number
  email: string
  nickname: string
}

const router = useRouter()
const authStore = useAuthStore()
const loading = ref(false)
const errorMsg = ref('')
const form = reactive({ email: '', password: '' })

async function handleLogin() {
  if (!form.email || !form.password) {
    errorMsg.value = '请输入邮箱和密码'
    return
  }
  loading.value = true
  errorMsg.value = ''
  try {
    const res = await request.post<ApiResponse<AdminLoginVO>>('/auth/login', {
      email: form.email,
      password: form.password,
    })
    const { token, id, email, nickname } = res.data.data
    authStore.setToken(token)
    authStore.setAdmin({ id, email, nickname })
    router.push('/')
  } catch (err: unknown) {
    const axiosErr = err as { response?: { data?: { message?: string }; status?: number } }
    if (axiosErr.response?.status === 401) {
      errorMsg.value = '邮箱或密码错误'
    } else {
      errorMsg.value = axiosErr.response?.data?.message ?? '登录失败，请稍后重试'
    }
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-container {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  background: #f5f7fa;
}

.login-card {
  width: 400px;
}

.login-header {
  text-align: center;
  margin-bottom: 24px;
}

.login-header h2 {
  margin: 0 0 8px;
  font-size: 22px;
  color: #303133;
}

.login-hint {
  font-size: 12px;
  color: #909399;
  margin: 0;
}

.error-msg {
  color: #f56c6c;
  font-size: 13px;
  margin: -8px 0 8px;
  text-align: center;
}
</style>
