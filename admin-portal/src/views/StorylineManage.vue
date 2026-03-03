<template>
  <div class="storyline-manage">
    <!-- Header -->
    <div class="page-header">
      <h2>故事线管理</h2>
      <el-button type="primary" @click="openCreateDialog">
        <el-icon><Plus /></el-icon> 新建故事线
      </el-button>
    </div>

    <!-- Table -->
    <el-table :data="storylines" v-loading="loading" border stripe>
      <el-table-column prop="title" label="标题" min-width="160" />
      <el-table-column prop="genre" label="题材" width="120" />
      <el-table-column prop="contentType" label="类型" width="100">
        <template #default="{ row }">
          <el-tag :type="row.contentType === 'COMIC' ? 'primary' : 'success'" size="small">
            {{ row.contentType === 'COMIC' ? '漫画' : '小说' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="100">
        <template #default="{ row }">
          <el-switch
            :model-value="row.status === 'ACTIVE'"
            @change="(val: boolean) => handleToggleStatus(row, val)"
            :loading="togglingId === row.id"
          />
        </template>
      </el-table-column>
      <el-table-column prop="generatedCount" label="已生成" width="90" align="center" />
      <el-table-column prop="createdAt" label="创建时间" width="160">
        <template #default="{ row }">{{ formatDate(row.createdAt) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="240" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="openEditDialog(row.id)">编辑</el-button>
          <el-button size="small" type="info" @click="openConfigDialog(row.id)">生成配置</el-button>
          <el-button size="small" type="success" :loading="generatingId === row.id" @click="handleGenerate(row)">生成</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- Create / Edit Dialog -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEditing ? '编辑故事线' : '新建故事线'"
      width="640px"
      :close-on-click-modal="false"
      @closed="resetForm"
    >
      <el-form
        ref="formRef"
        :model="form"
        :rules="formRules"
        label-width="100px"
        v-loading="formLoading"
      >
        <el-form-item label="标题" prop="title">
          <el-input v-model="form.title" placeholder="请输入故事线标题" maxlength="100" show-word-limit />
        </el-form-item>
        <el-form-item label="题材类型" prop="genre">
          <el-select v-model="form.genre" placeholder="请选择题材" style="width:100%">
            <el-option v-for="g in genreOptions" :key="g" :label="g" :value="g" />
          </el-select>
        </el-form-item>
        <el-form-item label="内容类型" prop="contentType">
          <el-radio-group v-model="form.contentType">
            <el-radio value="COMIC">漫画</el-radio>
            <el-radio value="NOVEL">小说</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="角色设定" prop="characterSettings">
          <el-input
            v-model="form.characterSettings"
            type="textarea"
            :rows="3"
            placeholder="描述主要角色的性格、背景、能力等"
          />
        </el-form-item>
        <el-form-item label="世界观" prop="worldview">
          <el-input
            v-model="form.worldview"
            type="textarea"
            :rows="3"
            placeholder="描述故事发生的世界背景、规则、设定等"
          />
        </el-form-item>
        <el-form-item label="剧情大纲" prop="plotOutline">
          <el-input
            v-model="form.plotOutline"
            type="textarea"
            :rows="4"
            placeholder="描述故事的主要情节走向和关键事件"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitForm">
          {{ isEditing ? '保存' : '创建' }}
        </el-button>
      </template>
    </el-dialog>

    <!-- Generation Config Dialog -->
    <el-dialog
      v-model="configDialogVisible"
      title="生成配置"
      width="560px"
      :close-on-click-modal="false"
      @closed="resetConfig"
    >
      <el-form
        ref="configFormRef"
        :model="configForm"
        :rules="configRules"
        label-width="130px"
        v-loading="configLoading"
      >
        <el-divider content-position="left">文本生成</el-divider>
        <el-form-item label="文本 Provider" prop="textProvider">
          <el-select v-model="configForm.textProvider" placeholder="选择Provider" style="width:100%" @change="onTextProviderChange">
            <el-option label="Gemini" value="gemini" />
            <el-option label="通义千问 (Qwen)" value="qwen" />
          </el-select>
        </el-form-item>
        <el-form-item label="文本模型" prop="textModel">
          <el-select v-model="configForm.textModel" placeholder="选择模型" style="width:100%">
            <el-option v-for="m in textModelOptions" :key="m.value" :label="m.label" :value="m.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="Temperature" prop="textTemperature">
          <el-slider
            v-model="configForm.textTemperature"
            :min="0" :max="2" :step="0.1"
            show-input
            style="width:100%"
          />
        </el-form-item>

        <el-divider content-position="left">图像生成</el-divider>
        <el-form-item label="图像 Provider" prop="imageProvider">
          <el-select v-model="configForm.imageProvider" placeholder="选择Provider" style="width:100%" @change="onImageProviderChange">
            <el-option label="通义万象 (Wanxiang)" value="wanxiang" />
          </el-select>
        </el-form-item>
        <el-form-item label="图像模型" prop="imageModel">
          <el-select v-model="configForm.imageModel" placeholder="选择模型" style="width:100%">
            <el-option v-for="m in imageModelOptions" :key="m.value" :label="m.label" :value="m.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="图像尺寸" prop="imageSize">
          <el-select v-model="configForm.imageSize" placeholder="选择尺寸" style="width:100%">
            <el-option v-for="s in imageSizeOptions" :key="s" :label="s" :value="s" />
          </el-select>
        </el-form-item>

        <el-divider content-position="left">生成策略</el-divider>
        <el-form-item label="每次生成章节数" prop="chaptersPerGeneration">
          <el-input-number
            v-model="configForm.chaptersPerGeneration"
            :min="1" :max="10"
            style="width:100%"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="configDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="configSubmitting" @click="submitConfig">保存配置</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { storylineApi, type Storyline, type StorylineForm, type GenerationConfig } from '../api/storyline'

// ── State ──────────────────────────────────────────────────────────────────
const loading = ref(false)
const storylines = ref<Storyline[]>([])
const togglingId = ref<number | null>(null)
const generatingId = ref<number | null>(null)

// Storyline form dialog
const dialogVisible = ref(false)
const isEditing = ref(false)
const editingId = ref<number | null>(null)
const formLoading = ref(false)
const submitting = ref(false)
const formRef = ref<FormInstance>()

const form = reactive<StorylineForm>({
  title: '',
  genre: '',
  contentType: 'COMIC',
  characterSettings: '',
  worldview: '',
  plotOutline: '',
})

// Generation config dialog
const configDialogVisible = ref(false)
const configStorylineId = ref<number | null>(null)
const configLoading = ref(false)
const configSubmitting = ref(false)
const configFormRef = ref<FormInstance>()

const configForm = reactive<GenerationConfig>({
  textProvider: 'gemini',
  textModel: 'gemini-2.0-flash',
  imageProvider: 'wanxiang',
  imageModel: 'wanx-v1',
  textTemperature: 0.8,
  imageSize: '1024x1024',
  chaptersPerGeneration: 1,
})

// ── Options ────────────────────────────────────────────────────────────────
const genreOptions = ['奇幻', '科幻', '武侠', '都市', '历史', '悬疑', '言情', '恐怖', '其他']

const textModels: Record<string, { label: string; value: string }[]> = {
  gemini: [
    { label: 'Gemini 2.0 Flash', value: 'gemini-2.0-flash' },
    { label: 'Gemini 1.5 Pro', value: 'gemini-1.5-pro' },
    { label: 'Gemini 1.5 Flash', value: 'gemini-1.5-flash' },
  ],
  qwen: [
    { label: 'Qwen-Max', value: 'qwen-max' },
    { label: 'Qwen-Plus', value: 'qwen-plus' },
    { label: 'Qwen-Turbo', value: 'qwen-turbo' },
  ],
}

const imageModels: Record<string, { label: string; value: string }[]> = {
  wanxiang: [
    { label: 'Wanx v1', value: 'wanx-v1' },
    { label: 'Wanx Lite', value: 'wanx-lite' },
  ],
}

const imageSizes = {
  wanxiang: ['1024x1024', '720x1280', '1280x720'],
}

const textModelOptions = computed(() => textModels[configForm.textProvider] ?? [])
const imageModelOptions = computed(() => imageModels[configForm.imageProvider] ?? [])
const imageSizeOptions = computed(() => imageSizes[configForm.imageProvider as keyof typeof imageSizes] ?? ['1024x1024'])

// ── Validation rules ───────────────────────────────────────────────────────
const formRules: FormRules = {
  title: [{ required: true, message: '请输入故事线标题', trigger: 'blur' }],
  genre: [{ required: true, message: '请选择题材类型', trigger: 'change' }],
  contentType: [{ required: true, message: '请选择内容类型', trigger: 'change' }],
}

const configRules: FormRules = {
  textProvider: [{ required: true, message: '请选择文本Provider', trigger: 'change' }],
  textModel: [{ required: true, message: '请选择文本模型', trigger: 'change' }],
  imageProvider: [{ required: true, message: '请选择图像Provider', trigger: 'change' }],
  imageModel: [{ required: true, message: '请选择图像模型', trigger: 'change' }],
  imageSize: [{ required: true, message: '请选择图像尺寸', trigger: 'change' }],
  chaptersPerGeneration: [{ required: true, message: '请设置每次生成章节数', trigger: 'change' }],
}

// ── Helpers ────────────────────────────────────────────────────────────────
function formatDate(dateStr: string): string {
  if (!dateStr) return '-'
  return new Date(dateStr).toLocaleString('zh-CN', { hour12: false }).replace(/\//g, '-')
}

function onTextProviderChange() {
  const models = textModels[configForm.textProvider]
  configForm.textModel = models?.[0]?.value ?? ''
}

function onImageProviderChange() {
  const models = imageModels[configForm.imageProvider]
  configForm.imageModel = models?.[0]?.value ?? ''
  const sizes = imageSizes[configForm.imageProvider as keyof typeof imageSizes]
  configForm.imageSize = sizes?.[0] ?? '1024x1024'
}

// ── Data loading ───────────────────────────────────────────────────────────
async function fetchStorylines() {
  loading.value = true
  try {
    const res = await storylineApi.list()
    const data = res.data as any
    storylines.value = Array.isArray(data) ? data : (data?.records ?? [])
  } catch {
    ElMessage.error('加载故事线列表失败')
  } finally {
    loading.value = false
  }
}

// ── Storyline CRUD ─────────────────────────────────────────────────────────
function openCreateDialog() {
  isEditing.value = false
  editingId.value = null
  dialogVisible.value = true
}

async function openEditDialog(id: number) {
  isEditing.value = true
  editingId.value = id
  dialogVisible.value = true
  formLoading.value = true
  try {
    const res = await storylineApi.get(id)
    const d = res.data
    form.title = d.title
    form.genre = d.genre
    form.contentType = d.contentType
    form.characterSettings = d.characterSettings ?? ''
    form.worldview = d.worldview ?? ''
    form.plotOutline = d.plotOutline ?? ''
  } catch {
    ElMessage.error('加载故事线详情失败')
    dialogVisible.value = false
  } finally {
    formLoading.value = false
  }
}

function resetForm() {
  form.title = ''
  form.genre = ''
  form.contentType = 'COMIC'
  form.characterSettings = ''
  form.worldview = ''
  form.plotOutline = ''
  formRef.value?.clearValidate()
}

async function submitForm() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    if (isEditing.value && editingId.value !== null) {
      await storylineApi.update(editingId.value, { ...form })
      ElMessage.success('故事线已更新')
    } else {
      await storylineApi.create({ ...form })
      ElMessage.success('故事线已创建')
    }
    dialogVisible.value = false
    fetchStorylines()
  } catch {
    // error handled by interceptor
  } finally {
    submitting.value = false
  }
}

async function handleToggleStatus(row: Storyline, active: boolean) {
  const newStatus = active ? 'ENABLED' : 'DISABLED'
  const label = active ? '启用' : '停用'
  try {
    await ElMessageBox.confirm(`确定要${label}「${row.title}」吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    })
  } catch {
    return // user cancelled
  }

  togglingId.value = row.id
  try {
    await storylineApi.toggleStatus(row.id, newStatus)
    row.status = active ? 'ACTIVE' : 'INACTIVE'
    ElMessage.success(`已${label}`)
  } catch {
    // error handled by interceptor
  } finally {
    togglingId.value = null
  }
}

// ── Manual Generate ────────────────────────────────────────────────────────
async function handleGenerate(row: Storyline) {
  if (row.status !== 'ACTIVE') {
    ElMessage.warning('请先启用该故事线再生成内容')
    return
  }
  generatingId.value = row.id
  try {
    const res = await storylineApi.generate(row.id)
    if (res.code !== 200) {
      ElMessage.error(res.message || '生成失败')
    } else {
      ElMessage.success('生成成功，内容已进入审核队列')
      fetchStorylines()
    }
  } catch {
    // error handled by interceptor
  } finally {
    generatingId.value = null
  }
}

// ── Generation Config ──────────────────────────────────────────────────────
async function openConfigDialog(id: number) {
  configStorylineId.value = id
  configDialogVisible.value = true
  configLoading.value = true
  try {
    const res = await storylineApi.getGenerationConfig(id)
    if (res.data) {
      Object.assign(configForm, res.data)
    }
  } catch {
    // no config yet — use defaults
  } finally {
    configLoading.value = false
  }
}

function resetConfig() {
  configStorylineId.value = null
  configFormRef.value?.clearValidate()
}

async function submitConfig() {
  const valid = await configFormRef.value?.validate().catch(() => false)
  if (!valid) return

  if (configStorylineId.value === null) return
  configSubmitting.value = true
  try {
    await storylineApi.saveGenerationConfig(configStorylineId.value, { ...configForm })
    ElMessage.success('生成配置已保存')
    configDialogVisible.value = false
  } catch {
    // error handled by interceptor
  } finally {
    configSubmitting.value = false
  }
}

// ── Init ───────────────────────────────────────────────────────────────────
onMounted(fetchStorylines)
</script>

<style scoped>
.storyline-manage {
  padding: 24px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.page-header h2 {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
}
</style>
