<template>
  <div class="bulk-price-change">
    <!-- 顶部提示信息 -->
    <div class="notice-banner">
      <el-alert
        title="小提示：大部分日库房数多180天，携程、Booking.com、Agoda、Expedia对数730天，途家可推至一个月的直连后一天，美团民宿可推至一个月的后一天，您可按服长的日期推送，但超出渠道限制的将无法上传。"
        type="info"
        :closable="false"
        show-icon
      />
    </div>

    <!-- 单页面所有模块 -->
    <div class="content-sections">
      <!-- 1. 选择渠道 -->
      <div class="section-panel">
        <div class="section-title">
          <span class="section-number">1</span>
          <span class="section-text">选择渠道</span>
        </div>

        <div class="section-content">
          <div class="channel-summary">
            <el-checkbox v-model="selectAllChannels" @change="onSelectAllChannelsChange">
              全选 已选 {{ selectedChannels.length }} 个渠道
            </el-checkbox>
          </div>

          <div class="channel-options">
            <div class="channel-group">
              <span class="channel-label">系统</span>
              <el-checkbox v-model="selectedChannels" value="门市价" @change="onChannelChange">
                门市价
              </el-checkbox>
            </div>
          </div>
        </div>
      </div>

      <!-- 2. 选择房型 -->
      <div class="section-panel">
        <div class="section-title">
          <span class="section-number">2</span>
          <span class="section-text">选择房型</span>
        </div>

        <div class="section-content">
          <!-- 未选择渠道时的提示 -->
          <div v-if="selectedChannels.length === 0" class="disabled-message">请先选择渠道</div>

          <!-- 已选择渠道时显示房型选择 -->
          <template v-else>
            <div v-if="loadingRoomTypes" class="loading-message">
              <el-icon class="is-loading"><Loading /></el-icon>
              <span>加载房型中...</span>
            </div>
            <template v-else-if="roomTypeOptions.length > 0">
              <div class="room-type-summary">
                <el-checkbox v-model="selectAllRoomTypes" @change="onSelectAllRoomTypesChange">
                  全选 已选 {{ selectedRoomTypes.length }} 个房型，对应
                  {{ selectedRoomTypes.length }} 个渠道房型
                </el-checkbox>
              </div>

              <div class="room-type-selection">
                <el-button
                  v-for="roomType in roomTypeOptions"
                  :key="roomType.id"
                  :class="['room-type-btn', { selected: selectedRoomTypes.includes(roomType.id) }]"
                  @click="toggleRoomType(roomType.id)"
                >
                  {{ roomType.name }}
                </el-button>
              </div>
            </template>
            <div v-else class="disabled-message">暂无房型数据</div>
          </template>
        </div>
      </div>

      <!-- 3. 选择日期 -->
      <div class="section-panel">
        <div class="section-title">
          <span class="section-number">3</span>
          <span class="section-text">选择日期</span>
        </div>

        <div class="section-content">
          <div class="date-summary">
            <span class="selected-count">已选 {{ totalSelectedDays }} 天</span>
            <el-button link type="primary" @click="clearDates">重置</el-button>
          </div>

          <!-- 多个日期范围 -->
          <div class="date-ranges-container">
            <div v-for="(range, index) in dateRanges" :key="index" class="date-range-item">
              <div class="date-range-section">
                <span class="date-label">适用日期</span>
                <div class="date-inputs">
                  <el-date-picker
                    v-model="range.start"
                    type="date"
                    placeholder="开始日期"
                    format="YYYY-MM-DD"
                    value-format="YYYY-MM-DD"
                    size="small"
                    @change="updateDateRange"
                  />
                  <span class="date-separator">至</span>
                  <el-date-picker
                    v-model="range.end"
                    type="date"
                    placeholder="结束日期"
                    format="YYYY-MM-DD"
                    value-format="YYYY-MM-DD"
                    size="small"
                    @change="updateDateRange"
                  />
                  <el-button
                    v-if="dateRanges.length > 1"
                    icon="Delete"
                    size="small"
                    type="danger"
                    text
                    @click="removeDateRange(index)"
                  />
                </div>
              </div>

              <div class="holiday-section">
                <el-button
                  v-for="holiday in holidayOptions"
                  :key="holiday.name"
                  :class="['holiday-btn', { selected: isHolidaySelected(holiday, index) }]"
                  @click="selectHoliday(holiday, index)"
                  size="small"
                >
                  {{ holiday.name }}
                </el-button>
              </div>
            </div>
          </div>

          <div class="add-dates-section">
            <el-button
              link
              type="primary"
              icon="Plus"
              @click="addMoreDates"
              size="small"
              :disabled="dateRanges.length >= 10"
            >
              添加日期 最多添加10个
            </el-button>
          </div>

          <div class="weekday-section">
            <div class="weekday-header">
              <span class="weekday-label">适用周几</span>
              <el-checkbox v-model="selectAllWeekdays" @change="onSelectAllWeekdaysChange">
                全选
              </el-checkbox>
            </div>

            <div class="weekday-checkboxes">
              <el-checkbox
                v-for="weekday in weekdays"
                :key="weekday.value"
                v-model="selectedWeekdays"
                :value="weekday.value"
                @change="onWeekdayChange"
                size="small"
              >
                {{ weekday.label }}
              </el-checkbox>
            </div>
          </div>
        </div>
      </div>

      <!-- 4. 设置价格 -->
      <div class="section-panel">
        <div class="section-title">
          <span class="section-number">4</span>
          <span class="section-text">设置价格</span>
        </div>

        <div class="section-content">
          <div class="price-settings">
            <div class="weekend-section">
              <div class="setting-header">
                <span class="setting-label">区分平日周末</span>
                <el-tooltip content="周末为周五/周六" placement="top">
                  <el-icon class="help-icon"><QuestionFilled /></el-icon>
                </el-tooltip>
              </div>

              <el-radio-group v-model="weekendDifferentiation" size="small">
                <el-radio value="no">不区分</el-radio>
                <el-radio value="yes">区分</el-radio>
              </el-radio-group>
            </div>

            <div class="price-method-section">
              <div class="setting-label">改价方式</div>

              <el-radio-group v-model="priceMethod" size="small">
                <el-radio value="fixed">指定金额</el-radio>
                <el-radio value="rule">规则调价</el-radio>
              </el-radio-group>
            </div>

            <div class="price-input-section" v-if="priceMethod === 'fixed'">
              <!-- 不区分平日周末 -->
              <template v-if="weekendDifferentiation === 'no'">
                <div class="setting-label">价格统一设置为</div>

                <div class="price-input-row">
                  <span class="currency">￥</span>
                  <el-input
                    v-model.number="unifiedPrice"
                    type="number"
                    placeholder="请输入指定金额"
                    class="unified-price-input"
                    size="small"
                    @input="onUnifiedPriceChange"
                  />
                </div>
              </template>

              <!-- 区分平日周末 -->
              <template v-else>
                <div class="setting-label">价格统一设置为</div>

                <div class="weekend-price-inputs">
                  <div class="price-input-row">
                    <span class="price-label">平日价</span>
                    <span class="currency">￥</span>
                    <el-input
                      v-model.number="unifiedPrice"
                      type="number"
                      placeholder="请输入平日价"
                      class="unified-price-input"
                      size="small"
                      @input="onUnifiedPriceChange"
                    />
                  </div>

                  <div class="price-input-row">
                    <span class="price-label">周末价</span>
                    <span class="currency">￥</span>
                    <el-input
                      v-model.number="unifiedWeekendPrice"
                      type="number"
                      placeholder="请输入周末价"
                      class="unified-price-input"
                      size="small"
                      @input="onUnifiedWeekendPriceChange"
                    />
                  </div>
                </div>
              </template>
            </div>
          </div>

          <div class="bottom-table" v-if="selectedRoomTypes.length > 0">
            <div class="table-header" :class="{ 'weekend-mode': weekendDifferentiation === 'yes' }">
              <span class="table-title">本地房型</span>
              <span class="table-title">统一设置</span>
              <template v-if="weekendDifferentiation === 'no'">
                <span class="table-title">门市价</span>
              </template>
              <template v-else>
                <span class="table-title">门市价-平日</span>
                <span class="table-title">门市价-周末</span>
              </template>
              <span class="table-title"
                >操作 <el-icon class="edit-icon"><Edit /></el-icon
              ></span>
            </div>

            <div class="table-rows">
              <div
                v-for="(room, index) in dynamicRoomPriceDataArray"
                :key="room.id"
                class="table-row"
                :class="{ 'weekend-mode': weekendDifferentiation === 'yes' }"
              >
                <span class="room-name">{{ room.roomType }}</span>

                <!-- 统一设置列 -->
                <div class="price-input-cell">
                  <template v-if="weekendDifferentiation === 'no'">
                    <span class="currency">￥</span>
                    <el-input
                      v-model.number="room.unifiedPrice"
                      type="number"
                      class="table-price-input"
                      size="small"
                      placeholder="统一设置"
                      @input="onRoomUnifiedPriceChange(index)"
                    />
                  </template>
                  <template v-else>
                    <div class="weekend-unified-inputs">
                      <div class="unified-price-row">
                        <span class="currency">￥</span>
                        <el-input
                          v-model.number="room.unifiedPrice"
                          type="number"
                          class="table-price-input small"
                          size="small"
                          placeholder="平日"
                          @input="onRoomUnifiedPriceChange(index)"
                        />
                      </div>
                      <div class="unified-price-row">
                        <span class="currency">￥</span>
                        <el-input
                          v-model.number="room.unifiedWeekendPrice"
                          type="number"
                          class="table-price-input small"
                          size="small"
                          placeholder="周末"
                          @input="onRoomUnifiedWeekendPriceChange(index)"
                        />
                      </div>
                    </div>
                  </template>
                </div>

                <!-- 门市价列 -->
                <template v-if="weekendDifferentiation === 'no'">
                  <div class="price-input-cell">
                    <span class="currency">￥</span>
                    <el-input
                      v-model.number="room.currentPrice"
                      type="number"
                      class="table-price-input"
                      size="small"
                      placeholder="金额"
                    />
                  </div>
                </template>
                <template v-else>
                  <!-- 门市价-平日 -->
                  <div class="price-input-cell">
                    <span class="currency">￥</span>
                    <el-input
                      v-model.number="room.currentPrice"
                      type="number"
                      class="table-price-input"
                      size="small"
                      placeholder="平日价"
                    />
                  </div>
                  <!-- 门市价-周末 -->
                  <div class="price-input-cell">
                    <span class="currency">￥</span>
                    <el-input
                      v-model.number="room.currentWeekendPrice"
                      type="number"
                      class="table-price-input"
                      size="small"
                      placeholder="周末价"
                    />
                  </div>
                </template>

                <div class="action-buttons">
                  <el-button link type="primary" icon="Edit" size="small">操作</el-button>
                  <el-button link type="danger" @click="deleteRoomPrice(index)" size="small">
                    删除
                  </el-button>
                </div>
              </div>
            </div>
          </div>

          <!-- 未选择房型时的提示 -->
          <div class="no-room-selected" v-else>
            <div class="empty-message">请先选择房型才能设置价格</div>
          </div>
        </div>
      </div>
    </div>

    <!-- 底部提交按钮 -->
    <div class="submit-section">
      <el-button type="primary" @click="submitPriceChanges" :loading="submitting" size="large">
        提交
      </el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { QuestionFilled, Edit, Loading } from '@element-plus/icons-vue'
