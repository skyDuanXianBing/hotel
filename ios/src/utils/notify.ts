export const APP_TOAST_EVENT_NAME = 'ios-app-toast'
export const DEFAULT_TOAST_DURATION = 2400

export type AppToastColor = 'primary' | 'success' | 'warning' | 'danger'

export interface AppToastEventDetail {
  message: string
  color?: AppToastColor
  duration?: number
}

export const sanitizeUserFacingMessage = (rawMessage: string) => {
  if (!rawMessage) {
    return rawMessage
  }

  return rawMessage.replace(/\bSU\b/gi, '').replace(/\s{2,}/g, ' ').trim()
}

export const emitToast = (detail: AppToastEventDetail) => {
  if (typeof window === 'undefined' || !detail.message) {
    return
  }

  window.dispatchEvent(
    new CustomEvent<AppToastEventDetail>(APP_TOAST_EVENT_NAME, {
      detail: {
        color: 'primary',
        duration: DEFAULT_TOAST_DURATION,
        ...detail,
      },
    }),
  )
}

export const showErrorToast = (message: string) => {
  emitToast({
    message: sanitizeUserFacingMessage(message),
    color: 'danger',
  })
}

export const showSuccessToast = (message: string) => {
  emitToast({
    message: sanitizeUserFacingMessage(message),
    color: 'success',
  })
}

export const showWarningToast = (message: string) => {
  emitToast({
    message: sanitizeUserFacingMessage(message),
    color: 'warning',
  })
}
