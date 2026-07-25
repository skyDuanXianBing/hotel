<script setup lang="ts">
// 画布编辑态递归渲染：与公开端 CanvasRenderer 同构，但每个节点带 data-node-id 与编辑交互。
// 注意：与公开渲染器不同，这里不做 fail-closed 的整树 normalize —— 父组件（CanvasEditor）
// 在载入时已 normalize，编辑过程中的中间态由自动保存前的 validateCanvasSchema 拦截，
// 避免单个文本违规导致整棵编辑树消失。
import { h, ref, type FunctionalComponent, type VNodeChild } from 'vue'
import { Calendar } from '@element-plus/icons-vue'
import { useI18n } from 'vue-i18n'
import type { PublicIndependentSiteRoomType } from '@/types/independentSite'
import {
  isCanvasElementNode,
  isCanvasSlotNode,
  isCanvasTextNode,
  type CanvasElementNode,
  type CanvasNode,
  type CanvasSlotNode,
  type CanvasTextNode,
  type IndependentSiteCanvasSchema,
} from '../canvasSchema'
import SlotRoomList from '../canvas/slots/SlotRoomList.vue'

const props = withDefaults(
  defineProps<{
    schema: IndependentSiteCanvasSchema
    // 供 room-list 插槽预览（管理端按发布范围映射的精简房型数据）
    roomTypes?: PublicIndependentSiteRoomType[]
    // 最近一次 AI 修改涉及的顶层区块 id，短暂描边高亮
    highlightIds?: string[]
  }>(),
  {
    roomTypes: () => [],
    highlightIds: () => [],
  },
)

const emit = defineEmits<{
  updateText: [nodeId: string, text: string]
  requestImage: [nodeId: string]
  moveSection: [nodeId: string, offset: number]
  removeSection: [nodeId: string]
  aiRedoSection: [nodeId: string]
}>()

const { t } = useI18n()

// 同一时刻只允许一个文本节点处于编辑态
const editingTextId = ref<string | null>(null)
const editingTextValue = ref('')

const startTextEdit = (node: CanvasTextNode) => {
  editingTextId.value = node.id
  editingTextValue.value = node.text
}

const commitTextEdit = () => {
  const nodeId = editingTextId.value
  editingTextId.value = null
  if (!nodeId) {
    return
  }
  const text = editingTextValue.value.trim()
  // 空文本不合法（契约 1..500），视为放弃本次修改
  if (text) {
    emit('updateText', nodeId, text)
  }
}

const cancelTextEdit = () => {
  editingTextId.value = null
}

// 父组件在关闭编辑器/冲刷保存前调用：强制提交仍在编辑中的文本框，
// 避免已输入内容滞留 textarea（未写回 JSON）而丢失
const commitActiveEdit = () => {
  commitTextEdit()
}

defineExpose({ commitActiveEdit })

const autoSizeTextarea = (el: HTMLTextAreaElement) => {
  el.style.height = 'auto'
  el.style.height = `${el.scrollHeight}px`
}

const renderTextNode = (node: CanvasTextNode): VNodeChild => {
  if (editingTextId.value === node.id) {
    return h('textarea', {
      'data-node-id': node.id,
      class: 'canvas-edit-textarea',
      value: editingTextValue.value,
      rows: 1,
      onInput: (event: Event) => {
        const el = event.target as HTMLTextAreaElement
        editingTextValue.value = el.value
        autoSizeTextarea(el)
      },
      onBlur: commitTextEdit,
      onKeydown: (event: KeyboardEvent) => {
        if (event.key === 'Escape') {
          event.preventDefault()
          cancelTextEdit()
        } else if (event.key === 'Enter' && !event.shiftKey && !event.isComposing) {
          // Enter / Cmd(Ctrl)+Enter 提交；Shift+Enter 换行；IME 组词中的 Enter 不触发提交
          event.preventDefault()
          commitTextEdit()
        }
      },
      onVnodeMounted: (vnode) => {
        const el = vnode.el as HTMLTextAreaElement | null
        if (el) {
          autoSizeTextarea(el)
          el.focus()
          el.select()
        }
      },
    })
  }
  return h(
    'span',
    {
      'data-node-id': node.id,
      class: 'canvas-edit-text',
      title: t('independentSite.canvas.editTextTitle'),
      onClick: (event: MouseEvent) => {
        event.stopPropagation()
        event.preventDefault()
        startTextEdit(node)
      },
    },
    node.text,
  )
}

const renderSlotNode = (node: CanvasSlotNode): VNodeChild => {
  // booking-flow 编辑态只渲染占位块（真实预订流程只在公开页挂载），不可交互
  if (node.slot === 'booking-flow') {
    return h('div', { 'data-node-id': node.id, class: 'canvas-edit-slot-booking' }, [
      h(Calendar, { class: 'canvas-edit-slot-booking-icon' }),
      h(
        'span',
        { class: 'canvas-edit-slot-booking-text' },
        t('independentSite.canvas.bookingSlotPlaceholder'),
      ),
    ])
  }
  return h('div', { 'data-node-id': node.id, class: 'canvas-edit-slot' }, [
    h('div', { class: 'canvas-edit-slot-inner' }, [
      h(SlotRoomList, {
        layout: node.props?.layout ?? 'grid',
        roomTypes: props.roomTypes,
      }),
    ]),
    h(
      'span',
      { class: 'canvas-edit-slot-badge' },
      t('independentSite.canvas.roomListSlotBadge'),
    ),
  ])
}

