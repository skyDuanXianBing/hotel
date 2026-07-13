import { isRequestCancellationError } from '../../utils/requestCancellation'

export type CalendarDateRange = readonly [string, string]

export interface CalendarWindowContext {
  userId: number
  storeId: number
  timezone: string
  permissionContext: string
}

interface CacheEntry<T> {
  value: T
  expiresAt: number
}

interface FailureState {
  attempts: number
  retryAt: number
}

export class CalendarWindowBackoffError extends Error {
  constructor(public readonly retryAt: number) {
    super(`Calendar window retry is delayed until ${retryAt}`)
    this.name = 'CalendarWindowBackoffError'
  }
}

const assertYmd = (value: string): [number, number, number] => {
  const match = /^(\d{4})-(\d{2})-(\d{2})$/.exec(value)
  if (!match) throw new Error(`Invalid YMD date: ${value}`)
  return [Number(match[1]), Number(match[2]), Number(match[3])]
}

export const shiftCalendarYmd = (value: string, days: number): string => {
  const [year, month, day] = assertYmd(value)
  const shifted = new Date(Date.UTC(year, month - 1, day + days))
  return [
    shifted.getUTCFullYear(),
    String(shifted.getUTCMonth() + 1).padStart(2, '0'),
    String(shifted.getUTCDate()).padStart(2, '0'),
  ].join('-')
}

export const shiftCalendarWindow = (
  range: CalendarDateRange,
  days: number,
): [string, string] => [shiftCalendarYmd(range[0], days), shiftCalendarYmd(range[1], days)]

const encodeKeyPart = (value: string | number) => encodeURIComponent(String(value))

export const buildCalendarWindowKey = (
  context: CalendarWindowContext,
  range: CalendarDateRange,
): string =>
  [
    context.userId,
    context.storeId,
    context.timezone.trim(),
    context.permissionContext,
    range[0],
    range[1],
  ]
    .map(encodeKeyPart)
    .join('|')

export interface CalendarWindowCacheOptions {
  maxEntries?: number
  ttlMs?: number
  baseBackoffMs?: number
  maxBackoffMs?: number
  jitter?: () => number
}

export class CalendarWindowCache<T> {
  private readonly entries = new Map<string, CacheEntry<T>>()
  private readonly pending = new Map<string, Promise<T>>()
  private readonly failures = new Map<string, FailureState>()
  private readonly maxEntries: number
  private readonly ttlMs: number
  private readonly baseBackoffMs: number
  private readonly maxBackoffMs: number
  private readonly jitter: () => number

  constructor(options: CalendarWindowCacheOptions = {}) {
    this.maxEntries = Math.max(1, options.maxEntries ?? 3)
    this.ttlMs = Math.max(1, options.ttlMs ?? 90_000)
    this.baseBackoffMs = Math.max(1, options.baseBackoffMs ?? 5_000)
    this.maxBackoffMs = Math.max(this.baseBackoffMs, options.maxBackoffMs ?? 60_000)
    this.jitter = options.jitter ?? Math.random
  }

  get(key: string, now = Date.now()): T | undefined {
    const entry = this.entries.get(key)
    if (!entry) return undefined
    if (entry.expiresAt < now) {
      this.entries.delete(key)
      return undefined
    }
    this.entries.delete(key)
    this.entries.set(key, entry)
    return entry.value
  }

  set(key: string, value: T, now = Date.now()): void {
    this.entries.delete(key)
    this.entries.set(key, { value, expiresAt: now + this.ttlMs })
    this.failures.delete(key)
    while (this.entries.size > this.maxEntries) {
      const oldestKey = this.entries.keys().next().value
      if (oldestKey == null) break
      this.entries.delete(oldestKey)
    }
  }

  getPending(key: string): Promise<T> | undefined {
    return this.pending.get(key)
  }

  canRequest(key: string, now = Date.now()): boolean {
    return (this.failures.get(key)?.retryAt ?? 0) <= now
  }

  retryAt(key: string): number | undefined {
    return this.failures.get(key)?.retryAt
  }

  load(
    key: string,
    loader: () => Promise<T>,
    options: { force?: boolean; now?: number } = {},
  ): Promise<T> {
    const pending = this.pending.get(key)
    if (pending) return pending

    const now = options.now ?? Date.now()
    const retryAt = this.failures.get(key)?.retryAt ?? 0
    if (!options.force && retryAt > now) {
      return Promise.reject(new CalendarWindowBackoffError(retryAt))
    }

    const request = loader()
      .then((value) => {
        this.set(key, value)
        return value
      })
      .catch((error) => {
        if (isRequestCancellationError(error)) {
          throw error
        }

        const attempts = (this.failures.get(key)?.attempts ?? 0) + 1
        const baseDelay = Math.min(
          this.maxBackoffMs,
          this.baseBackoffMs * 2 ** Math.max(0, attempts - 1),
        )
        const jitterFactor = 0.9 + Math.min(1, Math.max(0, this.jitter())) * 0.2
        this.failures.set(key, {
          attempts,
          retryAt: Date.now() + Math.round(baseDelay * jitterFactor),
        })
        throw error
      })
      .finally(() => {
        if (this.pending.get(key) === request) this.pending.delete(key)
      })
    this.pending.set(key, request)
    return request
  }

  clear(): void {
    this.entries.clear()
    this.failures.clear()
  }

  get size(): number {
    return this.entries.size
  }
}

export interface IdleWindowSchedulerOptions {
  delayMs?: number
  timeoutMs?: number
}

export const scheduleCalendarWindowIdleTask = (
  task: () => void,
  options: IdleWindowSchedulerOptions = {},
): (() => void) => {
  let cancelled = false
  let frameId: number | null = null
  let timerId: number | null = null
  let idleId: number | null = null
  const delayMs = options.delayMs ?? 400
  const timeoutMs = options.timeoutMs ?? 1_200

  const run = () => {
    if (!cancelled) task()
  }
  const scheduleIdle = () => {
    if (cancelled) return
    const requestIdle = window.requestIdleCallback
    if (typeof requestIdle === 'function') {
      idleId = requestIdle(run, { timeout: timeoutMs })
      return
    }
    timerId = window.setTimeout(run, 150)
  }

  Promise.resolve().then(() => {
    if (cancelled) return
    frameId = window.requestAnimationFrame(() => {
      frameId = null
      timerId = window.setTimeout(scheduleIdle, delayMs)
    })
  })

  return () => {
    cancelled = true
    if (frameId != null) window.cancelAnimationFrame(frameId)
    if (timerId != null) window.clearTimeout(timerId)
    if (idleId != null && typeof window.cancelIdleCallback === 'function') {
      window.cancelIdleCallback(idleId)
    }
  }
}