import { getAllRoomTypes, type RoomTypeDTO } from '@/api/roomType'
import { bulkPriceChange, type BulkPriceChangeRequest } from '@/api/roomPrice'

// 加载状态
const submitting = ref(false)
const loadingRoomTypes = ref(false)

// 1. 渠道选择
const selectedChannels = ref<string[]>([])
const selectAllChannels = ref(false)

// 2. 房型选择
const selectedRoomTypes = ref<number[]>([])
const selectAllRoomTypes = ref(false)

// 房型选项（从后端获取）
const roomTypeOptions = ref<RoomTypeDTO[]>([])

// 3. 日期选择
const dateRanges = ref([{ start: '', end: '' }])
const selectedWeekdays = ref<number[]>([1, 2, 3, 4, 5, 6, 0]) // 全选
const selectAllWeekdays = ref(true)

// 节假日选项
const holidayOptions = [
  { name: '中秋', startDate: '2025-09-15', endDate: '2025-09-17' },
  { name: '国庆', startDate: '2025-10-01', endDate: '2025-10-07' },
]

// 4. 价格设置
const weekendDifferentiation = ref('no')
const priceMethod = ref('fixed')
const unifiedPrice = ref<number | string>('')
const unifiedWeekendPrice = ref<number | string>('')
const roomPriceData = ref([{ roomType: '大床房', currentPrice: 12 }])

