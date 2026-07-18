export interface AsyncTaskQueueTask<T> {
  key: string
  priority?: number
  run: (signal: AbortSignal) => Promise<T>
  onSuccess?: (result: T) => void
  onError?: (error: unknown) => void
}

export interface AsyncTaskQueue {
  enqueue<T>(task: AsyncTaskQueueTask<T>): boolean
  has(key: string): boolean
  cancelAll(): void
}

interface QueuedTask extends AsyncTaskQueueTask<unknown> {
  generation: number
  sequence: number
  scopedKey: string
}

export function isAbortError(error: unknown) {
  return error instanceof DOMException && error.name === 'AbortError'
}

export function createAsyncTaskQueue(concurrency = 2): AsyncTaskQueue {
  const maxConcurrency = Math.max(1, Math.floor(concurrency))
  const queuedTasks: QueuedTask[] = []
  const queuedKeys = new Set<string>()
  const activeKeys = new Set<string>()
  const activeControllers = new Set<AbortController>()

  let generation = 0
  let sequence = 0
  let activeCount = 0

  const buildScopedKey = (key: string, targetGeneration = generation) =>
    `${targetGeneration}:${key}`

  const pump = () => {
    while (activeCount < maxConcurrency && queuedTasks.length > 0) {
      const task = queuedTasks.shift()
      if (!task) {
        return
      }

      queuedKeys.delete(task.scopedKey)
      if (task.generation !== generation) {
        continue
      }

      const controller = new AbortController()
      activeControllers.add(controller)
      activeKeys.add(task.scopedKey)
      activeCount += 1

      void Promise.resolve()
        .then(() => task.run(controller.signal))
        .then((result) => {
          if (task.generation === generation && !controller.signal.aborted) {
            task.onSuccess?.(result)
          }
        })
        .catch((error) => {
          if (
            task.generation === generation &&
            !controller.signal.aborted &&
            !isAbortError(error)
          ) {
            task.onError?.(error)
          }
        })
        .finally(() => {
          activeControllers.delete(controller)
          activeKeys.delete(task.scopedKey)
          activeCount -= 1
          pump()
        })
    }
  }

  return {
    enqueue<T>(task: AsyncTaskQueueTask<T>) {
      const scopedKey = buildScopedKey(task.key)
      if (queuedKeys.has(scopedKey) || activeKeys.has(scopedKey)) {
        return false
      }

      queuedKeys.add(scopedKey)
      queuedTasks.push({
        key: task.key,
        priority: task.priority,
        run: task.run as (signal: AbortSignal) => Promise<unknown>,
        onSuccess: task.onSuccess
          ? (result) => task.onSuccess?.(result as T)
          : undefined,
        onError: task.onError,
        generation,
        sequence: sequence++,
        scopedKey,
      })
      queuedTasks.sort((first, second) => {
        const priorityDifference = (second.priority ?? 0) - (first.priority ?? 0)
        return priorityDifference || first.sequence - second.sequence
      })
      pump()
      return true
    },

    has(key: string) {
      const scopedKey = buildScopedKey(key)
      return queuedKeys.has(scopedKey) || activeKeys.has(scopedKey)
    },

    cancelAll() {
      generation += 1
      queuedTasks.length = 0
      queuedKeys.clear()
      for (const controller of activeControllers) {
        controller.abort()
      }
    },
  }
}
