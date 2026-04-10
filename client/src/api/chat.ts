import { request } from '../utils/request'

export interface ChatMessageRequest {
  message: string
  sessionId?: string
  userId?: string
  taskType?: 'DEFAULT' | 'TRANSLATION'
}

export interface ChatMessageRequestOptions {
  timeoutMs?: number
  suppressErrorToast?: boolean
}

export interface ChatMessageResponse {
  reply: string
  timestamp: string
  sessionId: string
  status: string
  errorMessage?: string
  processingTime?: number
}

export interface ApiResponse<T> {
  success: boolean
  message: string
  data: T
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

  const sourceText = message
    .slice(startIndex + TRANSLATION_MARKER_START.length, endIndex)
    .trim()
  if (!sourceText || sourceText.length <= 1) {
    return null
  }

  const targetInstruction = message
    .slice(0, startIndex)
    .trim()

  return {
    sourceText,
    targetInstruction,
  }
}

const buildTranslationPrompt = (payload: TranslationPayload, strictMode = false) => {
  return [
    'You are a translation engine.',
    'You MUST translate only SOURCE_TEXT and MUST NOT answer the question.',
    'You MUST NOT add any explanation, greeting, or extra sentence.',
    'Keep links, order numbers, dates, times, room numbers, and currency unchanged.',
    payload.targetInstruction ? `Target language instruction: ${payload.targetInstruction}` : '',
    strictMode
      ? 'Return STRICT JSON only: {"translation":"..."}. No markdown, no extra text.'
      : 'Return JSON: {"translation":"..."}.',
    '',
    'SOURCE_TEXT:',
    payload.sourceText,
  ]
    .filter(Boolean)
    .join('\n')
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
    return ''
  } catch {
    return ''
  }
}

const postChatMessage = (data: ChatMessageRequest, options?: ChatMessageRequestOptions) => {
  return request({
    url: '/chat/message',
    method: 'POST',
    data,
    timeout: options?.timeoutMs,
    suppressErrorToast: options?.suppressErrorToast,
  }) as Promise<ApiResponse<ChatMessageResponse>>
}

/**
 * 发送聊天消息并获取AI回复
 */
export const sendChatMessage = async (
  data: ChatMessageRequest,
  options?: ChatMessageRequestOptions,
): Promise<ApiResponse<ChatMessageResponse>> => {
  const translationPayload = extractTranslationPayload(data.message)
  if (!translationPayload) {
    return postChatMessage(data, options)
  }

  const baseTranslationData: ChatMessageRequest = {
    ...data,
    sessionId: createTranslationSessionId(),
    taskType: 'TRANSLATION',
  }

  const firstResponse = await postChatMessage(
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

  const retryResponse = await postChatMessage(
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

  throw new Error('翻译结果解析失败，请重试')
}

/**
 * 获取欢迎消息
 */
export const getWelcomeMessage = (
  sessionId?: string,
): Promise<ApiResponse<ChatMessageResponse>> => {
  return request({
    url: '/chat/welcome',
    method: 'GET',
    params: sessionId ? { sessionId } : undefined,
  })
}

/**
 * 检查聊天服务健康状态
 */
export const checkChatHealth = (): Promise<ApiResponse<Record<string, unknown>>> => {
  return request({
    url: '/chat/health',
    method: 'GET',
  })
}

/**
 * 获取聊天服务信息
 */
export const getChatServiceInfo = (): Promise<ApiResponse<Record<string, string>>> => {
  return request({
    url: '/chat/info',
    method: 'GET',
  })
}
