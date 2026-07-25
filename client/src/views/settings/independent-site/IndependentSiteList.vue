<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import type { FormInstance, FormItemRule, FormRules } from 'element-plus'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useI18n } from 'vue-i18n'
import {
  CopyDocument,
  CreditCard,
  Delete,
  Plus,
  Promotion,
  Refresh,
  Setting,
  TopRight,
} from '@element-plus/icons-vue'
import {
  createIndependentSite,
  deleteIndependentSite,
  generateIndependentSitePageDraftForPage,
  listIndependentSites,
  updateIndependentSitePage,
} from '@/api/independentSite'
import { useStoreStore } from '@/stores/store'
import type { IndependentSiteDetail, IndependentSiteSummary } from '@/types/independentSite'
import StripeSettingsDialog from '@/views/settings/independent-site/StripeSettingsDialog.vue'
import { normalizeCanvasSchema } from '@/views/independent-site/canvasSchema'
import { CANVAS_STYLE_PRESETS } from '@/views/independent-site/canvasStylePresets'
import {
  INDEPENDENT_SITE_THEME_LABELS,
  normalizeIndependentSiteThemeKey,
} from '@/views/independent-site/themes'

interface CreateSiteForm {
  name: string
  slug: string
  // 可选风格描述：创建成功后作为首页 AI 初稿的 prompt
  styleDescription: string
  // 可选风格预设卡 id（canvasStylePresets），与风格描述二选一优先描述
  stylePresetId: string
}

const router = useRouter()
const storeStore = useStoreStore()
const { locale } = useI18n()
const createFormRef = ref<FormInstance>()
const loading = ref(true)
// 创建流程分两阶段：创建站点 → 生成首页 AI 初稿（按钮文案随阶段变化）
const creatingPhase = ref<'idle' | 'creating' | 'generating'>('idle')
const creating = computed(() => creatingPhase.value !== 'idle')
const deletingSiteId = ref<number | null>(null)
const createDialogVisible = ref(false)
const stripeDialogVisible = ref(false)
const loadError = ref('')
const sites = ref<IndependentSiteSummary[]>([])
let loadSequence = 0

const createForm = reactive<CreateSiteForm>({
  name: '',
  slug: '',
  styleDescription: '',
  stylePresetId: '',
})

const slugValidator: FormItemRule['validator'] = (_rule, value, callback) => {
  const slug = String(value || '').trim()
  if (!slug) {
    callback(new Error('请输入公开链接后缀'))
    return
  }
  if (slug.length < 3 || slug.length > 63) {
    callback(new Error('链接后缀需为 3–63 个字符'))
    return
  }
  if (!/^[a-z0-9]+(?:-[a-z0-9]+)*$/.test(slug)) {
    callback(new Error('仅支持小写字母、数字和单个连字符，不能以连字符开头或结尾'))
    return
  }
  callback()
}

const createRules: FormRules = {
  name: [
    { required: true, message: '请输入站点名称', trigger: 'blur' },
    { min: 1, max: 120, message: '站点名称需为 1–120 个字符', trigger: 'blur' },
  ],
  slug: [{ required: true, validator: slugValidator, trigger: ['blur', 'change'] }],
  styleDescription: [{ max: 500, message: '风格描述不能超过 500 个字符', trigger: 'blur' }],
}

const getErrorMessage = (error: unknown, fallback: string) => {
  if (error && typeof error === 'object') {
    const responseMessage = (error as { response?: { data?: { message?: unknown } } }).response
      ?.data?.message
    if (typeof responseMessage === 'string' && responseMessage.trim()) {
      return responseMessage
    }
  }
  if (error instanceof Error && error.message) {
    return error.message
  }
  return fallback
}

const themeLabel = (themeKey: string) =>
  INDEPENDENT_SITE_THEME_LABELS[normalizeIndependentSiteThemeKey(themeKey)]

const sitePublicUrl = (slug: string) => {
  const origin = typeof window === 'undefined' ? '' : window.location.origin
  return `${origin}/stay/${slug}`
}

const loadSites = async () => {
  const sequence = ++loadSequence
  loading.value = true
  loadError.value = ''
  try {
    const response = await listIndependentSites()
    if (sequence !== loadSequence) {
      return
    }
    if (!response.success) {
      throw new Error(response.message || '加载独立站列表失败')
    }
    sites.value = Array.isArray(response.data) ? response.data : []
  } catch (error) {
    if (sequence !== loadSequence) {
      return
    }
    sites.value = []
    loadError.value = getErrorMessage(error, '加载独立站列表失败')
  } finally {
    if (sequence === loadSequence) {
      loading.value = false
    }
  }
}

