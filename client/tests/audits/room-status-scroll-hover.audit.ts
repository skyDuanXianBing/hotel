import { readFileSync } from 'node:fs'
import { dirname, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'

const auditDir = dirname(fileURLToPath(import.meta.url))
const clientRoot = resolve(auditDir, '../..')
const calendar = readFileSync(resolve(clientRoot, 'src/views/room-status/RoomStatusCalendar.vue'), 'utf8')
const assert = (condition: boolean, message: string): void => {
  if (!condition) throw new Error(`[room-status-scroll-hover.audit] ${message}`)
}

const handlerStart = calendar.indexOf('const onCalendarScroll =')
const handlerEnd = calendar.indexOf('const normalizeReservationDetail', handlerStart)
const handler = calendar.slice(handlerStart, handlerEnd)

assert(calendar.includes('@scroll.passive="onCalendarScroll"'), 'scroller must use one passive handler')
assert(handlerStart >= 0 && handlerEnd > handlerStart, 'scroll handler must remain auditable')
assert(!handler.includes('getBoundingClientRect'), 'scroll handler must not read geometry')
assert(!handler.includes('scrollLeft'), 'scroll handler must not synchronize scroll positions')
assert(!handler.includes('getRoomStatusCalendar'), 'scroll handler must not request calendar data')
assert(!handler.includes('getReservationHoverSummaries'), 'scroll handler must not request hover data')
assert(calendar.includes('if (calendarIsScrolling) return'), 'mouseenter must be suppressed while scrolling')
assert(calendar.includes('CALENDAR_WINDOW_SCROLL_SETTLE_MS = 140'), 'hover must recover after settling')

console.log('[room-status-scroll-hover.audit] ok')
