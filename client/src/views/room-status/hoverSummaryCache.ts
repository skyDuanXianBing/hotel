export interface HoverSummaryCacheEntry<T> {
  value: T
  fetchedAt: number
  generation: number
}

export const normalizeReservationIds = (ids: Iterable<number>): number[] => {
  const unique = new Set<number>()
  for (const rawId of ids) {
    const id = Number(rawId)
    if (Number.isSafeInteger(id) && id > 0) {
      unique.add(id)
    }
  }
  return Array.from(unique)
}

export const chunkReservationIds = (ids: Iterable<number>, size = 200): number[][] => {
  if (!Number.isSafeInteger(size) || size <= 0) {
    throw new Error('chunk size must be a positive integer')
  }
  const normalized = normalizeReservationIds(ids)
  const chunks: number[][] = []
  for (let index = 0; index < normalized.length; index += size) {
    chunks.push(normalized.slice(index, index + size))
  }
  return chunks
}

export const runWithConcurrency = async <T>(
  tasks: Array<() => Promise<T>>,
  concurrency = 2,
): Promise<PromiseSettledResult<T>[]> => {
  if (!Number.isSafeInteger(concurrency) || concurrency <= 0) {
    throw new Error('concurrency must be a positive integer')
  }
  const results: PromiseSettledResult<T>[] = new Array(tasks.length)
  let nextIndex = 0
  const worker = async () => {
    while (nextIndex < tasks.length) {
      const index = nextIndex++
      try {
        results[index] = { status: 'fulfilled', value: await tasks[index]() }
      } catch (reason) {
        results[index] = { status: 'rejected', reason }
      }
    }
  }
  await Promise.all(
    Array.from({ length: Math.min(concurrency, tasks.length) }, () => worker()),
  )
  return results
}

export const didAllHoverSummaryBatchesSucceed = (
  results: PromiseSettledResult<boolean>[],
): boolean =>
  results.length > 0 &&
  results.every((result) => result.status === 'fulfilled' && result.value === true)

export class HoverSummaryCache<T> {
  private readonly entries = new Map<string, HoverSummaryCacheEntry<T>>()

  constructor(private readonly ttlMs = 60_000) {}

  static key(storeId: number, reservationId: number): string {
    return `${storeId}:${reservationId}`
  }

  get(storeId: number, reservationId: number, generation: number, now = Date.now()): T | undefined {
    const key = HoverSummaryCache.key(storeId, reservationId)
    const entry = this.entries.get(key)
    if (!entry || entry.generation !== generation) {
      return undefined
    }
    if (now - entry.fetchedAt > this.ttlMs) {
      this.entries.delete(key)
      return undefined
    }
    return entry.value
  }

  set(storeId: number, reservationId: number, generation: number, value: T, now = Date.now()) {
    this.entries.set(HoverSummaryCache.key(storeId, reservationId), {
      value,
      fetchedAt: now,
      generation,
    })
  }

  delete(storeId: number, reservationId: number) {
    this.entries.delete(HoverSummaryCache.key(storeId, reservationId))
  }

  clear() {
    this.entries.clear()
  }
}

export class HoverSummaryRefreshGate {
  private generation = 0
  private lastAttemptAt: number | null = null
  private lastSuccessAt: number | null = null

  constructor(
    private readonly ttlMs = 60_000,
    private readonly retryIntervalMs = 5_000,
  ) {}

  reset(generation: number) {
    this.generation = generation
    this.lastAttemptAt = null
    this.lastSuccessAt = null
  }

  canSchedule(generation: number, now = Date.now()) {
    if (generation !== this.generation) return false
    if (this.lastAttemptAt == null) return true
    if (now - this.lastAttemptAt < this.retryIntervalMs) return false
    return this.lastSuccessAt == null || now - this.lastSuccessAt > this.ttlMs
  }

  markScheduled(generation: number, now = Date.now()) {
    if (generation === this.generation) this.lastAttemptAt = now
  }

  markSuccess(generation: number, now = Date.now()) {
    if (generation === this.generation) this.lastSuccessAt = now
  }
}
