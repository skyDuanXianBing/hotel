import { readStoredValue, writeStoredValue } from '@/utils/storage'

/**
 * 登录页偏好记忆：登录成功后持久化邮箱与勾选状态，下次进入登录页自动预填。
 * 只存展示层偏好，不存任何凭据；会话保持由 autoLogin 凭据与长效 token 承担。
 */
export interface LoginPrefs {
  email: string
  rememberMe: boolean
  agreeToTerms: boolean
}

const LOGIN_PREFS_STORAGE_KEY = 'loginPrefs'

export const readLoginPrefs = (): LoginPrefs | null => {
  const rawValue = readStoredValue(LOGIN_PREFS_STORAGE_KEY)

  if (!rawValue) {
    return null
  }

  try {
    const parsed = JSON.parse(rawValue) as Partial<LoginPrefs>

    return {
      email: typeof parsed.email === 'string' ? parsed.email : '',
      rememberMe: parsed.rememberMe === true,
      agreeToTerms: parsed.agreeToTerms === true,
    }
  } catch {
    return null
  }
}

export const saveLoginPrefs = (prefs: LoginPrefs): void => {
  try {
    writeStoredValue(LOGIN_PREFS_STORAGE_KEY, JSON.stringify(prefs))
  } catch {
    // 存储不可用时静默降级，不影响登录主流程
  }
}
