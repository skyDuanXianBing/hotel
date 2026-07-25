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
import { getCanvasStylePresets } from '@/views/independent-site/canvasStylePresets'
import {
  getIndependentSiteThemeLabel,
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
const { locale, t } = useI18n()
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

const stylePresets = computed(() => getCanvasStylePresets(t))

const slugValidator: FormItemRule['validator'] = (_rule, value, callback) => {
  const slug = String(value || '').trim()
  if (!slug) {
    callback(new Error(t('independentSite.list.validation.slugRequired')))
    return
  }
  if (slug.length < 3 || slug.length > 63) {
    callback(new Error(t('independentSite.list.validation.slugLength')))
    return
  }
  if (!/^[a-z0-9]+(?:-[a-z0-9]+)*$/.test(slug)) {
    callback(new Error(t('independentSite.list.validation.slugFormat')))
    return
  }
  callback()
}

const createRules: FormRules = {
  name: [
    { required: true, message: t('independentSite.list.validation.nameRequired'), trigger: 'blur' },
    { min: 1, max: 120, message: t('independentSite.list.validation.nameLength'), trigger: 'blur' },
  ],
  slug: [{ required: true, validator: slugValidator, trigger: ['blur', 'change'] }],
  styleDescription: [
    { max: 500, message: t('independentSite.list.validation.styleLength'), trigger: 'blur' },
  ],
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
  getIndependentSiteThemeLabel(t, normalizeIndependentSiteThemeKey(themeKey))

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
      throw new Error(response.message || t('independentSite.list.messages.loadFailed'))
    }
    sites.value = Array.isArray(response.data) ? response.data : []
  } catch (error) {
    if (sequence !== loadSequence) {
      return
    }
    sites.value = []
    loadError.value = getErrorMessage(error, t('independentSite.list.messages.loadFailed'))
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
  const preset = stylePresets.value.find((item) => item.id === createForm.stylePresetId)
  const prompt = description || preset?.prompt || ''
  const homePage = Array.isArray(site.pages)
    ? site.pages.find((page) => page.type === 'HOME')
    : undefined
  if (!prompt || !homePage) {
    ElMessage.success(t('independentSite.list.messages.created'))
    return
  }
  creatingPhase.value = 'generating'
  try {
    const response = await generateIndependentSitePageDraftForPage(site.id, homePage.id, {
      prompt,
      language: locale.value,
    })
    if (!response.success || !response.data) {
      throw new Error(response.message || t('independentSite.list.messages.aiDraftFailed'))
    }
    if (!response.data.publishable) {
      throw new Error(t('independentSite.list.messages.aiDraftUnpublishable'))
    }
    const rawSchema = (response.data as { pageSchema?: unknown }).pageSchema ?? response.data
    const canvasSchema = normalizeCanvasSchema(rawSchema)
    if (!canvasSchema) {
      throw new Error(t('independentSite.list.messages.aiDraftInvalid'))
    }
    const saveResponse = await updateIndependentSitePage(site.id, homePage.id, {
      draftSchema: canvasSchema,
    })
    if (!saveResponse.success) {
      throw new Error(saveResponse.message || t('independentSite.list.messages.aiDraftSaveFailed'))
    }
    ElMessage.success(t('independentSite.list.messages.draftGenerated'))
  } catch (error) {
    console.warn('[independent-site] 首页 AI 初稿生成失败', error)
    ElMessage.warning(t('independentSite.list.messages.draftGenerationFailed'))
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
      throw new Error(response.message || t('independentSite.list.messages.createFailed'))
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
    ElMessage.error(getErrorMessage(error, t('independentSite.list.messages.createFailed')))
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
      t('independentSite.list.messages.deleteConfirm', { name: site.name || site.slug }),
      t('independentSite.list.messages.deleteTitle'),
      {
        confirmButtonText: t('independentSite.common.delete'),
        cancelButtonText: t('independentSite.common.cancel'),
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
      throw new Error(response.message || t('independentSite.list.messages.deleteFailed'))
    }
    ElMessage.success(t('independentSite.list.messages.deleted'))
    sites.value = sites.value.filter((item) => item.id !== site.id)
  } catch (error) {
    // 409（存在支付记录等）与其他错误都直接展示后端 message
    ElMessage.error(getErrorMessage(error, t('independentSite.list.messages.deleteFailed')))
  } finally {
    deletingSiteId.value = null
  }
}

