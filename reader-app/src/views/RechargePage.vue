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
      <!-- Step 1: Select amount -->
      <template v-if="step === 'select'">
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

        <!-- Payment method -->
        <section class="payment-method">
          <h2 class="payment-method__title">支付方式</h2>
          <div class="payment-method__options">
            <button
              class="payment-method__btn"
              :class="{ 'payment-method__btn--active': payMethod === 'xunhupay' }"
              @click="payMethod = 'xunhupay'"
            >
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <rect x="1" y="4" width="22" height="16" rx="2" ry="2" />
                <line x1="1" y1="10" x2="23" y2="10" />
              </svg>
              支付宝 / 微信
            </button>
          </div>
        </section>

        <!-- Error -->
        <p v-if="errorMsg" class="recharge-error" role="alert">{{ errorMsg }}</p>

        <!-- Confirm button -->
        <button
          class="btn-confirm"
          :disabled="finalAmount <= 0 || loading"
          @click="handleCreateOrder"
        >
          <span v-if="loading" class="spinner" aria-hidden="true" />
          {{ loading ? '创建订单中...' : `确认充值 ¥${finalAmount.toFixed(2)}` }}
        </button>
      </template>

      <!-- Step 2: QR code / pay link -->
      <template v-if="step === 'paying'">
        <section class="qrcode-section">
          <div class="qrcode-header">
            <h2 class="qrcode-title">扫码支付</h2>
            <p class="qrcode-amount">¥{{ orderAmount.toFixed(2) }}</p>
          </div>

          <div class="qrcode-wrap">
            <div v-if="qrcodeUrl" class="qrcode-frame">
              <img :src="qrcodeUrl" alt="支付二维码" class="qrcode-img" />
            </div>
            <p class="qrcode-hint">请使用 支付宝 / 微信 扫描二维码完成支付</p>
          </div>

          <!-- Mobile: direct pay link -->
          <a v-if="payUrl" :href="payUrl" class="btn-mobile-pay" target="_blank">
            手机端直接支付 →
          </a>

          <div class="qrcode-status">
            <div v-if="polling" class="polling-indicator">
              <div class="polling-dot" />
              等待支付中...
            </div>
            <p class="qrcode-expire">
              二维码有效期 {{ xunhupayProperties.orderExpireMinutes || 5 }} 分钟，过期后请重新创建订单
            </p>
          </div>

          <div class="qrcode-actions">
            <button class="btn-secondary" @click="cancelPayment">取消支付</button>
          </div>
        </section>
      </template>
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
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { getUserProfileApi, createPaymentOrderApi, getPaymentStatusApi } from '../api/user'
import { useAuthStore } from '../stores/auth'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

const presetAmounts = [10, 30, 50, 100]

const currentBalance = ref(0)
const selectedAmount = ref(30)
const customAmountStr = ref('')
const customMode = ref(false)
const loading = ref(false)
const errorMsg = ref('')
const showSuccess = ref(false)
const payMethod = ref('xunhupay')

type Step = 'select' | 'paying'
const step = ref<Step>('select')

const orderNo = ref('')
const orderAmount = ref(0)
const qrcodeUrl = ref('')
const payUrl = ref('')
const polling = ref(false)
let pollTimer: ReturnType<typeof setInterval> | null = null

const xunhupayProperties = { orderExpireMinutes: 5 }

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
    currentBalance.value = authStore.user?.balance ?? 0
  }
}

async function handleCreateOrder() {
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
    const res = await createPaymentOrderApi(finalAmount.value)
    const data = res.data.data
    orderNo.value = data.orderNo
    orderAmount.value = data.amount
    qrcodeUrl.value = data.qrcodeUrl
    payUrl.value = data.payUrl
    step.value = 'paying'
    startPolling()
  } catch (err: unknown) {
    const axiosErr = err as { response?: { data?: { message?: string } } }
    errorMsg.value = axiosErr.response?.data?.message ?? '创建订单失败，请稍后重试'
  } finally {
    loading.value = false
  }
}

