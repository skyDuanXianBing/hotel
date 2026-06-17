<template>
  <div class="ai-message-knowledge-page">
    <div class="page-header">
      <h2 class="page-title">{{ t('settings.aiMessageKnowledge.title') }}</h2>
      <el-button :loading="loading" @click="handleRefresh">
        {{ t('settings.aiMessageKnowledge.actions.refresh') }}
      </el-button>
    </div>

    <el-alert
      v-if="errorMessage"
      class="page-alert"
      :title="errorMessage"
      type="warning"
      show-icon
      :closable="false"
    />

    <section class="store-knowledge-panel">
      <div class="store-knowledge-header">
        <div class="store-knowledge-heading">
          <span class="store-knowledge-kicker">
            {{ t('settings.aiMessageKnowledge.storeArea.kicker') }}
          </span>
          <h3 class="store-knowledge-title">{{ currentStoreName }}</h3>
          <p class="store-knowledge-description">
            {{
              t('settings.aiMessageKnowledge.storeArea.description', {
                storeName: currentStoreName,
              })
            }}
          </p>
        </div>

        <div class="store-knowledge-stats">
          <div class="store-knowledge-stat">
            <span class="store-knowledge-stat-value">{{ pagination.totalElements }}</span>
            <span class="store-knowledge-stat-label">
              {{ t('settings.aiMessageKnowledge.storeArea.totalItems') }}
            </span>
          </div>
          <div class="store-knowledge-stat">
            <span class="store-knowledge-stat-value">{{ currentPageItemCount }}</span>
            <span class="store-knowledge-stat-label">
              {{ t('settings.aiMessageKnowledge.storeArea.currentPageItems') }}
            </span>
          </div>
        </div>
      </div>

      <el-form class="filter-panel" :model="filters" label-position="top">
        <div class="filter-grid">
          <el-form-item :label="t('settings.aiMessageKnowledge.filters.keyword')">
            <el-input
              v-model="filters.keyword"
              clearable
              :placeholder="t('settings.aiMessageKnowledge.placeholders.keyword')"
              @keyup.enter="handleSearch"
            />
          </el-form-item>

          <el-form-item :label="t('settings.aiMessageKnowledge.filters.status')">
            <el-select v-model="filters.status" clearable filterable>
              <el-option :label="t('settings.common.all')" value="" />
              <el-option
                v-for="option in statusOptions"
                :key="option.value"
                :label="t(option.labelKey)"
                :value="option.value"
              />
            </el-select>
          </el-form-item>

          <el-form-item :label="t('settings.aiMessageKnowledge.filters.scopeType')">
            <el-select v-model="filters.scopeType" clearable filterable>
              <el-option :label="t('settings.common.all')" value="" />
              <el-option
                v-for="option in scopeOptions"
                :key="option.value"
                :label="t(option.labelKey)"
                :value="option.value"
              />
            </el-select>
          </el-form-item>

          <el-form-item :label="t('settings.aiMessageKnowledge.filters.topicCode')">
            <el-input
              v-model="filters.topicCode"
              clearable
              :placeholder="t('settings.aiMessageKnowledge.placeholders.topicCode')"
              @keyup.enter="handleSearch"
            />
          </el-form-item>

          <div class="filter-actions">
            <el-button type="primary" :loading="loading" @click="handleSearch">
              {{ t('settings.common.search') }}
            </el-button>
            <el-button @click="handleReset">
              {{ t('settings.common.reset') }}
            </el-button>
          </div>
        </div>
      </el-form>

      <div class="knowledge-table-section">
        <el-table
          v-loading="loading"
          :data="knowledgeItems"
          class="knowledge-table"
          row-key="id"
          :empty-text="t('settings.aiMessageKnowledge.empty')"
        >
          <el-table-column
            :label="t('settings.aiMessageKnowledge.columns.knowledge')"
            min-width="300"
          >
            <template #default="{ row }">
              <div class="knowledge-cell">
                <div class="knowledge-title">{{ getKnowledgeTitle(row) }}</div>
                <div v-if="getKnowledgeSummary(row)" class="knowledge-summary">
                  {{ getKnowledgeSummary(row) }}
                </div>
              </div>
            </template>
          </el-table-column>

          <el-table-column :label="t('settings.aiMessageKnowledge.columns.scope')" min-width="180">
            <template #default="{ row }">
              <div class="meta-stack">
                <span>{{ getScopeLabel(row.scopeType) }}</span>
                <span v-if="getTextValue(row.scopeName)" class="muted-text">
                  {{ getTextValue(row.scopeName) }}
                </span>
              </div>
            </template>
          </el-table-column>

          <el-table-column :label="t('settings.aiMessageKnowledge.columns.topic')" min-width="150">
            <template #default="{ row }">
              {{ getTopicLabel(row) }}
            </template>
          </el-table-column>

          <el-table-column :label="t('settings.aiMessageKnowledge.columns.status')" width="130">
            <template #default="{ row }">
              <el-tag :type="getStatusTagType(row.status)" effect="light">
                {{ getStatusLabel(row.status) }}
              </el-tag>
            </template>
          </el-table-column>

          <el-table-column
            prop="evidenceCount"
            :label="t('settings.aiMessageKnowledge.columns.evidence')"
            width="110"
            align="center"
          >
            <template #default="{ row }">
              {{ getEvidenceCount(row) }}
            </template>
          </el-table-column>

          <el-table-column :label="t('settings.aiMessageKnowledge.columns.language')" width="150">
            <template #default="{ row }">
              <div v-if="getLanguageValues(row).length > 0" class="language-tags">
                <el-tag
                  v-for="language in getLanguageValues(row)"
                  :key="language"
                  size="small"
                  effect="plain"
                >
                  {{ language }}
                </el-tag>
              </div>
              <span v-else class="muted-text">{{ t('settings.aiMessageKnowledge.none') }}</span>
            </template>
          </el-table-column>

          <el-table-column :label="t('settings.aiMessageKnowledge.columns.updatedAt')" width="180">
            <template #default="{ row }">
              {{ formatDateTime(row.updatedAt || row.lastSeenAt || row.createdAt) }}
            </template>
          </el-table-column>

          <el-table-column
            class-name="knowledge-action-cell"
            :label="t('settings.common.actions')"
            width="230"
          >
            <template #default="{ row }">
              <div class="action-buttons">
                <el-button link type="primary" @click="handleViewEvidence(row)">
                  {{ t('settings.aiMessageKnowledge.actions.viewEvidence') }}
                </el-button>
                <el-button
                  link
                  type="success"
                  :disabled="!canApprove(row)"
                  :loading="isOperating(row)"
                  @click="handleApprove(row)"
                >
                  {{ t('settings.aiMessageKnowledge.actions.approve') }}
                </el-button>
                <el-button
                  link
                  type="warning"
                  :disabled="!canReject(row)"
                  :loading="isOperating(row)"
                  @click="handleReject(row)"
                >
                  {{ t('settings.aiMessageKnowledge.actions.reject') }}
                </el-button>
                <el-button
                  link
                  type="info"
                  :disabled="!canArchive(row)"
                  :loading="isOperating(row)"
                  @click="handleArchive(row)"
                >
                  {{ t('settings.aiMessageKnowledge.actions.archive') }}
                </el-button>
              </div>
            </template>
          </el-table-column>
        </el-table>

        <div class="pagination-row">
          <el-pagination
            v-model:current-page="pagination.page"
            v-model:page-size="pagination.size"
            :page-sizes="pageSizeOptions"
            :total="pagination.totalElements"
            layout="total, sizes, prev, pager, next"
            background
            @size-change="handleSizeChange"
            @current-change="handleCurrentChange"
          />
        </div>
      </div>
    </section>

    <el-drawer
      v-model="evidenceDrawerVisible"
      :title="t('settings.aiMessageKnowledge.evidence.title')"
      size="min(92vw, 560px)"
      destroy-on-close
    >
      <div class="drawer-heading">
        <div class="knowledge-title">{{ selectedKnowledgeTitle }}</div>
      </div>

      <el-alert
        v-if="evidenceErrorMessage"
        class="page-alert"
        :title="evidenceErrorMessage"
        type="warning"
        show-icon
        :closable="false"
      />

      <el-skeleton v-if="evidenceLoading" :rows="5" animated />

      <el-empty
        v-else-if="evidenceItems.length === 0"
        :description="t('settings.aiMessageKnowledge.evidence.empty')"
      />

      <div v-else class="evidence-list">
        <section
          v-for="(evidence, index) in evidenceItems"
          :key="getEvidenceKey(evidence, index)"
          class="evidence-item"
        >
          <div class="evidence-meta">
            <span class="evidence-source">{{ getEvidenceSource(evidence) }}</span>
            <el-tag v-if="getEvidenceLanguage(evidence)" size="small" effect="plain" type="info">
              {{ getEvidenceLanguage(evidence) }}
            </el-tag>
            <span>{{ formatDateTime(evidence.occurredAt || evidence.createdAt) }}</span>
          </div>
          <p class="evidence-content">{{ getEvidenceContent(evidence) }}</p>
          <div v-if="getEvidenceConfidence(evidence)" class="evidence-confidence">
            {{ t('settings.aiMessageKnowledge.evidence.confidence') }}:
            {{ getEvidenceConfidence(evidence) }}
          </div>
        </section>
      </div>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useStoreStore } from '@/stores/store'