const blockLabel = (node: CanvasNode): string => {
  if (isCanvasSlotNode(node)) {
    return node.slot === 'booking-flow'
      ? t('independentSite.canvas.bookingSlot')
      : t('independentSite.canvas.roomListSlot')
  }
  if (isCanvasTextNode(node)) {
    return t('independentSite.canvas.textBlock')
  }
  return node.tag
}

// 顶层区块（root 直接子节点）的悬浮工具栏：上移/下移/删除/AI 重做这一块
const renderSectionFrame = (node: CanvasNode, index: number, count: number): VNodeChild => {
  const highlighted = props.highlightIds.includes(node.id)
  const toolbar = h('div', { class: 'canvas-block-toolbar' }, [
    h('span', { class: 'canvas-block-label' }, blockLabel(node)),
    h(
      'button',
      {
        type: 'button',
        disabled: index === 0,
        title: t('independentSite.canvas.moveBlockUp'),
        onClick: (event: MouseEvent) => {
          event.stopPropagation()
          emit('moveSection', node.id, -1)
        },
      },
      t('independentSite.editor.moveUp'),
    ),
    h(
      'button',
      {
        type: 'button',
        disabled: index === count - 1,
        title: t('independentSite.canvas.moveBlockDown'),
        onClick: (event: MouseEvent) => {
          event.stopPropagation()
          emit('moveSection', node.id, 1)
        },
      },
      t('independentSite.editor.moveDown'),
    ),
    h(
      'button',
      {
        type: 'button',
        title: t('independentSite.canvas.redoBlockTitle'),
        onClick: (event: MouseEvent) => {
          event.stopPropagation()
          emit('aiRedoSection', node.id)
        },
      },
      t('independentSite.canvas.redoBlock'),
    ),
    h(
      'button',
      {
        type: 'button',
        class: 'is-danger',
        title: t('independentSite.canvas.deleteBlockTitle'),
        onClick: (event: MouseEvent) => {
          event.stopPropagation()
          emit('removeSection', node.id)
        },
      },
      t('independentSite.common.delete'),
    ),
  ])
  return h(
    'div',
    {
      'data-node-id': node.id,
      class: ['canvas-edit-block', highlighted ? 'is-highlighted' : ''],
    },
    [toolbar, renderNode(node, 1)],
  )
}

const buildElementProps = (node: CanvasElementNode): Record<string, unknown> => {
  const elementProps: Record<string, unknown> = { key: node.id, 'data-node-id': node.id }
  if (node.tag === 'img') {
    elementProps.class = [node.class, 'canvas-edit-img']
    elementProps.src = node.attrs?.src
    elementProps.alt = node.attrs?.alt ?? ''
    elementProps.loading = 'lazy'
    elementProps.decoding = 'async'
    elementProps.title = t('independentSite.canvas.replaceImageTitle')
    elementProps.onClick = (event: MouseEvent) => {
      event.stopPropagation()
      event.preventDefault()
      emit('requestImage', node.id)
    }
    return elementProps
  }
  if (node.class) {
    elementProps.class = node.class
  }
  if (node.tag === 'button') {
    elementProps.type = 'button'
  }
  if (node.tag === 'a') {
    // 编辑态不跳转，仅展示样式
    if (node.attrs?.href) {
      elementProps.href = node.attrs.href
    }
    elementProps.onClick = (event: MouseEvent) => {
      event.preventDefault()
    }
    return elementProps
  }
  if (node.action === 'scroll-to-booking') {
    // 编辑态画布内没有 #booking，拦截滚动动作
    elementProps.onClick = (event: MouseEvent) => {
      event.preventDefault()
    }
  }
  return elementProps
}

// depth 0 为根节点；根的直接子节点包一层悬浮工具栏（顶层区块操作）
const renderNode = (node: CanvasNode, depth: number): VNodeChild => {
  if (isCanvasTextNode(node)) {
    return renderTextNode(node)
  }
  if (isCanvasSlotNode(node)) {
    return renderSlotNode(node)
  }
  if (isCanvasElementNode(node)) {
    const children = node.children?.map((child, index) =>
      depth === 0
        ? renderSectionFrame(child, index, node.children?.length ?? 0)
        : renderNode(child, depth + 1),
    )
    return h(node.tag, buildElementProps(node), children)
  }
  return null
}

// 函数式组件承载递归渲染（同 CanvasRenderer 模式），闭包共享本组件的编辑态
const CanvasNodeView: FunctionalComponent<{ node: CanvasNode }> = (nodeProps) =>
  renderNode(nodeProps.node, 0)
CanvasNodeView.props = ['node']
</script>

<template>
  <div class="canvas-editor-node">
    <CanvasNodeView :node="schema.root" />
  </div>
</template>

<style scoped>
.canvas-editor-node {
  min-height: 40vh;
}

