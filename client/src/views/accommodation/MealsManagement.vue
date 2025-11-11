<template>
  <div class="meals-management-container">
    <!-- 搜索栏 -->
    <div class="search-bar">
      <el-input
        v-model="searchKeyword"
        placeholder="请输入姓名、通道订单号、房间号、长住、手机号"
        clearable
        class="search-input"
      >
        <template #suffix>
          <el-icon class="search-icon"><Search /></el-icon>
        </template>
      </el-input>
    </div>

    <!-- 筛选器 -->
    <div class="filter-bar">
      <div class="filter-group">
        <label class="filter-label">房间状态</label>
        <el-select v-model="roomStatus" placeholder="全部" clearable class="filter-select">
          <el-option label="全部" value="" />
          <el-option label="在住" value="occupied" />
          <el-option label="预订" value="reserved" />
          <el-option label="空房" value="vacant" />
        </el-select>
      </div>

      <div class="filter-group">
        <label class="filter-label">餐桌名称</label>
        <el-select v-model="tableName" placeholder="全部" clearable class="filter-select">
          <el-option label="全部" value="" />
        </el-select>
      </div>

      <div class="filter-group">
        <label class="filter-label">使用日期</label>
        <el-date-picker
          v-model="startDate"
          type="date"
          placeholder="选择日期"
          format="YYYY/MM/DD"
          value-format="YYYY/MM/DD"
          class="filter-date"
        />
        <span class="date-separator">至</span>
        <el-date-picker
          v-model="endDate"
          type="date"
          placeholder="选择日期"
          format="YYYY/MM/DD"
          value-format="YYYY/MM/DD"
          class="filter-date"
        />
      </div>

      <div class="filter-group">
        <label class="filter-label">档期托管</label>
        <el-select v-model="periodManagement" placeholder="全部" clearable class="filter-select">
          <el-option label="全部" value="" />
        </el-select>
      </div>

      <div class="filter-group">
        <label class="filter-label">渠道</label>
        <el-select v-model="channel" placeholder="全部" clearable class="filter-select">
          <el-option label="全部" value="" />
        </el-select>
      </div>

      <el-button type="primary" class="search-btn">搜索</el-button>
    </div>

    <!-- 数据表格 -->
    <div class="table-container">
      <el-table
        :data="mealsData"
        border
        stripe
        class="meals-table"
        empty-text="暂无数据"
        height="calc(100vh - 320px)"
      >
        <el-table-column type="index" label="订单号/通道订单号" width="180" align="center" fixed>
          <template #default="{ row }">
            <div>{{ row.orderId }}</div>
            <div class="sub-text">{{ row.channelOrderId }}</div>
          </template>
        </el-table-column>
        <el-table-column prop="roomNo" label="房间号" width="100" align="center" />
        <el-table-column prop="tableName" label="餐桌名称" width="120" align="center" />
        <el-table-column prop="guestName" label="订单状态" width="100" align="center" />
        <el-table-column prop="checkIn" label="已到店" width="100" align="center" />
        <el-table-column prop="nights" label="已结账" width="100" align="center" />
        <el-table-column prop="totalPrice" label="联系人" width="120" align="center" />
        <el-table-column prop="channel" label="手机号" width="120" align="center" />
        <el-table-column prop="guestCount" label="入住客数" width="100" align="center" />
        <el-table-column prop="channel" label="通道" width="120" align="center" />
        <el-table-column prop="deposit" label="订单备注" width="150" align="center" />
        <el-table-column prop="deposit" label="志愿" width="100" align="center" />
        <el-table-column label="操作" width="100" align="center" fixed="right">
          <template #default="{ row }">
            <el-button type="text" size="small">详情</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { Search } from '@element-plus/icons-vue'

// 搜索关键字
const searchKeyword = ref('')

// 筛选条件
const roomStatus = ref('')
const tableName = ref('')
const startDate = ref('2025/11/08')
const endDate = ref('2025/11/08')
const periodManagement = ref('')
const channel = ref('')

// 餐食数据
interface MealItem {
  orderId: string
  channelOrderId: string
  roomNo: string
  tableName: string
  guestName: string
  checkIn: string
  nights: number
  totalPrice: number
  channel: string
  guestCount: number
  deposit: number
}

const mealsData = ref<MealItem[]>([])
</script>

<style scoped>
.meals-management-container {
  padding: 24px;
  background: #fff;
  height: 100%;
}

.search-bar {
  margin-bottom: 24px;
}

.search-input {
  max-width: 600px;
}

.search-icon {
  color: #909399;
}

.filter-bar {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 24px;
  flex-wrap: wrap;
}

.filter-group {
  display: flex;
  align-items: center;
  gap: 8px;
}

.filter-label {
  font-size: 14px;
  color: #606266;
  white-space: nowrap;
}

.filter-select {
  width: 120px;
}

.filter-date {
  width: 150px;
}

.date-separator {
  color: #909399;
  font-size: 14px;
}

.search-btn {
  margin-left: auto;
}

.table-container {
  overflow: auto;
}

.meals-table {
  width: 100%;
}

.sub-text {
  color: #909399;
  font-size: 12px;
  margin-top: 4px;
}

.meals-table :deep(.el-table__header th) {
  background-color: #f5f7fa;
  color: #606266;
  font-weight: 600;
}

.meals-table :deep(.el-table__row:hover) {
  background-color: #f5f7fa;
}
</style>
