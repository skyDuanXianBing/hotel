import { request } from '@/utils/request'
import { publicRequest } from '@/utils/publicRequest'
import type {
  IndependentSiteAiEditRequest,
  IndependentSiteAiGenerateRequest,
  IndependentSiteAiGenerateResult,
  IndependentSiteCheckoutRequest,
  IndependentSiteConfig,
  IndependentSiteConfigUpdateRequest,
  IndependentSiteCreateRequest,
  IndependentSiteDetail,
  IndependentSitePageCreateRequest,
  IndependentSitePageDetail,
  IndependentSitePageDraftPublishRequest,
  IndependentSitePageDraftSaveRequest,
  IndependentSitePageDraftState,
  IndependentSitePageImportRequest,
  IndependentSitePageSummary,
  IndependentSitePageUpdateRequest,
  IndependentSitePaymentAttempt,
  IndependentSitePaymentIntent,
  IndependentSiteQuote,
  IndependentSiteQuoteRequest,
  IndependentSiteRoomPagesGenerateResult,
  IndependentSiteStripeSettings,
  IndependentSiteStripeSettingsUpdateRequest,
  IndependentSiteSummary,
  IndependentSiteUpdateRequest,
  PublicIndependentSite,
  PublicIndependentSitePage,
} from '@/types/independentSite'

export interface IndependentSiteApiResponse<T> {
  success: boolean
  message?: string
  data: T
}

const MANAGEMENT_SITES_PATH = '/independent-sites'
const MANAGEMENT_CURRENT_PATH = '/independent-sites/current'
const PUBLIC_SITE_PATH = '/public/independent-sites'

// ------------------------------------------------------------------
// 站点 CRUD（一店多站）
// ------------------------------------------------------------------

export const listIndependentSites = (): Promise<
  IndependentSiteApiResponse<IndependentSiteSummary[]>
> =>
  request.get(MANAGEMENT_SITES_PATH, {
    suppressErrorToast: true,
  })

export const createIndependentSite = (
  data: IndependentSiteCreateRequest,
): Promise<IndependentSiteApiResponse<IndependentSiteDetail>> =>
  request.post(MANAGEMENT_SITES_PATH, data, {
    suppressErrorToast: true,
  })

export const getIndependentSite = (
  siteId: number,
): Promise<IndependentSiteApiResponse<IndependentSiteDetail>> =>
  request.get(`${MANAGEMENT_SITES_PATH}/${siteId}`, {
    suppressErrorToast: true,
  })

export const updateIndependentSite = (
  siteId: number,
  data: IndependentSiteUpdateRequest,
): Promise<IndependentSiteApiResponse<IndependentSiteDetail>> =>
  request.put(`${MANAGEMENT_SITES_PATH}/${siteId}`, data, {
    suppressErrorToast: true,
  })

export const deleteIndependentSite = (
  siteId: number,
): Promise<IndependentSiteApiResponse<null>> =>
  request.delete(`${MANAGEMENT_SITES_PATH}/${siteId}`, {
    suppressErrorToast: true,
  })

// ------------------------------------------------------------------
// 门店级 Stripe 设置（一店一套密钥，该店所有站点共享）
// ------------------------------------------------------------------

export const getIndependentSiteStripeSettings = (): Promise<
  IndependentSiteApiResponse<IndependentSiteStripeSettings>
> =>
  request.get(`${MANAGEMENT_SITES_PATH}/stripe-settings`, {
    suppressErrorToast: true,
  })

export const updateIndependentSiteStripeSettings = (
  data: IndependentSiteStripeSettingsUpdateRequest,
): Promise<IndependentSiteApiResponse<IndependentSiteStripeSettings>> =>
  request.put(`${MANAGEMENT_SITES_PATH}/stripe-settings`, data, {
    suppressErrorToast: true,
  })

// ------------------------------------------------------------------
// 页面 CRUD 与草稿/发布/AI 生成（一站多页面）
// ------------------------------------------------------------------

export const listIndependentSitePages = (
  siteId: number,
): Promise<IndependentSiteApiResponse<IndependentSitePageSummary[]>> =>
  request.get(`${MANAGEMENT_SITES_PATH}/${siteId}/pages`, {
    suppressErrorToast: true,
  })

