<template>
  <ion-page>
    <ion-header translucent>
      <ion-toolbar class="message-detail-header">
        <ion-buttons slot="start">
          <ion-back-button class="message-detail-header__back" :default-href="ROUTE_PATHS.messages" />
        </ion-buttons>
        <ion-title class="message-detail-header__title">
          <span>{{ pageTitle }}</span>
          <small v-if="headerCaption">{{ headerCaption }}</small>
        </ion-title>
        <ion-buttons slot="end">
          <ion-button
            v-if="reservationMatched"
            class="message-detail-header__action"
            fill="clear"
            @click="handleOpenReservation"
          >
            订单
          </ion-button>
        </ion-buttons>
      </ion-toolbar>
    </ion-header>

    <ion-content ref="contentRef" fullscreen class="mobile-page message-detail-page">
      <ion-refresher slot="fixed" @ionRefresh="handleRefresh">
        <ion-refresher-content pulling-text="下拉刷新会话" refreshing-spinner="crescent" />
      </ion-refresher>

      <div v-if="loadNotice || loading" class="message-detail-page__status">
        <ion-spinner v-if="loading" name="crescent" />
        <span>{{ loadNotice || '同步中...' }}</span>
      </div>

      <section v-if="messages.length > 0" class="message-thread-stream">
        <article
          v-for="message in messages"
          :key="message.id"
          class="message-row"
          :class="message.senderType === MessageSenderType.STAFF ? 'is-staff' : 'is-guest'"
        >
          <div class="message-row__avatar" aria-hidden="true">
            {{ resolveMessageAvatarLabel(message) }}
          </div>
          <div class="message-row__body">
            <div class="message-bubble">
              <p class="message-bubble__text">{{ message.content }}</p>
            </div>
            <div class="message-row__meta">
              <span>{{ formatDateTime(message.timestamp) }}</span>
              <span v-if="message.deliveryStatus === 'FAILED'" class="message-row__failed">发送失败</span>
            </div>
          </div>
        </article>
      </section>

      <section v-else-if="!loading" class="message-detail-empty-state">
        <h3>暂无消息</h3>
        <p class="mobile-note">该会话还没有聊天记录，可以先发送一条消息。</p>
      </section>

      <div slot="fixed" class="message-composer">
        <p v-if="activeThread?.closed" class="message-composer__closed-tip">当前会话已关闭，不能继续发送消息。</p>
        <div class="message-composer__bar">
          <ion-button
            class="message-composer__ai"
            fill="solid"
            :disabled="!activeThread || activeThread.closed"
            @click="handleOpenAiDraft"
          >
            AI
          </ion-button>
          <div class="message-composer__input" @click="handleFocusComposer">
            <ion-textarea
              ref="composerTextareaRef"
              v-model="composerValue"
              auto-grow
              :rows="1"
              class="message-composer__textarea"
              placeholder="输入给住客的消息"
              :disabled="sending || !activeThread || activeThread.closed"
            />
          </div>
          <ion-button
            class="message-composer__send"
            :disabled="sending || !composerValue.trim() || !activeThread || activeThread.closed"
            @click="handleSendMessage"
          >
            {{ sending ? '发送中' : '发送' }}
          </ion-button>
        </div>
      </div>

      <ion-modal class="message-ai-modal" :is-open="aiDraftOpen" @didDismiss="handleDismissAiDraft">
        <ion-header translucent class="message-ai-modal__header">
          <ion-toolbar class="message-ai-modal__toolbar">
            <ion-title>AI 回复草稿</ion-title>
            <ion-buttons slot="end">
              <ion-button @click="handleDismissAiDraft">关闭</ion-button>
            </ion-buttons>
          </ion-toolbar>
        </ion-header>

        <ion-content class="mobile-page message-draft-modal-page message-ai-modal__page">
          <section class="mobile-card message-draft-card message-ai-sheet">
            <label class="message-editor-card__field message-ai-field">
              <span>上下文摘要</span>
              <ion-textarea class="message-ai-textarea message-ai-textarea--readonly" :value="aiContextSummary" :rows="4" fill="outline" readonly />
            </label>

            <label class="message-editor-card__field message-ai-field">
              <span>补充要求</span>
              <ion-textarea
                v-model="aiInstruction"
                :rows="3"
                class="message-ai-textarea"
                fill="outline"
                placeholder="例如：更礼貌、加入入住步骤、简短一些"
              />
            </label>

            <label class="message-editor-card__field message-ai-field">
              <span>回复草稿</span>
              <ion-textarea v-model="aiDraft" :rows="8" fill="outline" placeholder="AI 草稿会显示在这里" />
            </label>

            <div class="message-editor-card__actions">
              <ion-button class="message-ai-button message-ai-button--secondary" fill="outline" :disabled="draftLoading" @click="handleGenerateAiDraft">
                {{ draftLoading ? '生成中...' : '重新生成' }}
              </ion-button>
              <ion-button
                class="message-ai-button message-ai-button--secondary"
                fill="outline"
                :disabled="draftLoading || !aiDraft.trim()"
                @click="handleOpenAiPolish"
              >
                继续优化
              </ion-button>
              <ion-button
                class="message-ai-button message-ai-button--secondary"
                fill="outline"
                :disabled="draftLoading || !aiDraft.trim()"
                @click="handleUseAiDraft"
              >
                回填输入框
              </ion-button>
              <ion-button class="message-ai-button message-ai-button--primary" :disabled="draftLoading || !aiDraft.trim()" @click="handleSendAiDraft">
                发送草稿
              </ion-button>
            </div>
          </section>
        </ion-content>
      </ion-modal>

      <ion-modal class="message-ai-modal" :is-open="aiPolishOpen" @didDismiss="handleDismissAiPolish">
        <ion-header translucent class="message-ai-modal__header">
          <ion-toolbar class="message-ai-modal__toolbar">
            <ion-title>继续优化 AI 草稿</ion-title>
            <ion-buttons slot="end">
              <ion-button @click="handleDismissAiPolish">完成</ion-button>
            </ion-buttons>
          </ion-toolbar>
        </ion-header>

        <ion-content class="mobile-page message-draft-modal-page message-ai-modal__page">
          <section class="mobile-card message-draft-card message-ai-sheet">
            <div class="message-ai-sheet__intro">
              <h2 class="mobile-section-title">优化历史</h2>
              <p class="mobile-note">保留本轮优化指令与 AI 返回结果，方便继续微调。</p>
            </div>

            <div v-if="aiPolishHistory.length > 0" class="ai-polish-history-list">
              <article
                v-for="(item, index) in aiPolishHistory"
                :key="`${item.role}-${index}`"
                :class="item.role === 'user' ? 'ai-polish-history-item is-user' : 'ai-polish-history-item is-assistant'"
              >
                <strong>{{ item.role === 'user' ? '你' : 'AI' }}</strong>
                <p>{{ item.content }}</p>
              </article>
            </div>
            <p v-else class="mobile-note">输入你希望优化的方向，例如更礼貌、更简短或增加入住步骤说明。</p>

            <label class="message-editor-card__field message-ai-field">
              <span>优化要求</span>
              <ion-textarea
                v-model="aiPolishInstruction"
                :rows="4"
                class="message-ai-textarea"
                fill="outline"
                placeholder="告诉 AI 你希望怎样改写当前草稿"
              />
            </label>

            <div class="message-editor-card__actions">
              <ion-button fill="outline" :disabled="aiPolishLoading" @click="handleDismissAiPolish">关闭</ion-button>
              <ion-button :disabled="aiPolishLoading || !aiPolishInstruction.trim()" @click="handlePolishAiDraft">
                {{ aiPolishLoading ? '优化中...' : '生成改进版并回填' }}
              </ion-button>
            </div>
          </section>
        </ion-content>
      </ion-modal>
    </ion-content>
  </ion-page>
