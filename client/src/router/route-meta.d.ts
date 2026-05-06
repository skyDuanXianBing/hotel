import 'vue-router'
import type {
  PermissionMatchMode,
  PermissionRequirement,
} from '@/stores/permission'

declare module 'vue-router' {
  interface RouteMeta {
    requiresAuth?: boolean
    title?: string
    requiredPermissions?: PermissionRequirement[]
    permissionMatchMode?: PermissionMatchMode
  }
}
