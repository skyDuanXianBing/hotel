import { readFileSync } from 'node:fs'
import { dirname, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'

const auditDir = dirname(fileURLToPath(import.meta.url))
const clientRoot = resolve(auditDir, '../..')
const api = readFileSync(resolve(clientRoot, 'src/api/managedOperationSettlement.ts'), 'utf8')
const page = readFileSync(
  resolve(clientRoot, 'src/views/settings/finance/ManagedOperationSettlement.vue'),
  'utf8',
)

const assert = (condition: boolean, message: string): void => {
  if (!condition) {
    console.error(`[managed-operation-settlement.audit] ${message}`)
    process.exit(1)
  }
}

const multipartHeaderCount =
  api.match(/headers: \{ 'Content-Type': 'multipart\/form-data' \}/g)?.length ?? 0
assert(
  multipartHeaderCount === 3,
  'stamp, preview and export must all override the JSON content type',
)
assert(
  api.includes('receivedAmount: number | null'),
  'received amount must preserve excluded nulls',
)
assert(api.includes('managementFee: number | null'), 'management fee must preserve excluded nulls')
assert(api.includes('scheduledTransfer: number | null'), 'transfer must preserve excluded nulls')
assert(page.includes('settingsUnsaved.value ||'), 'export guard must include unpersisted settings')
assert(
  page.includes(':disabled="settingsUnsaved || missingFiles"'),
  'preview must require saved settings',
)
assert(
  (page.match(/formatOptionalMoney\(row\./g)?.length ?? 0) === 3,
  'all nullable line amounts must render through the optional formatter',
)

console.log(
  '[managed-operation-settlement.audit] ok: multipart, persistence and null guards present',
)
