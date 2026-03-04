<template>
  <div
    class="novel-reader"
    :style="readerRootStyle"
    @click="handleContentClick"
  >
    <!-- Reading progress bar -->
    <div class="novel-reader__progress-bar" :style="{ width: readingProgress + '%' }" />

    <!-- Header (toggleable) -->
    <Transition name="bar-slide-top">
      <header v-show="barsVisible" class="novel-reader__header" :style="headerStyle">
        <button class="novel-reader__back" @click.stop="goBack" aria-label="返回">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <polyline points="15 18 9 12 15 6" />
          </svg>
        </button>
        <h1 class="novel-reader__title">{{ title }}</h1>
        <button class="novel-reader__toc-btn" @click.stop="tocOpen = true" aria-label="目录">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <line x1="3" y1="6" x2="21" y2="6" />
            <line x1="3" y1="12" x2="21" y2="12" />
            <line x1="3" y1="18" x2="21" y2="18" />
          </svg>
        </button>
      </header>
    </Transition>

    <!-- Brightness overlay -->
    <div v-if="settings.brightness < 100" class="novel-reader__brightness-mask" :style="{ opacity: (100 - settings.brightness) / 100 }" />

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

    <!-- Reading area -->
    <div v-else-if="activeChapter" ref="contentRef" class="novel-reader__content">
      <article class="novel-reader__article">
        <h2 class="novel-reader__chapter-title" :style="chapterTitleStyle">
          第 {{ activeChapter.chapterNumber }} 章：{{ activeChapter.chapterTitle }}
        </h2>
        <div v-if="activeChapter.chapterText" class="novel-reader__text" :style="textStyle">
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
          <button class="novel-reader__unlock-btn" @click.stop="handleUnlockClick(activeChapter)">
            解锁阅读{{ activeChapter.chapterPrice ? ` ¥${activeChapter.chapterPrice.toFixed(2)}` : '' }}
          </button>
        </div>
      </article>

      <!-- Chapter info footer -->
      <div class="novel-reader__chapter-info" :style="{ color: currentThemeStyle.textSecondary }">
        <span>{{ activeIndex + 1 }} / {{ chapters.length }} 章</span>
        <span v-if="activeChapter.chapterText">约 {{ wordCount }} 字</span>
      </div>
    </div>

    <!-- Bottom bar (toggleable) -->
    <Transition name="bar-slide-bottom">
      <div v-show="barsVisible" class="novel-reader__bottom" :style="headerStyle">
        <div class="novel-reader__nav">
          <button
            class="novel-reader__nav-btn"
            :disabled="activeIndex === 0"
            @click.stop="goToChapter(activeIndex - 1)"
          >
            上一章
          </button>
          <button class="novel-reader__nav-btn novel-reader__nav-btn--toc" @click.stop="tocOpen = true">
            目录
          </button>
          <button
            class="novel-reader__nav-btn novel-reader__nav-btn--settings"
            @click.stop="settingsOpen = true"
          >
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <circle cx="12" cy="12" r="3" />
              <path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1-2.83 2.83l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-4 0v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83-2.83l.06-.06A1.65 1.65 0 0 0 4.68 15a1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1 0-4h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 2.83-2.83l.06.06A1.65 1.65 0 0 0 9 4.68a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 4 0v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 2.83l-.06.06A1.65 1.65 0 0 0 19.4 9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 0 4h-.09a1.65 1.65 0 0 0-1.51 1z" />
            </svg>
            设置
          </button>
          <button
            class="novel-reader__nav-btn"
            :disabled="activeIndex === chapters.length - 1"
            @click.stop="goToChapter(activeIndex + 1)"
          >
            下一章
          </button>
        </div>
      </div>
    </Transition>

    <!-- TOC Drawer -->
    <Teleport to="body">
      <Transition name="drawer">
        <div v-if="tocOpen" class="toc-overlay" @click.self="tocOpen = false">
          <div class="toc-drawer" :style="{ background: currentThemeStyle.card, color: currentThemeStyle.text }">
            <div class="toc-drawer__header" :style="{ borderColor: currentThemeStyle.border }">
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
                :style="{ borderColor: currentThemeStyle.border }"
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

    <!-- Settings Panel -->
    <ReaderSettingsPanel
      :visible="settingsOpen"
      :auto-scroll="autoScrollEnabled"
      :scroll-speed="autoScrollSpeed"
      @close="settingsOpen = false"
      @toggle-auto-scroll="toggleAutoScroll"
      @scroll-speed="adjustScrollSpeed"
    />

    <!-- Paywall Modal -->
    <PaywallModal
      :visible="paywallVisible"
      :chapter-id="paywallChapterId"
      :price="paywallPrice"
      @unlock="onChapterUnlocked"
      @close="paywallVisible = false"
    />

    <!-- Auto-scroll indicator -->
    <Transition name="fade">
      <div v-if="autoScrollEnabled" class="novel-reader__auto-scroll-hint" @click.stop="toggleAutoScroll">
        <svg width="14" height="14" viewBox="0 0 24 24" fill="currentColor">
          <path d="M12 2l-5 9h3v11h4V11h3z" />
        </svg>
        自动滚动中 · 点击暂停
      </div>
    </Transition>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onBeforeUnmount, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getContentDetailApi, type NovelChapterVO } from '../api/content'
