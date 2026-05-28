<template>
  <el-drawer v-model="drawerVisible" direction="rtl" size="1000px">
    <template #header>
      <div class="detail-header">
        <el-tabs v-model="activeDetailTab">
          <el-tab-pane :label="t('stage6.components.reservationDetail.tabs.detail')" name="detail" />
          <el-tab-pane :label="t('stage6.components.reservationDetail.tabs.log')" name="log" />
          <el-tab-pane :label="t('stage6.components.reservationDetail.tabs.channel')" name="channel" />
        </el-tabs>
      </div>
    </template>

    <div class="booking-detail-content">
      <div v-if="activeDetailTab === 'detail'" class="detail-tab-content">
        <div class="guest-header">
          <div class="guest-main">
            <h3>{{ selectedReservation?.guestName || t('stage6.components.reservationDetail.guestFallback') }}</h3>
            <el-button
              type="primary"
              plain
              size="small"
              :disabled="!selectedReservation"
              @click="goToMessages"
            >
              {{ t('stage6.components.reservationDetail.actions.goToChat') }}
            </el-button>
          </div>
          <div class="status-tags">
            <el-tag :type="getStatusTagType(selectedReservation?.status || 'CONFIRMED')">
              {{ getReservationStatusText(selectedReservation?.status || 'CONFIRMED') }}
            </el-tag>
            <el-tag :style="{ backgroundColor: getChannelColor(selectedReservation?.channelName), borderColor: getChannelColor(selectedReservation?.channelName), color: '#fff' }">
              {{ selectedReservation?.channelName || t('stage6.components.reservationDetail.walkIn') }}
            </el-tag>
          </div>
        </div>

        <div class="order-summary">
          <div class="amounts">
            <div class="amount-item">
              <span>{{ t('stage6.components.reservationDetail.summary.orderAmount') }}</span>
              <span class="amount">¥{{ Number(selectedReservation?.totalAmount || 0).toFixed(2) }}</span>
            </div>
            <div
              v-if="
                selectedReservation?.currentRoomPrice &&
                selectedReservation.currentRoomPrice !== selectedReservation.totalAmount
              "
              class="amount-item"
            >
              <span>{{ t('stage6.components.reservationDetail.summary.currentRoomPrice') }}</span>
              <span class="amount current-price">¥{{ Number(selectedReservation?.currentRoomPrice || 0).toFixed(2) }}</span>
            </div>
            <div class="amount-item">
              <span>{{ t('stage6.components.reservationDetail.summary.paidAmount') }}</span>
              <span class="amount">¥{{ displayPaidAmount.toFixed(2) }}</span>
            </div>
            <div class="amount-item">
              <span>{{ t('stage6.components.reservationDetail.summary.remainingPayment') }}</span>
              <span class="amount" :class="{ red: remainingPayment > 0, green: remainingPayment < 0 }">
                {{ remainingPayment >= 0 ? `¥${remainingPayment.toFixed(2)}` : `+¥${Math.abs(remainingPayment).toFixed(2)}` }}
              </span>
            </div>
          </div>
        </div>

        <div class="room-info-detail">
          <h4>
            {{ t('stage6.components.reservationDetail.room.infoTitle', { amount: Number(selectedReservation?.totalAmount || 0).toFixed(2) }) }}
          </h4>
          <div class="room-card">
            <div class="room-header">
              <span class="room-name">{{ roomDisplayName }}</span>
              <span class="room-dates">
                {{ t('stage6.components.reservationDetail.room.dateRange', {
                  checkIn: selectedReservation?.checkInDate || '-',
                  checkOut: selectedReservation?.checkOutDate || '-',
                  nights: channelNightsText,
                }) }}
              </span>
              <div class="room-actions">
                <div class="actions-left">
                  <el-button
                    v-if="isConfirmedStatus(selectedReservation?.status)"
                    size="small"
                    @click="checkInFromDetail"
                  >
                    {{ t('stage6.components.reservationDetail.actions.checkIn') }}
                  </el-button>
                  <el-button
                    v-if="isCheckedInStatus(selectedReservation?.status)"
                    size="small"
                    type="danger"
                    @click="checkOutFromDetail"
                  >
                    {{ t('stage6.components.reservationDetail.actions.checkOut') }}
                  </el-button>
                  <el-button size="small" type="danger" plain @click="cancelFromDetail">
                    {{ t('stage6.components.reservationDetail.actions.cancelOrder') }}
                  </el-button>
                  <el-button
                    v-if="props.activeOrderTab === 'order-box'"
                    size="small"
                    @click="handleMoveOutOrderBoxFromDetail"
                  >
                    {{ t('stage6.components.reservationDetail.actions.moveOutOrderBox') }}
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
          <h4 class="panel-title">{{ t('stage6.components.reservationDetail.assign.title') }}</h4>
          <div class="assign-row">
            <div class="assign-label">{{ t('stage6.common.labels.roomType') }}</div>
            <el-select
              v-model="assignRoomTypeId"
              :placeholder="t('stage6.components.reservationDetail.assign.roomTypePlaceholder')"
              filterable
              :loading="assignableRoomsLoading"
              style="width: 100%"
              @change="handleRoomTypeChange"
            >
              <el-option
                v-for="roomType in assignableRoomTypes"
                :key="roomType.id"
                :label="t('stage6.components.reservationDetail.assign.roomTypeAvailable', { name: roomType.name, count: roomType.availableRooms })"
                :value="roomType.id"
              />
            </el-select>
          </div>
          <div class="assign-row">
            <div class="assign-label">{{ t('stage6.common.labels.roomNumber') }}</div>
            <el-select
              v-model="assignRoomId"
              :placeholder="t('stage6.components.reservationDetail.assign.roomPlaceholder')"
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
              {{ t('stage6.components.reservationDetail.actions.refreshAvailableRooms') }}
            </el-button>
            <el-button
              type="primary"
              :disabled="!assignRoomId"
              :loading="assignRoomSubmitting"
              @click="submitAssignRoom"
            >
              {{ t('stage6.components.reservationDetail.actions.confirmAssignRoom') }}
            </el-button>
          </div>
        </div>

        <div class="expandable-sections">
          <el-collapse v-model="activeCollapsePanels">
            <el-collapse-item name="1">
              <template #title>
                <span>{{ t('stage6.components.reservationDetail.sections.otherConsumption', { amount: Math.abs(totalConsumption).toFixed(2) }) }}</span>
              </template>
              <el-table
                v-if="consumptionList.length > 0"
                :data="consumptionList"
                border
                style="width: 100%"
                :header-cell-style="{ background: '#f5f7fa', color: '#606266' }"
              >
                <el-table-column prop="item" :label="t('stage6.components.reservationDetail.tables.consumptionItem')" width="180">
                  <template #default="{ row }">
                    {{ row.item }} × {{ row.quantity }}
                  </template>
                </el-table-column>
                <el-table-column prop="amount" :label="t('stage6.components.reservationDetail.tables.consumptionAmount')" width="140" align="right">
                  <template #default="{ row }">
                    ¥{{ Number(row.amount || 0).toFixed(2) }}
                  </template>
                </el-table-column>
                <el-table-column prop="date" :label="t('stage6.components.reservationDetail.tables.consumptionDate')" width="140" />
                <el-table-column prop="createdBy" :label="t('stage6.components.reservationDetail.tables.createdBy')" min-width="120" />
                <el-table-column :label="t('stage6.common.labels.actions')" width="100" align="center">
                  <template #default="{ row }">
                    <el-button link type="danger" @click="handleDeleteConsumption(row.id)">
                      {{ t('stage6.common.actions.delete') }}
                    </el-button>
                  </template>
                </el-table-column>
              </el-table>
              <el-empty v-else :description="t('stage6.components.reservationDetail.empty.noConsumption')" :image-size="60" />
            </el-collapse-item>

            <el-collapse-item name="2">
              <template #title>
                <span>{{ t('stage6.components.reservationDetail.sections.paymentAmount', { amount: totalPayment.toFixed(2) }) }}</span>
              </template>
              <el-table
                v-if="paymentList.length > 0"
                :data="paymentList"
                border
                style="width: 100%"
                :header-cell-style="{ background: '#f5f7fa', color: '#606266' }"
              >
                <el-table-column prop="type" :label="t('stage6.common.labels.project')" width="120" />
                <el-table-column prop="paymentMethod" :label="t('stage6.common.labels.paymentMethod')" width="140" />
                <el-table-column prop="amount" :label="t('stage6.common.labels.amount')" width="120" align="right">
                  <template #default="{ row }">
                    ¥{{ Number(row.amount || 0).toFixed(2) }}
                  </template>
                </el-table-column>
                <el-table-column prop="date" :label="t('stage6.common.labels.date')" width="140" />
                <el-table-column :label="t('stage6.common.labels.actions')" min-width="100" align="center">
                  <template #default="{ row }">
                    <el-button link type="danger" @click="handleDeletePayment(row.id)">
                      {{ t('stage6.common.actions.delete') }}
                    </el-button>
                  </template>
                </el-table-column>
              </el-table>
              <el-empty v-else :description="t('stage6.components.reservationDetail.empty.noPayment')" :image-size="60" />
            </el-collapse-item>

            <el-collapse-item :title="t('stage6.components.reservationDetail.sections.orderReminders', { count: 0 })" name="3">
              <el-empty :description="t('stage6.components.reservationDetail.empty.noReminders')" :image-size="60" />
            </el-collapse-item>
          </el-collapse>
        </div>

        <div class="order-info">
          <p><strong>{{ t('stage6.components.reservationDetail.labels.orderNumber') }}</strong>{{ selectedReservation?.orderNumber || '-' }}</p>
          <p><strong>{{ t('stage6.components.reservationDetail.labels.channelOrderNumber') }}</strong>{{ channelOrderNumberText }}</p>
          <p><strong>{{ t('stage6.components.reservationDetail.labels.guestPhone') }}</strong>{{ selectedReservation?.phone || '-' }}</p>
          <p><strong>{{ t('stage6.common.labels.note') }}:</strong>{{ selectedReservation?.notes || '-' }}</p>
        </div>

        <div class="more-actions">
          <el-dropdown @command="handleMoreActions">
            <el-button link>
              {{ t('stage6.components.reservationDetail.actions.more') }}
              <el-icon><ArrowDown /></el-icon>
            </el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item v-if="props.activeOrderTab === 'order-box'" command="moveOutOrderBox">
                  {{ t('stage6.components.reservationDetail.actions.moveOutOrderBox') }}
                </el-dropdown-item>
                <el-dropdown-item command="cancelReservation">
                  {{ t('stage6.components.reservationDetail.actions.cancelOrder') }}
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </div>

      <div v-if="activeDetailTab === 'log'" class="operation-logs" v-loading="operationLogsLoading">
        <div class="log-filter-buttons">
          <el-button :type="logFilterType === 'all' ? 'primary' : 'default'" size="small" @click="logFilterType = 'all'">
            {{ t('stage6.components.reservationDetail.logs.all') }}
          </el-button>
          <el-button :type="logFilterType === 'order' ? 'primary' : 'default'" size="small" @click="logFilterType = 'order'">
            {{ t('stage6.components.reservationDetail.logs.order') }}
          </el-button>
          <el-button :type="logFilterType === 'billing' ? 'primary' : 'default'" size="small" @click="logFilterType = 'billing'">
            {{ t('stage6.components.reservationDetail.logs.billing') }}
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
                <span class="log-operator">{{ t('stage6.components.reservationDetail.logs.operator', { operator: log.operator }) }}</span>
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

        <el-empty v-if="filteredOperationLogs.length === 0" :description="t('stage6.components.reservationDetail.empty.noOperationLogs')" />
      </div>

      <div v-if="activeDetailTab === 'channel'" class="channel-info-content" v-loading="channelInfoLoading">
        <div class="channel-header">
          <div class="channel-logo-wrapper">
            <div class="channel-logo">
              <span class="logo-text">{{ channelLogoText }}</span>
            </div>
            <h3>{{ channelInfo?.channelName || selectedReservation?.channelName || t('stage6.components.reservationDetail.walkIn') }}</h3>
          </div>
        </div>

        <div class="channel-section">
          <h4>{{ t('stage6.components.reservationDetail.channel.orderDetails') }}</h4>
          <div class="info-grid">
            <div class="info-item">
              <span class="label">{{ t('stage6.components.reservationDetail.labels.channelOrderNumber') }}</span>
              <span class="value">{{ channelInfo?.channelOrderNumber || channelOrderNumberText }}</span>
            </div>
            <div class="info-item">
              <span class="label">{{ t('stage6.components.reservationDetail.labels.reservationStatus') }}</span>
              <el-tag :type="getStatusTagType(selectedReservation?.status || 'CONFIRMED')">
                {{ getReservationStatusText(selectedReservation?.status || 'CONFIRMED') }}
              </el-tag>
            </div>
            <div class="info-item">
              <span class="label">{{ t('stage6.components.reservationDetail.labels.bookingDate') }}</span>
              <span class="value">{{ channelInfo?.bookingDate || selectedReservation?.createdAt || '-' }}</span>
            </div>
            <div class="info-item">
              <span class="label">{{ t('stage6.common.labels.paymentMethod') }}:</span>
              <el-tag v-if="channelInfo?.paymentMethod" type="warning">{{ channelInfo.paymentMethod }}</el-tag>
              <span v-else class="value">-</span>
            </div>
          </div>
        </div>

        <div class="channel-section">
          <h4>{{ t('stage6.components.reservationDetail.channel.priceInfo') }}</h4>
          <div class="price-table">
            <el-table :data="channelPriceRows" border>
              <el-table-column prop="label" label="" width="150" />
              <el-table-column prop="value" label="" align="right" />
            </el-table>
          </div>
        </div>

        <div class="channel-section">
          <h4>{{ t('stage6.components.reservationDetail.channel.roomDetails') }}</h4>
          <div class="info-grid">
            <div class="info-item">
              <span class="label">{{ t('stage6.common.labels.roomType') }}:</span>
              <span class="value">{{ selectedReservation?.roomTypeName || channelInfo?.roomType || '-' }}</span>
            </div>
            <div class="info-item">
              <span class="label">{{ t('stage6.components.reservationDetail.labels.guestName') }}</span>
              <span class="value">
                {{ selectedReservation?.guestName || channelInfo?.guestName || '-' }}
                {{ t('stage6.components.reservationDetail.labels.guestOccupancy', { adults: channelInfo?.adults ?? 1, children: channelInfo?.children ?? 0 }) }}
              </span>
            </div>
            <div class="info-item">
              <span class="label">{{ t('stage6.components.reservationDetail.labels.stayDates') }}</span>
              <span class="value">{{ selectedReservation?.checkInDate || '-' }} ~ {{ selectedReservation?.checkOutDate || '-' }}</span>
            </div>
            <div class="info-item">
              <span class="label">{{ t('stage6.components.reservationDetail.labels.nights') }}</span>
              <span class="value">{{ channelNightsText }}</span>
            </div>
            <div class="info-item">
              <span class="label">{{ t('stage6.components.reservationDetail.labels.pricePlan') }}</span>
              <span class="value">{{ channelInfo?.pricePlan || '-' }}</span>
            </div>
          </div>
        </div>

        <div class="channel-section">
          <h4>{{ t('stage6.components.reservationDetail.channel.specialRequests') }}</h4>
          <div class="special-requests">
            <p v-if="channelInfo?.specialRequests || selectedReservation?.notes">
              {{ channelInfo?.specialRequests || selectedReservation?.notes }}
            </p>
            <el-empty v-else :description="t('stage6.components.reservationDetail.empty.noSpecialRequests')" :image-size="60" />
          </div>
        </div>
      </div>

      <div class="detail-footer">
        <el-button @click="handlePrint">{{ t('stage6.components.reservationDetail.actions.print') }}</el-button>
        <el-button type="danger" plain @click="cancelFromDetail">{{ t('stage6.components.reservationDetail.actions.cancelOrder') }}</el-button>
        <el-button
          v-if="isConfirmedStatus(selectedReservation?.status)"
          type="success"
          @click="checkInFromDetail"
        >
          {{ t('stage6.components.reservationDetail.actions.checkIn') }}
        </el-button>
        <el-button
          v-if="isCheckedInStatus(selectedReservation?.status)"
          type="danger"
          @click="checkOutFromDetail"
        >
          {{ t('stage6.components.reservationDetail.actions.checkOut') }}
        </el-button>
      </div>
    </div>
  </el-drawer>
