<template>
  <div class="data-table">
    <el-table :data="data" v-bind="$attrs" stripe border>
      <slot />
    </el-table>
    <el-pagination
      v-if="total > 0"
      v-model:current-page="currentPage"
      v-model:page-size="pageSize"
      :total="total"
      :page-sizes="[10, 20, 50]"
      layout="total, sizes, prev, pager, next"
      style="margin-top: 16px; justify-content: flex-end"
      @size-change="emit('page-change', currentPage, pageSize)"
      @current-change="emit('page-change', currentPage, pageSize)"
    />
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'

defineProps<{
  data: unknown[]
  total: number
}>()

const emit = defineEmits<{
  'page-change': [page: number, size: number]
}>()

const currentPage = ref(1)
const pageSize = ref(10)
</script>
