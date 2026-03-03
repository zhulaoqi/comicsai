<template>
  <div class="content-review" v-loading="loading">
    <!-- Back -->
    <div class="page-header">
      <el-button :icon="ArrowLeft" @click="router.back()">返回</el-button>
      <h2>内容审核详情</h2>
    </div>

    <template v-if="detail">
      <el-row :gutter="24">
        <!-- Left: Info + Actions -->
        <el-col :span="8">
          <el-card shadow="never" class="info-card">
            <!-- Cover -->
            <div class="cover-wrap">
              <el-image
                v-if="detail.coverImageUrl"
                :src="detail.coverImageUrl"
                fit="cover"
                class="cover-img"
              />
              <div v-else class="cover-placeholder">无封面</div>
            </div>

            <!-- Meta -->
            <el-descriptions :column="1" border size="small" class="meta-desc">
              <el-descriptions-item label="标题">{{ detail.title }}</el-descriptions-item>
              <el-descriptions-item label="类型">
                <el-tag :type="detail.contentType === 'COMIC' ? 'primary' : 'success'" size="small">
                  {{ detail.contentType === 'COMIC' ? '漫画' : '小说' }}
                </el-tag>
              </el-descriptions-item>
              <el-descriptions-item label="状态">
                <el-tag :type="statusTagType(detail.status)" size="small">{{ statusLabel(detail.status) }}</el-tag>
              </el-descriptions-item>
              <el-descriptions-item label="付费">
                <el-tag v-if="detail.isPaid" type="warning" size="small">付费 ¥{{ detail.price }}</el-tag>
                <span v-else>免费</span>
              </el-descriptions-item>
              <el-descriptions-item label="故事线">{{ detail.storylineTitle || '-' }}</el-descriptions-item>
              <el-descriptions-item label="创建时间">{{ formatDate(detail.createdAt) }}</el-descriptions-item>
            </el-descriptions>

            <!-- Edit Section -->
            <el-divider content-position="left">编辑信息</el-divider>
            <el-form :model="editForm" label-width="80px" size="small">
              <el-form-item label="标题">
                <el-input v-model="editForm.title" maxlength="200" />
              </el-form-item>
              <el-form-item label="封面图URL">
                <el-input v-model="editForm.coverImageUrl" placeholder="https://..." />
              </el-form-item>
              <el-form-item>
                <el-button type="primary" size="small" :loading="editSubmitting" @click="submitEdit">保存修改</el-button>
              </el-form-item>
            </el-form>

            <!-- Paid Section -->
            <el-divider content-position="left">付费设置</el-divider>
            <el-form :model="paidForm" label-width="80px" size="small">
              <el-form-item label="付费内容">
                <el-switch v-model="paidForm.isPaid" />
              </el-form-item>
              <el-form-item v-if="paidForm.isPaid" label="价格">
                <el-input-number v-model="paidForm.price" :min="0.01" :precision="2" :step="1" style="width:100%" />
              </el-form-item>
              <el-form-item>
                <el-button type="warning" size="small" :loading="paidSubmitting" @click="submitPaid">保存付费</el-button>
              </el-form-item>
            </el-form>

            <!-- Review Actions -->
            <el-divider content-position="left">审核操作</el-divider>
            <div class="review-actions">
              <template v-if="detail.status === 'PENDING_REVIEW'">
                <el-button type="success" @click="handleApprove" :loading="reviewSubmitting">通过</el-button>
                <el-button type="danger" @click="rejectDialogVisible = true">拒绝</el-button>
              </template>
              <template v-else-if="detail.status === 'APPROVED'">
                <el-button type="primary" @click="handlePublish" :loading="publishSubmitting">上架发布</el-button>
              </template>
              <template v-else-if="detail.status === 'PUBLISHED'">
                <el-button type="warning" @click="handleUnpublish" :loading="publishSubmitting">下架</el-button>
              </template>
              <el-tag v-else type="info" size="small">{{ statusLabel(detail.status) }}，无可用操作</el-tag>
            </div>
          </el-card>
        </el-col>

        <!-- Right: Content Preview -->
        <el-col :span="16">
          <el-card shadow="never">
            <template #header>
              <span>内容预览</span>
              <span class="preview-count" v-if="detail.contentType === 'COMIC'">
                共 {{ detail.comicPages?.length ?? 0 }} 页
              </span>
              <span class="preview-count" v-else>
                共 {{ detail.novelChapters?.length ?? 0 }} 章
              </span>
            </template>

            <!-- Comic Pages -->
            <template v-if="detail.contentType === 'COMIC'">
              <div v-if="!detail.comicPages?.length" class="empty-tip">暂无漫画页面</div>
              <div v-else class="comic-grid">
                <div
                  v-for="page in detail.comicPages"
                  :key="page.id"
                  class="comic-page-item"
                >
                  <el-image
                    :src="page.imageUrl"
                    fit="cover"
                    class="comic-page-img"
                    :preview-src-list="comicImageUrls"
                    :initial-index="page.pageNumber - 1"
                    lazy
                  />
                  <div class="page-num">第 {{ page.pageNumber }} 页</div>
                  <div v-if="page.dialogueText" class="page-dialogue">{{ page.dialogueText }}</div>
                </div>
              </div>
            </template>

            <!-- Novel Chapters -->
            <template v-else>
              <div v-if="!detail.novelChapters?.length" class="empty-tip">暂无章节内容</div>
              <el-collapse v-else accordion>
                <el-collapse-item
                  v-for="chapter in detail.novelChapters"
                  :key="chapter.id"
                  :title="`第 ${chapter.chapterNumber} 章：${chapter.title}`"
                  :name="chapter.id"
                >
                  <div class="chapter-content">{{ chapter.content }}</div>
                </el-collapse-item>
              </el-collapse>
            </template>
          </el-card>
        </el-col>
      </el-row>
    </template>

    <!-- Reject Dialog -->
    <el-dialog v-model="rejectDialogVisible" title="拒绝原因" width="420px" :close-on-click-modal="false">
      <el-input
        v-model="rejectReason"
        type="textarea"
        :rows="4"
        placeholder="请输入拒绝原因（可选）"
        maxlength="500"
        show-word-limit
      />
      <template #footer>
        <el-button @click="rejectDialogVisible = false">取消</el-button>
        <el-button type="danger" :loading="reviewSubmitting" @click="handleReject">确认拒绝</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft } from '@element-plus/icons-vue'
