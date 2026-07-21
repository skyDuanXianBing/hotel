<template>
  <ion-page>
    <ion-header translucent>
      <ion-toolbar class="orders-header__toolbar">
        <ion-buttons slot="start">
          <ion-back-button class="app-page-header__back-btn" :default-href="ROUTE_PATHS.home" />
        </ion-buttons>
        <ion-title class="orders-header__title">{{ $t('routes.Messages') }}</ion-title>
        <ion-buttons slot="end" class="messages-header__actions">
          <ion-button
            class="orders-header__text-btn"
            :class="{ 'is-active': translationEnabled }"
            fill="clear"
            @click="handleOpenTranslationSettings"
          >
            <ion-icon :icon="languageOutline" aria-hidden="true" />
            <span>{{ t('messages.translate') }}</span>
          </ion-button>
          <ion-button class="orders-header__icon-btn" fill="clear" @click="handleToggleSearch">
            <ion-icon :icon="isSearchVisible ? closeOutline : searchOutline" />
          </ion-button>
        </ion-buttons>
      </ion-toolbar>
    </ion-header>

    <ion-content fullscreen class="mobile-page messages-page">
      <ion-refresher slot="fixed" @ionRefresh="handleRefresh">
        <ion-refresher-content :pulling-text="t('messages.pullToRefresh')" refreshing-spinner="crescent" />
      </ion-refresher>

      <div class="orders-page__shell messages-page__shell">
        <section v-if="isSearchVisible" class="orders-search-panel">
          <div class="orders-search-panel__bar">
            <ion-searchbar
              v-model="searchKeyword"
              :debounce="300"
              class="orders-searchbar"
              :placeholder="t('messages.searchPlaceholder')"
            />
            <button type="button" class="orders-search-panel__cancel" @click="handleHideSearch">
              {{ t('messages.cancel') }}
            </button>
          </div>
        </section>

        <section class="orders-tabs-strip">
          <div class="orders-tabs-strip__scroll">
            <button
              v-for="item in MESSAGE_TABS"
              :key="item.value"
              type="button"
              class="orders-tabs-strip__item"
              :class="{ 'is-active': item.value === activeTab }"
              @click="handleSelectTab(item.value)"
            >
              {{ item.label }}
            </button>
          </div>
        </section>

        <section class="orders-filter-row">
          <div class="orders-filter-row__scroll">
            <button
              type="button"
              class="orders-filter-chip"
              :class="{ 'is-active': activeChannel !== ALL_CHANNEL_VALUE }"
              @click="handleSelectChannel(ALL_CHANNEL_VALUE)"
            >
              {{ t('messages.allChannels') }}
            </button>
            <button
              v-for="item in channelOptions"
              :key="item.value"
              type="button"
              class="orders-filter-chip"
              :class="{ 'is-active': item.value === activeChannel }"
              @click="handleSelectChannel(item.value)"
            >
              {{ item.label }}
            </button>
            <button
              v-if="hasActiveFilters"
              type="button"
              class="orders-filter-chip orders-filter-chip--ghost"
              @click="handleResetFilters"
            >
              {{ t('messages.clear') }}
            </button>
          </div>
        </section>

        <section class="orders-list-section">
          <div class="orders-list-header">
            <div class="orders-list-header__heading">
              <p class="orders-list-header__summary">{{ resultsSummaryText }}</p>
              <div v-if="hasActiveFilters" class="orders-list-header__tags">
                <span v-if="activeChannelLabel" class="orders-list-header__tag">{{ activeChannelLabel }}</span>
                <span v-if="trimmedKeyword" class="orders-list-header__tag">{{ trimmedKeyword }}</span>
              </div>
            </div>
            <ion-spinner v-if="loading" name="crescent" class="orders-list-header__spinner" />
          </div>

          <p v-if="loadNotice" class="orders-notice-text orders-notice-text--warning">{{ loadNotice }}</p>
          <p v-else-if="resultsNoticeText" class="orders-notice-text">{{ resultsNoticeText }}</p>

          <div
            v-if="displayedThreads.length > 0"
            ref="threadListRef"
            class="messages-thread-list"
          >
            <button
              v-for="thread in displayedThreads"
              :key="thread.id"
              type="button"
              class="messages-thread-item"
              :class="{ 'is-unread': thread.unreadCount > 0 }"
              :style="resolveMessageThreadAvatarVars(thread)"
              @click="handleOpenThread(thread.id)"
            >
              <div class="messages-thread-item__avatar">
                <span class="messages-thread-item__avatar-text">
                  {{ resolveMessageThreadAvatarLabel(thread) }}
                </span>
                <span v-if="thread.unreadCount > 0" class="messages-thread-item__avatar-badge">
                  {{ formatUnreadCount(thread.unreadCount) }}
                </span>
              </div>

              <div class="messages-thread-item__body">
                <div class="messages-thread-item__row messages-thread-item__row--top">
                  <div class="messages-thread-item__title-line">
                    <h3 class="messages-thread-item__title">
                      {{ resolveMessageThreadTitle(thread) }}
                    </h3>
                    <div class="messages-thread-item__brand-row">
                      <span class="messages-thread-item__utility">
                        <ion-icon :icon="mailOutline" />
                      </span>
                    </div>
                  </div>
                  <span class="messages-thread-item__date">{{ formatThreadDate(thread.lastActivity) }}</span>
                </div>

                <p class="messages-thread-item__preview">
                  {{ getThreadPreviewText(thread) }}
                </p>

                <div class="messages-thread-item__meta">
                  <div class="messages-thread-item__meta-line">
                    <ion-icon :icon="homeOutline" />
                    <span class="messages-thread-item__meta-text">{{ resolveRoomLabel(thread) }}</span>
                  </div>
                  <div class="messages-thread-item__meta-line messages-thread-item__meta-line--stay">
                    <ion-icon :icon="timeOutline" />
                    <span class="messages-thread-item__meta-text">{{ formatStayDateRange(thread) }}</span>
                    <span
                      class="messages-thread-item__status"
                      :class="`is-${resolveStayStatus(thread).tone}`"
                    >
                      {{ resolveStayStatus(thread).label }}
                    </span>
                  </div>
                </div>
              </div>
            </button>
          </div>

          <div v-else-if="!loading" class="orders-empty-state">
            <div class="orders-empty-state__illustration" aria-hidden="true">
              <span class="orders-empty-state__box"></span>
            </div>
            <p class="orders-empty">{{ emptyTitle }}</p>
            <p class="orders-notice-text messages-page__empty-note">{{ emptyDescription }}</p>
          </div>
        </section>
      </div>
    </ion-content>

    <ion-modal
      class="message-translation-modal"
      :is-open="translationSettingsOpen"
      @didDismiss="handleDismissTranslationSettings"
    >
      <ion-header translucent class="message-translation-modal__header">
        <ion-toolbar class="message-translation-modal__toolbar">
          <ion-title>{{ t('messages.translationSettings') }}</ion-title>
          <ion-buttons slot="end">
            <ion-button @click="handleDismissTranslationSettings">{{ t('messages.close') }}</ion-button>
          </ion-buttons>
        </ion-toolbar>
      </ion-header>

      <ion-content class="mobile-page message-translation-modal__page">
        <section class="mobile-card message-translation-sheet">
          <div class="message-translation-sheet__intro">
            <h2 class="mobile-section-title">{{ t('messages.chatGptTranslation') }}</h2>
            <p class="mobile-note">{{ t('messages.translationDescription') }}</p>
          </div>

          <div class="message-translation-setting-row">
            <div>
              <strong>{{ t('messages.autoTranslate') }}</strong>
              <p>{{ t('messages.autoTranslateDescription') }}</p>
            </div>
            <ion-toggle v-model="translationEnabled" :aria-label="t('messages.autoTranslate')" />
          </div>

          <label class="message-translation-field">
            <span>{{ t('messages.targetLanguage') }}</span>
            <ion-select
              v-model="translationTargetLanguage"
              interface="action-sheet"
              :placeholder="t('messages.targetLanguagePlaceholder')"
            >
              <ion-select-option
                v-for="option in MESSAGE_TRANSLATION_LANGUAGE_OPTIONS"
                :key="option.value"
                :value="option.value"
              >
                {{ option.label }}
              </ion-select-option>
            </ion-select>
          </label>

          <div class="message-translation-actions">
            <ion-button
              fill="outline"
              :disabled="isApplyingTranslationSettings"
              @click="handleDismissTranslationSettings"
            >
              {{ t('messages.cancel') }}
            </ion-button>
            <ion-button :disabled="isApplyingTranslationSettings" @click="handleApplyTranslationSettings">
              {{ isApplyingTranslationSettings ? t('messages.saving') : t('messages.save') }}
            </ion-button>
          </div>
        </section>
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
  IonSearchbar,
  IonSelect,
  IonSelectOption,
  IonSpinner,
  IonTitle,
  IonToggle,
  IonToolbar,
  onIonViewWillEnter,
  onIonViewWillLeave,
} from '@ionic/vue'
import {
  closeOutline,
  homeOutline,
  languageOutline,
  mailOutline,
  searchOutline,
  timeOutline,
} from 'ionicons/icons'
import { computed, nextTick, onUnmounted, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute, useRouter } from 'vue-router'
import { getMessageThreads } from '@/api/message'
import { buildMessageDetailPath, ROUTE_PATHS } from '@/router/guards'
import { useNotificationCenterStore } from '@/stores/notificationCenter'
import type { MessageThreadDTO } from '@/types/message'
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
  MESSAGE_TRANSLATION_LANGUAGE_OPTIONS,
  requestAiMessageTranslation,
  saveMessageTranslationSettings,
  type MessageTranslationLanguageValue,
} from '@/utils/messageTranslation'
import {
  resolveMessageThreadAvatarLabel,
  resolveMessageThreadAvatarVars,
  resolveMessageThreadTitle,
} from '@/utils/messageThreadPresentation'
import { getStoredCurrentStoreId, getStoredUser } from '@/utils/storage'
import {
  formatBusinessDateLabel,
  formatStoreDateTime,
  getStoreBusinessDateFromDateTime,
  getStoreTodayDate,
} from '@/utils/storeBusinessDate'

