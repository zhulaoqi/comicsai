import { describe, it, expect, beforeEach, vi } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia, defineStore } from 'pinia'
import { createRouter, createMemoryHistory } from 'vue-router'
import PaywallModal from './PaywallModal.vue'

vi.mock('../api/user', () => ({
  unlockContentApi: vi.fn(),
}))

vi.mock('../api/content', () => ({
  unlockChapterApi: vi.fn(),
}))

import { unlockContentApi } from '../api/user'
import { unlockChapterApi } from '../api/content'

const mockedUnlockContent = vi.mocked(unlockContentApi)
const mockedUnlockChapter = vi.mocked(unlockChapterApi)

function setupAuthStore(balance: number) {
  const pinia = createPinia()
  setActivePinia(pinia)

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

async function mountModal(
  props: { visible: boolean; contentId?: number; chapterId?: number; price: number },
  balance: number,
) {
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
    const wrapper = await mountModal({ visible: false, chapterId: 1, price: 5 }, 10)
    expect(wrapper.find('.paywall-modal').exists()).toBe(false)
  })

  it('renders modal when visible is true', async () => {
    const wrapper = await mountModal({ visible: true, chapterId: 1, price: 5 }, 10)
    expect(wrapper.find('.paywall-modal').exists()).toBe(true)
  })

  it('shows chapter price', async () => {
    const wrapper = await mountModal({ visible: true, chapterId: 1, price: 9.99 }, 20)
    const rows = wrapper.findAll('.paywall-modal__info-row')
    expect(rows[0].text()).toContain('¥9.99')
  })

  it('shows unlock button when balance is sufficient', async () => {
    const wrapper = await mountModal({ visible: true, chapterId: 1, price: 5 }, 10)
    const btn = wrapper.find('.paywall-modal__btn--primary')
    expect(btn.text()).toContain('解锁')
  })

  it('shows recharge link when balance is insufficient', async () => {
    const wrapper = await mountModal({ visible: true, chapterId: 1, price: 20 }, 5)
    const btn = wrapper.find('.paywall-modal__btn--primary')
    expect(btn.text()).toContain('去充值')
  })

  it('calls unlockChapterApi when chapterId is provided', async () => {
    mockedUnlockChapter.mockResolvedValue({} as never)

    const wrapper = await mountModal({ visible: true, chapterId: 42, price: 5 }, 10)
    await wrapper.find('.paywall-modal__btn--primary').trigger('click')
    await flushPromises()

    expect(mockedUnlockChapter).toHaveBeenCalledWith(42)
    expect(wrapper.emitted('unlock')).toBeTruthy()
  })

  it('calls unlockContentApi when contentId is provided (legacy)', async () => {
    mockedUnlockContent.mockResolvedValue({
      data: { code: 200, message: 'ok', data: { success: true, newBalance: 5 } },
    } as never)

    const wrapper = await mountModal({ visible: true, contentId: 42, price: 5 }, 10)
    await wrapper.find('.paywall-modal__btn--primary').trigger('click')
    await flushPromises()

    expect(mockedUnlockContent).toHaveBeenCalledWith(42)
    expect(wrapper.emitted('unlock')).toBeTruthy()
  })

  it('emits close event when close button is clicked', async () => {
    const wrapper = await mountModal({ visible: true, chapterId: 1, price: 5 }, 10)
    await wrapper.find('.paywall-modal__close').trigger('click')
    expect(wrapper.emitted('close')).toBeTruthy()
  })

  it('shows error message on unlock failure', async () => {
    mockedUnlockChapter.mockRejectedValue({
      response: { status: 500, data: { message: '服务器错误' } },
    })

    const wrapper = await mountModal({ visible: true, chapterId: 1, price: 5 }, 10)
    await wrapper.find('.paywall-modal__btn--primary').trigger('click')
    await flushPromises()

    expect(wrapper.find('.paywall-modal__error').text()).toBe('服务器错误')
  })
})
