<template>
  <ion-modal :is-open="isOpen" @didDismiss="handleDidDismiss">
    <ion-header translucent>
      <ion-toolbar>
        <ion-title>{{ title }}</ion-title>
        <ion-buttons slot="end">
          <ion-button @click="handleDismiss">{{ $t('home.section.close') }}</ion-button>
        </ion-buttons>
      </ion-toolbar>
    </ion-header>

    <ion-content>
      <section class="channel-widget-modal__hero">
        <strong>{{ otaName }}</strong>
      </section>

      <section v-if="loading" class="channel-widget-modal__state">
        <ion-spinner name="crescent" />
        <p>{{ $t('iosStage5.channel.loadingWidget') }}</p>
      </section>

      <section v-else-if="errorMessage" class="channel-widget-modal__state channel-widget-modal__state--error">
        <p>{{ errorMessage }}</p>
        <ion-button @click="loadWidget">{{ $t('channel.mappingPriceSettings.actions.retry') }}</ion-button>
      </section>

      <section v-else class="channel-widget-modal__content">
        <div class="channel-widget-modal__toolbar">
          <span>{{ $t('settingsStage4.storeDetails.fields.language') }}</span>
          <ion-segment :value="widgetLanguage" @ionChange="handleLanguageChange">
            <ion-segment-button value="zn">
              <ion-label>{{ $t('language.option.zh-CN') }}</ion-label>
            </ion-segment-button>
            <ion-segment-button value="en">
              <ion-label>EN</ion-label>
            </ion-segment-button>
          </ion-segment>
        </div>
        <div :id="widgetContainerId" class="channel-widget-modal__widget"></div>
      </section>
    </ion-content>
  </ion-modal>
</template>

<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import {
  IonButton,
  IonButtons,
  IonContent,
  IonHeader,
  IonLabel,
  IonModal,
  IonSegment,
  IonSegmentButton,
  IonSpinner,
  IonTitle,
  IonToolbar,
} from '@ionic/vue'
import { computed, nextTick, onUnmounted, ref, watch } from 'vue'
import { getSuWidgetToken, type WidgetTokenResponse } from '@/api/otaIntegration'
import { SU_CONFIG_PROXY_BASE } from '@/constants/api'
import { showWarningToast } from '@/utils/notify'

const { t } = useI18n()

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

type WidgetLanguage = 'zn' | 'en'

const REACT_UMD_URL = 'https://unpkg.com/react@18.2.0/umd/react.production.min.js'
const REACT_DOM_UMD_URL = 'https://unpkg.com/react-dom@18.2.0/umd/react-dom.production.min.js'
const WIDGET_LANGUAGE_STORAGE_KEY = 'ios_su_widget_language'
const LOCAL_MOCK_CHANNEL_ID_HEADER = 'X-Su-Channel-Id'
const SU_HEADER_AUTHORIZATION = 'authorization'
const SU_HEADER_TOKEN_ID = 'token-id'
const SU_HEADER_APP_ID = 'app-id'
const SU_HEADER_CLIENT_ID = 'client-id'
const SU_PROXY_HEADER_AUTHORIZATION = 'X-Su-Authorization'
const SU_PROXY_HEADER_TOKEN_ID = 'X-Su-Token-Id'
const SU_PROXY_HEADER_APP_ID = 'X-Su-App-Id'
const SU_PROXY_HEADER_CLIENT_ID = 'X-Su-Client-Id'

const props = defineProps<{
  isOpen: boolean
  otaId: number
  otaName: string
}>()

const emit = defineEmits<{
  dismiss: []
  error: [message: string]
}>()

const title = computed(() =>
  t('stage5Final.channel.connectTitle', {
    name: props.otaName || t('home.quick.channels.0'),
  }),
)
const widgetContainerId = `su-widget-container-${Math.random().toString(36).slice(2)}`
const loading = ref(false)
const errorMessage = ref('')
const widgetScriptLoaded = ref(false)

