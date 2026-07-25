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
  INDEPENDENT_SITE_THEME_KEYS,
  INDEPENDENT_SITE_THEME_LABELS,
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
const { locale } = useI18n()
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

const priceAdjustmentValidator: FormItemRule['validator'] = (_rule, value, callback) => {
  if (typeof value !== 'number' || !Number.isFinite(value)) {
    callback(new Error('请输入独立站价格调整比例'))
    return
  }
  if (value < -99.99 || value > 1000) {
    callback(new Error('价格调整比例需在 -99.99% 至 1000% 之间'))
    return
  }
  callback()
}

const validatePagePath = (path: string): string => {
  if (!path) {
    return '请输入页面路径'
  }
  if (path.length > 255) {
    return '页面路径不能超过 255 个字符'
  }
  if (!/^\/[a-z0-9]+(?:[-/][a-z0-9]+)*$/.test(path)) {
    return '路径需以 / 开头，由小写字母、数字、连字符组成，如 /about、/rooms/king'
  }
  return ''
}

const pagePathValidator: FormItemRule['validator'] = (_rule, value, callback) => {
  const message = validatePagePath(String(value || '').trim())
  callback(message ? new Error(message) : undefined)
}

const basicRules: FormRules = {
  name: [
    { required: true, message: '请输入站点名称', trigger: 'blur' },
    { min: 1, max: 120, message: '站点名称需为 1–120 个字符', trigger: 'blur' },
  ],
  slug: [{ required: true, validator: slugValidator, trigger: ['blur', 'change'] }],
}

const paymentRules: FormRules = {
  defaultPricePlanId: [{ required: true, message: '请选择独立站基准价格计划', trigger: 'change' }],
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
    { required: true, message: '请输入页面标题', trigger: 'blur' },
    { min: 1, max: 120, message: '页面标题需为 1–120 个字符', trigger: 'blur' },
  ],
}

const renameRules: FormRules = {
  title: [
    { required: true, message: '请输入页面标题', trigger: 'blur' },
    { min: 1, max: 120, message: '页面标题需为 1–120 个字符', trigger: 'blur' },
  ],
  seoDescription: [{ max: 300, message: 'SEO 描述不能超过 300 个字符', trigger: 'blur' }],
}

const importUrlValidator: FormItemRule['validator'] = (_rule, value, callback) => {
  const url = String(value || '').trim()
  if (!url) {
    callback(new Error('请输入要导入的页面 URL'))
    return
  }
  if (url.length > 2048) {
    callback(new Error('URL 不能超过 2048 个字符'))
    return
  }
  if (!/^https?:\/\/\S+$/i.test(url)) {
    callback(new Error('仅支持 http/https 协议的完整 URL，例如 https://example.com/about'))
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
    callback(new Error('请输入页面标题'))
    return
  }
  if (title.length > 120) {
    callback(new Error('页面标题需为 1–120 个字符'))
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
    callback(new Error('请选择要覆盖草稿的页面'))
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
    return '请填写 -99.99% 至 1000% 之间的价格调整比例'
  }
  const finalRatio = 100 + adjustment
  if (adjustment > 0) {
    return `标准价上浮 ${formatPercent(adjustment)}%，售价为标准价的 ${formatPercent(finalRatio)}%`
  }
  if (adjustment < 0) {
    return `标准价下调 ${formatPercent(Math.abs(adjustment))}%，售价为标准价的 ${formatPercent(finalRatio)}%`
  }
  return '与标准价一致，售价为标准价的 100%'
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
    savedDraftSchema.value ?? publishedSchema.value ?? createEmptyIndependentSiteSchema()
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
      ElMessage.error(response.message || '刷新页面列表失败')
    }
  } catch (error) {
    if (!silent) {
      ElMessage.error(getErrorMessage(error, '刷新页面列表失败'))
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
    loadError.value = '无效的站点 ID，请从站点列表重新进入'
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
        ? getErrorMessage(siteResult.reason, '加载独立站配置失败')
        : siteResult.value.message || '加载独立站配置失败'
  }

  if (pagesResult.status === 'fulfilled' && pagesResult.value.success) {
    pages.value = Array.isArray(pagesResult.value.data) ? pagesResult.value.data : []
  } else if (!loadError.value) {
    loadError.value =
      pagesResult.status === 'rejected'
        ? getErrorMessage(pagesResult.reason, '加载页面列表失败')
        : pagesResult.value.message || '加载页面列表失败'
  }

  const optionErrors: string[] = []
  if (pricePlanResult.status === 'fulfilled' && pricePlanResult.value.success) {
    pricePlans.value = (pricePlanResult.value.data || []).filter((plan) => Boolean(plan.id))
  } else {
    optionErrors.push(
      pricePlanResult.status === 'rejected'
        ? getErrorMessage(pricePlanResult.reason, '价格计划加载失败')
        : pricePlanResult.value.message || '价格计划加载失败',
    )
  }

  if (roomTypeResult.status === 'fulfilled' && roomTypeResult.value.success) {
    roomTypes.value = normalizeRoomTypes(roomTypeResult.value.data)
  } else {
    optionErrors.push(
      roomTypeResult.status === 'rejected'
        ? getErrorMessage(roomTypeResult.reason, '房型与房间加载失败')
        : roomTypeResult.value.message || '房型与房间加载失败',
    )
  }
  optionsError.value = optionErrors.join('；')

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
    ElMessage.warning(getErrorMessage(error, 'Stripe 可用状态刷新失败，请手动刷新页面'))
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
    throw new Error('请输入 -99.99% 至 1000% 之间的价格调整比例')
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

const persistSettings = async (successMessage = '独立站配置已保存'): Promise<boolean> => {
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
      throw new Error(response.message || '保存独立站配置失败')
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
    // 启用门槛、slug 冲突等错误直接展示后端 message
    ElMessage.error(getErrorMessage(error, '保存独立站配置失败'))
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
    ElMessage.warning('启用独立站前请先在「发布范围」页签选择至少一个房型')
    activeTab.value = 'publication'
    return
  }
  await persistSettings()
}