export const createIndependentSitePage = (
  siteId: number,
  data: IndependentSitePageCreateRequest,
): Promise<IndependentSiteApiResponse<IndependentSitePageDetail>> =>
  request.post(`${MANAGEMENT_SITES_PATH}/${siteId}/pages`, data, {
    suppressErrorToast: true,
  })

export const deleteIndependentSitePage = (
  siteId: number,
  pageId: number,
): Promise<IndependentSiteApiResponse<null>> =>
  request.delete(`${MANAGEMENT_SITES_PATH}/${siteId}/pages/${pageId}`, {
    suppressErrorToast: true,
  })

export const generateIndependentSiteRoomPages = (
  siteId: number,
): Promise<IndependentSiteApiResponse<IndependentSiteRoomPagesGenerateResult>> =>
  request.post(`${MANAGEMENT_SITES_PATH}/${siteId}/pages/generate-room-pages`, undefined, {
    suppressErrorToast: true,
    timeout: 120000,
  })

export const getIndependentSitePage = (
  siteId: number,
  pageId: number,
): Promise<IndependentSiteApiResponse<IndependentSitePageDetail>> =>
  request.get(`${MANAGEMENT_SITES_PATH}/${siteId}/pages/${pageId}`, {
    suppressErrorToast: true,
  })

export const updateIndependentSitePage = (
  siteId: number,
  pageId: number,
  data: IndependentSitePageUpdateRequest,
): Promise<IndependentSiteApiResponse<IndependentSitePageDetail>> =>
  request.put(`${MANAGEMENT_SITES_PATH}/${siteId}/pages/${pageId}`, data, {
    suppressErrorToast: true,
  })

export const publishIndependentSitePage = (
  siteId: number,
  pageId: number,
  data: IndependentSitePageDraftPublishRequest,
): Promise<IndependentSiteApiResponse<IndependentSitePageDetail>> =>
  request.post(`${MANAGEMENT_SITES_PATH}/${siteId}/pages/${pageId}/publish`, data, {
    suppressErrorToast: true,
  })

export const aiEditIndependentSitePage = (
  siteId: number,
  pageId: number,
  data: IndependentSiteAiEditRequest,
): Promise<IndependentSiteApiResponse<IndependentSitePageDetail>> =>
  request.post(`${MANAGEMENT_SITES_PATH}/${siteId}/pages/${pageId}/ai-edit`, data, {
    suppressErrorToast: true,
    timeout: 120000,
  })

export const undoIndependentSitePageAiEdit = (
  siteId: number,
  pageId: number,
): Promise<IndependentSiteApiResponse<IndependentSitePageDetail>> =>
  request.post(`${MANAGEMENT_SITES_PATH}/${siteId}/pages/${pageId}/ai-edit/undo`, undefined, {
    suppressErrorToast: true,
  })

export const generateIndependentSitePageDraftForPage = (
  siteId: number,
  pageId: number,
  data: IndependentSiteAiGenerateRequest,
): Promise<IndependentSiteApiResponse<IndependentSiteAiGenerateResult>> =>
  request.post(`${MANAGEMENT_SITES_PATH}/${siteId}/pages/${pageId}/generate`, data, {
    suppressErrorToast: true,
    timeout: 120000,
  })

// 抓取 + AI 映射较慢，沿用 AI 类端点的 120s 超时
export const importIndependentSitePageFromUrl = (
  siteId: number,
  data: IndependentSitePageImportRequest,
): Promise<IndependentSiteApiResponse<IndependentSitePageDetail>> =>
  request.post(`${MANAGEMENT_SITES_PATH}/${siteId}/pages/import-url`, data, {
    suppressErrorToast: true,
    timeout: 120000,
  })

export const getPublicIndependentSitePage = (
  slug: string,
  path: string,
): Promise<IndependentSiteApiResponse<PublicIndependentSitePage>> => {
  const tail = path
    .split('/')
    .filter(Boolean)
    .map((segment) => encodeURIComponent(segment))
    .join('/')
  return publicRequest.get(`${PUBLIC_SITE_PATH}/${encodeURIComponent(slug)}/pages/${tail}`)
}

export const getCurrentIndependentSite = (): Promise<
  IndependentSiteApiResponse<IndependentSiteConfig | null>
> =>
  request.get(MANAGEMENT_CURRENT_PATH, {
    suppressErrorToast: true,
  })

