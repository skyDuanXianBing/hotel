import { ROUTE_PATHS } from '@/router/guards'
import type {
  CleanerContextDTO,
  CleanerDTO,
  CleanerSessionUser,
  LoginResponse,
  LoginTarget,
} from '@/types/auth'
import type { StoreDTO } from '@/types/store'
import { clearAutoLoginCredentials } from '@/utils/autoLogin'
import { clearCleanerSession, saveCleanerSession } from '@/utils/cleanerSession'
import {
  CURRENT_STORE_KEY,
  STORES_KEY,
  TOKEN_KEY,
  USER_KEY,
  clearSessionStorage,
  getStoredCurrentStore,
  writeStoredJson,
  writeStoredValue,
} from '@/utils/storage'

const LOGIN_TARGET_PMS: LoginTarget = 'PMS'
const LOGIN_TARGET_CLEANER: LoginTarget = 'CLEANER'
const INVALID_LOGIN_TARGET_MESSAGE = '登录响应缺少有效登录目标'
const UNAVAILABLE_LOGIN_TARGET_MESSAGE = '当前账号无法进入所选工作台'

interface ApplyUnifiedLoginOptions {
  resetPmsCurrentStore?: boolean
}

export interface UnifiedLoginSessionResult {
  target: LoginTarget
  redirectPath: string
}

interface ClearLoginSessionOptions {
  clearAutoLogin?: boolean
}

const isPositiveNumber = (value: unknown) => {
  return typeof value === 'number' && Number.isFinite(value) && value > 0
}

const normalizeRequiredText = (value: unknown) => {
  if (typeof value !== 'string') {
    return ''
  }

  return value.trim()
}

const isLoginTarget = (value: unknown): value is LoginTarget => {
  return value === LOGIN_TARGET_PMS || value === LOGIN_TARGET_CLEANER
}

export const normalizePreferredLoginTarget = (value: unknown): LoginTarget | undefined => {
  const normalized = String(value ?? '').trim().toUpperCase()
  return isLoginTarget(normalized) ? normalized : undefined
}

export const normalizeAvailableLoginTargets = (payload: LoginResponse): LoginTarget[] => {
  const availableTargets = Array.isArray(payload.availableLoginTargets)
    ? payload.availableLoginTargets.filter(isLoginTarget)
    : []
  const fallbackTargets = isLoginTarget(payload.loginTarget) ? [payload.loginTarget] : []

  return Array.from(new Set(availableTargets.length ? availableTargets : fallbackTargets))
}

const isMatchingCleanerContext = (context: CleanerContextDTO) => {
  return (
    isPositiveNumber(context.cleanerId) &&
    isPositiveNumber(context.storeId) &&
    context.cleaner?.id === context.cleanerId &&
    context.cleaner?.storeId === context.storeId &&
    context.cleaner?.isActive === true &&
    context.store?.id === context.storeId
  )
}

export const selectLoginTargetFromAuthorizedResponse = (
  payload: LoginResponse,
  target: LoginTarget,
): LoginResponse => {
  const availableTargets = normalizeAvailableLoginTargets(payload)

  if (!availableTargets.includes(target)) {
    throw new Error(UNAVAILABLE_LOGIN_TARGET_MESSAGE)
  }

  if (target === LOGIN_TARGET_PMS) {
    return {
      ...payload,
      loginTarget: LOGIN_TARGET_PMS,
      cleaner: null,
      currentStore: null,
      targetStoreId: null,
    }
  }

  const cleanerContext = payload.cleanerContexts?.find(isMatchingCleanerContext)

  if (!cleanerContext) {
    throw new Error('登录响应缺少可用的保洁工作台信息')
  }

  return {
    ...payload,
    loginTarget: LOGIN_TARGET_CLEANER,
    cleaner: cleanerContext.cleaner,
    currentStore: cleanerContext.store,
    targetStoreId: cleanerContext.storeId,
  }
}

const assertValidToken = (token: unknown) => {
  const normalizedToken = normalizeRequiredText(token)

  if (!normalizedToken) {
    throw new Error('登录响应缺少有效令牌')
  }

  return normalizedToken
}

const assertValidCleaner = (cleaner: CleanerDTO | null | undefined) => {
  if (!cleaner) {
    throw new Error('登录响应缺少保洁员身份信息')
  }

  if (!isPositiveNumber(cleaner.id) || !isPositiveNumber(cleaner.userId) || !isPositiveNumber(cleaner.storeId)) {
    throw new Error('登录响应中的保洁员身份信息不完整')
  }

  if (cleaner.isActive !== true) {
    throw new Error('保洁员账号未启用，请联系管理员')
  }

  if (!normalizeRequiredText(cleaner.email) || !normalizeRequiredText(cleaner.name)) {
    throw new Error('登录响应中的保洁员资料不完整')
  }

  return cleaner
}

