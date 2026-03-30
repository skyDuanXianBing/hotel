<template>
  <ion-page>
    <ion-header translucent>
      <ion-toolbar>
        <ion-buttons slot="start">
          <ion-back-button :default-href="ROUTE_PATHS.home" />
        </ion-buttons>
        <ion-title>订单通知</ion-title>
      </ion-toolbar>
    </ion-header>

    <ion-content fullscreen class="mobile-page notifications-page">
      <ion-refresher slot="fixed" @ionRefresh="handleRefresh">
        <ion-refresher-content pulling-text="下拉刷新订单通知" refreshing-spinner="crescent" />
      </ion-refresher>

      <section class="mobile-hero notifications-hero">
        <p class="mobile-note notifications-hero__eyebrow">订单通知迁移</p>
        <h1 class="mobile-title">订单提醒</h1>
        <p class="mobile-subtitle">保持与 Web 一致的订单通知分流，支持搜索、筛选和全部已读。</p>
        <div class="mobile-chip-row">
          <span class="mobile-chip">未读 {{ unreadCount }}</span>
          <span class="mobile-chip">列表 {{ notifications.length }}</span>
          <span class="mobile-chip">固定类型 ORDER</span>
        </div>
      </section>

      <div class="mobile-stack">
        <section class="mobile-card notifications-toolbar-card">
          <ion-searchbar v-model="searchKeyword" :debounce="300" placeholder="搜索订单通知标题或内容" />

          <ion-segment :value="activeFilter" @ionChange="handleFilterChange">
            <ion-segment-button value="all">
              <ion-label>全部</ion-label>
            </ion-segment-button>
            <ion-segment-button value="unread">
              <ion-label>未读</ion-label>
            </ion-segment-button>
            <ion-segment-button value="read">
              <ion-label>已读</ion-label>
            </ion-segment-button>
          </ion-segment>

          <div class="notifications-toolbar-card__actions">
            <ion-button fill="outline" size="small" @click="loadPage">刷新</ion-button>
            <ion-button fill="outline" size="small" :disabled="unreadCount <= 0" @click="handleMarkAllAsRead">
              全部已读
            </ion-button>
          </div>

          <p v-if="loadNotice" class="mobile-note notifications-toolbar-card__notice">{{ loadNotice }}</p>
        </section>

        <section class="mobile-card">
          <div class="mobile-inline-row">
            <div>
              <h2 class="mobile-section-title">通知列表</h2>
              <p class="mobile-note">订单类提醒单独展示，不与系统通知混流。</p>
            </div>
            <ion-spinner v-if="loading" name="crescent" />
          </div>

          <div v-if="notifications.length > 0" class="mobile-list notifications-list">
            <article v-for="item in notifications" :key="item.id" class="notification-card">
              <div class="notification-card__header">
                <div>
                  <strong>{{ item.title }}</strong>
                  <p>订单提醒 · {{ formatDateTime(item.createdAt) }}</p>
                </div>
                <span v-if="!item.isRead" class="notification-card__badge">未读</span>
              </div>

              <p class="notification-card__content">{{ item.content }}</p>

              <div class="notification-card__actions">
                <ion-button v-if="!item.isRead" fill="outline" size="small" @click="handleMarkAsRead(item.id)">
                  标为已读
                </ion-button>
                <ion-button color="danger" fill="clear" size="small" @click="handleDelete(item.id)">
                  删除
                </ion-button>
              </div>
            </article>
          </div>

          <div v-else-if="!loading" class="notifications-empty-state">
            <h3>当前条件下暂无订单通知</h3>
            <p class="mobile-note">稍后可下拉刷新，或切换未读 / 已读状态继续查看。</p>
          </div>
        </section>
      </div>
    </ion-content>
  </ion-page>
</template>

<script setup lang="ts">
import {
  alertController,
  IonBackButton,
  IonButton,
  IonButtons,
  IonContent,
  IonHeader,
  IonLabel,
  IonPage,
  IonRefresher,
  IonRefresherContent,
  IonSearchbar,
  IonSegment,
  IonSegmentButton,
  IonSpinner,
  IonTitle,
  IonToolbar,
  onIonViewWillEnter,
} from '@ionic/vue'
import { ref, watch } from 'vue'
import {
  deleteNotificationMessage,
  getNotificationMessagesByType,
  getUnreadNotificationCountByType,
  markAllNotificationsAsReadByType,
  markNotificationAsRead,
} from '@/api/notification'
import { ROUTE_PATHS } from '@/router/guards'
import { useUserStore } from '@/stores/user'
import type { NotificationMessageDTO, NotificationReadFilter } from '@/types/notification'
import { isHandledRequestError } from '@/utils/request'
import { showSuccessToast, showWarningToast } from '@/utils/notify'

