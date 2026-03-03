<template>
  <div class="profile-page">
    <header class="profile-header">
      <button class="profile-header__back" aria-label="返回" @click="router.back()">
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <line x1="19" y1="12" x2="5" y2="12" /><polyline points="12 19 5 12 12 5" />
        </svg>
      </button>
      <h1 class="profile-header__title">个人中心</h1>
      <button class="profile-header__logout" @click="handleLogout" :disabled="logoutLoading" aria-label="退出登录">
        {{ logoutLoading ? '...' : '退出' }}
      </button>
    </header>

    <main class="profile-content container">
      <!-- Loading -->
      <div v-if="loading" class="profile-loading">
        <div class="spinner" />
        <p>加载中...</p>
      </div>

      <!-- Error -->
      <div v-else-if="error" class="profile-error">
        <p>{{ error }}</p>
        <button class="btn-primary" @click="loadProfile">重试</button>
      </div>

      <template v-else-if="profile">
        <!-- User info card -->
        <section class="profile-card">
          <div class="profile-avatar" aria-hidden="true">
            {{ avatarLetter }}
          </div>
          <div class="profile-info">
            <h2 class="profile-info__name">{{ profile.nickname }}</h2>
            <p class="profile-info__email">{{ profile.email }}</p>
            <p class="profile-info__joined">注册于 {{ formatDate(profile.createdAt) }}</p>
          </div>
        </section>

        <!-- Balance card -->
        <section class="balance-card">
          <div class="balance-card__left">
            <span class="balance-card__label">账户余额</span>
            <span class="balance-card__amount">¥{{ profile.balance.toFixed(2) }}</span>
          </div>
          <router-link :to="{ name: 'Recharge' }" class="btn-primary balance-card__btn">
            充值
          </router-link>
        </section>

        <!-- Recharge records -->
        <section class="records-section">
          <h3 class="records-section__title">充值记录</h3>

          <div v-if="recordsLoading" class="records-loading">
            <div class="spinner spinner--sm" />
          </div>

          <ul v-else-if="rechargeRecords.length > 0" class="records-list">
            <li
              v-for="record in rechargeRecords"
              :key="record.id"
              class="records-list__item"
            >
              <span class="records-list__amount">+¥{{ record.amount.toFixed(2) }}</span>
              <span class="records-list__date">{{ formatDate(record.createdAt) }}</span>
            </li>
          </ul>

          <div v-else class="records-empty">
            <p>暂无充值记录</p>
            <router-link :to="{ name: 'Recharge' }" class="records-empty__link">
              去充值
            </router-link>
          </div>
        </section>
      </template>
    </main>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { getUserProfileApi, getRechargeRecordsApi, type UserProfile, type RechargeRecord } from '../api/user'

const router = useRouter()
const authStore = useAuthStore()

const loading = ref(true)
const error = ref('')
const logoutLoading = ref(false)
const profile = ref<UserProfile | null>(null)
const rechargeRecords = ref<RechargeRecord[]>([])
const recordsLoading = ref(false)

async function handleLogout() {
  logoutLoading.value = true
  await authStore.logout()
  router.push({ name: 'Home' })
}

const avatarLetter = computed(() => {
  return profile.value?.nickname?.charAt(0).toUpperCase() ?? '?'
})

function formatDate(dateStr: string): string {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  return d.toLocaleDateString('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit' })
}

async function loadProfile() {
  loading.value = true
  error.value = ''
  try {
    const res = await getUserProfileApi()
    profile.value = res.data.data
  } catch {
    error.value = '加载失败，请重试'
  } finally {
    loading.value = false
  }
}

async function loadRechargeRecords() {
  recordsLoading.value = true
  try {
    const res = await getRechargeRecordsApi()
    rechargeRecords.value = res.data.data ?? []
  } catch {
    rechargeRecords.value = []
  } finally {
    recordsLoading.value = false
  }
}

onMounted(async () => {
  await loadProfile()
  if (profile.value) {
    loadRechargeRecords()
  }
})
</script>

<style scoped>
.profile-page {
  min-height: 100vh;
  background: var(--color-bg);
  padding-bottom: var(--spacing-3xl);
}

.profile-header {
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

.profile-header__back {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  border-radius: var(--radius-full);
  color: var(--color-text-secondary);
  transition: background var(--transition-fast), color var(--transition-fast);
}

.profile-header__back:hover {
  background: var(--color-bg-hover);
  color: var(--color-text-primary);
}

.profile-header__logout {
  margin-left: auto;
  padding: var(--spacing-xs) var(--spacing-md);
  font-size: var(--font-size-sm);
  color: var(--color-error);
  border-radius: var(--radius-md);
  transition: background var(--transition-fast);
}

.profile-header__logout:hover:not(:disabled) {
  background: var(--color-bg-hover);
}

.profile-header__logout:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.profile-header__title {
  font-size: var(--font-size-lg);
  font-weight: var(--font-weight-semibold);
}

.profile-content {
  padding-top: var(--spacing-xl);
  display: flex;
  flex-direction: column;
  gap: var(--spacing-lg);
  max-width: 640px;
}

/* Loading / Error */
.profile-loading,
.profile-error {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--spacing-md);
  padding: var(--spacing-3xl) 0;
  color: var(--color-text-secondary);
}