import {
  approveAiMessageKnowledgeItem,
  archiveAiMessageKnowledgeItem,
  getAiMessageKnowledgeEvidence,
  getAiMessageKnowledgeItems,
  rejectAiMessageKnowledgeItem,
  type AiMessageKnowledgeEvidence,
  type AiMessageKnowledgeEvidenceData,
  type AiMessageKnowledgeItem,
  type AiMessageKnowledgePageData,
  type AiMessageKnowledgeScopeType,
  type AiMessageKnowledgeStatus,
  type ApiResponse,
} from '@/api/aiMessageKnowledge'

const DEFAULT_PAGE = 1
const DEFAULT_PAGE_SIZE = 20
const FIRST_PAGE_INDEX = 0
const DEFAULT_TOTAL = 0
const DEFAULT_COUNT = 0

const pageSizeOptions = [10, 20, 50]
const languageLabels: Record<string, string> = {
  en: 'EN',
  zh: 'ZH',
  'zh-cn': 'ZH-CN',
  'zh-tw': 'ZH-TW',
  ja: 'JA',
  ko: 'KO',
}

const statusOptions: Array<{ value: AiMessageKnowledgeStatus; labelKey: string }> = [
  { value: 'CANDIDATE', labelKey: 'settings.aiMessageKnowledge.status.CANDIDATE' },
  { value: 'ACTIVE', labelKey: 'settings.aiMessageKnowledge.status.ACTIVE' },
  { value: 'CONFLICTED', labelKey: 'settings.aiMessageKnowledge.status.CONFLICTED' },
  { value: 'ARCHIVED', labelKey: 'settings.aiMessageKnowledge.status.ARCHIVED' },
  { value: 'REJECTED', labelKey: 'settings.aiMessageKnowledge.status.REJECTED' },
]

