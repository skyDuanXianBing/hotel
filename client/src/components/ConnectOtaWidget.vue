<template>
  <el-dialog
    v-model="visible"
    :title="`连接到 ${otaName}`"
    width="800px"
    :close-on-click-modal="false"
    @close="handleClose"
  >
    <div class="widget-container">
      <div v-if="loading" class="loading-container">
        <el-icon class="is-loading" :size="40">
          <Loading />
        </el-icon>
        <p>正在加载连接向导...</p>
      </div>

      <div v-else-if="error" class="error-container">
        <el-icon :size="40" color="#f56c6c">
          <CircleClose />
        </el-icon>
        <p class="error-message">{{ error }}</p>
        <el-button type="primary" @click="loadWidget">重试</el-button>
      </div>

      <!-- Su Widget 挂载点 -->
      <div v-else id="su-widget-container" class="widget-content"></div>
    </div>

    <template #footer>
      <div class="dialog-footer">
        <div class="language-selector">
          <span class="language-label">语言</span>
          <el-select
            v-model="widgetLanguage"
            size="small"
            style="width: 120px"
            :disabled="loading"
            @change="handleWidgetLanguageChange"
          >
            <el-option label="中文" value="zn" />
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
          一键推送PMS房型列表（认证用）
        </el-button>
        <el-button @click="handleClose">关闭</el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { nextTick, ref, watch, onUnmounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Loading, CircleClose } from '@element-plus/icons-vue'
import {
  getSuWidgetToken,
  syncSuAri,
  syncSuRatePlans,
  syncSuRooms,
  type WidgetTokenResponse,
} from '@/api/otaIntegration'

/**
 * Su Widget 全局对象类型定义
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

let uninstallSuConfigProxy: null | (() => void) = null
let lastChannelHotelDeniedToastAt = 0

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
    '渠道酒店标识无权限或不存在，请使用Booking.com后台的正确Property ID，并确认该账号有该酒店权限。',
  )
}

const isLocalhost = (): boolean => {
  const host = window.location.hostname
  return host === 'localhost' || host === '127.0.0.1'
}

// 保存 React root 引用，用于正确卸载
let suWidgetRoot: any = null

const clearWidgetContainer = () => {
  const container = document.getElementById('su-widget-container')
  if (container) {
    // 如果有保存的 React root，先卸载
    if (suWidgetRoot) {
      try {
        suWidgetRoot.unmount()
      } catch (e) {
        console.warn('卸载 React root 失败:', e)
      }
      suWidgetRoot = null
    }
    
    // 尝试使用 ReactDOM 的方式卸载（兼容旧版本）
    if (window.ReactDOM && typeof (window.ReactDOM as any).unmountComponentAtNode === 'function') {
      try {
        (window.ReactDOM as any).unmountComponentAtNode(container)
      } catch (e) {
        // ignore
      }
    }
    
    // 清空容器内容
    container.innerHTML = ''
    
    // 移除 React 内部标记（React 18 会在容器上设置 _reactRootContainer）
    delete (container as any)._reactRootContainer
    delete (container as any).__reactContainer$
    
    // 移除所有以 __react 开头的属性
    Object.keys(container).forEach(key => {
      if (key.startsWith('__react')) {
        delete (container as any)[key]
      }
    })
    
    // 完全替换容器元素以确保 React 不会检测到旧的 root
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

    // 若已存在且全局对象已经就绪，直接认为已加载
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
        reject(new Error(`外部脚本加载失败：${scriptUrl}`))
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
      reject(new Error(`外部脚本加载失败：${scriptUrl}`))
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
      'Su Widget 依赖未就绪：ReactDOM.createRoot 不存在（通常是 react-dom 脚本未加载成功或被拦截）',
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
 * Su Widget 会在浏览器里直接请求 Su 的 Config 接口（connect(-sandbox).su-api.com/Config/...），
 * 但 Su 侧不对第三方域名开放 CORS，导致浏览器拦截并白屏。
 *
 * 这里用“临时拦截”把这些请求重写到后端代理：
 * - url: https://connect(-sandbox).su-api.com/Config/...  ->  {VITE_API_BASE_URL}/su/config/{env}/...
 * - 额外注入本系统 JWT（Authorization）用于访问后端 /api/v1/**
 * - 若 Su 请求本身需要 Authorization，则搬运到 X-Su-Authorization 给后端再转回 Su
 */