const handleSavePublication = () => persistSettings('发布范围已保存')

const handleSavePayment = async () => {
  const valid = await paymentFormRef.value?.validate().catch(() => false)
  if (!valid) {
    return
  }
  await persistSettings('支付与主题配置已保存')
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
  return roomType ? roomType.name : `房型 #${roomTypeId}`
}

const pageTypeLabel = (page: IndependentSitePageSummary) => {
  if (page.type === 'HOME') {
    return '首页'
  }
  if (page.type === 'ROOM_DETAIL') {
    return '房型页'
  }
  return '自定义'
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
      throw new Error(response.message || '更新页面启用状态失败')
    }
    page.enabled = Boolean(response.data.enabled)
    ElMessage.success(value ? '页面已启用' : '页面已禁用')
  } catch (error) {
    ElMessage.error(getErrorMessage(error, '更新页面启用状态失败'))
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
      throw new Error(response.message || '创建自定义页失败')
    }
    createPageDialogVisible.value = false
    ElMessage.success('自定义页已创建，可编辑内容或用 AI 生成草稿')
    await refreshPages()
  } catch (error) {
    ElMessage.error(getErrorMessage(error, '创建自定义页失败'))
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
    ElMessage.error('加载页面详情失败，请稍后重试')
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
      throw new Error(response.message || '保存页面信息失败')
    }
    renameDialogVisible.value = false
    ElMessage.success('页面信息已保存')
    await refreshPages(true)
  } catch (error) {
    ElMessage.error(getErrorMessage(error, '保存页面信息失败'))
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
      `确定删除页面「${page.title || page.path}」吗？该页面的草稿与已发布内容会一并删除，此操作不可恢复。`,
      '删除页面',
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

  deletingPageId.value = page.id
  try {
    const response = await deleteIndependentSitePage(siteId.value, page.id)
    if (!response.success) {
      throw new Error(response.message || '删除页面失败')
    }
    ElMessage.success('页面已删除')
    pages.value = pages.value.filter((item) => item.id !== page.id)
  } catch (error) {
    ElMessage.error(getErrorMessage(error, '删除页面失败'))
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
      throw new Error(response.message || '生成房型页失败')
    }
    const result = response.data
    if (Array.isArray(result.pages)) {
      pages.value = result.pages
    }
    const skipped = Array.isArray(result.skipped) ? result.skipped : []
    const skippedReasons = skipped
      .map(
        (item) => `${roomTypeName(item.roomTypeId) || `房型 #${item.roomTypeId}`}：${item.reason}`,
      )
      .join('；')
    ElNotification({
      title: '房型页生成完成',
      message: `新生成 ${result.generated} 个，刷新 ${result.refreshed} 个，跳过 ${skipped.length} 个${
        skippedReasons ? `（${skippedReasons}）` : ''
      }。生成内容为草稿，发布后才会上线。`,
      type: skipped.length > 0 ? 'warning' : 'success',
      duration: 8000,
    })
  } catch (error) {
    ElMessage.error(getErrorMessage(error, '生成房型页失败'))
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
      throw new Error(response.message || '从 URL 导入失败')
    }
    const importedPage = response.data
    importDialogVisible.value = false
    ElMessage.success('已从 URL 生成页面草稿，请在编辑器中检查后保存发布')
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
    // 400 URL_NOT_ALLOWED / 409 路径冲突 / 422 抓取失败 / 429 限流等直接展示后端 message
    ElMessage.error(getErrorMessage(error, '从 URL 导入失败，请稍后重试'))
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
      throw new Error(response.message || '加载页面详情失败')
    }
    applyPageDetail(response.data)
    if (overrideDraft) {
      pageDraft.value = overrideDraft
    }
  } catch (error) {
    editorVisible.value = false
    ElMessage.error(getErrorMessage(error, '加载页面详情失败'))
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
  ElMessageBox.confirm('当前页面有未保存的修改，关闭后修改将丢失，确定关闭吗？', '关闭编辑器', {
    confirmButtonText: '关闭',
    cancelButtonText: '继续编辑',
    type: 'warning',
  })
    .then(() => done())
    .catch(() => {})
}

