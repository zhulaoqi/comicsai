<template>
  <el-card class="filter-bar" shadow="never">
    <el-form :inline="true" :model="filters" @submit.prevent="emit('search')">
      <slot :filters="filters" />
      <el-form-item>
        <el-button type="primary" @click="emit('search')">查询</el-button>
        <el-button @click="emit('reset')">重置</el-button>
      </el-form-item>
    </el-form>
  </el-card>
</template>

<script setup lang="ts">
defineProps<{
  filters: Record<string, unknown>
}>()

const emit = defineEmits<{
  search: []
  reset: []
}>()
</script>

<style scoped>
.filter-bar {
  margin-bottom: 16px;
}
</style>
