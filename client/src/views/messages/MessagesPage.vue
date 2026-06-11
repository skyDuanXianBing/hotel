<template>
  <div class="messages-page">
    <div class="conversations-panel">
      <div class="panel-header">
        <h2>{{ t('stage6.components.messagesPage.inbox') }}</h2>
        <div class="panel-header-actions">
          <el-button text size="small" @click="translationDialogVisible = true">
            {{ t('stage6.components.messagesPage.translate') }}
          </el-button>
        </div>
      </div>

      <div class="filters-panel">
        <el-input
          v-model="searchQuery"
          :placeholder="uiText('searchPlaceholder')"
          :prefix-icon="Search"
          clearable
          @keyup.enter="handleThreadSearchEnter"
        />
        <div class="filter-row">
          <el-select
            v-model="filterChannel"
            :placeholder="uiText('channelFilterPlaceholder')"
            clearable
          >
            <el-option
              v-for="option in channelFilterOptions"
              :key="option.value"
              :label="option.label"
              :value="option.value"
            />
          </el-select>
          <el-select
            v-model="filterOrderStatuses"
            :placeholder="uiText('orderKindFilterPlaceholder')"
            clearable
            multiple
            collapse-tags
            collapse-tags-tooltip
          >
            <el-option
              v-for="option in orderStatusFilterOptions"
              :key="option.value"
              :label="option.label"
              :value="option.value"
            />
          </el-select>
        </div>
        <div class="filter-actions">
          <el-checkbox v-model="filterUnreadOnly">
            {{ uiText('unreadOnly') }}
          </el-checkbox>
        </div>
      </div>

      <div v-if="selectedThreadIds.length > 0" class="selection-bar">
        <span>{{ uiText('selectedThreads', { count: selectedThreadIds.length }) }}</span>
        <el-button link type="primary" @click="clearSelectedThreads">
          {{ uiText('clearSelection') }}
        </el-button>
      </div>

      <div
        ref="conversationsListRef"
        class="conversations-list"
        v-loading="isLoadingThreads"
        @scroll="handleThreadListScroll"
      >
        <div
          v-for="conversation in filteredConversations"
          :key="conversation.id"
          class="conversation-item"
          :class="{
            active: activeThreadId === conversation.id,
            selected: isThreadSelected(conversation.id),
          }"
          @click="selectConversation(conversation.id)"
        >
          <el-checkbox
            class="conversation-select"
            :model-value="isThreadSelected(conversation.id)"
            @click.stop
            @change="handleThreadSelectionChange(conversation.id, Boolean($event))"
          />
          <div class="conversation-avatar">
            <el-icon><User /></el-icon>
          </div>
          <div class="conversation-info">
            <div class="conversation-header">
              <span class="channel-name">{{
                conversation.guestName || conversation.listingName || conversation.channelName
              }}</span>
              <div class="conversation-meta">
                <span class="message-time">{{
                  formatConversationTime(conversation.lastActivity)
                }}</span>
                <span v-if="conversation.unreadCount > 0" class="unread-badge">
                  {{ formatUnreadCount(conversation.unreadCount) }}
                </span>
              </div>
            </div>
            <div class="last-message">
              <span class="channel-badge" :class="`channel-${resolveChannelStyle(conversation)}`">
                {{ resolveChannelLabel(conversation) }}
              </span>
              <span>{{ getConversationPreviewText(conversation) }}</span>
            </div>
            <div class="conversation-status">
              <span
                v-if="getConversationOrderKindLabel(conversation)"
                class="order-kind-tag"
                :class="`order-kind-${getConversationOrderKindStyle(conversation)}`"
              >
                {{ getConversationOrderKindLabel(conversation) }}
              </span>
              <span class="conversation-stay-info">
                {{ getConversationStayInfo(conversation) }}
              </span>
              <span v-if="conversation.closed" class="conversation-closed-text">
                {{ t('stage6.components.messagesPage.status.closed') }}
              </span>
            </div>
          </div>
        </div>

        <div v-if="isLoadingThreads && filteredConversations.length === 0" class="empty-state">
          <el-icon :size="48" color="#ddd"><ChatDotRound /></el-icon>
          <p>{{ uiText('loadingConversations') }}</p>
        </div>

        <div
          v-else-if="!isLoadingThreads && filteredConversations.length === 0"
          class="empty-state"
        >
          <el-icon :size="48" color="#ddd"><ChatDotRound /></el-icon>
          <p>{{ threadEmptyStateText }}</p>
        </div>

        <div
          v-if="filteredConversations.length > 0 && isLoadingMoreThreads"
          class="thread-pagination"
        >
          <el-icon class="spin"><Loading /></el-icon>
          <span>{{ uiText('loadingMoreThreads') }}</span>
        </div>
        <div
          v-else-if="filteredConversations.length > 0 && hasThreadQueryStarted && !threadPageHasNext"
          class="thread-pagination"
        >
          <span>{{ uiText('noMoreThreads') }}</span>
        </div>
      </div>
    </div>

    <div class="messages-panel">
      <template v-if="activeConversation">
        <div class="messages-header">
          <div class="channel-info">
            <div class="channel-avatar">
              <el-icon><User /></el-icon>
            </div>
            <div class="channel-details">
              <div class="channel-name">
                {{
                  activeConversation.guestName ||
                  activeConversation.listingName ||
                  activeConversation.channelName
                }}
              </div>
              <div class="channel-status-text">
                <span
                  class="channel-badge"
                  :class="`channel-${resolveChannelStyle(activeConversation)}`"
                >
                  {{ resolveChannelLabel(activeConversation) }}
                </span>
                <span>
                  {{
                    t('stage6.components.messagesPage.orderNumberWithValue', {
                      value: activeConversation.bookingId || activeConversation.threadId || '-',
                    })
                  }}
                </span>
                <span
                  v-if="getConversationOrderKindLabel(activeConversation)"
                  class="order-kind-tag"
                  :class="`order-kind-${getConversationOrderKindStyle(activeConversation)}`"
                >
                  {{ getConversationOrderKindLabel(activeConversation) }}
                </span>
                <span>{{ getConversationStayInfo(activeConversation) }}</span>
                <span v-if="activeConversation.closed">
                  {{ t('stage6.components.messagesPage.status.closed') }}
                </span>
              </div>
            </div>
          </div>
          <div class="header-actions">
            <el-button
              size="small"
              :loading="isResolvingReservation"
              :disabled="!activeConversation"
              @click="openOrderDetail"
            >
              {{ t('stage6.components.messagesPage.orderDetails') }}
            </el-button>
          </div>
        </div>

        <div
          ref="messagesListRef"
          class="messages-list"
          v-loading="isLoadingMessages"
          @scroll="handleMessagesScroll"
        >
          <div v-if="messages.length && hasMoreMessagesBefore" class="load-earlier-messages">
            <el-button
              text
              type="primary"
              :loading="isLoadingOlderMessages"
              @click="loadOlderMessages"
            >
              {{ uiText('loadEarlierMessages') }}
            </el-button>
          </div>

          <template v-for="group in groupedMessages" :key="group.key">
            <div class="message-date-divider">{{ group.label }}</div>
            <div
              v-for="message in group.items"
              :key="message.id"
              class="message-item"
              :data-message-id="message.id"
              :class="{
                'message-sent': message.senderType === SuMessagingSenderType.STAFF,
                'message-received': message.senderType === SuMessagingSenderType.GUEST,
              }"
            >
              <span
                v-if="
                  message.senderType === SuMessagingSenderType.STAFF &&
                  message.deliveryStatus === 'SENDING'
                "
                class="message-delivery-indicator sending"
                :aria-label="t('stage6.components.messagesPage.message.sending')"
              >
                <el-icon class="spin"><Loading /></el-icon>
              </span>
              <span
                v-else-if="
                  message.senderType === SuMessagingSenderType.STAFF &&
                  message.deliveryStatus === 'FAILED'
                "
                class="message-delivery-indicator failed"
                :title="t('stage6.components.messagesPage.message.sendFailed')"
              >
                <el-icon><WarningFilled /></el-icon>
              </span>

              <div class="message-content">
                <div class="message-text">
                  <template
                    v-for="(segment, index) in splitMessageContent(message.content)"
                    :key="`${message.id}-${index}`"
                  >
                    <a
                      v-if="segment.type === 'link'"
                      :href="segment.value"
                      class="message-link"
                      target="_blank"
                      rel="noopener noreferrer"
                    >
                      {{ segment.label }}
                    </a>
                    <span v-else>{{ segment.value }}</span>
                  </template>
                </div>
                <div v-if="shouldShowTranslatedMessage(message)" class="message-translation">
                  {{ getTranslatedMessageText(message) }}
                </div>
                <div class="message-time">{{ formatMessageTime(message.timestamp) }}</div>
              </div>
            </div>
          </template>

          <div v-if="!isLoadingMessages && activeConversation && messages.length === 0" class="empty-state">
            <el-icon :size="48" color="#ddd"><ChatDotRound /></el-icon>
            <p>{{ uiText('emptyMessages') }}</p>
          </div>
        </div>

        <div class="message-input-area">
          <el-input
            v-model="newMessage"
            type="textarea"
            :rows="3"
            :placeholder="t('stage6.components.messagesPage.messageInputPlaceholder')"
            :disabled="isSending || activeConversation.closed"
            @keydown.enter.exact.prevent="sendMessage"
          />
          <div class="input-actions">
            <div class="input-actions-left">
              <el-button
                type="default"
                :icon="ChatLineSquare"
                :disabled="isSending || !activeConversation"
                @click="openQuickReplyDialog"
              >
                {{ t('stage6.components.messagesPage.quickReply') }}
              </el-button>
              <el-button
                type="default"
                :icon="MagicStick"
                :disabled="isSending || activeConversation.closed || isAiGeneratingDraft"
                :loading="isAiGeneratingDraft"
                @click="openAiReplyAssistant"
              >
                {{ t('stage6.components.messagesPage.aiReplyAssistant') }}
              </el-button>
            </div>
            <el-button
              type="primary"
              @click="sendMessage"
              :disabled="!newMessage.trim() || isSending || activeConversation.closed"
              :loading="isSending"
            >
              {{ t('stage6.common.actions.send') }}
            </el-button>
          </div>
        </div>
      </template>

      <div v-else class="empty-state">
        <el-icon :size="80" color="#ccc"><ChatDotRound /></el-icon>
        <p>{{ t('stage6.components.messagesPage.selectConversationPrompt') }}</p>
      </div>
    </div>

    <el-dialog
      v-model="translationDialogVisible"
      :title="t('stage6.components.messagesPage.translation.title')"
      width="680px"
      :close-on-click-modal="false"
    >
      <div class="translation-dialog">
        <div class="translation-setting-row">
          <div>
            <div class="translation-setting-title">
              {{ t('stage6.components.messagesPage.translation.enableTitle') }}
            </div>
            <div class="translation-setting-desc">
              {{ t('stage6.components.messagesPage.translation.enableDescription') }}
            </div>
          </div>
          <el-switch v-model="translationEnabled" />
        </div>

        <div class="translation-setting-block">
          <div class="translation-setting-title">
            {{ t('stage6.components.messagesPage.translation.defaultLanguageTitle') }}
          </div>
          <div class="translation-setting-desc">
            {{ t('stage6.components.messagesPage.translation.defaultLanguageDescription') }}
          </div>
          <el-select v-model="translationTargetLanguage" style="width: 260px">
            <el-option
              v-for="option in translationLanguageOptions"
              :key="option.value"
              :label="option.label"
              :value="option.value"
            />
          </el-select>
          <div class="translation-setting-desc">
            {{ t('stage6.components.messagesPage.translation.resultCacheDescription') }}
          </div>
        </div>
      </div>

      <template #footer>
        <el-button @click="translationDialogVisible = false">{{
          t('stage6.common.actions.cancel')
        }}</el-button>
        <el-button
          type="primary"
          :loading="isApplyingTranslationSettings"
          @click="applyTranslationSettings"
        >
          {{ t('stage6.common.actions.confirm') }}
        </el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="aiDraftDialogVisible"
      :title="t('stage6.components.messagesPage.aiDraft.title')"
      width="760px"
      :close-on-click-modal="false"
      class="ai-draft-dialog"
    >
      <div class="ai-draft-section ai-draft-main-section">
        <div class="ai-draft-toolbar">
          <div class="ai-draft-label">
            {{
              aiDraftViewMode === 'system'
                ? t('stage6.components.messagesPage.aiDraft.systemLanguageVersionTitle', {
                    language: currentSystemLanguageLabel,
                  })
                : t('stage6.components.messagesPage.aiDraft.initialDraft')
            }}
          </div>
          <el-radio-group
            v-model="aiDraftViewMode"
            size="small"
            @change="handleAiDraftViewModeChange"
          >
            <el-radio-button label="draft">
              {{ t('stage6.components.messagesPage.aiDraft.editDraft') }}
            </el-radio-button>
            <el-radio-button label="system" :disabled="isAiGeneratingDraft || !aiDraftReply.trim()">
              {{ t('stage6.components.messagesPage.aiDraft.viewSystemLanguage') }}
            </el-radio-button>
          </el-radio-group>
        </div>
        <div
          class="ai-draft-input-shell"
          v-loading="isAiGeneratingDraft || (aiDraftViewMode === 'system' && isAiTranslatingDraft)"
        >
          <el-input
            v-if="aiDraftViewMode === 'draft'"
            v-model="aiDraftReply"
            type="textarea"
            :rows="8"
            :placeholder="t('stage6.components.messagesPage.aiDraft.initialDraftPlaceholder')"
            @input="handleAiDraftReplyInput"
          />
          <el-input
            v-else
            v-model="aiDraftSystemLanguageVersion"
            type="textarea"
            :rows="8"
            readonly
            :placeholder="
              t('stage6.components.messagesPage.aiDraft.systemLanguageVersionPlaceholder')
            "
          />
        </div>
        <div v-if="aiDraftViewMode === 'system'" class="ai-draft-reference-hint">
          {{ t('stage6.components.messagesPage.aiDraft.systemLanguageReferenceHint') }}
        </div>
      </div>
      <div class="ai-draft-section ai-draft-section-divider">
        <div class="ai-draft-label">
          {{ t('stage6.components.messagesPage.aiDraft.polishTitle') }}
        </div>
        <div class="ai-polish-history">
          <div
            v-for="(item, index) in aiPolishHistory"
            :key="`${item.role}-${index}`"
            :class="['ai-polish-item', item.role]"
          >
            <div class="role">
              {{
                item.role === 'user' ? t('stage6.components.messagesPage.aiDraft.roleYou') : 'GPT'
              }}
            </div>
            <div class="content">{{ item.content }}</div>
          </div>
          <div v-if="!aiPolishHistory.length" class="ai-polish-empty">
            {{ t('stage6.components.messagesPage.aiDraft.polishEmpty') }}
          </div>
        </div>
        <el-input
          v-model="aiPolishInstruction"
          type="textarea"
          :rows="4"
          :placeholder="t('stage6.components.messagesPage.aiDraft.polishPlaceholder')"
        />
        <div class="ai-polish-actions">
          <el-button
            :disabled="!aiPolishInstruction.trim() || isAiPolishing"
            :loading="isAiPolishing"
            @click="polishAiDraftReply"
          >
            {{ t('stage6.components.messagesPage.aiDraft.generatePolished') }}
          </el-button>
        </div>
      </div>
      <template #footer>
        <el-button @click="aiDraftDialogVisible = false">{{
          t('stage6.common.actions.close')
        }}</el-button>
        <el-button
          type="primary"
          :disabled="!aiDraftReply.trim() || isSending || isAiGeneratingDraft"
          :loading="isSending"
          @click="sendAiDraftReply"
        >
          {{ t('stage6.components.messagesPage.aiDraft.sendDraft') }}
        </el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="quickReplyDialogVisible"
      :title="t('stage6.components.messagesPage.quickReplyDialog.title')"
      width="820px"
      :close-on-click-modal="false"
    >
      <div class="quick-reply-toolbar">
        <el-input
          v-model="quickReplySearchQuery"
          :placeholder="t('stage6.components.messagesPage.quickReplyDialog.searchPlaceholder')"
          clearable
        />
        <el-button :loading="quickReplyLoading" @click="loadQuickReplies">
          {{ t('stage6.common.actions.refresh') }}
        </el-button>
      </div>

      <el-table
        :data="filteredQuickReplies"
        style="width: 100%"
        v-loading="quickReplyLoading"
        @row-click="handleQuickReplyPick"
      >
        <el-table-column
          prop="title"
          :label="t('stage6.components.messagesPage.quickReplyDialog.titleColumn')"
          min-width="180"
        />
        <el-table-column
          prop="message"
          :label="t('stage6.common.labels.content')"
          min-width="420"
          show-overflow-tooltip
        />
        <el-table-column :label="t('stage6.common.labels.actions')" width="120" align="center">
          <template #default="{ row }">
            <el-button
              link
              type="primary"
              :loading="isApplyingQuickReply"
              :disabled="isApplyingQuickReply"
              @click.stop="handleQuickReplyPick(row)"
            >
              {{ t('stage6.components.messagesPage.quickReplyDialog.fill') }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div v-if="!quickReplyLoading && !filteredQuickReplies.length" class="quick-reply-empty">
        {{ t('stage6.components.messagesPage.quickReplyDialog.empty') }}
      </div>

      <template #footer>
        <el-button @click="quickReplyDialogVisible = false">{{
          t('stage6.common.actions.close')
        }}</el-button>
      </template>
    </el-dialog>

    <ReservationDetailDrawer
      v-model="showOrderDetailDrawer"
      :reservation-id="selectedReservationId"
      :active-order-tab="'all'"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute } from 'vue-router'
