<template>
  <div class="cleaning-task-list">
    <CleaningTabs />
    <!-- 筛选条件 -->
    <el-card class="filter-card" shadow="never">
      <div class="filter-section">
        <div class="filter-row">
          <el-input
            v-model="filters.search"
            :placeholder="t('accommodation.cleaning.searchCleanerOrEmail')"
            clearable
            class="search-input"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>

          <span class="filter-label">{{ t('accommodation.common.taskType') }}</span>
          <el-select
            v-model="filters.taskType"
            :placeholder="t('accommodation.common.select')"
            clearable
            class="filter-select"
          >
            <el-option :label="t('accommodation.common.all')" value="" />
            <el-option :label="t('accommodation.cleaning.checkout')" value="checkout" />
            <el-option :label="t('accommodation.cleaning.daily')" value="daily" />
            <el-option :label="t('accommodation.cleaning.deep')" value="deep" />
          </el-select>

          <span class="filter-label">{{ t('stage5.cleaner.dashboard.taskStatus') }}</span>
          <el-select
            v-model="filters.status"
            :placeholder="t('accommodation.common.select')"
            clearable
            class="filter-select"
          >
            <el-option :label="t('accommodation.common.all')" value="" />
            <el-option
              :label="t('accommodation.cleaningTaskList.statusLabels.expired')"
              value="expired"
            />
            <el-option
              :label="t('accommodation.cleaningTaskList.statusLabels.pending')"
              value="pending"
            />
            <el-option
              :label="t('accommodation.cleaningTaskList.statusLabels.assigned')"
              value="assigned"
            />
            <el-option
              :label="t('accommodation.cleaningTaskList.statusLabels.in_progress')"
              value="in_progress"
            />
            <el-option
              :label="t('accommodation.cleaningTaskList.statusLabels.completed')"
              value="completed"
            />
          </el-select>

          <span class="filter-label">{{ t('accommodation.common.roomType') }}</span>
          <el-select
            v-model="filters.roomTypeId"
            :placeholder="t('accommodation.common.select')"
            clearable
            class="filter-select"
          >
            <el-option :label="t('accommodation.common.all')" :value="undefined" />
            <el-option
              v-for="roomType in roomTypeList"
              :key="roomType.id"
              :label="roomType.name"
              :value="roomType.id"
            />
          </el-select>
        </div>

        <div class="filter-row">
          <span class="filter-label filter-label--date">{{
            t('accommodation.cleaningTaskList.dateFilter')
          }}</span>
          <el-date-picker
            v-model="filterDateRange"
            type="daterange"
            :range-separator="t('accommodation.common.rangeTo')"
            :start-placeholder="t('accommodation.common.startDate')"
            :end-placeholder="t('accommodation.common.endDate')"
            format="YYYY/MM/DD"
            value-format="YYYY-MM-DD"
            :clearable="false"
            class="date-range-picker"
          />

          <div class="filter-actions">
            <el-button class="export-button" type="primary" @click="handleExport">{{
              t('accommodation.common.exportDetails')
            }}</el-button>
          </div>
        </div>
      </div>
    </el-card>

    <!-- 任务列表表格 -->
    <el-card class="table-card" shadow="never">
      <el-table
        :data="taskList"
        border
        class="task-list-table"
        style="width: 100%"
        :row-class-name="getRowClassName"
        v-loading="loading"
      >
        <el-table-column
          prop="taskDate"
          :label="t('accommodation.cleaning.taskDate')"
          min-width="130"
        />
        <el-table-column
          prop="roomType"
          :label="t('accommodation.cleaning.roomType')"
          min-width="132"
        />
        <el-table-column
          prop="roomNumber"
          :label="t('accommodation.cleaning.roomNumber')"
          min-width="110"
        />
        <el-table-column prop="taskStatus" :label="t('accommodation.common.status')" min-width="130">
          <template #default="{ row }">
            <span class="task-status-text" :class="getStatusTextClass(row.taskStatus)">
              {{ getStatusText(row.taskStatus) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="taskType" :label="t('accommodation.common.taskType')" min-width="130">
          <template #default="{ row }">
            {{ getTaskTypeText(row.taskType) }}
          </template>
        </el-table-column>
        <el-table-column prop="taskTime" :label="t('accommodation.common.taskTime')" min-width="180" />
        <el-table-column prop="cleaner" :label="t('accommodation.common.cleaner')" min-width="130">
          <template #default="{ row }">
            <span>{{ row.cleaner || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="approver" :label="t('accommodation.common.approver')" min-width="130">
          <template #default="{ row }">
            <span>{{ row.approver || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column :label="t('accommodation.common.actions')" min-width="110">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="handleViewDetail(row)">
              {{ t('accommodation.cleaning.detail') }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-container">
        <div class="pagination-info">
          {{ t('accommodation.common.totalCount', { count: pagination.total }) }}
        </div>
        <el-pagination
          v-model:current-page="pagination.current"
          v-model:page-size="pagination.size"
          :total="pagination.total"
          :page-sizes="[10, 20, 50, 100]"
          layout="sizes, prev, pager, next"
          @current-change="handlePageChange"
          @size-change="handleSizeChange"
        />
      </div>
    </el-card>

    <!-- 任务详情对话框 -->
    <el-dialog
      v-model="detailDialogVisible"
      :title="t('accommodation.cleaning.taskDetail')"
      width="760px"
      class="task-detail-dialog"
    >
      <div v-if="taskDetail" class="task-detail-content">
        <div class="task-detail-grid">
          <div class="detail-label">{{ t('accommodation.cleaning.taskDate') }}</div>
          <div class="detail-value">{{ taskDetail.taskDate }}</div>
          <div class="detail-label">{{ t('accommodation.cleaning.roomType') }}</div>
          <div class="detail-value">{{ taskDetail.roomType }}</div>

          <div class="detail-label">{{ t('accommodation.cleaning.roomNumber') }}</div>
          <div class="detail-value">{{ taskDetail.roomNumber }}</div>
          <div class="detail-label">{{ t('accommodation.common.status') }}</div>
          <div class="detail-value">
            <span class="task-status-text" :class="getStatusTextClass(taskDetail.taskStatus)">
              {{ getStatusText(taskDetail.taskStatus) }}
            </span>
          </div>

          <div class="detail-label">{{ t('accommodation.common.taskType') }}</div>
          <div class="detail-value">{{ getTaskTypeText(taskDetail.taskType) }}</div>
          <div class="detail-label">{{ t('accommodation.common.taskTime') }}</div>
          <div class="detail-value">{{ taskDetail.taskTime }}</div>

          <div class="detail-label">{{ t('accommodation.common.cleaner') }}</div>
          <div class="detail-value">{{ taskDetail.cleaner || '' }}</div>
          <div class="detail-label">{{ t('accommodation.common.approver') }}</div>
          <div class="detail-value">{{ taskDetail.approver || '' }}</div>

          <div class="detail-label">{{ t('accommodation.cleaning.createTime') }}</div>
          <div class="detail-value detail-value--wide">{{ taskDetail.createTime }}</div>

          <div class="detail-label">{{ t('accommodation.cleaning.completeTime') }}</div>
          <div class="detail-value detail-value--wide">{{ taskDetail.completeTime || '-' }}</div>

          <div class="detail-label">{{ t('accommodation.common.notes') }}</div>
          <div class="detail-value detail-value--wide detail-value--notes">
            {{ taskDetail.remark || '' }}
          </div>
        </div>
      </div>
      <el-form
        v-if="taskDetail && taskDetail.taskStatus === 'pending'"
        class="assign-form"
        label-width="90px"
      >
        <el-form-item :label="t('accommodation.common.cleaner')" required>
          <el-select
            v-model="assignCleanerId"
            :placeholder="t('accommodation.common.select')"
            class="detail-cleaner-select"
            filterable
          >
            <el-option
              v-for="cleaner in cleanerList"
              :key="cleaner.id"
              :label="cleaner.name"
              :value="cleaner.id"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button class="dialog-close-button" @click="detailDialogVisible = false">{{
          t('accommodation.common.close')
        }}</el-button>
        <el-button
          v-if="taskDetail && taskDetail.taskStatus === 'pending'"
          class="dialog-primary-button"
          type="primary"
          @click="handleAssignTask"
        >
          {{ t('accommodation.cleaning.assignTask') }}
        </el-button>
        <el-button
          v-if="
            taskDetail &&
            (taskDetail.taskStatus === 'assigned' || taskDetail.taskStatus === 'in_progress')
          "
          class="dialog-primary-button"
          type="success"
          @click="handleCompleteTask"
        >
          {{ t('accommodation.cleaningTaskList.completeTask') }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, onMounted, watch } from 'vue'
import { Search } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { useI18n } from 'vue-i18n'
import {
  assignCleaningTask,
  completeCleaningTask,
  getCleaners,
  getCleaningTaskById,
  getCleaningTasks,
  type CleanerDTO,
  type CleaningTaskDTO,
} from '@/api/cleaning'
import { getAllRoomTypes, type RoomTypeDTO } from '@/api/roomType'
import {
  addDaysToYmd,
  formatBackendDateTime,
  formatStoreTime,
  getStoreTodayYmd,
  resolveStoreTimeZoneFromStorage,
} from '@/utils/storeDateTime'
import CleaningTabs from './components/CleaningTabs.vue'
const { t } = useI18n()

// 是否收起筛选
const isCollapsed = ref(false)

// 房型列表
const roomTypeList = ref<RoomTypeDTO[]>([])

const getOffsetDate = (offsetDays: number) => {
  return addDaysToYmd(getStoreTodayYmd(), offsetDays)
}

// 筛选条件（默认显示最近 7 天到今天）
const filters = ref({
  search: '',
  taskType: '',
  status: '',
  roomType: '',
  roomTypeId: undefined as number | undefined,
  startDate: getOffsetDate(-7),
  endDate: getOffsetDate(0),
})

// 任务列表数据
const filterDateRange = computed<string[]>({
  get: () => [filters.value.startDate, filters.value.endDate],
  set: (range: string[]) => {
    if (!range || range.length !== 2) {
      return
    }
    filters.value.startDate = range[0]
    filters.value.endDate = range[1]
  },
})

interface TaskListItem {
  id: number
  taskDate: string
  roomType: string
  roomNumber: string
  taskStatus: string
  taskType: string
  taskTime: string
  cleaner: string
  approver: string
  cleanerId?: number
  approverId?: number
  createTime: string
  completeTime: string
  remark: string
}

const taskList = ref<TaskListItem[]>([])
const loading = ref(false)

// 分页
const pagination = ref({
  current: 1,
  size: 10,
  total: 0,
})

// 详情对话框
const detailDialogVisible = ref(false)
const taskDetail = ref<TaskListItem | null>(null)
const cleanerList = ref<CleanerDTO[]>([])
const assignCleanerId = ref<number | undefined>(undefined)

const currentStoreTimeZone = () => resolveStoreTimeZoneFromStorage()

// 获取状态类型
const getStatusType = (status: string) => {
  const typeMap: Record<string, string> = {
    expired: 'info',
    pending: 'warning',
    assigned: 'primary',
    in_progress: 'primary',
    completed: 'success',
  }
  return typeMap[status] || 'info'
}

// 获取状态文本
const getStatusText = (status: string) => {
  const textMap: Record<string, string> = {
    expired: t('accommodation.cleaningTaskList.statusLabels.expired'),
    pending: t('accommodation.cleaningTaskList.statusLabels.pending'),
    assigned: t('accommodation.cleaningTaskList.statusLabels.assigned'),
    in_progress: t('accommodation.cleaningTaskList.statusLabels.in_progress'),
    completed: t('accommodation.cleaningTaskList.statusLabels.completed'),
  }
  return textMap[status] || status
}

// 获取状态点样式
const getStatusTextClass = (status: string) => {
  return {
    'task-status-text--expired': status === 'expired',
    'task-status-text--pending': status === 'pending',
    'task-status-text--assigned': status === 'assigned',
    'task-status-text--in-progress': status === 'in_progress',
    'task-status-text--completed': status === 'completed',
  }
}

// 获取任务类型文本
const getTaskTypeText = (taskType: string) => {
  const typeMap: Record<string, string> = {
    checkout: t('accommodation.cleaningTaskList.taskTypeLabels.checkout'),
    daily: t('accommodation.cleaningTaskList.taskTypeLabels.daily'),
    deep: t('accommodation.cleaningTaskList.taskTypeLabels.deep'),
    'accommodation.cleaning.checkout': t('accommodation.cleaningTaskList.taskTypeLabels.checkout'),
    'accommodation.cleaning.daily': t('accommodation.cleaningTaskList.taskTypeLabels.daily'),
    'accommodation.cleaning.deep': t('accommodation.cleaningTaskList.taskTypeLabels.deep'),
    'accommodation.cleaning.taskType': t('accommodation.common.taskType'),
  }
  return typeMap[taskType] || taskType
}

// 格式化任务时间
const formatTaskTime = (task: CleaningTaskDTO) => {
  if (task.startTime && task.completeTime) {
    const start = formatStoreTime(task.startTime, currentStoreTimeZone())
    const end = formatStoreTime(task.completeTime, currentStoreTimeZone())
    return `${start}-${end}`
  } else if (task.estimatedTime) {
    return formatStoreTime(task.estimatedTime, currentStoreTimeZone())
  }
  return '-'
}

// 格式化日期时间
const formatDateTime = (datetime?: string) => {
  if (!datetime) return '-'
  return formatBackendDateTime(datetime, currentStoreTimeZone(), true)
}

// 转换API数据到列表项
const convertToListItem = (task: CleaningTaskDTO): TaskListItem => {
  return {
    id: task.id,
    taskDate: task.taskDate,
    roomType: task.roomType,
    roomNumber: task.roomNumber,
    taskStatus: task.status,
    taskType: task.taskType,
    taskTime: formatTaskTime(task),
    cleaner: task.cleanerName || '',
    approver: task.approverName || '',
    cleanerId: task.cleanerId,
    approverId: task.approverId,
    createTime: formatDateTime(task.createdAt),
    completeTime: formatDateTime(task.completeTime),
    remark: task.notes || '',
  }
}

// 加载房型列表
const loadRoomTypes = async () => {
  try {
    const response = await getAllRoomTypes()
    if (response.success && response.data) {
      roomTypeList.value = response.data
    }
  } catch (error: any) {
    console.error('获取房型列表失败:', error)
  }
}

// 加载保洁员列表（用于分配）
const loadCleaners = async () => {
  try {
    const response = await getCleaners()
    if (response.success && response.data) {
      cleanerList.value = response.data
    }
  } catch (error: any) {
    console.error('获取保洁员列表失败:', error)
  }
}

// 加载任务列表
const loadTasks = async () => {
  try {
    loading.value = true
    const normalizedStartDate = filters.value.startDate || getOffsetDate(-7)
    const normalizedEndDate = filters.value.endDate || getOffsetDate(0)
    const response = await getCleaningTasks({
      startDate: normalizedStartDate,
      endDate: normalizedEndDate,
      status: filters.value.status || undefined,
      taskType: filters.value.taskType || undefined,
      roomTypeId: filters.value.roomTypeId,
      search: filters.value.search || undefined,
      page: pagination.value.current - 1,
      size: pagination.value.size,
      sortBy: 'taskDate',
      sortDirection: 'DESC',
    })

    if (response.success && response.data) {
      taskList.value = response.data.content.map(convertToListItem)
      pagination.value.total = response.data.totalElements
    } else {
      ElMessage.error(response.message || t('accommodation.cleaningTaskList.loadTasksFailed'))
    }
  } catch (error: any) {
    console.error('获取任务列表失败:', error)
    ElMessage.error(error.message || t('accommodation.cleaningTaskList.loadTasksFailed'))
  } finally {
    loading.value = false
  }
}

// 收起/展开筛选
const handleCollapse = () => {
  isCollapsed.value = !isCollapsed.value
}

// 导出明细
const getRowClassName = ({ rowIndex }: { rowIndex: number }) => {
  return rowIndex % 2 === 0 ? 'even-row' : 'odd-row'
}

const handleExport = () => {
  if (taskList.value.length === 0) {
    ElMessage.warning(t('accommodation.cleaningTaskList.noExportData'))
    return
  }

  const headers = [
    t('accommodation.cleaning.taskDate'),
    t('accommodation.cleaning.roomType'),
    t('accommodation.cleaning.roomNumber'),
    t('accommodation.common.status'),
    t('accommodation.common.taskType'),
    t('accommodation.common.taskTime'),
    t('accommodation.common.cleaner'),
    t('accommodation.common.approver'),
    t('accommodation.cleaning.createTime'),
    t('accommodation.cleaning.completeTime'),
    t('accommodation.common.notes'),
  ]

  const escapeCsvCell = (value: string) => `"${(value || '').replace(/"/g, '""')}"`
  const rows = taskList.value.map((item) =>
    [
      item.taskDate,
      item.roomType,
      item.roomNumber,
      getStatusText(item.taskStatus),
      getTaskTypeText(item.taskType),
      item.taskTime,
      item.cleaner || '-',
      item.approver || '-',
      item.createTime,
      item.completeTime || '-',
      item.remark || '-',
    ]
      .map((cell) => escapeCsvCell(String(cell)))
      .join(','),
  )

  const csvContent = [headers.join(','), ...rows].join('\n')
  const blob = new Blob([`\uFEFF${csvContent}`], { type: 'text/csv;charset=utf-8;' })
  const link = document.createElement('a')
  const url = URL.createObjectURL(blob)
  link.href = url
  link.download = `cleaning-task-details_${filters.value.startDate || 'all'}_${filters.value.endDate || 'all'}.csv`
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  URL.revokeObjectURL(url)
  ElMessage.success(t('accommodation.cleaningTaskList.exportSuccess'))
}

// 查看详情
const handleViewDetail = async (row: TaskListItem) => {
  try {
    const response = await getCleaningTaskById(row.id)
    if (response.success && response.data) {
      const detail = convertToListItem(response.data)
      taskDetail.value = detail
      assignCleanerId.value = detail.cleanerId
    } else {
      taskDetail.value = { ...row }
      assignCleanerId.value = row.cleanerId
    }
  } catch (error) {
    taskDetail.value = { ...row }
    assignCleanerId.value = row.cleanerId
  }
  detailDialogVisible.value = true
}

// 分配任务
const handleAssignTask = async () => {
  if (!taskDetail.value) {
    return
  }
  if (!assignCleanerId.value) {
    ElMessage.warning(t('accommodation.cleaning.selectCleaner'))
    return
  }

  try {
    const response = await assignCleaningTask(taskDetail.value.id, assignCleanerId.value)
    if (!response.success) {
      ElMessage.error(response.message || t('accommodation.cleaningTaskList.assignFailed'))
      return
    }
    ElMessage.success(t('accommodation.cleaningTaskList.assignSuccess'))
    detailDialogVisible.value = false
    await loadTasks()
  } catch (error: any) {
    console.error('分配任务失败:', error)
    ElMessage.error(error.message || t('accommodation.cleaningTaskList.assignFailed'))
  }
}

// 完成任务
const handleCompleteTask = async () => {
  if (!taskDetail.value) {
    return
  }
  const rawUser = localStorage.getItem('user')
  let approverId: number | undefined
  if (rawUser) {
    try {
      const parsedUser = JSON.parse(rawUser) as { id?: number | string }
      if (typeof parsedUser.id === 'number') {
        approverId = parsedUser.id
      } else if (typeof parsedUser.id === 'string') {
        approverId = Number(parsedUser.id)
      }
    } catch (error) {
      console.error('解析用户信息失败:', error)
    }
  }

  if (!approverId || Number.isNaN(approverId)) {
    ElMessage.error(t('accommodation.cleaningTaskList.approverMissing'))
    return
  }

  try {
    const response = await completeCleaningTask(taskDetail.value.id, approverId)
    if (!response.success) {
      ElMessage.error(response.message || t('accommodation.cleaningTaskList.completeFailed'))
      return
    }
    ElMessage.success(t('accommodation.cleaningTaskList.completeSuccess'))
    detailDialogVisible.value = false
    await loadTasks()
  } catch (error: any) {
    console.error('完成任务失败:', error)
    ElMessage.error(error.message || t('accommodation.cleaningTaskList.completeFailed'))
  }
}

// 分页事件
const handlePageChange = (page: number) => {
  pagination.value.current = page
  loadTasks()
}

const handleSizeChange = (size: number) => {
  pagination.value.size = size
  pagination.value.current = 1
  loadTasks()
}

// 监听筛选条件变化
watch(
  () => [
    filters.value.search,
    filters.value.taskType,
    filters.value.status,
    filters.value.roomTypeId,
    filters.value.startDate,
    filters.value.endDate,
  ],
  () => {
    pagination.value.current = 1
    loadTasks()
  },
)

// 页面加载时获取数据
onMounted(() => {
  loadRoomTypes()
  loadCleaners()
  loadTasks()
})
</script>

<style scoped>
.cleaning-task-list {
  padding: 20px;
  background: #f5f5f5;
  min-height: 100%;
}

/* 筛选卡片 */
.filter-card {
  margin-bottom: 20px;
}

.filter-section {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.filter-row {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.filter-label {
  font-size: 14px;
  color: #333;
  white-space: nowrap;
  min-width: 60px;
}

.search-input {
  width: 280px;
}

.filter-select {
  width: 150px;
}

.date-picker {
  width: 180px;
}

.filter-separator {
  font-size: 14px;
  color: #666;
}

.filter-actions {
  margin-left: auto;
  display: flex;
  gap: 12px;
  align-items: center;
}

/* 表格卡片 */
.table-card {
  width: 100%;
}

/* 状态点 */
.status-dot {
  display: inline-block;
  width: 6px;
  height: 6px;
  border-radius: 50%;
  margin-right: 4px;
}

.status-dot.status-expired {
  background-color: #909399;
}

.status-dot.status-pending {
  background-color: #e6a23c;
}

.status-dot.status-assigned {
  background-color: #409eff;
}

.status-dot.status-in-progress {
  background-color: #409eff;
}

.status-dot.status-completed {
  background-color: #67c23a;
}

/* 分页 */
.pagination-container {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 0;
  margin-top: 16px;
}

.pagination-info {
  font-size: 14px;
  color: #666;
}

/* 响应式 */
@media (max-width: 768px) {
  .cleaning-task-list {
    padding: 12px;
  }

  .filter-row {
    flex-direction: column;
    align-items: stretch;
  }

  .filter-label {
    width: 100%;
  }

  .search-input,
  .filter-select,
  .date-picker {
    width: 100%;
  }

  .filter-actions {
    margin-left: 0;
    justify-content: flex-end;
  }
}

.cleaning-task-list {
  --cleaning-blue: #1d94f3;
  --cleaning-header-blue: #cfe7fb;
  --cleaning-border: #e2e2e2;
  --cleaning-row-alt: #f7f7f7;
  padding: 4px 24px 24px;
  min-height: 100vh;
  background: #f5f5f5;
}

.filter-card {
  margin-bottom: 12px;
  border: none;
  border-radius: 0;
  background: #ffffff;
  box-shadow: none;
}

.filter-card :deep(.el-card__body) {
  padding: 14px 24px 16px;
}

.filter-section {
  gap: 10px;
}

.filter-row {
  min-height: 34px;
  gap: 12px;
}

.filter-label {
  min-width: auto;
  color: #2f2f2f;
  font-size: 14px;
  font-weight: 500;
  line-height: 34px;
}

.search-input {
  width: 324px;
}

.filter-select {
  width: 150px;
}

.filter-card :deep(.el-input__wrapper),
.filter-card :deep(.el-select__wrapper),
.filter-card :deep(.date-range-picker.el-range-editor.el-input__wrapper) {
  min-height: 34px;
  height: 34px;
  border-radius: 4px;
  background: #ffffff;
  box-shadow: 0 0 0 1px #dcdfe6 inset;
}

.filter-card :deep(.el-input__wrapper:hover),
.filter-card :deep(.el-input__wrapper.is-focus),
.filter-card :deep(.el-select__wrapper:hover),
.filter-card :deep(.el-select__wrapper.is-focused),
.filter-card :deep(.date-range-picker.el-range-editor.el-input__wrapper:hover),
.filter-card :deep(.date-range-picker.el-range-editor.el-input__wrapper.is-active) {
  box-shadow: 0 0 0 1px #cdd4da inset;
}

.filter-card :deep(.el-input__inner),
.filter-card :deep(.el-select__placeholder),
.filter-card :deep(.el-select__selected-item) {
  color: #31353a;
  font-size: 14px;
  font-weight: 400;
}

.filter-card :deep(.el-input__inner::placeholder),
.filter-card :deep(.el-select__placeholder.is-transparent) {
  color: #b7b7b7;
}

.filter-card :deep(.el-input__prefix) {
  color: #9da5ad;
}

.date-range-picker {
  width: 286px !important;
  flex: 0 0 286px;
  max-width: 286px;
}

.filter-card :deep(.date-range-picker.el-date-editor.el-range-editor) {
  width: 286px !important;
  flex: 0 0 286px !important;
  max-width: 286px !important;
}

.filter-card :deep(.date-range-picker.el-range-editor.el-input__wrapper) {
  padding: 1px 1px 1px 6px;
  overflow: hidden;
}

.filter-card :deep(.date-range-picker .el-range__icon) {
  margin: 0 8px 0 2px;
  color: #b9b9b9;
  font-size: 16px;
}

.filter-card :deep(.date-range-picker .el-range-input) {
  height: 30px;
  padding: 0 4px;
  border-radius: 0;
  background: #fafafa;
  color: #39414a;
  font-size: 14px;
  font-weight: 500;
  line-height: 30px;
}

.filter-card :deep(.date-range-picker .el-range-input:first-child) {
  margin-left: 2px;
}

.filter-card :deep(.date-range-picker .el-range-separator) {
  width: 26px;
  padding: 0;
  color: #909399;
  font-size: 13px;
  line-height: 34px;
  text-align: center;
}

.filter-card :deep(.date-range-picker .el-range__close-icon) {
  width: 0;
  min-width: 0;
  margin-left: 0;
  overflow: hidden;
}

.filter-actions {
  justify-content: flex-end;
  margin-left: auto;
}

.export-button {
  min-width: 96px;
}

.filter-actions :deep(.el-button) {
  height: 34px;
  padding: 0 20px;
  border-radius: 4px;
  font-size: 14px;
  font-weight: 500;
}

.filter-actions :deep(.el-button--primary) {
  border-color: var(--cleaning-blue);
  background: var(--cleaning-blue);
  color: #ffffff;
}

.table-card {
  width: 100%;
  border: none;
  border-radius: 0;
  background: transparent;
  box-shadow: none;
}

.table-card :deep(.el-card__body) {
  padding: 0;
}

.task-list-table {
  border: 1px solid var(--cleaning-border);
  color: #252525;
  font-size: 13px;
}

.task-list-table :deep(.el-table__inner-wrapper::before),
.task-list-table :deep(.el-table__border-left-patch),
.task-list-table :deep(.el-table__body-wrapper::before) {
  display: none;
}

.task-list-table :deep(.el-table__header th.el-table__cell) {
  height: 34px;
  padding: 0;
  border-color: #d9d9d9;
  border-bottom: none;
  background: var(--cleaning-header-blue);
  color: #2d8fcc;
  font-size: 13px;
  font-weight: 600;
}

.task-list-table :deep(.el-table__header th .cell) {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 8px;
  color: #2d8fcc;
  line-height: 1.2;
}

.task-list-table :deep(.el-table__body td.el-table__cell) {
  height: 54px;
  padding: 0;
  border-color: #e3e3e3;
}

.task-list-table :deep(.el-table__body tr:last-child td.el-table__cell) {
  border-bottom: none;
}

.task-list-table :deep(.el-table__body tr:first-child td.el-table__cell) {
  border-top: none;
}

.task-list-table :deep(.even-row td.el-table__cell) {
  background-color: #ffffff;
}

.task-list-table :deep(.odd-row td.el-table__cell) {
  background-color: var(--cleaning-row-alt);
}

.task-list-table :deep(.el-table__body td .cell) {
  padding: 0 12px;
  color: #303030;
  font-size: 13px;
  font-weight: 500;
  line-height: 1.32;
  text-align: center;
  white-space: normal;
}

.task-list-table :deep(.el-table__row:hover > td.el-table__cell) {
  background: inherit;
}

.task-list-table :deep(.el-button.is-link) {
  height: auto;
  padding: 0;
  color: #2d8fcc;
  font-size: 13px;
  font-weight: 500;
}

.task-status-text {
  min-width: 54px;
  height: 24px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 0 12px;
  border-radius: 7px;
  background: #f4f4f4;
  color: #707070;
  font-size: 13px;
  font-weight: 700;
  line-height: 1;
}

.task-status-text--expired {
  background: #f0f0f0;
  color: #8d8d8d;
}

.task-status-text--pending,
.task-status-text--assigned,
.task-status-text--in-progress {
  background: #fff1f1;
  color: #ef2f35;
}

.task-status-text--completed {
  background: #edf9f1;
  color: #37a969;
}

.pagination-container {
  min-height: 58px;
  margin-top: -1px;
  padding: 10px 18px;
  border: 1px solid var(--cleaning-border);
  border-top: 1px solid #ececec;
  background: #ffffff;
}

.pagination-info {
  color: #666;
  font-size: 13px;
}

.pagination-container :deep(.el-pagination) {
  --el-pagination-button-width: 30px;
  --el-pagination-button-height: 30px;
  --el-pagination-font-size: 14px;
}

.pagination-container :deep(.el-pagination .el-select) {
  width: 112px;
}

.pagination-container :deep(.el-pagination .el-select .el-select__wrapper) {
  min-height: 32px;
  border-radius: 4px;
  box-shadow: 0 0 0 1px #e3e3e3 inset;
}

.pagination-container :deep(.el-pager) {
  gap: 4px;
}

.pagination-container :deep(.el-pager li),
.pagination-container :deep(.btn-prev),
.pagination-container :deep(.btn-next) {
  min-width: 30px;
  height: 30px;
  border-radius: 4px;
  background: #ffffff;
  color: #7a7a7a;
  font-weight: 500;
}

.pagination-container :deep(.el-pager li.is-active) {
  color: var(--cleaning-blue);
  font-weight: 700;
}

:deep(.task-detail-dialog.el-dialog) {
  width: 760px;
  border-radius: 0;
  box-shadow: 0 12px 30px rgba(0, 0, 0, 0.16);
}

:deep(.task-detail-dialog .el-dialog__header) {
  display: flex;
  align-items: center;
  min-height: 72px;
  margin: 0;
  padding: 24px 34px 10px;
}

:deep(.task-detail-dialog .el-dialog__title) {
  color: #111111;
  font-size: 22px;
  font-weight: 700;
  line-height: 1;
}

:deep(.task-detail-dialog .el-dialog__headerbtn) {
  top: 24px;
  right: 34px;
  width: 26px;
  height: 26px;
}

:deep(.task-detail-dialog .el-dialog__close) {
  color: #8b8b8b;
  font-size: 24px;
}

:deep(.task-detail-dialog .el-dialog__body) {
  padding: 18px 76px 0;
}

.task-detail-grid {
  display: grid;
  grid-template-columns: 120px 1fr 120px 1fr;
  border-top: 1px solid #e4e4e4;
  border-left: 1px solid #e4e4e4;
}

.detail-label,
.detail-value {
  min-height: 41px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 14px;
  border-right: 1px solid #e4e4e4;
  border-bottom: 1px solid #e4e4e4;
  color: #262626;
  font-size: 17px;
  line-height: 1.3;
}

.detail-label {
  background: #f8f8f8;
  font-weight: 700;
}

.detail-value {
  color: #5c5c5c;
  font-weight: 500;
}

.detail-value--wide {
  grid-column: span 3;
  justify-content: flex-start;
  padding-left: 16px;
}

.detail-value--notes {
  min-height: 43px;
}

.assign-form {
  margin-top: 26px;
}

.assign-form :deep(.el-form-item) {
  margin-bottom: 0;
}

.assign-form :deep(.el-form-item__label) {
  height: 42px;
  justify-content: flex-start;
  color: #303030;
  font-size: 18px;
  font-weight: 500;
}

.assign-form :deep(.el-form-item.is-required .el-form-item__label::before) {
  display: none;
}

.assign-form :deep(.el-form-item.is-required .el-form-item__label::after) {
  content: '*';
  margin-right: 0;
  margin-left: 1px;
  color: #ef2f35;
  font-size: 18px;
}

.assign-form :deep(.el-form-item__content) {
  min-height: 42px;
}

.detail-cleaner-select {
  width: 100%;
}

.detail-cleaner-select :deep(.el-select__wrapper) {
  min-height: 42px;
  height: 42px;
  border-radius: 4px;
  box-shadow: 0 0 0 1px #e2e2e2 inset;
}

.detail-cleaner-select :deep(.el-select__placeholder),
.detail-cleaner-select :deep(.el-select__selected-item) {
  color: #b3b3b3;
  font-size: 16px;
  font-weight: 400;
}

.detail-cleaner-select :deep(.el-select__suffix) {
  color: #9d9d9d;
  font-size: 18px;
}

:deep(.task-detail-dialog .el-dialog__footer) {
  padding: 32px 0 34px;
  text-align: center;
}

:deep(.task-detail-dialog .el-dialog__footer .el-button) {
  width: 130px;
  height: 42px;
  margin: 0 14px;
  border-radius: 5px;
  font-size: 17px;
  font-weight: 500;
}

:deep(.task-detail-dialog .dialog-close-button) {
  border-color: #e2e2e2;
  background: #ffffff;
  color: #8c8c8c;
}

:deep(.task-detail-dialog .dialog-primary-button) {
  border-color: var(--cleaning-blue);
  background: var(--cleaning-blue);
  color: #ffffff;
}

@media (max-width: 768px) {
  .cleaning-task-list {
    padding: 4px 12px 16px;
  }

  .filter-card :deep(.el-card__body) {
    padding: 12px;
  }

  .filter-row {
    align-items: stretch;
    flex-direction: column;
  }

  .search-input,
  .filter-select,
  .date-range-picker {
    width: 100%;
    flex-basis: auto;
  }

  .filter-actions {
    width: 100%;
    margin-left: 0;
  }

  .export-button {
    width: 100%;
  }

  :deep(.task-detail-dialog.el-dialog) {
    width: calc(100vw - 24px);
  }

  :deep(.task-detail-dialog .el-dialog__body) {
    padding: 12px 18px 0;
  }

  .task-detail-grid {
    grid-template-columns: 88px 1fr;
  }

  .detail-value--wide {
    grid-column: span 1;
  }
}
</style>
