<template>
  <div class="price-management">
    <div class="top-controls">
      <div class="control-left">
        <div class="date-navigation">
          <span class="control-label">选择日期</span>
          <el-button-group>
            <el-button size="small" @click="previousWeek">
              <el-icon><ArrowLeft /></el-icon>
              <span>上周</span>
            </el-button>
            <el-button size="small" @click="previousDay">
              <el-icon><ArrowLeft /></el-icon>
              天
            </el-button>
          </el-button-group>
          <el-date-picker
            v-model="selectedDate"
            type="date"
            placeholder="选择日期"
            format="YYYY/MM/DD"
            value-format="YYYY-MM-DD"
            @change="onDateChange"
            size="small"
            class="date-picker"
          />
          <el-button-group>
            <el-button size="small" @click="nextDay">
              天
              <el-icon><ArrowRight /></el-icon>
            </el-button>
            <el-button size="small" @click="nextWeek">
              <span>下周</span>
              <el-icon><ArrowRight /></el-icon>
            </el-button>
          </el-button-group>
        </div>
      </div>

      <div class="control-right">
        <div class="room-type-filter">
          <span class="control-label">房型筛选</span>
          <el-select
            v-model="selectedRoomTypeId"
            placeholder="全部"
            @change="handleFilterChange"
            size="small"
            clearable
            class="filter-select"
          >
            <el-option label="全部" :value="null" />
            <el-option
              v-for="roomType in roomTypes"
              :key="roomType.id"
              :label="roomType.name"
              :value="roomType.id"
            />
          </el-select>
        </div>

        <el-button type="primary" @click="goToBulkUpdate" size="small">
          批量更新
        </el-button>
      </div>
    </div>

    <div class="price-table-container">
      <el-table
        :data="priceTableData"
        border
        v-loading="loading"
        class="price-table"
        :header-cell-style="headerCellStyle"
        :cell-style="cellStyle"
        :row-class-name="getRowClassName"
      >
        <el-table-column label="日期" width="180" fixed="left">
          <template #default="{ row }">
            <div class="room-info-cell">
              <div v-if="row.isRoomHeader" class="room-type-header-cell">
                <div class="room-type-name">{{ row.roomTypeName }}</div>
              </div>
              <div v-else class="price-plan-cell">
                <div class="price-plan-info">
                  <div class="plan-name">{{ row.pricePlanName }}</div>
                  <div v-if="row.channelCount" class="channel-info">
                    <el-link type="primary" :underline="false" @click="showChannels(row)">
                      <el-icon><Link /></el-icon>
                      {{ row.channelCount }}
                    </el-link>
                  </div>
                </div>
              </div>
            </div>
          </template>
        </el-table-column>

        <el-table-column
          v-for="date in dateColumns"
          :key="date.dateStr"
          :label="date.label"
          width="90"
          align="center"
        >
          <template #header>
            <div class="date-header">
              <div class="date-day">{{ date.dayLabel }}</div>
              <div class="date-weekday">{{ date.weekday }}</div>
            </div>
          </template>

          <template #default="{ row }">
            <div v-if="row.isRoomHeader" class="empty-cell">
              <span class="room-count-value">{{ row.dates[date.dateStr]?.rooms || 0 }}</span>
            </div>
            <div
              v-else
              class="price-cell clickable"
              @click="openPriceEditDialog(row, date)"
            >
                <div class="price-content">
                  <div class="price-value">
                  {{ formatPrice(getDisplayPrice(row, date.dateStr)) }}
                  </div>
                  <div class="rooms-count">
                    <el-icon><Moon /></el-icon>
                    {{ row.dates[date.dateStr]?.rooms || 0 }}
                  </div>
              </div>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 价格编辑侧边栏 -->
    <el-drawer
      v-model="showPriceEditDialog"
      title="编辑"
      direction="rtl"
      size="500px"
      :before-close="closePriceEditDialog"
    >
      <div class="drawer-content">
        <el-form :model="editForm" label-width="80px" label-position="top">
          <el-form-item label="日期范围" required>
            <div class="date-range-input">
              <el-date-picker
                v-model="editForm.startDate"
                type="date"
                placeholder="开始日期"
                format="YYYY/MM/DD"
                value-format="YYYY-MM-DD"
                size="default"
                style="width: 100%"
              />
              <span class="date-separator">至</span>
              <el-date-picker
                v-model="editForm.endDate"
                type="date"
                placeholder="结束日期"
                format="YYYY/MM/DD"
                value-format="YYYY-MM-DD"
                size="default"
                style="width: 100%"
              />
            </div>
          </el-form-item>

          <el-form-item label="星期几" required>
            <el-checkbox-group v-model="editForm.weekdays" class="weekday-group" @change="handleWeekdayChange">
              <el-checkbox :label="0">全部</el-checkbox>
              <el-checkbox :label="1">周一</el-checkbox>
              <el-checkbox :label="2">周二</el-checkbox>
              <el-checkbox :label="3">周三</el-checkbox>
              <el-checkbox :label="4">周四</el-checkbox>
              <el-checkbox :label="5">周五</el-checkbox>
              <el-checkbox :label="6">周六</el-checkbox>
              <el-checkbox :label="7">周日</el-checkbox>
            </el-checkbox-group>
          </el-form-item>

          <el-form-item>
            <el-link type="primary" :underline="false" @click="addMoreSegments">
              增加时段
            </el-link>
          </el-form-item>

          <el-form-item label="设置" required>
            <el-select v-model="editForm.settingType" placeholder="请选择" style="width: 100%">
              <el-option label="价格" value="price" />
              <el-option label="最小入住天数" value="minStay" />
              <el-option label="最大入住天数" value="maxStay" />
            </el-select>
          </el-form-item>

          <el-divider />

          <div class="room-price-section">
            <div class="section-title">
              <span>{{ editForm.roomTypeName }}</span>
              <span class="room-code">{{ editForm.roomCode }}</span>
            </div>

            <!-- 价格输入 -->
            <el-form-item v-if="editForm.settingType === 'price'" :label="editForm.pricePlanName" required>
              <el-input
                v-model="editForm.price"
                placeholder="请输入价格"
                type="number"
              >
                <template #append>JPY</template>
              </el-input>
            </el-form-item>

            <!-- 最小入住天数输入 -->
            <el-form-item v-if="editForm.settingType === 'minStay'" label="最小入住天数" required>
              <el-input
                v-model="editForm.minStay"
                placeholder="请输入最小入住天数"
                type="number"
                :min="1"
                :max="99"
              >
                <template #append>天</template>
              </el-input>
            </el-form-item>

            <!-- 最大入住天数输入 -->
            <el-form-item v-if="editForm.settingType === 'maxStay'" label="最大入住天数" required>
              <el-input
                v-model="editForm.maxStay"
                placeholder="请输入最大入住天数"
                type="number"
                :min="1"
                :max="99"
              >
                <template #append>天</template>
              </el-input>
            </el-form-item>
          </div>
        </el-form>
      </div>

      <template #footer>
        <div class="drawer-footer">
          <el-button @click="closePriceEditDialog" size="large">取消</el-button>
          <el-button type="primary" @click="savePriceEdit" :loading="saving" size="large">
            确定
          </el-button>
        </div>
      </template>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowLeft, ArrowRight, Moon, Link } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { getAllRoomTypes } from '@/api/roomType'