const scopeOptions: Array<{ value: AiMessageKnowledgeScopeType; labelKey: string }> = [
  { value: 'STORE', labelKey: 'settings.aiMessageKnowledge.scope.STORE' },
  { value: 'ROOM_TYPE', labelKey: 'settings.aiMessageKnowledge.scope.ROOM_TYPE' },
  { value: 'ROOM', labelKey: 'settings.aiMessageKnowledge.scope.ROOM' },
  { value: 'RESERVATION', labelKey: 'settings.aiMessageKnowledge.scope.RESERVATION' },
  { value: 'CHANNEL', labelKey: 'settings.aiMessageKnowledge.scope.CHANNEL' },
  { value: 'THREAD', labelKey: 'settings.aiMessageKnowledge.scope.THREAD' },
]

interface FilterState {
  keyword: string
  status: AiMessageKnowledgeStatus | ''
  scopeType: AiMessageKnowledgeScopeType | ''
  topicCode: string
}

interface PaginationState {
  page: number
  size: number
  totalElements: number
}

interface NormalizedPage {
  content: AiMessageKnowledgeItem[]
  totalElements: number
  totalPages: number
  number: number
  size: number
}

const { t } = useI18n()
const storeStore = useStoreStore()

const currentStoreId = computed(() => storeStore.currentStore?.id ?? null)
const filters = ref<FilterState>({
  keyword: '',
  status: '',
  scopeType: '',
  topicCode: '',
})
const pagination = ref<PaginationState>({
  page: DEFAULT_PAGE,
  size: DEFAULT_PAGE_SIZE,
  totalElements: DEFAULT_TOTAL,
})
const knowledgeItems = ref<AiMessageKnowledgeItem[]>([])
const loading = ref(false)
const errorMessage = ref('')
const operatingId = ref<number | null>(null)

const evidenceDrawerVisible = ref(false)
const evidenceLoading = ref(false)
const evidenceErrorMessage = ref('')
const selectedKnowledgeItem = ref<AiMessageKnowledgeItem | null>(null)
const evidenceItems = ref<AiMessageKnowledgeEvidence[]>([])
let loadRequestToken = 0
let evidenceRequestToken = 0

const selectedKnowledgeTitle = computed(() => {
  if (!selectedKnowledgeItem.value) {
    return t('settings.aiMessageKnowledge.fallbackTitle')
  }
  return getKnowledgeTitle(selectedKnowledgeItem.value)
})

const isRecord = (value: unknown): value is Record<string, unknown> => {
  return typeof value === 'object' && value !== null
}

const isApiResponse = <T,>(value: unknown): value is ApiResponse<T> => {
  return isRecord(value) && typeof value.success === 'boolean'
}

const getTextValue = (value: unknown) => {
  if (typeof value !== 'string') {
    return ''
  }
  return value.trim()
}

const currentStoreName = computed(() => {
  const storeName = getTextValue(storeStore.currentStore?.name)
  if (storeName) {
    return storeName
  }
  return t('settings.aiMessageKnowledge.storeArea.unknownStore')
})

const currentPageItemCount = computed(() => knowledgeItems.value.length)

const pickTextField = (item: Record<string, unknown>, fieldNames: string[]) => {
  for (const fieldName of fieldNames) {
    const text = getTextValue(item[fieldName])
    if (text) {
      return text
    }
  }
  return ''
}

