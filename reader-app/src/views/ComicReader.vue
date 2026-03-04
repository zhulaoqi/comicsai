<template>
  <div
    class="comic-reader"
    :style="readerRootStyle"
    @keydown="handleKeydown"
    @click="handleContentClick"
    tabindex="0"
    ref="readerRef"
  >
    <!-- Progress bar -->
    <div class="comic-reader__progress-bar" :style="{ width: pageProgress + '%' }" />

    <!-- Brightness overlay -->
    <div v-if="settings.brightness < 100" class="comic-reader__brightness-mask" :style="{ opacity: (100 - settings.brightness) / 100 }" />

    <!-- Header -->
    <Transition name="bar-slide-top">
      <header v-show="barsVisible" class="comic-reader__header" :style="headerStyle">
        <button class="comic-reader__back" @click.stop="goBack" aria-label="返回">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <polyline points="15 18 9 12 15 6" />
          </svg>
          返回
        </button>
        <h1 class="comic-reader__title">{{ title }}</h1>
        <span class="comic-reader__indicator">{{ currentPage + 1 }} / {{ pages.length }}</span>
      </header>
    </Transition>

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
      <div v-if="pages[currentPage]?.dialogueText" class="comic-reader__dialogue" :style="{ background: currentThemeStyle.card, borderColor: currentThemeStyle.border, color: currentThemeStyle.text }">
        <p class="comic-reader__dialogue-text">{{ pages[currentPage].dialogueText }}</p>
      </div>
    </div>

    <!-- Bottom bar -->
    <Transition name="bar-slide-bottom">
      <div v-show="barsVisible" class="comic-reader__bottom" :style="headerStyle">
        <div class="comic-reader__nav">
          <button
            class="comic-reader__nav-btn"
            :disabled="currentPage === 0"
            @click.stop="prevPage"
            aria-label="上一页"
          >
            上一页
          </button>
          <span class="comic-reader__page-info">{{ currentPage + 1 }} / {{ pages.length }}</span>
          <button
            class="comic-reader__nav-btn comic-reader__nav-btn--settings"
            @click.stop="settingsOpen = true"
          >
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <circle cx="12" cy="12" r="3" />
              <path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1-2.83 2.83l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-4 0v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83-2.83l.06-.06A1.65 1.65 0 0 0 4.68 15a1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1 0-4h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 2.83-2.83l.06.06A1.65 1.65 0 0 0 9 4.68a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 4 0v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 2.83l-.06.06A1.65 1.65 0 0 0 19.4 9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 0 4h-.09a1.65 1.65 0 0 0-1.51 1z" />
            </svg>
          </button>
          <button
            class="comic-reader__nav-btn"
            :disabled="currentPage === pages.length - 1"
            @click.stop="nextPage"
            aria-label="下一页"
          >
            下一页
          </button>
        </div>
      </div>
    </Transition>

    <!-- Simplified settings panel for comic -->
    <Teleport to="body">
      <Transition name="settings-panel">
        <div v-if="settingsOpen" class="settings-overlay" @click.self="settingsOpen = false">
          <div class="comic-settings-panel" :style="{ background: currentThemeStyle.card, color: currentThemeStyle.text, borderColor: currentThemeStyle.border }">
            <!-- Brightness -->
            <div class="comic-settings-row">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" style="opacity:0.5;flex-shrink:0">
                <circle cx="12" cy="12" r="4" />
                <path d="M12 2v2M12 20v2M4.93 4.93l1.41 1.41M17.66 17.66l1.41 1.41M2 12h2M20 12h2M6.34 17.66l-1.41 1.41M19.07 4.93l-1.41 1.41" />
              </svg>
              <input
                type="range"
                class="comic-settings-slider"
                :min="40"
                :max="100"
                :value="settings.brightness"
                @input="setBrightness(Number(($event.target as HTMLInputElement).value))"
              />
              <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" style="opacity:0.8;flex-shrink:0">
                <circle cx="12" cy="12" r="4" />
                <path d="M12 2v2M12 20v2M4.93 4.93l1.41 1.41M17.66 17.66l1.41 1.41M2 12h2M20 12h2M6.34 17.66l-1.41 1.41M19.07 4.93l-1.41 1.41" />
              </svg>
            </div>
            <!-- Theme -->
            <div class="comic-settings-row comic-settings-row--theme">
              <button
                v-for="t in THEME_OPTIONS"
                :key="t.key"
                class="comic-settings-theme-btn"
                :class="{ 'comic-settings-theme-btn--active': settings.theme === t.key }"
                :style="{ background: t.color, color: t.key === 'dark' ? '#ccc' : '#333' }"
                @click="setTheme(t.key)"
              >
                {{ t.label }}
              </button>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onBeforeUnmount, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getContentDetailApi } from '../api/content'
