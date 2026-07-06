<template>
  <WorkspaceLayout
    storage-key="registration-review-sidebar-collapsed"
    content-padding="0 24px 24px 0"
    content-padding-narrow="0 20px 20px 0"
    content-padding-mobile="0 20px 20px 0"
  >
    <template #sidebar="{ collapsed, toggleSidebar }">
      <WorkspaceSidebar
        :collapsed="collapsed"
        :items="reviewSidebarItems"
        active-key="review"
        :collapse-label="t('accommodation.layout.collapseNav')"
        aria-label="Review navigation"
        @toggle="toggleSidebar"
        @parent-click="handleSidebarClick"
        @item-select="handleSidebarClick"
      />
    </template>

        <div class="review-page">
          <div class="section-label">{{ t('roomStatus.common.filter') }}</div>

          <section class="filter-surface">
            <div class="filter-row filter-row--actions">
              <el-select
                v-model="status"
                :placeholder="t('stage5.common.filters.status')"
                clearable
                class="filter-control filter-control--status"
                @change="load"
              >
                <el-option :label="t('stage5.common.status.draft')" value="DRAFT" />
                <el-option :label="t('stage5.common.status.submitted')" value="SUBMITTED" />
                <el-option :label="t('stage5.common.status.approved')" value="APPROVED" />
                <el-option :label="t('stage5.common.status.rejected')" value="REJECTED" />
              </el-select>
              <el-button
                type="primary"
                class="filter-button filter-button--primary"
                :disabled="selectedRows.length === 0"
                :loading="downloadingPdfs"
                @click="downloadSelectedPdfs"
              >
                {{ t('stage5.common.actions.downloadPdf') }}
              </el-button>
              <el-button class="filter-button" @click="openLinkDrawer">
                {{ t('stage5.dataCenter.registrations.linkList') }}
              </el-button>
              <el-button class="filter-button" :loading="loading" @click="load">
                {{ t('stage5.common.actions.refresh') }}
              </el-button>
            </div>

            <div class="filter-row filter-row--fields">
              <el-select
                v-model="channelId"
                filterable
                clearable
                :placeholder="t('stage5.common.filters.platform')"
                class="filter-control filter-control--platform"
              >
                <el-option v-for="c in channels" :key="c.id" :label="c.name" :value="c.id" />
              </el-select>
              <el-select
                v-model="reservationStatus"
                clearable
                :placeholder="t('stage5.common.filters.orderStatus')"
                class="filter-control filter-control--order"
              >
                <el-option
                  v-for="option in reservationStatusOptions"
                  :key="option.value"
                  :label="option.label"
                  :value="option.value"
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
                class="filter-control filter-control--room"
              >
                <el-option
                  v-for="room in selectableRooms"
                  :key="room.id"
                  :label="room.roomNumber"
                  :value="room.roomNumber"
                />
              </el-select>
              <el-select
                v-model="roomGroupId"
                filterable
                clearable
                :placeholder="t('stage5.common.filters.roomGroup')"
                class="filter-control filter-control--group"
              >
                <el-option
                  v-for="group in roomGroups"
                  :key="group.id"
                  :label="group.name"
                  :value="group.id"
                />
              </el-select>
              <div class="date-filter">
                <el-date-picker
                  v-model="checkInStartDate"
                  type="date"
                  :placeholder="t('stage5.common.fields.checkInDate')"
                  format="YYYY/MM/DD"
                  value-format="YYYY-MM-DD"
                  clearable
                  class="filter-date"
                />
                <span class="date-separator">{{ t('stage5.common.date.rangeTo') }}</span>
                <el-date-picker
                  v-model="checkOutEndDate"
                  type="date"
                  :placeholder="t('stage5.common.fields.checkOutDate')"
                  format="YYYY/MM/DD"
                  value-format="YYYY-MM-DD"
                  clearable
                  class="filter-date"
                />
              </div>
              <el-button type="primary" class="filter-submit" :loading="loading" @click="load">
                {{ t('stage5.common.actions.query') }}
              </el-button>
              <el-button class="filter-reset" :disabled="loading" @click="resetFilters">
                {{ t('stage5.common.actions.reset') }}
              </el-button>
            </div>
          </section>

          <div class="section-label section-label--table">
            {{ t('stage5.dataCenter.registrations.title') }}
          </div>

          <section class="table-surface">
            <el-table
              ref="tableRef"
              :data="rows"
              border
              v-loading="loading"
              class="review-table"
              style="width: 100%"
              :header-cell-style="headerCellStyle"
              :row-class-name="getRowClassName"
              @selection-change="handleSelectionChange"
              @row-click="handleRowClick"
            >
              <el-table-column type="selection" width="46" />
              <el-table-column
                prop="channelOrderNumber"
                :label="t('stage5.dataCenter.registrations.channelOrderNumber')"
                min-width="136"
              />
              <el-table-column
                prop="channelName"
                :label="t('stage5.common.filters.channel')"
                min-width="124"
              />
              <el-table-column
                prop="guestName"
                :label="t('stage5.common.fields.guestName')"
                min-width="170"
              />

              <el-table-column
                prop="checkInDate"
                :label="t('stage5.common.fields.checkIn')"
                min-width="112"
              >
                <template #default="{ row }">
                  {{ formatDate(row.checkInDate) }}
                </template>
              </el-table-column>
              <el-table-column
                prop="checkOutDate"
                :label="t('stage5.common.fields.checkOut')"
                min-width="112"
              >
                <template #default="{ row }">
                  {{ formatDate(row.checkOutDate) }}
                </template>
              </el-table-column>
              <el-table-column :label="t('stage5.common.filters.orderStatus')" min-width="104">
                <template #default="{ row }">
                  <span
                    class="reservation-status"
                    :class="`reservation-status--${normalizeStatusClass(row.reservationStatus)}`"
                  >
                    {{ getReservationStatusLabel(row.reservationStatus) }}
                  </span>
                </template>
              </el-table-column>
              <el-table-column :label="t('stage5.common.fields.status')" min-width="86">
                <template #default="{ row }">
                  <span
                    class="registration-status"
                    :class="`registration-status--${normalizeStatusClass(row.status)}`"
                  >
                    {{ getRegistrationStatusLabel(row.status) }}
                  </span>
                </template>
              </el-table-column>
              <el-table-column
                prop="submittedAt"
                :label="t('stage5.common.fields.submittedAt')"
                min-width="176"
              >
                <template #default="{ row }">
                  {{ formatDateTime(row.submittedAt) }}
                </template>
              </el-table-column>
              <el-table-column
                prop="updatedAt"
                :label="t('stage5.common.fields.updatedAt')"
                min-width="176"
              >
                <template #default="{ row }">
                  {{ formatDateTime(row.updatedAt) }}
                </template>
              </el-table-column>
              <el-table-column :label="t('stage5.common.fields.status')" width="86" fixed="right">
                <template #default="{ row }">
                  <el-tooltip
                    v-if="isCancelledReservation(row)"
                    :content="t('stage5.dataCenter.registrations.cancelled')"
                    placement="top"
                  >
                    <span class="disabled-action">
                      <el-button class="review-action-button" size="small" disabled>
                        {{ t('nav.review') }}
                      </el-button>
                    </span>
                  </el-tooltip>
                  <el-button
                    v-else
                    class="review-action-link"
                    size="small"
                    type="primary"
                    link
                    @click.stop="go(row)"
                  >
                    {{ t('nav.review') }}
                  </el-button>
                </template>
              </el-table-column>
            </el-table>
          </section>
        </div>

    <el-drawer
      v-model="linkDrawerVisible"
      :title="t('stage5.dataCenter.registrations.linkList')"
      size="80%"
    >
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
  </WorkspaceLayout>
