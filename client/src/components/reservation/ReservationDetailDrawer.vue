<template>
  <el-drawer v-model="drawerVisible" direction="rtl" size="1000px">
    <template #header>
      <div class="detail-header">
        <el-tabs v-model="activeDetailTab">
          <el-tab-pane label="详细信息" name="detail" />
          <el-tab-pane label="操作日志" name="log" />
          <el-tab-pane label="渠道信息" name="channel" />
        </el-tabs>
      </div>
    </template>

    <div class="booking-detail-content">
      <div v-if="activeDetailTab === 'detail'" class="detail-tab-content">
        <div class="guest-header">
          <h3>{{ selectedReservation?.guestName || '客户姓名' }}</h3>
          <div class="status-tags">
            <el-tag :type="getStatusTagType(selectedReservation?.status || 'CONFIRMED')">
              {{ getReservationStatusText(selectedReservation?.status || 'CONFIRMED') }}
            </el-tag>
            <el-tag :style="{ backgroundColor: getChannelColor(selectedReservation?.channelName), borderColor: getChannelColor(selectedReservation?.channelName), color: '#fff' }">
              {{ selectedReservation?.channelName || '自来客' }}
            </el-tag>
          </div>
        </div>

        <div class="order-summary">
          <div class="amounts">
            <div class="amount-item">
              <span>订单金额</span>
              <span class="amount">¥{{ Number(selectedReservation?.totalAmount || 0).toFixed(2) }}</span>
            </div>
            <div
              v-if="
                selectedReservation?.currentRoomPrice &&
                selectedReservation.currentRoomPrice !== selectedReservation.totalAmount
              "
              class="amount-item"
            >
              <span>当前房价</span>
              <span class="amount current-price">¥{{ Number(selectedReservation?.currentRoomPrice || 0).toFixed(2) }}</span>
            </div>
            <div class="amount-item">
              <span>已付金额</span>
              <span class="amount">¥{{ totalPayment.toFixed(2) }}</span>
            </div>
            <div class="amount-item">
              <span>还需付款</span>
              <span class="amount" :class="{ red: remainingPayment > 0, green: remainingPayment < 0 }">
                {{ remainingPayment >= 0 ? `¥${remainingPayment.toFixed(2)}` : `+¥${Math.abs(remainingPayment).toFixed(2)}` }}
              </span>
            </div>
          </div>
        </div>

        <div class="room-info-detail">
          <h4>房间信息：¥{{ Number(selectedReservation?.totalAmount || 0).toFixed(2) }} 排房</h4>
          <div class="room-card">
            <div class="room-header">
              <span class="room-name">{{ roomDisplayName }}</span>
              <span class="room-dates">{{ selectedReservation?.checkInDate || '-' }} 至 {{ selectedReservation?.checkOutDate || '-' }}，{{ channelNightsText }}</span>
              <div class="room-actions">
                <div class="actions-left">
                  <el-button
                    v-if="isConfirmedStatus(selectedReservation?.status)"
                    size="small"
                    @click="checkInFromDetail"
                  >
                    办理入住
                  </el-button>
                  <el-button
                    v-if="isCheckedInStatus(selectedReservation?.status)"
                    size="small"
                    type="danger"
                    @click="checkOutFromDetail"
                  >
                    办理退房
                  </el-button>
                  <el-button size="small" type="danger" plain @click="cancelFromDetail">
                    取消订单
                  </el-button>
                  <el-button
                    v-if="props.activeOrderTab === 'order-box'"
                    size="small"
                    @click="handleMoveOutOrderBoxFromDetail"
                  >
                    移出订单盒子
                  </el-button>
                </div>
                <div class="actions-right">
                  <span class="room-price">
                    ¥{{ Number(selectedReservation?.totalAmount || 0).toFixed(2) }}
                  </span>
                  <el-tag :type="getStatusTagType(selectedReservation?.status || 'CONFIRMED')">
                    {{ getReservationStatusText(selectedReservation?.status || 'CONFIRMED') }}
                  </el-tag>
                </div>
              </div>
            </div>
          </div>
        </div>

        <div v-if="shouldShowAssignRoom" class="assign-room-panel">
          <h4 class="panel-title">排房（按订单日期范围过滤可用房间）</h4>
          <div class="assign-row">
            <div class="assign-label">房型</div>
            <el-select
              v-model="assignRoomTypeId"
              placeholder="请选择房型"
              filterable
              :loading="assignableRoomsLoading"
              style="width: 100%"
              @change="handleRoomTypeChange"
            >
              <el-option
                v-for="roomType in assignableRoomTypes"
                :key="roomType.id"
                :label="`${roomType.name}（可用 ${roomType.availableRooms}）`"
                :value="roomType.id"
              />
            </el-select>
          </div>
          <div class="assign-row">
            <div class="assign-label">房间号</div>
            <el-select
              v-model="assignRoomId"
              placeholder="请选择房间"
              filterable
              :disabled="!assignRoomTypeId"
              :loading="assignableRoomsLoading"
              style="width: 100%"
            >
              <el-option
                v-for="room in assignableRooms"
                :key="room.id"
                :label="`${room.roomTypeName}-${room.roomNumber}`"
                :value="room.id"
              />
            </el-select>
          </div>
          <div class="assign-actions">
            <el-button :loading="assignableRoomsLoading" @click="refreshAssignableRoomTypes">
              刷新可用房间
            </el-button>
            <el-button
              type="primary"
              :disabled="!assignRoomId"
              :loading="assignRoomSubmitting"
              @click="submitAssignRoom"
            >
              确认排房
            </el-button>
          </div>
        </div>

        <div class="expandable-sections">
          <el-collapse v-model="activeCollapsePanels">
            <el-collapse-item name="1">
              <template #title>
                <span>其他消费：+¥{{ Math.abs(totalConsumption).toFixed(2) }}</span>
              </template>
              <el-table
                v-if="consumptionList.length > 0"
                :data="consumptionList"
                border
                style="width: 100%"
                :header-cell-style="{ background: '#f5f7fa', color: '#606266' }"
              >
                <el-table-column prop="item" label="消费项目" width="180">
                  <template #default="{ row }">
                    {{ row.item }} × {{ row.quantity }}
                  </template>
                </el-table-column>
                <el-table-column prop="amount" label="消费金额" width="140" align="right">
                  <template #default="{ row }">
                    ¥{{ Number(row.amount || 0).toFixed(2) }}
                  </template>
                </el-table-column>
                <el-table-column prop="date" label="消费日期" width="140" />
                <el-table-column prop="createdBy" label="录入人" min-width="120" />
                <el-table-column label="操作" width="100" align="center">
                  <template #default="{ row }">
                    <el-button link type="danger" @click="handleDeleteConsumption(row.id)">删除</el-button>
                  </template>
                </el-table-column>
              </el-table>
              <el-empty v-else description="暂无消费记录" :image-size="60" />
            </el-collapse-item>

            <el-collapse-item name="2">
              <template #title>
                <span>收款金额：¥{{ totalPayment.toFixed(2) }}</span>
              </template>
              <el-table
                v-if="paymentList.length > 0"
                :data="paymentList"
                border
                style="width: 100%"
                :header-cell-style="{ background: '#f5f7fa', color: '#606266' }"
              >
                <el-table-column prop="type" label="项目" width="120" />
                <el-table-column prop="paymentMethod" label="支付方式" width="140" />
                <el-table-column prop="amount" label="金额" width="120" align="right">
                  <template #default="{ row }">
                    ¥{{ Number(row.amount || 0).toFixed(2) }}
                  </template>
                </el-table-column>
                <el-table-column prop="date" label="日期" width="140" />
                <el-table-column label="操作" min-width="100" align="center">
                  <template #default="{ row }">
                    <el-button link type="danger" @click="handleDeletePayment(row.id)">删除</el-button>
                  </template>
                </el-table-column>
              </el-table>
              <el-empty v-else description="暂无收款记录" :image-size="60" />
            </el-collapse-item>

            <el-collapse-item title="订单提醒：0个" name="3">
              <el-empty description="暂无提醒" :image-size="60" />
            </el-collapse-item>
          </el-collapse>
        </div>

        <div class="order-info">
          <p><strong>订单号：</strong>{{ selectedReservation?.orderNumber || '-' }}</p>
          <p><strong>渠道订单号：</strong>{{ channelOrderNumberText }}</p>
          <p><strong>客人手机：</strong>{{ selectedReservation?.phone || '-' }}</p>
          <p><strong>备注：</strong>{{ selectedReservation?.notes || '-' }}</p>
        </div>

        <div class="more-actions">
          <el-dropdown @command="handleMoreActions">
            <el-button link>
              更多操作
              <el-icon><ArrowDown /></el-icon>
            </el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item v-if="props.activeOrderTab === 'order-box'" command="moveOutOrderBox">
                  移出订单盒子
                </el-dropdown-item>
                <el-dropdown-item command="cancelReservation">取消订单</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </div>

      <div v-if="activeDetailTab === 'log'" class="operation-logs" v-loading="operationLogsLoading">
        <div class="log-filter-buttons">
          <el-button :type="logFilterType === 'all' ? 'primary' : 'default'" size="small" @click="logFilterType = 'all'">
            全部日志
          </el-button>
          <el-button :type="logFilterType === 'order' ? 'primary' : 'default'" size="small" @click="logFilterType = 'order'">
            订单日志
          </el-button>
          <el-button :type="logFilterType === 'billing' ? 'primary' : 'default'" size="small" @click="logFilterType = 'billing'">
            账单日志
          </el-button>
        </div>

        <el-timeline class="operation-timeline">
          <el-timeline-item
            v-for="log in filteredOperationLogs"
            :key="log.id"
            :timestamp="log.timestamp"
            placement="top"
          >
            <div class="log-item">
              <div class="log-header">
                <span class="log-action">{{ log.action }}</span>
                <span class="log-operator">操作人: {{ log.operator }}</span>
              </div>
              <div v-if="log.content" class="log-content">{{ log.content }}</div>
              <div v-if="log.details && log.details.length > 0" class="log-details">
                <div v-for="(detail, index) in log.details" :key="index" class="detail-item">
                  <span class="detail-label">{{ detail.label }}:</span>
                  <span class="detail-value">{{ detail.value }}</span>
                </div>
              </div>
            </div>
          </el-timeline-item>
        </el-timeline>

        <el-empty v-if="filteredOperationLogs.length === 0" description="暂无操作日志" />
      </div>

      <div v-if="activeDetailTab === 'channel'" class="channel-info-content" v-loading="channelInfoLoading">
        <div class="channel-header">
          <div class="channel-logo-wrapper">
            <div class="channel-logo">
              <span class="logo-text">{{ channelLogoText }}</span>
            </div>
            <h3>{{ channelInfo?.channelName || selectedReservation?.channelName || '自来客' }}</h3>
          </div>
        </div>

        <div class="channel-section">
          <h4>订单详情</h4>
          <div class="info-grid">
            <div class="info-item">
              <span class="label">渠道订单号:</span>
              <span class="value">{{ channelInfo?.channelOrderNumber || channelOrderNumberText }}</span>
            </div>
            <div class="info-item">
              <span class="label">预订状态:</span>
              <el-tag :type="getStatusTagType(selectedReservation?.status || 'CONFIRMED')">
                {{ getReservationStatusText(selectedReservation?.status || 'CONFIRMED') }}
              </el-tag>
            </div>
            <div class="info-item">
              <span class="label">预订日期:</span>
              <span class="value">{{ channelInfo?.bookingDate || selectedReservation?.createdAt || '-' }}</span>
            </div>
            <div class="info-item">
              <span class="label">支付方式:</span>
              <el-tag v-if="channelInfo?.paymentMethod" type="warning">{{ channelInfo.paymentMethod }}</el-tag>
              <span v-else class="value">-</span>
            </div>
          </div>
        </div>

        <div class="channel-section">
          <h4>价格信息</h4>
          <div class="price-table">
            <el-table :data="channelPriceRows" border>
              <el-table-column prop="label" label="" width="150" />
              <el-table-column prop="value" label="" align="right" />
            </el-table>
          </div>
        </div>

        <div class="channel-section">
          <h4>房间详情</h4>
          <div class="info-grid">
            <div class="info-item">
              <span class="label">房型:</span>
              <span class="value">{{ selectedReservation?.roomTypeName || channelInfo?.roomType || '-' }}</span>
            </div>
            <div class="info-item">
              <span class="label">客人姓名:</span>
              <span class="value">
                {{ selectedReservation?.guestName || channelInfo?.guestName || '-' }}
                ({{ channelInfo?.adults ?? 1 }}成人, {{ channelInfo?.children ?? 0 }}儿童)
              </span>
            </div>
            <div class="info-item">
              <span class="label">入离日期:</span>
              <span class="value">{{ selectedReservation?.checkInDate || '-' }} ~ {{ selectedReservation?.checkOutDate || '-' }}</span>
            </div>
            <div class="info-item">
              <span class="label">间夜数:</span>
              <span class="value">{{ channelNightsText }}</span>
            </div>
            <div class="info-item">
              <span class="label">价格计划:</span>
              <span class="value">{{ channelInfo?.pricePlan || '-' }}</span>
            </div>
          </div>
        </div>

        <div class="channel-section">
          <h4>特殊需求</h4>
          <div class="special-requests">
            <p v-if="channelInfo?.specialRequests || selectedReservation?.notes">
              {{ channelInfo?.specialRequests || selectedReservation?.notes }}
            </p>
            <el-empty v-else description="无特殊需求" :image-size="60" />
          </div>
        </div>
      </div>

      <div class="detail-footer">
        <el-button @click="handlePrint">打印</el-button>
        <el-button type="danger" plain @click="cancelFromDetail">取消订单</el-button>
        <el-button
          v-if="isConfirmedStatus(selectedReservation?.status)"
          type="success"
          @click="checkInFromDetail"
        >
          办理入住
        </el-button>
        <el-button
          v-if="isCheckedInStatus(selectedReservation?.status)"
          type="danger"
          @click="checkOutFromDetail"
        >
          办理退房
        </el-button>
      </div>
    </div>
  </el-drawer>
