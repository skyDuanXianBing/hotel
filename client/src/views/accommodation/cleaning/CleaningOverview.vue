<template>
  <div class="cleaning-overview">
    <CleaningTabs />
    <!-- 顶部操作栏 -->
    <div class="header-toolbar">
      <div class="left-section">
        <button type="button" class="date-nav-button" @click="handlePreviousWeek">
          <el-icon><ArrowLeft /></el-icon>
        </button>
        <div class="cleaning-date-picker-shell">
          <el-date-picker
            v-model="currentDate"
            type="date"
            :placeholder="t('accommodation.common.select')"
            format="YYYY/MM/DD"
            value-format="YYYY-MM-DD"
            class="cleaning-date-picker cleaning-date-picker--primary"
            @change="handleDateChange"
          />
          <span v-if="currentDateDisplayText" class="cleaning-date-picker-display">
            {{ currentDateDisplayText }}
          </span>
        </div>
        <button type="button" class="date-nav-button date-nav-button--next" @click="handleNextWeek">
          <el-icon><ArrowRight /></el-icon>
        </button>
        <el-button class="toolbar-button refresh-button" :icon="Refresh" @click="handleRefresh">
          {{ t('accommodation.cleaning.refresh') }}
        </el-button>
        <el-dropdown @command="handleFilter">
          <el-button class="toolbar-button filter-button">
            {{ currentFilterLabel }} <el-icon class="el-icon--right"><ArrowDown /></el-icon>
          </el-button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="all">{{ t('accommodation.cleaning.allTasks') }}</el-dropdown-item>
              <el-dropdown-item command="pending">{{ t('accommodation.cleaning.pending') }}</el-dropdown-item>
              <el-dropdown-item command="assigned">{{ t('accommodation.cleaning.assigned') }}</el-dropdown-item>
              <el-dropdown-item command="in_progress">{{ t('accommodation.cleaning.inProgress') }}</el-dropdown-item>
              <el-dropdown-item command="completed">{{ t('accommodation.cleaning.completed') }}</el-dropdown-item>
              <el-dropdown-item command="expired">{{ t('accommodation.cleaning.expired') }}</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
      <div class="right-section">
        <el-button class="toolbar-button generate-button" type="primary" @click="handleGenerateTask">
          {{ t('accommodation.cleaning.generateTask') }}
        </el-button>
      </div>
    </div>

    <!-- 房态日历表格 -->
    <div class="calendar-container">
      <el-table
        :data="roomList"
        border
        class="calendar-table"
        :row-class-name="getRowClassName"
        v-loading="loading"
      >
        <!-- 房型列 -->
        <el-table-column :label="t('accommodation.cleaning.roomType')" width="100" fixed>
          <template #default="{ row }">
            <span>{{ row.roomType }}</span>
          </template>
        </el-table-column>

        <!-- 房间号列 -->
        <el-table-column :label="t('accommodation.cleaning.roomNumber')" width="80" fixed>
          <template #default="{ row }">
            <span>{{ row.roomNumber }}</span>
          </template>
        </el-table-column>

        <!-- 日期列 -->
        <el-table-column
          v-for="date in dateColumns"
          :key="date.date"
          :label="date.label"
          :class-name="date.isToday ? 'today-column' : ''"
          min-width="120"
        >
          <template #header>
            <div class="date-header">
              <div class="date-label" :class="{ 'today-label': date.isToday }">
                {{ date.label }}
              </div>
              <!-- <div class="room-count" v-if="date.roomCount !== null && date.roomCount !== undefined">
                剩{{ date.roomCount }}间
              </div> -->
            </div>
          </template>
          <template #default="{ row }">
            <div
              class="task-cell"
              @click="handleCellClick(row, date, $event)"
              @contextmenu.prevent="handleCellRightClick(row, date, $event)"
            >
              <div
                v-for="task in getTasksForCell(row.roomNumber, date.date)"
                :key="task.id"
                class="task-item"
                :class="getTaskClass(task)"
              >
                <span v-if="task.status === 'expired'">{{ t('accommodation.cleaning.expired') }}</span>
                <span v-else-if="task.status === 'pending'">{{ t('accommodation.cleaning.pending') }}</span>
                <span v-else-if="task.status === 'assigned'">{{ t('accommodation.cleaning.assigned') }}</span>
                <span v-else-if="task.status === 'in_progress'">{{ t('accommodation.cleaning.inProgress') }}</span>
                <span v-else-if="task.status === 'completed'">{{ t('accommodation.cleaning.completed') }}</span>
                <span v-else>{{ task.label }}</span>
              </div>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 右键菜单 -->
    <div
      v-show="contextMenuVisible"
      class="context-menu"
      :style="{ left: contextMenuPosition.x + 'px', top: contextMenuPosition.y + 'px' }"
      @click.stop
    >
      <div class="context-menu-item" @click="handleCreateTask">
        <span>{{ t('accommodation.cleaning.createTask') }}</span>
      </div>
      <div class="context-menu-item" @click="handleCancelMenu">
        <span>{{ t('accommodation.common.cancel') }}</span>
      </div>
    </div>

    <!-- 分配任务抽屉 -->
    <el-drawer
      v-model="assignDrawerVisible"
      :title="t('accommodation.cleaning.assignTask')"
      size="430px"
      direction="rtl"
      class="cleaning-task-drawer"
    >
      <div class="assign-drawer-content">
        <!-- 基本信息 -->
        <div class="section">
          <h3 class="section-title">{{ t('accommodation.cleaning.basicInfo') }}</h3>
          <el-descriptions :column="2" border>
            <el-descriptions-item :label="t('accommodation.cleaning.roomType')">{{ assignForm.roomType }}</el-descriptions-item>
            <el-descriptions-item :label="t('accommodation.cleaning.roomNumber')">{{ assignForm.roomNumber }}</el-descriptions-item>
            <el-descriptions-item :label="t('accommodation.common.date')" :span="2">{{
              assignForm.taskDate
            }}</el-descriptions-item>
          </el-descriptions>
        </div>

        <!-- 任务详情 -->
        <div class="section">
          <h3 class="section-title">{{ t('accommodation.cleaning.taskDetails') }}</h3>
          <el-form :model="assignForm" label-width="100px">
            <el-form-item :label="t('accommodation.common.taskType')">
              <span>{{ getTaskTypeText(assignForm.taskType) }}</span>
            </el-form-item>
            <el-form-item :label="t('accommodation.common.cleaner')" required>
              <el-select
                v-model="assignForm.cleaner"
                :placeholder="t('accommodation.common.select')"
                class="drawer-centered-select"
                style="width: 100%"
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
            <el-form-item :label="t('accommodation.common.taskTime')">
              <span>{{ assignForm.taskTime }}</span>
            </el-form-item>
            <el-form-item :label="t('accommodation.cleaning.taskNotification')">
              <el-input
                v-model="assignForm.remark"
                type="textarea"
                :rows="4"
                :placeholder="t('accommodation.cleaning.inputRemark')"
                maxlength="200"
                show-word-limit
              />
            </el-form-item>
          </el-form>
        </div>

        <!-- 底部按钮 -->
        <div class="drawer-footer">
          <el-button @click="handleCancelAssign" size="large" style="flex: 1">{{ t('accommodation.cleaning.cancelTask') }}</el-button>
          <el-button type="primary" @click="handleConfirmAssign" size="large" style="flex: 1">
            {{ t('accommodation.common.confirm') }}
          </el-button>
        </div>
      </div>
    </el-drawer>

    <!-- 新增任务抽屉 -->
    <el-drawer
      v-model="createDrawerVisible"
      :title="t('accommodation.cleaning.createTask')"
      size="430px"
      direction="rtl"
      class="cleaning-task-drawer"
    >
      <div class="assign-drawer-content">
        <!-- 基本信息 -->
        <div class="section">
          <h3 class="section-title">{{ t('accommodation.cleaning.basicInfo') }}</h3>
          <el-descriptions :column="2" border>
            <el-descriptions-item :label="t('accommodation.cleaning.roomType')">{{ createForm.roomType }}</el-descriptions-item>
            <el-descriptions-item :label="t('accommodation.cleaning.roomNumber')">{{ createForm.roomNumber }}</el-descriptions-item>
            <el-descriptions-item :label="t('accommodation.common.date')" :span="2">{{
              createForm.taskDate
            }}</el-descriptions-item>
          </el-descriptions>
        </div>

        <!-- 任务详情 -->
        <div class="section">
          <h3 class="section-title">{{ t('accommodation.cleaning.taskDetails') }}</h3>
          <el-form :model="createForm" label-width="80px">
            <el-form-item :label="t('accommodation.common.taskType')" required>
              <el-select
                v-model="createForm.taskType"
                :placeholder="t('accommodation.common.select')"
                style="width: 100%"
              >
                <el-option :label="t('accommodation.cleaning.checkout')" value="checkout" />
                <el-option :label="t('accommodation.cleaning.daily')" value="daily" />
                <el-option :label="t('accommodation.cleaning.deep')" value="deep" />
              </el-select>
            </el-form-item>
            <el-form-item :label="t('accommodation.common.cleaner')">
              <el-select
                v-model="createForm.cleaner"
                :placeholder="t('accommodation.common.select')"
                style="width: 100%"
                filterable
                clearable
              >
                <el-option
                  v-for="cleaner in cleanerList"
                  :key="cleaner.id"
                  :label="cleaner.name"
                  :value="cleaner.id"
                />
              </el-select>
            </el-form-item>
            <el-form-item :label="t('accommodation.common.taskTime')" required>
              <div style="display: flex; align-items: center; gap: 8px; width: 100%">
                <el-time-select
                  v-model="createForm.startTime"
                  :placeholder="t('accommodation.common.select')"
                  start="06:00"
                  step="00:30"
                  end="22:00"
                  style="flex: 1"
                />
                <span>-</span>
                <el-time-select
                  v-model="createForm.endTime"
                  :placeholder="t('accommodation.common.select')"
                  start="06:00"
                  step="00:30"
                  end="22:00"
                  :min-time="createForm.startTime"
                  style="flex: 1"
                />
              </div>
            </el-form-item>
            <el-form-item :label="t('accommodation.cleaning.taskNotification')">
              <el-input
                v-model="createForm.remark"
                type="textarea"
                :rows="4"
                :placeholder="t('accommodation.cleaning.inputRemark')"
                maxlength="200"
                show-word-limit
              />
            </el-form-item>
          </el-form>
        </div>

        <!-- 底部按钮 -->
        <div class="drawer-footer">
          <el-button @click="handleCancelCreate" size="large" style="flex: 1">{{ t('accommodation.common.cancel') }}</el-button>
          <el-button type="primary" @click="handleConfirmCreate" size="large" style="flex: 1">
            {{ t('accommodation.common.confirm') }}
          </el-button>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { ArrowLeft, ArrowRight, ArrowDown, Refresh } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { useI18n } from 'vue-i18n'
