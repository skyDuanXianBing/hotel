import messages from './generated/public-registration.json'
import type { SupportedLocale } from '@/locales/types'

type PublicRegistrationMessages = Record<
  SupportedLocale,
  {
    booking: Record<string, string>
    form: Record<string, string>
  }
>

export const publicRegistrationMessages = messages as PublicRegistrationMessages

export type PublicRegistrationMessageScope =
  keyof (typeof publicRegistrationMessages)['zh-CN']