</template>

<script setup lang="ts">
import { ArrowDown } from '@element-plus/icons-vue'
import { ElLoading, ElMessage } from 'element-plus'
import { computed, ref, watch } from 'vue'
import { deleteConsumption, getConsumptionsByReservationId, type ConsumptionDTO } from '@/api/consumption'
import { getOperationLogsByReservationId, type OperationLogDTO } from '@/api/operationLog'
import { getOrderBoxList, moveOutOrderBox } from '@/api/orderBox'
import { deletePayment, getPaymentsByReservationId, type PaymentDTO } from '@/api/payment'
import {
  assignReservationRoom,
  cancelReservation,
  checkInReservation,
  checkOutReservation,
  getAssignableRooms,
  getReservationById,
  getReservationChannelInfo,
  type AssignableRoomDTO,
  type AssignableRoomTypeDTO,
  type ReservationChannelInfoDTO,
  type ReservationDTO,
} from '@/api/reservation'

const props = withDefaults(
  defineProps<{
    modelValue: boolean
    reservationId: number | null
    activeOrderTab?: string
  }>(),
  {
    activeOrderTab: 'all',
  },
)

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
  (e: 'updated'): void
}>()

const drawerVisible = computed({
  get: () => props.modelValue,
  set: (value: boolean) => emit('update:modelValue', value),
})

