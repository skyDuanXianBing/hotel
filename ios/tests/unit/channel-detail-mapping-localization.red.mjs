import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const filePath = fileURLToPath(import.meta.url)
const currentDir = path.dirname(filePath)
const iosRoot = path.resolve(currentDir, '../..')

const channelDetailPageSource = readFileSync(
  path.join(iosRoot, 'src/views/channel/ChannelDetailPage.vue'),
  'utf8'
)
const channelMappingPageSource = readFileSync(
  path.join(iosRoot, 'src/views/channel/ChannelMappingPage.vue'),
  'utf8'
)
const channelsPageSource = readFileSync(path.join(iosRoot, 'src/views/channels/ChannelsPage.vue'), 'utf8')

const failures = []

const expectMatch = (source, regex, message) => {
  assert.equal(regex.test(source), true, message)
}

const expectNotMatch = (source, regex, message) => {
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

runCase('Normal Path', 'Booking.com 详情价格比例匹配不应使用 otaId===channelId', () => {
  expectNotMatch(
    channelDetailPageSource,
    /item\.channelId\s*===\s*otaId\.value/,
    '仍在使用 otaId 与 channelId 直接比较，Booking.com 详情可能误命中 Airbnb'
  )
})

runCase('Boundary Conditions', '详情页应存在按渠道业务标识匹配的归一化逻辑', () => {
  expectMatch(
    channelDetailPageSource,
    /(normalizeChannelCode|channelCode\s*\.\s*toUpperCase\(|code\s*\.\s*toUpperCase\()/,
    '缺少基于渠道标识(code/channelCode)的大小写归一化匹配，边界场景不可靠'
  )
})

runCase('Error/Negative Path', '映射页不应直接渲染 mappingStatus.error 原始英文诊断', () => {
  expectNotMatch(
    channelMappingPageSource,
    /mappingStatus\?\.error/,
    '映射页仍直接显示 mappingStatus.error，可能把英文诊断文案暴露给用户'
  )
})

runCase('Error/Negative Path', '渠道首页告警链路不应直接将 loadNotice 透传给 Toast', () => {
  expectNotMatch(
    channelsPageSource,
    /showWarningToast\(loadNotice\.value\)/,
    '渠道首页仍直接透传 loadNotice 到 Toast，英文诊断文案会原样暴露'
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
