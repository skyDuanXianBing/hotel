<template>
  <div class="cleaning-task-list">
    <!-- 筛选条件 -->
    <el-card class="filter-card">
      <div class="filter-section">
        <div class="filter-row">
          <el-input
            v-model="filters.search"
            placeholder="搜索保洁员姓名或邮箱"
            clearable
            class="search-input"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>

          <span class="filter-label">任务类型</span>
          <el-select v-model="filters.taskType" placeholder="请选择" clearable class="filter-select">
            <el-option label="全部" value="" />
            <el-option label="退房" value="checkout" />
            <el-option label="日常清洁" value="daily" />
            <el-option label="深度清洁" value="deep" />
          </el-select>

          <span class="filter-label">任务状态</span>
          <el-select v-model="filters.status" placeholder="请选择" clearable class="filter-select">
            <el-option label="全部" value="" />
            <el-option label="已过期" value="expired" />
            <el-option label="待分配" value="pending" />
            <el-option label="待清洁" value="assigned" />
            <el-option label="清洁中" value="in_progress" />
            <el-option label="已完成" value="completed" />
          </el-select>

          <span class="filter-label">房型</span>
          <el-select
            v-model="filters.roomTypeId"
            placeholder="请选择"
            clearable
            class="filter-select"
          >
            <el-option label="全部" :value="undefined" />
            <el-option
              v-for="roomType in roomTypeList"
              :key="roomType.id"
              :label="roomType.name"
              :value="roomType.id"
            />
          </el-select>
        </div>

        <div class="filter-row">
          <span class="filter-label">日期</span>
          <el-date-picker
            v-model="filters.startDate"
            type="date"
            placeholder="开始日期"
            format="YYYY/MM/DD"
            value-format="YYYY-MM-DD"
            class="date-picker"
          />
          <span class="filter-separator">至</span>
          <el-date-picker
            v-model="filters.endDate"
            type="date"
            placeholder="结束日期"
            format="YYYY/MM/DD"
            value-format="YYYY-MM-DD"
            class="date-picker"
          />

          <div class="filter-actions">
            <el-button link type="primary" @click="handleCollapse">
              {{ isCollapsed ? '展开' : '收起' }}
            </el-button>
            <el-button type="primary" @click="handleExport">导出明细</el-button>
          </div>
        </div>
      </div>
    </el-card>

    <!-- 任务列表表格 -->
    <el-card class="table-card">
      <el-table :data="taskList" border style="width: 100%" v-loading="loading">
        <el-table-column prop="taskDate" label="日期" width="120" />
        <el-table-column prop="roomType" label="房型" width="120" />
        <el-table-column prop="roomNumber" label="房间" width="100" />
        <el-table-column prop="taskStatus" label="任务状态" width="120">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.taskStatus)" size="small">
              <span class="status-dot" :class="getStatusDotClass(row.taskStatus)"></span>
              {{ getStatusText(row.taskStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="taskType" label="任务类型" width="120" />
        <el-table-column prop="taskTime" label="任务时间" width="180" />
        <el-table-column prop="cleaner" label="保洁员" width="120">
          <template #default="{ row }">
            <span>{{ row.cleaner || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="approver" label="审批人" width="120">
          <template #default="{ row }">
            <span>{{ row.approver || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="handleViewDetail(row)">
              详情
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-container">
        <div class="pagination-info">共 {{ pagination.total }} 条</div>
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
    <el-dialog v-model="detailDialogVisible" title="任务详情" width="600px">
      <el-descriptions v-if="taskDetail" :column="2" border>
        <el-descriptions-item label="日期">{{ taskDetail.taskDate }}</el-descriptions-item>
        <el-descriptions-item label="房型">{{ taskDetail.roomType }}</el-descriptions-item>
        <el-descriptions-item label="房间号">{{ taskDetail.roomNumber }}</el-descriptions-item>
        <el-descriptions-item label="任务状态">
          <el-tag :type="getStatusType(taskDetail.taskStatus)" size="small">
            {{ getStatusText(taskDetail.taskStatus) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="任务类型">{{ taskDetail.taskType }}</el-descriptions-item>
        <el-descriptions-item label="任务时间">{{ taskDetail.taskTime }}</el-descriptions-item>
        <el-descriptions-item label="保洁员">{{ taskDetail.cleaner || '-' }}</el-descriptions-item>
        <el-descriptions-item label="审批人">{{ taskDetail.approver || '-' }}</el-descriptions-item>
        <el-descriptions-item label="创建时间" :span="2">{{
          taskDetail.createTime
        }}</el-descriptions-item>
        <el-descriptions-item label="完成时间" :span="2">{{
          taskDetail.completeTime || '-'
        }}</el-descriptions-item>
        <el-descriptions-item label="备注" :span="2">{{
          taskDetail.remark || '-'
        }}</el-descriptions-item>
      </el-descriptions>
      <el-form
        v-if="taskDetail && taskDetail.taskStatus === 'pending'"
        label-width="90px"
        style="margin-top: 16px"
      >
        <el-form-item label="保洁员" required>
          <el-select v-model="assignCleanerId" placeholder="请选择保洁员" style="width: 100%" filterable>
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
        <el-button @click="detailDialogVisible = false">关闭</el-button>
        <el-button
          v-if="taskDetail && taskDetail.taskStatus === 'pending'"
          type="primary"
          @click="handleAssignTask"
        >
          分配任务
        </el-button>
        <el-button
          v-if="
            taskDetail &&
            (taskDetail.taskStatus === 'assigned' || taskDetail.taskStatus === 'in_progress')
          "
          type="success"
          @click="handleCompleteTask"
        >
          完成任务
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { Search } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
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

// 是否收起筛选
const isCollapsed = ref(false)

// 房型列表
const roomTypeList = ref<RoomTypeDTO[]>([])

const formatDate = (date: Date) => {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

const getOffsetDate = (offsetDays: number) => {
  const date = new Date()
  date.setDate(date.getDate() + offsetDays)
  return formatDate(date)
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
    expired: '已过期',
    pending: '待分配',
    assigned: '待清洁',
    in_progress: '清洁中',
    completed: '已完成',
  }
  return textMap[status] || status
}

// 获取状态点样式
const getStatusDotClass = (status: string) => {
  return {
    'status-expired': status === 'expired',
    'status-pending': status === 'pending',
    'status-assigned': status === 'assigned',
    'status-in-progress': status === 'in_progress',
    'status-completed': status === 'completed',
  }
}

// 获取任务类型文本
const getTaskTypeText = (taskType: string) => {
  const typeMap: Record<string, string> = {
    checkout: '退房',
    daily: '日常清洁',
    deep: '深度清洁',
  }
  return typeMap[taskType] || taskType
}

// 格式化任务时间
const formatTaskTime = (task: CleaningTaskDTO) => {
  if (task.startTime && task.completeTime) {
    const start = new Date(task.startTime).toLocaleTimeString('zh-CN', {
      hour: '2-digit',
      minute: '2-digit',
    })
    const end = new Date(task.completeTime).toLocaleTimeString('zh-CN', {
      hour: '2-digit',
      minute: '2-digit',
    })
    return `${start}-${end}`
  } else if (task.estimatedTime) {
    return new Date(task.estimatedTime).toLocaleTimeString('zh-CN', {
      hour: '2-digit',
      minute: '2-digit',
    })
  }
  return '-'
}

// 格式化日期时间
const formatDateTime = (datetime?: string) => {
  if (!datetime) return '-'
  return new Date(datetime).toLocaleString('zh-CN')
}

// 转换API数据到列表项
const convertToListItem = (task: CleaningTaskDTO): TaskListItem => {
  return {
    id: task.id,
    taskDate: task.taskDate,
    roomType: task.roomType,
    roomNumber: task.roomNumber,
    taskStatus: task.status,
    taskType: getTaskTypeText(task.taskType),
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
      ElMessage.error(response.message || '获取任务列表失败')
    }
  } catch (error: any) {
    console.error('获取任务列表失败:', error)
    ElMessage.error(error.message || '获取任务列表失败')
  } finally {
    loading.value = false
  }
}

// 收起/展开筛选
const handleCollapse = () => {
  isCollapsed.value = !isCollapsed.value
}

// 导出明细
const handleExport = () => {
  if (taskList.value.length === 0) {
    ElMessage.warning('暂无可导出数据')
    return
  }

  const headers = [
    '日期',
    '房型',
    '房间',
    '任务状态',
    '任务类型',
    '任务时间',
    '保洁员',
    '审批人',
    '创建时间',
    '完成时间',
    '备注',
  ]

  const escapeCsvCell = (value: string) => `"${(value || '').replace(/"/g, '""')}"`
  const rows = taskList.value.map((item) =>
    [
      item.taskDate,
      item.roomType,
      item.roomNumber,
      getStatusText(item.taskStatus),
      item.taskType,
      item.taskTime,
      item.cleaner || '-',
      item.approver || '-',
      item.createTime,
      item.completeTime || '-',
      item.remark || '-',
    ]
      .map((cell) => escapeCsvCell(String(cell)))
      .join(',')
  )

  const csvContent = [headers.join(','), ...rows].join('\n')
  const blob = new Blob([`\uFEFF${csvContent}`], { type: 'text/csv;charset=utf-8;' })
  const link = document.createElement('a')
  const url = URL.createObjectURL(blob)
  link.href = url
  link.download = `保洁任务明细_${filters.value.startDate || 'all'}_${filters.value.endDate || 'all'}.csv`
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  URL.revokeObjectURL(url)
  ElMessage.success('导出成功')
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
    ElMessage.warning('请选择保洁员')
    return
  }

  try {
    const response = await assignCleaningTask(taskDetail.value.id, assignCleanerId.value)
    if (!response.success) {
      ElMessage.error(response.message || '分配失败')
      return
    }
    ElMessage.success('分配成功')
    detailDialogVisible.value = false
    await loadTasks()
  } catch (error: any) {
    console.error('分配任务失败:', error)
    ElMessage.error(error.message || '分配任务失败')
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
    ElMessage.error('无法获取当前审批人，请重新登录后重试')
    return
  }

  try {
    const response = await completeCleaningTask(taskDetail.value.id, approverId)
    if (!response.success) {
      ElMessage.error(response.message || '完成任务失败')
      return
    }
    ElMessage.success('任务已完成')
    detailDialogVisible.value = false
    await loadTasks()
  } catch (error: any) {
    console.error('完成任务失败:', error)
    ElMessage.error(error.message || '完成任务失败')
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
  }
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
</style>
