import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/LoginPage.vue'),
  },
  {
    path: '/',
    name: 'Layout',
    component: () => import('../views/LayoutPage.vue'),
    redirect: '/storyline',
    meta: { requiresAuth: true },
    children: [
      {
        path: 'storyline',
        name: 'StorylineManage',
        component: () => import('../views/StorylineManage.vue'),
        meta: { title: '故事线管理' },
      },
      {
        path: 'content',
        name: 'ContentManage',
        component: () => import('../views/ContentManage.vue'),
        meta: { title: '内容管理' },
      },
      {
        path: 'content/review/:id',
        name: 'ContentReview',
        component: () => import('../views/ContentReview.vue'),
        meta: { title: '内容审核' },
      },
      {
        path: 'paid',
        name: 'PaidManage',
        component: () => import('../views/PaidManage.vue'),
        meta: { title: '付费管理' },
      },
      {
        path: 'analytics/user',
        name: 'UserAnalytics',
        component: () => import('../views/UserAnalytics.vue'),
        meta: { title: '用户使用统计' },
      },
      {
        path: 'analytics/token',
        name: 'TokenAnalytics',
        component: () => import('../views/TokenAnalytics.vue'),
        meta: { title: 'Token消耗看板' },
      },
      {
        path: 'analytics/recharge',
        name: 'RechargeAnalytics',
        component: () => import('../views/RechargeAnalytics.vue'),
        meta: { title: '充值统计看板' },
      },
    ],
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

// Navigation guard — redirect to login if not authenticated
router.beforeEach((to, _from, next) => {
  const token = localStorage.getItem('admin_token')
  if (to.meta.requiresAuth && !token) {
    next({ name: 'Login' })
  } else {
    next()
  }
})

export default router