const activeDetailTab = ref<'detail' | 'log' | 'channel'>('detail')
const selectedReservation = ref<ReservationDTO | null>(null)
const operationLogsLoading = ref(false)
const operationLogs = ref<OperationLogDTO[]>([])
const logFilterType = ref<'all' | 'order' | 'billing'>('all')
const channelInfoLoading = ref(false)
const channelInfo = ref<ReservationChannelInfoDTO | null>(null)
const consumptionList = ref<ConsumptionDTO[]>([])
const paymentList = ref<PaymentDTO[]>([])
const totalConsumption = ref(0)
const totalPayment = ref(0)
const activeCollapsePanels = ref<string[]>(['1', '2', '3'])

const assignableRoomsLoading = ref(false)
const assignRoomSubmitting = ref(false)
const assignRoomTypeId = ref<number | null>(null)
const assignRoomId = ref<number | null>(null)
const assignableRoomTypes = ref<AssignableRoomTypeDTO[]>([])
const assignableRooms = ref<AssignableRoomDTO[]>([])

const filteredOperationLogs = computed(() => {
  if (logFilterType.value === 'all') return operationLogs.value
  return operationLogs.value.filter((log) => log.type === logFilterType.value)
})

const remainingPayment = computed(() => {
  const total = Number(selectedReservation.value?.totalAmount || 0)
  return total - Number(totalPayment.value || 0) - Number(totalConsumption.value || 0)
})

