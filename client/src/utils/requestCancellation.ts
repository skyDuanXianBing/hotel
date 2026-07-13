type CancellationLike = {
  name?: unknown
  code?: unknown
}

export const isRequestCancellationError = (error: unknown): boolean => {
  if (!error || (typeof error !== 'object' && typeof error !== 'function')) {
    return false
  }

  const candidate = error as CancellationLike
  return (
    candidate.name === 'AbortError' ||
    candidate.name === 'CanceledError' ||
    candidate.code === 'ERR_CANCELED' ||
    candidate.code === 'ABORT_ERR'
  )
}