import { getAllPricePlans } from '@/api/pricePlan'
import { getRoomPriceManagementData, updatePriceByPlan, type RoomPriceManagementDTO } from '@/api/roomPrice'
import { getChannelPrices, type ChannelPriceDTO } from '@/api/pricelabs'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
const saving = ref(false)
const selectedDate = ref(new Date().toISOString().split('T')[0])
const selectedRoomTypeId = ref<number | null>(null)

const roomTypes = ref<any[]>([])
const pricePlans = ref<any[]>([])
const priceData = ref<RoomPriceManagementDTO[]>([])

const showPriceEditDialog = ref(false)
const priceLabsBasePriceMap = ref<Record<string, number>>({})

// 保存上一次的星期几选择值
let previousEditFormWeekdays: number[] = []

const editForm = ref({
  roomTypeId: 0,
  pricePlanId: 0,
  roomTypeName: '',
  pricePlanName: '',
  roomCode: '',
  date: '',
  dateLabel: '',
  startDate: '',
  endDate: '',
  weekdays: [] as number[],
  settingType: 'price',
  price: 0,
  availableRooms: 0,
  minStay: '',
  maxStay: ''
})

const dateColumns = computed(() => {
  const columns = []
  const startDate = new Date(selectedDate.value)

  for (let i = 0; i < 17; i++) {
    const currentDate = new Date(startDate)
    currentDate.setDate(startDate.getDate() + i)

    const dateStr = currentDate.toISOString().split('T')[0]
    const month = currentDate.getMonth() + 1
    const day = currentDate.getDate()
    const weekdayIndex = currentDate.getDay()
    const weekdays = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']

    columns.push({
      dateStr,
      dayLabel: `${month}月${day}日`,
      weekday: weekdays[weekdayIndex],
      label: `${month}月${day}日 ${weekdays[weekdayIndex]}`
    })
  }

  return columns
})

