export const INDEPENDENT_SITE_SCHEMA_VERSION = 'independent_site_page_v1' as const

export type IndependentSitePaymentStatus = 'PENDING' | 'SUCCEEDED' | 'FAILED' | 'EXPIRED'
export type IndependentSiteTypography = 'MODERN' | 'CLASSIC' | 'FRIENDLY'
export type IndependentSiteCornerStyle = 'SOFT' | 'SQUARE' | 'PILL'
export type IndependentSiteThemeKey = 'classic' | 'modern' | 'elegant'
export type IndependentSitePageType = 'HOME' | 'ROOM_DETAIL' | 'CUSTOM'
// 页面格式：BLOCKS（旧区块 schema）/ CANVAS（自由画布节点树）；后端未返回时一律按 BLOCKS 兜底
export type IndependentSitePageFormat = 'BLOCKS' | 'CANVAS'
export type IndependentSiteSectionType =
  | 'HERO'
  | 'ABOUT'
  | 'HIGHLIGHTS'
  | 'AMENITIES'
  | 'LOCATION'
  | 'HOUSE_RULES'
  | 'GALLERY'
  | 'BOOKING'

export interface IndependentSiteTheme {
  primaryColor: string
  accentColor: string
  surfaceColor: string
  textColor: string
  typography: IndependentSiteTypography
  cornerStyle: IndependentSiteCornerStyle
}

export interface IndependentSiteGalleryImage {
  url: string
  alt?: string
}

export interface IndependentSitePageSection {
  id?: string
  type: IndependentSiteSectionType
  title: string
  body?: string
  items?: string[]
  imageUrl?: string
  images?: IndependentSiteGalleryImage[]
  alignment: 'LEFT' | 'CENTER'
}

export interface IndependentSitePageSchema {
  schemaVersion: typeof INDEPENDENT_SITE_SCHEMA_VERSION
  theme: IndependentSiteTheme
  sections: IndependentSitePageSection[]
}

export interface IndependentSiteRoomOption {
  id: number
  roomNumber: string
  status?: string
}

export interface IndependentSiteRoomTypeOption {
  id: number
  name: string
  code?: string
  description?: string
  maxGuests?: number
  rooms: IndependentSiteRoomOption[]
}

export interface IndependentSiteConfig {
  id: number | null
  slug: string
  enabled: boolean
  publicPath?: string
  channelId?: number | null
  channelCode?: string | null
  defaultPricePlanId: number | null
  defaultPricePlanName?: string | null
  priceAdjustmentType: 'PERCENTAGE'
  priceAdjustmentValue: number
  paymentProvider?: 'SIMULATED' | 'STRIPE' | string | null
  simulatedPaymentEnabled: boolean
  publishedRoomTypeIds: number[]
  publishedRoomIds: number[]
  publishedPageSchema: IndependentSitePageSchema | null
  draftPageSchema: IndependentSitePageSchema | null
  draftUpdatedAt?: string | null
  draftVersion?: number | null
  publishedAt?: string | null
  version?: number | null
}

export interface IndependentSiteConfigUpdateRequest {
  slug: string
  enabled: boolean
  defaultPricePlanId: number
  priceAdjustmentValue: number
  simulatedPaymentEnabled: boolean
  publishedRoomTypeIds: number[]
  publishedRoomIds: number[]
}

export interface IndependentSiteAiGenerateRequest {
  prompt: string
  language?: string
  style?: 'MODERN' | 'CLASSIC' | 'FRIENDLY' | 'MINIMAL'
}

export interface IndependentSiteAiGenerateResult {
  providerStatus: string
  schemaVersion: string
  publishable: boolean
  // schema 结构随页面 format 变化（BLOCKS 区块 schema / CANVAS 节点树），消费前必须经对应 normalize
  pageSchema: unknown
  warnings?: string[]
}

export interface IndependentSitePageDraftSaveRequest {
  pageSchema: IndependentSitePageSchema
  expectedDraftVersion?: number
}

export interface IndependentSitePageDraftState {
  siteId: number
  schemaVersion: string
  pageSchema: IndependentSitePageSchema
  updatedAt: string
  draftVersion: number
}

