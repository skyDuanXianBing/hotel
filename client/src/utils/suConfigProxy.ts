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

  open(method: string, url: string | URL, async: boolean = true, username?: string | null, password?: string | null): void {
    const urlStr = typeof url === 'string' ? url : url.toString()
    this._proxyUrl = resolveSuConfigProxyUrl(urlStr)
    
    if (this._proxyUrl) {
      console.log('[Su Proxy] Intercepted XHR:', urlStr, '->', this._proxyUrl)
      this._isSuConfig = true
      return super.open(method, this._proxyUrl, async, username ?? null, password ?? null)
    }
    
    this._isSuConfig = false
    return super.open(method, url as string, async, username ?? null, password ?? null)
  }

  setRequestHeader(name: string, value: string): void {
    if (this._isSuConfig && name.toLowerCase() === 'authorization') {
      // 保存 Su 的 Authorization，稍后转移到 X-Su-Authorization
      this._suAuth = value
      return
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
          super.setRequestHeader('X-Su-Authorization', this._suAuth)
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

      return originalFetch(proxyUrl, { ...init, headers, credentials: 'include' })
    }
  }

  console.log('[Su Proxy] Global Su Config proxy installed, VITE_API_BASE_URL:', import.meta.env.VITE_API_BASE_URL)
}

// 立即安装！这个模块被导入时就会执行
installGlobalSuConfigProxy()
