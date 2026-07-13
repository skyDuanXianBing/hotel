import { describe, expect, test } from 'bun:test'
import { CanceledError } from 'axios'
import { isRequestCancellationError } from '../src/utils/requestCancellation'

describe('isRequestCancellationError', () => {
  test.each([
    ['DOM AbortError', new DOMException('aborted', 'AbortError')],
    ['plain AbortError', { name: 'AbortError' }],
    ['Axios CanceledError', new CanceledError('canceled')],
    ['plain CanceledError', { name: 'CanceledError' }],
    ['Axios cancellation code', { code: 'ERR_CANCELED' }],
    ['DOM cancellation code', { code: 'ABORT_ERR' }],
  ])('recognizes %s', (_label, error) => {
    expect(isRequestCancellationError(error)).toBe(true)
  })

  test.each([
    ['ordinary Error', new Error('failed')],
    ['timeout', { name: 'AxiosError', code: 'ECONNABORTED', message: 'timeout' }],
    ['HTTP error', { name: 'AxiosError', response: { status: 500 } }],
    ['null', null],
    ['string', 'canceled'],
  ])('rejects %s', (_label, error) => {
    expect(isRequestCancellationError(error)).toBe(false)
  })
})
