import axios, { type AxiosError, type InternalAxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '../stores/auth'
import router from '../router'

export interface ApiResponse<T = unknown> {
  code: number
  message: string
  data: T
}

const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api/admin',
  timeout: 15000,
  headers: {
    'Content-Type': 'application/json',
  },
})

// Request interceptor — attach admin Bearer token
request.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const authStore = useAuthStore()
    if (authStore.token) {
      config.headers.Authorization = `Bearer ${authStore.token}`
    }
    return config
  },
  (error) => Promise.reject(error),
)

// Response interceptor — unified error handling
request.interceptors.response.use(
  (response) => response,
  (error: AxiosError<ApiResponse>) => {
    const status = error.response?.status
    const message = error.response?.data?.message

    if (status === 401) {
      const authStore = useAuthStore()
      authStore.clearAuth()
      router.push({ name: 'Login' })
      ElMessage.error('登录已过期，请重新登录')
    } else if (status === 403) {
      ElMessage.error(message || '无操作权限')
    } else if (status === 404) {
      ElMessage.error(message || '请求的资源不存在')
    } else if (status && status >= 500) {
      ElMessage.error(message || '服务暂时不可用，请稍后重试')
    } else if (!error.response) {
      ElMessage.error('网络连接异常，请检查网络')
    }

    return Promise.reject(error)
  },
)

export default request
