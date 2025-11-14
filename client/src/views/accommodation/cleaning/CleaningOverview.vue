<template>
  <div class="cleaning-overview">
    <!-- 顶部操作栏 -->
    <div class="header-toolbar">
      <div class="left-section">
        <el-date-picker
          v-model="currentDate"
          type="date"
          placeholder="选择日期"
          format="YYYY/MM/DD"
          value-format="YYYY-MM-DD"
          @change="handleDateChange"
        />
        <el-button :icon="Refresh" @click="handleRefresh">刷新</el-button>
        <el-dropdown @command="handleFilter">
          <el-button>
            筛选 <el-icon class="el-icon--right"><ArrowDown /></el-icon>
          </el-button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="all">全部任务</el-dropdown-item>
              <el-dropdown-item command="pending">待分配</el-dropdown-item>
              <el-dropdown-item command="assigned">待清洁</el-dropdown-item>
              <el-dropdown-item command="in-progress">清洁中</el-dropdown-item>
              <el-dropdown-item command="completed">已完成</el-dropdown-item>
              <el-dropdown-item command="expired">已过期</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
      <div class="right-section">
        <el-button type="primary" @click="handleGenerateTask">生成任务</el-button>
        <el-button @click="handleSettings">设置</el-button>
      </div>
    </div>

    <!-- 日期导航 -->
    <div class="date-navigation">
      <el-button :icon="ArrowLeft" circle size="small" @click="handlePreviousWeek" />
      <div class="date-range">
        <span>{{ dateRangeText }}</span>
      </div>
      <el-button :icon="ArrowRight" circle size="small" @click="handleNextWeek" />
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
        <el-table-column label="房型" width="100" fixed>
          <template #default="{ row }">
            <span>{{ row.roomType }}</span>
          </template>
        </el-table-column>

        <!-- 房间号列 -->
        <el-table-column label="房间" width="80" fixed>
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
                <span v-if="task.status === 'expired'">已过期</span>
                <span v-else-if="task.status === 'pending'">待分配</span>
                <span v-else-if="task.status === 'assigned'">待接受</span>
                <span v-else-if="task.status === 'in_progress'">待打扫</span>
                <span v-else-if="task.status === 'completed'">已完成</span>
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
        <span>新增任务</span>
      </div>
      <div class="context-menu-item" @click="handleCancelMenu">
        <span>取消选择</span>
      </div>
    </div>

    <!-- 分配任务抽屉 -->
    <el-drawer v-model="assignDrawerVisible" title="分配任务" size="500px" direction="rtl">
      <div class="assign-drawer-content">
        <!-- 基本信息 -->
        <div class="section">
          <h3 class="section-title">基本信息</h3>
          <el-descriptions :column="2" border>
            <el-descriptions-item label="房型">{{ assignForm.roomType }}</el-descriptions-item>
            <el-descriptions-item label="房间">{{ assignForm.roomNumber }}</el-descriptions-item>
            <el-descriptions-item label="日期" :span="2">{{
              assignForm.taskDate
            }}</el-descriptions-item>
          </el-descriptions>
        </div>

        <!-- 任务详情 -->
        <div class="section">
          <h3 class="section-title">任务详情</h3>
          <el-form :model="assignForm" label-width="100px">
            <el-form-item label="任务类型">
              <span>{{ assignForm.taskType }}</span>
            </el-form-item>
            <el-form-item label="保洁员" required>
              <el-select
                v-model="assignForm.cleaner"
                placeholder="请选择"
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
            <el-form-item label="任务时间">
              <span>{{ assignForm.taskTime }}</span>
            </el-form-item>
            <el-form-item label="任务通知">
              <el-input
                v-model="assignForm.remark"
                type="textarea"
                :rows="4"
                placeholder="请输入"
                maxlength="200"
                show-word-limit
              />
            </el-form-item>
          </el-form>
        </div>

        <!-- 底部按钮 -->
        <div class="drawer-footer">
          <el-button @click="handleCancelAssign" size="large" style="flex: 1">取消任务</el-button>
          <el-button type="primary" @click="handleConfirmAssign" size="large" style="flex: 1">
            确定
          </el-button>
        </div>
      </div>
    </el-drawer>

    <!-- 新增任务抽屉 -->
    <el-drawer v-model="createDrawerVisible" title="新增任务" size="500px" direction="rtl">
      <div class="assign-drawer-content">
        <!-- 基本信息 -->
        <div class="section">
          <h3 class="section-title">基本信息</h3>
          <el-descriptions :column="2" border>
            <el-descriptions-item label="房型">{{ createForm.roomType }}</el-descriptions-item>
            <el-descriptions-item label="房间">{{ createForm.roomNumber }}</el-descriptions-item>
            <el-descriptions-item label="日期" :span="2">{{
              createForm.taskDate
            }}</el-descriptions-item>
          </el-descriptions>
        </div>

        <!-- 任务详情 -->
        <div class="section">
          <h3 class="section-title">任务详情</h3>
          <el-form :model="createForm" label-width="80px">
            <el-form-item label="任务类型" required>
              <el-select
                v-model="createForm.taskType"
                placeholder="请选择"
                style="width: 100%"
              >
                <el-option label="退房" value="checkout" />
                <el-option label="日常清洁" value="daily" />
                <el-option label="深度清洁" value="deep" />
              </el-select>
            </el-form-item>
            <el-form-item label="保洁员">
              <el-select
                v-model="createForm.cleaner"
                placeholder="请选择"
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
            <el-form-item label="任务时间" required>
              <div style="display: flex; align-items: center; gap: 8px; width: 100%">
                <el-time-select
                  v-model="createForm.startTime"
                  placeholder="Start time"
                  start="06:00"
                  step="00:30"
                  end="22:00"
                  style="flex: 1"
                />
                <span>To</span>
                <el-time-select
                  v-model="createForm.endTime"
                  placeholder="End time"
                  start="06:00"
                  step="00:30"
                  end="22:00"
                  :min-time="createForm.startTime"
                  style="flex: 1"
                />
              </div>
            </el-form-item>
            <el-form-item label="任务通知">
              <el-input
                v-model="createForm.remark"
                type="textarea"
                :rows="4"
                placeholder="请输入"
                maxlength="200"
                show-word-limit
              />
            </el-form-item>
          </el-form>
        </div>

        <!-- 底部按钮 -->
        <div class="drawer-footer">
          <el-button @click="handleCancelCreate" size="large" style="flex: 1">取消</el-button>
          <el-button type="primary" @click="handleConfirmCreate" size="large" style="flex: 1">
            确定
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
import {
  getCalendarViewData,
  getCleaners,
  assignCleaningTask,
  createCleaningTask,
  type CleaningTaskDTO,
  type CleanerDTO,
  type CleaningTaskCreateDTO,
} from '@/api/cleaning'
import { getRooms, type RoomDTO } from '@/api/room'

