/**
 * Mock Su API - 预订拉取与 ack 端点
 *
 * 端点：
 *   POST /SUAPI/jservice/Reservation
 *   POST /SUAPI/jservice/Reservation_notif
 *
 * 行为：
 * 1. 通过 Bearer Token 校验（共用 tokenValidator）
 * 2. 从 reservation pending state 读取已注册 reservation。
 *    - webhook-sender 后续在推送 Reservation Notification 前调用 state API 注册待拉取预订；
 *    - PMS 当前通常只携带 hotelid，因此没有 notifId 时返回该 hotelid 下所有 pending；
 *    - 携带 notifId 时优先按 hotelid + notifId 精确命中。
 * 3. 未匹配到 pending 时返回明确空结果与诊断信息，不随机返回 fixture。
 * 4. Reservation_notif ack 会记录并移除对应 pending。
 *
 * 返回格式：{ "reservations": [ ... ] }
 */

import { Request, Response } from 'express'
import express from 'express'
import fs from 'fs'
import path from 'path'

import config from '../config'
import {
  ackPendingReservations,
  findPendingReservation,
  getPendingReservation,
  listPendingReservations,
  registerPendingReservation,
  type JsonObject,
} from '../state/reservationPendingState'
import tokenValidator from './tokenValidator'

const { v4: uuidv4 } = require('uuid') as { v4: () => string }

const router = express.Router()
const DEFAULT_SIMULATOR_STORE_TIME_ZONE = 'Asia/Tokyo'
const MS_PER_DAY = 24 * 60 * 60 * 1000
const DATE_ONLY_PATTERN = /^\d{4}-\d{2}-\d{2}$/
const DATE_TIME_PATTERN = /^(\d{4}-\d{2}-\d{2}) (\d{2}:\d{2}:\d{2})$/
const OFFSETLESS_DATE_TIME_PATTERN = /^\d{4}-\d{2}-\d{2}(?:T|\s)\d{2}:\d{2}(?::\d{2}(?:\.\d{1,3})?)?$/

interface BusinessDate {
  year: number
  month: number
  day: number
}

// ---------------------------------------------------------------------------
// Fixture 加载
// ---------------------------------------------------------------------------
const FIXTURE_FILES = [
  'new-booking.json',
  'modification.json',
  'cancellation.json',
  'airbnb-booking.json',
  'multi-room.json',
]

function resolveFixturePath(fileName: string): string {
  const sourcePath = path.join(__dirname, '..', 'fixtures', 'reservations', fileName)
  if (fs.existsSync(sourcePath)) {
    return sourcePath
  }
  return path.join(__dirname, '..', '..', 'src', 'fixtures', 'reservations', fileName)
}

function loadFixture(fileName: string): JsonObject {
  const fullPath = resolveFixturePath(fileName)
  const raw = fs.readFileSync(fullPath, 'utf8')
  return JSON.parse(raw)
}

// 启动时一次性加载，运行期通过深拷贝复用
const fixtureCache = FIXTURE_FILES.reduce<Record<string, JsonObject>>((acc, file) => {
  try {
    acc[file] = loadFixture(file)
  } catch (err) {
    // eslint-disable-next-line no-console
    const message = err instanceof Error ? err.message : String(err)
    console.warn(`[mock-su-api/reservation] failed to load fixture ${file}:`, message)
  }
  return acc
}, {})

function deepClone<T>(value: T): T {
  return JSON.parse(JSON.stringify(value))
}

// ---------------------------------------------------------------------------
// 工具：日期偏移，把 fixture 中的固定日期改为 "今天起" 的未来日期
// ---------------------------------------------------------------------------
function pad2(n: number): string {
  return n < 10 ? `0${n}` : String(n)
}

function normalizeEnvText(value: unknown): string | null {
  if (value === null || value === undefined) {
    return null
  }
  const text = String(value).trim()
  return text ? text : null
}

function getSimulatorStoreTimeZone(): string {
  const timeZone = normalizeEnvText(process.env.SIMULATOR_STORE_TIME_ZONE) || DEFAULT_SIMULATOR_STORE_TIME_ZONE
  try {
    new Intl.DateTimeFormat('en-US', { timeZone }).format(new Date(0))
  } catch {
    throw new Error(`Invalid SIMULATOR_STORE_TIME_ZONE: ${timeZone}`)
  }
  return timeZone
}