import { recordViewApi, recordDurationApi } from '../api/analytics'
import { useReaderSettings, THEME_OPTIONS } from '../composables/useReaderSettings'

export interface ComicPage {
  id: number
  pageNumber: number
  imageUrl: string
  dialogueText: string
}

const route = useRoute()
const router = useRouter()
const readerRef = ref<HTMLElement | null>(null)

const { settings, currentThemeStyle, setTheme, setBrightness } = useReaderSettings()

const contentId = ref(Number(route.params.id))
const title = ref('')
const pages = ref<ComicPage[]>([])
const currentPage = ref(0)
const loading = ref(true)
const error = ref('')
const flipping = ref(false)
const enterTime = ref(0)
const barsVisible = ref(true)
const settingsOpen = ref(false)

const pageProgress = computed(() => {
  if (pages.value.length <= 1) return 100
  return ((currentPage.value + 1) / pages.value.length) * 100
})

const readerRootStyle = computed(() => ({
  background: currentThemeStyle.value.bg,
  color: currentThemeStyle.value.text,
}))

const headerStyle = computed(() => ({
  background: currentThemeStyle.value.headerBg,
  borderColor: currentThemeStyle.value.border,
  color: currentThemeStyle.value.text,
}))

function handleContentClick(e: MouseEvent) {
  if (settingsOpen.value) return
  const target = e.target as HTMLElement
  if (target.closest('button') || target.closest('a')) return

  const rect = (e.currentTarget as HTMLElement).getBoundingClientRect()
  const x = e.clientX - rect.left
  const relX = x / rect.width

  if (relX < 0.3) {
    prevPage()
  } else if (relX > 0.7) {
    nextPage()
  } else {
    barsVisible.value = !barsVisible.value
  }
}

function progressKey(id: number) {
  return `comic-progress-${id}`
}

function saveProgress() {
  try {
    localStorage.setItem(progressKey(contentId.value), String(currentPage.value))
  } catch { /* ignore */ }
}

function loadProgress(): number {
  try {
    const saved = localStorage.getItem(progressKey(contentId.value))
    return saved !== null ? Number(saved) : 0
  } catch {
    return 0
  }
}

function preloadImages(pageIndex: number) {
  const indices = [pageIndex - 1, pageIndex, pageIndex + 1]
  for (const idx of indices) {
    if (idx >= 0 && idx < pages.value.length) {
      const img = new Image()
      img.src = pages.value[idx].imageUrl
    }
  }
}

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
    const data = (res.data as { data: { title: string; comicPages: ComicPage[] } }).data
    title.value = data.title || ''
    pages.value = (data.comicPages || []).sort((a: ComicPage, b: ComicPage) => a.pageNumber - b.pageNumber)

    const savedPage = loadProgress()
    currentPage.value = savedPage < pages.value.length ? savedPage : 0

    if (pages.value.length > 0) {
      preloadImages(currentPage.value)
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

watch(currentPage, () => { saveProgress() })

onMounted(() => {
  loadContent()
  recordView()
  enterTime.value = Date.now()
  readerRef.value?.focus()
})

onBeforeUnmount(() => { recordDuration() })
</script>

<style scoped>
.comic-reader {
  min-height: 100vh;
  outline: none;
  display: flex;
  flex-direction: column;
  position: relative;
  transition: background 0.3s ease, color 0.3s ease;
}

/* Progress bar */
.comic-reader__progress-bar {
  position: fixed;
  top: 0;
  left: 0;
  height: 2px;
  background: #4a6cf7;
  z-index: 20;
  transition: width 0.3s ease;
}

/* Brightness overlay */
.comic-reader__brightness-mask {
  position: fixed;
  inset: 0;
  background: #000;
  pointer-events: none;
  z-index: 15;
  transition: opacity 0.2s;
}

/* Header */
.comic-reader__header {
  display: flex;
  align-items: center;
  gap: var(--spacing-md);
  padding: var(--spacing-sm) var(--spacing-lg);
  border-bottom: 1px solid;
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 12;
  transition: background 0.3s, color 0.3s, border-color 0.3s;
}

.comic-reader__back {
  display: flex;
  align-items: center;
  gap: 4px;
  color: #4a6cf7;
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-medium);
  padding: var(--spacing-xs) var(--spacing-sm);
  border-radius: var(--radius-sm);
  transition: background var(--transition-fast);
}

.comic-reader__back:hover {
  background: rgba(74, 108, 247, 0.1);
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
  opacity: 0.6;
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
  opacity: 0.6;
  padding-top: 60px;
}

.comic-reader__spinner {
  width: 36px;
  height: 36px;
  border: 3px solid currentColor;
  border-top-color: #4a6cf7;
  border-radius: 50%;
  opacity: 0.3;
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
  opacity: 0.6;
}

