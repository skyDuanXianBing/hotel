import { Request, Response } from 'express'
import express from 'express'
import fs from 'fs'
import path from 'path'

import config from '../config'
import { registerPendingReservation } from '../state/reservationPendingState'
import { sendMessagingWebhook, sendReservationWebhook } from './sender'

const { v4: uuidv4 } = require('uuid') as { v4: () => string }

type JsonObject = Record<string, any>

interface Scenario {
  name: string
  label: string
  description: string
  type: 'reservation' | 'messaging'
  mode?: 'pull' | 'push'
  fixturePath?: string
  batchCount?: number
  getPayload(hotelId: string): JsonObject
}

interface SerializedScenario {
  name: string
  label: string
  description: string
  type: 'reservation' | 'messaging'
  mode: 'pull' | 'push' | null
  batchCount: number | null
}

interface PendingRegistrationSummary {
  notifId: string
  hotelId: string
  registered: boolean
  fixtureName?: string
}

// ---------------------------------------------------------------------------
// Fixture 加载辅助
// ---------------------------------------------------------------------------

/**
 * 读取并解析 fixture JSON 文件，文件缺失时返回 null（不抛异常）
 */
function resolveFixturePath(relativePath: string): string {
  const sourcePath = path.join(__dirname, '..', 'fixtures', relativePath)
  if (fs.existsSync(sourcePath)) {
    return sourcePath
  }
  return path.join(__dirname, '..', '..', 'src', 'fixtures', relativePath)
}

function loadFixture(relativePath: string): JsonObject | null {
  const absPath = resolveFixturePath(relativePath)
  try {
    if (!fs.existsSync(absPath)) return null
    const raw = fs.readFileSync(absPath, 'utf-8')
    return JSON.parse(raw)
  } catch (_err) {
    return null
  }
}

/**
 * 在字符串中替换 HOTEL_ID / NOTIF_ID_* 占位符
 *
 * - HOTEL_ID -> 实际 hotelId
 * - NOTIF_ID_<XXX> -> 随机 uuid（同一占位符在一次替换中保持一致）
 */
function applyPlaceholders(text: string, hotelId: string): string {
  let result = text.replace(/HOTEL_ID/g, String(hotelId))

  const notifMap = new Map<string, string>()
  result = result.replace(/NOTIF_ID_[A-Za-z0-9_]+/g, (match) => {
    const existing = notifMap.get(match)
    if (existing) {
      return existing
    }
    const generated = uuidv4()
    notifMap.set(match, generated)
    return generated
  })

  return result
}

/**
 * 对任意 JSON 结构做占位符替换：序列化 -> 替换 -> 反序列化
 */
function fillFixture(data: JsonObject | null, hotelId: string): JsonObject | null {
  if (data == null) return data
  try {
    const text = JSON.stringify(data)
    const replaced = applyPlaceholders(text, hotelId)
    return JSON.parse(replaced)
  } catch (_err) {
    return data
  }
}

/**
 * 加载预订 fixture，缺失时返回最小可用结构
 */
function loadReservationFixture(relativePath: string, hotelId: string): JsonObject {
  const data = loadFixture(relativePath)
  if (!data) {
    return { hotelid: String(hotelId), reservations: [] }
  }
  const filled = fillFixture(data, hotelId)
  if (!filled) {
    return { hotelid: String(hotelId), reservations: [] }
  }
  // 兜底补充 hotelid
  if (filled && typeof filled === 'object' && !filled.hotelid) {
    filled.hotelid = String(hotelId)
  }
  return filled
}

function cloneJson<T>(value: T): T {
  return JSON.parse(JSON.stringify(value))
}

function getReservations(payload: JsonObject): JsonObject[] {
  if (!payload || !Array.isArray(payload.reservations)) {
    return []
  }
  return payload.reservations.filter((item) => item && typeof item === 'object') as JsonObject[]
}

function getNotifIds(payload: JsonObject): string[] {
  const reservationNotif = payload ? payload.reservation_notif : null
  const rawIds = reservationNotif ? reservationNotif.reservation_notif_id : null
  if (!Array.isArray(rawIds)) {
    return []
  }

  const ids: string[] = []
  for (const rawId of rawIds) {
    const notifId = String(rawId || '').trim()
    if (notifId) {
      ids.push(notifId)
    }
  }
  return ids
}

function buildPendingReservation(
  fixturePath: string,
  hotelId: string,
  notifId: string,
  index: number,
): JsonObject | null {
  const fixturePayload = loadReservationFixture(fixturePath, hotelId)
  const reservations = getReservations(fixturePayload)
  if (reservations.length === 0) {
    return null
  }

  const sourceReservation = reservations[index % reservations.length]
  const reservation = cloneJson(sourceReservation)
  reservation.hotel_id = String(hotelId)
  reservation.reservation_notif_id = notifId
  return reservation
}

