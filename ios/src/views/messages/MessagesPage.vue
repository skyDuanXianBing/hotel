<template>
  <ion-page>
    <ion-header translucent>
      <ion-toolbar class="orders-header__toolbar">
        <ion-title class="orders-header__title">消息</ion-title>
        <ion-buttons slot="end">
          <ion-button class="orders-header__icon-btn" fill="clear" @click="handleToggleSearch">
            <ion-icon :icon="isSearchVisible ? closeOutline : searchOutline" />
          </ion-button>
        </ion-buttons>
      </ion-toolbar>
    </ion-header>

    <ion-content fullscreen class="mobile-page messages-page">
      <ion-refresher slot="fixed" @ionRefresh="handleRefresh">
        <ion-refresher-content pulling-text="下拉刷新消息" refreshing-spinner="crescent" />
      </ion-refresher>

      <div class="orders-page__shell messages-page__shell">
        <section v-if="isSearchVisible" class="orders-search-panel">
          <div class="orders-search-panel__bar">
            <ion-searchbar
              v-model="searchKeyword"
              :debounce="0"
              class="orders-searchbar"
              placeholder="搜索住客、房型、渠道、订单号或消息内容"
            />
            <button type="button" class="orders-search-panel__cancel" @click="handleHideSearch">取消</button>
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
              全部渠道
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
              清空
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

          <div v-if="displayedThreads.length > 0" class="messages-thread-list">
            <button
              v-for="thread in displayedThreads"
              :key="thread.id"
              type="button"
              class="messages-thread-item"
              :class="{ 'is-unread': thread.unreadCount > 0 }"
              :style="resolveThreadVars(thread)"
              @click="handleOpenThread(thread.id)"
            >
              <div class="messages-thread-item__avatar">
                <span class="messages-thread-item__avatar-text">{{ resolveAvatarLabel(thread) }}</span>
                <span v-if="thread.unreadCount > 0" class="messages-thread-item__avatar-badge">
                  {{ formatUnreadCount(thread.unreadCount) }}
                </span>
              </div>

              <div class="messages-thread-item__body">
                <div class="messages-thread-item__row messages-thread-item__row--top">
                  <div class="messages-thread-item__title-line">
                    <h3 class="messages-thread-item__title">{{ formatThreadTitle(thread) }}</h3>
                    <div class="messages-thread-item__brand-row">
                      <span class="messages-thread-item__utility">
                        <ion-icon :icon="mailOutline" />
                      </span>
                    </div>
                  </div>
                  <span class="messages-thread-item__date">{{ formatThreadDate(thread.lastActivity) }}</span>
                </div>

                <p class="messages-thread-item__preview">{{ thread.lastMessage || '暂无最新消息' }}</p>

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
  </ion-page>
</template>

<script setup lang="ts">
import {
  IonButton,
  IonButtons,
  IonContent,
  IonHeader,
  IonIcon,
  IonPage,
  IonRefresher,
  IonRefresherContent,
  IonSearchbar,
  IonSpinner,
  IonTitle,
  IonToolbar,
  onIonViewWillEnter,
} from '@ionic/vue'
import { closeOutline, homeOutline, mailOutline, searchOutline, timeOutline } from 'ionicons/icons'
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getMessageThreads } from '@/api/message'
import { buildMessageDetailPath } from '@/router/guards'
import { useNotificationCenterStore } from '@/stores/notificationCenter'
import type { MessageThreadDTO } from '@/types/message'
import { isHandledRequestError } from '@/utils/request'
import { showWarningToast } from '@/utils/notify'

type MessageTabValue = 'all' | 'unread' | 'closed'

interface MessageTabOption {
  label: string
  value: MessageTabValue
}

interface ChannelFilterOption {
  label: string
  value: string
}

interface ChannelBrand {
  accent: string
  soft: string
  border: string
  avatarStart: string
  avatarEnd: string
  label: string
  icon?: string
}

interface ThreadStatusView {
  label: string
  tone: 'upcoming' | 'staying' | 'closed' | 'completed' | 'active' | 'reply'
}

const ALL_CHANNEL_VALUE = '__all__'

