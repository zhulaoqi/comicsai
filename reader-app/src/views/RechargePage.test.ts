import { describe, it, expect, beforeEach, vi } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { createRouter, createMemoryHistory } from 'vue-router'
import RechargePage from './RechargePage.vue'

vi.mock('../api/user', () => ({
  getUserProfileApi: vi.fn(),
  rechargeApi: vi.fn(),
  getRechargeRecordsApi: vi.fn(),
}))

import { getUserProfileApi, rechargeApi } from '../api/user'

const mockedGetProfile = vi.mocked(getUserProfileApi)
const mockedRecharge = vi.mocked(rechargeApi)

function createTestRouter() {
  return createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/', name: 'Home', component: { template: '<div />' } },
      { path: '/profile', name: 'Profile', component: { template: '<div />' } },
      { path: '/recharge', name: 'Recharge', component: RechargePage },
    ],
  })
}

async function mountRechargePage() {
  const pinia = createPinia()
  setActivePinia(pinia)
  const router = createTestRouter()
  await router.push('/recharge')
  await router.isReady()

  return mount(RechargePage, {
    global: { plugins: [pinia, router] },
  })
}

describe('RechargePage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mockedGetProfile.mockResolvedValue({
      data: { code: 200, message: 'ok', data: { id: 1, nickname: 'Alice', email: 'a@b.com', balance: 20, createdAt: '' } },
    } as never)
  })

  it('renders preset amount options', async () => {
    const wrapper = await mountRechargePage()
    await flushPromises()

    const options = wrapper.findAll('.amount-option')
    expect(options).toHaveLength(4)
    expect(options[0].find('.amount-option__value').text()).toBe('¥10')
    expect(options[1].find('.amount-option__value').text()).toBe('¥30')
    expect(options[2].find('.amount-option__value').text()).toBe('¥50')
    expect(options[3].find('.amount-option__value').text()).toBe('¥100')
  })

  it('shows current balance after loading', async () => {
    const wrapper = await mountRechargePage()
    await flushPromises()

    expect(wrapper.find('.balance-display__amount').text()).toBe('¥20.00')
  })

  it('selects preset amount on click', async () => {
    const wrapper = await mountRechargePage()
    await flushPromises()

    const options = wrapper.findAll('.amount-option')
    await options[2].trigger('click') // ¥50

    expect(options[2].classes()).toContain('amount-option--active')
    expect(wrapper.find('.btn-confirm').text()).toContain('¥50.00')
  })

  it('updates summary when preset is selected', async () => {
    const wrapper = await mountRechargePage()
    await flushPromises()

    const options = wrapper.findAll('.amount-option')
    await options[3].trigger('click') // ¥100

    const rows = wrapper.findAll('.recharge-summary__row')
    expect(rows[0].find('.recharge-summary__value').text()).toBe('¥100.00')
    // After recharge: 20 + 100 = 120
    expect(rows[1].find('.recharge-summary__value').text()).toBe('¥120.00')
  })

  it('disables confirm button when custom mode has empty input', async () => {
    const wrapper = await mountRechargePage()
    await flushPromises()

    // Switch to custom mode with empty input — finalAmount becomes 0
    const input = wrapper.find('.custom-amount__input')
    await input.trigger('focus')
    await input.setValue('')

    const btn = wrapper.find('.btn-confirm')
    expect(btn.attributes('disabled')).toBeDefined()
  })

  it('calls rechargeApi with selected amount on confirm', async () => {
    mockedRecharge.mockResolvedValue({
      data: { code: 200, message: 'ok', data: { balance: 50 } },
    } as never)

    const wrapper = await mountRechargePage()
    await flushPromises()

    // Select ¥30 (default)
    await wrapper.find('.btn-confirm').trigger('click')
    await flushPromises()

    expect(mockedRecharge).toHaveBeenCalledWith(30)
  })

  it('shows success overlay after successful recharge', async () => {
    mockedRecharge.mockResolvedValue({
      data: { code: 200, message: 'ok', data: { balance: 50 } },
    } as never)

    const wrapper = await mountRechargePage()
    await flushPromises()

    await wrapper.find('.btn-confirm').trigger('click')
    await flushPromises()

    expect(wrapper.find('.success-overlay').exists()).toBe(true)
    expect(wrapper.find('.success-balance').text()).toBe('余额：¥50.00')
  })

  it('shows error message on recharge failure', async () => {
    mockedRecharge.mockRejectedValue({
      response: { data: { message: '充值服务暂不可用' } },
    })

    const wrapper = await mountRechargePage()
    await flushPromises()

    await wrapper.find('.btn-confirm').trigger('click')
    await flushPromises()

    expect(wrapper.find('.recharge-error').text()).toBe('充值服务暂不可用')
  })

  it('custom amount input switches to custom mode', async () => {
    const wrapper = await mountRechargePage()
    await flushPromises()

    const input = wrapper.find('.custom-amount__input')
    await input.trigger('focus')
    await input.setValue('75')

    // Confirm button should show custom amount
    expect(wrapper.find('.btn-confirm').text()).toContain('¥75.00')
  })
})