// 当前日期
const currentDate = ref(new Date().toISOString().split('T')[0])

// 日期范围文本
const dateRangeText = computed(() => {
  const start = dateColumns.value[0]?.date || ''
  const end = dateColumns.value[dateColumns.value.length - 1]?.date || ''
  return `${start.replace(/-/g, '/')} - ${end.replace(/-/g, '/')}`
})

// 生成日期列(显示10天)
const dateColumns = computed(() => {
  const columns = []
  const realToday = new Date().toISOString().split('T')[0] // 实际的今天
  const viewDate = new Date(currentDate.value) // 视图中心日期
  const startDate = new Date(viewDate)
  startDate.setDate(viewDate.getDate() - 2) // 从视图中心日期前2天开始

  for (let i = 0; i < 10; i++) {
    const date = new Date(startDate)
    date.setDate(startDate.getDate() + i)
    const dateStr = date.toISOString().split('T')[0]
    const month = date.getMonth() + 1
    const day = date.getDate()
    const weekDays = ['日', '一', '二', '三', '四', '五', '六']
    const weekDay = weekDays[date.getDay()]
    const isWeekend = date.getDay() === 0 || date.getDay() === 6
    const isToday = dateStr === realToday

    // 如果是今天,显示"今天",否则显示日期
    let label = ''
    if (isToday) {
      label = '今天'
    } else {
      label = `${month}/${String(day).padStart(2, '0')}(周${weekDay})`
    }

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
    ElMessage.error(error.message || '获取保洁员列表失败')
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
      ElMessage.error(response.message || '获取任务数据失败')
    }
  } catch (error: any) {
    console.error('获取任务数据失败:', error)
    ElMessage.error(error.message || '获取任务数据失败')
  } finally {
    loading.value = false
  }
}