type MessageTabValue = 'all' | 'unread' | 'closed'

interface MessageTabOption {
  label: string
  value: MessageTabValue
}

interface ChannelFilterOption {
  label: string
  value: string
}

interface ThreadStatusView {
  label: string
  tone: 'upcoming' | 'staying' | 'closed' | 'completed' | 'active' | 'reply'
}

const ALL_CHANNEL_VALUE = '__all__'
const VISIBLE_PREVIEW_TRANSLATION_LIMIT = 6
const PREVIEW_TRANSLATION_SETTLE_MS = 250

const route = useRoute()
const router = useRouter()
const notificationCenterStore = useNotificationCenterStore()
const { t } = useI18n()

const MESSAGE_TABS = computed<MessageTabOption[]>(() => [
  { label: t('messages.tabs.all'), value: 'all' },
  { label: t('messages.tabs.unread'), value: 'unread' },
  { label: t('messages.tabs.closed'), value: 'closed' },
])

const loading = ref(false)
const isSearchVisible = ref(false)
const searchKeyword = ref('')
const loadNotice = ref('')
const activeTab = ref<MessageTabValue>('all')
const activeChannel = ref(ALL_CHANNEL_VALUE)
const threads = ref<MessageThreadDTO[]>([])
const routeTargetHandled = ref(false)
const translationSettingsOpen = ref(false)
const isApplyingTranslationSettings = ref(false)
const translationEnabled = ref(false)
const translationTargetLanguage = ref<MessageTranslationLanguageValue>('zh-CN')
const translatedThreadPreviewMap = ref<Record<string, string>>({})
const threadListRef = ref<HTMLElement | null>(null)
const previewTranslationQueue = createAsyncTaskQueue(2)
const visibleThreadIds = new Set<number>()

