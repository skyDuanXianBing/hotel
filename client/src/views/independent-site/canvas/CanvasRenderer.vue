<script setup lang="ts">
import { computed, h, onMounted, type FunctionalComponent, type VNodeChild } from 'vue'
import type {
  IndependentSiteThemeKey,
  PublicIndependentSiteRoomType,
  PublicIndependentSite,
} from '@/types/independentSite'
import { ensureCanvasTailwind } from '@/utils/canvasTailwind'
import {
  isCanvasElementNode,
  isCanvasSlotNode,
  isCanvasTextNode,
  normalizeCanvasSchema,
  type CanvasElementNode,
  type CanvasNode,
  type CanvasSlotNode,
  type IndependentSiteCanvasSchema,
} from '../canvasSchema'
import { buildIndependentSiteCssVars, normalizeIndependentSiteThemeKey } from '../themes'
import BookingFlow from '../booking/BookingFlow.vue'
import SlotRoomList from './slots/SlotRoomList.vue'

const props = withDefaults(
  defineProps<{
    schema: IndependentSiteCanvasSchema
    preview?: boolean
    themeKey?: IndependentSiteThemeKey | string
    // 公开站点发布范围内的房型，供 room-list 插槽渲染；缺省为空数组（插槽显示占位）
    roomTypes?: PublicIndependentSiteRoomType[]
    // booking-flow 插槽所需的公开页上下文：由公开页传入并透传给 BookingFlow；
    // 缺省（如编辑器预览复用）时插槽渲染占位块而非真实预订流程
    bookingSite?: PublicIndependentSite | null
    bookingPreview?: boolean
    bookingPreviewAuthorized?: boolean
    bookingInitialRoomTypeId?: number | null
    // BookingFlow 的强制重建 key（公开页传 `${slug}:${pagePath}`，与原固定挂载语义一致）
    bookingKey?: string
  }>(),
  {
    preview: false,
    roomTypes: () => [],
    bookingSite: null,
    bookingPreview: false,
    bookingPreviewAuthorized: false,
    bookingInitialRoomTypeId: null,
    bookingKey: '',
  },
)

const emit = defineEmits<{
  bookingRequest: []
  selectRoomType: [roomTypeId: number]
}>()

// 与 BLOCKS 渲染器一致的 fail-closed 策略：契约校验不通过则整树不渲染
const safeSchema = computed(() => normalizeCanvasSchema(props.schema))

const themeStyle = computed(() =>
  buildIndependentSiteCssVars(normalizeIndependentSiteThemeKey(props.themeKey)),
)

onMounted(() => {
  ensureCanvasTailwind()
})

// action=scroll-to-booking：a/button 统一拦截并上抛，由公开页滚动到固定挂载的 #booking
const handleActionClick = (event: MouseEvent) => {
  event.preventDefault()
  emit('bookingRequest')
}

const buildElementProps = (node: CanvasElementNode): Record<string, unknown> => {
  const elementProps: Record<string, unknown> = { key: node.id }
  if (node.class) {
    elementProps.class = node.class
  }
  if (node.tag === 'button') {
    elementProps.type = 'button'
  }
  if (node.tag === 'a' && node.attrs?.href) {
    elementProps.href = node.attrs.href
    if (node.attrs.target === '_blank') {
      elementProps.target = '_blank'
      elementProps.rel = 'noopener noreferrer'
    }
  }
  if (node.tag === 'img') {
    elementProps.src = node.attrs?.src
    elementProps.alt = node.attrs?.alt ?? ''
    elementProps.loading = 'lazy'
    elementProps.decoding = 'async'
  }
  if (node.action === 'scroll-to-booking') {
    elementProps.onClick = handleActionClick
  }
  return elementProps
}

// booking-flow 插槽：有公开站点上下文时挂载真实 BookingFlow（组件根节点自带 id="booking"，
// scroll-to-booking 语义不变），否则渲染占位块（编辑器预览复用等场景不崩溃）
const renderBookingFlowSlot = (node: CanvasSlotNode): VNodeChild => {
  if (!props.bookingSite) {
    return h(
      'div',
      { key: node.id, class: 'canvas-slot-booking-placeholder' },
      '预订流程将在公开页此处显示',
    )
  }
  return h('div', { key: node.id, class: 'canvas-slot-booking' }, [
    h(BookingFlow, {
      key: props.bookingKey || 'booking-flow',
      site: props.bookingSite,
      preview: props.bookingPreview,
      previewAuthorized: props.bookingPreviewAuthorized,
      initialRoomTypeId: props.bookingInitialRoomTypeId,
    }),
  ])
}

// 递归渲染节点树：element→白名单 tag，text→纯文本插值，slot→插槽组件；全程无 v-html
const renderCanvasNode = (node: CanvasNode): VNodeChild => {
  if (isCanvasTextNode(node)) {
    return node.text
  }
  if (isCanvasSlotNode(node)) {
    if (node.slot === 'booking-flow') {
      return renderBookingFlowSlot(node)
    }
    return h(SlotRoomList, {
      key: node.id,
      layout: node.props?.layout ?? 'grid',
      roomTypes: props.roomTypes,
      onBookingRequest: () => emit('bookingRequest'),
      onSelectRoomType: (roomTypeId: number) => emit('selectRoomType', roomTypeId),
    })
  }
  if (isCanvasElementNode(node)) {
    const children = node.children?.map((child) => renderCanvasNode(child))
    return h(node.tag, buildElementProps(node), children)
  }
  return null
}

// 函数式组件承载递归渲染，模板侧只需渲染根节点
const CanvasNodeView: FunctionalComponent<{ node: CanvasNode }> = (nodeProps) =>
  renderCanvasNode(nodeProps.node)
CanvasNodeView.props = ['node']
</script>

<template>
  <div
    v-if="safeSchema"
    class="canvas-renderer"
    :class="{ 'is-preview': preview }"
    :style="themeStyle"
  >
    <CanvasNodeView :node="safeSchema.root" />
  </div>
</template>

<style scoped>
.canvas-renderer {
  min-height: 40vh;
  color: var(--site-text, #1f2a28);
  background: var(--site-surface, #fff);
  font-family: var(--site-font-body, inherit);
}

/* 运行时无 preflight，给 img 一个兜底防撑破布局；AI 生成的 class 仍可覆盖 */
.canvas-renderer :deep(img) {
  max-width: 100%;
  height: auto;
}

/* 未传 booking 上下文时 booking-flow 插槽的占位块（编辑器预览复用场景） */
.canvas-renderer :deep(.canvas-slot-booking-placeholder) {
  display: grid;
  min-height: 160px;
  place-content: center;
  margin: 24px;
  padding: 32px;
  border: 1px dashed rgba(31, 42, 40, 0.3);
  border-radius: 12px;
  color: var(--site-text, #485652);
  font-size: 14px;
}
</style>
