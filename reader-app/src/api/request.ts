import axios, { type AxiosError, type InternalAxiosRequestConfig } from 'axios'
import { useAuthStore } from '../stores/auth'
import router from '../router'

export interface ApiResponse<T = unknown> {
  code: number
  message: string
  data: T
}

const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 15000,
  headers: {
    'Content-Type': 'application/json',
  },
})

// Request interceptor — attach Bearer token
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

    if (status === 401) {
      // Unauthorized — clear token and redirect to login
      const authStore = useAuthStore()
      authStore.clearAuth()
      router.push({ name: 'Login', query: { redirect: router.currentRoute.value.fullPath } })
    } else if (status === 403) {
      // Forbidden — paywall or insufficient balance
      // Components handle this via the rejected promise
    } else if (status === 404) {
      // Not found
    } else if (status && status >= 500) {
      // Server error
      console.error('Server error:', error.response?.data?.message || 'Service unavailable')
    } else if (!error.response) {
      // Network error
      console.error('Network error: please check your connection')
    }

    return Promise.reject(error)
  },
)

export default request
