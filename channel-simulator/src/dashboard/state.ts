import { computed, ref } from 'vue'

import { apiRequest } from './api'
import { buildMessagingBody, buildRunBody, buildScenarioExample } from './payloads'
import type {
  ApiEnvelope,
  BootstrapData,
  ConfigSummary,
  Credentials,
  JsonObject,
  LoadingState,
  LogEntry,
  MessagingForm,
  ReadinessParts,
  RunForm,
  ScenarioSummary,
  StatusText,
  ViewId,
  ViewItem,
} from './types'
import {
  buildReadinessParts,
  formatDateTime,
  formatEntityList,
  formatLogPreview,
  getEnvelopeData,
  getLifecycleSteps,
  getLogKey,
  getRunSteps,
  getText,
  isObject,
  labelForEntity,
  prettyJson,
  statusLabel,
  valueForEntity,
} from './utils'

const LEGACY_LOCAL_STORAGE_KEY = 'channelSimulator.credentials.v1'
const DEFAULT_HOTEL_ID = '1'

const viewItems: ViewItem[] = [
  { id: 'readiness', title: '环境与就绪', description: 'PMS 凭据、配置和前置条件' },
  { id: 'run', title: 'E2E Run', description: '单次预订链路模拟' },
  { id: 'lifecycle', title: '生命周期 / 消息', description: '修改、取消与客人消息' },
  { id: 'scenarios', title: '场景发送', description: '手工发送 webhook 场景' },
  { id: 'logs', title: '日志与配置', description: '请求日志和运行配置' },
]

const activeView = ref<ViewId>('readiness')
const credentials = ref<Credentials>(loadCredentials())
const config = ref<ConfigSummary | null>(null)
const logs = ref<LogEntry[]>([])
const scenarios = ref<ScenarioSummary[]>([])
const selectedScenarioName = ref('')
const useDefaultPayload = ref(true)
const customPayloadText = ref('')
const hotelId = ref(DEFAULT_HOTEL_ID)

const readiness = ref<unknown>(null)
const runResult = ref<unknown>(null)
const lifecycleResult = ref<unknown>(null)
const messagingResult = ref<unknown>(null)
const scenarioSendResult = ref<unknown>(null)

const runForm = ref<RunForm>({
  mode: 'PUSH',
  channel: 'BOOKING',
  scenario: 'NEW',
  roomTypeId: '',
  roomId: '',
  stayStartDays: '',
})

const messagingForm = ref<MessagingForm>({
  channel: 'AIRBNB',
  message: '请问可以提前入住吗？',
  bookingId: '',
  threadId: '',
})

const loading = ref<LoadingState>({
  config: false,
  bootstrap: false,
  logs: false,
  readiness: false,
  run: false,
  lifecycle: false,
  messaging: false,
  scenarios: false,
  sendScenario: false,
})

const statusText = ref<StatusText>({
  credentials: '',
  bootstrap: '',
  config: '',
  logs: '',
  readiness: '',
  run: '',
  lifecycle: '',
  messaging: '',
  scenarios: '',
  sendScenario: '',
})

const selectedScenario = computed(() => {
  for (const scenario of scenarios.value) {
    if (scenario.name === selectedScenarioName.value) {
      return scenario
    }
  }
  return null
})
const readinessParts = computed<ReadinessParts>(() => buildReadinessParts(readiness.value))
const runData = computed(() => getEnvelopeData(runResult.value))
const lifecycleData = computed(() => getEnvelopeData(lifecycleResult.value))
const selectedRoomTypes = computed(() => readinessParts.value.roomTypes)
const selectedRooms = computed(() => readinessParts.value.rooms)
const hasSessionCredentials = computed(() => {
  return Boolean(credentials.value.token.trim() && credentials.value.storeId.trim())
})
const hasManagedTestSupportKey = computed(() => {
  return config.value?.testSupportAuth?.hasKey === true
})
const hasAvailableTestSupportKey = computed(() => {
  return hasManagedTestSupportKey.value || Boolean(credentials.value.testSupportKey.trim())
})
const readinessBadge = computed(() => {
  if (readinessParts.value.ready === true) {
    return {
      text: '已就绪',
      tone: 'ready',
    }
  }
  if (!hasAvailableTestSupportKey.value) {
    return {
      text: '管理密钥未配置',
      tone: 'warning',
    }
  }
  if (!hasSessionCredentials.value) {
    return {
      text: '等待初始化',
      tone: 'neutral',
    }
  }
  return {
    text: '需检查就绪',
    tone: 'warning',
  }
})
const reversedLogs = computed(() => {
  return logs.value.slice().reverse()
})