let previewObserver: IntersectionObserver | null = null
let previewTranslationTimer = 0
let messagesPageActive = false

const trimmedKeyword = computed(() => searchKeyword.value.trim())

const unreadThreadCount = computed(() => {
  let count = 0

  for (const thread of threads.value) {
    if (thread.unreadCount > 0) {
      count += 1
    }
  }

  return count
})

const channelOptions = computed<ChannelFilterOption[]>(() => {
  const nextItems: ChannelFilterOption[] = []
  const seenValues = new Set<string>()

  for (const thread of threads.value) {
    const value = (thread.channelName || '').trim()
    if (!value || seenValues.has(value)) {
      continue
    }

    seenValues.add(value)
    nextItems.push({
      label: value,
      value,
    })
  }

  return nextItems
})

const displayedThreads = computed(() => {
  const keyword = trimmedKeyword.value.toLowerCase()
  const nextItems: MessageThreadDTO[] = []

  for (const thread of threads.value) {
    if (activeTab.value === 'unread' && thread.unreadCount <= 0) {
      continue
    }

    if (activeTab.value === 'closed' && !thread.closed) {
      continue
    }

    if (activeChannel.value !== ALL_CHANNEL_VALUE && thread.channelName !== activeChannel.value) {
      continue
    }

    if (!keyword) {
      nextItems.push(thread)
      continue
    }

    const searchableValues = [
      resolveMessageThreadTitle(thread),
      thread.channelName || '',
      thread.bookingId || '',
      thread.threadId || '',
      thread.roomTypeName || '',
      thread.listingName || '',
      thread.lastMessage || '',
      formatStayDateRange(thread),
    ]

    let matched = false
    for (const value of searchableValues) {
      if (value.toLowerCase().includes(keyword)) {
        matched = true
        break
      }
    }

    if (matched) {
      nextItems.push(thread)
    }
  }

  return nextItems
})

const activeTabLabel = computed(() => {
  return MESSAGE_TABS.value.find((item) => item.value === activeTab.value)?.label || t('messages.tabs.all')
})

const activeChannelLabel = computed(() => {
  if (activeChannel.value === ALL_CHANNEL_VALUE) {
    return ''
  }

  return channelOptions.value.find((item) => item.value === activeChannel.value)?.label || activeChannel.value
})

const hasActiveFilters = computed(() => {
  return activeChannel.value !== ALL_CHANNEL_VALUE || Boolean(trimmedKeyword.value)
})

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
    suReservationId: getRouteQueryText(route.query.suReservationId),
    reservationNotifId: getRouteQueryText(route.query.reservationNotifId),
    guestName: getRouteQueryText(route.query.guestName),
  }
})

const hasRouteTarget = computed(() => {
  const target = routeTarget.value
  return Boolean(
    target.reservationId ||
      target.orderNumber ||
      target.channelOrderNumber ||
      target.suReservationId ||
      target.reservationNotifId ||
      target.guestName,
  )
})

const routeTargetKey = computed(() => {
  const target = routeTarget.value
  return [
    target.reservationId ? String(target.reservationId) : '',
    target.orderNumber,
    target.channelOrderNumber,
    target.suReservationId,
    target.reservationNotifId,
    target.guestName,
  ].join('|')
})

const resultsSummaryText = computed(() => {
  const unreadLabel =
    unreadThreadCount.value > 0
      ? t('messages.summaryUnread', { count: unreadThreadCount.value })
      : ''
  return `${t('messages.summary', {
    tab: activeTabLabel.value,
    count: displayedThreads.value.length,
  })}${unreadLabel}`
})

const resultsNoticeText = computed(() => {
  if (activeTab.value === 'unread') {
    return t('messages.noticeUnread')
  }

  if (activeTab.value === 'closed') {
    return t('messages.noticeClosed')
  }

  return ''
})

const emptyTitle = computed(() => {
  if (trimmedKeyword.value) {
    return t('messages.emptySearch')
  }

  if (activeTab.value === 'unread') {
    return t('messages.emptyUnread')
  }

  if (activeTab.value === 'closed') {
    return t('messages.emptyClosed')
  }

  return t('messages.emptyDefault')
})

const emptyDescription = computed(() => {
  if (trimmedKeyword.value) {
    return t('messages.emptySearchDescription')
  }

  if (activeChannel.value !== ALL_CHANNEL_VALUE) {
    return t('messages.emptyChannelDescription')
  }

  return t('messages.emptyDefaultDescription')
})

function resolveWarningMessage(error: unknown, fallbackMessage: string) {
  if (error instanceof Error && error.message) {
    return error.message
  }

  return fallbackMessage
}

function syncTranslationSettingsFromStorage() {
  const settings = loadMessageTranslationSettings()
  const shouldClearCaches =
    translationEnabled.value !== settings.enabled ||
    translationTargetLanguage.value !== settings.targetLanguage

  translationEnabled.value = settings.enabled
  translationTargetLanguage.value = settings.targetLanguage

  if (shouldClearCaches) {
    resetThreadTranslationState()
  }
}

function clearPreviewTranslationTimer() {
  if (previewTranslationTimer) {
    window.clearTimeout(previewTranslationTimer)
    previewTranslationTimer = 0
  }
}

function resetThreadTranslationState() {
  clearPreviewTranslationTimer()
  previewTranslationQueue.cancelAll()
  translatedThreadPreviewMap.value = {}
}

function getThreadPreviewKey(thread: MessageThreadDTO) {
  return `thread:${thread.id}:${translationTargetLanguage.value}:${thread.lastMessage || ''}`
}

function getTranslatedThreadPreviewText(thread: MessageThreadDTO) {
  return translatedThreadPreviewMap.value[getThreadPreviewKey(thread)] || ''
}