/* h() 渲染的内部 DOM 不带本组件 scopeId，全部用 :deep 命中 */
.canvas-editor-node :deep(.canvas-edit-text) {
  border-radius: 4px;
  cursor: text;
  transition: outline-color 0.15s;
  outline: 1px dashed transparent;
  outline-offset: 2px;
}

.canvas-editor-node :deep(.canvas-edit-text:hover) {
  outline-color: rgba(53, 125, 112, 0.65);
  background: rgba(53, 125, 112, 0.08);
}

.canvas-editor-node :deep(.canvas-edit-textarea) {
  display: block;
  width: 100%;
  min-width: 12em;
  padding: 4px 6px;
  border: 1px dashed #357d70;
  border-radius: 4px;
  background: rgba(255, 255, 255, 0.96);
  color: inherit;
  font: inherit;
  text-align: inherit;
  letter-spacing: inherit;
  resize: none;
  outline: none;
  box-shadow: 0 4px 16px rgba(36, 59, 54, 0.18);
}

.canvas-editor-node :deep(.canvas-edit-img) {
  cursor: pointer;
  outline: 2px dashed transparent;
  outline-offset: -2px;
  transition: outline-color 0.15s;
}

.canvas-editor-node :deep(.canvas-edit-img:hover) {
  outline-color: rgba(53, 125, 112, 0.75);
}

.canvas-editor-node :deep(.canvas-edit-slot) {
  position: relative;
}

.canvas-editor-node :deep(.canvas-edit-slot-inner) {
  pointer-events: none;
}

.canvas-editor-node :deep(.canvas-edit-slot-badge) {
  position: absolute;
  right: 12px;
  bottom: 12px;
  z-index: 2;
  padding: 4px 10px;
  border-radius: 999px;
  background: rgba(32, 35, 34, 0.78);
  color: #fff;
  font-size: 12px;
  letter-spacing: 0.02em;
}

/* booking-flow 插槽编辑态占位：虚线框 + 图标 + 说明，不可交互 */
.canvas-editor-node :deep(.canvas-edit-slot-booking) {
  display: flex;
  gap: 10px;
  align-items: center;
  justify-content: center;
  min-height: 140px;
  margin: 24px;
  padding: 32px;
  border: 1px dashed rgba(53, 125, 112, 0.55);
  border-radius: 12px;
  background: rgba(53, 125, 112, 0.05);
  color: #357d70;
  font-size: 14px;
  pointer-events: none;
}

.canvas-editor-node :deep(.canvas-edit-slot-booking-icon) {
  width: 20px;
  height: 20px;
}

.canvas-editor-node :deep(.canvas-edit-block) {
  position: relative;
  outline: 1px dashed transparent;
  outline-offset: -1px;
  transition: outline-color 0.15s;
}

.canvas-editor-node :deep(.canvas-edit-block:hover) {
  outline-color: rgba(53, 125, 112, 0.4);
}

.canvas-editor-node :deep(.canvas-block-toolbar) {
  position: absolute;
  top: 8px;
  right: 8px;
  z-index: 5;
  display: flex;
  gap: 4px;
  align-items: center;
  padding: 4px 6px;
  border: 1px solid #d7e2de;
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.96);
  box-shadow: 0 6px 20px rgba(36, 59, 54, 0.16);
  opacity: 0;
  pointer-events: none;
  transition: opacity 0.15s;
}

.canvas-editor-node :deep(.canvas-edit-block:hover > .canvas-block-toolbar) {
  opacity: 1;
  pointer-events: auto;
}

.canvas-editor-node :deep(.canvas-block-label) {
  padding: 0 6px;
  color: #5b6c66;
  font-family: 'SFMono-Regular', Consolas, monospace;
  font-size: 11px;
}

.canvas-editor-node :deep(.canvas-block-toolbar button) {
  padding: 3px 8px;
  border: none;
  border-radius: 6px;
  background: transparent;
  color: #1f5f53;
  cursor: pointer;
  font-size: 12px;
  white-space: nowrap;
}

.canvas-editor-node :deep(.canvas-block-toolbar button:hover:not(:disabled)) {
  background: rgba(53, 125, 112, 0.12);
}

.canvas-editor-node :deep(.canvas-block-toolbar button:disabled) {
  color: #a8b5b0;
  cursor: not-allowed;
}

.canvas-editor-node :deep(.canvas-block-toolbar button.is-danger) {
  color: #b4423a;
}

.canvas-editor-node :deep(.canvas-block-toolbar button.is-danger:hover) {
  background: rgba(180, 66, 58, 0.1);
}

/* AI 修改后的区块描边高亮（约 2 秒，由父组件控制时长） */
.canvas-editor-node :deep(.canvas-edit-block.is-highlighted) {
  outline: 2px solid rgba(53, 125, 112, 0.85);
  animation: canvas-block-flash 2s ease-out;
}

@keyframes canvas-block-flash {
  0% {
    outline-color: rgba(53, 125, 112, 0.95);
    background: rgba(53, 125, 112, 0.1);
  }
  100% {
    outline-color: rgba(53, 125, 112, 0);
    background: transparent;
  }
}
</style>