import {
  ChatDotRound,
  ChatLineSquare,
  Loading,
  MagicStick,
  Search,
  User,
  WarningFilled,
} from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import {
  generateSuMessagingAiReplyDraft,
  getSuThreadMessagesPage,
  getSuThreadsPage,
  sendSuThreadMessage,
  SuMessagingSenderType,
  translateSuThreadMessage,
  type SuMessagingAiReplyDraftRecentMessage,
  type SuMessagingAiReplyDraftResponse,
  type SuMessagingMessageOrderStatus,
  type SuMessagingMessageDTO,
  type SuMessagingMessagePageDTO,
  type SuMessagingOrderKind,
  type SuMessagingReservationStatus,
  type SuMessagingThreadPageDTO,
  type SuMessagingThreadDTO,
  type SuMessagingChannelCode,
  type SuMessageTranslationResponse,
} from '@/api/suMessaging'
import { sendChatMessage } from '@/api/chat'
import {
  getReservationById,
  getReservationsWithFilters,
  type ReservationDTO,
} from '@/api/reservation'
import { getAllQuickReplies, type QuickReplyDTO } from '@/api/quickReply'
import { createSuMessagingSocket, type SuMessagingRealtimeEvent } from '@/utils/suMessagingSocket'
import ReservationDetailDrawer from '@/components/reservation/ReservationDetailDrawer.vue'
import {
  formatMissingQuickReplyKeyLabels,
  renderQuickReplyTemplate,
  type QuickReplyTemplateContext,
} from '@/utils/quickReplyTemplate'
import {
  clearExpiredTranslationCache,
  getCachedTranslation,
  setCachedTranslation,
  type TranslationCacheScope,
} from '@/utils/translationCache'
import { useStoreStore } from '@/stores/store'
import { useNotificationCenterStore } from '@/stores/notificationCenter'
import { LANGUAGE_OPTIONS, getStoreOptionLabel } from '@/constants/storeOptions'
import {
  diffYmdDays,
  getStoreDateKey as getStoreDateKeyByZone,
  getYmdWeekdayIndex,
  parseUtcDateTime,
  parseYmdAsUtcDate,
} from '@/utils/storeDateTime'

interface MessageItem {
  id: number
  threadId?: number
  senderType: SuMessagingSenderType
  content: string
  timestamp: Date
  senderName?: string
  deliveryStatus?: 'SENDING' | 'SENT' | 'FAILED'
}

interface RealtimeMessageItem {
  id: number
  threadId: number
  senderType: 'GUEST' | 'STAFF'
  content: string
  timestamp: string
  senderName?: string
  deliveryStatus?: 'SENDING' | 'SENT' | 'FAILED'
}

interface GroupedMessages {
  key: string
  label: string
  items: MessageItem[]
}

interface MessageSegment {
  type: 'text' | 'link'
  value: string
  label: string
}

interface AiPolishItem {
  role: 'user' | 'assistant'
  content: string
}

type AiDraftViewMode = 'draft' | 'system'
type UiTextLocale = 'zh' | 'en' | 'ja'
type ThreadOrderStatusFilter = SuMessagingMessageOrderStatus

interface ApiResponse<T> {
  success?: boolean
  message?: string
  data?: T
}

interface UiTextOptions {
  count?: number
}

const sanitizeUserFacingMessage = (rawMessage?: string) => {
  if (!rawMessage) {
    return ''
  }
  return rawMessage
    .replace(/\bSU\b/gi, '')
    .replace(/\s{2,}/g, ' ')
    .trim()
}

const AI_CONTEXT_LIMITS = {
  maxSingleMessageChars: 1200,
} as const
const AI_DRAFT_RECENT_MESSAGE_LIMIT = 8
const AI_TRANSLATION_TIMEOUT_MS = 45_000
const AI_ASSISTANT_TIMEOUT_MS = 60_000
const INITIAL_MESSAGE_TRANSLATION_BATCH = 20
const SCROLL_TRANSLATION_DEBOUNCE_MS = 220
const TRANSLATION_SETTINGS_STORAGE_KEY = 'messages.translation.settings'
const TRANSLATION_LANGUAGE_VALUES = ['zh-CN', 'en', 'ja', 'ko'] as const
const THREAD_PAGE_SIZE = 30
const MESSAGE_PAGE_LIMIT = 50
const CHANNEL_AIRBNB_ID = 244
const CHANNEL_BOOKING_ID = 19
const THREAD_SEARCH_DEBOUNCE_MS = 350
const THREAD_SCROLL_BOTTOM_THRESHOLD = 80
type TranslationLanguageValue = (typeof TRANSLATION_LANGUAGE_VALUES)[number]
interface TranslationRunResult {
  attempted: number
  failed: number
  errorMessage?: string
}

interface MessageTranslationOptions {
  notifyOnError?: boolean
}

const truncateText = (content: string, maxChars: number) => {
  if (content.length <= maxChars) {
    return content
  }
  return `${content.slice(0, maxChars)}...`
}

const resolveAiErrorMessage = (error: unknown, fallbackMessage: string) => {
  if (error && typeof error === 'object') {
    const responseMessage = sanitizeUserFacingMessage(
      (error as { response?: { data?: { message?: string } } }).response?.data?.message,
    )
    if (responseMessage) {
      return responseMessage
    }
  }

  if (error instanceof Error) {
    const normalizedMessage = sanitizeUserFacingMessage(error.message)
    if (normalizedMessage) {
      return normalizedMessage
    }
  }

  return fallbackMessage
}

const createTranslationRunResult = (): TranslationRunResult => ({
  attempted: 0,
  failed: 0,
})

const mergeTranslationRunResults = (
  current: TranslationRunResult,
  next: TranslationRunResult,
): TranslationRunResult => ({
  attempted: current.attempted + next.attempted,
  failed: current.failed + next.failed,
  errorMessage: current.errorMessage || next.errorMessage,
})

const resolveTranslationFailureMessage = (error?: unknown) =>
  resolveAiErrorMessage(error, t('stage6.components.messagesPage.errors.translationFailed'))

const searchQuery = ref('')
const filterChannel = ref<SuMessagingChannelCode | ''>('')
const filterOrderStatuses = ref<ThreadOrderStatusFilter[]>([])
const filterUnreadOnly = ref(true)
const activeThreadId = ref<number | null>(null)
const newMessage = ref('')
const conversationsListRef = ref<HTMLElement | null>(null)
const messagesListRef = ref<HTMLElement | null>(null)
const conversations = ref<SuMessagingThreadDTO[]>([])
const messages = ref<MessageItem[]>([])
const hasThreadQueryStarted = ref(false)
const isLoadingThreads = ref(false)
const isLoadingMoreThreads = ref(false)
const threadPageNumber = ref(0)
const threadPageHasNext = ref(false)
const selectedThreadIds = ref<number[]>([])
const isLoadingMessages = ref(false)
const isLoadingOlderMessages = ref(false)
const hasMoreMessagesBefore = ref(false)
const nextBeforeMessageId = ref<number | null>(null)
const isSending = ref(false)
const isAiGeneratingDraft = ref(false)
const isAiPolishing = ref(false)
const aiDraftDialogVisible = ref(false)
const aiDraftViewMode = ref<AiDraftViewMode>('draft')
const aiContextSummary = ref('')
const aiDraftReply = ref('')
const aiDraftSystemLanguageVersion = ref('')
const aiDraftSystemLanguageSource = ref('')
const isAiTranslatingDraft = ref(false)
const aiPolishInstruction = ref('')
const aiPolishHistory = ref<AiPolishItem[]>([])
const aiAssistantSessionId = ref<string>()
const showOrderDetailDrawer = ref(false)
const selectedReservationId = ref<number | null>(null)
const isResolvingReservation = ref(false)

const storeStore = useStoreStore()
const notificationCenterStore = useNotificationCenterStore()
const route = useRoute()
const { t, locale } = useI18n()
const routeTargetHandled = ref(false)

const quickReplyDialogVisible = ref(false)
const quickReplyLoading = ref(false)
const quickReplies = ref<QuickReplyDTO[]>([])
const quickReplySearchQuery = ref('')
const isApplyingQuickReply = ref(false)
const translationDialogVisible = ref(false)
const isApplyingTranslationSettings = ref(false)
const translationEnabled = ref(false)
const translationTargetLanguage = ref<TranslationLanguageValue>('zh-CN')
const translatedMessageMap = ref<Record<string, string>>({})

