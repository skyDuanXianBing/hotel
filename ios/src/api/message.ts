import request from '@/utils/request'
import {
  getMockMessageThreads,
  getMockThreadMessages,
  MESSAGE_API_MOCK_ENABLED,
  pollMockThreadMessages,
  sendMockThreadMessage,
} from '@/mocks/message'
import type { ApiResponse } from '@/types/api'
import type {
  ChatMessageRequest,
  ChatMessageResponse,
  MessageDTO,
  MessageAiReplyDraftRequest,
  MessageAiReplyDraftResponse,
  MessagePageDTO,
  MessagePageRequest,
  MessageSendRequest,
  MessageThreadDTO,
  MessageThreadPageDTO,
  MessageThreadPageRequest,
  MessageTranslationSetting,
  MessageTranslationRequest,
  MessageTranslationResponse,
  MessageUnreadSummaryDTO,
} from '@/types/message'
import { i18n } from '@/locales'

export { MESSAGE_API_MOCK_ENABLED }

export interface ChatMessageRequestOptions {
  timeoutMs?: number
  signal?: AbortSignal
  suppressErrorToast?: boolean
}

// su-messaging 接口需要聚合渠道数据，响应较慢；与 client 端 SU_MESSAGING_TIMEOUT_MS 对齐
const SU_MESSAGING_TIMEOUT_MS = 60000

const TRANSLATION_MARKER_START = '<<<TEXT>>>'
const TRANSLATION_MARKER_END = '<<<END>>>'

interface TranslationPayload {
  sourceText: string
  targetInstruction: string
}

const extractTranslationPayload = (message: string): TranslationPayload | null => {
  if (!message.includes(TRANSLATION_MARKER_START) || !message.includes(TRANSLATION_MARKER_END)) {
    return null
  }

  const endIndex = message.lastIndexOf(TRANSLATION_MARKER_END)
  const startIndex = message.lastIndexOf(TRANSLATION_MARKER_START, endIndex)
  if (startIndex < 0 || endIndex < 0) {
    return null
  }

  const sourceText = message.slice(startIndex + TRANSLATION_MARKER_START.length, endIndex).trim()
  if (!sourceText || sourceText.length <= 1) {
    return null
  }

  return {
    sourceText,
    targetInstruction: message.slice(0, startIndex).trim(),
  }
}

const buildTranslationPrompt = (payload: TranslationPayload, strictMode = false) => {
  const promptLines = [
    'You are a translation engine.',
    'You MUST translate only SOURCE_TEXT and MUST NOT answer the question.',
    'You MUST NOT add any explanation, greeting, or extra sentence.',
    'Keep links, order numbers, dates, times, room numbers, and currency unchanged.',
  ]

  if (payload.targetInstruction) {
    promptLines.push(`Target language instruction: ${payload.targetInstruction}`)
  }

  if (strictMode) {
    promptLines.push('Return STRICT JSON only: {"translation":"..."}. No markdown, no extra text.')
  } else {
    promptLines.push('Return JSON: {"translation":"..."}.')
  }

  promptLines.push('', 'SOURCE_TEXT:', payload.sourceText)
  return promptLines.join('\n')
}

const createTranslationSessionId = () =>
  `translation_${Date.now()}_${Math.random().toString(36).slice(2, 10)}`

const extractTranslationFromReply = (replyText?: string) => {
  const trimmedReply = (replyText || '').trim()
  if (!trimmedReply) {
    return ''
  }

  const jsonBlockMatch = trimmedReply.match(/\{[\s\S]*\}/)
  if (!jsonBlockMatch) {
    return ''
  }

  try {
    const parsed = JSON.parse(jsonBlockMatch[0]) as { translation?: unknown }
    if (typeof parsed.translation === 'string') {
      return parsed.translation.trim()
    }
  } catch {
    return ''
  }

  return ''
}

const postAiChatMessage = (data: ChatMessageRequest, options?: ChatMessageRequestOptions) => {
  return request<ApiResponse<ChatMessageResponse>>({
    url: '/chat/message',
    method: 'POST',
    data,
    timeoutMs: options?.timeoutMs,
    signal: options?.signal,
    suppressErrorToast: options?.suppressErrorToast,
  })
}

