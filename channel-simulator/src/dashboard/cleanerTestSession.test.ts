/// <reference types="node" />

import assert from 'node:assert/strict'
import test from 'node:test'

import { clearLogs, getLogs, logRequest } from '../logger'
import { normalizeCleanerTestSession } from './cleanerTestSession'

test('normalizes a valid in-memory cleaner session without exposing extra fields', () => {
  const session = normalizeCleanerTestSession({
    cleanerToken: 'temporary-cleaner-token',
    storeId: 41,
    cleanerId: 51,
    displayName: ' Local E2E Cleaner ',
    password: 'must-not-survive',
  })
  assert.deepEqual(session, {
    cleanerToken: 'temporary-cleaner-token',
    storeId: 41,
    cleanerId: 51,
    displayName: 'Local E2E Cleaner',
  })
})

test('rejects incomplete cleaner sessions', () => {
  assert.equal(normalizeCleanerTestSession({ storeId: 41, cleanerId: 51 }), null)
  assert.equal(normalizeCleanerTestSession({
    cleanerToken: 'token', storeId: 0, cleanerId: 51, displayName: 'Cleaner',
  }), null)
})

test('simulator logs redact cleaner tokens from bootstrap responses', () => {
  clearLogs()
  logRequest('MOCK_API', 'POST', '/api/e2e/bootstrap', null, {
    success: true,
    data: {
      cleanerSession: {
        cleanerToken: 'temporary-cleaner-token',
        displayName: 'Local E2E Cleaner',
      },
    },
  }, 200)
  const response = getLogs()[0]?.responseBody as {
    data?: { cleanerSession?: { cleanerToken?: string; displayName?: string } }
  }
  assert.equal(response.data?.cleanerSession?.cleanerToken, '[redacted]')
  assert.equal(response.data?.cleanerSession?.displayName, 'Local E2E Cleaner')
})
