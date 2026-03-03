<template>
  <router-link :to="cardLink" class="content-card" :aria-label="content.title">
    <div class="content-card__cover">
      <img
        :src="content.coverUrl"
        :srcset="coverSrcset"
        sizes="(max-width: 639px) calc(50vw - 24px), (max-width: 767px) calc(33vw - 24px), (max-width: 1023px) calc(25vw - 24px), 220px"
        :alt="content.title"
        loading="lazy"
        class="content-card__image"
        @error="onImageError"
      />
      <span class="content-card__type" :class="`content-card__type--${content.contentType.toLowerCase()}`">
        {{ content.contentType === 'COMIC' ? '漫画' : '小说' }}
      </span>
      <span v-if="content.isPaid" class="content-card__paid">
        ¥{{ content.price ?? '' }}
      </span>
    </div>
    <div class="content-card__body">
      <h3 class="content-card__title">{{ content.title }}</h3>
      <time class="content-card__date" :datetime="content.publishedAt">
        {{ formatDate(content.publishedAt) }}
      </time>
    </div>
  </router-link>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { ContentItem } from '../api/content'

const props = defineProps<{ content: ContentItem }>()

const cardLink = computed(() => {
  const route = props.content.contentType === 'COMIC' ? 'ComicReader' : 'NovelReader'
  return { name: route, params: { id: props.content.id } }
})

// Build srcset for responsive cover images
const coverSrcset = computed(() => {
  const base = props.content.coverUrl
  if (!base) return undefined
  // Append size hints as query params; backend serves resized images via ?w=
  return `${base}?w=300 300w, ${base}?w=480 480w, ${base}?w=640 640w`
})

function formatDate(dateStr: string): string {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

function onImageError(e: Event) {
  const img = e.target as HTMLImageElement
  img.src = 'data:image/svg+xml,' + encodeURIComponent(
    '<svg xmlns="http://www.w3.org/2000/svg" width="300" height="400" fill="%23e8ecf1"><rect width="300" height="400"/><text x="150" y="200" text-anchor="middle" fill="%239ca3af" font-size="14">No Image</text></svg>'
  )
}
</script>

<style scoped>
.content-card {
  display: block;
  border-radius: var(--radius-lg);
  overflow: hidden;
  background: var(--color-bg-card);
  box-shadow: var(--shadow-sm);
  transition: transform var(--transition-fast), box-shadow var(--transition-fast);
  text-decoration: none;
  color: inherit;
}

.content-card:hover {
  transform: translateY(-4px);
  box-shadow: var(--shadow-lg);
}

.content-card__cover {
  position: relative;
  width: 100%;
  aspect-ratio: 3 / 4;
  overflow: hidden;
  background: var(--color-border-light);
}

.content-card__image {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform var(--transition-normal);
}

.content-card:hover .content-card__image {
  transform: scale(1.05);
}

.content-card__type {
  position: absolute;
  top: var(--spacing-sm);
  left: var(--spacing-sm);
  padding: 2px 8px;
  border-radius: var(--radius-sm);
  font-size: var(--font-size-xs);
  font-weight: var(--font-weight-medium);
  color: var(--color-text-inverse);
}

.content-card__type--comic {
  background: var(--color-primary);
}

.content-card__type--novel {
  background: var(--color-success);
}

.content-card__paid {
  position: absolute;
  top: var(--spacing-sm);
  right: var(--spacing-sm);
  padding: 2px 8px;
  border-radius: var(--radius-sm);
  font-size: var(--font-size-xs);
  font-weight: var(--font-weight-semibold);
  background: var(--color-accent);
  color: var(--color-text-inverse);
}

.content-card__body {
  padding: var(--spacing-md);
}

.content-card__title {
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-semibold);
  line-height: var(--line-height-tight);
  color: var(--color-text-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.content-card__date {
  display: block;
  margin-top: var(--spacing-xs);
  font-size: var(--font-size-xs);
  color: var(--color-text-muted);
}
</style>
