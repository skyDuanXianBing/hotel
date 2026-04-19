import { readStoredValue, writeStoredValue } from '@/utils/storage'

export type ThemePreference = 'system' | 'light' | 'dark'

const THEME_PREFERENCE_KEY = 'themePreference'

function isValidThemePreference(value: string): value is ThemePreference {
  return value === 'system' || value === 'light' || value === 'dark'
}

function getThemeRootElement() {
  if (typeof document === 'undefined') {
    return null
  }

  return document.documentElement
}

/** 读取用户保存的主题偏好，未设置时默认跟随系统。 */
export function getStoredThemePreference(): ThemePreference {
  const storedValue = readStoredValue(THEME_PREFERENCE_KEY)

  if (isValidThemePreference(storedValue)) {
    return storedValue
  }

  return 'system'
}

/** 将主题偏好同步到根节点并更新浏览器配色提示。 */
export function applyThemePreference(preference: ThemePreference) {
  const rootElement = getThemeRootElement()
  if (!rootElement) {
    return
  }

  if (preference === 'system') {
    rootElement.removeAttribute('data-theme')
    rootElement.style.colorScheme = 'light dark'
    return
  }

  rootElement.setAttribute('data-theme', preference)
  rootElement.style.colorScheme = preference
}

/** 保存用户选择的主题偏好。 */
export function setStoredThemePreference(preference: ThemePreference) {
  writeStoredValue(THEME_PREFERENCE_KEY, preference)
}

/** 启动时恢复并应用本地保存的主题偏好。 */
export function applyStoredThemePreference() {
  const preference = getStoredThemePreference()
  applyThemePreference(preference)
  return preference
}