import {
  getCalendarViewData,
  getCleaners,
  assignCleaningTask,
  createCleaningTask,
  generateCleaningTasks,
  type CleaningTaskDTO,
  type CleanerDTO,
  type CleaningTaskCreateDTO,
} from '@/api/cleaning'
import { getRooms, type RoomDTO } from '@/api/room'
import { useAccommodationI18n } from '@/composables/useAccommodationI18n'
import { addDaysToYmd, formatYmdMonthDay, getStoreTodayYmd, getYmdWeekdayIndex } from '@/utils/storeDateTime'
import CleaningTabs from './components/CleaningTabs.vue'

const { t } = useI18n()
const { weekdayFullMap, weekdayShortMap } = useAccommodationI18n()

// 当前日期
const currentDate = ref(getStoreTodayYmd())

// 当前日期展示文本
const currentDateDisplayText = computed(() => {
  if (!currentDate.value) {
    return ''
  }

  return `${currentDate.value.replace(/-/g, '/')} ${weekdayFullMap.value[getYmdWeekdayIndex(currentDate.value)]}`
})

// 生成日期列(显示10天)
const dateColumns = computed(() => {
  const columns = []
  const realToday = getStoreTodayYmd() // 按门店时区计算今天
  const startDate = addDaysToYmd(currentDate.value, -2) // 从视图中心日期前2天开始

  for (let i = 0; i < 10; i++) {
    const dateStr = addDaysToYmd(startDate, i)
    const { month, day } = formatYmdMonthDay(dateStr)
    const weekdayIndex = getYmdWeekdayIndex(dateStr)
    const dayLabel = `${month}-${day}`
    const weekDay = weekdayShortMap.value[weekdayIndex]
    const isWeekend = weekdayIndex === 0 || weekdayIndex === 6
    const isToday = dateStr === realToday

    const label = `${dayLabel} ${weekDay}`

    columns.push({
      date: dateStr,
      label,
      isToday,
      isWeekend,
      roomCount: i === 0 ? 0 : null, // 示例数据
    })
  }
  return columns
})

