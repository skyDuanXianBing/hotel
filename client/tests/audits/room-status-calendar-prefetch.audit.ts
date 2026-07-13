import { readFileSync } from 'node:fs'
import { dirname, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'

const auditDir = dirname(fileURLToPath(import.meta.url))
const clientRoot = resolve(auditDir, '../..')
const read = (path: string) => readFileSync(resolve(clientRoot, path), 'utf8')
const assert = (condition: boolean, message: string): void => {
  if (!condition) throw new Error(`[room-status-calendar-prefetch.audit] ${message}`)
}

const calendar = read('src/views/room-status/RoomStatusCalendar.vue')
const navigationStart = calendar.indexOf('const navigateToCalendarWindow =')
const navigationEnd = calendar.indexOf('// 业务变更和首次进入', navigationStart)
const navigation = calendar.slice(navigationStart, navigationEnd)
const prefetchStart = calendar.indexOf('const prefetchCalendarWindow =')
const prefetchEnd = calendar.indexOf('const scheduleAdjacentCalendarWindows', prefetchStart)
const prefetch = calendar.slice(prefetchStart, prefetchEnd)
const applyStart = calendar.indexOf('const applyCalendarWindow =')
const applyEnd = calendar.indexOf('const prefetchCalendarWindow =', applyStart)
const apply = calendar.slice(applyStart, applyEnd)
const defaultPriceStart = calendar.indexOf('const loadCalendarDefaultManagementPrices =')
const planPriceEnd = calendar.indexOf('const loadCalendarPricePlanOptions =', defaultPriceStart)
const priceLoaders = calendar.slice(defaultPriceStart, planPriceEnd)
const roomTypesStart = calendar.indexOf('const loadRoomTypesData =')
const roomTypesEnd = calendar.indexOf('const initFilterOptions =', roomTypesStart)
const roomTypesLoader = calendar.slice(roomTypesStart, roomTypesEnd)
const sensitiveClearStart = calendar.indexOf('const clearSensitiveCalendarUiState =')
const sensitiveClearEnd = calendar.indexOf('const invalidateCalendarWindowContext =', sensitiveClearStart)
const sensitiveClear = calendar.slice(sensitiveClearStart, sensitiveClearEnd)
const contextInvalidationStart = calendar.indexOf('const invalidateCalendarWindowContext =')
const contextInvalidationEnd = calendar.indexOf('const transformCalendarWindow =', contextInvalidationStart)
const contextInvalidation = calendar.slice(contextInvalidationStart, contextInvalidationEnd)

assert(navigationStart >= 0 && navigationEnd > navigationStart, 'navigation must remain auditable')
assert(navigation.includes('calendarWindowCache.get(key)'), 'navigation must check cache first')
assert(!navigation.includes('loadRoomTypesData'), 'navigation must not rebuild room skeletons')
assert(!navigation.includes('loading.value = true'), 'navigation hit/miss must avoid full-table loading')
assert(prefetch.includes('requestCalendarWindow(range)'), 'prefetch must use the calendar read path')
assert(!prefetch.includes('calendarData.value'), 'prefetch must never expand or update current DOM data')
assert(calendar.includes('maxEntries: 3'), 'calendar window cache must remain bounded')
assert(calendar.includes('scheduleCalendarWindowIdleTask'), 'neighbor requests must wait for idle')
assert(calendar.includes('shiftCalendarWindow'), 'neighbor ranges must reuse exact navigation shifts')
assert(apply.includes('calendarData.value = data'), 'a legal empty calendar response must be applied')
assert(
  apply.includes('if (!data.rooms.length)') && apply.includes('roomExtraStatus.value = new Map()'),
  'a legal empty calendar response must clear prior room overlays',
)
assert(
  !apply.includes("console.warn('后端返回的房间数据为空，保留当前房态数据')"),
  'a legal empty response must never preserve another context calendar',
)
assert(
  calendar.includes('invalidateCalendarWindowContext({ clearApplied: true })'),
  'context changes must clear previously applied guest data immediately',
)
assert(
  roomTypesLoader.includes('response.success && Array.isArray(response.data)'),
  'a successful empty room-type response must clear the previous room skeleton',
)
assert(
  !roomTypesLoader.includes('response.data.length > 0'),
  'room-type loading must not treat a legal empty result as a failure',
)
assert(
  (priceLoaders.match(/isCalendarPriceCommitCurrent\(/g) || []).length >= 2,
  'both price loaders must guard async commits by generation and context',
)
assert(
  calendar.includes('lastAppliedCalendarRange?.[0] === startDate'),
  'price responses must also match the last successfully applied calendar range',
)
assert(
  calendar.includes('restoreVisibleRangeAfterNavigationFailure()'),
  'foreground failures must restore the last applied range',
)
assert(
  calendar.includes('if (restoringLastAppliedCalendarRange)'),
  'range restoration must bypass the visible-range reload watcher',
)
assert(
  sensitiveClearStart >= 0 && sensitiveClearEnd > sensitiveClearStart,
  'sensitive calendar UI cleanup must remain centralized and auditable',
)
;[
  'showBookingSidebar.value = false',
  'showBookingDetailSidebar.value = false',
  'showCancelReservationSidebar.value = false',
  'showAddConsumptionSidebar.value = false',
  'showPaymentSidebar.value = false',
  'showRoomChangeConfirmDialog.value = false',
  'paymentList.value = []',
  'consumptionList.value = []',
  'totalPayment.value = 0',
  'totalConsumption.value = 0',
  'selectedRefundRecords.value = []',
  'channelInfo.value = null',
  'pendingRoomChange.value = null',
  'selectedReservation.value = null',
].forEach((statement) => {
  assert(sensitiveClear.includes(statement), `sensitive cleanup must include: ${statement}`)
})
assert(
  sensitiveClear.includes('sensitiveCalendarUiGeneration += 1') &&
    sensitiveClear.includes('reservationDetailRequestId.value += 1') &&
    sensitiveClear.includes('ElMessageBox.close()'),
  'context cleanup must invalidate pending sensitive detail responses',
)
assert(
  contextInvalidation.includes('if (options.clearApplied)') &&
    contextInvalidation.includes('clearSensitiveCalendarUiState()'),
  'clearApplied context invalidation must call centralized sensitive cleanup',
)
assert(
  (calendar.match(/generation !== sensitiveCalendarUiGeneration/g) || []).length >= 2 &&
    (calendar.match(/generation === sensitiveCalendarUiGeneration/g) || []).length >= 4,
  'sensitive async loaders must reject responses from a previous context generation',
)

console.log('[room-status-calendar-prefetch.audit] ok')
