import { describe, it, expect, beforeEach, vi } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia, defineStore } from 'pinia'
import { createRouter, createMemoryHistory } from 'vue-router'
import PaywallModal from './PaywallModal.vue'

vi.mock('../api/user', () => ({
  unlockContentApi: vi.fn(),
}))

import { unlockContentApi } from '../api/user'

const mockedUnlock = vi.mocked(unlockContentApi)

// Helper to set up auth store with a given balance
function setupAuthStore(balance: number) {
  const pinia = createPinia()
  setActivePinia(pinia)

  // Manually set user in auth store
  const useAuthStore = defineStore('auth', {
    state: () => ({
      token: 'test-token',
      user: { id: 1, nickname: 'Alice', email: 'a@b.com', balance },
    }),
    getters: {
      isLoggedIn: (state) => !!state.token,
    },
    actions: {
      setUser(userData: { id: number; nickname: string; email: string; balance: number }) {
        this.user = userData
      },
      clearAuth() {
        this.token = null
        this.user = null
      },
    },
  })
  useAuthStore()
  return pinia
}

function createTestRouter() {
  return createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/', name: 'Home', component: { template: '<div />' } },
      { path: '/recharge', name: 'Recharge', component: { template: '<div />' } },
    ],
  })
}

async function mountModal(props: { visible: boolean; contentId: number; price: number }, balance: number) {
  const pinia = setupAuthStore(balance)
  const router = createTestRouter()
  await router.push('/')
  await router.isReady()

  return mount(PaywallModal, {
    props,
    global: {
      plugins: [pinia, router],
      stubs: { Teleport: true },
    },
  })
}

describe('PaywallModal', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('renders nothing when visible is false', async () => {
    const wrapper = await mountModal({ visible: false, contentId: 1, price: 5 }, 10)
    expect(wrapper.find('.paywall-modal').exists()).toBe(false)
  })

  it('renders modal when visible is true', async () => {
    const wrapper = await mountModal({ visible: true, contentId: 1, price: 5 }, 10)
    expect(wrapper.find('.paywall-modal').exists()).toBe(true)
  })

  it('shows content price', async () => {
    const wrapper = await mountModal({ visible: true, contentId: 1, price: 9.99 }, 20)
    const rows = wrapper.findAll('.paywall-modal__info-row')
    expect(rows[0].text()).toContain('¥9.99')
  })

  it('shows current balance', async () => {
    const wrapper = await mountModal({ visible: true, contentId: 1, price: 5 }, 42.5)
    const rows = wrapper.findAll('.paywall-modal__info-row')
    expect(rows[1].text()).toContain('¥42.50')
  })

  it('shows unlock button when balance is sufficient', async () => {
    const wrapper = await mountModal({ visible: true, contentId: 1, price: 5 }, 10)
    const btn = wrapper.find('.paywall-modal__btn--primary')
    expect(btn.text()).toContain('解锁')
  })

  it('shows recharge link when balance is insufficient', async () => {
    const wrapper = await mountModal({ visible: true, contentId: 1, price: 20 }, 5)
    const btn = wrapper.find('.paywall-modal__btn--primary')
    expect(btn.text()).toContain('去充值')
    expect(btn.attributes('href')).toBe('/recharge')
  })

  it('shows shortfall amount when balance is insufficient', async () => {
    const wrapper = await mountModal({ visible: true, contentId: 1, price: 20 }, 5)
    const rows = wrapper.findAll('.paywall-modal__info-row')
    // Third row shows shortfall
    expect(rows[2].text()).toContain('¥15.00')
  })

  it('emits close event when close button is clicked', async () => {
    const wrapper = await mountModal({ visible: true, contentId: 1, price: 5 }, 10)
    await wrapper.find('.paywall-modal__close').trigger('click')
    expect(wrapper.emitted('close')).toBeTruthy()
  })

  it('emits close event when cancel button is clicked', async () => {
    const wrapper = await mountModal({ visible: true, contentId: 1, price: 5 }, 10)
    await wrapper.find('.paywall-modal__btn--secondary').trigger('click')
    expect(wrapper.emitted('close')).toBeTruthy()
  })

  it('calls unlockContentApi and emits unlock on success', async () => {
    mockedUnlock.mockResolvedValue({
      data: { code: 200, message: 'ok', data: { success: true, newBalance: 5 } },
    } as never)

    const wrapper = await mountModal({ visible: true, contentId: 42, price: 5 }, 10)
    await wrapper.find('.paywall-modal__btn--primary').trigger('click')
    await flushPromises()

    expect(mockedUnlock).toHaveBeenCalledWith(42)
    expect(wrapper.emitted('unlock')).toBeTruthy()
  })

  it('shows error message on unlock failure', async () => {
    mockedUnlock.mockRejectedValue({
      response: { status: 500, data: { message: '服务器错误' } },
    })

    const wrapper = await mountModal({ visible: true, contentId: 1, price: 5 }, 10)
    await wrapper.find('.paywall-modal__btn--primary').trigger('click')
    await flushPromises()

    expect(wrapper.find('.paywall-modal__error').text()).toBe('服务器错误')
  })

  it('shows insufficient balance error on 402', async () => {
    mockedUnlock.mockRejectedValue({
      response: { status: 402, data: { message: '余额不足' } },
    })

    const wrapper = await mountModal({ visible: true, contentId: 1, price: 5 }, 10)
    await wrapper.find('.paywall-modal__btn--primary').trigger('click')
    await flushPromises()

    expect(wrapper.find('.paywall-modal__error').text()).toBe('余额不足，请先充值')
  })
})
