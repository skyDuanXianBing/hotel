export type ThemePreference = 'system' | 'light' | 'dark'

function getThemeRootElement() {
  if (typeof document === 'undefined') {
    return null
  }

  return document.documentElement
}

/** Force the app to use the single supported light theme. */
export function applyLightTheme() {
  const rootElement = getThemeRootElement()
  if (!rootElement) {
    return
  }

  rootElement.setAttribute('data-theme', 'light')
  rootElement.style.colorScheme = 'light'
}

export function getStoredThemePreference(): ThemePreference {
  return 'light'
}

export function applyThemePreference(_preference: ThemePreference) {
  applyLightTheme()
}

export function setStoredThemePreference(_preference: ThemePreference) {
  // Theme switching has been removed. Keep the no-op for compatibility.
}

export function applyStoredThemePreference() {
  applyLightTheme()
  return 'light' as const
}
