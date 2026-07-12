import type { Router } from 'vue-router'
import type { CleanerContextDTO, CleanerDTO, LoginResponse, UserDTO } from '@/api/auth'
import type { StoreDTO } from '@/api/store'
import { usePermissionStore } from '@/stores/permission'
import { useStoreStore } from '@/stores/store'
import { useUserStore } from '@/stores/user'
import {
  PMS_TOKEN_KEY,
  clearAllLocalSessions,
  clearCleanerSession,
  clearPmsSessionStorage,
  saveCleanerSession,
  saveAvailableLoginTargets,
  type CleanerSessionContext,
  type CleanerSessionStore,
  type CleanerSessionUser,
} from '@/utils/cleanerSession'

const PMS_TARGET = 'PMS'
const CLEANER_TARGET = 'CLEANER'
const STORE_SELECTION_PATH = '/store/selection'
const CLEANER_DASHBOARD_PATH = '/cleaner/dashboard'
const INCOMPLETE_LOGIN_RESPONSE_MESSAGE = '登录响应不完整，请重新登录'
const INVALID_LOGIN_TARGET_MESSAGE = '登录响应缺少有效的登录目标，请联系管理员'
const INCOMPLETE_CLEANER_RESPONSE_MESSAGE = '保洁账号信息不完整，请联系管理员检查账号配置'

export const normalizePreferredLoginTarget = (value: unknown): 'PMS' | 'CLEANER' | undefined => {
  const normalized = String(value || '').toUpperCase()
  return normalized === 'PMS' || normalized === 'CLEANER' ? normalized : undefined
}

const isNumber = (value: unknown): value is number => {
  return typeof value === 'number' && Number.isFinite(value)
}

const isString = (value: unknown): value is string => {
  return typeof value === 'string'
}

const assertPmsPayload = (token: string, user: UserDTO | null | undefined) => {
  if (!token || !user) {
    throw new Error(INCOMPLETE_LOGIN_RESPONSE_MESSAGE)
  }
}

const toCleanerSessionStore = (store: StoreDTO): CleanerSessionStore => {
  return { ...store }
}

const buildCleanerSessionUser = (cleaner: CleanerDTO | null | undefined): CleanerSessionUser => {
  if (
    !cleaner ||
    !isNumber(cleaner.id) ||
    !isNumber(cleaner.userId) ||
    !isNumber(cleaner.storeId) ||
    cleaner.isActive !== true ||
    !isString(cleaner.email) ||
    !isString(cleaner.name)
  ) {
    throw new Error(INCOMPLETE_CLEANER_RESPONSE_MESSAGE)
  }

  return {
    userId: cleaner.userId,
    cleanerId: cleaner.id,
    email: cleaner.email,
    nickname: cleaner.name || cleaner.email,
    avatar: undefined,
    gender: 'private',
    createdAt: cleaner.createdAt,
    updatedAt: cleaner.updatedAt,
    isCleaner: true,
  }
}

const resolveCleanerStore = (loginData: LoginResponse, cleaner: CleanerDTO): CleanerSessionStore => {
  const targetStoreId = isNumber(loginData.targetStoreId)
    ? loginData.targetStoreId
    : cleaner.storeId

  if (targetStoreId !== cleaner.storeId) {
    throw new Error(INCOMPLETE_CLEANER_RESPONSE_MESSAGE)
  }

  if (loginData.currentStore?.id === targetStoreId) {
    return toCleanerSessionStore(loginData.currentStore)
  }

  const storeList = Array.isArray(loginData.stores) ? loginData.stores : []
  const matchedStore = storeList.find((store: StoreDTO) => store.id === targetStoreId)
  if (matchedStore) {
    return toCleanerSessionStore(matchedStore)
  }

  throw new Error(INCOMPLETE_CLEANER_RESPONSE_MESSAGE)
}

const resolveCleanerContexts = (loginData: LoginResponse): CleanerSessionContext[] => {
  const contexts = Array.isArray(loginData.cleanerContexts) ? loginData.cleanerContexts : []
  return contexts
    .filter((context: CleanerContextDTO) => {
      return (
        isNumber(context.cleanerId) &&
        isNumber(context.storeId) &&
        context.cleaner?.id === context.cleanerId &&
        context.cleaner?.storeId === context.storeId &&
        context.cleaner?.isActive === true &&
        context.store?.id === context.storeId
      )
    })
    .map((context) => ({
      cleanerId: context.cleanerId,
      storeId: context.storeId,
      cleaner: { ...context.cleaner },
      store: toCleanerSessionStore(context.store),
    }))
}

const clearPiniaSessionState = () => {
  const userStore = useUserStore()
  const storeStore = useStoreStore()
  const permissionStore = usePermissionStore()

  userStore.setUser(null)
  storeStore.clearStoreData()
  permissionStore.clearPermissions()
}

const resolvePmsLogin = async (loginData: LoginResponse, router: Router) => {
  const stores = Array.isArray(loginData.stores) ? loginData.stores : []
  assertPmsPayload(loginData.token, loginData.user)

  const userStore = useUserStore()
  const storeStore = useStoreStore()
  const permissionStore = usePermissionStore()

  clearCleanerSession()
  clearPmsSessionStorage()
  permissionStore.clearPermissions()
  storeStore.clearStoreData()

  localStorage.setItem(PMS_TOKEN_KEY, loginData.token)
  userStore.setUser(loginData.user)
  storeStore.setStores(stores)
  saveAvailableLoginTargets(loginData.availableLoginTargets || ['PMS'])

  await router.push(STORE_SELECTION_PATH)
}

const resolveCleanerLogin = async (loginData: LoginResponse, router: Router) => {
  clearAllLocalSessions()
  clearPiniaSessionState()

  if (!loginData.token) {
    throw new Error(INCOMPLETE_LOGIN_RESPONSE_MESSAGE)
  }

  const cleaner = loginData.cleaner
  const cleanerUser = buildCleanerSessionUser(cleaner)
  const cleanerStore = resolveCleanerStore(loginData, cleaner as CleanerDTO)
  const cleanerContexts = resolveCleanerContexts(loginData)
  const normalizedContexts = cleanerContexts.length
    ? cleanerContexts
    : [{
        cleanerId: (cleaner as CleanerDTO).id,
        storeId: cleanerStore.id,
        cleaner: { ...(cleaner as CleanerDTO) },
        store: cleanerStore,
      }]

  saveCleanerSession(loginData.token, cleanerUser, cleanerStore, normalizedContexts)
  saveAvailableLoginTargets(loginData.availableLoginTargets || ['CLEANER'])
  await router.push(CLEANER_DASHBOARD_PATH)
}

export const resolveLoginTarget = async (
  loginData: LoginResponse,
  router: Router
): Promise<void> => {
  if (loginData.loginTarget === PMS_TARGET) {
    await resolvePmsLogin(loginData, router)
    return
  }

  if (loginData.loginTarget === CLEANER_TARGET) {
    await resolveCleanerLogin(loginData, router)
    return
  }

  clearAllLocalSessions()
  clearPiniaSessionState()
  throw new Error(INVALID_LOGIN_TARGET_MESSAGE)
}