let socket: WebSocket | null = null
let reconnectTimer: number | null = null
let isDestroyed = false
let localMessageSeed = -1
let chatIncomingAudio: HTMLAudioElement | null = null
let messageScrollTranslateTimer: number | null = null
let threadSearchTimer: number | null = null
let threadRequestId = 0
let shouldSkipNextSearchQueryWatch = false
let shouldSkipNextThreadFilterWatch = false
let aiDraftSystemLanguageRequestId = 0
let hasShownPersistentTranslationFallbackNotice = false
let hasShownBackgroundTranslationFailureNotice = false
const reservationIdCache = new Map<string, number | null>()
const translationPendingKeys = new Set<string>()

const DEFAULT_STORE_LANGUAGE = 'zh'
const DEFAULT_STORE_TIME_ZONE = 'Asia/Shanghai'
const STORE_LANGUAGE_AI_LABEL_MAP: Record<string, string> = {
  zh: 'Simplified Chinese',
  en: 'English',
  ja: 'Japanese',
  ko: 'Korean',
}
const GUEST_LANGUAGE_AI_LABEL_MAP: Record<string, string> = {
  zh: 'Simplified Chinese',
  en: 'English',
  ja: 'Japanese',
  ko: 'Korean',
}
const translationLanguageOptions = computed(() => [
  {
    value: 'zh-CN' as const,
    label: t('stage6.components.messagesPage.translation.languages.zhCN'),
  },
  { value: 'en' as const, label: t('stage6.components.messagesPage.translation.languages.en') },
  { value: 'ja' as const, label: t('stage6.components.messagesPage.translation.languages.ja') },
  { value: 'ko' as const, label: t('stage6.components.messagesPage.translation.languages.ko') },
])

const weekdayLabels = computed(() => [
  t('stage6.components.messagesPage.date.weekdays.sun'),
  t('stage6.components.messagesPage.date.weekdays.mon'),
  t('stage6.components.messagesPage.date.weekdays.tue'),
  t('stage6.components.messagesPage.date.weekdays.wed'),
  t('stage6.components.messagesPage.date.weekdays.thu'),
  t('stage6.components.messagesPage.date.weekdays.fri'),
  t('stage6.components.messagesPage.date.weekdays.sat'),
])

const UI_TEXT: Record<UiTextLocale, Record<string, string>> = {
  en: {
    searchPlaceholder: 'Search guest, order, listing, or message',
    channelFilterPlaceholder: 'Channel',
    orderKindFilterPlaceholder: 'Order status',
    unreadOnly: 'Unread only',
    selectedThreads: '{count} selected',
    clearSelection: 'Clear',
    loadingConversations: 'Loading conversations...',
    emptyUnreadConversations: 'No unread conversations',
    emptyConversations: 'No conversations match the filters',
    loadingMoreThreads: 'Loading more...',
    noMoreThreads: 'No more conversations',
    loadEarlierMessages: 'Load earlier messages',
    emptyMessages: 'No messages in this conversation',
    inquiry: 'Inquiry',
    requested: 'Pending check-in',
    pendingConfirmation: 'Pending confirmation',
    checkedIn: 'Checked in',
    cancelled: 'Cancelled',
    completed: 'Completed',
    unmatchedOrder: 'Unmatched order',
    unknownOrder: 'Unknown',
    airbnb: 'Airbnb',
    booking: 'Booking.com',
    allChannels: 'All channels',
    routeTargetNotFound: 'No conversation matched this order. Try adjusting the filters.',
  },
  zh: {
    searchPlaceholder: '搜索住客、订单、房源或消息',
    channelFilterPlaceholder: '渠道',
    orderKindFilterPlaceholder: '订单状态',
    unreadOnly: '只看未读',
    selectedThreads: '已选择 {count} 个会话',
    clearSelection: '清空',
    loadingConversations: '正在加载会话...',
    emptyUnreadConversations: '暂无未读会话',
    emptyConversations: '没有匹配的会话',
    loadingMoreThreads: '正在加载更多...',
    noMoreThreads: '没有更多会话',
    loadEarlierMessages: '加载更早消息',
    emptyMessages: '当前会话暂无消息',
    inquiry: '咨询',
    requested: '待入住',
    pendingConfirmation: '待确认',
    checkedIn: '入住中',
    cancelled: '已取消',
    completed: '已完成',
    unmatchedOrder: '未匹配订单',
    unknownOrder: '未知',
    airbnb: 'Airbnb',
    booking: 'Booking.com',
    allChannels: '全部渠道',
    routeTargetNotFound: '没有匹配到该订单的会话，可调整筛选条件后重试。',
  },
  ja: {
    searchPlaceholder: 'ゲスト、予約、リスティング、メッセージを検索',
    channelFilterPlaceholder: 'チャネル',
    orderKindFilterPlaceholder: '予約ステータス',
    unreadOnly: '未読のみ',
    selectedThreads: '{count} 件選択中',
    clearSelection: 'クリア',
    loadingConversations: '会話を読み込んでいます...',
    emptyUnreadConversations: '未読の会話はありません',
    emptyConversations: '条件に一致する会話はありません',
    loadingMoreThreads: 'さらに読み込んでいます...',
    noMoreThreads: 'これ以上会話はありません',
    loadEarlierMessages: '以前のメッセージを読み込む',
    emptyMessages: 'この会話にメッセージはありません',
    inquiry: '問い合わせ',
    requested: 'チェックイン待ち',
    pendingConfirmation: '確認待ち',
    checkedIn: '滞在中',
    cancelled: 'キャンセル済み',
    completed: '完了',
    unmatchedOrder: '未照合予約',
    unknownOrder: '不明',
    airbnb: 'Airbnb',
    booking: 'Booking.com',
    allChannels: 'すべてのチャネル',
    routeTargetNotFound: 'この予約に一致する会話が見つかりませんでした。',
  },
}

const uiTextLocale = computed<UiTextLocale>(() => {
  const code = String(locale.value || '').toLowerCase()
  if (code.startsWith('zh')) {
    return 'zh'
  }
  if (code.startsWith('ja')) {
    return 'ja'
  }
  return 'en'
})

const uiText = (key: string, options: UiTextOptions = {}) => {
  const text = UI_TEXT[uiTextLocale.value][key] || UI_TEXT.en[key] || key
  if (typeof options.count === 'number') {
    return text.replace('{count}', String(options.count))
  }
  return text
}

const channelFilterOptions = computed(() => [
  { value: 'AIRBNB' as const, label: uiText('airbnb') },
  { value: 'BOOKING' as const, label: uiText('booking') },
])

const orderStatusFilterOptions = computed(() => [
  { value: 'INQUIRY' as const, label: uiText('inquiry') },
  { value: 'CONFIRMED' as const, label: uiText('requested') },
  { value: 'CHECKED_IN' as const, label: uiText('checkedIn') },
  { value: 'CHECKED_OUT' as const, label: uiText('completed') },
  { value: 'CANCELLED' as const, label: uiText('cancelled') },
])

const isThreadListBusy = computed(() => {
  return isLoadingThreads.value || isLoadingMoreThreads.value
})

const currentStoreLanguageCode = computed(() => {
  const rawCode = (storeStore.currentStore?.language || DEFAULT_STORE_LANGUAGE).trim().toLowerCase()
  if (rawCode.startsWith('zh')) {
    return 'zh'
  }
  if (rawCode.startsWith('en')) {
    return 'en'
  }
  if (rawCode.startsWith('ja')) {
    return 'ja'
  }
  if (rawCode.startsWith('ko')) {
    return 'ko'
  }
  return DEFAULT_STORE_LANGUAGE
})

const currentSystemLanguageLabel = computed(() => {
  const raw =
    getStoreOptionLabel(LANGUAGE_OPTIONS, currentStoreLanguageCode.value, t) ||
    t('stage6.components.messagesPage.translation.languages.zhCN')
  return (
    raw.replace(/[（(].*?[）)]/g, '').trim() ||
    t('stage6.components.messagesPage.translation.languages.zhCN')
  )
})

const currentStoreTimeZone = computed(() => {
  const timezone = (storeStore.currentStore?.timezone || DEFAULT_STORE_TIME_ZONE).trim()
  return timezone || DEFAULT_STORE_TIME_ZONE
})

const resolveSystemLanguageAiLabel = () =>
  STORE_LANGUAGE_AI_LABEL_MAP[currentStoreLanguageCode.value] || STORE_LANGUAGE_AI_LABEL_MAP.zh

const detectTextLanguageCode = (text?: string) => {
  const normalized = (text || '').trim()
  if (!normalized) {
    return 'en'
  }
  if (/[\u3040-\u30ff]/.test(normalized)) {
    return 'ja'
  }
  if (/[\uac00-\ud7af]/.test(normalized)) {
    return 'ko'
  }
  if (/[\u4e00-\u9fff]/.test(normalized)) {
    return 'zh'
  }
  return 'en'
}

const buildDateTimeFormatter = (
  locale: string,
  timeZone: string,
  options: Omit<Intl.DateTimeFormatOptions, 'timeZone'>,
) => new Intl.DateTimeFormat(locale, { ...options, timeZone })

const getStoreTimeFormatter = () =>
  buildDateTimeFormatter(locale.value, currentStoreTimeZone.value, {
    hour: '2-digit',
    minute: '2-digit',
    hour12: false,
  })

const getStoreDateKey = (date: Date) => {
  return getStoreDateKeyByZone(date, currentStoreTimeZone.value)
}

const parseDateKeyUtc = (dateKey: string) => {
  return parseYmdAsUtcDate(dateKey)
}

const getDateDiffByStoreDay = (target: Date, base: Date) => {
  return diffYmdDays(getStoreDateKey(target), getStoreDateKey(base))
}

const filteredConversations = computed(() => {
  return conversations.value
})

const filteredQuickReplies = computed(() => {
  const query = quickReplySearchQuery.value.trim().toLowerCase()
  if (!query) {
    return quickReplies.value
  }

  return quickReplies.value.filter((item) => {
    return (
      (item.title || '').toLowerCase().includes(query) ||
      (item.message || '').toLowerCase().includes(query)
    )
  })
})

const activeConversation = computed(() => {
  if (!activeThreadId.value) {
    return null
  }
  return (
    filteredConversations.value.find((conversation) => conversation.id === activeThreadId.value) ||
    null
  )
})

const hasActiveThreadFilters = computed(() => {
  return Boolean(
    searchQuery.value.trim() ||
      filterChannel.value ||
      filterOrderStatuses.value.length > 0 ||
      !filterUnreadOnly.value,
  )
})

const threadEmptyStateText = computed(() => {
  if (filterUnreadOnly.value && !hasActiveThreadFilters.value) {
    return uiText('emptyUnreadConversations')
  }
  return uiText('emptyConversations')
})

const loadTranslationSettings = () => {
  const raw = localStorage.getItem(TRANSLATION_SETTINGS_STORAGE_KEY)
  if (!raw) {
    return
  }

  try {
    const parsed = JSON.parse(raw) as {
      enabled?: boolean
      targetLanguage?: TranslationLanguageValue
    }
    translationEnabled.value = Boolean(parsed.enabled)
    clearTranslationCaches()
    if (parsed.targetLanguage && TRANSLATION_LANGUAGE_VALUES.includes(parsed.targetLanguage)) {
      translationTargetLanguage.value = parsed.targetLanguage
    }
  } catch (error) {
    console.error('Failed to read translation settings:', error)
  }
}

const persistTranslationSettings = () => {
  localStorage.setItem(
    TRANSLATION_SETTINGS_STORAGE_KEY,
    JSON.stringify({
      enabled: translationEnabled.value,
      targetLanguage: translationTargetLanguage.value,
    }),
  )
}

const clearTranslationCaches = () => {
  translatedMessageMap.value = {}
  translationPendingKeys.clear()
  hasShownPersistentTranslationFallbackNotice = false
  hasShownBackgroundTranslationFailureNotice = false
}

const clearMessageTranslationCache = () => {
  translatedMessageMap.value = {}
  translationPendingKeys.clear()
  hasShownPersistentTranslationFallbackNotice = false
  hasShownBackgroundTranslationFailureNotice = false
}

const clearActiveConversationDetails = () => {
  activeThreadId.value = null
  messages.value = []
  selectedReservationId.value = null
  showOrderDetailDrawer.value = false
  clearMessageTranslationCache()
}

const syncActiveConversationWithFilteredList = () => {
  if (!activeThreadId.value) {
    return
  }

  const isVisible = filteredConversations.value.some(
    (conversation) => conversation.id === activeThreadId.value,
  )
  if (!isVisible) {
    clearActiveConversationDetails()
  }
}

const requestAiTranslationToLanguage = async (sourceText: string, targetLanguageLabel: string) => {
  const trimmed = sourceText.trim()
  if (!trimmed) {
    return ''
  }

  const isolatedTranslationSessionId = `translation_${Date.now()}_${Math.random().toString(36).slice(2, 10)}`
  const response = (await sendChatMessage(
    {
      sessionId: isolatedTranslationSessionId,
      taskType: 'TRANSLATION',
      message: [
        `Translate the following text into ${targetLanguageLabel}.`,
        'Requirements:',
        '1. Translate only the provided text. Do not use history, context, or invented content.',
        '2. Return only the translated body without explanations, titles, quotes, or prefixes.',
        '3. Preserve original links, order numbers, dates, room numbers, amounts, and other structured content.',
        '4. If the original text is already in the target language, return a polished version in the same language.',
        '',
        '<<<TEXT>>>',
        trimmed,
        '<<<END>>>',
      ].join('\n'),
    },
    {
      timeoutMs: AI_TRANSLATION_TIMEOUT_MS,
      suppressErrorToast: true,
    },
  )) as ApiResponse<{ reply: string }>

  if (response.success === false || !response.data?.reply) {
    throw new Error(
      sanitizeUserFacingMessage(response.message) ||
        t('stage6.components.messagesPage.errors.translationFailed'),
    )
  }

  return normalizeTranslatedText(response.data.reply)
}