// 房间列表
interface RoomListItem {
  id: number
  roomType: string
  roomNumber: string
  roomTypeId: number
}

const roomList = ref<RoomListItem[]>([])

// 保洁员列表
const cleanerList = ref<CleanerDTO[]>([])

// 加载房间列表
const loadRooms = async () => {
  try {
    const response = await getRooms()
    if (response.success && response.data) {
      roomList.value = response.data.map((room: RoomDTO) => ({
        id: room.id,
        roomType: room.roomType.name,
        roomNumber: room.roomNumber,
        roomTypeId: room.roomType.id,
      }))
    }
  } catch (error: any) {
    console.error('获取房间列表失败:', error)
  }
}

// 加载保洁员列表
const loadCleaners = async () => {
  try {
    const response = await getCleaners()
    if (response.success && response.data) {
      cleanerList.value = response.data
    }
  } catch (error: any) {
    console.error('获取保洁员列表失败:', error)
    ElMessage.error(error.message || t('accommodation.cleaningOverview.loadCleanersFailed'))
  }
}

// 任务数据
interface TaskItem {
  id: number
  roomNumber: string
  date: string
  status: string
  type: string
  label: string
}

const tasks = ref<TaskItem[]>([])
const loading = ref(false)
const currentFilter = ref('all')

const currentFilterLabel = computed(() => {
  const labelMap: Record<string, string> = {
    all: t('accommodation.cleaning.allTasks'),
    pending: t('accommodation.cleaning.pending'),
    assigned: t('accommodation.cleaning.assigned'),
    in_progress: t('accommodation.cleaning.inProgress'),
    completed: t('accommodation.cleaning.completed'),
    expired: t('accommodation.cleaning.expired'),
  }

  return labelMap[currentFilter.value] || t('accommodation.cleaning.filter')
})

// 加载任务数据
const loadTasks = async () => {
  try {
    loading.value = true
    const startDate = dateColumns.value[0]?.date
    const endDate = dateColumns.value[dateColumns.value.length - 1]?.date

    if (!startDate || !endDate) {
      return
    }

    const response = await getCalendarViewData({
      startDate,
      endDate,
      status: currentFilter.value === 'all' ? undefined : currentFilter.value,
    })

    if (response.success && response.data) {
      // 转换API数据到任务列表
      const taskList: TaskItem[] = []
      const tasksMap = response.data.tasks

      for (const key in tasksMap) {
        const taskArray = tasksMap[key]
        taskArray.forEach((task: CleaningTaskDTO) => {
          taskList.push({
            id: task.id,
            roomNumber: task.roomNumber,
            date: task.taskDate,
            status: task.status,
            type: task.taskType,
            label: getStatusLabel(task.status),
          })
        })
      }

      tasks.value = taskList
    } else {
      ElMessage.error(response.message || t('accommodation.cleaningOverview.loadTasksFailed'))
    }
  } catch (error: any) {
    console.error('获取任务数据失败:', error)
    ElMessage.error(error.message || t('accommodation.cleaningOverview.loadTasksFailed'))
  } finally {
    loading.value = false
  }
}

// 获取状态标签
const getStatusLabel = (status: string) => {
  const labelMap: Record<string, string> = {
    expired: t('accommodation.cleaningOverview.statusLabels.expired'),
    pending: t('accommodation.cleaningOverview.statusLabels.pending'),
    assigned: t('accommodation.cleaningOverview.statusLabels.assigned'),
    in_progress: t('accommodation.cleaningOverview.statusLabels.in_progress'),
    completed: t('accommodation.cleaningOverview.statusLabels.completed'),
  }
  return labelMap[status] || status
}

