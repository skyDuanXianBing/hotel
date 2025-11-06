<template>
  <div class="price-management">
    <!-- 顶部导航栏 -->
    <div class="top-nav">
      <div class="nav-tabs">
        <el-button
          :type="activeTab === 'price-setting' ? 'primary' : 'default'"
          @click="switchTab('price-setting')"
        >
          按来源查看价格
        </el-button>
        <el-button
          :type="activeTab === 'price-view' ? 'primary' : 'default'"
          @click="switchTab('price-view')"
        >
          按渠道查看价格
        </el-button>
      </div>

      <div class="nav-actions">
        <span class="system-label">系统</span>
        <el-button type="primary">门市价</el-button>
      </div>
    </div>

    <!-- 控制面板 -->
    <div class="control-panel">
      <!-- 日期选择 -->
      <div class="control-group">
        <span class="control-label">日期</span>
        <div class="date-control">
          <el-button icon="ArrowLeft" @click="previousDate" :disabled="loading" />
          <el-date-picker
            v-model="selectedDate"
            type="date"
            placeholder="选择日期"
            format="YYYY-MM-DD 周一"
            value-format="YYYY-MM-DD"
            @change="onDateChange"
            :disabled="loading"
          />
          <el-button icon="ArrowRight" @click="nextDate" :disabled="loading" />
        </div>
      </div>

      <!-- 房型筛选 -->
      <div class="control-group">
        <span class="control-label">房型筛选</span>
        <el-select
          v-model="roomTypeFilter"
          placeholder="全部房型"
          @change="onRoomTypeChange"
          :disabled="loading"
          class="room-type-select"
        >
          <el-option label="全部房型" value="all" />
          <el-option 
            v-for="roomType in roomTypes" 
            :key="roomType.id" 
            :label="roomType.name" 
            :value="roomType.id.toString()" 
          />
        </el-select>
      </div>

      <!-- 显示库存按钮 -->
      <div class="control-group">
        <el-switch
          v-model="showInventory"
          active-text="显示库存"
          @change="onShowInventoryChange"
          :disabled="loading"
        />
      </div>
    </div>

    <!-- 房价表格 -->
    <div class="price-table-container">
      <el-table
        :data="priceTableData"
        border
        v-loading="loading"
        element-loading-text="加载中..."
        :header-cell-style="{ background: '#f5f7fa', color: '#606266', textAlign: 'center' }"
        :cell-style="{ textAlign: 'center' }"
      >
        <!-- 房型列 -->
        <el-table-column prop="roomTypeName" label="本地房型" width="180" fixed="left">
          <template #default="scope">
            <span class="room-type-name">{{ scope.row.roomTypeName }}</span>
          </template>
        </el-table-column>

        <!-- 动态日期列 -->
        <el-table-column
          v-for="date in dateColumns"
          :key="date.date"
          :label="date.label"
          :prop="date.prop"
          width="130"
        >
          <template #header>
            <div class="date-header">
              <div class="date-label" :class="date.dayClass">{{ date.dayLabel }}</div>
              <div class="date-day">{{ date.weekday }}</div>
              <div v-if="showInventory" class="total-rooms">
                共{{ dailyTotalRooms[date.prop] || 0 }}间
              </div>
            </div>
          </template>

          <template #default="scope">
            <div
              class="price-cell"
              :class="{
                'has-special': scope.row[date.prop]?.hasSpecial,
                clickable: scope.row[date.prop]?.price,
              }"
              @click="openPriceEditor(scope.row, date)"
            >
              <div v-if="scope.row[date.prop]?.price" class="price-value">
                ¥{{ scope.row[date.prop].price }}
              </div>
              <div v-else class="no-price">-</div>
              <div
                v-if="showInventory && scope.row[date.prop]?.availableRooms"
                class="rooms-available"
              >
                剩{{ scope.row[date.prop].availableRooms }}间
              </div>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 价格编辑侧边栏 -->
    <el-drawer
      v-model="showPriceEditor"
      title="修改价格"
      direction="rtl"
      size="400px"
      :before-close="closePriceEditor"
    >
      <div class="price-editor">
        <!-- 房型信息 -->
        <div class="room-info">
          <el-tag type="info">全部房型</el-tag>
          <span class="room-name">{{ editingPrice.roomType }}</span>
        </div>

        <!-- 改价日期 -->
        <div class="form-item">
          <label class="form-label"> <span class="required">*</span>改价日期 </label>
          <div class="date-range">
            <el-date-picker
              v-model="editingPrice.startDate"
              type="date"
              placeholder="开始日期"
              format="YYYY-MM-DD"
              value-format="YYYY-MM-DD"
              style="width: 120px"
            />
            <span class="date-separator">至</span>
            <el-date-picker
              v-model="editingPrice.endDate"
              type="date"
              placeholder="结束日期"
              format="YYYY-MM-DD"
              value-format="YYYY-MM-DD"
              style="width: 120px"
            />
          </div>
        </div>

        <!-- 门市价 -->
        <div class="form-item">
          <label class="form-label"> <span class="required">*</span>门市价 </label>
          <div class="price-input">
            <span class="currency">￥</span>
            <el-input
              v-model.number="editingPrice.newPrice"
              type="number"
              placeholder="请输入价格"
              :min="0"
              :precision="2"
            />
          </div>
        </div>

        <!-- 操作按钮 -->
        <div class="action-buttons">
          <el-button @click="closePriceEditor">取消</el-button>
          <el-button type="primary" @click="savePriceChange" :loading="loading"> 保存 </el-button>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { ArrowLeft, ArrowRight } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { getAllRoomTypes, type RoomTypeDTO } from '@/api/roomType'
