<template>
  <el-dialog
    v-model="visible"
    :title="t('stage6.components.connectOtaWidget.title', { name: otaName })"
    width="800px"
    :close-on-click-modal="false"
    @close="handleClose"
  >
    <div class="widget-container">
      <div v-if="loading" class="loading-container">
        <el-icon class="is-loading" :size="40">
          <Loading />
        </el-icon>
        <p>{{ t('stage6.components.connectOtaWidget.loading') }}</p>
      </div>

      <div v-else-if="error" class="error-container">
        <el-icon :size="40" color="#f56c6c">
          <CircleClose />
        </el-icon>
        <p class="error-message">{{ error }}</p>
        <el-button type="primary" @click="loadWidget">{{ t('stage6.common.actions.retry') }}</el-button>
      </div>

      <!-- Su Widget mount point -->
      <div v-else id="su-widget-container" class="widget-content"></div>
    </div>

    <template #footer>
      <div class="dialog-footer">
        <div class="language-selector">
          <span class="language-label">{{ t('stage6.components.connectOtaWidget.language') }}</span>
          <el-select
            v-model="widgetLanguage"
            size="small"
            style="width: 120px"
            :disabled="loading"
            @change="handleWidgetLanguageChange"
          >
            <el-option :label="t('stage6.components.connectOtaWidget.chinese')" value="zn" />
            <el-option label="English" value="en" />
          </el-select>
        </div>
        <el-button
          v-if="false"
          type="primary"
          :loading="syncingRooms"
          :disabled="loading"
          @click="handleSyncRooms"
        >
          {{ t('stage6.components.connectOtaWidget.syncForCertification') }}
        </el-button>
        <el-button @click="handleClose">{{ t('stage6.common.actions.close') }}</el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { nextTick, ref, watch, onUnmounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Loading, CircleClose } from '@element-plus/icons-vue'
import { useI18n } from 'vue-i18n'
import {
  getSuWidgetToken,
  syncSuAri,
  syncSuRatePlans,
  syncSuRooms,
  type WidgetTokenResponse,
} from '@/api/otaIntegration'
import { clearGlobalSuConfigProxyContext, setGlobalSuConfigProxyContext } from '@/utils/suConfigProxy'

/**
 * Su Widget global object type definition
 */
interface SuOtaSwitchConfig {
  type: string
  elementId: string
  appId: string
  propertyId: string
  token: string
  channelCode: string
  language?: string
  themeColor?: {
    iconColor?: string
  }
}

declare global {
  interface Window {
    loadScript?: (config: SuOtaSwitchConfig) => void
    React?: unknown
    ReactDOM?: {
      createRoot?: unknown
    }
  }
}

interface Props {
  modelValue: boolean
  otaId: number
  otaName: string
}

const props = defineProps<Props>()
const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  success: []
  synced: []
  error: [error: string]
}>()
const { t } = useI18n()

const visible = ref(false)
const loading = ref(false)
const error = ref('')
const widgetScriptLoaded = ref(false)
const syncingRooms = ref(false)
type WidgetLanguage = 'zn' | 'en'
const WIDGET_LANGUAGE_STORAGE_KEY = 'su_widget_language'

const readInitialWidgetLanguage = (): WidgetLanguage => {
  const saved = localStorage.getItem(WIDGET_LANGUAGE_STORAGE_KEY)
  return saved === 'en' ? 'en' : 'zn'
}

const widgetLanguage = ref<WidgetLanguage>(readInitialWidgetLanguage())

const REACT_UMD_URL = 'https://unpkg.com/react@18.2.0/umd/react.production.min.js'
const REACT_DOM_UMD_URL = 'https://unpkg.com/react-dom@18.2.0/umd/react-dom.production.min.js'

const SU_CONFIG_PROXY_BASE = `${import.meta.env.VITE_API_BASE_URL || '/api/v1'}/su/config`
const CHANNEL_HOTEL_ACCESS_DENIED_PATTERN =
  /(HOTEL_ACCESS_DENIED|Request for forbidden hotel id\(s\)|forbidden hotel id\(s\))/i