const toNumber = (value: unknown, fallback: number) => {
  const numericValue = Number(value)
  if (Number.isFinite(numericValue)) {
    return numericValue
  }
  return fallback
}

const getErrorText = (error: unknown, fallback: string) => {
  if (!isRecord(error)) {
    return fallback
  }

  const response = error.response
  if (isRecord(response)) {
    const data = response.data
    if (isRecord(data)) {
      const responseMessage = getTextValue(data.message)
      if (responseMessage) {
        return responseMessage
      }
    }
  }

  const errorMessageText = getTextValue(error.message)
  if (errorMessageText) {
    return errorMessageText
  }

  return fallback
}

const normalizeEnumValue = (value: unknown) => {
  const text = getTextValue(value)
  if (!text) {
    return ''
  }
  return text.toUpperCase()
}

const getKnowledgeTitle = (item: AiMessageKnowledgeItem) => {
  const title = pickTextField(item, [
    'title',
    'topicLabel',
    'topicName',
    'question',
    'normalizedQuestion',
    'sourceQuestion',
  ])
  if (title) {
    return title
  }
  return t('settings.aiMessageKnowledge.fallbackTitle')
}

const getKnowledgeSummary = (item: AiMessageKnowledgeItem) => {
  const summary = pickTextField(item, ['answer', 'content', 'knowledgeText', 'summary'])
  if (summary === getKnowledgeTitle(item)) {
    return ''
  }
  return summary
}

const getTopicLabel = (item: AiMessageKnowledgeItem) => {
  const topic = pickTextField(item, ['topicLabel', 'topicName', 'topicCode'])
  if (topic) {
    return topic
  }
  return t('settings.aiMessageKnowledge.none')
}

const getScopeLabel = (scopeType: unknown) => {
  const normalizedScope = normalizeEnumValue(scopeType)
  const option = scopeOptions.find((scopeOption) => scopeOption.value === normalizedScope)
  if (option) {
    return t(option.labelKey)
  }

  const scopeText = getTextValue(scopeType)
  if (scopeText) {
    return scopeText
  }

  return t('settings.aiMessageKnowledge.none')
}

const getStatusLabel = (status: unknown) => {
  const normalizedStatus = normalizeEnumValue(status)
  const option = statusOptions.find((statusOption) => statusOption.value === normalizedStatus)
  if (option) {
    return t(option.labelKey)
  }

  const statusText = getTextValue(status)
  if (statusText) {
    return statusText
  }

  return t('settings.aiMessageKnowledge.status.UNKNOWN')
}

const getStatusTagType = (status: unknown) => {
  const normalizedStatus = normalizeEnumValue(status)
  if (normalizedStatus === 'ACTIVE') {
    return 'success'
  }
  if (normalizedStatus === 'CONFLICTED') {
    return 'warning'
  }
  if (normalizedStatus === 'REJECTED') {
    return 'danger'
  }
  if (normalizedStatus === 'ARCHIVED') {
    return 'info'
  }
  return 'primary'
}

const getEvidenceCount = (item: AiMessageKnowledgeItem) => {
  return toNumber(item.evidenceCount, DEFAULT_COUNT)
}

const getLanguageLabel = (value: unknown) => {
  const language = getTextValue(value).replace('_', '-')
  if (!language) {
    return ''
  }

  const label = languageLabels[language.toLowerCase()]
  if (label) {
    return label
  }
  return language
}

const addLanguageValue = (target: string[], value: unknown) => {
  const label = getLanguageLabel(value)
  if (!label) {
    return
  }
  if (!target.includes(label)) {
    target.push(label)
  }
}

const addLanguageSummaryValues = (target: string[], value: unknown) => {
  const summary = getTextValue(value)
  if (!summary) {
    return
  }
  const parts = summary.split(/[,/]+/)
  for (const part of parts) {
    addLanguageValue(target, part)
  }
}

const getLanguageValues = (item: AiMessageKnowledgeItem) => {
  const values: string[] = []
  addLanguageValue(values, item.language)
  if (Array.isArray(item.evidenceLanguages)) {
    for (const language of item.evidenceLanguages) {
      addLanguageValue(values, language)
    }
  }
  addLanguageSummaryValues(values, item.languageSummary)
  return values
}

const formatDateTime = (value: unknown) => {
  const text = getTextValue(value)
  if (!text) {
    return t('settings.aiMessageKnowledge.none')
  }

  const date = new Date(text)
  if (Number.isNaN(date.getTime())) {
    return text
  }

  return new Intl.DateTimeFormat(undefined, {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  }).format(date)
}

