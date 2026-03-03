<template>
  <div class="user-analytics">
    <!-- Filter Bar -->
    <el-card shadow="never" class="filter-card">
      <el-form inline>
        <el-form-item label="时间范围">
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
            style="width: 260px"
          />
        </el-form-item>
        <el-form-item label="内容类型">
          <el-select v-model="filters.contentType" placeholder="全部" clearable style="width: 120px">
            <el-option label="漫画" value="COMIC" />
            <el-option label="小说" value="NOVEL" />
          </el-select>
        </el-form-item>
        <el-form-item label="付费状态">
          <el-select v-model="filters.isPaid" placeholder="全部" clearable style="width: 120px">
            <el-option label="免费" :value="false" />
            <el-option label="付费" :value="true" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="fetchData">查询</el-button>
          <el-button @click="resetFilters">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- Summary Cards -->
    <el-row :gutter="16" class="summary-row" v-loading="loading">
      <el-col :span="8">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-value">{{ data?.totalViews ?? '-' }}</div>
          <div class="stat-label">总浏览次数</div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-value">{{ data?.uniqueViewers ?? '-' }}</div>
          <div class="stat-label">独立访客数</div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-value">{{ avgDurationDisplay }}</div>
          <div class="stat-label">平均阅读时长</div>
        </el-card>
      </el-col>
    </el-row>

    <!-- Content Usage Table -->
    <el-card shadow="never" class="table-card">
      <template #header>内容使用明细</template>
      <el-table :data="data?.contentUsageList ?? []" v-loading="loading" stripe>
        <el-table-column prop="title" label="内容标题" min-width="200" show-overflow-tooltip />
        <el-table-column prop="contentType" label="类型" width="80">
          <template #default="{ row }">
            <el-tag :type="row.contentType === 'COMIC' ? 'primary' : 'success'" size="small">
              {{ row.contentType === 'COMIC' ? '漫画' : '小说' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="isPaid" label="付费" width="70">
          <template #default="{ row }">
            <el-tag :type="row.isPaid ? 'warning' : 'info'" size="small">
              {{ row.isPaid ? '付费' : '免费' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="totalViews" label="浏览次数" width="100" sortable />
        <el-table-column prop="uniqueViewers" label="独立访客" width="100" sortable />
        <el-table-column label="平均时长" width="110" sortable :sort-method="sortByDuration">
          <template #default="{ row }">
            {{ formatDuration(row.averageDurationSeconds) }}
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { analyticsApi, type UsageAnalyticsVO, type UsageQueryParams } from '../api/analytics'

const loading = ref(false)
const data = ref<UsageAnalyticsVO | null>(null)
const dateRange = ref<[string, string] | null>(null)

const filters = ref<UsageQueryParams>({
  contentType: '',
  isPaid: '',
})

const avgDurationDisplay = computed(() => {
  if (!data.value) return '-'
  return formatDuration(data.value.averageDurationSeconds)
})

function formatDuration(seconds: number): string {
  if (!seconds) return '0秒'
  const m = Math.floor(seconds / 60)
  const s = Math.round(seconds % 60)
  return m > 0 ? `${m}分${s}秒` : `${s}秒`
}

function sortByDuration(a: { averageDurationSeconds: number }, b: { averageDurationSeconds: number }) {
  return a.averageDurationSeconds - b.averageDurationSeconds
}

async function fetchData() {
  loading.value = true
  try {
    const params: UsageQueryParams = {}
    if (dateRange.value) {
      params.startDate = dateRange.value[0]
      params.endDate = dateRange.value[1]
    }
    if (filters.value.contentType) params.contentType = filters.value.contentType
    if (filters.value.isPaid !== '') params.isPaid = filters.value.isPaid

    const res = await analyticsApi.getUsage(params)
    if (res.code === 200) {
      data.value = res.data
    } else {
      ElMessage.error(res.message || '获取数据失败')
    }
  } catch {
    ElMessage.error('获取用户统计数据失败')
  } finally {
    loading.value = false
  }
}

function resetFilters() {
  dateRange.value = null
  filters.value = { contentType: '', isPaid: '' }
  fetchData()
}

onMounted(fetchData)
</script>

<style scoped>
.user-analytics {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.filter-card :deep(.el-card__body) {
  padding: 16px;
}
.summary-row {
  margin: 0;
}
.stat-card {
  text-align: center;
  padding: 8px 0;
}
.stat-value {
  font-size: 32px;
  font-weight: 700;
  color: var(--el-color-primary);
  line-height: 1.2;
}
.stat-label {
  font-size: 13px;
  color: var(--el-text-color-secondary);
  margin-top: 6px;
}
.table-card {
  flex: 1;
}
</style>
