<template>
  <div class="novel-reader">
    <!-- Header -->
    <header class="novel-reader__header">
      <button class="novel-reader__back" @click="goBack" aria-label="返回">
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <polyline points="15 18 9 12 15 6" />
        </svg>
      </button>
      <h1 class="novel-reader__title">{{ title }}</h1>
      <button class="novel-reader__toc-btn" @click="tocOpen = true" aria-label="目录">
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <line x1="3" y1="6" x2="21" y2="6" />
          <line x1="3" y1="12" x2="21" y2="12" />
          <line x1="3" y1="18" x2="21" y2="18" />
        </svg>
      </button>
    </header>

    <!-- Loading -->
    <div v-if="loading" class="novel-reader__loading">
      <div class="novel-reader__spinner" />
      <p>加载中...</p>
    </div>

    <!-- Error -->
    <div v-else-if="error" class="novel-reader__error">
      <p>{{ error }}</p>
      <button class="novel-reader__retry-btn" @click="loadContent">重试</button>
    </div>

    <!-- Reading area (full width) -->
    <div v-else-if="activeChapter" class="novel-reader__content">
      <article class="novel-reader__article">
        <h2 class="novel-reader__chapter-title">
          第 {{ activeChapter.chapterNumber }} 章：{{ activeChapter.chapterTitle }}
        </h2>
        <div v-if="activeChapter.chapterText" class="novel-reader__text">
          <p
            v-for="(para, idx) in paragraphs"
            :key="idx"
            class="novel-reader__paragraph"
          >{{ para }}</p>
        </div>
        <div v-else class="novel-reader__locked-hint">
          <div class="novel-reader__locked-icon">
            <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
              <rect x="3" y="11" width="18" height="11" rx="2" ry="2" />
              <path d="M7 11V7a5 5 0 0 1 10 0v4" />
            </svg>
          </div>
          <p>本章节为付费内容</p>
          <button class="novel-reader__unlock-btn" @click="handleUnlockClick(activeChapter)">
            解锁阅读{{ activeChapter.chapterPrice ? ` ¥${activeChapter.chapterPrice.toFixed(2)}` : '' }}
          </button>
        </div>
      </article>

      <!-- Bottom navigation -->
      <div class="novel-reader__nav">
        <button
          class="novel-reader__nav-btn"
          :disabled="activeIndex === 0"
          @click="goToChapter(activeIndex - 1)"
        >
          上一章
        </button>
        <button class="novel-reader__nav-btn novel-reader__nav-btn--toc" @click="tocOpen = true">
          目录
        </button>
        <button
          class="novel-reader__nav-btn"
          :disabled="activeIndex === chapters.length - 1"
          @click="goToChapter(activeIndex + 1)"
        >
          下一章
        </button>
      </div>
    </div>

    <!-- TOC Drawer (overlay) -->
    <Teleport to="body">
      <Transition name="drawer">
        <div v-if="tocOpen" class="toc-overlay" @click.self="tocOpen = false">
          <div class="toc-drawer">
            <div class="toc-drawer__header">
              <h3 class="toc-drawer__title">目录</h3>
              <span class="toc-drawer__count">共 {{ chapters.length }} 章</span>
              <button class="toc-drawer__close" @click="tocOpen = false" aria-label="关闭目录">
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                  <line x1="18" y1="6" x2="6" y2="18" /><line x1="6" y1="6" x2="18" y2="18" />
                </svg>
              </button>
            </div>
            <ul class="toc-drawer__list">
              <li
                v-for="ch in chapters"
                :key="ch.id"
                class="toc-drawer__item"
                :class="{
                  'toc-drawer__item--active': activeChapter?.id === ch.id,
                  'toc-drawer__item--locked': !ch.accessible,
                }"
                @click="selectChapter(ch)"
              >
                <span class="toc-drawer__ch-title">
                  第 {{ ch.chapterNumber }} 章：{{ ch.chapterTitle }}
                </span>
                <span v-if="!ch.accessible && ch.chapterPrice" class="toc-drawer__price">
                  ¥{{ ch.chapterPrice.toFixed(2) }}
                </span>
                <span v-else-if="!ch.accessible" class="toc-drawer__lock">
                  <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <rect x="3" y="11" width="18" height="11" rx="2" ry="2" />
                    <path d="M7 11V7a5 5 0 0 1 10 0v4" />
                  </svg>
                </span>
              </li>
            </ul>
          </div>
        </div>
      </Transition>
    </Teleport>

    <!-- Paywall Modal -->
    <PaywallModal
      :visible="paywallVisible"
      :chapter-id="paywallChapterId"
      :price="paywallPrice"
      @unlock="onChapterUnlocked"
      @close="paywallVisible = false"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onBeforeUnmount, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getContentDetailApi, type NovelChapterVO } from '../api/content'
