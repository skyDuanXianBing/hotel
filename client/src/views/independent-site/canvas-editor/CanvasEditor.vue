<script setup lang="ts">
// 画布编辑器（CANVAS 页）：左聊天面板（可折叠，320px）+ 右大画布。
// 直改自动保存（debounce 1.5s，expectedDraftVersion 乐观锁）；AI 修改以已保存草稿为基底，
// 返回后直接用服务端详情更新画布与 draftVersion（后端已落库）；发布按钮为状态机。
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ChatDotRound, Close, MagicStick, Promotion, TopRight } from '@element-plus/icons-vue'
import { useI18n } from 'vue-i18n'
import {
  aiEditIndependentSitePage,
  generateIndependentSitePageDraftForPage,
  getIndependentSitePage,
  publishIndependentSitePage,
  undoIndependentSitePageAiEdit,
  updateIndependentSitePage,
} from '@/api/independentSite'
import type {
  IndependentSitePageDetail,
  PublicIndependentSiteRoomType,
} from '@/types/independentSite'
import { ensureCanvasTailwind } from '@/utils/canvasTailwind'
import SectionImageUpload from '../editor/SectionImageUpload.vue'
import {
  collectCanvasNodeIds,
  findCanvasNodeById,
  isCanvasElementNode,
  isCanvasTextNode,
  normalizeCanvasSchema,
  validateCanvasSchema,
  type CanvasNode,
  type IndependentSiteCanvasSchema,
} from '../canvasSchema'
import type { CanvasStylePreset } from '../canvasStylePresets'
import { safeIndependentSiteImageUrl } from '../pageSchema'
import { buildIndependentSiteCssVars, normalizeIndependentSiteThemeKey } from '../themes'
import CanvasChatPanel from './CanvasChatPanel.vue'
import CanvasEditorNode from './CanvasEditorNode.vue'

const props = withDefaults(
  defineProps<{
    siteId: number
    pageId: number
    pageTitle: string
    pagePath: string
    // HOME / ROOM_DETAIL / CUSTOM（预览链接拼装用）
    pageType: string
    slug: string
    themeKey?: string
    // 发布范围内房型的精简数据，供 room-list 插槽预览
    roomTypes?: PublicIndependentSiteRoomType[]
    // AI 整页生成等场景带入的新草稿：挂载后直接上画布并立即自动保存
    overrideSchema?: IndependentSiteCanvasSchema | null
  }>(),
  {
    themeKey: '',
    roomTypes: () => [],
    overrideSchema: null,
  },
)

const emit = defineEmits<{
  // 草稿/发布/AI 任何一次服务端落库后发出，父组件用于刷新页面列表
  updated: []
  close: []
}>()

const { locale } = useI18n()

const loading = ref(true)
const loadError = ref('')
// normalize 失败的 fail-closed 错误态（不渲染画布）
const schemaError = ref('')
const draftSchema = ref<IndependentSiteCanvasSchema | null>(null)
const savedDraftJson = ref('')
const publishedJson = ref('')
const draftVersion = ref<number | null>(null)
const draftUpdatedAt = ref<string | null>(null)
const hasAiBackup = ref(false)
const saveState = ref<'idle' | 'saving' | 'saved' | 'error'>('idle')
const saveError = ref('')
const lastSavedAt = ref<Date | null>(null)
const publishing = ref(false)
const aiBusy = ref(false)
const undoingAi = ref(false)
const chatCollapsed = ref(false)
const highlightIds = ref<string[]>([])
const chatPanelRef = ref<InstanceType<typeof CanvasChatPanel> | null>(null)
const editorNodeRef = ref<InstanceType<typeof CanvasEditorNode> | null>(null)

// 换图对话框
const imageDialogVisible = ref(false)
const imageTargetId = ref('')
const imageUrlInput = ref('')

// 「AI 重做这一块」对话框
const aiRedoVisible = ref(false)
const aiRedoNodeId = ref('')
const aiRedoInstruction = ref('')

const AUTOSAVE_DELAY = 1500
let autosaveTimer: number | null = null
let highlightTimer: number | null = null
// AI/服务端结果写回画布期间屏蔽自动保存（后端已落库）
let suppressAutosave = false
let saveInFlight = false
let saveQueued = false