function parseSimulatorNow(todayOverride?: Date): { now: Date; fixedNow: string | null } {
  if (todayOverride) {
    return { now: todayOverride, fixedNow: null }
  }

  const fixedNow = normalizeEnvText(process.env.SIMULATOR_FIXED_NOW)
  if (!fixedNow) {
    return { now: new Date(), fixedNow: null }
  }

  let normalized = fixedNow
  if (DATE_ONLY_PATTERN.test(fixedNow)) {
    normalized = `${fixedNow}T00:00:00.000Z`
  } else if (OFFSETLESS_DATE_TIME_PATTERN.test(fixedNow)) {
    normalized = `${fixedNow.replace(' ', 'T')}Z`
  }

  const now = new Date(normalized)
  if (Number.isNaN(now.getTime())) {
    throw new Error(`Invalid SIMULATOR_FIXED_NOW: ${fixedNow}`)
  }
  return { now, fixedNow }
}

function getBusinessDateForInstant(instant: Date, timeZone: string): BusinessDate {
  const formatter = new Intl.DateTimeFormat('en-CA', {
    timeZone,
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
  })
  const parts = formatter.formatToParts(instant)
  const values: Record<string, number> = {}
  for (const part of parts) {
    if (part.type !== 'literal') {
      values[part.type] = Number(part.value)
    }
  }
  return {
    year: values.year,
    month: values.month,
    day: values.day,
  }
}

function getSimulatorBusinessDate(todayOverride?: Date): BusinessDate {
  const timeZone = getSimulatorStoreTimeZone()
  const parsed = parseSimulatorNow(todayOverride)
  return getBusinessDateForInstant(parsed.now, timeZone)
}

function formatBusinessDate(date: BusinessDate): string {
  return `${date.year}-${pad2(date.month)}-${pad2(date.day)}`
}

function parseBusinessDate(value: string): BusinessDate | null {
  if (!DATE_ONLY_PATTERN.test(value)) {
    return null
  }
  return {
    year: Number(value.slice(0, 4)),
    month: Number(value.slice(5, 7)),
    day: Number(value.slice(8, 10)),
  }
}

function businessDateToUtcTime(date: BusinessDate): number {
  return Date.UTC(date.year, date.month - 1, date.day)
}

function addBusinessDays(date: BusinessDate, days: number): BusinessDate {
  const next = new Date(businessDateToUtcTime(date) + days * MS_PER_DAY)
  return {
    year: next.getUTCFullYear(),
    month: next.getUTCMonth() + 1,
    day: next.getUTCDate(),
  }
}

function businessDaysBetween(from: BusinessDate, to: BusinessDate): number {
  return Math.round((businessDateToUtcTime(to) - businessDateToUtcTime(from)) / MS_PER_DAY)
}

/**
 * 计算 fixture 中第一间房的 arrival_date 与今天的差值，
 * 以此作为整体日期平移的基准，把所有日期推到 "今天 + N" 的未来。
 */
function computeDateShift(reservation: JsonObject, today: BusinessDate): number {
  const firstRoom = reservation.rooms && reservation.rooms[0]
  if (!firstRoom || !firstRoom.arrival_date) return 0

  const arrival = parseBusinessDate(String(firstRoom.arrival_date))
  if (!arrival) return 0

  // 让到达日距今天 7 天
  const desiredArrival = addBusinessDays(today, 7)
  return businessDaysBetween(arrival, desiredArrival)
}

function shiftDateString(value: unknown, shiftDays: number): unknown {
  if (!value || typeof value !== 'string') return value
  // 仅处理 YYYY-MM-DD 或 YYYY-MM-DD HH:mm:ss
  if (DATE_ONLY_PATTERN.test(value)) {
    const date = parseBusinessDate(value)
    return date ? formatBusinessDate(addBusinessDays(date, shiftDays)) : value
  }
  const dateTime = DATE_TIME_PATTERN.exec(value)
  if (dateTime) {
    const date = parseBusinessDate(dateTime[1])
    return date ? `${formatBusinessDate(addBusinessDays(date, shiftDays))} ${dateTime[2]}` : value
  }
  return value
}

const DATE_FIELDS_IN_RESERVATION = ['booked_at', 'modified_at', 'processed_at']
const DATE_FIELDS_IN_ROOM = ['arrival_date', 'departure_date']

