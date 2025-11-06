<template>
  <div class="housekeeping-list">
    <!-- 搜索栏 -->
    <div class="search-bar">
      <el-input
        v-model="searchQuery"
        placeholder="搜索房型房间、保洁员"
        class="search-input"
        clearable
      >
        <template #suffix>
          <el-icon class="search-icon"><Search /></el-icon>
        </template>
      </el-input>

      <el-date-picker
        v-model="selectedDate"
        type="date"
        placeholder="日期选择"
        format="YYYY-MM-DD"
        value-format="YYYY-MM-DD"
        class="date-picker"
      />

      <el-select
        v-model="selectedHousekeeper"
        placeholder="保洁员"
        clearable
        class="housekeeper-select"
      >
        <el-option label="全部" value="" />
        <el-option
          v-for="housekeeper in housekeepers"
          :key="housekeeper.id"
          :label="housekeeper.name"
          :value="housekeeper.id"
        />
      </el-select>

      <el-button class="status-btn">全部</el-button>
    </div>

    <!-- 筛选栏 -->
    <div class="filter-bar">
      <div class="filter-left">
        <el-select
          v-model="filters.cleaningStatus"
          placeholder="保洁状态"
          clearable
          class="filter-select"
        >
          <el-option label="全部" value="" />
          <el-option label="未开始" value="not_started" />
          <el-option label="进行中" value="in_progress" />
          <el-option label="已完成" value="completed" />
        </el-select>

        <el-select
          v-model="filters.roomType"
          placeholder="房间类型"
          clearable
          class="filter-select"
        >
          <el-option label="全部" value="" />
        </el-select>

        <el-select
          v-model="filters.roomGroup"
          placeholder="房间分组"
          clearable
          class="filter-select"
        >
          <el-option label="全部" value="" />
        </el-select>

        <el-select
          v-model="filters.auditStatus"
          placeholder="审核状态"
          clearable
          class="filter-select"
        >
          <el-option label="全部" value="" />
        </el-select>
      </div>

      <div class="filter-right">
        <el-button link type="primary">收起筛选 <el-icon><ArrowUp /></el-icon></el-button>
        <el-button type="primary" size="small">导出明细</el-button>
        <el-button type="primary" size="small">添加任务</el-button>
      </div>
    </div>

    <!-- 数据表格 -->
    <el-table
      :data="housekeepingList"
      style="width: 100%"
      empty-text="暂无数据"
      @selection-change="handleSelectionChange"
    >
      <el-table-column type="selection" width="55" />
      <el-table-column prop="timeSlot" label="时段" width="80" />
      <el-table-column prop="roomType" label="房型" width="120" />
      <el-table-column prop="roomNumber" label="房间" width="100" />
      <el-table-column prop="roomGroup" label="房间分组" width="120" />
      <el-table-column prop="checkoutTime" label="退房时间" width="180" />
      <el-table-column prop="housekeeper" label="保洁员" width="150" />
      <el-table-column prop="cleaningStatus" label="保洁状态" width="100">
        <template #default="{ row }">
          <el-tag :type="getStatusType(row.cleaningStatus)">
            {{ row.cleaningStatus }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="250" fixed="right">
        <template #default>
          <el-button link type="primary" size="small">通知保洁员</el-button>
          <el-button link type="primary" size="small">编辑</el-button>
          <el-button link type="danger" size="small">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 批量操作栏和分页 -->
    <div class="table-footer">
      <div class="batch-actions">
        <el-checkbox v-model="selectAll" @change="handleSelectAll">全选</el-checkbox>
        <span class="selected-count">已选 {{ selectedItems.length }} 条</span>
        <el-button size="small">批量通知保洁员</el-button>
        <el-button size="small">批量调整保洁状态</el-button>
      </div>

      <el-pagination
        v-model:current-page="pagination.current"
        v-model:page-size="pagination.size"
        :total="pagination.total"
        :page-sizes="[25, 50, 100]"
        layout="total, sizes, prev, pager, next"
        @current-change="handlePageChange"
        @size-change="handleSizeChange"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { Search, ArrowUp } from '@element-plus/icons-vue'

// 搜索和筛选
const searchQuery = ref('')
const selectedDate = ref('')
const selectedHousekeeper = ref('')

// 保洁员列表
const housekeepers = ref<any[]>([
  { id: 1, name: '小林' },
  { id: 2, name: 'll' }
])

// 筛选条件
const filters = ref({
  cleaningStatus: '',
  roomType: '',
  roomGroup: '',
  auditStatus: ''
})

// 房务列表数据
const housekeepingList = ref<any[]>([
  {
    id: 1,
    timeSlot: '-',
    roomType: '大床房',
    roomNumber: 'a01',
    roomGroup: '未分组房间',
    checkoutTime: '2025-10-01 12:00:00',
    housekeeper: '小林、ll',
    cleaningStatus: '未开始'
  }
])

// 批量选择
const selectAll = ref(false)
const selectedItems = ref<any[]>([])

// 分页
const pagination = ref({
  current: 1,
  size: 25,
  total: 1
})

// 处理选择变化
const handleSelectionChange = (selection: any[]) => {
  selectedItems.value = selection
  selectAll.value = selection.length === housekeepingList.value.length
}

// 处理全选
const handleSelectAll = (checked: boolean) => {
  // 这里需要手动触发表格的全选/取消全选
  console.log('全选状态:', checked)
}

// 分页事件
const handlePageChange = (page: number) => {
  pagination.value.current = page
}

const handleSizeChange = (size: number) => {
  pagination.value.size = size
}

// 状态类型转换
const getStatusType = (status: string) => {
  const typeMap: Record<string, string> = {
    未开始: 'info',
    进行中: 'warning',
    已完成: 'success'
  }
  return typeMap[status] || 'info'
}
</script>

<style scoped>
.housekeeping-list {
  padding: 20px;
  background: #fff;
}

/* 搜索栏 */
.search-bar {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
  align-items: center;
}

.search-input {
  width: 300px;
}

.date-picker {
  width: 200px;
}

.housekeeper-select {
  width: 150px;
}

.status-btn {
  min-width: 80px;
}

/* 筛选栏 */
.filter-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  padding: 12px 0;
  border-top: 1px solid #f0f0f0;
  border-bottom: 1px solid #f0f0f0;
}

.filter-left {
  display: flex;
  gap: 12px;
}

.filter-select {
  width: 150px;
}

.filter-right {
  display: flex;
  gap: 12px;
  align-items: center;
}

/* 表格底部 */
.table-footer {
  margin-top: 16px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 16px;
  border-top: 1px solid #f0f0f0;
}

.batch-actions {
  display: flex;
  gap: 16px;
  align-items: center;
}

.selected-count {
  font-size: 14px;
  color: #666;
}

/* 响应式 */
@media (max-width: 1400px) {
  .search-bar {
    flex-wrap: wrap;
  }

  .filter-bar {
    flex-direction: column;
    gap: 12px;
    align-items: flex-start;
  }

  .filter-left,
  .filter-right {
    width: 100%;
  }

  .filter-right {
    justify-content: flex-end;
  }
}
</style>
