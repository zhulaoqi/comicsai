<template>
  <div class="recharge-analytics">
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
          <div class="stat-value">{{ data?.totalRechargeCount ?? '-' }}</div>
          <div class="stat-label">充值总笔数</div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-value">¥{{ data ? Number(data.totalRechargeAmount).toFixed(2) : '-' }}</div>
          <div class="stat-label">充值总金额</div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-value">¥{{ data ? Number(data.averageRechargeAmount).toFixed(2) : '-' }}</div>
          <div class="stat-label">平均充值金额</div>
        </el-card>
      </el-col>
    </el-row>

    <!-- User Recharge Table -->
    <el-card shadow="never">
      <template #header>充值用户明细</template>
      <el-table :data="data?.rechargeUsers ?? []" v-loading="loading" stripe>
        <el-table-column prop="nickname" label="昵称" min-width="120" show-overflow-tooltip />
        <el-table-column prop="email" label="邮箱" min-width="180" show-overflow-tooltip />
        <el-table-column prop="rechargeCount" label="充值次数" width="100" sortable />
        <el-table-column label="充值总额" width="120" sortable :sort-method="sortByRecharge">
          <template #default="{ row }">¥{{ Number(row.totalRechargeAmount).toFixed(2) }}</template>
        </el-table-column>
        <el-table-column prop="unlockCount" label="解锁内容数" width="110" sortable />
        <el-table-column label="消费总额" width="120" sortable :sort-method="sortBySpent">
          <template #default="{ row }">¥{{ Number(row.totalSpent).toFixed(2) }}</template>
        </el-table-column>
        <el-table-column label="消费率" width="100">
          <template #default="{ row }">
            <el-progress
              :percentage="spendRate(row)"
              :stroke-width="8"
              :show-text="false"
              :color="spendRate(row) > 80 ? '#f56c6c' : '#409eff'"
            />
            <span style="font-size: 12px">{{ spendRate(row) }}%</span>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { analyticsApi, type RechargeAnalyticsVO, type RechargeUserVO } from '../api/analytics'

const loading = ref(false)
const data = ref<RechargeAnalyticsVO | null>(null)
const dateRange = ref<[string, string] | null>(null)

function spendRate(row: RechargeUserVO): number {
  if (!row.totalRechargeAmount) return 0
  return Math.min(100, Math.round((Number(row.totalSpent) / Number(row.totalRechargeAmount)) * 100))
}

function sortByRecharge(a: RechargeUserVO, b: RechargeUserVO) {
  return Number(a.totalRechargeAmount) - Number(b.totalRechargeAmount)
}

function sortBySpent(a: RechargeUserVO, b: RechargeUserVO) {
  return Number(a.totalSpent) - Number(b.totalSpent)
}

async function fetchData() {
  loading.value = true
  try {
    const params: { startDate?: string; endDate?: string } = {}
    if (dateRange.value) {
      params.startDate = dateRange.value[0]
      params.endDate = dateRange.value[1]
    }
    const res = await analyticsApi.getRecharge(params)
    if (res.code === 200) {
      data.value = res.data
    } else {
      ElMessage.error(res.message || '获取数据失败')
    }
  } catch {
    ElMessage.error('获取充值统计数据失败')
  } finally {
    loading.value = false
  }
}

function resetFilters() {
  dateRange.value = null
  fetchData()
}

onMounted(fetchData)
</script>

<style scoped>
.recharge-analytics {
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
</style>
