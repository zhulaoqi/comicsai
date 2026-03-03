<template>
  <div class="comic-reader" @keydown="handleKeydown" tabindex="0" ref="readerRef">
    <!-- Header -->
    <header class="comic-reader__header">
      <button class="comic-reader__back" @click="goBack" aria-label="返回">
        ← 返回
      </button>
      <h1 class="comic-reader__title">{{ title }}</h1>
      <span class="comic-reader__indicator">{{ currentPage + 1 }} / {{ pages.length }}</span>
    </header>

    <!-- Loading state -->
    <div v-if="loading" class="comic-reader__loading">
      <div class="comic-reader__spinner" />
      <p>加载中...</p>
    </div>

    <!-- Error state -->
    <div v-else-if="error" class="comic-reader__error">
      <p>{{ error }}</p>
      <button class="comic-reader__retry-btn" @click="loadContent">重试</button>
    </div>

    <!-- Reader content -->
    <div v-else-if="pages.length > 0" class="comic-reader__content">
      <div class="comic-reader__stage">
        <div
          class="comic-reader__page"
          :class="{ 'comic-reader__page--flip': flipping }"
          :key="currentPage"
        >
          <img
            :src="pages[currentPage]?.imageUrl"
            :srcset="pages[currentPage]?.imageUrl ? `${pages[currentPage].imageUrl} 1x, ${pages[currentPage].imageUrl}?w=1440 2x` : undefined"
            sizes="(max-width: 720px) 100vw, 720px"
            :alt="`第 ${currentPage + 1} 页`"
            class="comic-reader__image"
          />
        </div>
      </div>

      <!-- Dialogue text -->
      <div v-if="pages[currentPage]?.dialogueText" class="comic-reader__dialogue">
        <p class="comic-reader__dialogue-text">{{ pages[currentPage].dialogueText }}</p>
      </div>

      <!-- Navigation -->
      <div class="comic-reader__nav">
        <button
          class="comic-reader__nav-btn comic-reader__nav-btn--prev"
          :disabled="currentPage === 0"
          @click="prevPage"
          aria-label="上一页"
        >
          ‹ 上一页
        </button>
        <span class="comic-reader__page-info">{{ currentPage + 1 }} / {{ pages.length }}</span>
        <button
          class="comic-reader__nav-btn comic-reader__nav-btn--next"
          :disabled="currentPage === pages.length - 1"
          @click="nextPage"
          aria-label="下一页"
        >
          下一页 ›
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getContentDetailApi } from '../api/content'
import { recordViewApi, recordDurationApi } from '../api/analytics'

export interface ComicPage {
  id: number
  pageNumber: number
  imageUrl: string
  dialogueText: string
}

const route = useRoute()
const router = useRouter()
const readerRef = ref<HTMLElement | null>(null)

const contentId = ref(Number(route.params.id))
const title = ref('')
const pages = ref<ComicPage[]>([])
const currentPage = ref(0)
const loading = ref(true)
const error = ref('')
const flipping = ref(false)
const enterTime = ref(0)

// Progress persistence key
function progressKey(id: number) {
  return `comic-progress-${id}`
}

function saveProgress() {
  try {
    localStorage.setItem(progressKey(contentId.value), String(currentPage.value))
  } catch {
    // localStorage may be unavailable
  }
}

function loadProgress(): number {
  try {
    const saved = localStorage.getItem(progressKey(contentId.value))
    return saved !== null ? Number(saved) : 0
  } catch {
    return 0
  }
}

// Image preloading: current page + adjacent pages
function preloadImages(pageIndex: number) {
  const indices = [pageIndex - 1, pageIndex, pageIndex + 1]
  for (const idx of indices) {
    if (idx >= 0 && idx < pages.value.length) {
      const img = new Image()
      img.src = pages.value[idx].imageUrl
    }
  }
}

// Page navigation with flip animation
function goToPage(index: number) {
  if (index < 0 || index >= pages.value.length || index === currentPage.value) return
  flipping.value = true
  setTimeout(() => {
    currentPage.value = index
    flipping.value = false
    saveProgress()
    preloadImages(index)
  }, 250)
}

function nextPage() {
  goToPage(currentPage.value + 1)
}

function prevPage() {
  goToPage(currentPage.value - 1)
}

function handleKeydown(e: KeyboardEvent) {
  if (e.key === 'ArrowRight' || e.key === 'ArrowDown') {
    e.preventDefault()
    nextPage()
  } else if (e.key === 'ArrowLeft' || e.key === 'ArrowUp') {
    e.preventDefault()
    prevPage()
  }
}

function goBack() {
  router.back()
}

