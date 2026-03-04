import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createRouter, createMemoryHistory } from 'vue-router'
import { createPinia, setActivePinia, defineStore } from 'pinia'
import NovelReader from './NovelReader.vue'

const mockChapters = [
  { id: 1, chapterNumber: 1, chapterTitle: '初遇', chapterText: '第一段\n第二段', accessible: true, chapterPrice: null, price: null, contentId: 1, chapterSummary: null },
  { id: 2, chapterNumber: 2, chapterTitle: '相知', chapterText: '第三段\n第四段', accessible: true, chapterPrice: null, price: null, contentId: 1, chapterSummary: null },
  { id: 3, chapterNumber: 3, chapterTitle: '别离', chapterText: '第五段', accessible: true, chapterPrice: null, price: null, contentId: 1, chapterSummary: null },
]

const mockGetContentDetailApi = vi.fn()
const mockRecordViewApi = vi.fn()
const mockRecordDurationApi = vi.fn()

vi.mock('../api/content', () => ({
  getContentDetailApi: (...args: unknown[]) => mockGetContentDetailApi(...args),
  unlockChapterApi: vi.fn(),
}))

vi.mock('../api/analytics', () => ({
  recordViewApi: (...args: unknown[]) => mockRecordViewApi(...args),
  recordDurationApi: (...args: unknown[]) => mockRecordDurationApi(...args),
}))

function setupAuthStore() {
  const pinia = createPinia()
  setActivePinia(pinia)
  defineStore('auth', {
    state: () => ({
      token: 'test-token',
      user: { id: 1, nickname: 'Alice', email: 'a@b.com', balance: 100 },
    }),
    getters: { isLoggedIn: (state) => !!state.token },
    actions: {
      setUser(userData: unknown) { this.user = userData as typeof this.user },
      clearAuth() { this.token = null as unknown as string; this.user = null as unknown as typeof this.user },
    },
  })()
  return pinia
}

function createTestRouter() {
  return createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/', name: 'Home', component: { template: '<div />' } },
      { path: '/novel/:id', name: 'NovelReader', component: NovelReader },
      { path: '/recharge', name: 'Recharge', component: { template: '<div />' } },
    ],
  })
}

function mockApiResponse(title: string, novelChapterVOs: typeof mockChapters) {
  return { data: { data: { title, isPaid: false, novelChapterVOs, novelChapters: null } } }
}

describe('NovelReader', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.useFakeTimers()
    localStorage.clear()
    mockGetContentDetailApi.mockResolvedValue(mockApiResponse('测试小说', mockChapters))
    mockRecordViewApi.mockResolvedValue({ data: { code: 200 } })
    mockRecordDurationApi.mockResolvedValue({ data: { code: 200 } })
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  async function mountReader(id = 42) {
    const pinia = setupAuthStore()
    const router = createTestRouter()
    await router.push(`/novel/${id}`)
    await router.isReady()
    const wrapper = mount(NovelReader, {
      global: { plugins: [router, pinia], stubs: { Teleport: true } },
    })
    await flushPromises()
    return { wrapper, router }
  }

  it('fetches content detail on mount', async () => {
    await mountReader(42)
    expect(mockGetContentDetailApi).toHaveBeenCalledWith(42)
  })

  it('renders title', async () => {
    const { wrapper } = await mountReader()
    expect(wrapper.find('.novel-reader__title').text()).toBe('测试小说')
  })

  it('renders chapter TOC list', async () => {
    const { wrapper } = await mountReader()
    const tocItems = wrapper.findAll('.novel-reader__toc-item')
    expect(tocItems).toHaveLength(3)
  })

  it('renders first accessible chapter content', async () => {
    const { wrapper } = await mountReader()
    expect(wrapper.find('.novel-reader__chapter-title').text()).toBe('第 1 章：初遇')
    const paragraphs = wrapper.findAll('.novel-reader__paragraph')
    expect(paragraphs).toHaveLength(2)
    expect(paragraphs[0].text()).toBe('第一段')
  })

  it('navigates to next chapter on button click', async () => {
    const { wrapper } = await mountReader()
    await wrapper.find('.novel-reader__nav-btn--next').trigger('click')
    await flushPromises()

    expect(wrapper.find('.novel-reader__chapter-info').text()).toBe('2 / 3')
    expect(wrapper.find('.novel-reader__chapter-title').text()).toBe('第 2 章：相知')
  })

  it('disables prev button on first chapter', async () => {
    const { wrapper } = await mountReader()
    const prevBtn = wrapper.find('.novel-reader__nav-btn--prev')
    expect((prevBtn.element as HTMLButtonElement).disabled).toBe(true)
  })

  it('records view event on mount', async () => {
    await mountReader(42)
    expect(mockRecordViewApi).toHaveBeenCalledWith(42)
  })

  it('records duration on unmount', async () => {
    const { wrapper } = await mountReader(42)
    vi.advanceTimersByTime(5000)
    wrapper.unmount()
    await flushPromises()

    expect(mockRecordDurationApi).toHaveBeenCalledWith(42, expect.any(Number))
  })

  it('shows loading state initially', async () => {
    mockGetContentDetailApi.mockReturnValue(new Promise(() => {}))
    const pinia = setupAuthStore()
    const router = createTestRouter()
    await router.push('/novel/1')
    await router.isReady()
    const wrapper = mount(NovelReader, { global: { plugins: [router, pinia], stubs: { Teleport: true } } })

    expect(wrapper.find('.novel-reader__loading').exists()).toBe(true)
  })

  it('shows error state on API failure', async () => {
    mockGetContentDetailApi.mockRejectedValue(new Error('Network error'))
    const { wrapper } = await mountReader()

    expect(wrapper.find('.novel-reader__error').exists()).toBe(true)
    expect(wrapper.find('.novel-reader__error').text()).toContain('加载失败')
  })

  it('sorts chapters by chapterNumber', async () => {
    const shuffled = [mockChapters[2], mockChapters[0], mockChapters[1]]
    mockGetContentDetailApi.mockResolvedValue(mockApiResponse('测试小说', shuffled))
    const { wrapper } = await mountReader()

    const tocItems = wrapper.findAll('.novel-reader__toc-title')
    expect(tocItems[0].text()).toContain('初遇')
    expect(tocItems[1].text()).toContain('相知')
    expect(tocItems[2].text()).toContain('别离')
  })
})