// 与后端 defaultCanvasSchema / 前端 createDefaultCanvasSchema 同形的骨架 id 集合；
// 页面仍是默认骨架时预设卡走整页 generate，否则走 ai-edit
const DEFAULT_SKELETON_IDS = new Set([
  'root',
  'sec-hero',
  'hero-title',
  'hero-title-t',
  'hero-sub',
  'hero-sub-t',
  'hero-cta',
  'hero-cta-t',
  'slot-rooms',
  'slot-booking',
])

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

const currentNormalizedJson = (): string => {
  if (!draftSchema.value) {
    return ''
  }
  const normalized = normalizeCanvasSchema(draftSchema.value)
  return normalized ? JSON.stringify(normalized) : ''
}

const hasUnsavedChanges = computed(() => {
  const json = currentNormalizedJson()
  return json !== '' && json !== savedDraftJson.value
})

const hasUnpublishedChanges = computed(
  () => savedDraftJson.value !== '' && savedDraftJson.value !== publishedJson.value,
)

const canPublish = computed(() =>
  Boolean(
    draftSchema.value &&
      draftVersion.value !== null &&
      hasUnpublishedChanges.value &&
      !hasUnsavedChanges.value &&
      saveState.value !== 'saving' &&
      saveState.value !== 'error' &&
      !aiBusy.value &&
      !publishing.value &&
      !loading.value,
  ),
)

const publishDisabledReason = computed(() => {
  if (hasUnsavedChanges.value) {
    return '有修改正在等待自动保存'
  }
  if (saveState.value === 'saving') {
    return '正在自动保存草稿'
  }
  if (saveState.value === 'error') {
    return '自动保存失败，请先重试'
  }
  if (aiBusy.value) {
    return 'AI 正在修改页面'
  }
  return ''
})

const pad2 = (value: number) => String(value).padStart(2, '0')

const saveStatusText = computed(() => {
  if (saveState.value === 'saving') {
    return '保存中…'
  }
  if (saveState.value === 'error') {
    return '保存失败'
  }
  if (lastSavedAt.value) {
    const time = lastSavedAt.value
    return `已自动保存 ${pad2(time.getHours())}:${pad2(time.getMinutes())}:${pad2(time.getSeconds())}`
  }
  return '更改将自动保存'
})

const themeStyle = computed(() =>
  buildIndependentSiteCssVars(normalizeIndependentSiteThemeKey(props.themeKey)),
)

const isDefaultSkeleton = computed(() => {
  if (!draftSchema.value) {
    return false
  }
  const ids = collectCanvasNodeIds(draftSchema.value.root)
  return ids.length === DEFAULT_SKELETON_IDS.size && ids.every((id) => DEFAULT_SKELETON_IDS.has(id))
})

const previewUrl = computed(() => {
  if (!props.slug) {
    return ''
  }
  const origin = typeof window === 'undefined' ? '' : window.location.origin
  if (props.pageType === 'HOME') {
    return `${origin}/stay/${props.slug}?preview=1`
  }
  const tail = props.pagePath.replace(/^\/+/, '')
  return `${origin}/stay/${props.slug}/p/${tail}?preview=1`
})

// ------------------------------------------------------------------
// 载入 / 服务端详情写回（normalize 不过则 fail-closed）
// ------------------------------------------------------------------

const applyPageDetail = (detail: IndependentSitePageDetail): boolean => {
  const normalizedDraft = normalizeCanvasSchema(detail.draftSchema ?? detail.publishedSchema)
  if (!normalizedDraft) {
    return false
  }
  suppressAutosave = true
  draftSchema.value = normalizedDraft
  savedDraftJson.value = JSON.stringify(normalizedDraft)
  const normalizedPublished = normalizeCanvasSchema(detail.publishedSchema)
  publishedJson.value = normalizedPublished ? JSON.stringify(normalizedPublished) : ''
  draftUpdatedAt.value = detail.draftUpdatedAt || null
  const version = Number(detail.draftVersion)
  draftVersion.value = Number.isInteger(version) && version >= 0 ? version : null
  hasAiBackup.value = Boolean(detail.hasAiBackup)
  saveState.value = 'saved'
  saveError.value = ''
  void nextTick(() => {
    suppressAutosave = false
  })
  return true
}

