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
          <el-button
            type="primary"
            :disabled="selectedRows.length === 0"
            :loading="downloadingPdfs"
            @click="downloadSelectedPdfs"
          >
            {{ t('stage5.common.actions.downloadPdf') }}
          </el-button>
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
          v-model="roomGroupId"
          filterable
          clearable
          :placeholder="t('stage5.common.filters.roomGroup')"
          style="width: 200px"
        >
          <el-option
            v-for="group in roomGroups"
            :key="group.id"
            :label="group.name"
            :value="group.id"
          />
        </el-select>
        <el-select
          v-model="roomNumbers"
          multiple
          filterable
          clearable
          collapse-tags
          collapse-tags-tooltip
          :placeholder="t('stage5.common.fields.roomNumber')"
          style="width: 220px"
        >
          <el-option
            v-for="room in selectableRooms"
            :key="room.id"
            :label="room.roomNumber"
            :value="room.roomNumber"
          />
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
        ref="tableRef"
        :data="rows"
        border
        stripe
        class="review-table"
        style="width: 100%"
        :row-class-name="getRowClassName"
        @selection-change="handleSelectionChange"
        @row-click="handleRowClick"
      >
        <el-table-column type="selection" width="55" />
        <el-table-column prop="channelOrderNumber" :label="t('stage5.dataCenter.registrations.channelOrderNumber')" min-width="160" />
        <el-table-column prop="channelName" :label="t('stage5.common.filters.channel')" min-width="140" />
        <el-table-column prop="guestName" :label="t('stage5.common.fields.guestName')" min-width="120" />
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
import axios from 'axios'
import { computed, nextTick, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter, type LocationQueryRaw } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'
import { getAllChannels, type ChannelDTO } from '@/api/channel'
import { getRegistrationLinkInbox, type RegistrationLinkInboxItemDTO } from '@/api/registrationLinkInbox'
import { getRooms, type RoomDTO } from '@/api/room'
import { getAllRoomGroups, getGroupMembers, type RoomGroupDTO } from '@/api/roomGroup'

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

const registrationReviewStatusFilterValues = ['DRAFT', 'SUBMITTED', 'APPROVED', 'REJECTED']
const router = useRouter()
const route = useRoute()
const { t } = useI18n()
const rows = ref<Row[]>([])
const loading = ref(false)
const selectedRows = ref<Row[]>([])
const downloadingPdfs = ref(false)
const status = ref<string | null>(null)
const channels = ref<ChannelDTO[]>([])
const rooms = ref<RoomDTO[]>([])
const roomGroups = ref<Array<RoomGroupDTO & { id: number }>>([])
const roomIdsInSelectedGroup = ref<number[] | null>(null)
const tableRef = ref<{ clearSelection: () => void } | null>(null)
const channelId = ref<number | null>(null)
const reservationStatus = ref<string | null>(null)
const roomNumbers = ref<string[]>([])
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

const selectableRooms = computed(() => {
  if (roomGroupId.value == null || roomIdsInSelectedGroup.value == null) {
    return rooms.value
  }

  const allowedIds = new Set(roomIdsInSelectedGroup.value)
  return rooms.value.filter((room) => allowedIds.has(room.id))
})

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
  const now = new Date()
  return Math.floor(Date.UTC(now.getFullYear(), now.getMonth(), now.getDate()) / 86400000)
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
      ? (resp.data || []).filter(
          (group): group is RoomGroupDTO & { id: number } => typeof group.id === 'number',
        )
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

function getQueryStrings(value: unknown) {
  if (Array.isArray(value)) {
    return value
      .filter((item): item is string => typeof item === 'string')
      .map((item) => item.trim())
      .filter(Boolean)
  }
  const queryValue = getQueryString(value)
  return queryValue ? [queryValue] : []
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
  status.value = getAllowedQueryString(route.query.status, registrationReviewStatusFilterValues)
  channelId.value = getQueryNumber(route.query.channelId)
  reservationStatus.value = getAllowedQueryString(
    route.query.reservationStatus,
    reservationStatusFilterValues,
  )
  roomNumbers.value = getQueryStrings(route.query.roomNumber)
  roomGroupId.value = getQueryNumber(route.query.roomGroupId)
  checkInDate.value = getQueryString(route.query.checkInDate)
  checkOutDate.value = getQueryString(route.query.checkOutDate)
}

function buildFilterQuery(): LocationQueryRaw {
  const query: LocationQueryRaw = {}
  if (status.value) query.status = status.value
  if (channelId.value != null) query.channelId = String(channelId.value)
  if (reservationStatus.value) query.reservationStatus = reservationStatus.value
  if (roomNumbers.value.length > 0) query.roomNumber = roomNumbers.value
  if (roomGroupId.value != null) query.roomGroupId = String(roomGroupId.value)
  if (checkInDate.value) query.checkInDate = checkInDate.value
  if (checkOutDate.value) query.checkOutDate = checkOutDate.value
  return query
}

function normalizeQueryValue(value: unknown) {
  if (Array.isArray(value)) {
    return value.filter((item): item is string => typeof item === 'string')
  }
  return typeof value === 'string' ? value : ''
}

function isSameQuery(left: LocationQueryRaw, right: LocationQueryRaw) {
  const leftKeys = Object.keys(left).sort()
  const rightKeys = Object.keys(right).sort()
  if (leftKeys.length !== rightKeys.length) {
    return false
  }

  return leftKeys.every((key, index) => {
    if (key !== rightKeys[index]) {
      return false
    }
    return JSON.stringify(normalizeQueryValue(left[key])) === JSON.stringify(normalizeQueryValue(right[key]))
  })
}