</template>

<script setup lang="ts">
import axios from 'axios'
import { computed, nextTick, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter, type LocationQueryRaw } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { DocumentChecked } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import WorkspaceLayout from '@/components/layout/WorkspaceLayout.vue'
import WorkspaceSidebar from '@/components/layout/WorkspaceSidebar.vue'
import type { WorkspaceSidebarItem } from '@/components/layout/workspace'
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
  roomTypeName?: string | null
  checkInDate: string
  checkOutDate: string
  reservationStatus?: string | null
  status: string
  submittedAt?: string
  updatedAt?: string
}
type ReservationStatusTagType = 'success' | 'warning' | 'info' | 'danger'
type DateRange = [string, string]

const registrationReviewStatusFilterValues = ['DRAFT', 'SUBMITTED', 'APPROVED', 'REJECTED']
const router = useRouter()
const route = useRoute()
const { t } = useI18n()
const reviewSidebarItems = computed<WorkspaceSidebarItem[]>(() => [
  {
    key: 'review',
    label: t('nav.review'),
    icon: DocumentChecked,
  },
])

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
const checkInDateRange = ref<DateRange | null>(null)
const checkOutDateRange = ref<DateRange | null>(null)
const linkDrawerVisible = ref(false)
const linkLoading = ref(false)
const linkRows = ref<RegistrationLinkInboxItemDTO[]>([])
const linkReservationStatus = ref<string | null>(null)

const headerCellStyle = {
  background: '#fbfbfb',
  color: '#252525',
  fontSize: '12px',
  fontWeight: '600',
  padding: '0',
}

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

