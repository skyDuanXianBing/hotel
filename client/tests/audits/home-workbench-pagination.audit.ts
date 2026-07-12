import { readFileSync } from 'node:fs'
import { dirname, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'
import {
  allowedStatusesByType,
  getWorkbenchStatusLabelKey,
  normalizeWorkbenchStatus,
  resolveWorkbenchTypeCounts,
  toWorkbenchStatusParam,
} from '../../src/views/home/composables/workbenchPagination'

const root = resolve(dirname(fileURLToPath(import.meta.url)), '../..')
const read = (path: string) => readFileSync(resolve(root, path), 'utf8')
const assert = (condition: boolean, message: string) => {
  if (!condition) throw new Error(`[home-workbench-pagination.audit] ${message}`)
}

const api = read('src/api/homeWorkbench.ts')
const composable = read('src/views/home/composables/useHomeTaskWorkbench.ts')
const component = read('src/views/home/components/TaskWorkbench.vue')

const expectedStatusMatrix = {
  all: [
    'pending',
    'awaiting_review',
    'awaiting_reply',
    'unassigned',
    'assigned',
    'in_progress',
    'overdue',
    'completed',
  ],
  cleaning: ['pending', 'in_progress', 'overdue'],
  review: ['awaiting_review', 'completed'],
  order: ['pending'],
  message: ['awaiting_reply'],
  other: ['unassigned', 'assigned', 'completed'],
}
assert(
  JSON.stringify(allowedStatusesByType) === JSON.stringify(expectedStatusMatrix),
  'runtime type/status matrix must match the backend validation matrix',
)
assert(
  normalizeWorkbenchStatus('message', 'completed') === 'all',
  'invalid cached state must reset',
)
assert(
  normalizeWorkbenchStatus('cleaning', 'expired') === 'overdue',
  'legacy expired must map to overdue',
)
assert(
  toWorkbenchStatusParam('review', 'awaiting_reply') === undefined,
  'invalid status must not be sent',
)
assert(
  getWorkbenchStatusLabelKey('other', 'assigned') !==
    getWorkbenchStatusLabelKey('all', 'assigned'),
  'other task labels must not reuse global room-assignment labels',
)
const auditedCounts = resolveWorkbenchTypeCounts(
  [{ type: 'review', count: 584, connected: true }],
  584,
  true,
)
assert(auditedCounts.byType.get('review') === 584, 'type card must use backend count')
assert(auditedCounts.allCount === 584, 'all card must use backend page total')

for (const field of ['status?: string', 'cursor?: string', 'includeSummaries?: boolean']) {
  assert(api.includes(field), `API request must include ${field}`)
}
for (const field of [
  'query: {',
  'status: string | null',
  'returnedElements: number',
  'totalElements: number | null',
]) {
  assert(api.includes(field), `API response must include ${field}`)
}
assert(composable.includes('size: WORKBENCH_LIMIT'), 'first and later pages must request 50 items')
assert(composable.includes('loadMoreRequest?.context.key === context.key'), 'same cursor must be single-flight')
assert(composable.includes('return loadMoreRequest.promise'), 'repeated observer calls must share one request')
assert(composable.includes('appendUniqueWorkbenchItems'), 'later pages must append with stable-key deduplication')
assert(composable.includes('requestGeneration'), 'stale context responses must be rejected')
assert(composable.includes('loadMoreRequest?.controller.abort()'), 'context changes must abort the old next page')
assert(composable.includes("loadMoreError.value = t('pages.home.workbench.loadMoreFailed')"), 'page failure must be separate')
assert(composable.includes('data.page?.totalElements'), 'total must come from the backend page contract')
assert(composable.includes('responseMatchesContext'), 'response.query must be checked against the active query')
assert(!composable.includes('tasks.slice().sort'), 'client must preserve backend order')
assert(component.includes("rootMargin: '0px 0px 400px 0px'"), 'observer must prefetch 400px before the bottom')
assert(component.includes('load-more-error'), 'load-more failure must remain retryable')
assert(component.includes('handleLoadMoreRetry'), 'failed next page must have an explicit retry')
assert(component.includes('!loadMoreError.value'), 'observer must not create an automatic retry storm')
assert(component.includes('hasNewTasks'), 'deep-list refresh must show a user-controlled reset')
assert(component.includes('loadMoreObserver?.disconnect()'), 'observer must be disposed')

console.log('[home-workbench-pagination.audit] ok')
