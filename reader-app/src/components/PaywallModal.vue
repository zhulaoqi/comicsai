<template>
  <Teleport to="body">
    <Transition name="paywall-fade">
      <div
        v-if="visible"
        class="paywall-overlay"
        role="dialog"
        aria-modal="true"
        aria-labelledby="paywall-title"
        @click.self="emit('close')"
      >
        <div class="paywall-modal">
          <!-- Close button -->
          <button class="paywall-modal__close" aria-label="关闭" @click="emit('close')">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <line x1="18" y1="6" x2="6" y2="18" /><line x1="6" y1="6" x2="18" y2="18" />
            </svg>
          </button>

          <!-- Lock icon -->
          <div class="paywall-modal__icon" aria-hidden="true">
            <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
              <rect x="3" y="11" width="18" height="11" rx="2" ry="2" />
              <path d="M7 11V7a5 5 0 0 1 10 0v4" />
            </svg>
          </div>

          <h2 id="paywall-title" class="paywall-modal__title">付费内容</h2>
          <p class="paywall-modal__desc">解锁此内容以继续阅读</p>

          <!-- Price info -->
          <div class="paywall-modal__info">
            <div class="paywall-modal__info-row">
              <span class="paywall-modal__info-label">内容价格</span>
              <span class="paywall-modal__info-value paywall-modal__info-value--price">
                ¥{{ price.toFixed(2) }}
              </span>
            </div>
            <div class="paywall-modal__info-row">
              <span class="paywall-modal__info-label">当前余额</span>
              <span
                class="paywall-modal__info-value"
                :class="{ 'paywall-modal__info-value--insufficient': !hasSufficientBalance }"
              >
                ¥{{ currentBalance.toFixed(2) }}
              </span>
            </div>
            <div v-if="!hasSufficientBalance" class="paywall-modal__info-row paywall-modal__info-row--gap">
              <span class="paywall-modal__info-label">还需充值</span>
              <span class="paywall-modal__info-value paywall-modal__info-value--gap">
                ¥{{ shortfall.toFixed(2) }}
              </span>
            </div>
          </div>

          <!-- Error -->
          <p v-if="errorMsg" class="paywall-modal__error" role="alert">{{ errorMsg }}</p>

          <!-- Actions -->
          <div class="paywall-modal__actions">
            <template v-if="hasSufficientBalance">
              <button
                class="paywall-modal__btn paywall-modal__btn--primary"
                :disabled="unlocking"
                @click="handleUnlock"
              >
                <span v-if="unlocking" class="spinner" aria-hidden="true" />
                {{ unlocking ? '解锁中...' : `解锁 ¥${price.toFixed(2)}` }}
              </button>
            </template>
            <template v-else>
              <router-link
                :to="{ name: 'Recharge' }"
                class="paywall-modal__btn paywall-modal__btn--primary"
                @click="emit('close')"
              >
                去充值
              </router-link>
            </template>
            <button
              class="paywall-modal__btn paywall-modal__btn--secondary"
              @click="emit('close')"
            >
              取消
            </button>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { unlockContentApi } from '../api/user'
import { useAuthStore } from '../stores/auth'

const props = defineProps<{
  visible: boolean
  contentId: number
  price: number
}>()

const emit = defineEmits<{
  (e: 'unlock'): void
  (e: 'close'): void
}>()

const authStore = useAuthStore()
const unlocking = ref(false)
const errorMsg = ref('')

const currentBalance = computed(() => authStore.user?.balance ?? 0)
const hasSufficientBalance = computed(() => currentBalance.value >= props.price)
const shortfall = computed(() => Math.max(0, props.price - currentBalance.value))

async function handleUnlock() {
  if (!hasSufficientBalance.value) return
  unlocking.value = true
  errorMsg.value = ''
  try {
    const res = await unlockContentApi(props.contentId)
    const { success, newBalance } = res.data.data
    if (success) {
      // Update auth store balance
      if (authStore.user) {
        authStore.setUser({ ...authStore.user, balance: newBalance })
      }
      emit('unlock')
    } else {
      errorMsg.value = '解锁失败，请重试'
    }
  } catch (err: unknown) {
    const axiosErr = err as { response?: { data?: { message?: string }; status?: number } }
    if (axiosErr.response?.status === 402 || axiosErr.response?.status === 400) {
      errorMsg.value = '余额不足，请先充值'
    } else {
      errorMsg.value = axiosErr.response?.data?.message ?? '解锁失败，请稍后重试'
    }
  } finally {
    unlocking.value = false
  }
}
</script>