export const updateCurrentIndependentSite = (
  data: IndependentSiteConfigUpdateRequest,
): Promise<IndependentSiteApiResponse<IndependentSiteConfig>> =>
  request.put(MANAGEMENT_CURRENT_PATH, data, {
    suppressErrorToast: true,
  })

export const generateIndependentSitePageDraft = (
  siteId: number,
  data: IndependentSiteAiGenerateRequest,
): Promise<IndependentSiteApiResponse<IndependentSiteAiGenerateResult>> =>
  request.post(`/independent-sites/${siteId}/page-drafts/generate`, data, {
    suppressErrorToast: true,
    timeout: 120000,
  })

export const saveIndependentSitePageDraft = (
  data: IndependentSitePageDraftSaveRequest,
): Promise<IndependentSiteApiResponse<IndependentSitePageDraftState>> =>
  request.put(`${MANAGEMENT_CURRENT_PATH}/page-draft`, data, {
    suppressErrorToast: true,
  })

export const publishIndependentSitePageDraft = (
  data: IndependentSitePageDraftPublishRequest,
): Promise<IndependentSiteApiResponse<IndependentSiteConfig>> =>
  request.post(`${MANAGEMENT_CURRENT_PATH}/page-draft/publish`, data, {
    suppressErrorToast: true,
  })

export const getPublicIndependentSite = (
  slug: string,
): Promise<IndependentSiteApiResponse<PublicIndependentSite>> =>
  publicRequest.get(`${PUBLIC_SITE_PATH}/${encodeURIComponent(slug)}`)

export const getPublicIndependentSiteQuote = (
  slug: string,
  data: IndependentSiteQuoteRequest,
): Promise<IndependentSiteApiResponse<IndependentSiteQuote>> =>
  publicRequest.post(`${PUBLIC_SITE_PATH}/${encodeURIComponent(slug)}/quotes`, data)

export const createIndependentSitePreviewHold = (
  slug: string,
  data: IndependentSiteCheckoutRequest,
): Promise<IndependentSiteApiResponse<IndependentSitePaymentAttempt>> =>
  request.post(`/independent-sites/${encodeURIComponent(slug)}/preview-holds`, data, {
    suppressErrorToast: true,
  })

export const simulateIndependentSitePreviewPayment = (
  paymentAttemptId: string,
): Promise<IndependentSiteApiResponse<IndependentSitePaymentAttempt>> =>
  request.post(
    `${MANAGEMENT_CURRENT_PATH}/payments/${encodeURIComponent(paymentAttemptId)}/simulate`,
    undefined,
    {
      suppressErrorToast: true,
    },
  )

export const createPublicIndependentSiteHold = (
  slug: string,
  data: IndependentSiteCheckoutRequest,
): Promise<IndependentSiteApiResponse<IndependentSitePaymentAttempt>> =>
  publicRequest.post(`${PUBLIC_SITE_PATH}/${encodeURIComponent(slug)}/holds`, data)

export const confirmPublicIndependentSitePayment = (
  slug: string,
  paymentAttemptId: string,
): Promise<IndependentSiteApiResponse<IndependentSitePaymentAttempt>> =>
  publicRequest.post(
    `${PUBLIC_SITE_PATH}/${encodeURIComponent(slug)}/payments/${encodeURIComponent(paymentAttemptId)}/confirm`,
  )

// 创建/复用 Stripe PaymentIntent；attempt 非 PENDING 时响应不含 clientSecret
export const createPublicIndependentSitePaymentIntent = (
  slug: string,
  paymentAttemptId: string,
): Promise<IndependentSiteApiResponse<IndependentSitePaymentIntent>> =>
  publicRequest.post(
    `${PUBLIC_SITE_PATH}/${encodeURIComponent(slug)}/payments/${encodeURIComponent(paymentAttemptId)}/intent`,
    undefined,
    {
      timeout: 60000,
    },
  )

export const getPublicIndependentSitePayment = (
  slug: string,
  paymentAttemptId: string,
): Promise<IndependentSiteApiResponse<IndependentSitePaymentAttempt>> =>
  publicRequest.get(
    `${PUBLIC_SITE_PATH}/${encodeURIComponent(slug)}/payments/${encodeURIComponent(paymentAttemptId)}`,
  )
