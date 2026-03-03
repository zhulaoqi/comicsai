import { describe, it, expect, beforeEach, vi } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createRouter, createMemoryHistory } from 'vue-router'
import SearchPage from './SearchPage.vue'

// Mock the content API
vi.mock('../api/content', () => ({
  searchContentsApi: vi.fn(),
}))

import { searchContentsApi } from '../api/content'
const mockedSearch = vi.mocked(searchContentsApi)

function createTestRouter(initialRoute = '/search') {
  return createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/search', name: 'Search', component: SearchPage },
      { path: '/', name: 'Home', component: { template: '<div>Home</div>' } },
      { path: '/comic/:id', name: 'ComicReader', component: { template: '<div />' } },
      { path: '/novel/:id', name: 'NovelReader', component: { template: '<div />' } },
    ],
  })
}

function makePage(records: unknown[] = [], hasNext = false, total = 0) {
  return {
    data: {
      code: 200,
      message: 'ok',
      data: { records, total, page: 1, size: 12, hasNext },
    },
  }
}

const sampleItem = {
  id: 1,
  title: '测试漫画',
  contentType: 'COMIC',
  coverUrl: '/img/1.jpg',
  description: '描述',
  isPaid: false,
  price: null,
  publishedAt: '2024-01-01T00:00:00',
}

describe('SearchPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('shows initial state with search prompt when no search performed', async () => {
    const router = createTestRouter()
    await router.push('/search')
    await router.isReady()

    const wrapper = mount(SearchPage, {
      global: { plugins: [router] },
    })

    expect(wrapper.find('.search-initial').exists()).toBe(true)
    expect(wrapper.find('.search-initial__text').text()).toContain('输入关键词')
    expect(mockedSearch).not.toHaveBeenCalled()
  })

  it('does not call API when submitting blank keyword', async () => {
    const router = createTestRouter()
    await router.push('/search')
    await router.isReady()

    const wrapper = mount(SearchPage, {
      global: { plugins: [router] },
    })

    const input = wrapper.find('.search-box__input')
    await input.setValue('   ')
    await wrapper.find('.search-box').trigger('submit')
    await flushPromises()

    expect(mockedSearch).not.toHaveBeenCalled()
  })

  it('does not call API when submitting empty keyword', async () => {
    const router = createTestRouter()
    await router.push('/search')
    await router.isReady()

    const wrapper = mount(SearchPage, {
      global: { plugins: [router] },
    })

    await wrapper.find('.search-box').trigger('submit')
    await flushPromises()

    expect(mockedSearch).not.toHaveBeenCalled()
  })

  it('calls API and displays results for valid keyword', async () => {
    mockedSearch.mockResolvedValue(makePage([sampleItem], false, 1) as never)

    const router = createTestRouter()
    await router.push('/search')
    await router.isReady()

    const wrapper = mount(SearchPage, {
      global: { plugins: [router] },
    })

    const input = wrapper.find('.search-box__input')
    await input.setValue('漫画')
    await wrapper.find('.search-box').trigger('submit')
    await flushPromises()

    expect(mockedSearch).toHaveBeenCalledWith('漫画', 1, 12)
    expect(wrapper.find('.search-results-count').text()).toContain('1')
    expect(wrapper.find('.content-grid').exists()).toBe(true)
  })

  it('shows empty state when search returns no results', async () => {
    mockedSearch.mockResolvedValue(makePage([], false, 0) as never)

    const router = createTestRouter()
    await router.push('/search')
    await router.isReady()

    const wrapper = mount(SearchPage, {
      global: { plugins: [router] },
    })

    const input = wrapper.find('.search-box__input')
    await input.setValue('不存在的内容')
    await wrapper.find('.search-box').trigger('submit')
    await flushPromises()

    expect(wrapper.find('.search-empty').exists()).toBe(true)
    expect(wrapper.find('.search-empty__title').text()).toContain('未找到')
    expect(wrapper.find('.search-empty__hint').text()).toContain('试试其他关键词')
  })

  it('restores keyword from URL query on mount', async () => {
    mockedSearch.mockResolvedValue(makePage([sampleItem], false, 1) as never)

    const router = createTestRouter()
    await router.push('/search?keyword=测试')
    await router.isReady()

    const wrapper = mount(SearchPage, {
      global: { plugins: [router] },
    })
    await flushPromises()

    expect(mockedSearch).toHaveBeenCalledWith('测试', 1, 12)
    const input = wrapper.find('.search-box__input').element as HTMLInputElement
    expect(input.value).toBe('测试')
  })

  it('clears results and resets state when clear button is clicked', async () => {
    mockedSearch.mockResolvedValue(makePage([sampleItem], false, 1) as never)

    const router = createTestRouter()
    await router.push('/search')
    await router.isReady()

    const wrapper = mount(SearchPage, {
      global: { plugins: [router] },
    })

    // Perform a search first
    await wrapper.find('.search-box__input').setValue('漫画')
    await wrapper.find('.search-box').trigger('submit')
    await flushPromises()

    expect(wrapper.find('.content-grid').exists()).toBe(true)

    // Click clear
    await wrapper.find('.search-box__clear').trigger('click')
    await flushPromises()

    expect(wrapper.find('.search-initial').exists()).toBe(true)
    expect(wrapper.find('.content-grid').exists()).toBe(false)
  })
})