interface PriceTableRow {
  id: string
  roomTypeId: number
  roomTypeName: string
  pricePlanId?: number
  pricePlanName?: string
  channelCount?: number
  isRoomHeader: boolean
  dates: {
    [dateStr: string]: {
      price?: number
      rooms?: number
    }
  }
}

const buildPriceLabsKey = (roomTypeId: number, pricePlanId: number, priceDate: string): string => {
  return `${roomTypeId}_${pricePlanId}_${priceDate}`
}

const getPriceLabsBasePrice = (row: PriceTableRow, priceDate: string): number | undefined => {
  if (row.isRoomHeader) return undefined
  if (!row.pricePlanId) return undefined

  const key = buildPriceLabsKey(row.roomTypeId, row.pricePlanId, priceDate)
  return priceLabsBasePriceMap.value[key]
}

const getDisplayPrice = (row: PriceTableRow, priceDate: string): number | undefined => {
  const priceLabsBasePrice = getPriceLabsBasePrice(row, priceDate)
  if (priceLabsBasePrice !== undefined && priceLabsBasePrice !== null) {
    return priceLabsBasePrice
  }
  return row.dates[priceDate]?.price
}

const priceTableData = computed<PriceTableRow[]>(() => {
  const rows: PriceTableRow[] = []

  console.log('🔄 计算表格数据:', {
    房型数量: roomTypes.value.length,
    价格计划数量: pricePlans.value.length,
    价格数据记录数: priceData.value.length,
    日期列数: dateColumns.value.length
  })

  // 筛选要显示的房型
  let displayRoomTypes = roomTypes.value
  if (selectedRoomTypeId.value !== null) {
    displayRoomTypes = roomTypes.value.filter(rt => rt.id === selectedRoomTypeId.value)
  }

  // 按房型展示
  displayRoomTypes.forEach(roomType => {
    // 为每个房型创建一个头部行，包含每个日期的剩余房间数
    const headerDates: { [dateStr: string]: { rooms: number } } = {}

    dateColumns.value.forEach(dateCol => {
      // 统计该房型在该日期的剩余房间数
      // 从priceData中获取availableRooms，如果没有则使用房型的totalRooms
      const priceRecords = priceData.value.filter(
        p => p.roomTypeId === roomType.id && p.priceDate === dateCol.dateStr
      )

      let availableRooms = roomType.totalRooms || 0
      if (priceRecords.length > 0) {
        // 使用第一条记录的availableRooms（所有价格计划共享同一个房型的剩余房间数）
        // 注意: availableRooms 可能为 0,不能用 || 运算符
        availableRooms = priceRecords[0].availableRooms !== undefined && priceRecords[0].availableRooms !== null
          ? priceRecords[0].availableRooms
          : (roomType.totalRooms || 0)
      }

      headerDates[dateCol.dateStr] = { rooms: availableRooms }
    })

    rows.push({
      id: `roomtype-header-${roomType.id}`,
      roomTypeId: roomType.id,
      roomTypeName: roomType.name,
      isRoomHeader: true,
      dates: headerDates
    })

    // 只为该房型实际关联的价格计划创建一行
    // 获取该房型在当前日期范围内有数据的价格计划
    const roomTypePricePlans = new Set(
      priceData.value
        .filter(p => p.roomTypeId === roomType.id)
        .map(p => p.pricePlanId)
        .filter(id => id !== null && id !== undefined)
    )

    console.log(`📋 房型 [${roomType.name}] 关联的价格计划:`, Array.from(roomTypePricePlans))

    pricePlans.value.forEach(plan => {
      // 只显示该房型已关联的价格计划
      if (!roomTypePricePlans.has(plan.id)) {
        return
      }

      const dates: { [dateStr: string]: { price: number; rooms: number } } = {}

      dateColumns.value.forEach(dateCol => {
        const priceRecord = priceData.value.find(
          p => p.roomTypeId === roomType.id &&
               p.pricePlanId === plan.id &&
               p.priceDate === dateCol.dateStr
        )

        dates[dateCol.dateStr] = {
          price: priceRecord?.price || 0,
          rooms: priceRecord?.minStay || 1
        }

        if (priceRecord) {
          console.log(`  📌 ${dateCol.dateStr}: 价格=${priceRecord.price}, 最小入住=${priceRecord.minStay}`)
        }
      })

      rows.push({
        id: `roomtype-plan-${roomType.id}-${plan.id}`,
        roomTypeId: roomType.id,
        roomTypeName: roomType.name,
        pricePlanId: plan.id,
        pricePlanName: plan.name,
        channelCount: 0, // TODO: 从后端获取关联渠道数
        isRoomHeader: false,
        dates
      })
    })
  })

  return rows
})

