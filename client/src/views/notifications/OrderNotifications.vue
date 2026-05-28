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
        <el-button v-if="unreadCount > 0" type="primary" @click="handleMarkAllAsRead">
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
        <el-table-column prop="title" :label="t('pages.notifications.order.columns.title')" min-width="180" />
        <el-table-column prop="content" :label="t('pages.notifications.order.columns.content')" min-width="480">
          <template #default="{ row }">
            <span class="content-text">{{ row.content }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" :label="t('pages.notifications.order.columns.time')" width="190">
          <template #default="{ row }">
            {{ formatDateTime(row.createdAt) }}
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
import {
  getNotificationMessagesByType,
  getUnreadNotificationCountByType,
  markAllNotificationsAsReadByType,
  type NotificationMessageDTO,
  type PageResponse,
} from '@/api/notification'
import { formatStoreDateTime, resolveStoreTimeZone } from '@/utils/storeDateTime'

const userStore = useUserStore()
const storeStore = useStoreStore()
const { t } = useI18n()
const currentStoreTimeZone = computed(() => resolveStoreTimeZone(storeStore.currentStore?.timezone))

const activeTab = ref('all')
const searchQuery = ref('')

const loading = ref(false)
const notifications = ref<NotificationMessageDTO[]>([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(25)
const unreadCount = ref(0)

const ORDER_NOTIFICATION_TYPE = 'ORDER'

const getNotificationTypeLabel = (type: string): string => {
  if (type === ORDER_NOTIFICATION_TYPE) {
    return t('pages.notifications.order.typeLabel')
  }
  return type
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
      searchQuery.value
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
      ORDER_NOTIFICATION_TYPE
    )
    if (response.success) {
      unreadCount.value = response.data as number
    }
  } catch (error) {
    console.error('加载未读数量失败:', error)
  }
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

const handleMarkAllAsRead = async () => {
  if (!userStore.currentUser?.id) return

  try {
    await ElMessageBox.confirm(t('pages.notifications.order.markAllConfirm'), t('pages.notifications.common.confirmTitle'), {
      confirmButtonText: t('stage6.common.actions.confirm'),
      cancelButtonText: t('stage6.common.actions.cancel'),
      type: 'warning',
    })

    const response = await markAllNotificationsAsReadByType(
      userStore.currentUser.id,
      ORDER_NOTIFICATION_TYPE
    )

    if (response.success) {
      ElMessage.success(t('pages.notifications.common.markedCount', { count: response.data }))
      await loadNotifications()
      await loadUnreadCount()
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('一键已读失败:', error)
      ElMessage.error(t('pages.notifications.order.markAllFailed'))
    }
  }
}

onMounted(() => {
  loadNotifications()
  loadUnreadCount()
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