const checkInStartDate = computed({
  get: () => checkInDateRange.value?.[0] || '',
  set: (value: string) => {
    if (!value) {
      checkInDateRange.value = null
      return
    }
    checkInDateRange.value = [value, value]
  },
})

const checkOutEndDate = computed({
  get: () => checkOutDateRange.value?.[1] || '',
  set: (value: string) => {
    if (!value) {
      checkOutDateRange.value = null
      return
    }
    checkOutDateRange.value = [value, value]
  },
})

const handleSidebarClick = () => {
  if (route.path !== '/data-center/registrations') {
    router.push('/data-center/registrations')
  }
}

const normalizeStatusClass = (value?: string | null) => {
  return (value || 'unknown').toLowerCase().replace(/_/g, '-')
}

const formatDate = (value?: string | null) => {
  if (!value) {
    return '-'
  }
  const datePart = value.split('T')[0]
  const match = datePart.match(/^(\d{4})-(\d{2})-(\d{2})$/)
  return match ? `${match[1]}/${match[2]}/${match[3]}` : value
}

const formatDateTime = (value?: string | null) => {
  if (!value) {
    return '-'
  }
  const normalized = value.replace('T', ' ')
  const match = normalized.match(/^(\d{4})-(\d{2})-(\d{2})\s+(\d{2}):(\d{2}):(\d{2})/)
  return match
    ? `${match[1]}/${match[2]}/${match[3]} ${match[4]}:${match[5]}:${match[6]}`
    : normalized
}

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

function getDateRangeFromQuery(startValue: unknown, endValue: unknown, legacyValue: unknown) {
  const startDate = getQueryString(startValue)
  const endDate = getQueryString(endValue)
  if (startDate && endDate) {
    return [startDate, endDate] as DateRange
  }

  const legacyDate = getQueryString(legacyValue)
  if (legacyDate) {
    return [legacyDate, legacyDate] as DateRange
  }

  return null
}

function getCompleteDateRange(dateRange: DateRange | null) {
  if (!dateRange) {
    return null
  }

  const startDate = dateRange[0]?.trim()
  const endDate = dateRange[1]?.trim()
  if (!startDate || !endDate) {
    return null
  }

  return [startDate, endDate] as DateRange
}

function addDateRangeToQuery(
  query: LocationQueryRaw,
  dateRange: DateRange | null,
  startKey: string,
  endKey: string,
) {
  const completeRange = getCompleteDateRange(dateRange)
  if (!completeRange) {
    return
  }

  query[startKey] = completeRange[0]
  query[endKey] = completeRange[1]
}

function addDateRangeToParams(
  params: Record<string, string | number | string[]>,
  dateRange: DateRange | null,
  startKey: string,
  endKey: string,
) {
  const completeRange = getCompleteDateRange(dateRange)
  if (!completeRange) {
    return
  }

  params[startKey] = completeRange[0]
  params[endKey] = completeRange[1]
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
  checkInDateRange.value = getDateRangeFromQuery(
    route.query.checkInStartDate,
    route.query.checkInEndDate,
    route.query.checkInDate,
  )
  checkOutDateRange.value = getDateRangeFromQuery(
    route.query.checkOutStartDate,
    route.query.checkOutEndDate,
    route.query.checkOutDate,
  )
}

