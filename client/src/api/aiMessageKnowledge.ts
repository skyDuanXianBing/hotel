import { request } from '@/utils/request'

const KNOWLEDGE_ITEMS_URL = '/su-messaging/knowledge-items'
const DEFAULT_SUPPRESS_ERROR_TOAST = true

export type AiMessageKnowledgeStatus =
  | 'CANDIDATE'
  | 'ACTIVE'
  | 'CONFLICTED'
  | 'ARCHIVED'
  | 'REJECTED'

export type AiMessageKnowledgeScopeType =
  | 'STORE'
  | 'ROOM_TYPE'
  | 'ROOM'
  | 'RESERVATION'
  | 'CHANNEL'
  | 'THREAD'

export interface ApiResponse<T> {
  success: boolean
  message?: string
  data?: T
}

export interface AiMessageKnowledgeListParams {
  page?: number
  size?: number
  status?: AiMessageKnowledgeStatus | string
  keyword?: string
  scopeType?: AiMessageKnowledgeScopeType | string
  topicCode?: string
}

export interface AiMessageKnowledgeItem {
  id: number
  title?: string | null
  topicCode?: string | null
  topicLabel?: string | null
  topicName?: string | null
  status?: AiMessageKnowledgeStatus | string | null
  scopeType?: AiMessageKnowledgeScopeType | string | null
  scopeName?: string | null
  question?: string | null
  sourceQuestion?: string | null
  normalizedQuestion?: string | null
  answer?: string | null
  content?: string | null
  summary?: string | null
  knowledgeText?: string | null
  language?: string | null
  evidenceLanguages?: string[] | null
  languageSummary?: string | null
  evidenceCount?: number | null
  confidence?: number | string | null
  createdAt?: string | null
  updatedAt?: string | null
  lastSeenAt?: string | null
  [key: string]: unknown
}

export interface AiMessageKnowledgePageData {
  content?: AiMessageKnowledgeItem[]
  items?: AiMessageKnowledgeItem[]
  totalElements?: number
  total?: number
  totalPages?: number
  number?: number
  page?: number
  size?: number
  [key: string]: unknown
}

export interface AiMessageKnowledgeEvidence {
  id?: number
  sourceType?: string | null
  sourceTitle?: string | null
  sourceText?: string | null
  content?: string | null
  messageContent?: string | null
  guestMessage?: string | null
  staffMessage?: string | null
  channelName?: string | null
  topicCode?: string | null
  language?: string | null
  occurredAt?: string | null
  createdAt?: string | null
  confidence?: number | string | null
  [key: string]: unknown
}

export interface AiMessageKnowledgeEvidenceData {
  item?: AiMessageKnowledgeItem
  evidence?: AiMessageKnowledgeEvidence[]
  content?: AiMessageKnowledgeEvidence[]
  items?: AiMessageKnowledgeEvidence[]
  [key: string]: unknown
}

export interface AiMessageKnowledgeRejectRequest {
  reason?: string
}

export interface AiMessageKnowledgeRequestOptions {
  suppressErrorToast?: boolean
}

const buildParams = (source: Record<string, unknown>) => {
  const params: Record<string, unknown> = {}
  for (const key of Object.keys(source)) {
    const value = source[key]
    if (value === undefined || value === null || value === '') {
      continue
    }
    params[key] = value
  }
  return params
}

const shouldSuppressErrorToast = (options: AiMessageKnowledgeRequestOptions = {}) => {
  if (options.suppressErrorToast === undefined) {
    return DEFAULT_SUPPRESS_ERROR_TOAST
  }
  return options.suppressErrorToast
}

export const getAiMessageKnowledgeItems = (
  params: AiMessageKnowledgeListParams,
  options: AiMessageKnowledgeRequestOptions = {},
): Promise<ApiResponse<AiMessageKnowledgePageData>> => {
  return request.get(KNOWLEDGE_ITEMS_URL, {
    params: buildParams(params as Record<string, unknown>),
    suppressErrorToast: shouldSuppressErrorToast(options),
  })
}

export const getAiMessageKnowledgeEvidence = (
  id: number,
  options: AiMessageKnowledgeRequestOptions = {},
): Promise<ApiResponse<AiMessageKnowledgeEvidenceData | AiMessageKnowledgeEvidence[]>> => {
  return request.get(`${KNOWLEDGE_ITEMS_URL}/${id}/evidence`, {
    suppressErrorToast: shouldSuppressErrorToast(options),
  })
}

export const approveAiMessageKnowledgeItem = (
  id: number,
  options: AiMessageKnowledgeRequestOptions = {},
): Promise<ApiResponse<AiMessageKnowledgeItem>> => {
  return request.post(`${KNOWLEDGE_ITEMS_URL}/${id}/approve`, undefined, {
    suppressErrorToast: shouldSuppressErrorToast(options),
  })
}

export const rejectAiMessageKnowledgeItem = (
  id: number,
  data: AiMessageKnowledgeRejectRequest = {},
  options: AiMessageKnowledgeRequestOptions = {},
): Promise<ApiResponse<AiMessageKnowledgeItem>> => {
  return request.post(`${KNOWLEDGE_ITEMS_URL}/${id}/reject`, data, {
    suppressErrorToast: shouldSuppressErrorToast(options),
  })
}

export const archiveAiMessageKnowledgeItem = (
  id: number,
  options: AiMessageKnowledgeRequestOptions = {},
): Promise<ApiResponse<AiMessageKnowledgeItem>> => {
  return request.post(`${KNOWLEDGE_ITEMS_URL}/${id}/archive`, undefined, {
    suppressErrorToast: shouldSuppressErrorToast(options),
  })
}