</template>

<script setup lang="ts">
import {
  IonBackButton,
  IonButton,
  IonButtons,
  IonContent,
  IonHeader,
  IonModal,
  IonPage,
  IonRefresher,
  IonRefresherContent,
  IonSpinner,
  IonTextarea,
  IonTitle,
  IonToolbar,
  onIonViewWillEnter,
  onIonViewWillLeave,
} from '@ionic/vue'
import { computed, nextTick, onUnmounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  getThreadMessages,
  getMessageThreads,
  MESSAGE_API_MOCK_ENABLED,
  pollThreadMessages,
  sendAiChatMessage,
  sendThreadMessage,
} from '@/api/message'
import { getReservationsWithFilters, type ReservationDTO } from '@/api/reservation'
import { ROUTE_PATHS } from '@/router/guards'
import { useNotificationCenterStore } from '@/stores/notificationCenter'
import type { MessageDTO, MessageThreadDTO } from '@/types/message'
import { MessageSenderType } from '@/types/message'
import { isHandledRequestError } from '@/utils/request'
import { showSuccessToast, showWarningToast } from '@/utils/notify'

const MESSAGE_POLL_INTERVAL = 8000

interface AiPolishHistoryItem {
  role: 'user' | 'assistant'
  content: string
}

interface IonContentScrollTarget {
  scrollToBottom(duration?: number): Promise<void>
}

interface IonTextareaFocusTarget {
  setFocus?: () => Promise<void>
  getInputElement?: () => Promise<HTMLInputElement | HTMLTextAreaElement>
}

