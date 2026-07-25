import type { IndependentSiteSectionType } from '@/types/independentSite'

export const INDEPENDENT_SITE_SECTION_TYPE_LABELS: Record<IndependentSiteSectionType, string> = {
  HERO: '首屏横幅',
  ABOUT: '关于我们',
  HIGHLIGHTS: '住宿亮点',
  AMENITIES: '设施与服务',
  LOCATION: '位置与交通',
  HOUSE_RULES: '入住须知',
  GALLERY: '图片墙',
  BOOKING: '订房入口',
}

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
