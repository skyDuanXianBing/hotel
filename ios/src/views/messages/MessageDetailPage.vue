<template>
  <ion-page>
    <ion-header translucent>
      <ion-toolbar class="message-detail-header">
        <ion-buttons slot="start">
          <ion-back-button class="message-detail-header__back" :default-href="defaultBackHref" />
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
            {{ t('messageDetail.reservation') }}
          </ion-button>
        </ion-buttons>
      </ion-toolbar>
    </ion-header>

    <ion-content
      ref="contentRef"
      fullscreen
      class="mobile-page message-detail-page"
    >
      <ion-refresher slot="fixed" @ionRefresh="handleRefresh">
        <ion-refresher-content :pulling-text="t('messageDetail.pullToRefresh')" refreshing-spinner="crescent" />
      </ion-refresher>

      <div v-if="loadNotice || loading" class="message-detail-page__status">
        <ion-spinner v-if="loading" name="crescent" />
        <span>{{ loadNotice || t('messageDetail.syncing') }}</span>
      </div>

      <section
        v-if="messages.length > 0"
        ref="messageStreamRef"
        class="message-thread-stream"
      >
        <article
          v-for="message in messages"
          :key="message.id"
          v-memo="[
            translationEnabled,
            message.id,
            message.content,
            message.timestamp,
            message.deliveryStatus,
            getTranslatedMessageText(message),
            shouldShowTranslationPending(message),
            activeThreadAvatarVars,
          ]"
          :data-message-id="message.id"
          class="message-row"
          :class="message.senderType === MessageSenderType.STAFF ? 'is-staff' : 'is-guest'"
        >
          <div
            class="message-row__avatar"
            :style="
              message.senderType === MessageSenderType.STAFF
                ? undefined
                : activeThreadAvatarVars
            "
            aria-hidden="true"
          >
            <span>{{ resolveMessageAvatarLabel(message) }}</span>
          </div>
          <div class="message-row__body">
            <div class="message-bubble">
              <p class="message-bubble__text">
                <template v-for="(segment, index) in parseMessageContent(message.content)" :key="`${message.id}-${index}`">
                  <a
                    v-if="segment.type === 'link'"
                    class="message-bubble__link"
                    :href="segment.href"
                    target="_blank"
                    rel="noopener noreferrer"
                    @click.prevent="handleOpenMessageLink(segment.href)"
                  >
                    {{ segment.text }}
                  </a>
                  <span v-else>{{ segment.text }}</span>
                </template>
              </p>
            </div>
            <div v-if="shouldShowTranslatedMessage(message)" class="message-translation-card">
              <span class="message-translation-card__label">{{ t('messageDetail.translation') }}</span>
              <p>{{ getTranslatedMessageText(message) }}</p>
            </div>
            <div v-else-if="shouldShowTranslationPending(message)" class="message-translation-card is-loading">
              <ion-spinner name="crescent" />
              <span>{{ t('messageDetail.translating') }}</span>
            </div>
            <button
              v-else-if="shouldShowTranslationAction(message)"
              type="button"
              class="message-translation-action"
              @click.stop="handleTranslateMessage(message)"
            >
              <ion-icon :icon="languageOutline" />
              {{ t('messageDetail.translate') }}
            </button>
            <div class="message-row__meta">
              <span>{{ formatDateTime(message.timestamp) }}</span>
              <span v-if="message.deliveryStatus === 'FAILED'" class="message-row__failed">
                {{ t('messageDetail.sendFailed') }}
              </span>
            </div>
          </div>
        </article>
      </section>

      <section v-else-if="!loading" class="message-detail-empty-state">
        <h3>{{ t('messageDetail.emptyTitle') }}</h3>
        <p class="mobile-note">{{ t('messageDetail.emptyDescription') }}</p>
      </section>

      <div slot="fixed" class="message-composer">
        <p v-if="activeThread?.closed" class="message-composer__closed-tip">
          {{ t('messageDetail.closedTip') }}
        </p>
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
              :placeholder="t('messageDetail.composerPlaceholder')"
              :disabled="sending || !activeThread || activeThread.closed"
            />
          </div>
          <ion-button
            class="message-composer__send"
            :disabled="sending || !composerValue.trim() || !activeThread || activeThread.closed"
            @click="handleSendMessage"
          >
            {{ sending ? t('messageDetail.sending') : t('messageDetail.send') }}
          </ion-button>
        </div>
      </div>
    </ion-content>

    <ion-modal
      class="message-ai-modal"
      :is-open="aiDraftOpen"
      @didDismiss="handleDismissAiDraft"
    >
      <ion-header translucent class="message-ai-page__header">
        <ion-toolbar class="message-ai-page__toolbar">
          <ion-buttons slot="start">
            <ion-button
              class="message-ai-page__back"
              fill="clear"
              :aria-label="t('messageDetail.backToConversation')"
              @click="handleDismissAiDraft"
            >
              <ion-icon :icon="chevronBackOutline" />
              <span>{{ t('messageDetail.back') }}</span>
            </ion-button>
          </ion-buttons>
          <ion-title>{{ t('messageDetail.aiDraftTitle') }}</ion-title>
        </ion-toolbar>
      </ion-header>

      <ion-content class="mobile-page mobile-page--dashboard message-ai-page">
        <div class="message-ai-page-shell">
          <section class="message-ai-page-card">
            <section class="message-ai-section message-ai-section--draft">
              <h2 class="message-ai-section__title">
                {{
                  isAiDraftTranslationView
                    ? t('messageDetail.staffTranslation', { language: aiDraftTranslationLanguageLabel })
                    : t('messageDetail.initialDraft')
                }}
              </h2>

              <div class="message-ai-input-frame message-ai-textarea-shell">
                <ion-textarea
                  v-if="!isAiDraftTranslationView"
                  v-model="aiDraft"
                  :rows="8"
                  class="message-ai-textarea message-ai-textarea--draft"
                  :placeholder="
                    draftLoading ? t('messageDetail.draftGenerating') : t('messageDetail.draftPlaceholder')
                  "
                />
                <ion-textarea
                  v-else
                  :value="aiDraftTranslation"
                  :rows="8"
                  class="message-ai-textarea message-ai-textarea--draft message-ai-textarea--translation"
                  readonly
                />
                <div v-if="draftLoading" class="message-ai-textarea-shell__loading">
                  <ion-spinner name="crescent" />
                  <span>{{ t('messageDetail.generating') }}</span>
                </div>
              </div>

              <div class="message-ai-section__actions">
                <ion-button
                  class="message-ai-action-button"
                  :class="{ 'message-ai-action-button--return': isAiDraftTranslationView }"
                  :disabled="
                    draftLoading ||
                    aiPolishLoading ||
                    aiDraftTranslationLoading ||
                    !aiDraft.trim()
                  "
                  @click="handleToggleAiDraftTranslation"
                >
                  {{
                    aiDraftTranslationLoading
                      ? t('messageDetail.translationLoading')
                      : isAiDraftTranslationView
                        ? t('messageDetail.back')
                        : t('messageDetail.translate')
                  }}
                </ion-button>
              </div>
            </section>

            <section class="message-ai-section message-ai-section--instruction">
              <h2 class="message-ai-section__title">{{ t('messageDetail.requirements') }}</h2>
              <p class="message-ai-section__description">
                {{ t('messageDetail.requirementsDescription') }}
              </p>

              <div class="message-ai-input-frame">
                <ion-textarea
                  v-model="aiInstruction"
                  :rows="3"
                  class="message-ai-textarea message-ai-textarea--instruction"
                  :placeholder="t('messageDetail.requirementsPlaceholder')"
                />
              </div>

              <div class="message-ai-section__actions">
                <ion-button
                  class="message-ai-action-button"
                  :disabled="
                    draftLoading ||
                    aiPolishLoading ||
                    aiDraftTranslationLoading ||
                    !aiDraft.trim() ||
                    !aiInstruction.trim()
                  "
                  @click="handlePolishAiDraft"
                >
                  {{ aiPolishLoading ? t('messageDetail.polishing') : t('messageDetail.polish') }}
                </ion-button>
              </div>
            </section>

            <ion-button
              class="message-ai-fill-button"
              expand="block"
              :disabled="
                draftLoading || aiPolishLoading || aiDraftTranslationLoading || !aiDraft.trim()
              "
              @click="handleUseAiDraft"
            >
              {{ t('messageDetail.useDraft') }}
            </ion-button>
          </section>
        </div>
      </ion-content>
    </ion-modal>
  </ion-page>
