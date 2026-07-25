// 画布风格预设卡：前端常量，供画布聊天面板与新建站点对话框共用。
// prompt 只描述视觉与文案风格，不包含价格/链接等受禁内容（与 §3 文本禁令兼容）。

type Translator = (key: string, values?: Record<string, unknown>) => string

export interface CanvasStylePreset {
  id: string
  name: string
  // 一句调性描述（卡片副标题）
  description: string
  // 发送给 generate / ai-edit 的风格指令
  prompt: string
}

const STYLE_PRESET_CONFIG = [
  {
    id: 'french-luxury',
    messageKey: 'frenchLuxury',
  },
  {
    id: 'japanese-zen',
    messageKey: 'japaneseZen',
  },
  {
    id: 'modern-urban',
    messageKey: 'modernUrban',
  },
  {
    id: 'family-warm',
    messageKey: 'familyWarm',
  },
] as const

export const getCanvasStylePresets = (t: Translator): CanvasStylePreset[] =>
  STYLE_PRESET_CONFIG.map(({ id, messageKey }) => ({
    id,
    name: t(`independentSite.stylePresets.${messageKey}.name`),
    description: t(`independentSite.stylePresets.${messageKey}.description`),
    prompt: t(`independentSite.stylePresets.${messageKey}.prompt`),
  }))