function registerPullScenarioReservations(
  scenario: Scenario,
  hotelId: string,
  payload: JsonObject,
): PendingRegistrationSummary[] {
  if (scenario.type !== 'reservation' || scenario.mode !== 'pull' || !scenario.fixturePath) {
    return []
  }

  const notifIds = getNotifIds(payload)
  const registered: PendingRegistrationSummary[] = []

  for (let index = 0; index < notifIds.length; index += 1) {
    const notifId = notifIds[index]
    const reservation = buildPendingReservation(scenario.fixturePath, hotelId, notifId, index)
    if (!reservation) {
      registered.push({
        notifId,
        hotelId,
        registered: false,
        fixtureName: scenario.fixturePath,
      })
      continue
    }

    const result = registerPendingReservation({
      notifId,
      hotelId,
      reservation,
      fixtureName: scenario.fixturePath,
    })

    registered.push({
      notifId,
      hotelId,
      registered: Boolean(result),
      fixtureName: scenario.fixturePath,
    })
  }

  return registered
}

/**
 * 加载消息 fixture，缺失时返回最小可用结构
 */
function loadMessageFixture(relativePath: string, hotelId: string): JsonObject {
  const data = loadFixture(relativePath)
  if (!data) {
    return { hotelid: String(hotelId), messages: [] }
  }
  const filled = fillFixture(data, hotelId)
  if (!filled) {
    return { hotelid: String(hotelId), messages: [] }
  }
  if (filled && typeof filled === 'object' && !filled.hotelid) {
    filled.hotelid = String(hotelId)
  }
  return filled
}

// ---------------------------------------------------------------------------
// 预设场景定义
// ---------------------------------------------------------------------------

export const scenarios: Scenario[] = [
  {
    name: 'new-booking-pull',
    label: '新预订 (Pull模式 - Booking.com)',
    description: '发送预订通知ID，PMS会回调Mock API拉取详情',
    type: 'reservation',
    mode: 'pull',
    fixturePath: 'reservations/new-booking.json',
    getPayload(hotelId) {
      const notifId = uuidv4()
      return {
        hotelid: String(hotelId),
        reservation_notif: {
          reservation_notif_id: [notifId],
        },
      }
    },
  },
  {
    name: 'new-booking-push',
    label: '新预订 (Push模式 - Booking.com)',
    description: '直接推送完整预订数据，无需 PMS 回拉',
    type: 'reservation',
    mode: 'push',
    getPayload(hotelId) {
      return loadReservationFixture('reservations/new-booking.json', hotelId)
    },
  },
  {
    name: 'airbnb-booking-push',
    label: 'Airbnb 新预订 (Push模式)',
    description: '推送 Airbnb 渠道的完整预订数据',
    type: 'reservation',
    mode: 'push',
    getPayload(hotelId) {
      return loadReservationFixture('reservations/airbnb-booking.json', hotelId)
    },
  },
  {
    name: 'modification-push',
    label: '修改预订 (Push模式)',
    description: '推送预订变更（修改入住日期/房型等）',
    type: 'reservation',
    mode: 'push',
    getPayload(hotelId) {
      return loadReservationFixture('reservations/modification.json', hotelId)
    },
  },
  {
    name: 'cancellation-push',
    label: '取消预订 (Push模式)',
    description: '推送预订取消通知',
    type: 'reservation',
    mode: 'push',
    getPayload(hotelId) {
      return loadReservationFixture('reservations/cancellation.json', hotelId)
    },
  },
  {
    name: 'multi-room-push',
    label: '多房间预订 (Push模式)',
    description: '一笔预订包含多个房间/房型',
    type: 'reservation',
    mode: 'push',
    getPayload(hotelId) {
      return loadReservationFixture('reservations/multi-room.json', hotelId)
    },
  },
  {
    name: 'batch-pull',
    label: '批量预订 (Pull模式, >20条, 触发异步处理)',
    description: '一次发送 25 个 notif_id，验证 PMS 的异步批处理逻辑',
    type: 'reservation',
    mode: 'pull',
    fixturePath: 'reservations/new-booking.json',
    batchCount: 25,
    getPayload(hotelId) {
      const ids = []
      for (let i = 0; i < 25; i += 1) ids.push(uuidv4())
      return {
        hotelid: String(hotelId),
        reservation_notif: {
          reservation_notif_id: ids,
        },
      }
    },
  },
  {
    name: 'guest-message',
    label: '客人消息',
    description: '推送一条来自客人的咨询消息',
    type: 'messaging',
    getPayload(hotelId) {
      return loadMessageFixture('messages/guest-inquiry.json', hotelId)
    },
  },
]