const requestAiTranslation = async (sourceText: string) =>
  requestAiTranslationToLanguage(sourceText, getTranslationLanguageLabel())

const notifyPersistentTranslationFallback = (error: unknown) => {
  console.warn('Persistent message translation failed, using fallback translation:', error)
  if (hasShownPersistentTranslationFallbackNotice) {
    return
  }
  hasShownPersistentTranslationFallbackNotice = true
  const fallbackWarning = t('stage6.components.messagesPage.translation.backendFallbackWarning')
  const failureDetail = resolveAiErrorMessage(error, '')
  ElMessage.warning(failureDetail ? `${fallbackWarning} ${failureDetail}` : fallbackWarning)
}

const notifyBackgroundTranslationFailure = (errorMessage: string) => {
  if (hasShownBackgroundTranslationFailureNotice) {
    return
  }
  hasShownBackgroundTranslationFailureNotice = true
  ElMessage.error(errorMessage)
}

const resolveMessageTranslationThreadId = (message: MessageItem) => {
  if (message.threadId && message.threadId > 0) {
    return message.threadId
  }
  if (activeThreadId.value && activeThreadId.value > 0) {
    return activeThreadId.value
  }
  return null
}

const canRequestPersistentMessageTranslation = (message: MessageItem) => {
  return Boolean(resolveMessageTranslationThreadId(message) && message.id > 0)
}

const requestPersistentMessageTranslation = async (message: MessageItem) => {
  const threadId = resolveMessageTranslationThreadId(message)
  if (!threadId || message.id <= 0) {
    throw new Error(resolveTranslationFailureMessage())
  }

  const response = (await translateSuThreadMessage(
    threadId,
    message.id,
    {
      targetLanguage: translationTargetLanguage.value,
    },
    {
      timeoutMs: AI_TRANSLATION_TIMEOUT_MS,
      suppressErrorToast: true,
    },
  )) as ApiResponse<SuMessageTranslationResponse>

  const translatedContent = normalizeTranslatedText(response.data?.translatedContent || '')
  if (response.success === false || !translatedContent) {
    throw new Error(
      sanitizeUserFacingMessage(response.message) || resolveTranslationFailureMessage(),
    )
  }

  return translatedContent
}

const syncSystemLanguageDraftVersion = async (guestFacingDraft: string) => {
  const trimmed = guestFacingDraft.trim()
  if (!trimmed) {
    aiDraftSystemLanguageVersion.value = ''
    aiDraftSystemLanguageSource.value = ''
    return
  }

  const requestId = ++aiDraftSystemLanguageRequestId
  isAiTranslatingDraft.value = true
  try {
    aiDraftSystemLanguageVersion.value = ''
    const translated = await requestAiTranslationToLanguage(trimmed, resolveSystemLanguageAiLabel())
    if (requestId !== aiDraftSystemLanguageRequestId) {
      return
    }
    aiDraftSystemLanguageVersion.value = translated || trimmed
    aiDraftSystemLanguageSource.value = trimmed
  } catch (error) {
    if (requestId !== aiDraftSystemLanguageRequestId) {
      return
    }
    console.error('Failed to generate the system language version:', error)
    aiDraftSystemLanguageVersion.value = trimmed
    aiDraftSystemLanguageSource.value = trimmed
  } finally {
    if (requestId === aiDraftSystemLanguageRequestId) {
      isAiTranslatingDraft.value = false
    }
  }
}

const ensureSystemLanguageDraftVersion = async () => {
  const trimmed = aiDraftReply.value.trim()
  if (!trimmed) {
    aiDraftSystemLanguageVersion.value = ''
    aiDraftSystemLanguageSource.value = ''
    return
  }

  if (aiDraftSystemLanguageSource.value === trimmed && aiDraftSystemLanguageVersion.value.trim()) {
    return
  }

  await syncSystemLanguageDraftVersion(trimmed)
}

const handleAiDraftViewModeChange = (value: string | number | boolean | undefined) => {
  if (value === 'system') {
    void ensureSystemLanguageDraftVersion()
  }
}

const handleAiDraftReplyInput = () => {
  if (aiDraftReply.value.trim() === aiDraftSystemLanguageSource.value) {
    return
  }

  aiDraftSystemLanguageRequestId += 1
  isAiTranslatingDraft.value = false
  aiDraftSystemLanguageSource.value = ''
  aiDraftSystemLanguageVersion.value = ''
}

const setMessageTranslatedText = (message: MessageItem, sourceText: string, translated: string) => {
  const normalizedTranslation = normalizeTranslatedText(translated)
  if (!normalizedTranslation) {
    return ''
  }

  const key = getMessageTranslationKey(message)
  translatedMessageMap.value = {
    ...translatedMessageMap.value,
    [key]: normalizedTranslation,
  }
  setCachedTranslation(getTranslationCacheScope(), sourceText, normalizedTranslation)
  return normalizedTranslation
}

const requestFallbackMessageTranslation = async (message: MessageItem, sourceText: string) => {
  const cachedTranslation = getCachedTranslation(getTranslationCacheScope(), sourceText)
  if (cachedTranslation) {
    return setMessageTranslatedText(message, sourceText, cachedTranslation)
  }

  const translated = await requestAiTranslation(sourceText)
  return setMessageTranslatedText(message, sourceText, translated)
}

const ensureMessageTranslation = async (
  message: MessageItem,
  options: MessageTranslationOptions = {},
): Promise<TranslationRunResult> => {
  const skippedResult = createTranslationRunResult()
  if (!translationEnabled.value || !message.content.trim()) {
    return skippedResult
  }

  const key = getMessageTranslationKey(message)
  if (translatedMessageMap.value[key] || translationPendingKeys.has(key)) {
    return skippedResult
  }

  const sourceText = message.content.trim()

  translationPendingKeys.add(key)
  try {
    let translated = ''
    if (canRequestPersistentMessageTranslation(message)) {
      try {
        translated = await requestPersistentMessageTranslation(message)
        setMessageTranslatedText(message, sourceText, translated)
      } catch (error) {
        notifyPersistentTranslationFallback(error)
        translated = await requestFallbackMessageTranslation(message, sourceText)
      }
    } else {
      translated = await requestFallbackMessageTranslation(message, sourceText)
    }
    if (!translated) {
      throw new Error(resolveTranslationFailureMessage())
    }
    return { attempted: 1, failed: 0 }
  } catch (error) {
    console.error('Failed to translate message:', error)
    const errorMessage = resolveTranslationFailureMessage(error)
    if (options.notifyOnError !== false) {
      notifyBackgroundTranslationFailure(errorMessage)
    }
    return {
      attempted: 1,
      failed: 1,
      errorMessage,
    }
  } finally {
    translationPendingKeys.delete(key)
  }
}

const translateMessagesSequentially = async (
  items: MessageItem[],
  options: { stopOnError?: boolean } = {},
) => {
  let result = createTranslationRunResult()
  for (const message of items) {
    const nextResult = await ensureMessageTranslation(message, {
      notifyOnError: options.stopOnError !== true,
    })
    result = mergeTranslationRunResults(result, nextResult)
    if (options.stopOnError && nextResult.failed > 0) {
      break
    }
  }
  return result
}

const translateCurrentConversation = async (options: { stopOnError?: boolean } = {}) => {
  if (!translationEnabled.value) {
    return createTranslationRunResult()
  }
  const latestMessages = sortMessagesByTime(messages.value).slice(
    -INITIAL_MESSAGE_TRANSLATION_BATCH,
  )
  return translateMessagesSequentially(latestMessages, options)
}

const getVisibleMessagesInViewport = () => {
  const container = messagesListRef.value
  if (!container || messages.value.length === 0) {
    return [] as MessageItem[]
  }

  const containerRect = container.getBoundingClientRect()
  const messageNodes = Array.from(
    container.querySelectorAll<HTMLElement>('.message-item[data-message-id]'),
  )
  const messageById = new Map(messages.value.map((item) => [item.id, item]))

  const visibleItems: MessageItem[] = []
  const seenIds = new Set<number>()

  for (const node of messageNodes) {
    const messageId = Number(node.dataset.messageId)
    if (!Number.isFinite(messageId) || seenIds.has(messageId)) {
      continue
    }

    const rect = node.getBoundingClientRect()
    const isVisible = rect.bottom >= containerRect.top && rect.top <= containerRect.bottom
    if (!isVisible) {
      continue
    }

    const item = messageById.get(messageId)
    if (!item) {
      continue
    }
    seenIds.add(messageId)
    visibleItems.push(item)
  }

  return sortMessagesByTime(visibleItems)
}

const translateVisibleMessages = async (options: { stopOnError?: boolean } = {}) => {
  if (!translationEnabled.value) {
    return createTranslationRunResult()
  }
  const visibleItems = getVisibleMessagesInViewport()
  if (!visibleItems.length) {
    return createTranslationRunResult()
  }
  return translateMessagesSequentially(visibleItems, options)
}

const clearMessageScrollTranslateTimer = () => {
  if (messageScrollTranslateTimer) {
    window.clearTimeout(messageScrollTranslateTimer)
    messageScrollTranslateTimer = null
  }
}

const handleMessagesScroll = () => {
  const container = messagesListRef.value
  if (
    container &&
    container.scrollTop <= 24 &&
    hasMoreMessagesBefore.value &&
    !isLoadingOlderMessages.value
  ) {
    void loadOlderMessages()
  }

  if (!translationEnabled.value) {
    return
  }
  clearMessageScrollTranslateTimer()
  messageScrollTranslateTimer = window.setTimeout(() => {
    void translateVisibleMessages()
  }, SCROLL_TRANSLATION_DEBOUNCE_MS)
}

const applyTranslationSettings = async () => {
  isApplyingTranslationSettings.value = true
  try {
    if (translationEnabled.value) {
      clearTranslationCaches()
      let translationResult = await translateCurrentConversation({ stopOnError: true })
      await nextTick()
      if (translationResult.failed === 0) {
        translationResult = mergeTranslationRunResults(
          translationResult,
          await translateVisibleMessages({ stopOnError: true }),
        )
      }
      if (translationResult.failed > 0) {
        throw new Error(translationResult.errorMessage || resolveTranslationFailureMessage())
      }
    } else {
      clearTranslationCaches()
    }
    persistTranslationSettings()
    translationDialogVisible.value = false
    ElMessage.success(t('stage6.components.messagesPage.translation.applySuccess'))
  } catch (error) {
    console.error('Failed to apply translation settings:', error)
    ElMessage.error(
      resolveAiErrorMessage(error, t('stage6.components.messagesPage.translation.saveFailed')),
    )
  } finally {
    isApplyingTranslationSettings.value = false
  }
}

const groupedMessages = computed<GroupedMessages[]>(() => {
  const groups = new Map<string, MessageItem[]>()

  for (const message of sortMessagesByTime(messages.value)) {
    const key = getStoreDateKey(message.timestamp)
    const existing = groups.get(key)
    if (existing) {
      existing.push(message)
    } else {
      groups.set(key, [message])
    }
  }

  return Array.from(groups.entries()).map(([key, items]) => ({
    key,
    label: formatDateDividerLabel(key),
    items,
  }))
})

const formatUnreadCount = (count: number) => (count > 99 ? '99+' : String(count))

const resolveChannelStyle = (conversation: SuMessagingThreadDTO) => {
  const normalizedName = (conversation.channelName || '').trim().toLowerCase()
  if (conversation.channelId === CHANNEL_BOOKING_ID || normalizedName.includes('booking')) {
    return 'booking'
  }
  if (conversation.channelId === CHANNEL_AIRBNB_ID || normalizedName.includes('airbnb')) {
    return 'airbnb'
  }
  return 'default'
}

const resolveChannelLabel = (conversation: SuMessagingThreadDTO) => {
  const style = resolveChannelStyle(conversation)
  if (style === 'booking') {
    return 'Booking.com'
  }
  if (style === 'airbnb') {
    return 'Airbnb'
  }
  return conversation.channelName || t('stage6.components.messagesPage.channelFallback')
}

const isAirbnbConversation = (conversation: SuMessagingThreadDTO) => {
  const normalizedName = (conversation.channelName || '').trim().toLowerCase()
  return (
    conversation.channelCode === 'AIRBNB' ||
    conversation.channelId === CHANNEL_AIRBNB_ID ||
    normalizedName.includes('airbnb')
  )
}

const getConversationOrderKindLabel = (conversation: SuMessagingThreadDTO) => {
  if (!conversation.orderKind) {
    return ''
  }
  if (conversation.orderKind === 'INQUIRY' && !isAirbnbConversation(conversation)) {
    return ''
  }

  const labelMap: Partial<Record<SuMessagingOrderKind, string>> = {
    INQUIRY: uiText('inquiry'),
    REQUESTED: uiText('pendingConfirmation'),
    CONFIRMED: resolveConfirmedConversationStatusLabel(conversation.reservationStatus),
    CANCELLED: uiText('cancelled'),
    COMPLETED: uiText('completed'),
    UNMATCHED_ORDER: uiText('unmatchedOrder'),
    UNKNOWN: uiText('unknownOrder'),
  }
  return labelMap[conversation.orderKind] || ''
}

