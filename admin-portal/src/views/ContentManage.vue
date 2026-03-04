<template>
  <div class="content-manage">
    <!-- Header -->
    <div class="page-header">
      <h2>内容管理</h2>
    </div>

    <!-- Filter Bar -->
    <el-card class="filter-card" shadow="never">
      <el-form inline>
        <el-form-item label="状态">
          <el-select v-model="filters.status" placeholder="全部" clearable style="width:140px" @change="fetchList">
            <el-option label="待审核" value="PENDING_REVIEW" />
            <el-option label="待发布" value="PENDING_PUBLISH" />
            <el-option label="已拒绝" value="REJECTED" />
            <el-option label="已发布" value="PUBLISHED" />
            <el-option label="已下架" value="OFFLINE" />
            <el-option label="重新生成中" value="REGENERATING" />
          </el-select>
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="filters.contentType" placeholder="全部" clearable style="width:120px" @change="fetchList">
            <el-option label="漫画" value="COMIC" />
            <el-option label="小说" value="NOVEL" />
          </el-select>
        </el-form-item>
        <el-form-item label="付费">
          <el-select v-model="filters.isPaid" placeholder="全部" clearable style="width:120px" @change="fetchList">
            <el-option label="免费" :value="false" />
            <el-option label="付费" :value="true" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button @click="resetFilters">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- Batch Actions -->
    <div v-if="selectedIds.length > 0" class="batch-bar">
      <span class="batch-info">已选 {{ selectedIds.length }} 项</span>
      <el-button size="small" type="success" @click="batchApprove">批量通过</el-button>
      <el-button size="small" type="danger" @click="batchReject">批量拒绝</el-button>
      <el-button size="small" type="warning" @click="openBatchPaidDialog">批量设置付费</el-button>
    </div>

    <!-- Table -->
    <el-table
      :data="list"
      v-loading="loading"
      border
      stripe
      style="width:100%"
      @selection-change="handleSelectionChange"
    >
      <el-table-column type="selection" width="44" />
      <el-table-column prop="title" label="标题" min-width="140" show-overflow-tooltip />
      <el-table-column prop="contentType" label="类型" width="70" align="center">
        <template #default="{ row }">
          <el-tag :type="row.contentType === 'COMIC' ? 'primary' : 'success'" size="small">
            {{ row.contentType === 'COMIC' ? '漫画' : '小说' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="100" align="center">
        <template #default="{ row }">
          <el-tag :type="statusTagType(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="付费" width="100" align="center">
        <template #default="{ row }">
          <template v-if="row.isPaid">
            <span v-if="row.contentType === 'NOVEL' && row.defaultChapterPrice" class="price-text">¥{{ row.defaultChapterPrice }}/章</span>
            <span v-else-if="row.price != null" class="price-text">¥{{ row.price }}</span>
            <el-tag v-else type="warning" size="small">付费</el-tag>
          </template>
          <span v-else class="text-muted">免费</span>
        </template>
      </el-table-column>
      <el-table-column prop="storylineId" label="故事线" width="80" align="center">
        <template #default="{ row }">{{ row.storylineId || '-' }}</template>
      </el-table-column>
      <el-table-column prop="createdAt" label="创建时间" width="160">
        <template #default="{ row }">{{ formatDate(row.createdAt) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="300" fixed="right" class-name="col-actions">
        <template #default="{ row }">
          <el-button size="small" link type="primary" :icon="View" @click="goReview(row.id)">审核</el-button>
          <el-button size="small" link type="primary" :icon="Edit" @click="openEditDialog(row)">编辑</el-button>
          <el-button
            v-if="row.status === 'PENDING_PUBLISH'"
            size="small" link type="success" :icon="Top"
            @click="handlePublish(row)"
          >上架</el-button>
          <el-button
            v-if="row.status === 'PUBLISHED'"
            size="small" link type="warning" :icon="Bottom"
            @click="handleUnpublish(row)"
          >下架</el-button>
          <el-button size="small" link type="info" :icon="PriceTag" @click="openPaidDialog(row)">付费</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- Pagination -->
    <el-pagination
      class="pagination"
      v-model:current-page="pagination.page"
      v-model:page-size="pagination.size"
      :total="pagination.total"
      :page-sizes="[10, 20, 50]"
      layout="total, sizes, prev, pager, next"
      @size-change="fetchList"
      @current-change="fetchList"
    />

    <!-- Edit Dialog -->
    <el-dialog v-model="editDialogVisible" title="编辑内容" width="480px" :close-on-click-modal="false">
      <el-form :model="editForm" label-width="90px">
        <el-form-item label="标题">
          <el-input v-model="editForm.title" maxlength="200" show-word-limit />
        </el-form-item>
        <el-form-item label="封面图URL">
          <el-input v-model="editForm.coverUrl" placeholder="https://..." />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="editSubmitting" @click="submitEdit">保存</el-button>
      </template>
    </el-dialog>

    <!-- Paid Dialog (single) -->
    <el-dialog v-model="paidDialogVisible" title="付费设置" width="440px" :close-on-click-modal="false">
      <el-form :model="paidForm" label-width="100px">
        <el-form-item label="付费内容">
          <el-switch v-model="paidForm.isPaid" />
        </el-form-item>
        <el-form-item v-if="paidForm.isPaid" label="作品价格">
          <el-input-number v-model="paidForm.price" :min="0.01" :precision="2" :step="1" style="width:100%" />
        </el-form-item>
        <template v-if="paidForm.isPaid && paidTargetType === 'NOVEL'">
          <el-form-item label="免费章节数">
            <el-input-number v-model="paidForm.freeChapterCount" :min="0" :step="1" style="width:100%" />
          </el-form-item>
          <el-form-item label="默认章节价格">
            <el-input-number v-model="paidForm.defaultChapterPrice" :min="0.01" :precision="2" :step="0.5" style="width:100%" />
          </el-form-item>
        </template>
      </el-form>
      <template #footer>
        <el-button @click="paidDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="paidSubmitting" @click="submitPaid">保存</el-button>
      </template>
    </el-dialog>

    <!-- Batch Paid Dialog -->
    <el-dialog v-model="batchPaidDialogVisible" title="批量付费设置" width="400px" :close-on-click-modal="false">
      <el-form :model="batchPaidForm" label-width="80px">
        <el-form-item label="付费内容">
          <el-switch v-model="batchPaidForm.isPaid" />
        </el-form-item>
        <el-form-item v-if="batchPaidForm.isPaid" label="价格">
          <el-input-number v-model="batchPaidForm.price" :min="0.01" :precision="2" :step="1" style="width:100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="batchPaidDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="batchPaidSubmitting" @click="submitBatchPaid">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  contentApi,
  type ContentItem,
  type ContentStatus,
  type ContentType,
} from '../api/content'
import { View, Edit, Top, Bottom, PriceTag } from '@element-plus/icons-vue'

const router = useRouter()

// ── State ──────────────────────────────────────────────────────────────────
const loading = ref(false)
const list = ref<ContentItem[]>([])
const selectedIds = ref<number[]>([])

const filters = reactive<{
  status: ContentStatus | ''
  contentType: ContentType | ''
  isPaid: boolean | ''
}>({
  status: '',
  contentType: '',
  isPaid: '',
})

const pagination = reactive({ page: 1, size: 20, total: 0 })

// Edit dialog
const editDialogVisible = ref(false)
const editSubmitting = ref(false)
const editingId = ref<number | null>(null)
const editForm = reactive({ title: '', coverUrl: '' })

// Paid dialog (single)
const paidDialogVisible = ref(false)
const paidSubmitting = ref(false)
const paidTargetId = ref<number | null>(null)
const paidTargetType = ref<ContentType>('COMIC')
const paidForm = reactive({ isPaid: false, price: 9.9, freeChapterCount: 0, defaultChapterPrice: 1.99 })

// Batch paid dialog
const batchPaidDialogVisible = ref(false)
const batchPaidSubmitting = ref(false)
const batchPaidForm = reactive({ isPaid: false, price: 9.9 })

// ── Helpers ────────────────────────────────────────────────────────────────
function formatDate(d: string) {
  if (!d) return '-'
  return new Date(d).toLocaleString('zh-CN', { hour12: false }).replace(/\//g, '-')
}

function statusLabel(s: ContentStatus) {
  const map: Record<ContentStatus, string> = {
    PENDING_REVIEW: '待审核',
    PENDING_PUBLISH: '待发布',
    REJECTED: '已拒绝',
    PUBLISHED: '已发布',
    OFFLINE: '已下架',
    REGENERATING: '重新生成中',
  }
  return map[s] ?? s
}

function statusTagType(s: ContentStatus) {
  const map: Record<ContentStatus, string> = {
    PENDING_REVIEW: 'info',
    PENDING_PUBLISH: 'success',
    REJECTED: 'danger',
    PUBLISHED: 'primary',
    OFFLINE: 'warning',
    REGENERATING: 'warning',
  }
  return map[s] ?? ''
}

// ── Data ───────────────────────────────────────────────────────────────────
async function fetchList() {
  loading.value = true
  try {
    const params: Record<string, unknown> = {
      page: pagination.page,
      size: pagination.size,
    }
    if (filters.status !== '') params.status = filters.status
    if (filters.contentType !== '') params.contentType = filters.contentType
    if (filters.isPaid !== '') params.isPaid = filters.isPaid

    const res = await contentApi.list(params as Parameters<typeof contentApi.list>[0])
    list.value = res.data?.records ?? []
    pagination.total = res.data?.total ?? 0
  } catch {
    // handled by interceptor
  } finally {
    loading.value = false
  }
}

function resetFilters() {
  filters.status = ''
  filters.contentType = ''
  filters.isPaid = ''
  pagination.page = 1
  fetchList()
}

function handleSelectionChange(rows: ContentItem[]) {
  selectedIds.value = rows.map(r => r.id)
}

// ── Navigation ─────────────────────────────────────────────────────────────
function goReview(id: number) {
  router.push({ name: 'ContentReview', params: { id } })
}


// ── Edit ───────────────────────────────────────────────────────────────────
function openEditDialog(row: ContentItem) {
  editingId.value = row.id
  editForm.title = row.title
  editForm.coverUrl = row.coverUrl ?? ''
  editDialogVisible.value = true
}

async function submitEdit() {
  if (editingId.value === null) return
  editSubmitting.value = true
  try {
    await contentApi.update(editingId.value, {
      title: editForm.title,
      coverUrl: editForm.coverUrl || undefined,
    })
    ElMessage.success('内容已更新')
    editDialogVisible.value = false
    fetchList()
  } catch {
    // handled
  } finally {
    editSubmitting.value = false
  }
}

// ── Publish / Unpublish ────────────────────────────────────────────────────
async function handlePublish(row: ContentItem) {
  await ElMessageBox.confirm(`确定上架「${row.title}」吗？`, '提示', { type: 'info' }).catch(() => { throw new Error('cancel') })
  try {
    await contentApi.publish(row.id)
    ElMessage.success('已上架')
    fetchList()
  } catch (e: unknown) {
    if ((e as Error).message !== 'cancel') { /* handled */ }
  }
}

async function handleUnpublish(row: ContentItem) {
  await ElMessageBox.confirm(`确定下架「${row.title}」吗？`, '提示', { type: 'warning' }).catch(() => { throw new Error('cancel') })
  try {
    await contentApi.unpublish(row.id)
    ElMessage.success('已下架')
    fetchList()
  } catch (e: unknown) {
    if ((e as Error).message !== 'cancel') { /* handled */ }
  }
}

// ── Paid (single) ──────────────────────────────────────────────────────────
function openPaidDialog(row: ContentItem) {
  paidTargetId.value = row.id
  paidTargetType.value = row.contentType
  paidForm.isPaid = row.isPaid
  paidForm.price = row.price ?? 9.9
  paidForm.freeChapterCount = 0
  paidForm.defaultChapterPrice = 1.99
  paidDialogVisible.value = true
}

async function submitPaid() {
  if (paidTargetId.value === null) return
  paidSubmitting.value = true
  try {
    await contentApi.setPaid(paidTargetId.value, {
      isPaid: paidForm.isPaid,
      price: paidForm.isPaid ? paidForm.price : undefined,
      freeChapterCount: paidForm.isPaid && paidTargetType.value === 'NOVEL' ? paidForm.freeChapterCount : undefined,
      defaultChapterPrice: paidForm.isPaid && paidTargetType.value === 'NOVEL' ? paidForm.defaultChapterPrice : undefined,
    })
    ElMessage.success('付费设置已保存')
    paidDialogVisible.value = false
    fetchList()
  } catch {
    // handled
  } finally {
    paidSubmitting.value = false
  }
}

// ── Batch Actions ──────────────────────────────────────────────────────────
async function batchApprove() {
  await ElMessageBox.confirm(`确定批量通过 ${selectedIds.value.length} 项内容吗？`, '批量审核', { type: 'info' }).catch(() => { throw new Error('cancel') })
  try {
    await contentApi.batchReview({ contentIds: selectedIds.value, action: 'approve' })
    ElMessage.success('批量通过成功')
    fetchList()
  } catch (e: unknown) {
    if ((e as Error).message !== 'cancel') { /* handled */ }
  }
}

async function batchReject() {
  await ElMessageBox.confirm(`确定批量拒绝 ${selectedIds.value.length} 项内容吗？`, '批量审核', { type: 'warning' }).catch(() => { throw new Error('cancel') })
  try {
    await contentApi.batchReview({ contentIds: selectedIds.value, action: 'reject' })
    ElMessage.success('批量拒绝成功')
    fetchList()
  } catch (e: unknown) {
    if ((e as Error).message !== 'cancel') { /* handled */ }
  }
}

function openBatchPaidDialog() {
  batchPaidForm.isPaid = false
  batchPaidForm.price = 9.9
  batchPaidDialogVisible.value = true
}

async function submitBatchPaid() {
  batchPaidSubmitting.value = true
  try {
    await contentApi.batchPaid({
      contentIds: selectedIds.value,
      isPaid: batchPaidForm.isPaid,
      price: batchPaidForm.isPaid ? batchPaidForm.price : undefined,
    })
    ElMessage.success('批量付费设置成功')
    batchPaidDialogVisible.value = false
    fetchList()
  } catch {
    // handled
  } finally {
    batchPaidSubmitting.value = false
  }
}

// ── Init ───────────────────────────────────────────────────────────────────
onMounted(fetchList)
</script>

<style scoped>
.content-manage {
  padding: 24px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.page-header h2 {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
}

.filter-card {
  margin-bottom: 16px;
}

.filter-card :deep(.el-card__body) {
  padding: 16px 20px 0;
}

.batch-bar {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
  padding: 8px 12px;
  background: #ecf5ff;
  border-radius: 4px;
}

.batch-info {
  font-size: 13px;
  color: #409eff;
  margin-right: 4px;
}

.text-muted {
  color: #909399;
  font-size: 13px;
}

.price-text {
  font-size: 13px;
  color: #e6a23c;
  font-weight: 500;
}

.pagination {
  margin-top: 16px;
  justify-content: flex-end;
}

:deep(.col-actions .cell) {
  display: flex;
  align-items: center;
  gap: 4px;
  white-space: nowrap;
}
</style>
