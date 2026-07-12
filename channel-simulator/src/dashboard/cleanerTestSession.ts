import type { CleanerTestSession } from './types'

export function normalizeCleanerTestSession(value: unknown): CleanerTestSession | null {
  if (!value || typeof value !== 'object') {
    return null
  }
  const candidate = value as Partial<CleanerTestSession>
  if (
    typeof candidate.cleanerToken !== 'string' || !candidate.cleanerToken.trim() ||
    typeof candidate.storeId !== 'number' || !Number.isFinite(candidate.storeId) || candidate.storeId <= 0 ||
    typeof candidate.cleanerId !== 'number' || !Number.isFinite(candidate.cleanerId) || candidate.cleanerId <= 0 ||
    typeof candidate.displayName !== 'string' || !candidate.displayName.trim()
  ) {
    return null
  }
  return {
    cleanerToken: candidate.cleanerToken,
    storeId: candidate.storeId,
    cleanerId: candidate.cleanerId,
    displayName: candidate.displayName.trim(),
  }
}