</template>

<script setup lang="ts">
import {
  IonBackButton,
  IonButton,
  IonButtons,
  IonContent,
  IonHeader,
  IonIcon,
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
import { chevronBackOutline, languageOutline } from 'ionicons/icons'
import { computed, nextTick, onUnmounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute, useRouter } from 'vue-router'
import {
  getThreadMessages,
  getMessageThreads,
  MESSAGE_API_MOCK_ENABLED,
  pollThreadMessages,
  sendAiChatMessage,
  sendThreadMessage,
  translateThreadMessage,
} from '@/api/message'
import { getReservationsWithFilters, type ReservationDTO } from '@/api/reservation'
import { ROUTE_PATHS } from '@/router/guards'
import { useNotificationCenterStore } from '@/stores/notificationCenter'
import type { MessageDTO, MessageThreadDTO } from '@/types/message'
import { MessageSenderType } from '@/types/message'
import { createAsyncTaskQueue } from '@/utils/asyncTaskQueue'
import {
  getCachedMessageTranslation,
  setCachedMessageTranslation,
  type MessageTranslationCacheScope,
} from '@/utils/messageTranslationCache'
import { isHandledRequestError } from '@/utils/request'
import { showSuccessToast, showWarningToast } from '@/utils/notify'
import {
  loadMessageTranslationSettings,
  normalizeTranslatedText,
  requestAiMessageTranslation,
  resolveMessageTranslationLanguageLabel,
  type MessageTranslationLanguageValue,
} from '@/utils/messageTranslation'
import {
  resolveMessageThreadAvatarLabel,
  resolveMessageThreadAvatarVars,
} from '@/utils/messageThreadPresentation'
import { getStoredCurrentStoreId, getStoredUser } from '@/utils/storage'
import {
  compareStoreDateTimes,
  formatStoreDateTime,
} from '@/utils/storeBusinessDate'

const MESSAGE_POLL_INTERVAL = 8000
const INITIAL_TRANSLATION_BATCH_SIZE = 6
const VISIBLE_TRANSLATION_LIMIT = 8
const MESSAGE_TRANSLATION_SETTLE_MS = 180
const MESSAGE_TRANSLATION_TIMEOUT_MS = 45000

interface IonContentScrollTarget {
  scrollToBottom(duration?: number): Promise<void>
  getScrollElement?(): Promise<HTMLElement>
}

interface IonTextareaFocusTarget {
  setFocus?: () => Promise<void>
  getInputElement?: () => Promise<HTMLInputElement | HTMLTextAreaElement>
}

interface MessageTextSegment {
  type: 'text'
  text: string
}

interface MessageLinkSegment {
  type: 'link'
  text: string
  href: string
}

type MessageContentSegment = MessageTextSegment | MessageLinkSegment

const URL_PATTERN = /((?:https?:\/\/|www\.)[^\s<]+)/gi
const TRAILING_URL_PUNCTUATION_PATTERN = /[),.!?\]}]+$/

const route = useRoute()
const router = useRouter()
const { t } = useI18n()
const notificationCenterStore = useNotificationCenterStore()

function resolveRouteThreadId() {
  return Number(route.params.threadId || 0)
}

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
const aiPolishLoading = ref(false)
const aiDraftTranslationLoading = ref(false)
const aiDraftTranslation = ref('')
const aiDraftTranslationSource = ref('')
const aiDraftTranslationTarget = ref<MessageTranslationLanguageValue | null>(null)
const isAiDraftTranslationView = ref(false)
const translationEnabled = ref(false)
const translationTargetLanguage = ref<MessageTranslationLanguageValue>('zh-CN')
const translatedMessageMap = ref<Record<string, string>>({})
const translationPendingMap = ref<Record<string, boolean>>({})
const translationFailedMap = ref<Record<string, boolean>>({})
const contentRef = ref<unknown>(null)
const composerTextareaRef = ref<unknown>(null)
const messageStreamRef = ref<HTMLElement | null>(null)
const messageTranslationQueue = createAsyncTaskQueue(2)
const visibleMessageIds = new Set<number>()