const route = useRoute()
const router = useRouter()
const notificationCenterStore = useNotificationCenterStore()

const loading = ref(false)
const sending = ref(false)
const draftLoading = ref(false)
const loadNotice = ref('')
const composerValue = ref('')
const messages = ref<MessageDTO[]>([])
const threads = ref<MessageThreadDTO[]>([])
const reservationId = ref<number | null>(null)
const aiDraftOpen = ref(false)
const aiDraft = ref('')
const aiContextSummary = ref('')
const aiInstruction = ref('')
const aiSessionId = ref('')
const aiPolishOpen = ref(false)
const aiPolishLoading = ref(false)
const aiPolishInstruction = ref('')
const aiPolishHistory = ref<AiPolishHistoryItem[]>([])
const contentRef = ref<unknown>(null)
const composerTextareaRef = ref<unknown>(null)

let pollTimer = 0

const threadId = computed(() => Number(route.params.threadId || 0))

function hasValidThreadId(value: number) {
  return Number.isInteger(value) && value > 0
}

const activeThread = computed(() => {
  for (const item of threads.value) {
    if (item.id === threadId.value) {
      return item
    }
  }

  return null
})

const pageTitle = computed(() => {
  if (!activeThread.value) {
    return '消息详情'
  }

  return threadTitle.value
})

const threadTitle = computed(() => {
  if (!activeThread.value) {
    return '消息详情'
  }

  if (activeThread.value.guestName) {
    return activeThread.value.guestName
  }

  if (activeThread.value.listingName) {
    return activeThread.value.listingName
  }

  return activeThread.value.channelName || '会话详情'
})

const reservationMatched = computed(() => reservationId.value !== null)

const headerCaption = computed(() => {
  if (!activeThread.value) {
    return ''
  }

  const parts: string[] = []

  if (activeThread.value.channelName) {
    parts.push(activeThread.value.channelName)
  }

  parts.push(activeThread.value.closed ? '已关闭' : '会话进行中')

  if (reservationMatched.value) {
    parts.push('已关联订单')
  }

  return parts.join(' · ')
})

function resolveWarningMessage(error: unknown, fallbackMessage: string) {
  if (error instanceof Error && error.message) {
    return error.message
  }

  return fallbackMessage
}

function sortMessages(list: MessageDTO[]) {
  const nextItems = [...list]
  nextItems.sort((firstItem, secondItem) => {
    return new Date(firstItem.timestamp).getTime() - new Date(secondItem.timestamp).getTime()
  })
  return nextItems
}

function isIonContentScrollTarget(value: unknown): value is IonContentScrollTarget {
  return Boolean(value) && typeof (value as IonContentScrollTarget).scrollToBottom === 'function'
}

function resolveContentScrollTarget() {
  const contentValue = contentRef.value as { $el?: unknown } | null

  if (isIonContentScrollTarget(contentValue)) {
    return contentValue
  }

  if (contentValue && isIonContentScrollTarget(contentValue.$el)) {
    return contentValue.$el
  }

  return null
}

function isIonTextareaFocusTarget(value: unknown): value is IonTextareaFocusTarget {
  return Boolean(value) && (typeof (value as IonTextareaFocusTarget).setFocus === 'function' || typeof (value as IonTextareaFocusTarget).getInputElement === 'function')
}

function resolveComposerTextareaTarget() {
  const textareaValue = composerTextareaRef.value as { $el?: unknown } | null

  if (isIonTextareaFocusTarget(textareaValue)) {
    return textareaValue
  }

  if (textareaValue?.$el && isIonTextareaFocusTarget(textareaValue.$el)) {
    return textareaValue.$el
  }

  return null
}

async function focusComposerInput() {
  if (sending.value || !activeThread.value || activeThread.value.closed) {
    return
  }

  await nextTick()

  const target = resolveComposerTextareaTarget()
  if (!target) {
    return
  }

  try {
    if (target.setFocus) {
      await target.setFocus()
      return
    }

    if (target.getInputElement) {
      const input = await target.getInputElement()
      input.focus()
    }
  } catch {
    // Ignore focus failures caused by page transition timing.
  }
}

function handleFocusComposer() {
  void focusComposerInput()
}

async function scrollToConversationBottom(duration = 0) {
  await nextTick()

  const scrollTarget = resolveContentScrollTarget()
  if (!scrollTarget) {
    return
  }

  try {
    await scrollTarget.scrollToBottom(duration)
  } catch {
    // Ignore scroll failures caused by page transition timing.
  }
}

function resolveSenderLabel(message: MessageDTO) {
  if (message.senderType === MessageSenderType.STAFF) {
    return message.senderName || '酒店客服'
  }

  return message.senderName || '住客'
}