const applyServerDetail = (detail: IndependentSitePageDetail): boolean => {
  if (applyPageDetail(detail)) {
    return true
  }
  schemaError.value = '服务端返回的页面内容不符合画布契约，请重新加载编辑器'
  ElMessage.error(schemaError.value)
  return false
}

const reloadPageDetail = async () => {
  try {
    const response = await getIndependentSitePage(props.siteId, props.pageId)
    if (response.success && response.data) {
      applyServerDetail(response.data)
    }
  } catch {
    // 保留当前状态，下一次保存/发布/AI 会再次提示
  }
}

const loadPage = async () => {
  loading.value = true
  loadError.value = ''
  schemaError.value = ''
  try {
    const response = await getIndependentSitePage(props.siteId, props.pageId)
    if (!response.success || !response.data) {
      throw new Error(response.message || '加载页面详情失败')
    }
    if (!applyPageDetail(response.data)) {
      schemaError.value = '页面内容不符合画布契约（Canvas Schema v1），无法使用画布编辑器'
      return
    }
    if (props.overrideSchema) {
      const override = normalizeCanvasSchema(props.overrideSchema)
      if (override) {
        suppressAutosave = true
        draftSchema.value = override
        await nextTick()
        suppressAutosave = false
        await performAutosave()
      }
    }
  } catch (error) {
    loadError.value = getErrorMessage(error, '加载页面详情失败')
  } finally {
    loading.value = false
  }
}

// ------------------------------------------------------------------
// 自动保存（debounce 1.5s + expectedDraftVersion 乐观锁）
// ------------------------------------------------------------------

const scheduleAutosave = (delay = AUTOSAVE_DELAY) => {
  if (suppressAutosave) {
    return
  }
  if (autosaveTimer !== null) {
    window.clearTimeout(autosaveTimer)
  }
  autosaveTimer = window.setTimeout(() => {
    autosaveTimer = null
    void performAutosave()
  }, delay)
}

const performAutosave = async (): Promise<boolean> => {
  if (saveInFlight) {
    // 保存请求串行化：在途期间的新变更排队，完成后立即再保存一次
    saveQueued = true
    return true
  }
  if (!draftSchema.value) {
    return false
  }
  const errors = validateCanvasSchema(draftSchema.value)
  if (errors.length > 0) {
    saveState.value = 'error'
    saveError.value = `内容未通过安全校验：${errors[0]}`
    ElMessage.error(saveError.value)
    return false
  }
  const normalized = normalizeCanvasSchema(draftSchema.value)
  if (!normalized) {
    saveState.value = 'error'
    saveError.value = '内容未通过安全校验'
    ElMessage.error(saveError.value)
    return false
  }
  const json = JSON.stringify(normalized)
  if (json === savedDraftJson.value) {
    saveState.value = 'saved'
    return true
  }

  saveInFlight = true
  saveState.value = 'saving'
  saveError.value = ''
  let succeeded = false
  try {
    const response = await updateIndependentSitePage(props.siteId, props.pageId, {
      draftSchema: normalized,
      expectedDraftVersion: draftVersion.value ?? undefined,
    })
    if (!response.success || !response.data) {
      throw new Error(response.message || '保存页面草稿失败')
    }
    const saved = normalizeCanvasSchema(response.data.draftSchema)
    if (!saved) {
      throw new Error('服务端返回的草稿不符合画布契约')
    }
    const savedVersion = Number(response.data.draftVersion)
    if (!Number.isInteger(savedVersion) || savedVersion < 0) {
      throw new Error('服务端返回的草稿版本无效')
    }
    savedDraftJson.value = JSON.stringify(saved)
    draftVersion.value = savedVersion
    draftUpdatedAt.value = response.data.draftUpdatedAt || null
    saveState.value = 'saved'
    lastSavedAt.value = new Date()
    succeeded = true
    emit('updated')
  } catch (error) {
    if (isDraftVersionConflict(error)) {
      saveState.value = 'error'
      saveError.value = '页面草稿已被其他操作更新，已重新加载最新内容'
      ElMessage.error(`${saveError.value}，请确认后再修改`)
      await reloadPageDetail()
    } else {
      saveState.value = 'error'
      saveError.value = getErrorMessage(error, '自动保存失败')
      ElMessage.error(saveError.value)
    }
  } finally {
    saveInFlight = false
    if (saveQueued) {
      saveQueued = false
      scheduleAutosave(0)
    }
  }
  return succeeded
}