const showChannels = (row: PriceTableRow) => {
  ElMessage.info(`价格计划: ${row.pricePlanName} 的渠道信息`)
}

const formatPrice = (price: number | undefined): string => {
  if (price === undefined || price === null) return '¥0'
  return `¥${price.toLocaleString()}`
}

const headerCellStyle = {
  background: '#f5f7fa',
  color: '#606266',
  textAlign: 'center' as const,
  fontSize: '12px',
  fontWeight: '500',
  padding: '8px 0'
}

const cellStyle = {
  textAlign: 'center' as const,
  padding: '0',
  fontSize: '12px'
}

const getRowClassName = ({ row }: { row: PriceTableRow }): string => {
  return row.isRoomHeader ? 'room-type-header-row' : 'price-plan-row'
}

const previousDay = () => {
  const current = new Date(selectedDate.value)
  current.setDate(current.getDate() - 1)
  selectedDate.value = current.toISOString().split('T')[0]
}

const nextDay = () => {
  const current = new Date(selectedDate.value)
  current.setDate(current.getDate() + 1)
  selectedDate.value = current.toISOString().split('T')[0]
}

const previousWeek = () => {
  const current = new Date(selectedDate.value)
  current.setDate(current.getDate() - 7)
  selectedDate.value = current.toISOString().split('T')[0]
}

const nextWeek = () => {
  const current = new Date(selectedDate.value)
  current.setDate(current.getDate() + 7)
  selectedDate.value = current.toISOString().split('T')[0]
}

const onDateChange = () => {
  loadPriceData()
}

const handleFilterChange = () => {
  loadPriceData()
}

// 加载房型列表
const loadRoomTypes = async () => {
  try {
    console.log('🏠 开始加载房型列表...')
    const response = await getAllRoomTypes()
    console.log('🏠 房型API响应:', response)
    if (response.success && response.data) {
      roomTypes.value = response.data
      console.log('✅ 房型列表已加载:', response.data)
    } else {
      console.warn('⚠️ 房型API返回失败:', response.message)
    }
  } catch (error) {
    console.error('❌ 加载房型列表失败:', error)
    ElMessage.error('加载房型列表失败')
  }
}