const shouldShowAssignRoom = computed(() => {
  return false
})

const roomDisplayName = computed(() => {
  const roomTypeName = selectedReservation.value?.roomTypeName || '未排房'
  const roomNumber = selectedReservation.value?.roomNumber
  return roomNumber ? `${roomTypeName}-${roomNumber}` : roomTypeName
})

const channelOrderNumberText = computed(() => {
  const channelOrderNumber = selectedReservation.value?.channelOrderNumber
  if (channelOrderNumber && channelOrderNumber.trim()) return channelOrderNumber
  const orderNumber = selectedReservation.value?.orderNumber || ''
  const fallback = orderNumber.includes('_') ? orderNumber.split('_')[0] : ''
  return fallback || '-'
})

const channelLogoText = computed(() => {
  const name = channelInfo.value?.channelName || selectedReservation.value?.channelName || ''
  const normalized = name.replace(/\.com$/i, '').trim()
  if (!normalized) return 'OTA'
  return normalized.length > 10 ? normalized.slice(0, 10) : normalized
})

const channelNightsText = computed(() => {
  const nights = channelInfo.value?.nights
  if (typeof nights === 'number' && nights > 0) {
    return `${nights}晚`
  }
  const checkIn = selectedReservation.value?.checkInDate
  const checkOut = selectedReservation.value?.checkOutDate
  if (!checkIn || !checkOut) return '-'
  const start = new Date(checkIn)
  const end = new Date(checkOut)
  if (Number.isNaN(start.getTime()) || Number.isNaN(end.getTime())) return '-'
  const diffDays = Math.round((end.getTime() - start.getTime()) / (1000 * 60 * 60 * 24))
  return diffDays > 0 ? `${diffDays}晚` : '-'
})