// 动态房型价格数据（响应式）
const dynamicRoomPriceDataArray = ref<
  Array<{
    id: number
    roomType: string
    currentPrice: number
    currentWeekendPrice: number
    unifiedPrice: number | string
    unifiedWeekendPrice: number | string
  }>
>([])

// 监听房型选择变化
watch(
  selectedRoomTypes,
  (newRoomTypes) => {
    // 如果没有选择任何房型，清空动态数据
    if (!newRoomTypes || newRoomTypes.length === 0) {
      dynamicRoomPriceDataArray.value = []
      return
    }

    // 保存当前的数据以便在重新生成时保留用户输入
    const currentData = [...dynamicRoomPriceDataArray.value]

    const newData = newRoomTypes.map((roomTypeId) => {
      const roomType = roomTypeOptions.value.find((rt) => rt.id === roomTypeId)
      const existingData = currentData.find((d) => d.id === roomTypeId)

      if (!roomType) {
        return {
          id: roomTypeId,
          roomType: '未知房型',
          currentPrice: 0,
          currentWeekendPrice: 0,
          unifiedPrice: '',
          unifiedWeekendPrice: '',
        }
      }

      return {
        id: roomTypeId,
        roomType: roomType.name || '未知房型',
        currentPrice: existingData?.currentPrice || 0,
        currentWeekendPrice: existingData?.currentWeekendPrice || 0,
        unifiedPrice: existingData?.unifiedPrice || unifiedPrice.value || '',
        unifiedWeekendPrice: existingData?.unifiedWeekendPrice || unifiedWeekendPrice.value || '',
      }
    })

    dynamicRoomPriceDataArray.value = newData
  },
  { immediate: true, deep: true },
)