// 加载价格计划列表
const loadPricePlans = async () => {
  try {
    console.log('💰 开始加载价格计划列表...')
    const userId = userStore.currentUser?.id
    if (!userId) {
      console.warn('⚠️ 用户ID不存在,无法加载价格计划')
      return
    }

    console.log('💰 请求价格计划, userId:', userId)
    const response = await getAllPricePlans(userId)
    console.log('💰 价格计划API响应:', response)
    if (response && response.data) {
      pricePlans.value = response.data
      console.log('✅ 价格计划已加载:', response.data)
    } else {
      console.warn('⚠️ 价格计划API返回失败')
    }
  } catch (error) {
    console.error('❌ 加载价格计划列表失败:', error)
    ElMessage.error('加载价格计划列表失败')
  }
}

// 加载价格数据
const loadPriceLabsBasePrices = async (
  startDate: string,
  endDate: string,
  roomTypeId?: number,
) => {
  try {
    const response = await getChannelPrices({
      roomTypeId,
      startDate,
      endDate,
    })

    if (!response.success || !response.data) {
      priceLabsBasePriceMap.value = {}
      return
    }

    const nextMap: Record<string, number> = {}
    for (const item of response.data as ChannelPriceDTO[]) {
      const key = buildPriceLabsKey(item.roomTypeId, item.pricePlanId, item.priceDate)
      if (nextMap[key] === undefined && item.basePrice !== undefined && item.basePrice !== null) {
        nextMap[key] = item.basePrice
      }
    }
    priceLabsBasePriceMap.value = nextMap
  } catch (error) {
    console.error('加载 PriceLabs 回传价格失败:', error)
    priceLabsBasePriceMap.value = {}
  }
}

const loadPriceData = async () => {
  try {
    loading.value = true

    // 计算日期范围
    const startDate = selectedDate.value
    const endDate = new Date(selectedDate.value)
    endDate.setDate(endDate.getDate() + 16)
    const endDateStr = endDate.toISOString().split('T')[0]

    console.log('📅 加载价格数据:', {
      startDate,
      endDate: endDateStr,
      roomTypeId: selectedRoomTypeId.value
    })

    const response = await getRoomPriceManagementData(
      startDate,
      endDateStr,
      selectedRoomTypeId.value || undefined
    )

    await loadPriceLabsBasePrices(startDate, endDateStr, selectedRoomTypeId.value || undefined)

    console.log('📦 API响应:', response)

    if (response.success && response.data) {
      priceData.value = response.data
      console.log('✅ 价格数据已加载:', {
        记录数: response.data.length,
        数据示例: response.data.slice(0, 2)
      })
    } else {
      console.warn('⚠️ API返回失败:', response.message)
    }
  } catch (error) {
    console.error('❌ 加载价格数据失败:', error)
    ElMessage.error('加载价格数据失败')
  } finally {
    loading.value = false
  }
}

const openPriceEditDialog = (row: PriceTableRow, date: any) => {
  if (row.isRoomHeader) return

  const priceData = row.dates[date.dateStr]

  // 获取房型代码
  const roomType = roomTypes.value.find(rt => rt.id === row.roomTypeId)

  editForm.value = {
    roomTypeId: row.roomTypeId,
    pricePlanId: row.pricePlanId || 0,
    roomTypeName: row.roomTypeName,
    pricePlanName: row.pricePlanName || '',
    roomCode: roomType?.code || '',
    date: date.dateStr,
    dateLabel: date.label,
    startDate: date.dateStr,
    endDate: date.dateStr,
    weekdays: [],
    settingType: 'price',
    price: getDisplayPrice(row, date.dateStr) || 0,
    availableRooms: priceData?.rooms || 0,
    minStay: '',
    maxStay: ''
  }

  showPriceEditDialog.value = true
}

