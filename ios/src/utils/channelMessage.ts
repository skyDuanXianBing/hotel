const RAW_MAPPING_RESPONSE_SHAPE_MESSAGE = 'Unexpected mappings response shape'

export const MAPPINGS_RESPONSE_SHAPE_NOTICE = '渠道映射数据暂时无法识别，请稍后刷新重试或重新进入授权流程'

export function sanitizeChannelWarningMessage(rawMessage: string, fallbackMessage = '') {
  if (!rawMessage) {
    return fallbackMessage
  }

  const trimmedMessage = rawMessage.trim()
  if (!trimmedMessage) {
    return fallbackMessage
  }

  return trimmedMessage.replaceAll(
    RAW_MAPPING_RESPONSE_SHAPE_MESSAGE,
    MAPPINGS_RESPONSE_SHAPE_NOTICE,
  )
}

export function resolveChannelWarningMessage(error: unknown, fallbackMessage: string) {
  if (error instanceof Error && error.message) {
    return sanitizeChannelWarningMessage(error.message, fallbackMessage)
  }

  if (typeof error === 'string') {
    return sanitizeChannelWarningMessage(error, fallbackMessage)
  }

  return fallbackMessage
}

export function resolveMappingStatusNotice(errorMessage?: string | null) {
  return sanitizeChannelWarningMessage(errorMessage || '', '')
}
