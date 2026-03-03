<template>
  <div class="recharge-page">
    <header class="recharge-header">
      <button class="recharge-header__back" aria-label="返回" @click="router.back()">
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <line x1="19" y1="12" x2="5" y2="12" /><polyline points="12 19 5 12 12 5" />
        </svg>
      </button>
      <h1 class="recharge-header__title">充值</h1>
    </header>

    <main class="recharge-content container">
      <!-- Current balance -->
      <section class="balance-display">
        <span class="balance-display__label">当前余额</span>
        <span class="balance-display__amount">¥{{ currentBalance.toFixed(2) }}</span>
      </section>

      <!-- Preset amounts -->
      <section class="amount-section">
        <h2 class="amount-section__title">选择充值金额</h2>
        <div class="amount-grid" role="group" aria-label="充值金额选项">
          <button
            v-for="preset in presetAmounts"
            :key="preset"
            class="amount-option"
            :class="{ 'amount-option--active': selectedAmount === preset && !customMode }"
            @click="selectPreset(preset)"
          >
            <span class="amount-option__value">¥{{ preset }}</span>
          </button>
        </div>

        <!-- Custom amount -->
        <div class="custom-amount">
          <label class="custom-amount__label" for="customAmount">自定义金额</label>
          <div class="custom-amount__input-wrap" :class="{ 'custom-amount__input-wrap--active': customMode }">
            <span class="custom-amount__prefix">¥</span>
            <input
              id="customAmount"
              v-model="customAmountStr"
              type="number"
              class="custom-amount__input"
              placeholder="输入金额"
              min="1"
              max="9999"
              step="1"
              @focus="customMode = true"
              @input="onCustomInput"
            />
          </div>
        </div>
      </section>

      <!-- Summary -->
      <section class="recharge-summary">
        <div class="recharge-summary__row">
          <span>充值金额</span>
          <span class="recharge-summary__value">¥{{ finalAmount.toFixed(2) }}</span>
        </div>
        <div class="recharge-summary__row">
          <span>充值后余额</span>
          <span class="recharge-summary__value recharge-summary__value--highlight">
            ¥{{ (currentBalance + finalAmount).toFixed(2) }}
          </span>
        </div>
      </section>

      <!-- Error -->
      <p v-if="errorMsg" class="recharge-error" role="alert">{{ errorMsg }}</p>

      <!-- Confirm button -->
      <button
        class="btn-confirm"
        :disabled="finalAmount <= 0 || loading"
        @click="handleRecharge"
      >
        <span v-if="loading" class="spinner" aria-hidden="true" />
        {{ loading ? '处理中...' : `确认充值 ¥${finalAmount.toFixed(2)}` }}
      </button>
    </main>

    <!-- Success overlay -->
    <Transition name="fade">
      <div v-if="showSuccess" class="success-overlay" role="status" aria-live="polite">
        <div class="success-card">
          <div class="success-icon" aria-hidden="true">✓</div>
          <p class="success-title">充值成功</p>
          <p class="success-balance">余额：¥{{ currentBalance.toFixed(2) }}</p>
          <button class="btn-primary" @click="goToProfile">返回个人中心</button>
        </div>
      </div>
    </Transition>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getUserProfileApi, rechargeApi } from '../api/user'
import { useAuthStore } from '../stores/auth'

const router = useRouter()
const authStore = useAuthStore()

const presetAmounts = [10, 30, 50, 100]

const currentBalance = ref(0)
const selectedAmount = ref(30)
const customAmountStr = ref('')
const customMode = ref(false)
const loading = ref(false)
const errorMsg = ref('')
const showSuccess = ref(false)

const finalAmount = computed(() => {
  if (customMode.value) {
    const val = parseFloat(customAmountStr.value)
    return isNaN(val) || val <= 0 ? 0 : Math.floor(val)
  }
  return selectedAmount.value
})

function selectPreset(amount: number) {
  selectedAmount.value = amount
  customMode.value = false
  customAmountStr.value = ''
  errorMsg.value = ''
}

function onCustomInput() {
  errorMsg.value = ''
}

async function loadBalance() {
  try {
    const res = await getUserProfileApi()
    currentBalance.value = res.data.data.balance
  } catch {
    // Use balance from auth store as fallback
    currentBalance.value = authStore.user?.balance ?? 0
  }
}

async function handleRecharge() {
  if (finalAmount.value <= 0) {
    errorMsg.value = '请选择或输入有效的充值金额'
    return
  }
  if (finalAmount.value > 9999) {
    errorMsg.value = '单次充值金额不能超过 ¥9999'
    return
  }

  loading.value = true
  errorMsg.value = ''
  try {
    const res = await rechargeApi(finalAmount.value)
    currentBalance.value = res.data.data.balance
    // Sync auth store balance
    if (authStore.user) {
      authStore.setUser({ ...authStore.user, balance: currentBalance.value })
    }
    showSuccess.value = true
  } catch (err: unknown) {
    const axiosErr = err as { response?: { data?: { message?: string } } }
    errorMsg.value = axiosErr.response?.data?.message ?? '充值失败，请稍后重试'
  } finally {
    loading.value = false
  }
}

function goToProfile() {
  router.push({ name: 'Profile' })
}

onMounted(() => {
  loadBalance()
})
</script>

<style scoped>
.recharge-page {
  min-height: 100vh;
  background: var(--color-bg);
  padding-bottom: var(--spacing-3xl);
}