/**
 * 按名称查找场景定义
 * @param {string} name
 */
export function getScenario(name: string): Scenario | null {
  if (!name) return null
  return scenarios.find((s) => s.name === name) || null
}

/**
 * 序列化场景元数据（不含 getPayload）供 API 返回
 */
function serializeScenario(scenario: Scenario): SerializedScenario {
  return {
    name: scenario.name,
    label: scenario.label,
    description: scenario.description,
    type: scenario.type,
    mode: scenario.mode || null,
    batchCount: scenario.batchCount || null,
  }
}

/**
 * 根据场景类型 + payload 执行实际发送
 */
async function dispatchScenario(scenario: Scenario, hotelId: string, payload: JsonObject) {
  if (scenario.type === 'messaging') {
    const result = await sendMessagingWebhook({ hotelId, payload })
    return {
      pendingReservations: [],
      result,
    }
  }

  const pendingReservations = registerPullScenarioReservations(scenario, hotelId, payload)
  const result = await sendReservationWebhook({
    mode: scenario.mode || 'pull',
    hotelId,
    payload,
  })

  return {
    pendingReservations,
    result,
  }
}

// ---------------------------------------------------------------------------
// Express Router
// ---------------------------------------------------------------------------

export const router = express.Router()

function listScenariosHandler(_req: Request, res: Response) {
  res.json({
    success: true,
    data: scenarios.map(serializeScenario),
  })
}

async function sendScenarioHandler(req: Request, res: Response) {
  const name = String(req.params.name)
  const scenario = getScenario(name)
  if (!scenario) {
    res.status(404).json({
      success: false,
      message: `Scenario not found: ${name}`,
    })
    return
  }

  const hotelId = String((req.body && req.body.hotelId) || config.defaultHotelId)
  let payload: JsonObject
  try {
    payload =
      req.body && req.body.customPayload
        ? req.body.customPayload
        : scenario.getPayload(hotelId)
  } catch (err) {
    const message = err instanceof Error ? err.message : String(err)
    res.status(500).json({
      success: false,
      message: `Failed to build payload: ${message}`,
    })
    return
  }

  try {
    const dispatchResult = await dispatchScenario(scenario, hotelId, payload)
    res.json({
      success: true,
      scenario: serializeScenario(scenario),
      pendingReservations: dispatchResult.pendingReservations,
      result: dispatchResult.result,
    })
  } catch (err) {
    const message = err instanceof Error ? err.message : String(err)
    res.status(500).json({
      success: false,
      scenario: serializeScenario(scenario),
      message,
    })
  }
}

async function sendCustomReservationHandler(req: Request, res: Response) {
  const body = (req.body || {}) as JsonObject
  const mode = body.mode === 'push' ? 'push' : 'pull'
  const hotelId = String(body.hotelId || (body.payload && body.payload.hotelid) || config.defaultHotelId)
  const payload = body.payload

  if (!payload || typeof payload !== 'object') {
    res.status(400).json({
      success: false,
      message: 'payload (object) is required',
    })
    return
  }

  try {
    const result = await sendReservationWebhook({ mode, hotelId, payload })
    res.json({ success: true, type: 'reservation', mode, result })
  } catch (err) {
    const message = err instanceof Error ? err.message : String(err)
    res.status(500).json({
      success: false,
      message,
    })
  }
}

async function sendCustomMessagingHandler(req: Request, res: Response) {
  const body = (req.body || {}) as JsonObject
  const hotelId = String(body.hotelId || config.defaultHotelId)
  const payload = body.payload

  if (!payload || typeof payload !== 'object') {
    res.status(400).json({
      success: false,
      message: 'payload (object) is required',
    })
    return
  }

  try {
    const result = await sendMessagingWebhook({ hotelId, payload })
    res.json({ success: true, type: 'messaging', result })
  } catch (err) {
    const message = err instanceof Error ? err.message : String(err)
    res.status(500).json({
      success: false,
      message,
    })
  }
}

async function sendCustomWebhookHandler(req: Request, res: Response) {
  const body = (req.body || {}) as JsonObject
  if (body.type === 'messaging') {
    await sendCustomMessagingHandler(req, res)
    return
  }
  await sendCustomReservationHandler(req, res)
}

// 清晰管理 API
router.get('/api/webhooks/scenarios', listScenariosHandler)
router.post('/api/webhooks/scenarios/:name/send', sendScenarioHandler)
router.post('/api/webhooks/reservation/send', sendCustomReservationHandler)
router.post('/api/webhooks/messaging/send', sendCustomMessagingHandler)

// 兼容既有入口
router.get('/scenarios', listScenariosHandler)
router.post('/scenarios/:name/send', sendScenarioHandler)
router.post('/webhook/send', sendCustomWebhookHandler)