// AI 调用/发布/关闭前：先把未落库的直改冲刷到服务端，保证 AI 以画布当前状态为基底
const flushSave = async (): Promise<boolean> => {
  // 行内文本编辑框仍打开时先强制提交，避免已输入内容滞留 textarea 丢失
  editorNodeRef.value?.commitActiveEdit()
  if (autosaveTimer !== null) {
    window.clearTimeout(autosaveTimer)
    autosaveTimer = null
  }
  const deadline = Date.now() + 10000
  while (saveInFlight && Date.now() < deadline) {
    await new Promise((resolve) => {
      window.setTimeout(resolve, 120)
    })
  }
  if (saveInFlight) {
    return false
  }
  const json = currentNormalizedJson()
  if (json && json === savedDraftJson.value) {
    return true
  }
  return performAutosave()
}

watch(
  draftSchema,
  () => {
    if (!suppressAutosave) {
      scheduleAutosave()
    }
  },
  { deep: true },
)

// ------------------------------------------------------------------
// AI 修改摘要（顶层区块 diff）与短暂高亮
// ------------------------------------------------------------------

const topSectionSignatures = (schema: IndependentSiteCanvasSchema | null) => {
  const signatures = new Map<string, string>()
  if (schema && isCanvasElementNode(schema.root) && schema.root.children) {
    for (const child of schema.root.children) {
      signatures.set(child.id, JSON.stringify(child))
    }
  }
  return signatures
}

const diffTopSections = (
  before: Map<string, string>,
  after: Map<string, string>,
): { changed: string[]; added: string[]; removed: number } => {
  const changed: string[] = []
  const added: string[] = []
  let removed = 0
  for (const [id, json] of after) {
    if (!before.has(id)) {
      added.push(id)
    } else if (before.get(id) !== json) {
      changed.push(id)
    }
  }
  for (const id of before.keys()) {
    if (!after.has(id)) {
      removed += 1
    }
  }
  return { changed, added, removed }
}

const buildDiffSummary = (diff: { changed: string[]; added: string[]; removed: number }) => {
  const parts: string[] = []
  if (diff.changed.length > 0) {
    parts.push(`修改 ${diff.changed.length} 个区块`)
  }
  if (diff.added.length > 0) {
    parts.push(`新增 ${diff.added.length} 个区块`)
  }
  if (diff.removed > 0) {
    parts.push(`删除 ${diff.removed} 个区块`)
  }
  return parts.length > 0 ? `已更新页面：${parts.join('，')}` : '已更新页面：内容细节已调整'
}

const highlightSections = (ids: string[]) => {
  highlightIds.value = ids
  if (highlightTimer !== null) {
    window.clearTimeout(highlightTimer)
  }
  highlightTimer = window.setTimeout(() => {
    highlightIds.value = []
    highlightTimer = null
  }, 2200)
}

// ------------------------------------------------------------------
// AI：对话修改 / 整页生成 / 撤销
// ------------------------------------------------------------------