const openCreateDialog = () => {
  createForm.name = ''
  createForm.slug = ''
  createForm.styleDescription = ''
  createForm.stylePresetId = ''
  createDialogVisible.value = true
}

const toggleStylePreset = (presetId: string) => {
  createForm.stylePresetId = createForm.stylePresetId === presetId ? '' : presetId
}

// 创建成功后按风格描述/预设为首页生成一次 AI 初稿并保存为草稿；
// AI 失败不阻断：保留默认骨架，toast 提示可在编辑器中重试
const generateHomePageDraft = async (site: IndependentSiteDetail) => {
  const description = createForm.styleDescription.trim()
  const preset = CANVAS_STYLE_PRESETS.find((item) => item.id === createForm.stylePresetId)
  const prompt = description || preset?.prompt || ''
  const homePage = Array.isArray(site.pages)
    ? site.pages.find((page) => page.type === 'HOME')
    : undefined
  if (!prompt || !homePage) {
    ElMessage.success('独立站已创建，可在画布编辑器中用 AI 生成首页')
    return
  }
  creatingPhase.value = 'generating'
  try {
    const response = await generateIndependentSitePageDraftForPage(site.id, homePage.id, {
      prompt,
      language: locale.value,
    })
    if (!response.success || !response.data) {
      throw new Error(response.message || 'AI 初稿生成失败')
    }
    if (!response.data.publishable) {
      throw new Error('AI 返回的初稿不可发布')
    }
    const rawSchema = (response.data as { pageSchema?: unknown }).pageSchema ?? response.data
    const canvasSchema = normalizeCanvasSchema(rawSchema)
    if (!canvasSchema) {
      throw new Error('AI 返回的初稿不符合画布契约')
    }
    const saveResponse = await updateIndependentSitePage(site.id, homePage.id, {
      draftSchema: canvasSchema,
    })
    if (!saveResponse.success) {
      throw new Error(saveResponse.message || 'AI 初稿保存失败')
    }
    ElMessage.success('独立站已创建，AI 首页初稿已生成并保存为草稿')
  } catch (error) {
    console.warn('[independent-site] 首页 AI 初稿生成失败', error)
    ElMessage.warning('已创建站点，AI 初稿生成失败可在编辑器中重试')
  }
}

const handleCreateSite = async () => {
  const valid = await createFormRef.value?.validate().catch(() => false)
  if (!valid) {
    return
  }

  creatingPhase.value = 'creating'
  try {
    const response = await createIndependentSite({
      name: createForm.name.trim(),
      slug: createForm.slug.trim(),
    })
    if (!response.success || !response.data) {
      throw new Error(response.message || '创建独立站失败')
    }
    const newSite = response.data
    await generateHomePageDraft(newSite)
    createDialogVisible.value = false
    const newSiteId = Number(newSite.id)
    if (Number.isInteger(newSiteId) && newSiteId > 0) {
      await router.push({ name: 'IndependentSiteDetail', params: { id: newSiteId } })
    } else {
      await loadSites()
    }
  } catch (error) {
    ElMessage.error(getErrorMessage(error, '创建独立站失败'))
  } finally {
    creatingPhase.value = 'idle'
  }
}

const handleManageSite = (site: IndependentSiteSummary) => {
  void router.push({ name: 'IndependentSiteDetail', params: { id: site.id } })
}

const handleDeleteSite = async (site: IndependentSiteSummary) => {
  try {
    await ElMessageBox.confirm(
      `确定删除站点「${site.name || site.slug}」吗？该站点的页面与发布范围会一并删除，此操作不可恢复；存在支付记录时服务端会拒绝删除。`,
      '删除独立站',
      {
        confirmButtonText: '删除',
        cancelButtonText: '取消',
        type: 'warning',
        confirmButtonClass: 'el-button--danger',
      },
    )
  } catch {
    return
  }

  deletingSiteId.value = site.id
  try {
    const response = await deleteIndependentSite(site.id)
    if (!response.success) {
      throw new Error(response.message || '删除独立站失败')
    }
    ElMessage.success('独立站已删除')
    sites.value = sites.value.filter((item) => item.id !== site.id)
  } catch (error) {
    // 409（存在支付记录等）与其他错误都直接展示后端 message
    ElMessage.error(getErrorMessage(error, '删除独立站失败'))
  } finally {
    deletingSiteId.value = null
  }
}