const readInitialWidgetLanguage = (): WidgetLanguage => {
  const savedValue = localStorage.getItem(WIDGET_LANGUAGE_STORAGE_KEY)
  return savedValue === 'en' ? 'en' : 'zn'
}

const widgetLanguage = ref<WidgetLanguage>(readInitialWidgetLanguage())

let uninstallSuConfigProxy: null | (() => void) = null
let reactRoot: { unmount: () => void } | null = null
let suConfigProxyContext: {
  tokenId: string
  appId: string
  clientId: string
} | null = null

function setSuConfigProxyContext(widgetConfig: WidgetTokenResponse) {
  suConfigProxyContext = {
    tokenId: widgetConfig.tokenId?.trim() || '',
    appId: widgetConfig.appId?.trim() || '',
    clientId: widgetConfig.clientId?.trim() || widgetConfig.appId?.trim() || '',
  }
}

function clearSuConfigProxyContext() {
  suConfigProxyContext = null
}

function clearWidgetContainer() {
  const container = document.getElementById(widgetContainerId)
  if (!container) {
    return
  }

  if (reactRoot) {
    try {
      reactRoot.unmount()
    } catch {
      // ignore
    }
    reactRoot = null
  }

  container.innerHTML = ''
}

function isLocalhost() {
  const host = window.location.hostname
  return host === 'localhost' || host === '127.0.0.1'
}