const runAiEdit = async (instruction: string) => {
  if (aiBusy.value || !draftSchema.value) {
    return
  }
  const text = instruction.trim()
  if (!text) {
    return
  }
  if (text.length > 2000) {
    ElMessage.warning('修改指令不能超过 2000 个字符')
    return
  }
  const flushed = await flushSave()
  if (!flushed) {
    const message = '草稿尚未保存成功，AI 修改未执行，请先解决顶部的保存问题'
    ElMessage.warning(message)
    chatPanelRef.value?.appendAiError(message)
    return
  }
  const before = topSectionSignatures(draftSchema.value)
  aiBusy.value = true
  try {
    const response = await aiEditIndependentSitePage(props.siteId, props.pageId, {
      instruction: text,
    })
    if (!response.success || !response.data) {
      throw new Error(response.message || 'AI 修改失败')
    }
    if (!applyServerDetail(response.data)) {
      chatPanelRef.value?.appendAiError('AI 返回内容未通过本地校验，请重新加载编辑器')
      return
    }
    const diff = diffTopSections(before, topSectionSignatures(draftSchema.value))
    highlightSections([...diff.changed, ...diff.added])
    lastSavedAt.value = new Date()
    chatPanelRef.value?.appendAiDelivery(buildDiffSummary(diff))
    emit('updated')
  } catch (error) {
    const message = getErrorMessage(error, 'AI 修改失败，请稍后重试')
    ElMessage.error(message)
    chatPanelRef.value?.appendAiError(message)
  } finally {
    aiBusy.value = false
  }
}

const runGenerate = async (prompt: string) => {
  if (aiBusy.value || !draftSchema.value) {
    return
  }
  const flushed = await flushSave()
  if (!flushed) {
    const message = '草稿尚未保存成功，AI 生成未执行，请先解决顶部的保存问题'
    ElMessage.warning(message)
    chatPanelRef.value?.appendAiError(message)
    return
  }
  const before = topSectionSignatures(draftSchema.value)
  aiBusy.value = true
  try {
    const response = await generateIndependentSitePageDraftForPage(props.siteId, props.pageId, {
      prompt,
      language: locale.value,
    })
    if (!response.success || !response.data) {
      throw new Error(response.message || 'AI 页面生成失败')
    }
    if (!response.data.publishable) {
      throw new Error('AI 返回的页面草稿不可发布，已保留当前页面内容')
    }
    const rawSchema = (response.data as { pageSchema?: unknown }).pageSchema ?? response.data
    const normalized = normalizeCanvasSchema(rawSchema)
    if (!normalized) {
      throw new Error('AI 返回的内容不符合画布契约，已保留当前页面内容')
    }
    suppressAutosave = true
    draftSchema.value = normalized
    await nextTick()
    suppressAutosave = false
    // generate 端点只返回 schema 不落库，立即自动保存为草稿
    const saved = await performAutosave()
    const diff = diffTopSections(before, topSectionSignatures(draftSchema.value))
    highlightSections([...diff.changed, ...diff.added])
    if (saved) {
      chatPanelRef.value?.appendAiDelivery(buildDiffSummary(diff))
    } else {
      chatPanelRef.value?.appendAiError('AI 草稿已生成但自动保存失败，请检查顶部保存状态后重试')
    }
  } catch (error) {
    const message = getErrorMessage(error, 'AI 页面生成失败，已保留当前页面内容')
    ElMessage.error(message)
    chatPanelRef.value?.appendAiError(message)
  } finally {
    aiBusy.value = false
  }
}

const handleChatSend = (instruction: string) => {
  void runAiEdit(instruction)
}

const handlePreset = (preset: CanvasStylePreset) => {
  if (isDefaultSkeleton.value) {
    void runGenerate(preset.prompt)
  } else {
    void runAiEdit(preset.prompt)
  }
}

const handleUndoAi = async () => {
  if (!hasAiBackup.value || undoingAi.value || aiBusy.value) {
    return
  }
  undoingAi.value = true
  try {
    const response = await undoIndependentSitePageAiEdit(props.siteId, props.pageId)
    if (!response.success || !response.data) {
      throw new Error(response.message || '撤销 AI 修改失败')
    }
    applyServerDetail(response.data)
    lastSavedAt.value = new Date()
    ElMessage.success('已撤销最近一次 AI 修改')
    emit('updated')
  } catch (error) {
    ElMessage.error(getErrorMessage(error, '撤销 AI 修改失败'))
  } finally {
    undoingAi.value = false
  }
}

// ------------------------------------------------------------------
// 直改：文本 / 换图 / 顶层区块操作
// ------------------------------------------------------------------

