<template>
  <div class="search-page">
    <header class="search-header">
      <button class="search-header__back" aria-label="返回" @click="goBack">
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <line x1="19" y1="12" x2="5" y2="12" /><polyline points="12 19 5 12 12 5" />
        </svg>
      </button>
      <form class="search-box" role="search" @submit.prevent="handleSearch">
        <svg class="search-box__icon" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
          <circle cx="11" cy="11" r="8" /><line x1="21" y1="21" x2="16.65" y2="16.65" />
        </svg>
        <input
          ref="searchInputRef"
          v-model="keyword"
          type="search"
          class="search-box__input"
          placeholder="搜索漫画、小说..."
          aria-label="搜索关键词"
          @input="onInputChange"
        />
        <button
          v-if="keyword"
          type="button"
          class="search-box__clear"
          aria-label="清除搜索"
          @click="clearSearch"
        >
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <line x1="18" y1="6" x2="6" y2="18" /><line x1="6" y1="6" x2="18" y2="18" />
          </svg>
        </button>
      </form>
    </header>

    <main class="search-content container">
      <!-- Initial state: no search yet -->
      <div v-if="!hasSearched && !loading" class="search-initial">
        <svg class="search-initial__icon" width="64" height="64" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
          <circle cx="11" cy="11" r="8" /><line x1="21" y1="21" x2="16.65" y2="16.65" />
        </svg>
        <p class="search-initial__text">输入关键词搜索你感兴趣的内容</p>
      </div>

      <!-- Loading skeleton on first search -->
      <SkeletonLoader v-else-if="loading && results.length === 0" />

      <!-- Search results -->
      <InfiniteScroll
        v-else-if="results.length > 0"
        :loading="loading"
        :finished="finished"
        @load-more="loadMore"
      >
        <div class="search-results-header">
          <span class="search-results-count">找到 {{ total }} 个结果</span>
        </div>
        <div class="content-grid">
          <ContentCard
            v-for="item in results"
            :key="item.id"
            :content="item"
          />
        </div>
      </InfiniteScroll>

      <!-- Empty results -->
      <div v-else-if="hasSearched && !loading && results.length === 0" class="search-empty">
        <svg class="search-empty__icon" width="64" height="64" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
          <circle cx="11" cy="11" r="8" /><line x1="21" y1="21" x2="16.65" y2="16.65" />
          <line x1="8" y1="11" x2="14" y2="11" />
        </svg>
        <p class="search-empty__title">未找到相关内容</p>
        <p class="search-empty__hint">试试其他关键词，或浏览以下推荐</p>
        <router-link :to="{ name: 'Home' }" class="search-empty__action">
          返回首页浏览
        </router-link>
      </div>
    </main>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { searchContentsApi, type ContentItem } from '../api/content'
import ContentCard from '../components/ContentCard.vue'
import InfiniteScroll from '../components/InfiniteScroll.vue'
import SkeletonLoader from '../components/SkeletonLoader.vue'

const router = useRouter()
const route = useRoute()

const searchInputRef = ref<HTMLInputElement | null>(null)
const keyword = ref('')
const results = ref<ContentItem[]>([])
const loading = ref(false)
const finished = ref(false)
const hasSearched = ref(false)
const total = ref(0)
const page = ref(1)
const pageSize = 12

function isBlankKeyword(value: string): boolean {
  return value.trim().length === 0
}

