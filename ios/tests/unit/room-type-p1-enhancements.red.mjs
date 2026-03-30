import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const filePath = fileURLToPath(import.meta.url)
const currentDir = path.dirname(filePath)
const iosRoot = path.resolve(currentDir, '../..')

const guardsSource = readFileSync(path.join(iosRoot, 'src/router/guards.ts'), 'utf8')
const routerSource = readFileSync(path.join(iosRoot, 'src/router/index.ts'), 'utf8')
const roomTypeSettingsPageSource = readFileSync(
  path.join(iosRoot, 'src/views/settings/RoomTypeSettingsPage.vue'),
  'utf8'
)
const roomTypeDetailPageSource = readFileSync(
  path.join(iosRoot, 'src/views/settings/RoomTypeDetailPage.vue'),
  'utf8'
)
const roomsPageSource = readFileSync(path.join(iosRoot, 'src/views/rooms/RoomsPage.vue'), 'utf8')

const failures = []

const expectMatch = (source, regex, message) => {
  assert.equal(regex.test(source), true, message)
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

runCase('Normal Path', '详情路由常量 ROUTE_PATHS.settingsRoomTypesDetail 与注册一致', () => {
  const detailPathMatch = guardsSource.match(/settingsRoomTypesDetail:\s*'([^']+)'/)

  assert.ok(detailPathMatch, 'guards.ts 未定义 ROUTE_PATHS.settingsRoomTypesDetail')
  assert.equal(detailPathMatch[1], '/tabs/settings/room-types/:id/details')
  expectMatch(routerSource, /path:\s*'settings\/room-types\/:id\/details'/, 'router 未注册详情页路径')
  expectMatch(routerSource, /name:\s*'SettingsRoomTypeDetail'/, 'router 未注册详情页路由名')
})

runCase('Normal Path', '房型设置页存在扩展字段与价格字段编辑入口', () => {
  expectMatch(roomTypeSettingsPageSource, /入住指南/, '缺少入住指南编辑入口')
  expectMatch(roomTypeSettingsPageSource, /房型描述/, '缺少房型描述编辑入口')
  expectMatch(roomTypeSettingsPageSource, /儿童入住|maxChildOccupancy/, '缺少儿童入住编辑入口')
  expectMatch(roomTypeSettingsPageSource, /面积|sizeMeasurement/, '缺少面积字段编辑入口')
  expectMatch(roomTypeSettingsPageSource, /设施|facilities/, '缺少设施编辑入口')
  expectMatch(roomTypeSettingsPageSource, /图片|photoUrls/, '缺少图片编辑入口')
  expectMatch(roomTypeSettingsPageSource, /defaultPrice/, '缺少默认价格编辑入口')
  expectMatch(roomTypeSettingsPageSource, /mondayPrice/, '缺少周价格编辑入口')
})

runCase('Boundary Conditions', 'RoomsPage 三态空状态分支 key 存在', () => {
  expectMatch(roomsPageSource, /no-room-type/, '缺少 no-room-type 分支')
  expectMatch(roomsPageSource, /no-room/, '缺少 no-room 分支')
  expectMatch(roomsPageSource, /filtered-empty/, '缺少 filtered-empty 分支')
})

runCase('Boundary Conditions', 'RoomsPage 三态空状态文案覆盖“无房型/有房型但无房间/筛选结果为空”', () => {
  expectMatch(roomsPageSource, /无房型/, '缺少“无房型”文案')
  expectMatch(roomsPageSource, /有房型但无房间/, '缺少“有房型但无房间”文案')
  expectMatch(roomsPageSource, /筛选结果为空/, '缺少“筛选结果为空”文案')
})

runCase('Error/Negative Path', '设置页与详情页都明确“独立房间 CRUD 暂不支持”限制', () => {
  expectMatch(
    roomTypeSettingsPageSource,
    /独立房间\s*CRUD\s*暂不支持/,
    '设置页未明确“独立房间 CRUD 暂不支持”'
  )
  expectMatch(
    roomTypeDetailPageSource,
    /独立房间\s*CRUD\s*暂不支持/,
    '详情页未明确“独立房间 CRUD 暂不支持”'
  )
})

runCase('Error/Negative Path', '详情页未出现独立房间新增/编辑/删除入口承诺', () => {
  expectMatch(roomTypeDetailPageSource, /不支持独立房间新增、编辑、删除/, '详情页缺少限制性说明')
})

if (failures.length > 0) {
  console.error(`\nTotal failures: ${failures.length}`)

  for (const [index, failure] of failures.entries()) {
    console.error(`${index + 1}. [${failure.category}] ${failure.name}`)
  }

  process.exit(1)
}

console.log('\nAll assertions passed.')
