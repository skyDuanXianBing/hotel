import type { IndependentSiteTheme, IndependentSiteThemeKey } from '@/types/independentSite'

type Translator = (key: string, values?: Record<string, unknown>) => string

export interface IndependentSiteThemeTokens {
  primaryColor: string
  accentColor: string
  surfaceColor: string
  textColor: string
  fontBody: string
  fontHeading: string
  radius: string
  sectionSpacing: string
  imageRatio: string
  imageRadius: string
}

export const INDEPENDENT_SITE_THEME_KEYS: IndependentSiteThemeKey[] = [
  'classic',
  'modern',
  'elegant',
]

export const DEFAULT_INDEPENDENT_SITE_THEME_KEY: IndependentSiteThemeKey = 'classic'

const THEME_TOKENS: Record<IndependentSiteThemeKey, IndependentSiteThemeTokens> = {
  classic: {
    primaryColor: '#214E46',
    accentColor: '#D19A66',
    surfaceColor: '#FFFFFF',
    textColor: '#1F2A28',
    fontBody: "'Avenir Next', 'PingFang SC', 'Microsoft YaHei', sans-serif",
    fontHeading: "Georgia, 'Times New Roman', 'Songti SC', serif",
    radius: '18px',
    sectionSpacing: '72px',
    imageRatio: '4 / 3',
    imageRadius: '24px',
  },
  modern: {
    primaryColor: '#2563EB',
    accentColor: '#F59E0B',
    surfaceColor: '#FFFFFF',
    textColor: '#111827',
    fontBody: "'Inter', 'PingFang SC', 'Microsoft YaHei', sans-serif",
    fontHeading: "'Inter', 'PingFang SC', 'Microsoft YaHei', sans-serif",
    radius: '14px',
    sectionSpacing: '64px',
    imageRatio: '16 / 9',
    imageRadius: '16px',
  },
  elegant: {
    primaryColor: '#3E2F2A',
    accentColor: '#B9975B',
    surfaceColor: '#FBF7F1',
    textColor: '#2B2420',
    fontBody: "'Avenir Next', 'PingFang SC', 'Microsoft YaHei', sans-serif",
    fontHeading: "'Didot', 'Bodoni MT', Georgia, 'Songti SC', serif",
    radius: '8px',
    sectionSpacing: '88px',
    imageRatio: '3 / 2',
    imageRadius: '6px',
  },
}

export const normalizeIndependentSiteThemeKey = (value: unknown): IndependentSiteThemeKey => {
  const normalized = typeof value === 'string' ? value.trim().toLowerCase() : ''
  return (INDEPENDENT_SITE_THEME_KEYS as string[]).includes(normalized)
    ? (normalized as IndependentSiteThemeKey)
    : DEFAULT_INDEPENDENT_SITE_THEME_KEY
}

export const getIndependentSiteThemeLabel = (t: Translator, themeKey: unknown): string =>
  t(`independentSite.themes.${normalizeIndependentSiteThemeKey(themeKey)}`)

export const resolveIndependentSiteThemeTokens = (
  themeKey: unknown,
): IndependentSiteThemeTokens => THEME_TOKENS[normalizeIndependentSiteThemeKey(themeKey)]

// 主题 token 映射为 CSS 变量；页面 schema 内的 4 色作为覆盖层，有值时优先于 token
export const buildIndependentSiteCssVars = (
  themeKey: unknown,
  overrides?: Partial<IndependentSiteTheme> | null,
): Record<string, string> => {
  const tokens = resolveIndependentSiteThemeTokens(themeKey)
  return {
    '--site-primary': overrides?.primaryColor || tokens.primaryColor,
    '--site-accent': overrides?.accentColor || tokens.accentColor,
    '--site-surface': overrides?.surfaceColor || tokens.surfaceColor,
    '--site-text': overrides?.textColor || tokens.textColor,
    '--site-font-body': tokens.fontBody,
    '--site-font-heading': tokens.fontHeading,
    '--site-radius': tokens.radius,
    '--site-section-spacing': tokens.sectionSpacing,
    '--site-image-ratio': tokens.imageRatio,
    '--site-image-radius': tokens.imageRadius,
  }
}
