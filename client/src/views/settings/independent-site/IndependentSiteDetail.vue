<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import type { FormInstance, FormItemRule, FormRules } from 'element-plus'
import { ElMessage, ElMessageBox, ElNotification } from 'element-plus'
import { useI18n } from 'vue-i18n'
import {
  ArrowLeft,
  Check,
  CopyDocument,
  Link,
  MagicStick,
  Plus,
  Promotion,
  Refresh,
  RefreshLeft,
  TopRight,
} from '@element-plus/icons-vue'
import {
  aiEditIndependentSitePage,
  createIndependentSitePage,
  deleteIndependentSitePage,
  generateIndependentSitePageDraftForPage,
  generateIndependentSiteRoomPages,
  getIndependentSite,
  getIndependentSitePage,
  importIndependentSitePageFromUrl,
  listIndependentSitePages,
  publishIndependentSitePage,
  undoIndependentSitePageAiEdit,
  updateIndependentSite,
  updateIndependentSitePage,
} from '@/api/independentSite'
import { isUpgradeGuided } from '@/utils/request'
import { getCurrentStorePricePlans, type PricePlanDTO } from '@/api/pricePlan'
import { getAllRoomTypesWithRooms } from '@/api/roomType'
import { useStoreStore } from '@/stores/store'
import type {
  IndependentSiteDetail,
  IndependentSitePageDetail,
  IndependentSitePageImportMode,
  IndependentSitePageImportRequest,
  IndependentSitePageSchema,
  IndependentSitePageSummary,
  IndependentSiteRoomTypeOption,
  IndependentSiteThemeKey,
  IndependentSiteUpdateRequest,
  PublicIndependentSiteRoomType,
} from '@/types/independentSite'
import CanvasEditor from '@/views/independent-site/canvas-editor/CanvasEditor.vue'
import {
  normalizeCanvasSchema,
  normalizeIndependentSitePageFormat,
  type IndependentSiteCanvasSchema,
} from '@/views/independent-site/canvasSchema'
import IndependentSitePageRenderer from '@/views/independent-site/components/IndependentSitePageRenderer.vue'
import IndependentSitePageEditor from '@/views/independent-site/editor/IndependentSitePageEditor.vue'
import {
  createEmptyIndependentSiteSchema,
  normalizeIndependentSiteSchema,
} from '@/views/independent-site/pageSchema'
import {
  getIndependentSiteThemeLabel,
  INDEPENDENT_SITE_THEME_KEYS,
  normalizeIndependentSiteThemeKey,
  resolveIndependentSiteThemeTokens,
} from '@/views/independent-site/themes'
import StripeSettingsDialog from '@/views/settings/independent-site/StripeSettingsDialog.vue'

interface SiteForm {
  name: string
  slug: string
  enabled: boolean
  themeKey: IndependentSiteThemeKey
  defaultPricePlanId: number | null
  priceAdjustmentValue: number | null
  paymentProvider: string
  simulatedPaymentEnabled: boolean
  publishedRoomTypeIds: number[]
  publishedRoomIds: number[]
}

interface CreatePageForm {
  path: string
  title: string
}

interface RenamePageForm {
  title: string
  seoDescription: string
}

interface ImportPageForm {
  url: string
  mode: IndependentSitePageImportMode
  path: string
  title: string
  pageId: number | null
}

interface ApiListResponse<T> {
  success: boolean
  message?: string
  data?: T[]
}

const route = useRoute()
const router = useRouter()
const { locale, t } = useI18n()
const storeStore = useStoreStore()

const basicFormRef = ref<FormInstance>()
const paymentFormRef = ref<FormInstance>()
const createPageFormRef = ref<FormInstance>()
const renameFormRef = ref<FormInstance>()
const importFormRef = ref<FormInstance>()

const loading = ref(true)
const saving = ref(false)
const loadError = ref('')
const optionsError = ref('')
const activeTab = ref('basic')
const siteId = ref<number | null>(null)
const pages = ref<IndependentSitePageSummary[]>([])
const pagesLoading = ref(false)
const pricePlans = ref<PricePlanDTO[]>([])
const roomTypes = ref<IndependentSiteRoomTypeOption[]>([])
const serverPublicUrl = ref('')
const savedSlug = ref('')
const savedEnabled = ref(false)
const formSnapshot = ref('')
let loadSequence = 0

// 页面编辑抽屉状态（按选中页面参数化，HOME/房型页/自定义页通用）
const editorVisible = ref(false)
const editorLoading = ref(false)
const editingPageId = ref<number | null>(null)
const editingPageTitle = ref('')
const pageDraft = ref<IndependentSitePageSchema | null>(null)
const publishedSchema = ref<IndependentSitePageSchema | null>(null)
const savedDraftSchema = ref<IndependentSitePageSchema | null>(null)
const draftVersion = ref<number | null>(null)
const draftUpdatedAt = ref<string | null>(null)
const pagePublishedAt = ref<string | null>(null)
const hasAiBackup = ref(false)
const aiEditInstruction = ref('')
const savingDraft = ref(false)
const publishing = ref(false)
const aiEditing = ref(false)
const undoingAiEdit = ref(false)

// 预览抽屉状态
const previewVisible = ref(false)
const previewLoading = ref(false)
const previewPage = ref<IndependentSitePageSummary | null>(null)
const previewDetail = ref<IndependentSitePageDetail | null>(null)
const previewSource = ref<'draft' | 'published'>('draft')

// 画布编辑器（CANVAS 页）全屏抽屉状态；CanvasEditor 自行加载页面详情
const canvasEditorVisible = ref(false)
const canvasEditorPage = ref<IndependentSitePageSummary | null>(null)
const canvasEditorOverride = ref<IndependentSiteCanvasSchema | null>(null)
const canvasEditorRef = ref<InstanceType<typeof CanvasEditor> | null>(null)

// 行级操作状态
const togglingPageId = ref<number | null>(null)
const deletingPageId = ref<number | null>(null)

// 对话框状态
const createPageDialogVisible = ref(false)
const creatingPage = ref(false)
const renameDialogVisible = ref(false)
const renameLoading = ref(false)
const renaming = ref(false)
const renamePageId = ref<number | null>(null)
const generateDialogVisible = ref(false)
const generating = ref(false)
const generatePage = ref<IndependentSitePageSummary | null>(null)
const generatePrompt = ref('')
const generatingRoomPages = ref(false)
const importDialogVisible = ref(false)
const importing = ref(false)

const form = reactive<SiteForm>({
  name: '',
  slug: '',
  enabled: false,
  themeKey: 'classic',
  defaultPricePlanId: null,
  priceAdjustmentValue: 0,
  paymentProvider: 'SIMULATED',
  simulatedPaymentEnabled: false,
  publishedRoomTypeIds: [],
  publishedRoomIds: [],
})

// 门店 Stripe 密钥是否已配齐（来自站点详情 stripeAvailable），未配齐时禁选 STRIPE
const stripeAvailable = ref(false)
const stripeDialogVisible = ref(false)

const createPageForm = reactive<CreatePageForm>({
  path: '',
  title: '',
})

const renameForm = reactive<RenamePageForm>({
  title: '',
  seoDescription: '',
})

const importForm = reactive<ImportPageForm>({
  url: '',
  mode: 'NEW_PAGE',
  path: '',
  title: '',
  pageId: null,
})

const slugValidator: FormItemRule['validator'] = (_rule, value, callback) => {
  const slug = String(value || '').trim()
  if (!slug) {
    callback(new Error(t('independentSite.detail.validation.slugRequired')))
    return
  }
  if (slug.length < 3 || slug.length > 63) {
    callback(new Error(t('independentSite.detail.validation.slugLength')))
    return
  }
  if (!/^[a-z0-9]+(?:-[a-z0-9]+)*$/.test(slug)) {
    callback(new Error(t('independentSite.detail.validation.slugFormat')))
    return
  }
  callback()
}

const priceAdjustmentValidator: FormItemRule['validator'] = (_rule, value, callback) => {
  if (typeof value !== 'number' || !Number.isFinite(value)) {
    callback(new Error(t('independentSite.detail.validation.priceAdjustmentRequired')))
    return
  }
  if (value < -99.99 || value > 1000) {
    callback(new Error(t('independentSite.detail.validation.priceAdjustmentRange')))
    return
  }
  callback()
}

const validatePagePath = (path: string): string => {
  if (!path) {
    return t('independentSite.detail.validation.pagePathRequired')
  }
  if (path.length > 255) {
    return t('independentSite.detail.validation.pagePathLength')
  }
  if (!/^\/[a-z0-9]+(?:[-/][a-z0-9]+)*$/.test(path)) {
    return t('independentSite.detail.validation.pagePathFormat')
  }
  return ''
}

const pagePathValidator: FormItemRule['validator'] = (_rule, value, callback) => {
  const message = validatePagePath(String(value || '').trim())
  callback(message ? new Error(message) : undefined)
}

const basicRules: FormRules = {
  name: [
    { required: true, message: t('independentSite.detail.validation.siteNameRequired'), trigger: 'blur' },
    { min: 1, max: 120, message: t('independentSite.detail.validation.siteNameLength'), trigger: 'blur' },
  ],
  slug: [{ required: true, validator: slugValidator, trigger: ['blur', 'change'] }],
}

const paymentRules: FormRules = {
  defaultPricePlanId: [
    { required: true, message: t('independentSite.detail.validation.defaultPricePlanRequired'), trigger: 'change' },
  ],
  priceAdjustmentValue: [
    {
      required: true,
      validator: priceAdjustmentValidator,
      trigger: ['blur', 'change'],
    },
  ],
}

const createPageRules: FormRules = {
  path: [{ required: true, validator: pagePathValidator, trigger: ['blur', 'change'] }],
  title: [
    { required: true, message: t('independentSite.detail.validation.pageTitleRequired'), trigger: 'blur' },
    { min: 1, max: 120, message: t('independentSite.detail.validation.pageTitleLength'), trigger: 'blur' },
  ],
}

const renameRules: FormRules = {
  title: [
    { required: true, message: t('independentSite.detail.validation.pageTitleRequired'), trigger: 'blur' },
    { min: 1, max: 120, message: t('independentSite.detail.validation.pageTitleLength'), trigger: 'blur' },
  ],
  seoDescription: [
    { max: 300, message: t('independentSite.detail.validation.seoDescriptionLength'), trigger: 'blur' },
  ],
}

const importUrlValidator: FormItemRule['validator'] = (_rule, value, callback) => {
  const url = String(value || '').trim()
  if (!url) {
    callback(new Error(t('independentSite.detail.validation.importUrlRequired')))
    return
  }
  if (url.length > 2048) {
    callback(new Error(t('independentSite.detail.validation.importUrlLength')))
    return
  }
  if (!/^https?:\/\/\S+$/i.test(url)) {
    callback(new Error(t('independentSite.detail.validation.importUrlFormat')))
    return
  }
  callback()
}

// path/title 仅在新建页面模式下必填；覆盖草稿模式复用 pagePathValidator 的完整规则
const importPathValidator: FormItemRule['validator'] = (_rule, value, callback) => {
  if (importForm.mode !== 'NEW_PAGE') {
    callback()
    return
  }
  const message = validatePagePath(String(value || '').trim())
  callback(message ? new Error(message) : undefined)
}