function startPolling() {
  polling.value = true
  pollTimer = setInterval(async () => {
    try {
      const res = await getPaymentStatusApi(orderNo.value)
      const status = res.data.data.status
      if (status === 'PAID') {
        stopPolling()
        await loadBalance()
        if (authStore.user) {
          authStore.setUser({ ...authStore.user, balance: currentBalance.value })
        }
        showSuccess.value = true
        step.value = 'select'
      } else if (status === 'EXPIRED' || status === 'FAILED') {
        stopPolling()
        errorMsg.value = status === 'EXPIRED' ? '订单已过期，请重新充值' : '支付失败，请重试'
        step.value = 'select'
      }
    } catch {
      // 轮询失败静默处理
    }
  }, 3000)
}

function stopPolling() {
  polling.value = false
  if (pollTimer) {
    clearInterval(pollTimer)
    pollTimer = null
  }
}

function cancelPayment() {
  stopPolling()
  step.value = 'select'
  errorMsg.value = ''
}

function goToProfile() {
  router.push({ name: 'Profile' })
}

onMounted(() => {
  loadBalance()
  if (route.query.status === 'success') {
    loadBalance().then(() => {
      if (authStore.user) {
        authStore.setUser({ ...authStore.user, balance: currentBalance.value })
      }
      showSuccess.value = true
    })
  }
})

onBeforeUnmount(() => {
  stopPolling()
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

.custom-amount__input::-webkit-outer-spin-button,
.custom-amount__input::-webkit-inner-spin-button {
  -webkit-appearance: none;
}

/* Payment method */
.payment-method {
  background: var(--color-bg-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-xl);
  padding: var(--spacing-xl);
  display: flex;
  flex-direction: column;
  gap: var(--spacing-md);
}

.payment-method__title {
  font-size: var(--font-size-base);
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-primary);
}

.payment-method__options {
  display: flex;
  gap: var(--spacing-sm);
}

.payment-method__btn {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--spacing-sm);
  padding: var(--spacing-md);
  border: 2px solid var(--color-border);
  border-radius: var(--radius-lg);
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-medium);
  color: var(--color-text-primary);
  transition: border-color var(--transition-fast), background var(--transition-fast);
}

.payment-method__btn:hover {
  border-color: var(--color-primary-light);
}

.payment-method__btn--active {
  border-color: var(--color-primary);
  background: var(--color-secondary);
  color: var(--color-primary);
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

/* QR Code section */
.qrcode-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--spacing-xl);
}

.qrcode-header {
  text-align: center;
}

.qrcode-title {
  font-size: var(--font-size-xl);
  font-weight: var(--font-weight-bold);
  color: var(--color-text-primary);
}

.qrcode-amount {
  font-size: var(--font-size-3xl);
  font-weight: var(--font-weight-bold);
  color: var(--color-primary);
  margin-top: var(--spacing-xs);
}

.qrcode-wrap {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--spacing-md);
}

.qrcode-frame {
  width: 240px;
  height: 240px;
  background: #fff;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: var(--spacing-md);
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: var(--shadow-md);
}

.qrcode-img {
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.qrcode-hint {
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
  text-align: center;
}

.btn-mobile-pay {
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
}

.btn-mobile-pay:hover {
  background: var(--color-primary-dark);
  color: var(--color-text-inverse);
}

.qrcode-status {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--spacing-sm);
}

.polling-indicator {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
  font-size: var(--font-size-sm);
  color: var(--color-primary);
  font-weight: var(--font-weight-medium);
}

.polling-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--color-primary);
  animation: pulse 1.5s ease-in-out infinite;
}

@keyframes pulse {
  0%, 100% { opacity: 1; transform: scale(1); }
  50% { opacity: 0.4; transform: scale(0.8); }
}

.qrcode-expire {
  font-size: var(--font-size-xs);
  color: var(--color-text-muted);
  text-align: center;
}

.qrcode-actions {
  display: flex;
  gap: var(--spacing-md);
}

.btn-secondary {
  padding: var(--spacing-sm) var(--spacing-xl);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-medium);
  color: var(--color-text-secondary);
  background: var(--color-bg-card);
  cursor: pointer;
  transition: background var(--transition-fast), border-color var(--transition-fast);
}

.btn-secondary:hover {
  background: var(--color-bg-hover);
  border-color: var(--color-text-muted);
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
