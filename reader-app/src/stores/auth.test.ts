import { describe, it, expect, beforeEach, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useAuthStore } from './auth'

// Mock the auth API module
vi.mock('../api/auth', () => ({
  loginApi: vi.fn(),
  registerApi: vi.fn(),
  logoutApi: vi.fn(),
  getProfileApi: vi.fn(),
}))

import { loginApi, registerApi, logoutApi, getProfileApi } from '../api/auth'

const mockedLoginApi = vi.mocked(loginApi)
const mockedRegisterApi = vi.mocked(registerApi)
const mockedLogoutApi = vi.mocked(logoutApi)
const mockedGetProfileApi = vi.mocked(getProfileApi)

describe('auth store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    localStorage.clear()
    vi.clearAllMocks()
  })

  it('starts with no user and not logged in when no token in localStorage', () => {
    const store = useAuthStore()
    expect(store.isLoggedIn).toBe(false)
    expect(store.user).toBeNull()
    expect(store.token).toBeNull()
  })

  it('reads token from localStorage on init', () => {
    localStorage.setItem('token', 'saved-token')
    // Need a fresh pinia since the store reads localStorage at creation
    setActivePinia(createPinia())
    const store = useAuthStore()
    expect(store.token).toBe('saved-token')
    expect(store.isLoggedIn).toBe(true)
  })

  it('login sets token and user', async () => {
    const mockUser = { id: 1, email: 'test@example.com', nickname: 'Test', balance: 0 }
    mockedLoginApi.mockResolvedValue({
      data: { code: 200, message: 'ok', data: { token: 'jwt-token', user: mockUser } },
    } as never)

    const store = useAuthStore()
    await store.login({ email: 'test@example.com', password: '123456' })

    expect(store.token).toBe('jwt-token')
    expect(store.user).toEqual(mockUser)
    expect(store.isLoggedIn).toBe(true)
    expect(localStorage.getItem('token')).toBe('jwt-token')
  })

  it('register calls registerApi and returns user data', async () => {
    const mockUser = { id: 2, email: 'new@example.com', nickname: 'New', balance: 0 }
    mockedRegisterApi.mockResolvedValue({
      data: { code: 200, message: 'ok', data: mockUser },
    } as never)

    const store = useAuthStore()
    const result = await store.register({
      email: 'new@example.com',
      password: '123456',
      nickname: 'New',
    })

    expect(result).toEqual(mockUser)
    // Register should NOT auto-login
    expect(store.isLoggedIn).toBe(false)
  })

  it('logout clears auth state even if API fails', async () => {
    mockedLogoutApi.mockRejectedValue(new Error('network error'))

    const store = useAuthStore()
    store.setToken('some-token')
    store.setUser({ id: 1, email: 'a@b.com', nickname: 'A', balance: 10 })

    await store.logout()

    expect(store.token).toBeNull()
    expect(store.user).toBeNull()
    expect(store.isLoggedIn).toBe(false)
    expect(localStorage.getItem('token')).toBeNull()
  })

  it('logout calls logoutApi and clears state on success', async () => {
    mockedLogoutApi.mockResolvedValue({ data: { code: 200, message: 'ok', data: null } } as never)

    const store = useAuthStore()
    store.setToken('token-123')
    store.setUser({ id: 1, email: 'a@b.com', nickname: 'A', balance: 0 })

    await store.logout()

    expect(mockedLogoutApi).toHaveBeenCalled()
    expect(store.isLoggedIn).toBe(false)
  })

  it('fetchProfile sets user when token exists', async () => {
    const mockUser = { id: 1, email: 'a@b.com', nickname: 'A', balance: 50 }
    mockedGetProfileApi.mockResolvedValue({
      data: { code: 200, message: 'ok', data: mockUser },
    } as never)

    const store = useAuthStore()
    store.setToken('valid-token')

    await store.fetchProfile()

    expect(store.user).toEqual(mockUser)
  })

  it('fetchProfile clears auth when API fails', async () => {
    mockedGetProfileApi.mockRejectedValue(new Error('401'))

    const store = useAuthStore()
    store.setToken('expired-token')

    await store.fetchProfile()

    expect(store.token).toBeNull()
    expect(store.isLoggedIn).toBe(false)
  })

  it('fetchProfile does nothing when no token', async () => {
    const store = useAuthStore()
    await store.fetchProfile()
    expect(mockedGetProfileApi).not.toHaveBeenCalled()
  })

  it('clearAuth removes token from localStorage', () => {
    const store = useAuthStore()
    store.setToken('t')
    store.clearAuth()
    expect(localStorage.getItem('token')).toBeNull()
  })
})