const importTitleValidator: FormItemRule['validator'] = (_rule, value, callback) => {
  if (importForm.mode !== 'NEW_PAGE') {
    callback()
    return
  }
  const title = String(value || '').trim()
  if (!title) {
    callback(new Error(t('independentSite.detail.validation.pageTitleRequired')))
    return
  }
  if (title.length > 120) {
    callback(new Error(t('independentSite.detail.validation.pageTitleLength')))
    return
  }
  callback()
}

const importPageIdValidator: FormItemRule['validator'] = (_rule, value, callback) => {
  if (importForm.mode !== 'OVERWRITE_DRAFT') {
    callback()
    return
  }
  const pageId = Number(value)
  if (!Number.isInteger(pageId) || pageId <= 0) {
    callback(new Error(t('independentSite.detail.validation.importTargetPageRequired')))
    return
  }
  callback()
}

const importRules: FormRules = {
  url: [{ required: true, validator: importUrlValidator, trigger: ['blur', 'change'] }],
  path: [{ validator: importPathValidator, trigger: ['blur', 'change'] }],
  title: [{ validator: importTitleValidator, trigger: ['blur', 'change'] }],
  pageId: [{ validator: importPageIdValidator, trigger: 'change' }],
}

const formFields = () => ({
  name: form.name.trim(),
  slug: form.slug.trim(),
  enabled: form.enabled,
  themeKey: form.themeKey,
  defaultPricePlanId: form.defaultPricePlanId,
  priceAdjustmentValue: form.priceAdjustmentValue,
  paymentProvider: form.paymentProvider,
  simulatedPaymentEnabled: form.simulatedPaymentEnabled,
  publishedRoomTypeIds: [...form.publishedRoomTypeIds],
  publishedRoomIds: [...form.publishedRoomIds],
})

const formDirty = computed(
  () => formSnapshot.value !== '' && JSON.stringify(formFields()) !== formSnapshot.value,
)

const priceRatioDescription = computed(() => {
  const adjustment = form.priceAdjustmentValue
  if (typeof adjustment !== 'number' || !Number.isFinite(adjustment)) {
    return t('independentSite.detail.validation.priceAdjustmentRange')
  }
  const finalRatio = 100 + adjustment
  if (adjustment > 0) {
    return t('independentSite.detail.priceAdjustmentIncrease', {
      adjustment: formatPercent(adjustment),
      finalRatio: formatPercent(finalRatio),
    })
  }
  if (adjustment < 0) {
    return t('independentSite.detail.priceAdjustmentDecrease', {
      adjustment: formatPercent(Math.abs(adjustment)),
      finalRatio: formatPercent(finalRatio),
    })
  }
  return t('independentSite.detail.priceAdjustmentEqual')
})

const publicUrl = computed(() => {
  if (serverPublicUrl.value && form.slug === savedSlug.value) {
    if (/^https?:\/\//i.test(serverPublicUrl.value)) {
      return serverPublicUrl.value
    }
    const origin = typeof window === 'undefined' ? '' : window.location.origin
    return `${origin}${serverPublicUrl.value.startsWith('/') ? '' : '/'}${serverPublicUrl.value}`
  }
  if (!form.slug) {
    return ''
  }
  const origin = typeof window === 'undefined' ? '' : window.location.origin
  return `${origin}/stay/${form.slug}`
})

const canOpenPublicSite = computed(() =>
  Boolean(siteId.value && savedEnabled.value && form.slug === savedSlug.value && publicUrl.value),
)

const selectedRoomTypes = computed(() =>
  roomTypes.value.filter((roomType) => form.publishedRoomTypeIds.includes(roomType.id)),
)

const hasUnsavedDraftChanges = computed(() => {
  if (!pageDraft.value) {
    return false
  }
  const persistedDraft = savedDraftSchema.value ?? publishedSchema.value
  return JSON.stringify(pageDraft.value) !== JSON.stringify(persistedDraft)
})

const hasSavedDraftReady = computed(
  () =>
    Boolean(savedDraftSchema.value && draftVersion.value !== null) &&
    JSON.stringify(savedDraftSchema.value) !== JSON.stringify(publishedSchema.value),
)

const previewDraftSchema = computed(() =>
  normalizeIndependentSiteSchema(previewDetail.value?.draftSchema),
)
const previewPublishedSchema = computed(() =>
  normalizeIndependentSiteSchema(previewDetail.value?.publishedSchema),
)
const previewSchema = computed(() =>
  previewSource.value === 'draft'
    ? (previewDraftSchema.value ?? previewPublishedSchema.value)
    : (previewPublishedSchema.value ?? previewDraftSchema.value),
)

const previewPublicUrl = computed(() => {
  const slug = savedSlug.value || form.slug
  const page = previewPage.value
  if (!slug || !page) {
    return ''
  }
  const origin = typeof window === 'undefined' ? '' : window.location.origin
  if (page.type === 'HOME') {
    return `${origin}/stay/${slug}?preview=1`
  }
  const tail = page.path.replace(/^\/+/, '')
  return `${origin}/stay/${slug}/p/${tail}?preview=1`
})