import { 
  getRoomPrices, 
  updateRoomPrice, 
  buildRoomPriceTableData,
  type RoomPriceDTO,
  type UpdateRoomPriceRequest,
  type RoomPriceTableData 
} from '@/api/roomPrice'

// 响应式数据
const activeTab = ref('price-setting')
const selectedDate = ref(new Date().toISOString().split('T')[0])
const roomTypeFilter = ref('all')
const showInventory = ref(false)
const loading = ref(false)

// 数据存储
const roomTypes = ref<RoomTypeDTO[]>([])
const roomPrices = ref<RoomPriceDTO[]>([])

// 价格编辑相关数据
const showPriceEditor = ref(false)
const editingPrice = ref({
  roomType: '',
  roomTypeId: 0,
  roomCode: '',
  date: '',
  dateLabel: '',
  weekday: '',
  currentPrice: 0,
  newPrice: 0,
  startDate: '',
  endDate: '',
})

// 计算日期列
const dateColumns = computed(() => {
  const columns = []
  const startDate = new Date(selectedDate.value)

  for (let i = 0; i < 11; i++) {
    const currentDate = new Date(startDate)
    currentDate.setDate(startDate.getDate() + i)

    const dateStr = currentDate.toISOString().split('T')[0]
    const month = currentDate.getMonth() + 1
    const day = currentDate.getDate()
    const weekday = ['周日', '周一', '周二', '周三', '周四', '周五', '周六'][currentDate.getDay()]

    // 判断是否为今天
    const today = new Date()
    const isToday = currentDate.toDateString() === today.toDateString()

    // 判断是否为节假日（这里简化处理，实际应该根据真实的节假日数据）
    const isHoliday = currentDate.getDay() === 0 || currentDate.getDay() === 6

    let dayLabel = `${month.toString().padStart(2, '0')}-${day.toString().padStart(2, '0')}`
    let dayClass = ''

    if (isToday) {
      dayLabel = '今天'
      dayClass = 'today'
    } else if (isHoliday) {
      dayClass = 'holiday'
    }

    columns.push({
      date: dateStr,
      label: `${dayLabel} ${weekday}`,
      dayLabel,
      weekday,
      prop: `date_${dateStr.replace(/-/g, '_')}`,
      dayClass,
      isToday,
      isHoliday,
    })
  }

  return columns
})

