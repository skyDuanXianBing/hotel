<template>
  <ion-page>
    <ion-header translucent>
      <ion-toolbar>
        <ion-buttons slot="start">
          <ion-back-button :default-href="ROUTE_PATHS.home" />
        </ion-buttons>
        <ion-title>系统通知</ion-title>
      </ion-toolbar>
    </ion-header>

    <ion-content fullscreen class="mobile-page notifications-page">
      <ion-refresher slot="fixed" @ionRefresh="handleRefresh">
        <ion-refresher-content pulling-text="下拉刷新通知" refreshing-spinner="crescent" />
      </ion-refresher>

      <section class="mobile-hero notifications-hero">
        <p class="mobile-note notifications-hero__eyebrow">系统通知迁移</p>
        <h1 class="mobile-title">系统与任务通知</h1>
        <p class="mobile-subtitle">保留筛选、搜索、未读状态、单条已读、批量已读和删除能力。</p>
        <div class="mobile-chip-row">
          <span class="mobile-chip">未读 {{ unreadCount }}</span>
          <span class="mobile-chip">列表 {{ notifications.length }}</span>
          <span class="mobile-chip">当前筛选 {{ typeLabel }}</span>
        </div>
      </section>

      <div class="mobile-stack">
        <section class="mobile-card notifications-toolbar-card">
          <ion-searchbar v-model="searchKeyword" :debounce="300" placeholder="搜索标题或内容" />

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

          <label class="notifications-toolbar-card__field">
            <span>消息类型</span>
            <ion-select
              :value="selectedType"
              cancel-text="取消"
              fill="outline"
              interface="action-sheet"
              @ionChange="handleTypeChange"
            >
              <ion-select-option value="">全部系统通知</ion-select-option>
              <ion-select-option value="SYSTEM">系统通知</ion-select-option>
              <ion-select-option value="CLEANING">保洁通知</ion-select-option>
              <ion-select-option value="TASK">任务待分配</ion-select-option>
            </ion-select>
          </label>

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
              <p class="mobile-note">长按不依赖桌面表格，操作收敛到卡片按钮。</p>
            </div>
            <ion-spinner v-if="loading" name="crescent" />
          </div>

          <div v-if="notifications.length > 0" class="mobile-list notifications-list">
            <article v-for="item in notifications" :key="item.id" class="notification-card">
              <div class="notification-card__header">
                <div>
                  <strong>{{ item.title }}</strong>
                  <p>{{ formatTypeLabel(item.notificationType) }} · {{ formatDateTime(item.createdAt) }}</p>
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
            <h3>当前条件下暂无系统通知</h3>
            <p class="mobile-note">可以切换筛选条件或稍后下拉刷新再查看。</p>
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
  IonSelect,
  IonSelectOption,
  IonSpinner,
  IonTitle,
  IonToolbar,
  onIonViewWillEnter,
} from '@ionic/vue'
import { ref, watch } from 'vue'
import {
  deleteNotificationMessage,
  getNotificationMessages,
  markAllNotificationsAsRead,
  markAllNotificationsAsReadByType,
  markNotificationAsRead,
} from '@/api/notification'
import { ROUTE_PATHS } from '@/router/guards'
import { useUserStore } from '@/stores/user'
import type { NotificationMessageDTO, NotificationReadFilter } from '@/types/notification'
import { isHandledRequestError } from '@/utils/request'
import { showSuccessToast, showWarningToast } from '@/utils/notify'

const SYSTEM_TYPES = ['SYSTEM', 'CLEANING', 'TASK']

const userStore = useUserStore()

const loading = ref(false)
const loadNotice = ref('')
const searchKeyword = ref('')
const selectedType = ref('')
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

function formatTypeLabel(type: string) {
  if (type === 'SYSTEM') {
    return '系统通知'
  }
  if (type === 'CLEANING') {
    return '保洁通知'
  }
  if (type === 'TASK') {
    return '任务待分配'
  }
  return type || '系统通知'
}

const typeLabel = ref('全部系统通知')

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

function applyLocalFilter(items: NotificationMessageDTO[]) {
  const nextItems: NotificationMessageDTO[] = []
  const keyword = searchKeyword.value.trim().toLowerCase()

  for (const item of items) {
    if (!SYSTEM_TYPES.includes(item.notificationType)) {
      continue
    }

    if (selectedType.value && item.notificationType !== selectedType.value) {
      continue
    }

    if (activeFilter.value === 'unread' && item.isRead) {
      continue
    }

    if (activeFilter.value === 'read' && !item.isRead) {
      continue
    }

    if (keyword) {
      const title = item.title.toLowerCase()
      const content = item.content.toLowerCase()
      if (!title.includes(keyword) && !content.includes(keyword)) {
        continue
      }
    }

    nextItems.push(item)
  }

  notifications.value = nextItems
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

    applyLocalFilter(response.data.content || [])

    let nextUnreadCount = 0
    for (const item of response.data.content || []) {
      if (SYSTEM_TYPES.includes(item.notificationType) && !item.isRead) {
        nextUnreadCount += 1
      }
    }
    unreadCount.value = nextUnreadCount

    if (!selectedType.value) {
      typeLabel.value = '全部系统通知'
    } else {
      typeLabel.value = formatTypeLabel(selectedType.value)
    }
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

async function handleMarkAllAsRead() {
  const userId = await ensureUserId()
  try {
    if (selectedType.value) {
      const response = await markAllNotificationsAsReadByType(userId, selectedType.value)
      if (!response.success) {
        throw new Error(response.message || '批量已读失败')
      }
    } else {
      const response = await markAllNotificationsAsRead(userId)
      if (!response.success) {
        throw new Error(response.message || '批量已读失败')
      }
    }

    showSuccessToast('未读通知已全部标记')
    await loadPage()
  } catch (error) {
    if (!isHandledRequestError(error)) {
      showWarningToast(resolveWarningMessage(error, '批量已读失败'))
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

async function handleTypeChange(event: CustomEvent) {
  selectedType.value = event.detail.value || ''
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

.notifications-toolbar-card__field {
  display: grid;
  gap: 8px;
}

.notifications-toolbar-card__field span {
  color: var(--app-heading);
  font-size: 13px;
  font-weight: 600;
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