let pollTimer = 0
let pageRequestToken = 0
let messageTranslationObserver: IntersectionObserver | null = null
let messageTranslationTimer = 0
let messagePageActive = false

const threadId = ref(resolveRouteThreadId())
const defaultBackHref = computed(() => {
  const routeBackHref = route.query.defaultHref
  if (typeof routeBackHref === 'string' && routeBackHref) {
    return routeBackHref
  }

  return ROUTE_PATHS.messages
})

function hasValidThreadId(value: number) {
  return Number.isInteger(value) && value > 0
}

function invalidatePageRequests() {
  pageRequestToken += 1
}

function isActivePageRequest(requestToken: number, expectedThreadId: number) {
  return requestToken === pageRequestToken && threadId.value === expectedThreadId
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
    return t('routes.MessageDetail')
  }

  return threadTitle.value
})

const threadTitle = computed(() => {
  if (!activeThread.value) {
    return t('routes.MessageDetail')
  }

  if (activeThread.value.guestName) {
    return activeThread.value.guestName
  }

  if (activeThread.value.listingName) {
    return activeThread.value.listingName
  }

  return activeThread.value.channelName || t('routes.MessageDetail')
})

const reservationMatched = computed(() => reservationId.value !== null)
const activeThreadAvatarVars = computed(() => {
  if (!activeThread.value) {
    return undefined
  }

  return resolveMessageThreadAvatarVars(activeThread.value)
})
const aiDraftTranslationLanguageLabel = computed(() =>
  resolveMessageTranslationLanguageLabel(translationTargetLanguage.value),
)

const headerCaption = computed(() => {
  if (!activeThread.value) {
    return ''
  }

  const parts: string[] = []

  if (activeThread.value.channelName) {
    parts.push(activeThread.value.channelName)
  }

  parts.push(activeThread.value.closed ? t('messageDetail.closed') : t('messageDetail.active'))

  if (reservationMatched.value) {
    parts.push(t('messageDetail.reservationLinked'))
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
    return compareStoreDateTimes(firstItem.timestamp, secondItem.timestamp)
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
    return message.senderName || t('messageDetail.staff')
  }

  return message.senderName || t('messageDetail.guest')
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
    return resolveAvatarCharacter(
      message.senderName || t('messageDetail.staffAvatar'),
      t('messageDetail.staffAvatar'),
    )
  }

  if (activeThread.value) {
    return resolveMessageThreadAvatarLabel(activeThread.value)
  }

  return resolveAvatarCharacter(resolveSenderLabel(message), t('messageDetail.guestAvatar'))
}

function formatDateTime(value: string) {
  return formatStoreDateTime(value, 'month-day-time', t('messageDetail.unknownTime'))
}

function normalizeExternalUrl(rawUrl: string) {
  const trimmedUrl = rawUrl.trim()
  if (!trimmedUrl) {
    return ''
  }

  if (/^https?:\/\//i.test(trimmedUrl)) {
    return trimmedUrl
  }

  return `https://${trimmedUrl}`
}

function splitTrailingUrlPunctuation(rawUrl: string) {
  const trailingMatch = rawUrl.match(TRAILING_URL_PUNCTUATION_PATTERN)
  if (!trailingMatch) {
    return {
      normalizedUrl: rawUrl,
      trailingText: '',
    }
  }

  return {
    normalizedUrl: rawUrl.slice(0, -trailingMatch[0].length),
    trailingText: trailingMatch[0],
  }
}

function parseMessageContent(content: string) {
  const segments: MessageContentSegment[] = []
  let lastIndex = 0
  let match: RegExpExecArray | null

  URL_PATTERN.lastIndex = 0

  while ((match = URL_PATTERN.exec(content)) !== null) {
    const matchedText = match[0]
    const matchStart = match.index
    const matchEnd = matchStart + matchedText.length

    if (matchStart > lastIndex) {
      segments.push({
        type: 'text',
        text: content.slice(lastIndex, matchStart),
      })
    }

    const { normalizedUrl, trailingText } = splitTrailingUrlPunctuation(matchedText)
    if (normalizedUrl) {
      segments.push({
        type: 'link',
        text: normalizedUrl,
        href: normalizeExternalUrl(normalizedUrl),
      })
    }

    if (trailingText) {
      segments.push({
        type: 'text',
        text: trailingText,
      })
    }

    lastIndex = matchEnd
  }

  if (lastIndex < content.length) {
    segments.push({
      type: 'text',
      text: content.slice(lastIndex),
    })
  }

  if (segments.length === 0) {
    segments.push({
      type: 'text',
      text: content,
    })
  }

  return segments
}

function handleOpenMessageLink(url: string) {
  if (!url) {
    return
  }

  window.open(url, '_blank', 'noopener,noreferrer')
}

function buildConversationContext() {
  const historyLines: string[] = []
  const recentMessages = messages.value.slice(-20)

  for (const message of recentMessages) {
    const role =
      message.senderType === MessageSenderType.STAFF
        ? t('messageDetail.staff')
        : t('messageDetail.guest')
    historyLines.push(`[${role}] ${message.content}`)
  }

  const header = [
    t('messageDetail.contextChannel', { value: activeThread.value?.channelName || '-' }),
    t('messageDetail.contextReservation', {
      value: activeThread.value?.bookingId || activeThread.value?.threadId || '-',
    }),
    t('messageDetail.contextStatus', {
      value: activeThread.value?.closed
        ? t('messageDetail.closed')
        : t('messageDetail.contextActive'),
    }),
  ].join('；')

  return `${header}\n\n${t('messageDetail.contextHistory')}\n${
    historyLines.join('\n') || t('messageDetail.noHistory')
  }`
}

function parseAiDraft(rawReply: string) {
  const contextMatch = rawReply.match(/\[CONTEXT\]([\s\S]*?)\[\/CONTEXT\]/i)
  const draftMatch = rawReply.match(/\[DRAFT\]([\s\S]*?)\[\/DRAFT\]/i)

  return {
    contextSummary: contextMatch?.[1]?.trim() || '',
    draftReply: draftMatch?.[1]?.trim() || rawReply.trim(),
  }
}