const resolveConfirmedConversationStatusLabel = (status?: SuMessagingReservationStatus) => {
  if (status === 'CHECKED_IN') {
    return uiText('checkedIn')
  }
  if (status === 'CHECKED_OUT') {
    return uiText('completed')
  }
  if (status === 'CANCELLED') {
    return uiText('cancelled')
  }
  return uiText('requested')
}

const getConversationOrderKindStyle = (conversation: SuMessagingThreadDTO) => {
  if (conversation.reservationStatus === 'CHECKED_IN') {
    return 'checked-in'
  }
  if (conversation.reservationStatus === 'CHECKED_OUT') {
    return 'completed'
  }
  if (conversation.reservationStatus === 'CANCELLED') {
    return 'cancelled'
  }
  return (conversation.orderKind || 'UNKNOWN').toLowerCase().replace(/_/g, '-')
}

const getMessageTranslationKey = (message: MessageItem) =>
  `message:${message.id}:${message.content}`

const getTranslationLanguageLabel = () =>
  translationLanguageOptions.value.find((item) => item.value === translationTargetLanguage.value)
    ?.label || t('stage6.components.messagesPage.translation.languages.zhCN')

const normalizeTranslatedText = (text: string) =>
  text
    .replace(/^\u8bd1\u6587[:：]\s*/i, '')
    .replace(/^translation[:：]\s*/i, '')
    .trim()

const getConversationPreviewText = (conversation: SuMessagingThreadDTO) => {
  const bookingNo = conversation.bookingId || conversation.threadId || '-'
  return t('stage6.components.messagesPage.orderNumberWithValue', { value: bookingNo })
}

const getTranslatedMessageText = (message: MessageItem) =>
  translatedMessageMap.value[getMessageTranslationKey(message)] || ''

const shouldShowTranslatedMessage = (message: MessageItem) => {
  if (!translationEnabled.value) {
    return false
  }
  const translated = getTranslatedMessageText(message)
  return Boolean(translated && translated !== message.content.trim())
}

const normalizeSenderName = (senderName?: string) => {
  if (!senderName) {
    return senderName
  }
  const trimmed = senderName.trim()
  if (!trimmed) {
    return trimmed
  }

  if (/^AI/i.test(trimmed) && /[\uFFFD\u00C0-\u00FF]/.test(trimmed)) {
    return t('stage6.components.messagesPage.aiAssistant')
  }

  if (/^AI/i.test(trimmed) && /[åæ]/i.test(trimmed)) {
    return t('stage6.components.messagesPage.aiAssistant')
  }

  return trimmed
}

const mapMessage = (message: SuMessagingMessageDTO): MessageItem => ({
  id: message.id,
  threadId: message.threadId,
  senderType: message.senderType,
  content: message.content,
  timestamp: parseUtcDateTime(message.timestamp),
  senderName: normalizeSenderName(message.senderName),
  deliveryStatus: message.deliveryStatus,
})

const mapRealtimeMessage = (message: RealtimeMessageItem): MessageItem => ({
  id: message.id,
  threadId: message.threadId,
  senderType: message.senderType as SuMessagingSenderType,
  content: message.content,
  timestamp: parseUtcDateTime(message.timestamp),
  senderName: normalizeSenderName(message.senderName),
  deliveryStatus: message.deliveryStatus,
})

const URL_REGEX = /https?:\/\/[^\s]+/gi

const normalizeInlineSpaces = (text: string) => text.replace(/[ \t\u00A0\u3000]{2,}/g, ' ')

const normalizeMessageContent = (content: string) =>
  content
    .replace(/\r\n/g, '\n')
    .replace(/\r/g, '\n')
    .replace(/[\u00A0\u3000]/g, ' ')
    .replace(/[ \t]+\n/g, '\n')
    .replace(/\n{3,}/g, '\n\n')

const splitMessageContent = (content: string): MessageSegment[] => {
  if (!content) {
    return []
  }

  const normalized = normalizeMessageContent(content)
  const segments: MessageSegment[] = []
  let cursor = 0
  let match = URL_REGEX.exec(normalized)

  while (match) {
    const fullUrl = match[0]
    const start = match.index

    if (start > cursor) {
      const text = normalized.slice(cursor, start)
      if (text) {
        segments.push({
          type: 'text',
          value: normalizeInlineSpaces(text),
          label: normalizeInlineSpaces(text),
        })
      }
    }

    segments.push({
      type: 'link',
      value: fullUrl,
      label: fullUrl,
    })
    cursor = start + fullUrl.length
    match = URL_REGEX.exec(normalized)
  }

  if (cursor < normalized.length) {
    const text = normalized.slice(cursor)
    if (text) {
      segments.push({
        type: 'text',
        value: normalizeInlineSpaces(text),
        label: normalizeInlineSpaces(text),
      })
    }
  }

  return segments
}

const getCurrentToken = () => localStorage.getItem('token') || ''

const getCurrentStoreId = () => {
  const raw = localStorage.getItem('currentStore')
  if (!raw) {
    return null
  }

  try {
    const store = JSON.parse(raw)
    const id = typeof store?.id === 'number' ? store.id : Number(store?.id)
    return Number.isFinite(id) ? id : null
  } catch (error) {
    console.error('Failed to parse currentStore:', error)
    return null
  }
}

const getCurrentUserId = () => {
  const raw = localStorage.getItem('user')
  if (!raw) {
    return null
  }

  try {
    const user = JSON.parse(raw)
    const id = typeof user?.id === 'number' ? user.id : Number(user?.id)
    return Number.isFinite(id) ? id : null
  } catch (error) {
    console.error('Failed to parse user:', error)
    return null
  }
}

const getTranslationCacheScope = (): TranslationCacheScope => ({
  storeId: getCurrentStoreId(),
  userId: getCurrentUserId(),
  targetLanguage: translationTargetLanguage.value,
})

const formatDateDividerLabel = (dateKey: string) => {
  const parsed = parseDateKeyUtc(dateKey)
  if (Number.isNaN(parsed.getTime())) {
    return dateKey
  }

  const [yearText, monthText, dayText] = dateKey.split('-')
  const year = Number(yearText)
  const month = Number(monthText)
  const day = Number(dayText)
  const storeNowYear = Number(getStoreDateKey(new Date()).slice(0, 4))
  const weekday = weekdayLabels.value[getYmdWeekdayIndex(dateKey)]
  if (year === storeNowYear) {
    return t('stage6.components.messagesPage.date.monthDayWeekday', { month, day, weekday })
  }
  return t('stage6.components.messagesPage.date.yearMonthDayWeekday', { year, month, day, weekday })
}

const sortMessagesByTime = (list: MessageItem[]) =>
  [...list].sort((a, b) => a.timestamp.getTime() - b.timestamp.getTime())

const mergeMessagesById = (list: MessageItem[]) => {
  const messageById = new Map<number, MessageItem>()
  for (const message of list) {
    const existing = messageById.get(message.id)
    messageById.set(message.id, existing ? { ...existing, ...message } : message)
  }
  return sortMessagesByTime([...messageById.values()])
}

const scrollToBottom = async () => {
  await nextTick()
  if (messagesListRef.value) {
    messagesListRef.value.scrollTop = messagesListRef.value.scrollHeight
  }
}

const preserveScrollPositionAfterPrepend = async (previousScrollHeight: number) => {
  await nextTick()
  if (!messagesListRef.value) {
    return
  }
  messagesListRef.value.scrollTop = messagesListRef.value.scrollHeight - previousScrollHeight
}

const updateThreadSelectionAfterListChange = () => {
  const visibleIds = new Set(conversations.value.map((conversation) => conversation.id))
  selectedThreadIds.value = selectedThreadIds.value.filter((threadId) => visibleIds.has(threadId))
}

const isThreadSelected = (threadId: number) => selectedThreadIds.value.includes(threadId)

const handleThreadSelectionChange = (threadId: number, checked: string | number | boolean) => {
  const shouldSelect = Boolean(checked)
  if (shouldSelect && !selectedThreadIds.value.includes(threadId)) {
    selectedThreadIds.value = [...selectedThreadIds.value, threadId]
    return
  }
  if (!shouldSelect) {
    selectedThreadIds.value = selectedThreadIds.value.filter((selectedId) => selectedId !== threadId)
  }
}

const clearSelectedThreads = () => {
  selectedThreadIds.value = []
}

const resolveThreadOrderStatusesParam = () => {
  if (filterOrderStatuses.value.length === 0) {
    return undefined
  }
  return filterOrderStatuses.value.join(',')
}

const buildThreadPageParams = (page: number) => {
  return {
    page,
    size: THREAD_PAGE_SIZE,
    channel: filterChannel.value || undefined,
    orderStatuses: resolveThreadOrderStatusesParam(),
    unread: filterUnreadOnly.value || undefined,
    search: searchQuery.value.trim() || undefined,
  }
}

const applyThreadPage = (pageData: SuMessagingThreadPageDTO, append: boolean) => {
  const incoming = pageData.items || []
  conversations.value = append ? mergeThreadSummaries([...conversations.value, ...incoming]) : incoming
  threadPageNumber.value = pageData.page
  threadPageHasNext.value = Boolean(pageData.hasNext)
  hasThreadQueryStarted.value = true
  updateThreadSelectionAfterListChange()
  syncActiveConversationWithFilteredList()
}

const fetchThreadPage = async (page: number, append: boolean) => {
  const currentRequestId = ++threadRequestId
  try {
    if (append) {
      isLoadingMoreThreads.value = true
    } else {
      isLoadingThreads.value = true
    }
    const response = (await getSuThreadsPage(
      buildThreadPageParams(page),
    )) as ApiResponse<SuMessagingThreadPageDTO>
    if (currentRequestId !== threadRequestId) {
      return
    }
    if (response.success === false) {
      throw new Error(
        sanitizeUserFacingMessage(response.message) ||
          t('stage6.components.messagesPage.errors.refreshFailed'),
      )
    }

    applyThreadPage(
      response.data || {
        items: [],
        page,
        size: THREAD_PAGE_SIZE,
        totalElements: 0,
        totalPages: 0,
        hasNext: false,
      },
      append,
    )
    await notificationCenterStore.refreshChatUnreadCount()
  } catch (error) {
    if (currentRequestId !== threadRequestId) {
      return
    }
    console.error('Failed to refresh conversation list:', error)
    ElMessage.error(t('stage6.components.messagesPage.errors.refreshFailed'))
  } finally {
    if (currentRequestId === threadRequestId) {
      isLoadingThreads.value = false
      isLoadingMoreThreads.value = false
    }
  }
}

const applyThreadFilters = async () => {
  clearActiveConversationDetails()
  clearSelectedThreads()
  threadPageNumber.value = 0
  threadPageHasNext.value = false
  await fetchThreadPage(0, false)
}

const loadNextThreadPage = async () => {
  if (!threadPageHasNext.value || isThreadListBusy.value) {
    return
  }
  await fetchThreadPage(threadPageNumber.value + 1, true)
}

const handleThreadListScroll = () => {
  const container = conversationsListRef.value
  if (!container || !threadPageHasNext.value || isThreadListBusy.value) {
    return
  }

  const distanceToBottom = container.scrollHeight - container.scrollTop - container.clientHeight
  if (distanceToBottom <= THREAD_SCROLL_BOTTOM_THRESHOLD) {
    void loadNextThreadPage()
  }
}

const mergeThreadSummaries = (items: SuMessagingThreadDTO[]) => {
  const threadById = new Map<number, SuMessagingThreadDTO>()
  for (const item of items) {
    const existing = threadById.get(item.id)
    threadById.set(item.id, existing ? { ...existing, ...item } : item)
  }
  return [...threadById.values()].sort((a, b) => {
    const left = parseUtcDateTime(a.lastActivity).getTime()
    const right = parseUtcDateTime(b.lastActivity).getTime()
    if (Number.isNaN(left) || Number.isNaN(right)) {
      return b.id - a.id
    }
    if (left === right) {
      return b.id - a.id
    }
    return right - left
  })
}

const updateThreadSummary = (threadId: number, patch: Partial<SuMessagingThreadDTO>) => {
  conversations.value = mergeThreadSummaries(
    conversations.value.map((conversation) => {
      if (conversation.id !== threadId) {
        return conversation
      }
      return { ...conversation, ...patch }
    }),
  )
}