import { recordViewApi, recordDurationApi } from '../api/analytics'
import PaywallModal from '../components/PaywallModal.vue'
import ReaderSettingsPanel from '../components/ReaderSettingsPanel.vue'
import { useReaderSettings } from '../composables/useReaderSettings'

const route = useRoute()
const router = useRouter()

const { settings, currentThemeStyle, currentFontFamily } = useReaderSettings()

const contentId = ref(Number(route.params.id))
const title = ref('')
const chapters = ref<NovelChapterVO[]>([])
const activeChapter = ref<NovelChapterVO | null>(null)
const loading = ref(true)
const error = ref('')
const enterTime = ref(0)
const tocOpen = ref(false)
const settingsOpen = ref(false)
const barsVisible = ref(true)
const contentRef = ref<HTMLElement | null>(null)

const paywallVisible = ref(false)
const paywallChapterId = ref(0)
const paywallPrice = ref(0)

// Auto-scroll
const autoScrollEnabled = ref(false)
const autoScrollSpeed = ref(2)
let autoScrollTimer: ReturnType<typeof setInterval> | null = null

const activeIndex = computed(() => {
  if (!activeChapter.value) return -1
  return chapters.value.findIndex(ch => ch.id === activeChapter.value!.id)
})

const paragraphs = computed(() => {
  const text = activeChapter.value?.chapterText ?? ''
  return text.split('\n').filter((p) => p.trim().length > 0)
})

const wordCount = computed(() => {
  const text = activeChapter.value?.chapterText ?? ''
  return text.replace(/\s/g, '').length
})

const readingProgress = computed(() => {
  if (chapters.value.length === 0) return 0
  return ((activeIndex.value + 1) / chapters.value.length) * 100
})

// Dynamic styles based on reader settings
const readerRootStyle = computed(() => ({
  background: currentThemeStyle.value.bg,
  color: currentThemeStyle.value.text,
  '--reader-bg': currentThemeStyle.value.bg,
  '--reader-card': currentThemeStyle.value.card,
  '--reader-text': currentThemeStyle.value.text,
  '--reader-text-secondary': currentThemeStyle.value.textSecondary,
  '--reader-border': currentThemeStyle.value.border,
}))

const headerStyle = computed(() => ({
  background: currentThemeStyle.value.headerBg,
  borderColor: currentThemeStyle.value.border,
  color: currentThemeStyle.value.text,
}))

const textStyle = computed(() => ({
  fontSize: settings.fontSize + 'px',
  lineHeight: String(settings.lineHeight),
  fontFamily: currentFontFamily.value,
}))

const chapterTitleStyle = computed(() => ({
  color: currentThemeStyle.value.text,
}))

// Toggle bars on center click for immersive reading
function handleContentClick(e: MouseEvent) {
  if (settingsOpen.value || tocOpen.value || paywallVisible.value) return
  const target = e.target as HTMLElement
  if (target.closest('button') || target.closest('a') || target.closest('.toc-overlay')) return

  const rect = (e.currentTarget as HTMLElement).getBoundingClientRect()
  const y = e.clientY - rect.top
  const relY = y / rect.height

  if (relY > 0.25 && relY < 0.75) {
    barsVisible.value = !barsVisible.value
  }
}

// Auto-scroll
function toggleAutoScroll() {
  autoScrollEnabled.value = !autoScrollEnabled.value
  if (autoScrollEnabled.value) {
    startAutoScroll()
  } else {
    stopAutoScroll()
  }
}

function startAutoScroll() {
  stopAutoScroll()
  const speedMap = [0, 0.5, 1, 1.5, 2.5, 4]
  autoScrollTimer = setInterval(() => {
    window.scrollBy({ top: speedMap[autoScrollSpeed.value] || 1, behavior: 'auto' })
  }, 16)
}