// 获取单元格中的任务
const getTasksForCell = (roomNumber: string, date: string) => {
  return tasks.value.filter((task) => task.roomNumber === roomNumber && task.date === date)
}

// 获取任务样式类（只根据状态设置颜色，不受任务类型影响）
const getTaskClass = (task: any) => {
  return {
    'task-expired': task.status === 'expired',
    'task-pending': task.status === 'pending',
    'task-assigned': task.status === 'assigned',
    'task-in-progress': task.status === 'in_progress',
    'task-completed': task.status === 'completed',
  }
}

// 获取行样式
const getRowClassName = ({ rowIndex }: { rowIndex: number }) => {
  return rowIndex % 2 === 0 ? 'even-row' : 'odd-row'
}

// 右键菜单
const contextMenuVisible = ref(false)
const contextMenuPosition = ref({ x: 0, y: 0 })
const selectedCell = ref<any>(null)

// 分配任务抽屉
const assignDrawerVisible = ref(false)
const assignForm = ref({
  taskId: 0,
  roomNumber: '',
  roomType: '',
  taskType: '',
  taskDate: '',
  taskTime: '10:00-16:00',
  cleaner: undefined as number | undefined,
  remark: '',
})

// 新增任务抽屉
const createDrawerVisible = ref(false)
const createForm = ref({
  roomId: 0,
  roomNumber: '',
  roomType: '',
  taskDate: '',
  taskType: '',
  cleaner: undefined as number | undefined,
  startTime: '10:00',
  endTime: '16:00',
  remark: '',
})

// 处理单元格点击
const handleCellClick = (row: any, date: any, event: MouseEvent) => {
  // 阻止事件冒泡,防止触发全局点击关闭菜单
  event.stopPropagation()

  const cellTasks = getTasksForCell(row.roomNumber, date.date)
  const today = getStoreTodayYmd()
  const clickDate = date.date

  // 判断是否是历史日期
  if (clickDate < today && cellTasks.length === 0) {
    ElMessage.warning(t('accommodation.cleaningOverview.historyDateUnsupported'))
    contextMenuVisible.value = false
    return
  }

  if (cellTasks.length > 0) {
    // 如果有任务,关闭右键菜单
    contextMenuVisible.value = false

    const task = cellTasks[0]
    if (task.status === 'pending') {
      // 待分配任务,打开分配抽屉
      assignForm.value = {
        taskId: task.id,
        roomNumber: row.roomNumber,
        roomType: row.roomType,
        taskType: task.type,
        taskDate: formatDateDisplay(date.date),
        taskTime: '10:00-16:00',
        cleaner: undefined,
        remark: '',
      }
      assignDrawerVisible.value = true
    } else if (task.status === 'expired') {
      ElMessage.info(t('accommodation.cleaningOverview.taskExpired'))
    } else {
      ElMessage.info(t('accommodation.cleaningOverview.taskAssigned'))
    }
  } else {
    // 空白格子,显示菜单
    selectedCell.value = { row, date }
    contextMenuPosition.value = {
      x: event.clientX,
      y: event.clientY,
    }
    contextMenuVisible.value = true
  }
}

// 处理右键点击
const handleCellRightClick = (row: any, date: any, event: MouseEvent) => {
  const cellTasks = getTasksForCell(row.roomNumber, date.date)
  const today = getStoreTodayYmd()
  const clickDate = date.date

  // 只有空白格子且不是历史日期才显示右键菜单
  if (cellTasks.length === 0 && clickDate >= today) {
    selectedCell.value = { row, date }
    contextMenuPosition.value = {
      x: event.clientX,
      y: event.clientY,
    }
    contextMenuVisible.value = true
  }
}

// 创建任务
const handleCreateTask = () => {
  if (!selectedCell.value) return

  const { row, date } = selectedCell.value
  createForm.value = {
    roomId: row.id,
    roomNumber: row.roomNumber,
    roomType: row.roomType,
    taskDate: formatDateDisplay(date.date),
    taskType: '',
    cleaner: undefined,
    startTime: '10:00',
    endTime: '16:00',
    remark: '',
  }
  createDrawerVisible.value = true
  contextMenuVisible.value = false
}

// 取消菜单
const handleCancelMenu = () => {
  contextMenuVisible.value = false
  selectedCell.value = null
}

// 获取任务类型文本
const getTaskTypeText = (type: string) => {
  const typeMap: Record<string, string> = {
    daily: t('accommodation.cleaning.daily'),
    checkout: t('accommodation.cleaning.checkout'),
    deep: t('accommodation.cleaning.deep'),
  }
  return typeMap[type] || type
}

// 格式化日期显示
const formatDateDisplay = (dateStr: string) => {
  const [year, month, day] = dateStr.split('-')
  return `${year}/${month}/${day}`
}

// 取消分配
const handleCancelAssign = () => {
  assignDrawerVisible.value = false
  assignForm.value = {
    taskId: 0,
    roomNumber: '',
    roomType: '',
    taskType: '',
    taskDate: '',
    taskTime: '10:00-16:00',
    cleaner: undefined,
    remark: '',
  }
}