function resolveAvatarCharacter(source: string, fallbackValue: string) {
  const normalized = source.trim()
  if (!normalized) {
    return fallbackValue
  }

  const firstCharacter = normalized.slice(0, 1)
  if (/[a-z]/i.test(firstCharacter)) {
    return firstCharacter.toUpperCase()
  }

  return firstCharacter
}

function resolveMessageAvatarLabel(message: MessageDTO) {
  if (message.senderType === MessageSenderType.STAFF) {
    return resolveAvatarCharacter(message.senderName || '店', '店')
  }

  return resolveAvatarCharacter(resolveSenderLabel(message), '客')
}

function formatDateTime(value: string) {
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return '时间未知'
  }

  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hours = String(date.getHours()).padStart(2, '0')
  const minutes = String(date.getMinutes()).padStart(2, '0')
  return `${month}-${day} ${hours}:${minutes}`
}

function buildConversationContext() {
  const historyLines: string[] = []
  const recentMessages = messages.value.slice(-20)

  for (const message of recentMessages) {
    const role = message.senderType === MessageSenderType.STAFF ? '客服' : '住客'
    historyLines.push(`[${role}] ${message.content}`)
  }

  const header = [
    `渠道：${activeThread.value?.channelName || '-'}`,
    `订单号：${activeThread.value?.bookingId || activeThread.value?.threadId || '-'}`,
    `会话状态：${activeThread.value?.closed ? '已关闭' : '活跃'}`,
  ].join('；')

  return `${header}\n\n最近会话：\n${historyLines.join('\n') || '暂无历史消息'}`
}

function parseAiDraft(rawReply: string) {
  const contextMatch = rawReply.match(/\[CONTEXT\]([\s\S]*?)\[\/CONTEXT\]/i)
  const draftMatch = rawReply.match(/\[DRAFT\]([\s\S]*?)\[\/DRAFT\]/i)

  return {
    contextSummary: contextMatch?.[1]?.trim() || '',
    draftReply: draftMatch?.[1]?.trim() || rawReply.trim(),
  }
}

function getLatestTimestamp() {
  if (messages.value.length === 0) {
    return ''
  }

  return messages.value[messages.value.length - 1].timestamp
}

async function loadThreads() {
  const response = await getMessageThreads()
  if (!response.success || !response.data) {
    throw new Error(response.message || '加载会话失败')
  }

  threads.value = response.data
  notificationCenterStore.syncUnreadMessageCount(threads.value)
}

async function loadMessages() {
  const response = await getThreadMessages(threadId.value)
  if (!response.success || !response.data) {
    throw new Error(response.message || '加载消息失败')
  }

  messages.value = sortMessages(response.data)
}

async function resolveReservationId() {
  reservationId.value = null
  if (MESSAGE_API_MOCK_ENABLED) {
    return
  }

  if (!activeThread.value) {
    return
  }

  const keyword = (activeThread.value.bookingId || activeThread.value.threadId || '').trim()
  if (!keyword) {
    return
  }

  const response = await getReservationsWithFilters({
    page: 0,
    size: 20,
    searchKeyword: keyword,
  })

  if (!response.success || !response.data) {
    return
  }

  const items = response.data.content || []
  let matchedReservation: ReservationDTO | null = null

  for (const item of items) {
    if (item.channelOrderNumber === keyword || item.orderNumber === keyword) {
      matchedReservation = item
      break
    }
  }

  if (!matchedReservation && items.length > 0) {
    matchedReservation = items[0]
  }

  if (matchedReservation) {
    reservationId.value = matchedReservation.id
  }
}

function stopPolling() {
  if (pollTimer) {
    window.clearInterval(pollTimer)
    pollTimer = 0
  }
}

function startPolling() {
  stopPolling()

  const currentThreadId = threadId.value
  if (!hasValidThreadId(currentThreadId)) {
    return
  }

  pollTimer = window.setInterval(async () => {
    const activeThreadId = threadId.value
    if (!hasValidThreadId(activeThreadId) || activeThreadId !== currentThreadId) {
      stopPolling()
      return
    }

    const latestTimestamp = getLatestTimestamp()
    if (!latestTimestamp) {
      return
    }

    try {
      const response = await pollThreadMessages(activeThreadId, latestTimestamp)
      if (!response.success || !response.data || response.data.length === 0) {
        return
      }

      const existing = [...messages.value]
      for (const incoming of response.data) {
        let exists = false
        for (const item of existing) {
          if (item.id === incoming.id) {
            exists = true
            break
          }
        }

        if (!exists) {
          existing.push(incoming)
        }
      }

      messages.value = sortMessages(existing)
      await loadThreads()
      await scrollToConversationBottom(180)
      if (!activeThread.value) {
        stopPolling()
      }
    } catch {
      stopPolling()
    }
  }, MESSAGE_POLL_INTERVAL)
}

