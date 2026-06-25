import { computed, nextTick, onMounted, ref, watch, type Ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import {
  createDoorLockIntegration,
  deleteDoorLockBinding,
  getDoorLockDevices,
  getDoorLockIntegrations,
  getDoorLockRooms,
  refreshDoorLockToken,
  saveDoorLockBinding,
  syncDoorLockDevices,
  testDoorLockIntegration,
  updateDoorLockIntegration,
  type DoorLockBindingDTO,
  type DoorLockBindingPayload,
  type DoorLockDeviceDTO,
  type DoorLockIntegrationDTO,
  type DoorLockIntegrationPayload,
  type DoorLockProvider,
  type DoorLockRoomDTO,
} from '@/api/doorLock'
import { useStoreStore } from '@/stores/store'

const PROVIDER_SWITCHBOT: DoorLockProvider = 'SWITCHBOT'
const PROVIDER_TTLOCK: DoorLockProvider = 'TTLOCK'
const EMPTY_ROOM_TYPE_FILTER = ''
const MASKED_ID_VISIBLE_LENGTH = 4
const STATUS_SOURCE_BOUND_LOCK = 'BOUND_LOCK'
const ROLE_CONTROL = 'control' as const
const ROLE_PASSCODE = 'passcode' as const
const SWITCHBOT_AUTHENTICATION_PANEL_TYPES = [
  'keypad',
  'keypad touch',
  'keypad vision',
  'keypad vision pro',
]
const SWITCHBOT_CONTROL_DEVICE_TYPE_KEYWORD = 'lock'
const SWITCHBOT_PASSCODE_DEVICE_TYPES = [
  'keypad',
  'keypad touch',
  'keypad vision',
  'keypad vision pro',
  'lock vision',
  'lock vision pro',
]
const CONTROL_CAPABILITY_VALUES = [
  'CONTROL',
  'LOCK_CONTROL',
  'LOCK',
  'UNLOCK',
  'STATUS',
  'BATTERY',
]
const PASSCODE_CAPABILITY_VALUES = [
  'PASSCODE',
  'PASSCODE_MANAGEMENT',
  'PASSCODE_MANAGE',
  'PIN',
  'PIN_CODE',
  'PASSWORD',
]

interface DoorLockConfigForm {
  enabled: boolean
  name: string
  token: string
  secret: string
  clientId: string
  clientSecret: string
  username: string
  password: string
}

interface RoomTypeOption {
  id: number
  name: string
}

type CredentialPayloadKey =
  | 'switchBotToken'
  | 'switchBotSecret'
  | 'ttLockClientId'
  | 'ttLockClientSecret'
  | 'ttLockUsername'
  | 'ttLockPassword'

interface ResourceLoadingOwner {
  provider: DoorLockProvider
  requestToken: number
  storeId: number
}

type DoorLockBindingRole = typeof ROLE_CONTROL | typeof ROLE_PASSCODE

interface DoorLockRoleSelection {
  controlDeviceKey: string
  passcodeDeviceKey: string
}

interface RoomBindingValidation {
  canSave: boolean
  messages: string[]
}

interface DoorLockApiResponseLike {
  message?: string
  data?: unknown
}

export function useDoorLockSettings() {
  const { t } = useI18n()
  const storeStore = useStoreStore()
  const configFormRef = ref<FormInstance>()

  const providerOptions = [
    { value: PROVIDER_SWITCHBOT, labelKey: 'settings.doorLocks.providers.SWITCHBOT' },
    { value: PROVIDER_TTLOCK, labelKey: 'settings.doorLocks.providers.TTLOCK' },
  ]

  const activeProvider = ref<DoorLockProvider>(PROVIDER_SWITCHBOT)
  const pageLoading = ref(false)
  const resourceLoading = ref(false)
  const savingConfigProviders = ref(new Set<DoorLockProvider>())
  const testingConnectionProviders = ref(new Set<DoorLockProvider>())
  const refreshingTokenProviders = ref(new Set<DoorLockProvider>())
  const syncingDevicesProviders = ref(new Set<DoorLockProvider>())
  const savingBindingKeys = ref(new Set<string>())
  const unbindingKeys = ref(new Set<string>())
  const errorMessage = ref('')
  const roomTypeFilter = ref<number | ''>(EMPTY_ROOM_TYPE_FILTER)
  const devices = ref<DoorLockDeviceDTO[]>([])
  const rooms = ref<DoorLockRoomDTO[]>([])
  const selectedDeviceByRoomId = ref<Record<number, DoorLockRoleSelection>>({})
  const integrations = ref(createEmptyIntegrationMap())
  const configForms = ref(createEmptyConfigForms())

  let loadRequestToken = 0
  let resourceLoadingOwner: ResourceLoadingOwner | null = null

  const currentStoreId = computed(() => storeStore.currentStore?.id ?? null)
  const currentStoreName = computed(() => storeStore.currentStore?.name ?? '')
  const activeIntegration = computed(() => integrations.value[activeProvider.value])
  const activeConfigForm = computed(() => configForms.value[activeProvider.value])
  const isNewIntegration = computed(() => !activeIntegration.value?.id)
  const savingConfig = computed(() => savingConfigProviders.value.has(activeProvider.value))
  const testingConnection = computed(() => testingConnectionProviders.value.has(activeProvider.value))
  const refreshingToken = computed(() => refreshingTokenProviders.value.has(activeProvider.value))
  const syncingDevices = computed(() => syncingDevicesProviders.value.has(activeProvider.value))

  const configRules = computed<FormRules>(() => ({
    token: [{ validator: validateSwitchBotToken, trigger: 'blur' }],
    secret: [{ validator: validateSwitchBotSecret, trigger: 'blur' }],
    clientId: [{ validator: validateTtLockClientId, trigger: 'blur' }],
    clientSecret: [{ validator: validateTtLockClientSecret, trigger: 'blur' }],
    username: [{ validator: validateTtLockUsername, trigger: 'blur' }],
    password: [{ validator: validateTtLockPassword, trigger: 'blur' }],
  }))

  const roomTypeOptions = computed<RoomTypeOption[]>(() => {
    const options = new Map<number, string>()
    for (const room of rooms.value) {
      const roomTypeId = getRoomTypeId(room)
      const roomTypeName = getRoomTypeName(room)
      if (roomTypeId && roomTypeName) {
        options.set(roomTypeId, roomTypeName)
      }
    }

    return Array.from(options.entries())
      .map(([id, name]) => ({ id, name }))
      .sort((first, second) => first.name.localeCompare(second.name))
  })

  const filteredRooms = computed(() => {
    if (roomTypeFilter.value === EMPTY_ROOM_TYPE_FILTER) {
      return rooms.value
    }

    return rooms.value.filter((room) => getRoomTypeId(room) === roomTypeFilter.value)
  })

  const maskedCredentialList = computed(() => {
    const integration = activeIntegration.value
    if (!integration) {
      return []
    }

    if (activeProvider.value === PROVIDER_SWITCHBOT) {
      return [
        buildMaskedCredentialItem(t('settings.doorLocks.fields.token'), getMaskedCredential(integration, 'token')),
        buildMaskedCredentialItem(t('settings.doorLocks.fields.secret'), getMaskedCredential(integration, 'secret')),
      ].filter(Boolean) as Array<{ label: string; value: string }>
    }

    return [
      buildMaskedCredentialItem(t('settings.doorLocks.fields.clientId'), getMaskedCredential(integration, 'clientId')),
      buildMaskedCredentialItem(
        t('settings.doorLocks.fields.clientSecret'),
        getMaskedCredential(integration, 'clientSecret'),
      ),
      buildMaskedCredentialItem(
        t('settings.doorLocks.fields.ttLockUsername'),
        getMaskedCredential(integration, 'username'),
      ),
      buildMaskedCredentialItem(
        t('settings.doorLocks.fields.ttLockPassword'),
        getMaskedCredential(integration, 'password'),
      ),
      buildMaskedCredentialItem(
        t('settings.doorLocks.fields.accessToken'),
        getMaskedCredential(integration, 'accessToken'),
      ),
    ].filter(Boolean) as Array<{ label: string; value: string }>
  })

  function createEmptyConfigForm(): DoorLockConfigForm {
    return {
      enabled: false,
      name: '',
      token: '',
      secret: '',
      clientId: '',
      clientSecret: '',
      username: '',
      password: '',
    }
  }

  function createEmptyConfigForms(): Record<DoorLockProvider, DoorLockConfigForm> {
    return {
      SWITCHBOT: createEmptyConfigForm(),
      TTLOCK: createEmptyConfigForm(),
    }
  }

  function createEmptyIntegrationMap(): Record<DoorLockProvider, DoorLockIntegrationDTO | null> {
    return {
      SWITCHBOT: null,
      TTLOCK: null,
    }
  }

  function createEmptyRoleSelection(): DoorLockRoleSelection {
    return {
      controlDeviceKey: '',
      passcodeDeviceKey: '',
    }
  }

  function clearErrorMessage() {
    errorMessage.value = ''
  }

  function getNextLoadToken() {
    loadRequestToken += 1
    return loadRequestToken
  }

  function getProviderRoomActionKey(provider: DoorLockProvider, roomId: number) {
    return `${provider}:${roomId}`
  }

  function setProviderActionLoading(
    loadingRef: Ref<Set<DoorLockProvider>>,
    provider: DoorLockProvider,
    loading: boolean,
  ) {
    const next = new Set(loadingRef.value)
    if (loading) {
      next.add(provider)
    } else {
      next.delete(provider)
    }
    loadingRef.value = next
  }

  function setRoomActionLoading(
    loadingRef: Ref<Set<string>>,
    provider: DoorLockProvider,
    roomId: number,
    loading: boolean,
  ) {
    const next = new Set(loadingRef.value)
    const key = getProviderRoomActionKey(provider, roomId)
    if (loading) {
      next.add(key)
    } else {
      next.delete(key)
    }
    loadingRef.value = next
  }

  function clearActionLoadingState() {
    savingConfigProviders.value = new Set()
    testingConnectionProviders.value = new Set()
    refreshingTokenProviders.value = new Set()
    syncingDevicesProviders.value = new Set()
    savingBindingKeys.value = new Set()
    unbindingKeys.value = new Set()
  }

  function isCurrentStoreRequest(storeId: number) {
    return currentStoreId.value === storeId
  }

  function isCurrentProviderRequest(storeId: number, provider: DoorLockProvider) {
    return isCurrentStoreRequest(storeId) && activeProvider.value === provider
  }

  function isActiveRequest(requestToken: number, storeId: number, provider?: DoorLockProvider) {
    if (requestToken !== loadRequestToken || currentStoreId.value !== storeId) {
      return false
    }

    if (provider && activeProvider.value !== provider) {
      return false
    }

    return true
  }

  function beginResourceLoading(provider: DoorLockProvider, requestToken: number, storeId: number) {
    if (!isCurrentProviderRequest(storeId, provider)) {
      return
    }

    resourceLoadingOwner = { provider, requestToken, storeId }
    resourceLoading.value = true
  }

  function endResourceLoading(provider: DoorLockProvider, requestToken: number, storeId: number) {
    if (
      resourceLoadingOwner?.provider !== provider ||
      resourceLoadingOwner.requestToken !== requestToken ||
      resourceLoadingOwner.storeId !== storeId
    ) {
      return
    }

    resourceLoadingOwner = null
    resourceLoading.value = false
  }

  function resetPageState() {
    getNextLoadToken()
    resourceLoadingOwner = null
    integrations.value = createEmptyIntegrationMap()
    configForms.value = createEmptyConfigForms()
    clearProviderState()
    clearActionLoadingState()
    errorMessage.value = ''
    pageLoading.value = false
    resourceLoading.value = false
  }

  function clearProviderState() {
    devices.value = []
    rooms.value = []
    selectedDeviceByRoomId.value = {}
    roomTypeFilter.value = EMPTY_ROOM_TYPE_FILTER
  }

  function getErrorText(error: unknown, fallback: string) {
    const response = getObjectProperty(error, 'response')
    const responseData = getObjectProperty(response, 'data')
    const responseMessage = getObjectMessage(responseData)
    if (responseMessage) {
      return responseMessage
    }

    const directMessage = getObjectMessage(error)
    if (directMessage) {
      return directMessage
    }

    if (error instanceof Error && error.message.trim()) {
      return error.message
    }

    if (typeof error === 'string' && error.trim()) {
      return error
    }

    return fallback
  }

  function getResponseErrorText(response: DoorLockApiResponseLike, fallback: string) {
    const dataMessage = getObjectMessage(response.data)
    if (dataMessage) {
      return dataMessage
    }

    if (response.message?.trim()) {
      return response.message
    }

    return fallback
  }

  function getObjectMessage(value: unknown) {
    const message = getObjectProperty(value, 'message')
    if (typeof message === 'string' && message.trim()) {
      return message
    }

    return ''
  }

  function getObjectProperty(value: unknown, key: string) {
    if (!isRecord(value)) {
      return undefined
    }

    return value[key]
  }

  function isRecord(value: unknown): value is Record<string, unknown> {
    return typeof value === 'object' && value !== null
  }

  function isIntegrationEnabled(integration: DoorLockIntegrationDTO) {
    return Boolean(integration.enabled)
  }

  function applyIntegrationToForm(integration: DoorLockIntegrationDTO) {
    const form = configForms.value[integration.provider]
    form.enabled = isIntegrationEnabled(integration)
    form.name = integration.name || ''
    form.token = ''
    form.secret = ''
    form.clientId = ''
    form.clientSecret = ''
    form.username = ''
    form.password = ''
  }

  function applyIntegrations(items: DoorLockIntegrationDTO[]) {
    integrations.value = createEmptyIntegrationMap()
    configForms.value = createEmptyConfigForms()

    for (const integration of items) {
      if (integration.provider === PROVIDER_SWITCHBOT || integration.provider === PROVIDER_TTLOCK) {
        integrations.value[integration.provider] = integration
        applyIntegrationToForm(integration)
      }
    }
  }

  async function loadPageData() {
    const storeIdAtRequest = currentStoreId.value
    if (!storeIdAtRequest) {
      resetPageState()
      errorMessage.value = t('settings.doorLocks.messages.selectStore')
      return
    }

    const requestToken = getNextLoadToken()
    pageLoading.value = true
    errorMessage.value = ''
    clearProviderState()

    try {
      const response = await getDoorLockIntegrations()
      if (!isActiveRequest(requestToken, storeIdAtRequest)) {
        return
      }

      if (!response.success) {
        throw new Error(getResponseErrorText(response, t('settings.doorLocks.messages.loadFailed')))
      }

      applyIntegrations(response.data || [])
      await loadProviderResources(activeProvider.value, requestToken, storeIdAtRequest)
    } catch (error) {
      if (isActiveRequest(requestToken, storeIdAtRequest)) {
        errorMessage.value = getErrorText(error, t('settings.doorLocks.messages.loadFailed'))
      }
    } finally {
      if (isActiveRequest(requestToken, storeIdAtRequest)) {
        pageLoading.value = false
      }
    }
  }

  async function loadProviderResources(
    provider: DoorLockProvider,
    existingRequestToken?: number,
    existingStoreId?: number,
  ) {
    const storeIdAtRequest = existingStoreId ?? currentStoreId.value
    if (!storeIdAtRequest) {
      clearProviderState()
      return
    }

    const requestToken = existingRequestToken ?? getNextLoadToken()
    beginResourceLoading(provider, requestToken, storeIdAtRequest)

    try {
      const [devicesResponse, roomsResponse] = await Promise.all([
        getDoorLockDevices({ provider }),
        getDoorLockRooms({ provider }),
      ])
      if (!isActiveRequest(requestToken, storeIdAtRequest, provider)) {
        return
      }

      if (!devicesResponse.success) {
        throw new Error(getResponseErrorText(devicesResponse, t('settings.doorLocks.messages.loadFailed')))
      }

      if (!roomsResponse.success) {
        throw new Error(getResponseErrorText(roomsResponse, t('settings.doorLocks.messages.loadFailed')))
      }

      devices.value = devicesResponse.data || []
      rooms.value = roomsResponse.data || []
      syncSelectedDevicesFromRooms()
    } catch (error) {
      if (isActiveRequest(requestToken, storeIdAtRequest, provider)) {
        devices.value = []
        rooms.value = []
        selectedDeviceByRoomId.value = {}
        errorMessage.value = getErrorText(error, t('settings.doorLocks.messages.loadFailed'))
      }
    } finally {
      endResourceLoading(provider, requestToken, storeIdAtRequest)
    }
  }

  function syncSelectedDevicesFromRooms() {
    const nextSelected: Record<number, DoorLockRoleSelection> = {}
    for (const room of rooms.value) {
      const binding = getRoomBinding(room)
      const roomId = getRoomId(room)
      if (roomId) {
        if (binding?.provider && binding.provider !== activeProvider.value) {
          nextSelected[roomId] = createEmptyRoleSelection()
          continue
        }
        nextSelected[roomId] = {
          controlDeviceKey: binding ? getBindingRoleDeviceKey(binding, ROLE_CONTROL) : '',
          passcodeDeviceKey: binding ? getBindingRoleDeviceKey(binding, ROLE_PASSCODE) : '',
        }
      }
    }
    selectedDeviceByRoomId.value = nextSelected
  }

  function validateSwitchBotToken(_rule: unknown, value: string, callback: (error?: Error) => void) {
    if (activeProvider.value !== PROVIDER_SWITCHBOT || !isNewIntegration.value) {
      callback()
      return
    }

    if (value?.trim()) {
      callback()
      return
    }

    callback(new Error(t('settings.doorLocks.messages.configRequired')))
  }

  function validateSwitchBotSecret(_rule: unknown, value: string, callback: (error?: Error) => void) {
    if (activeProvider.value !== PROVIDER_SWITCHBOT || !isNewIntegration.value) {
      callback()
      return
    }

    if (value?.trim()) {
      callback()
      return
    }

    callback(new Error(t('settings.doorLocks.messages.configRequired')))
  }

  function validateTtLockClientId(_rule: unknown, value: string, callback: (error?: Error) => void) {
    if (activeProvider.value !== PROVIDER_TTLOCK || !isNewIntegration.value) {
      callback()
      return
    }

    if (value?.trim()) {
      callback()
      return
    }

    callback(new Error(t('settings.doorLocks.messages.configRequired')))
  }

  function validateTtLockClientSecret(
    _rule: unknown,
    value: string,
    callback: (error?: Error) => void,
  ) {
    if (activeProvider.value !== PROVIDER_TTLOCK || !isNewIntegration.value) {
      callback()
      return
    }

    if (value?.trim()) {
      callback()
      return
    }

    callback(new Error(t('settings.doorLocks.messages.configRequired')))
  }

  function validateTtLockUsername(_rule: unknown, value: string, callback: (error?: Error) => void) {
    if (activeProvider.value !== PROVIDER_TTLOCK || !isNewIntegration.value) {
      callback()
      return
    }

    if (value?.trim()) {
      callback()
      return
    }

    callback(new Error(t('settings.doorLocks.messages.configRequired')))
  }

  function validateTtLockPassword(_rule: unknown, value: string, callback: (error?: Error) => void) {
    if (activeProvider.value !== PROVIDER_TTLOCK || !isNewIntegration.value) {
      callback()
      return
    }

    if (value?.trim()) {
      callback()
      return
    }

    callback(new Error(t('settings.doorLocks.messages.configRequired')))
  }

  async function validateConfigForm() {
    if (!configFormRef.value) {
      return false
    }

    try {
      await configFormRef.value.validate()
      return true
    } catch {
      return false
    }
  }

  function addCredential(payload: DoorLockIntegrationPayload, key: CredentialPayloadKey, value: string) {
    const trimmedValue = value.trim()
    if (trimmedValue) {
      payload[key] = trimmedValue
    }
  }

  function buildConfigPayload(provider: DoorLockProvider): DoorLockIntegrationPayload {
    const form = configForms.value[provider]
    const payload: DoorLockIntegrationPayload = {
      provider,
      enabled: form.enabled,
      name: form.name.trim(),
    }

    if (provider === PROVIDER_SWITCHBOT) {
      addCredential(payload, 'switchBotToken', form.token)
      addCredential(payload, 'switchBotSecret', form.secret)
    } else {
      addCredential(payload, 'ttLockClientId', form.clientId)
      addCredential(payload, 'ttLockClientSecret', form.clientSecret)
      addCredential(payload, 'ttLockUsername', form.username)
      addCredential(payload, 'ttLockPassword', form.password)
    }

    return payload
  }

  async function handleSaveConfig() {
    const providerAtRequest = activeProvider.value
    const storeIdAtRequest = currentStoreId.value
    if (!storeIdAtRequest) {
      ElMessage.warning(t('settings.doorLocks.messages.selectStore'))
      return
    }

    const valid = await validateConfigForm()
    if (!isCurrentProviderRequest(storeIdAtRequest, providerAtRequest)) {
      return
    }

    if (!valid) {
      ElMessage.warning(t('settings.doorLocks.messages.configRequired'))
      return
    }

    setProviderActionLoading(savingConfigProviders, providerAtRequest, true)
    try {
      const integration = integrations.value[providerAtRequest]
      const payload = buildConfigPayload(providerAtRequest)
      const response = integration?.id
        ? await updateDoorLockIntegration(integration.id, payload)
        : await createDoorLockIntegration(payload)

      if (!isCurrentStoreRequest(storeIdAtRequest)) {
        return
      }

      if (!response.success || !response.data) {
        throw new Error(getResponseErrorText(response, t('settings.doorLocks.messages.saveFailed')))
      }

      integrations.value[payload.provider] = response.data
      applyIntegrationToForm(response.data)
      if (isCurrentProviderRequest(storeIdAtRequest, providerAtRequest)) {
        ElMessage.success(t('settings.doorLocks.messages.saveSuccess'))
        await loadProviderResources(providerAtRequest, undefined, storeIdAtRequest)
      }
    } catch (error) {
      if (isCurrentProviderRequest(storeIdAtRequest, providerAtRequest)) {
        ElMessage.error(getErrorText(error, t('settings.doorLocks.messages.saveFailed')))
      }
    } finally {
      setProviderActionLoading(savingConfigProviders, providerAtRequest, false)
    }
  }

  async function handleTestConnection() {
    const providerAtRequest = activeProvider.value
    const integration = integrations.value[providerAtRequest]
    const storeIdAtRequest = currentStoreId.value
    if (!integration?.id || !storeIdAtRequest) {
      ElMessage.warning(t('settings.doorLocks.messages.saveConfigFirst'))
      return
    }

    setProviderActionLoading(testingConnectionProviders, providerAtRequest, true)
    try {
      const response = await testDoorLockIntegration(integration.id)
      if (!isCurrentStoreRequest(storeIdAtRequest)) {
        return
      }

      if (!response.success || response.data?.success === false) {
        throw new Error(getResponseErrorText(response, t('settings.doorLocks.messages.testFailed')))
      }

      if (isCurrentProviderRequest(storeIdAtRequest, providerAtRequest)) {
        ElMessage.success(t('settings.doorLocks.messages.testSuccess'))
        await loadProviderResources(providerAtRequest, undefined, storeIdAtRequest)
      }
    } catch (error) {
      if (isCurrentProviderRequest(storeIdAtRequest, providerAtRequest)) {
        ElMessage.error(getErrorText(error, t('settings.doorLocks.messages.testFailed')))
      }
    } finally {
      setProviderActionLoading(testingConnectionProviders, providerAtRequest, false)
    }
  }

  async function handleRefreshToken() {
    const providerAtRequest = activeProvider.value
    const integration = integrations.value[providerAtRequest]
    const storeIdAtRequest = currentStoreId.value
    if (!integration?.id || !storeIdAtRequest) {
      ElMessage.warning(t('settings.doorLocks.messages.saveConfigFirst'))
      return
    }

    setProviderActionLoading(refreshingTokenProviders, providerAtRequest, true)
    try {
      const response = await refreshDoorLockToken(integration.id)
      if (!isCurrentStoreRequest(storeIdAtRequest)) {
        return
      }

      if (!response.success || !response.data) {
        throw new Error(getResponseErrorText(response, t('settings.doorLocks.messages.refreshFailed')))
      }

      integrations.value[providerAtRequest] = response.data
      applyIntegrationToForm(response.data)
      if (isCurrentProviderRequest(storeIdAtRequest, providerAtRequest)) {
        ElMessage.success(t('settings.doorLocks.messages.refreshSuccess'))
        await loadProviderResources(providerAtRequest, undefined, storeIdAtRequest)
      }
    } catch (error) {
      if (isCurrentProviderRequest(storeIdAtRequest, providerAtRequest)) {
        ElMessage.error(getErrorText(error, t('settings.doorLocks.messages.refreshFailed')))
      }
    } finally {
      setProviderActionLoading(refreshingTokenProviders, providerAtRequest, false)
    }
  }

  async function handleSyncDevices() {
    const providerAtRequest = activeProvider.value
    const integration = integrations.value[providerAtRequest]
    const storeIdAtRequest = currentStoreId.value
    if (!integration?.id || !storeIdAtRequest) {
      ElMessage.warning(t('settings.doorLocks.messages.saveConfigFirst'))
      return
    }

    setProviderActionLoading(syncingDevicesProviders, providerAtRequest, true)
    try {
      const response = await syncDoorLockDevices(integration.id)
      if (!isCurrentStoreRequest(storeIdAtRequest)) {
        return
      }

      if (!response.success) {
        throw new Error(getResponseErrorText(response, t('settings.doorLocks.messages.syncFailed')))
      }

      if (isCurrentProviderRequest(storeIdAtRequest, providerAtRequest)) {
        ElMessage.success(t('settings.doorLocks.messages.syncSuccess'))
        await loadProviderResources(providerAtRequest, undefined, storeIdAtRequest)
      }
    } catch (error) {
      if (isCurrentProviderRequest(storeIdAtRequest, providerAtRequest)) {
        ElMessage.error(getErrorText(error, t('settings.doorLocks.messages.syncFailed')))
      }
    } finally {
      setProviderActionLoading(syncingDevicesProviders, providerAtRequest, false)
    }
  }

  async function handleSaveBinding(room: DoorLockRoomDTO) {
    const providerAtRequest = activeProvider.value
    const roomId = getRoomId(room)
    const integration = integrations.value[providerAtRequest]
    const storeIdAtRequest = currentStoreId.value

    if (!storeIdAtRequest) {
      ElMessage.warning(t('settings.doorLocks.messages.selectStore'))
      return
    }

    if (!integration?.id) {
      ElMessage.warning(t('settings.doorLocks.messages.saveConfigFirst'))
      return
    }

    const validation = getRoomBindingValidation(room)
    if (!validation.canSave) {
      ElMessage.warning(
        validation.messages[0] || t('settings.doorLocks.messages.selectAtLeastOneRoleDevice'),
      )
      return
    }

    setRoomActionLoading(savingBindingKeys, providerAtRequest, roomId, true)
    try {
      const bindingPayload = buildBindingPayload(room, integration.id)
      const response = await saveDoorLockBinding(bindingPayload)
      if (!isCurrentStoreRequest(storeIdAtRequest)) {
        return
      }

      if (!response.success) {
        throw new Error(getResponseErrorText(response, t('settings.doorLocks.messages.bindFailed')))
      }

      if (isCurrentProviderRequest(storeIdAtRequest, providerAtRequest)) {
        ElMessage.success(t('settings.doorLocks.messages.bindSuccess'))
        await loadProviderResources(providerAtRequest, undefined, storeIdAtRequest)
      }
    } catch (error) {
      if (isCurrentProviderRequest(storeIdAtRequest, providerAtRequest)) {
        ElMessage.error(getErrorText(error, t('settings.doorLocks.messages.bindFailed')))
      }
    } finally {
      setRoomActionLoading(savingBindingKeys, providerAtRequest, roomId, false)
    }
  }

  async function handleUnbind(room: DoorLockRoomDTO) {
    const providerAtRequest = activeProvider.value
    const binding = getRoomBinding(room)
    const storeIdAtRequest = currentStoreId.value
    if (!binding?.id || !storeIdAtRequest) {
      return
    }

    try {
      await ElMessageBox.confirm(
        t('settings.doorLocks.messages.unbindConfirm', {
          device: getRoomBoundDeviceName(room),
          room: getRoomDisplayName(room),
        }),
        t('settings.doorLocks.messages.unbindConfirmTitle'),
        {
          confirmButtonText: t('settings.common.confirmButton'),
          cancelButtonText: t('settings.common.cancelButton'),
          type: 'warning',
        },
      )

      if (!isCurrentStoreRequest(storeIdAtRequest)) {
        return
      }

      const roomId = getRoomId(room)
      setRoomActionLoading(unbindingKeys, providerAtRequest, roomId, true)
      const response = await deleteDoorLockBinding(binding.id)
      if (!isCurrentStoreRequest(storeIdAtRequest)) {
        return
      }

      if (!response.success) {
        throw new Error(getResponseErrorText(response, t('settings.doorLocks.messages.unbindFailed')))
      }

      if (isCurrentProviderRequest(storeIdAtRequest, providerAtRequest)) {
        ElMessage.success(t('settings.doorLocks.messages.unbindSuccess'))
        await loadProviderResources(providerAtRequest, undefined, storeIdAtRequest)
      }
    } catch (error) {
      if (error !== 'cancel' && isCurrentProviderRequest(storeIdAtRequest, providerAtRequest)) {
        ElMessage.error(getErrorText(error, t('settings.doorLocks.messages.unbindFailed')))
      }
    } finally {
      setRoomActionLoading(unbindingKeys, providerAtRequest, getRoomId(room), false)
    }
  }

  function handleProviderTabChange() {
    clearProviderState()
    nextTick(() => {
      configFormRef.value?.clearValidate()
    })
    loadProviderResources(activeProvider.value)
  }

  function handleRoomTypeChange() {
    selectedDeviceByRoomId.value = { ...selectedDeviceByRoomId.value }
  }

  function handleClearRoomTypeFilter() {
    roomTypeFilter.value = EMPTY_ROOM_TYPE_FILTER
    handleRoomTypeChange()
  }

  function handleReload() {
    loadPageData()
  }

  function getIntegrationTagType(provider: DoorLockProvider) {
    return integrations.value[provider] ? 'success' : 'info'
  }

  function getIntegrationStatusLabel(provider: DoorLockProvider) {
    return integrations.value[provider]
      ? t('settings.doorLocks.status.configured')
      : t('settings.doorLocks.status.notConfigured')
  }

  function buildMaskedCredentialItem(label: string, value?: string) {
    if (!value) {
      return null
    }

    return { label, value }
  }

  function getMaskedCredential(
    integration: DoorLockIntegrationDTO,
    key: keyof NonNullable<DoorLockIntegrationDTO['maskedCredentials']>,
  ) {
    if (integration.maskedCredentials?.[key]) {
      return integration.maskedCredentials[key]
    }

    return ''
  }

  function getDeviceKey(device: DoorLockDeviceDTO) {
    const key = device.id || device.providerLockId || device.providerDeviceId
    return String(key || '')
  }

  function getDeviceDisplayName(device: DoorLockDeviceDTO) {
    const name = device.lockName
    if (name) {
      return name
    }

    const deviceKey = getDeviceKey(device)
    if (!deviceKey) {
      return t('settings.doorLocks.status.unknown')
    }

    const visibleSuffix = deviceKey.slice(-MASKED_ID_VISIBLE_LENGTH)
    return `${t(`settings.doorLocks.providers.${device.provider}`)} ${t('settings.doorLocks.fields.device')} ****${visibleSuffix}`
  }

  function getDeviceStatusLabel(device: DoorLockDeviceDTO) {
    if (device.lockStatus) {
      return formatLockStatus(device.lockStatus)
    }

    if (device.online === false) {
      return t('settings.doorLocks.status.offline')
    }

    if (device.online === true) {
      return t('settings.doorLocks.status.online')
    }

    if (device.statusSource === 'UNAVAILABLE') {
      return t('settings.doorLocks.status.unavailable')
    }

    return t('settings.doorLocks.status.pendingRefresh')
  }

  function getDeviceStatusTagType(device: DoorLockDeviceDTO) {
    const normalizedStatus = normalizeDeviceType(device.lockStatus)
    if (normalizedStatus.includes('jam')) {
      return 'danger'
    }

    if (normalizedStatus.includes('unlock') || normalizedStatus.includes('open')) {
      return 'warning'
    }

    if (normalizedStatus.includes('lock') || normalizedStatus.includes('close')) {
      return 'success'
    }

    if (device.online === true) {
      return 'success'
    }

    if (device.online === false) {
      return 'info'
    }

    if (device.statusSource === 'UNAVAILABLE') {
      return 'info'
    }

    return 'warning'
  }

  function getDeviceCapabilityLabels(device: DoorLockDeviceDTO) {
    const labels: string[] = []
    if (canDeviceServeRole(device, ROLE_CONTROL)) {
      labels.push(t('settings.doorLocks.capabilities.control'))
    }

    if (canDeviceServeRole(device, ROLE_PASSCODE)) {
      labels.push(t('settings.doorLocks.capabilities.passcodeManagement'))
    }

    return labels
  }

  function getDeviceStatusSourceLabel(device: DoorLockDeviceDTO) {
    if (!isSwitchBotAuthenticationPanel(device)) {
      return ''
    }

    if (device.statusSource === STATUS_SOURCE_BOUND_LOCK || device.auxiliaryDeviceId) {
      return t('settings.doorLocks.status.statusFromBoundLock')
    }

    return t('settings.doorLocks.status.boundLockMissing')
  }

  function getRoomId(room: DoorLockRoomDTO) {
    return room.roomId ?? room.id ?? 0
  }

  function getRoomDisplayName(room: DoorLockRoomDTO) {
    return room.roomNumber || room.roomName || t('settings.doorLocks.status.unknown')
  }

  function getRoomTypeId(room: DoorLockRoomDTO) {
    return room.roomTypeId ?? room.roomType?.id ?? 0
  }

  function getRoomTypeName(room: DoorLockRoomDTO) {
    return room.roomTypeName || room.roomType?.name || '-'
  }

  function getRoomBinding(room: DoorLockRoomDTO): DoorLockBindingDTO | null {
    return room.binding || room.lockBinding || null
  }

  function getRoomBoundDeviceName(room: DoorLockRoomDTO) {
    const binding = getRoomBinding(room)
    if (!binding) {
      return '-'
    }

    const controlDevice = getRoomBoundRoleDeviceName(room, ROLE_CONTROL)
    const passcodeDevice = getRoomBoundRoleDeviceName(room, ROLE_PASSCODE)
    if (controlDevice === '-' && passcodeDevice === '-') {
      return '-'
    }

    return [
      `${t('settings.doorLocks.fields.controlDevice')}: ${controlDevice}`,
      `${t('settings.doorLocks.fields.passcodeDevice')}: ${passcodeDevice}`,
    ].join('; ')
  }

  function getRoomBoundRoleDeviceName(room: DoorLockRoomDTO, role: DoorLockBindingRole) {
    const binding = getRoomBinding(room)
    if (!binding) {
      return '-'
    }

    const roleName = getBindingRoleDeviceName(binding, role)
    if (roleName) {
      return roleName
    }

    const roleDeviceKey = getBindingRoleDeviceKey(binding, role)
    if (!roleDeviceKey) {
      return '-'
    }

    const device = findDeviceByKey(roleDeviceKey)
    if (device) {
      return getDeviceDisplayName(device)
    }

    return t('settings.doorLocks.status.unknown')
  }

  function getDeviceBoundRoomLabel(device: DoorLockDeviceDTO) {
    const boundRoom = rooms.value.find((room) => {
      const binding = getRoomBinding(room)
      return binding ? isBindingForDevice(binding, device) : false
    })
    if (!boundRoom) {
      return ''
    }

    return getRoomDisplayName(boundRoom)
  }

  function isDeviceBoundToOtherRoom(device: DoorLockDeviceDTO, room: DoorLockRoomDTO) {
    return isRoleDeviceBoundToOtherRoom(device, room)
  }

  function isRoleDeviceBoundToOtherRoom(device: DoorLockDeviceDTO, room: DoorLockRoomDTO) {
    const roomId = getRoomId(room)
    if (!roomId) {
      return false
    }

    return rooms.value.some((item) => {
      const binding = getRoomBinding(item)
      return Boolean(binding && getRoomId(item) !== roomId && isBindingForDevice(binding, device))
    })
  }

  function isRoomBoundToDifferentProvider(room: DoorLockRoomDTO) {
    const binding = getRoomBinding(room)
    return Boolean(binding?.provider && binding.provider !== activeProvider.value)
  }

  function getBindingDeviceKey(binding: DoorLockBindingDTO) {
    return String(binding.deviceId || binding.providerLockId || '')
  }

  function getBindingRoleDeviceKey(binding: DoorLockBindingDTO, role: DoorLockBindingRole) {
    const roleKey = getExplicitBindingRoleDeviceKey(binding, role)
    if (roleKey) {
      return roleKey
    }

    if (hasExplicitBindingRoleFields(binding)) {
      return ''
    }

    return getLegacyBindingRoleDeviceKey(binding, role)
  }

  function getExplicitBindingRoleDeviceKey(binding: DoorLockBindingDTO, role: DoorLockBindingRole) {
    if (role === ROLE_CONTROL) {
      return getBindingRoleKeyFromValues(
        binding.controlDeviceId,
        binding.controlProviderLockId,
        binding.controlDevice?.deviceId,
        binding.controlDevice?.providerLockId,
        binding.controlDevice?.providerDeviceId,
      )
    }

    return getBindingRoleKeyFromValues(
      binding.passcodeDeviceId,
      binding.passcodeProviderLockId,
      binding.passcodeDevice?.deviceId,
      binding.passcodeDevice?.providerLockId,
      binding.passcodeDevice?.providerDeviceId,
    )
  }

  function getBindingRoleKeyFromValues(
    primaryDeviceId?: number | null,
    primaryProviderLockId?: string | null,
    nestedDeviceId?: number,
    nestedProviderLockId?: string,
    nestedProviderDeviceId?: string,
  ) {
    if (primaryDeviceId) {
      return String(primaryDeviceId)
    }

    if (nestedDeviceId) {
      return String(nestedDeviceId)
    }

    if (primaryProviderLockId) {
      return primaryProviderLockId
    }

    if (nestedProviderLockId) {
      return nestedProviderLockId
    }

    if (nestedProviderDeviceId) {
      return nestedProviderDeviceId
    }

    return ''
  }

  function hasExplicitBindingRoleFields(binding: DoorLockBindingDTO) {
    return (
      binding.controlDeviceId !== undefined ||
      binding.controlProviderLockId !== undefined ||
      binding.controlDevice !== undefined ||
      binding.passcodeDeviceId !== undefined ||
      binding.passcodeProviderLockId !== undefined ||
      binding.passcodeDevice !== undefined
    )
  }

  function getLegacyBindingRoleDeviceKey(binding: DoorLockBindingDTO, role: DoorLockBindingRole) {
    const legacyKey = getBindingDeviceKey(binding)
    if (!legacyKey) {
      return ''
    }

    const device = findDeviceByKey(legacyKey)
    if (device) {
      if (canDeviceServeRole(device, role)) {
        return legacyKey
      }

      return ''
    }

    if (binding.provider !== PROVIDER_SWITCHBOT) {
      return legacyKey
    }

    return ''
  }

  function getBindingRoleDeviceName(binding: DoorLockBindingDTO, role: DoorLockBindingRole) {
    if (role === ROLE_CONTROL) {
      return (
        binding.controlLockName ||
        binding.controlDevice?.lockName ||
        getLegacyBindingRoleName(binding, role)
      )
    }

    return (
      binding.passcodeLockName ||
      binding.passcodeDevice?.lockName ||
      getLegacyBindingRoleName(binding, role)
    )
  }

  function getLegacyBindingRoleName(binding: DoorLockBindingDTO, role: DoorLockBindingRole) {
    if (!hasExplicitBindingRoleFields(binding) && getLegacyBindingRoleDeviceKey(binding, role)) {
      return binding.lockName || ''
    }

    return ''
  }

  function isBindingForDevice(binding: DoorLockBindingDTO, device: DoorLockDeviceDTO) {
    if (binding.provider && binding.provider !== device.provider) {
      return false
    }

    const deviceKey = getDeviceKey(device)
    if (!deviceKey) {
      return false
    }

    const bindingControlKey = getBindingRoleDeviceKey(binding, ROLE_CONTROL)
    const bindingPasscodeKey = getBindingRoleDeviceKey(binding, ROLE_PASSCODE)
    if (bindingControlKey === deviceKey || bindingPasscodeKey === deviceKey) {
      return true
    }

    if (device.providerLockId) {
      return (
        binding.controlProviderLockId === device.providerLockId ||
        binding.passcodeProviderLockId === device.providerLockId ||
        binding.providerLockId === device.providerLockId
      )
    }

    return false
  }

  function isSavingRoom(room: DoorLockRoomDTO) {
    return savingBindingKeys.value.has(getProviderRoomActionKey(activeProvider.value, getRoomId(room)))
  }

  function isUnbindingRoom(room: DoorLockRoomDTO) {
    return unbindingKeys.value.has(getProviderRoomActionKey(activeProvider.value, getRoomId(room)))
  }

  function canSaveRoomBinding(room: DoorLockRoomDTO) {
    return getRoomBindingValidation(room).canSave
  }

  function getRoomBindingHintMessages(room: DoorLockRoomDTO) {
    return getRoomBindingValidation(room).messages
  }

  function getRoomBindingValidation(room: DoorLockRoomDTO): RoomBindingValidation {
    const messages: string[] = []
    let hasBlockingError = false
    const selection = getRoomRoleSelection(room)
    const controlDevice = findDeviceByKey(selection.controlDeviceKey)
    const passcodeDevice = findDeviceByKey(selection.passcodeDeviceKey)
    const providerConflictMessage = getRoomProviderConflictMessage(room)

    if (providerConflictMessage) {
      messages.push(providerConflictMessage)
      hasBlockingError = true
    }

    if (!selection.controlDeviceKey && !selection.passcodeDeviceKey) {
      messages.push(t('settings.doorLocks.messages.selectAtLeastOneRoleDevice'))
      return {
        canSave: false,
        messages,
      }
    }

    if (!selection.controlDeviceKey) {
      messages.push(t('settings.doorLocks.hints.missingControlDevice'))
    }

    if (!selection.passcodeDeviceKey) {
      messages.push(t('settings.doorLocks.hints.missingPasscodeDevice'))
    }

    if (selection.controlDeviceKey) {
      if (!controlDevice || !canDeviceServeRole(controlDevice, ROLE_CONTROL)) {
        messages.push(t('settings.doorLocks.messages.roleDeviceCapabilityMismatch'))
        hasBlockingError = true
      } else if (isRoleDeviceUnavailableForRoom(controlDevice, room)) {
        messages.push(t('settings.doorLocks.messages.roleDeviceAlreadyBound'))
        hasBlockingError = true
      }
    }

    if (selection.passcodeDeviceKey) {
      if (!passcodeDevice || !canDeviceServeRole(passcodeDevice, ROLE_PASSCODE)) {
        messages.push(t('settings.doorLocks.messages.roleDeviceCapabilityMismatch'))
        hasBlockingError = true
      } else if (isRoleDeviceUnavailableForRoom(passcodeDevice, room)) {
        messages.push(t('settings.doorLocks.messages.roleDeviceAlreadyBound'))
        hasBlockingError = true
      }
    }

    if (
      controlDevice &&
      passcodeDevice &&
      selection.controlDeviceKey === selection.passcodeDeviceKey
    ) {
      if (
        !canDeviceServeRole(controlDevice, ROLE_CONTROL) ||
        !canDeviceServeRole(controlDevice, ROLE_PASSCODE)
      ) {
        messages.push(t('settings.doorLocks.hints.sameRoomSameDeviceAllowed'))
        hasBlockingError = true
      }
    }

    const switchBotMessage = getSwitchBotPasscodeValidationMessage(
      room,
      controlDevice,
      passcodeDevice,
    )
    if (switchBotMessage) {
      messages.push(switchBotMessage)
      hasBlockingError = true
    }

    if (hasAnyRoleBindingState(room) && !hasRoomBindingChanged(room)) {
      messages.push(t('settings.doorLocks.messages.noChangedRoleBinding'))
      hasBlockingError = true
    }

    return {
      canSave: Boolean(activeIntegration.value?.id && !isSavingRoom(room) && !hasBlockingError),
      messages,
    }
  }

  function getRoomRoleSelection(room: DoorLockRoomDTO) {
    return getRoleSelectionByRoomId(getRoomId(room))
  }

  function getRoleSelectionByRoomId(roomId: number) {
    return selectedDeviceByRoomId.value[roomId] || createEmptyRoleSelection()
  }

  function getSelectedRoleDeviceKey(room: DoorLockRoomDTO, role: DoorLockBindingRole) {
    const selection = getRoomRoleSelection(room)
    if (role === ROLE_CONTROL) {
      return selection.controlDeviceKey
    }

    return selection.passcodeDeviceKey
  }

  function setSelectedRoleDeviceKey(
    room: DoorLockRoomDTO,
    role: DoorLockBindingRole,
    value?: unknown,
  ) {
    const roomId = getRoomId(room)
    if (!roomId) {
      return
    }

    const currentSelection = getRoleSelectionByRoomId(roomId)
    const nextSelection = { ...currentSelection }
    const nextValue = value ? String(value) : ''
    if (role === ROLE_CONTROL) {
      nextSelection.controlDeviceKey = nextValue
    } else {
      nextSelection.passcodeDeviceKey = nextValue
    }

    selectedDeviceByRoomId.value = {
      ...selectedDeviceByRoomId.value,
      [roomId]: nextSelection,
    }
  }

  function handleControlDeviceChange(room: DoorLockRoomDTO, value?: unknown) {
    setSelectedRoleDeviceKey(room, ROLE_CONTROL, value)
    syncTtLockCounterpartRole(room, ROLE_PASSCODE, value)
  }

  function handlePasscodeDeviceChange(room: DoorLockRoomDTO, value?: unknown) {
    setSelectedRoleDeviceKey(room, ROLE_PASSCODE, value)
    syncTtLockCounterpartRole(room, ROLE_CONTROL, value)
  }

  function syncTtLockCounterpartRole(
    room: DoorLockRoomDTO,
    counterpartRole: DoorLockBindingRole,
    value?: unknown,
  ) {
    if (activeProvider.value !== PROVIDER_TTLOCK) {
      return
    }

    const deviceKey = value ? String(value) : ''
    if (!deviceKey) {
      setSelectedRoleDeviceKey(room, counterpartRole, '')
      return
    }

    const device = findDeviceByKey(deviceKey)
    if (!device || !canDeviceServeRole(device, counterpartRole)) {
      return
    }

    setSelectedRoleDeviceKey(room, counterpartRole, deviceKey)
  }

  function getRoleDeviceOptions(role: DoorLockBindingRole) {
    return devices.value.filter((device) => canDeviceServeRole(device, role))
  }

  function isRoleDeviceOptionDisabled(
    device: DoorLockDeviceDTO,
    room: DoorLockRoomDTO,
    role: DoorLockBindingRole,
  ) {
    if (!canDeviceServeRole(device, role)) {
      return true
    }

    if (isRoleDeviceUnavailableForRoom(device, room)) {
      return true
    }

    if (role === ROLE_PASSCODE && isSwitchBotAssociatedLockBoundToOtherRoom(device, room)) {
      return true
    }

    return false
  }

  function isRoleDeviceUnavailableForRoom(device: DoorLockDeviceDTO, room: DoorLockRoomDTO) {
    return (
      isRoleDeviceBoundToOtherRoom(device, room) ||
      isRoleDeviceSelectedByOtherRoom(device, room)
    )
  }

  function isRoleDeviceSelectedByOtherRoom(device: DoorLockDeviceDTO, room: DoorLockRoomDTO) {
    const deviceKey = getDeviceKey(device)
    const roomId = getRoomId(room)
    if (!deviceKey || !roomId) {
      return false
    }

    for (const [selectedRoomId, selection] of Object.entries(selectedDeviceByRoomId.value)) {
      if (Number(selectedRoomId) === roomId) {
        continue
      }

      if (selection.controlDeviceKey === deviceKey || selection.passcodeDeviceKey === deviceKey) {
        return true
      }
    }

    return false
  }

  function hasRoomBindingChanged(room: DoorLockRoomDTO) {
    const binding = getRoomBinding(room)
    const selection = getRoomRoleSelection(room)
    if (!binding) {
      return Boolean(selection.controlDeviceKey || selection.passcodeDeviceKey)
    }

    const bindingControlKey = getBindingRoleDeviceKey(binding, ROLE_CONTROL)
    const bindingPasscodeKey = getBindingRoleDeviceKey(binding, ROLE_PASSCODE)
    return (
      bindingControlKey !== selection.controlDeviceKey ||
      bindingPasscodeKey !== selection.passcodeDeviceKey
    )
  }

  function hasAnyRoleBindingState(room: DoorLockRoomDTO) {
    const selection = getRoomRoleSelection(room)
    return Boolean(
      getRoomBinding(room) ||
        selection.controlDeviceKey ||
        selection.passcodeDeviceKey,
    )
  }

  function buildBindingPayload(
    room: DoorLockRoomDTO,
    integrationId: number,
  ): DoorLockBindingPayload {
    const selection = getRoomRoleSelection(room)
    const controlDevice = findDeviceByKey(selection.controlDeviceKey)
    const passcodeDevice = findDeviceByKey(selection.passcodeDeviceKey)
    const payload: DoorLockBindingPayload = {
      integrationId,
      roomId: getRoomId(room),
      controlDeviceId: null,
      controlProviderLockId: null,
      passcodeDeviceId: null,
      passcodeProviderLockId: null,
    }

    applyRoleDeviceToPayload(payload, ROLE_CONTROL, controlDevice)
    applyRoleDeviceToPayload(payload, ROLE_PASSCODE, passcodeDevice)
    return payload
  }

  function applyRoleDeviceToPayload(
    payload: DoorLockBindingPayload,
    role: DoorLockBindingRole,
    device?: DoorLockDeviceDTO,
  ) {
    const deviceId = device?.id || null
    const providerLockId = device ? getDeviceProviderLockPayloadValue(device) : null
    if (role === ROLE_CONTROL) {
      payload.controlDeviceId = deviceId
      payload.controlProviderLockId = providerLockId
      return
    }

    payload.passcodeDeviceId = deviceId
    payload.passcodeProviderLockId = providerLockId
  }

  function getDeviceProviderLockPayloadValue(device: DoorLockDeviceDTO) {
    return device.providerLockId || device.providerDeviceId || null
  }

  function findDeviceByKey(deviceKey?: string) {
    if (!deviceKey) {
      return undefined
    }

    return devices.value.find((device) => getDeviceKey(device) === deviceKey)
  }

  function canDeviceServeRole(device: DoorLockDeviceDTO, role: DoorLockBindingRole) {
    const explicitCapability = getExplicitRoleCapability(device, role)
    if (explicitCapability !== null) {
      return explicitCapability
    }

    if (role === ROLE_CONTROL) {
      return hasFallbackControlCapability(device)
    }

    return hasFallbackPasscodeCapability(device)
  }

  function getExplicitRoleCapability(device: DoorLockDeviceDTO, role: DoorLockBindingRole) {
    if (role === ROLE_CONTROL && typeof device.supportsControl === 'boolean') {
      return device.supportsControl
    }

    if (role === ROLE_PASSCODE && typeof device.supportsPasscode === 'boolean') {
      return device.supportsPasscode
    }

    if (!device.capabilities?.length) {
      return null
    }

    if (role === ROLE_CONTROL) {
      return hasCapabilityValue(device, CONTROL_CAPABILITY_VALUES)
    }

    return hasCapabilityValue(device, PASSCODE_CAPABILITY_VALUES)
  }

  function hasCapabilityValue(device: DoorLockDeviceDTO, expectedValues: string[]) {
    if (!device.capabilities?.length) {
      return false
    }

    for (const capability of device.capabilities) {
      const normalizedCapability = normalizeCapability(capability)
      if (expectedValues.includes(normalizedCapability)) {
        return true
      }
    }

    return false
  }

  function hasFallbackControlCapability(device: DoorLockDeviceDTO) {
    if (device.provider === PROVIDER_TTLOCK) {
      return true
    }

    if (device.provider !== PROVIDER_SWITCHBOT) {
      return true
    }

    if (isSwitchBotAuthenticationPanel(device)) {
      return false
    }

    return normalizeDeviceType(device.deviceType).includes(SWITCHBOT_CONTROL_DEVICE_TYPE_KEYWORD)
  }

  function hasFallbackPasscodeCapability(device: DoorLockDeviceDTO) {
    if (device.provider === PROVIDER_TTLOCK) {
      return true
    }

    if (device.provider !== PROVIDER_SWITCHBOT) {
      return true
    }

    return SWITCHBOT_PASSCODE_DEVICE_TYPES.includes(normalizeDeviceType(device.deviceType))
  }

  function getSwitchBotPasscodeValidationMessage(
    room: DoorLockRoomDTO,
    controlDevice?: DoorLockDeviceDTO,
    passcodeDevice?: DoorLockDeviceDTO,
  ) {
    if (!passcodeDevice || !isSwitchBotAuthenticationPanel(passcodeDevice)) {
      return ''
    }

    if (isSwitchBotAssociatedLockBoundToOtherRoom(passcodeDevice, room)) {
      return t('settings.doorLocks.messages.switchBotPasscodeLockAlreadyBound')
    }

    if (controlDevice && isSwitchBotPasscodeControlMismatch(passcodeDevice, controlDevice)) {
      return t('settings.doorLocks.messages.switchBotPasscodeLockMismatch')
    }

    return ''
  }

  function isSwitchBotAssociatedLockBoundToOtherRoom(
    passcodeDevice: DoorLockDeviceDTO,
    room: DoorLockRoomDTO,
  ) {
    const associatedControlDevice = getSwitchBotAssociatedControlDevice(passcodeDevice)
    if (!associatedControlDevice) {
      return false
    }

    return isRoleDeviceUnavailableForRoom(associatedControlDevice, room)
  }

  function isSwitchBotPasscodeControlMismatch(
    passcodeDevice: DoorLockDeviceDTO,
    controlDevice: DoorLockDeviceDTO,
  ) {
    const associatedLockKey = normalizeExternalKey(passcodeDevice.auxiliaryDeviceId)
    if (!associatedLockKey) {
      return false
    }

    return !getDeviceExternalKeys(controlDevice).includes(associatedLockKey)
  }

  function getSwitchBotAssociatedControlDevice(passcodeDevice: DoorLockDeviceDTO) {
    if (!isSwitchBotAuthenticationPanel(passcodeDevice)) {
      return undefined
    }

    const associatedLockKey = normalizeExternalKey(passcodeDevice.auxiliaryDeviceId)
    if (!associatedLockKey) {
      return undefined
    }

    return devices.value.find((device) => {
      if (!canDeviceServeRole(device, ROLE_CONTROL)) {
        return false
      }

      return getDeviceExternalKeys(device).includes(associatedLockKey)
    })
  }

  function getDeviceExternalKeys(device: DoorLockDeviceDTO) {
    return [
      normalizeExternalKey(device.providerDeviceId),
      normalizeExternalKey(device.providerLockId),
      normalizeExternalKey(device.statusSourceDeviceId),
    ].filter(Boolean)
  }

  function getDeviceBatteryLabel(device: DoorLockDeviceDTO) {
    if (typeof device.battery !== 'number') {
      if (device.statusSource === 'UNAVAILABLE') {
        return t('settings.doorLocks.status.unavailable')
      }

      return t('settings.doorLocks.status.pendingRefresh')
    }

    return formatBattery(device.battery)
  }

  function formatBattery(value: number) {
    return `${Math.round(value)}%`
  }

  function formatDateTime(value?: string) {
    if (!value) {
      return '-'
    }

    const date = new Date(value)
    if (Number.isNaN(date.getTime())) {
      return value
    }

    return date.toLocaleString()
  }

  function formatLockStatus(status: string) {
    const normalizedStatus = normalizeDeviceType(status)
    if (normalizedStatus.includes('jam')) {
      return t('settings.doorLocks.status.jammed')
    }

    if (normalizedStatus.includes('unlock') || normalizedStatus.includes('open')) {
      return t('settings.doorLocks.status.unlocked')
    }

    if (normalizedStatus.includes('lock') || normalizedStatus.includes('close')) {
      return t('settings.doorLocks.status.locked')
    }

    return t('settings.doorLocks.status.unknown')
  }

  function normalizeDeviceType(value?: string) {
    if (!value) {
      return ''
    }

    return value.trim().toLowerCase()
  }

  function normalizeCapability(value?: string) {
    if (!value) {
      return ''
    }

    return value.trim().toUpperCase().replace(/[\s-]+/g, '_')
  }

  function normalizeExternalKey(value?: string) {
    if (!value) {
      return ''
    }

    return value.trim().toLowerCase()
  }

  function getRoomProviderConflictMessage(room: DoorLockRoomDTO) {
    const binding = getRoomBinding(room)
    if (!binding?.provider || binding.provider === activeProvider.value) {
      return ''
    }

    return t('settings.doorLocks.messages.providerConflict', {
      provider: getProviderLabel(binding.provider),
    })
  }

  function getProviderLabel(provider: DoorLockProvider) {
    return t(`settings.doorLocks.providers.${provider}`)
  }

  function isSwitchBotAuthenticationPanel(device: DoorLockDeviceDTO) {
    if (device.provider !== PROVIDER_SWITCHBOT) {
      return false
    }

    return SWITCHBOT_AUTHENTICATION_PANEL_TYPES.includes(normalizeDeviceType(device.deviceType))
  }

  watch(currentStoreId, (storeId, previousStoreId) => {
    if (storeId === previousStoreId) {
      return
    }

    resetPageState()
    if (storeId) {
      loadPageData()
    } else {
      errorMessage.value = t('settings.doorLocks.messages.selectStore')
    }
  })

  onMounted(() => {
    loadPageData()
  })

  return {
    PROVIDER_SWITCHBOT,
    PROVIDER_TTLOCK,
    EMPTY_ROOM_TYPE_FILTER,
    ROLE_CONTROL,
    ROLE_PASSCODE,
    t,
    providerOptions,
    activeProvider,
    pageLoading,
    resourceLoading,
    savingConfig,
    testingConnection,
    refreshingToken,
    syncingDevices,
    errorMessage,
    roomTypeFilter,
    devices,
    configForms,
    selectedDeviceByRoomId,
    currentStoreId,
    currentStoreName,
    activeIntegration,
    activeConfigForm,
    isNewIntegration,
    configRules,
    roomTypeOptions,
    filteredRooms,
    maskedCredentialList,
    configFormRef,
    clearErrorMessage,
    handleSaveConfig,
    handleTestConnection,
    handleRefreshToken,
    handleSyncDevices,
    handleSaveBinding,
    handleUnbind,
    handleControlDeviceChange,
    handlePasscodeDeviceChange,
    handleProviderTabChange,
    handleRoomTypeChange,
    handleClearRoomTypeFilter,
    handleReload,
    getIntegrationTagType,
    getIntegrationStatusLabel,
    getDeviceKey,
    getDeviceDisplayName,
    getDeviceStatusLabel,
    getDeviceStatusTagType,
    getDeviceCapabilityLabels,
    getDeviceStatusSourceLabel,
    getRoomId,
    getRoomDisplayName,
    getRoomTypeName,
    getRoomBinding,
    getRoomBoundDeviceName,
    getRoomBoundRoleDeviceName,
    getSelectedRoleDeviceKey,
    getRoleDeviceOptions,
    isRoleDeviceOptionDisabled,
    getRoomBindingHintMessages,
    getDeviceBoundRoomLabel,
    isDeviceBoundToOtherRoom,
    isSavingRoom,
    isUnbindingRoom,
    isRoomBoundToDifferentProvider,
    canSaveRoomBinding,
    getDeviceBatteryLabel,
    formatDateTime,
  }
}
