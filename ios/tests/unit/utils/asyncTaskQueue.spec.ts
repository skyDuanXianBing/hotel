import { describe, expect, it, vi } from 'vitest'
import { createAsyncTaskQueue } from '@/utils/asyncTaskQueue'

const flushPromises = async () => {
  for (let index = 0; index < 8; index += 1) {
    await Promise.resolve()
  }
}

const createDeferred = () => {
  let resolve!: () => void
  const promise = new Promise<void>((resolvePromise) => {
    resolve = resolvePromise
  })
  return { promise, resolve }
}

describe('createAsyncTaskQueue', () => {
  it('limits concurrency and starts queued work after a slot is released', async () => {
    const queue = createAsyncTaskQueue(2)
    const first = createDeferred()
    const second = createDeferred()
    const third = createDeferred()
    const started: string[] = []

    queue.enqueue({
      key: 'first',
      run: async () => {
        started.push('first')
        await first.promise
      },
    })
    queue.enqueue({
      key: 'second',
      run: async () => {
        started.push('second')
        await second.promise
      },
    })
    queue.enqueue({
      key: 'third',
      run: async () => {
        started.push('third')
        await third.promise
      },
    })

    await flushPromises()
    expect(started).toEqual(['first', 'second'])

    first.resolve()
    await flushPromises()
    expect(started).toEqual(['first', 'second', 'third'])

    second.resolve()
    third.resolve()
    await flushPromises()
  })

  it('deduplicates queued and active tasks by key', async () => {
    const queue = createAsyncTaskQueue(1)
    const deferred = createDeferred()
    const run = vi.fn(async () => deferred.promise)

    expect(queue.enqueue({ key: 'same', run })).toBe(true)
    expect(queue.enqueue({ key: 'same', run })).toBe(false)
    await flushPromises()
    expect(run).toHaveBeenCalledTimes(1)

    deferred.resolve()
    await flushPromises()
  })

  it('aborts active work and drops stale callbacks when cancelled', async () => {
    const queue = createAsyncTaskQueue(1)
    const onSuccess = vi.fn()
    const onError = vi.fn()
    const queuedRun = vi.fn(async () => 'queued')

    queue.enqueue({
      key: 'active',
      run: (signal) =>
        new Promise<string>((resolve, reject) => {
          signal.addEventListener(
            'abort',
            () => reject(new DOMException('Aborted', 'AbortError')),
            { once: true },
          )
          void resolve
        }),
      onSuccess,
      onError,
    })
    queue.enqueue({
      key: 'queued',
      run: queuedRun,
      onSuccess,
      onError,
    })

    await flushPromises()
    queue.cancelAll()
    await flushPromises()

    expect(queuedRun).not.toHaveBeenCalled()
    expect(onSuccess).not.toHaveBeenCalled()
    expect(onError).not.toHaveBeenCalled()
  })
})
