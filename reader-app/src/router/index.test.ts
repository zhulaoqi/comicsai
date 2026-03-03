import { describe, it, expect, beforeEach } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { useAuthStore } from '../stores/auth'
import router from './index'

describe('route guard', () => {
  beforeEach(() => {
    localStorage.clear()
    setActivePinia(createPinia())
  })

  it('redirects guest to login when accessing auth-required route', async () => {
    const store = useAuthStore()
    expect(store.isLoggedIn).toBe(false)

    await router.push('/profile')
    await router.isReady()

    expect(router.currentRoute.value.name).toBe('Login')
    expect(router.currentRoute.value.query.redirect).toBe('/profile')
  })

  it('allows guest to access public routes', async () => {
    await router.push('/')
    await router.isReady()

    expect(router.currentRoute.value.name).toBe('Home')
  })

  it('allows logged-in user to access auth-required routes', async () => {
    const store = useAuthStore()
    store.setToken('valid-token')

    await router.push('/profile')
    await router.isReady()

    expect(router.currentRoute.value.name).toBe('Profile')
  })

  it('redirects logged-in user away from login page', async () => {
    const store = useAuthStore()
    store.setToken('valid-token')

    await router.push('/login')
    await router.isReady()

    expect(router.currentRoute.value.name).toBe('Home')
  })

  it('preserves redirect query for recharge page', async () => {
    const store = useAuthStore()
    expect(store.isLoggedIn).toBe(false)

    await router.push('/recharge')
    await router.isReady()

    expect(router.currentRoute.value.name).toBe('Login')
    expect(router.currentRoute.value.query.redirect).toBe('/recharge')
  })
})
