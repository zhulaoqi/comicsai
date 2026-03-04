<template>
  <div class="detail-page">
    <!-- Loading -->
    <div v-if="loading" class="detail-page__loading">
      <div class="detail-page__spinner" />
      <p>加载中...</p>
    </div>

    <!-- Error -->
    <div v-else-if="error" class="detail-page__error">
      <p>{{ error }}</p>
      <button class="detail-page__retry-btn" @click="loadDetail">重试</button>
    </div>

    <!-- Content -->
    <template v-else-if="detail">
      <!-- Hero section: cover + info -->
      <section class="detail-hero">
        <button class="detail-hero__back" @click="router.back()" aria-label="返回">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <polyline points="15 18 9 12 15 6" />
          </svg>
        </button>
        <div class="detail-hero__cover">
          <img :src="detail.coverUrl" :alt="detail.title" class="detail-hero__img" @error="onImgError" />
        </div>
        <div class="detail-hero__info">
          <span class="detail-hero__type" :class="`detail-hero__type--${detail.contentType.toLowerCase()}`">
            {{ detail.contentType === 'COMIC' ? '漫画' : '小说' }}
          </span>
          <h1 class="detail-hero__title">{{ detail.title }}</h1>
          <p class="detail-hero__desc">{{ detail.description || '暂无简介' }}</p>
          <div class="detail-hero__meta">
            <span v-if="chapterCount > 0">共 {{ chapterCount }} 章</span>
            <span v-if="detail.isPaid && detail.freeChapterCount > 0" class="detail-hero__free-tag">
              前 {{ detail.freeChapterCount }} 章免费
            </span>
            <span v-if="detail.isPaid" class="detail-hero__paid-tag">付费</span>
            <span v-else class="detail-hero__free-label">免费</span>
          </div>
          <button class="detail-hero__read-btn" @click="startReading">
            {{ readBtnText }}
          </button>
        </div>
      </section>

      <!-- Chapter list -->
      <section v-if="chapters.length > 0" class="detail-chapters">
        <div class="detail-chapters__header">
          <h2 class="detail-chapters__title">目录</h2>
          <button class="detail-chapters__sort" @click="sortAsc = !sortAsc">
            {{ sortAsc ? '正序' : '倒序' }}
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <polyline v-if="sortAsc" points="6 9 12 15 18 9" />
              <polyline v-else points="18 15 12 9 6 15" />
            </svg>
          </button>
        </div>
        <ul class="detail-chapters__list">
          <li
            v-for="ch in sortedChapters"
            :key="ch.id"
            class="detail-chapters__item"
            :class="{ 'detail-chapters__item--locked': !ch.accessible }"
            @click="openChapter(ch)"
          >
            <span class="detail-chapters__ch-title">
              第 {{ ch.chapterNumber }} 章：{{ ch.chapterTitle }}
            </span>
            <span v-if="!ch.accessible && ch.chapterPrice" class="detail-chapters__price">
              ¥{{ ch.chapterPrice.toFixed(2) }}
            </span>
            <span v-else-if="!ch.accessible" class="detail-chapters__lock-icon">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <rect x="3" y="11" width="18" height="11" rx="2" ry="2" />
                <path d="M7 11V7a5 5 0 0 1 10 0v4" />
              </svg>
            </span>
            <span v-else-if="detail.isPaid" class="detail-chapters__free-badge">免费</span>
            <svg class="detail-chapters__arrow" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <polyline points="9 18 15 12 9 6" />
            </svg>
          </li>
        </ul>
      </section>

      <!-- Comic pages note -->
      <section v-else-if="detail.contentType === 'COMIC'" class="detail-chapters">
        <div class="detail-chapters__header">
          <h2 class="detail-chapters__title">内容</h2>
        </div>
        <div class="detail-chapters__comic-hint">
          <p>共 {{ comicPageCount }} 页漫画</p>
          <button class="detail-hero__read-btn" @click="startReading">开始阅读</button>
        </div>
      </section>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getContentDetailApi, type NovelChapterVO } from '../api/content'

interface DetailData {
  id: number
  title: string
  contentType: 'COMIC' | 'NOVEL'
  coverUrl: string
  description: string | null
  isPaid: boolean
  freeChapterCount: number
  defaultChapterPrice: number | null
  comicPages?: { id: number }[]
  novelChapterVOs?: NovelChapterVO[]
  novelChapters?: NovelChapterVO[]
}

const route = useRoute()
const router = useRouter()