const handleSaveDraft = async (): Promise<boolean> => {
  const normalizedSchema = normalizeIndependentSiteSchema(pageDraft.value)
  if (!normalizedSchema || normalizedSchema.sections.length === 0) {
    ElMessage.warning('请先编辑或生成有效的页面内容')
    return false
  }
  if (!siteId.value || !editingPageId.value) {
    ElMessage.error('未选择页面，无法保存草稿')
    return false
  }
  if (!hasUnsavedDraftChanges.value) {
    ElMessage.info('当前草稿已保存')
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
      throw new Error(response.message || '保存页面草稿失败')
    }
    const savedSchema = normalizeIndependentSiteSchema(response.data.draftSchema)
    if (!savedSchema) {
      throw new Error('服务端返回的页面草稿不符合受控 schema')
    }
    const savedDraftVersion = Number(response.data.draftVersion)
    if (!Number.isInteger(savedDraftVersion) || savedDraftVersion < 0) {
      throw new Error('服务端返回的草稿版本无效')
    }
    pageDraft.value = savedSchema
    savedDraftSchema.value = savedSchema
    draftUpdatedAt.value = response.data.draftUpdatedAt || null
    draftVersion.value = savedDraftVersion
    ElMessage.success('页面草稿已保存，尚未发布')
    void refreshPages(true)
    return true
  } catch (error) {
    if (isDraftVersionConflict(error)) {
      ElMessage.error('页面草稿已被其他操作更新，已重新加载最新草稿，请确认后重试')
      await reloadEditingPageDetail()
      return false
    }
    ElMessage.error(getErrorMessage(error, '保存页面草稿失败'))
    return false
  } finally {
    savingDraft.value = false
  }
}

const handlePublish = async () => {
  if (hasUnsavedDraftChanges.value) {
    ElMessage.warning('页面内容尚未保存，请先保存草稿再发布')
    return
  }
  if (!hasSavedDraftReady.value || draftVersion.value === null) {
    ElMessage.warning('当前没有可发布的已保存草稿')
    return
  }
  if (!siteId.value || !editingPageId.value) {
    ElMessage.error('未选择页面，无法发布页面')
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
      throw new Error(response.message || '发布页面失败')
    }
    applyPageDetail(response.data)
    ElMessage.success('页面已发布')
    void refreshPages(true)
  } catch (error) {
    if (isDraftVersionConflict(error)) {
      ElMessage.error('页面草稿版本已变化，已重新加载最新页面数据，请确认后重试')
      await reloadEditingPageDetail()
      return
    }
    ElMessage.error(getErrorMessage(error, '发布页面失败'))
  } finally {
    publishing.value = false
  }
}

