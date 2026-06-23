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
const SWITCHBOT_AUTHENTICATION_PANEL_TYPES = [
  'keypad',
  'keypad touch',
  'keypad vision',
  'keypad vision pro',
]
const SWITCHBOT_PASSCODE_DEVICE_TYPES = [
  'keypad',
  'keypad touch',
  'keypad vision',
  'keypad vision pro',
  'lock vision',
  'lock vision pro',
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
  const selectedDeviceByRoomId = ref<Record<number, string>>({})
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

  function getErrorText(_error: unknown, fallback: string) {
    return fallback
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
        throw new Error(t('settings.doorLocks.messages.loadFailed'))
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
        throw new Error(t('settings.doorLocks.messages.loadFailed'))
      }

      if (!roomsResponse.success) {
        throw new Error(t('settings.doorLocks.messages.loadFailed'))
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
    const nextSelected: Record<number, string> = {}
    for (const room of rooms.value) {
      const binding = getRoomBinding(room)
      const roomId = getRoomId(room)
      if (binding?.deviceId && roomId) {
        nextSelected[roomId] = String(binding.deviceId)
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
        throw new Error(t('settings.doorLocks.messages.saveFailed'))
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
        throw new Error(t('settings.doorLocks.messages.testFailed'))
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
        throw new Error(t('settings.doorLocks.messages.refreshFailed'))
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
        throw new Error(t('settings.doorLocks.messages.syncFailed'))
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
    const deviceId = selectedDeviceByRoomId.value[roomId]
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

    if (!deviceId) {
      ElMessage.warning(t('settings.doorLocks.messages.selectDevice'))
      return
    }

    const selectedDevice = devices.value.find((device) => getDeviceKey(device) === deviceId)
    if (!selectedDevice) {
      ElMessage.warning(t('settings.doorLocks.messages.selectDevice'))
      return
    }

    if (selectedDevice && isDeviceBoundToOtherRoom(selectedDevice, room)) {
      ElMessage.warning(t('settings.doorLocks.hints.deviceReuse'))
      return
    }

    const currentBinding = getRoomBinding(room)
    if (currentBinding && getBindingDeviceKey(currentBinding) === deviceId) {
      ElMessage.info(t('settings.doorLocks.messages.noChangedBinding'))
      return
    }

    setRoomActionLoading(savingBindingKeys, providerAtRequest, roomId, true)
    try {
      const bindingPayload: DoorLockBindingPayload = {
        integrationId: integration.id,
        roomId,
      }
      if (selectedDevice.id) {
        bindingPayload.deviceId = selectedDevice.id
      } else {
        bindingPayload.provider = providerAtRequest
        bindingPayload.providerLockId = selectedDevice.providerLockId
      }

      const response = await saveDoorLockBinding(bindingPayload)
      if (!isCurrentStoreRequest(storeIdAtRequest)) {
        return
      }

      if (!response.success) {
        throw new Error(t('settings.doorLocks.messages.bindFailed'))
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
        throw new Error(t('settings.doorLocks.messages.unbindFailed'))
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

    return t('settings.doorLocks.status.pendingOrUnavailable')
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

    return 'warning'
  }

  function getDeviceCapabilityLabels(device: DoorLockDeviceDTO) {
    if (!device.provider) {
      return []
    }

    if (isSwitchBotAuthenticationPanel(device)) {
      return [t('settings.doorLocks.capabilities.passcode')]
    }

    const labels = [
      t('settings.doorLocks.capabilities.lock'),
      t('settings.doorLocks.capabilities.unlock'),
    ]
    if (hasPasscodeCapability(device)) {
      labels.push(t('settings.doorLocks.capabilities.passcode'))
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

    if (binding.lockName) {
      return binding.lockName
    }

    const device = devices.value.find((item) => isBindingForDevice(binding, item))
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
    const roomId = getRoomId(room)
    if (!roomId) {
      return false
    }

    return rooms.value.some((item) => {
      const binding = getRoomBinding(item)
      return Boolean(binding && getRoomId(item) !== roomId && isBindingForDevice(binding, device))
    })
  }

  function getBindingDeviceKey(binding: DoorLockBindingDTO) {
    return String(binding.deviceId || binding.providerLockId || '')
  }

  function isBindingForDevice(binding: DoorLockBindingDTO, device: DoorLockDeviceDTO) {
    const deviceKey = getDeviceKey(device)
    if (!deviceKey) {
      return false
    }

    return getBindingDeviceKey(binding) === deviceKey || binding.providerLockId === device.providerLockId
  }

  function isSavingRoom(room: DoorLockRoomDTO) {
    return savingBindingKeys.value.has(getProviderRoomActionKey(activeProvider.value, getRoomId(room)))
  }

  function isUnbindingRoom(room: DoorLockRoomDTO) {
    return unbindingKeys.value.has(getProviderRoomActionKey(activeProvider.value, getRoomId(room)))
  }

  function canSaveRoomBinding(room: DoorLockRoomDTO) {
    const roomId = getRoomId(room)
    const selectedDeviceId = selectedDeviceByRoomId.value[roomId]
    const selectedDevice = devices.value.find((device) => getDeviceKey(device) === selectedDeviceId)
    if (selectedDevice && isDeviceBoundToOtherRoom(selectedDevice, room)) {
      return false
    }

    return Boolean(activeIntegration.value?.id && selectedDeviceId && !isSavingRoom(room))
  }

  function getDeviceBatteryLabel(device: DoorLockDeviceDTO) {
    if (typeof device.battery !== 'number') {
      return t('settings.doorLocks.status.pendingOrUnavailable')
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

  function isSwitchBotAuthenticationPanel(device: DoorLockDeviceDTO) {
    if (device.provider !== PROVIDER_SWITCHBOT) {
      return false
    }

    return SWITCHBOT_AUTHENTICATION_PANEL_TYPES.includes(normalizeDeviceType(device.deviceType))
  }

  function hasPasscodeCapability(device: DoorLockDeviceDTO) {
    if (device.provider !== PROVIDER_SWITCHBOT) {
      return true
    }

    return SWITCHBOT_PASSCODE_DEVICE_TYPES.includes(normalizeDeviceType(device.deviceType))
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
    getDeviceBoundRoomLabel,
    isDeviceBoundToOtherRoom,
    isSavingRoom,
    isUnbindingRoom,
    canSaveRoomBinding,
    getDeviceBatteryLabel,
    formatDateTime,
  }
}