</template>

<script setup lang="ts">
import { ArrowDown } from '@element-plus/icons-vue'
import { ElLoading, ElMessage } from 'element-plus'
import { computed, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
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
const router = useRouter()
const { t } = useI18n()

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

const isReservationSettled = computed(() => {
  const reservation = selectedReservation.value
  if (!reservation) return false
  const status = (reservation.status || '').toUpperCase()
  const totalAmount = Number(reservation.totalAmount || 0)
  const paidAmount = Number(reservation.paidAmount || 0)
  const hasSuSource = Boolean(reservation.suReservationId?.trim())
  const checkedInOrOut = status === 'CHECKED_IN' || status === 'CHECKED_OUT'
  const amountSettled = totalAmount > 0 && paidAmount >= totalAmount
  return Boolean(reservation.settled) || hasSuSource || checkedInOrOut || amountSettled
})

const displayPaidAmount = computed(() => {
  const reservationTotal = Number(selectedReservation.value?.totalAmount || 0)
  const paymentTotal = Number(totalPayment.value || 0)
  if (isReservationSettled.value && reservationTotal > paymentTotal) {
    return reservationTotal
  }
  return paymentTotal
})

const remainingPayment = computed(() => {
  const total = Number(selectedReservation.value?.totalAmount || 0)
  return total - Number(displayPaidAmount.value || 0) - Number(totalConsumption.value || 0)
})

const shouldShowAssignRoom = computed(() => {
  return false
})

const roomDisplayName = computed(() => {
  const roomTypeName = selectedReservation.value?.roomTypeName || t('stage6.components.reservationDetail.room.unassigned')
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
    return t('stage6.components.reservationDetail.room.nightsCount', { count: nights })
  }
  const checkIn = selectedReservation.value?.checkInDate
  const checkOut = selectedReservation.value?.checkOutDate
  if (!checkIn || !checkOut) return '-'
  const start = new Date(checkIn)
  const end = new Date(checkOut)
  if (Number.isNaN(start.getTime()) || Number.isNaN(end.getTime())) return '-'
  const diffDays = Math.round((end.getTime() - start.getTime()) / (1000 * 60 * 60 * 24))
  return diffDays > 0 ? t('stage6.components.reservationDetail.room.nightsCount', { count: diffDays }) : '-'
})

