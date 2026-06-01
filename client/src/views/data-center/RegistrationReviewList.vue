<template>
  <StatisticsLayout>
    <div class="wrap">
      <div class="header">
        <div class="title">{{ t('stage5.dataCenter.registrations.title') }}</div>
        <div class="actions">
          <el-select v-model="status" :placeholder="t('stage5.common.filters.status')" clearable style="width: 160px" @change="load">
            <el-option :label="t('stage5.common.status.draft')" value="DRAFT" />
            <el-option :label="t('stage5.common.status.submitted')" value="SUBMITTED" />
            <el-option :label="t('stage5.common.status.approved')" value="APPROVED" />
            <el-option :label="t('stage5.common.status.rejected')" value="REJECTED" />
          </el-select>
          <el-button @click="openLinkDrawer">{{ t('stage5.dataCenter.registrations.linkList') }}</el-button>
          <el-button :loading="loading" @click="load">{{ t('stage5.common.actions.refresh') }}</el-button>
        </div>
      </div>

      <div class="filters">
        <el-select
          v-model="channelId"
          filterable
          clearable
          :placeholder="t('stage5.common.filters.platform')"
          style="width: 200px"
        >
          <el-option v-for="c in channels" :key="c.id" :label="c.name" :value="c.id" />
        </el-select>
        <el-select
          v-model="reservationStatus"
          clearable
          :placeholder="t('stage5.common.filters.orderStatus')"
          style="width: 180px"
        >
          <el-option
            v-for="option in reservationStatusOptions"
            :key="option.value"
            :label="option.label"
            :value="option.value"
          />
        </el-select>
        <el-select
          v-model="roomNumber"
          filterable
          clearable
          allow-create
          default-first-option
          :reserve-keyword="false"
          :placeholder="t('stage5.common.fields.roomNumber')"
          style="width: 160px"
        >
          <el-option v-for="room in rooms" :key="room.id" :label="room.roomNumber" :value="room.roomNumber" />
        </el-select>
        <el-select
          v-model="roomGroupId"
          filterable
          clearable
          :placeholder="t('roomStatus.filterDrawer.groupFilter')"
          style="width: 180px"
        >
          <el-option v-for="group in roomGroups" :key="group.id" :label="group.name" :value="group.id" />
        </el-select>
        <el-date-picker
          v-model="checkInDate"
          type="date"
          value-format="YYYY-MM-DD"
          clearable
          :placeholder="t('stage5.common.fields.checkInDate')"
          style="width: 160px"
        />
        <el-date-picker
          v-model="checkOutDate"
          type="date"
          value-format="YYYY-MM-DD"
          clearable
          :placeholder="t('stage5.common.fields.checkOutDate')"
          style="width: 160px"
        />
        <el-button type="primary" :loading="loading" @click="load">{{ t('stage5.common.actions.query') }}</el-button>
        <el-button :disabled="loading" @click="resetFilters">{{ t('stage5.common.actions.reset') }}</el-button>
      </div>

      <el-table
        :data="rows"
        border
        stripe
        class="review-table"
        style="width: 100%"
        :row-class-name="getRowClassName"
        @row-click="go"
      >
        <el-table-column prop="channelOrderNumber" :label="t('stage5.dataCenter.registrations.channelOrderNumber')" min-width="160" />
        <el-table-column prop="channelName" :label="t('stage5.common.filters.channel')" min-width="140" />
        <el-table-column prop="guestName" :label="t('stage5.common.fields.guestName')" min-width="120" />
        <el-table-column :label="t('stage5.common.fields.roomNumber')" min-width="110">
          <template #default="{ row }">
            {{ row.roomNumber || '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="checkInDate" :label="t('stage5.common.fields.checkIn')" min-width="110" />
        <el-table-column prop="checkOutDate" :label="t('stage5.common.fields.checkOut')" min-width="110" />
        <el-table-column :label="t('stage5.common.filters.orderStatus')" min-width="120">
          <template #default="{ row }">
            <el-tag :type="getReservationStatusTagType(row.reservationStatus)" effect="plain">
              {{ getReservationStatusLabel(row.reservationStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="t('stage5.common.fields.status')" min-width="110">
          <template #default="{ row }">
            {{ getRegistrationStatusLabel(row.status) }}
          </template>
        </el-table-column>
        <el-table-column prop="submittedAt" :label="t('stage5.common.fields.submittedAt')" min-width="170" />
        <el-table-column prop="updatedAt" :label="t('stage5.common.fields.updatedAt')" min-width="170" />
        <el-table-column :label="t('stage5.common.fields.actions')" width="110" fixed="right">
          <template #default="{ row }">
            <el-tooltip
              v-if="isCancelledReservation(row)"
              :content="t('stage5.dataCenter.registrations.cancelled')"
              placement="top"
            >
              <span class="disabled-action">
                <el-button size="small" disabled>{{ t('nav.review') }}</el-button>
              </span>
            </el-tooltip>
            <el-button v-else size="small" type="primary" link @click.stop="go(row)">
              {{ t('nav.review') }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-drawer v-model="linkDrawerVisible" :title="t('stage5.dataCenter.registrations.linkList')" size="80%">
      <div class="drawer-actions">
        <el-select
          v-model="linkReservationStatus"
          clearable
          :placeholder="t('stage5.common.filters.reservationStatus')"
          style="width: 160px"
          @change="loadLinks"
        >
          <el-option
            v-for="option in linkReservationStatusOptions"
            :key="option.value"
            :label="option.label"
            :value="option.value"
          />
        </el-select>
        <el-button :loading="linkLoading" @click="loadLinks">{{ t('stage5.common.actions.refresh') }}</el-button>
      </div>

      <el-table :data="linkRows" border stripe style="width: 100%" v-loading="linkLoading">
        <el-table-column prop="createdAt" :label="t('stage5.common.fields.time')" min-width="170" />
        <el-table-column prop="guestName" :label="t('stage5.common.fields.guestName')" min-width="110" />
        <el-table-column prop="checkInDate" :label="t('stage5.common.fields.checkIn')" min-width="110" />
        <el-table-column prop="checkOutDate" :label="t('stage5.common.fields.checkOut')" min-width="110" />
        <el-table-column :label="t('stage5.common.filters.reservationStatus')" min-width="120">
          <template #default="{ row }">
            {{ getReservationStatusLabel(row.reservationStatus) }}
          </template>
        </el-table-column>
        <el-table-column prop="roomCount" :label="t('stage5.common.fields.roomCount')" width="90" />
        <el-table-column :label="t('stage5.dataCenter.registrations.link')" min-width="240">
          <template #default="{ row }">
            <div class="link-cell">
              <el-input :model-value="row.linkUrl" readonly size="small" />
              <el-button size="small" @click="copy(row.linkUrl)">{{ t('stage5.common.actions.copy') }}</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </el-drawer>
  </StatisticsLayout>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute, useRouter, type LocationQueryRaw } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'
import { getAllChannels, type ChannelDTO } from '@/api/channel'
import { getRegistrationLinkInbox, type RegistrationLinkInboxItemDTO } from '@/api/registrationLinkInbox'
import { getRooms, type RoomDTO } from '@/api/room'
import { getAllRoomGroups, type RoomGroupDTO } from '@/api/roomGroup'
import { getStoreTodayYmd } from '@/utils/storeDateTime'

type Row = {
  formId: number
  orderNumber: string
  channelOrderNumber?: string | null
  channelName?: string | null
  guestName: string
  roomNumber?: string | null
  roomTypeName?: string | null
  checkInDate: string
  checkOutDate: string
  reservationStatus?: string | null
  status: string
  submittedAt?: string
  updatedAt?: string
}
type ReservationStatusTagType = 'success' | 'warning' | 'info' | 'danger'

const registrationStatusFilterValues = ['DRAFT', 'SUBMITTED', 'APPROVED', 'REJECTED']
const router = useRouter()
const route = useRoute()
const { t } = useI18n()
const rows = ref<Row[]>([])
const loading = ref(false)
const status = ref<string | null>(null)
const channels = ref<ChannelDTO[]>([])
const rooms = ref<RoomDTO[]>([])
const roomGroups = ref<Array<RoomGroupDTO & { id: number }>>([])
const channelId = ref<number | null>(null)
const reservationStatus = ref<string | null>(null)
const roomNumber = ref<string | null>(null)
const roomGroupId = ref<number | null>(null)
const checkInDate = ref<string | null>(null)
const checkOutDate = ref<string | null>(null)
const linkDrawerVisible = ref(false)
const linkLoading = ref(false)
const linkRows = ref<RegistrationLinkInboxItemDTO[]>([])
const linkReservationStatus = ref<string | null>(null)

const reservationStatusOptions = [
  { label: t('stage5.dataCenter.registrations.pendingConfirmation'), value: 'REQUESTED' },
  { label: t('stage5.dataCenter.registrations.booked'), value: 'CONFIRMED' },
  { label: t('stage5.dataCenter.registrations.checkedIn'), value: 'CHECKED_IN' },
  { label: t('stage5.dataCenter.registrations.checkedOut'), value: 'CHECKED_OUT' },
  { label: t('stage5.dataCenter.registrations.cancelled'), value: 'CANCELLED' },
  { label: t('stage5.dataCenter.registrations.noShow'), value: 'NO_SHOW' },
]

const linkReservationStatusOptions = [
  { label: t('stage5.dataCenter.registrations.booked'), value: 'CONFIRMED' },
  { label: t('stage5.dataCenter.registrations.cancelled'), value: 'CANCELLED' },
]
const reservationStatusFilterValues = reservationStatusOptions.map((option) => option.value)

function toDayNumber(dateValue?: string | null) {
  if (!dateValue) {
    return null
  }

  const match = dateValue.trim().match(/^(\d{4})-(\d{2})-(\d{2})$/)
  if (!match) {
    return null
  }

  const [, year, month, day] = match
  return Math.floor(Date.UTC(Number(year), Number(month) - 1, Number(day)) / 86400000)
}

function getTodayDayNumber() {
  return toDayNumber(getStoreTodayYmd()) ?? 0
}

function compareRowsByCheckInPriority(left: Row, right: Row) {
  const today = getTodayDayNumber()
  const leftCheckIn = toDayNumber(left.checkInDate)
  const rightCheckIn = toDayNumber(right.checkInDate)

  const getPriority = (dayNumber: number | null) => {
    if (dayNumber == null) {
      return { bucket: 2, distance: Number.MAX_SAFE_INTEGER }
    }
    if (dayNumber >= today) {
      return { bucket: 0, distance: dayNumber - today }
    }
    return { bucket: 1, distance: today - dayNumber }
  }

  const leftPriority = getPriority(leftCheckIn)
  const rightPriority = getPriority(rightCheckIn)

  if (leftPriority.bucket !== rightPriority.bucket) {
    return leftPriority.bucket - rightPriority.bucket
  }
  if (leftPriority.distance !== rightPriority.distance) {
    return leftPriority.distance - rightPriority.distance
  }

  const leftUpdatedAt = left.updatedAt || left.submittedAt || ''
  const rightUpdatedAt = right.updatedAt || right.submittedAt || ''
  return rightUpdatedAt.localeCompare(leftUpdatedAt)
}

async function loadChannels() {
  try {
    const resp = await getAllChannels()
    channels.value = resp.success ? (resp.data || []) : []
    if (!resp.success) {
      ElMessage.error(resp.message || t('stage5.common.messages.loadChannelsFailed'))
    }
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || e?.message || t('stage5.common.messages.loadChannelsFailed'))
  }
}

async function loadRooms() {
  try {
    const resp = await getRooms()
    rooms.value = resp.success ? (resp.data || []) : []
    if (!resp.success) {
      ElMessage.error(resp.message || t('stage5.common.messages.dataLoadFailed'))
    }
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || e?.message || t('stage5.common.messages.dataLoadFailed'))
  }
}

async function loadRoomGroups() {
  try {
    const resp = await getAllRoomGroups()
    roomGroups.value = resp.success
      ? (resp.data || []).filter((group): group is RoomGroupDTO & { id: number } => typeof group.id === 'number')
      : []
    if (!resp.success) {
      ElMessage.error(resp.message || t('stage5.common.messages.dataLoadFailed'))
    }
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || e?.message || t('stage5.common.messages.dataLoadFailed'))
  }
}

function getQueryValue(value: unknown) {
  if (Array.isArray(value)) {
    return typeof value[0] === 'string' ? value[0] : null
  }
  return typeof value === 'string' ? value : null
}

function getQueryString(value: unknown) {
  const queryValue = getQueryValue(value)?.trim()
  return queryValue ? queryValue : null
}

function getAllowedQueryString(value: unknown, allowedValues: string[]) {
  const queryValue = getQueryString(value)
  return queryValue && allowedValues.includes(queryValue) ? queryValue : null
}

function getQueryNumber(value: unknown) {
  const queryValue = getQueryString(value)
  if (!queryValue) {
    return null
  }
  const parsed = Number(queryValue)
  return Number.isFinite(parsed) ? parsed : null
}

function hydrateFiltersFromRouteQuery() {
  status.value = getAllowedQueryString(route.query.status, registrationStatusFilterValues)
  channelId.value = getQueryNumber(route.query.channelId)
  reservationStatus.value = getAllowedQueryString(route.query.reservationStatus, reservationStatusFilterValues)
  roomNumber.value = getQueryString(route.query.roomNumber)
  roomGroupId.value = getQueryNumber(route.query.roomGroupId)
  checkInDate.value = getQueryString(route.query.checkInDate)
  checkOutDate.value = getQueryString(route.query.checkOutDate)
}

function buildFilterQuery(): LocationQueryRaw {
  const query: LocationQueryRaw = {}
  if (status.value) query.status = status.value
  if (channelId.value != null) query.channelId = String(channelId.value)
  if (reservationStatus.value) query.reservationStatus = reservationStatus.value
  if (roomNumber.value?.trim()) query.roomNumber = roomNumber.value.trim()
  if (roomGroupId.value != null) query.roomGroupId = String(roomGroupId.value)
  if (checkInDate.value) query.checkInDate = checkInDate.value
  if (checkOutDate.value) query.checkOutDate = checkOutDate.value
  return query
}

function isSameQuery(left: LocationQueryRaw, right: LocationQueryRaw) {
  const leftKeys = Object.keys(left).sort()
  const rightKeys = Object.keys(right).sort()
  if (leftKeys.length !== rightKeys.length) {
    return false
  }
  return leftKeys.every((key, index) => key === rightKeys[index] && getQueryValue(left[key]) === getQueryValue(right[key]))
}

async function syncFiltersToRouteQuery() {
  const query = buildFilterQuery()
  if (isSameQuery(route.query, query)) {
    return
  }
  await router.replace({ name: 'DataCenterRegistrations', query })
}

function resetFilters() {
  channelId.value = null
  reservationStatus.value = null
  roomNumber.value = null
  roomGroupId.value = null
  checkInDate.value = null
  checkOutDate.value = null
  load()
}

async function load() {
  loading.value = true
  try {
    await syncFiltersToRouteQuery()
    const params: Record<string, string | number> = {}
    if (status.value) params.status = status.value
    if (channelId.value) params.channelId = channelId.value
    if (reservationStatus.value) params.reservationStatus = reservationStatus.value
    if (roomNumber.value?.trim()) params.roomNumber = roomNumber.value.trim()
    if (roomGroupId.value != null) params.roomGroupId = roomGroupId.value
    if (checkInDate.value) params.checkInDate = checkInDate.value
    if (checkOutDate.value) params.checkOutDate = checkOutDate.value

    const resp = (await request.get('/registrations', { params })) as {
      success: boolean
      message?: string
      data?: Row[]
    }
    if (resp.success) {
      rows.value = ((resp.data || []) as Row[])
        .map((r) => ({
          ...r,
          channelOrderNumber: r.channelOrderNumber || '-',
          channelName: r.channelName || '-',
          roomNumber: r.roomNumber || '',
        }))
        .sort(compareRowsByCheckInPriority)
    } else {
      rows.value = []
      ElMessage.error(resp.message || t('stage5.common.messages.dataLoadFailed'))
    }
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || e?.message || t('stage5.common.messages.dataLoadFailed'))
  } finally {
    loading.value = false
  }
}

function go(row: Row) {
  if (isCancelledReservation(row)) {
    ElMessage.warning(t('stage5.dataCenter.registrations.cancelled'))
    return
  }
  router.push({ name: 'DataCenterRegistrationDetail', params: { formId: row.formId } })
}

function isCancelledReservation(row: Row) {
  return row.reservationStatus === 'CANCELLED'
}

function getRowClassName({ row }: { row: Row }) {
  return isCancelledReservation(row) ? 'is-cancelled-row' : ''
}

async function loadLinks() {
  linkLoading.value = true
  try {
    const resp = await getRegistrationLinkInbox(linkReservationStatus.value)
    linkRows.value = resp.success ? (resp.data || []) : []
    if (!resp.success) {
      ElMessage.error(resp.message || t('stage5.common.messages.dataLoadFailed'))
    }
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || e?.message || t('stage5.common.messages.dataLoadFailed'))
  } finally {
    linkLoading.value = false
  }
}