async function loadPage() {
  if (!hasValidThreadId(threadId.value)) {
    stopPolling()
    loadNotice.value = '缺少会话编号'
    return
  }

  loading.value = true
  loadNotice.value = ''

  try {
    await loadThreads()
    await loadMessages()
    await resolveReservationId()
    startPolling()
    await scrollToConversationBottom()
  } catch (error) {
    loadNotice.value = resolveWarningMessage(error, '会话加载失败')
    if (!isHandledRequestError(error)) {
      showWarningToast(loadNotice.value)
    }
  } finally {
    loading.value = false
  }
}

async function handleSendMessage() {
  const content = composerValue.value.trim()
  if (!content || !threadId.value) {
    return
  }

  sending.value = true
  try {
    const response = await sendThreadMessage(threadId.value, {
      content,
      senderName: '酒店客服',
    })
    if (!response.success || !response.data) {
      throw new Error(response.message || '发送消息失败')
    }

    composerValue.value = ''
    messages.value = sortMessages([...messages.value, response.data])
    await loadThreads()
    await scrollToConversationBottom(180)
    showSuccessToast('消息已发送')
  } catch (error) {
    if (!isHandledRequestError(error)) {
      showWarningToast(resolveWarningMessage(error, '发送消息失败'))
    }
  } finally {
    sending.value = false
  }
}

async function handleOpenReservation() {
  if (!reservationId.value) {
    showWarningToast('当前会话未匹配到订单')
    return
  }

  await router.push({
    name: 'OrderReservationDetail',
    params: { reservationId: reservationId.value },
    query: { defaultHref: ROUTE_PATHS.messages },
  })
}

async function handleGenerateAiDraft() {
  if (!activeThread.value || activeThread.value.closed) {
    showWarningToast('当前会话不可生成 AI 草稿')
    return
  }

  draftLoading.value = true
  try {
    const promptLines = [
      '你是酒店客服 AI 助手，请根据会话生成一版可直接发送给住客的回复。',
      '请严格输出以下格式：',
      '[CONTEXT]',
      '这里输出问题摘要',
      '[/CONTEXT]',
      '[DRAFT]',
      '这里输出回复正文',
      '[/DRAFT]',
      '',
      '要求：语气专业、简洁、友好，不要编造未确认事实。',
    ]

    if (aiInstruction.value.trim()) {
      promptLines.push(`补充要求：${aiInstruction.value.trim()}`)
    }

    promptLines.push('', '会话上下文：', buildConversationContext())

    const response = await sendAiChatMessage({
      sessionId: aiSessionId.value || undefined,
      message: promptLines.join('\n'),
    })
    if (!response.success || !response.data?.reply) {
      throw new Error(response.message || '生成 AI 草稿失败')
    }

    aiSessionId.value = response.data.sessionId || aiSessionId.value
    const parsed = parseAiDraft(response.data.reply)
    aiContextSummary.value = parsed.contextSummary || '已根据最近会话生成上下文摘要。'
    aiDraft.value = parsed.draftReply
  } catch (error) {
    if (!isHandledRequestError(error)) {
      showWarningToast(resolveWarningMessage(error, '生成 AI 草稿失败'))
    }
  } finally {
    draftLoading.value = false
  }
}

async function handleOpenAiDraft() {
  aiDraftOpen.value = true
  if (!aiDraft.value.trim()) {
    await handleGenerateAiDraft()
  }
}

function handleUseAiDraft() {
  composerValue.value = aiDraft.value.trim()
  aiDraftOpen.value = false
  aiPolishOpen.value = false
}

async function handleSendAiDraft() {
  const draft = aiDraft.value.trim()
  if (!draft) {
    return
  }

  composerValue.value = draft
  aiDraftOpen.value = false
  aiPolishOpen.value = false
  await handleSendMessage()
}

function handleDismissAiDraft() {
  aiDraftOpen.value = false
  aiPolishOpen.value = false
}

function handleOpenAiPolish() {
  if (!aiDraft.value.trim()) {
    showWarningToast('请先生成 AI 草稿')
    return
  }

  aiPolishOpen.value = true
}

function handleDismissAiPolish() {
  aiPolishOpen.value = false
}