import { contentApi, type ContentDetail, type ContentStatus } from '../api/content'

const route = useRoute()
const router = useRouter()

// ── State ──────────────────────────────────────────────────────────────────
const loading = ref(false)
const detail = ref<ContentDetail | null>(null)

const editForm = reactive({ title: '', coverImageUrl: '' })
const editSubmitting = ref(false)

const paidForm = reactive({ isPaid: false, price: 9.9 })
const paidSubmitting = ref(false)

const reviewSubmitting = ref(false)
const publishSubmitting = ref(false)

const rejectDialogVisible = ref(false)
const rejectReason = ref('')

// ── Computed ───────────────────────────────────────────────────────────────
const comicImageUrls = computed(() => detail.value?.comicPages?.map(p => p.imageUrl) ?? [])

// ── Helpers ────────────────────────────────────────────────────────────────
function formatDate(d: string) {
  if (!d) return '-'
  return new Date(d).toLocaleString('zh-CN', { hour12: false }).replace(/\//g, '-')
}

function statusLabel(s: ContentStatus) {
  const map: Record<ContentStatus, string> = {
    PENDING_REVIEW: '待审核',
    APPROVED: '已通过',
    REJECTED: '已拒绝',
    PUBLISHED: '已发布',
    UNPUBLISHED: '已下架',
  }
  return map[s] ?? s
}

function statusTagType(s: ContentStatus) {
  const map: Record<ContentStatus, string> = {
    PENDING_REVIEW: 'info',
    APPROVED: 'success',
    REJECTED: 'danger',
    PUBLISHED: 'primary',
    UNPUBLISHED: 'warning',
  }
  return map[s] ?? ''
}

// ── Data ───────────────────────────────────────────────────────────────────
async function fetchDetail() {
  const id = Number(route.params.id)
  if (!id) return
  loading.value = true
  try {
    const res = await contentApi.get(id)
    detail.value = res.data
    editForm.title = res.data.title
    editForm.coverImageUrl = res.data.coverImageUrl ?? ''
    paidForm.isPaid = res.data.isPaid
    paidForm.price = res.data.price ?? 9.9
  } catch {
    // handled
  } finally {
    loading.value = false
  }
}

// ── Edit ───────────────────────────────────────────────────────────────────
async function submitEdit() {
  if (!detail.value) return
  editSubmitting.value = true
  try {
    await contentApi.update(detail.value.id, {
      title: editForm.title,
      coverImageUrl: editForm.coverImageUrl || undefined,
    })
    ElMessage.success('内容已更新')
    await fetchDetail()
  } catch {
    // handled
  } finally {
    editSubmitting.value = false
  }
}

// ── Paid ───────────────────────────────────────────────────────────────────
async function submitPaid() {
  if (!detail.value) return
  paidSubmitting.value = true
  try {
    await contentApi.setPaid(detail.value.id, {
      isPaid: paidForm.isPaid,
      price: paidForm.isPaid ? paidForm.price : undefined,
    })
    ElMessage.success('付费设置已保存')
    await fetchDetail()
  } catch {
    // handled
  } finally {
    paidSubmitting.value = false
  }
}

// ── Review ─────────────────────────────────────────────────────────────────
async function handleApprove() {
  if (!detail.value) return
  reviewSubmitting.value = true
  try {
    await contentApi.review(detail.value.id, { action: 'APPROVE' })
    ElMessage.success('已通过审核')
    await fetchDetail()
  } catch {
    // handled
  } finally {
    reviewSubmitting.value = false
  }
}

async function handleReject() {
  if (!detail.value) return
  reviewSubmitting.value = true
  try {
    await contentApi.review(detail.value.id, {
      action: 'REJECT',
      reason: rejectReason.value || undefined,
    })
    ElMessage.success('已拒绝')
    rejectDialogVisible.value = false
    rejectReason.value = ''
    await fetchDetail()
  } catch {
    // handled
  } finally {
    reviewSubmitting.value = false
  }
}

// ── Publish ────────────────────────────────────────────────────────────────
async function handlePublish() {
  if (!detail.value) return
  publishSubmitting.value = true
  try {
    await contentApi.publish(detail.value.id)
    ElMessage.success('已上架发布')
    await fetchDetail()
  } catch {
    // handled
  } finally {
    publishSubmitting.value = false
  }
}

async function handleUnpublish() {
  if (!detail.value) return
  publishSubmitting.value = true
  try {
    await contentApi.unpublish(detail.value.id)
    ElMessage.success('已下架')
    await fetchDetail()
  } catch {
    // handled
  } finally {
    publishSubmitting.value = false
  }
}

// ── Init ───────────────────────────────────────────────────────────────────
onMounted(fetchDetail)
</script>

<style scoped>
.content-review {
  padding: 24px;
}

.page-header {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 20px;
}

.page-header h2 {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
}

.info-card {
  position: sticky;
  top: 20px;
}

.cover-wrap {
  width: 100%;
  height: 200px;
  margin-bottom: 16px;
  border-radius: 6px;
  overflow: hidden;
  background: #f5f7fa;
}

.cover-img {
  width: 100%;
  height: 100%;
}

.cover-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #c0c4cc;
  font-size: 14px;
}

.meta-desc {
  margin-bottom: 8px;
}

.review-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.preview-count {
  margin-left: 8px;
  font-size: 13px;
  color: #909399;
}

.empty-tip {
  text-align: center;
  color: #909399;
  padding: 40px 0;
}

.comic-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
  gap: 12px;
}

.comic-page-item {
  border: 1px solid #ebeef5;
  border-radius: 6px;
  overflow: hidden;
}

.comic-page-img {
  width: 100%;
  height: 200px;
  display: block;
}

.page-num {
  padding: 4px 8px;
  font-size: 12px;
  color: #606266;
  background: #f5f7fa;
}

.page-dialogue {
  padding: 6px 8px;
  font-size: 12px;
  color: #303133;
  border-top: 1px solid #ebeef5;
  white-space: pre-wrap;
  max-height: 80px;
  overflow-y: auto;
}

.chapter-content {
  white-space: pre-wrap;
  line-height: 1.8;
  font-size: 14px;
  color: #303133;
  max-height: 400px;
  overflow-y: auto;
  padding: 8px 0;
}
</style>