const rootChildren = (): CanvasNode[] | null => {
  const root = draftSchema.value?.root
  return root && isCanvasElementNode(root) && root.children ? root.children : null
}

const handleUpdateText = (nodeId: string, text: string) => {
  const root = draftSchema.value?.root
  if (!root) {
    return
  }
  const node = findCanvasNodeById(root, nodeId)
  if (node && isCanvasTextNode(node)) {
    node.text = text
  }
}

const handleRequestImage = (nodeId: string) => {
  const root = draftSchema.value?.root
  const node = root ? findCanvasNodeById(root, nodeId) : null
  if (!node || !isCanvasElementNode(node) || node.tag !== 'img') {
    return
  }
  imageTargetId.value = nodeId
  imageUrlInput.value = node.attrs?.src ?? ''
  imageDialogVisible.value = true
}

const applyImageUrl = (url: string) => {
  const safeUrl = safeIndependentSiteImageUrl(url)
  if (!safeUrl) {
    ElMessage.warning('请输入合法的图片地址（http/https 或以 / 开头的相对路径）')
    return
  }
  const root = draftSchema.value?.root
  const node = root ? findCanvasNodeById(root, imageTargetId.value) : null
  if (!node || !isCanvasElementNode(node) || node.tag !== 'img') {
    imageDialogVisible.value = false
    ElMessage.error('目标图片节点不存在，请重新加载编辑器')
    return
  }
  node.attrs = { ...node.attrs, src: safeUrl }
  imageDialogVisible.value = false
  ElMessage.success('图片已更新')
}

const handleMoveSection = (nodeId: string, offset: number) => {
  const children = rootChildren()
  if (!children) {
    return
  }
  const index = children.findIndex((child) => child.id === nodeId)
  const target = index + offset
  if (index < 0 || target < 0 || target >= children.length) {
    return
  }
  const [moved] = children.splice(index, 1)
  children.splice(target, 0, moved)
}

const handleRemoveSection = async (nodeId: string) => {
  const children = rootChildren()
  if (!children) {
    return
  }
  const index = children.findIndex((child) => child.id === nodeId)
  if (index < 0) {
    return
  }
  try {
    await ElMessageBox.confirm('确定删除该区块吗？删除后会自动保存，暂不支持恢复。', '删除区块', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning',
      confirmButtonClass: 'el-button--danger',
    })
  } catch {
    return
  }
  children.splice(index, 1)
  ElMessage.success('区块已删除')
}

const handleAiRedoSection = (nodeId: string) => {
  aiRedoNodeId.value = nodeId
  aiRedoInstruction.value = ''
  aiRedoVisible.value = true
}

const submitAiRedo = () => {
  const text = aiRedoInstruction.value.trim()
  if (!text) {
    ElMessage.warning('请输入修改要求')
    return
  }
  if (text.length > 1900) {
    ElMessage.warning('修改要求不能超过 1900 个字符')
    return
  }
  const nodeId = aiRedoNodeId.value
  aiRedoVisible.value = false
  chatPanelRef.value?.appendUserMessage(`重做区块 ${nodeId}：${text}`)
  void runAiEdit(`仅修改 id 为 ${nodeId} 的子树，其余部分原样返回。修改要求：${text}`)
}

// ------------------------------------------------------------------
// 发布状态机与打开网站
// ------------------------------------------------------------------

const handlePublish = async () => {
  if (!draftSchema.value) {
    return
  }
  const flushed = await flushSave()
  if (!flushed) {
    ElMessage.warning('草稿尚未保存成功，请先解决顶部的保存问题再发布')
    return
  }
  if (draftVersion.value === null) {
    ElMessage.warning('草稿版本缺失，无法发布，请重新加载编辑器')
    return
  }
  publishing.value = true
  try {
    const response = await publishIndependentSitePage(props.siteId, props.pageId, {
      draftVersion: draftVersion.value,
    })
    if (!response.success || !response.data) {
      throw new Error(response.message || '发布页面失败')
    }
    applyServerDetail(response.data)
    ElMessage.success('页面已发布')
    emit('updated')
  } catch (error) {
    if (isDraftVersionConflict(error)) {
      ElMessage.error('页面草稿版本已变化，已重新加载最新页面数据，请确认后重试')
      await reloadPageDetail()
    } else {
      ElMessage.error(getErrorMessage(error, '发布页面失败'))
    }
  } finally {
    publishing.value = false
  }
}