// 确认分配
const handleConfirmAssign = async () => {
  console.log('=== 开始分配任务 ===')
  console.log('assignForm.value:', assignForm.value)

  if (!assignForm.value.cleaner) {
    ElMessage.warning(t('accommodation.cleaning.selectCleaner'))
    return
  }

  console.log('准备调用API - taskId:', assignForm.value.taskId, 'cleanerId:', assignForm.value.cleaner)

  try {
    // 调用后端API保存分配信息
    const response = await assignCleaningTask(assignForm.value.taskId, assignForm.value.cleaner)
    console.log('API响应:', response)

    if (response.success) {
      // 更新本地任务状态
      const taskIndex = tasks.value.findIndex((t) => t.id === assignForm.value.taskId)
      if (taskIndex !== -1) {
        tasks.value[taskIndex].status = 'assigned'
        tasks.value[taskIndex].label = t('accommodation.cleaningOverview.statusLabels.assigned')
      }

      ElMessage.success(t('accommodation.cleaningOverview.assignSuccess'))
      assignDrawerVisible.value = false

      // 重新加载任务数据以确保与后端同步
      console.log('重新加载任务数据...')
      await loadTasks()
    } else {
      console.error('分配失败:', response.message)
      ElMessage.error(response.message || t('accommodation.cleaningOverview.assignFailed'))
    }
  } catch (error: any) {
    console.error('分配任务失败:', error)
    ElMessage.error(error.message || t('accommodation.cleaningOverview.assignFailed'))
  }
}

// 取消新增任务
const handleCancelCreate = () => {
  createDrawerVisible.value = false
  createForm.value = {
    roomId: 0,
    roomNumber: '',
    roomType: '',
    taskDate: '',
    taskType: '',
    cleaner: undefined,
    startTime: '10:00',
    endTime: '16:00',
    remark: '',
  }
}

// 确认新增任务
const handleConfirmCreate = async () => {
  console.log('=== 开始创建任务 ===')
  console.log('createForm.value:', createForm.value)

  if (!createForm.value.taskType) {
    ElMessage.warning(t('accommodation.cleaningOverview.selectTaskType'))
    return
  }

  if (!createForm.value.startTime || !createForm.value.endTime) {
    ElMessage.warning(t('accommodation.cleaningOverview.selectTaskTime'))
    return
  }

  try {
    // 从日期字符串解析出日期
    const dateStr = createForm.value.taskDate.replace(/\//g, '-')

    // 构造创建任务的DTO
    const createDTO: CleaningTaskCreateDTO = {
      taskDate: dateStr,
      roomId: createForm.value.roomId,
      taskType: createForm.value.taskType,
      cleanerId: createForm.value.cleaner,
      estimatedTime: `${createForm.value.startTime}-${createForm.value.endTime}`,
      notes: createForm.value.remark || undefined,
    }

    console.log('准备调用创建任务API:', createDTO)

    // 调用后端API创建任务
    const response = await createCleaningTask(createDTO)
    console.log('API响应:', response)

    if (response.success) {
      ElMessage.success(t('accommodation.cleaningOverview.createSuccess'))
      createDrawerVisible.value = false

      // 重置表单
      createForm.value = {
        roomId: 0,
        roomNumber: '',
        roomType: '',
        taskDate: '',
        taskType: '',
        cleaner: undefined,
        startTime: '10:00',
        endTime: '16:00',
        remark: '',
      }

      // 重新加载任务数据
      console.log('重新加载任务数据...')
      await loadTasks()
    } else {
      console.error('创建任务失败:', response.message)
      ElMessage.error(response.message || t('accommodation.cleaningOverview.createFailed'))
    }
  } catch (error: any) {
    console.error('创建任务失败:', error)
    ElMessage.error(error.message || t('accommodation.cleaningOverview.createFailed'))
  }
}

// 处理日期变化
const handleDateChange = () => {
  console.log('日期变化:', currentDate.value)
  loadTasks()
}

// 刷新
const handleRefresh = () => {
  loadTasks()
  ElMessage.success(t('accommodation.cleaningOverview.refreshSuccess'))
}

// 筛选
const handleFilter = (command: string) => {
  currentFilter.value = command
  loadTasks()
}

// 生成任务
const handleGenerateTask = async () => {
  const startDate = dateColumns.value[0]?.date
  const endDate = dateColumns.value[dateColumns.value.length - 1]?.date

  if (!startDate || !endDate) {
    ElMessage.warning(t('accommodation.cleaningOverview.invalidDateRange'))
    return
  }

  try {
    const response = await generateCleaningTasks({ startDate, endDate })
    if (response.success && response.data) {
      ElMessage.success(
        t('accommodation.cleaningOverview.generatedSummary', {
          processedReservations: response.data.processedReservations,
          createdCount: response.data.createdCount,
          updatedCount: response.data.updatedCount,
          skippedCount: response.data.skippedCount,
        })
      )
      await loadTasks()
    } else {
      ElMessage.error(response.message || t('accommodation.cleaningOverview.generateFailed'))
    }
  } catch (error: any) {
    console.error('生成任务失败:', error)
    ElMessage.error(error.message || t('accommodation.cleaningOverview.generateFailed'))
  }
}

// 设置
const handleSettings = () => {
  console.log('打开设置')
  // TODO: 打开设置对话框
}

// 上一周
const handlePreviousWeek = () => {
  currentDate.value = addDaysToYmd(currentDate.value, -7)
  loadTasks()
}

// 下一周
const handleNextWeek = () => {
  currentDate.value = addDaysToYmd(currentDate.value, 7)
  loadTasks()
}

// 全局点击事件监听,关闭右键菜单
const handleGlobalClick = () => {
  contextMenuVisible.value = false
}

// 监听日期列变化,重新加载任务
watch(
  () => dateColumns.value,
  () => {
    loadTasks()
  },
  { deep: true }
)

// 组件挂载时添加全局点击监听
onMounted(() => {
  document.addEventListener('click', handleGlobalClick)
  loadRooms()
  loadCleaners()
  loadTasks()
})

// 组件卸载时移除监听
onUnmounted(() => {
  document.removeEventListener('click', handleGlobalClick)
})
</script>

<style scoped>
.cleaning-overview {
  padding: 20px;
  background: #f5f5f5;
  min-height: 100%;
}

/* 顶部工具栏 */
.header-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  padding: 16px;
  background: #fff;
  border-radius: 4px;
}