const contentId = ref(Number(route.params.id))
const detail = ref<DetailData | null>(null)
const chapters = ref<NovelChapterVO[]>([])
const loading = ref(true)
const error = ref('')
const sortAsc = ref(true)

const chapterCount = computed(() => chapters.value.length)
const comicPageCount = computed(() => detail.value?.comicPages?.length ?? 0)

const sortedChapters = computed(() => {
  const arr = [...chapters.value]
  return sortAsc.value
    ? arr.sort((a, b) => a.chapterNumber - b.chapterNumber)
    : arr.sort((a, b) => b.chapterNumber - a.chapterNumber)
})

const readBtnText = computed(() => {
  if (!detail.value) return '开始阅读'
  if (detail.value.contentType === 'COMIC') return '开始阅读'
  if (chapters.value.length === 0) return '暂无章节'
  const savedId = loadSavedChapterId()
  if (savedId !== null) return '继续阅读'
  return '开始阅读'
})

function loadSavedChapterId(): number | null {
  try {
    const saved = localStorage.getItem(`novel-progress-${contentId.value}`)
    return saved !== null ? Number(saved) : null
  } catch {
    return null
  }
}

async function loadDetail() {
  loading.value = true
  error.value = ''
  try {
    const res = await getContentDetailApi(contentId.value)
    const data = (res.data as { data: DetailData }).data
    detail.value = data

    const rawChapters = (data.novelChapterVOs || data.novelChapters || []) as NovelChapterVO[]
    chapters.value = rawChapters.sort((a, b) => a.chapterNumber - b.chapterNumber)
  } catch {
    error.value = '加载失败，请重试'
  } finally {
    loading.value = false
  }
}

function startReading() {
  if (!detail.value) return
  if (detail.value.contentType === 'COMIC') {
    router.push({ name: 'ComicReader', params: { id: contentId.value } })
    return
  }
  if (chapters.value.length === 0) return
  const savedId = loadSavedChapterId()
  router.push({
    name: 'NovelReader',
    params: { id: contentId.value },
    query: savedId !== null ? { ch: String(savedId) } : undefined,
  })
}

function openChapter(ch: NovelChapterVO) {
  if (!detail.value) return
  router.push({
    name: 'NovelReader',
    params: { id: contentId.value },
    query: { ch: String(ch.id) },
  })
}

function onImgError(e: Event) {
  const img = e.target as HTMLImageElement
  img.src = 'data:image/svg+xml,' + encodeURIComponent(
    '<svg xmlns="http://www.w3.org/2000/svg" width="300" height="400" fill="%23e8ecf1"><rect width="300" height="400"/><text x="150" y="200" text-anchor="middle" fill="%239ca3af" font-size="14">No Image</text></svg>'
  )
}

onMounted(loadDetail)
</script>

<style scoped>
.detail-page {
  min-height: 100vh;
  background: var(--color-bg);
}

.detail-page__loading,
.detail-page__error {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 60vh;
  gap: var(--spacing-md);
  color: var(--color-text-secondary);
}

.detail-page__spinner {
  width: 36px;
  height: 36px;
  border: 3px solid var(--color-border);
  border-top-color: var(--color-primary);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.detail-page__retry-btn {
  padding: var(--spacing-sm) var(--spacing-lg);
  background: var(--color-primary);
  color: var(--color-text-inverse);
  border-radius: var(--radius-md);
}

/* Hero */
.detail-hero {
  position: relative;
  display: flex;
  gap: var(--spacing-xl);
  padding: var(--spacing-xl) var(--spacing-lg);
  max-width: 800px;
  margin: 0 auto;
}

.detail-hero__back {
  position: absolute;
  top: var(--spacing-md);
  left: var(--spacing-md);
  width: 36px;
  height: 36px;
  border-radius: var(--radius-full);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--color-text-secondary);
  background: var(--color-bg-card);
  box-shadow: var(--shadow-sm);
  transition: background var(--transition-fast);
  z-index: 2;
}

.detail-hero__back:hover {
  background: var(--color-bg-hover);
  color: var(--color-primary);
}

.detail-hero__cover {
  flex-shrink: 0;
  width: 180px;
  aspect-ratio: 3 / 4;
  border-radius: var(--radius-lg);
  overflow: hidden;
  background: var(--color-border-light);
  box-shadow: var(--shadow-md);
}

.detail-hero__img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.detail-hero__info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: var(--spacing-sm);
  padding-top: var(--spacing-sm);
}

