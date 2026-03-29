import test from 'node:test'
import assert from 'node:assert/strict'
import fs from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const __filename = fileURLToPath(import.meta.url)
const __dirname = path.dirname(__filename)
const repoRoot = path.resolve(__dirname, '../../..')

const SOURCE_FILES = {
  api: path.join(repoRoot, 'ios/src/api/roomStatus.ts'),
  store: path.join(repoRoot, 'ios/src/stores/roomStatus.ts'),
  summaryCard: path.join(repoRoot, 'ios/src/components/room-status/ReservationSummaryCard.vue')
}

function readSource(filePath) {
  return fs.readFileSync(filePath, 'utf8')
}

function normalizeAmount(rawAmount) {
  if (rawAmount === null || rawAmount === undefined) {
    return undefined
  }

  if (typeof rawAmount !== 'number' || Number.isNaN(rawAmount) || !Number.isFinite(rawAmount)) {
    throw new TypeError(`invalid amount value: ${String(rawAmount)}`)
  }

  return rawAmount
}

function mapReservationInfoToSummaryModel(reservationInfo) {
  return {
    id: reservationInfo.id,
    orderNumber: reservationInfo.orderNumber,
    totalAmount: normalizeAmount(reservationInfo.totalAmount)
  }
}

function formatAmount(rawAmount) {
  const amount = normalizeAmount(rawAmount)
  return `¥${Number(amount ?? 0).toFixed(2)}`
}

function renderSummaryAmount(reservationSummaryModel) {
  return formatAmount(reservationSummaryModel.totalAmount)
}

function renderDetailAmount(reservationDetailModel) {
  return formatAmount(reservationDetailModel.totalAmount)
}

test('正常路径：同一订单摘要与详情金额一致（Green）', () => {
  const apiSource = readSource(SOURCE_FILES.api)
  const storeSource = readSource(SOURCE_FILES.store)
  const summaryCardSource = readSource(SOURCE_FILES.summaryCard)

  assert.match(
    apiSource,
    /interface\s+ReservationInfoDTO[\s\S]*totalAmount\??\s*:\s*number/,
    '房态 API 类型缺少 totalAmount 字段，无法承接金额'
  )
  assert.equal(/reservation\.totalAmount/.test(storeSource), true, 'store 未消费房态金额字段')
  assert.match(summaryCardSource, /reservation\.totalAmount/, '摘要卡未消费 reservation.totalAmount')

  const reservationId = 9527
  const calendarReservationInfo = {
    id: reservationId,
    orderNumber: 'RSV-20260325-0001',
    totalAmount: 25000
  }
  const reservationDetail = {
    id: reservationId,
    orderNumber: 'RSV-20260325-0001',
    totalAmount: 25000
  }

  const summaryModel = mapReservationInfoToSummaryModel(calendarReservationInfo)
  const summaryAmountText = renderSummaryAmount(summaryModel)
  const detailAmountText = renderDetailAmount(reservationDetail)

  assert.equal(summaryAmountText, '¥25000.00')
  assert.equal(summaryAmountText, detailAmountText)
})

test('边界条件：最小/零金额在摘要与详情链路保持一致（Green）', () => {
  const minValueReservation = {
    id: 9528,
    orderNumber: 'RSV-20260325-0002',
    totalAmount: 0.01
  }
  const zeroValueReservation = {
    id: 9529,
    orderNumber: 'RSV-20260325-0003',
    totalAmount: 0
  }

  const minSummaryAmountText = renderSummaryAmount(
    mapReservationInfoToSummaryModel(minValueReservation)
  )
  const zeroSummaryAmountText = renderSummaryAmount(
    mapReservationInfoToSummaryModel(zeroValueReservation)
  )

  assert.equal(minSummaryAmountText, '¥0.01')
  assert.equal(minSummaryAmountText, renderDetailAmount(minValueReservation))
  assert.equal(zeroSummaryAmountText, '¥0.00')
  assert.equal(zeroSummaryAmountText, renderDetailAmount(zeroValueReservation))
})

test('异常路径：非法金额应被拦截并可在实现中定位防护（Green）', () => {
  const storeSource = readSource(SOURCE_FILES.store)
  const summaryCardSource = readSource(SOURCE_FILES.summaryCard)

  const hasRuntimeGuard =
    /throw\s+new\s+(Error|TypeError)\(/.test(storeSource) ||
    /throw\s+new\s+(Error|TypeError)\(/.test(summaryCardSource)

  const hasNumericValidation =
    /Number\.is(Finite|NaN)\(/.test(storeSource) ||
    /typeof\s+\w+\s*!==\s*'number'/.test(storeSource) ||
    /Number\.is(Finite|NaN)\(/.test(summaryCardSource) ||
    /typeof\s+\w+\s*!==\s*'number'/.test(summaryCardSource)

  assert.equal(hasRuntimeGuard, true, '实现中缺少非法金额拦截逻辑')
  assert.equal(hasNumericValidation, true, '实现中缺少金额类型校验逻辑')

  assert.throws(
    () =>
      mapReservationInfoToSummaryModel({
        id: 9530,
        orderNumber: 'RSV-20260325-0004',
        totalAmount: 'NOT_A_NUMBER'
      }),
    /invalid amount/i,
    '等价映射逻辑未拦截非法金额'
  )
})
