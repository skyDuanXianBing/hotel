/// <reference types="vite/client" />

interface ImportMetaEnv {
  readonly VITE_API_BASE_URL?: string
  readonly VITE_LOCAL_API_BASE_URL?: string
  readonly VITE_CLOUD_API_BASE_URL?: string
  readonly VITE_USE_CLOUD_API?: string
  readonly VITE_ALLOW_SU_WIDGET_LOCAL?: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}
