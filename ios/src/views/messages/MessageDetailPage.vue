<template>
  <ion-page>
    <ion-header translucent>
      <ion-toolbar>
        <ion-buttons slot="start">
          <ion-back-button :default-href="ROUTE_PATHS.messages" />
        </ion-buttons>
        <ion-title>{{ pageTitle }}</ion-title>
      </ion-toolbar>
    </ion-header>

    <ion-content fullscreen class="mobile-page message-detail-page">
      <ion-refresher slot="fixed" @ionRefresh="handleRefresh">
        <ion-refresher-content pulling-text="下拉刷新会话" refreshing-spinner="crescent" />
      </ion-refresher>

      <section v-if="activeThread" class="mobile-hero message-detail-hero">
        <p class="mobile-note message-detail-hero__eyebrow">消息详情</p>
        <h1 class="mobile-title">{{ threadTitle }}</h1>
        <p class="mobile-subtitle">
          {{ activeThread.channelName }} · 订单 {{ activeThread.bookingId || activeThread.threadId || '-' }}
        </p>
        <div class="mobile-chip-row">
          <span class="mobile-chip">{{ activeThread.closed ? '已关闭' : '活跃中' }}</span>
          <span class="mobile-chip">消息 {{ messages.length }}</span>
          <span class="mobile-chip" v-if="reservationMatched">已关联订单</span>
        </div>
      </section>

      <div class="mobile-stack">
        <section class="mobile-card message-detail-toolbar-card">
          <div class="message-detail-toolbar-card__actions">
            <ion-button fill="outline" size="small" @click="loadPage">刷新会话</ion-button>
            <ion-button fill="outline" size="small" :disabled="!reservationMatched" @click="handleOpenReservation">
              查看订单
            </ion-button>
            <ion-button fill="outline" size="small" :disabled="!activeThread || activeThread.closed" @click="handleOpenAiDraft">
              AI 回复助手
            </ion-button>
          </div>
          <p v-if="loadNotice" class="mobile-note message-detail-toolbar-card__notice">{{ loadNotice }}</p>
        </section>

        <section class="mobile-card">
          <div class="mobile-inline-row">
            <div>
              <h2 class="mobile-section-title">聊天记录</h2>
              <p class="mobile-note">下拉刷新或等待轮询同步最新消息。</p>
            </div>
            <ion-spinner v-if="loading" name="crescent" />
          </div>

          <div v-if="messages.length > 0" class="mobile-list message-detail-list">
            <article
              v-for="message in messages"
              :key="message.id"
              class="message-bubble"
              :class="message.senderType === MessageSenderType.STAFF ? 'is-staff' : 'is-guest'"
            >
              <div class="message-bubble__meta">
                <strong>{{ resolveSenderLabel(message) }}</strong>
                <span class="mobile-note">{{ formatDateTime(message.timestamp) }}</span>
              </div>
              <p>{{ message.content }}</p>
              <span v-if="message.deliveryStatus === 'FAILED'" class="message-bubble__failed">发送失败</span>
            </article>
          </div>

          <div v-else-if="!loading" class="message-detail-empty-state">
            <h3>暂无消息</h3>
            <p class="mobile-note">该会话还没有聊天记录，可以先发送一条消息。</p>
          </div>
        </section>

        <section class="mobile-card message-editor-card">
          <h2 class="mobile-section-title">发送消息</h2>
          <label class="message-editor-card__field">
            <span>消息内容</span>
            <ion-textarea
              v-model="composerValue"
              :rows="4"
              fill="outline"
              placeholder="输入给住客的消息"
              :disabled="sending || !activeThread || activeThread.closed"
            />
          </label>

          <div class="message-editor-card__actions">
            <ion-button fill="outline" :disabled="sending || !composerValue.trim()" @click="handleClearComposer">
              清空
            </ion-button>
            <ion-button :disabled="sending || !composerValue.trim() || !activeThread || activeThread.closed" @click="handleSendMessage">
              {{ sending ? '发送中...' : '发送消息' }}
            </ion-button>
          </div>
          <p v-if="activeThread?.closed" class="mobile-note message-editor-card__notice">当前会话已关闭，不能继续发送消息。</p>
        </section>
      </div>

      <ion-modal :is-open="aiDraftOpen" @didDismiss="handleDismissAiDraft">
        <ion-header>
          <ion-toolbar>
            <ion-title>AI 回复草稿</ion-title>
            <ion-buttons slot="end">
              <ion-button @click="handleDismissAiDraft">关闭</ion-button>
            </ion-buttons>
          </ion-toolbar>
        </ion-header>

        <ion-content class="mobile-page message-draft-modal-page">
          <section class="mobile-card message-draft-card">
            <label class="message-editor-card__field">
              <span>上下文摘要</span>
              <ion-textarea :value="aiContextSummary" :rows="4" fill="outline" readonly />
            </label>

            <label class="message-editor-card__field">
              <span>补充要求</span>
              <ion-textarea
                v-model="aiInstruction"
                :rows="3"
                fill="outline"
                placeholder="例如：更礼貌、加入入住步骤、简短一些"
              />
            </label>

            <label class="message-editor-card__field">
              <span>回复草稿</span>
              <ion-textarea v-model="aiDraft" :rows="8" fill="outline" placeholder="AI 草稿会显示在这里" />
            </label>

            <div class="message-editor-card__actions">
              <ion-button fill="outline" :disabled="draftLoading" @click="handleGenerateAiDraft">
                {{ draftLoading ? '生成中...' : '重新生成' }}
              </ion-button>
              <ion-button fill="outline" :disabled="draftLoading || !aiDraft.trim()" @click="handleOpenAiPolish">
                继续优化
              </ion-button>
              <ion-button fill="outline" :disabled="draftLoading || !aiDraft.trim()" @click="handleUseAiDraft">
                回填输入框
              </ion-button>
              <ion-button :disabled="draftLoading || !aiDraft.trim()" @click="handleSendAiDraft">
                发送草稿
              </ion-button>
            </div>
          </section>
        </ion-content>
      </ion-modal>

      <ion-modal :is-open="aiPolishOpen" @didDismiss="handleDismissAiPolish">
        <ion-header>
          <ion-toolbar>
            <ion-title>继续优化 AI 草稿</ion-title>
            <ion-buttons slot="end">
              <ion-button @click="handleDismissAiPolish">完成</ion-button>
            </ion-buttons>
          </ion-toolbar>
        </ion-header>

        <ion-content class="mobile-page message-draft-modal-page">
          <section class="mobile-card message-draft-card">
            <div>
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

            <label class="message-editor-card__field">
              <span>优化要求</span>
              <ion-textarea
                v-model="aiPolishInstruction"
                :rows="4"
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
} from '@ionic/vue'
import { computed, onUnmounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getThreadMessages, getMessageThreads, pollThreadMessages, sendAiChatMessage, sendThreadMessage } from '@/api/message'
import { getReservationsWithFilters, type ReservationDTO } from '@/api/reservation'
import { ROUTE_PATHS } from '@/router/guards'
import type { MessageDTO, MessageThreadDTO } from '@/types/message'
import { MessageSenderType } from '@/types/message'
import { isHandledRequestError } from '@/utils/request'
import { showSuccessToast, showWarningToast } from '@/utils/notify'