async function handlePolishAiDraft() {
  const instruction = aiPolishInstruction.value.trim()
  if (!instruction) {
    showWarningToast('请输入优化要求')
    return
  }

  if (!aiDraft.value.trim()) {
    showWarningToast('当前没有可优化的草稿')
    return
  }

  aiPolishLoading.value = true
  aiPolishHistory.value.push({
    role: 'user',
    content: instruction,
  })

  try {
    const promptLines = [
      '你是酒店客服改写助手，请根据要求改进下面这条客服回复草稿。',
      '请直接返回可发送给住客的完整回复正文，不要附加解释。',
      '',
      `会话上下文总结：${aiContextSummary.value || '暂无上下文总结'}`,
      '',
      '当前草稿：',
      aiDraft.value,
      '',
      '改写要求：',
      instruction,
    ]

    const response = await sendAiChatMessage({
      sessionId: aiSessionId.value || undefined,
      message: promptLines.join('\n'),
    })
    if (!response.success || !response.data?.reply) {
      throw new Error(response.message || '优化 AI 草稿失败')
    }

    aiSessionId.value = response.data.sessionId || aiSessionId.value
    aiDraft.value = response.data.reply.trim()
    aiPolishHistory.value.push({
      role: 'assistant',
      content: aiDraft.value,
    })
    aiPolishInstruction.value = ''
  } catch (error) {
    if (!isHandledRequestError(error)) {
      showWarningToast(resolveWarningMessage(error, '优化 AI 草稿失败'))
    }
  } finally {
    aiPolishLoading.value = false
  }
}

async function handleRefresh(event: CustomEvent) {
  try {
    await loadPage()
  } finally {
    event.detail.complete()
  }
}

onIonViewWillEnter(async () => {
  await loadPage()
})

onIonViewWillLeave(() => {
  stopPolling()
})

onUnmounted(() => {
  stopPolling()
})
</script>

