<template>
  <div class="price-change-history">
    <!-- 筛选条件 -->
    <div class="filter-panel">
      <div class="filter-row">
        <!-- 操作日期 -->
        <div class="filter-group">
          <span class="filter-label">操作日期</span>
          <el-date-picker
            v-model="operateDateStart"
            type="date"
            placeholder="开始日期"
            format="YYYY/MM/DD"
            value-format="YYYY-MM-DD"
            size="default"
            style="width: 160px"
          />
          <span class="date-separator">至</span>
          <el-date-picker
            v-model="operateDateEnd"
            type="date"
            placeholder="结束日期"
            format="YYYY/MM/DD"
            value-format="YYYY-MM-DD"
            size="default"
            style="width: 160px"
          />
        </div>

        <!-- 本地房型 -->
        <div class="filter-group">
          <span class="filter-label">本地房型</span>
          <el-select
            v-model="filters.roomTypeId"
            placeholder="全部房型"
            clearable
            style="width: 180px"
          >
            <el-option label="全部房型" :value="null" />
            <el-option
              v-for="roomType in roomTypes"
              :key="roomType.id"
              :label="roomType.name"
              :value="roomType.id"
            />
          </el-select>
        </div>

        <!-- PMS价格计划 -->
        <div class="filter-group">
          <span class="filter-label">PMS价格计划</span>
          <el-select
            v-model="filters.pricePlanId"
            placeholder="全部价格计划"
            clearable
            style="width: 180px"
          >
            <el-option label="全部价格计划" :value="null" />
            <el-option
              v-for="plan in pricePlans"
              :key="plan.id"
              :label="plan.name"
              :value="plan.id"
            />
          </el-select>
        </div>

        <!-- 收起按钮 -->
        <el-link type="primary" :underline="false" @click="toggleFilters" class="toggle-link">
          {{ showAllFilters ? '收起' : '展开' }} <el-icon><ArrowUp v-if="showAllFilters" /><ArrowDown v-else /></el-icon>
        </el-link>
      </div>

      <!-- 第二行筛选(可展开/收起) -->
      <div v-if="showAllFilters" class="filter-row">
        <!-- 价格日期 -->
        <div class="filter-group">
          <span class="filter-label">价格日期</span>
          <el-date-picker
            v-model="priceDateStart"
            type="date"
            placeholder="开始日期"
            format="YYYY/MM/DD"
            value-format="YYYY-MM-DD"
            size="default"
            style="width: 160px"
          />
          <span class="date-separator">至</span>
          <el-date-picker
            v-model="priceDateEnd"
            type="date"
            placeholder="结束日期"
            format="YYYY/MM/DD"
            value-format="YYYY-MM-DD"
            size="default"
            style="width: 160px"
          />
        </div>

        <!-- 操作人 -->
        <div class="filter-group">
          <span class="filter-label">操作人</span>
          <el-select
            v-model="filters.operator"
            placeholder="全部"
            clearable
            style="width: 180px"
          >
            <el-option label="全部" :value="null" />
            <el-option label="系统" value="系统" />
          </el-select>
        </div>

        <!-- 更改项目 -->
        <div class="filter-group">
          <span class="filter-label">更改项目</span>
          <el-select
            v-model="filters.changeType"
            placeholder="价格"
            style="width: 180px"
          >
            <el-option label="价格" value="价格" />
          </el-select>
        </div>
      </div>
    </div>

    <!-- 数据表格 -->
    <div class="table-container">
      <el-table
        :data="tableData"
        border
        :header-cell-style="{ background: '#f5f7fa', color: '#606266' }"
        v-loading="loading"
        style="width: 100%"
      >
        <el-table-column prop="roomTypeName" label="本地房型" min-width="120" />
        <el-table-column prop="pricePlanName" label="PMS价格计划" min-width="150" />
        <el-table-column prop="priceDate" label="价格日期" min-width="180" />
        <el-table-column prop="applyDays" label="适用周几" width="100" />
        <el-table-column prop="changeType" label="调整类型" width="100" />
        <el-table-column prop="changeValue" label="调整记录" width="120">
          <template #default="{ row }">
            <span class="price-value">{{ row.changeValue }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="operator" label="操作人" width="100" />
        <el-table-column prop="operateTime" label="操作时间" min-width="180" />
      </el-table>

      <!-- 分页 -->
      <div class="pagination-container">
        <span class="pagination-total">共 {{ pagination.total }} 条</span>
        <el-pagination
          v-model:current-page="pagination.pageNum"
          v-model:page-size="pagination.pageSize"
          :page-sizes="[25, 50, 100, 200]"
          :total="pagination.total"
          layout="sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handlePageChange"
        />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, watch } from 'vue'