const CHANNEL_HOTEL_DENIED_TOAST_COOLDOWN_MS = 3000
const SU_HEADER_AUTHORIZATION = 'authorization'
const SU_HEADER_TOKEN_ID = 'token-id'
const SU_HEADER_APP_ID = 'app-id'
const SU_HEADER_CLIENT_ID = 'client-id'
const SU_PROXY_HEADER_AUTHORIZATION = 'X-Su-Authorization'
const SU_PROXY_HEADER_TOKEN_ID = 'X-Su-Token-Id'
const SU_PROXY_HEADER_APP_ID = 'X-Su-App-Id'
const SU_PROXY_HEADER_CLIENT_ID = 'X-Su-Client-Id'
const LOCAL_MOCK_CHANNEL_ID_HEADER = 'X-Su-Channel-Id'
const BOOKING_LOCAL_MOCK_CHANNEL_ID = '19'
const AIRBNB_LOCAL_MOCK_CHANNEL_ID = '244'

let uninstallSuConfigProxy: null | (() => void) = null
let lastChannelHotelDeniedToastAt = 0
let suConfigProxyContext: {
  tokenId: string
  appId: string
  clientId: string
  localMockChannelId: string | null
} | null = null

const setSuConfigProxyContext = (widgetConfig: WidgetTokenResponse) => {
  const appId = widgetConfig.appId?.trim() || ''
  const clientId = widgetConfig.clientId?.trim() || appId
  const localMockChannelId = resolveAllowedLocalMockChannelId(widgetConfig.channelId)
  suConfigProxyContext = {
    tokenId: widgetConfig.tokenId?.trim() || '',
    appId,
    clientId,
    localMockChannelId,
  }
  setGlobalSuConfigProxyContext({
    tokenId: suConfigProxyContext.tokenId,
    appId: suConfigProxyContext.appId,
    clientId: suConfigProxyContext.clientId,
    channelId: localMockChannelId,
  })
}

const clearSuConfigProxyContext = () => {
  suConfigProxyContext = null
  clearGlobalSuConfigProxyContext()
}

const maybeNotifyChannelHotelIdDenied = (raw: unknown) => {
  if (typeof raw !== 'string' || !raw) {
    return
  }
  if (!CHANNEL_HOTEL_ACCESS_DENIED_PATTERN.test(raw)) {
    return
  }
  const now = Date.now()
  if (now - lastChannelHotelDeniedToastAt < CHANNEL_HOTEL_DENIED_TOAST_COOLDOWN_MS) {
    return
  }
  lastChannelHotelDeniedToastAt = now
  ElMessage.error(
    t('stage6.components.connectOtaWidget.hotelAccessDenied'),
  )
}

const isLocalhost = (): boolean => {
  const host = window.location.hostname
  return host === 'localhost' || host === '127.0.0.1'
}

const isLocalWidgetMockAllowed = (): boolean => {
  return isLocalhost() && import.meta.env.DEV && import.meta.env.VITE_ALLOW_SU_WIDGET_LOCAL === 'true'
}

const resolveAllowedLocalMockChannelId = (
  channelId: string | null | undefined,
): string | null => {
  if (!isLocalWidgetMockAllowed()) {
    return null
  }

  const normalizedChannelId = channelId?.trim() || ''
  if (
    normalizedChannelId === BOOKING_LOCAL_MOCK_CHANNEL_ID ||
    normalizedChannelId === AIRBNB_LOCAL_MOCK_CHANNEL_ID
  ) {
    return normalizedChannelId
  }

  return null
}

// Keep the React root reference so it can be unmounted correctly.
let suWidgetRoot: any = null