async function loadContent() {
  loading.value = true
  error.value = ''
  try {
    const res = await getContentDetailApi(contentId.value)
    const data = (res.data as { data: { title: string; pages: ComicPage[] } }).data
    title.value = data.title || ''
    pages.value = (data.pages || []).sort((a: ComicPage, b: ComicPage) => a.pageNumber - b.pageNumber)

    // Restore reading progress
    const savedPage = loadProgress()
    currentPage.value = savedPage < pages.value.length ? savedPage : 0

    // Preload images around current page
    if (pages.value.length > 0) {
      preloadImages(currentPage.value)
    }
  } catch {
    error.value = '加载失败，请重试'
  } finally {
    loading.value = false
  }
}

// Record view event
async function recordView() {
  try {
    await recordViewApi(contentId.value)
  } catch {
    // Silently fail analytics
  }
}

// Record reading duration
function recordDuration() {
  if (enterTime.value > 0) {
    const seconds = Math.floor((Date.now() - enterTime.value) / 1000)
    if (seconds > 0) {
      recordDurationApi(contentId.value, seconds).catch(() => {
        // Silently fail analytics
      })
    }
  }
}

// Watch for page changes to save progress
watch(currentPage, () => {
  saveProgress()
})

onMounted(() => {
  loadContent()
  recordView()
  enterTime.value = Date.now()
  // Focus the reader for keyboard navigation
  readerRef.value?.focus()
})

onBeforeUnmount(() => {
  recordDuration()
})
</script>

<style scoped>
.comic-reader {
  min-height: 100vh;
  background: var(--color-bg);
  outline: none;
  display: flex;
  flex-direction: column;
}

.comic-reader__header {
  display: flex;
  align-items: center;
  gap: var(--spacing-md);
  padding: var(--spacing-md) var(--spacing-lg);
  background: var(--color-bg-card);
  border-bottom: 1px solid var(--color-border);
  position: sticky;
  top: 0;
  z-index: 10;
}

.comic-reader__back {
  color: var(--color-primary);
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-medium);
  padding: var(--spacing-xs) var(--spacing-sm);
  border-radius: var(--radius-sm);
  transition: background var(--transition-fast);
}

.comic-reader__back:hover {
  background: var(--color-secondary);
}

.comic-reader__title {
  flex: 1;
  font-size: var(--font-size-lg);
  font-weight: var(--font-weight-semibold);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.comic-reader__indicator {
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
  white-space: nowrap;
}

/* Loading */
.comic-reader__loading {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: var(--spacing-md);
  color: var(--color-text-secondary);
}

.comic-reader__spinner {
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

/* Error */
.comic-reader__error {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: var(--spacing-md);
  color: var(--color-text-secondary);
}

.comic-reader__retry-btn {
  padding: var(--spacing-sm) var(--spacing-lg);
  background: var(--color-primary);
  color: var(--color-text-inverse);
  border-radius: var(--radius-md);
  font-weight: var(--font-weight-medium);
  transition: background var(--transition-fast);
}

.comic-reader__retry-btn:hover {
  background: var(--color-primary-dark);
}

/* Content */
.comic-reader__content {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: var(--spacing-lg);
  gap: var(--spacing-lg);
}

.comic-reader__stage {
  width: 100%;
  max-width: 720px;
  overflow: hidden;
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-lg);
  background: var(--color-bg-card);
}

.comic-reader__page {
  transition: opacity 0.25s ease, transform 0.25s ease;
}

.comic-reader__page--flip {
  opacity: 0;
  transform: translateX(-8px);
}

.comic-reader__image {
  width: 100%;
  height: auto;
  display: block;
}

/* Dialogue */
.comic-reader__dialogue {
  width: 100%;
  max-width: 720px;
  background: var(--color-bg-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  padding: var(--spacing-md) var(--spacing-lg);
}

.comic-reader__dialogue-text {
  font-size: var(--font-size-base);
  line-height: var(--line-height-relaxed);
  color: var(--color-text-primary);
}

/* Navigation */
.comic-reader__nav {
  display: flex;
  align-items: center;
  gap: var(--spacing-lg);
  padding: var(--spacing-md) 0;
}

.comic-reader__nav-btn {
  padding: var(--spacing-sm) var(--spacing-lg);
  background: var(--color-primary);
  color: var(--color-text-inverse);
  border-radius: var(--radius-md);
  font-weight: var(--font-weight-medium);
  font-size: var(--font-size-sm);
  transition: background var(--transition-fast), opacity var(--transition-fast);
}

.comic-reader__nav-btn:hover:not(:disabled) {
  background: var(--color-primary-dark);
}

.comic-reader__nav-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.comic-reader__page-info {
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
  min-width: 60px;
  text-align: center;
}

/* Responsive */
@media (max-width: 640px) {
  .comic-reader__header {
    padding: var(--spacing-sm) var(--spacing-md);
  }

  .comic-reader__title {
    font-size: var(--font-size-base);
  }

  .comic-reader__content {
    padding: var(--spacing-md);
  }

  .comic-reader__nav-btn {
    padding: var(--spacing-xs) var(--spacing-md);
    font-size: var(--font-size-xs);
  }
}
</style>