.left-section,
.right-section {
  display: flex;
  gap: 12px;
  align-items: center;
}

/* 日期导航 */
/* 日历容器 */
.calendar-container {
  background: #fff;
  border-radius: 4px;
  overflow: auto;
}

.calendar-table {
  width: 100%;
}

/* 日期表头 */
.date-header {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}

.date-label {
  font-size: 13px;
  font-weight: 500;
  color: #333;
}

.date-label.today-label {
  color: #409eff;
  font-weight: 600;
}

.room-count {
  font-size: 12px;
  color: #666;
}

/* 今天的列 */
.calendar-table :deep(.today-column) {
  background-color: #e6f7ff;
}

/* 任务单元格 */
.task-cell {
  min-height: 60px;
  padding: 4px;
  display: flex;
  flex-direction: column;
  gap: 4px;
  cursor: pointer;
  position: relative;
}

.task-cell:hover {
  background-color: #f5f5f5;
}

/* 任务项 */
.task-item {
  padding: 6px 12px;
  border-radius: 4px;
  font-size: 12px;
  text-align: center;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* 已过期任务 */
.task-item.task-expired {
  background-color: #909399;
  color: #fff;
}

/* 待分配任务 */
.task-item.task-pending {
  background-color: #409eff;
  color: #fff;
}

/* 已分配任务（待接受） */
.task-item.task-assigned {
  background-color: #f56c6c;
  color: #fff;
}

/* 进行中任务（待打扫） */
.task-item.task-in-progress {
  background-color: #e6a23c;
  color: #fff;
}

/* 已完成任务 */
.task-item.task-completed {
  background-color: #67c23a;
  color: #fff;
}

/* 表格行样式 */
.calendar-table :deep(.even-row) {
  background-color: #fafafa;
}

.calendar-table :deep(.odd-row) {
  background-color: #fff;
}

/* 表格样式优化 */
.calendar-table :deep(.el-table__header th) {
  background: #f5f5f5;
  color: #333;
  font-weight: 500;
  font-size: 13px;
  padding: 8px 0;
}

.calendar-table :deep(.el-table__body td) {
  padding: 0;
}

.calendar-table :deep(.el-table__fixed-header-wrapper th) {
  background: #f5f5f5;
}

/* 右键菜单 */
.context-menu {
  position: fixed;
  background: #333;
  border-radius: 4px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.3);
  z-index: 9999;
  min-width: 120px;
  padding: 4px 0;
}

.context-menu-item {
  padding: 10px 16px;
  cursor: pointer;
  color: #fff;
  font-size: 14px;
  transition: background-color 0.2s;
  user-select: none;
}

.context-menu-item:hover {
  background-color: #444;
}

.context-menu-item:active {
  background-color: #555;
}

/* 分配任务抽屉 */
.assign-drawer-content {
  display: flex;
  flex-direction: column;
  height: 100%;
  padding: 0 20px 20px;
}

.section {
  margin-bottom: 24px;
}

.section-title {
  font-size: 16px;
  font-weight: 500;
  color: #333;
  margin: 0 0 16px 0;
}

.drawer-footer {
  display: flex;
  gap: 12px;
  padding-top: 20px;
  margin-top: auto;
  border-top: 1px solid #e8e8e8;
}

/* 响应式 */
@media (max-width: 768px) {
  .cleaning-overview {
    padding: 12px;
  }

  .header-toolbar {
    flex-direction: column;
    align-items: stretch;
    gap: 12px;
  }

  .left-section,
  .right-section {
    flex-wrap: wrap;
  }
}

.cleaning-overview {
  --cleaning-blue: #1d94f3;
  --cleaning-header-blue: #cfe7fb;
  --cleaning-border: #e2e2e2;
  --cleaning-row-alt: #f7f7f7;
  padding: 4px 24px 24px;
  min-height: 100vh;
}

.header-toolbar {
  display: flex;
  align-items: center;
  gap: 16px;
  width: 100%;
  margin-bottom: 12px;
  padding: 10px 16px;
  background: #ffffff;
  border: 1px solid var(--cleaning-border);
  border-radius: 4px;
  box-sizing: border-box;
}

.left-section,
.right-section {
  gap: 8px;
}

.left-section {
  min-width: 0;
  flex-wrap: nowrap;
}

.right-section {
  margin-left: auto;
}

.date-nav-button {
  width: 24px;
  height: 24px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 0;
  border: 1px solid #d8ebf4;
  border-radius: 999px;
  background: #ffffff;
  color: #9aa8ad;
  cursor: pointer;
  font-size: 13px;
  transition:
    border-color 0.2s ease,
    color 0.2s ease,
    background-color 0.2s ease;
}

