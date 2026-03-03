import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import { createRouter, createMemoryHistory } from 'vue-router'
import ContentCard from './ContentCard.vue'
import type { ContentItem } from '../api/content'

function createTestRouter() {
  return createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/', name: 'Home', component: { template: '<div />' } },
      { path: '/comic/:id', name: 'ComicReader', component: { template: '<div />' } },
      { path: '/novel/:id', name: 'NovelReader', component: { template: '<div />' } },
    ],
  })
}

const comicItem: ContentItem = {
  id: 1,
  title: 'My Comic',
  contentType: 'COMIC',
  coverUrl: 'https://example.com/cover.jpg',
  description: 'A comic',
  isPaid: false,
  price: null,
  publishedAt: '2024-03-15T10:00:00',
}

const paidNovelItem: ContentItem = {
  id: 2,
  title: 'My Novel',
  contentType: 'NOVEL',
  coverUrl: 'https://example.com/novel.jpg',
  description: 'A novel',
  isPaid: true,
  price: 5.99,
  publishedAt: '2024-06-20T08:30:00',
}

describe('ContentCard', () => {
  async function mountCard(content: ContentItem) {
    const router = createTestRouter()
    await router.push('/')
    await router.isReady()
    return mount(ContentCard, {
      props: { content },
      global: { plugins: [router] },
    })
  }

  it('renders title and formatted date', async () => {
    const wrapper = await mountCard(comicItem)
    expect(wrapper.find('.content-card__title').text()).toBe('My Comic')
    expect(wrapper.find('.content-card__date').text()).toBe('2024-03-15')
  })

  it('shows COMIC type badge', async () => {
    const wrapper = await mountCard(comicItem)
    const badge = wrapper.find('.content-card__type')
    expect(badge.text()).toBe('漫画')
    expect(badge.classes()).toContain('content-card__type--comic')
  })

  it('shows NOVEL type badge', async () => {
    const wrapper = await mountCard(paidNovelItem)
    const badge = wrapper.find('.content-card__type')
    expect(badge.text()).toBe('小说')
    expect(badge.classes()).toContain('content-card__type--novel')
  })

  it('shows paid badge for paid content', async () => {
    const wrapper = await mountCard(paidNovelItem)
    const paid = wrapper.find('.content-card__paid')
    expect(paid.exists()).toBe(true)
    expect(paid.text()).toBe('¥5.99')
  })

  it('hides paid badge for free content', async () => {
    const wrapper = await mountCard(comicItem)
    expect(wrapper.find('.content-card__paid').exists()).toBe(false)
  })

  it('links to ComicReader for comic content', async () => {
    const wrapper = await mountCard(comicItem)
    const link = wrapper.find('a')
    expect(link.attributes('href')).toBe('/comic/1')
  })

  it('links to NovelReader for novel content', async () => {
    const wrapper = await mountCard(paidNovelItem)
    const link = wrapper.find('a')
    expect(link.attributes('href')).toBe('/novel/2')
  })

  it('uses lazy loading on image', async () => {
    const wrapper = await mountCard(comicItem)
    const img = wrapper.find('img')
    expect(img.attributes('loading')).toBe('lazy')
  })
})