function shiftReservationDates(reservation: JsonObject, shiftDays: number): void {
  if (!shiftDays) return
  for (const field of DATE_FIELDS_IN_RESERVATION) {
    if (reservation[field]) {
      reservation[field] = shiftDateString(reservation[field], shiftDays)
    }
  }
  if (Array.isArray(reservation.rooms)) {
    for (const room of reservation.rooms) {
      for (const field of DATE_FIELDS_IN_ROOM) {
        if (room[field]) {
          room[field] = shiftDateString(room[field], shiftDays)
        }
      }
      if (Array.isArray(room.price)) {
        for (const p of room.price) {
          if (p.date) p.date = shiftDateString(p.date, shiftDays)
        }
      }
    }
  }
}

// ---------------------------------------------------------------------------
// 占位符替换 + 字段动态填充
// ---------------------------------------------------------------------------
function generateChannelBookingId(reservation: JsonObject): string {
  // Airbnb 类用字母+数字风格，其他用纯数字
  const ota = reservation.affiliation && reservation.affiliation.OTA_Code
  if (ota === '244') {
    const rand = uuidv4().replace(/-/g, '').slice(0, 9).toUpperCase()
    return rand
  }
  // 10 位数字
  return String(Math.floor(1000000000 + Math.random() * 9000000000))
}

function generateNotifId(): string {
  return `NOTIF-${uuidv4()}`
}

/**
 * 用真实数据替换 fixture 中的占位符。
 * 占位符：
 *   - HOTEL_ID
 *   - NOTIF_ID_1 / NOTIF_ID_2 / NOTIF_ID_3 / NOTIF_ID_4 / NOTIF_ID_5
 */
function fillPlaceholders(
  reservation: JsonObject,
  hotelId: string,
  notifId: string,
  channelBookingId: string,
) {
  // hotel_id
  reservation.hotel_id = hotelId

  // reservation_notif_id
  reservation.reservation_notif_id = notifId

  // channel_booking_id
  const oldChannelBookingId = reservation.channel_booking_id
  reservation.channel_booking_id = channelBookingId

  // id 字段：{channel_booking_id}_{hotelId}
  reservation.id = `${channelBookingId}_${hotelId}`

  // 如果有 customer.bookingid 等关联字段也按需替换（当前 fixture 无）
  // 处理 booking_details 之类（消息 fixture，不在此处）

  // 为 thread_id / guest_id 保留（Airbnb 等渠道用）
  return { oldChannelBookingId }
}

interface BuildReservationOptions {
  today?: Date
  channelBookingId?: string
  notifId?: string
}

export function buildReservationFromFixture(
  fixtureFile: string,
  hotelId: string,
  options: BuildReservationOptions = {},
): JsonObject {
  const cached = fixtureCache[fixtureFile]
  if (!cached) {
    throw new Error(`fixture not found: ${fixtureFile}`)
  }
  const cloned = deepClone(cached)
  const reservation = cloned.reservations && cloned.reservations[0]
  if (!reservation) {
    throw new Error(`fixture has no reservation: ${fixtureFile}`)
  }

  const today = getSimulatorBusinessDate(options.today)
  const shiftDays = computeDateShift(reservation, today)
  shiftReservationDates(reservation, shiftDays)

  const channelBookingId = options.channelBookingId || generateChannelBookingId(reservation)
  const notifId = options.notifId || generateNotifId()

  fillPlaceholders(reservation, hotelId, notifId, channelBookingId)

  return reservation
}

// ---------------------------------------------------------------------------
// 路由
// ---------------------------------------------------------------------------
function firstText(...values: unknown[]): string | null {
  for (const value of values) {
    if (value === null || value === undefined) {
      continue
    }
    const text = String(value).trim()
    if (text.length > 0) {
      return text
    }
  }
  return null
}

function extractHotelId(body: JsonObject, query: JsonObject): string {
  return firstText(extractExplicitHotelId(body, query), config.defaultHotelId) || config.defaultHotelId
}

function extractExplicitHotelId(body: JsonObject, query: JsonObject): string | null {
  return firstText(
    body.hotelid,
    body.hotelId,
    body.hotel_id,
    query.hotelid,
    query.hotelId,
    query.hotel_id,
  )
}

function extractNotifId(body: JsonObject, query: JsonObject): string | null {
  return firstText(
    body.reservation_notif_id,
    body.reservationNotifId,
    body.notifid,
    body.notifId,
    query.reservation_notif_id,
    query.reservationNotifId,
    query.notifid,
    query.notifId,
  )
}

function toTextList(value: unknown): string[] {
  if (Array.isArray(value)) {
    const result: string[] = []
    for (const item of value) {
      const text = firstText(item)
      if (text) {
        result.push(text)
      }
    }
    return result
  }

  const text = firstText(value)
  return text ? [text] : []
}

