/// <reference types="vite/client" />

import '@vue/runtime-core'
import type { ComposerTranslation } from 'vue-i18n'

interface ImportMetaEnv {
  readonly VITE_API_BASE_URL?: string
  readonly VITE_LOCAL_API_BASE_URL?: string
  readonly VITE_CLOUD_API_BASE_URL?: string
  readonly VITE_USE_CLOUD_API?: string
  readonly VITE_USE_MESSAGE_MOCK?: string
  readonly VITE_ALLOW_SU_WIDGET_LOCAL?: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}

declare module '@vue/runtime-core' {
  interface ComponentCustomProperties {
    $t: ComposerTranslation
  }
}

export {}
