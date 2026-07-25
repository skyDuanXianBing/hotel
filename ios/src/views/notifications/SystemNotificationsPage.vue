<template>
  <ion-page>
    <ion-header translucent>
      <ion-toolbar class="orders-header__toolbar">
        <ion-buttons slot="start">
          <ion-back-button class="notifications-header__back-btn" :default-href="ROUTE_PATHS.home" />
        </ion-buttons>
        <ion-title class="orders-header__title">{{ $t('routes.SystemNotifications') }}</ion-title>
        <ion-buttons slot="end">
          <ion-button class="orders-header__icon-btn" fill="clear" @click="handleToggleSearch">
            <ion-icon :icon="searchOutline" />
          </ion-button>
        </ion-buttons>
      </ion-toolbar>
    </ion-header>

    <ion-content fullscreen class="mobile-page notifications-page">
      <ion-refresher slot="fixed" @ionRefresh="handleRefresh">
        <ion-refresher-content
          :pulling-text="t('notifications.system.pullToRefresh')"
          refreshing-spinner="crescent"
        />
      </ion-refresher>

      <div class="orders-page__shell">
        <section v-if="isSearchVisible" class="orders-search-panel">
          <div class="orders-search-panel__bar">
            <ion-searchbar
              v-model="searchKeyword"
              :debounce="0"
              class="orders-searchbar"
              :placeholder="t('notifications.searchPlaceholder')"
            />
            <button type="button" class="orders-search-panel__cancel" @click="handleHideSearch">
              {{ t('notifications.cancel') }}
            </button>
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
              <p class="orders-list-header__summary">
                {{ t('notifications.system.results', { tab: activeTabLabel, count: totalElements }) }}
              </p>
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
import { useI18n } from 'vue-i18n'
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
const { t } = useI18n()

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

const displayTabs = computed<Array<{ label: string; value: NotificationReadFilter }>>(() => [
  { label: t('notifications.tabs.all'), value: 'all' },
  { label: t('notifications.tabs.unread'), value: 'unread' },
  { label: t('notifications.tabs.read'), value: 'read' },
])

const typeOptions = computed<Array<{ label: string; value: SystemNotificationType | '' }>>(() => [
  { label: t('notifications.system.allTypes'), value: '' },
  { label: t('notifications.system.type.system'), value: 'SYSTEM' },
  { label: t('notifications.system.type.cleaning'), value: 'CLEANING' },
  { label: t('notifications.system.type.task'), value: 'TASK' },
])

const totalElements = computed(() => notifications.value.length)
const activeTabLabel = computed(() => {
  if (activeTab.value === 'unread') {
    return t('notifications.tabLabel.unread')
  }
  if (activeTab.value === 'read') {
    return t('notifications.tabLabel.read')
  }
  return t('notifications.tabLabel.all')
})
const activeTabTag = computed(() => {
  if (activeTab.value === 'unread') {
    return t('notifications.tabTag.unread')
  }
  if (activeTab.value === 'read') {
    return t('notifications.tabTag.read')
  }
  return t('notifications.tabTag.all')
})
const typeFilterTag = computed(() => {
  if (!selectedType.value) {
    return t('notifications.system.allTypes')
  }
  return formatTypeLabel(selectedType.value)
})
const emptyTitle = computed(() => {
  if (committedKeyword.value) {
    return t('notifications.system.emptySearch')
  }
  if (activeTab.value === 'unread') {
    return t('notifications.system.emptyUnread')
  }
  if (activeTab.value === 'read') {
    return t('notifications.system.emptyRead')
  }
  if (selectedType.value) {
    return t('notifications.system.emptyType', { type: formatTypeLabel(selectedType.value) })
  }
  return t('notifications.system.emptyDefault')
})

function resolveWarningMessage(error: unknown, fallbackMessage: string) {
  if (error instanceof Error && error.message) {
    return error.message
  }

  return fallbackMessage
}