// 周几选项
const weekdays = [
  { value: 1, label: '周一' },
  { value: 2, label: '周二' },
  { value: 3, label: '周三' },
  { value: 4, label: '周四' },
  { value: 5, label: '周五' },
  { value: 6, label: '周六' },
  { value: 0, label: '周日' },
]

// 计算属性
const totalSelectedDays = computed(() => {
  let totalDays = 0
  dateRanges.value.forEach((range) => {
    if (range.start && range.end) {
      const start = new Date(range.start)
      const end = new Date(range.end)
      totalDays += Math.ceil((end.getTime() - start.getTime()) / (1000 * 60 * 60 * 24)) + 1
    }
  })
  return totalDays
})

// 渠道选择方法
const onSelectAllChannelsChange = (checked: boolean) => {
  if (checked) {
    selectedChannels.value = ['门市价']
  } else {
    selectedChannels.value = []
  }
}

const onChannelChange = () => {
  selectAllChannels.value = selectedChannels.value.length === 1
}

// 房型选择方法
const onSelectAllRoomTypesChange = (checked: boolean) => {
  if (checked) {
    selectedRoomTypes.value = roomTypeOptions.value.map((rt) => rt.id)
  } else {
    selectedRoomTypes.value = []
  }
}

// 周几选择方法
const onSelectAllWeekdaysChange = (checked: boolean) => {
  if (checked) {
    selectedWeekdays.value = [1, 2, 3, 4, 5, 6, 0]
  } else {
    selectedWeekdays.value = []
  }
}

const onWeekdayChange = () => {
  selectAllWeekdays.value = selectedWeekdays.value.length === 7
}

// 房型选择方法 - 按钮切换
const toggleRoomType = (roomTypeId: number) => {
  const index = selectedRoomTypes.value.indexOf(roomTypeId)
  if (index > -1) {
    selectedRoomTypes.value.splice(index, 1)
  } else {
    selectedRoomTypes.value.push(roomTypeId)
  }
  selectAllRoomTypes.value = selectedRoomTypes.value.length === roomTypeOptions.value.length

  // 强制触发响应式更新
  selectedRoomTypes.value = [...selectedRoomTypes.value]
}

// 节假日选择方法
const isHolidaySelected = (holiday: { startDate: string; endDate: string }, rangeIndex: number) => {
  const range = dateRanges.value[rangeIndex]
  return range.start === holiday.startDate && range.end === holiday.endDate
}

