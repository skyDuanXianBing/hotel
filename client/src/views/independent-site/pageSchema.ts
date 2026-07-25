import {
  INDEPENDENT_SITE_SCHEMA_VERSION,
  type IndependentSiteCornerStyle,
  type IndependentSiteGalleryImage,
  type IndependentSitePageSchema,
  type IndependentSitePageSection,
  type IndependentSiteSectionType,
  type IndependentSiteTheme,
  type IndependentSiteTypography,
} from '@/types/independentSite'

type Translator = (key: string, values?: Record<string, unknown>) => string

const MAX_SECTIONS = 8
const MAX_LIST_ITEMS = 12
const MAX_GALLERY_IMAGES = 12
const MAX_IMAGE_URL_LENGTH = 1500

export const INDEPENDENT_SITE_MAX_SECTIONS = MAX_SECTIONS
export const INDEPENDENT_SITE_MAX_LIST_ITEMS = MAX_LIST_ITEMS
export const INDEPENDENT_SITE_MAX_GALLERY_IMAGES = MAX_GALLERY_IMAGES
const SECTION_TYPES = new Set<IndependentSiteSectionType>([
  'HERO',
  'ABOUT',
  'HIGHLIGHTS',
  'AMENITIES',
  'LOCATION',
  'HOUSE_RULES',
  'GALLERY',
  'BOOKING',
])
const ITEM_SECTION_TYPES = new Set<IndependentSiteSectionType>([
  'HIGHLIGHTS',
  'AMENITIES',
  'HOUSE_RULES',
])
const IMAGE_URL_SECTION_TYPES = new Set<IndependentSiteSectionType>(['HERO', 'ABOUT'])
const SECTION_ID_PATTERN = /^[A-Za-z0-9-]{1,40}$/
const TYPOGRAPHIES = new Set<IndependentSiteTypography>(['MODERN', 'CLASSIC', 'FRIENDLY'])
const CORNER_STYLES = new Set<IndependentSiteCornerStyle>(['SOFT', 'SQUARE', 'PILL'])

export const DEFAULT_INDEPENDENT_SITE_THEME: IndependentSiteTheme = {
  primaryColor: '#214E46',
  accentColor: '#D19A66',
  surfaceColor: '#FFFFFF',
  textColor: '#1F2A28',
  typography: 'MODERN',
  cornerStyle: 'SOFT',
}

export const createEmptyIndependentSiteSchema = (t: Translator): IndependentSitePageSchema => ({
  schemaVersion: INDEPENDENT_SITE_SCHEMA_VERSION,
  theme: { ...DEFAULT_INDEPENDENT_SITE_THEME },
  sections: [
    {
      id: 's-hero',
      type: 'HERO',
      title: t('independentSite.defaults.heroTitle'),
      body: t('independentSite.defaults.heroBody'),
      alignment: 'CENTER',
    },
  ],
})

// 编辑器“添加区块”时的默认内容；id 必须稳定且符合白名单格式
export const createIndependentSiteSection = (
  type: IndependentSiteSectionType,
  t: Translator,
): IndependentSitePageSection => {
  const base = {
    id: generateSectionId(),
    type,
    alignment: 'LEFT' as const,
  }
  switch (type) {
    case 'HERO':
      return {
        ...base,
        title: t('independentSite.defaults.heroTitle'),
        body: t('independentSite.defaults.heroBody'),
        alignment: 'CENTER',
      }
    case 'ABOUT':
      return { ...base, title: t('independentSite.sectionTypes.ABOUT'), body: '' }
    case 'HIGHLIGHTS':
      return {
        ...base,
        title: t('independentSite.defaults.highlights'),
        items: [
          t('independentSite.defaults.convenientLocation'),
          t('independentSite.defaults.cleanComfortable'),
        ],
      }
    case 'AMENITIES':
      return {
        ...base,
        title: t('independentSite.sectionTypes.AMENITIES'),
        items: [t('independentSite.defaults.freeWifi')],
      }
    case 'LOCATION':
      return { ...base, title: t('independentSite.sectionTypes.LOCATION'), body: '' }
    case 'HOUSE_RULES':
      return {
        ...base,
        title: t('independentSite.sectionTypes.HOUSE_RULES'),
        items: [t('independentSite.defaults.checkInRule')],
      }
    case 'GALLERY':
      return { ...base, title: t('independentSite.sectionTypes.GALLERY'), images: [] }
    case 'BOOKING':
      return {
        ...base,
        title: t('independentSite.defaults.bookNow'),
        body: t('independentSite.defaults.bookBody'),
      }
    default:
      return { ...base, title: t('independentSite.defaults.newSection') }
  }
}

const isRecord = (value: unknown): value is Record<string, unknown> =>
  Boolean(value) && typeof value === 'object' && !Array.isArray(value)

const safeText = (value: unknown, maxLength: number): string => {
  if (typeof value !== 'string') {
    return ''
  }
  return value.trim().slice(0, maxLength)
}

const safeColor = (value: unknown, fallback: string): string => {
  if (typeof value !== 'string') {
    return fallback
  }
  const normalized = value.trim()
  return /^#[0-9a-fA-F]{6}$/.test(normalized) ? normalized.toUpperCase() : fallback
}