const openSite = () => {
  if (!previewUrl.value) {
    ElMessage.warning('站点链接未配置，无法打开')
    return
  }
  window.open(previewUrl.value, '_blank', 'noopener,noreferrer')
}

// ------------------------------------------------------------------
// 关闭：先冲刷自动保存，失败则确认放弃
// ------------------------------------------------------------------

const prepareClose = async (): Promise<boolean> => {
  const flushed = await flushSave()
  if (flushed) {
    return true
  }
  try {
    await ElMessageBox.confirm(
      '草稿尚未保存成功，关闭将丢失最近的修改，确定关闭吗？',
      '关闭画布编辑器',
      {
        confirmButtonText: '关闭',
        cancelButtonText: '继续编辑',
        type: 'warning',
      },
    )
    return true
  } catch {
    return false
  }
}

const handleCloseClick = async () => {
  if (await prepareClose()) {
    emit('close')
  }
}

defineExpose({ prepareClose })

onMounted(() => {
  void ensureCanvasTailwind()
  void loadPage()
})

onBeforeUnmount(() => {
  if (autosaveTimer !== null) {
    window.clearTimeout(autosaveTimer)
    autosaveTimer = null
  }
  if (highlightTimer !== null) {
    window.clearTimeout(highlightTimer)
    highlightTimer = null
  }
})
</script>

<template>
  <div v-loading="loading" class="canvas-editor">
    <div v-if="loadError" class="canvas-editor-state">
      <el-result icon="error" title="页面加载失败" :sub-title="loadError">
        <template #extra>
          <el-button type="primary" @click="loadPage">重新加载</el-button>
          <el-button @click="emit('close')">关闭</el-button>
        </template>
      </el-result>
    </div>

    <div v-else-if="schemaError" class="canvas-editor-state">
      <el-result icon="warning" title="无法使用画布编辑器" :sub-title="schemaError">
        <template #extra>
          <el-button type="primary" @click="loadPage">重新加载</el-button>
          <el-button @click="emit('close')">关闭</el-button>
        </template>
      </el-result>
    </div>

    <template v-else-if="draftSchema">
      <header class="editor-toolbar">
        <div class="toolbar-page">
          <strong class="page-title">{{ pageTitle }}</strong>
          <code class="page-path">{{ pageType === 'HOME' ? '/' : pagePath }}</code>
          <el-tag size="small" type="info" effect="plain">CANVAS</el-tag>
        </div>
        <div class="toolbar-status">
          <span class="save-status" :class="`is-${saveState}`" :title="saveError || undefined">
            {{ saveStatusText }}
          </span>
          <el-button
            v-if="saveState === 'error'"
            size="small"
            link
            type="primary"
            @click="void performAutosave()"
          >
            重试
          </el-button>
        </div>
        <div class="toolbar-actions">
          <el-button size="small" :icon="ChatDotRound" @click="chatCollapsed = !chatCollapsed">
            {{ chatCollapsed ? '展开聊天' : '收起聊天' }}
          </el-button>
          <template v-if="hasUnpublishedChanges">
            <el-button
              size="small"
              type="success"
              :icon="Promotion"
              :loading="publishing"
              :disabled="!canPublish"
              :title="publishDisabledReason || undefined"
              @click="handlePublish"
            >
              发布
            </el-button>
          </template>
          <template v-else>
            <el-tag type="info" effect="plain">已发布</el-tag>
            <el-button size="small" :icon="TopRight" @click="openSite">打开网站</el-button>
          </template>
          <el-button size="small" :icon="Close" @click="handleCloseClick">关闭</el-button>
        </div>
      </header>

      <div class="editor-body">
        <aside v-show="!chatCollapsed" class="editor-chat">
          <CanvasChatPanel
            ref="chatPanelRef"
            :ai-busy="aiBusy"
            :undoing-ai="undoingAi"
            :has-ai-backup="hasAiBackup"
            @send="handleChatSend"
            @preset="handlePreset"
            @undo="handleUndoAi"
          />
        </aside>
        <main class="editor-canvas" :style="themeStyle">
          <CanvasEditorNode
            ref="editorNodeRef"
            :schema="draftSchema"
            :room-types="roomTypes"
            :highlight-ids="highlightIds"
            @update-text="handleUpdateText"
            @request-image="handleRequestImage"
            @move-section="handleMoveSection"
            @remove-section="handleRemoveSection"
            @ai-redo-section="handleAiRedoSection"
          />
        </main>
      </div>
    </template>

    <el-dialog v-model="imageDialogVisible" title="更换图片" width="440px">
      <div class="image-dialog-body">
        <SectionImageUpload button-text="上传新图片" @uploaded="applyImageUrl" />
        <el-divider content-position="center">或粘贴图片地址</el-divider>
        <el-input
          v-model.trim="imageUrlInput"
          maxlength="1500"
          autocomplete="off"
          placeholder="https://… 或以 / 开头的相对路径"
          @keyup.enter="applyImageUrl(imageUrlInput)"
        >
          <template #prepend>URL</template>
        </el-input>
      </div>
      <template #footer>
        <el-button @click="imageDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="applyImageUrl(imageUrlInput)">使用此图片</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="aiRedoVisible" title="AI 重做这一块" width="480px">
      <p class="ai-redo-hint">
        只修改当前区块（id：<code>{{ aiRedoNodeId }}</code
        >），页面其余部分保持不变。
      </p>
      <el-input
        v-model="aiRedoInstruction"
        type="textarea"
        :rows="4"
        maxlength="1900"
        show-word-limit
        resize="vertical"
        placeholder="例如：改成深色背景配金色标题，文案更简短"
      />
      <template #footer>
        <el-button @click="aiRedoVisible = false">取消</el-button>
        <el-button type="primary" :icon="MagicStick" :loading="aiBusy" @click="submitAiRedo">
          开始重做
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.canvas-editor {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 0;
  background: #f7f9f8;
}