function getThreadPreviewText(thread: MessageThreadDTO) {
  const originalText = thread.lastMessage || t('messages.noLatestMessage')
  if (!translationEnabled.value) {
    return originalText
  }

  const translatedText = getTranslatedThreadPreviewText(thread)
  if (translatedText && translatedText !== thread.lastMessage?.trim()) {
    return translatedText
  }

  return originalText
}

function getTranslationCacheScope(): MessageTranslationCacheScope {
  return {
    storeId: getStoredCurrentStoreId(),
    userId: getStoredUser()?.id,
    targetLanguage: translationTargetLanguage.value,
  }
}

function setThreadPreviewTranslation(thread: MessageThreadDTO, translatedText: string) {
  const normalizedTranslation = translatedText.trim()
  if (!normalizedTranslation) {
    return
  }

  const key = getThreadPreviewKey(thread)
  translatedThreadPreviewMap.value[key] = normalizedTranslation
  setCachedMessageTranslation(
    getTranslationCacheScope(),
    thread.lastMessage || '',
    normalizedTranslation,
  )
}

function ensureThreadPreviewTranslation(thread: MessageThreadDTO) {
  const sourceText = thread.lastMessage?.trim() || ''
  if (
    !messagesPageActive ||
    translationSettingsOpen.value ||
    !translationEnabled.value ||
    !sourceText
  ) {
    return
  }

  const key = getThreadPreviewKey(thread)
  if (translatedThreadPreviewMap.value[key] || previewTranslationQueue.has(key)) {
    return
  }

  const cachedTranslation = getCachedMessageTranslation(getTranslationCacheScope(), sourceText)
  if (cachedTranslation) {
    translatedThreadPreviewMap.value[key] = cachedTranslation
    return
  }

  previewTranslationQueue.enqueue({
    key,
    priority: thread.unreadCount,
    run: (signal) =>
      requestAiMessageTranslation(sourceText, translationTargetLanguage.value, {
        signal,
        suppressErrorToast: true,
      }),
    onSuccess: (translatedText) => {
      if (messagesPageActive && translationEnabled.value) {
        setThreadPreviewTranslation(thread, translatedText)
      }
    },
    onError: (error) => {
      console.error('翻译会话预览失败:', error)
    },
  })
}

function translateVisibleThreadPreviews() {
  if (
    !messagesPageActive ||
    translationSettingsOpen.value ||
    !translationEnabled.value
  ) {
    return
  }

  const targetThreads = displayedThreads.value
    .filter((thread) => thread.unreadCount > 0 && visibleThreadIds.has(thread.id))
    .slice(0, VISIBLE_PREVIEW_TRANSLATION_LIMIT)

  for (const thread of targetThreads) {
    ensureThreadPreviewTranslation(thread)
  }
}

function scheduleVisibleThreadTranslations() {
  clearPreviewTranslationTimer()
  if (
    !messagesPageActive ||
    translationSettingsOpen.value ||
    !translationEnabled.value
  ) {
    return
  }

  previewTranslationTimer = window.setTimeout(() => {
    previewTranslationTimer = 0
    translateVisibleThreadPreviews()
  }, PREVIEW_TRANSLATION_SETTLE_MS)
}

function refreshThreadPreviewObserver() {
  previewObserver?.disconnect()
  visibleThreadIds.clear()

  if (!messagesPageActive || !threadListRef.value) {
    return
  }

  const threadElements = threadListRef.value.querySelectorAll<HTMLElement>(
    '.messages-thread-item',
  )
  if (typeof IntersectionObserver === 'undefined') {
    for (const thread of displayedThreads.value.slice(0, VISIBLE_PREVIEW_TRANSLATION_LIMIT)) {
      visibleThreadIds.add(thread.id)
    }
    scheduleVisibleThreadTranslations()
    return
  }

  if (!previewObserver) {
    previewObserver = new IntersectionObserver(
      (entries) => {
        for (const entry of entries) {
          const threadId = Number((entry.target as HTMLElement).dataset.threadId)
          if (!Number.isInteger(threadId)) {
            continue
          }
          if (entry.isIntersecting) {
            visibleThreadIds.add(threadId)
          } else {
            visibleThreadIds.delete(threadId)
          }
        }
        scheduleVisibleThreadTranslations()
      },
      {
        threshold: 0.45,
      },
    )
  }

  threadElements.forEach((element, index) => {
    const thread = displayedThreads.value[index]
    if (!thread) {
      return
    }
    element.dataset.threadId = String(thread.id)
    previewObserver?.observe(element)
  })
}

function stopThreadPreviewTranslations() {
  clearPreviewTranslationTimer()
  previewTranslationQueue.cancelAll()
  previewObserver?.disconnect()
  visibleThreadIds.clear()
}

async function restartVisibleThreadTranslations() {
  stopThreadPreviewTranslations()
  if (!messagesPageActive || !translationEnabled.value) {
    return
  }
  await nextTick()
  refreshThreadPreviewObserver()
  scheduleVisibleThreadTranslations()
}

function handleOpenTranslationSettings() {
  syncTranslationSettingsFromStorage()
  stopThreadPreviewTranslations()
  translationSettingsOpen.value = true
}

function handleDismissTranslationSettings() {
  const shouldRestoreSavedSettings = translationSettingsOpen.value
  translationSettingsOpen.value = false
  if (!shouldRestoreSavedSettings) {
    return
  }

  syncTranslationSettingsFromStorage()
  if (translationEnabled.value) {
    void restartVisibleThreadTranslations()
  }
}

async function handleApplyTranslationSettings() {
  isApplyingTranslationSettings.value = true
  try {
    saveMessageTranslationSettings({
      enabled: translationEnabled.value,
      targetLanguage: translationTargetLanguage.value,
    })
    translationSettingsOpen.value = false
    resetThreadTranslationState()

    if (translationEnabled.value) {
      await restartVisibleThreadTranslations()
    }

    showSuccessToast(t('messages.settingsUpdated'))
  } catch (error) {
    console.error('保存全局翻译设置失败:', error)
    showWarningToast(t('messages.settingsSaveFailed'))
  } finally {
    isApplyingTranslationSettings.value = false
  }
}

