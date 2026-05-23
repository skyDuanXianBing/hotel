import type { E2ERunRecord, E2ERunStep, E2ERunStatus } from '../e2e/types'

const runs = new Map<string, E2ERunRecord>()

function cloneJson<T>(value: T): T {
  return JSON.parse(JSON.stringify(value))
}

function nowIso(): string {
  return new Date().toISOString()
}

export function saveE2ERun(run: E2ERunRecord): E2ERunRecord {
  const stored = cloneJson(run)
  runs.set(stored.runId, stored)
  return cloneJson(stored)
}

export function getE2ERun(runId: string): E2ERunRecord | null {
  const run = runs.get(runId)
  return run ? cloneJson(run) : null
}

export function updateE2ERun(
  runId: string,
  updates: Partial<Omit<E2ERunRecord, 'runId' | 'createdAt'>>,
): E2ERunRecord | null {
  const existing = runs.get(runId)
  if (!existing) {
    return null
  }

  const next: E2ERunRecord = {
    ...existing,
    ...cloneJson(updates),
    updatedAt: nowIso(),
  }
  runs.set(runId, next)
  return cloneJson(next)
}

export function appendE2ERunStep(runId: string, step: E2ERunStep): E2ERunRecord | null {
  const existing = runs.get(runId)
  if (!existing) {
    return null
  }

  const next: E2ERunRecord = {
    ...existing,
    steps: [...existing.steps, cloneJson(step)],
    updatedAt: nowIso(),
  }
  runs.set(runId, next)
  return cloneJson(next)
}

export function markE2ERunStatus(runId: string, status: E2ERunStatus): E2ERunRecord | null {
  return updateE2ERun(runId, { status })
}

export function clearE2ERunState(): void {
  runs.clear()
}