function syncTranslationSettingsFromStorage() {
  const settings = loadMessageTranslationSettings()
  const shouldClearCaches =
    translationEnabled.value !== settings.enabled ||
    translationTargetLanguage.value !== settings.targetLanguage

  translationEnabled.value = settings.enabled
  translationTargetLanguage.value = settings.targetLanguage

  if (shouldClearCaches) {
    clearTranslationCaches()
    clearAiDraftTranslation()
  }
}

function clearTranslationCaches() {
  stopMessageTranslations()
  translatedMessageMap.value = {}
  translationFailedMap.value = {}
}

function getMessageTranslationKey(message: MessageDTO) {
  return `message:${message.id}:${translationTargetLanguage.value}:${message.content}`
}

function getTranslatedMessageText(message: MessageDTO) {
  return translatedMessageMap.value[getMessageTranslationKey(message)] || ''
}

function markTranslationPending(key: string, pending: boolean) {
  if (pending) {
    translationPendingMap.value[key] = true
    return
  }

  delete translationPendingMap.value[key]
}

function shouldShowTranslatedMessage(message: MessageDTO) {
  if (!translationEnabled.value) {
    return false
  }

  const translated = getTranslatedMessageText(message)
  return Boolean(translated && translated !== message.content.trim())
}

function shouldShowTranslationPending(message: MessageDTO) {
  if (!translationEnabled.value) {
    return false
  }

  const key = getMessageTranslationKey(message)
  return Boolean(translationPendingMap.value[key] && !getTranslatedMessageText(message))
}

function shouldShowTranslationAction(message: MessageDTO) {
  const key = getMessageTranslationKey(message)
  return Boolean(
    translationEnabled.value &&
      message.content.trim() &&
      !getTranslatedMessageText(message) &&
      !shouldShowTranslationPending(message) &&
      (message.senderType === MessageSenderType.STAFF || translationFailedMap.value[key]),
  )
}

function getTranslationCacheScope(): MessageTranslationCacheScope {
  return {
    storeId: getStoredCurrentStoreId(),
    userId: getStoredUser()?.id,
    targetLanguage: translationTargetLanguage.value,
  }
}

async function requestMessageTranslation(message: MessageDTO, signal: AbortSignal) {
  if (MESSAGE_API_MOCK_ENABLED) {
    return requestAiMessageTranslation(message.content, translationTargetLanguage.value, {
      signal,
      suppressErrorToast: true,
    })
  }

  const messageThreadId = message.threadId || threadId.value
  const response = await translateThreadMessage(
    messageThreadId,
    message.id,
    {
      targetLanguage: translationTargetLanguage.value,
    },
    {
      timeoutMs: MESSAGE_TRANSLATION_TIMEOUT_MS,
      signal,
      suppressErrorToast: true,
    },
  )
  const translatedText = normalizeTranslatedText(response.data?.translatedContent || '')
  if (!response.success || !translatedText) {
    throw new Error(response.message || t('messageDetail.translationFailed'))
  }
  return translatedText
}

async function isConversationNearBottom() {
  const scrollTarget = resolveContentScrollTarget()
  if (!scrollTarget?.getScrollElement) {
    return false
  }

  try {
    const scrollElement = await scrollTarget.getScrollElement()
    return (
      scrollElement.scrollHeight - scrollElement.scrollTop - scrollElement.clientHeight <= 96
    )
  } catch {
    return false
  }
}

async function applyMessageTranslation(message: MessageDTO, translatedText: string) {
  const normalizedTranslation = translatedText.trim()
  if (!normalizedTranslation) {
    return
  }

  const key = getMessageTranslationKey(message)
  const shouldPinToBottom = await isConversationNearBottom()
  if (
    !messagePageActive ||
    !translationEnabled.value ||
    key !== getMessageTranslationKey(message)
  ) {
    return
  }

  translatedMessageMap.value[key] = normalizedTranslation
  setCachedMessageTranslation(
    getTranslationCacheScope(),
    message.content,
    normalizedTranslation,
  )

  if (shouldPinToBottom) {
    await scrollToConversationBottom()
  }
}

interface EnsureMessageTranslationOptions {
  manual?: boolean
  priority?: number
}

function ensureMessageTranslation(
  message: MessageDTO,
  options: EnsureMessageTranslationOptions = {},
) {
  const sourceText = message.content.trim()
  if (
    !messagePageActive ||
    !translationEnabled.value ||
    !sourceText ||
    (!options.manual && message.senderType !== MessageSenderType.GUEST)
  ) {
    return
  }

  const key = getMessageTranslationKey(message)
  if (translatedMessageMap.value[key] || messageTranslationQueue.has(key)) {
    return
  }
  delete translationFailedMap.value[key]

  const cachedTranslation = getCachedMessageTranslation(getTranslationCacheScope(), sourceText)
  if (cachedTranslation) {
    void applyMessageTranslation(message, cachedTranslation)
    return
  }

  const enqueued = messageTranslationQueue.enqueue({
    key,
    priority: options.priority,
    run: (signal) => requestMessageTranslation(message, signal),
    onSuccess: (translatedText) => {
      delete translationFailedMap.value[key]
      void applyMessageTranslation(message, translatedText).finally(() => {
        markTranslationPending(key, false)
      })
    },
    onError: (error) => {
      markTranslationPending(key, false)
      translationFailedMap.value[key] = true
      console.error('翻译消息失败:', error)
      if (options.manual) {
        showWarningToast(resolveWarningMessage(error, t('messageDetail.translationFailed')))
      }
    },
  })
  if (enqueued) {
    markTranslationPending(key, true)
  }
}

function translateMessages(items: MessageDTO[], priority = 0) {
  for (let index = 0; index < items.length; index += 1) {
    ensureMessageTranslation(items[index], {
      priority: priority + index,
    })
  }
}

function translateCurrentConversation() {
  if (!messagePageActive || !translationEnabled.value) {
    return
  }

  const latestGuestMessages = sortMessages(messages.value)
    .filter((message) => message.senderType === MessageSenderType.GUEST)
    .slice(-INITIAL_TRANSLATION_BATCH_SIZE)
  translateMessages(latestGuestMessages, 100)
}