const channelPriceRows = computed(() => {
  const totalAmount = channelInfo.value?.totalAmount ?? selectedReservation.value?.totalAmount
  return [
    { label: '总价', value: formatMoney(totalAmount) },
    { label: '佣金', value: formatMoney(channelInfo.value?.commission) },
    { label: '其他附加费', value: formatMoney(channelInfo.value?.otherFees) },
  ]
})

const getStatusTagType = (status: string) => {
  const normalizedStatus = status.toUpperCase()
  if (normalizedStatus === 'CONFIRMED' || normalizedStatus === 'NEW') return 'success'
  if (normalizedStatus === 'REQUESTED') return 'warning'
  if (normalizedStatus === 'CHECKED_IN') return 'primary'
  if (normalizedStatus === 'CHECKED_OUT') return 'info'
  if (normalizedStatus === 'CANCELLED') return 'danger'
  return 'info'
}

const getReservationStatusText = (status: string) => {
  const normalizedStatus = status.toUpperCase()
  if (normalizedStatus === 'CONFIRMED' || normalizedStatus === 'NEW') return '已确认'
  if (normalizedStatus === 'REQUESTED') return '待确认'
  if (normalizedStatus === 'CHECKED_IN') return '已入住'
  if (normalizedStatus === 'CHECKED_OUT') return '已退房'
  if (normalizedStatus === 'CANCELLED') return '已取消'
  return status || '-'
}

const getChannelColor = (channelName?: string) => {
  const normalizedName = (channelName || '').toLowerCase()
  if (normalizedName.includes('booking')) return '#003580'
  if (normalizedName.includes('airbnb')) return '#ff5a5f'
  if (normalizedName.includes('agoda')) return '#5a65e0'
  return '#409eff'
}

const isConfirmedStatus = (status?: string) => {
  const normalizedStatus = (status || '').toUpperCase()
  return normalizedStatus === 'CONFIRMED' || normalizedStatus === 'NEW' || normalizedStatus === 'REQUESTED'
}

const isCheckedInStatus = (status?: string) => {
  return (status || '').toUpperCase() === 'CHECKED_IN'
}

const formatMoney = (amount?: number | null) => {
  if (amount === null || amount === undefined) return '-'
  const numberValue = Number(amount)
  return Number.isNaN(numberValue) ? '-' : `¥${numberValue.toFixed(2)}`
}

const loadReservationDetail = async (reservationId: number) => {
  try {
    const res = await getReservationById(reservationId)
    if (res.success) {
      selectedReservation.value = res.data
      return
    }
    ElMessage.error(res.message || '加载订单详情失败')
    selectedReservation.value = null
  } catch (error) {
    console.error('加载订单详情失败:', error)
    ElMessage.error('加载订单详情失败')
    selectedReservation.value = null
  }
}

const loadOperationLogs = async (reservationId: number) => {
  operationLogsLoading.value = true
  operationLogs.value = []
  try {
    const res = await getOperationLogsByReservationId(reservationId)
    operationLogs.value = res.success ? (res.data || []) : []
  } catch (error) {
    console.error('加载操作日志失败:', error)
    operationLogs.value = []
  } finally {
    operationLogsLoading.value = false
  }
}

const loadChannelInfo = async (reservationId: number) => {
  channelInfoLoading.value = true
  channelInfo.value = null
  try {
    const res = await getReservationChannelInfo(reservationId)
    channelInfo.value = res.success ? (res.data || null) : null
  } catch (error) {
    console.error('加载渠道信息失败:', error)
    channelInfo.value = null
  } finally {
    channelInfoLoading.value = false
  }
}

