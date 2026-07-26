import type { IndependentSiteSectionType } from '@/types/independentSite'

type Translator = (key: string, values?: Record<string, unknown>) => string

export const getIndependentSiteSectionTypeLabel = (
  t: Translator,
  type: IndependentSiteSectionType,
): string => t(`independentSite.sectionTypes.${type}`)

export const INDEPENDENT_SITE_ADDABLE_SECTION_TYPES: IndependentSiteSectionType[] = [
  'HERO',
  'ABOUT',
  'HIGHLIGHTS',
  'AMENITIES',
  'LOCATION',
  'HOUSE_RULES',
  'GALLERY',
  'BOOKING',
]

export const INDEPENDENT_SITE_IMAGE_MAX_BYTES = 5 * 1024 * 1024
