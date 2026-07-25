// 画布风格预设卡：前端常量，供画布聊天面板与新建站点对话框共用。
// prompt 只描述视觉与文案风格，不包含价格/链接等受禁内容（与 §3 文本禁令兼容）。

export interface CanvasStylePreset {
  id: string
  name: string
  // 一句调性描述（卡片副标题）
  description: string
  // 发送给 generate / ai-edit 的风格指令
  prompt: string
}

export const CANVAS_STYLE_PRESETS: CanvasStylePreset[] = [
  {
    id: 'french-luxury',
    name: '法式奢华',
    description: '奶油白与金色点缀，克制优雅的高级感',
    prompt:
      '以法式奢华酒店风格设计页面：奶油白、深棕与金色的高级配色，大面积留白，精致的衬线字体标题与优雅排版，内容节奏克制从容，营造低调奢华的氛围。',
  },
  {
    id: 'japanese-zen',
    name: '日式禅意',
    description: '大量留白与原木质感，安静疗愈的极简',
    prompt:
      '以日式禅意风格设计页面：米白、原木色与墨色配色，极简布局与大量留白，竖向节奏舒缓，文案简短安静，营造宁静、疗愈、贴近自然的氛围。',
  },
  {
    id: 'modern-urban',
    name: '现代都市',
    description: '深色高对比，时尚有力的都市节奏',
    prompt:
      '以现代都市精品酒店风格设计页面：深色背景搭配高对比亮色点缀，醒目的大标题排版与紧凑网格布局，文案简洁有力，营造时尚、高效、充满活力的氛围。',
  },
  {
    id: 'family-warm',
    name: '亲子温情',
    description: '暖色圆角，亲切活泼的家庭氛围',
    prompt:
      '以亲子温情风格设计页面：明亮柔和的暖色调，圆润的卡片与按钮，亲切活泼的文案，突出家庭友好、安全与陪伴感。',
  },
]
