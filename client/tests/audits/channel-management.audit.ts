import { readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'
import { dirname, resolve } from 'node:path'

const auditDir = dirname(fileURLToPath(import.meta.url))
const clientRoot = resolve(auditDir, '../..')

const readClientFile = (path: string): string => {
  return readFileSync(resolve(clientRoot, path), 'utf8')
}

const fail = (message: string): never => {
  console.error(`[channel-management.audit] ${message}`)
  process.exit(1)
}

const assert = (condition: boolean, message: string): void => {
  if (!condition) {
    fail(message)
  }
}

const assertIncludes = (source: string, snippet: string, label: string): void => {
  assert(source.includes(snippet), `${label} is missing expected snippet: ${snippet}`)
}

const assertNotIncludes = (source: string, snippet: string, label: string): void => {
  assert(!source.includes(snippet), `${label} still contains removed snippet: ${snippet}`)
}

const countMatches = (source: string, pattern: RegExp): number => {
  const matches = source.match(pattern)
  return matches ? matches.length : 0
}

const mappingTab = readClientFile('src/views/channel/components/tabs/MappingTab.vue')
const roomStatusChannel = readClientFile('src/views/room-status/RoomStatusChannel.vue')
const useChannelData = readClientFile('src/views/channel/composables/useChannelData.ts')

assertIncludes(mappingTab, ':model-value="selectedHotelId"', 'MappingTab hotel selector')
assertIncludes(mappingTab, 'disabled', 'MappingTab hotel selector and write actions')
assertIncludes(mappingTab, "t('channel.mapping.localPmsOnly')", 'MappingTab PMS local note')

const writeNotReadyCount = countMatches(mappingTab, /channel\.messages\.channelWriteNotReady/g)
assert(writeNotReadyCount >= 4, 'MappingTab should guard write actions with NotReady tooltips')

const disabledWriteButtonCount = countMatches(
  mappingTab,
  /<el-button[\s\S]*?disabled[\s\S]*?@click="emit\('(save|edit|manage|disconnect)'/g,
)
assert(disabledWriteButtonCount >= 5, 'MappingTab write buttons should be disabled')

assertIncludes(mappingTab, "@click=\"emit('cancelEdit')\"", 'MappingTab cancel action')
assertNotIncludes(mappingTab, "disabled @click=\"emit('cancelEdit')\"", 'MappingTab cancel action')

assertIncludes(roomStatusChannel, "t('pages.roomStatusChannel.unavailableTitle')", 'RoomStatusChannel')
assertIncludes(
  roomStatusChannel,
  "t('pages.roomStatusChannel.unavailableDescription')",
  'RoomStatusChannel',
)
assertIncludes(
  roomStatusChannel,
  "t('pages.roomStatusChannel.unavailableActionTip')",
  'RoomStatusChannel',
)
assertIncludes(roomStatusChannel, '<el-date-picker', 'RoomStatusChannel date picker')
assertIncludes(roomStatusChannel, '<el-select', 'RoomStatusChannel room type selector')
assert(countMatches(roomStatusChannel, /\sdisabled(?:\s|>|$)/g) >= 4, 'RoomStatusChannel controls should be disabled')

for (const removedEntry of [
  'mockChannelData',
  'mockStatusLogs',
  'showEditDialog',
  'showLogsDialog',
  'ElMessage',
  'useRoomStatusStore',
]) {
  assertNotIncludes(roomStatusChannel, removedEntry, 'RoomStatusChannel')
}

assertIncludes(useChannelData, "normalized === 'BOOKING'", 'resolveSuChannelId')
assertIncludes(useChannelData, "normalized === 'BOOKING.COM'", 'resolveSuChannelId')
assertIncludes(useChannelData, "normalized === 'AIRBNB'", 'resolveSuChannelId')
assertIncludes(useChannelData, 'getSuMappings(channel.id, channelId)', 'Su mapping request')
assertIncludes(useChannelData, 'mappingsResp.data?.[channelId]', 'Su mapping response selection')
assertIncludes(useChannelData, 'mappingsResp.data[channelId] as SuMappingEntry[]', 'Su mapping payload')
assertIncludes(useChannelData, 'pickRoomTypeForRoomId(roomId, roomTypes)', 'PMS room exact match')
assertIncludes(useChannelData, 'pickPricePlanForRateId(rateId, pricePlans)', 'PMS price plan exact match')
assertIncludes(
  useChannelData,
  "const status = mappingActive && pmsRoomType && pmsPlanName ? 'connected' : 'disconnected'",
  'Mapping connected status rule',
)

for (const flattenedField of [
  'isFirstInGroup',
  'groupRowCount',
  'selectedPmsRoom',
  'selectedPmsPricePlan',
]) {
  assertIncludes(useChannelData, flattenedField, 'flattenMappingData')
}

console.log('[channel-management.audit] ok: channel static guards and data-flow sentinels present')