const normalizePageNumber = (
  data: AiMessageKnowledgePageData,
  requestedPageIndex: number,
) => {
  if (data.number !== undefined) {
    return toNumber(data.number, requestedPageIndex)
  }

  if (data.page !== undefined) {
    const pageValue = toNumber(data.page, requestedPageIndex)
    if (pageValue > FIRST_PAGE_INDEX) {
      return pageValue - 1
    }
    return pageValue
  }

  return requestedPageIndex
}

const normalizePageData = (
  data: AiMessageKnowledgePageData | AiMessageKnowledgeItem[] | undefined,
  requestedPageIndex: number,
  requestedPageSize: number,
): NormalizedPage => {
  if (Array.isArray(data)) {
    return {
      content: data,
      totalElements: data.length,
      totalPages: DEFAULT_PAGE,
      number: requestedPageIndex,
      size: requestedPageSize,
    }
  }

  if (!data) {
    return {
      content: [],
      totalElements: DEFAULT_TOTAL,
      totalPages: DEFAULT_TOTAL,
      number: requestedPageIndex,
      size: requestedPageSize,
    }
  }

  let content: AiMessageKnowledgeItem[] = []
  if (Array.isArray(data.content)) {
    content = data.content
  } else if (Array.isArray(data.items)) {
    content = data.items
  }

  const totalElements = toNumber(data.totalElements ?? data.total, content.length)
  let totalPages = toNumber(data.totalPages, DEFAULT_TOTAL)
  if (totalPages === DEFAULT_TOTAL && requestedPageSize > DEFAULT_COUNT) {
    totalPages = Math.ceil(totalElements / requestedPageSize)
  }

  return {
    content,
    totalElements,
    totalPages,
    number: normalizePageNumber(data, requestedPageIndex),
    size: toNumber(data.size, requestedPageSize),
  }
}

const getPageDataFromResponse = (
  response: ApiResponse<AiMessageKnowledgePageData> | AiMessageKnowledgePageData,
): AiMessageKnowledgePageData | undefined => {
  if (isApiResponse<AiMessageKnowledgePageData>(response)) {
    if (!response.success) {
      const message = getTextValue(response.message)
      if (message) {
        errorMessage.value = message
      } else {
        errorMessage.value = t('settings.aiMessageKnowledge.messages.loadFailed')
      }
      return undefined
    }
    return response.data
  }

  return response as AiMessageKnowledgePageData
}

const isActiveKnowledgeRequest = (requestToken: number, storeId: number) => {
  return requestToken === loadRequestToken && currentStoreId.value === storeId
}

const isActiveEvidenceRequest = (requestToken: number, storeId: number, itemId: number) => {
  return (
    requestToken === evidenceRequestToken &&
    currentStoreId.value === storeId &&
    selectedKnowledgeItem.value?.id === itemId
  )
}

const resetEvidenceState = () => {
  evidenceRequestToken += 1
  evidenceDrawerVisible.value = false
  evidenceLoading.value = false
  evidenceErrorMessage.value = ''
  selectedKnowledgeItem.value = null
  evidenceItems.value = []
}

const resetKnowledgeState = () => {
  loadRequestToken += 1
  loading.value = false
  operatingId.value = null
  errorMessage.value = ''
  knowledgeItems.value = []
  pagination.value = {
    page: DEFAULT_PAGE,
    size: DEFAULT_PAGE_SIZE,
    totalElements: DEFAULT_TOTAL,
  }
  resetEvidenceState()
}

const loadKnowledgeItems = async () => {
  const storeIdAtRequest = currentStoreId.value
  if (!storeIdAtRequest) {
    resetKnowledgeState()
    errorMessage.value = t('settings.aiMessageKnowledge.messages.selectStore')
    return
  }

  const requestToken = loadRequestToken + 1
  loadRequestToken = requestToken
  loading.value = true
  errorMessage.value = ''
  const requestedPageIndex = pagination.value.page - 1
  const requestedPageSize = pagination.value.size

  try {
    const response = await getAiMessageKnowledgeItems({
      page: requestedPageIndex,
      size: requestedPageSize,
      keyword: filters.value.keyword,
      status: filters.value.status,
      scopeType: filters.value.scopeType,
      topicCode: filters.value.topicCode,
    })
    if (!isActiveKnowledgeRequest(requestToken, storeIdAtRequest)) {
      return
    }

    const pageData = getPageDataFromResponse(response)
    const normalizedPage = normalizePageData(pageData, requestedPageIndex, requestedPageSize)
    knowledgeItems.value = normalizedPage.content
    pagination.value = {
      page: normalizedPage.number + 1,
      size: normalizedPage.size,
      totalElements: normalizedPage.totalElements,
    }
  } catch (error) {
    if (!isActiveKnowledgeRequest(requestToken, storeIdAtRequest)) {
      return
    }

    knowledgeItems.value = []
    pagination.value.totalElements = DEFAULT_TOTAL
    errorMessage.value = getErrorText(error, t('settings.aiMessageKnowledge.messages.loadFailed'))
  } finally {
    if (isActiveKnowledgeRequest(requestToken, storeIdAtRequest)) {
      loading.value = false
    }
  }
}