export const getMessageThreads = () => {
  if (MESSAGE_API_MOCK_ENABLED) {
    return getMockMessageThreads()
  }

  return request<ApiResponse<MessageThreadDTO[]>>({
    url: '/su-messaging/threads',
    method: 'GET',
    timeoutMs: SU_MESSAGING_TIMEOUT_MS,
  })
}

const buildThreadPageQuery = (params: MessageThreadPageRequest) => {
  const query: Record<string, string | number | boolean> = {}
  for (const [key, value] of Object.entries(params)) {
    if (value === undefined || value === null || value === '') {
      continue
    }
    query[key] = value as string | number | boolean
  }
  return query
}

const getMockMessageThreadsPage = async (
  params: MessageThreadPageRequest,
): Promise<ApiResponse<MessageThreadPageDTO>> => {
  const response = await getMockMessageThreads()
  const allThreads = (response.data || []).filter((thread) => {
    if (params.unread && thread.unreadCount <= 0) {
      return false
    }
    if (params.closed && !thread.closed) {
      return false
    }
    return true
  })

  const page = params.page ?? 0
  const size = params.size ?? 30
  const items = allThreads.slice(page * size, page * size + size)
  const totalPages = size > 0 ? Math.ceil(allThreads.length / size) : 0

  return {
    success: true,
    message: response.message,
    data: {
      items,
      page,
      size,
      totalElements: allThreads.length,
      totalPages,
      hasNext: page + 1 < totalPages,
    },
  }
}

export const getMessageThreadsPage = (params: MessageThreadPageRequest) => {
  if (MESSAGE_API_MOCK_ENABLED) {
    return getMockMessageThreadsPage(params)
  }

  return request<ApiResponse<MessageThreadPageDTO>>({
    url: '/su-messaging/threads/page',
    method: 'GET',
    params: buildThreadPageQuery(params),
    timeoutMs: SU_MESSAGING_TIMEOUT_MS,
  })
}

export const getMessageUnreadSummary = () => {
  return request<ApiResponse<MessageUnreadSummaryDTO>>({
    url: '/su-messaging/unread-summary',
    method: 'GET',
    suppressErrorToast: true,
  })
}

const getMockMessageThread = async (threadId: number): Promise<ApiResponse<MessageThreadDTO>> => {
  const response = await getMockMessageThreads()
  const thread = (response.data || []).find((item) => item.id === threadId)
  if (!thread) {
    throw new Error('Thread not found')
  }

  return {
    success: true,
    message: response.message,
    data: thread,
  }
}

export const getMessageThread = (threadId: number) => {
  if (MESSAGE_API_MOCK_ENABLED) {
    return getMockMessageThread(threadId)
  }

  return request<ApiResponse<MessageThreadDTO>>({
    url: `/su-messaging/threads/${threadId}`,
    method: 'GET',
    suppressErrorStatuses: [400, 403, 404],
    timeoutMs: SU_MESSAGING_TIMEOUT_MS,
  })
}

export const getThreadMessages = (threadId: number) => {
  if (MESSAGE_API_MOCK_ENABLED) {
    return getMockThreadMessages(threadId)
  }

  return request<ApiResponse<MessageDTO[]>>({
    url: `/su-messaging/threads/${threadId}/messages`,
    method: 'GET',
    suppressErrorStatuses: [400, 403, 404],
    timeoutMs: SU_MESSAGING_TIMEOUT_MS,
  })
}

const getMockThreadMessagesPage = async (
  threadId: number,
  params: MessagePageRequest,
): Promise<ApiResponse<MessagePageDTO>> => {
  const response = await getMockThreadMessages(threadId)
  const allMessages = response.data || []
  const limit = params.limit ?? 50

  let candidates = allMessages
  if (params.beforeMessageId) {
    candidates = allMessages.filter((item) => item.id < (params.beforeMessageId as number))
  }
  const items = candidates.slice(-limit)
  const oldestId = items[0]?.id
  const hasMoreBefore = Boolean(oldestId && candidates.length > items.length)

  return {
    success: true,
    message: response.message,
    data: {
      items,
      limit,
      hasMoreBefore,
      nextBeforeMessageId: hasMoreBefore ? oldestId : undefined,
    },
  }
}

