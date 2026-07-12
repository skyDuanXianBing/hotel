import assert from 'node:assert/strict'
import test from 'node:test'

import { redactForOutput, redactSensitiveText } from './outputRedaction'

const JWT = 'eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxMjMifQ.signature_value'

test('redacts nested token and cleanerToken fields', () => {
  assert.deepEqual(redactForOutput({
    summary: {
      token: JWT,
      nested: { cleanerToken: 'cleaner-secret', storeId: 41 },
    },
  }), {
    summary: {
      token: '[redacted]',
      nested: { cleanerToken: '[redacted]', storeId: 41 },
    },
  })
})

test('redacts authorization, standalone JWT, and named test-support keys in text', () => {
  const text = `request Authorization=Bearer ${JWT}; jwt=${JWT}; X-Test-Support-Key: local-e2e-key; {"token":"opaque-token"}`
  const redacted = redactSensitiveText(text)
  assert.equal(redacted.includes(JWT), false)
  assert.equal(redacted.includes('local-e2e-key'), false)
  assert.equal(redacted.includes('opaque-token'), false)
  assert.match(redacted, /Bearer \[redacted\]/)
  assert.match(redacted, /X-Test-Support-Key: \[redacted\]/)
})

test('preserves ordinary business text and non-sensitive identifiers', () => {
  const value = {
    message: 'Guest asked whether a physical room key token is required.',
    displayName: 'Local E2E Cleaner',
    storeId: 41,
    scenario: 'booking lifecycle push',
  }
  assert.deepEqual(redactForOutput(value), value)
})