function resolveSuConfigProxyUrl(url: unknown) {
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

  const env = isSandbox ? 'sandbox' : 'prod'
  const path = parsed.pathname.replace(/^\/Config\//, '')
  return `${SU_CONFIG_PROXY_BASE}/${env}/${path}${parsed.search}`
}

function resolveLocalMockChannelId(channelId: unknown) {
  if (!isLocalhost()) {
    return null
  }

  if (import.meta.env.VITE_ALLOW_SU_WIDGET_LOCAL !== 'true') {
    return null
  }

  if (typeof channelId !== 'string') {
    return null
  }

  const normalizedChannelId = channelId.trim()
  if (normalizedChannelId === '19' || normalizedChannelId === '244') {
    return normalizedChannelId
  }

  return null
}

function installSuConfigProxy(localMockChannelId: string | null) {
  const originalOpen = XMLHttpRequest.prototype.open
  const originalSend = XMLHttpRequest.prototype.send
  const originalSetRequestHeader = XMLHttpRequest.prototype.setRequestHeader
  const originalFetch = window.fetch?.bind(window)

  type XhrMeta = {
    isSuConfig: boolean
    suAuthorization?: string
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
    const proxyUrl = resolveSuConfigProxyUrl(typeof url === 'string' ? url : url.toString())
    if (proxyUrl) {
      metaByXhr.set(this, { isSuConfig: true })
      return originalOpen.call(this, method, proxyUrl, async ?? true, username ?? null, password ?? null)
    }

    metaByXhr.set(this, { isSuConfig: false })
    return originalOpen.call(this, method, url as string, async ?? true, username ?? null, password ?? null)
  }

  XMLHttpRequest.prototype.setRequestHeader = function (name, value) {
    const meta = metaByXhr.get(this)
    if (meta?.isSuConfig && typeof name === 'string') {
      const lowerName = name.toLowerCase()
      if (lowerName === SU_HEADER_AUTHORIZATION) {
        meta.suAuthorization = value
        metaByXhr.set(this, meta)
        return
      }
      if (lowerName === SU_HEADER_TOKEN_ID) {
        meta.hasTokenIdHeader = true
        metaByXhr.set(this, meta)
      } else if (lowerName === SU_HEADER_APP_ID) {
        meta.hasAppIdHeader = true
        metaByXhr.set(this, meta)
      } else if (lowerName === SU_HEADER_CLIENT_ID) {
        meta.hasClientIdHeader = true
        metaByXhr.set(this, meta)
      }
    }

    return originalSetRequestHeader.call(this, name, value)
  }

  XMLHttpRequest.prototype.send = function (body) {
    const meta = metaByXhr.get(this)
    if (meta?.isSuConfig) {
      const token = localStorage.getItem('token')
      if (meta.suAuthorization) {
        try {
          originalSetRequestHeader.call(this, SU_PROXY_HEADER_AUTHORIZATION, meta.suAuthorization)
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
      if (token) {
        try {
          originalSetRequestHeader.call(this, 'Authorization', `Bearer ${token}`)
        } catch {
          // ignore
        }
      }
      if (localMockChannelId) {
        try {
          originalSetRequestHeader.call(this, LOCAL_MOCK_CHANNEL_ID_HEADER, localMockChannelId)
        } catch {
          // ignore
        }
      }
    }

    return originalSend.call(this, body as Document | XMLHttpRequestBodyInit | null | undefined)
  }

  if (originalFetch) {
    window.fetch = async (input: RequestInfo | URL, init?: RequestInit) => {
      const inputUrl = typeof input === 'string' ? input : 'url' in input ? input.url : input.toString()
      const proxyUrl = resolveSuConfigProxyUrl(inputUrl)
      if (!proxyUrl) {
        return originalFetch(input as RequestInfo, init)
      }

      const upstreamRequest = new Request(input as RequestInfo, init)
      const proxiedRequest = new Request(proxyUrl, upstreamRequest)
      const headers = new Headers(proxiedRequest.headers)
      const token = localStorage.getItem('token')
      const suAuthorization = headers.get(SU_HEADER_AUTHORIZATION)

      if (suAuthorization) {
        headers.delete(SU_HEADER_AUTHORIZATION)
        headers.set(SU_PROXY_HEADER_AUTHORIZATION, suAuthorization)
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
      if (token) {
        headers.set('Authorization', `Bearer ${token}`)
      }
      if (localMockChannelId) {
        headers.set(LOCAL_MOCK_CHANNEL_ID_HEADER, localMockChannelId)
      }

      return originalFetch(proxiedRequest, { headers, credentials: 'include' })
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

function loadExternalScriptOnce(id: string, scriptUrl: string) {
  return new Promise<void>((resolve, reject) => {
    const existingScript = document.getElementById(id) as HTMLScriptElement | null
    if (existingScript) {
      if (existingScript.dataset.loaded === 'true') {
        resolve()
        return
      }

      existingScript.addEventListener('load', () => resolve(), { once: true })
      existingScript.addEventListener('error', () => reject(new Error(t('stage5VisibleText.93', { url: scriptUrl }))), {
        once: true,
      })
      return
    }

    const script = document.createElement('script')
    script.id = id
    script.src = scriptUrl
    script.async = true
    script.crossOrigin = 'anonymous'
    script.onload = () => {
      script.dataset.loaded = 'true'
      resolve()
    }
    script.onerror = () => {
      reject(new Error(t('stage5VisibleText.93', { url: scriptUrl })))
    }

    document.body.appendChild(script)
  })
}

async function ensureWidgetRuntimeReady() {
  await loadExternalScriptOnce('ios-widget-react-script', REACT_UMD_URL)
  await loadExternalScriptOnce('ios-widget-react-dom-script', REACT_DOM_UMD_URL)

  if (typeof window.ReactDOM?.createRoot !== 'function') {
    throw new Error(t('iosStage5.channel.widgetDependencyMissing'))
  }
}

function loadWidgetScript(scriptUrl: string) {
  return new Promise<void>((resolve, reject) => {
    if (widgetScriptLoaded.value || window.loadScript) {
      widgetScriptLoaded.value = true
      resolve()
      return
    }

    const existingScript = document.querySelector(`script[src="${scriptUrl}"]`)
    if (existingScript) {
      widgetScriptLoaded.value = true
      resolve()
      return
    }

    const script = document.createElement('script')
    script.src = scriptUrl
    script.async = true
    script.onload = () => {
      widgetScriptLoaded.value = true
      resolve()
    }
    script.onerror = () => {
      reject(new Error(t('stage5VisibleText.77')))
    }
    document.head.appendChild(script)
  })
}

async function initializeWidget(widgetConfig: WidgetTokenResponse) {
  if (!window.loadScript) {
    throw new Error(t('stage5VisibleText.5'))
  }

  await ensureWidgetRuntimeReady()

  window.loadScript({
    type: widgetConfig.type || 'channel-Mapping',
    elementId: widgetContainerId,
    appId: widgetConfig.appId,
    propertyId: widgetConfig.propertyId,
    token: widgetConfig.tokenId,
    channelCode: widgetConfig.channelCode,
    language: widgetConfig.language || widgetLanguage.value,
    themeColor: {
      iconColor: '#0f766e',
    },
  })
}

async function loadWidget() {
  if (!props.otaId) {
    errorMessage.value = t('iosStage5.channel.channelConfigMissing')
    return
  }

  loading.value = true
  errorMessage.value = ''
  clearWidgetContainer()
  clearSuConfigProxyContext()

  try {
    const response = await getSuWidgetToken(props.otaId, {
      language: widgetLanguage.value,
    })
    if (!response.success || !response.data) {
      throw new Error(response.message || t('iosStage5.channel.loadWizardFailed'))
    }
    setSuConfigProxyContext(response.data)

    if (isLocalhost() && import.meta.env.VITE_ALLOW_SU_WIDGET_LOCAL !== 'true') {
      throw new Error(
        t('stage5Final.channel.widgetUnavailable'),
      )
    }

    uninstallSuConfigProxy?.()
    uninstallSuConfigProxy = installSuConfigProxy(resolveLocalMockChannelId(response.data.channelId))

    await loadWidgetScript(response.data.scriptUrl)
    loading.value = false
    await nextTick()
    await initializeWidget(response.data)
  } catch (error) {
    const message =
      error instanceof Error && error.message ? error.message : t('stage5VisibleText.9')
    errorMessage.value = message
    showWarningToast(message)
    emit('error', message)
  } finally {
    loading.value = false
  }
}

function handleLanguageChange(event: CustomEvent) {
  const language = event.detail.value as WidgetLanguage
  widgetLanguage.value = language === 'en' ? 'en' : 'zn'
  localStorage.setItem(WIDGET_LANGUAGE_STORAGE_KEY, widgetLanguage.value)
  if (props.isOpen) {
    void loadWidget()
  }
}

function cleanupWidget() {
  clearWidgetContainer()
  clearSuConfigProxyContext()
  uninstallSuConfigProxy?.()
  uninstallSuConfigProxy = null
}

function handleDismiss() {
  cleanupWidget()
  emit('dismiss')
}

function handleDidDismiss() {
  cleanupWidget()
}

watch(
  () => props.isOpen,
  (isOpen) => {
    if (isOpen) {
      void loadWidget()
      return
    }

    cleanupWidget()
  },
)

onUnmounted(() => {
  cleanupWidget()
})
</script>

<style scoped>
.channel-widget-modal__hero {
  margin: 16px;
  padding: 18px;
  border-radius: 20px;
  background: var(--app-primary-soft);
}

.channel-widget-modal__hero strong {
  display: block;
  font-size: 18px;
  color: var(--app-heading);
}

.channel-widget-modal__hero p,
.channel-widget-modal__state p {
  margin: 8px 0 0;
  color: var(--app-muted);
  font-size: 14px;
  line-height: 1.6;
}

.channel-widget-modal__state {
  min-height: 280px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  gap: 14px;
  padding: 24px;
}

.channel-widget-modal__state--error p {
  color: var(--ion-color-danger);
  text-align: center;
}

.channel-widget-modal__content {
  padding: 0 16px 16px;
}

.channel-widget-modal__toolbar {
  display: grid;
  gap: 8px;
  margin-bottom: 12px;
}

.channel-widget-modal__toolbar span {
  color: var(--app-muted);
  font-size: 13px;
}

.channel-widget-modal__widget {
  min-height: 420px;
  border-radius: 22px;
  overflow: hidden;
  background: rgba(255, 255, 255, 0.94);
  border: 1px solid var(--app-border);
}
</style>
