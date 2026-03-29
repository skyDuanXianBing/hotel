import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const filePath = fileURLToPath(import.meta.url)
const currentDir = path.dirname(filePath)
const iosRoot = path.resolve(currentDir, '../..')

const settingsPageSource = readFileSync(path.join(iosRoot, 'src/views/settings/SettingsPage.vue'), 'utf8')
const roomsPageSource = readFileSync(path.join(iosRoot, 'src/views/rooms/RoomsPage.vue'), 'utf8')
const guardsSource = readFileSync(path.join(iosRoot, 'src/router/guards.ts'), 'utf8')
const routerSource = readFileSync(path.join(iosRoot, 'src/router/index.ts'), 'utf8')
const roomTypeSettingsPageSource = readFileSync(
  path.join(iosRoot, 'src/views/settings/RoomTypeSettingsPage.vue'),
  'utf8'
)

const failures = []

const expectMatch = (source, regex, message) => {
  assert.equal(regex.test(source), true, message)
}

const expectNoMatch = (source, regex, message) => {
  assert.equal(regex.test(source), false, message)
}

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

runCase('Normal Path', '设置页存在“房型设置”入口并绑定 ROUTE_PATHS.settingsRoomTypes', () => {
  expectMatch(settingsPageSource, /房型设置/, 'SettingsPage 缺少“房型设置”文案')
  expectMatch(
    settingsPageSource,
    /ROUTE_PATHS\.settingsRoomTypes/,
    'SettingsPage 未绑定 ROUTE_PATHS.settingsRoomTypes'
  )
})

runCase('Normal Path', '房态空状态存在“添加房型”CTA 且跳转到 settingsRoomTypes', () => {
  expectMatch(roomsPageSource, /添加房型/, 'RoomsPage 空态缺少“添加房型”CTA')
  expectMatch(
    roomsPageSource,
    /ROUTE_PATHS\.settingsRoomTypes/,
    'RoomsPage 未引用 ROUTE_PATHS.settingsRoomTypes'
  )
  expectMatch(
    roomsPageSource,
    /router\.push\([\s\S]*ROUTE_PATHS\.settingsRoomTypes[\s\S]*\)/,
    'RoomsPage 未将 CTA 跳转至 settingsRoomTypes'
  )
})

runCase('Normal Path', '空状态不应继续只显示旧文案“当前筛选下暂无房间”', () => {
  expectNoMatch(roomsPageSource, /当前筛选下暂无房间/, 'RoomsPage 仍保留旧空态文案“当前筛选下暂无房间”')
})

runCase('Normal Path', '空状态应明确区分“无房型”与“无房间”两类主文案', () => {
  expectMatch(roomsPageSource, /无房型/, 'RoomsPage 未提供“无房型”场景文案')
  expectMatch(roomsPageSource, /无房间/, 'RoomsPage 未提供“无房间”场景文案')
})

runCase('Normal Path', 'ROUTE_PATHS.settingsRoomTypes 与 router 注册路径一致', () => {
  const constMatch = guardsSource.match(/settingsRoomTypes:\s*'([^']+)'/)

  assert.ok(constMatch, 'guards.ts 中未找到 settingsRoomTypes 常量')
  assert.equal(constMatch[1], '/tabs/settings/room-types')
  expectMatch(routerSource, /path:\s*'settings\/room-types'/, 'router 未注册 settings/room-types 路径')
  expectMatch(routerSource, /name:\s*'SettingsRoomTypes'/, 'router 未注册 SettingsRoomTypes 路由名')
  expectMatch(routerSource, /RoomTypeSettingsPage\.vue/, 'router 未绑定 RoomTypeSettingsPage 组件')
})

runCase('Boundary Conditions', '房型名称必填校验存在', () => {
  expectMatch(roomTypeSettingsPageSource, /房型名称/, '缺少房型名称字段')
  expectMatch(
    roomTypeSettingsPageSource,
    /请输入房型名称|房型名称不能为空/,
    '缺少房型名称必填校验提示'
  )
})

runCase('Boundary Conditions', '至少一个房间号与房间号非空校验存在', () => {
  expectMatch(roomTypeSettingsPageSource, /至少.*房间号/, '缺少“至少一个房间号”规则')
  expectMatch(roomTypeSettingsPageSource, /房间号不能为空/, '缺少“房间号不能为空”规则')
  expectMatch(roomTypeSettingsPageSource, /trim\(\)/, '缺少对房间号空白字符的 trim 处理')
})

runCase('Boundary Conditions', '边界约束：至少保留一个房间号输入位', () => {
  expectMatch(
    roomTypeSettingsPageSource,
    /roomNumbers\.length\s*<=\s*1/,
    '未检测到“至少保留一个房间号输入位”的长度边界保护'
  )
})

runCase('Error/Negative Path', '重复房间号会被拦截并提示错误', () => {
  expectMatch(
    roomTypeSettingsPageSource,
    /房间号.*重复|重复.*房间号|房间号不能重复/,
    '缺少重复房间号拦截错误提示'
  )
})

runCase('Error/Negative Path', '重复校验需执行规范化（trim + toUpperCase）', () => {
  expectMatch(
    roomTypeSettingsPageSource,
    /trim\(\)\.toUpperCase\(\)/,
    '未检测到重复校验中的 trim + toUpperCase 规范化处理'
  )
})

if (failures.length > 0) {
  console.error(`\nTotal failures: ${failures.length}`)

  for (const [index, failure] of failures.entries()) {
    console.error(`${index + 1}. [${failure.category}] ${failure.name}`)
  }

  process.exit(1)
}

console.log('\nAll assertions passed.')