const installSuConfigProxy = (): (() => void) => {
  const originalOpen = XMLHttpRequest.prototype.open
  const originalSend = XMLHttpRequest.prototype.send
  const originalSetRequestHeader = XMLHttpRequest.prototype.setRequestHeader
  const originalFetch = window.fetch?.bind(window)

  type XhrMeta = {
    isSuConfig: boolean
    suAuth?: string
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
    if (meta?.isSuConfig && typeof name === 'string' && name.toLowerCase() === 'authorization') {
      // 避免与本系统 JWT 冲突：先把 Su 的 Authorization 暂存，后续通过 X-Su-Authorization 传给后端
      meta.suAuth = value
      metaByXhr.set(this, meta)
      return
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
          originalSetRequestHeader.call(this, 'X-Su-Authorization', meta.suAuth)
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

      const token = localStorage.getItem('token')
      const headers = new Headers(init?.headers || {})

      const suAuth = headers.get('authorization')
      if (suAuth) {
        headers.delete('authorization')
        headers.set('X-Su-Authorization', suAuth)
      }
      if (token) {
        headers.set('Authorization', `Bearer ${token}`)
      }

      const response = await originalFetch(proxyUrl, { ...init, headers, credentials: 'include' })
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
 * 监听modelValue变化,同步到visible
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
 * 监听visible变化,同步到modelValue
 */
watch(visible, (newValue) => {
  emit('update:modelValue', newValue)
})

/**
 * 加载Su Widget脚本
 */
const loadWidgetScript = (scriptUrl: string): Promise<void> => {
  return new Promise((resolve, reject) => {
    // 如果脚本已加载,直接返回
    if (widgetScriptLoaded.value || window.loadScript) {
      resolve()
      return
    }

    // 检查是否已经有脚本标签
    const existingScript = document.querySelector(`script[src="${scriptUrl}"]`)
    if (existingScript) {
      widgetScriptLoaded.value = true
      resolve()
      return
    }

    // 动态创建script标签加载Su Widget脚本
    const script = document.createElement('script')
    script.src = scriptUrl
    script.async = true

    script.onload = () => {
      widgetScriptLoaded.value = true
      resolve()
    }

    script.onerror = () => {
      reject(new Error('无法加载Su Widget脚本'))
    }

    document.head.appendChild(script)
  })
}

/**
 * 初始化Su Widget
 */
const initializeWidget = async (widgetConfig: WidgetTokenResponse) => {
  try {
    // 确保loadScript全局函数已加载
    if (!window.loadScript) {
      throw new Error('Su Widget脚本未正确加载（loadScript 不存在）')
    }

    // 确保 React/ReactDOM 依赖先加载完成（Su 官方 script.js 自带加载但不等待，会导致偶发 race）
    await ensureWidgetRuntimeReady()

    if (!widgetConfig.appId) {
      throw new Error('缺少 Su appId（client-id）')
    }

    if (!widgetConfig.propertyId) {
      throw new Error('缺少 Su propertyId（proppmsid）')
    }

    if (!widgetConfig.tokenId) {
      throw new Error('缺少 Su tokenId（token_id）')
    }

    // 配置Widget
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

    // 代理拦截会在 loadWidget() 中优先安装；这里兜底确保已安装（仅在弹窗期间生效，关闭时会卸载）
    if (!uninstallSuConfigProxy) {
      uninstallSuConfigProxy = installSuConfigProxy()
    }

    // 初始化Widget
    window.loadScript(config)

    // 监听Widget事件(如果Su提供了事件API)
    // 注: 根据Su API文档,Widget可能会触发连接成功/失败等事件
    // 这里需要根据实际Su Widget文档调整

    ElMessage.success('连接向导已加载')
  } catch (err) {
    console.error('Widget初始化失败:', err)
    throw err
  }
}

/**
 * 加载Widget
 */
const loadWidget = async () => {
  if (!props.otaId) {
    error.value = '缺少OTA配置ID'
    return
  }

  loading.value = true
  error.value = ''
  clearWidgetContainer()

  try {
    // 1. 获取Widget Token
    const response = await getSuWidgetToken(props.otaId, {
      language: widgetLanguage.value,
    })
    if (!response.success || !response.data) {
      throw new Error(response.message || '获取连接配置失败')
    }

    // 本地开发环境：允许请求后端拿到 token（便于验证后端是否已联通 Su），但不强制加载 Widget
    if (isLocalhost() && import.meta.env.VITE_ALLOW_SU_WIDGET_LOCAL !== 'true') {
      const currentOrigin = window.location.origin
      const tip =
        `已从后端获取 Su 授权信息（token_id / proppmsid），但 Su Widget 不能在本地地址加载（当前：${currentOrigin}）。请使用已部署域名/内网穿透域名并让 Su 侧加入白名单；如需强制本地尝试，可设置 VITE_ALLOW_SU_WIDGET_LOCAL=true。`
      error.value = tip
      emit('error', tip)
      ElMessage.warning(tip)
      loading.value = false
      return
    }

    // 2. 安装 Su Config 代理拦截（必须早于加载 Su script.js：script 加载后可能立即发起 Config 请求）
    uninstallSuConfigProxy?.()
    uninstallSuConfigProxy = installSuConfigProxy()

    // 3. 加载Widget脚本（scriptUrl 来自后端）
    await loadWidgetScript(response.data.scriptUrl)

    // 4. 确保容器已渲染再初始化（否则 loading 状态下容器不存在，widget 无法挂载）
    loading.value = false
    await nextTick()
    clearWidgetContainer()
    await initializeWidget(response.data)
  } catch (err: any) {
    console.error('加载Widget失败:', err)
    error.value = err?.message || '加载连接向导失败，请重试'
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
 * 关闭对话框
 */
const handleSyncRooms = async () => {
  if (!props.otaId) {
    ElMessage.error('缺少OTA配置ID')
    return
  }

  syncingRooms.value = true
  try {
    const roomsResp = await syncSuRooms(props.otaId)
    if (!roomsResp.success || !roomsResp.data) {
      throw new Error(roomsResp.message || '房型列表同步失败')
    }
    if (!roomsResp.data.roomsSynced) {
      throw new Error(roomsResp.data.roomsError || '房型列表同步失败')
    }

    const ratePlansResp = await syncSuRatePlans(props.otaId)
    if (!ratePlansResp.success || !ratePlansResp.data) {
      throw new Error(ratePlansResp.message || '价格计划列表同步失败')
    }
    if (!ratePlansResp.data.ratePlansSynced) {
      throw new Error(ratePlansResp.data.ratePlansError || '价格计划列表同步失败')
    }

    const ariResp = await syncSuAri(props.otaId, 365)
    if (!ariResp.success || !ariResp.data) {
      throw new Error(ariResp.message || '基础ARI同步失败')
    }
    if (!ariResp.data.availabilityPushed || !ariResp.data.ratesPushed) {
      throw new Error(ariResp.data.error || '基础ARI同步失败')
    }
    ElMessage.success(
      `基础ARI已推送未来${ariResp.data.days}天（可用性段${ariResp.data.availabilitySegments}，房价段${ariResp.data.rateSegments}）`,
    )
    ElMessage.success(
      `已推送${roomsResp.data.roomCount}个房型、${ratePlansResp.data.pricePlanCount}个价格计划，并预热未来${ariResp.data.days}天基础ARI`,
    )
    await loadWidget()
    emit('synced')
  } catch (err: any) {
    const message = err?.message || '同步失败'
    if (message.includes('roomid') && message.includes('20')) {
      await ElMessageBox.alert(message, 'ä¸€é”®æŽ¨é€å¤±è´¥', {
        type: 'error',
        confirmButtonText: 'çŸ¥é“äº†',
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
  // 清理Widget容器
  clearWidgetContainer()
  uninstallSuConfigProxy?.()
  uninstallSuConfigProxy = null
}

/**
 * 组件卸载时清理
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