import { recordViewApi, recordDurationApi } from '../api/analytics'
import PaywallModal from '../components/PaywallModal.vue'

const route = useRoute()
const router = useRouter()

const contentId = ref(Number(route.params.id))
const title = ref('')
const chapters = ref<NovelChapterVO[]>([])
const activeChapter = ref<NovelChapterVO | null>(null)
const loading = ref(true)
const error = ref('')
const enterTime = ref(0)
const tocOpen = ref(false)

const paywallVisible = ref(false)
const paywallChapterId = ref(0)
const paywallPrice = ref(0)

const activeIndex = computed(() => {
  if (!activeChapter.value) return -1
  return chapters.value.findIndex(ch => ch.id === activeChapter.value!.id)
})

const paragraphs = computed(() => {
  const text = activeChapter.value?.chapterText ?? ''
  return text.split('\n').filter((p) => p.trim().length > 0)
})

function progressKey(id: number) {
  return `novel-progress-${id}`
}

function saveProgress() {
  if (activeChapter.value) {
    try {
      localStorage.setItem(progressKey(contentId.value), String(activeChapter.value.id))
    } catch { /* ignore */ }
  }
}

function loadSavedChapterId(): number | null {
  try {
    const saved = localStorage.getItem(progressKey(contentId.value))
    return saved !== null ? Number(saved) : null
  } catch {
    return null
  }
}

