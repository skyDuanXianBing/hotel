<template>
  <ion-modal :is-open="isOpen" @didDismiss="handleDidDismiss">
    <ion-header translucent>
      <ion-toolbar>
        <ion-title>{{ title }}</ion-title>
        <ion-buttons slot="end">
          <ion-button @click="handleDismiss">关闭</ion-button>
        </ion-buttons>
      </ion-toolbar>
    </ion-header>

    <ion-content>
      <section class="channel-widget-modal__hero">
        <strong>{{ otaName }}</strong>
      </section>

      <section v-if="loading" class="channel-widget-modal__state">
        <ion-spinner name="crescent" />
        <p>正在加载渠道向导...</p>
      </section>

      <section v-else-if="errorMessage" class="channel-widget-modal__state channel-widget-modal__state--error">
        <p>{{ errorMessage }}</p>
        <ion-button @click="loadWidget">重试</ion-button>
      </section>

      <section v-else class="channel-widget-modal__content">
        <div class="channel-widget-modal__toolbar">
          <span>语言</span>
          <ion-segment :value="widgetLanguage" @ionChange="handleLanguageChange">
            <ion-segment-button value="zn">
              <ion-label>中文</ion-label>
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

const props = defineProps<{
  isOpen: boolean
  otaId: number
  otaName: string
}>()

const emit = defineEmits<{
  dismiss: []
  error: [message: string]
}>()

const title = computed(() => `连接到 ${props.otaName || '渠道'}`)
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

function installSuConfigProxy() {
  const originalOpen = XMLHttpRequest.prototype.open
  const originalSend = XMLHttpRequest.prototype.send
  const originalSetRequestHeader = XMLHttpRequest.prototype.setRequestHeader
  const originalFetch = window.fetch?.bind(window)

  type XhrMeta = {
    isSuConfig: boolean
    suAuthorization?: string
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
    if (meta?.isSuConfig && name.toLowerCase() === 'authorization') {
      meta.suAuthorization = value
      metaByXhr.set(this, meta)
      return
    }

    return originalSetRequestHeader.call(this, name, value)
  }

  XMLHttpRequest.prototype.send = function (body) {
    const meta = metaByXhr.get(this)
    if (meta?.isSuConfig) {
      const token = localStorage.getItem('token')
      if (meta.suAuthorization) {
        try {
          originalSetRequestHeader.call(this, 'X-Su-Authorization', meta.suAuthorization)
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

      const headers = new Headers(init?.headers || {})
      const token = localStorage.getItem('token')
      const suAuthorization = headers.get('authorization')

      if (suAuthorization) {
        headers.delete('authorization')
        headers.set('X-Su-Authorization', suAuthorization)
      }
      if (token) {
        headers.set('Authorization', `Bearer ${token}`)
      }

      return originalFetch(proxyUrl, {
        ...init,
        headers,
        credentials: 'include',
      })
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
      existingScript.addEventListener('error', () => reject(new Error(`脚本加载失败：${scriptUrl}`)), {
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
      reject(new Error(`脚本加载失败：${scriptUrl}`))
    }

    document.body.appendChild(script)
  })
}

async function ensureWidgetRuntimeReady() {
  await loadExternalScriptOnce('ios-widget-react-script', REACT_UMD_URL)
  await loadExternalScriptOnce('ios-widget-react-dom-script', REACT_DOM_UMD_URL)

  if (typeof window.ReactDOM?.createRoot !== 'function') {
    throw new Error('Su Widget 依赖未就绪，请稍后重试')
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
      reject(new Error('无法加载 Su Widget 脚本'))
    }
    document.head.appendChild(script)
  })
}

async function initializeWidget(widgetConfig: WidgetTokenResponse) {
  if (!window.loadScript) {
    throw new Error('Su Widget 未正确加载')
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
    errorMessage.value = '缺少渠道配置标识'
    return
  }

  loading.value = true
  errorMessage.value = ''
  clearWidgetContainer()

  try {
    const response = await getSuWidgetToken(props.otaId, {
      language: widgetLanguage.value,
    })
    if (!response.success || !response.data) {
      throw new Error(response.message || '获取渠道向导失败')
    }

    if (isLocalhost() && import.meta.env.VITE_ALLOW_SU_WIDGET_LOCAL !== 'true') {
      throw new Error('本地地址无法直接加载 Su Widget，请使用已部署域名或开启 VITE_ALLOW_SU_WIDGET_LOCAL=true')
    }

    uninstallSuConfigProxy?.()
    uninstallSuConfigProxy = installSuConfigProxy()

    await loadWidgetScript(response.data.scriptUrl)
    loading.value = false
    await nextTick()
    await initializeWidget(response.data)
  } catch (error) {
    const message = error instanceof Error && error.message ? error.message : '加载渠道向导失败'
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
