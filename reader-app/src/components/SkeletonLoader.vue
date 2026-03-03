<template>
  <div class="skeleton-grid" role="status" aria-label="Loading content">
    <div v-for="n in count" :key="n" class="skeleton-card">
      <div class="skeleton-cover skeleton-pulse" />
      <div class="skeleton-body">
        <div class="skeleton-line skeleton-title skeleton-pulse" />
        <div class="skeleton-meta">
          <div class="skeleton-line skeleton-tag skeleton-pulse" />
          <div class="skeleton-line skeleton-date skeleton-pulse" />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
withDefaults(defineProps<{ count?: number }>(), { count: 8 })
</script>

<style scoped>
.skeleton-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: var(--spacing-lg);
}

.skeleton-card {
  border-radius: var(--radius-lg);
  overflow: hidden;
  background: var(--color-bg-card);
}

.skeleton-cover {
  width: 100%;
  aspect-ratio: 3 / 4;
  background: var(--color-border-light);
}

.skeleton-body {
  padding: var(--spacing-md);
}

.skeleton-line {
  border-radius: var(--radius-sm);
  background: var(--color-border-light);
}

.skeleton-title {
  height: 16px;
  width: 75%;
  margin-bottom: var(--spacing-sm);
}

.skeleton-meta {
  display: flex;
  gap: var(--spacing-sm);
}

.skeleton-tag {
  height: 12px;
  width: 40px;
}

.skeleton-date {
  height: 12px;
  width: 60px;
}

.skeleton-pulse {
  animation: pulse 1.5s ease-in-out infinite;
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.4; }
}
</style>
