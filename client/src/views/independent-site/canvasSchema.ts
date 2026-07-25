// Canvas Schema v1：与技术设计任务书 §3 契约逐条镜像（后端 IndependentSiteCanvasValidator 同规则）。
// 渲染侧只接受 normalize 通过的树；AI 生成/编辑结果经 validate 收集全部违规项。

import type { IndependentSitePageFormat } from '@/types/independentSite'

export const INDEPENDENT_SITE_CANVAS_SCHEMA_VERSION = 'independent_site_canvas_v1' as const

// 页面 format 归一化：后端未返回或返回未知值时一律按 BLOCKS 兜底（旧站点零变化）
export const normalizeIndependentSitePageFormat = (value: unknown): IndependentSitePageFormat =>
  value === 'CANVAS' ? 'CANVAS' : 'BLOCKS'

export const CANVAS_MAX_NODES = 300
export const CANVAS_MAX_DEPTH = 14
export const CANVAS_MAX_CHILDREN = 25
export const CANVAS_MAX_CLASS_LENGTH = 1500
export const CANVAS_MAX_TEXT_LENGTH = 500
export const CANVAS_MAX_ALT_LENGTH = 200
export const CANVAS_MAX_URL_LENGTH = 1500

export const CANVAS_TAGS = [
  'div',
  'section',
  'header',
  'footer',
  'main',
  'nav',
  'h1',
  'h2',
  'h3',
  'h4',
  'h5',
  'h6',
  'p',
  'span',
  'a',
  'img',
  'ul',
  'ol',
  'li',
  'button',
  'figure',
  'figcaption',
  'hr',
  'strong',
  'em',
  'small',
  'blockquote',
] as const

export type CanvasTag = (typeof CANVAS_TAGS)[number]

export type CanvasAction = 'scroll-to-booking'

export type CanvasRoomListLayout = 'grid' | 'list'

export interface CanvasElementAttrs {
  href?: string
  target?: '_blank'
  src?: string
  alt?: string
}

export interface CanvasElementNode {
  id: string
  type: 'element'
  tag: CanvasTag
  class?: string
  attrs?: CanvasElementAttrs
  action?: CanvasAction
  children?: CanvasNode[]
}

export interface CanvasTextNode {
  id: string
  type: 'text'
  text: string
}

// 插槽类型：room-list（在售房型列表）与 booking-flow（完整预订流程挂载点），每种每页至多 1 个
export type CanvasSlotKind = 'room-list' | 'booking-flow'

export interface CanvasSlotNode {
  id: string
  type: 'slot'
  slot: CanvasSlotKind
  // 仅 room-list 使用；booking-flow 无 props
  props?: {
    layout?: CanvasRoomListLayout
  }
}

export type CanvasNode = CanvasElementNode | CanvasTextNode | CanvasSlotNode

export interface IndependentSiteCanvasSchema {
  schemaVersion: typeof INDEPENDENT_SITE_CANVAS_SCHEMA_VERSION
  root: CanvasNode
}

