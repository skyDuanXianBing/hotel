<template>
  <div class="housekeeping-list">
    <div class="search-bar">
      <el-input
        v-model="searchQuery"
        :placeholder="t('pages.housekeepingList.searchPlaceholder')"
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
        :placeholder="t('pages.housekeepingList.datePlaceholder')"
        format="YYYY-MM-DD"
        value-format="YYYY-MM-DD"
        class="date-picker"
      />

      <el-select
        v-model="selectedHousekeeper"
        :placeholder="t('pages.housekeepingList.housekeeperPlaceholder')"
        clearable
        class="housekeeper-select"
      >
        <el-option :label="t('pages.housekeepingList.all')" value="" />
        <el-option
          v-for="housekeeper in housekeepers"
          :key="housekeeper.id"
          :label="housekeeper.name"
          :value="housekeeper.id"
        />
      </el-select>

      <el-button class="status-btn">{{ t('pages.housekeepingList.all') }}</el-button>
    </div>

    <div class="filter-bar">
      <div class="filter-left">
        <el-select
          v-model="filters.cleaningStatus"
          :placeholder="t('pages.housekeepingList.filters.cleaningStatus')"
          clearable
          class="filter-select"
        >
          <el-option :label="t('pages.housekeepingList.all')" value="" />
          <el-option
            :label="t('pages.housekeepingList.statuses.notStarted')"
            value="not_started"
          />
          <el-option
            :label="t('pages.housekeepingList.statuses.inProgress')"
            value="in_progress"
          />
          <el-option
            :label="t('pages.housekeepingList.statuses.completed')"
            value="completed"
          />
        </el-select>

        <el-select
          v-model="filters.roomType"
          :placeholder="t('pages.housekeepingList.filters.roomType')"
          clearable
          class="filter-select"
        >
          <el-option :label="t('pages.housekeepingList.all')" value="" />
        </el-select>

        <el-select
          v-model="filters.roomGroup"
          :placeholder="t('pages.housekeepingList.filters.roomGroup')"
          clearable
          class="filter-select"
        >
          <el-option :label="t('pages.housekeepingList.all')" value="" />
        </el-select>

        <el-select
          v-model="filters.auditStatus"
          :placeholder="t('pages.housekeepingList.filters.auditStatus')"
          clearable
          class="filter-select"
        >
          <el-option :label="t('pages.housekeepingList.all')" value="" />
        </el-select>
      </div>

      <div class="filter-right">
        <el-button link type="primary">
          {{ t('pages.housekeepingList.collapseFilters') }}
          <el-icon><ArrowUp /></el-icon>
        </el-button>
        <el-button type="primary" size="small">
          {{ t('pages.housekeepingList.exportDetails') }}
        </el-button>
        <el-button type="primary" size="small">{{ t('pages.housekeepingList.addTask') }}</el-button>
      </div>
    </div>

    <el-table
      :data="housekeepingList"
      style="width: 100%"
      :empty-text="t('pages.housekeepingList.empty')"
      @selection-change="handleSelectionChange"
    >
      <el-table-column type="selection" width="55" />
      <el-table-column prop="timeSlot" :label="t('pages.housekeepingList.columns.timeSlot')" width="80" />
      <el-table-column prop="roomType" :label="t('pages.housekeepingList.columns.roomType')" width="120" />
      <el-table-column prop="roomNumber" :label="t('pages.housekeepingList.columns.roomNumber')" width="100" />
      <el-table-column prop="roomGroup" :label="t('pages.housekeepingList.columns.roomGroup')" width="120" />
      <el-table-column prop="checkoutTime" :label="t('pages.housekeepingList.columns.checkoutTime')" width="180" />
      <el-table-column prop="housekeeper" :label="t('pages.housekeepingList.columns.housekeeper')" width="150" />
      <el-table-column
        prop="cleaningStatus"
        :label="t('pages.housekeepingList.columns.cleaningStatus')"
        width="120"
      >
        <template #default="{ row }">
          <el-tag :type="getStatusType(row.cleaningStatus)">
            {{ getStatusLabel(row.cleaningStatus) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column :label="t('pages.housekeepingList.columns.actions')" width="250" fixed="right">
        <template #default>
          <el-button link type="primary" size="small">
            {{ t('pages.housekeepingList.actions.notify') }}
          </el-button>
          <el-button link type="primary" size="small">{{ t('pages.housekeepingList.actions.edit') }}</el-button>
          <el-button link type="danger" size="small">{{ t('pages.housekeepingList.actions.delete') }}</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="table-footer">
      <div class="batch-actions">
        <el-checkbox v-model="selectAll" @change="handleSelectAll">
          {{ t('pages.housekeepingList.selectAll') }}
        </el-checkbox>
        <span class="selected-count">
          {{ t('pages.housekeepingList.selectedCount', { count: selectedItems.length }) }}
        </span>
        <el-button size="small">{{ t('pages.housekeepingList.batchNotify') }}</el-button>
        <el-button size="small">{{ t('pages.housekeepingList.batchUpdateStatus') }}</el-button>
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
import { useI18n } from 'vue-i18n'
import { ArrowUp, Search } from '@element-plus/icons-vue'

const { t } = useI18n()

const searchQuery = ref('')
const selectedDate = ref('')
const selectedHousekeeper = ref('')

const housekeepers = ref([
  { id: 1, name: t('pages.housekeepingList.mock.housekeeperA') },
  { id: 2, name: t('pages.housekeepingList.mock.housekeeperB') },
])

const filters = ref({
  cleaningStatus: '',
  roomType: '',
  roomGroup: '',
  auditStatus: '',
})

const housekeepingList = ref([
  {
    id: 1,
    timeSlot: '-',
    roomType: t('pages.housekeepingList.mock.roomType'),
    roomNumber: 'a01',
    roomGroup: t('pages.housekeepingList.mock.roomGroup'),
    checkoutTime: '2025-10-01 12:00:00',
    housekeeper: t('pages.housekeepingList.mock.joinedHousekeepers'),
    cleaningStatus: 'not_started',
  },
])

const selectAll = ref(false)
const selectedItems = ref<any[]>([])

const pagination = ref({
  current: 1,
  size: 25,
  total: 1,
})

const handleSelectionChange = (selection: any[]) => {
  selectedItems.value = selection
  selectAll.value = selection.length === housekeepingList.value.length
}

const handleSelectAll = (checked: boolean) => {
  console.log('select all status:', checked)
}

const handlePageChange = (page: number) => {
  pagination.value.current = page
}

const handleSizeChange = (size: number) => {
  pagination.value.size = size
}

const getStatusType = (status: string) => {
  const typeMap: Record<string, string> = {
    not_started: 'info',
    in_progress: 'warning',
    completed: 'success',
  }
  return typeMap[status] || 'info'
}

const getStatusLabel = (status: string) => {
  const labelMap: Record<string, string> = {
    not_started: t('pages.housekeepingList.statuses.notStarted'),
    in_progress: t('pages.housekeepingList.statuses.inProgress'),
    completed: t('pages.housekeepingList.statuses.completed'),
  }
  return labelMap[status] || status
}
</script>

<style scoped>
.housekeeping-list {
  padding: 20px;
  background: #fff;
}

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