const normalizeTheme = (value: unknown): IndependentSiteTheme => {
  const theme = isRecord(value) ? value : {}
  const typography = safeText(theme.typography, 20).toUpperCase() as IndependentSiteTypography
  const cornerStyle = safeText(theme.cornerStyle, 20).toUpperCase() as IndependentSiteCornerStyle
  return {
    primaryColor: safeColor(theme.primaryColor, DEFAULT_INDEPENDENT_SITE_THEME.primaryColor),
    accentColor: safeColor(theme.accentColor, DEFAULT_INDEPENDENT_SITE_THEME.accentColor),
    surfaceColor: safeColor(theme.surfaceColor, DEFAULT_INDEPENDENT_SITE_THEME.surfaceColor),
    textColor: safeColor(theme.textColor, DEFAULT_INDEPENDENT_SITE_THEME.textColor),
    typography: TYPOGRAPHIES.has(typography)
      ? typography
      : DEFAULT_INDEPENDENT_SITE_THEME.typography,
    cornerStyle: CORNER_STYLES.has(cornerStyle)
      ? cornerStyle
      : DEFAULT_INDEPENDENT_SITE_THEME.cornerStyle,
  }
}

const generateSectionId = (): string =>
  `s-${Math.random().toString(36).slice(2, 6)}${Date.now().toString(36).slice(-4)}`

const normalizeSectionId = (value: unknown): string => {
  const normalized = safeText(value, 40)
  return SECTION_ID_PATTERN.test(normalized) ? normalized : generateSectionId()
}

const normalizeGalleryImages = (value: unknown): IndependentSiteGalleryImage[] | undefined => {
  if (!Array.isArray(value)) {
    return undefined
  }
  const images: IndependentSiteGalleryImage[] = []
  for (const item of value.slice(0, MAX_GALLERY_IMAGES)) {
    if (!isRecord(item)) {
      continue
    }
    const url = safeIndependentSiteImageUrl(item.url)
    if (!url) {
      continue
    }
    const alt = safeText(item.alt, 100) || undefined
    images.push(alt ? { url, alt } : { url })
  }
  return images.length ? images : undefined
}

const normalizeSection = (rawValue: unknown): IndependentSitePageSection | null => {
  if (!isRecord(rawValue)) {
    return null
  }

  const type = safeText(rawValue.type, 32).toUpperCase() as IndependentSiteSectionType
  if (!SECTION_TYPES.has(type)) {
    return null
  }
  const title = safeText(rawValue.title, 120)
  if (!title) {
    return null
  }
  const body = safeText(rawValue.body, 600) || undefined
  const alignment = safeText(rawValue.alignment, 16).toUpperCase() === 'CENTER' ? 'CENTER' : 'LEFT'
  const items =
    ITEM_SECTION_TYPES.has(type) && Array.isArray(rawValue.items)
      ? rawValue.items
          .slice(0, MAX_LIST_ITEMS)
          .map((item) => safeText(item, 100))
          .filter(Boolean)
      : undefined
  const imageUrl = IMAGE_URL_SECTION_TYPES.has(type)
    ? safeIndependentSiteImageUrl(rawValue.imageUrl) || undefined
    : undefined
  const images = type === 'GALLERY' ? normalizeGalleryImages(rawValue.images) : undefined
  if (type === 'GALLERY' && !images) {
    return null
  }

  return {
    id: normalizeSectionId(rawValue.id),
    type,
    title,
    body,
    items: items?.length ? items : undefined,
    imageUrl,
    images,
    alignment,
  }
}

export const normalizeIndependentSiteSchema = (
  value: unknown,
): IndependentSitePageSchema | null => {
  if (
    !isRecord(value) ||
    value.schemaVersion !== INDEPENDENT_SITE_SCHEMA_VERSION ||
    !Array.isArray(value.sections)
  ) {
    return null
  }

  const sections = value.sections
    .slice(0, MAX_SECTIONS)
    .map(normalizeSection)
    .filter((section): section is IndependentSitePageSection => Boolean(section))
  if (!sections.some((section) => section.type === 'HERO')) {
    return null
  }
  // 后端约束同一页面内 section 类型不可重复（BOOKING 因而每页至多 1 个），保留首个同类型区块
  const seenTypes = new Set<IndependentSiteSectionType>()
  const uniqueSections = sections.filter((section) => {
    if (seenTypes.has(section.type)) {
      return false
    }
    seenTypes.add(section.type)
    return true
  })

  return {
    schemaVersion: INDEPENDENT_SITE_SCHEMA_VERSION,
    theme: normalizeTheme(value.theme),
    sections: uniqueSections,
  }
}

export const safeIndependentSiteImageUrl = (value: unknown): string => {
  if (typeof value !== 'string') {
    return ''
  }
  const normalized = value.trim()
  if (!normalized || normalized.length > MAX_IMAGE_URL_LENGTH) {
    return ''
  }
  if (normalized.startsWith('/') && !normalized.startsWith('//')) {
    return normalized
  }
  try {
    const parsed = new URL(normalized)
    return parsed.protocol === 'https:' || parsed.protocol === 'http:' ? parsed.toString() : ''
  } catch {
    return ''
  }
}
