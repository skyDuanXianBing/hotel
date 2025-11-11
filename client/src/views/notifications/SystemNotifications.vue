<template>
  <div class="notifications-page">
    <div class="page-header">
      <h2>系统通知</h2>
    </div>

    <div class="notifications-content">
      <!-- 标签页 -->
      <el-tabs v-model="activeTab" @tab-change="handleTabChange">
        <el-tab-pane label="全部" name="all" />
        <el-tab-pane name="unread">
          <template #label>
            <span class="tab-label">
              未读
              <el-badge v-if="unreadCount > 0" :value="unreadCount" class="unread-badge" />
            </span>
          </template>
        </el-tab-pane>
        <el-tab-pane label="已读" name="read" />
      </el-tabs>

      <!-- 搜索和筛选栏 -->
      <div class="filter-bar">
        <el-input
          v-model="searchQuery"
          placeholder="搜索消息标题、消息内容"
          :prefix-icon="Search"
          clearable
          class="search-input"
          @input="handleSearch"
        />
        <el-select
          v-model="selectedType"
          placeholder="消息类型"
          clearable
          class="type-select"
          @change="handleTypeChange"
        >
          <el-option label="全部" value="" />
          <el-option label="系统通知" value="SYSTEM" />
          <el-option label="保洁通知" value="CLEANING" />
          <el-option label="任务待分配" value="TASK" />
        </el-select>
        <el-button
          v-if="unreadCount > 0"
          type="primary"
          @click="handleMarkAllAsRead"
        >
          一键已读
        </el-button>
      </div>

      <!-- 通知表格 -->
      <el-table
        v-loading="loading"
        :data="notifications"
        stripe
        class="notifications-table"
      >
        <el-table-column prop="notificationType" label="消息类型" width="120">
          <template #default="{ row }">
            <el-tag :type="getNotificationTypeTag(row.notificationType)" size="small">
              {{ getNotificationTypeLabel(row.notificationType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="title" label="消息标题" min-width="200">
          <template #default="{ row }">
            <div class="title-cell">
              <span v-if="!row.isRead" class="unread-dot"></span>
              <span :class="{ 'unread-text': !row.isRead }">{{ row.title }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="content" label="消息内容" min-width="300">
          <template #default="{ row }">
            <span class="content-text">{{ row.content }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="时间" width="180">
          <template #default="{ row }">
            {{ formatDateTime(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="!row.isRead"
              type="primary"
              link
              size="small"
              @click="handleMarkAsRead(row.id)"
            >
              标为已读
            </el-button>
            <el-button
              type="danger"
              link
              size="small"
              @click="handleDelete(row.id)"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
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
import { useUserStore } from '@/stores/user'
import {
  getNotificationMessages,
  getNotificationMessagesByType,
  getUnreadNotificationCount,
  markNotificationAsRead,
  markAllNotificationsAsRead,
  markAllNotificationsAsReadByType,
  deleteNotificationMessage,
  type NotificationMessageDTO,
  type PageResponse,
} from '@/api/notification'

const userStore = useUserStore()

// 标签页状态
const activeTab = ref('all')
const searchQuery = ref('')
const selectedType = ref('')

// 表格数据
const loading = ref(false)
const notifications = ref<NotificationMessageDTO[]>([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(25)
const unreadCount = ref(0)

// 通知类型标签映射
const notificationTypeMap: Record<string, { label: string; tag: string }> = {
  SYSTEM: { label: '系统通知', tag: 'primary' },
  CLEANING: { label: '保洁通知', tag: 'success' },
  TASK: { label: '任务待分配', tag: 'warning' },
}

/**
 * 获取通知类型标签
 */
const getNotificationTypeTag = (type: string): string => {
  return notificationTypeMap[type]?.tag || 'info'
}

/**
 * 获取通知类型标签文本
 */
const getNotificationTypeLabel = (type: string): string => {
  return notificationTypeMap[type]?.label || type
}

/**
 * 格式化日期时间
 */
const formatDateTime = (dateStr: string): string => {
  const date = new Date(dateStr)
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hours = String(date.getHours()).padStart(2, '0')
  const minutes = String(date.getMinutes()).padStart(2, '0')
  return `${year}-${month}-${day} ${hours}:${minutes}`
}

/**
 * 加载通知列表
 */
const loadNotifications = async () => {
  if (!userStore.currentUser?.id) return

  try {
    loading.value = true
    const userId = userStore.currentUser.id

    let response
    if (selectedType.value) {
      response = await getNotificationMessagesByType(
        userId,
        selectedType.value,
        currentPage.value - 1,
        pageSize.value
      )
    } else {
      response = await getNotificationMessages(userId, currentPage.value - 1, pageSize.value)
    }

    if (response.success) {
      const pageData = response.data as PageResponse<NotificationMessageDTO>

      // 根据标签页过滤数据
      let filteredData = pageData.content
      if (activeTab.value === 'unread') {
        filteredData = filteredData.filter((n) => !n.isRead)
      } else if (activeTab.value === 'read') {
        filteredData = filteredData.filter((n) => n.isRead)
      }

      // 根据搜索关键词过滤
      if (searchQuery.value.trim()) {
        const query = searchQuery.value.toLowerCase()
        filteredData = filteredData.filter(
          (n) =>
            n.title.toLowerCase().includes(query) || n.content.toLowerCase().includes(query)
        )
      }

      notifications.value = filteredData
      total.value = pageData.totalElements
    }
  } catch (error) {
    console.error('加载通知失败:', error)
    ElMessage.error('加载通知失败')
  } finally {
    loading.value = false
  }
}

/**
 * 加载未读数量
 */
const loadUnreadCount = async () => {
  if (!userStore.currentUser?.id) return

  try {
    const response = await getUnreadNotificationCount(userStore.currentUser.id)
    if (response.success) {
      unreadCount.value = response.data as number
    }
  } catch (error) {
    console.error('加载未读数量失败:', error)
  }
}

/**
 * 标签页切换
 */
const handleTabChange = () => {
  currentPage.value = 1
  loadNotifications()
}

/**
 * 搜索
 */
const handleSearch = () => {
  currentPage.value = 1
  loadNotifications()
}

/**
 * 类型筛选变化
 */
const handleTypeChange = () => {
  currentPage.value = 1
  loadNotifications()
}

/**
 * 分页大小变化
 */
const handlePageSizeChange = () => {
  currentPage.value = 1
  loadNotifications()
}

/**
 * 页码变化
 */
const handlePageChange = () => {
  loadNotifications()
}

/**
 * 标记为已读
 */
const handleMarkAsRead = async (id: number) => {
  try {
    const response = await markNotificationAsRead(id)
    if (response.success) {
      ElMessage.success('已标记为已读')
      await loadNotifications()
      await loadUnreadCount()
    }
  } catch (error) {
    console.error('标记已读失败:', error)
    ElMessage.error('标记已读失败')
  }
}

/**
 * 一键已读
 */
const handleMarkAllAsRead = async () => {
  if (!userStore.currentUser?.id) return

  try {
    await ElMessageBox.confirm('确定要将所有未读消息标记为已读吗?', '确认操作', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    })

    let response
    if (selectedType.value) {
      response = await markAllNotificationsAsReadByType(userStore.currentUser.id, selectedType.value)
    } else {
      response = await markAllNotificationsAsRead(userStore.currentUser.id)
    }

    if (response.success) {
      ElMessage.success(`已标记 ${response.data} 条消息为已读`)
      await loadNotifications()
      await loadUnreadCount()
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('一键已读失败:', error)
      ElMessage.error('一键已读失败')
    }
  }
}

/**
 * 删除通知
 */
const handleDelete = async (id: number) => {
  try {
    await ElMessageBox.confirm('确定要删除这条通知吗?', '确认删除', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    })

    const response = await deleteNotificationMessage(id)
    if (response.success) {
      ElMessage.success('删除成功')
      await loadNotifications()
      await loadUnreadCount()
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除失败:', error)
      ElMessage.error('删除失败')
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
  padding: 20px;
  background: #fff;
  min-height: calc(100vh - 100px);
}

.page-header {
  margin-bottom: 20px;
}

.page-header h2 {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
  color: #303133;
}

.notifications-content {
  background: #fff;
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
  gap: 12px;
  margin-bottom: 20px;
  align-items: center;
}

.search-input {
  width: 300px;
}

.type-select {
  width: 150px;
}

.notifications-table {
  margin-bottom: 20px;
}

.title-cell {
  display: flex;
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

.unread-text {
  font-weight: 600;
  color: #303133;
}

.content-text {
  color: #606266;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  text-overflow: ellipsis;
}

.pagination-container {
  display: flex;
  justify-content: flex-end;
  padding: 20px 0;
}

:deep(.el-tabs__nav-wrap::after) {
  display: none;
}

:deep(.el-tabs__header) {
  margin-bottom: 24px;
}

:deep(.el-tabs__item) {
  font-size: 14px;
  padding: 0 20px;
}

:deep(.el-tabs__item.is-active) {
  color: #409eff;
}

:deep(.el-tabs__active-bar) {
  background-color: #409eff;
}
</style>