function formatPercent(value: number): string {
  return Number.isInteger(value)
    ? String(value)
    : value.toFixed(2).replace(/0+$/, '').replace(/\.$/, '')
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

// 后端错误体为 { success:false, message, data:{ code } }；409 或 code 兜底识别草稿版本冲突
const isDraftVersionConflict = (error: unknown) => {
  if (!error || typeof error !== 'object') {
    return false
  }
  const response = (
    error as { response?: { status?: number; data?: { data?: { code?: unknown } } } }
  ).response
  return response?.status === 409 || response?.data?.data?.code === 'DRAFT_VERSION_CONFLICT'
}

const normalizeIdList = (value: unknown): number[] => {
  if (!Array.isArray(value)) {
    return []
  }
  return Array.from(
    new Set(value.map((item) => Number(item)).filter((item) => Number.isInteger(item) && item > 0)),
  )
}

const applySiteDetail = (detail: IndependentSiteDetail) => {
  siteId.value = Number(detail.id) || null
  form.name = detail.name || ''
  form.slug = detail.slug || ''
  form.enabled = Boolean(detail.enabled)
  form.themeKey = normalizeIndependentSiteThemeKey(detail.themeKey)
  savedSlug.value = form.slug
  savedEnabled.value = form.enabled
  form.defaultPricePlanId = Number(detail.defaultPricePlanId) || null
  const priceAdjustmentValue = Number(detail.priceAdjustmentValue ?? 0)
  form.priceAdjustmentValue = Number.isFinite(priceAdjustmentValue) ? priceAdjustmentValue : 0
  form.paymentProvider = detail.paymentProvider === 'STRIPE' ? 'STRIPE' : 'SIMULATED'
  form.simulatedPaymentEnabled = Boolean(detail.simulatedPaymentEnabled)
  stripeAvailable.value = Boolean(detail.stripeAvailable)
  form.publishedRoomTypeIds = normalizeIdList(detail.publishedRoomTypeIds)
  form.publishedRoomIds = normalizeIdList(detail.publishedRoomIds)
  serverPublicUrl.value = detail.publicPath || ''
  formSnapshot.value = JSON.stringify(formFields())
}

const applyPageDetail = (page: IndependentSitePageDetail) => {
  publishedSchema.value = normalizeIndependentSiteSchema(page.publishedSchema)
  savedDraftSchema.value = normalizeIndependentSiteSchema(page.draftSchema)
  pageDraft.value =
    savedDraftSchema.value ?? publishedSchema.value ?? createEmptyIndependentSiteSchema(t)
  draftUpdatedAt.value = page.draftUpdatedAt || null
  pagePublishedAt.value = page.publishedAt || null
  const pageDraftVersion = Number(page.draftVersion)
  draftVersion.value =
    page.draftVersion !== null &&
    page.draftVersion !== undefined &&
    Number.isInteger(pageDraftVersion) &&
    pageDraftVersion >= 0
      ? pageDraftVersion
      : null
  hasAiBackup.value = Boolean(page.hasAiBackup)
  editingPageTitle.value = page.title || editingPageTitle.value
}

const normalizeRoomTypes = (value: unknown): IndependentSiteRoomTypeOption[] => {
  if (!Array.isArray(value)) {
    return []
  }
  const normalized: IndependentSiteRoomTypeOption[] = []
  for (const item of value) {
    if (!item || typeof item !== 'object') {
      continue
    }
    const raw = item as Record<string, unknown>
    const id = Number(raw.id)
    const name = typeof raw.name === 'string' ? raw.name.trim() : ''
    if (!Number.isInteger(id) || id <= 0 || !name) {
      continue
    }

    const rooms: IndependentSiteRoomTypeOption['rooms'] = []
    if (Array.isArray(raw.rooms)) {
      for (const room of raw.rooms) {
        if (!room || typeof room !== 'object') {
          continue
        }
        const roomRaw = room as Record<string, unknown>
        const roomId = Number(roomRaw.id)
        const roomNumber = typeof roomRaw.roomNumber === 'string' ? roomRaw.roomNumber.trim() : ''
        if (!Number.isInteger(roomId) || roomId <= 0 || !roomNumber) {
          continue
        }
        rooms.push({
          id: roomId,
          roomNumber,
          status: typeof roomRaw.status === 'string' ? roomRaw.status : undefined,
        })
      }
    }

    normalized.push({
      id,
      name,
      code: typeof raw.code === 'string' ? raw.code : undefined,
      description: typeof raw.description === 'string' ? raw.description : undefined,
      maxGuests: Number(raw.maxGuests) || undefined,
      rooms,
    })
  }
  return normalized
}

const resetSiteState = () => {
  siteId.value = null
  pages.value = []
  serverPublicUrl.value = ''
  savedSlug.value = ''
  savedEnabled.value = false
  formSnapshot.value = ''
  editorVisible.value = false
  previewVisible.value = false
  canvasEditorVisible.value = false
  canvasEditorPage.value = null
  canvasEditorOverride.value = null
  editingPageId.value = null
  form.name = ''
  form.slug = ''
  form.enabled = false
  form.themeKey = 'classic'
  form.defaultPricePlanId = null
  form.priceAdjustmentValue = 0
  form.paymentProvider = 'SIMULATED'
  form.simulatedPaymentEnabled = false
  form.publishedRoomTypeIds = []
  form.publishedRoomIds = []
}

const parseRouteSiteId = () => {
  const raw = Number(route.params.id)
  return Number.isInteger(raw) && raw > 0 ? raw : null
}

const refreshPages = async (silent = false) => {
  if (!siteId.value) {
    return
  }
  if (!silent) {
    pagesLoading.value = true
  }
  try {
    const response = await listIndependentSitePages(siteId.value)
    if (response.success) {
      pages.value = Array.isArray(response.data) ? response.data : []
    } else if (!silent) {
      ElMessage.error(response.message || t('independentSite.detail.errors.refreshPagesFailed'))
    }
  } catch (error) {
    if (!silent && !isUpgradeGuided(error)) {
      ElMessage.error(getErrorMessage(error, t('independentSite.detail.errors.refreshPagesFailed')))
    }
  } finally {
    if (!silent) {
      pagesLoading.value = false
    }
  }
}

const loadPage = async () => {
  const sequence = ++loadSequence
  loading.value = true
  loadError.value = ''
  optionsError.value = ''
  resetSiteState()

  const targetSiteId = parseRouteSiteId()
  if (!targetSiteId) {
    loadError.value = t('independentSite.detail.errors.invalidSiteId')
    loading.value = false
    return
  }
  siteId.value = targetSiteId

  const [siteResult, pagesResult, pricePlanResult, roomTypeResult] = await Promise.allSettled([
    getIndependentSite(targetSiteId),
    listIndependentSitePages(targetSiteId),
    getCurrentStorePricePlans() as unknown as Promise<ApiListResponse<PricePlanDTO>>,
    getAllRoomTypesWithRooms(),
  ])

  if (sequence !== loadSequence) {
    return
  }

  if (siteResult.status === 'fulfilled' && siteResult.value.success && siteResult.value.data) {
    applySiteDetail(siteResult.value.data)
  } else {
    loadError.value =
      siteResult.status === 'rejected'
        ? getErrorMessage(siteResult.reason, t('independentSite.detail.errors.loadSiteFailed'))
        : siteResult.value.message || t('independentSite.detail.errors.loadSiteFailed')
  }

  if (pagesResult.status === 'fulfilled' && pagesResult.value.success) {
    pages.value = Array.isArray(pagesResult.value.data) ? pagesResult.value.data : []
  } else if (!loadError.value) {
    loadError.value =
      pagesResult.status === 'rejected'
        ? getErrorMessage(pagesResult.reason, t('independentSite.detail.errors.loadPagesFailed'))
        : pagesResult.value.message || t('independentSite.detail.errors.loadPagesFailed')
  }

  const optionErrors: string[] = []
  if (pricePlanResult.status === 'fulfilled' && pricePlanResult.value.success) {
    pricePlans.value = (pricePlanResult.value.data || []).filter((plan) => Boolean(plan.id))
  } else {
    optionErrors.push(
      pricePlanResult.status === 'rejected'
        ? getErrorMessage(pricePlanResult.reason, t('independentSite.detail.errors.loadPricePlansFailed'))
        : pricePlanResult.value.message || t('independentSite.detail.errors.loadPricePlansFailed'),
    )
  }

  if (roomTypeResult.status === 'fulfilled' && roomTypeResult.value.success) {
    roomTypes.value = normalizeRoomTypes(roomTypeResult.value.data)
  } else {
    optionErrors.push(
      roomTypeResult.status === 'rejected'
        ? getErrorMessage(roomTypeResult.reason, t('independentSite.detail.errors.loadRoomTypesFailed'))
        : roomTypeResult.value.message || t('independentSite.detail.errors.loadRoomTypesFailed'),
    )
  }
  optionsError.value = optionErrors.join(t('independentSite.detail.errorSeparator'))

  if (sequence !== loadSequence) {
    return
  }
  loading.value = false
}

// Stripe 密钥保存成功后只刷新门店级可用性，让 STRIPE 立即可选；不回填整个表单，避免覆盖未保存修改
const refreshStripeAvailability = async () => {
  if (!siteId.value) {
    return
  }
  try {
    const response = await getIndependentSite(siteId.value)
    if (response.success && response.data) {
      stripeAvailable.value = Boolean(response.data.stripeAvailable)
    }
  } catch (error) {
    ElMessage.warning(
      getErrorMessage(error, t('independentSite.detail.errors.refreshStripeAvailabilityFailed')),
    )
  }
}

const buildUpdateRequest = (): IndependentSiteUpdateRequest => {
  const priceAdjustmentValue = form.priceAdjustmentValue
  if (
    typeof priceAdjustmentValue !== 'number' ||
    !Number.isFinite(priceAdjustmentValue) ||
    priceAdjustmentValue < -99.99 ||
    priceAdjustmentValue > 1000
  ) {
    throw new Error(t('independentSite.detail.validation.priceAdjustmentRange'))
  }

  return {
    name: form.name.trim(),
    slug: form.slug.trim(),
    enabled: form.enabled,
    themeKey: form.themeKey,
    defaultPricePlanId: Number(form.defaultPricePlanId),
    priceAdjustmentValue,
    paymentProvider: form.paymentProvider,
    simulatedPaymentEnabled: form.simulatedPaymentEnabled,
    publishedRoomTypeIds: [...form.publishedRoomTypeIds],
    publishedRoomIds: [...form.publishedRoomIds],
  }
}

const persistSettings = async (
  successMessage = t('independentSite.detail.messages.settingsSaved'),
): Promise<boolean> => {
  if (!siteId.value) {
    return false
  }

  const sequence = loadSequence
  saving.value = true
  try {
    const response = await updateIndependentSite(siteId.value, buildUpdateRequest())
    if (sequence !== loadSequence) {
      return false
    }
    if (!response.success || !response.data) {
      throw new Error(response.message || t('independentSite.detail.errors.saveSettingsFailed'))
    }
    applySiteDetail(response.data)
    ElMessage.success(successMessage)
    // 发布范围变化可能联动禁用房型详情页，静默刷新页面列表
    void refreshPages(true)
    return true
  } catch (error) {
    if (sequence !== loadSequence) {
      return false
    }
    // 启用门槛、slug 冲突等错误直接展示后端 message；
    // 402 无独立站权益已由全局升级引导弹窗接管，跳过通用错误 toast（P10 双 toast 修复）
    if (!isUpgradeGuided(error)) {
      ElMessage.error(getErrorMessage(error, t('independentSite.detail.errors.saveSettingsFailed')))
    }
    return false
  } finally {
    saving.value = false
  }
}

const handleSaveBasic = async () => {
  const valid = await basicFormRef.value?.validate().catch(() => false)
  if (!valid) {
    return
  }
  if (form.enabled && form.publishedRoomTypeIds.length === 0) {
    ElMessage.warning(t('independentSite.detail.enableRequiresPublication'))
    activeTab.value = 'publication'
    return
  }
  await persistSettings()
}

const handleSavePublication = () => persistSettings(t('independentSite.detail.messages.publicationSaved'))

const handleSavePayment = async () => {
  const valid = await paymentFormRef.value?.validate().catch(() => false)
  if (!valid) {
    return
  }
  await persistSettings(t('independentSite.detail.messages.paymentThemeSaved'))
}

const handleRoomTypeChange = () => {
  const allowedRoomIds = new Set(
    selectedRoomTypes.value.flatMap((roomType) => roomType.rooms.map((room) => room.id)),
  )
  form.publishedRoomIds = form.publishedRoomIds.filter((roomId) => allowedRoomIds.has(roomId))
}

const roomTypeName = (roomTypeId: number | null | undefined) => {
  if (!roomTypeId) {
    return ''
  }
  const roomType = roomTypes.value.find((item) => item.id === roomTypeId)
  return roomType
    ? roomType.name
    : t('independentSite.detail.roomTypeFallback', { id: roomTypeId })
}

const pageTypeLabel = (page: IndependentSitePageSummary) => {
  if (page.type === 'HOME') {
    return t('independentSite.detail.pageTypeHome')
  }
  if (page.type === 'ROOM_DETAIL') {
    return t('independentSite.detail.pageTypeRoomDetail')
  }
  return t('independentSite.detail.pageTypeCustom')
}

const pageTypeTagType = (page: IndependentSitePageSummary) => {
  if (page.type === 'HOME') {
    return 'success'
  }
  if (page.type === 'ROOM_DETAIL') {
    return 'warning'
  }
  return 'info'
}

// 页面 format 分派：CANVAS → 画布编辑器；BLOCKS（含缺省）→ 旧区块编辑器
const isCanvasPage = (page: IndependentSitePageSummary) =>
  normalizeIndependentSitePageFormat(page.format) === 'CANVAS'

// 画布编辑器 room-list 插槽预览数据：发布范围内房型的精简映射（无照片时插槽显示占位）
const canvasEditorRoomTypes = computed<PublicIndependentSiteRoomType[]>(() =>
  roomTypes.value
    .filter((roomType) => form.publishedRoomTypeIds.includes(roomType.id))
    .map((roomType) => ({
      id: roomType.id,
      name: roomType.name,
      code: roomType.code,
      description: roomType.description,
      maxGuests: roomType.maxGuests,
    })),
)

// ------------------------------------------------------------------
// 页面表格操作
// ------------------------------------------------------------------

const handlePageEnabledChange = async (page: IndependentSitePageSummary, value: boolean) => {
  if (!siteId.value || togglingPageId.value !== null) {
    return
  }
  togglingPageId.value = page.id
  try {
    const response = await updateIndependentSitePage(siteId.value, page.id, { enabled: value })
    if (!response.success || !response.data) {
      throw new Error(response.message || t('independentSite.detail.errors.updatePageStatusFailed'))
    }
    page.enabled = Boolean(response.data.enabled)
    ElMessage.success(
      t(value ? 'independentSite.detail.messages.pageEnabled' : 'independentSite.detail.messages.pageDisabled'),
    )
  } catch (error) {
    if (!isUpgradeGuided(error)) {
      ElMessage.error(getErrorMessage(error, t('independentSite.detail.errors.updatePageStatusFailed')))
    }
  } finally {
    togglingPageId.value = null
  }
}

const openCreatePageDialog = () => {
  createPageForm.path = ''
  createPageForm.title = ''
  createPageDialogVisible.value = true
}

const handleCreatePage = async () => {
  const valid = await createPageFormRef.value?.validate().catch(() => false)
  if (!valid || !siteId.value) {
    return
  }

  creatingPage.value = true
  try {
    const response = await createIndependentSitePage(siteId.value, {
      path: createPageForm.path.trim(),
      title: createPageForm.title.trim(),
    })
    if (!response.success || !response.data) {
      throw new Error(response.message || t('independentSite.detail.errors.createPageFailed'))
    }
    createPageDialogVisible.value = false
    ElMessage.success(t('independentSite.detail.messages.pageCreated'))
    await refreshPages()
  } catch (error) {
    if (!isUpgradeGuided(error)) {
      ElMessage.error(getErrorMessage(error, t('independentSite.detail.errors.createPageFailed')))
    }
  } finally {
    creatingPage.value = false
  }
}

const openRenameDialog = async (page: IndependentSitePageSummary) => {
  if (!siteId.value) {
    return
  }
  renamePageId.value = page.id
  renameForm.title = page.title
  renameForm.seoDescription = ''
  renameDialogVisible.value = true
  renameLoading.value = true
  try {
    const response = await getIndependentSitePage(siteId.value, page.id)
    if (response.success && response.data) {
      renameForm.title = response.data.title || page.title
      renameForm.seoDescription = response.data.seoDescription || ''
    }
  } catch {
    // 保留摘要中的标题，SEO 描述留空提交时会被服务端按原值保留之外覆盖为空；
    // 加载失败时阻止保存，避免误清空 SEO 描述
    renamePageId.value = null
    renameDialogVisible.value = false
    ElMessage.error(t('independentSite.detail.errors.loadPageDetailsRetry'))
  } finally {
    renameLoading.value = false
  }
}

const handleRenamePage = async () => {
  const valid = await renameFormRef.value?.validate().catch(() => false)
  if (!valid || !siteId.value || !renamePageId.value) {
    return
  }

  renaming.value = true
  try {
    const response = await updateIndependentSitePage(siteId.value, renamePageId.value, {
      title: renameForm.title.trim(),
      seoDescription: renameForm.seoDescription.trim(),
    })
    if (!response.success || !response.data) {
      throw new Error(response.message || t('independentSite.detail.errors.savePageInfoFailed'))
    }
    renameDialogVisible.value = false
    ElMessage.success(t('independentSite.detail.messages.pageInfoSaved'))
    await refreshPages(true)
  } catch (error) {
    if (!isUpgradeGuided(error)) {
      ElMessage.error(getErrorMessage(error, t('independentSite.detail.errors.savePageInfoFailed')))
    }
  } finally {
    renaming.value = false
  }
}

const handleDeletePage = async (page: IndependentSitePageSummary) => {
  if (!siteId.value || page.type === 'HOME') {
    return
  }
  try {
    await ElMessageBox.confirm(
      t('independentSite.detail.deletePageConfirm', { name: page.title || page.path }),
      t('independentSite.detail.deletePageTitle'),
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

  deletingPageId.value = page.id
  try {
    const response = await deleteIndependentSitePage(siteId.value, page.id)
    if (!response.success) {
      throw new Error(response.message || t('independentSite.detail.errors.deletePageFailed'))
    }
    ElMessage.success(t('independentSite.detail.messages.pageDeleted'))
    pages.value = pages.value.filter((item) => item.id !== page.id)
  } catch (error) {
    if (!isUpgradeGuided(error)) {
      ElMessage.error(getErrorMessage(error, t('independentSite.detail.errors.deletePageFailed')))
    }
  } finally {
    deletingPageId.value = null
  }
}

const handleGenerateRoomPages = async () => {
  if (!siteId.value || generatingRoomPages.value) {
    return
  }
  generatingRoomPages.value = true
  try {
    const response = await generateIndependentSiteRoomPages(siteId.value)
    if (!response.success || !response.data) {
      throw new Error(response.message || t('independentSite.detail.errors.generateRoomPagesFailed'))
    }
    const result = response.data
    if (Array.isArray(result.pages)) {
      pages.value = result.pages
    }
    const skipped = Array.isArray(result.skipped) ? result.skipped : []
    const skippedReasons = skipped
      .map(
        (item) =>
          t('independentSite.detail.roomPageSkippedReason', {
            name:
              roomTypeName(item.roomTypeId) ||
              t('independentSite.detail.roomTypeFallback', { id: item.roomTypeId }),
            reason: item.reason,
          }),
      )
      .join(t('independentSite.detail.errorSeparator'))
    ElNotification({
      title: t('independentSite.detail.roomPageGenerationTitle'),
      message: t('independentSite.detail.roomPageGenerationMessage', {
        generated: result.generated,
        refreshed: result.refreshed,
        skipped: skipped.length,
        reasons: skippedReasons
          ? t('independentSite.detail.roomPageGenerationReasons', { reasons: skippedReasons })
          : '',
      }),
      type: skipped.length > 0 ? 'warning' : 'success',
      duration: 8000,
    })
  } catch (error) {
    if (!isUpgradeGuided(error)) {
      ElMessage.error(getErrorMessage(error, t('independentSite.detail.errors.generateRoomPagesFailed')))
    }
  } finally {
    generatingRoomPages.value = false
  }
}

// ------------------------------------------------------------------
// 从 URL 导入（抓取 + AI 生成草稿，成功后打开编辑器检查）
// ------------------------------------------------------------------

const openImportDialog = () => {
  importForm.url = ''
  importForm.mode = 'NEW_PAGE'
  importForm.path = ''
  importForm.title = ''
  importForm.pageId = null
  importDialogVisible.value = true
}

const handleImportModeChange = () => {
  importFormRef.value?.clearValidate()
}

const handleImportPage = async () => {
  const valid = await importFormRef.value?.validate().catch(() => false)
  if (!valid || !siteId.value) {
    return
  }

  const sequence = loadSequence
  const payload: IndependentSitePageImportRequest = {
    url: importForm.url.trim(),
    mode: importForm.mode,
  }
  if (importForm.mode === 'NEW_PAGE') {
    payload.path = importForm.path.trim()
    payload.title = importForm.title.trim()
  } else if (importForm.pageId) {
    payload.pageId = importForm.pageId
  }

  importing.value = true
  try {
    const response = await importIndependentSitePageFromUrl(siteId.value, payload)
    if (sequence !== loadSequence) {
      return
    }
    if (!response.success || !response.data) {
      throw new Error(response.message || t('independentSite.detail.errors.importFailed'))
    }
    const importedPage = response.data
    importDialogVisible.value = false
    ElMessage.success(t('independentSite.detail.messages.importSucceeded'))
    await refreshPages()
    // 复用编辑器抽屉让用户立刻检查导入结果；列表缺失时用详情构造摘要兜底
    const summary = pages.value.find((item) => item.id === importedPage.id) ?? {
      id: importedPage.id,
      path: importedPage.path,
      type: importedPage.type,
      title: importedPage.title,
      enabled: importedPage.enabled,
      sortOrder: importedPage.sortOrder,
      roomTypeId: importedPage.roomTypeId ?? null,
      draftUpdatedAt: importedPage.draftUpdatedAt ?? null,
      publishedAt: importedPage.publishedAt ?? null,
      hasUnpublishedChanges: true,
    }
    await openPageEditor(summary)
  } catch (error) {
    if (sequence !== loadSequence) {
      return
    }
    // 400 URL_NOT_ALLOWED / 409 路径冲突 / 422 抓取失败 / 429 限流等直接展示后端 message；
    // 402（AI 配额耗尽 / 无权益）已由全局升级引导弹窗接管，跳过通用错误 toast（P10 双 toast 修复）
    if (!isUpgradeGuided(error)) {
      ElMessage.error(getErrorMessage(error, t('independentSite.detail.errors.importRetry')))
    }
  } finally {
    importing.value = false
  }
}

// ------------------------------------------------------------------
// 编辑内容抽屉（按页面参数化：保存草稿/发布/AI 修改/撤销）
// CANVAS 页分派到画布编辑器（全屏抽屉），BLOCKS 页沿用旧区块编辑器
// ------------------------------------------------------------------

const openCanvasPageEditor = (
  page: IndependentSitePageSummary,
  overrideSchema: IndependentSiteCanvasSchema | null = null,
) => {
  canvasEditorPage.value = page
  canvasEditorOverride.value = overrideSchema
  canvasEditorVisible.value = true
}

const handleCanvasEditorBeforeClose = (done: () => void) => {
  const editor = canvasEditorRef.value
  if (!editor) {
    done()
    return
  }
  void editor.prepareClose().then((ok) => {
    if (ok) {
      done()
    }
  })
}

const handleCanvasEditorClose = () => {
  canvasEditorVisible.value = false
}

// v-if 卸载抽屉时 el-drawer 的 @closed 不会触发，改用可见性监听做清理与页面列表刷新
watch(canvasEditorVisible, (visible, previous) => {
  if (previous && !visible) {
    canvasEditorPage.value = null
    canvasEditorOverride.value = null
    void refreshPages(true)
  }
})

const handleCanvasEditorUpdated = () => {
  void refreshPages(true)
}

const openPageEditor = async (
  page: IndependentSitePageSummary,
  overrideDraft?: IndependentSitePageSchema,
) => {
  if (!siteId.value) {
    return
  }
  if (isCanvasPage(page)) {
    openCanvasPageEditor(page)
    return
  }
  editingPageId.value = page.id
  editingPageTitle.value = page.title || page.path
  aiEditInstruction.value = ''
  editorLoading.value = true
  editorVisible.value = true
  try {
    const response = await getIndependentSitePage(siteId.value, page.id)
    if (!response.success || !response.data) {
      throw new Error(response.message || t('independentSite.detail.errors.loadPageDetailsFailed'))
    }
    applyPageDetail(response.data)
    if (overrideDraft) {
      pageDraft.value = overrideDraft
    }
  } catch (error) {
    editorVisible.value = false
    ElMessage.error(getErrorMessage(error, t('independentSite.detail.errors.loadPageDetailsFailed')))
  } finally {
    editorLoading.value = false
  }
}

const reloadEditingPageDetail = async () => {
  if (!siteId.value || !editingPageId.value) {
    return
  }
  try {
    const response = await getIndependentSitePage(siteId.value, editingPageId.value)
    if (response.success && response.data) {
      applyPageDetail(response.data)
    }
  } catch {
    // 保留当前状态，下一次保存/发布会再次提示
  }
}

const handleEditorChange = (schema: IndependentSitePageSchema | null) => {
  pageDraft.value = schema
}

const handleEditorBeforeClose = (done: () => void) => {
  if (!hasUnsavedDraftChanges.value) {
    done()
    return
  }
  ElMessageBox.confirm(t('independentSite.detail.closeEditorConfirm'), t('independentSite.detail.closeEditorTitle'), {
    confirmButtonText: t('independentSite.common.close'),
    cancelButtonText: t('independentSite.detail.continueEditing'),
    type: 'warning',
  })
    .then(() => done())
    .catch(() => {})
}

const handleSaveDraft = async (): Promise<boolean> => {
  const normalizedSchema = normalizeIndependentSiteSchema(pageDraft.value)
  if (!normalizedSchema || normalizedSchema.sections.length === 0) {
    ElMessage.warning(t('independentSite.detail.errors.invalidPageContent'))
    return false
  }
  if (!siteId.value || !editingPageId.value) {
    ElMessage.error(t('independentSite.detail.errors.pageNotSelectedForSave'))
    return false
  }
  if (!hasUnsavedDraftChanges.value) {
    ElMessage.info(t('independentSite.detail.messages.draftAlreadySaved'))
    return true
  }

  const sequence = loadSequence
  savingDraft.value = true
  try {
    const response = await updateIndependentSitePage(siteId.value, editingPageId.value, {
      draftSchema: normalizedSchema,
      expectedDraftVersion: draftVersion.value ?? undefined,
    })
    if (sequence !== loadSequence) {
      return false
    }
    if (!response.success || !response.data) {
      throw new Error(response.message || t('independentSite.detail.errors.saveDraftFailed'))
    }
    const savedSchema = normalizeIndependentSiteSchema(response.data.draftSchema)
    if (!savedSchema) {
      throw new Error(t('independentSite.detail.errors.serverDraftSchemaInvalid'))
    }
    const savedDraftVersion = Number(response.data.draftVersion)
    if (!Number.isInteger(savedDraftVersion) || savedDraftVersion < 0) {
      throw new Error(t('independentSite.detail.errors.serverDraftVersionInvalid'))
    }
    pageDraft.value = savedSchema
    savedDraftSchema.value = savedSchema
    draftUpdatedAt.value = response.data.draftUpdatedAt || null
    draftVersion.value = savedDraftVersion
    ElMessage.success(t('independentSite.detail.messages.draftSaved'))
    void refreshPages(true)
    return true
  } catch (error) {
    if (isDraftVersionConflict(error)) {
      ElMessage.error(t('independentSite.detail.errors.draftUpdatedElsewhere'))
      await reloadEditingPageDetail()
      return false
    }
    ElMessage.error(getErrorMessage(error, t('independentSite.detail.errors.saveDraftFailed')))
    return false
  } finally {
    savingDraft.value = false
  }
}

const handlePublish = async () => {
  if (hasUnsavedDraftChanges.value) {
    ElMessage.warning(t('independentSite.detail.errors.draftNotSavedBeforePublish'))
    return
  }
  if (!hasSavedDraftReady.value || draftVersion.value === null) {
    ElMessage.warning(t('independentSite.detail.errors.noSavedDraftToPublish'))
    return
  }
  if (!siteId.value || !editingPageId.value) {
    ElMessage.error(t('independentSite.detail.errors.pageNotSelectedForPublish'))
    return
  }

  const sequence = loadSequence
  publishing.value = true
  try {
    const response = await publishIndependentSitePage(siteId.value, editingPageId.value, {
      draftVersion: draftVersion.value,
    })
    if (sequence !== loadSequence) {
      return
    }
    if (!response.success || !response.data) {
      throw new Error(response.message || t('independentSite.detail.errors.publishPageFailed'))
    }
    applyPageDetail(response.data)
    ElMessage.success(t('independentSite.detail.messages.pagePublished'))
    void refreshPages(true)
  } catch (error) {
    if (isDraftVersionConflict(error)) {
      ElMessage.error(t('independentSite.detail.errors.draftVersionChanged'))
      await reloadEditingPageDetail()
      return
    }
    ElMessage.error(getErrorMessage(error, t('independentSite.detail.errors.publishPageFailed')))
  } finally {
    publishing.value = false
  }
}

const handleAiEdit = async () => {
  const instruction = aiEditInstruction.value.trim()
  if (!instruction) {
    ElMessage.warning(t('independentSite.detail.errors.aiInstructionRequired'))
    return
  }
  if (instruction.length > 2000) {
    ElMessage.warning(t('independentSite.detail.errors.aiInstructionLength'))
    return
  }
  if (!siteId.value || !editingPageId.value) {
    ElMessage.warning(t('independentSite.detail.errors.pageNotSelectedForAiEdit'))
    return
  }
  if (hasUnsavedDraftChanges.value) {
    ElMessage.warning(t('independentSite.detail.errors.draftNotSavedBeforeAiEdit'))
    return
  }

  const sequence = loadSequence
  aiEditing.value = true
  try {
    const response = await aiEditIndependentSitePage(siteId.value, editingPageId.value, {
      instruction,
    })
    if (sequence !== loadSequence) {
      return
    }
    if (!response.success || !response.data) {
      throw new Error(response.message || t('independentSite.detail.errors.aiEditFailed'))
    }
    applyPageDetail(response.data)
    aiEditInstruction.value = ''
    ElMessage.success(t('independentSite.detail.messages.aiEditSucceeded'))
    void refreshPages(true)
  } catch (error) {
    if (sequence !== loadSequence) {
      return
    }
    ElMessage.error(getErrorMessage(error, t('independentSite.detail.errors.aiEditRetry')))
  } finally {
    aiEditing.value = false
  }
}

const handleUndoAiEdit = async () => {
  if (!siteId.value || !editingPageId.value || !hasAiBackup.value) {
    return
  }

  const sequence = loadSequence
  undoingAiEdit.value = true
  try {
    const response = await undoIndependentSitePageAiEdit(siteId.value, editingPageId.value)
    if (sequence !== loadSequence) {
      return
    }
    if (!response.success || !response.data) {
      throw new Error(response.message || t('independentSite.detail.errors.undoAiFailed'))
    }
    applyPageDetail(response.data)
    ElMessage.success(t('independentSite.detail.messages.undoAiSucceeded'))
    void refreshPages(true)
  } catch (error) {
    if (sequence !== loadSequence) {
      return
    }
    ElMessage.error(getErrorMessage(error, t('independentSite.detail.errors.undoAiFailed')))
  } finally {
    undoingAiEdit.value = false
  }
}

// ------------------------------------------------------------------
// AI 整页生成（结果进入编辑器，检查后保存草稿）
// ------------------------------------------------------------------

const openGenerateDialog = (page: IndependentSitePageSummary) => {
  generatePage.value = page
  generatePrompt.value = ''
  generateDialogVisible.value = true
}

const handleGenerate = async () => {
  const prompt = generatePrompt.value.trim()
  if (prompt.length < 10) {
    ElMessage.warning(t('independentSite.detail.errors.aiPromptMinLength'))
    return
  }
  if (prompt.length > 1000) {
    ElMessage.warning(t('independentSite.detail.errors.aiPromptLength'))
    return
  }
  if (!siteId.value || !generatePage.value) {
    return
  }

  const sequence = loadSequence
  const targetPage = generatePage.value
  generating.value = true
  try {
    const response = await generateIndependentSitePageDraftForPage(siteId.value, targetPage.id, {
      prompt,
      language: locale.value,
    })
    if (sequence !== loadSequence) {
      return
    }
    if (!response.success || !response.data) {
      throw new Error(response.message || t('independentSite.detail.errors.aiGenerateFailed'))
    }
    if (!response.data.publishable) {
      throw new Error(t('independentSite.detail.errors.aiDraftUnpublishable'))
    }
    const rawSchema = (response.data as { pageSchema?: unknown }).pageSchema ?? response.data
    if (isCanvasPage(targetPage)) {
      const canvasSchema = normalizeCanvasSchema(rawSchema)
      if (!canvasSchema) {
        throw new Error(t('independentSite.detail.errors.aiCanvasSchemaInvalid'))
      }
      generateDialogVisible.value = false
      ElMessage.success(t('independentSite.detail.messages.aiCanvasDraftLoaded'))
      openCanvasPageEditor(targetPage, canvasSchema)
      return
    }
    const normalizedSchema = normalizeIndependentSiteSchema(rawSchema)
    if (!normalizedSchema || normalizedSchema.sections.length === 0) {
      throw new Error(t('independentSite.detail.errors.aiSchemaInvalid'))
    }
    generateDialogVisible.value = false
    ElMessage.success(t('independentSite.detail.messages.aiDraftLoaded'))
    await openPageEditor(targetPage, normalizedSchema)
  } catch (error) {
    ElMessage.error(getErrorMessage(error, t('independentSite.detail.errors.aiGenerateRetry')))
  } finally {
    generating.value = false
  }
}

// ------------------------------------------------------------------
// 预览
// ------------------------------------------------------------------

const openPagePreview = async (page: IndependentSitePageSummary) => {
  if (!siteId.value) {
    return
  }
  if (isCanvasPage(page)) {
    // CANVAS 页预览直接走公开页新标签（沿用 ?preview=1 语义），不经过 BLOCKS 预览抽屉
    const slug = savedSlug.value || form.slug
    if (!slug) {
      ElMessage.warning(t('independentSite.detail.errors.saveSlugBeforePreview'))
      return
    }
    const origin = typeof window === 'undefined' ? '' : window.location.origin
    const url =
      page.type === 'HOME'
        ? `${origin}/stay/${slug}?preview=1`
        : `${origin}/stay/${slug}/p/${page.path.replace(/^\/+/, '')}?preview=1`
    window.open(url, '_blank', 'noopener,noreferrer')
    return
  }
  previewPage.value = page
  previewDetail.value = null
  previewVisible.value = true
  previewLoading.value = true
  try {
    const response = await getIndependentSitePage(siteId.value, page.id)
    if (!response.success || !response.data) {
      throw new Error(response.message || t('independentSite.detail.errors.loadPreviewFailed'))
    }
    previewDetail.value = response.data
    previewSource.value = normalizeIndependentSiteSchema(response.data.draftSchema)
      ? 'draft'
      : 'published'
  } catch (error) {
    previewVisible.value = false
    ElMessage.error(getErrorMessage(error, t('independentSite.detail.errors.loadPreviewFailed')))
  } finally {
    previewLoading.value = false
  }
}

const openPreviewInNewTab = () => {
  if (!previewPublicUrl.value) {
    ElMessage.warning(t('independentSite.detail.errors.saveSlugBeforeNewTabPreview'))
    return
  }
  window.open(previewPublicUrl.value, '_blank', 'noopener,noreferrer')
}

const copyPublicUrl = async () => {
  if (!publicUrl.value) {
    ElMessage.warning(t('independentSite.detail.errors.publicLinkSuffixRequired'))
    return
  }
  try {
    await navigator.clipboard.writeText(publicUrl.value)
    ElMessage.success(t('independentSite.detail.messages.publicLinkCopied'))
  } catch {
    ElMessage.error(t('independentSite.detail.errors.copyPublicLinkFailed'))
  }
}

const openPublicSite = () => {
  if (!canOpenPublicSite.value) {
    ElMessage.warning(t('independentSite.detail.errors.openPublicPagePrerequisite'))
    return
  }
  window.open(publicUrl.value, '_blank', 'noopener,noreferrer')
}

const themeTokensOf = (themeKey: IndependentSiteThemeKey) =>
  resolveIndependentSiteThemeTokens(themeKey)

const goBack = () => {
  void router.push({ name: 'IndependentSiteSettings' })
}

onMounted(loadPage)

watch(
  () => route.params.id,
  (next, previous) => {
    if (next !== previous) {
      loadPage()
    }
  },
)

watch(
  () => storeStore.currentStore?.id,
  (currentStoreId, previousStoreId) => {
    if (previousStoreId && currentStoreId && currentStoreId !== previousStoreId) {
      loadPage()
    } else if (previousStoreId && !currentStoreId) {
      loadSequence += 1
      resetSiteState()
      loading.value = false
    }
  },
)
</script>

<template>
  <div v-loading="loading" class="independent-site-detail">
    <header class="page-header">
      <div class="header-left">
        <el-button :icon="ArrowLeft" @click="goBack">
          {{ t('independentSite.detail.backToList') }}
        </el-button>
        <div>
          <div class="header-eyebrow">
            <el-icon><Promotion /></el-icon>
            {{ t('independentSite.detail.directBooking') }}
          </div>
          <h1>{{ form.name || t('independentSite.detail.defaultSiteName') }}</h1>
          <p>/stay/{{ savedSlug || form.slug || '…' }}</p>
        </div>
      </div>
      <div class="header-actions">
        <el-tag :type="savedEnabled ? 'success' : 'info'" effect="plain">
          {{ savedEnabled ? t('independentSite.common.enabled') : t('independentSite.common.disabled') }}
        </el-tag>
        <el-tag v-if="formDirty" type="warning" effect="plain">
          {{ t('independentSite.detail.unsavedChanges') }}
        </el-tag>
        <el-button :icon="CopyDocument" :disabled="Boolean(loadError)" @click="copyPublicUrl">
          {{ t('independentSite.detail.copyPublicLink') }}
        </el-button>
        <el-button :icon="TopRight" :disabled="!canOpenPublicSite" @click="openPublicSite">
          {{ t('independentSite.detail.openPublicPage') }}
        </el-button>
      </div>
    </header>

    <el-alert
      v-if="loadError"
      class="page-alert"
      type="error"
      :title="loadError"
      :description="t('independentSite.detail.loadErrorDescription')"
      show-icon
      :closable="false"
    >
      <template #default>
        <el-button class="alert-action" size="small" :icon="Refresh" @click="loadPage">
          {{ t('independentSite.common.reload') }}
        </el-button>
      </template>
    </el-alert>

    <el-alert
      v-if="optionsError"
      class="page-alert"
      type="warning"
      :title="optionsError"
      :description="t('independentSite.detail.optionsErrorDescription')"
      show-icon
      :closable="false"
    />

    <div v-if="!loadError" class="tabs-card">
      <el-tabs v-model="activeTab" class="site-tabs">
        <el-tab-pane :label="t('independentSite.detail.basicTab')" name="basic">
          <el-form
            ref="basicFormRef"
            :model="form"
            :rules="basicRules"
            label-position="top"
            class="tab-form"
          >
            <div class="form-grid">
              <el-form-item :label="t('independentSite.detail.siteName')" prop="name">
                <el-input
                  v-model.trim="form.name"
                  maxlength="120"
                  show-word-limit
                  autocomplete="off"
                  :placeholder="t('independentSite.detail.siteNamePlaceholder')"
                />
                <div class="field-help">{{ t('independentSite.detail.siteNameHelp') }}</div>
              </el-form-item>

              <el-form-item :label="t('independentSite.detail.slug')" prop="slug">
                <el-input
                  v-model.trim="form.slug"
                  maxlength="63"
                  show-word-limit
                  autocomplete="off"
                  :placeholder="t('independentSite.detail.slugPlaceholder')"
                >
                  <template #prepend>/stay/</template>
                </el-input>
                <div class="field-help">
                  {{ t('independentSite.detail.slugHelp') }}
                </div>
              </el-form-item>

              <el-form-item :label="t('independentSite.detail.publicUrl')">
                <el-input
                  :model-value="publicUrl"
                  readonly
                  :placeholder="t('independentSite.detail.publicUrlPlaceholder')"
                >
                  <template #append>
                    <el-button
                      :icon="CopyDocument"
                      :aria-label="t('independentSite.detail.copyPublicLink')"
                      @click="copyPublicUrl"
                    />
                  </template>
                </el-input>
                <div class="field-help">
                  {{ t('independentSite.detail.publicUrlHelp') }}
                </div>
              </el-form-item>
            </div>

            <div class="payment-row">
              <div>
                <h3>{{ t('independentSite.detail.enableSite') }}</h3>
                <p>{{ t('independentSite.detail.enableSiteHelp') }}</p>
              </div>
              <el-switch
                v-model="form.enabled"
                inline-prompt
                :active-text="t('independentSite.detail.enable')"
                :inactive-text="t('independentSite.detail.disable')"
                :aria-label="t('independentSite.detail.enableSiteAria')"
              />
            </div>

            <div class="tab-actions">
              <el-button
                type="primary"
                :icon="Check"
                :loading="saving"
                :disabled="publishing || savingDraft || generating"
                @click="handleSaveBasic"
              >
                {{ t('independentSite.detail.saveBasic') }}
              </el-button>
            </div>
          </el-form>
        </el-tab-pane>

        <el-tab-pane :label="t('independentSite.detail.pagesTab')" name="pages">
          <div class="tab-toolbar">
            <p class="toolbar-hint">
              {{ t('independentSite.detail.pagesHelp') }}
            </p>
            <div class="toolbar-actions">
              <el-button :icon="Plus" @click="openCreatePageDialog">
                {{ t('independentSite.detail.newCustomPage') }}
              </el-button>
              <el-button :icon="Link" @click="openImportDialog">
                {{ t('independentSite.detail.importFromUrl') }}
              </el-button>
              <el-button
                type="primary"
                :icon="MagicStick"
                :loading="generatingRoomPages"
                @click="handleGenerateRoomPages"
              >
                {{ t('independentSite.detail.generateRoomPages') }}
              </el-button>
            </div>
          </div>

          <el-table v-loading="pagesLoading" :data="pages" row-key="id" class="page-table">
            <el-table-column :label="t('independentSite.detail.pageTitle')" min-width="160">
              <template #default="{ row }">
                <span class="page-title">{{ row.title }}</span>
              </template>
            </el-table-column>
            <el-table-column :label="t('independentSite.detail.pagePath')" min-width="140">
              <template #default="{ row }">
                <code class="page-path">{{ row.path }}</code>
              </template>
            </el-table-column>
            <el-table-column :label="t('independentSite.detail.pageType')" width="130">
              <template #default="{ row }">
                <el-tag :type="pageTypeTagType(row)" effect="plain">{{
                  pageTypeLabel(row)
                }}</el-tag>
                <div v-if="row.type === 'ROOM_DETAIL'" class="page-sub-meta">
                  {{ roomTypeName(row.roomTypeId) }}
                </div>
              </template>
            </el-table-column>
            <el-table-column :label="t('independentSite.detail.enable')" width="80" align="center">
              <template #default="{ row }">
                <el-switch
                  :model-value="row.enabled"
                  :loading="togglingPageId === row.id"
                  :aria-label="t('independentSite.detail.enablePageAria', { title: row.title })"
                  @change="
                    (value: string | number | boolean) =>
                      handlePageEnabledChange(row, Boolean(value))
                  "
                />
              </template>
            </el-table-column>
            <el-table-column :label="t('independentSite.detail.publishStatus')" width="130">
              <template #default="{ row }">
                <el-tag v-if="!row.publishedAt" type="info" effect="plain">
                  {{ t('independentSite.detail.unpublished') }}
                </el-tag>
                <el-tag v-else-if="row.hasUnpublishedChanges" type="warning" effect="plain">
                  {{ t('independentSite.detail.unpublishedChanges') }}
                </el-tag>
                <el-tag v-else type="success" effect="plain">
                  {{ t('independentSite.common.published') }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column :label="t('independentSite.detail.actions')" width="330" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" size="small" @click="openPageEditor(row)">
                  {{ t('independentSite.detail.editContent') }}
                </el-button>
                <el-button link type="primary" size="small" @click="openGenerateDialog(row)">
                  {{ t('independentSite.detail.aiGeneratePage') }}
                </el-button>
                <el-button link type="primary" size="small" @click="openPagePreview(row)">
                  {{ t('independentSite.detail.preview') }}
                </el-button>
                <el-button link type="primary" size="small" @click="openRenameDialog(row)">
                  {{ t('independentSite.detail.renameSeo') }}
                </el-button>
                <el-button
                  link
                  type="danger"
                  size="small"
                  :disabled="row.type === 'HOME'"
                  :loading="deletingPageId === row.id"
                  :title="row.type === 'HOME' ? t('independentSite.detail.homeCannotDelete') : ''"
                  @click="handleDeletePage(row)"
                >
                  {{ t('independentSite.common.delete') }}
                </el-button>
              </template>
            </el-table-column>
            <template #empty>
              <el-empty :description="t('independentSite.detail.noPages')" />
            </template>
          </el-table>
        </el-tab-pane>

        <el-tab-pane :label="t('independentSite.detail.publicationTab')" name="publication">
          <div class="card-heading">
            <div>
              <h2>{{ t('independentSite.detail.publishRoomTypesAndRooms') }}</h2>
            </div>
            <span class="selection-summary">
              {{
                t('independentSite.detail.selectionSummary', {
                  roomTypes: form.publishedRoomTypeIds.length,
                  rooms: form.publishedRoomIds.length,
                })
              }}
            </span>
          </div>

          <el-empty
            v-if="roomTypes.length === 0"
            :description="t('independentSite.detail.noPublishableRoomTypes')"
          />
          <el-checkbox-group
            v-else
            v-model="form.publishedRoomTypeIds"
            class="room-type-grid"
            :aria-label="t('independentSite.detail.selectPublishedRoomTypes')"
            @change="handleRoomTypeChange"
          >
            <div
              v-for="roomType in roomTypes"
              :key="roomType.id"
              class="room-type-card"
              :class="{ 'is-selected': form.publishedRoomTypeIds.includes(roomType.id) }"
            >
              <el-checkbox :value="roomType.id">
                <span class="room-type-name">{{ roomType.name }}</span>
              </el-checkbox>
              <span
                >{{
                  t('independentSite.detail.roomTypeMeta', {
                    rooms: roomType.rooms.length,
                    guests: roomType.maxGuests || '—',
                  })
                }}</span
              >
            </div>
          </el-checkbox-group>

          <div v-if="selectedRoomTypes.length" class="specific-room-area">
            <div class="subsection-heading">
              <h3>{{ t('independentSite.detail.specificRooms') }}</h3>
              <p>{{ t('independentSite.detail.specificRoomsHelp') }}</p>
            </div>
            <div class="specific-room-list">
              <div
                v-for="roomType in selectedRoomTypes"
                :key="roomType.id"
                class="specific-room-group"
              >
                <strong>{{ roomType.name }}</strong>
                <el-checkbox-group
                  v-if="roomType.rooms.length"
                  v-model="form.publishedRoomIds"
                  :aria-label="t('independentSite.detail.specificRoomsAria', { name: roomType.name })"
                >
                  <el-checkbox
                    v-for="room in roomType.rooms"
                    :key="room.id"
                    :value="room.id"
                    :disabled="room.status === 'OUT_OF_SERVICE'"
                  >
                    {{ room.roomNumber }}
                  </el-checkbox>
                </el-checkbox-group>
                <span v-else class="empty-inline">{{ t('independentSite.detail.noPhysicalRooms') }}</span>
              </div>
            </div>
          </div>

          <div class="tab-actions">
            <el-button
              type="primary"
              :icon="Check"
              :loading="saving"
              @click="handleSavePublication"
            >
              {{ t('independentSite.detail.savePublication') }}
            </el-button>
          </div>
        </el-tab-pane>

        <el-tab-pane :label="t('independentSite.detail.paymentTab')" name="payment">
          <el-form
            ref="paymentFormRef"
            :model="form"
            :rules="paymentRules"
            label-position="top"
            class="tab-form"
          >
            <div class="form-grid">
              <el-form-item :label="t('independentSite.detail.defaultPricePlan')" prop="defaultPricePlanId">
                <el-select
                  v-model="form.defaultPricePlanId"
                  filterable
                  :placeholder="t('independentSite.detail.selectPricePlan')"
                  :empty-values="[null, undefined]"
                  style="width: 100%"
                >
                  <el-option
                    v-for="plan in pricePlans"
                    :key="plan.id"
                    :label="plan.name"
                    :value="plan.id!"
                  />
                </el-select>
                <div class="field-help">{{ t('independentSite.detail.defaultPricePlanHelp') }}</div>
              </el-form-item>

              <el-form-item :label="t('independentSite.detail.priceAdjustment')" prop="priceAdjustmentValue">
                <el-input-number
                  v-model="form.priceAdjustmentValue"
                  :min="-99.99"
                  :max="1000"
                  :precision="2"
                  :step="1"
                  :value-on-clear="null"
                  controls-position="right"
                  style="width: 100%"
                >
                  <template #suffix>%</template>
                </el-input-number>
                <div class="ratio-explanation">
                  <strong>{{ priceRatioDescription }}</strong>
                  <span>{{ t('independentSite.detail.priceAdjustmentHint') }}</span>
                </div>
              </el-form-item>
            </div>

            <el-form-item :label="t('independentSite.detail.siteTheme')" class="theme-form-item">
              <div
                class="theme-picker"
                role="radiogroup"
                :aria-label="t('independentSite.detail.selectSiteTheme')"
              >
                <button
                  v-for="themeKey in INDEPENDENT_SITE_THEME_KEYS"
                  :key="themeKey"
                  type="button"
                  class="theme-card"
                  :class="{ 'is-active': form.themeKey === themeKey }"
                  @click="form.themeKey = themeKey"
                >
                  <span class="theme-swatches" aria-hidden="true">
                    <i :style="{ background: themeTokensOf(themeKey).primaryColor }"></i>
                    <i :style="{ background: themeTokensOf(themeKey).accentColor }"></i>
                    <i :style="{ background: themeTokensOf(themeKey).surfaceColor }"></i>
                  </span>
                  <span class="theme-card-name">{{ getIndependentSiteThemeLabel(t, themeKey) }}</span>
                </button>
              </div>
              <div class="field-help">
                {{ t('independentSite.detail.themeHelp') }}
              </div>
            </el-form-item>

            <div class="payment-row payment-provider-row">
              <div>
                <h3>{{ t('independentSite.detail.paymentProvider') }}</h3>
                <p>{{ t('independentSite.detail.paymentProviderHelp') }}</p>
                <p v-if="!stripeAvailable" class="field-help">
                  {{ t('independentSite.detail.stripeSetupNeeded') }}
                  <el-button link type="primary" size="small" @click="stripeDialogVisible = true">
                    {{ t('independentSite.detail.configure') }}
                  </el-button>
                </p>
              </div>
              <el-select
                v-model="form.paymentProvider"
                class="payment-provider-select"
                :aria-label="t('independentSite.detail.selectPaymentProvider')"
              >
                <el-option value="SIMULATED" :label="t('independentSite.detail.simulatedPaymentProvider')" />
                <el-option value="STRIPE" label="Stripe（STRIPE）" :disabled="!stripeAvailable" />
              </el-select>
            </div>

            <div class="payment-row">
              <div>
                <h3>{{ t('independentSite.detail.previewSimulatedPayment') }}</h3>
                <p>{{ t('independentSite.detail.previewSimulatedPaymentHelp') }}</p>
              </div>
              <el-switch
                v-model="form.simulatedPaymentEnabled"
                :active-text="t('independentSite.detail.previewEnabled')"
                :inactive-text="t('independentSite.detail.previewDisabled')"
                :aria-label="t('independentSite.detail.previewSimulatedPaymentAria')"
              />
            </div>

            <div class="tab-actions">
              <el-button type="primary" :icon="Check" :loading="saving" @click="handleSavePayment">
                {{ t('independentSite.detail.savePaymentTheme') }}
              </el-button>
            </div>
          </el-form>
        </el-tab-pane>
      </el-tabs>
    </div>

    <el-drawer
      v-if="editorVisible"
      v-model="editorVisible"
      :title="t('independentSite.detail.editPageTitle', { title: editingPageTitle })"
      size="100%"
      direction="rtl"
      class="page-editor-drawer"
      destroy-on-close
      :before-close="handleEditorBeforeClose"
    >
      <div v-loading="editorLoading" class="editor-drawer-body">
        <IndependentSitePageEditor
          v-if="!editorLoading && editingPageId"
          :key="editingPageId"
          :schema="pageDraft"
          :theme-key="form.themeKey"
          @change="handleEditorChange"
        />
      </div>
      <template #footer>
        <div class="editor-footer">
          <div class="editor-ai-row">
            <el-input
              v-model="aiEditInstruction"
              maxlength="2000"
              autocomplete="off"
              :placeholder="t('independentSite.detail.aiEditPlaceholder')"
              :disabled="aiEditing || undoingAiEdit"
              @keyup.enter="handleAiEdit"
            >
              <template #prepend>{{ t('independentSite.detail.aiEdit') }}</template>
            </el-input>
            <el-button
              type="primary"
              :icon="MagicStick"
              :loading="aiEditing"
              :disabled="undoingAiEdit || savingDraft || publishing || editorLoading"
              @click="handleAiEdit"
            >
              {{ t('independentSite.detail.submitEdit') }}
            </el-button>
            <el-button
              :icon="RefreshLeft"
              :loading="undoingAiEdit"
              :disabled="!hasAiBackup || aiEditing || savingDraft || publishing"
              :title="t('independentSite.detail.undoAiTitle')"
              @click="handleUndoAiEdit"
            >
              {{ t('independentSite.detail.undoAi') }}
            </el-button>
          </div>
          <div class="editor-action-row">
            <el-tag v-if="hasUnsavedDraftChanges" type="warning" effect="plain">
              {{ t('independentSite.detail.unsavedChanges') }}
            </el-tag>
            <el-tag v-else-if="hasSavedDraftReady" type="warning" effect="plain">
              {{ t('independentSite.detail.unpublishedChanges') }}
            </el-tag>
            <el-tag v-else type="success" effect="plain">
              {{ t('independentSite.detail.matchesPublished') }}
            </el-tag>
            <span v-if="draftUpdatedAt" class="editor-meta">
              {{ t('independentSite.detail.draftSavedAt', { time: draftUpdatedAt }) }}
            </span>
            <span v-if="pagePublishedAt" class="editor-meta">
              {{ t('independentSite.detail.publishedAt', { time: pagePublishedAt }) }}
            </span>
            <span class="editor-footer-spacer"></span>
            <el-button
              type="primary"
              :icon="Check"
              :loading="savingDraft"
              :disabled="
                !pageDraft ||
                !hasUnsavedDraftChanges ||
                aiEditing ||
                undoingAiEdit ||
                publishing ||
                editorLoading
              "
              @click="handleSaveDraft"
            >
              {{ t('independentSite.detail.saveDraft') }}
            </el-button>
            <el-button
              type="success"
              :icon="Promotion"
              :loading="publishing"
              :disabled="
                !hasSavedDraftReady ||
                hasUnsavedDraftChanges ||
                aiEditing ||
                undoingAiEdit ||
                savingDraft ||
                editorLoading
              "
              @click="handlePublish"
            >
              {{ t('independentSite.detail.publishPage') }}
            </el-button>
          </div>
        </div>
      </template>
    </el-drawer>

    <el-drawer
      v-if="canvasEditorVisible"
      v-model="canvasEditorVisible"
      size="100%"
      direction="rtl"
      class="canvas-editor-drawer"
      destroy-on-close
      :with-header="false"
      :before-close="handleCanvasEditorBeforeClose"
    >
      <CanvasEditor
        v-if="canvasEditorPage && siteId"
        ref="canvasEditorRef"
        :key="canvasEditorPage.id"
        :site-id="siteId"
        :page-id="canvasEditorPage.id"
        :page-title="canvasEditorPage.title || canvasEditorPage.path"
        :page-path="canvasEditorPage.path"
        :page-type="String(canvasEditorPage.type)"
        :slug="savedSlug || form.slug"
        :theme-key="form.themeKey"
        :room-types="canvasEditorRoomTypes"
        :override-schema="canvasEditorOverride"
        @updated="handleCanvasEditorUpdated"
        @close="handleCanvasEditorClose"
      />
    </el-drawer>

    <el-drawer
      v-model="previewVisible"
      :title="t('independentSite.detail.previewTitle', { title: previewPage?.title ?? '' })"
      size="86%"
      direction="rtl"
      class="site-preview-drawer"
    >
      <div v-loading="previewLoading" class="preview-body">
        <div v-if="previewDetail" class="preview-toolbar">
          <el-radio-group
            v-model="previewSource"
            :aria-label="t('independentSite.detail.previewSourceAria')"
          >
            <el-radio-button value="draft" :disabled="!previewDraftSchema">
              {{ t('independentSite.detail.draft') }}
            </el-radio-button>
            <el-radio-button value="published" :disabled="!previewPublishedSchema">
              {{ t('independentSite.common.published') }}
            </el-radio-button>
          </el-radio-group>
          <span class="preview-hint">{{ t('independentSite.detail.previewNewTabHint') }}</span>
        </div>
        <div v-if="previewSchema" class="preview-shell">
          <IndependentSitePageRenderer :schema="previewSchema" :theme-key="form.themeKey" preview />
        </div>
        <el-empty
          v-else-if="!previewLoading"
          :description="t('independentSite.detail.noPreviewContent')"
        />
      </div>
      <template #footer>
        <div class="drawer-footer">
          <span v-if="previewSource === 'draft'">{{ t('independentSite.detail.draftPreviewing') }}</span>
          <span v-else>{{ t('independentSite.detail.publishedPreviewing') }}</span>
          <div>
            <el-button @click="previewVisible = false">{{ t('independentSite.common.close') }}</el-button>
            <el-button type="primary" :icon="TopRight" @click="openPreviewInNewTab">
              {{ t('independentSite.detail.openPreviewNewTab') }}
            </el-button>
          </div>
        </div>
      </template>
    </el-drawer>

    <el-dialog
      v-model="createPageDialogVisible"
      :title="t('independentSite.detail.createCustomPageTitle')"
      width="480px"
    >
      <el-form
        ref="createPageFormRef"
        :model="createPageForm"
        :rules="createPageRules"
        label-position="top"
      >
        <el-form-item :label="t('independentSite.detail.pagePath')" prop="path">
          <el-input
            v-model.trim="createPageForm.path"
            maxlength="255"
            autocomplete="off"
            :placeholder="t('independentSite.detail.pagePathPlaceholder')"
          />
          <div class="field-help">
            {{ t('independentSite.detail.pagePathHelp') }}
          </div>
        </el-form-item>
        <el-form-item :label="t('independentSite.detail.pageTitle')" prop="title">
          <el-input
            v-model.trim="createPageForm.title"
            maxlength="120"
            show-word-limit
            autocomplete="off"
            :placeholder="t('independentSite.detail.pageTitlePlaceholder')"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createPageDialogVisible = false">{{ t('independentSite.common.cancel') }}</el-button>
        <el-button type="primary" :loading="creatingPage" @click="handleCreatePage">
          {{ t('independentSite.detail.createPage') }}
        </el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="importDialogVisible"
      :title="t('independentSite.detail.importPageTitle')"
      width="560px"
    >
      <el-alert
        class="import-notice-alert"
        type="warning"
        :closable="false"
        show-icon
        :title="t('independentSite.detail.importNotice')"
      />
      <el-form
        ref="importFormRef"
        v-loading="importing"
        :element-loading-text="t('independentSite.detail.importLoading')"
        :model="importForm"
        :rules="importRules"
        label-position="top"
      >
        <el-form-item :label="t('independentSite.detail.pageUrl')" prop="url">
          <el-input
            v-model.trim="importForm.url"
            maxlength="2048"
            autocomplete="off"
            placeholder="https://example.com/about"
            :disabled="importing"
          />
          <div class="field-help">
            {{ t('independentSite.detail.importUrlHelp') }}
          </div>
        </el-form-item>

        <el-form-item :label="t('independentSite.detail.importMode')">
          <el-radio-group
            v-model="importForm.mode"
            :disabled="importing"
            :aria-label="t('independentSite.detail.importModeAria')"
            @change="handleImportModeChange"
          >
            <el-radio value="NEW_PAGE">{{ t('independentSite.detail.importNewPage') }}</el-radio>
            <el-radio value="OVERWRITE_DRAFT">{{ t('independentSite.detail.importOverwriteDraft') }}</el-radio>
          </el-radio-group>
        </el-form-item>

        <template v-if="importForm.mode === 'NEW_PAGE'">
          <el-form-item :label="t('independentSite.detail.pagePath')" prop="path">
            <el-input
              v-model.trim="importForm.path"
              maxlength="255"
              autocomplete="off"
              :placeholder="t('independentSite.detail.pagePathPlaceholder')"
              :disabled="importing"
            />
            <div class="field-help">
              {{ t('independentSite.detail.pagePathHelp') }}
            </div>
          </el-form-item>
          <el-form-item :label="t('independentSite.detail.pageTitle')" prop="title">
            <el-input
              v-model.trim="importForm.title"
              maxlength="120"
              show-word-limit
              autocomplete="off"
              :placeholder="t('independentSite.detail.pageTitlePlaceholder')"
              :disabled="importing"
            />
          </el-form-item>
        </template>

        <template v-else>
          <el-form-item :label="t('independentSite.detail.targetPage')" prop="pageId">
            <el-select
              v-model="importForm.pageId"
              filterable
              :placeholder="t('independentSite.detail.selectOverwritePage')"
              :disabled="importing"
              style="width: 100%"
            >
              <el-option
                v-for="page in pages"
                :key="page.id"
                :value="page.id"
                :label="`${page.title}（${page.path}）`"
              />
            </el-select>
            <el-alert
              class="import-overwrite-alert"
              type="warning"
              :closable="false"
              show-icon
              :title="t('independentSite.detail.importOverwriteNotice')"
            />
          </el-form-item>
        </template>
      </el-form>
      <template #footer>
        <el-button :disabled="importing" @click="importDialogVisible = false">
          {{ t('independentSite.common.cancel') }}
        </el-button>
        <el-button type="primary" :icon="Link" :loading="importing" @click="handleImportPage">
          {{ importing ? t('independentSite.detail.importLoading') : t('independentSite.detail.startImport') }}
        </el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="renameDialogVisible"
      :title="t('independentSite.detail.renameSeoTitle')"
      width="480px"
    >
      <div v-loading="renameLoading">
        <el-form ref="renameFormRef" :model="renameForm" :rules="renameRules" label-position="top">
          <el-form-item :label="t('independentSite.detail.pageTitle')" prop="title">
            <el-input
              v-model.trim="renameForm.title"
              maxlength="120"
              show-word-limit
              autocomplete="off"
            />
          </el-form-item>
          <el-form-item :label="t('independentSite.detail.seoDescription')" prop="seoDescription">
            <el-input
              v-model="renameForm.seoDescription"
              type="textarea"
              :rows="3"
              maxlength="300"
              show-word-limit
              resize="vertical"
              :placeholder="t('independentSite.detail.seoDescriptionPlaceholder')"
            />
          </el-form-item>
        </el-form>
      </div>
      <template #footer>
        <el-button @click="renameDialogVisible = false">{{ t('independentSite.common.cancel') }}</el-button>
        <el-button
          type="primary"
          :loading="renaming"
          :disabled="renameLoading"
          @click="handleRenamePage"
        >
          {{ t('independentSite.common.save') }}
        </el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="generateDialogVisible"
      :title="t('independentSite.detail.generatePageTitle', { title: generatePage?.title ?? '' })"
      width="560px"
    >
      <el-input
        v-model="generatePrompt"
        type="textarea"
        :rows="6"
        maxlength="1000"
        show-word-limit
        resize="vertical"
        :placeholder="t('independentSite.detail.generatePlaceholder')"
      />
      <el-alert
        class="ai-boundary-alert"
        type="info"
        :closable="false"
        show-icon
        :title="t('independentSite.detail.safetyBoundary')"
        :description="t('independentSite.detail.safetyBoundaryDescription')"
      />
      <template #footer>
        <el-button @click="generateDialogVisible = false">
          {{ t('independentSite.common.cancel') }}
        </el-button>
        <el-button type="primary" :icon="MagicStick" :loading="generating" @click="handleGenerate">
          {{ t('independentSite.detail.generateDraft') }}
        </el-button>
      </template>
    </el-dialog>

    <StripeSettingsDialog v-model="stripeDialogVisible" @saved="refreshStripeAvailability" />
  </div>
</template>

<style scoped>
.independent-site-detail {
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

.header-left {
  display: flex;
  gap: 16px;
  align-items: center;
  min-width: 0;
}

.page-header h1 {
  margin: 6px 0;
  color: #173c36;
  font-size: 30px;
  letter-spacing: -0.03em;
}

.page-header p {
  margin: 0;
  color: #69716f;
  font-family: 'SFMono-Regular', Consolas, monospace;
  font-size: 13px;
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
.tabs-card {
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

.tabs-card {
  padding: 10px 28px 28px;
  border: 1px solid #e5e9e7;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.94);
  box-shadow: 0 12px 40px rgba(36, 59, 54, 0.045);
}

.tab-form {
  max-width: 980px;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 18px 26px;
}

.field-help,
.ratio-explanation {
  margin-top: 8px;
  color: #7b8381;
  font-size: 12px;
  line-height: 1.55;
}

.ratio-explanation {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.ratio-explanation strong {
  color: #28695e;
  font-size: 13px;
}

.tab-actions {
  display: flex;
  gap: 10px;
  margin-top: 22px;
}

.tab-toolbar {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 14px;
}

.toolbar-hint {
  margin: 0;
  color: #7b8381;
  font-size: 12px;
  line-height: 1.6;
}

.toolbar-actions {
  display: flex;
  gap: 10px;
}

.page-table {
  width: 100%;
}

.page-title {
  font-weight: 700;
}

.page-path {
  padding: 2px 6px;
  border-radius: 6px;
  color: #357d70;
  background: #eef4f2;
  font-size: 12px;
}

.page-sub-meta {
  margin-top: 4px;
  color: #8b9290;
  font-size: 12px;
}

.tab-inline-alert {
  margin-bottom: 18px;
}

.card-heading {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 20px;
  margin-bottom: 18px;
}

.card-heading h2 {
  margin: 0;
  color: #193f38;
  font-size: 18px;
  letter-spacing: -0.015em;
}

.selection-summary {
  color: #357d70;
  font-size: 13px;
  font-weight: 700;
}

.room-type-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
  width: 100%;
}

.room-type-card {
  display: flex;
  flex-direction: column;
  gap: 8px;
  box-sizing: border-box;
  min-height: 92px;
  padding: 16px;
  border: 1px solid #e2e7e5;
  border-radius: 13px;
  transition:
    border-color 0.2s,
    box-shadow 0.2s,
    background-color 0.2s;
}

.room-type-card:hover,
.room-type-card.is-selected {
  border-color: #4d9386;
  background: #f4faf8;
  box-shadow: 0 8px 22px rgba(41, 105, 94, 0.08);
}

.room-type-card > span {
  padding-left: 24px;
  color: #7b8381;
  font-size: 12px;
}

.room-type-name {
  color: #26302e;
  font-weight: 700;
}

.specific-room-area {
  margin-top: 26px;
  padding-top: 24px;
  border-top: 1px solid #edf0ef;
}

.specific-room-list {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  margin-top: 14px;
}

.specific-room-group {
  padding: 14px 16px;
  border-radius: 12px;
  background: #f7f9f8;
}

.specific-room-group > strong {
  display: block;
  margin-bottom: 10px;
  color: #36534d;
  font-size: 13px;
}

.subsection-heading h3 {
  margin: 0 0 5px;
  font-size: 15px;
}

.subsection-heading p {
  margin: 0;
  color: #7d766c;
  font-size: 12px;
  line-height: 1.55;
}

.empty-inline {
  color: #8b9290;
  font-size: 12px;
}

.theme-form-item {
  margin-top: 6px;
}

.theme-picker {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
  width: 100%;
}

.theme-card {
  display: flex;
  flex-direction: column;
  gap: 10px;
  align-items: flex-start;
  padding: 16px;
  border: 1px solid #e2e7e5;
  border-radius: 13px;
  background: #fff;
  cursor: pointer;
  transition:
    border-color 0.2s,
    box-shadow 0.2s,
    background-color 0.2s;
}

.theme-card:hover,
.theme-card.is-active {
  border-color: #4d9386;
  background: #f4faf8;
  box-shadow: 0 8px 22px rgba(41, 105, 94, 0.08);
}

.theme-swatches {
  display: flex;
  gap: 6px;
}

.theme-swatches i {
  width: 22px;
  height: 22px;
  border: 1px solid rgba(0, 0, 0, 0.08);
  border-radius: 7px;
}

.theme-card-name {
  color: #26302e;
  font-size: 13px;
  font-weight: 700;
}

.payment-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
  margin-top: 18px;
  padding: 20px;
  border: 1px solid #f0d9b9;
  border-radius: 14px;
  background: #fffaf2;
}

.payment-provider-row {
  border-color: #e2e7e5;
  background: #f7f9f8;
}

.payment-row h3 {
  margin: 0 0 5px;
  font-size: 15px;
}

.payment-row p {
  margin: 0;
  color: #7d766c;
  font-size: 12px;
  line-height: 1.55;
}

.payment-provider-select {
  width: 240px;
}

.editor-drawer-body {
  min-height: 320px;
}

/* 画布编辑器抽屉：无头部、body 去内边距，让编辑器工具条贴边铺满 */
.canvas-editor-drawer :deep(.el-drawer__body) {
  padding: 0;
}

.editor-footer {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.editor-ai-row {
  display: flex;
  gap: 10px;
  align-items: center;
}

.editor-ai-row .el-input {
  flex: 1;
}

.editor-action-row {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  align-items: center;
}

.editor-meta {
  color: #929896;
  font-size: 11px;
}

.editor-footer-spacer {
  flex: 1;
}

.preview-body {
  min-height: 320px;
}

.preview-toolbar {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  align-items: center;
  margin-bottom: 14px;
}

.preview-hint {
  color: #8b9290;
  font-size: 12px;
}

.preview-shell {
  overflow: hidden;
  border: 1px solid #e0e5e3;
  border-radius: 18px;
  background: #f5f1e8;
}

.drawer-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
  color: #7a8280;
  font-size: 13px;
}

.ai-boundary-alert {
  margin-top: 14px;
}

.import-notice-alert {
  margin-bottom: 18px;
}

.import-overwrite-alert {
  margin-top: 10px;
}

@media (max-width: 980px) {
  .room-type-grid,
  .theme-picker {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 720px) {
  .independent-site-detail {
    padding: 4px 12px 28px;
  }

  .page-header,
  .card-heading,
  .payment-row,
  .drawer-footer {
    align-items: stretch;
    flex-direction: column;
  }

  .header-left,
  .header-actions {
    justify-content: flex-start;
  }

  .form-grid,
  .room-type-grid,
  .theme-picker,
  .specific-room-list {
    grid-template-columns: 1fr;
  }

  .tabs-card {
    padding: 6px 16px 20px;
    border-radius: 14px;
  }

  .payment-provider-select {
    width: 100%;
  }
}
</style>