function getRouteQueryText(value: unknown) {
  if (Array.isArray(value)) {
    return String(value[0] || '').trim()
  }

  if (typeof value === 'string') {
    return value.trim()
  }

  return ''
}

function toComparableText(value?: string | null) {
  return String(value || '').trim().toLowerCase()
}

function matchesThreadKeyword(thread: MessageThreadDTO, keyword: string) {
  const normalizedKeyword = toComparableText(keyword)
  if (!normalizedKeyword) {
    return false
  }

  const values = [thread.bookingId, thread.threadId]
  for (const value of values) {
    const normalizedValue = toComparableText(value)
    if (!normalizedValue) {
      continue
    }

    if (
      normalizedValue === normalizedKeyword ||
      normalizedValue.includes(normalizedKeyword) ||
      normalizedKeyword.includes(normalizedValue)
    ) {
      return true
    }
  }

  return false
}

function resolveThreadByRouteTarget() {
  if (threads.value.length === 0) {
    return null
  }

  const target = routeTarget.value
  const bookingCandidates = [
    target.channelOrderNumber,
    target.suReservationId,
    target.reservationNotifId,
    target.orderNumber,
  ]
    .map((item) => item.trim())
    .filter(Boolean)

  for (const thread of threads.value) {
    for (const candidate of bookingCandidates) {
      if (matchesThreadKeyword(thread, candidate)) {
        return thread
      }
    }
  }

  const guestNameKeyword = toComparableText(target.guestName)
  if (!guestNameKeyword) {
    return null
  }

  for (const thread of threads.value) {
    const threadGuestName = toComparableText(thread.guestName)
    if (threadGuestName === guestNameKeyword || threadGuestName.includes(guestNameKeyword)) {
      return thread
    }
  }

  return null
}

function getMessageDetailDefaultHref() {
  const routeDefaultHref = route.query.defaultHref
  if (typeof routeDefaultHref === 'string' && routeDefaultHref) {
    return routeDefaultHref
  }

  return ROUTE_PATHS.messages
}

async function applyRouteMessageTarget() {
  if (!hasRouteTarget.value || routeTargetHandled.value || threads.value.length === 0) {
    return
  }

  const target = routeTarget.value
  const preferredKeyword =
    target.channelOrderNumber ||
    target.suReservationId ||
    target.reservationNotifId ||
    target.orderNumber ||
    target.guestName
  if (preferredKeyword && !searchKeyword.value.trim()) {
    searchKeyword.value = preferredKeyword
  }

  const matchedThread = resolveThreadByRouteTarget()
  routeTargetHandled.value = true

  if (!matchedThread) {
    if (!loadNotice.value) {
      loadNotice.value = t('messages.routeTargetNotFound')
    }
    return
  }

  await router.push({
    path: buildMessageDetailPath(matchedThread.id),
    query: {
      defaultHref: getMessageDetailDefaultHref(),
    },
  })
}

function ensureActiveChannelStillExists(nextThreads: MessageThreadDTO[]) {
  if (activeChannel.value === ALL_CHANNEL_VALUE) {
    return
  }

  for (const thread of nextThreads) {
    if (thread.channelName === activeChannel.value) {
      return
    }
  }

  activeChannel.value = ALL_CHANNEL_VALUE
}

function formatMonthDay(value?: string) {
  return formatBusinessDateLabel(value, 'month-day', '--')
}

function formatThreadDate(value: string) {
  const messageDate = getStoreBusinessDateFromDateTime(value)
  if (!messageDate) {
    return '--'
  }

  if (messageDate === getStoreTodayDate()) {
    return formatStoreDateTime(value, 'time', '--')
  }

  return formatMonthDay(value)
}

function resolveRoomLabel(thread: MessageThreadDTO) {
  if (thread.roomTypeName) {
    return thread.roomTypeName
  }

  if (thread.listingName) {
    return thread.listingName
  }

  return t('messages.noRoomType')
}

function formatStayDateRange(thread: MessageThreadDTO) {
  const checkInLabel = formatMonthDay(thread.checkInDate)
  const checkOutLabel = formatMonthDay(thread.checkOutDate)

  if (checkInLabel === '--' && checkOutLabel === '--') {
    return t('messages.noStayDate')
  }

  if (checkInLabel === '--') {
    return t('messages.checkout', { date: checkOutLabel })
  }

  if (checkOutLabel === '--') {
    return t('messages.checkin', { date: checkInLabel })
  }

  return `${checkInLabel} ~ ${checkOutLabel}`
}

function getTodayDateKey() {
  return getStoreTodayDate()
}

function resolveStayStatus(thread: MessageThreadDTO): ThreadStatusView {
  if (thread.closed) {
    return {
      label: t('messages.status.closed'),
      tone: 'closed',
    }
  }

  const today = getTodayDateKey()
  const checkInDate = thread.checkInDate || ''
  const checkOutDate = thread.checkOutDate || ''

  if (checkInDate && today < checkInDate) {
    return {
      label: t('messages.status.upcoming'),
      tone: 'upcoming',
    }
  }

  if (checkInDate && checkOutDate && today >= checkInDate && today < checkOutDate) {
    return {
      label: t('messages.status.staying'),
      tone: 'staying',
    }
  }

  if (checkOutDate && today >= checkOutDate) {
    return {
      label: t('messages.status.departed'),
      tone: 'completed',
    }
  }

  if (thread.unreadCount > 0) {
    return {
      label: t('messages.status.reply'),
      tone: 'reply',
    }
  }

  return {
    label: t('messages.status.active'),
    tone: 'active',
  }
}

function formatUnreadCount(value: number) {
  if (value > 99) {
    return '99+'
  }

  return String(value)
}