const selectHoliday = (holiday: { startDate: string; endDate: string }, rangeIndex: number) => {
  if (isHolidaySelected(holiday, rangeIndex)) {
    // 如果已选中，则取消选中
    dateRanges.value[rangeIndex] = { start: '', end: '' }
  } else {
    // 选中该节假日
    dateRanges.value[rangeIndex] = {
      start: holiday.startDate,
      end: holiday.endDate,
    }
  }
}

// 添加日期范围
const addMoreDates = () => {
  if (dateRanges.value.length < 10) {
    dateRanges.value.push({ start: '', end: '' })
  } else {
    ElMessage.warning('最多只能添加10个日期范围')
  }
}

// 删除日期范围
const removeDateRange = (index: number) => {
  if (dateRanges.value.length > 1) {
    dateRanges.value.splice(index, 1)
  }
}

// 更新日期范围
const updateDateRange = () => {
  // 日期变化时的回调
}

// 其他方法
const clearAllData = () => {
  selectedChannels.value = []
  selectedRoomTypes.value = []
  dateRanges.value = [{ start: '', end: '' }]
  selectedWeekdays.value = []
  selectAllChannels.value = false
  selectAllRoomTypes.value = false
  selectAllWeekdays.value = false
  unifiedPrice.value = ''
  ElMessage.success('数据已清空')
}

const clearDates = () => {
  dateRanges.value = [{ start: '', end: '' }]
  selectedWeekdays.value = []
  selectAllWeekdays.value = false
}

const clearData = () => {
  clearAllData()
}

const deleteRoomPrice = (index: number) => {
  roomPriceData.value.splice(index, 1)
}

// 统一改价方法
const onUnifiedPriceChange = (value: number | string) => {
  if (value !== '' && value !== null && value !== undefined) {
    // 当统一价格输入框有值时，同步更新所有房型的统一设置价格和门市价
    dynamicRoomPriceDataArray.value.forEach((room) => {
      room.unifiedPrice = value
      room.currentPrice = typeof value === 'number' ? value : parseFloat(value.toString()) || 0
    })
  }
}

// 统一周末改价方法
const onUnifiedWeekendPriceChange = (value: number | string) => {
  if (value !== '' && value !== null && value !== undefined) {
    // 当统一周末价格输入框有值时，同步更新所有房型的统一设置周末价格和门市周末价
    dynamicRoomPriceDataArray.value.forEach((room) => {
      room.unifiedWeekendPrice = value
      room.currentWeekendPrice =
        typeof value === 'number' ? value : parseFloat(value.toString()) || 0
    })
  }
}

// 单个房型统一价格改变
const onRoomUnifiedPriceChange = (index: number) => {
  // 当单个房型的统一设置改变时，同步更新该房型的门市价
  const room = dynamicRoomPriceDataArray.value[index]
  if (
    room &&
    room.unifiedPrice !== '' &&
    room.unifiedPrice !== null &&
    room.unifiedPrice !== undefined
  ) {
    const price =
      typeof room.unifiedPrice === 'number'
        ? room.unifiedPrice
        : parseFloat(room.unifiedPrice.toString()) || 0
    room.currentPrice = price
  }
}

// 单个房型统一周末价格改变
const onRoomUnifiedWeekendPriceChange = (index: number) => {
  // 当单个房型的统一周末价设置改变时，同步更新该房型的门市周末价
  const room = dynamicRoomPriceDataArray.value[index]
  if (
    room &&
    room.unifiedWeekendPrice !== '' &&
    room.unifiedWeekendPrice !== null &&
    room.unifiedWeekendPrice !== undefined
  ) {
    const price =
      typeof room.unifiedWeekendPrice === 'number'
        ? room.unifiedWeekendPrice
        : parseFloat(room.unifiedWeekendPrice.toString()) || 0
    room.currentWeekendPrice = price
  }
}

