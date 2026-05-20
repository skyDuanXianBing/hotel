import { sendAiChatMessage } from '@/api/message'

export type MessageTranslationLanguageValue = 'zh-CN' | 'en' | 'ja' | 'ko'

export interface MessageTranslationSettings {
  enabled: boolean
  targetLanguage: MessageTranslationLanguageValue
}

export const MESSAGE_TRANSLATION_SETTINGS_STORAGE_KEY = 'ios.messages.translation.settings'

export const MESSAGE_TRANSLATION_LANGUAGE_OPTIONS: Array<{
  value: MessageTranslationLanguageValue
  label: string
}> = [
  { value: 'zh-CN', label: '中文(简体)' },
  { value: 'en', label: 'English' },
  { value: 'ja', label: '日本語' },
  { value: 'ko', label: '한국어' },
]

export const DEFAULT_MESSAGE_TRANSLATION_SETTINGS: MessageTranslationSettings = {
  enabled: false,
  targetLanguage: 'zh-CN',
}

const AI_TRANSLATION_TIMEOUT_MS = 45000

function isSupportedTranslationLanguage(value: unknown): value is MessageTranslationLanguageValue {
  if (typeof value !== 'string') {
    return false
  }

  for (const option of MESSAGE_TRANSLATION_LANGUAGE_OPTIONS) {
    if (option.value === value) {
      return true
    }
  }

  return false
}

export function loadMessageTranslationSettings(): MessageTranslationSettings {
  const rawSettings = localStorage.getItem(MESSAGE_TRANSLATION_SETTINGS_STORAGE_KEY)
  if (!rawSettings) {
    return { ...DEFAULT_MESSAGE_TRANSLATION_SETTINGS }
  }

  try {
    const parsed = JSON.parse(rawSettings) as {
      enabled?: boolean
      targetLanguage?: unknown
    }

    return {
      enabled: Boolean(parsed.enabled),
      targetLanguage: isSupportedTranslationLanguage(parsed.targetLanguage)
        ? parsed.targetLanguage
        : DEFAULT_MESSAGE_TRANSLATION_SETTINGS.targetLanguage,
    }
  } catch (error) {
    console.error('读取消息翻译设置失败:', error)
    return { ...DEFAULT_MESSAGE_TRANSLATION_SETTINGS }
  }
}

export function saveMessageTranslationSettings(settings: MessageTranslationSettings) {
  localStorage.setItem(MESSAGE_TRANSLATION_SETTINGS_STORAGE_KEY, JSON.stringify(settings))
}

export function resolveMessageTranslationLanguageLabel(language: MessageTranslationLanguageValue) {
  for (const option of MESSAGE_TRANSLATION_LANGUAGE_OPTIONS) {
    if (option.value === language) {
      return option.label
    }
  }

  return '中文(简体)'
}

export function normalizeTranslatedText(text: string) {
  return text.replace(/^译文[:：]\s*/i, '').replace(/^translation[:：]\s*/i, '').trim()
}

export async function requestAiMessageTranslation(
  sourceText: string,
  targetLanguage: MessageTranslationLanguageValue,
) {
  const trimmed = sourceText.trim()
  if (!trimmed) {
    return ''
  }

  const targetLanguageLabel = resolveMessageTranslationLanguageLabel(targetLanguage)
  const response = await sendAiChatMessage(
    {
      taskType: 'TRANSLATION',
      message: [
        `请把下面提供的正文翻译成${targetLanguageLabel}。`,
        '要求：',
        '1. 只能翻译这一次提供的文本，严禁结合历史消息、上下文或自行补全。',
        '2. 只返回翻译后的正文，不要添加解释、标题、引号或前缀。',
        '3. 保留原始链接、订单号、日期、房号、金额等结构化内容。',
        '4. 如果原文已经是目标语言，也只返回润色后的同语言正文。',
        '',
        '<<<TEXT>>>',
        trimmed,
        '<<<END>>>',
      ].join('\n'),
    },
    {
      timeoutMs: AI_TRANSLATION_TIMEOUT_MS,
    },
  )

  if (!response.success || !response.data?.reply) {
    throw new Error(response.message || '翻译失败')
  }

  return normalizeTranslatedText(response.data.reply)
}