const MESSAGE_POLL_INTERVAL = 8000

interface AiPolishHistoryItem {
  role: 'user' | 'assistant'
  content: string
}

const route = useRoute()
const router = useRouter()

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

let pollTimer = 0

const threadId = computed(() => Number(route.params.threadId || 0))

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

function resolveSenderLabel(message: MessageDTO) {
  if (message.senderType === MessageSenderType.STAFF) {
    return message.senderName || '酒店客服'
  }

  return message.senderName || '住客'
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

  if (!threadId.value) {
    return
  }

  pollTimer = window.setInterval(async () => {
    const latestTimestamp = getLatestTimestamp()
    if (!latestTimestamp) {
      return
    }

    try {
      const response = await pollThreadMessages(threadId.value, latestTimestamp)
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
    } catch {
      // polling failure should not interrupt current conversation usage
    }
  }, MESSAGE_POLL_INTERVAL)
}

async function loadPage() {
  if (!threadId.value) {
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
    showSuccessToast('消息已发送')
  } catch (error) {
    if (!isHandledRequestError(error)) {
      showWarningToast(resolveWarningMessage(error, '发送消息失败'))
    }
  } finally {
    sending.value = false
  }
}

function handleClearComposer() {
  composerValue.value = ''
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

onUnmounted(() => {
  stopPolling()
})
</script>

<style scoped>
.message-detail-page {
  display: block;
}

.message-detail-hero {
  margin-top: 4px;
}

.message-detail-hero__eyebrow {
  color: var(--ion-color-primary);
  font-weight: 700;
}

.message-detail-toolbar-card {
  display: grid;
  gap: 10px;
}

.message-detail-toolbar-card__actions,
.message-editor-card__actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.message-detail-toolbar-card__notice,
.message-editor-card__notice {
  color: var(--ion-color-warning);
}

.message-detail-list {
  margin-top: 16px;
}

.message-bubble {
  padding: 14px;
  border-radius: 20px;
  border: 1px solid var(--app-border);
}

.message-bubble.is-guest {
  background: rgba(255, 255, 255, 0.88);
}

.message-bubble.is-staff {
  background: var(--app-primary-soft-strong);
}

.message-bubble__meta {
  display: flex;
  justify-content: space-between;
  gap: 12px;
}

.message-bubble__meta strong,
.message-bubble__meta span,
.message-bubble p,
.message-bubble__failed {
  margin: 0;
}

.message-bubble p {
  margin-top: 10px;
  white-space: pre-wrap;
  line-height: 1.6;
}

.message-bubble__failed {
  display: inline-block;
  margin-top: 8px;
  color: var(--ion-color-danger);
  font-size: 12px;
  font-weight: 600;
}

.message-detail-empty-state {
  display: grid;
  gap: 10px;
  justify-items: flex-start;
  padding-top: 18px;
}

.message-detail-empty-state h3,
.message-detail-empty-state p {
  margin: 0;
}

.message-editor-card {
  display: grid;
  gap: 14px;
}

.message-editor-card__field {
  display: grid;
  gap: 8px;
}

.message-editor-card__field span {
  color: var(--app-heading);
  font-size: 13px;
  font-weight: 600;
}

.message-draft-modal-page {
  --padding-top: 16px;
  --padding-bottom: 24px;
  --padding-start: 16px;
  --padding-end: 16px;
}

.message-draft-card {
  display: grid;
  gap: 14px;
}

.ai-polish-history-list {
  display: grid;
  gap: 10px;
}

.ai-polish-history-item {
  padding: 12px;
  border-radius: 16px;
  border: 1px solid var(--app-border);
}

.ai-polish-history-item.is-user {
  background: rgba(37, 99, 235, 0.08);
}

.ai-polish-history-item.is-assistant {
  background: var(--app-surface);
}

.ai-polish-history-item strong,
.ai-polish-history-item p {
  margin: 0;
}

.ai-polish-history-item p {
  margin-top: 8px;
  white-space: pre-wrap;
  line-height: 1.6;
}
</style>