<style scoped>
.message-detail-page {
  display: block;
  --padding-top: 8px;
  --padding-start: 0;
  --padding-end: 0;
  --padding-bottom: calc(118px + var(--app-safe-bottom));
  --background: linear-gradient(180deg, #f7f8fa 0%, #f1f3f6 100%);
}

ion-header {
  backdrop-filter: blur(16px);
}

ion-header::after {
  display: none;
}

.message-detail-header {
  --background: rgba(248, 249, 251, 0.92);
  --border-width: 0;
  --padding-start: 6px;
  --padding-end: 6px;
}

.message-detail-header__back {
  --color: #1b2330;
}

.message-detail-header__title {
  color: #1b2330;
  text-align: center;
}

.message-detail-header__title span,
.message-detail-header__title small {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.message-detail-header__title span {
  font-size: 17px;
  font-weight: 700;
  letter-spacing: -0.03em;
}

.message-detail-header__title small {
  margin-top: 2px;
  color: #98a1af;
  font-size: 11px;
  font-weight: 500;
  line-height: 1.2;
}

.message-detail-header__action {
  --color: #536074;
  --padding-start: 8px;
  --padding-end: 8px;
  font-size: 14px;
  font-weight: 600;
}

.message-detail-page__status {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 2px 16px 10px;
  color: #8d97a8;
  font-size: 12px;
  line-height: 1.4;
}

.message-detail-page__status ion-spinner {
  width: 14px;
  height: 14px;
  color: #8d97a8;
}

.message-thread-stream {
  display: grid;
  gap: 14px;
  padding: 4px 12px 28px;
}

.message-row {
  display: flex;
  align-items: flex-end;
  gap: 10px;
}

.message-row.is-staff {
  flex-direction: row-reverse;
}

.message-row__avatar {
  flex-shrink: 0;
  display: grid;
  place-items: center;
  width: 36px;
  height: 36px;
  border-radius: 12px;
  color: #ffffff;
  font-size: 17px;
  font-weight: 700;
  letter-spacing: -0.03em;
  box-shadow: 0 10px 22px rgba(15, 23, 42, 0.12);
}

.message-row.is-guest .message-row__avatar {
  background: linear-gradient(180deg, #84e1d0 0%, #3bc1a8 100%);
}

.message-row.is-staff .message-row__avatar {
  background: linear-gradient(180deg, #b4d786 0%, #8acb51 100%);
  color: #17301a;
}

.message-row__body {
  display: grid;
  gap: 6px;
  max-width: min(78%, 420px);
}

.message-row.is-staff .message-row__body {
  justify-items: end;
}

.message-bubble {
  padding: 12px 14px;
  border-radius: 18px;
  background: #ffffff;
  color: #1b2330;
  box-shadow: 0 10px 24px rgba(15, 23, 42, 0.08);
}

.message-row.is-guest .message-bubble {
  border-bottom-left-radius: 6px;
}

.message-row.is-staff .message-bubble {
  background: linear-gradient(180deg, #d7f3bb 0%, #c8ec9c 100%);
  border-bottom-right-radius: 6px;
  box-shadow: 0 12px 24px rgba(110, 169, 75, 0.2);
}

.message-bubble__text {
  margin: 0;
  white-space: pre-wrap;
  line-height: 1.56;
  font-size: 15px;
}

.message-row__meta {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 0 4px;
  color: #9ba4b3;
  font-size: 11px;
  line-height: 1;
}

.message-row__failed {
  color: #d45d52;
  font-weight: 600;
}

.message-detail-empty-state {
  display: grid;
  place-content: center;
  justify-items: center;
  gap: 8px;
  padding: 32px 24px 0;
  text-align: center;
  color: #7f8898;
}

.message-detail-empty-state h3 {
  margin: 0;
  color: #243042;
  font-size: 20px;
  font-weight: 700;
  letter-spacing: -0.03em;
}

.message-detail-empty-state p {
  margin: 0;
  max-width: 240px;
  line-height: 1.6;
}

.message-composer {
  left: 0;
  right: 0;
  bottom: 0;
  padding: 10px 12px calc(12px + var(--app-safe-bottom));
  background: linear-gradient(180deg, rgba(241, 243, 246, 0) 0%, rgba(241, 243, 246, 0.94) 28%, #f1f3f6 100%);
}

.message-composer__closed-tip {
  margin: 0 6px 8px;
  color: #d9962e;
  font-size: 12px;
  line-height: 1.45;
}

.message-composer__bar {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  gap: 8px;
  align-items: flex-end;
  padding: 8px;
  border: 1px solid rgba(218, 223, 232, 0.9);
  border-radius: 26px;
  background: rgba(255, 255, 255, 0.94);
  backdrop-filter: blur(16px);
  box-shadow: 0 16px 32px rgba(15, 23, 42, 0.08);
}

.message-composer__ai,
.message-composer__send {
  margin: 0;
  min-height: 40px;
  font-size: 14px;
  font-weight: 700;
}

.message-composer__ai {
  --color: #2f6bff;
  --background: #eef3ff;
  --border-radius: 18px;
  min-width: 48px;
}

.message-composer__input {
  min-width: 0;
  padding: 0 2px;
  border-radius: 20px;
  background: #f5f7fa;
  overflow: hidden;
}

.message-composer__textarea {
  margin: 0;
  --composer-textarea-max-height: 136px;
  --background: transparent;
  --padding-start: 12px;
  --padding-end: 12px;
  --padding-top: 10px;
  --padding-bottom: 10px;
  --color: #182231;
  --placeholder-color: #a0a8b6;
}

.message-composer__textarea :deep(.textarea-wrapper),
.message-composer__textarea :deep(.textarea-wrapper-inner),
.message-composer__textarea :deep(.native-wrapper) {
  max-height: var(--composer-textarea-max-height);
}

.message-composer__textarea :deep(.native-wrapper) {
  overflow: hidden;
}

.message-composer__textarea :deep(.native-textarea.sc-ion-textarea-ios),
.message-composer__textarea :deep(.native-textarea.sc-ion-textarea-md) {
  max-height: var(--composer-textarea-max-height);
  overflow-y: auto !important;
  overscroll-behavior: contain;
  -webkit-overflow-scrolling: touch;
  scrollbar-width: thin;
  scrollbar-color: rgba(139, 148, 164, 0.72) transparent;
}

.message-composer__textarea :deep(.native-textarea.sc-ion-textarea-ios::-webkit-scrollbar),
.message-composer__textarea :deep(.native-textarea.sc-ion-textarea-md::-webkit-scrollbar) {
  width: 6px;
}

.message-composer__textarea :deep(.native-textarea.sc-ion-textarea-ios::-webkit-scrollbar-track),
.message-composer__textarea :deep(.native-textarea.sc-ion-textarea-md::-webkit-scrollbar-track) {
  background: transparent;
}

.message-composer__textarea :deep(.native-textarea.sc-ion-textarea-ios::-webkit-scrollbar-thumb),
.message-composer__textarea :deep(.native-textarea.sc-ion-textarea-md::-webkit-scrollbar-thumb) {
  border-radius: 999px;
  background: rgba(139, 148, 164, 0.72);
}

.message-composer__textarea :deep(.native-textarea.sc-ion-textarea-ios::-webkit-scrollbar-thumb:hover),
.message-composer__textarea :deep(.native-textarea.sc-ion-textarea-md::-webkit-scrollbar-thumb:hover) {
  background: rgba(103, 113, 132, 0.82);
}

.message-composer__send {
  --background: #95ec69;
  --color: #16311a;
  --border-radius: 18px;
  min-width: 64px;
}

.message-composer__send[disabled] {
  opacity: 0.52;
}

ion-modal.message-ai-modal {
  --border-radius: 30px 30px 0 0;
  --backdrop-opacity: 0.24;
  --box-shadow: 0 -28px 56px rgba(15, 23, 42, 0.2);
}

.message-ai-modal__header {
  backdrop-filter: blur(16px);
}

.message-ai-modal__toolbar {
  --background: rgba(248, 249, 251, 0.94);
  --border-width: 0;
  --padding-start: 10px;
  --padding-end: 10px;
}

.message-ai-modal__toolbar ion-title {
  padding: 4px 0 2px;
  color: #1b2330;
  font-size: 17px;
  font-weight: 700;
  letter-spacing: -0.03em;
}

.message-ai-modal__toolbar ion-button {
  --color: #536074;
  --padding-start: 10px;
  --padding-end: 10px;
  font-size: 14px;
  font-weight: 600;
}

.message-draft-modal-page,
.message-ai-modal__page {
  --background: linear-gradient(180deg, #f7f8fa 0%, #f1f3f6 100%);
  --padding-top: 18px;
  --padding-bottom: calc(28px + var(--app-safe-bottom));
  --padding-start: 16px;
  --padding-end: 16px;
}

.message-draft-card,
.message-ai-sheet {
  display: grid;
  gap: 16px;
}

.message-ai-sheet {
  padding: 18px;
  border: 1px solid rgba(218, 223, 232, 0.92);
  border-radius: 28px;
  background: rgba(255, 255, 255, 0.82);
  backdrop-filter: blur(22px);
  box-shadow: 0 18px 40px rgba(15, 23, 42, 0.1);
}

.message-ai-sheet__intro {
  display: grid;
  gap: 6px;
  padding: 2px 2px 4px;
}

.message-ai-sheet__intro h2,
.message-ai-sheet__intro p {
  margin: 0;
}

.message-ai-sheet__intro p {
  color: #8b95a5;
  line-height: 1.55;
}

.message-editor-card__actions {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.message-editor-card__field {
  display: grid;
  gap: 8px;
}

.message-ai-field {
  gap: 10px;
  padding: 12px 14px 14px;
  border: 1px solid rgba(224, 229, 237, 0.96);
  border-radius: 22px;
  background: linear-gradient(180deg, rgba(249, 250, 252, 0.98), rgba(242, 245, 248, 0.92));
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.82);
}

.message-editor-card__field span {
  color: #556173;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.02em;
}

.message-ai-field :deep(.textarea-wrapper) {
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.96);
  border: 1px solid rgba(219, 224, 232, 0.96);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.85);
}

.message-ai-textarea {
  margin: 0;
  --background: transparent;
  --padding-start: 12px;
  --padding-end: 12px;
  --padding-top: 10px;
  --padding-bottom: 10px;
  --color: #1b2330;
  --placeholder-color: #a0a8b6;
}

.message-ai-textarea :deep(.native-textarea) {
  min-height: 70px;
  line-height: 1.6;
}

.message-ai-textarea--readonly {
  --color: #667488;
}

.message-ai-textarea--draft :deep(.native-textarea) {
  min-height: 172px;
  font-size: 15px;
}

.message-ai-sheet .message-ai-field:nth-of-type(3) :deep(.native-textarea) {
  min-height: 172px;
  font-size: 15px;
}

.message-editor-card__actions ion-button,
.message-ai-button {
  margin: 0;
  min-height: 44px;
  font-size: 14px;
  font-weight: 700;
}

.message-ai-button--secondary,
.message-editor-card__actions ion-button:not([class*='message-ai-button']):first-child {
  --color: #536074;
  --border-color: rgba(191, 201, 216, 0.96);
  --background: rgba(255, 255, 255, 0.8);
  --border-radius: 18px;
}

.message-ai-button--primary,
.message-editor-card__actions ion-button:not([class*='message-ai-button']):last-child {
  --background: linear-gradient(180deg, #c9ee9e 0%, #a8df68 100%);
  --color: #17301a;
  --border-radius: 18px;
  box-shadow: 0 14px 28px rgba(151, 211, 95, 0.24);
}

.message-editor-card__actions ion-button[disabled] {
  opacity: 0.56;
}

.ai-polish-history-list {
  display: grid;
  gap: 12px;
}

.ai-polish-history-item {
  padding: 14px 16px;
  border-radius: 20px;
  border: none;
  box-shadow: 0 14px 28px rgba(15, 23, 42, 0.08);
}

.ai-polish-history-item.is-user {
  margin-left: 28px;
  background: linear-gradient(180deg, #eef4ff 0%, #e2ebff 100%);
}

.ai-polish-history-item.is-assistant {
  margin-right: 28px;
  background: rgba(255, 255, 255, 0.92);
}

.ai-polish-history-item strong,
.ai-polish-history-item p {
  margin: 0;
}

.ai-polish-history-item strong {
  color: #7b8798;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.04em;
}

.ai-polish-history-item p {
  margin-top: 8px;
  color: #1d2735;
  white-space: pre-wrap;
  line-height: 1.6;
}

@media (max-width: 420px) {
  .message-editor-card__actions {
    grid-template-columns: minmax(0, 1fr);
  }
}
</style>