async function syncFiltersToRouteQuery() {
  const query = buildFilterQuery()
  if (isSameQuery(route.query, query)) {
    return
  }
  await router.replace({ query })
}

function resetFilters() {
  status.value = null
  channelId.value = null
  reservationStatus.value = null
  roomNumbers.value = []
  roomGroupId.value = null
  checkInDate.value = null
  checkOutDate.value = null
  load()
}

async function load() {
  loading.value = true
  try {
    await syncFiltersToRouteQuery()
    const params: Record<string, string | number | string[]> = {}
    if (status.value) params.status = status.value
    if (channelId.value) params.channelId = channelId.value
    if (reservationStatus.value) params.reservationStatus = reservationStatus.value
    if (roomNumbers.value.length > 0) params.roomNumber = roomNumbers.value
    if (roomGroupId.value != null) params.roomGroupId = roomGroupId.value
    if (checkInDate.value) params.checkInDate = checkInDate.value
    if (checkOutDate.value) params.checkOutDate = checkOutDate.value

    const resp = (await request.get('/registrations', {
      params,
      paramsSerializer: {
        indexes: null,
      },
    })) as {
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
      selectedRows.value = []
      await nextTick()
      tableRef.value?.clearSelection()
    } else {
      rows.value = []
      selectedRows.value = []
      ElMessage.error(resp.message || t('stage5.common.messages.dataLoadFailed'))
    }
  } catch (e: any) {
    selectedRows.value = []
    ElMessage.error(e?.response?.data?.message || e?.message || t('stage5.common.messages.dataLoadFailed'))
  } finally {
    loading.value = false
  }
}

async function loadGroupMembers(groupId: number | null) {
  if (groupId == null) {
    roomIdsInSelectedGroup.value = null
    return
  }

  try {
    const resp = await getGroupMembers(groupId)
    roomIdsInSelectedGroup.value = resp.success
      ? (resp.data || [])
          .map((member) => member.roomId)
          .filter((roomId): roomId is number => typeof roomId === 'number')
      : []
    if (!resp.success) {
      ElMessage.error(resp.message || t('stage5.common.messages.dataLoadFailed'))
    }
  } catch (e: any) {
    roomIdsInSelectedGroup.value = []
    ElMessage.error(e?.response?.data?.message || e?.message || t('stage5.common.messages.dataLoadFailed'))
  }
}

function go(row: Row) {
  if (isCancelledReservation(row)) {
    ElMessage.warning(t('stage5.dataCenter.registrations.cancelled'))
    return
  }
  router.push({ name: 'DataCenterRegistrationDetail', params: { formId: row.formId } })
}

function handleSelectionChange(selection: Row[]) {
  selectedRows.value = selection
}

function handleRowClick(row: Row, column?: { type?: string }) {
  if (column?.type === 'selection') {
    return
  }
  go(row)
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

function isCancelledReservation(row: Row) {
  return row.reservationStatus === 'CANCELLED'
}

function getRowClassName({ row }: { row: Row }) {
  return isCancelledReservation(row) ? 'is-cancelled-row' : ''
}

function authHeaders(): Record<string, string> {
  const token = localStorage.getItem('token')
  const currentStore = localStorage.getItem('currentStore')
  let storeId: string | undefined
  if (currentStore) {
    try {
      const store = JSON.parse(currentStore)
      if (store?.id) {
        storeId = String(store.id)
      }
    } catch {
      // ignore malformed local storage
    }
  }
  return {
    ...(token ? { Authorization: `Bearer ${token}` } : {}),
    ...(storeId ? { 'X-Store-Id': storeId } : {}),
  }
}

function registrationPdfUrl(formId: number) {
  const base = (import.meta.env.VITE_API_BASE_URL as string) || '/api/v1'
  return `${base}/registrations/${formId}/pdf`
}

function downloadBlob(blob: Blob, filename: string) {
  const blobUrl = URL.createObjectURL(blob)
  const anchor = document.createElement('a')
  anchor.href = blobUrl
  anchor.download = filename
  document.body.appendChild(anchor)
  anchor.click()
  anchor.remove()
  URL.revokeObjectURL(blobUrl)
}

async function downloadRegistrationPdf(row: Row) {
  const response = await axios.get(registrationPdfUrl(row.formId), {
    responseType: 'blob',
    headers: authHeaders(),
  })
  downloadBlob(response.data, `registration-${row.orderNumber || row.formId}.pdf`)
}

async function downloadSelectedPdfs() {
  if (selectedRows.value.length === 0) {
    return
  }

  downloadingPdfs.value = true
  let failedCount = 0

  try {
    for (const row of selectedRows.value) {
      try {
        await downloadRegistrationPdf(row)
      } catch {
        failedCount += 1
      }
    }
    if (failedCount > 0) {
      ElMessage.error(t('stage5.common.messages.downloadFailed'))
    }
  } finally {
    downloadingPdfs.value = false
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

watch(roomGroupId, async (groupId) => {
  await loadGroupMembers(groupId)
  if (roomNumbers.value.length === 0) {
    return
  }

  const allowedRoomNumbers = new Set(selectableRooms.value.map((room) => room.roomNumber))
  roomNumbers.value = roomNumbers.value.filter((roomNumber) => allowedRoomNumbers.has(roomNumber))
})

onMounted(() => {
  hydrateFiltersFromRouteQuery()
  loadChannels()
  loadRooms()
  loadRoomGroups()
  loadGroupMembers(roomGroupId.value).finally(() => {
    if (roomNumbers.value.length > 0) {
      const allowedRoomNumbers = new Set(selectableRooms.value.map((room) => room.roomNumber))
      roomNumbers.value = roomNumbers.value.filter((roomNumber) =>
        allowedRoomNumbers.has(roomNumber),
      )
    }
    load()
  })
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