export function useDashboardState() {
  return {
    activeView,
    config,
    credentials,
    customPayloadText,
    hotelId,
    lifecycleData,
    lifecycleResult,
    loading,
    logs,
    messagingForm,
    messagingResult,
    readinessParts,
    readinessBadge,
    reversedLogs,
    runData,
    runForm,
    runResult,
    scenarioSendResult,
    scenarios,
    selectedRooms,
    selectedRoomTypes,
    selectedScenario,
    selectedScenarioName,
    statusText,
    useDefaultPayload,
    viewItems,
    clearCredentials,
    clearLogs,
    formatDateTime,
    formatEntityList,
    formatLogPreview,
    getLifecycleSteps,
    getLogKey,
    getRunSteps,
    hasTestSupportInputs,
    bootstrapLocalEnvironment,
    hasAvailableTestSupportKey,
    hasManagedTestSupportKey,
    hasSessionCredentials,
    isObject,
    labelForEntity,
    loadConfig,
    loadLogs,
    loadReadiness,
    loadScenarios,
    prettyJson,
    refreshRun,
    resetScenarioPayload,
    runE2E,
    runLifecycle,
    runMessaging,
    selectScenario,
    selectView,
    sendScenario,
    statusLabel,
    valueForEntity,
  }
}

function loadCredentials(): Credentials {
  window.localStorage.removeItem(LEGACY_LOCAL_STORAGE_KEY)
  return createEmptyCredentials()
}

function createEmptyCredentials(): Credentials {
  return {
    token: '',
    storeId: '',
    testSupportKey: '',
  }
}

function applyBootstrapData(data: BootstrapData): void {
  credentials.value.token = data.token || ''
  credentials.value.storeId = data.storeId ? String(data.storeId) : ''
  hotelId.value = data.suHotelId || hotelId.value
  runForm.value.roomTypeId = data.roomTypeId ? String(data.roomTypeId) : runForm.value.roomTypeId
  runForm.value.roomId = data.roomId ? String(data.roomId) : runForm.value.roomId
  readiness.value = data.readiness || null
}

function clearCredentials(): void {
  credentials.value = createEmptyCredentials()
  window.localStorage.removeItem(LEGACY_LOCAL_STORAGE_KEY)
  readiness.value = null
  runForm.value.roomTypeId = ''
  runForm.value.roomId = ''
  statusText.value.credentials = '当前页面会话凭据已清除。'
  statusText.value.bootstrap = ''
}

function selectView(id: ViewId): void {
  activeView.value = id
}

function hasTestSupportInputs(): boolean {
  return hasSessionCredentials.value && hasAvailableTestSupportKey.value
}

async function bootstrapLocalEnvironment(): Promise<void> {
  if (!hasAvailableTestSupportKey.value) {
    statusText.value.bootstrap = '请先输入本地 E2E 管理密钥，或在 simulator 后端配置 CHANNEL_E2E_TEST_SUPPORT_KEY。'
    return
  }

  loading.value.bootstrap = true
  statusText.value.bootstrap = '正在初始化本地模拟环境...'
  try {
    const response = await apiRequest<ApiEnvelope<BootstrapData>>(
      'POST',
      '/api/e2e/bootstrap',
      null,
      true,
      credentials.value,
    )
    if (!response.data) {
      throw new Error(response.message || '初始化没有返回有效数据。')
    }
    applyBootstrapData(response.data)
    statusText.value.bootstrap = '本地模拟环境已初始化，就绪状态已刷新。'
    statusText.value.credentials = 'PMS token 和门店 ID 已写入当前页面会话。'
  } catch (err) {
    statusText.value.bootstrap = err instanceof Error ? err.message : String(err)
  } finally {
    loading.value.bootstrap = false
  }
}

