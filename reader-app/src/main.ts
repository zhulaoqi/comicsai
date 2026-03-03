import { createApp } from 'vue'
import { createPinia } from 'pinia'
import router from './router'
import App from './App.vue'
import './styles/global.css'

const app = createApp(App)
const pinia = createPinia()

app.use(pinia)
app.use(router)

// Restore user profile on page load if token exists
router.isReady().then(async () => {
  const { useAuthStore } = await import('./stores/auth')
  const authStore = useAuthStore()
  if (authStore.token) {
    authStore.fetchProfile()
  }
})

app.mount('#app')