const loadThreadMessages = async (threadId: number, beforeMessageId?: number) => {
  const isOlderPage = typeof beforeMessageId === 'number'
  const previousScrollHeight = messagesListRef.value?.scrollHeight || 0
  try {
    if (isOlderPage) {
      isLoadingOlderMessages.value = true
    } else {
      isLoadingMessages.value = true
      messages.value = []
      hasMoreMessagesBefore.value = false
      nextBeforeMessageId.value = null
      clearMessageTranslationCache()
    }

    const response = (await getSuThreadMessagesPage(threadId, {
      limit: MESSAGE_PAGE_LIMIT,
      beforeMessageId,
      markRead: !isOlderPage,
    })) as ApiResponse<SuMessagingMessagePageDTO>
    if (response.success === false) {
      throw new Error(
        sanitizeUserFacingMessage(response.message) ||
          t('stage6.components.messagesPage.errors.loadMessagesFailed'),
      )
    }

    const pageData =
      response.data || {
        items: [],
        limit: MESSAGE_PAGE_LIMIT,
        hasMoreBefore: false,
      }
    const incoming = (pageData.items || []).map(mapMessage)
    messages.value = mergeMessagesById(isOlderPage ? [...incoming, ...messages.value] : incoming)
    hasMoreMessagesBefore.value = Boolean(pageData.hasMoreBefore)
    nextBeforeMessageId.value = pageData.nextBeforeMessageId || null
    if (!isOlderPage) {
      updateThreadSummary(threadId, { unreadCount: 0 })
      await notificationCenterStore.refreshChatUnreadCount()
    }
    if (translationEnabled.value) {
      void translateCurrentConversation()
    }
    if (isOlderPage) {
      await preserveScrollPositionAfterPrepend(previousScrollHeight)
    } else {
      await scrollToBottom()
    }
    if (translationEnabled.value) {
      await nextTick()
      void translateVisibleMessages()
    }
  } catch (error) {
    console.error('Failed to load conversation messages:', error)
    ElMessage.error(t('stage6.components.messagesPage.errors.loadMessagesFailed'))
  } finally {
    isLoadingMessages.value = false
    isLoadingOlderMessages.value = false
  }
}

const loadOlderMessages = async () => {
  if (!activeThreadId.value || !nextBeforeMessageId.value || isLoadingOlderMessages.value) {
    return
  }
  await loadThreadMessages(activeThreadId.value, nextBeforeMessageId.value)
}

const selectConversation = async (threadId: number) => {
  if (activeThreadId.value === threadId && messages.value.length > 0) {
    return
  }
  activeThreadId.value = threadId
  await loadThreadMessages(threadId)
}

const getLatestGuestMessage = () =>
  [...sortMessagesByTime(messages.value)]
    .reverse()
    .find((item) => item.senderType === SuMessagingSenderType.GUEST)

const resolveLatestGuestLanguageLabel = () => {
  const latestGuestMessage = getLatestGuestMessage()
  const languageCode = detectTextLanguageCode(latestGuestMessage?.content)
  return GUEST_LANGUAGE_AI_LABEL_MAP[languageCode] || GUEST_LANGUAGE_AI_LABEL_MAP.en
}

const buildFallbackContextSummary = () => {
  const conversation = activeConversation.value
  const latestGuestMessage = getLatestGuestMessage()

  const headline =
    latestGuestMessage?.content?.slice(0, 80) ||
    t('stage6.components.messagesPage.aiDraft.fallbackHeadline')
  return t('stage6.components.messagesPage.aiDraft.fallbackContextSummary', {
    channel:
      conversation?.channelName || t('stage6.components.messagesPage.aiDraft.unknownChannel'),
    orderNumber: conversation?.bookingId || conversation?.threadId || '-',
    headline,
  })
}

const resolveAiDraftLanguage = () => {
  const languageCode = detectTextLanguageCode(getLatestGuestMessage()?.content)
  if (languageCode === 'zh') {
    return 'zh-CN'
  }
  return languageCode
}

const resolveAiDraftChannel = (conversation: SuMessagingThreadDTO): SuMessagingChannelCode | undefined => {
  if (conversation.channelCode) {
    return conversation.channelCode
  }
  const style = resolveChannelStyle(conversation)
  if (style === 'airbnb') {
    return 'AIRBNB'
  }
  if (style === 'booking') {
    return 'BOOKING'
  }
  return undefined
}

const toAiDraftSentAt = (timestamp: Date) => {
  if (Number.isNaN(timestamp.getTime())) {
    return undefined
  }
  return timestamp.toISOString()
}

const buildAiDraftRecentMessages = (): SuMessagingAiReplyDraftRecentMessage[] => {
  const recentMessages: SuMessagingAiReplyDraftRecentMessage[] = []
  for (const item of sortMessagesByTime(messages.value).slice(-AI_DRAFT_RECENT_MESSAGE_LIMIT)) {
    const content = truncateText(item.content || '', AI_CONTEXT_LIMITS.maxSingleMessageChars).trim()
    if (!content) {
      continue
    }
    recentMessages.push({
      direction: item.senderType === SuMessagingSenderType.GUEST ? 'GUEST' : 'STAFF',
      content,
      sentAt: toAiDraftSentAt(item.timestamp),
    })
  }
  return recentMessages
}

const getLatestGuestMessageId = () => {
  const latestGuestMessage = getLatestGuestMessage()
  if (!latestGuestMessage || latestGuestMessage.id <= 0) {
    return undefined
  }
  return latestGuestMessage.id
}

const openAiReplyAssistant = async () => {
  if (!activeConversation.value) {
    ElMessage.warning(t('stage6.components.messagesPage.errors.selectConversationFirst'))
    return
  }
  if (activeConversation.value.closed) {
    ElMessage.warning(t('stage6.components.messagesPage.errors.closedConversation'))
    return
  }
  if (isAiGeneratingDraft.value) {
    return
  }

  isAiGeneratingDraft.value = true
  aiDraftDialogVisible.value = true
  aiDraftViewMode.value = 'draft'
  aiPolishHistory.value = []
  aiPolishInstruction.value = ''
  aiContextSummary.value = t('stage6.components.messagesPage.aiDraft.analyzingContext')
  aiDraftReply.value = ''
  aiDraftSystemLanguageVersion.value = ''
  aiDraftSystemLanguageSource.value = ''
  aiDraftSystemLanguageRequestId += 1
  isAiTranslatingDraft.value = false

  try {
    const conversation = activeConversation.value
    if (!conversation || !activeThreadId.value) {
      throw new Error(t('stage6.components.messagesPage.errors.selectConversationFirst'))
    }

    const reservation = await loadReservationForAiDraft(conversation)
    const reservationId =
      normalizePositiveId(conversation.reservationId) ||
      normalizePositiveId(selectedReservationId.value) ||
      normalizePositiveId(reservation?.id)

    const response = (await generateSuMessagingAiReplyDraft(
      activeThreadId.value,
      {
        reservationId: reservationId || undefined,
        bookingId: conversation.bookingId?.trim() || undefined,
        externalThreadId: conversation.threadId?.trim() || undefined,
        channel: resolveAiDraftChannel(conversation),
        guestName: reservation?.guestName || conversation.guestName || undefined,
        roomId: normalizePositiveId(reservation?.roomId) || undefined,
        roomNumber: reservation?.roomNumber?.trim() || undefined,
        roomTypeName: reservation?.roomTypeName || conversation.roomTypeName || undefined,
        latestGuestMessageId: getLatestGuestMessageId(),
        recentMessages: buildAiDraftRecentMessages(),
        language: resolveAiDraftLanguage(),
      },
      {
        timeoutMs: AI_ASSISTANT_TIMEOUT_MS,
        suppressErrorToast: true,
      },
    )) as ApiResponse<SuMessagingAiReplyDraftResponse>

    const draftReply = response.data?.draftReply?.trim()
    if (response.success === false || !draftReply) {
      throw new Error(
        sanitizeUserFacingMessage(response.message) ||
          t('stage6.components.messagesPage.errors.generateFailed'),
      )
    }

    aiContextSummary.value = buildFallbackContextSummary()
    aiDraftReply.value = draftReply
    await syncSystemLanguageDraftVersion(draftReply)
  } catch (error) {
    console.error('Failed to generate AI draft:', error)
    aiContextSummary.value = buildFallbackContextSummary()
    aiDraftReply.value = ''
    aiDraftSystemLanguageVersion.value = ''
    aiDraftSystemLanguageSource.value = ''
    ElMessage.error(
      resolveAiErrorMessage(error, t('stage6.components.messagesPage.errors.aiDraftFailed')),
    )
  } finally {
    isAiGeneratingDraft.value = false
  }
}

const polishAiDraftReply = async () => {
  const instruction = aiPolishInstruction.value.trim()
  if (!instruction) {
    return
  }
  if (!aiDraftReply.value.trim()) {
    ElMessage.warning(t('stage6.components.messagesPage.errors.generateDraftFirst'))
    return
  }

  isAiPolishing.value = true
  aiPolishHistory.value.push({
    role: 'user',
    content: instruction,
  })

  try {
    const guestLanguageLabel = resolveLatestGuestLanguageLabel()
    const prompt = [
      'You are a hotel guest reply editor.',
      'Improve the following draft reply for the guest.',
      `Return only the final guest-facing reply in ${guestLanguageLabel}.`,
      `Do not switch to ${resolveSystemLanguageAiLabel()} unless the guest message is in that language.`,
      'Do not add explanations.',
      '',
      `Staff summary: ${aiContextSummary.value}`,
      '',
      'Current draft:',
      aiDraftReply.value,
      '',
      'Revision request:',
      instruction,
    ].join('\n')

    const response = (await sendChatMessage(
      {
        sessionId: aiAssistantSessionId.value,
        message: prompt,
      },
      {
        timeoutMs: AI_ASSISTANT_TIMEOUT_MS,
        suppressErrorToast: true,
      },
    )) as ApiResponse<{ reply: string; sessionId: string }>

    if (response.success === false || !response.data?.reply) {
      throw new Error(
        sanitizeUserFacingMessage(response.message) ||
          t('stage6.components.messagesPage.errors.rewriteFailed'),
      )
    }

    aiAssistantSessionId.value = response.data.sessionId || aiAssistantSessionId.value
    const polished = response.data.reply.trim()
    aiDraftReply.value = polished
    aiDraftViewMode.value = 'draft'
    aiDraftSystemLanguageRequestId += 1
    isAiTranslatingDraft.value = false
    aiDraftSystemLanguageSource.value = ''
    aiDraftSystemLanguageVersion.value = ''
    aiPolishHistory.value.push({
      role: 'assistant',
      content: polished,
    })
    aiPolishInstruction.value = ''
  } catch (error) {
    console.error('Failed to polish draft:', error)
    ElMessage.error(
      resolveAiErrorMessage(error, t('stage6.components.messagesPage.errors.polishFailed')),
    )
  } finally {
    isAiPolishing.value = false
  }
}

const createOptimisticMessage = (content: string): MessageItem => ({
  id: localMessageSeed--,
  threadId: activeThreadId.value || undefined,
  senderType: SuMessagingSenderType.STAFF,
  content,
  timestamp: new Date(),
  deliveryStatus: 'SENDING',
})

const replaceMessageById = (id: number, incoming: MessageItem) => {
  const nextMessages: MessageItem[] = []
  let replaced = false

  for (const message of messages.value) {
    if (message.id === id || message.id === incoming.id) {
      if (!replaced) {
        nextMessages.push({ ...message, ...incoming })
        replaced = true
      }
      continue
    }

    nextMessages.push(message)
  }

  if (!replaced) {
    nextMessages.push(incoming)
  }

  messages.value = mergeMessagesById(nextMessages)
}

const sendMessageContent = async (content: string) => {
  if (
    !activeThreadId.value ||
    !content.trim() ||
    isSending.value ||
    activeConversation.value?.closed
  ) {
    return false
  }

  isSending.value = true

  const optimistic = createOptimisticMessage(content)
  messages.value = mergeMessagesById([...messages.value, optimistic])
  await scrollToBottom()

  try {
    const response = (await sendSuThreadMessage(activeThreadId.value, {
      content,
    })) as ApiResponse<SuMessagingMessageDTO>

    if (response.success === false || !response.data) {
      throw new Error(
        sanitizeUserFacingMessage(response.message) ||
          t('stage6.components.messagesPage.message.sendFailed'),
      )
    }

    const sentMessage = mapMessage(response.data)
    replaceMessageById(optimistic.id, sentMessage)
    updateThreadSummary(activeThreadId.value, {
      lastMessage: content,
      lastActivity: response.data.timestamp,
      unreadCount: 0,
    })
    await scrollToBottom()
    await notificationCenterStore.refreshChatUnreadCount()
    return true
  } catch (error) {
    const index = messages.value.findIndex((message) => message.id === optimistic.id)
    if (index >= 0) {
      messages.value[index] = {
        ...messages.value[index],
        deliveryStatus: 'FAILED',
      }
    }
    const errorMessage =
      sanitizeUserFacingMessage(error instanceof Error ? error.message : '') ||
      t('stage6.components.messagesPage.message.sendFailed')
    console.error('Failed to send message:', errorMessage)
    ElMessage.error(errorMessage)
    return false
  } finally {
    isSending.value = false
  }
}

const sendMessage = async () => {
  const content = newMessage.value.trim()
  if (!content) {
    return
  }
  newMessage.value = ''
  await sendMessageContent(content)
}

const sendAiDraftReply = async () => {
  const content = aiDraftReply.value.trim()
  if (!content) {
    ElMessage.warning(t('stage6.components.messagesPage.errors.prepareDraftFirst'))
    return
  }

  const sent = await sendMessageContent(content)
  if (sent) {
    aiDraftDialogVisible.value = false
    aiPolishInstruction.value = ''
  }
}

