<template>
  <el-card class="chart-panel" shadow="hover">
    <template #header>
      <span>{{ title }}</span>
    </template>
    <div ref="chartRef" class="chart-container" :style="{ height: height + 'px' }"></div>
  </el-card>
</template>

<script setup lang="ts">
import { ref } from 'vue'

defineProps<{
  title: string
  height?: number
}>()

const chartRef = ref<HTMLDivElement>()

defineExpose({ chartRef })
</script>

<style scoped>
.chart-container {
  width: 100%;
}
</style>
