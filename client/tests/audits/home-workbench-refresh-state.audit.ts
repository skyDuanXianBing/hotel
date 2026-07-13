import { readFileSync } from 'node:fs'
import { dirname, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'

const auditDir = dirname(fileURLToPath(import.meta.url))
const clientRoot = resolve(auditDir, '../..')
const read = (path: string) => readFileSync(resolve(clientRoot, path), 'utf8')
const assert = (condition: boolean, message: string): void => {
  if (!condition) {
    console.error(`[home-workbench-refresh-state.audit] ${message}`)
    process.exit(1)
  }
}

const assertMatches = (source: string, pattern: RegExp, message: string): void => {
  assert(pattern.test(source), message)
}

const composable = read('src/views/home/composables/useHomeTaskWorkbench.ts')
const component = read('src/views/home/components/TaskWorkbench.vue')
const internalTaskCenter = read('src/views/home/components/InternalTaskCenter.vue')
const loadStart = composable.indexOf('const executeWorkbenchRequest = (')
const loadEnd = composable.indexOf('const resetWorkbenchPage = () =>')
const loadSource = composable.slice(loadStart, loadEnd)
const clearStart = composable.indexOf('const resetWorkbenchPage = () =>')
const clearEnd = composable.indexOf('const loadWorkbench = async', clearStart)
const clearSource = composable.slice(clearStart, clearEnd)
const refreshFromTopStart = composable.indexOf('const refreshFromTop = async () =>')
const refreshFromTopEnd = composable.indexOf('const assignTask = async', refreshFromTopStart)
const refreshFromTopSource = composable.slice(refreshFromTopStart, refreshFromTopEnd)
const submitCreateStart = internalTaskCenter.indexOf('const submitCreate = async () =>')
const submitCreateEnd = internalTaskCenter.indexOf('const saveAssignee = async', submitCreateStart)
const submitCreateSource = internalTaskCenter.slice(submitCreateStart, submitCreateEnd)
const createdRefreshStart = component.indexOf('const refreshAfterInternalTaskCreated = () =>')
const createdRefreshEnd = component.indexOf('const refreshWhenActive = () =>', createdRefreshStart)
const createdRefreshSource = component.slice(createdRefreshStart, createdRefreshEnd)

assert(loadStart >= 0 && loadEnd > loadStart, 'workbench request coordinator must be present')
assert(!loadSource.includes('workbenchData.value = null'), 'refresh failure must preserve workbench data')
assert(!loadSource.includes('assignSelections.value = {}'), 'refresh failure must preserve assignments')
assert(loadSource.includes("loadError.value = ''"), 'successful refresh must clear the error state')
assert(loadSource.includes('activeRequest !== entry'), 'stale requests must be rejected')
assert(loadSource.includes('runningRequest.context.key === context.key'), 'same-context requests must be reused')
assert(loadSource.includes('return runningRequest.promise'), 'single-flight callers must share one promise')
assert(loadSource.includes('runningRequest.controller.abort()'), 'context changes must cancel stale requests')
assert(loadSource.includes('signal: entry.controller.signal'), 'HTTP requests must receive an abort signal')
assertMatches(
  loadSource,
  /options\.markTrailing\s*&&\s*runningRequest\.canScheduleTrailing[\s\S]*runningRequest\.trailingRequested\s*=\s*true/,
  'event/mutation refreshes must mark one trailing refresh on the active request',
)
assertMatches(
  loadSource,
  /entry\.canScheduleTrailing\s*&&\s*entry\.trailingRequested[\s\S]*executeWorkbenchRequest\(createRequestContext\(\), false, false\)/,
  'an active request must consume its marker with exactly one non-recursive trailing request',
)
assert(composable.includes('new AbortController()'), 'requests must be cancellable')
assert(
  composable.includes("candidate.code === 'ERR_CANCELED'") ||
    composable.includes("candidate.name === 'AbortError'"),
  'axios/browser cancellation must be recognized from the complete composable',
)
assert(
  composable.includes("candidate.name === 'CanceledError'"),
  'axios CanceledError must be recognized from the complete composable',
)
assertMatches(
  loadSource,
  /if \(!isCancellationError\(error\)[\s\S]*loadError\.value\s*=/,
  'cancellation must bypass logging and user-visible error state',
)
assert(loadSource.includes('suppressErrorToast: true'), 'background refresh must not create toast storms')
assert(clearSource.includes('workbenchData.value = null'), 'explicit context clear must remove old-store data')
assert(clearSource.includes('assignSelections.value = {}'), 'explicit context clear must remove assignments')
assert(clearSource.includes('activeRequest?.controller.abort()'), 'hard refresh must abort the old first-page request')
assert(clearSource.includes('loadMoreRequest?.controller.abort()'), 'hard refresh must abort the old load-more request')
assert(clearSource.includes('nextCursor.value = null'), 'hard refresh must invalidate the old cursor')
assert(clearSource.includes('hasMore.value = false'), 'hard refresh must clear old pagination state')
assert(
  refreshFromTopSource.indexOf('resetWorkbenchPage()') <
    refreshFromTopSource.indexOf('reloadWorkbenchData()'),
  'hard refresh must clear the old snapshot before requesting the current first page',
)
assert(
  !clearSource.includes('activeType.value =') && !clearSource.includes('activeStatus.value ='),
  'hard refresh must retain the current type/status filters',
)
assert(
  loadSource.includes('requestParamsFor(context, null, true)'),
  'first-page refresh must use cursor=null and include summaries',
)
assert(component.includes('v-if="loadError"'), 'first-load and refresh errors must be observable')
assert(component.includes(':type="hasWorkbenchData ? \'warning\' : \'error\'"'), 'error severity must distinguish cached data')
assert(component.includes('REFRESH_EVENT_DEBOUNCE_MS = 350'), 'refresh events must be debounced')
assertMatches(
  component,
  /performWorkbenchRefresh\('event'\)/,
  'debounced focus/visibility/websocket events must use the event refresh source',
)
assertMatches(
  component,
  /reloadWorkbenchData\(\{ markTrailing: source !== 'manual' \}\)/,
  'event refresh source must explicitly request trailing coalescing',
)
assertMatches(
  component,
  /refreshFromTop\(\)/,
  'manual refresh must start a new snapshot from the top',
)
assert(component.includes('window.setTimeout'), 'polling must be scheduled after request completion')
assert(!component.includes('window.setInterval'), 'fixed intervals must not overlap slow requests')
assert(component.includes('disposeWorkbench()'), 'unmount must abort the active request')
assert(component.includes('clearRefreshDebounce()'), 'unmount/store changes must clear queued events')
assert(
  internalTaskCenter.includes("emit('updated', 'created')"),
  'successful internal-task creation must emit the created reason',
)
assert(
  submitCreateStart >= 0 &&
    submitCreateEnd > submitCreateStart &&
    submitCreateSource.indexOf('if (!response.success) throw') <
      submitCreateSource.indexOf("emit('updated', 'created')") &&
    submitCreateSource.indexOf("emit('updated', 'created')") < submitCreateSource.indexOf('} catch'),
  'failed creates must not emit a hard-refresh event',
)
assert(
  component.includes('@updated="handleInternalTaskUpdated"'),
  'internal-task mutations must be routed through the reason-aware handler',
)
assert(createdRefreshStart >= 0 && createdRefreshEnd > createdRefreshStart, 'created hard-refresh handler must exist')
assert(
  createdRefreshSource.includes('createdRefreshPromise'),
  'duplicate created invalidations must share one hard-refresh flight',
)
assert(
  createdRefreshSource.includes('refreshFromTop()'),
  'created invalidations must reset and reload summaries plus the current first page',
)
assert(
  (createdRefreshSource.match(/refreshFromTop\(\)/g) || []).length === 1,
  'one created event must start exactly one hard-refresh request chain',
)
assert(
  !createdRefreshSource.includes('loadedCount') &&
    !createdRefreshSource.includes('document.visibilityState'),
  'created hard refresh must apply to shallow, deep-scrolled, and hidden workbench states',
)
assert(
  createdRefreshSource.includes('clearRefreshTimer()') &&
    createdRefreshSource.includes('clearRefreshDebounce()'),
  'created invalidations must supersede queued poll/event refreshes',
)
assert(
  createdRefreshSource.includes("reason === 'created'") &&
    createdRefreshSource.includes('queueWorkbenchRefresh()'),
  'only created mutations may hard refresh; ordinary mutations must keep preserve semantics',
)

console.log('[home-workbench-refresh-state.audit] ok')