const channelPriceRows = computed(() => {
  const totalAmount = channelInfo.value?.totalAmount ?? selectedReservation.value?.totalAmount
  return [
    { label: t('stage6.components.reservationDetail.priceRows.totalPrice'), value: formatMoney(totalAmount) },
    { label: t('stage6.components.reservationDetail.priceRows.commission'), value: formatMoney(channelInfo.value?.commission) },
    { label: t('stage6.components.reservationDetail.priceRows.otherFees'), value: formatMoney(channelInfo.value?.otherFees) },
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
  if (normalizedStatus === 'CONFIRMED' || normalizedStatus === 'NEW') return t('stage6.components.reservationDetail.status.confirmed')
  if (normalizedStatus === 'REQUESTED') return t('stage6.components.reservationDetail.status.requested')
  if (normalizedStatus === 'CHECKED_IN') return t('stage6.components.reservationDetail.status.checkedIn')
  if (normalizedStatus === 'CHECKED_OUT') return t('stage6.components.reservationDetail.status.checkedOut')
  if (normalizedStatus === 'CANCELLED') return t('stage6.components.reservationDetail.status.cancelled')
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

const goToMessages = async () => {
  const reservation = selectedReservation.value
  if (!reservation) {
    ElMessage.warning(t('stage6.components.reservationDetail.messages.reservationLoading'))
    return
  }

  const query: Record<string, string> = {}
  if (reservation.id) {
    query.reservationId = String(reservation.id)
  }

  const orderNumber = reservation.orderNumber?.trim()
  if (orderNumber) {
    query.orderNumber = orderNumber
  }

  const channelOrderNumber = reservation.channelOrderNumber?.trim()
  if (channelOrderNumber) {
    query.channelOrderNumber = channelOrderNumber
  }

  const guestName = reservation.guestName?.trim()
  if (guestName) {
    query.guestName = guestName
  }

  await router.push({
    name: 'Messages',
    query,
  })
}

const loadReservationDetail = async (reservationId: number) => {
  try {
    const res = await getReservationById(reservationId)
    if (res.success) {
      selectedReservation.value = res.data
      return
    }
    ElMessage.error(res.message || t('stage6.components.reservationDetail.messages.loadReservationFailed'))
    selectedReservation.value = null
  } catch (error) {
    console.error(t('stage6.components.reservationDetail.messages.loadReservationFailed'), error)
    ElMessage.error(t('stage6.components.reservationDetail.messages.loadReservationFailed'))
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
    console.error(t('stage6.components.reservationDetail.messages.loadOperationLogsFailed'), error)
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
    console.error(t('stage6.components.reservationDetail.messages.loadChannelInfoFailed'), error)
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
    console.error(t('stage6.components.reservationDetail.messages.loadBillingDataFailed'), error)
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
      ElMessage.error(res.message || t('stage6.components.reservationDetail.messages.loadAssignableRoomTypesFailed'))
      return
    }
    assignableRoomTypes.value = res.data.roomTypes || []
    assignableRooms.value = []
    assignRoomTypeId.value = null
    assignRoomId.value = null
  } catch (error) {
    console.error(t('stage6.components.reservationDetail.messages.loadAssignableRoomTypesFailed'), error)
    ElMessage.error(t('stage6.components.reservationDetail.messages.loadAssignableRoomTypesFailed'))
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
      ElMessage.error(res.message || t('stage6.components.reservationDetail.messages.loadAvailableRoomsFailed'))
      return
    }
    assignableRooms.value = res.data.rooms || []
  } catch (error) {
    console.error(t('stage6.components.reservationDetail.messages.loadAvailableRoomsFailed'), error)
    ElMessage.error(t('stage6.components.reservationDetail.messages.loadAvailableRoomsFailed'))
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
      ElMessage.error(res.message || t('stage6.components.reservationDetail.messages.assignRoomFailed'))
      return
    }
    ElMessage.success(t('stage6.components.reservationDetail.messages.assignRoomSuccess'))
    selectedReservation.value = res.data
    await Promise.all([
      loadOperationLogs(reservationId),
      loadChannelInfo(reservationId),
      loadConsumptionAndPaymentData(reservationId),
      refreshAssignableRoomTypes(reservationId),
    ])
    emit('updated')
  } catch (error) {
    console.error(t('stage6.components.reservationDetail.messages.assignRoomFailed'), error)
    ElMessage.error(t('stage6.components.reservationDetail.messages.assignRoomFailed'))
  } finally {
    assignRoomSubmitting.value = false
  }
}