const handleSearch = () => {
  pagination.value.page = DEFAULT_PAGE
  loadKnowledgeItems()
}

const handleReset = () => {
  filters.value = {
    keyword: '',
    status: '',
    scopeType: '',
    topicCode: '',
  }
  pagination.value.page = DEFAULT_PAGE
  loadKnowledgeItems()
}

const handleRefresh = () => {
  loadKnowledgeItems()
}

const handleSizeChange = (size: number) => {
  pagination.value.size = size
  pagination.value.page = DEFAULT_PAGE
  loadKnowledgeItems()
}

const handleCurrentChange = (page: number) => {
  pagination.value.page = page
  loadKnowledgeItems()
}

const normalizeEvidenceData = (
  data: AiMessageKnowledgeEvidenceData | AiMessageKnowledgeEvidence[] | undefined,
) => {
  if (Array.isArray(data)) {
    return data
  }

  if (!data) {
    return []
  }

  if (Array.isArray(data.evidence)) {
    return data.evidence
  }

  if (Array.isArray(data.content)) {
    return data.content
  }

  if (Array.isArray(data.items)) {
    return data.items
  }

  return []
}

const handleViewEvidence = async (row: AiMessageKnowledgeItem) => {
  const storeIdAtRequest = currentStoreId.value
  if (!storeIdAtRequest) {
    ElMessage.warning(t('settings.aiMessageKnowledge.messages.selectStore'))
    return
  }

  const requestToken = evidenceRequestToken + 1
  evidenceRequestToken = requestToken
  selectedKnowledgeItem.value = row
  evidenceDrawerVisible.value = true
  evidenceLoading.value = true
  evidenceErrorMessage.value = ''
  evidenceItems.value = []

  try {
    const response = await getAiMessageKnowledgeEvidence(row.id)
    if (!isActiveEvidenceRequest(requestToken, storeIdAtRequest, row.id)) {
      return
    }

    let responseData: AiMessageKnowledgeEvidenceData | AiMessageKnowledgeEvidence[] | undefined
    const responseLike = response as
      | ApiResponse<AiMessageKnowledgeEvidenceData | AiMessageKnowledgeEvidence[]>
      | AiMessageKnowledgeEvidenceData
      | AiMessageKnowledgeEvidence[]
    if (isApiResponse<AiMessageKnowledgeEvidenceData | AiMessageKnowledgeEvidence[]>(
      responseLike,
    )) {
      if (!responseLike.success) {
        const message = getTextValue(responseLike.message)
        if (message) {
          evidenceErrorMessage.value = message
        } else {
          evidenceErrorMessage.value = t('settings.aiMessageKnowledge.messages.evidenceLoadFailed')
        }
      }
      responseData = responseLike.data
    } else {
      responseData = responseLike as AiMessageKnowledgeEvidenceData | AiMessageKnowledgeEvidence[]
    }
    evidenceItems.value = normalizeEvidenceData(responseData)
  } catch (error) {
    if (!isActiveEvidenceRequest(requestToken, storeIdAtRequest, row.id)) {
      return
    }

    evidenceErrorMessage.value = getErrorText(
      error,
      t('settings.aiMessageKnowledge.messages.evidenceLoadFailed'),
    )
  } finally {
    if (isActiveEvidenceRequest(requestToken, storeIdAtRequest, row.id)) {
      evidenceLoading.value = false
    }
  }
}

const getEvidenceKey = (evidence: AiMessageKnowledgeEvidence, index: number) => {
  if (typeof evidence.id === 'number') {
    return evidence.id
  }
  return `${index}-${getEvidenceSource(evidence)}`
}

const getEvidenceSource = (evidence: AiMessageKnowledgeEvidence) => {
  const source = pickTextField(evidence, ['sourceTitle', 'sourceType', 'channelName', 'topicCode'])
  if (source) {
    return source
  }
  return t('settings.aiMessageKnowledge.evidence.defaultSource')
}

const getEvidenceContent = (evidence: AiMessageKnowledgeEvidence) => {
  const content = pickTextField(evidence, [
    'sourceText',
    'content',
    'messageContent',
    'guestMessage',
    'staffMessage',
  ])
  if (content) {
    return content
  }
  return t('settings.aiMessageKnowledge.evidence.noContent')
}

const getEvidenceConfidence = (evidence: AiMessageKnowledgeEvidence) => {
  const confidence = evidence.confidence
  if (confidence === undefined || confidence === null || confidence === '') {
    return ''
  }
  return String(confidence)
}

const getEvidenceLanguage = (evidence: AiMessageKnowledgeEvidence) => {
  return getLanguageLabel(evidence.language)
}