// 过滤后的房型数据
const filteredRoomTypes = computed(() => {
  if (roomTypeFilter.value === 'all') {
    return roomTypes.value
  } else {
    const filterValue = parseInt(roomTypeFilter.value)
    return roomTypes.value.filter(rt => rt.id === filterValue)
  }
})

// 房价表格数据
const priceTableData = computed(() => {
  if (!filteredRoomTypes.value.length) {
    return []
  }

  // 使用API构建表格数据
  return buildRoomPriceTableData(filteredRoomTypes.value, roomPrices.value, dateColumns.value)
})

// 计算每日总间数
const dailyTotalRooms = computed(() => {
  const totals: { [key: string]: number } = {}

  dateColumns.value.forEach((dateCol) => {
    let total = 0
    priceTableData.value.forEach((row) => {
      if (row[dateCol.prop]?.availableRooms) {
        total += row[dateCol.prop].availableRooms
      }
    })
    totals[dateCol.prop] = total
  })

  return totals
})

// 方法
const switchTab = (tab: string) => {
  activeTab.value = tab
}

const previousDate = () => {
  const current = new Date(selectedDate.value)
  current.setDate(current.getDate() - 1)
  selectedDate.value = current.toISOString().split('T')[0]
}

const nextDate = () => {
  const current = new Date(selectedDate.value)
  current.setDate(current.getDate() + 1)
  selectedDate.value = current.toISOString().split('T')[0]
}

const onDateChange = (date: string) => {
  if (date) {
    selectedDate.value = date
    loadPriceData()
  }
}

const onRoomTypeChange = () => {
  loadPriceData()
}

const onShowInventoryChange = () => {
  // 切换显示库存的逻辑
}

// 打开价格编辑器
const openPriceEditor = (roomRow: RoomPriceTableData, dateColumn: any) => {
  if (!roomRow[dateColumn.prop]?.price) return

  editingPrice.value = {
    roomType: roomRow.roomTypeName,
    roomTypeId: roomRow.roomTypeId,
    roomCode: roomRow.roomTypeCode,
    date: dateColumn.date,
    dateLabel: dateColumn.dayLabel,
    weekday: dateColumn.weekday,
    currentPrice: roomRow[dateColumn.prop].price,
    newPrice: roomRow[dateColumn.prop].price,
    startDate: dateColumn.date,
    endDate: dateColumn.date,
  }

  showPriceEditor.value = true
}

// 关闭价格编辑器
const closePriceEditor = () => {
  showPriceEditor.value = false
}

// 保存价格修改
const savePriceChange = async () => {
  try {
    loading.value = true

    const updateRequest: UpdateRoomPriceRequest = {
      roomTypeId: editingPrice.value.roomTypeId,
      startDate: editingPrice.value.startDate,
      endDate: editingPrice.value.endDate,
      price: editingPrice.value.newPrice
    }

    const response = await updateRoomPrice(updateRequest)
    if (response.success) {
      ElMessage.success('价格修改成功')
      closePriceEditor()
      loadPriceData() // 重新加载数据
    } else {
      ElMessage.error('价格修改失败：' + response.message)
    }
  } catch (error) {
    console.error('保存价格失败:', error)
    ElMessage.error('保存价格失败')
  } finally {
    loading.value = false
  }
}

// 加载房型数据
const loadRoomTypes = async () => {
  try {
    const response = await getAllRoomTypes()
    if (response.success) {
      roomTypes.value = response.data
    } else {
      ElMessage.error('获取房型数据失败')
    }
  } catch (error) {
    console.error('加载房型数据失败:', error)
    ElMessage.error('加载房型数据失败')
  }
}

// 加载房价数据
const loadPriceData = async () => {
  loading.value = true
  try {
    const startDate = selectedDate.value
    const endDate = new Date(startDate)
    endDate.setDate(endDate.getDate() + 10) // 加载11天数据
    const endDateStr = endDate.toISOString().split('T')[0]

    // 获取房价数据
    const response = await getRoomPrices(startDate, endDateStr)
    if (response.success) {
      roomPrices.value = response.data
    } else {
      ElMessage.error('获取房价数据失败')
    }
  } catch (error) {
    console.error('加载房价数据失败:', error)
    ElMessage.error('加载房价数据失败')
  } finally {
    loading.value = false
  }
}