async function loadConfig(): Promise<void> {
  loading.value.config = true
  statusText.value.config = '正在加载配置...'
  try {
    const response = await apiRequest<ApiEnvelope<ConfigSummary>>(
      'GET',
      '/api/config',
      null,
      false,
      credentials.value,
    )
    config.value = response.data || null
    if (config.value?.defaultHotelId && hotelId.value === DEFAULT_HOTEL_ID) {
      hotelId.value = String(config.value.defaultHotelId)
    }
    statusText.value.config = '配置已加载。'
  } catch (err) {
    statusText.value.config = err instanceof Error ? err.message : String(err)
  } finally {
    loading.value.config = false
  }
}

async function loadReadiness(): Promise<void> {
  loading.value.readiness = true
  statusText.value.readiness = '正在检查就绪状态...'
  try {
    const response = await apiRequest<unknown>('GET', '/api/e2e/readiness', null, true, credentials.value)
    readiness.value = response
    if (readinessParts.value.ready === true) {
      statusText.value.readiness = '就绪检查已通过。'
    } else {
      statusText.value.readiness = '就绪检查已返回，请查看缺失项。'
    }
  } catch (err) {
    statusText.value.readiness = err instanceof Error ? err.message : String(err)
  } finally {
    loading.value.readiness = false
  }
}

async function runE2E(): Promise<void> {
  loading.value.run = true
  statusText.value.run = '正在启动单次 E2E Run...'
  try {
    const response = await apiRequest<unknown>(
      'POST',
      '/api/e2e/runs',
      buildRunBody(runForm.value),
      true,
      credentials.value,
    )
    runResult.value = response
    statusText.value.run = '单次 E2E Run 已完成。'
  } catch (err) {
    runResult.value = err
    statusText.value.run = err instanceof Error ? err.message : String(err)
  } finally {
    loading.value.run = false
  }
}

async function refreshRun(): Promise<void> {
  const data = getEnvelopeData(runData.value)
  const run = isObject(data) && isObject(data.run) ? data.run : null
  const runId = run ? getText(run, ['runId']) : ''
  if (!runId) {
    statusText.value.run = '暂无可刷新的 Run ID。'
    return
  }

  loading.value.run = true
  try {
    const endpoint = `/api/e2e/runs/${encodeURIComponent(runId)}`
    runResult.value = await apiRequest<unknown>('GET', endpoint, null, true, credentials.value)
    statusText.value.run = 'Run 结果已刷新。'
  } catch (err) {
    statusText.value.run = err instanceof Error ? err.message : String(err)
  } finally {
    loading.value.run = false
  }
}

async function runLifecycle(): Promise<void> {
  loading.value.lifecycle = true
  statusText.value.lifecycle = '正在运行生命周期测试...'
  try {
    lifecycleResult.value = await apiRequest<unknown>(
      'POST',
      '/api/e2e/lifecycle',
      buildRunBody(runForm.value),
      true,
      credentials.value,
    )
    statusText.value.lifecycle = '生命周期测试已完成。'
  } catch (err) {
    lifecycleResult.value = err
    statusText.value.lifecycle = err instanceof Error ? err.message : String(err)
  } finally {
    loading.value.lifecycle = false
  }
}