function clearMessageTranslationTimer() {
  if (messageTranslationTimer) {
    window.clearTimeout(messageTranslationTimer)
    messageTranslationTimer = 0
  }
}

function translateVisibleMessages() {
  if (!messagePageActive || !translationEnabled.value) {
    return
  }

  const visibleGuestMessages = messages.value
    .filter(
      (message) =>
        message.senderType === MessageSenderType.GUEST && visibleMessageIds.has(message.id),
    )
    .slice(-VISIBLE_TRANSLATION_LIMIT)
  translateMessages(visibleGuestMessages, 200)
}

function scheduleVisibleMessageTranslations() {
  clearMessageTranslationTimer()
  if (!messagePageActive || !translationEnabled.value) {
    return
  }

  messageTranslationTimer = window.setTimeout(() => {
    messageTranslationTimer = 0
    translateVisibleMessages()
  }, MESSAGE_TRANSLATION_SETTLE_MS)
}

function refreshMessageTranslationObserver() {
  messageTranslationObserver?.disconnect()
  visibleMessageIds.clear()

  if (!messagePageActive || !messageStreamRef.value) {
    return
  }

  if (typeof IntersectionObserver === 'undefined') {
    translateCurrentConversation()
    return
  }

  if (!messageTranslationObserver) {
    messageTranslationObserver = new IntersectionObserver(
      (entries) => {
        for (const entry of entries) {
          const messageId = Number((entry.target as HTMLElement).dataset.messageId)
          if (!Number.isInteger(messageId)) {
            continue
          }
          if (entry.isIntersecting) {
            visibleMessageIds.add(messageId)
          } else {
            visibleMessageIds.delete(messageId)
          }
        }
        scheduleVisibleMessageTranslations()
      },
      {
        threshold: 0.35,
      },
    )
  }

  const messageElements =
    messageStreamRef.value.querySelectorAll<HTMLElement>('.message-row[data-message-id]')
  messageElements.forEach((element) => messageTranslationObserver?.observe(element))
}

function stopMessageTranslations() {
  clearMessageTranslationTimer()
  messageTranslationQueue.cancelAll()
  messageTranslationObserver?.disconnect()
  visibleMessageIds.clear()
  translationPendingMap.value = {}
}

function handleTranslateMessage(message: MessageDTO) {
  ensureMessageTranslation(message, {
    manual: true,
    priority: 500,
  })
}

function getLatestTimestamp() {
  if (messages.value.length === 0) {
    return ''
  }

  return messages.value[messages.value.length - 1].timestamp
}

function findThreadById(threadItems: MessageThreadDTO[], targetThreadId: number) {
  for (const item of threadItems) {
    if (item.id === targetThreadId) {
      return item
    }
  }

  return null
}

async function loadThreads(requestToken?: number, expectedThreadId?: number) {
  const response = await getMessageThreads()
  if (!response.success || !response.data) {
    throw new Error(response.message || t('messageDetail.loadConversationFailed'))
  }

  if (
    typeof requestToken === 'number' &&
    typeof expectedThreadId === 'number' &&
    !isActivePageRequest(requestToken, expectedThreadId)
  ) {
    return null
  }

  threads.value = response.data
  notificationCenterStore.syncMessageThreads(threads.value)
  return response.data
}

async function loadMessages(expectedThreadId: number, requestToken?: number) {
  const response = await getThreadMessages(expectedThreadId)
  if (!response.success || !response.data) {
    throw new Error(response.message || t('messageDetail.loadMessagesFailed'))
  }

  if (
    typeof requestToken === 'number' &&
    !isActivePageRequest(requestToken, expectedThreadId)
  ) {
    return false
  }

  messages.value = sortMessages(response.data)
  return true
}

async function resolveReservationId(
  targetThread: MessageThreadDTO | null,
  expectedThreadId: number,
  requestToken?: number,
) {
  reservationId.value = null
  if (MESSAGE_API_MOCK_ENABLED) {
    return
  }

  if (!targetThread) {
    return
  }

  const keyword = (targetThread.bookingId || targetThread.threadId || '').trim()
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

  if (
    typeof requestToken === 'number' &&
    !isActivePageRequest(requestToken, expectedThreadId)
  ) {
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

function startPolling(expectedThreadId: number, requestToken: number) {
  stopPolling()

  if (!hasValidThreadId(expectedThreadId)) {
    return
  }

  pollTimer = window.setInterval(async () => {
    if (!isActivePageRequest(requestToken, expectedThreadId)) {
      stopPolling()
      return
    }

    const latestTimestamp = getLatestTimestamp()
    if (!latestTimestamp) {
      return
    }

    try {
      const response = await pollThreadMessages(expectedThreadId, latestTimestamp)
      if (!response.success || !response.data || response.data.length === 0) {
        return
      }

      if (!isActivePageRequest(requestToken, expectedThreadId)) {
        return
      }

      const existing = [...messages.value]
      const newMessages: MessageDTO[] = []
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
          newMessages.push(incoming)
        }
      }

      messages.value = sortMessages(existing)
      if (translationEnabled.value && newMessages.length > 0) {
        translateMessages(
          newMessages.filter((message) => message.senderType === MessageSenderType.GUEST),
          300,
        )
      }
      const nextThreads = await loadThreads(requestToken, expectedThreadId)
      if (!nextThreads || !isActivePageRequest(requestToken, expectedThreadId)) {
        return
      }

      await scrollToConversationBottom(180)
      await nextTick()
      refreshMessageTranslationObserver()
      if (!activeThread.value) {
        stopPolling()
      }
    } catch {
      stopPolling()
    }
  }, MESSAGE_POLL_INTERVAL)
}

