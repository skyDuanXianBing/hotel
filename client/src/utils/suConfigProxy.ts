/**
 * Su Widget CORS 代理拦截器
 * 
 * 必须在应用最早期安装，以确保在任何第三方脚本（如 Su Widget）加载之前
 * 就已经替换了 XMLHttpRequest
 * 
 * 原理：Su Widget 内部使用 axios，axios 会在模块加载时保存 XMLHttpRequest 引用。
 * 我们通过替换全局 XMLHttpRequest 构造函数本身来实现拦截。
 */

const SU_CONFIG_PROXY_BASE = `${import.meta.env.VITE_API_BASE_URL || '/api/v1'}/su/config`
const LOCAL_MOCK_CHANNEL_ID_HEADER = 'X-Su-Channel-Id'
const SU_HEADER_AUTHORIZATION = 'authorization'
const SU_HEADER_TOKEN_ID = 'token-id'
const SU_HEADER_APP_ID = 'app-id'
const SU_HEADER_CLIENT_ID = 'client-id'
const SU_PROXY_HEADER_AUTHORIZATION = 'X-Su-Authorization'
const SU_PROXY_HEADER_TOKEN_ID = 'X-Su-Token-Id'
const SU_PROXY_HEADER_APP_ID = 'X-Su-App-Id'
const SU_PROXY_HEADER_CLIENT_ID = 'X-Su-Client-Id'
const BOOKING_LOCAL_MOCK_CHANNEL_ID = '19'
const AIRBNB_LOCAL_MOCK_CHANNEL_ID = '244'

interface SuConfigProxyContext {
  tokenId: string
  appId: string
  clientId: string
  localMockChannelId: string | null
}

let suConfigProxyContext: SuConfigProxyContext | null = null

const isLocalWidgetMockAllowed = (): boolean => {
  if (!import.meta.env.DEV) {
    return false
  }

  const host = window.location.hostname
  const isLocalhost = host === 'localhost' || host === '127.0.0.1'
  return isLocalhost && import.meta.env.VITE_ALLOW_SU_WIDGET_LOCAL === 'true'
}

