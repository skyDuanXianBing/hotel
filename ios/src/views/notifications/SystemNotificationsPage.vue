<template>
  <ion-page>
    <ion-header translucent>
      <ion-toolbar class="orders-header__toolbar">
        <ion-buttons slot="start">
          <ion-back-button class="notifications-header__back-btn" :default-href="ROUTE_PATHS.home" text="" />
        </ion-buttons>
        <ion-title class="orders-header__title">系统通知</ion-title>
        <ion-buttons slot="end">
          <ion-button class="orders-header__icon-btn" fill="clear" @click="handleToggleSearch">
            <ion-icon :icon="searchOutline" />
          </ion-button>
        </ion-buttons>
      </ion-toolbar>
    </ion-header>

    <ion-content fullscreen class="mobile-page notifications-page">
      <ion-refresher slot="fixed" @ionRefresh="handleRefresh">
        <ion-refresher-content pulling-text="下拉刷新系统通知" refreshing-spinner="crescent" />
      </ion-refresher>

      <div class="orders-page__shell">
        <section v-if="isSearchVisible" class="orders-search-panel">
          <div class="orders-search-panel__bar">
            <ion-searchbar
              v-model="searchKeyword"
              :debounce="0"
              class="orders-searchbar"
              placeholder="搜索通知标题或内容"
            />
            <button type="button" class="orders-search-panel__cancel" @click="handleHideSearch">取消</button>
          </div>
        </section>

        <section class="orders-tabs-strip">
          <div class="orders-tabs-strip__scroll">
            <button
              v-for="item in displayTabs"
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

        <section class="system-notifications-controls">
          <div class="orders-filter-row system-notifications-controls__rail">
            <div class="orders-filter-row__scroll">
              <button
                v-for="item in typeOptions"
                :key="item.value || 'all-types'"
                type="button"
                class="orders-filter-chip"
                :class="{ 'is-active': item.value === selectedType }"
                @click="handleSelectType(item.value)"
              >
                {{ item.label }}
              </button>
            </div>
          </div>
        </section>

        <section class="orders-list-section">
          <div class="orders-list-header">
            <div class="orders-list-header__heading">
              <p class="orders-list-header__summary">{{ activeTabLabel }} {{ totalElements }}条结果</p>
              <div v-if="selectedType || activeTab !== 'all' || committedKeyword" class="orders-list-header__tags">
                <span v-if="activeTab !== 'all'" class="orders-list-header__tag">{{ activeTabTag }}</span>
                <span v-if="selectedType" class="orders-list-header__tag">{{ typeFilterTag }}</span>
                <span v-if="committedKeyword" class="orders-list-header__tag">{{ committedKeyword }}</span>
              </div>
            </div>
            <ion-spinner v-if="loading" name="crescent" class="orders-list-header__spinner" />
          </div>

          <p v-if="loadNotice" class="orders-notice-text orders-notice-text--warning">{{ loadNotice }}</p>

          <div v-if="notifications.length > 0" class="orders-list">
            <SystemNotificationCard
              v-for="item in notifications"
              :key="item.id"
              :notification="item"
              @open-actions="presentNotificationActions(item)"
            />
          </div>

          <div v-else-if="!loading" class="orders-empty-state">
            <div class="orders-empty-state__illustration" aria-hidden="true">
              <span class="orders-empty-state__box"></span>
            </div>
            <p class="orders-empty">{{ emptyTitle }}</p>
          </div>
        </section>
      </div>
    </ion-content>
  </ion-page>
</template>