async function loadPage() {
  const currentThreadId = threadId.value
  const requestToken = ++pageRequestToken

  if (!hasValidThreadId(currentThreadId)) {
    stopPolling()
    loadNotice.value = t('messageDetail.missingThreadId')
    return
  }

  stopPolling()
  stopMessageTranslations()
  loading.value = true
  loadNotice.value = ''

  try {
    const nextThreads = await loadThreads(requestToken, currentThreadId)
    if (!nextThreads || !isActivePageRequest(requestToken, currentThreadId)) {
      return
    }

    const messagesLoaded = await loadMessages(currentThreadId, requestToken)
    if (!messagesLoaded || !isActivePageRequest(requestToken, currentThreadId)) {
      return
    }

    const targetThread = findThreadById(nextThreads, currentThreadId)
    await resolveReservationId(targetThread, currentThreadId, requestToken)
    if (!isActivePageRequest(requestToken, currentThreadId)) {
      return
    }

    startPolling(currentThreadId, requestToken)
    await scrollToConversationBottom()
    await nextTick()
    refreshMessageTranslationObserver()
    translateCurrentConversation()
  } catch (error) {
    if (!isActivePageRequest(requestToken, currentThreadId)) {
      return
    }
    loadNotice.value = resolveWarningMessage(error, t('messageDetail.pageLoadFailed'))
    if (!isHandledRequestError(error)) {
      showWarningToast(loadNotice.value)
    }
  } finally {
    if (requestToken === pageRequestToken) {
      loading.value = false
    }
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
      senderName: t('messageDetail.staff'),
    })
    if (!response.success || !response.data) {
      throw new Error(response.message || t('messageDetail.sendMessageFailed'))
    }

    composerValue.value = ''
    messages.value = sortMessages([...messages.value, response.data])
    await loadThreads()
    await scrollToConversationBottom(180)
    await nextTick()
    refreshMessageTranslationObserver()
    showSuccessToast(t('messageDetail.sent'))
  } catch (error) {
    if (!isHandledRequestError(error)) {
      showWarningToast(resolveWarningMessage(error, t('messageDetail.sendMessageFailed')))
    }
  } finally {
    sending.value = false
  }
}

async function handleOpenReservation() {
  if (!reservationId.value) {
    showWarningToast(t('messageDetail.reservationMissing'))
    return
  }

  await router.push({
    name: 'OrderReservationDetail',
    params: { reservationId: reservationId.value },
    query: { defaultHref: route.fullPath },
  })
}

async function handleGenerateAiDraft() {
  if (!activeThread.value || activeThread.value.closed) {
    showWarningToast(t('messageDetail.draftUnavailable'))
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
      throw new Error(response.message || t('messageDetail.draftFailed'))
    }

    aiSessionId.value = response.data.sessionId || aiSessionId.value
    const parsed = parseAiDraft(response.data.reply)
    aiContextSummary.value = parsed.contextSummary || t('messageDetail.contextSummary')
    aiDraft.value = parsed.draftReply
    clearAiDraftTranslation()
  } catch (error) {
    if (!isHandledRequestError(error)) {
      showWarningToast(resolveWarningMessage(error, t('messageDetail.draftFailed')))
    }
  } finally {
    draftLoading.value = false
  }
}

async function handleOpenAiDraft() {
  isAiDraftTranslationView.value = false
  aiDraftOpen.value = true
  if (!aiDraft.value.trim()) {
    await handleGenerateAiDraft()
  }
}

function handleUseAiDraft() {
  composerValue.value = aiDraft.value.trim()
  aiDraftOpen.value = false
}

function handleDismissAiDraft() {
  aiDraftOpen.value = false
  isAiDraftTranslationView.value = false
}

function clearAiDraftTranslation() {
  aiDraftTranslation.value = ''
  aiDraftTranslationSource.value = ''
  aiDraftTranslationTarget.value = null
  isAiDraftTranslationView.value = false
}

async function handleToggleAiDraftTranslation() {
  if (isAiDraftTranslationView.value) {
    isAiDraftTranslationView.value = false
    return
  }

  const draft = aiDraft.value.trim()
  if (!draft) {
    showWarningToast(t('messageDetail.noDraftToTranslate'))
    return
  }

  if (
    aiDraftTranslation.value &&
    aiDraftTranslationSource.value === draft &&
    aiDraftTranslationTarget.value === translationTargetLanguage.value
  ) {
    isAiDraftTranslationView.value = true
    return
  }

  aiDraftTranslationLoading.value = true
  try {
    const translatedDraft = await requestAiMessageTranslation(
      draft,
      translationTargetLanguage.value,
    )
    if (!translatedDraft) {
      throw new Error(t('messageDetail.emptyTranslation'))
    }

    aiDraftTranslation.value = translatedDraft
    aiDraftTranslationSource.value = draft
    aiDraftTranslationTarget.value = translationTargetLanguage.value
    isAiDraftTranslationView.value = true
  } catch (error) {
    if (!isHandledRequestError(error)) {
      showWarningToast(resolveWarningMessage(error, t('messageDetail.draftTranslationFailed')))
    }
  } finally {
    aiDraftTranslationLoading.value = false
  }
}

async function handlePolishAiDraft() {
  const instruction = aiInstruction.value.trim()
  if (!instruction) {
    showWarningToast(t('messageDetail.requirementsRequired'))
    return
  }

  if (!aiDraft.value.trim()) {
    showWarningToast(t('messageDetail.noDraftToPolish'))
    return
  }

  aiPolishLoading.value = true

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
      throw new Error(response.message || t('messageDetail.polishFailed'))
    }

    aiSessionId.value = response.data.sessionId || aiSessionId.value
    aiDraft.value = response.data.reply.trim()
    clearAiDraftTranslation()
  } catch (error) {
    if (!isHandledRequestError(error)) {
      showWarningToast(resolveWarningMessage(error, t('messageDetail.polishFailed')))
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
  threadId.value = resolveRouteThreadId()
  messagePageActive = true
  syncTranslationSettingsFromStorage()
  await loadPage()
})

onIonViewWillLeave(() => {
  messagePageActive = false
  invalidatePageRequests()
  stopPolling()
  stopMessageTranslations()
})

onUnmounted(() => {
  messagePageActive = false
  invalidatePageRequests()
  stopPolling()
  stopMessageTranslations()
  messageTranslationObserver = null
})
</script>

<style scoped>
.message-detail-page {
  display: block;
  --padding-top: 8px;
  --padding-start: 0;
  --padding-end: 0;
  --padding-bottom: 90px;
  --background: var(--ios-pms-dashboard-page-background);
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
  --color: var(--ios-pms-header-control-color);
}