export interface IndependentSitePageDraftPublishRequest {
  draftVersion: number
}

export interface IndependentSiteSummary {
  id: number
  name: string
  slug: string
  enabled: boolean
  themeKey: string
  paymentProvider?: string | null
  publicPath?: string
  pageCount: number
  publicationCount: number
  isDefault: boolean
  publishedAt?: string | null
  updatedAt?: string | null
}

export interface IndependentSitePageSummary {
  id: number
  path: string
  type: IndependentSitePageType | string
  title: string
  enabled: boolean
  sortOrder: number
  roomTypeId?: number | null
  format?: IndependentSitePageFormat
  draftUpdatedAt?: string | null
  publishedAt?: string | null
  hasUnpublishedChanges: boolean
}

export interface IndependentSiteDetail {
  id: number
  name: string
  slug: string
  enabled: boolean
  themeKey: string
  publicPath?: string
  channelId?: number | null
  channelCode?: string | null
  defaultPricePlanId: number | null
  defaultPricePlanName?: string | null
  priceAdjustmentType: 'PERCENTAGE'
  priceAdjustmentValue: number
  paymentProvider?: 'SIMULATED' | 'STRIPE' | string | null
  simulatedPaymentEnabled: boolean
  publishedRoomTypeIds: number[]
  publishedRoomIds: number[]
  publishedAt?: string | null
  version?: number | null
  pages: IndependentSitePageSummary[]
  // 门店 Stripe 密钥是否已配齐（管理端控制 STRIPE 渠道是否可选）
  stripeAvailable?: boolean
}

// GET/PUT /independent-sites/stripe-settings 响应（门店级；sk/whsec 只回配置状态与尾 4 位，不回明文）
export interface IndependentSiteStripeSettings {
  configured: boolean
  publishableKey: string | null
  secretKeyConfigured: boolean
  secretKeyLast4: string | null
  webhookSecretConfigured: boolean
  webhookSecretLast4: string | null
}

// PUT /independent-sites/stripe-settings 请求体：缺省或空串表示保持不变
export interface IndependentSiteStripeSettingsUpdateRequest {
  publishableKey?: string
  secretKey?: string
  webhookSecret?: string
}

export interface IndependentSiteCreateRequest {
  name: string
  slug: string
  themeKey?: IndependentSiteThemeKey
}

export interface IndependentSiteUpdateRequest {
  name?: string
  slug: string
  enabled: boolean
  themeKey?: IndependentSiteThemeKey
  defaultPricePlanId: number
  priceAdjustmentValue: number
  paymentProvider?: string
  simulatedPaymentEnabled: boolean
  publishedRoomTypeIds: number[]
  publishedRoomIds: number[]
}

export interface IndependentSitePageDetail {
  id: number
  siteId: number
  path: string
  type: IndependentSitePageType | string
  title: string
  seoDescription?: string | null
  roomTypeId?: number | null
  enabled: boolean
  sortOrder: number
  format?: IndependentSitePageFormat
  // schema 结构随 format 变化（BLOCKS 区块 schema / CANVAS 节点树），消费前必须经对应 normalize
  draftSchema: unknown
  publishedSchema: unknown
  draftVersion?: number | null
  draftUpdatedAt?: string | null
  publishedAt?: string | null
  hasAiBackup?: boolean
}

export interface IndependentSiteAiEditRequest {
  instruction: string
}

export type IndependentSitePageImportMode = 'NEW_PAGE' | 'OVERWRITE_DRAFT'

export interface IndependentSitePageImportRequest {
  url: string
  mode: IndependentSitePageImportMode
  pageId?: number
  path?: string
  title?: string
}

export interface IndependentSitePageUpdateRequest {
  title?: string
  seoDescription?: string
  path?: string
  enabled?: boolean
  sortOrder?: number
  // draftSchema 结构随页面 format 变化（BLOCKS 区块 schema / CANVAS 节点树）
  draftSchema?: unknown
  expectedDraftVersion?: number
}

export interface IndependentSitePageCreateRequest {
  path: string
  title: string
  type?: IndependentSitePageType | string
  seoDescription?: string
  sortOrder?: number
}