async function loadThreads() {
  loading.value = true
  loadNotice.value = ''

  try {
    const response = await getMessageThreads()
    if (!response.success || !response.data) {
      throw new Error(response.message || t('messages.loadFailed'))
    }

    ensureActiveChannelStillExists(response.data)
    threads.value = response.data
    notificationCenterStore.syncMessageThreads(threads.value)
    await applyRouteMessageTarget()
  } catch (error) {
    loadNotice.value = resolveWarningMessage(error, t('messages.loadFailed'))
    if (!isHandledRequestError(error)) {
      showWarningToast(loadNotice.value)
    }
  } finally {
    loading.value = false
  }
}

function handleSelectTab(value: MessageTabValue) {
  activeTab.value = value
}

function handleSelectChannel(value: string) {
  activeChannel.value = value
}

function handleResetFilters() {
  activeChannel.value = ALL_CHANNEL_VALUE
  searchKeyword.value = ''
}

function handleToggleSearch() {
  isSearchVisible.value = !isSearchVisible.value
}

function handleHideSearch() {
  isSearchVisible.value = false
}

async function handleOpenThread(threadId: number) {
  await router.push(buildMessageDetailPath(threadId))
}

async function handleRefresh(event: CustomEvent) {
  try {
    await loadThreads()
  } finally {
    event.detail.complete()
  }
}

watch(
  () => routeTargetKey.value,
  () => {
    routeTargetHandled.value = false
    void applyRouteMessageTarget()
  },
)

watch(
  () => displayedThreads.value.map((thread) => getThreadPreviewKey(thread)).join('|'),
  async () => {
    if (messagesPageActive) {
      await nextTick()
      refreshThreadPreviewObserver()
    }
  },
)

onIonViewWillEnter(async () => {
  messagesPageActive = true
  syncTranslationSettingsFromStorage()
  await loadThreads()
  await nextTick()
  refreshThreadPreviewObserver()
})

onIonViewWillLeave(() => {
  messagesPageActive = false
  stopThreadPreviewTranslations()
})

onUnmounted(() => {
  messagesPageActive = false
  stopThreadPreviewTranslations()
  previewObserver = null
})
</script>