const clearWidgetContainer = () => {
  const container = document.getElementById('su-widget-container')
  if (container) {
    // Unmount any saved React root first.
    if (suWidgetRoot) {
      try {
        suWidgetRoot.unmount()
      } catch (e) {
        console.warn('Failed to unmount React root:', e)
      }
      suWidgetRoot = null
    }
    
    // Try ReactDOM unmounting for older versions.
    if (window.ReactDOM && typeof (window.ReactDOM as any).unmountComponentAtNode === 'function') {
      try {
        (window.ReactDOM as any).unmountComponentAtNode(container)
      } catch (e) {
        // ignore
      }
    }
    
    // Clear the container contents.
    container.innerHTML = ''
    
    // Remove React internal markers. React 18 sets _reactRootContainer on the container.
    delete (container as any)._reactRootContainer
    delete (container as any).__reactContainer$
    
    // Remove all properties starting with __react.
    Object.keys(container).forEach(key => {
      if (key.startsWith('__react')) {
        delete (container as any)[key]
      }
    })
    
    // Replace the container element entirely so React cannot detect the old root.
    const parent = container.parentElement
    if (parent) {
      const newContainer = document.createElement('div')
      newContainer.id = 'su-widget-container'
      newContainer.className = container.className
      parent.replaceChild(newContainer, container)
    }
  }
}

const loadExternalScriptOnce = (id: string, scriptUrl: string): Promise<void> => {
  return new Promise((resolve, reject) => {
    const existing = document.getElementById(id) as HTMLScriptElement | null
    const markLoaded = (script: HTMLScriptElement) => {
      script.dataset.loaded = 'true'
    }

    // If it already exists and the global object is ready, treat it as loaded.
    if (existing) {
      const loadedFlag = existing.dataset.loaded === 'true'
      const isReactReady = id === 'widget-react-script' ? Boolean(window.React) : true
      const isReactDomReady =
        id === 'widget-react-dom-script'
          ? typeof (window.ReactDOM as any)?.createRoot === 'function'
          : true

      if (loadedFlag || (isReactReady && isReactDomReady)) {
        markLoaded(existing)
        resolve()
        return
      }

      existing.addEventListener('load', () => {
        markLoaded(existing)
        resolve()
      })
      existing.addEventListener('error', () => {
        reject(new Error(t('stage6.components.connectOtaWidget.externalScriptFailed', { url: scriptUrl })))
      })
      return
    }

    const script = document.createElement('script')
    script.id = id
    script.src = scriptUrl
    script.async = true
    script.crossOrigin = 'anonymous'

    script.onload = () => {
      markLoaded(script)
      resolve()
    }
    script.onerror = () => {
      reject(new Error(t('stage6.components.connectOtaWidget.externalScriptFailed', { url: scriptUrl })))
    }

    document.body.appendChild(script)
  })
}

const ensureWidgetRuntimeReady = async () => {
  await loadExternalScriptOnce('widget-react-script', REACT_UMD_URL)
  await loadExternalScriptOnce('widget-react-dom-script', REACT_DOM_UMD_URL)

  const hasCreateRoot = typeof (window.ReactDOM as any)?.createRoot === 'function'
  if (!hasCreateRoot) {
    throw new Error(
      t('stage6.components.connectOtaWidget.runtimeNotReady'),
    )
  }
}