function openLinkDrawer() {
  linkDrawerVisible.value = true
  loadLinks()
}

function getReservationStatusLabel(status?: string | null) {
  switch (status) {
    case 'CONFIRMED':
      return t('stage5.dataCenter.registrations.booked')
    case 'CANCELLED':
      return t('stage5.dataCenter.registrations.cancelled')
    case 'CHECKED_IN':
      return t('stage5.dataCenter.registrations.checkedIn')
    case 'CHECKED_OUT':
      return t('stage5.dataCenter.registrations.checkedOut')
    case 'REQUESTED':
      return t('stage5.dataCenter.registrations.pendingConfirmation')
    case 'NO_SHOW':
      return t('stage5.dataCenter.registrations.noShow')
    default:
      return '-'
  }
}

function getReservationStatusTagType(status?: string | null): ReservationStatusTagType {
  switch (status) {
    case 'CONFIRMED':
    case 'CHECKED_IN':
    case 'CHECKED_OUT':
      return 'success'
    case 'CANCELLED':
      return 'danger'
    case 'REQUESTED':
      return 'warning'
    case 'NO_SHOW':
      return 'info'
    default:
      return 'info'
  }
}

function getRegistrationStatusLabel(status?: string | null) {
  switch (status) {
    case 'DRAFT':
      return t('stage5.common.status.draft')
    case 'SUBMITTED':
      return t('stage5.common.status.submitted')
    case 'APPROVED':
      return t('stage5.common.status.approved')
    case 'REJECTED':
      return t('stage5.common.status.rejected')
    default:
      return '-'
  }
}

