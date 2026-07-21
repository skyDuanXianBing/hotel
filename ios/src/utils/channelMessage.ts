import { i18n } from '@/locales'

const RAW_MAPPING_RESPONSE_SHAPE_MESSAGE = 'Unexpected mappings response shape'

const getMappingsResponseShapeNotice = () =>
  i18n.global.t('runtime.channel.mappingsResponseShapeNotice')

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
    getMappingsResponseShapeNotice(),
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