function stopAutoScroll() {
  if (autoScrollTimer) {
    clearInterval(autoScrollTimer)
    autoScrollTimer = null
  }
}

function adjustScrollSpeed(delta: number) {
  autoScrollSpeed.value = Math.max(1, Math.min(5, autoScrollSpeed.value + delta))
  if (autoScrollEnabled.value) startAutoScroll()
}

// Chapter navigation
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

onBeforeUnmount(() => {
  recordDuration()
  stopAutoScroll()
})
</script>

<style scoped>
.novel-reader {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  position: relative;
  transition: background 0.3s ease, color 0.3s ease;
}

/* Progress bar */
.novel-reader__progress-bar {
  position: fixed;
  top: 0;
  left: 0;
  height: 2px;
  background: #4a6cf7;
  z-index: 20;
  transition: width 0.3s ease;
}

/* Brightness overlay */
.novel-reader__brightness-mask {
  position: fixed;
  inset: 0;
  background: #000;
  pointer-events: none;
  z-index: 15;
  transition: opacity 0.2s;
}

/* Header */
.novel-reader__header {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
  padding: var(--spacing-sm) var(--spacing-md);
  border-bottom: 1px solid;
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 12;
  transition: background 0.3s, color 0.3s, border-color 0.3s;
}

.novel-reader__back,
.novel-reader__toc-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border-radius: var(--radius-full);
  color: inherit;
  opacity: 0.7;
  transition: background var(--transition-fast), opacity var(--transition-fast);
  flex-shrink: 0;
}

.novel-reader__back:hover,
.novel-reader__toc-btn:hover {
  opacity: 1;
  background: rgba(128, 128, 128, 0.1);
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
  opacity: 0.6;
  padding-top: 60px;
}

.novel-reader__spinner {
  width: 36px;
  height: 36px;
  border: 3px solid currentColor;
  border-top-color: #4a6cf7;
  border-radius: 50%;
  opacity: 0.3;
  animation: spin 0.8s linear infinite;
}

@keyframes spin { to { transform: rotate(360deg); } }

.novel-reader__retry-btn {
  padding: var(--spacing-sm) var(--spacing-lg);
  background: #4a6cf7;
  color: #fff;
  border-radius: var(--radius-md);
}

/* Content */
.novel-reader__content {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 72px var(--spacing-lg) 100px;
}

.novel-reader__article {
  width: 100%;
  max-width: 720px;
  background: var(--reader-card, var(--color-bg-card));
  border-radius: var(--radius-lg);
  padding: var(--spacing-xl) var(--spacing-2xl);
  min-height: 60vh;
  transition: background 0.3s ease;
}

.novel-reader__chapter-title {
  font-size: var(--font-size-xl);
  font-weight: var(--font-weight-semibold);
  margin-bottom: var(--spacing-xl);
  line-height: var(--line-height-tight);
  text-align: center;
  transition: color 0.3s;
}

.novel-reader__text {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-md);
  transition: font-size 0.2s ease, line-height 0.2s ease;
}

.novel-reader__paragraph {
  text-indent: 2em;
  transition: font-size 0.2s ease, line-height 0.2s ease;
}

/* Chapter info */
.novel-reader__chapter-info {
  display: flex;
  justify-content: center;
  gap: var(--spacing-lg);
  padding: var(--spacing-xl) 0 var(--spacing-md);
  font-size: 13px;
  opacity: 0.5;
}

/* Locked hint */
.novel-reader__locked-hint {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--spacing-md);
  padding: var(--spacing-3xl) 0;
  opacity: 0.7;
}

.novel-reader__locked-icon {
  width: 80px;
  height: 80px;
  border-radius: var(--radius-full);
  background: rgba(74, 108, 247, 0.1);
  color: #4a6cf7;
  display: flex;
  align-items: center;
  justify-content: center;
}

.novel-reader__unlock-btn {
  padding: var(--spacing-sm) var(--spacing-xl);
  background: #4a6cf7;
  color: #fff;
  border-radius: var(--radius-md);
  font-weight: var(--font-weight-semibold);
  min-height: 44px;
  transition: background var(--transition-fast);
}

.novel-reader__unlock-btn:hover {
  background: #3451d1;
}

/* Bottom bar */
.novel-reader__bottom {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  border-top: 1px solid;
  z-index: 12;
  transition: background 0.3s, color 0.3s, border-color 0.3s;
  padding-bottom: env(safe-area-inset-bottom);
}