const checkInFromDetail = async () => {
  const reservation = selectedReservation.value
  if (!reservation) return
  const loadingInstance = ElLoading.service({ text: t('stage6.components.reservationDetail.messages.checkInLoading') })
  try {
    const response = await checkInReservation(reservation.id)
    if (!response.success) {
      ElMessage.error(response.message || t('stage6.components.reservationDetail.messages.checkInFailed'))
      return
    }
    ElMessage.success(t('stage6.components.reservationDetail.messages.checkInSuccess', { orderNumber: reservation.orderNumber }))
    await Promise.all([
      loadReservationDetail(reservation.id),
      loadOperationLogs(reservation.id),
      loadChannelInfo(reservation.id),
      loadConsumptionAndPaymentData(reservation.id),
    ])
    emit('updated')
  } catch (error) {
    console.error(t('stage6.components.reservationDetail.messages.checkInFailed'), error)
    ElMessage.error(t('stage6.components.reservationDetail.messages.checkInFailed'))
  } finally {
    loadingInstance.close()
  }
}

const checkOutFromDetail = async () => {
  const reservation = selectedReservation.value
  if (!reservation) return
  const loadingInstance = ElLoading.service({ text: t('stage6.components.reservationDetail.messages.checkOutLoading') })
  try {
    const response = await checkOutReservation(reservation.id)
    if (!response.success) {
      ElMessage.error(response.message || t('stage6.components.reservationDetail.messages.checkOutFailed'))
      return
    }
    ElMessage.success(t('stage6.components.reservationDetail.messages.checkOutSuccess', { orderNumber: reservation.orderNumber }))
    await Promise.all([
      loadReservationDetail(reservation.id),
      loadOperationLogs(reservation.id),
      loadChannelInfo(reservation.id),
      loadConsumptionAndPaymentData(reservation.id),
    ])
    emit('updated')
  } catch (error) {
    console.error(t('stage6.components.reservationDetail.messages.checkOutFailed'), error)
    ElMessage.error(t('stage6.components.reservationDetail.messages.checkOutFailed'))
  } finally {
    loadingInstance.close()
  }
}

