<template>
  <div class="home-page">
    <header class="home-header">
      <h1 class="home-header__title">发现</h1>
      <div class="home-header__actions">
        <router-link :to="{ name: 'Search' }" class="home-header__icon-btn" aria-label="搜索">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <circle cx="11" cy="11" r="8" /><line x1="21" y1="21" x2="16.65" y2="16.65" />
          </svg>
        </router-link>
        <router-link v-if="isLoggedIn" :to="{ name: 'Profile' }" class="home-header__avatar" aria-label="个人中心">
          {{ avatarLetter }}
        </router-link>
        <router-link v-else :to="{ name: 'Login' }" class="home-header__login-btn">
          登录
        </router-link>
      </div>
    </header>

    <nav class="category-tabs" role="tablist" aria-label="内容分类">
      <button
        v-for="tab in tabs"
        :key="tab.value"
        role="tab"
        :aria-selected="activeTab === tab.value"
        class="category-tabs__item"
        :class="{ 'category-tabs__item--active': activeTab === tab.value }"
        @click="switchTab(tab.value)"
      >
        {{ tab.label }}
      </button>
    </nav>

    <main class="home-content container">
      <SkeletonLoader v-if="loading && contents.length === 0" />

      <InfiniteScroll
        v-else
        :loading="loading"
        :finished="finished"
        @load-more="loadMore"
      >
        <div v-if="contents.length === 0 && !loading" class="home-empty">
          暂无内容
        </div>
        <div v-else class="content-grid">
          <ContentCard
            v-for="item in contents"
            :key="item.id"
            :content="item"
          />
        </div>
      </InfiniteScroll>
    </main>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { getContentsApi, type ContentItem, type ContentType } from '../api/content'
import { useAuthStore } from '../stores/auth'
import ContentCard from '../components/ContentCard.vue'
import InfiniteScroll from '../components/InfiniteScroll.vue'
import SkeletonLoader from '../components/SkeletonLoader.vue'

const authStore = useAuthStore()
const isLoggedIn = computed(() => authStore.isLoggedIn)
const avatarLetter = computed(() => authStore.user?.nickname?.charAt(0).toUpperCase() ?? '我')

interface Tab {
  label: string
  value: ContentType | undefined
}

const tabs: Tab[] = [
  { label: '全部', value: undefined },
  { label: '漫画', value: 'COMIC' },
  { label: '小说', value: 'NOVEL' },
]

const activeTab = ref<ContentType | undefined>(undefined)
const contents = ref<ContentItem[]>([])
const loading = ref(false)
const finished = ref(false)
const page = ref(1)
const pageSize = 12

async function fetchContents(reset = false) {
  if (loading.value) return
  loading.value = true

  if (reset) {
    page.value = 1
    contents.value = []
    finished.value = false
  }

  try {
    const res = await getContentsApi({
      page: page.value,
      size: pageSize,
      type: activeTab.value,
    })
    const data = res.data.data
    contents.value.push(...data.records)
    finished.value = !data.hasNext
    page.value++
  } catch {
    // Error handled by Axios interceptor
  } finally {
    loading.value = false
  }
}

function switchTab(value: ContentType | undefined) {
  if (activeTab.value === value) return
  activeTab.value = value
  fetchContents(true)
}

function loadMore() {
  fetchContents()
}

onMounted(() => {
  fetchContents()
})
</script>

<style scoped>
.home-page {
  min-height: 100vh;
  padding-bottom: var(--spacing-3xl);
}

.home-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--spacing-lg) var(--spacing-lg) 0;
  max-width: var(--max-width);
  margin: 0 auto;
}

.home-header__title {
  font-size: var(--font-size-2xl);
  font-weight: var(--font-weight-bold);
  color: var(--color-text-primary);
}

.home-header__actions {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
}

.home-header__icon-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  border-radius: var(--radius-full);
  color: var(--color-text-secondary);
  transition: background var(--transition-fast), color var(--transition-fast);
}

.home-header__icon-btn:hover {
  background: var(--color-bg-hover);
  color: var(--color-primary);
}

.home-header__avatar {
  width: 36px;
  height: 36px;
  border-radius: var(--radius-full);
  background: var(--color-primary);
  color: var(--color-text-inverse);
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-bold);
  display: flex;
  align-items: center;
  justify-content: center;
  text-decoration: none;
  transition: opacity var(--transition-fast);
}

.home-header__avatar:hover {
  opacity: 0.85;
}

.home-header__login-btn {
  padding: var(--spacing-xs) var(--spacing-md);
  background: var(--color-primary);
  color: var(--color-text-inverse);
  border-radius: var(--radius-full);
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-medium);
  text-decoration: none;
  transition: background var(--transition-fast);
}

.home-header__login-btn:hover {
  background: var(--color-primary-dark);
  color: var(--color-text-inverse);
}

.category-tabs {
  display: flex;
  gap: var(--spacing-sm);
  padding: var(--spacing-lg) var(--spacing-lg) 0;
  max-width: var(--max-width);
  margin: 0 auto;
  overflow-x: auto;
}

.category-tabs__item {
  padding: var(--spacing-sm) var(--spacing-lg);
  border-radius: var(--radius-full);
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-medium);
  color: var(--color-text-secondary);
  background: var(--color-bg-hover);
  transition: all var(--transition-fast);
  white-space: nowrap;
}

.category-tabs__item:hover {
  color: var(--color-primary);
}

.category-tabs__item--active {
  background: var(--color-primary);
  color: var(--color-text-inverse);
}

.category-tabs__item--active:hover {
  color: var(--color-text-inverse);
}

.home-content {
  margin-top: var(--spacing-lg);
}

.content-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: var(--spacing-lg);
}

.home-empty {
  text-align: center;
  padding: var(--spacing-3xl) 0;
  color: var(--color-text-muted);
  font-size: var(--font-size-lg);
}

@media (max-width: 640px) {
  .content-grid {
    grid-template-columns: repeat(2, 1fr);
    gap: var(--spacing-md);
  }
}
</style>
