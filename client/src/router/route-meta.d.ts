import 'vue-router'
import type { PermissionMatchMode, PermissionRequirement } from '@/stores/permission'

declare module 'vue-router' {
  interface RouteMeta {
    requiresAuth?: boolean
    title?: string
    titleKey?: string
    requiredPermissions?: PermissionRequirement[]
    permissionMatchMode?: PermissionMatchMode
    /** SaaS 权益门禁：RBAC 检查通过后按 featureCode 校验套餐权益（entitlement store，fail-open）。 */
    requiredFeatures?: string[]
    /** 工作区标记：'admin' = 平台管理端（adminToken 会话，跳过门店守卫与 RBAC）。 */
    workspace?: 'admin'
  }
}