const MESSAGE_TABS: MessageTabOption[] = [
  { label: '全部', value: 'all' },
  { label: '未读', value: 'unread' },
  { label: '已关闭', value: 'closed' },
]

const DEFAULT_CHANNEL_BRAND: ChannelBrand = {
  accent: '#4a98ff',
  soft: 'rgba(74, 152, 255, 0.12)',
  border: 'rgba(74, 152, 255, 0.18)',
  avatarStart: '#8dbdff',
  avatarEnd: '#4a98ff',
  label: '客',
}

const router = useRouter()
const notificationCenterStore = useNotificationCenterStore()

const loading = ref(false)
const isSearchVisible = ref(false)
const searchKeyword = ref('')
const loadNotice = ref('')
const activeTab = ref<MessageTabValue>('all')
const activeChannel = ref(ALL_CHANNEL_VALUE)
const threads = ref<MessageThreadDTO[]>([])

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
      formatThreadTitle(thread),
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
  return MESSAGE_TABS.find((item) => item.value === activeTab.value)?.label || '全部'
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

const resultsSummaryText = computed(() => {
  const unreadLabel = unreadThreadCount.value > 0 ? ` · 未读 ${unreadThreadCount.value}` : ''
  return `${activeTabLabel.value} · ${displayedThreads.value.length} 条会话${unreadLabel}`
})

const resultsNoticeText = computed(() => {
  if (activeTab.value === 'unread') {
    return '仅显示未读会话，方便优先处理住客消息。'
  }

  if (activeTab.value === 'closed') {
    return '已关闭或归档的会话会保留在这里，便于后续查阅。'
  }

  return ''
})

const emptyTitle = computed(() => {
  if (trimmedKeyword.value) {
    return '没有匹配的会话'
  }

  if (activeTab.value === 'unread') {
    return '当前没有未读消息'
  }

  if (activeTab.value === 'closed') {
    return '当前没有已关闭会话'
  }

  return '当前暂无会话'
})

const emptyDescription = computed(() => {
  if (trimmedKeyword.value) {
    return '可以尝试调整关键词，或清空筛选后重新查看全部会话。'
  }

  if (activeChannel.value !== ALL_CHANNEL_VALUE) {
    return '当前渠道下暂时没有匹配会话，可以切换其他渠道查看。'
  }

  return '新会话同步后会显示在这里，点击任一会话可进入详情继续沟通。'
})

function resolveWarningMessage(error: unknown, fallbackMessage: string) {
  if (error instanceof Error && error.message) {
    return error.message
  }

  return fallbackMessage
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

function normalizeChannelName(value?: string) {
  return (value || '').trim().toLowerCase()
}

function resolveChannelBrand(thread: MessageThreadDTO): ChannelBrand {
  const normalized = normalizeChannelName(thread.channelName)

  if (normalized.includes('meituan') || (thread.channelName || '').includes('美团')) {
    return {
      accent: '#38c2a4',
      soft: 'rgba(56, 194, 164, 0.14)',
      border: 'rgba(56, 194, 164, 0.24)',
      avatarStart: '#87ead4',
      avatarEnd: '#38c2a4',
      label: '美',
    }
  }

  if (normalized.includes('airbnb')) {
    return {
      accent: '#ff6f7a',
      soft: 'rgba(255, 111, 122, 0.14)',
      border: 'rgba(255, 111, 122, 0.2)',
      avatarStart: '#ffb0b8',
      avatarEnd: '#ff6f7a',
      label: 'A',
    }
  }

  if (normalized.includes('booking')) {
    return {
      accent: '#4f72dc',
      soft: 'rgba(79, 114, 220, 0.14)',
      border: 'rgba(79, 114, 220, 0.2)',
      avatarStart: '#9cadff',
      avatarEnd: '#4f72dc',
      label: 'B',
    }
  }

  return DEFAULT_CHANNEL_BRAND
}

function resolveThreadVars(thread: MessageThreadDTO) {
  const brand = resolveChannelBrand(thread)

  return {
    '--thread-accent': brand.accent,
    '--thread-soft': brand.soft,
    '--thread-border': brand.border,
    '--thread-avatar-start': brand.avatarStart,
    '--thread-avatar-end': brand.avatarEnd,
  }
}

function formatThreadTitle(thread: MessageThreadDTO) {
  if (thread.guestName) {
    return thread.guestName
  }

  if (thread.listingName) {
    return thread.listingName
  }

  return thread.channelName || '未命名会话'
}

function resolveAvatarLabel(thread: MessageThreadDTO) {
  const source = formatThreadTitle(thread).trim()
  if (!source) {
    return '客'
  }

  const firstCharacter = source.slice(0, 1)
  if (/[a-z]/i.test(firstCharacter)) {
    return firstCharacter.toUpperCase()
  }

  return firstCharacter
}

function formatTwoDigit(value: number) {
  return String(value).padStart(2, '0')
}

function formatMonthDay(value?: string) {
  if (!value) {
    return '--'
  }

  if (/^\d{4}-\d{2}-\d{2}$/.test(value)) {
    return value.slice(5)
  }

  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return '--'
  }

  return `${formatTwoDigit(date.getMonth() + 1)}-${formatTwoDigit(date.getDate())}`
}

