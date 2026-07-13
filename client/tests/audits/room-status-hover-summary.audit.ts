import { readFileSync } from 'node:fs'
import { dirname, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'

const auditDir = dirname(fileURLToPath(import.meta.url))
const clientRoot = resolve(auditDir, '../..')
const read = (path: string) => readFileSync(resolve(clientRoot, path), 'utf8')
const assert = (condition: boolean, message: string): void => {
  if (!condition) throw new Error(`[room-status-hover-summary.audit] ${message}`)
}

const calendar = read('src/views/room-status/RoomStatusCalendar.vue')
const hoverStart = calendar.indexOf('const onCellHover =')
const hoverEnd = calendar.indexOf('// 隐藏悬停卡片', hoverStart)
const hoverHandler = calendar.slice(hoverStart, hoverEnd)

assert(hoverStart >= 0 && hoverEnd > hoverStart, 'hover handler must remain auditable')
assert(!hoverHandler.includes('getReservationById'), 'cell hover must not request reservation details')
assert(!hoverHandler.includes('getTotalPayment'), 'cell hover must not request payment totals')
assert(calendar.includes('getReservationHoverSummaries'), 'calendar must prefetch batch summaries')
assert(
  calendar.includes('scheduleWindowHoverSummaryPrefetch(summaryContext)'),
  'initial summary prefetch must be scheduled after calendar assignment',
)
assert(calendar.includes('requestAnimationFrame(() =>'), 'summary prefetch must wait for a frame')
assert(
  !calendar.includes('prefetchHoverSummaries(reservationIds, summaryContext)'),
  'calendar assignment must not immediately start the batch request',
)
assert(calendar.includes('totalAmountKnown:'), 'calendar amount missingness must be preserved')
assert(calendar.includes('stopHoverSummaryRequests()'), 'unmount/deactivation cleanup must remain')

console.log('[room-status-hover-summary.audit] ok')
