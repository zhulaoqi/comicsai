<template>
  <div class="novel-reader">
    <!-- Header -->
    <header class="novel-reader__header">
      <button class="novel-reader__back" @click="goBack" aria-label="返回">
        ← 返回
      </button>
      <h1 class="novel-reader__title">{{ title }}</h1>
      <span class="novel-reader__indicator" v-if="chapters.length > 0">
        第 {{ currentChapter + 1 }} 章 / 共 {{ chapters.length }} 章
      </span>
    </header>

    <!-- Loading state -->
    <div v-if="loading" class="novel-reader__loading">
      <div class="novel-reader__spinner" />
      <p>加载中...</p>
    </div>

    <!-- Error state -->
    <div v-else-if="error" class="novel-reader__error">
      <p>{{ error }}</p>
      <button class="novel-reader__retry-btn" @click="loadContent">重试</button>
    </div>

    <!-- Reader content -->
    <div v-else-if="chapters.length > 0" class="novel-reader__content">
      <!-- Chapter selector -->
      <div class="novel-reader__chapter-bar">
        <select
          class="novel-reader__chapter-select"
          :value="currentChapter"
          @change="onSelectChapter"
          aria-label="选择章节"
        >
          <option
            v-for="(ch, idx) in chapters"
            :key="ch.id"
            :value="idx"
          >
            第 {{ ch.chapterNumber }} 章：{{ ch.chapterTitle }}
          </option>
        </select>
      </div>

      <!-- Chapter text -->
      <article class="novel-reader__article">
        <h2 class="novel-reader__chapter-title">
          第 {{ chapters[currentChapter].chapterNumber }} 章：{{ chapters[currentChapter].chapterTitle }}
        </h2>
        <div class="novel-reader__text">
          <p
            v-for="(para, idx) in paragraphs"
            :key="idx"
            class="novel-reader__paragraph"
          >{{ para }}</p>
        </div>
      </article>

      <!-- Navigation -->
      <div class="novel-reader__nav">
        <button
          class="novel-reader__nav-btn novel-reader__nav-btn--prev"
          :disabled="currentChapter === 0"
          @click="prevChapter"
          aria-label="上一章"
        >
          ‹ 上一章
        </button>
        <span class="novel-reader__chapter-info">
          {{ currentChapter + 1 }} / {{ chapters.length }}
        </span>
        <button
          class="novel-reader__nav-btn novel-reader__nav-btn--next"
          :disabled="currentChapter === chapters.length - 1"
          @click="nextChapter"
          aria-label="下一章"
        >
          下一章 ›
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onBeforeUnmount, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getContentDetailApi } from '../api/content'
import { recordViewApi, recordDurationApi } from '../api/analytics'

export interface NovelChapter {
  id: number
  chapterNumber: number
  chapterTitle: string
  chapterText: string
}

const route = useRoute()
const router = useRouter()

const contentId = ref(Number(route.params.id))
const title = ref('')
const chapters = ref<NovelChapter[]>([])
const currentChapter = ref(0)
const loading = ref(true)
const error = ref('')
const enterTime = ref(0)

// Split chapter text into paragraphs for display
const paragraphs = computed(() => {
  const text = chapters.value[currentChapter.value]?.chapterText ?? ''
  return text.split('\n').filter((p) => p.trim().length > 0)
})

// Progress persistence
function progressKey(id: number) {
  return `novel-progress-${id}`
}