const addMoreSegments = () => {
  ElMessage.info('增加时段功能开发中')
}

// 处理星期几选择变化
const handleWeekdayChange = (values: number[]) => {
  // 如果新选中了"全部"
  if (values.includes(0) && !previousEditFormWeekdays.includes(0)) {
    // 选中全部,同时选中周一到周日
    editForm.value.weekdays = [0, 1, 2, 3, 4, 5, 6, 7]
  }
  // 如果取消了"全部"
  else if (!values.includes(0) && previousEditFormWeekdays.includes(0)) {
    // 取消全部,同时取消所有
    editForm.value.weekdays = []
  }
  // 如果已经选中了全部,但取消了某个具体的星期
  else if (values.includes(0) && previousEditFormWeekdays.includes(0) && values.length < previousEditFormWeekdays.length) {
    // 取消全部,只保留剩余的具体星期
    editForm.value.weekdays = values.filter(v => v !== 0)
  }
  // 如果选中了周一到周日所有具体的星期(不包括全部)
  else if (!values.includes(0) && values.length === 7 && [1, 2, 3, 4, 5, 6, 7].every(v => values.includes(v))) {
    // 自动选中全部
    editForm.value.weekdays = [0, 1, 2, 3, 4, 5, 6, 7]
  }

  // 更新上一次的值
  setTimeout(() => {
    previousEditFormWeekdays = [...editForm.value.weekdays]
  }, 0)
}

const closePriceEditDialog = () => {
  showPriceEditDialog.value = false
  // 重置上一次的星期几选择值
  previousEditFormWeekdays = []
}

const savePriceEdit = async () => {
  try {
    saving.value = true
    const operator = userStore.currentUser?.nickname || userStore.currentUser?.email || '系统'

    // 准备请求数据
    const requestData: any = {
      roomTypeId: editForm.value.roomTypeId,
      pricePlanId: editForm.value.pricePlanId,
      startDate: editForm.value.startDate,
      endDate: editForm.value.endDate,
      weekdays: editForm.value.weekdays.length > 0 ? editForm.value.weekdays : undefined
    }

    // 根据设置类型添加对应的字段
    if (editForm.value.settingType === 'price') {
      requestData.price = Number(editForm.value.price)
      requestData.availableRooms = editForm.value.availableRooms || undefined
    } else if (editForm.value.settingType === 'minStay') {
      requestData.minStay = Number(editForm.value.minStay)
      console.log('💾 准备保存最小入住天数:', requestData.minStay)
    } else if (editForm.value.settingType === 'maxStay') {
      requestData.maxStay = Number(editForm.value.maxStay)
      console.log('💾 准备保存最大入住天数:', requestData.maxStay)
    }

    console.log('💾 发送更新请求:', requestData)
    const response = await updatePriceByPlan(requestData, operator)
    console.log('💾 更新响应:', response)

    if (response.success) {
      const successMsg = editForm.value.settingType === 'price' ? '价格修改成功' :
                         editForm.value.settingType === 'minStay' ? '最小入住天数修改成功' :
                         '最大入住天数修改成功'
      ElMessage.success(successMsg)
      closePriceEditDialog()
      // 重新加载数据
      await loadPriceData()
    } else {
      ElMessage.error(response.message || '保存失败')
    }
  } catch (error) {
    console.error('保存价格失败:', error)
    ElMessage.error('保存价格失败')
  } finally {
    saving.value = false
  }
}

const goToBulkUpdate = () => {
  router.push('/accommodation/room-price-bulk-update')
}

// 监听日期变化
watch(selectedDate, () => {
  loadPriceData()
})

// 监听房型筛选变化
watch(selectedRoomTypeId, () => {
  loadPriceData()
})