.date-nav-button :deep(svg) {
  width: 30px;
  height: 30px;
}

.date-nav-button:hover {
  border-color: #67b6d9;
  color: #55add4;
  background: #f6fcff;
}

.date-nav-button--next {
  border-color: #8cc7df;
  color: #5baed1;
}

.cleaning-date-picker-shell {
  position: relative;
  display: inline-flex;
  align-items: center;
  width: 190px;
  height: 32px;
}

.cleaning-date-picker-shell :deep(.cleaning-date-picker) {
  width: 190px !important;
}

.cleaning-date-picker-display {
  position: absolute;
  top: 1px;
  right: 1px;
  bottom: 1px;
  left: 53px;
  display: inline-flex;
  align-items: center;
  justify-content: flex-end;
  min-width: 0;
  padding: 0 10px 0 12px;
  border-radius: 0 3px 3px 0;
  background: #f3f3f3;
  color: #2f2f2f;
  font-size: 14px;
  font-weight: 600;
  line-height: 1;
  letter-spacing: 0.1px;
  pointer-events: none;
  white-space: nowrap;
}

.cleaning-date-picker-shell :deep(.cleaning-date-picker--primary .el-input__wrapper) {
  height: 32px;
  min-height: 32px;
  padding: 0 2px 0 0;
  border-radius: 4px;
  background: #ffffff;
  box-shadow:
    0 0 0 1px #d8d8d8 inset,
    0 1px 2px rgba(20, 20, 20, 0.04);
}

.cleaning-date-picker-shell :deep(.cleaning-date-picker--primary .el-input__wrapper:hover),
.cleaning-date-picker-shell :deep(.cleaning-date-picker--primary .el-input__wrapper.is-focus) {
  box-shadow:
    0 0 0 1px #cdd4da inset,
    0 1px 2px rgba(20, 20, 20, 0.05);
}

.cleaning-date-picker-shell :deep(.cleaning-date-picker--primary .el-input__prefix) {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 54px;
  margin-right: 0;
  color: #a8afb7;
}

.cleaning-date-picker-shell :deep(.cleaning-date-picker--primary .el-input__prefix-inner) {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 100%;
}

.cleaning-date-picker-shell :deep(.cleaning-date-picker--primary .el-input__icon) {
  font-size: 16px;
}

.cleaning-date-picker-shell :deep(.cleaning-date-picker--primary .el-input__suffix) {
  display: none;
}

.cleaning-date-picker-shell :deep(.cleaning-date-picker--primary .el-input__inner) {
  height: 28px;
  margin: 2px 0;
  padding: 0 12px 0 52px;
  border-radius: 0;
  background: transparent;
  color: transparent !important;
  -webkit-text-fill-color: transparent !important;
  caret-color: transparent;
  opacity: 0 !important;
  text-shadow: none;
  font-size: 14px;
  font-weight: 600;
  text-align: left;
  letter-spacing: 0.1px;
  cursor: pointer;
}

.cleaning-date-picker-shell
  :deep(.cleaning-date-picker--primary .el-input__inner::placeholder) {
  color: transparent !important;
  -webkit-text-fill-color: transparent !important;
  text-align: right;
}

.header-toolbar :deep(.toolbar-button.el-button) {
  height: 32px;
  min-height: 32px;
  padding: 0 14px;
  border-radius: 4px;
  border-color: #dcdcdc;
  background: #ffffff;
  color: #343434;
  font-size: 13px;
  font-weight: 600;
}

.header-toolbar :deep(.toolbar-button.el-button:hover) {
  border-color: #b9dff7;
  color: var(--cleaning-blue);
}

.right-section :deep(.generate-button.el-button--primary) {
  min-width: 88px;
  border-color: var(--cleaning-blue);
  background: var(--cleaning-blue);
  color: #ffffff;
  box-shadow: none;
}

.calendar-container {
  border: 1px solid var(--cleaning-border);
  border-radius: 0;
  background: #ffffff;
  box-shadow: none;
}

.calendar-table {
  color: #252525;
  font-size: 13px;
}

.date-header {
  justify-content: center;
  min-height: 30px;
  gap: 0;
}

.date-label,
.date-label.today-label {
  color: #2d8fcc;
  font-size: 13px;
  font-weight: 600;
  line-height: 1.2;
}

.calendar-table :deep(.today-column) {
  background-color: transparent;
}

.task-cell {
  min-height: 52px;
  padding: 6px 8px;
  align-items: center;
  justify-content: center;
  gap: 6px;
}

.task-cell:hover {
  background-color: transparent;
}

.task-item {
  min-width: 86px;
  height: 32px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 0 12px;
  border-radius: 4px;
  font-size: 13px;
  font-weight: 600;
  line-height: 1;
}

.task-item.task-expired {
  background-color: #8e9298;
}

.task-item.task-pending {
  background-color: var(--cleaning-blue);
}

.task-item.task-assigned {
  background-color: #ff4d4f;
}

.task-item.task-in-progress {
  background-color: #e8a23b;
}

.task-item.task-completed {
  background-color: #37b26c;
}

.calendar-table :deep(.even-row td.el-table__cell) {
  background-color: #ffffff;
}

.calendar-table :deep(.odd-row td.el-table__cell) {
  background-color: var(--cleaning-row-alt);
}

.calendar-table :deep(.el-table__inner-wrapper::before),
.calendar-table :deep(.el-table__border-left-patch),
.calendar-table :deep(.el-table__body-wrapper::before) {
  display: none;
}