<style scoped>
.messages-page {
  display: block;
  --padding-top: 8px;
  --padding-start: 0;
  --padding-end: 0;
  --background: linear-gradient(180deg, #ffffff 0%, #fcfcfe 100%);
}

.orders-header__toolbar {
  --background: rgba(255, 255, 255, 0.94);
  --border-width: 0;
  --padding-start: 10px;
  --padding-end: 10px;
}

.orders-header__title {
  color: var(--ios-pms-header-title-color);
  font-size: var(--ios-pms-font-title-xl-size);
  font-weight: 500;
  letter-spacing: -0.04em;
}

.messages-header__actions {
  gap: 2px;
}

.orders-header__icon-btn {
  --padding-start: 0;
  --padding-end: 0;
  --padding-top: 0;
  --padding-bottom: 0;
  --color: #141821;
  width: 34px;
  height: 34px;
  margin: 0;
}

.orders-header__icon-btn ion-icon {
  font-size: 19px;
}

.orders-header__text-btn {
  --padding-start: 5px;
  --padding-end: 5px;
  --background-activated: rgba(47, 156, 255, 0.08);
  --border-radius: 8px;
  --color: var(--ios-pms-header-control-color);
  min-width: 0;
  height: 34px;
  margin: 0;
  font-size: 13px;
  font-weight: 500;
  letter-spacing: 0;
}

.orders-header__text-btn ion-icon {
  margin-right: 3px;
  font-size: 15px;
}

.orders-header__text-btn.is-active {
  --color: #2f8fff;
}

ion-modal.message-translation-modal {
  --border-radius: 30px 30px 0 0;
  --backdrop-opacity: 0.24;
  --box-shadow: 0 -28px 56px rgba(15, 23, 42, 0.2);
}

.message-translation-modal__header {
  backdrop-filter: blur(16px);
}

.message-translation-modal__toolbar {
  --background: rgba(248, 249, 251, 0.94);
  --border-width: 0;
  --padding-start: 10px;
  --padding-end: 10px;
}

.message-translation-modal__toolbar ion-title {
  padding: 4px 0 2px;
  color: #1b2330;
  font-size: 17px;
  font-weight: 700;
  letter-spacing: -0.03em;
}

.message-translation-modal__toolbar ion-button {
  --color: #536074;
  --padding-start: 10px;
  --padding-end: 10px;
  font-size: 14px;
  font-weight: 600;
}

.message-translation-modal__page {
  --background: linear-gradient(180deg, #f7f8fa 0%, #f1f3f6 100%);
  --padding-top: 18px;
  --padding-bottom: calc(28px + var(--app-safe-bottom));
  --padding-start: 16px;
  --padding-end: 16px;
}

.message-translation-sheet {
  display: grid;
  gap: 16px;
  padding: 18px;
  border: 1px solid rgba(218, 223, 232, 0.92);
  border-radius: 28px;
  background: rgba(255, 255, 255, 0.86);
  backdrop-filter: blur(22px);
  box-shadow: 0 18px 40px rgba(15, 23, 42, 0.1);
}

.message-translation-sheet__intro {
  display: grid;
  gap: 6px;
}

.message-translation-sheet__intro h2,
.message-translation-sheet__intro p {
  margin: 0;
}

.message-translation-setting-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 14px 16px;
  border: 1px solid rgba(224, 229, 237, 0.96);
  border-radius: 22px;
  background: linear-gradient(180deg, rgba(249, 250, 252, 0.98), rgba(242, 245, 248, 0.92));
}

.message-translation-setting-row strong {
  display: block;
  color: #1b2330;
  font-size: 15px;
  line-height: 1.3;
}

.message-translation-setting-row p {
  margin: 4px 0 0;
  color: #8791a2;
  font-size: 12px;
  line-height: 1.45;
}

.message-translation-setting-row ion-toggle {
  flex-shrink: 0;
}

.message-translation-field {
  display: grid;
  gap: 10px;
  padding: 12px 14px 14px;
  border: 1px solid rgba(224, 229, 237, 0.96);
  border-radius: 22px;
  background: linear-gradient(180deg, rgba(249, 250, 252, 0.98), rgba(242, 245, 248, 0.92));
}

.message-translation-field span {
  color: #556173;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.02em;
}

.message-translation-actions {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.message-translation-actions ion-button {
  margin: 0;
  min-height: 44px;
  font-size: 14px;
  font-weight: 700;
}

.orders-page__shell {
  display: flex;
  flex-direction: column;
  gap: 10px;
  min-height: 100%;
  padding: 0 0 32px;
  background: linear-gradient(180deg, #ffffff 0%, #fcfcfe 100%);
}

.messages-page__shell {
  padding-bottom: 32px;
}

.orders-search-panel {
  padding: 0 12px;
}

.orders-search-panel__bar {
  display: flex;
  align-items: center;
  gap: 12px;
}

.orders-searchbar {
  flex: 1;
  margin: 0;
  padding: 0;
  --background: #fafafa;
  --border-radius: 0;
  --box-shadow: none;
  --color: #666666;
  --icon-color: #666666;
  --placeholder-color: #666666;
  --placeholder-opacity: 0.72;
  --clear-button-color: #666666;
  --padding-start: 0;
  --padding-end: 0;
  --padding-top: 0;
  --padding-bottom: 0;
  --cancel-button-color: #8c909b;
  border: 0;
  border-radius: 0;
  background: transparent;
  box-shadow: none;
}

.orders-searchbar :deep(.searchbar-input-container) {
  border: 1px solid #eceff5;
  border-radius: 22px;
  background: #fafafa;
  box-shadow: none;
  overflow: hidden;
}

.orders-search-panel__cancel {
  border: 0;
  background: transparent;
  color: #8c909b;
  font: inherit;
  font-size: 16px;
  font-weight: 500;
}

.orders-tabs-strip,
.orders-filter-row {
  position: relative;
  --orders-strip-edge: 12px;
  --orders-strip-fade-width: 12px;
}

.orders-tabs-strip::after,
.orders-filter-row::after {
  content: '';
  position: absolute;
  top: 0;
  right: 0;
  bottom: 0;
  width: var(--orders-strip-fade-width);
  background: linear-gradient(90deg, rgba(255, 255, 255, 0), #ffffff 78%);
  pointer-events: none;
}

.orders-tabs-strip__scroll,
.orders-filter-row__scroll {
  display: flex;
  gap: 8px;
  overflow-x: auto;
  padding-left: var(--orders-strip-edge);
  padding-right: calc(var(--orders-strip-edge) + var(--orders-strip-fade-width));
  scroll-padding-left: var(--orders-strip-edge);
  scroll-padding-right: calc(var(--orders-strip-edge) + var(--orders-strip-fade-width));
  scrollbar-width: none;
  -ms-overflow-style: none;
}

.orders-tabs-strip__scroll::-webkit-scrollbar,
.orders-filter-row__scroll::-webkit-scrollbar {
  display: none;
}

.orders-tabs-strip__item {
  position: relative;
  flex: 0 0 auto;
  padding: 0 2px 14px;
  border: 0;
  background: transparent;
  color: #8f94a2;
  font: inherit;
  font-size: 17px;
  font-weight: 500;
  white-space: nowrap;
}

.orders-tabs-strip__item.is-active {
  color: #4a98ff;
}

.orders-tabs-strip__item.is-active::after {
  content: '';
  position: absolute;
  right: 0;
  bottom: 0;
  left: 0;
  height: 4px;
  border-radius: 999px;
  background: #4a98ff;
}

.orders-filter-chip {
  flex: 0 0 auto;
  min-height: 36px;
  padding: 0 14px;
  border: 1px solid #e9ebf1;
  border-radius: 13px;
  background: #ffffff;
  color: #5a6170;
  font: inherit;
  font-size: 15px;
  font-weight: 500;
  box-shadow: 0 2px 8px rgba(15, 23, 42, 0.03);
  white-space: nowrap;
}

.orders-filter-chip.is-active {
  border-color: #56d0b0;
  box-shadow: inset 0 0 0 1px rgba(86, 208, 176, 0.2);
  color: #3bb596;
}

.orders-filter-chip--ghost {
  border-color: #dce5ff;
  color: #4a98ff;
}

.orders-list-section {
  flex: 1;
  padding: 2px 0 40px;
}

.orders-list-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 10px;
  padding: 0 14px 12px;
}

.orders-list-header__heading {
  min-width: 0;
}

.orders-list-header__summary {
  margin: 0;
  color: var(--ios-pms-text-muted);
  font-size: 13px;
  font-weight: var(--ios-pms-weight-medium);
  letter-spacing: -0.01em;
  line-height: 1.45;
}

.orders-list-header__tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 6px;
}

.orders-list-header__tag {
  display: inline-flex;
  align-items: center;
  min-height: 24px;
  padding: 0 10px;
  border: 1px solid rgba(var(--ion-color-primary-rgb), 0.08);
  border-radius: 999px;
  background: var(--ios-pms-primary-soft);
  color: var(--ios-pms-primary-strong);
  font-size: 11px;
  font-weight: var(--ios-pms-weight-bold);
}

.orders-list-header__spinner {
  flex-shrink: 0;
  color: #4a98ff;
}

.orders-notice-text {
  margin: 0;
  padding: 0 14px 12px;
  color: var(--ios-pms-text-soft);
  font-size: 12px;
  line-height: 1.55;
}

.orders-notice-text--warning {
  color: #d99a3e;
}

.orders-empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 58vh;
  padding: 28px 0 0;
}

.orders-empty-state__illustration {
  position: relative;
  width: 132px;
  height: 116px;
  margin-bottom: 18px;
}

.orders-empty-state__illustration::before {
  content: '';
  position: absolute;
  right: 16px;
  bottom: 10px;
  left: 16px;
  height: 18px;
  border-radius: 50%;
  background: rgba(17, 24, 39, 0.05);
  filter: blur(1px);
}

.orders-empty-state__box {
  position: absolute;
  inset: 0;
  border: 2px solid #eef1f6;
  border-radius: 18px;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.95), rgba(248, 249, 252, 0.72));
}