const upsertMessage = (incoming: MessageItem) => {
  const existing = messages.value.find((item) => item.id === incoming.id)
  const merged = existing ? { ...existing, ...incoming } : incoming
  messages.value = mergeMessagesById([
    ...messages.value.filter((item) => item.id !== incoming.id),
    merged,
  ])
  if (translationEnabled.value) {
    void ensureMessageTranslation(incoming)
  }
}

const handleRealtimeEvent = async (event: SuMessagingRealtimeEvent) => {
  if (event.eventType !== 'MESSAGE_CREATED' && event.eventType !== 'MESSAGE_UPDATED') {
    return
  }

  const { threadId, message } = event
  if (!message) {
    return
  }

  if (
    event.eventType === 'MESSAGE_CREATED' &&
    String(message.senderType || '').toUpperCase() === 'GUEST' &&
    notificationCenterStore.settings.chatSound
  ) {
    try {
      if (!chatIncomingAudio) {
        chatIncomingAudio = new Audio('/sounds/chat-notification.mp3')
        chatIncomingAudio.preload = 'auto'
      }
      chatIncomingAudio.currentTime = 0
      await chatIncomingAudio.play()
    } catch (error) {
      console.warn('Failed to play inbox chat notification sound:', error)
    }
  }

  if (threadId === activeThreadId.value) {
    const mappedMessage = mapRealtimeMessage(message)
    upsertMessage(mappedMessage)
    updateThreadSummary(threadId, {
      lastMessage: mappedMessage.content,
      lastActivity: message.timestamp,
      unreadCount: 0,
    })
    await scrollToBottom()
  } else {
    const existing = conversations.value.find((conversation) => conversation.id === threadId)
    if (existing) {
      updateThreadSummary(threadId, {
        lastMessage: message.content,
        lastActivity: message.timestamp,
        unreadCount:
          String(message.senderType || '').toUpperCase() === 'GUEST'
            ? Number(existing.unreadCount || 0) + 1
            : existing.unreadCount,
      })
    }
  }

  await notificationCenterStore.refreshChatUnreadCount()
}

const clearReconnectTimer = () => {
  if (reconnectTimer) {
    window.clearTimeout(reconnectTimer)
    reconnectTimer = null
  }
}

const scheduleReconnect = () => {
  if (isDestroyed) {
    return
  }
  clearReconnectTimer()
  reconnectTimer = window.setTimeout(() => {
    connectRealtimeSocket()
  }, 3000)
}

const closeRealtimeSocket = () => {
  clearReconnectTimer()
  if (socket) {
    socket.onopen = null
    socket.onmessage = null
    socket.onclose = null
    socket.onerror = null
    socket.close()
    socket = null
  }
}

const connectRealtimeSocket = () => {
  closeRealtimeSocket()

  const token = getCurrentToken()
  const storeId = getCurrentStoreId()
  if (!token || !storeId) {
    return
  }

  socket = createSuMessagingSocket(token, storeId)
  socket.onmessage = (event) => {
    try {
      const payload = JSON.parse(event.data) as SuMessagingRealtimeEvent
      void handleRealtimeEvent(payload)
    } catch (error) {
      console.error('Failed to handle realtime message:', error)
    }
  }
  socket.onclose = () => {
    scheduleReconnect()
  }
}

const formatConversationTime = (dateString: string) => {
  const date = parseUtcDateTime(dateString)
  if (Number.isNaN(date.getTime())) {
    return '-'
  }

  const now = new Date()
  const diffDays = getDateDiffByStoreDay(date, now)

  if (diffDays === 0) {
    return getStoreTimeFormatter().format(date)
  }

  if (diffDays === 1) {
    return t('stage6.components.messagesPage.date.yesterday')
  }

  const [, month, day] = getStoreDateKey(date).split('-')
  return `${month}-${day}`
}

const formatMessageTime = (date: Date) => {
  if (Number.isNaN(date.getTime())) {
    return '-'
  }
  return getStoreTimeFormatter().format(date)
}

const formatStayDate = (dateText?: string) => {
  if (!dateText) {
    return '-'
  }
  return dateText
}

const getConversationStayInfo = (conversation: SuMessagingThreadDTO) => {
  const parts = [
    t('stage6.components.messagesPage.stay.checkIn', {
      date: formatStayDate(conversation.checkInDate),
    }),
    t('stage6.components.messagesPage.stay.checkOut', {
      date: formatStayDate(conversation.checkOutDate),
    }),
  ]

  if (conversation.roomTypeName) {
    parts.push(
      t('stage6.components.messagesPage.stay.roomType', { roomType: conversation.roomTypeName }),
    )
  }

  return parts.join(' · ')
}

const toComparableText = (value?: string | null) => (value || '').trim().toLowerCase()

const getRouteQueryText = (value: unknown) => {
  if (Array.isArray(value)) {
    return (value[0] || '').trim()
  }
  if (typeof value === 'string') {
    return value.trim()
  }
  return ''
}

const routeTarget = computed(() => {
  const reservationIdText = getRouteQueryText(route.query.reservationId)
  const reservationIdNumber = Number(reservationIdText)
  return {
    reservationId:
      reservationIdText && Number.isInteger(reservationIdNumber) && reservationIdNumber > 0
        ? reservationIdNumber
        : null,
    orderNumber: getRouteQueryText(route.query.orderNumber),
    channelOrderNumber: getRouteQueryText(route.query.channelOrderNumber),
    guestName: getRouteQueryText(route.query.guestName),
  }
})

const hasRouteTarget = computed(() => {
  const target = routeTarget.value
  return Boolean(
    target.reservationId || target.orderNumber || target.channelOrderNumber || target.guestName,
  )
})

const routeTargetKey = computed(() => {
  const target = routeTarget.value
  return [
    target.reservationId ? String(target.reservationId) : '',
    target.orderNumber,
    target.channelOrderNumber,
    target.guestName,
  ].join('|')
})

const normalizePositiveId = (value?: number | null) => {
  const normalized = Number(value)
  if (Number.isInteger(normalized) && normalized > 0) {
    return normalized
  }
  return null
}

const resolveConversationLookupKey = (conversation: SuMessagingThreadDTO) => {
  const key = (conversation.bookingId || conversation.threadId || '').trim()
  return key ? `${conversation.channelId}:${key}` : ''
}

const findReservationIdForConversation = async (conversation: SuMessagingThreadDTO) => {
  const directReservationId = normalizePositiveId(conversation.reservationId)
  if (directReservationId) {
    const lookupKey = resolveConversationLookupKey(conversation)
    if (lookupKey) {
      reservationIdCache.set(lookupKey, directReservationId)
    }
    return directReservationId
  }

  const lookupKey = resolveConversationLookupKey(conversation)
  if (!lookupKey) {
    return null
  }

  if (reservationIdCache.has(lookupKey)) {
    return reservationIdCache.get(lookupKey) ?? null
  }

  const keyword = (conversation.bookingId || conversation.threadId || '').trim()
  if (!keyword) {
    reservationIdCache.set(lookupKey, null)
    return null
  }

  try {
    const response = await getReservationsWithFilters({
      page: 0,
      size: 100,
      searchKeyword: keyword,
    })
    if (!response.success) {
      reservationIdCache.set(lookupKey, null)
      return null
    }

    const reservations = response.data?.content || []
    const normalizedKeyword = toComparableText(keyword)

    const exactMatch = reservations.find((item: ReservationDTO) => {
      return (
        toComparableText(item.channelOrderNumber) === normalizedKeyword ||
        toComparableText(item.orderNumber) === normalizedKeyword
      )
    })

    const fuzzyMatch = reservations.find((item: ReservationDTO) => {
      return (
        toComparableText(item.channelOrderNumber).includes(normalizedKeyword) ||
        toComparableText(item.orderNumber).includes(normalizedKeyword)
      )
    })

    const resolved = exactMatch?.id || fuzzyMatch?.id || null
    reservationIdCache.set(lookupKey, resolved)
    return resolved
  } catch (error) {
    console.error('Failed to match reservation:', error)
    reservationIdCache.set(lookupKey, null)
    return null
  }
}

const loadReservationForAiDraft = async (
  conversation: SuMessagingThreadDTO,
): Promise<ReservationDTO | null> => {
  let reservationId = normalizePositiveId(conversation.reservationId) || selectedReservationId.value
  if (!reservationId) {
    await preloadActiveReservationId()
    reservationId = selectedReservationId.value
  }
  if (!reservationId) {
    return null
  }

  try {
    const response = await getReservationById(reservationId)
    if (response.success && response.data) {
      return response.data
    }
  } catch (error) {
    console.error('Failed to load reservation for AI draft:', error)
  }
  return null
}

const matchesConversationKeyword = (conversation: SuMessagingThreadDTO, keyword: string) => {
  const normalizedKeyword = toComparableText(keyword)
  if (!normalizedKeyword) {
    return false
  }

  const bookingId = toComparableText(conversation.bookingId)
  const threadIdText = String(conversation.threadId || '')
  const threadId = toComparableText(threadIdText)
  if (
    bookingId &&
    (bookingId === normalizedKeyword ||
      bookingId.includes(normalizedKeyword) ||
      normalizedKeyword.includes(bookingId))
  ) {
    return true
  }

  if (
    threadId &&
    (threadId === normalizedKeyword ||
      threadId.includes(normalizedKeyword) ||
      normalizedKeyword.includes(threadId))
  ) {
    return true
  }

  return false
}

const resolveConversationByRouteTarget = async () => {
  if (!conversations.value.length) {
    return null
  }

  const target = routeTarget.value
  const bookingCandidates = [target.channelOrderNumber, target.orderNumber]
    .map((item) => item.trim())
    .filter(Boolean)

  for (const conversation of conversations.value) {
    for (const candidate of bookingCandidates) {
      if (matchesConversationKeyword(conversation, candidate)) {
        return conversation
      }
    }
  }

  const guestNameKeyword = toComparableText(target.guestName)
  if (guestNameKeyword) {
    const guestMatch = conversations.value.find((conversation) => {
      const conversationGuestName = toComparableText(conversation.guestName)
      return (
        conversationGuestName === guestNameKeyword ||
        conversationGuestName.includes(guestNameKeyword)
      )
    })
    if (guestMatch) {
      return guestMatch
    }
  }

  if (target.reservationId) {
    for (const conversation of conversations.value) {
      const resolvedReservationId = await findReservationIdForConversation(conversation)
      if (resolvedReservationId === target.reservationId) {
        return conversation
      }
    }
  }

  return null
}

const buildRouteTargetSearchText = async () => {
  const target = routeTarget.value
  const directCandidates = [target.channelOrderNumber, target.orderNumber, target.guestName]
    .map((item) => item.trim())
    .filter(Boolean)
  if (directCandidates.length) {
    return directCandidates[0]
  }

  if (!target.reservationId) {
    return ''
  }

  try {
    const response = await getReservationById(target.reservationId)
    if (!response.success || !response.data) {
      return ''
    }
    const reservation = response.data
    return (
      reservation.channelOrderNumber?.trim() ||
      reservation.orderNumber?.trim() ||
      reservation.guestName?.trim() ||
      ''
    )
  } catch (error) {
    console.error('Failed to load reservation for message route target:', error)
    return ''
  }
}

const disableUnreadFilterForRouteTarget = () => {
  if (!filterUnreadOnly.value) {
    return false
  }

  shouldSkipNextThreadFilterWatch = true
  filterUnreadOnly.value = false
  return true
}

const applyRouteConversationTarget = async () => {
  if (!hasRouteTarget.value || routeTargetHandled.value) {
    return
  }

  const preferredKeyword = await buildRouteTargetSearchText()
  const didDisableUnreadFilter = disableUnreadFilterForRouteTarget()
  if (preferredKeyword) {
    shouldSkipNextSearchQueryWatch = true
    searchQuery.value = preferredKeyword
    await fetchThreadPage(0, false)
  } else if (didDisableUnreadFilter) {
    await fetchThreadPage(0, false)
  }

  const matchedConversation = await resolveConversationByRouteTarget()
  if (!matchedConversation) {
    if (preferredKeyword) {
      ElMessage.warning(uiText('routeTargetNotFound'))
    }
    routeTargetHandled.value = true
    return
  }

  if (activeThreadId.value !== matchedConversation.id) {
    activeThreadId.value = matchedConversation.id
    await loadThreadMessages(matchedConversation.id)
  }
  routeTargetHandled.value = true
}

const preloadActiveReservationId = async () => {
  const conversation = activeConversation.value
  if (!conversation) {
    selectedReservationId.value = null
    return
  }

  isResolvingReservation.value = true
  try {
    selectedReservationId.value = await findReservationIdForConversation(conversation)
  } finally {
    isResolvingReservation.value = false
  }
}

const loadQuickReplies = async () => {
  try {
    quickReplyLoading.value = true
    const response = (await getAllQuickReplies()) as ApiResponse<QuickReplyDTO[]>
    if (response.success === false) {
      throw new Error(
        sanitizeUserFacingMessage(response.message) ||
          t('stage6.components.messagesPage.quickReplyDialog.loadFailed'),
      )
    }

    quickReplies.value = response.data || []
  } catch (error) {
    console.error('Failed to load quick replies:', error)
    ElMessage.error(t('stage6.components.messagesPage.quickReplyDialog.loadFailed'))
    quickReplies.value = []
  } finally {
    quickReplyLoading.value = false
  }
}

