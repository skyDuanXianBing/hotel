export interface InternalTaskCapabilities {
  canCreate: boolean
  canLoadAssignees: boolean
  canLoadAllTasks: boolean
  canAssign: boolean
  canArchive: boolean
}

export function resolveInternalTaskCapabilities(
  canCreate: boolean,
  canManage: boolean
): InternalTaskCapabilities {
  return {
    canCreate,
    canLoadAssignees: canCreate || canManage,
    canLoadAllTasks: canManage,
    canAssign: canManage,
    canArchive: canManage,
  }
}
