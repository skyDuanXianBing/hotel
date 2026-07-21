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
  MessageSendRequest,
  MessageThreadDTO,
  MessageTranslationRequest,
  MessageTranslationResponse,
} from '@/types/message'
import { i18n } from '@/locales'

export { MESSAGE_API_MOCK_ENABLED }

export interface ChatMessageRequestOptions {
  timeoutMs?: number
  signal?: AbortSignal
  suppressErrorToast?: boolean
}

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