<script setup lang="ts">
import {
  actionSheetController,
  alertController,
  IonBackButton,
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
import { searchOutline } from 'ionicons/icons'
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import SystemNotificationCard from '@/components/notification/SystemNotificationCard.vue'
import {
  deleteNotificationMessage,
  getNotificationMessages,
  markNotificationAsRead,
} from '@/api/notification'
import { ROUTE_PATHS } from '@/router/guards'
import { useUserStore } from '@/stores/user'
import type { NotificationMessageDTO, NotificationReadFilter } from '@/types/notification'
import { showSuccessToast, showWarningToast } from '@/utils/notify'
import { isHandledRequestError } from '@/utils/request'

const SYSTEM_TYPES = ['SYSTEM', 'CLEANING', 'TASK'] as const
const SEARCH_DEBOUNCE = 280

type SystemNotificationType = (typeof SYSTEM_TYPES)[number]

const userStore = useUserStore()

const loading = ref(false)
const loadNotice = ref('')
const searchKeyword = ref('')
const committedKeyword = ref('')
const selectedType = ref<SystemNotificationType | ''>('')
const activeTab = ref<NotificationReadFilter>('all')
const isSearchVisible = ref(false)
const allNotifications = ref<NotificationMessageDTO[]>([])
const notifications = ref<NotificationMessageDTO[]>([])

let searchTimer = 0

const displayTabs: Array<{ label: string; value: NotificationReadFilter }> = [
  { label: '全部', value: 'all' },
  { label: '未读', value: 'unread' },
  { label: '已读', value: 'read' },
]

const typeOptions: Array<{ label: string; value: SystemNotificationType | '' }> = [
  { label: '全部类型', value: '' },
  { label: '系统通知', value: 'SYSTEM' },
  { label: '保洁通知', value: 'CLEANING' },
  { label: '任务通知', value: 'TASK' },
]

const totalElements = computed(() => notifications.value.length)
const activeTabLabel = computed(() => {
  if (activeTab.value === 'unread') {
    return '未读提醒'
  }
  if (activeTab.value === 'read') {
    return '已读提醒'
  }
  return '全部提醒'
})
const activeTabTag = computed(() => {
  if (activeTab.value === 'unread') {
    return '仅看未读'
  }
  if (activeTab.value === 'read') {
    return '仅看已读'
  }
  return '全部'
})
const typeFilterTag = computed(() => {
  if (!selectedType.value) {
    return '全部类型'
  }
  return formatTypeLabel(selectedType.value)
})
const emptyTitle = computed(() => {
  if (committedKeyword.value) {
    return '没有匹配的系统通知'
  }
  if (activeTab.value === 'unread') {
    return '暂无未读系统通知'
  }
  if (activeTab.value === 'read') {
    return '暂无已读系统通知'
  }
  if (selectedType.value) {
    return `${formatTypeLabel(selectedType.value)}暂时为空`
  }
  return '暂无系统通知'
})

function resolveWarningMessage(error: unknown, fallbackMessage: string) {
  if (error instanceof Error && error.message) {
    return error.message
  }

  return fallbackMessage
}

function formatTypeLabel(type: string) {
  if (type === 'SYSTEM') {
    return '系统通知'
  }
  if (type === 'CLEANING') {
    return '保洁通知'
  }
  if (type === 'TASK') {
    return '任务通知'
  }
  return type || '系统通知'
}

function isSystemNotificationType(type: string): type is SystemNotificationType {
  return SYSTEM_TYPES.includes(type as SystemNotificationType)
}

function applyLocalFilter() {
  const keyword = committedKeyword.value.trim().toLowerCase()

  notifications.value = allNotifications.value.filter((item) => {
    if (selectedType.value && item.notificationType !== selectedType.value) {
      return false
    }

    if (activeTab.value === 'unread' && item.isRead) {
      return false
    }

    if (activeTab.value === 'read' && !item.isRead) {
      return false
    }

    if (!keyword) {
      return true
    }

    const title = item.title?.toLowerCase() || ''
    const content = item.content?.toLowerCase() || ''
    return title.includes(keyword) || content.includes(keyword)
  })
}

async function ensureUserId() {
  if (userStore.currentUser?.id) {
    return userStore.currentUser.id
  }

  const user = await userStore.fetchCurrentUser(true)
  if (!user?.id) {
    throw new Error('未获取到当前用户')
  }

  return user.id
}

async function loadPage() {
  loading.value = true
  loadNotice.value = ''

  try {
    const userId = await ensureUserId()
    const response = await getNotificationMessages(userId, 0, 100)
    if (!response.success || !response.data) {
      throw new Error(response.message || '加载系统通知失败')
    }

    allNotifications.value = (response.data.content || []).filter((item) =>
      isSystemNotificationType(item.notificationType),
    )
    applyLocalFilter()
  } catch (error) {
    loadNotice.value = resolveWarningMessage(error, '加载系统通知失败')
    if (!isHandledRequestError(error)) {
      showWarningToast(loadNotice.value)
    }
  } finally {
    loading.value = false
  }
}

async function handleMarkAsRead(id: number) {
  try {
    const response = await markNotificationAsRead(id)
    if (!response.success) {
      throw new Error(response.message || '标记已读失败')
    }

    showSuccessToast('已标记为已读')
    await loadPage()
  } catch (error) {
    if (!isHandledRequestError(error)) {
      showWarningToast(resolveWarningMessage(error, '标记已读失败'))
    }
  }
}

async function handleDelete(id: number) {
  const alert = await alertController.create({
    header: '删除通知',
    message: '确认删除这条系统通知吗？',
    buttons: [
      { text: '取消', role: 'cancel' },
      { text: '删除', role: 'destructive' },
    ],
  })

  await alert.present()
  const result = await alert.onDidDismiss()
  if (result.role !== 'destructive') {
    return
  }

  try {
    const response = await deleteNotificationMessage(id)
    if (!response.success) {
      throw new Error(response.message || '删除通知失败')
    }

    showSuccessToast('通知已删除')
    await loadPage()
  } catch (error) {
    if (!isHandledRequestError(error)) {
      showWarningToast(resolveWarningMessage(error, '删除通知失败'))
    }
  }
}

async function presentNotificationActions(item: NotificationMessageDTO) {
  const buttons: Array<Record<string, unknown>> = []

  if (!item.isRead) {
    buttons.push({
      text: '标记已读',
      handler: () => {
        void handleMarkAsRead(item.id)
      },
    })
  }

  buttons.push({
    text: '删除通知',
    role: 'destructive',
    handler: () => {
      void handleDelete(item.id)
    },
  })

  buttons.push({
    text: '取消',
    role: 'cancel',
  })

  const actionSheet = await actionSheetController.create({
    header: item.title || '系统通知',
    subHeader: formatTypeLabel(item.notificationType),
    buttons,
  })

  await actionSheet.present()
}

async function handleRefresh(event: CustomEvent) {
  try {
    await loadPage()
  } finally {
    event.detail.complete()
  }
}

function handleSelectTab(tab: NotificationReadFilter) {
  if (activeTab.value === tab) {
    return
  }

  activeTab.value = tab
  applyLocalFilter()
}

function handleSelectType(type: SystemNotificationType | '') {
  if (selectedType.value === type) {
    return
  }

  selectedType.value = type
  applyLocalFilter()
}

function handleToggleSearch() {
  isSearchVisible.value = !isSearchVisible.value
  if (!isSearchVisible.value && !committedKeyword.value) {
    searchKeyword.value = ''
  }
}

function handleHideSearch() {
  isSearchVisible.value = false
  if (!committedKeyword.value) {
    searchKeyword.value = ''
  }
}

watch(
  () => searchKeyword.value,
  (keyword) => {
    window.clearTimeout(searchTimer)

    const nextKeyword = keyword.trim()
    if (nextKeyword === committedKeyword.value) {
      return
    }

    searchTimer = window.setTimeout(() => {
      committedKeyword.value = nextKeyword
      applyLocalFilter()
    }, SEARCH_DEBOUNCE)
  },
)

onBeforeUnmount(() => {
  window.clearTimeout(searchTimer)
})

onIonViewWillEnter(async () => {
  await loadPage()
})
</script>

<style scoped>
.notifications-page {
  display: block;
  --background: #ffffff;
}

.notifications-page :deep(ion-header) {
  backdrop-filter: blur(14px);
}

.notifications-page :deep(ion-header::after) {
  display: none;
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

.notifications-header__back-btn {
  --color: #141821;
  --icon-font-size: 22px;
  --padding-start: 0;
  --padding-end: 6px;
  min-height: 34px;
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
  gap: 12px;
  min-height: 100%;
  padding: 10px 0 32px;
  background: linear-gradient(180deg, #ffffff 0%, #fcfcfe 100%);
}

.orders-search-panel {
  padding: 0 14px;
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
  --orders-strip-edge: 14px;
  --orders-strip-fade-width: 18px;
}

.orders-tabs-strip::after,
.orders-filter-row::after {
  content: '';
  position: absolute;
  right: 0;
  top: 0;
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
  overflow-y: hidden;
  padding-left: var(--orders-strip-edge);
  padding-right: calc(var(--orders-strip-edge) + var(--orders-strip-fade-width) + 22px);
  scroll-padding-left: var(--orders-strip-edge);
  scroll-padding-right: calc(var(--orders-strip-edge) + var(--orders-strip-fade-width) + 22px);
  -webkit-overflow-scrolling: touch;
  overscroll-behavior-x: contain;
  touch-action: pan-x;
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
  left: 0;
  right: 0;
  bottom: 0;
  height: 4px;
  border-radius: 999px;
  background: #4a98ff;
}

.system-notifications-controls {
  display: block;
}

.system-notifications-controls__rail {
  user-select: none;
}

.orders-filter-chip {
  flex: 0 0 auto;
  display: inline-flex;
  align-items: center;
  justify-content: center;
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

.orders-filter-chip:disabled {
  opacity: 0.56;
}

.orders-list-section {
  flex: 1;
  padding: 2px 14px 40px;
}

.orders-list-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 10px;
  padding: 0 2px 14px;
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
  color: #4a98ff;
  flex-shrink: 0;
}

.orders-list {
  display: grid;
  gap: 10px;
}

.orders-notice-text {
  margin: 0;
  padding: 0 2px 14px;
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
  left: 16px;
  right: 16px;
  bottom: 10px;
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
  background: #ffffff;
}

.orders-empty-state__box::before {
  left: 14px;
  border-right: 0;
  transform: skewX(-38deg);
  border-top-left-radius: 12px;
}

.orders-empty-state__box::after {
  right: 14px;
  border-left: 0;
  transform: skewX(38deg);
  border-top-right-radius: 12px;
}

.orders-empty {
  margin: 0;
  color: #1b1e26;
  font-size: 17px;
  font-weight: 500;
  text-align: center;
}
</style>
