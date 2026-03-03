import { describe, it, expect, beforeEach, vi } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { createRouter, createMemoryHistory } from 'vue-router'
import ProfilePage from './ProfilePage.vue'

vi.mock('../api/user', () => ({
  getUserProfileApi: vi.fn(),
  getRechargeRecordsApi: vi.fn(),
}))

import { getUserProfileApi, getRechargeRecordsApi } from '../api/user'

const mockedGetProfile = vi.mocked(getUserProfileApi)
const mockedGetRecords = vi.mocked(getRechargeRecordsApi)

const mockProfile = {
  id: 1,
  nickname: 'Alice',
  email: 'alice@example.com',
  balance: 42.5,
  createdAt: '2024-01-15T10:00:00',
}

const mockRecords = [
  { id: 1, amount: 30, createdAt: '2024-03-01T12:00:00' },
  { id: 2, amount: 10, createdAt: '2024-04-10T08:00:00' },
]

function createTestRouter() {
  return createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/', name: 'Home', component: { template: '<div />' } },
      { path: '/profile', name: 'Profile', component: ProfilePage },
      { path: '/recharge', name: 'Recharge', component: { template: '<div />' } },
    ],
  })
}

async function mountProfilePage() {
  const pinia = createPinia()
  setActivePinia(pinia)
  const router = createTestRouter()
  await router.push('/profile')
  await router.isReady()

  return mount(ProfilePage, {
    global: { plugins: [pinia, router] },
  })
}

describe('ProfilePage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('shows loading state initially', async () => {
    mockedGetProfile.mockReturnValue(new Promise(() => {}))
    mockedGetRecords.mockResolvedValue({ data: { code: 200, message: 'ok', data: [] } } as never)

    const wrapper = await mountProfilePage()
    expect(wrapper.find('.spinner').exists()).toBe(true)
  })

  it('renders user info after loading', async () => {
    mockedGetProfile.mockResolvedValue({
      data: { code: 200, message: 'ok', data: mockProfile },
    } as never)
    mockedGetRecords.mockResolvedValue({
      data: { code: 200, message: 'ok', data: [] },
    } as never)

    const wrapper = await mountProfilePage()
    await flushPromises()

    expect(wrapper.find('.profile-info__name').text()).toBe('Alice')
    expect(wrapper.find('.profile-info__email').text()).toBe('alice@example.com')
  })

  it('displays avatar letter from nickname', async () => {
    mockedGetProfile.mockResolvedValue({
      data: { code: 200, message: 'ok', data: mockProfile },
    } as never)
    mockedGetRecords.mockResolvedValue({ data: { code: 200, message: 'ok', data: [] } } as never)

    const wrapper = await mountProfilePage()
    await flushPromises()

    expect(wrapper.find('.profile-avatar').text()).toBe('A')
  })

  it('displays balance correctly', async () => {
    mockedGetProfile.mockResolvedValue({
      data: { code: 200, message: 'ok', data: mockProfile },
    } as never)
    mockedGetRecords.mockResolvedValue({ data: { code: 200, message: 'ok', data: [] } } as never)

    const wrapper = await mountProfilePage()
    await flushPromises()

    expect(wrapper.find('.balance-card__amount').text()).toBe('¥42.50')
  })

  it('renders recharge records', async () => {
    mockedGetProfile.mockResolvedValue({
      data: { code: 200, message: 'ok', data: mockProfile },
    } as never)
    mockedGetRecords.mockResolvedValue({
      data: { code: 200, message: 'ok', data: mockRecords },
    } as never)

    const wrapper = await mountProfilePage()
    await flushPromises()

    const items = wrapper.findAll('.records-list__item')
    expect(items).toHaveLength(2)
    expect(items[0].find('.records-list__amount').text()).toBe('+¥30.00')
    expect(items[1].find('.records-list__amount').text()).toBe('+¥10.00')
  })

  it('shows empty state when no recharge records', async () => {
    mockedGetProfile.mockResolvedValue({
      data: { code: 200, message: 'ok', data: mockProfile },
    } as never)
    mockedGetRecords.mockResolvedValue({
      data: { code: 200, message: 'ok', data: [] },
    } as never)

    const wrapper = await mountProfilePage()
    await flushPromises()

    expect(wrapper.find('.records-empty').exists()).toBe(true)
    expect(wrapper.find('.records-list').exists()).toBe(false)
  })

  it('shows error state on profile load failure', async () => {
    mockedGetProfile.mockRejectedValue(new Error('Network error'))

    const wrapper = await mountProfilePage()
    await flushPromises()

    expect(wrapper.find('.profile-error').exists()).toBe(true)
    expect(wrapper.find('.profile-error').text()).toContain('加载失败')
  })

  it('has a link to recharge page', async () => {
    mockedGetProfile.mockResolvedValue({
      data: { code: 200, message: 'ok', data: mockProfile },
    } as never)
    mockedGetRecords.mockResolvedValue({ data: { code: 200, message: 'ok', data: [] } } as never)

    const wrapper = await mountProfilePage()
    await flushPromises()

    const rechargeLink = wrapper.find('.balance-card__btn')
    expect(rechargeLink.attributes('href')).toBe('/recharge')
  })
})
