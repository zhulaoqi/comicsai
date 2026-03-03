<template>
  <div class="infinite-scroll">
    <slot />
    <div ref="sentinelRef" class="infinite-scroll__sentinel" />
    <div v-if="loading" class="infinite-scroll__indicator" role="status">
      <span class="infinite-scroll__spinner" aria-hidden="true" />
      <span>加载中...</span>
    </div>
    <div v-else-if="finished" class="infinite-scroll__done">
      已加载全部内容
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'

const props = defineProps<{
  loading: boolean
  finished: boolean
}>()

const emit = defineEmits<{ (e: 'load-more'): void }>()

const sentinelRef = ref<HTMLElement | null>(null)
let observer: IntersectionObserver | null = null

onMounted(() => {
  if (!sentinelRef.value) return
  observer = new IntersectionObserver(
    (entries) => {
      if (entries[0]?.isIntersecting && !props.loading && !props.finished) {
        emit('load-more')
      }
    },
    { rootMargin: '200px' },
  )
  observer.observe(sentinelRef.value)
})

onUnmounted(() => {
  observer?.disconnect()
})
</script>

<style scoped>
.infinite-scroll__sentinel {
  height: 1px;
}

.infinite-scroll__indicator {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--spacing-sm);
  padding: var(--spacing-xl) 0;
  color: var(--color-text-muted);
  font-size: var(--font-size-sm);
}

.infinite-scroll__spinner {
  width: 20px;
  height: 20px;
  border: 2px solid var(--color-border);
  border-top-color: var(--color-primary);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.infinite-scroll__done {
  text-align: center;
  padding: var(--spacing-xl) 0;
  color: var(--color-text-muted);
  font-size: var(--font-size-sm);
}
</style>