async function runMessaging(): Promise<void> {
  loading.value.messaging = true
  statusText.value.messaging = '正在运行消息测试...'
  try {
    messagingResult.value = await apiRequest<unknown>(
      'POST',
      '/api/e2e/messaging',
      buildMessagingBody(messagingForm.value),
      true,
      credentials.value,
    )
    statusText.value.messaging = '消息测试已完成。'
  } catch (err) {
    messagingResult.value = err
    statusText.value.messaging = err instanceof Error ? err.message : String(err)
  } finally {
    loading.value.messaging = false
  }
}

async function loadScenarios(): Promise<void> {
  loading.value.scenarios = true
  statusText.value.scenarios = '正在加载场景...'
  try {
    const response = await apiRequest<ApiEnvelope<ScenarioSummary[]>>(
      'GET',
      '/api/webhooks/scenarios',
      null,
      false,
      credentials.value,
    )
    scenarios.value = Array.isArray(response.data) ? response.data : []
    if (!selectedScenarioName.value && scenarios.value.length > 0) {
      selectScenario(scenarios.value[0].name)
    }
    statusText.value.scenarios = `已加载 ${scenarios.value.length} 个场景。`
  } catch (err) {
    scenarios.value = []
    statusText.value.scenarios = err instanceof Error ? err.message : String(err)
  } finally {
    loading.value.scenarios = false
  }
}

function selectScenario(name: string): void {
  selectedScenarioName.value = name
  scenarioSendResult.value = null
  customPayloadText.value = prettyJson(
    buildScenarioExample(selectedScenario.value, hotelId.value, DEFAULT_HOTEL_ID),
  )
}

function resetScenarioPayload(): void {
  customPayloadText.value = prettyJson(
    buildScenarioExample(selectedScenario.value, hotelId.value, DEFAULT_HOTEL_ID),
  )
  statusText.value.sendScenario = '示例请求内容已重置。'
}

async function sendScenario(): Promise<void> {
  const scenario = selectedScenario.value
  if (!scenario) {
    statusText.value.sendScenario = '请先选择一个场景。'
    return
  }

  const body: JsonObject = {
    hotelId: hotelId.value.trim() || DEFAULT_HOTEL_ID,
  }
  if (!useDefaultPayload.value) {
    try {
      const parsed = JSON.parse(customPayloadText.value)
      if (!isObject(parsed)) {
        throw new Error('请求内容必须是 JSON 对象。')
      }
      body.customPayload = parsed
    } catch (err) {
      statusText.value.sendScenario = err instanceof Error ? err.message : String(err)
      return
    }
  }

  loading.value.sendScenario = true
  statusText.value.sendScenario = '正在发送场景...'
  try {
    const endpoint = `/api/webhooks/scenarios/${encodeURIComponent(scenario.name)}/send`
    scenarioSendResult.value = await apiRequest<unknown>(
      'POST',
      endpoint,
      body,
      false,
      credentials.value,
    )
    statusText.value.sendScenario = '场景已发送，日志已刷新。'
    await loadLogs()
  } catch (err) {
    scenarioSendResult.value = err
    statusText.value.sendScenario = err instanceof Error ? err.message : String(err)
  } finally {
    loading.value.sendScenario = false
  }
}

async function loadLogs(): Promise<void> {
  loading.value.logs = true
  statusText.value.logs = '正在加载日志...'
  try {
    const response = await apiRequest<ApiEnvelope<LogEntry[]>>(
      'GET',
      '/api/logs',
      null,
      false,
      credentials.value,
    )
    logs.value = Array.isArray(response.data) ? response.data : []
    statusText.value.logs = `已加载 ${logs.value.length} 条日志。`
  } catch (err) {
    logs.value = []
    statusText.value.logs = err instanceof Error ? err.message : String(err)
  } finally {
    loading.value.logs = false
  }
}

async function clearLogs(): Promise<void> {
  loading.value.logs = true
  try {
    await apiRequest<unknown>('DELETE', '/api/logs', null, false, credentials.value)
    logs.value = []
    statusText.value.logs = '日志已清空。'
  } catch (err) {
    statusText.value.logs = err instanceof Error ? err.message : String(err)
  } finally {
    loading.value.logs = false
  }
}