const submitPriceChanges = async () => {
  try {
    // 数据验证
    if (selectedRoomTypes.value.length === 0) {
      ElMessage.warning('请至少选择一个房型')
      return
    }

    // 检查日期范围
    const validDateRanges = dateRanges.value.filter((range) => range.start && range.end)
    if (validDateRanges.length === 0) {
      ElMessage.warning('请至少选择一个日期范围')
      return
    }

    // 检查价格
    if (!unifiedPrice.value || Number(unifiedPrice.value) <= 0) {
      ElMessage.warning('请输入有效的价格')
      return
    }

    if (weekendDifferentiation.value === 'yes' && (!unifiedWeekendPrice.value || Number(unifiedWeekendPrice.value) <= 0)) {
      ElMessage.warning('请输入有效的周末价格')
      return
    }

    await ElMessageBox.confirm(`确定要提交批量改价吗？`, '确认提交', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    })

    submitting.value = true

    // 构建请求数据
    const request: BulkPriceChangeRequest = {
      roomTypeIds: selectedRoomTypes.value,
      dateRanges: validDateRanges.map((range) => ({
        startDate: range.start,
        endDate: range.end,
      })),
      weekdays: selectedWeekdays.value.length === 7 ? undefined : selectedWeekdays.value,
      weekendDifferentiation: weekendDifferentiation.value === 'yes',
      weekdayPrice: Number(unifiedPrice.value),
      weekendPrice: weekendDifferentiation.value === 'yes' ? Number(unifiedWeekendPrice.value) : undefined,
    }

    // 调用后端API
    const response = await bulkPriceChange(request)

    if (response.success) {
      ElMessage.success(`批量改价成功，已更新 ${response.data?.length || 0} 条价格记录`)
      clearData()
    } else {
      ElMessage.error(response.message || '批量改价失败')
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('批量改价失败:', error)
      ElMessage.error('批量改价失败')
    }
  } finally {
    submitting.value = false
  }
}

// 加载房型列表
const fetchRoomTypes = async () => {
  try {
    loadingRoomTypes.value = true
    const response = await getAllRoomTypes()
    if (response.success && response.data) {
      roomTypeOptions.value = response.data
    } else {
      ElMessage.error('加载房型列表失败')
    }
  } catch (error) {
    console.error('加载房型列表失败:', error)
    ElMessage.error('加载房型列表失败')
  } finally {
    loadingRoomTypes.value = false
  }
}

// 初始化组件
onMounted(() => {
  fetchRoomTypes()
})
</script>

<style scoped>
.bulk-price-change {
  padding: 20px;
  background: #fff;
  min-height: 100vh;
}

/* 顶部提示栏 */
.notice-banner {
  margin-bottom: 16px;
}