const ORDER_NOTIFICATION_TYPE = 'ORDER'

const userStore = useUserStore()

const loading = ref(false)
const loadNotice = ref('')
const searchKeyword = ref('')
const activeFilter = ref<NotificationReadFilter>('all')
const notifications = ref<NotificationMessageDTO[]>([])
const unreadCount = ref(0)

function resolveWarningMessage(error: unknown, fallbackMessage: string) {
  if (error instanceof Error && error.message) {
    return error.message
  }

  return fallbackMessage
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

function resolveReadFlag() {
  if (activeFilter.value === 'unread') {
    return false
  }

  if (activeFilter.value === 'read') {
    return true
  }

  return undefined
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
    const [listResponse, unreadResponse] = await Promise.all([
      getNotificationMessagesByType(
        userId,
        ORDER_NOTIFICATION_TYPE,
        0,
        100,
        resolveReadFlag(),
        searchKeyword.value,
      ),
      getUnreadNotificationCountByType(userId, ORDER_NOTIFICATION_TYPE),
    ])

    if (!listResponse.success || !listResponse.data) {
      throw new Error(listResponse.message || '加载订单通知失败')
    }

    notifications.value = listResponse.data.content || []
    if (unreadResponse.success) {
      unreadCount.value = Number(unreadResponse.data || 0)
    }
  } catch (error) {
    loadNotice.value = resolveWarningMessage(error, '加载订单通知失败')
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

async function handleMarkAllAsRead() {
  try {
    const userId = await ensureUserId()
    const response = await markAllNotificationsAsReadByType(userId, ORDER_NOTIFICATION_TYPE)
    if (!response.success) {
      throw new Error(response.message || '批量已读失败')
    }
    showSuccessToast('订单通知已全部标记')
    await loadPage()
  } catch (error) {
    if (!isHandledRequestError(error)) {
      showWarningToast(resolveWarningMessage(error, '批量已读失败'))
    }
  }
}

async function handleDelete(id: number) {
  const alert = await alertController.create({
    header: '删除订单通知',
    message: '确认删除这条订单通知吗？',
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

async function handleRefresh(event: CustomEvent) {
  try {
    await loadPage()
  } finally {
    event.detail.complete()
  }
}

async function handleFilterChange(event: CustomEvent) {
  activeFilter.value = event.detail.value as NotificationReadFilter
  await loadPage()
}

watch(
  () => searchKeyword.value,
  async () => {
    await loadPage()
  },
)

onIonViewWillEnter(async () => {
  await loadPage()
})
</script>

<style scoped>
.notifications-page {
  display: block;
}

.notifications-hero {
  margin-top: 4px;
}

.notifications-hero__eyebrow {
  color: var(--ion-color-primary);
  font-weight: 700;
}

.notifications-toolbar-card {
  display: grid;
  gap: 12px;
}

.notifications-toolbar-card__actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.notifications-toolbar-card__notice {
  color: var(--ion-color-warning);
}

.notifications-list {
  margin-top: 16px;
}

.notification-card {
  padding: 14px;
  border-radius: 20px;
  border: 1px solid var(--app-border);
  background: rgba(255, 255, 255, 0.84);
}

.notification-card__header {
  display: flex;
  justify-content: space-between;
  gap: 12px;
}

.notification-card__header strong,
.notification-card__header p,
.notification-card__content {
  margin: 0;
}

.notification-card__header p {
  margin-top: 6px;
  color: var(--app-muted);
  font-size: 13px;
}

.notification-card__badge {
  padding: 5px 10px;
  border-radius: 999px;
  background: rgba(220, 38, 38, 0.12);
  color: var(--ion-color-danger);
  font-size: 12px;
  font-weight: 700;
}

.notification-card__content {
  margin-top: 12px;
  color: var(--app-heading);
  line-height: 1.6;
  white-space: pre-wrap;
}

.notification-card__actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  margin-top: 14px;
}

.notifications-empty-state {
  display: grid;
  gap: 10px;
  justify-items: flex-start;
  padding-top: 18px;
}

.notifications-empty-state h3,
.notifications-empty-state p {
  margin: 0;
}
</style>