function formatThreadDate(value: string) {
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return '--'
  }

  const now = new Date()
  const isSameDay =
    date.getFullYear() === now.getFullYear() &&
    date.getMonth() === now.getMonth() &&
    date.getDate() === now.getDate()

  if (isSameDay) {
    return `${formatTwoDigit(date.getHours())}:${formatTwoDigit(date.getMinutes())}`
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

  return '未关联房型'
}

function formatStayDateRange(thread: MessageThreadDTO) {
  const checkInLabel = formatMonthDay(thread.checkInDate)
  const checkOutLabel = formatMonthDay(thread.checkOutDate)

  if (checkInLabel === '--' && checkOutLabel === '--') {
    return '未关联入住日期'
  }

  if (checkInLabel === '--') {
    return `离店 ${checkOutLabel}`
  }

  if (checkOutLabel === '--') {
    return `入住 ${checkInLabel}`
  }

  return `${checkInLabel} ~ ${checkOutLabel}`
}

function getTodayDateKey() {
  const today = new Date()
  return `${today.getFullYear()}-${formatTwoDigit(today.getMonth() + 1)}-${formatTwoDigit(today.getDate())}`
}

function resolveStayStatus(thread: MessageThreadDTO): ThreadStatusView {
  if (thread.closed) {
    return {
      label: '已关闭',
      tone: 'closed',
    }
  }

  const today = getTodayDateKey()
  const checkInDate = thread.checkInDate || ''
  const checkOutDate = thread.checkOutDate || ''

  if (checkInDate && today < checkInDate) {
    return {
      label: '待入住',
      tone: 'upcoming',
    }
  }

  if (checkInDate && checkOutDate && today >= checkInDate && today < checkOutDate) {
    return {
      label: '在住',
      tone: 'staying',
    }
  }

  if (checkOutDate && today >= checkOutDate) {
    return {
      label: '已离店',
      tone: 'completed',
    }
  }

  if (thread.unreadCount > 0) {
    return {
      label: '待回复',
      tone: 'reply',
    }
  }

  return {
    label: '进行中',
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
      throw new Error(response.message || '加载会话失败')
    }

    ensureActiveChannelStillExists(response.data)
    threads.value = response.data
    notificationCenterStore.syncUnreadMessageCount(threads.value)
  } catch (error) {
    loadNotice.value = resolveWarningMessage(error, '加载会话失败')
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

onIonViewWillEnter(async () => {
  await loadThreads()
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
  color: var(--ios-pms-text-primary);
  font-size: var(--ios-pms-font-title-xl-size);
  font-weight: var(--ios-pms-weight-heavy);
  letter-spacing: -0.04em;
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
  font-size: 25px;
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
  --background: transparent;
  --border-radius: 0;
  --box-shadow: none;
  --color: #10131a;
  --icon-color: #c7ccd8;
  --placeholder-color: #c7ccd8;
  --placeholder-opacity: 1;
  --clear-button-color: #8a90a0;
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
  background: #ffffff;
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
  border: 1px solid rgba(255, 255, 255, 0.28);
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
  border: 2px solid #ffffff;
  border-radius: 999px;
  background: #4a98ff;
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