/* 单页面所有模块 */
.content-sections {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.section-panel {
  background: #f8f9fa;
  border-radius: 8px;
  padding: 20px;
  border: 1px solid #e8e8e8;
}

.section-title {
  display: flex;
  align-items: center;
  margin-bottom: 16px;
  gap: 12px;
}

.section-number {
  width: 24px;
  height: 24px;
  background: #909399;
  color: #fff;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 500;
  font-size: 14px;
}

.section-text {
  font-size: 16px;
  font-weight: 500;
  color: #333;
}

.section-content {
  padding-left: 36px;
}

/* 1. 渠道选择样式 */
.channel-summary {
  margin-bottom: 12px;
}

.channel-options {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
}

.channel-group {
  display: flex;
  align-items: center;
  gap: 8px;
}

.channel-label {
  background: #f0f0f0;
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 12px;
  color: #666;
}

/* 2. 房型选择样式 */
.room-type-summary {
  margin-bottom: 12px;
}

.room-type-selection {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 12px;
}

.room-type-btn {
  border: 1px solid #d9d9d9;
  background: #fff;
  color: #333;
}

.room-type-btn.selected {
  background: #409eff;
  color: #fff;
  border-color: #409eff;
}

.disabled-message {
  color: #999;
  font-size: 14px;
  text-align: center;
  padding: 20px;
  background: #f5f5f5;
  border-radius: 4px;
  border: 1px dashed #d9d9d9;
}

/* 3. 日期选择样式 */
.date-summary {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  padding: 8px 12px;
  background: #f0f7ff;
  border-radius: 4px;
  font-size: 14px;
}

.selected-count {
  color: #409eff;
  font-weight: 500;
}

.date-ranges-container {
  margin-bottom: 16px;
}

.date-range-item {
  margin-bottom: 16px;
  padding: 12px;
  background: #fff;
  border-radius: 4px;
  border: 1px solid #e8e8e8;
}

.date-range-item:last-child {
  margin-bottom: 0;
}

.date-range-section {
  margin-bottom: 12px;
}

.date-label {
  font-size: 14px;
  color: #333;
  margin-bottom: 8px;
  display: block;
}

.date-inputs {
  display: flex;
  align-items: center;
  gap: 8px;
}

.date-separator {
  font-size: 14px;
  color: #666;
}

.holiday-section {
  margin-bottom: 12px;
}

.holiday-btn {
  margin-right: 8px;
  margin-bottom: 8px;
  border: 1px solid #d9d9d9;
  background: #fff;
  color: #333;
}

.holiday-btn.selected {
  background: #e6a23c;
  color: #fff;
  border-color: #e6a23c;
}

.add-dates-section {
  margin-bottom: 16px;
}

.weekday-section {
  background: #fff;
  padding: 12px;
  border-radius: 4px;
  border: 1px solid #e8e8e8;
}

.weekday-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.weekday-label {
  font-size: 14px;
  color: #333;
  font-weight: 500;
}

.weekday-checkboxes {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

/* 4. 价格设置样式 */
.price-settings {
  margin-bottom: 20px;
}

.weekend-section,
.price-method-section,
.price-input-section {
  margin-bottom: 16px;
}

.setting-header {
  display: flex;
  align-items: center;
  margin-bottom: 8px;
}

.setting-label {
  font-size: 14px;
  color: #333;
  font-weight: 500;
  margin-bottom: 8px;
  display: block;
}

.help-icon {
  color: #999;
  cursor: pointer;
  margin-left: 4px;
}

.price-input-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.currency {
  font-size: 14px;
  color: #333;
  font-weight: 500;
}

.unified-price-input {
  width: 150px;
}

/* 区分平日周末的价格输入样式 */
.weekend-price-inputs {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.price-label {
  font-size: 14px;
  color: #333;
  margin-right: 8px;
  min-width: 50px;
}

/* 底部表格 */
.bottom-table {
  background: #fff;
  border-radius: 4px;
  border: 1px solid #e8e8e8;
  overflow: hidden;
}

.table-header {
  display: grid;
  grid-template-columns: 1fr 1fr 1fr 1fr;
  background: #fafafa;
  border-bottom: 1px solid #e8e8e8;
  padding: 8px 12px;
  font-size: 14px;
  font-weight: 500;
  color: #333;
}

.table-header.weekend-mode {
  grid-template-columns: 1fr 1fr 1fr 1fr 1fr;
}

.table-title {
  display: flex;
  align-items: center;
  gap: 4px;
}

.edit-icon {
  color: #409eff;
  font-size: 12px;
}

.table-rows {
  max-height: 200px;
  overflow-y: auto;
}

.table-row {
  display: grid;
  grid-template-columns: 1fr 1fr 1fr 1fr;
  padding: 8px 12px;
  border-bottom: 1px solid #f0f0f0;
  align-items: center;
}

.table-row.weekend-mode {
  grid-template-columns: 1fr 1fr 1fr 1fr 1fr;
}

.table-row:last-child {
  border-bottom: none;
}

.room-name {
  font-size: 14px;
  color: #333;
}

.price-input-cell {
  display: flex;
  align-items: center;
  gap: 4px;
}

.table-price-input {
  width: 80px;
}

.table-price-input.small {
  width: 60px;
}

/* 统一设置列的周末模式样式 */
.weekend-unified-inputs {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.unified-price-row {
  display: flex;
  align-items: center;
  gap: 4px;
}

.action-buttons {
  display: flex;
  gap: 4px;
}

/* 未选择房型提示 */
.no-room-selected {
  margin-top: 16px;
}

.empty-message {
  color: #999;
  font-size: 14px;
  text-align: center;
  padding: 20px;
  background: #f5f5f5;
  border-radius: 4px;
  border: 1px dashed #d9d9d9;
}

/* 底部提交按钮 */
.submit-section {
  text-align: center;
  margin-top: 30px;
  padding-top: 20px;
  border-top: 1px solid #e8e8e8;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .bulk-price-change {
    padding: 16px;
  }

  .date-range-inputs {
    flex-direction: column;
    align-items: stretch;
  }

  .weekday-checkboxes {
    flex-direction: column;
    gap: 8px;
  }

  .action-buttons {
    flex-direction: column;
  }
}
</style>