const loadConsumptionAndPaymentData = async (reservationId: number) => {
  try {
    const [consumptionRes, paymentRes] = await Promise.all([
      getConsumptionsByReservationId(reservationId),
      getPaymentsByReservationId(reservationId),
    ])
    consumptionList.value = consumptionRes.success ? (consumptionRes.data || []) : []
    paymentList.value = paymentRes.success ? (paymentRes.data || []) : []
    totalConsumption.value = consumptionList.value.reduce((sum, item) => sum + Number(item.amount || 0), 0)
    totalPayment.value = paymentList.value.reduce((sum, item) => sum + Number(item.amount || 0), 0)
  } catch (error) {
    console.error('加载消费/收款数据失败:', error)
    consumptionList.value = []
    paymentList.value = []
    totalConsumption.value = 0
    totalPayment.value = 0
  }
}

const refreshAssignableRoomTypes = async (reservationId?: number) => {
  const targetReservationId = reservationId ?? selectedReservation.value?.id
  if (!targetReservationId || !shouldShowAssignRoom.value) return
  assignableRoomsLoading.value = true
  try {
    const res = await getAssignableRooms(targetReservationId)
    if (!res.success) {
      ElMessage.error(res.message || '加载可排房房型失败')
      return
    }
    assignableRoomTypes.value = res.data.roomTypes || []
    assignableRooms.value = []
    assignRoomTypeId.value = null
    assignRoomId.value = null
  } catch (error) {
    console.error('加载可排房房型失败:', error)
    ElMessage.error('加载可排房房型失败')
  } finally {
    assignableRoomsLoading.value = false
  }
}

const handleRoomTypeChange = async (roomTypeId: number) => {
  const reservationId = selectedReservation.value?.id
  if (!reservationId) return
  assignRoomId.value = null
  assignableRoomsLoading.value = true
  try {
    const res = await getAssignableRooms(reservationId, roomTypeId)
    if (!res.success) {
      ElMessage.error(res.message || '加载可用房间失败')
      return
    }
    assignableRooms.value = res.data.rooms || []
  } catch (error) {
    console.error('加载可用房间失败:', error)
    ElMessage.error('加载可用房间失败')
  } finally {
    assignableRoomsLoading.value = false
  }
}

const submitAssignRoom = async () => {
  const reservationId = selectedReservation.value?.id
  const roomId = assignRoomId.value
  if (!reservationId || !roomId) return
  assignRoomSubmitting.value = true
  try {
    const res = await assignReservationRoom(reservationId, roomId)
    if (!res.success) {
      ElMessage.error(res.message || '排房失败')
      return
    }
    ElMessage.success('排房成功')
    selectedReservation.value = res.data
    await Promise.all([
      loadOperationLogs(reservationId),
      loadChannelInfo(reservationId),
      loadConsumptionAndPaymentData(reservationId),
      refreshAssignableRoomTypes(reservationId),
    ])
    emit('updated')
  } catch (error) {
    console.error('排房失败:', error)
    ElMessage.error('排房失败')
  } finally {
    assignRoomSubmitting.value = false
  }
}

const checkInFromDetail = async () => {
  const reservation = selectedReservation.value
  if (!reservation) return
  const loadingInstance = ElLoading.service({ text: '正在办理入住...' })
  try {
    const response = await checkInReservation(reservation.id)
    if (!response.success) {
      ElMessage.error(response.message || '办理入住失败')
      return
    }
    ElMessage.success(`办理入住成功: ${reservation.orderNumber}`)
    await Promise.all([
      loadReservationDetail(reservation.id),
      loadOperationLogs(reservation.id),
      loadChannelInfo(reservation.id),
      loadConsumptionAndPaymentData(reservation.id),
    ])
    emit('updated')
  } catch (error) {
    console.error('办理入住失败:', error)
    ElMessage.error('办理入住失败')
  } finally {
    loadingInstance.close()
  }
}

const checkOutFromDetail = async () => {
  const reservation = selectedReservation.value
  if (!reservation) return
  const loadingInstance = ElLoading.service({ text: '正在办理退房...' })
  try {
    const response = await checkOutReservation(reservation.id)
    if (!response.success) {
      ElMessage.error(response.message || '办理退房失败')
      return
    }
    ElMessage.success(`办理退房成功: ${reservation.orderNumber}`)
    await Promise.all([
      loadReservationDetail(reservation.id),
      loadOperationLogs(reservation.id),
      loadChannelInfo(reservation.id),
      loadConsumptionAndPaymentData(reservation.id),
    ])
    emit('updated')
  } catch (error) {
    console.error('办理退房失败:', error)
    ElMessage.error('办理退房失败')
  } finally {
    loadingInstance.close()
  }
}

