import { describe, it, expect, beforeEach, vi } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { createRouter, createMemoryHistory } from 'vue-router'
import LoginPage from './LoginPage.vue'

// Mock the auth API
vi.mock('../api/auth', () => ({
  loginApi: vi.fn(),
  registerApi: vi.fn(),
  logoutApi: vi.fn(),
  getProfileApi: vi.fn(),
}))

import { loginApi, registerApi } from '../api/auth'

const mockedLoginApi = vi.mocked(loginApi)
const mockedRegisterApi = vi.mocked(registerApi)

function createTestRouter() {
  return createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/', name: 'Home', component: { template: '<div>Home</div>' } },
      { path: '/login', name: 'Login', component: LoginPage },
    ],
  })
}

function mountLoginPage() {
  const pinia = createPinia()
  setActivePinia(pinia)
  const router = createTestRouter()

  return mount(LoginPage, {
    global: {
      plugins: [pinia, router],
    },
  })
}

describe('LoginPage', () => {
  beforeEach(() => {
    localStorage.clear()
    vi.clearAllMocks()
  })

  it('renders login form by default', () => {
    const wrapper = mountLoginPage()
    expect(wrapper.find('.login-title').text()).toBe('登录')
    expect(wrapper.find('#email').exists()).toBe(true)
    expect(wrapper.find('#password').exists()).toBe(true)
    // Nickname and confirm password should not be visible in login mode
    expect(wrapper.find('#nickname').exists()).toBe(false)
    expect(wrapper.find('#confirmPassword').exists()).toBe(false)
  })

  it('toggles to register form', async () => {
    const wrapper = mountLoginPage()
    await wrapper.find('.toggle-btn').trigger('click')

    expect(wrapper.find('.login-title').text()).toBe('注册')
    expect(wrapper.find('#nickname').exists()).toBe(true)
    expect(wrapper.find('#confirmPassword').exists()).toBe(true)
  })

  it('toggles back to login form', async () => {
    const wrapper = mountLoginPage()
    await wrapper.find('.toggle-btn').trigger('click')
    await wrapper.find('.toggle-btn').trigger('click')

    expect(wrapper.find('.login-title').text()).toBe('登录')
    expect(wrapper.find('#nickname').exists()).toBe(false)
  })

  it('shows error when register passwords do not match', async () => {
    const wrapper = mountLoginPage()
    // Switch to register mode
    await wrapper.find('.toggle-btn').trigger('click')

    await wrapper.find('#email').setValue('test@example.com')
    await wrapper.find('#nickname').setValue('Test')
    await wrapper.find('#password').setValue('123456')
    await wrapper.find('#confirmPassword').setValue('654321')

    await wrapper.find('form').trigger('submit')
    await flushPromises()

    expect(wrapper.find('.form-error').text()).toBe('两次输入的密码不一致')
  })

  it('calls login API on login submit', async () => {
    const mockUser = { id: 1, email: 'test@example.com', nickname: 'Test', balance: 0 }
    mockedLoginApi.mockResolvedValue({
      data: { code: 200, message: 'ok', data: { token: 'jwt', user: mockUser } },
    } as never)

    const wrapper = mountLoginPage()
    await wrapper.find('#email').setValue('test@example.com')
    await wrapper.find('#password').setValue('123456')

    await wrapper.find('form').trigger('submit')
    await flushPromises()

    expect(mockedLoginApi).toHaveBeenCalledWith({
      email: 'test@example.com',
      password: '123456',
    })
  })

  it('shows error message on login failure', async () => {
    mockedLoginApi.mockRejectedValue({
      response: { status: 401, data: { message: '邮箱或密码错误' } },
    })

    const wrapper = mountLoginPage()
    await wrapper.find('#email').setValue('test@example.com')
    await wrapper.find('#password').setValue('wrong')

    await wrapper.find('form').trigger('submit')
    await flushPromises()

    expect(wrapper.find('.form-error').text()).toBe('邮箱或密码错误')
  })

  it('shows duplicate email error on 409', async () => {
    const wrapper = mountLoginPage()
    // Switch to register mode
    await wrapper.find('.toggle-btn').trigger('click')

    mockedRegisterApi.mockRejectedValue({
      response: { status: 409, data: { message: '该邮箱已注册' } },
    })

    await wrapper.find('#email').setValue('dup@example.com')
    await wrapper.find('#nickname').setValue('Dup')
    await wrapper.find('#password').setValue('123456')
    await wrapper.find('#confirmPassword').setValue('123456')

    await wrapper.find('form').trigger('submit')
    await flushPromises()

    expect(wrapper.find('.form-error').text()).toBe('该邮箱已注册，请直接登录')
  })

  it('disables submit button while loading', async () => {
    // Make login hang
    mockedLoginApi.mockReturnValue(new Promise(() => {}))

    const wrapper = mountLoginPage()
    await wrapper.find('#email').setValue('test@example.com')
    await wrapper.find('#password').setValue('123456')

    await wrapper.find('form').trigger('submit')
    // Don't flush — still loading

    expect(wrapper.find('.form-submit').attributes('disabled')).toBeDefined()
    expect(wrapper.find('.form-submit').text()).toContain('处理中')
  })
})