const handleAiEdit = async () => {
  const instruction = aiEditInstruction.value.trim()
  if (!instruction) {
    ElMessage.warning('请输入要 AI 修改的内容')
    return
  }
  if (instruction.length > 2000) {
    ElMessage.warning('修改指令不能超过 2000 个字符')
    return
  }
  if (!siteId.value || !editingPageId.value) {
    ElMessage.warning('请先选择要修改的页面')
    return
  }
  if (hasUnsavedDraftChanges.value) {
    ElMessage.warning('当前有未保存的编辑，请先保存草稿再使用 AI 局部修改')
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
      throw new Error(response.message || 'AI 局部修改失败')
    }
    applyPageDetail(response.data)
    aiEditInstruction.value = ''
    ElMessage.success('AI 已按指令更新草稿，可继续编辑或保存发布')
    void refreshPages(true)
  } catch (error) {
    if (sequence !== loadSequence) {
      return
    }
    ElMessage.error(getErrorMessage(error, 'AI 局部修改失败，请稍后重试'))
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
      throw new Error(response.message || '撤销 AI 修改失败')
    }
    applyPageDetail(response.data)
    ElMessage.success('已撤销最近一次 AI 修改')
    void refreshPages(true)
  } catch (error) {
    if (sequence !== loadSequence) {
      return
    }
    ElMessage.error(getErrorMessage(error, '撤销 AI 修改失败'))
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
    ElMessage.warning('请至少输入 10 个字符，说明期望的页面风格和内容')
    return
  }
  if (prompt.length > 1000) {
    ElMessage.warning('提示词不能超过 1000 个字符')
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
      throw new Error(response.message || 'AI 页面生成失败')
    }
    if (!response.data.publishable) {
      throw new Error('AI 返回的页面草稿不可发布，已保留当前页面内容')
    }
    const rawSchema = (response.data as { pageSchema?: unknown }).pageSchema ?? response.data
    if (isCanvasPage(targetPage)) {
      const canvasSchema = normalizeCanvasSchema(rawSchema)
      if (!canvasSchema) {
        throw new Error('AI 返回的页面配置不符合画布契约，已保留当前页面内容')
      }
      generateDialogVisible.value = false
      ElMessage.success('AI 草稿已生成并载入画布编辑器')
      openCanvasPageEditor(targetPage, canvasSchema)
      return
    }
    const normalizedSchema = normalizeIndependentSiteSchema(rawSchema)
    if (!normalizedSchema || normalizedSchema.sections.length === 0) {
      throw new Error('AI 返回的页面配置不符合受控 schema，已保留当前页面内容')
    }
    generateDialogVisible.value = false
    ElMessage.success('AI 草稿已生成并载入编辑器，请检查后保存草稿')
    await openPageEditor(targetPage, normalizedSchema)
  } catch (error) {
    ElMessage.error(getErrorMessage(error, 'AI 页面生成失败，已保留当前页面内容'))
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
      ElMessage.warning('请先保存站点链接后缀，再打开预览')
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
      throw new Error(response.message || '加载页面预览失败')
    }
    previewDetail.value = response.data
    previewSource.value = normalizeIndependentSiteSchema(response.data.draftSchema)
      ? 'draft'
      : 'published'
  } catch (error) {
    previewVisible.value = false
    ElMessage.error(getErrorMessage(error, '加载页面预览失败'))
  } finally {
    previewLoading.value = false
  }
}

const openPreviewInNewTab = () => {
  if (!previewPublicUrl.value) {
    ElMessage.warning('请先保存站点链接后缀，再打开新标签预览')
    return
  }
  window.open(previewPublicUrl.value, '_blank', 'noopener,noreferrer')
}

const copyPublicUrl = async () => {
  if (!publicUrl.value) {
    ElMessage.warning('请先填写公开链接后缀')
    return
  }
  try {
    await navigator.clipboard.writeText(publicUrl.value)
    ElMessage.success('公开链接已复制')
  } catch {
    ElMessage.error('复制失败，请手动复制链接')
  }
}