const cancelFromDetail = async () => {
  const reservation = selectedReservation.value
  if (!reservation) return
  const loadingInstance = ElLoading.service({ text: '正在取消订单...' })
  try {
    const response = await cancelReservation(reservation.id)
    if (!response.success) {
      ElMessage.error(response.message || '取消订单失败')
      return
    }
    ElMessage.success(`取消订单成功: ${reservation.orderNumber}`)
    await Promise.all([
      loadReservationDetail(reservation.id),
      loadOperationLogs(reservation.id),
      loadChannelInfo(reservation.id),
      loadConsumptionAndPaymentData(reservation.id),
    ])
    emit('updated')
  } catch (error) {
    console.error('取消订单失败:', error)
    ElMessage.error('取消订单失败')
  } finally {
    loadingInstance.close()
  }
}

const handleMoveOutOrderBoxFromDetail = async () => {
  const reservation = selectedReservation.value
  if (!reservation) return
  try {
    const orderBoxResp = await getOrderBoxList()
    if (!orderBoxResp.success) {
      ElMessage.error(orderBoxResp.message || '加载订单盒子失败')
      return
    }
    const boxItem = orderBoxResp.data.find((item) => item.reservation.id === reservation.id)
    if (!boxItem) {
      ElMessage.warning('未找到订单盒子记录')
      return
    }
    const res = await moveOutOrderBox({ orderBoxItemId: boxItem.id })
    if (!res.success) {
      ElMessage.error(res.message || '移出订单盒子失败')
      return
    }
    ElMessage.success('已移出订单盒子')
    await Promise.all([
      loadReservationDetail(reservation.id),
      loadOperationLogs(reservation.id),
      loadChannelInfo(reservation.id),
      loadConsumptionAndPaymentData(reservation.id),
    ])
    emit('updated')
  } catch (error) {
    console.error('移出订单盒子失败:', error)
    ElMessage.error('移出订单盒子失败')
  }
}

const handleDeleteConsumption = async (consumptionId?: number) => {
  if (!consumptionId || !selectedReservation.value) return
  try {
    const res = await deleteConsumption(consumptionId)
    if (!res.success) {
      ElMessage.error(res.message || '删除消费失败')
      return
    }
    ElMessage.success('删除消费成功')
    await loadConsumptionAndPaymentData(selectedReservation.value.id)
    await loadOperationLogs(selectedReservation.value.id)
    emit('updated')
  } catch (error) {
    console.error('删除消费失败:', error)
    ElMessage.error('删除消费失败')
  }
}

const handleDeletePayment = async (paymentId?: number) => {
  if (!paymentId || !selectedReservation.value) return
  try {
    const res = await deletePayment(paymentId)
    if (!res.success) {
      ElMessage.error(res.message || '删除收款失败')
      return
    }
    ElMessage.success('删除收款成功')
    await loadConsumptionAndPaymentData(selectedReservation.value.id)
    await loadOperationLogs(selectedReservation.value.id)
    emit('updated')
  } catch (error) {
    console.error('删除收款失败:', error)
    ElMessage.error('删除收款失败')
  }
}

const handleMoreActions = async (command: string) => {
  if (command === 'cancelReservation') {
    await cancelFromDetail()
    return
  }
  if (command === 'moveOutOrderBox') {
    await handleMoveOutOrderBoxFromDetail()
  }
}

const handlePrint = () => {
  window.print()
}

const openOrderDetail = async (reservationId: number) => {
  activeDetailTab.value = 'detail'
  logFilterType.value = 'all'
  operationLogs.value = []
  channelInfo.value = null
  assignRoomTypeId.value = null
  assignRoomId.value = null
  assignableRoomTypes.value = []
  assignableRooms.value = []
  consumptionList.value = []
  paymentList.value = []
  totalConsumption.value = 0
  totalPayment.value = 0

  await Promise.all([
    loadReservationDetail(reservationId),
    loadOperationLogs(reservationId),
    loadChannelInfo(reservationId),
    loadConsumptionAndPaymentData(reservationId),
  ])

  await refreshAssignableRoomTypes(reservationId)
}

watch(
  () => [props.modelValue, props.reservationId] as const,
  async ([visible, reservationId], [prevVisible, prevReservationId]) => {
    if (!visible || !reservationId) return
    if (!prevVisible || reservationId !== prevReservationId) {
      await openOrderDetail(reservationId)
    }
  },
)
</script>

<style scoped>
:deep(.el-drawer__header) {
  margin-bottom: 0;
  padding-bottom: 0;
}

:deep(.el-drawer__body) {
  padding: 0;
  overflow-y: auto;
}

.detail-header {
  border-bottom: 1px solid #e9ecef;
}