.novel-reader__nav {
  display: flex;
  align-items: center;
  justify-content: space-around;
  padding: var(--spacing-sm) var(--spacing-md);
}

.novel-reader__nav-btn {
  flex: 1;
  padding: var(--spacing-sm) 0;
  text-align: center;
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-medium);
  color: inherit;
  border-radius: var(--radius-md);
  transition: background var(--transition-fast), opacity var(--transition-fast);
}

.novel-reader__nav-btn:hover:not(:disabled) {
  background: rgba(128, 128, 128, 0.1);
}

.novel-reader__nav-btn:disabled {
  opacity: 0.3;
  cursor: not-allowed;
}

.novel-reader__nav-btn--toc {
  color: #4a6cf7;
  font-weight: var(--font-weight-semibold);
}

.novel-reader__nav-btn--settings {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  color: inherit;
  opacity: 0.7;
}

.novel-reader__nav-btn--settings:hover {
  opacity: 1;
}

/* Auto-scroll indicator */
.novel-reader__auto-scroll-hint {
  position: fixed;
  bottom: 70px;
  left: 50%;
  transform: translateX(-50%);
  background: rgba(0, 0, 0, 0.7);
  color: #fff;
  padding: 6px 16px;
  border-radius: 20px;
  font-size: 12px;
  display: flex;
  align-items: center;
  gap: 6px;
  z-index: 11;
  cursor: pointer;
  transition: opacity 0.2s;
  animation: auto-scroll-pulse 2s ease-in-out infinite;
}

@keyframes auto-scroll-pulse {
  0%, 100% { opacity: 0.7; }
  50% { opacity: 1; }
}

.novel-reader__auto-scroll-hint:hover {
  opacity: 1;
  animation: none;
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
  border-radius: var(--radius-xl) var(--radius-xl) 0 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  transition: background 0.3s, color 0.3s;
}

.toc-drawer__header {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
  padding: var(--spacing-md) var(--spacing-lg);
  border-bottom: 1px solid;
  flex-shrink: 0;
}

.toc-drawer__title {
  font-size: var(--font-size-lg);
  font-weight: var(--font-weight-bold);
}

.toc-drawer__count {
  font-size: var(--font-size-sm);
  opacity: 0.5;
}

.toc-drawer__close {
  margin-left: auto;
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: var(--radius-full);
  color: inherit;
  opacity: 0.5;
  transition: background var(--transition-fast), opacity var(--transition-fast);
}

.toc-drawer__close:hover {
  background: rgba(128, 128, 128, 0.1);
  opacity: 0.8;
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
  border-bottom: 1px solid;
  transition: background var(--transition-fast);
}

.toc-drawer__item:last-child {
  border-bottom: none;
}

.toc-drawer__item:hover {
  background: rgba(128, 128, 128, 0.06);
}

.toc-drawer__item--active {
  color: #4a6cf7;
  font-weight: var(--font-weight-semibold);
  background: rgba(74, 108, 247, 0.08);
}

.toc-drawer__item--locked {
  opacity: 0.5;
}

.toc-drawer__ch-title {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.toc-drawer__price {
  font-size: var(--font-size-xs);
  color: #4a6cf7;
  font-weight: var(--font-weight-semibold);
  padding: 2px 6px;
  background: rgba(74, 108, 247, 0.1);
  border-radius: var(--radius-sm);
  white-space: nowrap;
}

.toc-drawer__lock {
  opacity: 0.5;
  display: flex;
  align-items: center;
}

/* Bar transitions */
.bar-slide-top-enter-active,
.bar-slide-top-leave-active {
  transition: transform 0.3s cubic-bezier(0.33, 1, 0.68, 1), opacity 0.3s ease;
}
.bar-slide-top-enter-from,
.bar-slide-top-leave-to {
  transform: translateY(-100%);
  opacity: 0;
}

.bar-slide-bottom-enter-active,
.bar-slide-bottom-leave-active {
  transition: transform 0.3s cubic-bezier(0.33, 1, 0.68, 1), opacity 0.3s ease;
}
.bar-slide-bottom-enter-from,
.bar-slide-bottom-leave-to {
  transform: translateY(100%);
  opacity: 0;
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

/* Fade transition */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

/* Desktop TOC */
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
    padding: 60px var(--spacing-md) 80px;
  }

  .novel-reader__article {
    padding: var(--spacing-lg) var(--spacing-md);
    border-radius: var(--radius-md);
  }
}
</style>
