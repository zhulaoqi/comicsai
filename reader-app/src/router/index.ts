import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    name: 'Home',
    component: () => import('../views/HomePage.vue'),
  },
  {
    path: '/comic/:id',
    name: 'ComicReader',
    component: () => import('../views/ComicReader.vue'),
  },
  {
    path: '/novel/:id',
    name: 'NovelReader',
    component: () => import('../views/NovelReader.vue'),
  },
  {
    path: '/search',
    name: 'Search',
    component: () => import('../views/SearchPage.vue'),
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/LoginPage.vue'),
  },
  {
    path: '/profile',
    name: 'Profile',
    component: () => import('../views/ProfilePage.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: '/recharge',
    name: 'Recharge',
    component: () => import('../views/RechargePage.vue'),
    meta: { requiresAuth: true },
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

// Route guard: intercept access to auth-required pages for guests
router.beforeEach((to, _from, next) => {
  const authStore = useAuthStore()

  if (to.meta.requiresAuth && !authStore.isLoggedIn) {
    // Redirect guest to login, preserving the intended destination
    next({ name: 'Login', query: { redirect: to.fullPath } })
  } else if (to.name === 'Login' && authStore.isLoggedIn) {
    // Already logged in, redirect away from login page
    next({ name: 'Home' })
  } else {
    next()
  }
})

export default router