const cancelFromDetail = async () => {
  const reservation = selectedReservation.value
  if (!reservation) return
  const loadingInstance = ElLoading.service({ text: t('stage6.components.reservationDetail.messages.cancelOrderLoading') })
  try {
    const response = await cancelReservation(reservation.id)
    if (!response.success) {
      ElMessage.error(response.message || t('stage6.components.reservationDetail.messages.cancelOrderFailed'))
      return
    }
    ElMessage.success(t('stage6.components.reservationDetail.messages.cancelOrderSuccess', { orderNumber: reservation.orderNumber }))
    await Promise.all([
      loadReservationDetail(reservation.id),
      loadOperationLogs(reservation.id),
      loadChannelInfo(reservation.id),
      loadConsumptionAndPaymentData(reservation.id),
    ])
    emit('updated')
  } catch (error) {
    console.error(t('stage6.components.reservationDetail.messages.cancelOrderFailed'), error)
    ElMessage.error(t('stage6.components.reservationDetail.messages.cancelOrderFailed'))
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
      ElMessage.error(orderBoxResp.message || t('stage6.components.reservationDetail.messages.loadOrderBoxFailed'))
      return
    }
    const boxItem = orderBoxResp.data.find((item) => item.reservation.id === reservation.id)
    if (!boxItem) {
      ElMessage.warning(t('stage6.components.reservationDetail.messages.orderBoxItemNotFound'))
      return
    }
    const res = await moveOutOrderBox({ orderBoxItemId: boxItem.id })
    if (!res.success) {
      ElMessage.error(res.message || t('stage6.components.reservationDetail.messages.moveOutOrderBoxFailed'))
      return
    }
    ElMessage.success(t('stage6.components.reservationDetail.messages.moveOutOrderBoxSuccess'))
    await Promise.all([
      loadReservationDetail(reservation.id),
      loadOperationLogs(reservation.id),
      loadChannelInfo(reservation.id),
      loadConsumptionAndPaymentData(reservation.id),
    ])
    emit('updated')
  } catch (error) {
    console.error(t('stage6.components.reservationDetail.messages.moveOutOrderBoxFailed'), error)
    ElMessage.error(t('stage6.components.reservationDetail.messages.moveOutOrderBoxFailed'))
  }
}

