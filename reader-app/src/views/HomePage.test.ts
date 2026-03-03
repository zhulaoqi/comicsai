import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createRouter, createMemoryHistory } from 'vue-router'
import { createPinia, setActivePinia } from 'pinia'
import HomePage from './HomePage.vue'

const mockContents = [
  {
    id: 1,
    title: 'Test Comic',
    contentType: 'COMIC',
    coverUrl: 'https://example.com/cover1.jpg',
    description: 'A test comic',
    isPaid: false,
    price: null,
    publishedAt: '2024-01-15T10:00:00',
  },
  {
    id: 2,
    title: 'Test Novel',
    contentType: 'NOVEL',
    coverUrl: 'https://example.com/cover2.jpg',
    description: 'A test novel',
    isPaid: true,
    price: 9.99,
    publishedAt: '2024-01-16T10:00:00',
  },
]

const mockGetContentsApi = vi.fn()

vi.mock('../api/content', () => ({
  getContentsApi: (...args: unknown[]) => mockGetContentsApi(...args),
}))

function createTestRouter() {
  return createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/', name: 'Home', component: { template: '<div />' } },
      { path: '/search', name: 'Search', component: { template: '<div />' } },
      { path: '/comic/:id', name: 'ComicReader', component: { template: '<div />' } },
      { path: '/novel/:id', name: 'NovelReader', component: { template: '<div />' } },
    ],
  })
}

function mockApiResponse(records: unknown[], hasNext = false) {
  return {
    data: {
      data: {
        records,
        total: records.length,
        page: 1,
        size: 12,
        hasNext,
      },
    },
  }
}

describe('HomePage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    setActivePinia(createPinia())
    mockGetContentsApi.mockResolvedValue(mockApiResponse(mockContents))
  })

  async function mountHomePage() {
    const router = createTestRouter()
    await router.push('/')
    await router.isReady()
    const wrapper = mount(HomePage, {
      global: { plugins: [router] },
    })
    await flushPromises()
    return wrapper
  }

  it('renders category tabs', async () => {
    const wrapper = await mountHomePage()
    const tabs = wrapper.findAll('[role="tab"]')
    expect(tabs).toHaveLength(3)
    expect(tabs[0].text()).toBe('全部')
    expect(tabs[1].text()).toBe('漫画')
    expect(tabs[2].text()).toBe('小说')
  })

  it('fetches contents on mount', async () => {
    await mountHomePage()
    expect(mockGetContentsApi).toHaveBeenCalledWith({
      page: 1,
      size: 12,
      type: undefined,
    })
  })

  it('renders content cards after loading', async () => {
    const wrapper = await mountHomePage()
    const cards = wrapper.findAll('.content-card')
    expect(cards).toHaveLength(2)
  })

  it('switches tab and reloads content', async () => {
    const wrapper = await mountHomePage()
    mockGetContentsApi.mockResolvedValue(mockApiResponse([mockContents[0]]))

    const comicTab = wrapper.findAll('[role="tab"]')[1]
    await comicTab.trigger('click')
    await flushPromises()

    expect(mockGetContentsApi).toHaveBeenLastCalledWith({
      page: 1,
      size: 12,
      type: 'COMIC',
    })
  })

  it('shows empty state when no contents', async () => {
    mockGetContentsApi.mockResolvedValue(mockApiResponse([]))
    const wrapper = await mountHomePage()
    expect(wrapper.find('.home-empty').exists()).toBe(true)
    expect(wrapper.find('.home-empty').text()).toBe('暂无内容')
  })

  it('shows "已加载全部内容" when finished', async () => {
    mockGetContentsApi.mockResolvedValue(mockApiResponse(mockContents, false))
    const wrapper = await mountHomePage()
    expect(wrapper.find('.infinite-scroll__done').exists()).toBe(true)
  })
})