.detail-hero__type {
  align-self: flex-start;
  padding: 2px 10px;
  border-radius: var(--radius-full);
  font-size: var(--font-size-xs);
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-inverse);
}

.detail-hero__type--comic {
  background: var(--color-primary);
}

.detail-hero__type--novel {
  background: var(--color-success, #22c55e);
}

.detail-hero__title {
  font-size: var(--font-size-2xl);
  font-weight: var(--font-weight-bold);
  color: var(--color-text-primary);
  line-height: var(--line-height-tight);
}

.detail-hero__desc {
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
  line-height: var(--line-height-relaxed);
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.detail-hero__meta {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
  font-size: var(--font-size-xs);
  color: var(--color-text-muted);
  flex-wrap: wrap;
}

.detail-hero__free-tag {
  padding: 1px 8px;
  background: var(--color-secondary);
  color: var(--color-primary);
  border-radius: var(--radius-sm);
  font-weight: var(--font-weight-medium);
}

.detail-hero__paid-tag {
  padding: 1px 8px;
  background: #fef3c7;
  color: #d97706;
  border-radius: var(--radius-sm);
  font-weight: var(--font-weight-medium);
}

.detail-hero__free-label {
  padding: 1px 8px;
  background: #dcfce7;
  color: #16a34a;
  border-radius: var(--radius-sm);
  font-weight: var(--font-weight-medium);
}

.detail-hero__read-btn {
  margin-top: var(--spacing-sm);
  align-self: flex-start;
  padding: var(--spacing-sm) var(--spacing-2xl);
  background: var(--color-primary);
  color: var(--color-text-inverse);
  border-radius: var(--radius-full);
  font-size: var(--font-size-base);
  font-weight: var(--font-weight-semibold);
  transition: background var(--transition-fast);
  min-height: 44px;
}

.detail-hero__read-btn:hover {
  background: var(--color-primary-dark);
}

/* Chapters */
.detail-chapters {
  max-width: 800px;
  margin: 0 auto;
  padding: 0 var(--spacing-lg) var(--spacing-2xl);
}

.detail-chapters__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: var(--spacing-md);
}

.detail-chapters__title {
  font-size: var(--font-size-lg);
  font-weight: var(--font-weight-bold);
  color: var(--color-text-primary);
}

.detail-chapters__sort {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
  padding: var(--spacing-xs) var(--spacing-sm);
  border-radius: var(--radius-sm);
  transition: background var(--transition-fast);
}

.detail-chapters__sort:hover {
  background: var(--color-bg-hover);
  color: var(--color-primary);
}

.detail-chapters__list {
  list-style: none;
  padding: 0;
  margin: 0;
  background: var(--color-bg-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  overflow: hidden;
}

.detail-chapters__item {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
  padding: var(--spacing-md) var(--spacing-lg);
  cursor: pointer;
  border-bottom: 1px solid var(--color-border-light);
  transition: background var(--transition-fast);
}

.detail-chapters__item:last-child {
  border-bottom: none;
}

.detail-chapters__item:hover {
  background: var(--color-bg-hover);
}

.detail-chapters__item--locked {
  color: var(--color-text-muted);
}

.detail-chapters__ch-title {
  flex: 1;
  font-size: var(--font-size-sm);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.detail-chapters__price {
  font-size: var(--font-size-xs);
  color: var(--color-primary);
  font-weight: var(--font-weight-semibold);
  padding: 2px 8px;
  background: var(--color-secondary);
  border-radius: var(--radius-sm);
  white-space: nowrap;
}

.detail-chapters__lock-icon {
  color: var(--color-text-muted);
  display: flex;
  align-items: center;
}

.detail-chapters__free-badge {
  font-size: var(--font-size-xs);
  color: var(--color-success, #22c55e);
  white-space: nowrap;
}

.detail-chapters__arrow {
  color: var(--color-text-muted);
  flex-shrink: 0;
}

.detail-chapters__comic-hint {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--spacing-md);
  padding: var(--spacing-xl) 0;
  color: var(--color-text-secondary);
}

/* Responsive */
@media (max-width: 640px) {
  .detail-hero {
    flex-direction: column;
    align-items: center;
    text-align: center;
    padding: var(--spacing-lg);
    padding-top: 56px;
  }

  .detail-hero__cover {
    width: 140px;
  }

  .detail-hero__info {
    align-items: center;
  }

  .detail-hero__meta {
    justify-content: center;
  }

  .detail-hero__read-btn {
    align-self: center;
    width: 100%;
  }
}
</style>
