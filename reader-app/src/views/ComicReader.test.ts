import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createRouter, createMemoryHistory } from 'vue-router'
import ComicReader from './ComicReader.vue'

const mockPages = [
  { id: 1, pageNumber: 1, imageUrl: 'https://example.com/page1.jpg', dialogueText: '第一页对话' },
  { id: 2, pageNumber: 2, imageUrl: 'https://example.com/page2.jpg', dialogueText: '' },
  { id: 3, pageNumber: 3, imageUrl: 'https://example.com/page3.jpg', dialogueText: '第三页对话' },
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
      { path: '/comic/:id', name: 'ComicReader', component: ComicReader },
    ],
  })
}

function mockApiResponse(title: string, pages: typeof mockPages) {
  return { data: { data: { title, pages } } }
}

describe('ComicReader', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.useFakeTimers()
    localStorage.clear()
    mockGetContentDetailApi.mockResolvedValue(mockApiResponse('测试漫画', mockPages))
    mockRecordViewApi.mockResolvedValue({ data: { code: 200 } })
    mockRecordDurationApi.mockResolvedValue({ data: { code: 200 } })
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  async function mountReader(id = 42) {
    const router = createTestRouter()
    await router.push(`/comic/${id}`)
    await router.isReady()
    const wrapper = mount(ComicReader, {
      global: { plugins: [router] },
    })
    await flushPromises()
    return { wrapper, router }
  }

  it('fetches content detail on mount', async () => {
    await mountReader(42)
    expect(mockGetContentDetailApi).toHaveBeenCalledWith(42)
  })

  it('renders title and page indicator', async () => {
    const { wrapper } = await mountReader()
    expect(wrapper.find('.comic-reader__title').text()).toBe('测试漫画')
    expect(wrapper.find('.comic-reader__indicator').text()).toBe('1 / 3')
  })

  it('renders the first page image', async () => {
    const { wrapper } = await mountReader()
    const img = wrapper.find('.comic-reader__image')
    expect(img.attributes('src')).toBe('https://example.com/page1.jpg')
  })

  it('displays dialogue text when present', async () => {
    const { wrapper } = await mountReader()
    expect(wrapper.find('.comic-reader__dialogue').exists()).toBe(true)
    expect(wrapper.find('.comic-reader__dialogue-text').text()).toBe('第一页对话')
  })

  it('navigates to next page on button click', async () => {
    const { wrapper } = await mountReader()
    const nextBtn = wrapper.find('.comic-reader__nav-btn--next')
    await nextBtn.trigger('click')
    vi.advanceTimersByTime(300)
    await flushPromises()

    expect(wrapper.find('.comic-reader__page-info').text()).toBe('2 / 3')
  })

  it('navigates to previous page on button click', async () => {
    const { wrapper } = await mountReader()
    // Go to page 2 first
    await wrapper.find('.comic-reader__nav-btn--next').trigger('click')
    vi.advanceTimersByTime(300)
    await flushPromises()

    // Go back to page 1
    await wrapper.find('.comic-reader__nav-btn--prev').trigger('click')
    vi.advanceTimersByTime(300)
    await flushPromises()

    expect(wrapper.find('.comic-reader__page-info').text()).toBe('1 / 3')
  })

  it('disables prev button on first page', async () => {
    const { wrapper } = await mountReader()
    const prevBtn = wrapper.find('.comic-reader__nav-btn--prev')
    expect((prevBtn.element as HTMLButtonElement).disabled).toBe(true)
  })

  it('disables next button on last page', async () => {
    const { wrapper } = await mountReader()
    // Navigate to last page
    await wrapper.find('.comic-reader__nav-btn--next').trigger('click')
    vi.advanceTimersByTime(300)
    await flushPromises()
    await wrapper.find('.comic-reader__nav-btn--next').trigger('click')
    vi.advanceTimersByTime(300)
    await flushPromises()

    const nextBtn = wrapper.find('.comic-reader__nav-btn--next')
    expect((nextBtn.element as HTMLButtonElement).disabled).toBe(true)
  })

  it('navigates pages with keyboard arrow keys', async () => {
    const { wrapper } = await mountReader()
    const reader = wrapper.find('.comic-reader')

    await reader.trigger('keydown', { key: 'ArrowRight' })
    vi.advanceTimersByTime(300)
    await flushPromises()

    expect(wrapper.find('.comic-reader__page-info').text()).toBe('2 / 3')

    await reader.trigger('keydown', { key: 'ArrowLeft' })
    vi.advanceTimersByTime(300)
    await flushPromises()

    expect(wrapper.find('.comic-reader__page-info').text()).toBe('1 / 3')
  })

  it('hides dialogue section when dialogueText is empty', async () => {
    const { wrapper } = await mountReader()
    // Navigate to page 2 which has empty dialogue
    await wrapper.find('.comic-reader__nav-btn--next').trigger('click')
    vi.advanceTimersByTime(300)
    await flushPromises()

    expect(wrapper.find('.comic-reader__dialogue').exists()).toBe(false)
  })

  it('saves reading progress to localStorage', async () => {
    const { wrapper } = await mountReader(99)
    await wrapper.find('.comic-reader__nav-btn--next').trigger('click')
    vi.advanceTimersByTime(300)
    await flushPromises()

    expect(localStorage.getItem('comic-progress-99')).toBe('1')
  })

  it('restores reading progress from localStorage', async () => {
    localStorage.setItem('comic-progress-42', '2')
    const { wrapper } = await mountReader(42)

    expect(wrapper.find('.comic-reader__page-info').text()).toBe('3 / 3')
  })

  it('records view event on mount', async () => {
    await mountReader(42)
    expect(mockRecordViewApi).toHaveBeenCalledWith(42)
  })

  it('shows loading state initially', async () => {
    mockGetContentDetailApi.mockReturnValue(new Promise(() => {})) // never resolves
    const router = createTestRouter()
    await router.push('/comic/1')
    await router.isReady()
    const wrapper = mount(ComicReader, { global: { plugins: [router] } })

    expect(wrapper.find('.comic-reader__loading').exists()).toBe(true)
  })

  it('shows error state on API failure', async () => {
    mockGetContentDetailApi.mockRejectedValue(new Error('Network error'))
    const { wrapper } = await mountReader()

    expect(wrapper.find('.comic-reader__error').exists()).toBe(true)
    expect(wrapper.find('.comic-reader__error').text()).toContain('加载失败')
  })

  it('retries loading on retry button click', async () => {
    mockGetContentDetailApi.mockRejectedValueOnce(new Error('fail'))
    const { wrapper } = await mountReader()

    expect(wrapper.find('.comic-reader__error').exists()).toBe(true)

    mockGetContentDetailApi.mockResolvedValue(mockApiResponse('测试漫画', mockPages))
    await wrapper.find('.comic-reader__retry-btn').trigger('click')
    await flushPromises()

    expect(wrapper.find('.comic-reader__title').text()).toBe('测试漫画')
  })

  it('applies flip animation class during page transition', async () => {
    const { wrapper } = await mountReader()
    await wrapper.find('.comic-reader__nav-btn--next').trigger('click')

    // During the flip animation (before timeout)
    expect(wrapper.find('.comic-reader__page--flip').exists()).toBe(true)

    vi.advanceTimersByTime(300)
    await flushPromises()

    expect(wrapper.find('.comic-reader__page--flip').exists()).toBe(false)
  })
})