<style scoped>
.paywall-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 200;
  padding: var(--spacing-lg);
}

.paywall-modal {
  position: relative;
  background: var(--color-bg-card);
  border-radius: var(--radius-xl);
  padding: var(--spacing-2xl);
  width: 100%;
  max-width: 360px;
  box-shadow: var(--shadow-lg);
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--spacing-md);
}

.paywall-modal__close {
  position: absolute;
  top: var(--spacing-md);
  right: var(--spacing-md);
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border-radius: var(--radius-full);
  color: var(--color-text-muted);
  transition: background var(--transition-fast), color var(--transition-fast);
}

.paywall-modal__close:hover {
  background: var(--color-bg-hover);
  color: var(--color-text-secondary);
}

.paywall-modal__icon {
  width: 64px;
  height: 64px;
  border-radius: var(--radius-full);
  background: var(--color-secondary);
  color: var(--color-primary);
  display: flex;
  align-items: center;
  justify-content: center;
}

.paywall-modal__title {
  font-size: var(--font-size-xl);
  font-weight: var(--font-weight-bold);
  color: var(--color-text-primary);
  text-align: center;
}

.paywall-modal__desc {
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
  text-align: center;
}

/* Info rows */
.paywall-modal__info {
  width: 100%;
  background: var(--color-bg);
  border: 1px solid var(--color-border-light);
  border-radius: var(--radius-lg);
  padding: var(--spacing-md) var(--spacing-lg);
  display: flex;
  flex-direction: column;
  gap: var(--spacing-sm);
}

.paywall-modal__info-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: var(--font-size-sm);
}

.paywall-modal__info-label {
  color: var(--color-text-secondary);
}

.paywall-modal__info-value {
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-primary);
}

.paywall-modal__info-value--price {
  color: var(--color-primary);
}

.paywall-modal__info-value--insufficient {
  color: var(--color-error);
}

.paywall-modal__info-value--gap {
  color: var(--color-warning);
}

/* Error */
.paywall-modal__error {
  font-size: var(--font-size-sm);
  color: var(--color-error);
  text-align: center;
  width: 100%;
}

/* Actions */
.paywall-modal__actions {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-sm);
  width: 100%;
  margin-top: var(--spacing-xs);
}

.paywall-modal__btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--spacing-sm);
  width: 100%;
  padding: var(--spacing-sm) var(--spacing-lg);
  border-radius: var(--radius-md);
  font-size: var(--font-size-base);
  font-weight: var(--font-weight-semibold);
  cursor: pointer;
  border: none;
  text-decoration: none;
  transition: background var(--transition-fast), color var(--transition-fast);
  min-height: 44px;
}

.paywall-modal__btn--primary {
  background: var(--color-primary);
  color: var(--color-text-inverse);
}

.paywall-modal__btn--primary:hover:not(:disabled) {
  background: var(--color-primary-dark);
  color: var(--color-text-inverse);
}

.paywall-modal__btn--primary:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.paywall-modal__btn--secondary {
  background: var(--color-bg-hover);
  color: var(--color-text-secondary);
}

.paywall-modal__btn--secondary:hover {
  background: var(--color-border);
  color: var(--color-text-primary);
}

/* Spinner */
.spinner {
  width: 16px;
  height: 16px;
  border: 2px solid rgba(255, 255, 255, 0.3);
  border-top-color: #fff;
  border-radius: 50%;
  animation: spin 0.6s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

/* Transition */
.paywall-fade-enter-active,
.paywall-fade-leave-active {
  transition: opacity var(--transition-normal);
}

.paywall-fade-enter-from,
.paywall-fade-leave-to {
  opacity: 0;
}

.paywall-fade-enter-active .paywall-modal,
.paywall-fade-leave-active .paywall-modal {
  transition: transform var(--transition-normal);
}

.paywall-fade-enter-from .paywall-modal {
  transform: scale(0.95) translateY(8px);
}

.paywall-fade-leave-to .paywall-modal {
  transform: scale(0.95) translateY(8px);
}
</style>