const resolveAllowedLocalMockChannelId = (channelId: string | null | undefined): string | null => {
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

export const setGlobalSuConfigProxyContext = (context: {
  tokenId?: string | null
  appId?: string | null
  clientId?: string | null
  channelId?: string | null
}): void => {
  const appId = context.appId?.trim() || ''
  suConfigProxyContext = {
    tokenId: context.tokenId?.trim() || '',
    appId,
    clientId: context.clientId?.trim() || appId,
    localMockChannelId: resolveAllowedLocalMockChannelId(context.channelId),
  }
}

export const clearGlobalSuConfigProxyContext = (): void => {
  suConfigProxyContext = null
}

/**
 * 判断 URL 是否是 Su Config 请求，并返回代理后的 URL
 */
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

// 保存原始的 XMLHttpRequest 构造函数
const OriginalXMLHttpRequest = window.XMLHttpRequest
const originalFetch = window.fetch?.bind(window)

/**
 * 创建代理版本的 XMLHttpRequest
 */
class ProxiedXMLHttpRequest extends OriginalXMLHttpRequest {
  private _isSuConfig = false
  private _suAuth: string | null = null
  private _proxyUrl: string | null = null
  private _hasTokenIdHeader = false
  private _hasAppIdHeader = false
  private _hasClientIdHeader = false

  open(method: string, url: string | URL, async: boolean = true, username?: string | null, password?: string | null): void {
    const urlStr = typeof url === 'string' ? url : url.toString()
    this._proxyUrl = resolveSuConfigProxyUrl(urlStr)
    
    if (this._proxyUrl) {
      console.log('[Su Proxy] Intercepted XHR:', urlStr, '->', this._proxyUrl)
      this._isSuConfig = true
      this._suAuth = null
      this._hasTokenIdHeader = false
      this._hasAppIdHeader = false
      this._hasClientIdHeader = false
      return super.open(method, this._proxyUrl, async, username ?? null, password ?? null)
    }
    
    this._isSuConfig = false
    this._suAuth = null
    this._hasTokenIdHeader = false
    this._hasAppIdHeader = false
    this._hasClientIdHeader = false
    return super.open(method, url as string, async, username ?? null, password ?? null)
  }

  setRequestHeader(name: string, value: string): void {
    if (this._isSuConfig && typeof name === 'string') {
      const loweredName = name.toLowerCase()
      if (loweredName === SU_HEADER_AUTHORIZATION) {
        // 保存 Su 的 Authorization，稍后转移到 X-Su-Authorization
        this._suAuth = value
        return
      }
      if (loweredName === SU_HEADER_TOKEN_ID) {
        this._hasTokenIdHeader = true
      } else if (loweredName === SU_HEADER_APP_ID) {
        this._hasAppIdHeader = true
      } else if (loweredName === SU_HEADER_CLIENT_ID) {
        this._hasClientIdHeader = true
      }
    }
    return super.setRequestHeader(name, value)
  }

  send(body?: Document | XMLHttpRequestBodyInit | null): void {
    if (this._isSuConfig) {
      try {
        this.withCredentials = true
      } catch {
        // ignore
      }

      // 如果 Su 请求本身需要 Authorization，搬运到 X-Su-Authorization
      if (this._suAuth) {
        try {
          super.setRequestHeader(SU_PROXY_HEADER_AUTHORIZATION, this._suAuth)
        } catch {
          // ignore
        }
      }

      if (!this._hasTokenIdHeader && suConfigProxyContext?.tokenId) {
        try {
          super.setRequestHeader(SU_PROXY_HEADER_TOKEN_ID, suConfigProxyContext.tokenId)
        } catch {
          // ignore
        }
      }
      if (!this._hasAppIdHeader && suConfigProxyContext?.appId) {
        try {
          super.setRequestHeader(SU_PROXY_HEADER_APP_ID, suConfigProxyContext.appId)
        } catch {
          // ignore
        }
      }
      if (!this._hasClientIdHeader && suConfigProxyContext?.clientId) {
        try {
          super.setRequestHeader(SU_PROXY_HEADER_CLIENT_ID, suConfigProxyContext.clientId)
        } catch {
          // ignore
        }
      }

      // 注入本系统 JWT
      const token = localStorage.getItem('token')
      if (token) {
        try {
          super.setRequestHeader('Authorization', `Bearer ${token}`)
        } catch {
          // ignore
        }
      }

      if (suConfigProxyContext?.localMockChannelId) {
        try {
          super.setRequestHeader(LOCAL_MOCK_CHANNEL_ID_HEADER, suConfigProxyContext.localMockChannelId)
        } catch {
          // ignore
        }
      }
    }

    return super.send(body)
  }
}

/**
 * 安装全局 Su Config 代理拦截器
 */
export const installGlobalSuConfigProxy = (): void => {
  // 替换全局 XMLHttpRequest 构造函数
  (window as any).XMLHttpRequest = ProxiedXMLHttpRequest

  // 拦截 fetch（如果 Widget 使用 fetch）
  if (originalFetch) {
    window.fetch = async (input: RequestInfo | URL, init?: RequestInit) => {
      const url = typeof input === 'string' ? input : (input as any)?.url?.toString?.() || input.toString()
      const proxyUrl = resolveSuConfigProxyUrl(url)
      if (!proxyUrl) {
        return originalFetch(input as any, init)
      }

      console.log('[Su Proxy] Intercepted fetch:', url, '->', proxyUrl)

      const upstreamRequest = new Request(input as RequestInfo, init)
      const proxiedRequest = new Request(proxyUrl, upstreamRequest)
      const headers = new Headers(proxiedRequest.headers)
      const token = localStorage.getItem('token')

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
      if (token) {
        headers.set('Authorization', `Bearer ${token}`)
      }
      if (suConfigProxyContext?.localMockChannelId) {
        headers.set(LOCAL_MOCK_CHANNEL_ID_HEADER, suConfigProxyContext.localMockChannelId)
      }

      return originalFetch(proxiedRequest, { headers, credentials: 'include' })
    }
  }

  console.log('[Su Proxy] Global Su Config proxy installed, VITE_API_BASE_URL:', import.meta.env.VITE_API_BASE_URL)
}

// 立即安装！这个模块被导入时就会执行
installGlobalSuConfigProxy()