const copyPublicUrl = async (site: IndependentSiteSummary) => {
  try {
    await navigator.clipboard.writeText(sitePublicUrl(site.slug))
    ElMessage.success(t('independentSite.list.messages.copied'))
  } catch {
    ElMessage.error(t('independentSite.list.messages.copyFailed'))
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
          {{ t('independentSite.list.directBooking') }}
        </div>
        <h1>{{ t('independentSite.list.title') }}</h1>
        <p>{{ t('independentSite.list.description') }}</p>
      </div>
      <div class="header-actions">
        <el-button :icon="CreditCard" @click="stripeDialogVisible = true">
          {{ t('independentSite.list.stripeSettings') }}
        </el-button>
        <el-button
          type="primary"
          :icon="Plus"
          :disabled="Boolean(loadError)"
          @click="openCreateDialog"
        >
          {{ t('independentSite.list.newSite') }}
        </el-button>
      </div>
    </header>

    <el-alert
      v-if="loadError"
      class="page-alert"
      type="error"
      :title="loadError"
      :description="t('independentSite.list.loadErrorDescription')"
      show-icon
      :closable="false"
    >
      <template #default>
        <el-button class="alert-action" size="small" :icon="Refresh" @click="loadSites">
          {{ t('independentSite.common.reload') }}
        </el-button>
      </template>
    </el-alert>

    <el-empty
      v-if="!loading && !loadError && sites.length === 0"
      class="empty-sites"
      :description="t('independentSite.list.emptyDescription')"
    >
      <el-button type="primary" :icon="Plus" @click="openCreateDialog">
        {{ t('independentSite.list.newSite') }}
      </el-button>
    </el-empty>

    <div v-if="sites.length > 0" class="table-card">
      <el-table :data="sites" class="site-table" row-key="id">
        <el-table-column :label="t('independentSite.list.name')" min-width="180">
          <template #default="{ row }">
            <div class="site-name-cell">
              <span class="site-name">{{ row.name }}</span>
              <el-tag v-if="row.isDefault" size="small" effect="plain">
                {{ t('independentSite.common.default') }}
              </el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column :label="t('independentSite.list.publicLink')" min-width="240">
          <template #default="{ row }">
            <div class="slug-cell">
              <span class="slug-text">/stay/{{ row.slug }}</span>
              <el-button
                link
                size="small"
                :icon="CopyDocument"
                :aria-label="t('independentSite.list.copyPublicLink')"
                @click="copyPublicUrl(row)"
              />
              <el-button
                link
                size="small"
                :icon="TopRight"
                :aria-label="t('independentSite.list.openPublicLink')"
                @click="openPublicSite(row)"
              />
            </div>
          </template>
        </el-table-column>
        <el-table-column :label="t('independentSite.list.status')" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.enabled ? 'success' : 'info'" effect="plain">
              {{ row.enabled ? t('independentSite.common.enabled') : t('independentSite.common.disabled') }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="t('independentSite.list.pageCount')" width="90" align="center">
          <template #default="{ row }">{{ row.pageCount }}</template>
        </el-table-column>
        <el-table-column :label="t('independentSite.list.publicationScope')" width="90" align="center">
          <template #default="{ row }">
            {{ t('independentSite.list.roomTypeCount', { count: row.publicationCount }) }}
          </template>
        </el-table-column>
        <el-table-column :label="t('independentSite.list.theme')" width="110">
          <template #default="{ row }">{{ themeLabel(row.themeKey) }}</template>
        </el-table-column>
        <el-table-column :label="t('independentSite.list.actions')" width="170" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" :icon="Setting" @click="handleManageSite(row)">
              {{ t('independentSite.list.manage') }}
            </el-button>
            <el-button
              link
              type="danger"
              :icon="Delete"
              :loading="deletingSiteId === row.id"
              @click="handleDeleteSite(row)"
            >
              {{ t('independentSite.common.delete') }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog
      v-model="createDialogVisible"
      :title="t('independentSite.list.createTitle')"
      width="560px"
      :close-on-click-modal="!creating"
    >
      <el-form ref="createFormRef" :model="createForm" :rules="createRules" label-position="top">
        <el-form-item :label="t('independentSite.list.name')" prop="name">
          <el-input
            v-model.trim="createForm.name"
            maxlength="120"
            show-word-limit
            autocomplete="off"
            :placeholder="t('independentSite.list.namePlaceholder')"
            :disabled="creating"
          />
        </el-form-item>
        <el-form-item :label="t('independentSite.list.slug')" prop="slug">
          <el-input
            v-model.trim="createForm.slug"
            maxlength="63"
            show-word-limit
            autocomplete="off"
            :placeholder="t('independentSite.list.slugPlaceholder')"
            :disabled="creating"
          >
            <template #prepend>/stay/</template>
          </el-input>
        </el-form-item>
        <el-form-item :label="t('independentSite.list.styleDescription')" prop="styleDescription">
          <el-input
            v-model="createForm.styleDescription"
            type="textarea"
            :rows="3"
            maxlength="500"
            show-word-limit
            resize="vertical"
            :placeholder="t('independentSite.list.styleDescriptionPlaceholder')"
            :disabled="creating"
          />
        </el-form-item>
        <el-form-item :label="t('independentSite.list.stylePreset')">
          <div
            class="style-preset-grid"
            role="radiogroup"
            :aria-label="t('independentSite.list.chooseStylePreset')"
          >
            <button
              v-for="preset in stylePresets"
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
            {{ t('independentSite.list.stylePresetHelp') }}
          </p>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button :disabled="creating" @click="createDialogVisible = false">
          {{ t('independentSite.common.cancel') }}
        </el-button>
        <el-button type="primary" :loading="creating" @click="handleCreateSite">
          {{
            creating
              ? creatingPhase === 'generating'
                ? t('independentSite.list.generatingDraft')
                : t('independentSite.list.creating')
              : t('independentSite.list.createAction')
          }}
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