function selectChapter(ch: NovelChapterVO) {
  tocOpen.value = false
  if (!ch.accessible) {
    handleUnlockClick(ch)
    return
  }
  activeChapter.value = ch
  saveProgress()
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

function goToChapter(index: number) {
  if (index < 0 || index >= chapters.value.length) return
  const ch = chapters.value[index]
  if (!ch.accessible) {
    handleUnlockClick(ch)
    return
  }
  activeChapter.value = ch
  saveProgress()
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

function handleUnlockClick(ch: NovelChapterVO) {
  paywallChapterId.value = ch.id
  paywallPrice.value = ch.chapterPrice ?? 0
  paywallVisible.value = true
}

async function onChapterUnlocked() {
  paywallVisible.value = false
  await loadContent()
  const ch = chapters.value.find(c => c.id === paywallChapterId.value)
  if (ch && ch.accessible) {
    activeChapter.value = ch
  }
}

function goBack() {
  router.push({ name: 'ContentDetail', params: { id: contentId.value } })
}

function resolveInitialChapter(): NovelChapterVO | null {
  const qch = route.query.ch
  if (qch) {
    const target = chapters.value.find(c => c.id === Number(qch))
    if (target) return target
  }
  const savedId = loadSavedChapterId()
  if (savedId !== null) {
    const saved = chapters.value.find(c => c.id === savedId)
    if (saved && saved.accessible) return saved
  }
  return chapters.value.find(c => c.accessible) ?? chapters.value[0] ?? null
}

async function loadContent() {
  loading.value = true
  error.value = ''
  try {
    const res = await getContentDetailApi(contentId.value)
    const data = res.data as { data: {
      title: string
      isPaid: boolean
      novelChapterVOs: NovelChapterVO[] | null
      novelChapters: NovelChapterVO[] | null
    }}
    title.value = data.data.title || ''

    const rawChapters = (data.data.novelChapterVOs || data.data.novelChapters || []) as NovelChapterVO[]
    chapters.value = rawChapters.sort((a, b) => a.chapterNumber - b.chapterNumber)

    if (!activeChapter.value && chapters.value.length > 0) {
      activeChapter.value = resolveInitialChapter()
    } else if (activeChapter.value) {
      const refreshed = chapters.value.find(c => c.id === activeChapter.value!.id)
      if (refreshed) activeChapter.value = refreshed
    }
  } catch {
    error.value = '加载失败，请重试'
  } finally {
    loading.value = false
  }
}

async function recordView() {
  try { await recordViewApi(contentId.value) } catch { /* ignore */ }
}

function recordDuration() {
  if (enterTime.value > 0) {
    const seconds = Math.floor((Date.now() - enterTime.value) / 1000)
    if (seconds > 0) {
      recordDurationApi(contentId.value, seconds).catch(() => { /* ignore */ })
    }
  }
}

watch(activeChapter, () => { saveProgress() })

onMounted(() => {
  loadContent()
  recordView()
  enterTime.value = Date.now()
})

onBeforeUnmount(() => { recordDuration() })
</script>

<style scoped>
.novel-reader {
  min-height: 100vh;
  background: var(--color-bg);
  display: flex;
  flex-direction: column;
}

/* Header */
.novel-reader__header {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
  padding: var(--spacing-sm) var(--spacing-md);
  background: var(--color-bg-card);
  border-bottom: 1px solid var(--color-border);
  position: sticky;
  top: 0;
  z-index: 10;
}

.novel-reader__back,
.novel-reader__toc-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border-radius: var(--radius-full);
  color: var(--color-text-secondary);
  transition: background var(--transition-fast), color var(--transition-fast);
  flex-shrink: 0;
}

.novel-reader__back:hover,
.novel-reader__toc-btn:hover {
  background: var(--color-bg-hover);
  color: var(--color-primary);
}

.novel-reader__title {
  flex: 1;
  font-size: var(--font-size-base);
  font-weight: var(--font-weight-semibold);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  text-align: center;
}

/* Loading / Error */
.novel-reader__loading,
.novel-reader__error {
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

@keyframes spin { to { transform: rotate(360deg); } }

.novel-reader__retry-btn {
  padding: var(--spacing-sm) var(--spacing-lg);
  background: var(--color-primary);
  color: var(--color-text-inverse);
  border-radius: var(--radius-md);
}

/* Content */
.novel-reader__content {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: var(--spacing-lg);
  padding-bottom: 80px;
}

.novel-reader__article {
  width: 100%;
  max-width: 720px;
  background: var(--color-bg-card);
  border-radius: var(--radius-lg);
  padding: var(--spacing-xl) var(--spacing-2xl);
  min-height: 60vh;
}

.novel-reader__chapter-title {
  font-size: var(--font-size-xl);
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-primary);
  margin-bottom: var(--spacing-xl);
  line-height: var(--line-height-tight);
  text-align: center;
}

.novel-reader__text {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-md);
}

.novel-reader__paragraph {
  font-size: var(--font-size-base);
  line-height: 1.9;
  color: var(--color-text-primary);
  text-indent: 2em;
}

/* Locked hint */
.novel-reader__locked-hint {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--spacing-md);
  padding: var(--spacing-3xl) 0;
  color: var(--color-text-secondary);
}

.novel-reader__locked-icon {
  width: 80px;
  height: 80px;
  border-radius: var(--radius-full);
  background: var(--color-secondary);
  color: var(--color-primary);
  display: flex;
  align-items: center;
  justify-content: center;
}

.novel-reader__unlock-btn {
  padding: var(--spacing-sm) var(--spacing-xl);
  background: var(--color-primary);
  color: var(--color-text-inverse);
  border-radius: var(--radius-md);
  font-weight: var(--font-weight-semibold);
  min-height: 44px;
}

.novel-reader__unlock-btn:hover {
  background: var(--color-primary-dark);
}

/* Bottom navigation bar */
.novel-reader__nav {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  display: flex;
  align-items: center;
  justify-content: space-around;
  background: var(--color-bg-card);
  border-top: 1px solid var(--color-border);
  padding: var(--spacing-sm) var(--spacing-md);
  z-index: 10;
}

.novel-reader__nav-btn {
  flex: 1;
  padding: var(--spacing-sm) 0;
  text-align: center;
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-medium);
  color: var(--color-text-primary);
  border-radius: var(--radius-md);
  transition: background var(--transition-fast), color var(--transition-fast);
}

