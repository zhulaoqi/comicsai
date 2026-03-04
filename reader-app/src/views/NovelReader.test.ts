import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createRouter, createMemoryHistory } from 'vue-router'
import NovelReader from './NovelReader.vue'

const mockChapters = [
  { id: 1, chapterNumber: 1, chapterTitle: '初遇', chapterText: '第一段\n第二段' },
  { id: 2, chapterNumber: 2, chapterTitle: '相知', chapterText: '第三段\n第四段' },
  { id: 3, chapterNumber: 3, chapterTitle: '别离', chapterText: '第五段' },
]

const mockGetContentDetailApi = vi.fn()
const mockRecordViewApi = vi.fn()
const mockRecordDurationApi = vi.fn()

vi.mock('../api/content', () => ({
  getContentDetailApi: (...args: unknown[]) => mockGetContentDetailApi(...args),
}))

vi.mock('../api/analytics', () => ({
  recordViewApi: (...args: unknown[]) => mockRecordViewApi(...args),
  recordDurationApi: (...args: unknown[]) => mockRecordDurationApi(...args),
}))

function createTestRouter() {
  return createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/', name: 'Home', component: { template: '<div />' } },
      { path: '/novel/:id', name: 'NovelReader', component: NovelReader },
    ],
  })
}

function mockApiResponse(title: string, novelChapters: typeof mockChapters) {
  return { data: { data: { title, novelChapters } } }
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
    const router = createTestRouter()
    await router.push(`/novel/${id}`)
    await router.isReady()
    const wrapper = mount(NovelReader, {
      global: { plugins: [router] },
    })
    await flushPromises()
    return { wrapper, router }
  }

  it('fetches content detail on mount', async () => {
    await mountReader(42)
    expect(mockGetContentDetailApi).toHaveBeenCalledWith(42)
  })

  it('renders title and chapter indicator', async () => {
    const { wrapper } = await mountReader()
    expect(wrapper.find('.novel-reader__title').text()).toBe('测试小说')
    expect(wrapper.find('.novel-reader__indicator').text()).toBe('第 1 章 / 共 3 章')
  })

  it('renders first chapter title and text', async () => {
    const { wrapper } = await mountReader()
    expect(wrapper.find('.novel-reader__chapter-title').text()).toBe('第 1 章：初遇')
    const paragraphs = wrapper.findAll('.novel-reader__paragraph')
    expect(paragraphs).toHaveLength(2)
    expect(paragraphs[0].text()).toBe('第一段')
    expect(paragraphs[1].text()).toBe('第二段')
  })

  it('navigates to next chapter on button click', async () => {
    const { wrapper } = await mountReader()
    await wrapper.find('.novel-reader__nav-btn--next').trigger('click')
    await flushPromises()

    expect(wrapper.find('.novel-reader__chapter-info').text()).toBe('2 / 3')
    expect(wrapper.find('.novel-reader__chapter-title').text()).toBe('第 2 章：相知')
  })

  it('navigates to previous chapter on button click', async () => {
    const { wrapper } = await mountReader()
    await wrapper.find('.novel-reader__nav-btn--next').trigger('click')
    await flushPromises()
    await wrapper.find('.novel-reader__nav-btn--prev').trigger('click')
    await flushPromises()

    expect(wrapper.find('.novel-reader__chapter-info').text()).toBe('1 / 3')
  })

  it('disables prev button on first chapter', async () => {
    const { wrapper } = await mountReader()
    const prevBtn = wrapper.find('.novel-reader__nav-btn--prev')
    expect((prevBtn.element as HTMLButtonElement).disabled).toBe(true)
  })

  it('disables next button on last chapter', async () => {
    const { wrapper } = await mountReader()
    await wrapper.find('.novel-reader__nav-btn--next').trigger('click')
    await flushPromises()
    await wrapper.find('.novel-reader__nav-btn--next').trigger('click')
    await flushPromises()

    const nextBtn = wrapper.find('.novel-reader__nav-btn--next')
    expect((nextBtn.element as HTMLButtonElement).disabled).toBe(true)
  })

  it('renders chapter select dropdown with all chapters', async () => {
    const { wrapper } = await mountReader()
    const options = wrapper.findAll('.novel-reader__chapter-select option')
    expect(options).toHaveLength(3)
    expect(options[0].text()).toBe('第 1 章：初遇')
    expect(options[1].text()).toBe('第 2 章：相知')
    expect(options[2].text()).toBe('第 3 章：别离')
  })

  it('navigates via chapter select dropdown', async () => {
    const { wrapper } = await mountReader()
    const select = wrapper.find('.novel-reader__chapter-select')
    await select.setValue('2')
    await select.trigger('change')
    await flushPromises()

    expect(wrapper.find('.novel-reader__chapter-info').text()).toBe('3 / 3')
  })

  it('saves reading progress to localStorage', async () => {
    const { wrapper } = await mountReader(99)
    await wrapper.find('.novel-reader__nav-btn--next').trigger('click')
    await flushPromises()

    expect(localStorage.getItem('novel-progress-99')).toBe('1')
  })

  it('restores reading progress from localStorage', async () => {
    localStorage.setItem('novel-progress-42', '2')
    const { wrapper } = await mountReader(42)

    expect(wrapper.find('.novel-reader__chapter-info').text()).toBe('3 / 3')
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
    const seconds = mockRecordDurationApi.mock.calls[0][1]
    expect(seconds).toBeGreaterThan(0)
  })

  it('shows loading state initially', async () => {
    mockGetContentDetailApi.mockReturnValue(new Promise(() => {}))
    const router = createTestRouter()
    await router.push('/novel/1')
    await router.isReady()
    const wrapper = mount(NovelReader, { global: { plugins: [router] } })

    expect(wrapper.find('.novel-reader__loading').exists()).toBe(true)
  })

  it('shows error state on API failure', async () => {
    mockGetContentDetailApi.mockRejectedValue(new Error('Network error'))
    const { wrapper } = await mountReader()

    expect(wrapper.find('.novel-reader__error').exists()).toBe(true)
    expect(wrapper.find('.novel-reader__error').text()).toContain('加载失败')
  })

  it('retries loading on retry button click', async () => {
    mockGetContentDetailApi.mockRejectedValueOnce(new Error('fail'))
    const { wrapper } = await mountReader()

    expect(wrapper.find('.novel-reader__error').exists()).toBe(true)

    mockGetContentDetailApi.mockResolvedValue(mockApiResponse('测试小说', mockChapters))
    await wrapper.find('.novel-reader__retry-btn').trigger('click')
    await flushPromises()

    expect(wrapper.find('.novel-reader__title').text()).toBe('测试小说')
  })

  it('sorts chapters by chapterNumber', async () => {
    const shuffled = [mockChapters[2], mockChapters[0], mockChapters[1]]
    mockGetContentDetailApi.mockResolvedValue(mockApiResponse('测试小说', shuffled))
    const { wrapper } = await mountReader()

    const options = wrapper.findAll('.novel-reader__chapter-select option')
    expect(options[0].text()).toBe('第 1 章：初遇')
    expect(options[1].text()).toBe('第 2 章：相知')
    expect(options[2].text()).toBe('第 3 章：别离')
  })
})
