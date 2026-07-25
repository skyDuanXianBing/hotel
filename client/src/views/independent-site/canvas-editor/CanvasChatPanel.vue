<script setup lang="ts">
// 画布编辑器左侧聊天面板：对话流为本地组件状态（不持久化）。
// 面板只负责收集指令与展示消息，AI 请求由父组件 CanvasEditor 发出；
// AI 成功后父组件通过 expose 的 appendAiDelivery/appendAiError 追加交付消息。
import { nextTick, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { MagicStick, RefreshLeft } from '@element-plus/icons-vue'
import { CANVAS_STYLE_PRESETS, type CanvasStylePreset } from '../canvasStylePresets'

const props = withDefaults(
  defineProps<{
    aiBusy?: boolean
    undoingAi?: boolean
    hasAiBackup?: boolean
  }>(),
  {
    aiBusy: false,
    undoingAi: false,
    hasAiBackup: false,
  },
)

const emit = defineEmits<{
  send: [instruction: string]
  preset: [preset: CanvasStylePreset]
  undo: []
}>()

interface ChatMessage {
  id: number
  role: 'user' | 'ai'
  // delivery：AI 交付摘要（带撤销按钮）；error：AI 失败说明
  kind: 'text' | 'delivery' | 'error'
  text: string
}

let messageSeq = 0
const nextMessageId = () => {
  messageSeq += 1
  return messageSeq
}

const messages = ref<ChatMessage[]>([
  {
    id: nextMessageId(),
    role: 'ai',
    kind: 'text',
    text: '你好，我是 AI 设计助手。点击上方风格卡一键换风格，或直接告诉我想要的修改（例如「把首屏改成秋冬氛围」）；也可以直接点击画布中的文字、图片进行直改，所有修改都会自动保存。',
  },
])

const draft = ref('')
const messageListRef = ref<HTMLElement | null>(null)

const scrollToBottom = async () => {
  await nextTick()
  const el = messageListRef.value
  if (el) {
    el.scrollTop = el.scrollHeight
  }
}

const pushMessage = (message: Omit<ChatMessage, 'id'>) => {
  messages.value.push({ id: nextMessageId(), ...message })
  void scrollToBottom()
}

const appendUserMessage = (text: string) => pushMessage({ role: 'user', kind: 'text', text })

const appendAiDelivery = (summary: string) =>
  pushMessage({ role: 'ai', kind: 'delivery', text: summary })

const appendAiError = (message: string) =>
  pushMessage({ role: 'ai', kind: 'error', text: message })

defineExpose({ appendUserMessage, appendAiDelivery, appendAiError })

const handleSend = () => {
  const instruction = draft.value.trim()
  if (!instruction) {
    return
  }
  if (props.aiBusy) {
    ElMessage.warning('AI 正在处理上一条指令，请稍候')
    return
  }
  if (instruction.length > 2000) {
    ElMessage.warning('修改指令不能超过 2000 个字符')
    return
  }
  pushMessage({ role: 'user', kind: 'text', text: instruction })
  draft.value = ''
  emit('send', instruction)
}

const handlePreset = (preset: CanvasStylePreset) => {
  if (props.aiBusy) {
    ElMessage.warning('AI 正在处理上一条指令，请稍候')
    return
  }
  pushMessage({ role: 'user', kind: 'text', text: `使用「${preset.name}」风格` })
  emit('preset', preset)
}
</script>

<template>
  <div class="canvas-chat-panel">
    <header class="chat-header">
      <div class="chat-title">
        <el-icon><MagicStick /></el-icon>
        <span>AI 设计助手</span>
      </div>
      <el-button
        size="small"
        :icon="RefreshLeft"
        :loading="undoingAi"
        :disabled="!hasAiBackup || aiBusy"
        title="恢复最近一次 AI 修改前的草稿"
        @click="emit('undo')"
      >
        撤销 AI 修改
      </el-button>
    </header>

    <div class="preset-area">
      <p class="preset-hint">风格预设</p>
      <div class="preset-grid">
        <button
          v-for="preset in CANVAS_STYLE_PRESETS"
          :key="preset.id"
          type="button"
          class="preset-card"
          :disabled="aiBusy"
          @click="handlePreset(preset)"
        >
          <span class="preset-name">{{ preset.name }}</span>
          <span class="preset-desc">{{ preset.description }}</span>
        </button>
      </div>
    </div>

    <div ref="messageListRef" class="message-list">
      <div
        v-for="message in messages"
        :key="message.id"
        class="message-row"
        :class="`is-${message.role}`"
      >
        <div class="message-bubble" :class="`is-${message.kind}`">
          <p class="message-text">{{ message.text }}</p>
          <div v-if="message.kind === 'delivery'" class="message-actions">
            <el-button
              size="small"
              :icon="RefreshLeft"
              :loading="undoingAi"
              :disabled="!hasAiBackup || aiBusy"
              @click="emit('undo')"
            >
              撤销这次
            </el-button>
          </div>
        </div>
      </div>
      <div v-if="aiBusy" class="message-row is-ai">
        <div class="message-bubble is-pending">
          <p class="message-text">AI 正在修改页面，通常需要几十秒…</p>
        </div>
      </div>
    </div>

    <footer class="chat-input">
      <el-input
        v-model="draft"
        type="textarea"
        :rows="2"
        maxlength="2000"
        resize="none"
        placeholder="描述想要的修改，Enter 发送"
        :disabled="aiBusy"
        @keyup.enter.exact="handleSend"
      />
      <el-button
        type="primary"
        :icon="MagicStick"
        :loading="aiBusy"
        :disabled="!draft.trim()"
        @click="handleSend"
      >
        发送
      </el-button>
    </footer>
  </div>
</template>

<style scoped>
.canvas-chat-panel {
  display: flex;
  flex-direction: column;
  width: 100%;
  height: 100%;
  min-height: 0;
  background: #f7f9f8;
}

.chat-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  padding: 12px 14px;
  border-bottom: 1px solid #e5eae8;
}

