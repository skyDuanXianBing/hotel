import 'vue-router'

export type AppTabKey = 'home' | 'rooms' | 'orders' | 'channels' | 'statistics' | 'reviews' | 'settings'

declare module 'vue-router' {
  interface RouteMeta {
    title?: string
    publicAccess?: boolean
    requiresAuth?: boolean
    requiresStore?: boolean
    publicOnly?: boolean
    requiresCleanerAuth?: boolean
    requiresCleanerStore?: boolean
    cleanerPublicOnly?: boolean
    tab?: AppTabKey
  }
}