const openPublicSite = () => {
  if (!canOpenPublicSite.value) {
    ElMessage.warning('请先保存并启用站点；未发布的草稿可使用页面预览查看')
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
        <el-button :icon="ArrowLeft" @click="goBack">返回列表</el-button>
        <div>
          <div class="header-eyebrow">
            <el-icon><Promotion /></el-icon>
            DIRECT BOOKING
          </div>
          <h1>{{ form.name || '独立站' }}</h1>
          <p>/stay/{{ savedSlug || form.slug || '…' }}</p>
        </div>
      </div>
      <div class="header-actions">
        <el-tag :type="savedEnabled ? 'success' : 'info'" effect="plain">
          {{ savedEnabled ? '已启用' : '未启用' }}
        </el-tag>
        <el-tag v-if="formDirty" type="warning" effect="plain">有未保存修改</el-tag>
        <el-button :icon="CopyDocument" :disabled="Boolean(loadError)" @click="copyPublicUrl">
          复制公开链接
        </el-button>
        <el-button :icon="TopRight" :disabled="!canOpenPublicSite" @click="openPublicSite">
          打开公开页
        </el-button>
      </div>
    </header>

    <el-alert
      v-if="loadError"
      class="page-alert"
      type="error"
      :title="loadError"
      description="站点配置未加载，页面不会显示虚假的保存成功。请重试或返回列表。"
      show-icon
      :closable="false"
    >
      <template #default>
        <el-button class="alert-action" size="small" :icon="Refresh" @click="loadPage">
          重新加载
        </el-button>
      </template>
    </el-alert>

    <el-alert
      v-if="optionsError"
      class="page-alert"
      type="warning"
      :title="optionsError"
      description="相关选项不可用时不能完成发布，请检查后端接口后重试。"
      show-icon
      :closable="false"
    />

    <div v-if="!loadError" class="tabs-card">
      <el-tabs v-model="activeTab" class="site-tabs">
        <el-tab-pane label="基本配置" name="basic">
          <el-form
            ref="basicFormRef"
            :model="form"
            :rules="basicRules"
            label-position="top"
            class="tab-form"
          >
            <div class="form-grid">
              <el-form-item label="站点名称" prop="name">
                <el-input
                  v-model.trim="form.name"
                  maxlength="120"
                  show-word-limit
                  autocomplete="off"
                  placeholder="例如：海边民宿主站"
                />
                <div class="field-help">用于管理端区分多个站点；公开页仍展示门店名称。</div>
              </el-form-item>

              <el-form-item label="公开链接后缀" prop="slug">
                <el-input
                  v-model.trim="form.slug"
                  maxlength="63"
                  show-word-limit
                  autocomplete="off"
                  placeholder="例如：seaside-house"
                >
                  <template #prepend>/stay/</template>
                </el-input>
                <div class="field-help">
                  使用小写字母、数字和连字符；全局唯一，冲突会由服务端拒绝；修改后旧链接可能失效。
                </div>
              </el-form-item>

              <el-form-item label="系统公开链接">
                <el-input :model-value="publicUrl" readonly placeholder="填写链接后缀后生成">
                  <template #append>
                    <el-button
                      :icon="CopyDocument"
                      aria-label="复制公开链接"
                      @click="copyPublicUrl"
                    />
                  </template>
                </el-input>
                <div class="field-help">
                  公开页面始终展示已发布内容；草稿通过页面预览或新标签预览查看。
                </div>
              </el-form-item>
            </div>

            <div class="payment-row">
              <div>
                <h3>启用独立站</h3>
                <p>启用要求发布范围非空且首页已发布；未满足时服务端会拒绝并返回具体原因。</p>
              </div>
              <el-switch
                v-model="form.enabled"
                inline-prompt
                active-text="启用"
                inactive-text="停用"
                aria-label="启用或停用独立站"
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
                保存基本配置
              </el-button>
            </div>
          </el-form>
        </el-tab-pane>

        <el-tab-pane label="页面" name="pages">
          <div class="tab-toolbar">
            <p class="toolbar-hint">
              首页随站点自动创建且不可删除；自定义页路径需以 / 开头，如 /about、/rooms/king。
            </p>
            <div class="toolbar-actions">
              <el-button :icon="Plus" @click="openCreatePageDialog">新建自定义页</el-button>
              <el-button :icon="Link" @click="openImportDialog">从 URL 导入</el-button>
              <el-button
                type="primary"
                :icon="MagicStick"
                :loading="generatingRoomPages"
                @click="handleGenerateRoomPages"
              >
                生成房型页
              </el-button>
            </div>
          </div>

          <el-table v-loading="pagesLoading" :data="pages" row-key="id" class="page-table">
            <el-table-column label="标题" min-width="160">
              <template #default="{ row }">
                <span class="page-title">{{ row.title }}</span>
              </template>
            </el-table-column>
            <el-table-column label="路径" min-width="140">
              <template #default="{ row }">
                <code class="page-path">{{ row.path }}</code>
              </template>
            </el-table-column>
            <el-table-column label="类型" width="130">
              <template #default="{ row }">
                <el-tag :type="pageTypeTagType(row)" effect="plain">{{
                  pageTypeLabel(row)
                }}</el-tag>
                <div v-if="row.type === 'ROOM_DETAIL'" class="page-sub-meta">
                  {{ roomTypeName(row.roomTypeId) }}
                </div>
              </template>
            </el-table-column>
            <el-table-column label="启用" width="80" align="center">
              <template #default="{ row }">
                <el-switch
                  :model-value="row.enabled"
                  :loading="togglingPageId === row.id"
                  :aria-label="`启用或禁用页面 ${row.title}`"
                  @change="
                    (value: string | number | boolean) =>
                      handlePageEnabledChange(row, Boolean(value))
                  "
                />
              </template>
            </el-table-column>
            <el-table-column label="发布状态" width="130">
              <template #default="{ row }">
                <el-tag v-if="!row.publishedAt" type="info" effect="plain">未发布</el-tag>
                <el-tag v-else-if="row.hasUnpublishedChanges" type="warning" effect="plain">
                  有未发布变更
                </el-tag>
                <el-tag v-else type="success" effect="plain">已发布</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="330" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" size="small" @click="openPageEditor(row)">
                  编辑内容
                </el-button>
                <el-button link type="primary" size="small" @click="openGenerateDialog(row)">
                  AI 整页生成
                </el-button>
                <el-button link type="primary" size="small" @click="openPagePreview(row)">
                  预览
                </el-button>
                <el-button link type="primary" size="small" @click="openRenameDialog(row)">
                  重命名/SEO
                </el-button>
                <el-button
                  link
                  type="danger"
                  size="small"
                  :disabled="row.type === 'HOME'"
                  :loading="deletingPageId === row.id"
                  :title="row.type === 'HOME' ? '首页不可删除' : ''"
                  @click="handleDeletePage(row)"
                >
                  删除
                </el-button>
              </template>
            </el-table-column>
            <template #empty>
              <el-empty description="当前站点还没有页面，请新建自定义页或生成房型页" />
            </template>
          </el-table>
        </el-tab-pane>

        <el-tab-pane label="发布范围" name="publication">
          <el-alert
            class="tab-inline-alert"
            type="warning"
            show-icon
            :closable="false"
            title="从发布范围移除房型将自动禁用其房型详情页"
            description="公开报价只会返回已发布范围；最终可售仍以服务端实时房态为准。"
          />
          <div class="card-heading">
            <div>
              <h2>发布房型与房间</h2>
            </div>
            <span class="selection-summary">
              {{ form.publishedRoomTypeIds.length }} 个房型 ·
              {{ form.publishedRoomIds.length }} 个指定房间
            </span>
          </div>

          <el-empty
            v-if="roomTypes.length === 0"
            description="暂无可发布房型，请先创建房型或检查接口"
          />
          <el-checkbox-group
            v-else
            v-model="form.publishedRoomTypeIds"
            class="room-type-grid"
            aria-label="选择发布房型"
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
                >{{ roomType.rooms.length }} 间房 · 最多 {{ roomType.maxGuests || '—' }} 人</span
              >
            </div>
          </el-checkbox-group>

          <div v-if="selectedRoomTypes.length" class="specific-room-area">
            <div class="subsection-heading">
              <h3>指定物理房间（可选）</h3>
              <p>不勾选时按已发布房型自动分房；如需限制到具体房间，可在此勾选。</p>
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
                  :aria-label="`${roomType.name}指定房间`"
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
                <span v-else class="empty-inline">该房型暂无物理房间</span>
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
              保存发布范围
            </el-button>
          </div>
        </el-tab-pane>

        <el-tab-pane label="支付与主题" name="payment">
          <el-form
            ref="paymentFormRef"
            :model="form"
            :rules="paymentRules"
            label-position="top"
            class="tab-form"
          >
            <div class="form-grid">
              <el-form-item label="独立站基准价格计划" prop="defaultPricePlanId">
                <el-select
                  v-model="form.defaultPricePlanId"
                  filterable
                  placeholder="选择实际价格计划"
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
                <div class="field-help">必须显式选择，系统不会按“标准定价”等名称自动推断。</div>
              </el-form-item>

              <el-form-item label="独立站价格比例" prop="priceAdjustmentValue">
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
                  <span>输入 10 保存为加价 10%，不是把比例保存为 110。</span>
                </div>
              </el-form-item>
            </div>

            <el-form-item label="站点主题" class="theme-form-item">
              <div class="theme-picker" role="radiogroup" aria-label="选择站点主题">
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
                  <span class="theme-card-name">{{ INDEPENDENT_SITE_THEME_LABELS[themeKey] }}</span>
                </button>
              </div>
              <div class="field-help">
                主题决定公开页的配色、字体与图片风格，页面预览会即时应用。
              </div>
            </el-form-item>

            <div class="payment-row payment-provider-row">
              <div>
                <h3>支付渠道</h3>
                <p>模拟支付不产生真实扣款；配置 Stripe 密钥后可切换为真实收款。</p>
                <p v-if="!stripeAvailable" class="field-help">
                  请先在独立站列表页 Stripe 设置中配置门店密钥。
                  <el-button link type="primary" size="small" @click="stripeDialogVisible = true">
                    去配置
                  </el-button>
                </p>
              </div>
              <el-select
                v-model="form.paymentProvider"
                class="payment-provider-select"
                aria-label="选择支付渠道"
              >
                <el-option value="SIMULATED" label="模拟支付（SIMULATED）" />
                <el-option value="STRIPE" label="Stripe（STRIPE）" :disabled="!stripeAvailable" />
              </el-select>
            </div>

            <div class="payment-row">
              <div>
                <h3>管理预览模拟支付</h3>
                <p>仅限已登录人员从管理测试预览触发；普通公开页不能确认模拟付款。</p>
              </div>
              <el-switch
                v-model="form.simulatedPaymentEnabled"
                active-text="启用预览"
                inactive-text="关闭"
                aria-label="启用或关闭管理预览模拟支付"
              />
            </div>

            <div class="tab-actions">
              <el-button type="primary" :icon="Check" :loading="saving" @click="handleSavePayment">
                保存支付与主题
              </el-button>
            </div>
          </el-form>
        </el-tab-pane>
      </el-tabs>
    </div>

    <el-drawer
      v-if="editorVisible"
      v-model="editorVisible"
      :title="`编辑页面内容（${editingPageTitle}）`"
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
              placeholder="例如：把首屏标题改得更温暖，并补充一条亲子设施亮点"
              :disabled="aiEditing || undoingAiEdit"
              @keyup.enter="handleAiEdit"
            >
              <template #prepend>AI 局部修改</template>
            </el-input>
            <el-button
              type="primary"
              :icon="MagicStick"
              :loading="aiEditing"
              :disabled="undoingAiEdit || savingDraft || publishing || editorLoading"
              @click="handleAiEdit"
            >
              提交修改
            </el-button>
            <el-button
              :icon="RefreshLeft"
              :loading="undoingAiEdit"
              :disabled="!hasAiBackup || aiEditing || savingDraft || publishing"
              title="恢复 AI 修改前的草稿"
              @click="handleUndoAiEdit"
            >
              撤销 AI 修改
            </el-button>
          </div>
          <div class="editor-action-row">
            <el-tag v-if="hasUnsavedDraftChanges" type="warning" effect="plain">
              有未保存修改
            </el-tag>
            <el-tag v-else-if="hasSavedDraftReady" type="warning" effect="plain">
              未发布变更
            </el-tag>
            <el-tag v-else type="success" effect="plain">与已发布一致</el-tag>
            <span v-if="draftUpdatedAt" class="editor-meta">草稿保存：{{ draftUpdatedAt }}</span>
            <span v-if="pagePublishedAt" class="editor-meta">上次发布：{{ pagePublishedAt }}</span>
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
              保存草稿
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
              发布页面
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
      :title="`页面预览（${previewPage?.title ?? ''}）`"
      size="86%"
      direction="rtl"
      class="site-preview-drawer"
    >
      <div v-loading="previewLoading" class="preview-body">
        <div v-if="previewDetail" class="preview-toolbar">
          <el-radio-group v-model="previewSource" aria-label="选择预览内容版本">
            <el-radio-button value="draft" :disabled="!previewDraftSchema">草稿</el-radio-button>
            <el-radio-button value="published" :disabled="!previewPublishedSchema">
              已发布
            </el-radio-button>
          </el-radio-group>
          <span class="preview-hint">新标签预览携带 preview=1，与公开访客看到的入口一致。</span>
        </div>
        <div v-if="previewSchema" class="preview-shell">
          <IndependentSitePageRenderer :schema="previewSchema" :theme-key="form.themeKey" preview />
        </div>
        <el-empty
          v-else-if="!previewLoading"
          description="该页面还没有可预览的内容，请先编辑或生成草稿"
        />
      </div>
      <template #footer>
        <div class="drawer-footer">
          <span v-if="previewSource === 'draft'">当前预览为草稿版本</span>
          <span v-else>当前预览为已发布版本</span>
          <div>
            <el-button @click="previewVisible = false">关闭</el-button>
            <el-button type="primary" :icon="TopRight" @click="openPreviewInNewTab">
              新标签打开预览
            </el-button>
          </div>
        </div>
      </template>
    </el-drawer>

    <el-dialog v-model="createPageDialogVisible" title="新建自定义页" width="480px">
      <el-form
        ref="createPageFormRef"
        :model="createPageForm"
        :rules="createPageRules"
        label-position="top"
      >
        <el-form-item label="页面路径" prop="path">
          <el-input
            v-model.trim="createPageForm.path"
            maxlength="255"
            autocomplete="off"
            placeholder="例如：/about 或 /rooms/king"
          />
          <div class="field-help">
            以 / 开头，由小写字母、数字、连字符组成，可多级；在本站点内唯一。
          </div>
        </el-form-item>
        <el-form-item label="页面标题" prop="title">
          <el-input
            v-model.trim="createPageForm.title"
            maxlength="120"
            show-word-limit
            autocomplete="off"
            placeholder="例如：关于我们"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createPageDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="creatingPage" @click="handleCreatePage">
          创建页面
        </el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="importDialogVisible" title="从 URL 导入页面" width="560px">
      <el-alert
        class="import-notice-alert"
        type="warning"
        :closable="false"
        show-icon
        title="仅提取目标页面的文字内容与图片链接生成草稿，不复制原站版式与样式；请确认您有权使用导入的文字与图片内容。"
      />
      <el-form
        ref="importFormRef"
        v-loading="importing"
        element-loading-text="抓取并生成中，可能需要 1-2 分钟"
        :model="importForm"
        :rules="importRules"
        label-position="top"
      >
        <el-form-item label="页面 URL" prop="url">
          <el-input
            v-model.trim="importForm.url"
            maxlength="2048"
            autocomplete="off"
            placeholder="https://example.com/about"
            :disabled="importing"
          />
          <div class="field-help">
            仅支持 http/https 公开地址；内网或无法访问的地址会被服务端拒绝。
          </div>
        </el-form-item>

        <el-form-item label="导入方式">
          <el-radio-group
            v-model="importForm.mode"
            :disabled="importing"
            aria-label="选择导入方式"
            @change="handleImportModeChange"
          >
            <el-radio value="NEW_PAGE">新建页面</el-radio>
            <el-radio value="OVERWRITE_DRAFT">覆盖现有页草稿</el-radio>
          </el-radio-group>
        </el-form-item>

        <template v-if="importForm.mode === 'NEW_PAGE'">
          <el-form-item label="页面路径" prop="path">
            <el-input
              v-model.trim="importForm.path"
              maxlength="255"
              autocomplete="off"
              placeholder="例如：/about 或 /rooms/king"
              :disabled="importing"
            />
            <div class="field-help">
              以 / 开头，由小写字母、数字、连字符组成，可多级；在本站点内唯一。
            </div>
          </el-form-item>
          <el-form-item label="页面标题" prop="title">
            <el-input
              v-model.trim="importForm.title"
              maxlength="120"
              show-word-limit
              autocomplete="off"
              placeholder="例如：关于我们"
              :disabled="importing"
            />
          </el-form-item>
        </template>

        <template v-else>
          <el-form-item label="目标页面" prop="pageId">
            <el-select
              v-model="importForm.pageId"
              filterable
              placeholder="选择要覆盖草稿的页面"
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
              title="将覆盖该页当前草稿，可用编辑器的「撤销 AI 修改」恢复"
            />
          </el-form-item>
        </template>
      </el-form>
      <template #footer>
        <el-button :disabled="importing" @click="importDialogVisible = false">取消</el-button>
        <el-button type="primary" :icon="Link" :loading="importing" @click="handleImportPage">
          {{ importing ? '抓取并生成中，可能需要 1-2 分钟' : '开始导入' }}
        </el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="renameDialogVisible" title="重命名与 SEO 描述" width="480px">
      <div v-loading="renameLoading">
        <el-form ref="renameFormRef" :model="renameForm" :rules="renameRules" label-position="top">
          <el-form-item label="页面标题" prop="title">
            <el-input
              v-model.trim="renameForm.title"
              maxlength="120"
              show-word-limit
              autocomplete="off"
            />
          </el-form-item>
          <el-form-item label="SEO 描述" prop="seoDescription">
            <el-input
              v-model="renameForm.seoDescription"
              type="textarea"
              :rows="3"
              maxlength="300"
              show-word-limit
              resize="vertical"
              placeholder="用于搜索引擎展示的页面摘要，可留空"
            />
          </el-form-item>
        </el-form>
      </div>
      <template #footer>
        <el-button @click="renameDialogVisible = false">取消</el-button>
        <el-button
          type="primary"
          :loading="renaming"
          :disabled="renameLoading"
          @click="handleRenamePage"
        >
          保存
        </el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="generateDialogVisible"
      :title="`AI 整页生成（${generatePage?.title ?? ''}）`"
      width="560px"
    >
      <el-input
        v-model="generatePrompt"
        type="textarea"
        :rows="6"
        maxlength="1000"
        show-word-limit
        resize="vertical"
        placeholder="例如：为海边民宿设计安静、自然的品牌页，突出步行到海滩、亲子设施和入住政策。语气温暖克制。"
      />
      <el-alert
        class="ai-boundary-alert"
        type="info"
        :closable="false"
        show-icon
        title="安全边界"
        description="提示词发送到系统后端的门店级 AI 通道；浏览器不保存模型密钥，也不执行模型生成的 HTML 或 JavaScript。生成结果进入编辑器，保存草稿并发布后才会上线。"
      />
      <template #footer>
        <el-button @click="generateDialogVisible = false">取消</el-button>
        <el-button type="primary" :icon="MagicStick" :loading="generating" @click="handleGenerate">
          生成草稿
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