export const getThreadMessagesPage = (threadId: number, params: MessagePageRequest) => {
  if (MESSAGE_API_MOCK_ENABLED) {
    return getMockThreadMessagesPage(threadId, params)
  }

  return request<ApiResponse<MessagePageDTO>>({
    url: `/su-messaging/threads/${threadId}/messages/page`,
    method: 'GET',
    params: {
      limit: params.limit,
      beforeMessageId: params.beforeMessageId,
      afterMessageId: params.afterMessageId,
      markRead: params.markRead,
    },
    suppressErrorStatuses: [400, 403, 404],
    timeoutMs: SU_MESSAGING_TIMEOUT_MS,
  })
}

export const pollThreadMessages = (threadId: number, since: string) => {
  if (MESSAGE_API_MOCK_ENABLED) {
    return pollMockThreadMessages(threadId, since)
  }

  return request<ApiResponse<MessageDTO[]>>({
    url: `/su-messaging/threads/${threadId}/poll`,
    method: 'GET',
    params: { since },
    suppressErrorStatuses: [400, 403, 404],
    timeoutMs: SU_MESSAGING_TIMEOUT_MS,
  })
}

export const sendThreadMessage = (threadId: number, data: MessageSendRequest) => {
  if (MESSAGE_API_MOCK_ENABLED) {
    return sendMockThreadMessage(threadId, data)
  }

  return request<ApiResponse<MessageDTO>>({
    url: `/su-messaging/threads/${threadId}/send`,
    method: 'POST',
    data,
    timeoutMs: SU_MESSAGING_TIMEOUT_MS,
  })
}

export const translateThreadMessage = (
  threadId: number,
  messageId: number,
  data: MessageTranslationRequest,
  options?: ChatMessageRequestOptions,
) => {
  return request<ApiResponse<MessageTranslationResponse>>({
    url: `/su-messaging/threads/${threadId}/messages/${messageId}/translation`,
    method: 'POST',
    data,
    timeoutMs: options?.timeoutMs,
    signal: options?.signal,
    suppressErrorToast: options?.suppressErrorToast,
  })
}

export const getMessageTranslationSetting = () => {
  return request<ApiResponse<MessageTranslationSetting>>({
    url: '/su-messaging/translation-settings',
    method: 'GET',
    suppressErrorToast: true,
  })
}

export const updateMessageTranslationSetting = (data: MessageTranslationSetting) => {
  return request<ApiResponse<MessageTranslationSetting>>({
    url: '/su-messaging/translation-settings',
    method: 'PUT',
    data: {
      enabled: data.enabled,
      targetLanguage: data.targetLanguage,
    },
    suppressErrorToast: true,
  })
}

export const generateThreadAiReplyDraft = (
  threadId: number,
  data: MessageAiReplyDraftRequest,
) => {
  return request<ApiResponse<MessageAiReplyDraftResponse>>({
    url: `/su-messaging/threads/${threadId}/ai-reply-draft`,
    method: 'POST',
    data,
    timeoutMs: 45000,
    suppressErrorToast: true,
  })
}

export const sendAiChatMessage = async (
  data: ChatMessageRequest,
  options?: ChatMessageRequestOptions,
) => {
  const translationPayload = extractTranslationPayload(data.message)
  if (!translationPayload) {
    return postAiChatMessage(data, options)
  }

  const baseTranslationData: ChatMessageRequest = {
    ...data,
    sessionId: createTranslationSessionId(),
    taskType: 'TRANSLATION',
  }

  const firstResponse = await postAiChatMessage(
    {
      ...baseTranslationData,
      message: buildTranslationPrompt(translationPayload),
    },
    options,
  )
  const firstTranslation = extractTranslationFromReply(firstResponse.data?.reply)
  if (firstTranslation) {
    return {
      ...firstResponse,
      data: {
        ...firstResponse.data,
        reply: firstTranslation,
      },
    }
  }

  const retryResponse = await postAiChatMessage(
    {
      ...baseTranslationData,
      sessionId: createTranslationSessionId(),
      message: buildTranslationPrompt(translationPayload, true),
    },
    options,
  )
  const retryTranslation = extractTranslationFromReply(retryResponse.data?.reply)
  if (retryTranslation) {
    return {
      ...retryResponse,
      data: {
        ...retryResponse.data,
        reply: retryTranslation,
      },
    }
  }

  throw new Error(i18n.global.t('runtime.errors.translationParseFailed'))
}