function buildFilterQuery(): LocationQueryRaw {
  const query: LocationQueryRaw = {}
  if (status.value) query.status = status.value
  if (channelId.value != null) query.channelId = String(channelId.value)
  if (reservationStatus.value) query.reservationStatus = reservationStatus.value
  if (roomNumbers.value.length > 0) query.roomNumber = roomNumbers.value
  if (roomGroupId.value != null) query.roomGroupId = String(roomGroupId.value)
  addDateRangeToQuery(query, checkInDateRange.value, 'checkInStartDate', 'checkInEndDate')
  addDateRangeToQuery(query, checkOutDateRange.value, 'checkOutStartDate', 'checkOutEndDate')
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
  checkInDateRange.value = null
  checkOutDateRange.value = null
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
    addDateRangeToParams(params, checkInDateRange.value, 'checkInStartDate', 'checkInEndDate')
    addDateRangeToParams(params, checkOutDateRange.value, 'checkOutStartDate', 'checkOutEndDate')

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
.review-page {
  min-width: 1120px;
  padding: 0 0 12px 24px;
}

.section-label {
  height: 30px;
  display: flex;
  align-items: flex-end;
  padding: 0 0 7px;
  color: #d6d6d6;
  font-size: 14px;
  font-weight: 600;
  line-height: 1;
}

.section-label--table {
  height: 28px;
}

.filter-surface {
  --filter-control-width: 156px;
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 12px 18px;
  border: 1px solid #ededed;
  border-radius: 6px;
  background: #ffffff;
  overflow-x: auto;
}

.filter-row {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: max-content;
}

.filter-row--actions {
  gap: 6px;
}

.filter-control {
  width: var(--filter-control-width);
  flex: 0 0 var(--filter-control-width);
}

.date-filter {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  flex: 0 0 auto;
}

.filter-date {
  width: var(--filter-control-width) !important;
  flex: 0 0 var(--filter-control-width);
}

.filter-surface :deep(.filter-date.el-date-editor),
.filter-surface :deep(.filter-date.el-date-editor.el-input),
.filter-surface :deep(.filter-date.el-input) {
  width: var(--filter-control-width) !important;
  flex: 0 0 var(--filter-control-width);
}

.date-separator {
  color: #5f5f5f;
  font-size: 14px;
  line-height: 1;
}

.filter-button,
.filter-submit,
.filter-reset {
  height: 34px;
  min-width: 58px;
  padding: 0 14px;
  border-radius: 4px;
  font-size: 13px;
  font-weight: 500;
}

.filter-button--primary,
.filter-submit {
  border-color: #2196f3;
  background: #2196f3;
}

.filter-reset {
  margin-left: 6px;
}

.filter-submit,
.filter-reset {
  width: 72px;
  min-width: 72px;
  padding: 0;
}

.filter-surface :deep(.el-input__wrapper),
.filter-surface :deep(.el-select__wrapper) {
  min-height: 34px;
  border-radius: 4px;
  box-shadow: 0 0 0 1px #dddddd inset;
}

.filter-surface :deep(.el-select__wrapper) {
  padding: 0 0 0 14px;
}

.filter-surface :deep(.el-input__wrapper:hover),
.filter-surface :deep(.el-input__wrapper.is-focus),
.filter-surface :deep(.el-select__wrapper:hover),
.filter-surface :deep(.el-select__wrapper.is-focused) {
  box-shadow: 0 0 0 1px #bdbdbd inset;
}

.filter-surface :deep(.el-input__inner),
.filter-surface :deep(.el-select__placeholder),
.filter-surface :deep(.el-select__selected-item) {
  font-size: 13px;
}

.filter-surface :deep(.el-select__suffix) {
  align-self: stretch;
  width: 40px;
  margin-left: auto;
  padding: 0;
  border-left: 1px solid #dddddd;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: #a8abb2;
}

.filter-surface :deep(.el-select__suffix .el-icon) {
  margin: 0;
}

.table-surface {
  border: 1px solid #e3e3e3;
  border-radius: 0;
  background: #ffffff;
  overflow: hidden;
}

.review-table {
  width: 100%;
  --el-table-border-color: #e0e0e0;
  --el-table-header-bg-color: #fbfbfb;
  --el-table-row-hover-bg-color: #fafafa;
  color: #303030;
  font-size: 14px;
}

:deep(.review-table .el-table__inner-wrapper::before),
:deep(.review-table .el-table__border-left-patch) {
  display: none;
}

:deep(.review-table .el-table__header th.el-table__cell) {
  height: 42px;
  padding: 0;
  border-color: #d8d8d8;
}

:deep(.review-table .el-table__header th.el-table__cell .cell) {
  padding: 0 10px;
  color: #252525;
  line-height: 1.3;
}

:deep(.review-table .el-table__body td.el-table__cell) {
  height: 42px;
  padding: 0;
  border-color: #e5e5e5;
  background: #ffffff;
}

:deep(.review-table .el-table__body td.el-table__cell .cell) {
  padding: 0 10px;
  color: #303030;
  line-height: 1.3;
}

:deep(.review-table .el-table__body tr:nth-child(even) td.el-table__cell) {
  background: #ffffff;
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

.reservation-status,
.registration-status {
  font-size: 12px;
  font-weight: 600;
  white-space: nowrap;
}

.reservation-status--confirmed,
.reservation-status--checked-in,
.registration-status--approved {
  color: #23b26d;
}

.reservation-status--cancelled,
.registration-status--rejected {
  color: #f04b56;
}

.reservation-status--requested,
.registration-status--submitted {
  color: #d7961d;
}

.reservation-status--checked-out,
.registration-status--draft,
.reservation-status--no-show,
.reservation-status--unknown,
.registration-status--unknown {
  color: #858585;
}

.review-action-link {
  min-width: 34px;
  padding: 0;
  color: #1689df;
  font-size: 13px;
  font-weight: 500;
}

.review-action-button {
  height: 24px;
  padding: 0 9px;
  border-radius: 4px;
  font-size: 12px;
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

.disabled-action {
  display: inline-flex;
}

@media (max-width: 1280px) {
  .review-page {
    padding-left: 20px;
  }
}

@media (max-width: 768px) {
  .review-page {
    min-width: 980px;
    padding-left: 12px;
  }
}
</style>