.canvas-editor-state {
  display: grid;
  flex: 1;
  place-content: center;
}

.editor-toolbar {
  display: flex;
  flex-shrink: 0;
  align-items: center;
  gap: 16px;
  padding: 10px 16px;
  border-bottom: 1px solid #e2e9e6;
  background: #fff;
}

.toolbar-page {
  display: flex;
  gap: 8px;
  align-items: center;
  min-width: 0;
}

.page-title {
  overflow: hidden;
  color: #173c36;
  font-size: 14px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.page-path {
  color: #69716f;
  font-family: 'SFMono-Regular', Consolas, monospace;
  font-size: 12px;
}

.toolbar-status {
  display: flex;
  flex: 1;
  gap: 6px;
  align-items: center;
  justify-content: center;
  min-width: 0;
}

.save-status {
  font-size: 12px;
}

.save-status.is-saving {
  color: #b57a1f;
}

.save-status.is-saved,
.save-status.is-idle {
  color: #4a7a6d;
}

.save-status.is-error {
  color: #b4423a;
}

.toolbar-actions {
  display: flex;
  gap: 8px;
  align-items: center;
}

.editor-body {
  display: flex;
  flex: 1;
  min-height: 0;
}

.editor-chat {
  flex-shrink: 0;
  width: 320px;
  min-height: 0;
  border-right: 1px solid #e2e9e6;
}

.editor-canvas {
  flex: 1;
  min-width: 0;
  overflow-x: hidden;
  overflow-y: auto;
  background: #fff;
  font-family: var(--site-font-body, inherit);
}

.image-dialog-body {
  display: grid;
  gap: 4px;
  justify-items: start;
}

.image-dialog-body :deep(.el-divider) {
  margin: 14px 0;
}

.image-dialog-body .el-input {
  width: 100%;
}

.ai-redo-hint {
  margin: 0 0 10px;
  color: #69716f;
  font-size: 13px;
}

.ai-redo-hint code {
  color: #357d70;
  font-family: 'SFMono-Regular', Consolas, monospace;
}
</style>