const openQuickReplyDialog = async () => {
  if (!activeConversation.value) {
    ElMessage.warning(t('stage6.components.messagesPage.errors.selectConversationFirst'))
    return
  }

  quickReplyDialogVisible.value = true
  quickReplySearchQuery.value = ''

  if (!quickReplies.value.length && !quickReplyLoading.value) {
    await loadQuickReplies()
  }
}

const resolveQuickReplyTemplateContext = async (): Promise<QuickReplyTemplateContext> => {
  const conversation = activeConversation.value
  const propertyName = conversation?.listingName || storeStore.currentStore?.name || null

  let reservation: ReservationDTO | null = null
  if (!selectedReservationId.value && conversation) {
    await preloadActiveReservationId()
  }

  if (selectedReservationId.value) {
    try {
      const response = await getReservationById(selectedReservationId.value)
      if (response.success && response.data) {
        reservation = response.data
      }
    } catch (error) {
      console.error('Failed to get reservation detail:', error)
    }
  }

  return {
    propertyName,
    guestName: reservation?.guestName || conversation?.guestName || null,
    guestPhone: reservation?.phone || null,
    checkInDate: reservation?.checkInDate || null,
    checkOutDate: reservation?.checkOutDate || null,
    roomTypeName: reservation?.roomTypeName || null,
    ratePlanName: reservation?.pricePlan || null,
  }
}

const handleQuickReplyPick = async (quickReply: QuickReplyDTO) => {
  if (!quickReply?.message) {
    return
  }
  if (!activeConversation.value) {
    ElMessage.warning(t('stage6.components.messagesPage.errors.selectConversationFirst'))
    return
  }

  if (isApplyingQuickReply.value) {
    return
  }

  isApplyingQuickReply.value = true
  try {
    const context = await resolveQuickReplyTemplateContext()
    const { rendered, missingKeys } = renderQuickReplyTemplate(quickReply.message, context)

    const trimmedExisting = newMessage.value.trim()
    const trimmedIncoming = rendered.trim()
    newMessage.value = trimmedExisting
      ? `${trimmedExisting}\n\n${trimmedIncoming}`
      : trimmedIncoming

    if (missingKeys.length) {
      ElMessage.warning(
        t('stage6.components.messagesPage.quickReplyDialog.missingVariablesWarning', {
          variables: formatMissingQuickReplyKeyLabels(missingKeys, t),
        }),
      )
    } else {
      ElMessage.success(t('stage6.components.messagesPage.quickReplyDialog.filled'))
    }

    quickReplyDialogVisible.value = false
  } finally {
    isApplyingQuickReply.value = false
  }
}

const openOrderDetail = async () => {
  if (!activeConversation.value) {
    return
  }

  if (!selectedReservationId.value) {
    await preloadActiveReservationId()
  }

  if (!selectedReservationId.value) {
    ElMessage.warning(t('stage6.components.messagesPage.errors.reservationNotMatched'))
    return
  }

  showOrderDetailDrawer.value = true
}

const clearThreadSearchTimer = () => {
  if (threadSearchTimer) {
    window.clearTimeout(threadSearchTimer)
    threadSearchTimer = null
  }
}

const scheduleThreadFilterSearch = () => {
  clearThreadSearchTimer()
  threadSearchTimer = window.setTimeout(() => {
    void applyThreadFilters()
  }, THREAD_SEARCH_DEBOUNCE_MS)
}

const handleThreadSearchEnter = () => {
  clearThreadSearchTimer()
  void applyThreadFilters()
}

const initialize = async () => {
  await notificationCenterStore.refreshChatUnreadCount()
  await applyThreadFilters()
  await applyRouteConversationTarget()
  connectRealtimeSocket()
}

watch(
  () => activeThreadId.value,
  () => {
    void preloadActiveReservationId()
  },
)

watch(
  () => filteredConversations.value,
  () => {
    syncActiveConversationWithFilteredList()
  },
)

watch(
  () => routeTargetKey.value,
  () => {
    routeTargetHandled.value = false
    void applyRouteConversationTarget()
  },
)

watch(
  () => searchQuery.value,
  () => {
    if (shouldSkipNextSearchQueryWatch) {
      shouldSkipNextSearchQueryWatch = false
      return
    }
    scheduleThreadFilterSearch()
  },
)

watch(
  () => [filterChannel.value, filterOrderStatuses.value.join(','), filterUnreadOnly.value],
  () => {
    if (shouldSkipNextThreadFilterWatch) {
      shouldSkipNextThreadFilterWatch = false
      return
    }
    clearThreadSearchTimer()
    void applyThreadFilters()
  },
)

onMounted(() => {
  clearExpiredTranslationCache()
  loadTranslationSettings()
  void initialize()
})

onUnmounted(() => {
  isDestroyed = true
  closeRealtimeSocket()
  clearMessageScrollTranslateTimer()
  clearThreadSearchTimer()
})
</script>

<style scoped>
.messages-page {
  display: flex;
  height: calc(100vh - 60px);
  background: #f5f5f5;
}

.conversations-panel {
  width: 320px;
  background: #fff;
  border-right: 1px solid #e8e8e8;
  display: flex;
  flex-direction: column;
}

.panel-header {
  padding: 20px;
  border-bottom: 1px solid #e8e8e8;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.panel-header h2 {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: #333;
}

.panel-header-actions {
  display: flex;
  align-items: center;
  gap: 4px;
}

.filters-panel {
  padding: 16px;
  border-bottom: 1px solid #e8e8e8;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.filter-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
}

.filter-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.selection-bar {
  min-height: 36px;
  padding: 8px 16px;
  border-bottom: 1px solid #e8e8e8;
  background: #f8fbff;
  color: #606266;
  font-size: 13px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.conversations-list {
  flex: 1;
  overflow-y: auto;
}

.conversation-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 16px;
  cursor: pointer;
  border-bottom: 1px solid #f5f5f5;
  transition: background-color 0.2s ease;
}

.conversation-item:hover,
.conversation-item.active {
  background: #f8fbff;
}

.conversation-item.selected {
  background: #f4f9ff;
}

.conversation-select {
  flex-shrink: 0;
  margin-top: 9px;
}

.conversation-avatar,
.channel-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: #e6f4ff;
  color: #1677ff;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.conversation-info,
.channel-details {
  min-width: 0;
  flex: 1;
}

.conversation-header,
.channel-info {
  display: flex;
  gap: 12px;
}

.conversation-header {
  justify-content: space-between;
  align-items: center;
}

.conversation-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

.channel-name {
  color: #303133;
  font-weight: 600;
}

.last-message,
.channel-status-text {
  margin-top: 6px;
  color: #909399;
  font-size: 13px;
  line-height: 1.5;
}

.last-message {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  white-space: pre-wrap;
  word-break: break-word;
}

.conversation-status {
  margin-top: 8px;
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.conversation-stay-info,
.conversation-closed-text {
  color: #909399;
  font-size: 12px;
}

.order-kind-tag {
  display: inline-flex;
  align-items: center;
  min-height: 20px;
  padding: 0 7px;
  border-radius: 4px;
  font-size: 12px;
  line-height: 1;
  background: #f4f4f5;
  color: #606266;
  border: 1px solid #e9e9eb;
}

.order-kind-inquiry {
  background: #fff7e6;
  color: #b26a00;
  border-color: #ffd591;
}

.order-kind-requested {
  background: #e6f4ff;
  color: #0958d9;
  border-color: #91caff;
}

.order-kind-confirmed {
  background: #f0f9eb;
  color: #3c8618;
  border-color: #b7eb8f;
}

.order-kind-cancelled {
  background: #fff1f0;
  color: #cf1322;
  border-color: #ffa39e;
}

.order-kind-completed {
  background: #f5f5f5;
  color: #595959;
  border-color: #d9d9d9;
}

.thread-pagination,
.load-earlier-messages {
  padding: 12px 16px;
  text-align: center;
  color: #909399;
  font-size: 13px;
}

.channel-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 20px;
  padding: 0 8px;
  border-radius: 4px;
  font-size: 12px;
  line-height: 1;
  color: #fff;
  font-weight: 600;
}

.channel-booking {
  background: #1f4f9d;
}

.channel-airbnb {
  background: #ff2b35;
}

.channel-default {
  background: #909399;
}

.message-time {
  font-size: 12px;
  color: #999;
  white-space: nowrap;
}

.unread-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 20px;
  height: 20px;
  padding: 0 6px;
  border-radius: 10px;
  background: #ff2b35;
  color: #fff;
  font-size: 12px;
  font-weight: 700;
  line-height: 1;
}

.messages-panel {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.messages-header {
  padding: 20px 24px;
  background: #fff;
  border-bottom: 1px solid #e8e8e8;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
}

.channel-status-text {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.header-actions {
  flex-shrink: 0;
}

.messages-list {
  flex: 1;
  overflow-y: auto;
  padding: 24px;
}

.message-date-divider {
  margin: 12px 0 20px;
  font-size: 12px;
  color: #909399;
  text-align: center;
}

.message-item {
  display: flex;
  align-items: flex-end;
  margin-bottom: 16px;
  gap: 8px;
}

.message-sent {
  justify-content: flex-end;
}

.message-received {
  justify-content: flex-start;
}

.message-content {
  max-width: min(75%, 560px);
}

.message-text {
  padding: 12px 16px;
  border-radius: 16px;
  line-height: 1.6;
  white-space: pre-line;
  overflow-wrap: anywhere;
}

.message-translation {
  margin-top: 10px;
  padding-top: 10px;
  border-top: 1px solid #ebeef5;
  color: #606266;
  white-space: pre-wrap;
  word-break: break-word;
  line-height: 1.6;
}

.message-link {
  text-decoration: underline;
  word-break: break-all;
}

.message-sent .message-link {
  color: #fff;
}

.message-received .message-link {
  color: #1677ff;
}

.message-delivery-indicator {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 16px;
  height: 16px;
  margin-bottom: 24px;
  font-size: 16px;
  flex-shrink: 0;
}

.message-delivery-indicator.sending {
  color: #8c8c8c;
}

.message-delivery-indicator.failed {
  color: #ff4d4f;
}

.spin {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

.message-sent .message-text {
  background: #1890ff;
  color: #fff;
  border-bottom-right-radius: 4px;
}

.message-received .message-text {
  background: #fff;
  color: #333;
  border: 1px solid #e8e8e8;
  border-bottom-left-radius: 4px;
}

.message-input-area {
  padding: 16px 24px;
  background: #fff;
  border-top: 1px solid #e8e8e8;
}

.input-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  margin-top: 12px;
}

.input-actions-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.quick-reply-toolbar {
  display: flex;
  gap: 12px;
  align-items: center;
  margin-bottom: 12px;
}

.quick-reply-empty {
  margin-top: 12px;
  color: #909399;
  font-size: 13px;
  text-align: center;
}

.ai-draft-section {
  margin-bottom: 16px;
}

.ai-draft-main-section {
  margin-bottom: 18px;
}

.ai-draft-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 10px;
}

.ai-draft-input-shell {
  min-height: 218px;
}

.ai-draft-reference-hint {
  margin-top: 8px;
  color: #909399;
  font-size: 12px;
  line-height: 1.5;
}

.ai-draft-section-divider {
  padding-top: 16px;
  border-top: 1px solid #ebeef5;
}

.ai-draft-label {
  margin-bottom: 8px;
  font-size: 13px;
  color: #606266;
}

.ai-polish-history {
  margin-bottom: 16px;
  max-height: 260px;
  overflow-y: auto;
  border: 1px solid #e8e8e8;
  border-radius: 8px;
  background: #fafafa;
  padding: 12px;
}

.ai-polish-item {
  margin-bottom: 12px;
  padding: 10px 12px;
  border-radius: 6px;
}

.ai-polish-item.user {
  background: #e6f4ff;
}

.ai-polish-item.assistant {
  background: #fff;
  border: 1px solid #f0f0f0;
}

.ai-polish-item .role {
  font-size: 12px;
  font-weight: 600;
  color: #909399;
  margin-bottom: 6px;
}

.ai-polish-item .content {
  line-height: 1.6;
  white-space: pre-wrap;
  color: #303133;
}

.ai-polish-empty {
  font-size: 13px;
  color: #909399;
}

.ai-polish-actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 12px;
}

.translation-dialog {
  display: flex;
  flex-direction: column;
  gap: 28px;
}

.translation-setting-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.translation-setting-block {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.translation-setting-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.translation-setting-desc {
  margin-top: 6px;
  font-size: 14px;
  color: #909399;
  line-height: 1.6;
}

.empty-state {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #909399;
  padding: 24px;
  text-align: center;
}

.conversations-list::-webkit-scrollbar,
.messages-list::-webkit-scrollbar {
  width: 6px;
}

.conversations-list::-webkit-scrollbar-thumb,
.messages-list::-webkit-scrollbar-thumb {
  background: #ddd;
  border-radius: 3px;
}

.conversations-list::-webkit-scrollbar-thumb:hover,
.messages-list::-webkit-scrollbar-thumb:hover {
  background: #bbb;
}
</style>