async function copy(text: string) {
  if (!text) {
    return
  }
  try {
    await navigator.clipboard.writeText(text)
    ElMessage.success(t('stage5.common.messages.copied'))
  } catch {
    const textarea = document.createElement('textarea')
    textarea.value = text
    textarea.style.position = 'fixed'
    textarea.style.left = '-9999px'
    document.body.appendChild(textarea)
    textarea.select()
    document.execCommand('copy')
    document.body.removeChild(textarea)
    ElMessage.success(t('stage5.common.messages.copied'))
  }
}

onMounted(() => {
  hydrateFiltersFromRouteQuery()
  load()
  loadChannels()
  loadRooms()
  loadRoomGroups()
})
</script>

<style scoped>
.wrap {
  padding: 16px;
}
.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}
.filters {
  display: flex;
  gap: 10px;
  align-items: center;
  flex-wrap: wrap;
  margin-bottom: 12px;
}
.title {
  font-size: 16px;
  font-weight: 700;
}
.actions {
  display: flex;
  gap: 10px;
  align-items: center;
}
.drawer-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-bottom: 12px;
}
.link-cell {
  display: flex;
  gap: 8px;
  align-items: center;
}
.review-table :deep(.el-table__row:not(.is-cancelled-row)) {
  cursor: pointer;
}
.review-table :deep(.is-cancelled-row) {
  color: #909399;
}
.review-table :deep(.is-cancelled-row .el-table__cell) {
  background-color: #fafafa;
}
.disabled-action {
  display: inline-flex;
}
</style>