.calendar-table :deep(.el-table__header th.el-table__cell) {
  height: 32px;
  padding: 0;
  border-color: #d9d9d9;
  border-bottom: none;
  background: var(--cleaning-header-blue);
  color: #2d8fcc;
  font-weight: 600;
  font-size: 13px;
}

.calendar-table :deep(.el-table__header th .cell) {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 8px;
  line-height: 1.2;
  color: #2d8fcc;
}

.calendar-table :deep(.el-table__body td.el-table__cell) {
  height: 52px;
  padding: 0;
  border-color: #e3e3e3;
}

.calendar-table :deep(.el-table__body tr:first-child td.el-table__cell) {
  border-top: none;
}

.calendar-table :deep(.el-table__fixed-header-wrapper th) {
  background: var(--cleaning-header-blue);
}

.calendar-table :deep(.el-table__body td .cell) {
  padding: 0;
  color: #303030;
  text-align: center;
  line-height: 1.28;
  white-space: normal;
}

.calendar-table :deep(.el-table__row:hover > td.el-table__cell) {
  background: inherit;
}

.context-menu {
  background: #ffffff;
  border: 1px solid #e8e8e8;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.14);
}

.context-menu-item {
  color: #252525;
  font-size: 13px;
}

.context-menu-item:hover {
  background-color: #f5f5f5;
}

.assign-drawer-content {
  padding: 124px 64px 44px;
}

.section {
  margin-bottom: 42px;
}

.section-title {
  margin: 0 0 18px;
  color: #252525;
  font-size: 18px;
  font-weight: 700;
}

.drawer-footer {
  justify-content: space-between;
  gap: 72px;
  padding-top: 0;
  border-top: none;
}

.assign-drawer-content :deep(.el-descriptions__table) {
  width: 100%;
  border-collapse: collapse;
}

.assign-drawer-content :deep(.el-descriptions__cell) {
  height: 34px;
  padding: 0 10px;
  border-color: #e4e4e4;
  font-size: 14px;
}

.assign-drawer-content :deep(.el-descriptions__label) {
  width: 52px;
  background: #ffffff;
  color: #252525;
  font-weight: 700;
}

.assign-drawer-content :deep(.el-descriptions__content) {
  color: #6d6d6d;
  font-weight: 500;
}

.assign-drawer-content :deep(.el-form-item) {
  margin-bottom: 14px;
}

.assign-drawer-content :deep(.el-form-item__label) {
  height: 30px;
  align-items: center;
  justify-content: flex-start;
  color: #3f3f3f;
  font-size: 14px;
  font-weight: 500;
}

.assign-drawer-content :deep(.el-form-item__content) {
  min-height: 30px;
  align-items: center;
  color: #252525;
  font-size: 14px;
  font-weight: 600;
}

.assign-drawer-content :deep(.el-select),
.assign-drawer-content :deep(.el-input),
.assign-drawer-content :deep(.el-textarea) {
  width: 100%;
}

.assign-drawer-content :deep(.el-input__wrapper) {
  height: 28px;
  border-radius: 4px;
  box-shadow: 0 0 0 1px #e2e2e2 inset;
}

.assign-drawer-content :deep(.drawer-centered-select .el-select__wrapper) {
  min-height: 28px;
  height: 28px;
  padding: 0;
  border-radius: 4px;
  box-shadow: 0 0 0 1px #e2e2e2 inset;
}

.assign-drawer-content :deep(.drawer-centered-select .el-select__selection) {
  display: inline-flex;
  flex: 0 0 auto;
  align-items: center;
  justify-content: center;
  gap: 6px;
  max-width: calc(100% - 28px);
  margin-left: auto;
}

.assign-drawer-content :deep(.drawer-centered-select .el-select__placeholder) {
  position: static;
  transform: none;
  color: #b3b3b3;
  font-size: 13px;
  font-weight: 500;
}

.assign-drawer-content :deep(.drawer-centered-select .el-select__suffix) {
  position: static;
  flex: 0 0 auto;
  margin-right: auto;
  color: #9b9b9b;
}

.assign-drawer-content :deep(.el-textarea__inner) {
  min-height: 72px !important;
  border-radius: 4px;
  box-shadow: 0 0 0 1px #e2e2e2 inset;
  resize: none;
}

.drawer-footer :deep(.el-button) {
  flex: 1;
  height: 38px;
  border-radius: 4px;
  font-size: 14px;
  font-weight: 600;
}

.drawer-footer :deep(.el-button--primary) {
  border-color: var(--cleaning-blue);
  background: var(--cleaning-blue);
}

.cleaning-overview :deep(.el-drawer) {
  box-shadow: none;
}

.cleaning-overview :deep(.el-drawer__header) {
  margin: 0;
  padding: 30px 34px 0;
  color: #252525;
  font-size: 18px;
  font-weight: 700;
}

.cleaning-overview :deep(.el-drawer__title) {
  color: #252525;
  font-size: 18px;
  font-weight: 700;
}

.cleaning-overview :deep(.el-drawer__close-btn) {
  color: #8b8b8b;
  font-size: 18px;
}

.cleaning-overview :deep(.el-drawer__body) {
  padding: 0;
}

.assign-drawer-content :deep(.el-form-item .el-time-select) {
  flex: 1;
}

@media (max-width: 768px) {
  .left-section,
  .right-section {
    flex-wrap: wrap;
  }

  .assign-drawer-content {
    padding: 48px 24px 28px;
  }

  .drawer-footer {
    gap: 12px;
  }
}
</style>