const isOperating = (row: AiMessageKnowledgeItem) => {
  return operatingId.value === row.id
}

const canApprove = (row: AiMessageKnowledgeItem) => {
  const status = normalizeEnumValue(row.status)
  return status !== 'ACTIVE' && status !== 'ARCHIVED'
}

const canReject = (row: AiMessageKnowledgeItem) => {
  const status = normalizeEnumValue(row.status)
  return status !== 'REJECTED' && status !== 'ARCHIVED'
}

const canArchive = (row: AiMessageKnowledgeItem) => {
  const status = normalizeEnumValue(row.status)
  return status !== 'ARCHIVED'
}

const getActionStoreId = () => {
  const storeId = currentStoreId.value
  if (!storeId) {
    ElMessage.warning(t('settings.aiMessageKnowledge.messages.selectStore'))
    return null
  }
  return storeId
}

const handleApprove = async (row: AiMessageKnowledgeItem) => {
  const actionStoreId = getActionStoreId()
  if (!actionStoreId) {
    return
  }

  try {
    await ElMessageBox.confirm(
      t('settings.aiMessageKnowledge.messages.approveConfirm', {
        title: getKnowledgeTitle(row),
      }),
      t('settings.aiMessageKnowledge.messages.approveConfirmTitle'),
      {
        confirmButtonText: t('settings.common.confirmButton'),
        cancelButtonText: t('settings.common.cancelButton'),
        type: 'warning',
      },
    )

    if (currentStoreId.value !== actionStoreId) {
      return
    }

    operatingId.value = row.id
    const response = await approveAiMessageKnowledgeItem(row.id)
    if (currentStoreId.value !== actionStoreId) {
      return
    }

    if (response.success) {
      ElMessage.success(t('settings.aiMessageKnowledge.messages.approveSuccess'))
      await loadKnowledgeItems()
    } else {
      ElMessage.error(response.message || t('settings.aiMessageKnowledge.messages.approveFailed'))
    }
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(
        getErrorText(error, t('settings.aiMessageKnowledge.messages.approveFailed')),
      )
    }
  } finally {
    operatingId.value = null
  }
}

const handleReject = async (row: AiMessageKnowledgeItem) => {
  const actionStoreId = getActionStoreId()
  if (!actionStoreId) {
    return
  }

  try {
    const promptResult = await ElMessageBox.prompt(
      t('settings.aiMessageKnowledge.messages.rejectPrompt', {
        title: getKnowledgeTitle(row),
      }),
      t('settings.aiMessageKnowledge.messages.rejectConfirmTitle'),
      {
        confirmButtonText: t('settings.common.confirmButton'),
        cancelButtonText: t('settings.common.cancelButton'),
        inputPlaceholder: t('settings.aiMessageKnowledge.placeholders.rejectReason'),
        type: 'warning',
      },
    )

    if (currentStoreId.value !== actionStoreId) {
      return
    }

    const reason = promptResult.value.trim()
    const data = reason ? { reason } : {}
    operatingId.value = row.id
    const response = await rejectAiMessageKnowledgeItem(row.id, data)
    if (currentStoreId.value !== actionStoreId) {
      return
    }

    if (response.success) {
      ElMessage.success(t('settings.aiMessageKnowledge.messages.rejectSuccess'))
      await loadKnowledgeItems()
    } else {
      ElMessage.error(response.message || t('settings.aiMessageKnowledge.messages.rejectFailed'))
    }
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(getErrorText(error, t('settings.aiMessageKnowledge.messages.rejectFailed')))
    }
  } finally {
    operatingId.value = null
  }
}

const handleArchive = async (row: AiMessageKnowledgeItem) => {
  const actionStoreId = getActionStoreId()
  if (!actionStoreId) {
    return
  }

  try {
    await ElMessageBox.confirm(
      t('settings.aiMessageKnowledge.messages.archiveConfirm', {
        title: getKnowledgeTitle(row),
      }),
      t('settings.aiMessageKnowledge.messages.archiveConfirmTitle'),
      {
        confirmButtonText: t('settings.common.confirmButton'),
        cancelButtonText: t('settings.common.cancelButton'),
        type: 'warning',
      },
    )

    if (currentStoreId.value !== actionStoreId) {
      return
    }

    operatingId.value = row.id
    const response = await archiveAiMessageKnowledgeItem(row.id)
    if (currentStoreId.value !== actionStoreId) {
      return
    }

    if (response.success) {
      ElMessage.success(t('settings.aiMessageKnowledge.messages.archiveSuccess'))
      await loadKnowledgeItems()
    } else {
      ElMessage.error(response.message || t('settings.aiMessageKnowledge.messages.archiveFailed'))
    }
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(
        getErrorText(error, t('settings.aiMessageKnowledge.messages.archiveFailed')),
      )
    }
  } finally {
    operatingId.value = null
  }
}