const TAG_SET = new Set<string>(CANVAS_TAGS)
// 契约：img/hr 为 void 标签，禁止 children
const CHILDLESS_TAGS = new Set<string>(['img', 'hr'])
const ACTION_TAGS = new Set<string>(['button', 'a'])
const ATTRS_TAGS = new Set<string>(['a', 'img'])
const NODE_ID_PATTERN = /^[a-z0-9][a-z0-9-]{1,39}$/
// 与后端 URL_FORBIDDEN_CHARS 一致：禁空白与 <>{}"'
const UNSAFE_URL_CHARS = /[\s<>{}"']/
// class 黑名单（大小写不敏感，命中即非法；arbitrary value 如 bg-[#1a2b3c] 允许）
const CLASS_FORBIDDEN = [
  'url(',
  'javascript',
  'expression(',
  '<',
  '>',
  '{',
  '}',
  '`',
  '\\',
  '!',
  '@',
  ';',
]

// 各节点的字段白名单（后端 assertOnlyFields 同规则，出现未知字段即非法）
const ROOT_FIELDS = ['schemaVersion', 'root']
const ELEMENT_FIELDS = ['id', 'type', 'tag', 'class', 'attrs', 'action', 'children']
const TEXT_FIELDS = ['id', 'type', 'text']
const SLOT_FIELDS = ['id', 'type', 'slot', 'props']
const SLOT_PROPS_FIELDS = ['layout']
const A_ATTR_FIELDS = ['href', 'target']
const IMG_ATTR_FIELDS = ['src', 'alt']

// 以下文本规则与后端 safeContent 同清单（价格/货币/URL/代码禁令）
const HTML_TAG = /<[^>]+>/s
const MONEY_VALUE = /[$€£¥￥]|\b\d+(?:\.\d{1,2})?\s*(?:USD|CNY|RMB|JPY|EUR|GBP)\b/i
const CSS_DECLARATION =
  /(?:^|[;\s])(?:background(?:-color)?|border(?:-radius)?|color|display|font(?:-family|-size)?|height|margin|padding|position|width)\s*:/i
const URL_OR_ROUTE =
  /\b[a-z][a-z0-9+.-]{1,20}:\/\/|\bwww\.|\b[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?\.(?:com|net|org|cn|jp|co|io|hotel|travel)(?:[/\s]|$)|(?:^|\s)\/(?:[a-z0-9][a-z0-9._~-]*)(?:\/[a-z0-9][a-z0-9._~-]*)*(?:\?\S*)?(?:\s|$)/i
const FORBIDDEN_TEXT = [
  'http://',
  'https://',
  'www.',
  'javascript:',
  'data:',
  '<script',
  '<style',
  'onclick',
  'onerror',
  'onload',
  'function(',
  '=>',
  'href=',
  'src=',
  'style=',
  'class=',
  'classname=',
  '```',
  'window.',
  'document.',
  '/api/',
  '/stay/',
  '/checkout',
  '/payment',
  'payment',
  'checkout',
  'price',
  'currency',
  '支付',
  '价格',
  '金额',
  '路由',
]

const isRecord = (value: unknown): value is Record<string, unknown> =>
  Boolean(value) && typeof value === 'object' && !Array.isArray(value)

// href/src 规则：http(s) 绝对、/ 开头相对（禁 //）、# 锚点（src 禁用），禁 javascript:/data:
const isSafeCanvasUrl = (value: string, allowAnchor: boolean): boolean => {
  if (!value || value.length > CANVAS_MAX_URL_LENGTH || UNSAFE_URL_CHARS.test(value)) {
    return false
  }
  const lower = value.toLowerCase()
  if (lower.startsWith('https://') || lower.startsWith('http://')) {
    return true
  }
  if (lower.startsWith('javascript:') || lower.startsWith('data:') || value.startsWith('//')) {
    return false
  }
  if (value.startsWith('/')) {
    return true
  }
  return allowAnchor && value.startsWith('#')
}

const hasClassForbiddenHit = (value: string): boolean => {
  const lower = value.toLowerCase()
  return CLASS_FORBIDDEN.some((token) => lower.includes(token.toLowerCase()))
}

// 文本节点/alt 共用：禁 HTML、花括号、URL/域名、价格货币、CSS 声明、事件/代码关键词
const findTextViolation = (value: string): string | null => {
  if (HTML_TAG.test(value) || value.includes('{') || value.includes('}')) {
    return '包含 HTML/CSS/代码'
  }
  if (MONEY_VALUE.test(value)) {
    return '不得包含价格或货币值'
  }
  if (CSS_DECLARATION.test(value) || URL_OR_ROUTE.test(value)) {
    return '不得包含 CSS、URL 或路由'
  }
  const lower = value.toLowerCase()
  for (const forbidden of FORBIDDEN_TEXT) {
    if (lower.includes(forbidden.toLowerCase())) {
      return '包含不允许的页面能力'
    }
  }
  return null
}

export const generateCanvasNodeId = (prefix = 'n'): string =>
  `${prefix}-${Math.random().toString(36).slice(2, 6)}${Date.now().toString(36).slice(-4)}`

export const createCanvasText = (text: string, id = generateCanvasNodeId('t')): CanvasTextNode => ({
  id,
  type: 'text',
  text,
})

export const createCanvasElement = (
  tag: CanvasTag,
  options: {
    id?: string
    class?: string
    attrs?: CanvasElementAttrs
    action?: CanvasAction
    children?: CanvasNode[]
  } = {},
): CanvasElementNode => {
  const node: CanvasElementNode = {
    id: options.id ?? generateCanvasNodeId(),
    type: 'element',
    tag,
  }
  if (options.class) {
    node.class = options.class
  }
  if (options.attrs) {
    node.attrs = options.attrs
  }
  if (options.action) {
    node.action = options.action
  }
  if (options.children?.length) {
    node.children = options.children
  }
  return node
}

export const createCanvasSlot = (
  slot: CanvasSlotKind = 'room-list',
  options: { layout?: CanvasRoomListLayout; id?: string } = {},
): CanvasSlotNode => {
  const node: CanvasSlotNode = {
    id: options.id ?? generateCanvasNodeId('slot'),
    type: 'slot',
    slot,
  }
  if (slot === 'room-list') {
    node.props = { layout: options.layout ?? 'grid' }
  }
  return node
}

// 与后端 defaultCanvasSchema 同形的默认骨架（新建站点 HOME / 新自定义页草稿用）
export const createDefaultCanvasSchema = (hotelName = '酒店名称'): IndependentSiteCanvasSchema => ({
  schemaVersion: INDEPENDENT_SITE_CANVAS_SCHEMA_VERSION,
  root: createCanvasElement('main', {
    id: 'root',
    class: 'min-h-screen bg-white text-slate-800',
    children: [
      createCanvasElement('section', {
        id: 'sec-hero',
        class: 'flex min-h-[60vh] flex-col items-center justify-center gap-6 px-6 text-center',
        children: [
          createCanvasElement('h1', {
            id: 'hero-title',
            class: 'text-4xl font-bold tracking-wide md:text-6xl',
            children: [createCanvasText(hotelName, 'hero-title-t')],
          }),
          createCanvasElement('p', {
            id: 'hero-sub',
            class: 'max-w-xl text-lg text-slate-500',
            children: [createCanvasText('欢迎光临，直接预订享受最优住宿体验。', 'hero-sub-t')],
          }),
          createCanvasElement('button', {
            id: 'hero-cta',
            class: 'rounded-full bg-slate-900 px-8 py-3 text-white transition hover:bg-slate-700',
            action: 'scroll-to-booking',
            children: [createCanvasText('立即预订', 'hero-cta-t')],
          }),
        ],
      }),
      { ...createCanvasSlot('room-list'), id: 'slot-rooms' },
      createCanvasSlot('booking-flow', { id: 'slot-booking' }),
    ],
  }),
})

export const isCanvasElementNode = (node: CanvasNode): node is CanvasElementNode =>
  node.type === 'element'

export const isCanvasTextNode = (node: CanvasNode): node is CanvasTextNode => node.type === 'text'

export const isCanvasSlotNode = (node: CanvasNode): node is CanvasSlotNode => node.type === 'slot'

export const findCanvasNodeById = (root: CanvasNode, id: string): CanvasNode | null => {
  if (root.id === id) {
    return root
  }
  if (isCanvasElementNode(root) && root.children) {
    for (const child of root.children) {
      const found = findCanvasNodeById(child, id)
      if (found) {
        return found
      }
    }
  }
  return null
}

// 树中是否已存在指定插槽（公开渲染自动补 booking-flow 前的检查用）
export const canvasTreeHasSlot = (root: CanvasNode, slot: CanvasSlotKind): boolean => {
  if (isCanvasSlotNode(root)) {
    return root.slot === slot
  }
  if (isCanvasElementNode(root) && root.children) {
    return root.children.some((child) => canvasTreeHasSlot(child, slot))
  }
  return false
}

export const countCanvasNodes = (root: CanvasNode): number => {
  if (!isCanvasElementNode(root) || !root.children) {
    return 1
  }
  return root.children.reduce((total, child) => total + countCanvasNodes(child), 1)
}

export const collectCanvasNodeIds = (root: CanvasNode): string[] => {
  const ids = [root.id]
  if (isCanvasElementNode(root) && root.children) {
    for (const child of root.children) {
      ids.push(...collectCanvasNodeIds(child))
    }
  }
  return ids
}

interface CanvasProcessState {
  nodeCount: number
  roomListCount: number
  bookingFlowCount: number
  ids: Set<string>
  errors: string[]
}

const assertOnlyFields = (
  raw: Record<string, unknown>,
  allowed: readonly string[],
  location: string,
  errors: string[],
): boolean => {
  for (const key of Object.keys(raw)) {
    if (!allowed.includes(key)) {
      errors.push(`${location} 包含不允许的字段: ${key}`)
      return false
    }
  }
  return true
}

const normalizeAttrs = (
  raw: unknown,
  tag: CanvasTag,
  nodeId: string,
  errors: string[],
): CanvasElementAttrs | undefined => {
  if (raw === undefined || raw === null) {
    return undefined
  }
  if (!ATTRS_TAGS.has(tag)) {
    errors.push(`节点 ${nodeId} 的 ${tag} 标签不允许 attrs`)
    return undefined
  }
  if (!isRecord(raw)) {
    errors.push(`节点 ${nodeId} 的 attrs 必须是对象`)
    return undefined
  }
  const attrs: CanvasElementAttrs = {}
  if (tag === 'a') {
    if (!assertOnlyFields(raw, A_ATTR_FIELDS, `节点 ${nodeId} 的 a.attrs`, errors)) {
      return undefined
    }
    if (raw.href !== undefined && raw.href !== null) {
      const href = typeof raw.href === 'string' ? raw.href.trim() : ''
      if (!isSafeCanvasUrl(href, true)) {
        errors.push(`节点 ${nodeId} 的 href 不合法`)
        return undefined
      }
      attrs.href = href
    }
    if (raw.target !== undefined && raw.target !== null) {
      const target = typeof raw.target === 'string' ? raw.target.trim() : ''
      if (target !== '_blank') {
        errors.push(`节点 ${nodeId} 的 target 仅允许 _blank`)
        return undefined
      }
      attrs.target = '_blank'
    }
  } else {
    if (!assertOnlyFields(raw, IMG_ATTR_FIELDS, `节点 ${nodeId} 的 img.attrs`, errors)) {
      return undefined
    }
    const src = typeof raw.src === 'string' ? raw.src.trim() : ''
    if (!isSafeCanvasUrl(src, false)) {
      errors.push(`节点 ${nodeId} 的 img 缺少合法 src`)
      return undefined
    }
    attrs.src = src
    if (raw.alt !== undefined && raw.alt !== null) {
      const alt = typeof raw.alt === 'string' ? raw.alt.trim() : ''
      if (!alt || alt.length > CANVAS_MAX_ALT_LENGTH) {
        errors.push(`节点 ${nodeId} 的 alt 为空或超过长度限制`)
        return undefined
      }
      const violation = findTextViolation(alt)
      if (violation) {
        errors.push(`节点 ${nodeId} 的 alt ${violation}`)
        return undefined
      }
      attrs.alt = alt
    }
  }
  return Object.keys(attrs).length > 0 ? attrs : undefined
}

const normalizeNode = (
  raw: unknown,
  depth: number,
  state: CanvasProcessState,
  path: string,
): CanvasNode | null => {
  if (depth > CANVAS_MAX_DEPTH) {
    state.errors.push(`${path} 超出最大深度 ${CANVAS_MAX_DEPTH}`)
    return null
  }
  if (!isRecord(raw)) {
    state.errors.push(`${path} 必须是对象节点`)
    return null
  }

  const id = typeof raw.id === 'string' ? raw.id.trim() : ''
  if (!NODE_ID_PATTERN.test(id)) {
    state.errors.push(`${path} 的 id 必须匹配 ${NODE_ID_PATTERN.source}`)
    return null
  }
  if (state.ids.has(id)) {
    state.errors.push(`节点 ${id} 的 id 重复`)
    return null
  }

  const type = raw.type
  if (type === 'text') {
    state.ids.add(id)
    state.nodeCount += 1
    if (!assertOnlyFields(raw, TEXT_FIELDS, `text 节点 ${id}`, state.errors)) {
      return null
    }
    const text = typeof raw.text === 'string' ? raw.text.trim() : ''
    if (!text || text.length > CANVAS_MAX_TEXT_LENGTH) {
      state.errors.push(`节点 ${id} 的 text 长度需为 1..${CANVAS_MAX_TEXT_LENGTH}`)
      return null
    }
    const violation = findTextViolation(text)
    if (violation) {
      state.errors.push(`节点 ${id} 的文本${violation}`)
      return null
    }
    return { id, type: 'text', text }
  }

  if (type === 'slot') {
    state.ids.add(id)
    state.nodeCount += 1
    if (!assertOnlyFields(raw, SLOT_FIELDS, `slot 节点 ${id}`, state.errors)) {
      return null
    }
    if (raw.slot !== 'room-list' && raw.slot !== 'booking-flow') {
      state.errors.push(`节点 ${id} 的 slot 仅允许 room-list / booking-flow`)
      return null
    }
    if (raw.slot === 'booking-flow') {
      state.bookingFlowCount += 1
      if (state.bookingFlowCount > 1) {
        state.errors.push('每页至多 1 个 booking-flow 插槽')
        return null
      }
      // booking-flow 无 props
      if (raw.props !== undefined && raw.props !== null) {
        state.errors.push(`节点 ${id} 的 booking-flow 插槽不允许 props`)
        return null
      }
      return { id, type: 'slot', slot: 'booking-flow' }
    }
    state.roomListCount += 1
    if (state.roomListCount > 1) {
      state.errors.push('每页至多 1 个 room-list 插槽')
      return null
    }
    let layout: CanvasRoomListLayout = 'grid'
    if (raw.props !== undefined && raw.props !== null) {
      if (!isRecord(raw.props)) {
        state.errors.push(`节点 ${id} 的 props 必须是对象`)
        return null
      }
      if (
        !assertOnlyFields(raw.props, SLOT_PROPS_FIELDS, `节点 ${id} 的 slot.props`, state.errors)
      ) {
        return null
      }
      if (raw.props.layout !== undefined && raw.props.layout !== null) {
        const layoutValue =
          typeof raw.props.layout === 'string' ? raw.props.layout.trim().toLowerCase() : ''
        if (layoutValue !== 'grid' && layoutValue !== 'list') {
          state.errors.push(`节点 ${id} 的 layout 仅允许 grid 或 list`)
          return null
        }
        layout = layoutValue as CanvasRoomListLayout
      }
    }
    return { id, type: 'slot', slot: 'room-list', props: { layout } }
  }

  if (type !== 'element') {
    state.errors.push(`${path} 的 type 必须是 element / text / slot`)
    return null
  }

  state.ids.add(id)
  state.nodeCount += 1
  if (!assertOnlyFields(raw, ELEMENT_FIELDS, `element 节点 ${id}`, state.errors)) {
    return null
  }

  const rawTag = typeof raw.tag === 'string' ? raw.tag.trim().toLowerCase() : ''
  if (!TAG_SET.has(rawTag)) {
    state.errors.push(`节点 ${id} 的 tag 不在白名单`)
    return null
  }
  const tag = rawTag as CanvasTag

  const node: CanvasElementNode = { id, type: 'element', tag }

  if (raw.class !== undefined && raw.class !== null) {
    if (typeof raw.class !== 'string') {
      state.errors.push(`节点 ${id} 的 class 必须是文本`)
      return null
    }
    const className = raw.class.trim()
    if (className.length > CANVAS_MAX_CLASS_LENGTH) {
      state.errors.push(`节点 ${id} 的 class 超过长度限制`)
      return null
    }
    if (hasClassForbiddenHit(className)) {
      state.errors.push(`节点 ${id} 的 class 包含不允许的内容`)
      return null
    }
    if (className) {
      node.class = className
    }
  }

  const attrs = normalizeAttrs(raw.attrs, tag, id, state.errors)
  if (attrs) {
    node.attrs = attrs
  }

  if (raw.action !== undefined && raw.action !== null) {
    const action = typeof raw.action === 'string' ? raw.action.trim() : ''
    if (!ACTION_TAGS.has(tag)) {
      state.errors.push(`节点 ${id} 的 ${tag} 标签不允许 action`)
      return null
    }
    if (action !== 'scroll-to-booking') {
      state.errors.push(`节点 ${id} 的 action 仅允许 scroll-to-booking`)
      return null
    }
    if (tag === 'a' && node.attrs?.href) {
      state.errors.push(`节点 ${id} 的 a 标签 href 与 action 互斥`)
      return null
    }
    node.action = 'scroll-to-booking'
  }

  if (raw.children !== undefined && raw.children !== null) {
    if (CHILDLESS_TAGS.has(tag)) {
      state.errors.push(`节点 ${id} 的 ${tag} 标签禁止 children`)
      return null
    }
    if (!Array.isArray(raw.children)) {
      state.errors.push(`节点 ${id} 的 children 必须是数组`)
      return null
    }
    if (raw.children.length > CANVAS_MAX_CHILDREN) {
      state.errors.push(`节点 ${id} 的 children 超过 ${CANVAS_MAX_CHILDREN} 个`)
      return null
    }
    const children: CanvasNode[] = []
    for (let index = 0; index < raw.children.length; index += 1) {
      const child = normalizeNode(
        raw.children[index],
        depth + 1,
        state,
        `${path}.children[${index}]`,
      )
      if (!child) {
        return null
      }
      children.push(child)
    }
    if (children.length > 0) {
      node.children = children
    }
  }

  return node
}

// 返回 { schema, errors }：errors 非空时 schema 为 null（渲染 fail-closed，与后端同策略）
const processCanvasSchema = (
  value: unknown,
): { schema: IndependentSiteCanvasSchema | null; errors: string[] } => {
  if (!isRecord(value)) {
    return { schema: null, errors: ['schema 必须是对象'] }
  }
  const fieldErrors: string[] = []
  if (!assertOnlyFields(value, ROOT_FIELDS, '页面配置', fieldErrors)) {
    return { schema: null, errors: fieldErrors }
  }
  if (value.schemaVersion !== INDEPENDENT_SITE_CANVAS_SCHEMA_VERSION) {
    return {
      schema: null,
      errors: [`schemaVersion 必须是 ${INDEPENDENT_SITE_CANVAS_SCHEMA_VERSION}`],
    }
  }
  const state: CanvasProcessState = {
    nodeCount: 0,
    roomListCount: 0,
    bookingFlowCount: 0,
    ids: new Set<string>(),
    errors: [],
  }
  const root = normalizeNode(value.root, 1, state, 'root')
  if (state.nodeCount > CANVAS_MAX_NODES) {
    state.errors.push(`节点总数 ${state.nodeCount} 超过上限 ${CANVAS_MAX_NODES}`)
  }
  if (!root || state.errors.length > 0) {
    return { schema: null, errors: state.errors.length > 0 ? state.errors : ['root 节点不合法'] }
  }
  return {
    schema: { schemaVersion: INDEPENDENT_SITE_CANVAS_SCHEMA_VERSION, root },
    errors: [],
  }
}

// 渲染入口：任一违规即整树拒绝（null），与 pageSchema 的 normalize 风格一致
export const normalizeCanvasSchema = (value: unknown): IndependentSiteCanvasSchema | null =>
  processCanvasSchema(value).schema

// 编辑/AI 入口：收集全部违规项，便于修复循环回发
export const validateCanvasSchema = (value: unknown): string[] => processCanvasSchema(value).errors
