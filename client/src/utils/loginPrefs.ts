/**
 * 登录页偏好记忆：登录成功后持久化邮箱与勾选状态，下次进入登录页自动预填。
 * 只存展示层偏好，不存任何凭据；会话保持由后端按 rememberMe 签发的长效 token 承担。
 */
export interface LoginPrefs {
  email: string
  rememberMe: boolean
  agreeToTerms: boolean
}

const LOGIN_PREFS_STORAGE_KEY = 'loginPrefs'

export const readLoginPrefs = (): LoginPrefs | null => {
  try {
    const rawValue = localStorage.getItem(LOGIN_PREFS_STORAGE_KEY)
    if (!rawValue) {
      return null
    }

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
    localStorage.setItem(LOGIN_PREFS_STORAGE_KEY, JSON.stringify(prefs))
  } catch {
    // localStorage 不可用时静默降级，不影响登录主流程
  }
}