.booking-detail-content {
  padding: 0;
}

.detail-tab-content {
  padding: 20px;
}

.guest-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.guest-header h3 {
  margin: 0;
}

.status-tags {
  display: flex;
  gap: 6px;
  align-items: center;
}

.order-summary {
  background: #f8f9fa;
  padding: 15px;
  border-radius: 6px;
  margin-bottom: 20px;
}

.amounts {
  display: flex;
  justify-content: space-between;
  gap: 8px;
}

.amount-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
}

.amount {
  font-weight: bold;
  font-size: 16px;
}

.amount.red {
  color: #ff4d4f;
}

.amount.green {
  color: #10b981;
}

.current-price {
  color: #f56c6c;
}

.room-info-detail h4 {
  margin: 0 0 10px;
  color: #303133;
}

.room-card {
  border: 1px solid #e9ecef;
  border-radius: 6px;
  padding: 15px;
}

.room-header {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.room-name {
  font-weight: 600;
  color: #303133;
}

.room-dates {
  color: #606266;
}

.room-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.actions-left {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.actions-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.room-price {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
}

.assign-room-panel {
  margin-top: 16px;
  padding: 12px;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  background: #fafafa;
}

.panel-title {
  margin: 0 0 12px;
  font-size: 14px;
  font-weight: 600;
}

.assign-row {
  display: flex;
  gap: 12px;
  align-items: center;
  margin-bottom: 12px;
}

.assign-label {
  width: 60px;
  color: #606266;
  font-size: 13px;
}

.assign-actions {
  display: flex;
  gap: 10px;
  justify-content: flex-end;
}

.expandable-sections {
  margin: 20px 0;
}

.order-info {
  margin: 20px 0;
}

.order-info p {
  margin: 6px 0;
  color: #606266;
}

.more-actions {
  margin: 20px 0 0;
}

.operation-logs {
  padding: 20px;
}

.log-filter-buttons {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
  padding-bottom: 12px;
  border-bottom: 1px solid #e4e7ed;
}

.operation-timeline {
  margin-top: 0;
}

.log-item {
  background: #f5f7fa;
  padding: 16px;
  border-radius: 8px;
  border: 1px solid #e4e7ed;
}

.log-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
  gap: 12px;
}

.log-action {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
}

.log-operator {
  font-size: 13px;
  color: #909399;
}

.log-content {
  font-size: 14px;
  color: #606266;
  margin-bottom: 8px;
  line-height: 1.6;
}

.log-details {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 8px;
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid #dcdfe6;
}

.detail-item {
  font-size: 13px;
  color: #606266;
  display: flex;
  gap: 8px;
}

.detail-label {
  color: #909399;
  min-width: 80px;
}

.detail-value {
  color: #303133;
  font-weight: 500;
}

.channel-info-content {
  padding: 0;
}

.channel-header {
  padding: 32px 24px;
  background: linear-gradient(135deg, #7b68ee 0%, #9370db 100%);
  margin-bottom: 0;
}

.channel-logo-wrapper {
  display: flex;
  align-items: center;
  gap: 20px;
}

.channel-logo {
  width: 100px;
  height: 100px;
  background: #003580;
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.2);
}

.logo-text {
  font-size: 20px;
  font-weight: 700;
  color: #fff;
  letter-spacing: -0.5px;
}

.channel-header h3 {
  margin: 0;
  font-size: 28px;
  font-weight: 600;
  color: #fff;
}

.channel-section {
  padding: 24px;
  border-bottom: 1px solid #e4e7ed;
}

.channel-section:last-of-type {
  border-bottom: none;
}

.channel-section h4 {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  margin: 0 0 20px;
}

.info-grid {
  display: grid;
  grid-template-columns: 1fr;
  gap: 16px;
}

.info-item {
  display: grid;
  grid-template-columns: 120px 1fr;
  gap: 16px;
  font-size: 14px;
  align-items: start;
}

.info-item .label {
  color: #606266;
}

.info-item .value {
  color: #303133;
}

.price-table :deep(.el-table) {
  border: 1px solid #e4e7ed;
}

.price-table :deep(.el-table th) {
  background: #f5f7fa;
  color: #606266;
  font-weight: 600;
}

.special-requests p {
  font-size: 14px;
  color: #606266;
  line-height: 1.8;
  margin: 0;
}

.detail-footer {
  position: sticky;
  bottom: 0;
  border-top: 1px solid #e9ecef;
  background: #fff;
  padding: 14px 20px;
  display: flex;
  gap: 10px;
  justify-content: flex-end;
}
</style>