const resolveSuConfigProxyUrl = (url: unknown): string | null => {
  if (typeof url !== 'string' || !url) {
    return null
  }

  let parsed: URL
  try {
    parsed = new URL(url, window.location.origin)
  } catch {
    return null
  }

  const isProd = parsed.hostname === 'connect.su-api.com'
  const isSandbox = parsed.hostname === 'connect-sandbox.su-api.com'
  if (!isProd && !isSandbox) {
    return null
  }

  if (!parsed.pathname.startsWith('/Config/')) {
    return null
  }

  const remaining = parsed.pathname.replace(/^\/Config\//, '')
  const env = isSandbox ? 'sandbox' : 'prod'
  return `${SU_CONFIG_PROXY_BASE}/${env}/${remaining}${parsed.search}`
}

/**
 * Su Widget directly requests Su Config endpoints (connect(-sandbox).su-api.com/Config/...),
 * but Su does not allow CORS for third-party domains, which causes browser blocking and a blank page.
 *
 * This temporary interception rewrites those requests to the backend proxy:
 * - url: https://connect(-sandbox).su-api.com/Config/...  ->  {VITE_API_BASE_URL}/su/config/{env}/...
 * - injects the system JWT (Authorization) for /api/v1/** requests
 * - if the Su request itself needs Authorization, moves it to X-Su-Authorization and passes it back to Su
 */
const installSuConfigProxy = (): (() => void) => {
  const originalOpen = XMLHttpRequest.prototype.open
  const originalSend = XMLHttpRequest.prototype.send
  const originalSetRequestHeader = XMLHttpRequest.prototype.setRequestHeader
  const originalFetch = window.fetch?.bind(window)

  type XhrMeta = {
    isSuConfig: boolean
    suAuth?: string
    hasTokenIdHeader?: boolean
    hasAppIdHeader?: boolean
    hasClientIdHeader?: boolean
  }
  const metaByXhr = new WeakMap<XMLHttpRequest, XhrMeta>()

  XMLHttpRequest.prototype.open = function (
    method: string,
    url: string | URL,
    async?: boolean,
    username?: string | null,
    password?: string | null,
  ) {
    const proxyUrl = resolveSuConfigProxyUrl(typeof url === 'string' ? url : url?.toString())
    if (proxyUrl) {
      metaByXhr.set(this, { isSuConfig: true })
      return originalOpen.call(this, method, proxyUrl, async ?? true, username ?? null, password ?? null)
    }
    metaByXhr.set(this, { isSuConfig: false })
    return originalOpen.call(this, method, url as any, async ?? true, username ?? null, password ?? null)
  }

  XMLHttpRequest.prototype.setRequestHeader = function (name: string, value: string) {
    const meta = metaByXhr.get(this)
    if (meta?.isSuConfig && typeof name === 'string') {
      const loweredName = name.toLowerCase()
      if (loweredName === SU_HEADER_AUTHORIZATION) {
        // Avoid conflicting with the system JWT: stash Su Authorization first, then pass it to the backend via X-Su-Authorization.
        meta.suAuth = value
        metaByXhr.set(this, meta)
        return
      }
      if (loweredName === SU_HEADER_TOKEN_ID) {
        meta.hasTokenIdHeader = true
        metaByXhr.set(this, meta)
      } else if (loweredName === SU_HEADER_APP_ID) {
        meta.hasAppIdHeader = true
        metaByXhr.set(this, meta)
      } else if (loweredName === SU_HEADER_CLIENT_ID) {
        meta.hasClientIdHeader = true
        metaByXhr.set(this, meta)
      }
    }
    return originalSetRequestHeader.call(this, name, value)
  }

  XMLHttpRequest.prototype.send = function (body?: Document | XMLHttpRequestBodyInit | null) {
    const meta = metaByXhr.get(this)
    if (meta?.isSuConfig) {
      this.addEventListener(
        'loadend',
        () => {
          try {
            maybeNotifyChannelHotelIdDenied(this.responseText)
          } catch {
            // ignore
          }
        },
        { once: true },
      )
      try {
        this.withCredentials = true
      } catch {
        // ignore
      }

      if (meta.suAuth) {
        try {
          originalSetRequestHeader.call(this, SU_PROXY_HEADER_AUTHORIZATION, meta.suAuth)
        } catch {
          // ignore
        }
      }
      if (!meta.hasTokenIdHeader && suConfigProxyContext?.tokenId) {
        try {
          originalSetRequestHeader.call(this, SU_PROXY_HEADER_TOKEN_ID, suConfigProxyContext.tokenId)
        } catch {
          // ignore
        }
      }
      if (!meta.hasAppIdHeader && suConfigProxyContext?.appId) {
        try {
          originalSetRequestHeader.call(this, SU_PROXY_HEADER_APP_ID, suConfigProxyContext.appId)
        } catch {
          // ignore
        }
      }
      if (!meta.hasClientIdHeader && suConfigProxyContext?.clientId) {
        try {
          originalSetRequestHeader.call(this, SU_PROXY_HEADER_CLIENT_ID, suConfigProxyContext.clientId)
        } catch {
          // ignore
        }
      }
      if (suConfigProxyContext?.localMockChannelId) {
        try {
          originalSetRequestHeader.call(
            this,
            LOCAL_MOCK_CHANNEL_ID_HEADER,
            suConfigProxyContext.localMockChannelId,
          )
        } catch {
          // ignore
        }
      }

      const token = localStorage.getItem('token')
      if (token) {
        try {
          originalSetRequestHeader.call(this, 'Authorization', `Bearer ${token}`)
        } catch {
          // ignore
        }
      }
    }

    return originalSend.call(this, body as any)
  }

  if (originalFetch) {
    window.fetch = async (input: RequestInfo | URL, init?: RequestInit) => {
      const url = typeof input === 'string' ? input : (input as any)?.url?.toString?.() || input.toString()
      const proxyUrl = resolveSuConfigProxyUrl(url)
      if (!proxyUrl) {
        return originalFetch(input as any, init)
      }

      const upstreamRequest = new Request(input as RequestInfo, init)
      const proxiedRequest = new Request(proxyUrl, upstreamRequest)
      const token = localStorage.getItem('token')
      const headers = new Headers(proxiedRequest.headers)

      const suAuth = headers.get(SU_HEADER_AUTHORIZATION)
      if (suAuth) {
        headers.delete(SU_HEADER_AUTHORIZATION)
        headers.set(SU_PROXY_HEADER_AUTHORIZATION, suAuth)
      }
      if (!headers.has(SU_HEADER_TOKEN_ID) && suConfigProxyContext?.tokenId) {
        headers.set(SU_PROXY_HEADER_TOKEN_ID, suConfigProxyContext.tokenId)
      }
      if (!headers.has(SU_HEADER_APP_ID) && suConfigProxyContext?.appId) {
        headers.set(SU_PROXY_HEADER_APP_ID, suConfigProxyContext.appId)
      }
      if (!headers.has(SU_HEADER_CLIENT_ID) && suConfigProxyContext?.clientId) {
        headers.set(SU_PROXY_HEADER_CLIENT_ID, suConfigProxyContext.clientId)
      }
      if (suConfigProxyContext?.localMockChannelId) {
        headers.set(LOCAL_MOCK_CHANNEL_ID_HEADER, suConfigProxyContext.localMockChannelId)
      }
      if (token) {
        headers.set('Authorization', `Bearer ${token}`)
      }

      const response = await originalFetch(proxiedRequest, { headers, credentials: 'include' })
      try {
        const preview = await response.clone().text()
        maybeNotifyChannelHotelIdDenied(preview)
      } catch {
        // ignore
      }
      return response
    }
  }

  return () => {
    XMLHttpRequest.prototype.open = originalOpen
    XMLHttpRequest.prototype.send = originalSend
    XMLHttpRequest.prototype.setRequestHeader = originalSetRequestHeader
    if (originalFetch) {
      window.fetch = originalFetch
    }
  }
}

/**
 * Watch modelValue changes and sync them to visible.
 */
watch(
  () => props.modelValue,
  (newValue) => {
    visible.value = newValue
    if (newValue) {
      loadWidget()
    }
  },
  { immediate: true }
)

/**
 * Watch visible changes and sync them to modelValue.
 */
watch(visible, (newValue) => {
  emit('update:modelValue', newValue)
})

/**
 * Load the Su Widget script.
 */
const loadWidgetScript = (scriptUrl: string): Promise<void> => {
  return new Promise((resolve, reject) => {
    // Return immediately if the script is already loaded.
    if (widgetScriptLoaded.value || window.loadScript) {
      resolve()
      return
    }

    // Check whether a script tag already exists.
    const existingScript = document.querySelector(`script[src="${scriptUrl}"]`)
    if (existingScript) {
      widgetScriptLoaded.value = true
      resolve()
      return
    }

    // Dynamically create a script tag to load the Su Widget script.
    const script = document.createElement('script')
    script.src = scriptUrl
    script.async = true

    script.onload = () => {
      widgetScriptLoaded.value = true
      resolve()
    }

    script.onerror = () => {
      reject(new Error(t('stage6.components.connectOtaWidget.scriptLoadFailed')))
    }

    document.head.appendChild(script)
  })
}

/**
 * Initialize the Su Widget.
 */
const initializeWidget = async (widgetConfig: WidgetTokenResponse) => {
  try {
    // Ensure the global loadScript function is available.
    if (!window.loadScript) {
      throw new Error(t('stage6.components.connectOtaWidget.scriptNotLoaded'))
    }

    // Ensure React/ReactDOM dependencies finish loading first. Su's script starts loading them but does not wait, which can race.
    await ensureWidgetRuntimeReady()

    if (!widgetConfig.appId) {
      throw new Error(t('stage6.components.connectOtaWidget.missingAppId'))
    }

    if (!widgetConfig.propertyId) {
      throw new Error(t('stage6.components.connectOtaWidget.missingPropertyId'))
    }

    if (!widgetConfig.tokenId) {
      throw new Error(t('stage6.components.connectOtaWidget.missingTokenId'))
    }

    // Configure the widget.
    const config: SuOtaSwitchConfig = {
      type: widgetConfig.type || 'channel-Mapping',
      elementId: 'su-widget-container',
      appId: widgetConfig.appId,
      propertyId: widgetConfig.propertyId,
      token: widgetConfig.tokenId,
      channelCode: widgetConfig.channelCode,
      language: widgetConfig.language || 'zn',
      themeColor: {
        iconColor: '#1e8ce4',
      },
    }

    // The proxy interception is installed first in loadWidget(); this fallback ensures it is present while the dialog is open.
    if (!uninstallSuConfigProxy) {
      uninstallSuConfigProxy = installSuConfigProxy()
    }

    // Initialize the widget.
    window.loadScript(config)

    // Listen for widget events if Su exposes an event API.
    // According to Su API docs, the widget may emit connection success/failure events.
    // Adjust this based on the actual Su Widget documentation.

    ElMessage.success(t('stage6.components.connectOtaWidget.loaded'))
  } catch (err) {
    console.error('Widget initialization failed:', err)
    throw err
  }
}

/**
 * Load the widget.
 */
const loadWidget = async () => {
  if (!props.otaId) {
    error.value = t('stage6.components.connectOtaWidget.missingOtaConfigId')
    return
  }

  loading.value = true
  error.value = ''
  clearWidgetContainer()
  clearSuConfigProxyContext()

  try {
    // 1. Fetch the widget token.
    const response = await getSuWidgetToken(props.otaId, {
      language: widgetLanguage.value,
    })
    if (!response.success || !response.data) {
      throw new Error(response.message || t('stage6.components.connectOtaWidget.configLoadFailed'))
    }
    setSuConfigProxyContext(response.data)

    // Local development: allow fetching the token to verify backend Su connectivity, but do not force widget loading.
    if (isLocalhost() && import.meta.env.VITE_ALLOW_SU_WIDGET_LOCAL !== 'true') {
      const currentOrigin = window.location.origin
      const tip = t('stage6.components.connectOtaWidget.localWidgetBlocked', { origin: currentOrigin })
      error.value = tip
      emit('error', tip)
      ElMessage.warning(tip)
      loading.value = false
      return
    }

    // 2. Install the Su Config proxy before loading Su script.js, which may request Config immediately.
    uninstallSuConfigProxy?.()
    uninstallSuConfigProxy = installSuConfigProxy()

    // 3. Load the widget script. scriptUrl comes from the backend.
    await loadWidgetScript(response.data.scriptUrl)

    // 4. Ensure the container is rendered before initializing; it does not exist while loading.
    loading.value = false
    await nextTick()
    clearWidgetContainer()
    await initializeWidget(response.data)
  } catch (err: any) {
    console.error('Failed to load widget:', err)
    error.value = err?.message || t('stage6.components.connectOtaWidget.loadFailed')
    emit('error', error.value)
    ElMessage.error(error.value)
  } finally {
    loading.value = false
  }
}

const handleWidgetLanguageChange = async (language: WidgetLanguage) => {
  localStorage.setItem(WIDGET_LANGUAGE_STORAGE_KEY, language)
  if (visible.value) {
    await loadWidget()
  }
}

/**
 * Close the dialog.
 */
const handleSyncRooms = async () => {
  if (!props.otaId) {
    ElMessage.error(t('stage6.components.connectOtaWidget.missingOtaConfigId'))
    return
  }

  syncingRooms.value = true
  try {
    const roomsResp = await syncSuRooms(props.otaId)
    if (!roomsResp.success || !roomsResp.data) {
      throw new Error(roomsResp.message || t('stage6.components.connectOtaWidget.roomSyncFailed'))
    }
    if (!roomsResp.data.roomsSynced) {
      throw new Error(roomsResp.data.roomsError || t('stage6.components.connectOtaWidget.roomSyncFailed'))
    }

    const ratePlansResp = await syncSuRatePlans(props.otaId)
    if (!ratePlansResp.success || !ratePlansResp.data) {
      throw new Error(ratePlansResp.message || t('stage6.components.connectOtaWidget.ratePlanSyncFailed'))
    }
    if (!ratePlansResp.data.ratePlansSynced) {
      throw new Error(ratePlansResp.data.ratePlansError || t('stage6.components.connectOtaWidget.ratePlanSyncFailed'))
    }

    const ariResp = await syncSuAri(props.otaId, 365)
    if (!ariResp.success || !ariResp.data) {
      throw new Error(ariResp.message || t('stage6.components.connectOtaWidget.ariSyncFailed'))
    }
    if (!ariResp.data.availabilityPushed || !ariResp.data.ratesPushed) {
      throw new Error(ariResp.data.error || t('stage6.components.connectOtaWidget.ariSyncFailed'))
    }
    ElMessage.success(
      t('stage6.components.connectOtaWidget.ariPushed', {
        days: ariResp.data.days,
        availabilitySegments: ariResp.data.availabilitySegments,
        rateSegments: ariResp.data.rateSegments,
      }),
    )
    ElMessage.success(
      t('stage6.components.connectOtaWidget.syncCompleted', {
        roomCount: roomsResp.data.roomCount,
        pricePlanCount: ratePlansResp.data.pricePlanCount,
        days: ariResp.data.days,
      }),
    )
    await loadWidget()
    emit('synced')
  } catch (err: any) {
    const message = err?.message || t('stage6.components.connectOtaWidget.syncFailed')
    if (message.includes('roomid') && message.includes('20')) {
      await ElMessageBox.alert(message, t('stage6.components.connectOtaWidget.syncFailedTitle'), {
        type: 'error',
        confirmButtonText: t('stage6.components.connectOtaWidget.gotIt'),
      })
    } else {
      ElMessage.error(message)
    }
    emit('error', message)
  } finally {
    syncingRooms.value = false
  }
}

const handleClose = () => {
  visible.value = false
  // Clean up the widget container.
  clearWidgetContainer()
  clearSuConfigProxyContext()
  uninstallSuConfigProxy?.()
  uninstallSuConfigProxy = null
}

/**
 * Clean up on component unmount.
 */
onUnmounted(() => {
  handleClose()
})
</script>

<style scoped>
.widget-container {
  min-height: 400px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.loading-container,
.error-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 16px;
  padding: 40px;
}

.error-message {
  color: #f56c6c;
  font-size: 14px;
  margin: 8px 0;
}

.widget-content {
  width: 100%;
  min-height: 400px;
}

.dialog-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.language-selector {
  display: flex;
  align-items: center;
  gap: 8px;
}

.language-label {
  color: #606266;
  font-size: 13px;
}
</style>