const buildCleanerSessionUser = (cleaner: CleanerDTO): CleanerSessionUser => {
  return {
    userId: cleaner.userId,
    cleanerId: cleaner.id,
    email: cleaner.email,
    nickname: cleaner.name,
    gender: 'private',
    createdAt: cleaner.createdAt,
    updatedAt: cleaner.updatedAt,
    isCleaner: true,
  }
}

const findStoreById = (stores: StoreDTO[] | null | undefined, storeId: number) => {
  if (!Array.isArray(stores)) {
    return null
  }

  return stores.find((store) => store.id === storeId) ?? null
}

const resolveCleanerCurrentStore = (payload: LoginResponse, cleaner: CleanerDTO) => {
  const currentStore = payload.currentStore?.id ? payload.currentStore : null
  const targetStoreId = isPositiveNumber(payload.targetStoreId) ? payload.targetStoreId : null
  const resolvedStore = currentStore ?? (targetStoreId ? findStoreById(payload.stores, targetStoreId) : null)

  if (!resolvedStore?.id) {
    throw new Error('登录响应缺少当前保洁门店信息')
  }

  if (resolvedStore.id !== cleaner.storeId) {
    throw new Error('登录响应中的保洁门店与保洁员身份不一致')
  }

  if (targetStoreId && targetStoreId !== cleaner.storeId) {
    throw new Error('登录响应中的目标门店与保洁员身份不一致')
  }

  return resolvedStore
}

const applyPmsLoginResponse = (
  payload: LoginResponse,
  options: ApplyUnifiedLoginOptions,
): UnifiedLoginSessionResult => {
  const token = assertValidToken(payload.token)

  if (!payload.user) {
    throw new Error('登录响应缺少用户信息')
  }

  const stores = Array.isArray(payload.stores) ? payload.stores : []

  clearCleanerSession()
  writeStoredValue(TOKEN_KEY, token)
  writeStoredJson(USER_KEY, payload.user)
  writeStoredJson(STORES_KEY, stores)

  if (options.resetPmsCurrentStore) {
    writeStoredJson(CURRENT_STORE_KEY, null)
    return {
      target: LOGIN_TARGET_PMS,
      redirectPath: ROUTE_PATHS.storeSelection,
    }
  }

  const currentStore = getStoredCurrentStore()
  const hasMatchingCurrentStore = Boolean(
    currentStore?.id && stores.some((store) => store.id === currentStore.id),
  )

  if (!hasMatchingCurrentStore) {
    writeStoredJson(CURRENT_STORE_KEY, null)
    return {
      target: LOGIN_TARGET_PMS,
      redirectPath: ROUTE_PATHS.storeSelection,
    }
  }

  return {
    target: LOGIN_TARGET_PMS,
    redirectPath: ROUTE_PATHS.home,
  }
}

const applyCleanerLoginResponse = (payload: LoginResponse): UnifiedLoginSessionResult => {
  const token = assertValidToken(payload.token)
  const cleaner = assertValidCleaner(payload.cleaner)
  const currentStore = resolveCleanerCurrentStore(payload, cleaner)
  const cleanerUser = buildCleanerSessionUser(cleaner)

  clearSessionStorage()
  saveCleanerSession(token, cleanerUser, currentStore)

  return {
    target: LOGIN_TARGET_CLEANER,
    redirectPath: ROUTE_PATHS.cleanerDashboard,
  }
}

export const clearAllLoginSessions = (options: ClearLoginSessionOptions = {}) => {
  clearSessionStorage()
  clearCleanerSession()

  if (options.clearAutoLogin) {
    clearAutoLoginCredentials()
  }
}

export const applyUnifiedLoginResponse = (
  payload: LoginResponse,
  options: ApplyUnifiedLoginOptions = {},
): UnifiedLoginSessionResult => {
  try {
    if (payload.loginTarget === LOGIN_TARGET_PMS) {
      return applyPmsLoginResponse(payload, options)
    }

    if (payload.loginTarget === LOGIN_TARGET_CLEANER) {
      return applyCleanerLoginResponse(payload)
    }

    throw new Error(INVALID_LOGIN_TARGET_MESSAGE)
  } catch (error) {
    clearAllLoginSessions()
    throw error
  }
}