.comic-reader__retry-btn {
  padding: var(--spacing-sm) var(--spacing-lg);
  background: #4a6cf7;
  color: #fff;
  border-radius: var(--radius-md);
  font-weight: var(--font-weight-medium);
  transition: background var(--transition-fast);
}

.comic-reader__retry-btn:hover {
  background: #3451d1;
}

/* Content */
.comic-reader__content {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 60px var(--spacing-lg) 80px;
  gap: var(--spacing-lg);
}

.comic-reader__stage {
  width: 100%;
  max-width: 720px;
  overflow: hidden;
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-lg);
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
  border: 1px solid;
  border-radius: var(--radius-md);
  padding: var(--spacing-md) var(--spacing-lg);
  transition: background 0.3s, color 0.3s, border-color 0.3s;
}

.comic-reader__dialogue-text {
  font-size: var(--font-size-base);
  line-height: var(--line-height-relaxed);
}

/* Bottom bar */
.comic-reader__bottom {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  border-top: 1px solid;
  z-index: 12;
  transition: background 0.3s, color 0.3s, border-color 0.3s;
  padding-bottom: env(safe-area-inset-bottom);
}

.comic-reader__nav {
  display: flex;
  align-items: center;
  justify-content: space-around;
  gap: var(--spacing-md);
  padding: var(--spacing-sm) var(--spacing-md);
}

.comic-reader__nav-btn {
  padding: var(--spacing-sm) var(--spacing-lg);
  color: inherit;
  border-radius: var(--radius-md);
  font-weight: var(--font-weight-medium);
  font-size: var(--font-size-sm);
  transition: background var(--transition-fast), opacity var(--transition-fast);
}

.comic-reader__nav-btn:hover:not(:disabled) {
  background: rgba(128, 128, 128, 0.1);
}

.comic-reader__nav-btn:disabled {
  opacity: 0.3;
  cursor: not-allowed;
}

.comic-reader__nav-btn--settings {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  padding: var(--spacing-sm);
  opacity: 0.6;
}

.comic-reader__nav-btn--settings:hover {
  opacity: 1;
}

.comic-reader__page-info {
  font-size: var(--font-size-sm);
  opacity: 0.6;
  min-width: 60px;
  text-align: center;
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

/* Simplified settings */
.settings-overlay {
  position: fixed;
  inset: 0;
  z-index: 200;
  display: flex;
  align-items: flex-end;
  justify-content: center;
}

.comic-settings-panel {
  width: 100%;
  max-width: 480px;
  border-top: 1px solid;
  border-radius: 16px 16px 0 0;
  padding: 20px 16px calc(20px + env(safe-area-inset-bottom));
  display: flex;
  flex-direction: column;
  gap: 16px;
  box-shadow: 0 -4px 24px rgba(0, 0, 0, 0.12);
}

.comic-settings-row {
  display: flex;
  align-items: center;
  gap: 12px;
}

.comic-settings-row--theme {
  justify-content: center;
}

.comic-settings-slider {
  flex: 1;
  height: 4px;
  -webkit-appearance: none;
  appearance: none;
  background: currentColor;
  opacity: 0.2;
  border-radius: 2px;
  outline: none;
}

.comic-settings-slider::-webkit-slider-thumb {
  -webkit-appearance: none;
  width: 20px;
  height: 20px;
  border-radius: 50%;
  background: #4a6cf7;
  cursor: pointer;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.2);
}

.comic-settings-slider::-moz-range-thumb {
  width: 20px;
  height: 20px;
  border-radius: 50%;
  background: #4a6cf7;
  cursor: pointer;
  border: none;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.2);
}

.comic-settings-theme-btn {
  width: 56px;
  height: 36px;
  border-radius: 8px;
  font-size: 12px;
  font-weight: 500;
  border: 2px solid transparent;
  transition: border-color 0.2s, transform 0.15s;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

.comic-settings-theme-btn:hover {
  transform: scale(1.05);
}

.comic-settings-theme-btn--active {
  border-color: #4a6cf7;
  box-shadow: 0 0 0 2px rgba(74, 108, 247, 0.25);
}

/* Settings panel transition */
.settings-panel-enter-active,
.settings-panel-leave-active {
  transition: opacity 0.25s ease;
}

.settings-panel-enter-active .comic-settings-panel,
.settings-panel-leave-active .comic-settings-panel {
  transition: transform 0.3s cubic-bezier(0.33, 1, 0.68, 1);
}

.settings-panel-enter-from,
.settings-panel-leave-to {
  opacity: 0;
}

.settings-panel-enter-from .comic-settings-panel {
  transform: translateY(100%);
}

.settings-panel-leave-to .comic-settings-panel {
  transform: translateY(100%);
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
    padding: 52px var(--spacing-md) 72px;
  }

  .comic-reader__nav-btn {
    padding: var(--spacing-xs) var(--spacing-md);
    font-size: var(--font-size-xs);
  }
}
</style>
