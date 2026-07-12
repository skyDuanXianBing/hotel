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

const composable = read('src/views/home/composables/useHomeTaskWorkbench.ts')
const component = read('src/views/home/components/TaskWorkbench.vue')
const loadStart = composable.indexOf('const loadWorkbenchData = async')
const loadEnd = composable.indexOf('const reloadWorkbenchData = async')
const loadSource = composable.slice(loadStart, loadEnd)
const clearStart = composable.indexOf('const clearWorkbench = () =>')
const clearEnd = composable.indexOf('const loadWorkbench = async', clearStart)
const clearSource = composable.slice(clearStart, clearEnd)

assert(loadStart >= 0 && loadEnd > loadStart, 'loadWorkbenchData must be present')
assert(!loadSource.includes('workbenchData.value = null'), 'refresh failure must preserve workbench data')
assert(!loadSource.includes('assignSelections.value = {}'), 'refresh failure must preserve assignments')
assert(loadSource.includes("loadError.value = ''"), 'successful refresh must clear the error state')
assert(loadSource.includes('requestSeq !== workbenchRequestSeq'), 'stale requests must be rejected')
assert(loadSource.includes('suppressErrorToast: true'), 'background refresh must not create toast storms')
assert(clearSource.includes('workbenchData.value = null'), 'explicit context clear must remove old-store data')
assert(clearSource.includes('assignSelections.value = {}'), 'explicit context clear must remove assignments')
assert(component.includes('v-if="loadError"'), 'first-load and refresh errors must be observable')
assert(component.includes(':type="hasWorkbenchData ? \'warning\' : \'error\'"'), 'error severity must distinguish cached data')

console.log('[home-workbench-refresh-state.audit] ok')