function formatTypeLabel(type: string) {
  if (type === 'SYSTEM') {
    return t('notifications.system.type.system')
  }
  if (type === 'CLEANING') {
    return t('notifications.system.type.cleaning')
  }
  if (type === 'TASK') {
    return t('notifications.system.type.task')
  }
  return type || t('notifications.system.type.system')
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
    throw new Error(t('notifications.userMissing'))
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
      throw new Error(response.message || t('notifications.system.loadFailed'))
    }

    allNotifications.value = (response.data.content || []).filter((item) =>
      isSystemNotificationType(item.notificationType),
    )
    applyLocalFilter()
  } catch (error) {
    loadNotice.value = resolveWarningMessage(error, t('notifications.system.loadFailed'))
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
      throw new Error(response.message || t('notifications.markReadFailed'))
    }

    showSuccessToast(t('notifications.markReadSuccess'))
    await loadPage()
  } catch (error) {
    if (!isHandledRequestError(error)) {
      showWarningToast(resolveWarningMessage(error, t('notifications.markReadFailed')))
    }
  }
}

async function handleDelete(id: number) {
  const alert = await alertController.create({
    header: t('notifications.system.deleteTitle'),
    message: t('notifications.system.deleteConfirm'),
    buttons: [
      { text: t('notifications.cancel'), role: 'cancel' },
      { text: t('notifications.delete'), role: 'destructive' },
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
      throw new Error(response.message || t('notifications.deleteFailed'))
    }

    showSuccessToast(t('notifications.deleted'))
    await loadPage()
  } catch (error) {
    if (!isHandledRequestError(error)) {
      showWarningToast(resolveWarningMessage(error, t('notifications.deleteFailed')))
    }
  }
}

async function presentNotificationActions(item: NotificationMessageDTO) {
  const buttons: Array<Record<string, unknown>> = []

  if (!item.isRead) {
    buttons.push({
      text: t('notifications.markRead'),
      handler: () => {
        void handleMarkAsRead(item.id)
      },
    })
  }

  buttons.push({
    text: t('notifications.system.deleteTitle'),
    role: 'destructive',
    handler: () => {
      void handleDelete(item.id)
    },
  })

  buttons.push({
    text: t('notifications.cancel'),
    role: 'cancel',
  })

  const actionSheet = await actionSheetController.create({
    header: item.title || t('notifications.system.type.system'),
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
  --padding-start: 0;
  --padding-end: 0;
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
  color: var(--ios-pms-header-title-color);
  font-size: var(--ios-pms-font-title-xl-size);
  font-weight: 500;
  letter-spacing: -0.04em;
}

.notifications-header__back-btn {
  --color: var(--ios-pms-header-control-color);
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
  font-size: 21px;
}

.orders-header__icon-btn ion-icon {
  width: 21px;
  height: 21px;
}

.orders-page__shell {
  display: flex;
  flex-direction: column;
  min-height: 100%;
  padding: 2px 0 0;
  background: #ffffff;
}

.orders-search-panel {
  padding: 0 14px 8px;
  background: #ffffff;
}

.orders-search-panel__bar {
  display: flex;
  align-items: center;
  gap: 12px;
}

.orders-searchbar {
  flex: 1;
  min-height: 44px;
  margin: 0;
  padding: 0;
  --background: #fafafa;
  --border-radius: 22px;
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

.orders-searchbar::part(input) {
  min-height: 44px;
  border: 1px solid #ededed;
  border-radius: 22px;
  background: #fafafa;
  box-shadow: none;
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
  --orders-strip-fade-width: 14px;
}

.orders-tabs-strip::after,
.orders-filter-row::after {
  content: '';
  position: absolute;
  right: 0;
  top: 0;
  bottom: 0;
  width: var(--orders-strip-fade-width);
  background: linear-gradient(90deg, rgba(250, 252, 255, 0), #fafcff 78%);
  pointer-events: none;
}

.orders-tabs-strip {
  background: #ffffff;
}

.orders-tabs-strip::after {
  background: linear-gradient(90deg, rgba(255, 255, 255, 0), #ffffff 78%);
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
  left: 0;
  right: 0;
  bottom: 0;
  height: 4px;
  border-radius: 999px;
  background: #4a98ff;
}

.system-notifications-controls {
  display: block;
  padding: 10px 0 6px;
  background: #fafcff;
}

.system-notifications-controls__rail {
  user-select: none;
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

.orders-filter-chip:disabled {
  opacity: 0.56;
}

.orders-list-section {
  flex: 1;
  padding: 0 14px 40px;
  background: #fafcff;
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
  width: 18px;
  height: 18px;
  color: #4a98ff;
  flex-shrink: 0;
}

.orders-list {
  display: grid;
  gap: 12px;
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
