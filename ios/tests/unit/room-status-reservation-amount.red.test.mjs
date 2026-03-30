import test from 'node:test'
import assert from 'node:assert/strict'

function formatAmount(rawAmount) {
  return `¥${Number(rawAmount ?? 0).toFixed(2)}`
}

function renderSummaryAmount(reservationSummaryModel) {
  return formatAmount(reservationSummaryModel.totalAmount)
}

function renderDetailAmount(reservationDetailModel) {
  return formatAmount(reservationDetailModel.totalAmount)
}

test('正常路径：同一订单在摘要与详情应显示一致金额（当前 Red）', () => {
  const reservationId = 9527

  const summaryModel = {
    id: reservationId,
    orderNumber: 'RSV-20260325-0001'
  }

  const detailModel = {
    id: reservationId,
    orderNumber: 'RSV-20260325-0001',
    totalAmount: 25000
  }

  const summaryAmountText = renderSummaryAmount(summaryModel)
  const detailAmountText = renderDetailAmount(detailModel)

  assert.equal(
    summaryAmountText,
    detailAmountText,
    `同一订单金额不一致：summary=${summaryAmountText}, detail=${detailAmountText}`
  )
})

test('边界条件：摘要金额缺失时不应被兜底成 0（当前 Red）', () => {
  const reservationId = 9528

  const summaryModel = {
    id: reservationId,
    totalAmount: undefined
  }

  const detailModel = {
    id: reservationId,
    totalAmount: 0.01
  }

  const summaryAmountText = renderSummaryAmount(summaryModel)
  const detailAmountText = renderDetailAmount(detailModel)

  assert.equal(
    summaryAmountText,
    detailAmountText,
    `边界金额不一致：summary=${summaryAmountText}, detail=${detailAmountText}`
  )
})

test('异常路径：非法金额类型应报错而不是渲染 NaN（当前 Red）', () => {
  const invalidSummaryModel = {
    id: 9529,
    totalAmount: 'NOT_A_NUMBER'
  }

  assert.throws(
    () => renderSummaryAmount(invalidSummaryModel),
    /invalid|金额|number/i,
    '非法金额类型未被拦截'
  )
})
