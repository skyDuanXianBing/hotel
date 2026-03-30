import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const filePath = fileURLToPath(import.meta.url)
const currentDir = path.dirname(filePath)
const iosRoot = path.resolve(currentDir, '../..')

const routerSource = readFileSync(path.join(iosRoot, 'src/router/index.ts'), 'utf8')
const guardsSource = readFileSync(path.join(iosRoot, 'src/router/guards.ts'), 'utf8')
const tabsShellSource = readFileSync(path.join(iosRoot, 'src/views/shell/AppTabsShell.vue'), 'utf8')

const failures = []

const runCase = (category, name, assertion) => {
  try {
    assertion()
    console.log(`PASS [${category}] ${name}`)
  } catch (error) {
    const message = error instanceof Error ? error.message : String(error)
    failures.push({ category, name, message })
    console.error(`FAIL [${category}] ${name}`)
    console.error(`  ${message}`)
  }
}

runCase('Normal Path', '统计入口路由存在且包含统计 tab 入口', () => {
  assert.match(routerSource, /path:\s*'statistics'[\s\S]*name:\s*'StatisticsHome'/)
  assert.match(tabsShellSource, /<ion-tab-button\s+tab="statistics"[\s\S]*:href="ROUTE_PATHS\.statistics"/)
})

runCase('Normal Path', '审查入口应在底部 tab 直接可见', () => {
  assert.match(tabsShellSource, /<ion-tab-button\s+tab="reviews"/)
})

runCase('Normal Path', '审查列表到详情主链路关键路由存在', () => {
  assert.match(routerSource, /path:\s*'reviews'[\s\S]*name:\s*'RegistrationReviews'/)
  assert.match(routerSource, /path:\s*'reviews\/:formId'[\s\S]*name:\s*'RegistrationReviewDetail'/)
})

runCase('Boundary Conditions', 'ROUTE_PATHS 应定义审查详情参数化路径常量', () => {
  assert.match(guardsSource, /reviewsDetail:\s*'\/tabs\/reviews\/:formId'/)
})

runCase('Error/Negative Path', '守卫应拦截缺失 formId 的审查详情导航并回退列表', () => {
  assert.match(guardsSource, /RegistrationReviewDetail/)
  assert.match(guardsSource, /formId/)
  assert.match(guardsSource, /ROUTE_PATHS\.reviews/)
})

if (failures.length > 0) {
  console.error(`\nTotal failures: ${failures.length}`)
  for (const [index, failure] of failures.entries()) {
    console.error(`${index + 1}. [${failure.category}] ${failure.name}`)
  }
  process.exit(1)
}

console.log('\nAll assertions passed.')