// 监听日期变化
watch(selectedDate, () => {
  loadPriceData()
})

// 组件挂载时加载数据
onMounted(async () => {
  await loadRoomTypes()
  loadPriceData()
})
</script>

<style scoped>
.price-management {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: #fff;
}

/* 顶部导航 */
.top-nav {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  border-bottom: 1px solid #e8e8e8;
  background: #fafafa;
}

.nav-tabs {
  display: flex;
  gap: 12px;
}

.nav-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.system-label {
  font-size: 14px;
  color: #666;
}

/* 控制面板 */
.control-panel {
  display: flex;
  align-items: center;
  gap: 24px;
  padding: 16px 20px;
  border-bottom: 1px solid #e8e8e8;
  background: #fff;
}

.control-group {
  display: flex;
  align-items: center;
  gap: 8px;
}

.control-label {
  font-size: 14px;
  color: #666;
  min-width: 60px;
}

.date-control {
  display: flex;
  align-items: center;
  gap: 8px;
}

/* 房型选择器样式 */
.room-type-select {
  min-width: 160px;
  max-width: 250px;
}

/* 表格容器 */
.price-table-container {
  flex: 1;
  padding: 20px;
  overflow: auto;
}

/* 表格样式 */
.room-type-name {
  font-weight: 500;
  color: #333;
}

.date-header {
  text-align: center;
  line-height: 1.2;
}

.date-label {
  font-weight: 500;
  margin-bottom: 2px;
}

.date-label.today {
  color: #f56c6c;
}

.date-label.holiday {
  color: #e6a23c;
}

.date-day {
  font-size: 12px;
  color: #999;
  margin-bottom: 2px;
}

/* 总间数显示 */
.total-rooms {
  font-size: 11px;
  color: #409eff;
  font-weight: 500;
  margin-top: 2px;
}

.price-cell {
  padding: 4px;
  position: relative;
}

.price-cell.has-special {
  background-color: #fff7e6;
}

.price-cell.clickable {
  cursor: pointer;
  transition: all 0.2s ease;
}

.price-cell.clickable:hover {
  background-color: #e6f7ff;
  transform: scale(1.02);
}

.price-value {
  font-weight: 500;
  color: #333;
  margin-bottom: 2px;
}

.price-cell.clickable .price-value {
  color: #409eff;
  text-decoration: underline;
}

.no-price {
  color: #ccc;
  font-size: 12px;
}

/* 剩余房间数显示 */
.rooms-available {
  font-size: 11px;
  color: #67c23a;
  margin-top: 2px;
  font-weight: 500;
}

/* 价格编辑侧边栏样式 */
.price-editor {
  padding: 20px;
  height: 100%;
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.room-info {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px;
  background: #f8f9fa;
  border-radius: 8px;
}

.room-name {
  font-size: 16px;
  font-weight: 500;
  color: #333;
}

.form-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.form-label {
  font-size: 14px;
  color: #333;
  font-weight: 500;
  display: flex;
  align-items: center;
  gap: 4px;
}

.required {
  color: #f56c6c;
  font-weight: bold;
}

.date-range {
  display: flex;
  align-items: center;
  gap: 8px;
}

.date-separator {
  font-size: 14px;
  color: #666;
}

.price-input {
  display: flex;
  align-items: center;
  gap: 8px;
}

.currency {
  font-size: 16px;
  color: #333;
  font-weight: 500;
}

.action-buttons {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: auto;
  padding-top: 20px;
  border-top: 1px solid #e8e8e8;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .control-panel {
    flex-direction: column;
    align-items: stretch;
    gap: 12px;
  }

  .control-group {
    justify-content: space-between;
  }

  .top-nav {
    flex-direction: column;
    gap: 12px;
  }
}
</style>