.message-detail-header__title {
  color: var(--ios-pms-header-title-color);
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
  font-weight: 500;
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
  --color: var(--ios-pms-header-control-color);
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
  --message-opposite-edge-inset: 18px;
  padding: 4px 16px 28px;
}

.message-row {
  display: flex;
  align-items: flex-end;
  gap: 10px;
  width: 100%;
  min-width: 0;
}

.message-row.is-staff {
  flex-direction: row-reverse;
}

.message-row__avatar {
  position: relative;
  flex-shrink: 0;
  display: grid;
  place-items: center;
  width: 46px;
  height: 46px;
  border-radius: 50%;
  color: #ffffff;
  font-size: 18px;
  font-weight: 700;
  letter-spacing: -0.03em;
  box-shadow: 0 8px 18px rgba(15, 23, 42, 0.08);
}

.message-row__avatar::after {
  content: '';
  position: absolute;
  inset: 5px;
  border: 1px solid rgba(255, 255, 255, 0.82);
  border-radius: 50%;
}

.message-row__avatar span {
  position: relative;
  z-index: 1;
}

.message-row.is-guest .message-row__avatar {
  background: linear-gradient(
    180deg,
    var(--thread-avatar-start, #8dbdff),
    var(--thread-avatar-end, #4a98ff)
  );
}

.message-row.is-staff .message-row__avatar {
  background: linear-gradient(180deg, #6ee522 0%, #42c900 100%);
}

.message-row__body {
  flex: 1 1 auto;
  display: flex;
  flex-direction: column;
  gap: 6px;
  min-width: 0;
  max-width: calc(100% - 46px - var(--message-opposite-edge-inset));
  align-items: flex-start;
}

.message-row.is-staff .message-row__body {
  align-items: flex-end;
}

.message-bubble {
  width: fit-content;
  max-width: 100%;
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
  overflow-wrap: anywhere;
  word-break: break-word;
  line-height: 1.56;
  font-size: 15px;
}

.message-bubble__link {
  color: #1f6feb;
  text-decoration: underline;
  text-decoration-thickness: 1.5px;
  text-underline-offset: 2px;
  overflow-wrap: anywhere;
  word-break: break-word;
}

.message-translation-card {
  display: grid;
  gap: 6px;
  width: fit-content;
  max-width: 100%;
  padding: 10px 12px;
  border: 1px solid rgba(129, 140, 248, 0.2);
  border-radius: 16px;
  background: rgba(238, 242, 255, 0.92);
  color: #35405a;
  box-shadow: 0 8px 18px rgba(79, 70, 229, 0.08);
}

.message-row.is-staff .message-translation-card {
  background: rgba(250, 255, 243, 0.92);
  border-color: rgba(124, 184, 80, 0.2);
}

.message-translation-card__label {
  color: #6b7280;
  font-size: 11px;
  font-weight: 700;
  line-height: 1;
}

.message-translation-card p {
  margin: 0;
  white-space: pre-wrap;
  overflow-wrap: anywhere;
  word-break: break-word;
  font-size: 13px;
  line-height: 1.55;
}

.message-translation-card.is-loading {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  color: #7a8598;
  font-size: 12px;
}

.message-translation-card.is-loading ion-spinner {
  width: 13px;
  height: 13px;
}

.message-translation-action {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  width: fit-content;
  min-height: 28px;
  padding: 0 6px 0 4px;
  border: 0;
  border-radius: 6px;
  background: transparent;
  color: #667085;
  font-size: 12px;
  font-weight: 500;
  letter-spacing: 0;
}

.message-translation-action ion-icon {
  font-size: 15px;
}

.message-translation-action:active {
  background: rgba(102, 112, 133, 0.08);
}

.message-row.is-staff .message-translation-action {
  align-self: flex-end;
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
  padding: 10px 12px 12px;
  background: linear-gradient(
    180deg,
    rgba(247, 250, 255, 0) 0%,
    rgba(247, 250, 255, 0.94) 28%,
    var(--ios-pms-bg-page-plain) 100%
  );
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
  gap: 10px;
  align-items: flex-end;
}

.message-composer__ai,
.message-composer__send {
  margin: 0;
  min-height: 44px;
  height: 44px;
  font-size: 14px;
  font-weight: 700;
}

.message-composer__ai {
  --background: #2f9cff;
  --background-activated: #2189ec;
  --background-hover: #2f9cff;
  --box-shadow: none;
  --color: #ffffff;
  --padding-start: 0;
  --padding-end: 0;
  --border-radius: 50%;
  width: 44px;
  min-width: 44px;
}

.message-composer__input {
  min-width: 0;
  min-height: 44px;
  border-radius: 12px;
  background: #ffffff;
  box-shadow: 0 4px 14px rgba(68, 91, 132, 0.06);
  overflow: hidden;
}

.message-composer__textarea {
  margin: 0;
  min-height: 44px;
  --composer-textarea-max-height: 136px;
  --background: #ffffff;
  --padding-start: 12px;
  --padding-end: 12px;
  --padding-top: 11px;
  --padding-bottom: 11px;
  --color: #182231;
  --placeholder-color: #a0a8b6;
  font-size: 15px;
  line-height: 22px;
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
  --background: #31c85c;
  --background-activated: #29af4f;
  --background-hover: #31c85c;
  --box-shadow: none;
  --color: #ffffff;
  --border-radius: 11px;
  --padding-start: 14px;
  --padding-end: 14px;
  min-width: 64px;
}

.message-composer__send[disabled] {
  opacity: 0.52;
}

ion-modal.message-ai-modal {
  --width: 100%;
  --height: 100%;
  --border-radius: 0;
  --background: var(--ios-pms-dashboard-page-background);
}

.message-ai-page__header {
  backdrop-filter: blur(14px);
}

.message-ai-page__toolbar {
  --background: rgba(255, 255, 255, 0.94);
  --border-width: 0;
  --padding-start: 2px;
  --padding-end: 2px;
}

.message-ai-page__toolbar ion-title {
  color: var(--ios-pms-header-title-color);
  font-size: var(--ios-pms-font-title-xl-size);
  font-weight: 500;
  letter-spacing: 0;
}

.message-ai-page__back {
  --color: var(--ios-pms-header-control-color);
  --padding-start: 4px;
  --padding-end: 8px;
  min-width: 72px;
  margin: 0;
  font-size: var(--ios-pms-font-title-md-size);
  font-weight: 400;
  letter-spacing: 0;
}

.message-ai-page__back::part(native) {
  background: transparent;
  box-shadow: none;
}

.message-ai-page__back ion-icon {
  margin-right: 1px;
  font-size: 24px;
}

.message-ai-page {
  --background: var(--ios-pms-dashboard-page-background);
  --padding-top: var(--ios-pms-space-5);
  --padding-bottom: var(--ios-pms-space-6);
  --padding-start: var(--ios-pms-space-4);
  --padding-end: var(--ios-pms-space-4);
}

.message-ai-page-shell {
  width: min(100%, 640px);
  margin: 0 auto;
}

.message-ai-page-card {
  display: grid;
  gap: var(--ios-pms-space-3);
  padding: var(--ios-pms-space-5);
  border: 1px solid var(--ios-pms-border-soft);
  border-radius: var(--ios-pms-radius-card);
  background: rgba(255, 255, 255, 0.76);
  box-shadow: var(--ios-pms-shadow-card-strong);
  backdrop-filter: blur(18px);
}

.message-ai-section {
  display: grid;
  gap: var(--ios-pms-space-3);
  padding: var(--ios-pms-space-5);
  border: 1px solid var(--ios-pms-border-soft);
  border-radius: var(--ios-pms-radius-card);
  background: var(--ios-pms-surface-strong);
  box-shadow: var(--ios-pms-shadow-card);
}

.message-ai-section__title,
.message-ai-section__description {
  margin: 0;
}

.message-ai-section__title {
  min-width: 0;
  color: var(--ios-pms-text-primary);
  font-size: var(--ios-pms-font-title-md-size);
  font-weight: 400;
  line-height: 1.35;
  overflow-wrap: anywhere;
}

.message-ai-section__description {
  margin-top: calc(var(--ios-pms-space-2) * -1);
  color: var(--ios-pms-text-muted);
  font-size: 14px;
  line-height: 1.5;
}

.message-ai-textarea-shell {
  position: relative;
  min-width: 0;
}

.message-ai-input-frame {
  min-width: 0;
  overflow: hidden;
  border: 1px solid #c8c8ce;
  border-radius: 12px;
  background: #ffffff;
  transition: border-color 160ms ease;
}

.message-ai-input-frame:focus-within {
  border-color: #9fa7b3;
}

.message-ai-textarea {
  margin: 0;
  --background: transparent;
  --border-width: 0;
  --color: var(--ios-pms-text-primary);
  --highlight-color-focused: transparent;
  --padding-start: var(--ios-pms-space-3);
  --padding-end: var(--ios-pms-space-3);
  --padding-top: var(--ios-pms-space-3);
  --padding-bottom: var(--ios-pms-space-3);
  --placeholder-color: var(--ios-pms-text-disabled);
  --placeholder-opacity: 1;
  font-size: 15px;
  line-height: 1.5;
}

.message-ai-textarea--draft {
  --color: #666666;
}

.message-ai-textarea--draft :deep(.native-textarea) {
  min-height: 176px;
  color: #666666;
  line-height: 1.5;
}

.message-ai-textarea--instruction :deep(.native-textarea) {
  min-height: 82px;
  line-height: 1.5;
}

.message-ai-textarea--translation {
  --background: #f8fafc;
  --color: #666666;
}

.message-ai-textarea-shell__loading {
  position: absolute;
  inset: 0;
  z-index: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--ios-pms-space-2);
  border-radius: 11px;
  background: rgba(255, 255, 255, 0.84);
  color: var(--ios-pms-text-muted);
  font-size: 13px;
  backdrop-filter: blur(4px);
}

.message-ai-textarea-shell__loading ion-spinner {
  width: 18px;
  height: 18px;
}

.message-ai-section__actions {
  display: flex;
  justify-content: flex-end;
}

.message-ai-action-button {
  --background: #0088ff;
  --background-activated: #0077e6;
  --background-hover: #0088ff;
  --border-radius: 8px;
  --box-shadow: none;
  --color: #ffffff;
  --padding-start: 12px;
  --padding-end: 12px;
  --padding-top: 0;
  --padding-bottom: 0;
  width: auto;
  min-width: 0;
  min-height: 30px;
  height: 30px;
  margin: 0;
  font-size: 15px;
  font-weight: 400;
  letter-spacing: 0;
  text-transform: none;
}

.message-ai-action-button::part(native) {
  min-height: 30px;
  padding: 0 12px;
  border-radius: 8px;
  box-shadow: none;
}

.message-ai-action-button--return {
  --background: #004c8f;
  --background-activated: #003f78;
  --background-hover: #004c8f;
}

.message-ai-fill-button {
  --background: #34c759;
  --background-activated: #2eaf4f;
  --background-hover: #34c759;
  --border-radius: 8px;
  --box-shadow: none;
  --color: #ffffff;
  --padding-top: 0;
  --padding-bottom: 0;
  min-height: 36px;
  height: 36px;
  margin: 2px 0 0;
  font-size: var(--ios-pms-font-title-md-size);
  font-weight: 400;
  letter-spacing: 0;
  text-transform: none;
}

.message-ai-fill-button::part(native) {
  min-height: 36px;
  padding: 0 10px;
  border-radius: 8px;
  box-shadow: none;
}

.message-ai-action-button[disabled],
.message-ai-fill-button[disabled] {
  opacity: 0.52;
}

@media (max-width: 374px) {
  .message-ai-page {
    --padding-top: var(--ios-pms-space-3);
    --padding-start: var(--ios-pms-space-3);
    --padding-end: var(--ios-pms-space-3);
  }

  .message-ai-page-card,
  .message-ai-section {
    padding: var(--ios-pms-space-4);
  }

  .message-ai-section__title {
    font-size: 16px;
  }

  .message-ai-section__description {
    font-size: 13px;
  }
}
</style>