function saveProgress() {
  try {
    localStorage.setItem(progressKey(contentId.value), String(currentChapter.value))
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

function goToChapter(index: number) {
  if (index < 0 || index >= chapters.value.length || index === currentChapter.value) return
  currentChapter.value = index
  saveProgress()
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

function nextChapter() {
  goToChapter(currentChapter.value + 1)
}

function prevChapter() {
  goToChapter(currentChapter.value - 1)
}

function onSelectChapter(e: Event) {
  const idx = Number((e.target as HTMLSelectElement).value)
  goToChapter(idx)
}

function goBack() {
  router.back()
}

async function loadContent() {
  loading.value = true
  error.value = ''
  try {
    const res = await getContentDetailApi(contentId.value)
    const data = (res.data as { data: { title: string; novelChapters: NovelChapter[] } }).data
    title.value = data.title || ''
    chapters.value = (data.novelChapters || []).sort(
      (a: NovelChapter, b: NovelChapter) => a.chapterNumber - b.chapterNumber
    )

    const saved = loadProgress()
    currentChapter.value = saved < chapters.value.length ? saved : 0
  } catch {
    error.value = '加载失败，请重试'
  } finally {
    loading.value = false
  }
}

async function recordView() {
  try {
    await recordViewApi(contentId.value)
  } catch {
    // Silently fail analytics
  }
}

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

watch(currentChapter, () => {
  saveProgress()
})

onMounted(() => {
  loadContent()
  recordView()
  enterTime.value = Date.now()
})

onBeforeUnmount(() => {
  recordDuration()
})
</script>

<style scoped>
.novel-reader {
  min-height: 100vh;
  background: var(--color-bg);
  display: flex;
  flex-direction: column;
}

.novel-reader__header {
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

.novel-reader__back {
  color: var(--color-primary);
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-medium);
  padding: var(--spacing-xs) var(--spacing-sm);
  border-radius: var(--radius-sm);
  transition: background var(--transition-fast);
}

.novel-reader__back:hover {
  background: var(--color-secondary);
}

.novel-reader__title {
  flex: 1;
  font-size: var(--font-size-lg);
  font-weight: var(--font-weight-semibold);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.novel-reader__indicator {
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
  white-space: nowrap;
}

/* Loading */
.novel-reader__loading {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: var(--spacing-md);
  color: var(--color-text-secondary);
}

.novel-reader__spinner {
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
.novel-reader__error {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: var(--spacing-md);
  color: var(--color-text-secondary);
}

.novel-reader__retry-btn {
  padding: var(--spacing-sm) var(--spacing-lg);
  background: var(--color-primary);
  color: var(--color-text-inverse);
  border-radius: var(--radius-md);
  font-weight: var(--font-weight-medium);
  transition: background var(--transition-fast);
}

.novel-reader__retry-btn:hover {
  background: var(--color-primary-dark);
}

/* Content */
.novel-reader__content {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: var(--spacing-lg);
  gap: var(--spacing-lg);
}

/* Chapter selector bar */
.novel-reader__chapter-bar {
  width: 100%;
  max-width: 720px;
}

.novel-reader__chapter-select {
  width: 100%;
  padding: var(--spacing-sm) var(--spacing-md);
  background: var(--color-bg-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  font-size: var(--font-size-sm);
  color: var(--color-text-primary);
  cursor: pointer;
  outline: none;
  transition: border-color var(--transition-fast);
}

.novel-reader__chapter-select:focus {
  border-color: var(--color-primary);
}

/* Article */
.novel-reader__article {
  width: 100%;
  max-width: 720px;
  background: var(--color-bg-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: var(--spacing-xl) var(--spacing-2xl);
}

.novel-reader__chapter-title {
  font-size: var(--font-size-xl);
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-primary);
  margin-bottom: var(--spacing-lg);
  line-height: var(--line-height-tight);
}

.novel-reader__text {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-md);
}

.novel-reader__paragraph {
  font-size: var(--font-size-base);
  line-height: var(--line-height-relaxed);
  color: var(--color-text-primary);
  text-indent: 2em;
}

/* Navigation */
.novel-reader__nav {
  display: flex;
  align-items: center;
  gap: var(--spacing-lg);
  padding: var(--spacing-md) 0;
}

.novel-reader__nav-btn {
  padding: var(--spacing-sm) var(--spacing-lg);
  background: var(--color-primary);
  color: var(--color-text-inverse);
  border-radius: var(--radius-md);
  font-weight: var(--font-weight-medium);
  font-size: var(--font-size-sm);
  transition: background var(--transition-fast), opacity var(--transition-fast);
}

.novel-reader__nav-btn:hover:not(:disabled) {
  background: var(--color-primary-dark);
}

.novel-reader__nav-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.novel-reader__chapter-info {
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
  min-width: 60px;
  text-align: center;
}

/* Responsive */
@media (max-width: 640px) {
  .novel-reader__header {
    padding: var(--spacing-sm) var(--spacing-md);
  }

  .novel-reader__title {
    font-size: var(--font-size-base);
  }

  .novel-reader__content {
    padding: var(--spacing-md);
  }

  .novel-reader__article {
    padding: var(--spacing-lg) var(--spacing-md);
  }

  .novel-reader__nav-btn {
    padding: var(--spacing-xs) var(--spacing-md);
    font-size: var(--font-size-xs);
  }
}
</style>
