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
            <el-option label="清洁中" value="in-progress" />
            <el-option label="已完成" value="completed" />
          </el-select>

          <span class="filter-label">房型</span>
          <el-select v-model="filters.roomType" placeholder="请选择" clearable class="filter-select">
            <el-option label="全部" value="" />
            <el-option label="要町201" value="要町201" />
            <el-option label="要町401" value="要町401" />
            <el-option label="要町403" value="要町403" />
            <el-option label="束十条1F" value="束十条1F" />
            <el-option label="束十条2F" value="束十条2F" />
            <el-option label="束十条3/4F" value="束十条3/4F" />
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
      <el-table :data="taskList" border style="width: 100%">
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
      <el-descriptions :column="2" border>
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
      <template #footer>
        <el-button @click="detailDialogVisible = false">关闭</el-button>
        <el-button
          v-if="taskDetail.taskStatus === 'pending'"
          type="primary"
          @click="handleAssignTask"
        >
          分配任务
        </el-button>
        <el-button
          v-if="
            taskDetail.taskStatus === 'assigned' || taskDetail.taskStatus === 'in-progress'
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
import { ref } from 'vue'
import { Search } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

// 是否收起筛选
const isCollapsed = ref(false)

// 筛选条件
const filters = ref({
  search: '',
  taskType: '',
  status: '',
  roomType: '',
  startDate: '2025-11-08',
  endDate: '2025-11-08',
})

// 任务列表数据
const taskList = ref([
  {
    taskDate: '2025/11/08',
    roomType: '束十条3/4F',
    roomNumber: '301/401',
    taskStatus: 'expired',
    taskType: '退房',
    taskTime: '10:00-16:00',
    cleaner: '',
    approver: '',
    createTime: '2025-11-08 08:00:00',
    completeTime: '',
    remark: '',
  },
])

// 分页
const pagination = ref({
  current: 1,
  size: 10,
  total: 1,
})

// 详情对话框
const detailDialogVisible = ref(false)
const taskDetail = ref<any>({})

// 获取状态类型
const getStatusType = (status: string) => {
  const typeMap: Record<string, string> = {
    expired: 'info',
    pending: 'warning',
    assigned: 'primary',
    'in-progress': 'primary',
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
    'in-progress': '清洁中',
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
    'status-in-progress': status === 'in-progress',
    'status-completed': status === 'completed',
  }
}

// 收起/展开筛选
const handleCollapse = () => {
  isCollapsed.value = !isCollapsed.value
}

// 导出明细
const handleExport = () => {
  ElMessage.success('导出成功')
  // TODO: 实现导出逻辑
}

// 查看详情
const handleViewDetail = (row: any) => {
  taskDetail.value = { ...row }
  detailDialogVisible.value = true
}

// 分配任务
const handleAssignTask = () => {
  ElMessage.success('分配成功')
  detailDialogVisible.value = false
  // TODO: 实现分配逻辑
}

// 完成任务
const handleCompleteTask = () => {
  ElMessage.success('任务已完成')
  detailDialogVisible.value = false
  // TODO: 实现完成逻辑
}

// 分页事件
const handlePageChange = (page: number) => {
  pagination.value.current = page
  // TODO: 加载数据
}

const handleSizeChange = (size: number) => {
  pagination.value.size = size
  pagination.value.current = 1
  // TODO: 加载数据
}
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