const copyPublicUrl = async (site: IndependentSiteSummary) => {
  try {
    await navigator.clipboard.writeText(sitePublicUrl(site.slug))
    ElMessage.success('公开链接已复制')
  } catch {
    ElMessage.error('复制失败，请手动复制链接')
  }
}

const openPublicSite = (site: IndependentSiteSummary) => {
  window.open(sitePublicUrl(site.slug), '_blank', 'noopener,noreferrer')
}

onMounted(loadSites)

watch(
  () => storeStore.currentStore?.id,
  (currentStoreId, previousStoreId) => {
    if (previousStoreId && currentStoreId && currentStoreId !== previousStoreId) {
      loadSites()
    } else if (previousStoreId && !currentStoreId) {
      loadSequence += 1
      sites.value = []
      loading.value = false
    }
  },
)
</script>

<template>
  <div v-loading="loading" class="independent-site-list">
    <header class="page-header">
      <div>
        <div class="header-eyebrow">
          <el-icon><Promotion /></el-icon>
          DIRECT BOOKING
        </div>
        <h1>独立站</h1>
        <p>一个门店可以拥有多个公开订房站点，各自使用 PMS 的实时库存、价格和订单流程。</p>
      </div>
      <div class="header-actions">
        <el-button :icon="CreditCard" @click="stripeDialogVisible = true">Stripe 设置</el-button>
        <el-button
          type="primary"
          :icon="Plus"
          :disabled="Boolean(loadError)"
          @click="openCreateDialog"
        >
          新建站点
        </el-button>
      </div>
    </header>

    <el-alert
      v-if="loadError"
      class="page-alert"
      type="error"
      :title="loadError"
      description="站点列表未加载，页面不会显示虚假的删除或创建结果。请重试。"
      show-icon
      :closable="false"
    >
      <template #default>
        <el-button class="alert-action" size="small" :icon="Refresh" @click="loadSites">
          重新加载
        </el-button>
      </template>
    </el-alert>

    <el-empty
      v-if="!loading && !loadError && sites.length === 0"
      class="empty-sites"
      description="当前门店还没有独立站，先创建一个站点再配置页面与发布范围。"
    >
      <el-button type="primary" :icon="Plus" @click="openCreateDialog">新建站点</el-button>
    </el-empty>

    <div v-if="sites.length > 0" class="table-card">
      <el-table :data="sites" class="site-table" row-key="id">
        <el-table-column label="站点名称" min-width="180">
          <template #default="{ row }">
            <div class="site-name-cell">
              <span class="site-name">{{ row.name }}</span>
              <el-tag v-if="row.isDefault" size="small" effect="plain">默认</el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="公开链接" min-width="240">
          <template #default="{ row }">
            <div class="slug-cell">
              <span class="slug-text">/stay/{{ row.slug }}</span>
              <el-button
                link
                size="small"
                :icon="CopyDocument"
                aria-label="复制公开链接"
                @click="copyPublicUrl(row)"
              />
              <el-button
                link
                size="small"
                :icon="TopRight"
                aria-label="打开公开链接"
                @click="openPublicSite(row)"
              />
            </div>
          </template>
        </el-table-column>
        <el-table-column label="启用状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.enabled ? 'success' : 'info'" effect="plain">
              {{ row.enabled ? '已启用' : '未启用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="页面数" width="90" align="center">
          <template #default="{ row }">{{ row.pageCount }}</template>
        </el-table-column>
        <el-table-column label="发布范围" width="90" align="center">
          <template #default="{ row }">{{ row.publicationCount }} 个房型</template>
        </el-table-column>
        <el-table-column label="主题" width="110">
          <template #default="{ row }">{{ themeLabel(row.themeKey) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="170" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" :icon="Setting" @click="handleManageSite(row)">
              管理
            </el-button>
            <el-button
              link
              type="danger"
              :icon="Delete"
              :loading="deletingSiteId === row.id"
              @click="handleDeleteSite(row)"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog
      v-model="createDialogVisible"
      title="新建独立站"
      width="560px"
      :close-on-click-modal="!creating"
    >
      <el-form ref="createFormRef" :model="createForm" :rules="createRules" label-position="top">
        <el-form-item label="站点名称" prop="name">
          <el-input
            v-model.trim="createForm.name"
            maxlength="120"
            show-word-limit
            autocomplete="off"
            placeholder="例如：海边民宿二店"
            :disabled="creating"
          />
        </el-form-item>
        <el-form-item label="公开链接后缀" prop="slug">
          <el-input
            v-model.trim="createForm.slug"
            maxlength="63"
            show-word-limit
            autocomplete="off"
            placeholder="例如：seaside-annex"
            :disabled="creating"
          >
            <template #prepend>/stay/</template>
          </el-input>
        </el-form-item>
        <el-form-item label="风格描述（可选）" prop="styleDescription">
          <el-input
            v-model="createForm.styleDescription"
            type="textarea"
            :rows="3"
            maxlength="500"
            show-word-limit
            resize="vertical"
            placeholder="例如：我们是一家位于莫干山的日式温泉民宿，8 间房，主打亲子和私汤"
            :disabled="creating"
          />
        </el-form-item>
        <el-form-item label="风格预设（可选）">
          <div class="style-preset-grid" role="radiogroup" aria-label="选择风格预设">
            <button
              v-for="preset in CANVAS_STYLE_PRESETS"
              :key="preset.id"
              type="button"
              class="style-preset-card"
              :class="{ 'is-active': createForm.stylePresetId === preset.id }"
              :disabled="creating"
              @click="toggleStylePreset(preset.id)"
            >
              <span class="style-preset-name">{{ preset.name }}</span>
              <span class="style-preset-desc">{{ preset.description }}</span>
            </button>
          </div>
          <p class="style-preset-help">
            填写风格描述或选择预设卡后，创建站点时会自动生成首页 AI 初稿；都不填则使用默认骨架。
          </p>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button :disabled="creating" @click="createDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="creating" @click="handleCreateSite">
          {{ creating ? (creatingPhase === 'generating' ? '生成初稿中…' : '创建中…') : '创建站点' }}
        </el-button>
      </template>
    </el-dialog>

    <StripeSettingsDialog v-model="stripeDialogVisible" />
  </div>
</template>

<style scoped>
.independent-site-list {
  min-height: 100%;
  padding: 8px 20px 40px;
  color: #202322;
  background: radial-gradient(circle at 92% 0%, rgba(49, 117, 103, 0.08), transparent 26%), #f7f8f7;
}

.page-header {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 24px;
  width: min(1180px, 100%);
  margin: 0 auto;
  padding: 24px 0;
}

.page-header h1 {
  margin: 6px 0;
  color: #173c36;
  font-size: 34px;
  letter-spacing: -0.03em;
}

.page-header p {
  margin: 0;
  color: #69716f;
  font-size: 14px;
}

.header-eyebrow {
  display: flex;
  gap: 7px;
  align-items: center;
  color: #357d70;
  font-size: 11px;
  font-weight: 800;
  letter-spacing: 0.14em;
}

.header-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  align-items: center;
  justify-content: flex-end;
}

.page-alert,
.table-card,
.empty-sites {
  width: min(1180px, 100%);
  margin-right: auto;
  margin-left: auto;
}

.page-alert {
  margin-bottom: 16px;
}

.alert-action {
  margin-top: 10px;
}

.empty-sites {
  margin-top: 40px;
  margin-bottom: 40px;
}

.table-card {
  padding: 12px 20px 20px;
  border: 1px solid #e5e9e7;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.94);
  box-shadow: 0 12px 40px rgba(36, 59, 54, 0.045);
}

.site-name-cell,
.slug-cell {
  display: flex;
  gap: 8px;
  align-items: center;
}

.site-name {
  font-weight: 700;
}

.slug-text {
  color: #357d70;
  font-family: 'SFMono-Regular', Consolas, monospace;
  font-size: 12px;
}

.style-preset-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
  width: 100%;
}

.style-preset-card {
  display: grid;
  gap: 3px;
  padding: 9px 12px;
  border: 1px solid #dce5e2;
  border-radius: 10px;
  background: #fff;
  cursor: pointer;
  text-align: left;
  transition:
    border-color 0.15s,
    box-shadow 0.15s;
}

.style-preset-card:hover:not(:disabled) {
  border-color: #357d70;
}

.style-preset-card.is-active {
  border-color: #357d70;
  box-shadow: 0 0 0 1px #357d70 inset;
  background: rgba(53, 125, 112, 0.06);
}

.style-preset-card:disabled {
  cursor: not-allowed;
  opacity: 0.6;
}

.style-preset-name {
  color: #173c36;
  font-size: 13px;
  font-weight: 700;
}

.style-preset-desc {
  color: #69716f;
  font-size: 11px;
  line-height: 1.4;
}

.style-preset-help {
  margin: 8px 0 0;
  color: #929896;
  font-size: 12px;
  line-height: 1.5;
}
</style>