export interface IndependentSiteRoomPagesGenerateSkippedItem {
  roomTypeId: number
  reason: string
}

export interface IndependentSiteRoomPagesGenerateResult {
  generated: number
  refreshed: number
  skipped: IndependentSiteRoomPagesGenerateSkippedItem[]
  pages: IndependentSitePageSummary[]
}

export interface PublicIndependentSiteFacility {
  group?: string
  name: string
}

export interface PublicIndependentSiteRoomType {
  id: number
  name: string
  code?: string
  description?: string
  maxGuests?: number
  maxChildren?: number
  size?: number
  sizeUnit?: string
  desktopPhotoUrls?: string[]
  mobilePhotoUrls?: string[]
  facilities?: PublicIndependentSiteFacility[]
}

export interface PublicIndependentSiteRoom {
  id: number
  roomTypeId: number
  roomNumber: string
}

export interface PublicIndependentSitePageNavItem {
  path: string
  title: string
  type: IndependentSitePageType | string
  roomTypeId?: number | null
}

export interface PublicIndependentSitePage {
  path: string
  title: string
  seoDescription?: string | null
  type: IndependentSitePageType | string
  roomTypeId?: number | null
  // 缺省按 BLOCKS 处理
  format?: IndependentSitePageFormat
  // schema 结构随 format 变化（BLOCKS 区块 schema / CANVAS 节点树），消费前必须经对应 normalize
  schema: unknown
}

export interface PublicIndependentSite {
  slug: string
  name: string
  description?: string
  logo?: string
  address?: string
  city?: string
  state?: string
  country?: string
  currency: string
  // HOME 页格式，缺省按 BLOCKS 处理
  format?: IndependentSitePageFormat
  // HOME 页 schema，结构随 format 变化（BLOCKS 区块 schema / CANVAS 节点树），消费前必须经对应 normalize
  pageSchema: unknown
  roomTypes: PublicIndependentSiteRoomType[]
  rooms: PublicIndependentSiteRoom[]
  paymentProvider?: string
  simulatedPaymentEnabled: boolean
  paymentNotice?: string
  themeKey?: string
  pages?: PublicIndependentSitePageNavItem[]
}

export interface IndependentSiteQuoteRequest {
  roomTypeId: number
  checkInDate: string
  checkOutDate: string
  rooms: number
  adults: number
  children: number
}

export interface IndependentSiteNightlyRate {
  date: string
  baseRoomPrice: number
  adjustedRoomPrice: number
  extraGuestAmount: number
  nightTotal: number
}

export interface IndependentSiteQuote {
  slug: string
  roomTypeId: number
  roomTypeName: string
  checkInDate: string
  checkOutDate: string
  rooms: number
  adults: number
  children: number
  availableRooms: number
  currency: string
  adjustmentPercent: number
  nightlyRates: IndependentSiteNightlyRate[]
  totalAmount: number
  quotedAt?: string
  expiresAt?: string
}

export interface IndependentSiteGuestDetails {
  name: string
  email: string
  phone: string
  specialRequests?: string
}

export interface IndependentSiteCheckoutRequest {
  idempotencyKey: string
  roomTypeId: number
  checkInDate: string
  checkOutDate: string
  rooms: number
  adults: number
  children: number
  guest: IndependentSiteGuestDetails
}

export interface IndependentSitePaymentAttempt {
  paymentAttemptId: string
  status: IndependentSitePaymentStatus
  amount: number
  currency: string
  expiresAt?: string
  completedAt?: string
  groupOrderNo?: string
  reservationOrderNumbers?: string[]
  failureReason?: string
  simulated: boolean
  // hold 创建后由后端按站点配置返回
  provider?: 'SIMULATED' | 'STRIPE'
}

// POST /public/independent-sites/{slug}/payments/{id}/intent 响应；
// attempt 非 PENDING（已过期/已成功等）时不返回 clientSecret/publishableKey
export interface IndependentSitePaymentIntent {
  status: IndependentSitePaymentStatus | string
  clientSecret?: string
  publishableKey?: string
}
