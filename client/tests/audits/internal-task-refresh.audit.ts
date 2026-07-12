import { readFileSync } from 'node:fs'
import { dirname, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'

const auditDir = dirname(fileURLToPath(import.meta.url))
const clientRoot = resolve(auditDir, '../..')
const read = (path: string) => readFileSync(resolve(clientRoot, path), 'utf8')

const assert = (condition: boolean, message: string): void => {
  if (!condition) {
    console.error(`[internal-task-refresh.audit] ${message}`)
    process.exit(1)
  }
}

const center = read('src/views/home/components/InternalTaskCenter.vue')
const cleanerPanel = read('src/views/cleaner/components/CleanerInternalTaskPanel.vue')

assert(!center.includes('@click="loadCurrentTab"'), 'manager refresh must not forward PointerEvent')
assert(!center.includes('@open="loadCurrentTab"'), 'drawer open must use an argument-free wrapper')
assert(center.includes('@click="handleRefresh"'), 'manager refresh wrapper is required')
assert(center.includes('const handleRefresh = () => void loadCurrentTab(0)'), 'manager refresh must request page 0')
assert(center.includes('page: normalizedPage'), 'manager request must use a normalized numeric page')
assert(!center.includes('pageData.value = emptyPage()\n    }\n  } finally'), 'manager load failure must preserve prior page data')

assert(!cleanerPanel.includes('@click="loadTasks"'), 'cleaner refresh must not forward PointerEvent')
assert(!cleanerPanel.includes('@tab-change="loadTasks"'), 'cleaner tab change must use a wrapper')
assert(cleanerPanel.includes('@click="handleRefresh"'), 'cleaner refresh wrapper is required')
assert(!cleanerPanel.includes('pageData.value = emptyPage()\n      ElMessage.error'), 'cleaner load failure must preserve prior page data')

console.log('[internal-task-refresh.audit] ok')