const handleDeleteConsumption = async (consumptionId?: number) => {
  if (!consumptionId || !selectedReservation.value) return
  try {
    const res = await deleteConsumption(consumptionId)
    if (!res.success) {
      ElMessage.error(res.message || t('stage6.components.reservationDetail.messages.deleteConsumptionFailed'))
      return
    }
    ElMessage.success(t('stage6.components.reservationDetail.messages.deleteConsumptionSuccess'))
    await loadConsumptionAndPaymentData(selectedReservation.value.id)
    await loadOperationLogs(selectedReservation.value.id)
    emit('updated')
  } catch (error) {
    console.error(t('stage6.components.reservationDetail.messages.deleteConsumptionFailed'), error)
    ElMessage.error(t('stage6.components.reservationDetail.messages.deleteConsumptionFailed'))
  }
}

const handleDeletePayment = async (paymentId?: number) => {
  if (!paymentId || !selectedReservation.value) return
  try {
    const res = await deletePayment(paymentId)
    if (!res.success) {
      ElMessage.error(res.message || t('stage6.components.reservationDetail.messages.deletePaymentFailed'))
      return
    }
    ElMessage.success(t('stage6.components.reservationDetail.messages.deletePaymentSuccess'))
    await loadConsumptionAndPaymentData(selectedReservation.value.id)
    await loadOperationLogs(selectedReservation.value.id)
    emit('updated')
  } catch (error) {
    console.error(t('stage6.components.reservationDetail.messages.deletePaymentFailed'), error)
    ElMessage.error(t('stage6.components.reservationDetail.messages.deletePaymentFailed'))
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

.guest-main {
  display: flex;
  align-items: center;
  gap: 10px;
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