// 获取状态标签
const getStatusLabel = (status: string) => {
  const labelMap: Record<string, string> = {
    expired: '已过期',
    pending: '待分配',
    assigned: '待接受',
    in_progress: '待打扫',
    completed: '已完成',
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
  const today = new Date().toISOString().split('T')[0]
  const clickDate = date.date

  // 判断是否是历史日期
  if (clickDate < today && cellTasks.length === 0) {
    ElMessage.warning('历史日期不支持添加保洁任务')
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
        taskType: getTaskTypeText(task.type),
        taskDate: formatDateDisplay(date.date),
        taskTime: '10:00-16:00',
        cleaner: undefined,
        remark: '',
      }
      assignDrawerVisible.value = true
    } else if (task.status === 'expired') {
      ElMessage.info('该任务已过期')
    } else {
      ElMessage.info('该任务已分配')
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
  const today = new Date().toISOString().split('T')[0]
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
    daily: '日常清洁',
    checkout: '退房',
    deep: '深度清洁',
  }
  return typeMap[type] || type
}

// 格式化日期显示
const formatDateDisplay = (dateStr: string) => {
  const date = new Date(dateStr)
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
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
    ElMessage.warning('请选择保洁员')
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
        tasks.value[taskIndex].label = '待接受'
      }

      ElMessage.success('分配成功')
      assignDrawerVisible.value = false

      // 重新加载任务数据以确保与后端同步
      console.log('重新加载任务数据...')
      await loadTasks()
    } else {
      console.error('分配失败:', response.message)
      ElMessage.error(response.message || '分配失败')
    }
  } catch (error: any) {
    console.error('分配任务失败:', error)
    ElMessage.error(error.message || '分配任务失败')
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
    ElMessage.warning('请选择任务类型')
    return
  }

  if (!createForm.value.startTime || !createForm.value.endTime) {
    ElMessage.warning('请选择任务时间')
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
      ElMessage.success('任务创建成功')
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
      ElMessage.error(response.message || '创建任务失败')
    }
  } catch (error: any) {
    console.error('创建任务失败:', error)
    ElMessage.error(error.message || '创建任务失败')
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
  ElMessage.success('刷新成功')
}

// 筛选
const handleFilter = (command: string) => {
  currentFilter.value = command
  loadTasks()
}

// 生成任务
const handleGenerateTask = () => {
  console.log('生成任务')
  // TODO: 打开生成任务对话框
}

// 设置
const handleSettings = () => {
  console.log('打开设置')
  // TODO: 打开设置对话框
}

// 上一周
const handlePreviousWeek = () => {
  const date = new Date(currentDate.value)
  date.setDate(date.getDate() - 7)
  currentDate.value = date.toISOString().split('T')[0]
  loadTasks()
}

// 下一周
const handleNextWeek = () => {
  const date = new Date(currentDate.value)
  date.setDate(date.getDate() + 7)
  currentDate.value = date.toISOString().split('T')[0]
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
.date-navigation {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16px;
  margin-bottom: 16px;
  padding: 12px;
  background: #fff;
  border-radius: 4px;
}

.date-range {
  font-size: 14px;
  font-weight: 500;
  color: #333;
  min-width: 200px;
  text-align: center;
}

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
</style>
