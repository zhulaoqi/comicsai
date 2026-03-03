<template>
  <div class="token-analytics">
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
        <el-form-item label="AI Provider">
          <el-input v-model="filters.providerName" placeholder="全部" clearable style="width: 140px" />
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
          <div class="stat-value">{{ data ? (data.totalInputTokens + data.totalOutputTokens).toLocaleString() : '-' }}</div>
          <div class="stat-label">总Token消耗</div>
          <div class="stat-sub" v-if="data">
            输入 {{ data.totalInputTokens.toLocaleString() }} / 输出 {{ data.totalOutputTokens.toLocaleString() }}
          </div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-value">${{ data ? Number(data.totalEstimatedCost).toFixed(4) : '-' }}</div>
          <div class="stat-label">预估总成本</div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-value">{{ data ? totalCallCount.toLocaleString() : '-' }}</div>
          <div class="stat-label">总调用次数</div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16">
      <!-- Provider/Model Breakdown -->
      <el-col :span="12">
        <el-card shadow="never">
          <template #header>Provider / 模型维度</template>
          <el-table :data="data?.providerModelCosts ?? []" v-loading="loading" stripe size="small">
            <el-table-column prop="providerName" label="Provider" min-width="100" />
            <el-table-column prop="modelName" label="模型" min-width="120" show-overflow-tooltip />
            <el-table-column label="输入Token" width="100" sortable>
              <template #default="{ row }">{{ row.inputTokens.toLocaleString() }}</template>
            </el-table-column>
            <el-table-column label="输出Token" width="100" sortable>
              <template #default="{ row }">{{ row.outputTokens.toLocaleString() }}</template>
            </el-table-column>
            <el-table-column label="预估成本" width="100" sortable>
              <template #default="{ row }">${{ Number(row.estimatedCost).toFixed(4) }}</template>
            </el-table-column>
            <el-table-column prop="callCount" label="调用次数" width="90" sortable />
          </el-table>
        </el-card>
      </el-col>

      <!-- Storyline Breakdown -->
      <el-col :span="12">
        <el-card shadow="never">
          <template #header>Storyline 维度</template>
          <el-table :data="data?.storylineCosts ?? []" v-loading="loading" stripe size="small">
            <el-table-column prop="storylineTitle" label="故事线" min-width="140" show-overflow-tooltip />
            <el-table-column label="输入Token" width="100" sortable>
              <template #default="{ row }">{{ row.inputTokens.toLocaleString() }}</template>
            </el-table-column>
            <el-table-column label="输出Token" width="100" sortable>
              <template #default="{ row }">{{ row.outputTokens.toLocaleString() }}</template>
            </el-table-column>
            <el-table-column label="预估成本" width="100" sortable>
              <template #default="{ row }">${{ Number(row.estimatedCost).toFixed(4) }}</template>
            </el-table-column>
            <el-table-column prop="callCount" label="调用次数" width="90" sortable />
          </el-table>
        </el-card>
      </el-col>
    </el-row>

    <!-- Daily Trend Table -->
    <el-card shadow="never">
      <template #header>每日趋势</template>
      <el-table :data="data?.dailyTrend ?? []" v-loading="loading" stripe size="small">
        <el-table-column prop="date" label="日期" width="120" />
        <el-table-column label="输入Token" width="120">
          <template #default="{ row }">{{ row.inputTokens.toLocaleString() }}</template>
        </el-table-column>
        <el-table-column label="输出Token" width="120">
          <template #default="{ row }">{{ row.outputTokens.toLocaleString() }}</template>
        </el-table-column>
        <el-table-column label="预估成本" width="120">
          <template #default="{ row }">${{ Number(row.estimatedCost).toFixed(4) }}</template>
        </el-table-column>
        <el-table-column prop="callCount" label="调用次数" width="100" />
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { analyticsApi, type TokenCostAnalyticsVO, type TokenCostQueryParams } from '../api/analytics'

const loading = ref(false)
const data = ref<TokenCostAnalyticsVO | null>(null)
const dateRange = ref<[string, string] | null>(null)
const filters = ref<TokenCostQueryParams>({ providerName: '' })

const totalCallCount = computed(() => {
  if (!data.value) return 0
  return data.value.providerModelCosts.reduce((sum, p) => sum + p.callCount, 0)
})

async function fetchData() {
  loading.value = true
  try {
    const params: TokenCostQueryParams = {}
    if (dateRange.value) {
      params.startDate = dateRange.value[0]
      params.endDate = dateRange.value[1]
    }
    if (filters.value.providerName) params.providerName = filters.value.providerName

    const res = await analyticsApi.getTokenCost(params)
    if (res.code === 200) {
      data.value = res.data
    } else {
      ElMessage.error(res.message || '获取数据失败')
    }
  } catch {
    ElMessage.error('获取Token统计数据失败')
  } finally {
    loading.value = false
  }
}

function resetFilters() {
  dateRange.value = null
  filters.value = { providerName: '' }
  fetchData()
}

onMounted(fetchData)
</script>

<style scoped>
.token-analytics {
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
  font-size: 28px;
  font-weight: 700;
  color: var(--el-color-primary);
  line-height: 1.2;
}
.stat-label {
  font-size: 13px;
  color: var(--el-text-color-secondary);
  margin-top: 6px;
}
.stat-sub {
  font-size: 12px;
  color: var(--el-text-color-placeholder);
  margin-top: 4px;
}
</style>