.recharge-header {
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

.recharge-header__back {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  border-radius: var(--radius-full);
  color: var(--color-text-secondary);
  transition: background var(--transition-fast), color var(--transition-fast);
}

.recharge-header__back:hover {
  background: var(--color-bg-hover);
  color: var(--color-text-primary);
}

.recharge-header__title {
  font-size: var(--font-size-lg);
  font-weight: var(--font-weight-semibold);
}

.recharge-content {
  padding-top: var(--spacing-xl);
  display: flex;
  flex-direction: column;
  gap: var(--spacing-xl);
  max-width: 480px;
}

/* Balance display */
.balance-display {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--spacing-xs);
  padding: var(--spacing-xl);
  background: var(--color-bg-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-xl);
  text-align: center;
}

.balance-display__label {
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
}

.balance-display__amount {
  font-size: var(--font-size-3xl);
  font-weight: var(--font-weight-bold);
  color: var(--color-text-primary);
}

/* Amount section */
.amount-section {
  background: var(--color-bg-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-xl);
  padding: var(--spacing-xl);
  display: flex;
  flex-direction: column;
  gap: var(--spacing-lg);
}

.amount-section__title {
  font-size: var(--font-size-base);
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-primary);
}

.amount-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: var(--spacing-sm);
}

.amount-option {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: var(--spacing-md) var(--spacing-sm);
  border: 2px solid var(--color-border);
  border-radius: var(--radius-lg);
  background: var(--color-bg);
  cursor: pointer;
  transition: border-color var(--transition-fast), background var(--transition-fast);
}

.amount-option:hover {
  border-color: var(--color-primary-light);
  background: var(--color-secondary);
}

.amount-option--active {
  border-color: var(--color-primary);
  background: var(--color-secondary);
}

.amount-option__value {
  font-size: var(--font-size-base);
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-primary);
}

.amount-option--active .amount-option__value {
  color: var(--color-primary);
}

/* Custom amount */
.custom-amount {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-xs);
}

.custom-amount__label {
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-medium);
  color: var(--color-text-secondary);
}

.custom-amount__input-wrap {
  display: flex;
  align-items: center;
  gap: var(--spacing-xs);
  padding: var(--spacing-sm) var(--spacing-md);
  border: 2px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-bg);
  transition: border-color var(--transition-fast);
}

.custom-amount__input-wrap--active {
  border-color: var(--color-primary);
}

.custom-amount__prefix {
  font-size: var(--font-size-base);
  color: var(--color-text-secondary);
  flex-shrink: 0;
}

.custom-amount__input {
  flex: 1;
  border: none;
  background: transparent;
  font-size: var(--font-size-base);
  color: var(--color-text-primary);
  outline: none;
}

.custom-amount__input::placeholder {
  color: var(--color-text-muted);
}

/* Hide number input arrows */
.custom-amount__input::-webkit-outer-spin-button,
.custom-amount__input::-webkit-inner-spin-button {
  -webkit-appearance: none;
}

/* Summary */
.recharge-summary {
  background: var(--color-bg-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-xl);
  padding: var(--spacing-lg) var(--spacing-xl);
  display: flex;
  flex-direction: column;
  gap: var(--spacing-md);
}

.recharge-summary__row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
}

.recharge-summary__value {
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-primary);
}

.recharge-summary__value--highlight {
  color: var(--color-primary);
  font-size: var(--font-size-base);
}

/* Error */
.recharge-error {
  font-size: var(--font-size-sm);
  color: var(--color-error);
  text-align: center;
}

/* Confirm button */
.btn-confirm {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--spacing-sm);
  width: 100%;
  padding: var(--spacing-md);
  background: var(--color-primary);
  color: var(--color-text-inverse);
  font-size: var(--font-size-base);
  font-weight: var(--font-weight-semibold);
  border-radius: var(--radius-lg);
  transition: background var(--transition-fast);
  min-height: 52px;
  cursor: pointer;
  border: none;
}

.btn-confirm:hover:not(:disabled) {
  background: var(--color-primary-dark);
}

.btn-confirm:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

/* Success overlay */
.success-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 100;
  padding: var(--spacing-lg);
}

.success-card {
  background: var(--color-bg-card);
  border-radius: var(--radius-xl);
  padding: var(--spacing-3xl) var(--spacing-2xl);
  text-align: center;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--spacing-md);
  max-width: 320px;
  width: 100%;
  box-shadow: var(--shadow-lg);
}

.success-icon {
  width: 64px;
  height: 64px;
  border-radius: var(--radius-full);
  background: var(--color-success);
  color: white;
  font-size: var(--font-size-2xl);
  display: flex;
  align-items: center;
  justify-content: center;
}

.success-title {
  font-size: var(--font-size-xl);
  font-weight: var(--font-weight-bold);
  color: var(--color-text-primary);
}

.success-balance {
  font-size: var(--font-size-base);
  color: var(--color-text-secondary);
}

.btn-primary {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: var(--spacing-sm) var(--spacing-xl);
  background: var(--color-primary);
  color: var(--color-text-inverse);
  border-radius: var(--radius-md);
  font-weight: var(--font-weight-semibold);
  font-size: var(--font-size-sm);
  text-decoration: none;
  transition: background var(--transition-fast);
  cursor: pointer;
  border: none;
  margin-top: var(--spacing-sm);
}

.btn-primary:hover {
  background: var(--color-primary-dark);
}

/* Spinner */
.spinner {
  width: 18px;
  height: 18px;
  border: 2px solid rgba(255, 255, 255, 0.3);
  border-top-color: #fff;
  border-radius: 50%;
  animation: spin 0.6s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

/* Fade transition */
.fade-enter-active,
.fade-leave-active {
  transition: opacity var(--transition-normal);
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

@media (max-width: 480px) {
  .amount-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