function extractAckNotifIds(body: JsonObject): string[] {
  const reservationNotif = body.reservation_notif || body.reservationNotif || body.ReservationNotif || {}
  const nestedIds =
    reservationNotif.reservation_notif_id ||
    reservationNotif.reservationNotifId ||
    reservationNotif.notifid ||
    reservationNotif.notifId

  const directIds =
    body.reservation_notif_id ||
    body.reservationNotifId ||
    body.notifid ||
    body.notifId

  const ids = toTextList(nestedIds)
  if (ids.length > 0) {
    return ids
  }
  return toTextList(directIds)
}

export function registerNotifReservation(
  notifId: string,
  reservationData: JsonObject,
  hotelId?: string,
): boolean {
  const resolvedHotelId = firstText(hotelId, reservationData && reservationData.hotel_id, config.defaultHotelId)
  if (!resolvedHotelId) {
    return false
  }

  const pending = registerPendingReservation({
    notifId,
    hotelId: resolvedHotelId,
    reservation: reservationData,
  })

  return pending !== null
}

export function getNotifReservation(
  notifId: string | null | undefined,
  hotelId?: string,
): JsonObject | undefined {
  if (!notifId) {
    return undefined
  }
  const pending = getPendingReservation(notifId, hotelId)
  return pending ? pending.reservation : undefined
}

router.post('/SUAPI/jservice/Reservation', tokenValidator, (req: Request, res: Response) => {
  const body = (req.body || {}) as JsonObject
  const query = (req.query || {}) as JsonObject

  const explicitHotelId = extractExplicitHotelId(body, query)
  const hotelId = firstText(explicitHotelId, config.defaultHotelId) || config.defaultHotelId
  const notifId = extractNotifId(body, query)

  if (notifId) {
    let pending = findPendingReservation(hotelId, notifId)

    if (!pending && !explicitHotelId) {
      pending = getPendingReservation(notifId)
    }

    if (pending) {
      const reservation = deepClone(pending.reservation)
      reservation.hotel_id = pending.hotelId
      res.json({
        reservations: [reservation],
        diagnostic: {
          hotelid: pending.hotelId,
          reservation_notif_id: pending.notifId,
          pending_count: listPendingReservations(pending.hotelId).length,
        },
      })
      return
    }

    res.json({
      reservations: [],
      success: true,
      message: 'No pending reservation found for Reservation pull',
      diagnostic: {
        hotelid: hotelId,
        reservation_notif_id: notifId,
        pending_count: listPendingReservations(hotelId).length,
      },
    })
    return
  }

  const pendingReservations = listPendingReservations(hotelId)

  if (pendingReservations.length > 0) {
    const reservations = pendingReservations.map((pending) => {
      const reservation = deepClone(pending.reservation)
      reservation.hotel_id = pending.hotelId
      return reservation
    })

    res.json({
      reservations,
      diagnostic: {
        hotelid: hotelId,
        pending_count: pendingReservations.length,
        returned_count: reservations.length,
        reservation_notif_ids: pendingReservations.map((pending) => pending.notifId),
      },
    })
    return
  }

  res.json({
    reservations: [],
    success: true,
    message: 'No pending reservation found for Reservation pull',
    diagnostic: {
      hotelid: hotelId,
      reservation_notif_id: notifId,
      pending_count: listPendingReservations(hotelId).length,
    },
  })
})

router.post('/SUAPI/jservice/Reservation_notif', tokenValidator, (req: Request, res: Response) => {
  const body = (req.body || {}) as JsonObject
  const query = (req.query || {}) as JsonObject
  const hotelId = extractHotelId(body, query)
  const notifIds = extractAckNotifIds(body)

  if (notifIds.length === 0) {
    res.status(400).json({
      Status: 'Failure',
      message: 'reservation_notif.reservation_notif_id is required',
      diagnostic: {
        hotelid: hotelId,
      },
    })
    return
  }

  const results = ackPendingReservations(hotelId, notifIds)
  const ackedIds = results.filter((result) => result.acked).map((result) => result.notifId)

  res.json({
    Status: 'Success',
    reservation_notif: {
      reservation_notif_id: ackedIds,
    },
    acked: results,
    diagnostic: {
      hotelid: hotelId,
      pending_count: listPendingReservations(hotelId).length,
    },
  })
})

export default router