/* User info card */
.profile-card {
  display: flex;
  align-items: center;
  gap: var(--spacing-lg);
  padding: var(--spacing-xl);
  background: var(--color-bg-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-xl);
  box-shadow: var(--shadow-sm);
}

.profile-avatar {
  width: 64px;
  height: 64px;
  border-radius: var(--radius-full);
  background: var(--color-primary);
  color: var(--color-text-inverse);
  font-size: var(--font-size-2xl);
  font-weight: var(--font-weight-bold);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.profile-info {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-xs);
}

.profile-info__name {
  font-size: var(--font-size-xl);
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-primary);
}

.profile-info__email {
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
}

.profile-info__joined {
  font-size: var(--font-size-xs);
  color: var(--color-text-muted);
}

/* Balance card */
.balance-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--spacing-xl);
  background: linear-gradient(135deg, var(--color-primary), var(--color-primary-light));
  border-radius: var(--radius-xl);
  box-shadow: var(--shadow-md);
}

.balance-card__left {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-xs);
}

.balance-card__label {
  font-size: var(--font-size-sm);
  color: rgba(255, 255, 255, 0.8);
}

.balance-card__amount {
  font-size: var(--font-size-3xl);
  font-weight: var(--font-weight-bold);
  color: var(--color-text-inverse);
}

.balance-card__btn {
  padding: var(--spacing-sm) var(--spacing-xl);
  background: rgba(255, 255, 255, 0.2);
  color: var(--color-text-inverse);
  border: 1px solid rgba(255, 255, 255, 0.4);
  border-radius: var(--radius-full);
  font-weight: var(--font-weight-semibold);
  font-size: var(--font-size-sm);
  text-decoration: none;
  transition: background var(--transition-fast);
}

.balance-card__btn:hover {
  background: rgba(255, 255, 255, 0.35);
  color: var(--color-text-inverse);
}

/* Records section */
.records-section {
  background: var(--color-bg-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-xl);
  overflow: hidden;
}

.records-section__title {
  font-size: var(--font-size-base);
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-primary);
  padding: var(--spacing-lg) var(--spacing-xl);
  border-bottom: 1px solid var(--color-border-light);
}

.records-loading {
  display: flex;
  justify-content: center;
  padding: var(--spacing-xl);
}

.records-list {
  list-style: none;
}

.records-list__item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--spacing-md) var(--spacing-xl);
  border-bottom: 1px solid var(--color-border-light);
  transition: background var(--transition-fast);
}

.records-list__item:last-child {
  border-bottom: none;
}

.records-list__item:hover {
  background: var(--color-bg-hover);
}

.records-list__amount {
  font-size: var(--font-size-base);
  font-weight: var(--font-weight-semibold);
  color: var(--color-success);
}

.records-list__date {
  font-size: var(--font-size-sm);
  color: var(--color-text-muted);
}

.records-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--spacing-sm);
  padding: var(--spacing-2xl);
  color: var(--color-text-muted);
  font-size: var(--font-size-sm);
}

.records-empty__link {
  color: var(--color-primary);
  font-weight: var(--font-weight-medium);
  font-size: var(--font-size-sm);
}

.records-empty__link:hover {
  color: var(--color-primary-dark);
}

/* Shared */
.btn-primary {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: var(--spacing-sm) var(--spacing-lg);
  background: var(--color-primary);
  color: var(--color-text-inverse);
  border-radius: var(--radius-md);
  font-weight: var(--font-weight-semibold);
  font-size: var(--font-size-sm);
  text-decoration: none;
  transition: background var(--transition-fast);
  cursor: pointer;
  border: none;
}

.btn-primary:hover {
  background: var(--color-primary-dark);
  color: var(--color-text-inverse);
}

/* Spinner */
.spinner {
  width: 36px;
  height: 36px;
  border: 3px solid var(--color-border);
  border-top-color: var(--color-primary);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

.spinner--sm {
  width: 24px;
  height: 24px;
  border-width: 2px;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

@media (max-width: 640px) {
  .profile-card {
    flex-direction: column;
    text-align: center;
  }

  .balance-card {
    flex-direction: column;
    gap: var(--spacing-lg);
    text-align: center;
  }
}
</style>