watch(currentStoreId, (storeId, previousStoreId) => {
  if (storeId === previousStoreId) {
    return
  }

  resetKnowledgeState()
  if (storeId) {
    loadKnowledgeItems()
  } else {
    errorMessage.value = t('settings.aiMessageKnowledge.messages.selectStore')
  }
})

onMounted(() => {
  loadKnowledgeItems()
})
</script>

<style scoped>
.ai-message-knowledge-page {
  min-height: calc(100vh - 100px);
  padding: 20px;
  background: #f5f7fa;
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
}

.page-title {
  margin: 0;
  color: #303133;
  font-size: 22px;
  font-weight: 600;
  line-height: 1.3;
}

.page-alert {
  margin-bottom: 16px;
}

.store-knowledge-panel {
  background: #ffffff;
  border: 1px solid #ebeef5;
  border-radius: 8px;
}

.store-knowledge-panel {
  padding: 18px;
}

.store-knowledge-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 20px;
  padding-bottom: 16px;
  border-bottom: 1px solid #ebeef5;
}

.store-knowledge-heading {
  min-width: 0;
}

.store-knowledge-kicker {
  display: block;
  margin-bottom: 4px;
  color: #409eff;
  font-size: 12px;
  font-weight: 600;
  line-height: 1.4;
}

.store-knowledge-title {
  margin: 0;
  color: #303133;
  font-size: 18px;
  font-weight: 600;
  line-height: 1.35;
  overflow-wrap: anywhere;
}

.store-knowledge-description {
  max-width: 680px;
  margin: 6px 0 0;
  color: #606266;
  font-size: 13px;
  line-height: 1.55;
  overflow-wrap: anywhere;
}

.store-knowledge-stats {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 8px;
}

.store-knowledge-stat {
  min-width: 112px;
  padding: 8px 10px;
  border: 1px solid #dcdfe6;
  border-radius: 6px;
  background: #f8fafc;
}

.store-knowledge-stat-value,
.store-knowledge-stat-label {
  display: block;
}

.store-knowledge-stat-value {
  color: #303133;
  font-size: 18px;
  font-weight: 600;
  line-height: 1.2;
}

.store-knowledge-stat-label {
  margin-top: 2px;
  color: #606266;
  font-size: 12px;
  line-height: 1.4;
}

.filter-panel {
  padding-top: 16px;
}

.filter-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(160px, 1fr)) auto;
  gap: 12px;
  align-items: end;
}

.filter-actions {
  display: flex;
  gap: 8px;
  padding-bottom: 18px;
  white-space: nowrap;
}

.knowledge-table-section {
  padding-top: 4px;
}

.knowledge-table {
  width: 100%;
}

.action-buttons {
  display: flex;
  flex-wrap: wrap;
  gap: 4px 10px;
  align-items: center;
}

.action-buttons :deep(.el-button + .el-button) {
  margin-left: 0;
}

.knowledge-cell,
.meta-stack {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
}

.knowledge-title {
  color: #303133;
  font-size: 14px;
  font-weight: 600;
  line-height: 1.45;
  overflow-wrap: anywhere;
}

.knowledge-summary,
.muted-text {
  color: #606266;
  font-size: 13px;
  line-height: 1.45;
  overflow-wrap: anywhere;
}

.knowledge-summary {
  display: -webkit-box;
  overflow: hidden;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.language-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}

.pagination-row {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

.drawer-heading {
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid #ebeef5;
}

.evidence-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.evidence-item {
  padding: 14px;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  background: #ffffff;
}

.evidence-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 12px;
  margin-bottom: 8px;
  color: #909399;
  font-size: 12px;
}

.evidence-source {
  color: #303133;
  font-weight: 600;
}

.evidence-content {
  margin: 0;
  color: #303133;
  font-size: 14px;
  line-height: 1.6;
  overflow-wrap: anywhere;
  white-space: pre-wrap;
}

.evidence-confidence {
  margin-top: 8px;
  color: #606266;
  font-size: 12px;
}

@media (max-width: 960px) {
  .store-knowledge-header {
    flex-direction: column;
  }

  .store-knowledge-stats {
    justify-content: flex-start;
    width: 100%;
  }

  .filter-grid {
    grid-template-columns: repeat(2, minmax(160px, 1fr));
  }

  .filter-actions {
    grid-column: 1 / -1;
  }
}

@media (max-width: 640px) {
  .ai-message-knowledge-page {
    padding: 12px;
  }

  .store-knowledge-panel {
    padding: 14px;
  }

  .page-header {
    align-items: stretch;
    flex-direction: column;
  }

  .store-knowledge-stat {
    flex: 1 1 128px;
  }

  .filter-grid {
    grid-template-columns: 1fr;
  }

  .filter-actions {
    flex-wrap: wrap;
  }

  .pagination-row {
    justify-content: flex-start;
    overflow-x: auto;
  }
}
</style>