// 初始化
onMounted(() => {
  loadRoomTypes()
  loadPricePlans()
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

.top-controls {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 24px;
  border-bottom: 1px solid #e4e7ed;
  background: #fff;
}

.control-left {
  display: flex;
  align-items: center;
}

.control-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.date-navigation {
  display: flex;
  align-items: center;
  gap: 10px;
}

.control-label {
  font-size: 14px;
  color: #606266;
  font-weight: 500;
  margin-right: 8px;
}

.date-picker {
  width: 150px;
}

.room-type-filter {
  display: flex;
  align-items: center;
  gap: 8px;
}

.filter-select {
  width: 200px;
}

.price-table-container {
  flex: 1;
  padding: 0;
  overflow: auto;
  background: #fff;
}

.price-table {
  width: 100%;
}

:deep(.room-type-header-row) {
  background-color: #f5f7fa;
}

:deep(.price-plan-row) {
  background-color: #fff;
}

:deep(.price-plan-row:hover) {
  background-color: #fafafa;
}

.room-info-cell {
  padding: 10px 12px;
  text-align: left;
  min-height: 45px;
  display: flex;
  align-items: center;
}

.room-type-header-cell {
  display: flex;
  align-items: center;
  width: 100%;
}

.room-type-name {
  font-weight: 600;
  color: #303133;
  font-size: 13px;
}

.price-plan-cell {
  width: 100%;
}

.price-plan-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.plan-name {
  font-size: 12px;
  color: #303133;
  font-weight: 500;
}

.channel-info {
  font-size: 11px;
}

.channel-info :deep(.el-link) {
  font-size: 11px;
  font-weight: 400;
  display: flex;
  align-items: center;
  gap: 4px;
}

.date-header {
  text-align: center;
  line-height: 1.5;
  padding: 4px 0;
}

.date-day {
  font-weight: 600;
  font-size: 12px;
  color: #303133;
}

.date-weekday {
  font-size: 11px;
  color: #909399;
  margin-top: 2px;
}

.price-cell {
  padding: 10px 8px;
  min-height: 45px;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
}

.price-cell.clickable {
  cursor: pointer;
  transition: all 0.15s ease;
}

.price-cell.clickable:hover {
  background-color: #ecf5ff;
}

.price-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
}

.price-value {
  font-weight: 600;
  color: #303133;
  font-size: 13px;
}

.rooms-count {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #67c23a;
  font-weight: 500;
}

.rooms-count .el-icon {
  font-size: 14px;
}

.empty-cell {
  min-height: 45px;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: transparent;
}

.room-count-value {
  color: #f56c6c;
  font-size: 13px;
  font-weight: 600;
}

.date-range-input {
  display: flex;
  align-items: center;
  width: 100%;
}

:deep(.el-dialog) {
  border-radius: 8px;
}

/* Drawer 样式 */
.drawer-content {
  padding: 24px;
}

.drawer-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding: 16px 24px;
  border-top: 1px solid #e4e7ed;
}

.date-range-input {
  display: flex;
  align-items: center;
  gap: 12px;
  width: 100%;
}

.date-separator {
  color: #606266;
  font-size: 14px;
  flex-shrink: 0;
}

.weekday-group {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.weekday-group :deep(.el-checkbox) {
  margin-right: 0;
}

.room-price-section {
  margin-top: 16px;
}

.section-title {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 16px;
  font-size: 14px;
  font-weight: 500;
  color: #303133;
}

.room-code {
  color: #909399;
  font-size: 12px;
  font-weight: normal;
}

:deep(.el-form-item__label) {
  font-weight: 500;
  color: #606266;
}

:deep(.el-input-number) {
  width: 100%;
}

:deep(.el-drawer__header) {
  margin-bottom: 0;
  padding: 20px 24px;
  border-bottom: 1px solid #e4e7ed;
}

:deep(.el-drawer__body) {
  padding: 0;
  display: flex;
  flex-direction: column;
}

:deep(.el-drawer__footer) {
  margin-top: auto;
}

@media (max-width: 768px) {
  .top-controls {
    flex-direction: column;
    align-items: stretch;
    gap: 12px;
  }

  .control-left,
  .control-right {
    width: 100%;
  }

  .date-navigation {
    justify-content: space-between;
  }

  .filter-select {
    width: 100%;
  }
}
</style>