async function doSearch(reset = false) {
  // Frontend blank keyword interception (Requirement 2.4)
  if (isBlankKeyword(keyword.value)) return

  if (loading.value) return
  loading.value = true

  if (reset) {
    page.value = 1
    results.value = []
    finished.value = false
    total.value = 0
  }

  try {
    const res = await searchContentsApi(keyword.value.trim(), page.value, pageSize)
    const data = res.data.data
    results.value.push(...data.records)
    total.value = data.total
    finished.value = !data.hasNext
    hasSearched.value = true
    page.value++
  } catch {
    // Error handled by Axios interceptor
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  if (isBlankKeyword(keyword.value)) return
  // Update URL query param for shareable search links
  router.replace({ query: { keyword: keyword.value.trim() } })
  doSearch(true)
}

function onInputChange() {
  // Reset searched state when input is cleared
  if (!keyword.value) {
    hasSearched.value = false
    results.value = []
    total.value = 0
    finished.value = false
    router.replace({ query: {} })
  }
}

function clearSearch() {
  keyword.value = ''
  hasSearched.value = false
  results.value = []
  total.value = 0
  finished.value = false
  router.replace({ query: {} })
  searchInputRef.value?.focus()
}

function loadMore() {
  doSearch()
}

function goBack() {
  router.back()
}

onMounted(() => {
  // Restore keyword from URL query param
  const q = route.query.keyword
  if (q && typeof q === 'string' && !isBlankKeyword(q)) {
    keyword.value = q
    doSearch(true)
  } else {
    searchInputRef.value?.focus()
  }
})
</script>

<style scoped>
.search-page {
  min-height: 100vh;
  padding-bottom: var(--spacing-3xl);
}

.search-header {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
  padding: var(--spacing-lg);
  max-width: var(--max-width);
  margin: 0 auto;
}

.search-header__back {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  border-radius: var(--radius-full);
  color: var(--color-text-secondary);
  flex-shrink: 0;
  transition: background var(--transition-fast), color var(--transition-fast);
}

.search-header__back:hover {
  background: var(--color-bg-hover);
  color: var(--color-text-primary);
}

.search-box {
  display: flex;
  align-items: center;
  flex: 1;
  gap: var(--spacing-sm);
  padding: var(--spacing-sm) var(--spacing-md);
  background: var(--color-bg-hover);
  border: 2px solid transparent;
  border-radius: var(--radius-full);
  transition: border-color var(--transition-fast), background var(--transition-fast);
}

.search-box:focus-within {
  background: var(--color-bg-card);
  border-color: var(--color-primary);
}

.search-box__icon {
  flex-shrink: 0;
  color: var(--color-text-muted);
}

.search-box__input {
  flex: 1;
  border: none;
  background: transparent;
  font-size: var(--font-size-base);
  color: var(--color-text-primary);
  outline: none;
  line-height: var(--line-height-normal);
}

.search-box__input::placeholder {
  color: var(--color-text-muted);
}

/* Hide browser default search clear button */
.search-box__input::-webkit-search-cancel-button {
  display: none;
}

.search-box__clear {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border-radius: var(--radius-full);
  color: var(--color-text-muted);
  flex-shrink: 0;
  transition: background var(--transition-fast), color var(--transition-fast);
}

.search-box__clear:hover {
  background: var(--color-border);
  color: var(--color-text-secondary);
}

.search-content {
  margin-top: var(--spacing-md);
}

.search-results-header {
  margin-bottom: var(--spacing-lg);
}

.search-results-count {
  font-size: var(--font-size-sm);
  color: var(--color-text-muted);
}

.content-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: var(--spacing-lg);
}

/* Initial state */
.search-initial {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: var(--spacing-3xl) 0;
}

.search-initial__icon {
  color: var(--color-border);
  margin-bottom: var(--spacing-lg);
}

.search-initial__text {
  font-size: var(--font-size-base);
  color: var(--color-text-muted);
}

/* Empty state */
.search-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: var(--spacing-3xl) 0;
}

.search-empty__icon {
  color: var(--color-border);
  margin-bottom: var(--spacing-lg);
}

.search-empty__title {
  font-size: var(--font-size-lg);
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-primary);
  margin-bottom: var(--spacing-xs);
}

.search-empty__hint {
  font-size: var(--font-size-sm);
  color: var(--color-text-muted);
  margin-bottom: var(--spacing-lg);
}

.search-empty__action {
  display: inline-flex;
  align-items: center;
  padding: var(--spacing-sm) var(--spacing-xl);
  border-radius: var(--radius-full);
  background: var(--color-primary);
  color: var(--color-text-inverse);
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-medium);
  transition: background var(--transition-fast);
  text-decoration: none;
}

.search-empty__action:hover {
  background: var(--color-primary-dark);
  color: var(--color-text-inverse);
}

@media (max-width: 640px) {
  .content-grid {
    grid-template-columns: repeat(2, 1fr);
    gap: var(--spacing-md);
  }
}
</style>
