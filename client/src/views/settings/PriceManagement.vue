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
          placeholder="本地房型"
          @change="onRoomTypeChange"
          :disabled="loading"
          class="room-type-select"
        >
          <el-option label="本地房型" value="local" />
          <el-option label="普通房" value="normal" />
          <el-option label="全部房型" value="all" />
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
        <el-table-column prop="roomType" label="本地房型" width="120" fixed="left">
          <template #default="scope">
            <span class="room-type-name">{{ scope.row.roomType }}</span>
          </template>
        </el-table-column>

        <!-- 动态日期列 -->
        <el-table-column
          v-for="date in dateColumns"
          :key="date.date"
          :label="date.label"
          :prop="date.prop"
          width="100"
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

// 响应式数据
const activeTab = ref('price-setting')
const selectedDate = ref('2025-09-26')
const roomTypeFilter = ref('local')
const showInventory = ref(false)
const loading = ref(false)

// 价格编辑相关数据
const showPriceEditor = ref(false)
const editingPrice = ref({
  roomType: '',
  roomCode: '',
  date: '',
  dateLabel: '',
  weekday: '',
  currentPrice: 0,
  newPrice: 0,
  startDate: '',
  endDate: '',
})

// 房型数据
const roomTypes = [
  { id: 1, name: '大床房', code: 'big-bed' },
  { id: 2, name: 'aa', code: 'aa' },
  { id: 3, name: '大床房', code: 'big-bed-2' },
  { id: 4, name: '大厅', code: 'hall' },
]

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

// 房价表格数据
const priceTableData = computed(() => {
  const data = roomTypes.map((roomType) => {
    const row: any = {
      id: roomType.id,
      roomType: roomType.name,
      code: roomType.code,
    }

    // 为每个日期添加价格数据
    dateColumns.value.forEach((dateCol) => {
      // 模拟不同房型的价格
      let basePrice = 0
      switch (roomType.code) {
        case 'big-bed':
          basePrice = 140
          break
        case 'aa':
          basePrice = 12
          break
        case 'big-bed-2':
          basePrice = 140
          break
        case 'hall':
          basePrice = 12
          break
        default:
          basePrice = 100
      }

      // 周末加价
      const price = dateCol.isHoliday ? basePrice : basePrice

      // 模拟剩余房间数量
      const availableRooms = Math.floor(Math.random() * 10) + 1

      row[dateCol.prop] = {
        price: price,
        availableRooms: availableRooms,
        hasSpecial: dateCol.isHoliday,
      }
    })

    return row
  })

  return showInventory.value ? data.filter((row) => row.roomType !== '大厅') : data
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
const openPriceEditor = (roomRow: any, dateColumn: any) => {
  if (!roomRow[dateColumn.prop]?.price) return

  editingPrice.value = {
    roomType: roomRow.roomType,
    roomCode: roomRow.code,
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

    // 这里应该调用真实的API来保存价格修改
    // await updatePrice({
    //   roomCode: editingPrice.value.roomCode,
    //   startDate: editingPrice.value.startDate,
    //   endDate: editingPrice.value.endDate,
    //   price: editingPrice.value.newPrice
    // })

    // 模拟API调用延迟
    await new Promise((resolve) => setTimeout(resolve, 500))

    ElMessage.success('价格修改成功')
    closePriceEditor()
    loadPriceData() // 重新加载数据
  } catch (error) {
    console.error('保存价格失败:', error)
    ElMessage.error('保存价格失败')
  } finally {
    loading.value = false
  }
}

const loadPriceData = async () => {
  loading.value = true
  try {
    // 模拟API调用延迟
    await new Promise((resolve) => setTimeout(resolve, 500))

    // 这里应该调用真实的API来获取价格数据
    // const response = await getPriceData({
    //   date: selectedDate.value,
    //   roomTypeFilter: roomTypeFilter.value
    // })
  } catch (error) {
    console.error('加载房价数据失败:', error)
    ElMessage.error('加载房价数据失败')
  } finally {
    loading.value = false
  }
}

// 监听日期变化
watch(selectedDate, () => {
  // 可以在这里触发数据重新加载
})

// 组件挂载时加载数据
onMounted(() => {
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

.date-value {
  font-size: 12px;
  color: #666;
  margin-bottom: 2px;
}

.date-day {
  font-size: 12px;
  color: #999;
  margin-bottom: 2px;
}

.week-number {
  font-size: 11px;
  color: #ccc;
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

.week-indicator {
  font-size: 10px;
  color: #999;
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