.orders-empty-state__box::before,
.orders-empty-state__box::after {
  content: '';
  position: absolute;
  top: -28px;
  width: 54px;
  height: 44px;
  border: 2px solid #eef1f6;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(249, 250, 252, 0.86));
}

.orders-empty-state__box::before {
  left: 14px;
  border-right: 0;
  border-radius: 16px 0 0 16px;
  transform: skewY(-14deg);
}

.orders-empty-state__box::after {
  right: 14px;
  border-left: 0;
  border-radius: 0 16px 16px 0;
  transform: skewY(14deg);
}

.orders-empty {
  margin: 0;
  color: var(--ios-pms-text-primary);
  font-size: 18px;
  font-weight: var(--ios-pms-weight-heavy);
  line-height: 1.2;
}

.messages-page__empty-note {
  max-width: 260px;
  padding-top: 8px;
  text-align: center;
}

.messages-thread-list {
  display: grid;
}

.messages-thread-item {
  display: grid;
  grid-template-columns: 52px minmax(0, 1fr);
  gap: 12px;
  width: 100%;
  padding: 18px 14px;
  border: 0;
  border-bottom: 1px solid #eef1f5;
  background: transparent;
  color: inherit;
  text-align: left;
}

.messages-thread-item:last-child {
  border-bottom: 0;
}

.messages-thread-item__avatar {
  position: relative;
  display: grid;
  place-items: center;
  width: 46px;
  height: 46px;
  border-radius: 50%;
  background: linear-gradient(180deg, var(--thread-avatar-start), var(--thread-avatar-end));
  box-shadow: 0 8px 18px rgba(15, 23, 42, 0.08);
  color: #ffffff;
}

.messages-thread-item__avatar::after {
  content: '';
  position: absolute;
  inset: 5px;
  border: 1px solid rgba(255, 255, 255, 0.82);
  border-radius: 50%;
}

.messages-thread-item__avatar-text {
  position: relative;
  z-index: 1;
  font-size: 18px;
  font-weight: 700;
  letter-spacing: -0.03em;
}

.messages-thread-item__avatar-badge {
  position: absolute;
  top: -3px;
  right: -5px;
  z-index: 2;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 20px;
  height: 20px;
  padding: 0 5px;
  border: 0;
  border-radius: 999px;
  background: #ff3b30;
  color: #ffffff;
  font-size: 10px;
  font-weight: 700;
  line-height: 1;
}

.messages-thread-item__body {
  min-width: 0;
  display: grid;
  gap: 6px;
}

.messages-thread-item__row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.messages-thread-item__title-line {
  min-width: 0;
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 6px;
}

.messages-thread-item__title {
  margin: 0;
  color: var(--ios-pms-text-primary);
  font-size: 17px;
  font-weight: var(--ios-pms-weight-heavy);
  line-height: 1.22;
  letter-spacing: -0.03em;
}

.messages-thread-item__brand-row {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.messages-thread-item__brand,
.messages-thread-item__utility {
  display: grid;
  place-items: center;
  width: 24px;
  height: 24px;
  border-radius: 999px;
}

.messages-thread-item__brand {
  border: 1px solid var(--thread-border);
  background: var(--thread-soft);
  color: var(--thread-accent);
}

.messages-thread-item__brand span {
  font-size: 13px;
  font-weight: 700;
  line-height: 1;
}

.messages-thread-item__brand ion-icon,
.messages-thread-item__utility ion-icon {
  font-size: 13px;
}

.messages-thread-item__utility {
  border: 1px solid #eceff4;
  background: #ffffff;
  color: #99a1af;
}

.messages-thread-item__date {
  flex-shrink: 0;
  padding-top: 3px;
  color: #b2b8c3;
  font-size: 12px;
  font-weight: 500;
  white-space: nowrap;
}

.messages-thread-item__preview {
  margin: 0;
  color: #a4acb8;
  font-size: 14px;
  line-height: 1.45;
  display: -webkit-box;
  overflow: hidden;
  -webkit-line-clamp: 1;
  -webkit-box-orient: vertical;
}

.messages-thread-item__meta {
  display: grid;
  gap: 6px;
}

.messages-thread-item__meta-line {
  display: flex;
  align-items: center;
  gap: 7px;
  min-width: 0;
  color: #6f7786;
  font-size: 13px;
  line-height: 1.45;
}

.messages-thread-item__meta-line ion-icon {
  flex-shrink: 0;
  color: #9aa2b1;
  font-size: 14px;
}

.messages-thread-item__meta-line--stay {
  flex-wrap: wrap;
}

.messages-thread-item__meta-text {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.messages-thread-item__status {
  display: inline-flex;
  align-items: center;
  min-height: 23px;
  padding: 0 9px;
  margin-left: auto;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 600;
  white-space: nowrap;
}

.messages-thread-item__status.is-upcoming {
  background: rgba(86, 208, 176, 0.14);
  color: #3bb596;
}

.messages-thread-item__status.is-staying {
  background: rgba(74, 152, 255, 0.14);
  color: #4a98ff;
}

.messages-thread-item__status.is-reply {
  background: rgba(255, 184, 78, 0.14);
  color: #d9962e;
}

.messages-thread-item__status.is-active {
  background: rgba(148, 163, 184, 0.14);
  color: #677184;
}

.messages-thread-item__status.is-completed,
.messages-thread-item__status.is-closed {
  background: rgba(148, 163, 184, 0.14);
  color: #8b94a4;
}

.messages-thread-item.is-unread .messages-thread-item__title {
  color: #1d2430;
}

.messages-thread-item.is-unread .messages-thread-item__preview {
  color: #7e8696;
}
</style>