.novel-reader__nav-btn:hover:not(:disabled) {
  background: var(--color-bg-hover);
  color: var(--color-primary);
}

.novel-reader__nav-btn:disabled {
  color: var(--color-text-muted);
  opacity: 0.5;
  cursor: not-allowed;
}

.novel-reader__nav-btn--toc {
  color: var(--color-primary);
  font-weight: var(--font-weight-semibold);
}

/* TOC Drawer */
.toc-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.4);
  z-index: 100;
  display: flex;
  align-items: flex-end;
}

.toc-drawer {
  width: 100%;
  max-height: 70vh;
  background: var(--color-bg-card);
  border-radius: var(--radius-xl) var(--radius-xl) 0 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.toc-drawer__header {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
  padding: var(--spacing-md) var(--spacing-lg);
  border-bottom: 1px solid var(--color-border);
  flex-shrink: 0;
}

.toc-drawer__title {
  font-size: var(--font-size-lg);
  font-weight: var(--font-weight-bold);
  color: var(--color-text-primary);
}

.toc-drawer__count {
  font-size: var(--font-size-sm);
  color: var(--color-text-muted);
}

.toc-drawer__close {
  margin-left: auto;
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: var(--radius-full);
  color: var(--color-text-muted);
  transition: background var(--transition-fast);
}

.toc-drawer__close:hover {
  background: var(--color-bg-hover);
  color: var(--color-text-secondary);
}

.toc-drawer__list {
  list-style: none;
  padding: 0;
  margin: 0;
  overflow-y: auto;
  flex: 1;
}

.toc-drawer__item {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
  padding: var(--spacing-md) var(--spacing-lg);
  cursor: pointer;
  font-size: var(--font-size-sm);
  color: var(--color-text-primary);
  border-bottom: 1px solid var(--color-border-light);
  transition: background var(--transition-fast);
}

.toc-drawer__item:last-child {
  border-bottom: none;
}

.toc-drawer__item:hover {
  background: var(--color-bg-hover);
}

.toc-drawer__item--active {
  color: var(--color-primary);
  font-weight: var(--font-weight-semibold);
  background: var(--color-secondary);
}

.toc-drawer__item--locked {
  color: var(--color-text-muted);
}

.toc-drawer__ch-title {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.toc-drawer__price {
  font-size: var(--font-size-xs);
  color: var(--color-primary);
  font-weight: var(--font-weight-semibold);
  padding: 2px 6px;
  background: var(--color-secondary);
  border-radius: var(--radius-sm);
  white-space: nowrap;
}

.toc-drawer__lock {
  color: var(--color-text-muted);
  display: flex;
  align-items: center;
}

/* Drawer transition */
.drawer-enter-active,
.drawer-leave-active {
  transition: opacity 0.25s ease;
}

.drawer-enter-active .toc-drawer,
.drawer-leave-active .toc-drawer {
  transition: transform 0.3s cubic-bezier(0.33, 1, 0.68, 1);
}

.drawer-enter-from,
.drawer-leave-to {
  opacity: 0;
}

.drawer-enter-from .toc-drawer {
  transform: translateY(100%);
}

.drawer-leave-to .toc-drawer {
  transform: translateY(100%);
}

/* Desktop: drawer from right side instead of bottom */
@media (min-width: 769px) {
  .toc-overlay {
    align-items: stretch;
    justify-content: flex-end;
  }

  .toc-drawer {
    max-height: 100vh;
    width: 360px;
    border-radius: var(--radius-xl) 0 0 var(--radius-xl);
  }

  .drawer-enter-from .toc-drawer {
    transform: translateX(100%);
  }

  .drawer-leave-to .toc-drawer {
    transform: translateX(100%);
  }
}

/* Responsive */
@media (max-width: 640px) {
  .novel-reader__content {
    padding: var(--spacing-md);
    padding-bottom: 72px;
  }

  .novel-reader__article {
    padding: var(--spacing-lg) var(--spacing-md);
    border-radius: var(--radius-md);
  }

  .novel-reader__paragraph {
    font-size: var(--font-size-sm);
    line-height: 1.85;
  }
}
</style>
