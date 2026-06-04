<template>
  <div class="notifications-page">
    <div class="notifications-content">
      <el-tabs v-model="activeTab" @tab-change="handleTabChange">
        <el-tab-pane :label="t('pages.notifications.common.tabs.all')" name="all" />
        <el-tab-pane name="unread">
          <template #label>
            <span class="tab-label">
              {{ t('pages.notifications.common.tabs.unread') }}
              <el-badge
                v-if="unreadCount > 0"
                :value="unreadCount"
                :max="99"
                class="unread-badge"
              />
            </span>
          </template>
        </el-tab-pane>
        <el-tab-pane :label="t('pages.notifications.common.tabs.read')" name="read" />
      </el-tabs>

      <div class="filter-bar">
        <el-input
          v-model="searchQuery"
          :placeholder="t('pages.notifications.order.searchPlaceholder')"
          :suffix-icon="Search"
          clearable
          class="search-input"
          @input="handleSearch"
        />
        <el-button
          type="primary"
          :disabled="!hasUnreadNotifications"
          @click="handleMarkAllAsRead"
        >
          {{ t('pages.notifications.order.markAllAsRead') }}
        </el-button>
      </div>

      <el-table v-loading="loading" :data="notifications" class="notifications-table">
        <el-table-column
          prop="notificationType"
          :label="t('pages.notifications.order.columns.type')"
          width="140"
        >
          <template #default="{ row }">
            <div class="type-cell">
              <span v-if="!row.isRead" class="unread-dot"></span>
              <span>{{ getNotificationTypeLabel(row.notificationType) }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column
          prop="title"
          :label="t('pages.notifications.order.columns.title')"
          min-width="180"
        >
          <template #default="{ row }">
            {{ formatOrderNotificationTitle(row.title) }}
          </template>
        </el-table-column>
        <el-table-column
          prop="content"
          :label="t('pages.notifications.order.columns.content')"
          min-width="480"
        >
          <template #default="{ row }">
            <span class="content-text">{{ formatOrderNotificationContent(row.content) }}</span>
          </template>
        </el-table-column>
        <el-table-column
          prop="createdAt"
          :label="t('pages.notifications.order.columns.time')"
          width="190"
        >
          <template #default="{ row }">
            {{ formatDateTime(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column
          :label="t('pages.notifications.system.columns.actions')"
          width="150"
          fixed="right"
        >
          <template #default="{ row }">
            <el-button
              v-if="!row.isRead"
              type="primary"
              link
              size="small"
              @click="handleMarkAsRead(row.id)"
            >
              {{ t('pages.notifications.system.actions.markAsRead') }}
            </el-button>
            <el-button type="danger" link size="small" @click="handleDelete(row.id)">
              {{ t('pages.notifications.system.actions.delete') }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-container">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :total="total"
          :page-sizes="[10, 25, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handlePageSizeChange"
          @current-change="handlePageChange"
        />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { Search } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useI18n } from 'vue-i18n'
import { useStoreStore } from '@/stores/store'
import { useUserStore } from '@/stores/user'
import { useNotificationCenterStore } from '@/stores/notificationCenter'
import {
  getNotificationMessagesByType,
  getUnreadNotificationCountByType,
  markNotificationAsRead,
  markAllNotificationsAsReadByType,
  deleteNotificationMessage,
  type NotificationMessageDTO,
  type PageResponse,
} from '@/api/notification'
import { formatStoreDateTime, resolveStoreTimeZone } from '@/utils/storeDateTime'

const userStore = useUserStore()
const storeStore = useStoreStore()
const notificationCenterStore = useNotificationCenterStore()
const { t, locale } = useI18n()
const currentStoreTimeZone = computed(() => resolveStoreTimeZone(storeStore.currentStore?.timezone))

const activeTab = ref('all')
const searchQuery = ref('')

const loading = ref(false)
const notifications = ref<NotificationMessageDTO[]>([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(25)
const unreadCount = ref(0)
const hasUnreadNotifications = computed(() => {
  if (unreadCount.value > 0) {
    return true
  }
  return notifications.value.some((notification) => !notification.isRead)
})

const ORDER_NOTIFICATION_TYPE = 'ORDER'
const UNKNOWN_NOTIFICATION_TYPE_LABEL = '-'
const notificationTypeLabelMap: Record<string, string> = {
  [ORDER_NOTIFICATION_TYPE]: 'pages.notifications.order.typeLabel',
}

const ORDER_CREATED_TITLE = '订单创建'
const ORDER_UPDATED_TITLE = '订单修改'
const ORDER_CANCELLED_TITLE = '订单取消'
const CHINESE_TEXT_PATTERN = /[\u3400-\u9fff]/

const hasChineseText = (value: string): boolean => CHINESE_TEXT_PATTERN.test(value)

const getUnknownChannelLabel = (): string => {
  const currentLocale = String(locale.value)
  if (currentLocale === 'ja') {
    return '不明なチャネル'
  }
  if (currentLocale === 'zh-TW') {
    return '未知渠道'
  }
  if (currentLocale === 'zh-CN') {
    return '未知渠道'
  }
  return 'Unknown channel'
}

const getUnknownGuestLabel = (): string => {
  const currentLocale = String(locale.value)
  if (currentLocale === 'ja') {
    return '不明な宿泊者'
  }
  if (currentLocale === 'zh-TW') {
    return '未知客人'
  }
  if (currentLocale === 'zh-CN') {
    return '未知客人'
  }
  return 'Unknown guest'
}

const localizeOrderValue = (value: string): string => {
  const trimmed = value.trim()
  if (trimmed === '未知渠道') {
    return getUnknownChannelLabel()
  }
  if (trimmed === '未知客人') {
    return getUnknownGuestLabel()
  }
  return trimmed
}

const formatOrderNotificationTitle = (title?: string | null): string => {
  const rawTitle = String(title || '').trim()
  if (!rawTitle) {
    return t('pages.notifications.order.typeLabel')
  }

  const currentLocale = String(locale.value)
  if (currentLocale === 'zh-CN') {
    return rawTitle
  }

  if (rawTitle === ORDER_CREATED_TITLE) {
    if (currentLocale === 'ja') return '予約作成'
    if (currentLocale === 'zh-TW') return '訂單建立'
    return 'Order Created'
  }
  if (rawTitle === ORDER_UPDATED_TITLE) {
    if (currentLocale === 'ja') return '予約変更'
    if (currentLocale === 'zh-TW') return '訂單修改'
    return 'Order Updated'
  }
  if (rawTitle === ORDER_CANCELLED_TITLE) {
    if (currentLocale === 'ja') return '予約キャンセル'
    if (currentLocale === 'zh-TW') return '訂單取消'
    return 'Order Cancelled'
  }

  if (hasChineseText(rawTitle)) {
    return t('pages.notifications.order.typeLabel')
  }
  return rawTitle
}

const formatKnownOrderContent = (
  eventType: 'created' | 'updated' | 'cancelled',
  channel: string,
  guestName: string,
  channelOrderNumber: string,
): string => {
  const localizedChannel = localizeOrderValue(channel)
  const localizedGuestName = localizeOrderValue(guestName)
  const localizedChannelOrderNumber = channelOrderNumber.trim() || '-'
  const currentLocale = String(locale.value)

  if (currentLocale === 'zh-CN') {
    if (eventType === 'created') {
      return `${localizedChannel}发来了一个新订单，客人姓名：${localizedGuestName}，渠道订单号: ${localizedChannelOrderNumber}`
    }
    if (eventType === 'updated') {
      return `${localizedChannel}修改了${localizedGuestName}的订单，渠道订单号: ${localizedChannelOrderNumber}`
    }
    return `${localizedChannel}取消了${localizedGuestName}的订单，渠道订单号: ${localizedChannelOrderNumber}`
  }

  if (currentLocale === 'zh-TW') {
    if (eventType === 'created') {
      return `${localizedChannel}送來了一筆新訂單，客人姓名：${localizedGuestName}，渠道訂單號：${localizedChannelOrderNumber}`
    }
    if (eventType === 'updated') {
      return `${localizedChannel}修改了${localizedGuestName}的訂單，渠道訂單號：${localizedChannelOrderNumber}`
    }
    return `${localizedChannel}取消了${localizedGuestName}的訂單，渠道訂單號：${localizedChannelOrderNumber}`
  }

  if (currentLocale === 'ja') {
    if (eventType === 'created') {
      return `${localizedChannel}から新規予約が届きました。宿泊者: ${localizedGuestName}、チャネル予約番号: ${localizedChannelOrderNumber}`
    }
    if (eventType === 'updated') {
      return `${localizedChannel}が${localizedGuestName}の予約を変更しました。チャネル予約番号: ${localizedChannelOrderNumber}`
    }
    return `${localizedChannel}が${localizedGuestName}の予約をキャンセルしました。チャネル予約番号: ${localizedChannelOrderNumber}`
  }

  if (eventType === 'created') {
    return `${localizedChannel} sent a new order. Guest: ${localizedGuestName}; Channel order no.: ${localizedChannelOrderNumber}`
  }
  if (eventType === 'updated') {
    return `${localizedChannel} updated ${localizedGuestName}'s order. Channel order no.: ${localizedChannelOrderNumber}`
  }
  return `${localizedChannel} cancelled ${localizedGuestName}'s order. Channel order no.: ${localizedChannelOrderNumber}`
}

const formatOrderNotificationContent = (content?: string | null): string => {
  const rawContent = String(content || '').trim()
  if (!rawContent) {
    return '-'
  }

  const createdMatch = rawContent.match(/^(.+)发来了一个新订单，客人姓名：(.+)，渠道订单号:\s*(.+)$/)
  if (createdMatch) {
    return formatKnownOrderContent('created', createdMatch[1], createdMatch[2], createdMatch[3])
  }

  const updatedMatch = rawContent.match(/^(.+)修改了(.+)的订单，渠道订单号:\s*(.+)$/)
  if (updatedMatch) {
    return formatKnownOrderContent('updated', updatedMatch[1], updatedMatch[2], updatedMatch[3])
  }

  const cancelledMatch = rawContent.match(/^(.+)取消了(.+)的订单，渠道订单号:\s*(.+)$/)
  if (cancelledMatch) {
    return formatKnownOrderContent('cancelled', cancelledMatch[1], cancelledMatch[2], cancelledMatch[3])
  }

  if (String(locale.value) !== 'zh-CN' && hasChineseText(rawContent)) {
    return t('pages.notifications.order.typeLabel')
  }
  return rawContent
}

const getNotificationTypeLabel = (type: string): string => {
  const labelKey = notificationTypeLabelMap[type]
  if (!labelKey) {
    return UNKNOWN_NOTIFICATION_TYPE_LABEL
  }
  return t(labelKey)
}

const formatDateTime = (dateStr: string): string => {
  return formatStoreDateTime(dateStr, currentStoreTimeZone.value, true)
}

const getTabReadFilter = (): boolean | undefined => {
  if (activeTab.value === 'unread') {
    return false
  }
  if (activeTab.value === 'read') {
    return true
  }
  return undefined
}

const loadNotifications = async () => {
  if (!userStore.currentUser?.id) return

  try {
    loading.value = true
    const userId = userStore.currentUser.id
    const response = await getNotificationMessagesByType(
      userId,
      ORDER_NOTIFICATION_TYPE,
      currentPage.value - 1,
      pageSize.value,
      getTabReadFilter(),
      searchQuery.value,
    )

    if (response.success) {
      const pageData = response.data as PageResponse<NotificationMessageDTO>
      notifications.value = pageData.content
      total.value = pageData.totalElements
    }
  } catch (error) {
    console.error('加载订单通知失败:', error)
    ElMessage.error(t('pages.notifications.order.loadFailed'))
  } finally {
    loading.value = false
  }
}

const loadUnreadCount = async () => {
  if (!userStore.currentUser?.id) return

  try {
    const response = await getUnreadNotificationCountByType(
      userStore.currentUser.id,
      ORDER_NOTIFICATION_TYPE,
    )
    if (response.success) {
      unreadCount.value = response.data as number
    }
  } catch (error) {
    console.error('加载未读数量失败:', error)
  }
}

const refreshNotificationState = async () => {
  await Promise.all([
    loadNotifications(),
    loadUnreadCount(),
    notificationCenterStore.refreshOrderUnreadCount(),
  ])
}

const handleTabChange = () => {
  currentPage.value = 1
  loadNotifications()
}

const handleSearch = () => {
  currentPage.value = 1
  loadNotifications()
}

const handlePageSizeChange = () => {
  currentPage.value = 1
  loadNotifications()
}

const handlePageChange = () => {
  loadNotifications()
}

const handleMarkAsRead = async (id: number) => {
  try {
    const response = await markNotificationAsRead(id)
    if (response.success) {
      ElMessage.success(t('pages.notifications.system.markReadSuccess'))
      await refreshNotificationState()
    }
  } catch (error) {
    console.error('标记已读失败:', error)
    ElMessage.error(t('pages.notifications.system.markReadFailed'))
  }
}

const handleMarkAllAsRead = async () => {
  if (!userStore.currentUser?.id) return

  try {
    await ElMessageBox.confirm(
      t('pages.notifications.order.markAllConfirm'),
      t('pages.notifications.common.confirmTitle'),
      {
        confirmButtonText: t('stage6.common.actions.confirm'),
        cancelButtonText: t('stage6.common.actions.cancel'),
        type: 'warning',
      },
    )

    const response = await markAllNotificationsAsReadByType(
      userStore.currentUser.id,
      ORDER_NOTIFICATION_TYPE,
    )

    if (response.success) {
      ElMessage.success(t('pages.notifications.common.markedCount', { count: response.data }))
      await refreshNotificationState()
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('一键已读失败:', error)
      ElMessage.error(t('pages.notifications.order.markAllFailed'))
    }
  }
}

const handleDelete = async (id: number) => {
  try {
    await ElMessageBox.confirm(
      t('pages.notifications.system.deleteConfirm'),
      t('pages.notifications.common.deleteConfirmTitle'),
      {
        confirmButtonText: t('stage6.common.actions.confirm'),
        cancelButtonText: t('stage6.common.actions.cancel'),
        type: 'warning',
      },
    )

    const response = await deleteNotificationMessage(id)
    if (response.success) {
      ElMessage.success(t('pages.notifications.system.deleteSuccess'))
      await refreshNotificationState()
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除失败:', error)
      ElMessage.error(t('pages.notifications.system.deleteFailed'))
    }
  }
}

onMounted(() => {
  refreshNotificationState()
})
</script>

<style scoped>
.notifications-page {
  min-height: calc(100vh - 100px);
  background: #f5f7fa;
  padding: 20px;
}

.notifications-content {
  background: #fff;
  padding: 20px;
}

.tab-label {
  display: flex;
  align-items: center;
  gap: 8px;
}

.unread-badge :deep(.el-badge__content) {
  transform: translateY(0);
}

.filter-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 20px;
}

.search-input {
  width: 420px;
}

.notifications-table {
  margin-bottom: 20px;
}

.type-cell {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.unread-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #ff4d4f;
  flex-shrink: 0;
}

.content-text {
  color: #303133;
}

.pagination-container {
  display: flex;
  justify-content: flex-end;
  padding-top: 8px;
}

:deep(.el-table th.el-table__cell) {
  background: #f0f2f5;
  color: #303133;
}

:deep(.el-tabs__nav-wrap::after) {
  display: none;
}

:deep(.el-tabs__header) {
  margin-bottom: 20px;
}

:deep(.el-tabs__item) {
  font-size: 14px;
  padding: 0 20px;
}
</style>
