import { showWarningToast, sanitizeUserFacingMessage } from '@/utils/notify'
import { isHandledRequestError } from '@/utils/request'

export const resolveRequestErrorMessage = (error: unknown, fallbackMessage: string) => {
  if (error instanceof Error && error.message) {
    return sanitizeUserFacingMessage(error.message)
  }

  return sanitizeUserFacingMessage(fallbackMessage)
}

export const showUnhandledRequestWarning = (error: unknown, fallbackMessage: string) => {
  const message = resolveRequestErrorMessage(error, fallbackMessage)

  if (!isHandledRequestError(error)) {
    showWarningToast(message)
  }

  return message
}