import { ArrowUp, ArrowDown } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { getAllRoomTypes } from '@/api/roomType'
import { getAllPricePlans } from '@/api/pricePlan'
import { getPriceChangeHistory, type PriceChangeHistoryDTO } from '@/api/roomPrice'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const showAllFilters = ref(false)
const loading = ref(false)

// 筛选条件
const operateDateStart = ref('')
const operateDateEnd = ref('')
const priceDateStart = ref('')
const priceDateEnd = ref('')

const filters = reactive({
  roomTypeId: null as number | null,
  pricePlanId: null as number | null,
  operator: null as string | null,
  changeType: '价格'
})

// 房型数据
const roomTypes = ref<any[]>([])

// 价格计划数据
const pricePlans = ref<any[]>([])

// 表格数据
const tableData = ref<PriceChangeHistoryDTO[]>([])

// 分页信息
const pagination = reactive({
  pageNum: 1,
  pageSize: 25,
  total: 0
})

// 加载房型列表
const loadRoomTypes = async () => {
  try {
    const userId = userStore.currentUser?.id
    if (!userId) return

    const response = await getAllRoomTypes()
    if (response.success && response.data) {
      roomTypes.value = response.data
    }
  } catch (error) {
    console.error('加载房型列表失败:', error)
  }
}

// 加载价格计划列表
const loadPricePlans = async () => {
  try {
    const userId = userStore.currentUser?.id
    if (!userId) return

    const response = await getAllPricePlans(userId)
    if (response && response.data) {
      pricePlans.value = response.data
    }
  } catch (error) {
    console.error('加载价格计划列表失败:', error)
  }
}

// 加载改价历史
const loadHistory = async () => {
  try {
    loading.value = true
    const response = await getPriceChangeHistory({
      operateDateStart: operateDateStart.value || undefined,
      operateDateEnd: operateDateEnd.value || undefined,
      priceDateStart: priceDateStart.value || undefined,
      priceDateEnd: priceDateEnd.value || undefined,
      roomTypeId: filters.roomTypeId || undefined,
      pricePlanId: filters.pricePlanId ? String(filters.pricePlanId) : undefined,
      operator: filters.operator || undefined,
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize
    })

    if (response.success && response.data) {
      tableData.value = response.data.records
      pagination.total = response.data.total
    }
  } catch (error) {
    console.error('加载改价历史失败:', error)
    ElMessage.error('加载改价历史失败')
  } finally {
    loading.value = false
  }
}

// 切换筛选器显示/隐藏
const toggleFilters = () => {
  showAllFilters.value = !showAllFilters.value
}

// 分页大小变化
const handleSizeChange = (newSize: number) => {
  pagination.pageSize = newSize
  pagination.pageNum = 1
  loadHistory()
}

// 页码变化
const handlePageChange = (newPage: number) => {
  pagination.pageNum = newPage
  loadHistory()
}

// 监听筛选条件变化
watch(
  [operateDateStart, operateDateEnd, priceDateStart, priceDateEnd, () => filters.roomTypeId, () => filters.pricePlanId, () => filters.operator],
  () => {
    pagination.pageNum = 1
    loadHistory()
  }
)

// 初始化
onMounted(() => {
  loadRoomTypes()
  loadPricePlans()
  loadHistory()
})
</script>

<style scoped>
.price-change-history {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: #fff;
}

/* 筛选面板 */
.filter-panel {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 16px 20px;
  border-bottom: 1px solid #e8e8e8;
  background: #fff;
}

.filter-row {
  display: flex;
  align-items: center;
  gap: 16px;
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

.date-separator {
  color: #606266;
  font-size: 14px;
  margin: 0 4px;
}

.toggle-link {
  margin-left: auto;
  display: flex;
  align-items: center;
  gap: 4px;
}

/* 表格容器 */
.table-container {
  flex: 1;
  padding: 20px;
  overflow: auto;
  display: flex;
  flex-direction: column;
}

/* 价格值样式 */
.price-value {
  font-weight: 500;
  color: #303133;
}

/* 分页容器 */
.pagination-container {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 20px;
  padding-top: 16px;
}

.pagination-total {
  font-size: 14px;
  color: #606266;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .filter-panel {
    padding: 12px;
  }

  .filter-row {
    flex-direction: column;
    align-items: stretch;
  }

  .filter-group {
    justify-content: space-between;
  }

  .toggle-link {
    margin-left: 0;
    justify-content: center;
  }
}
</style>