.chat-title {
  display: flex;
  gap: 6px;
  align-items: center;
  color: #173c36;
  font-size: 14px;
  font-weight: 700;
}

.preset-area {
  padding: 10px 14px 12px;
  border-bottom: 1px solid #e5eae8;
}

.preset-hint {
  margin: 0 0 8px;
  color: #69716f;
  font-size: 12px;
}

.preset-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
}

.preset-card {
  display: grid;
  gap: 3px;
  padding: 8px 10px;
  border: 1px solid #dce5e2;
  border-radius: 10px;
  background: #fff;
  cursor: pointer;
  text-align: left;
  transition:
    border-color 0.15s,
    box-shadow 0.15s;
}

.preset-card:hover:not(:disabled) {
  border-color: #357d70;
  box-shadow: 0 4px 12px rgba(36, 59, 54, 0.1);
}

.preset-card:disabled {
  cursor: not-allowed;
  opacity: 0.6;
}

.preset-name {
  color: #173c36;
  font-size: 13px;
  font-weight: 700;
}

.preset-desc {
  color: #69716f;
  font-size: 11px;
  line-height: 1.4;
}

.message-list {
  flex: 1;
  min-height: 0;
  padding: 14px;
  overflow-y: auto;
}

.message-row {
  display: flex;
  margin-bottom: 10px;
}

.message-row.is-user {
  justify-content: flex-end;
}

.message-bubble {
  max-width: 88%;
  padding: 9px 12px;
  border-radius: 12px;
  font-size: 13px;
  line-height: 1.55;
  word-break: break-word;
}

.message-row.is-ai .message-bubble {
  border: 1px solid #e2e9e6;
  border-top-left-radius: 4px;
  background: #fff;
  color: #2b3431;
}

.message-row.is-user .message-bubble {
  border-top-right-radius: 4px;
  background: #214e46;
  color: #fff;
}

.message-bubble.is-error {
  border-color: #f0c9c4;
  background: #fdf1ef;
  color: #8f3a32;
}

.message-bubble.is-pending {
  color: #69716f;
}

.message-text {
  margin: 0;
  white-space: pre-wrap;
}

.message-actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 8px;
}

.chat-input {
  display: flex;
  gap: 8px;
  align-items: flex-end;
  padding: 12px 14px;
  border-top: 1px solid #e5eae8;
  background: #fff;
}

.chat-input :deep(.el-textarea__inner) {
  box-shadow: 0 0 0 1px #dce5e2 inset;
}
</style>
