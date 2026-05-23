import axios from 'axios'

import config from '../config'
import { logRequest } from '../logger'

type JsonObject = Record<string, any>

interface PostJsonContext {
  logType: 'WEBHOOK_SENT'
  url: string
  path: string
  body: JsonObject
}

interface PostJsonResult {
  success: boolean
  statusCode: number | null
  data: unknown
  error: string | null
}

export interface SendReservationWebhookOptions {
  mode?: 'pull' | 'push'
  hotelId?: string
  payload?: JsonObject
  notifIds?: string[]
  reservations?: JsonObject[]
  usePathRoute?: boolean
}

export interface SendMessagingWebhookOptions {
  hotelId?: string
  payload?: JsonObject
}

/**
 * 构造 Basic Auth 头（如已配置）
 */
function buildAuthHeader(): string | null {
  const { username, password } = config.webhookAuth || {}
  if (!username) return null
  const token = Buffer.from(`${username}:${password || ''}`).toString('base64')
  return `Basic ${token}`
}

/**
 * 拼接 PMS 基础地址与路径，确保斜杠正确
 */
function joinUrl(base: string, requestPath: string): string {
  const left = String(base || '').replace(/\/+$/, '')
  const right = String(requestPath || '').replace(/^\/+/, '')
  return `${left}/${right}`
}

/**
 * 通用 webhook 发送实现，统一处理鉴权、日志与异常
 *
 * @param {object} ctx
 * @param {'WEBHOOK_SENT'} ctx.logType
 * @param {string} ctx.url   完整请求地址
 * @param {string} ctx.path  仅用于日志记录的相对路径
 * @param {object} ctx.body  请求体
 */
async function postJson(ctx: PostJsonContext): Promise<PostJsonResult> {
  const headers: Record<string, string> = { 'Content-Type': 'application/json' }
  const auth = buildAuthHeader()
  if (auth) headers.Authorization = auth

  let statusCode: number | null = null
  let responseData: unknown = null
  let success = false
  let errorMessage: string | null = null

  try {
    const response = await axios.post(ctx.url, ctx.body, {
      headers,
      timeout: 15000,
      // 不要因为 4xx/5xx 就抛错，让我们自行判断
      validateStatus: () => true,
    })
    statusCode = response.status
    responseData = response.data
    success = response.status >= 200 && response.status < 300
    if (!success) {
      errorMessage = `HTTP ${response.status}`
    }
  } catch (err) {
    errorMessage = err instanceof Error ? err.message : String(err)
    if (axios.isAxiosError(err) && err.response) {
      statusCode = err.response.status
      responseData = err.response.data
    }
  }

  try {
    logRequest('WEBHOOK_SENT', 'POST', ctx.path || ctx.url, ctx.body, responseData, statusCode)
  } catch (_logErr) {
    // 日志失败不应影响主流程
  }

  return {
    success,
    statusCode,
    data: responseData,
    error: errorMessage,
  }
}

/**
 * 发送预订通知 webhook
 *
 * @param {object} options
 * @param {'pull'|'push'} [options.mode='pull']
 * @param {string} [options.hotelId]
 * @param {object} [options.payload]                自定义 payload，提供后将覆盖默认结构
 * @param {string[]} [options.notifIds]             pull 模式：通知 ID 列表
 * @param {Array<object>} [options.reservations]    push 模式：完整预订数据列表
 * @param {boolean} [options.usePathRoute=false]    是否使用门店级路径路由
 */
export async function sendReservationWebhook(
  options: SendReservationWebhookOptions = {},
): Promise<PostJsonResult> {
  const mode = options.mode === 'push' ? 'push' : 'pull'
  const hotelId = String(options.hotelId || config.defaultHotelId || '')
  const usePathRoute = Boolean(options.usePathRoute)

  let body: JsonObject
  if (options.payload && typeof options.payload === 'object') {
    body = options.payload
  } else if (mode === 'pull') {
    const notifIds = Array.isArray(options.notifIds) ? options.notifIds : []
    body = {
      hotelid: hotelId,
      reservation_notif: {
        reservation_notif_id: notifIds,
      },
    }
  } else {
    const reservations = Array.isArray(options.reservations) ? options.reservations : []
    body = {
      hotelid: hotelId,
      reservations,
    }
  }

  const basePath = '/api/v1/su/webhook/reservation-notif'
  const path = usePathRoute ? `${basePath}/${encodeURIComponent(hotelId)}` : basePath
  const url = joinUrl(config.pmsBaseUrl, path)

  return postJson({ logType: 'WEBHOOK_SENT', url, path, body })
}

/**
 * 发送消息通知 webhook
 *
 * @param {object} options
 * @param {string} [options.hotelId]
 * @param {object} options.payload
 */
export async function sendMessagingWebhook(
  options: SendMessagingWebhookOptions = {},
): Promise<PostJsonResult> {
  const hotelId = String(options.hotelId || config.defaultHotelId || '')
  const payload = options.payload && typeof options.payload === 'object' ? options.payload : {}

  // 若 payload 未带 hotelid，则补齐为当前 hotelId，便于 PMS 侧路由
  const body: JsonObject = payload.hotelid ? payload : { hotelid: hotelId, ...payload }

  const path = '/api/v1/su/webhook/messaging'
  const url = joinUrl(config.pmsBaseUrl, path)

  return postJson({ logType: 'WEBHOOK_SENT', url, path, body })
}
