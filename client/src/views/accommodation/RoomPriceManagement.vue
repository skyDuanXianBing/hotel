<template>
  <div class="price-management-page">
    <RoomPriceTabs />

    <section class="price-management-surface">
      <div class="top-controls">
        <div class="toolbar-left">
          <div class="date-navigation">
            <div class="date-nav-group">
              <el-button class="nav-button nav-button--wide" size="small" @click="previousWeek">
                <el-icon><ArrowLeft /></el-icon>
                <span>{{ t('accommodation.roomPrice.previousWeek') }}</span>
              </el-button>
              <el-button class="nav-button nav-button--compact" size="small" @click="previousDay">
                <el-icon><ArrowLeft /></el-icon>
                <span>{{ t('accommodation.common.dayUnit') }}</span>
              </el-button>
            </div>
            <el-date-picker
              v-model="selectedDate"
              type="date"
              :placeholder="t('accommodation.roomPrice.selectDate')"
              format="YYYY/MM/DD"
              value-format="YYYY-MM-DD"
              size="small"
              class="date-picker"
            />
            <div class="date-nav-group date-nav-group--right">
              <el-button class="nav-button nav-button--compact" size="small" @click="nextDay">
                <span>{{ t('accommodation.common.dayUnit') }}</span>
                <el-icon><ArrowRight /></el-icon>
              </el-button>
              <el-button class="nav-button nav-button--wide" size="small" @click="nextWeek">
                <span>{{ t('accommodation.roomPrice.nextWeek') }}</span>
                <el-icon><ArrowRight /></el-icon>
              </el-button>
            </div>
          </div>
        </div>

        <div class="toolbar-right">
          <label class="filter-field">
            <span class="filter-label">{{ t('accommodation.roomPrice.roomTypeFilter') }}</span>
            <el-select
              v-model="selectedRoomTypeId"
              :placeholder="t('accommodation.common.all')"
              size="small"
              clearable
              class="filter-select"
            >
              <el-option :label="t('accommodation.common.all')" :value="null" />
              <el-option
                v-for="roomType in roomTypes"
                :key="roomType.id"
                :label="roomType.name"
                :value="roomType.id"
              />
            </el-select>
          </label>

          <label class="filter-field">
            <span class="filter-label">{{ t('accommodation.roomPrice.groupFilter') }}</span>
            <el-select
              v-model="selectedRoomGroupId"
              :placeholder="t('accommodation.common.all')"
              size="small"
              clearable
              class="filter-select"
            >
              <el-option :label="t('accommodation.common.all')" :value="null" />
              <el-option
                v-for="group in roomGroupOptions"
                :key="group.id"
                :label="group.name"
                :value="group.id"
              />
            </el-select>
          </label>

          <el-button class="bulk-update-button" type="primary" size="small" @click="goToBulkUpdate">
            {{ t('accommodation.roomPrice.bulkUpdate') }}
          </el-button>
        </div>
      </div>

      <div class="price-table-container">
        <el-table
          :data="priceTableData"
          border
          v-loading="loading"
          class="price-table"
          :height="PRICE_TABLE_HEIGHT"
          :header-cell-style="headerCellStyle"
          :cell-style="cellStyle"
          :cell-class-name="getCellClassName"
          :row-class-name="getRowClassName"
        >
          <el-table-column :label="t('accommodation.common.date')" width="132" fixed="left">
            <template #default="{ row }">
              <div v-if="row.isRoomHeader" class="room-name-cell">
                <div class="room-type-name">{{ row.roomTypeName }}</div>
              </div>
              <div v-else class="plan-name-cell">
                <div class="plan-name">{{ row.pricePlanName }}</div>
                <div v-if="!isEditablePriceRow(row)" class="plan-uneditable-reason">
                  {{ getRowUneditableReason(row) }}
                </div>
                <el-link
                  v-if="hasChannelSummary(row)"
                  type="primary"
                  :underline="false"
                  class="channel-link"
                  @click="showChannels(row)"
                >
                  <el-icon><Link /></el-icon>
                  {{ row.channelCount }}
                </el-link>
              </div>
            </template>
          </el-table-column>

          <el-table-column
            v-for="date in dateColumns"
            :key="date.dateStr"
            :label="date.label"
            width="88"
            align="center"
          >
            <template #header>
              <div class="date-header">
                <div class="date-day">{{ date.dayLabel }}</div>
                <div class="date-weekday">{{ date.weekday }}</div>
              </div>
            </template>

            <template #default="{ row }">
              <div v-if="row.isRoomHeader" class="room-count-cell">
                <span class="room-count-value">{{ getRoomsCount(row, date.dateStr) }}</span>
              </div>
              <button
                v-else
                type="button"
                class="price-cell"
                :class="{
                  'sold-out-cell': isRoomsSoldOut(row, date.dateStr),
                  'uneditable-cell': !isEditablePriceRow(row),
                }"
                :aria-disabled="!isEditablePriceRow(row)"
                :title="!isEditablePriceRow(row) ? getRowUneditableReason(row) : undefined"
                @click="openPriceEditDialog(row, date)"
              >
                <div class="price-content">
                  <div class="price-value">
                    {{ formatDisplayPrice(getDisplayPrice(row, date.dateStr)) }}
                  </div>
                  <div class="rooms-count">
                    <el-icon><Moon /></el-icon>
                    {{ row.dates[date.dateStr]?.minStay ?? 1 }}
                  </div>
                </div>
              </button>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </section>

    <el-drawer
      v-model="showPriceEditDialog"
      :title="t('accommodation.roomPrice.editTitle')"
      direction="rtl"
      size="500px"
      :before-close="closePriceEditDialog"
    >
      <div class="drawer-content">
        <el-form :model="editForm" label-width="80px" label-position="top">
          <el-form-item :label="t('accommodation.common.dateRange')" required>
            <div class="date-range-input">
              <el-date-picker
                v-model="editForm.startDate"
                type="date"
                :placeholder="t('accommodation.common.startDate')"
                format="YYYY/MM/DD"
                value-format="YYYY-MM-DD"
                size="default"
                style="width: 100%"
              />
              <span class="date-separator">-</span>
              <el-date-picker
                v-model="editForm.endDate"
                type="date"
                :placeholder="t('accommodation.common.endDate')"
                format="YYYY/MM/DD"
                value-format="YYYY-MM-DD"
                size="default"
                style="width: 100%"
              />
            </div>
          </el-form-item>

          <el-form-item :label="t('accommodation.common.weekdays')" required>
            <div class="weekday-selector">
              <el-checkbox-group
                v-model="editForm.weekdays"
                class="weekday-group"
                @change="handleWeekdayChange"
              >
                <el-checkbox
                  v-for="weekday in weekdayOptions"
                  :key="weekday.value"
                  :label="weekday.value"
                >
                  {{ weekday.label }}
                </el-checkbox>
              </el-checkbox-group>
              <el-button text type="primary" @click="invertEditWeekdays">
                {{ t('accommodation.roomPrice.invertSelection') }}
              </el-button>
            </div>
          </el-form-item>

          <el-form-item>
            <el-link type="primary" :underline="false" @click="addMoreSegments">
              {{ t('accommodation.roomPrice.addMoreSegments') }}
            </el-link>
          </el-form-item>

          <el-form-item :label="t('accommodation.roomPrice.setting')" required>
            <el-select
              v-model="editForm.settingType"
              :placeholder="t('accommodation.common.select')"
              style="width: 100%"
            >
              <el-option
                v-for="option in roomPriceSettingOptions"
                :key="option.value"
                :label="option.label"
                :value="option.value"
              />
            </el-select>
          </el-form-item>

          <el-divider />

          <div class="room-price-section">
            <div class="section-title">
              <span>{{ editForm.roomTypeName }}</span>
              <span class="room-code">{{ editForm.roomCode }}</span>
            </div>

            <el-form-item
              v-if="editForm.settingType === 'price'"
              :label="editForm.pricePlanName"
              required
            >
              <el-input
                v-model="editForm.price"
                :placeholder="t('accommodation.roomPrice.inputPrice')"
                type="number"
              >
                <template #append>{{ t('accommodation.common.jpy') }}</template>
              </el-input>
            </el-form-item>

            <el-form-item
              v-if="editForm.settingType === 'minStay'"
              :label="t('accommodation.roomPrice.settingType.minStay')"
              required
            >
              <el-input
                v-model="editForm.minStay"
                :placeholder="t('accommodation.roomPrice.inputMinStay')"
                type="number"
                :min="1"
                :max="99"
              >
                <template #append>{{ t('accommodation.common.dayUnit') }}</template>
              </el-input>
            </el-form-item>

            <el-form-item
              v-if="editForm.settingType === 'maxStay'"
              :label="t('accommodation.roomPrice.settingType.maxStay')"
              required
            >
              <el-input
                v-model="editForm.maxStay"
                :placeholder="t('accommodation.roomPrice.inputMaxStay')"
                type="number"
                :min="1"
                :max="99"
              >
                <template #append>{{ t('accommodation.common.dayUnit') }}</template>
              </el-input>
            </el-form-item>

            <el-form-item
              v-if="editForm.settingType === 'closeRoom'"
              :label="t('accommodation.roomPrice.closeRoom')"
              required
            >
              <el-select
                v-model="editForm.closeRoomStatus"
                :placeholder="t('accommodation.common.select')"
                style="width: 100%"
              >
                <el-option
                  v-for="option in onOffOptions"
                  :key="option.value"
                  :label="option.label"
                  :value="option.value"
                />
              </el-select>
            </el-form-item>

            <el-form-item
              v-if="editForm.settingType === 'cta'"
              :label="t('accommodation.roomPrice.settingType.cta')"
              required
            >
              <el-select
                v-model="editForm.closeRoomStatus"
                :placeholder="t('accommodation.common.select')"
                style="width: 100%"
              >
                <el-option
                  v-for="option in onOffOptions"
                  :key="option.value"
                  :label="option.label"
                  :value="option.value"
                />
              </el-select>
            </el-form-item>

            <el-form-item
              v-if="editForm.settingType === 'ctd'"
              :label="t('accommodation.roomPrice.settingType.ctd')"
              required
            >
              <el-select
                v-model="editForm.closeRoomStatus"
                :placeholder="t('accommodation.common.select')"
                style="width: 100%"
              >
                <el-option
                  v-for="option in onOffOptions"
                  :key="option.value"
                  :label="option.label"
                  :value="option.value"
                />
              </el-select>
            </el-form-item>
          </div>
        </el-form>
      </div>

      <template #footer>
        <div class="drawer-footer">
          <el-button @click="closePriceEditDialog" size="large">
            {{ t('accommodation.common.cancel') }}
          </el-button>
          <el-button type="primary" @click="savePriceEdit" :loading="saving" size="large">
            {{ t('accommodation.common.confirm') }}
          </el-button>
        </div>
      </template>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { ArrowLeft, ArrowRight, Link, Moon } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getRoomPriceManagementData,
  updatePriceByPlan,
  type ChannelRefDTO,
  type RoomPriceManagementDTO,
} from '@/api/roomPrice'
import {
  getAllRoomGroups,
  getGroupMembers,
  type RoomGroupDTO,
  type RoomGroupMemberDTO,
} from '@/api/roomGroup'
import { getRooms, type RoomDTO } from '@/api/room'
import { getAllRoomTypes } from '@/api/roomType'
import { useAccommodationI18n } from '@/composables/useAccommodationI18n'
import { useUserStore } from '@/stores/user'
import {
  addCalendarMonthsToYmd,
  addDaysToYmd,
  formatYmdMonthDay,
  getStoreTodayYmd,
  getYmdWeekdayIndex,
  parseYmdAsUtcDate,
} from '@/utils/storeDateTime'
import RoomPriceTabs from './components/RoomPriceTabs.vue'

const router = useRouter()
const userStore = useUserStore()
const { t } = useI18n()
const { weekdayOptions, weekdayShortMap, onOffOptions, roomPriceSettingOptions } =
  useAccommodationI18n()

const PRICE_TABLE_HEIGHT = 'calc(100vh - 250px)'
const CALENDAR_MONTH_SPAN = 1
const BINDING_STATE_BOUND = 'BOUND'
const BINDING_STATE_UNBOUND = 'UNBOUND'

type RoomGroupOption = RoomGroupDTO & { id: number }

interface PriceTableRow {
  id: string
  roomTypeId: number
  roomTypeName: string
  planKey?: string
  pricePlanId?: number
  pricePlanName?: string
  bindingState?: string
  editable: boolean
  uneditableReason?: string
  channelCount?: number
  channelRefs: ChannelRefDTO[]
  isRoomHeader: boolean
  dates: Record<
    string,
    {
      price?: number
      rooms?: number
      minStay?: number
      priceSource?: string
      priceLabsBasePrice?: number
      priceLabsUpdatedAt?: string
      manualOverride?: boolean
      manualOverrideUntil?: string
    }
  >
}

const loading = ref(false)
const saving = ref(false)
const selectedDate = ref(getStoreTodayYmd())
const selectedRoomTypeId = ref<number | null>(null)
const selectedRoomGroupId = ref<number | null>(null)

const roomTypes = ref<any[]>([])
const priceData = ref<RoomPriceManagementDTO[]>([])
const roomGroupOptions = ref<RoomGroupOption[]>([])
const roomGroupRoomTypeIdsMap = ref<Record<number, number[]>>({})
const showPriceEditDialog = ref(false)

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
  maxStay: '',
  closeRoomStatus: 'off',
  currentCloseRoom: false,
  currentCta: false,
  currentCtd: false,
})

const getCalendarEndDateString = (startDateYmd: string): string => {
  const nextMonthDate = addCalendarMonthsToYmd(startDateYmd, CALENDAR_MONTH_SPAN)
  return addDaysToYmd(nextMonthDate, -1)
}

const dateColumns = computed(() => {
  const columns: Array<{
    dateStr: string
    dayLabel: string
    weekday: string
    label: string
  }> = []

  const endDate = getCalendarEndDateString(selectedDate.value)
  let currentDate = selectedDate.value

  while (currentDate <= endDate) {
    const weekdayIndex = getYmdWeekdayIndex(currentDate)
    const { month, day } = formatYmdMonthDay(currentDate)
    const dayLabel = `${month}/${day}`

    columns.push({
      dateStr: currentDate,
      dayLabel,
      weekday: weekdayShortMap.value[weekdayIndex],
      label: `${dayLabel} ${weekdayShortMap.value[weekdayIndex]}`,
    })

    currentDate = addDaysToYmd(currentDate, 1)
  }

  return columns
})

const getDisplayPrice = (row: PriceTableRow, priceDate: string): number | undefined => {
  const cellData = row.dates[priceDate]
  if (cellData?.price !== undefined && cellData?.price !== null) {
    return cellData.price
  }

  return undefined
}

const getRoomsCount = (row: PriceTableRow, priceDate: string): number => {
  const rooms = Number(row.dates[priceDate]?.rooms ?? 0)
  return Number.isFinite(rooms) ? rooms : 0
}

const isRoomsSoldOut = (row: PriceTableRow, priceDate: string): boolean => {
  return getRoomsCount(row, priceDate) <= 0
}

const selectedGroupRoomTypeIds = computed<number[]>(() => {
  if (selectedRoomGroupId.value === null) {
    return []
  }

  return roomGroupRoomTypeIdsMap.value[selectedRoomGroupId.value] || []
})

const getFilteredDisplayRoomTypes = () => {
  let displayRoomTypes = [...roomTypes.value]

  if (selectedRoomTypeId.value !== null) {
    displayRoomTypes = displayRoomTypes.filter(
      (roomType) => roomType.id === selectedRoomTypeId.value,
    )
  }

  if (selectedRoomGroupId.value !== null) {
    const allowedRoomTypeIds = new Set(selectedGroupRoomTypeIds.value)
    displayRoomTypes = displayRoomTypes.filter((roomType) => allowedRoomTypeIds.has(roomType.id))
  }

  return displayRoomTypes
}

const normalizeChannelRefs = (channelRefs?: ChannelRefDTO[]): ChannelRefDTO[] => {
  if (!Array.isArray(channelRefs)) {
    return []
  }

  return channelRefs.filter((channelRef) => typeof channelRef.channelId === 'number')
}

const getChannelCount = (record: RoomPriceManagementDTO): number => {
  if (typeof record.channelCount === 'number' && Number.isFinite(record.channelCount)) {
    return record.channelCount
  }

  return normalizeChannelRefs(record.channelRefs).length
}

const mergeChannelRefs = (target: ChannelRefDTO[], source: ChannelRefDTO[]) => {
  const existingIds = new Set(target.map((channelRef) => channelRef.channelId))
  source.forEach((channelRef) => {
    if (!existingIds.has(channelRef.channelId)) {
      target.push(channelRef)
      existingIds.add(channelRef.channelId)
    }
  })
}

const isEditablePriceRow = (row: PriceTableRow): boolean => {
  return (
    !row.isRoomHeader &&
    row.bindingState === BINDING_STATE_BOUND &&
    row.editable === true &&
    typeof row.pricePlanId === 'number'
  )
}

const getRowUneditableReason = (row: PriceTableRow): string => {
  return row.uneditableReason || t('accommodation.roomPrice.messages.noPricePlanBound')
}

const hasChannelSummary = (row: PriceTableRow): boolean => {
  return Boolean(row.channelCount && row.channelCount > 0 && row.channelRefs.length > 0)
}

const priceTableData = computed<PriceTableRow[]>(() => {
  const rows: PriceTableRow[] = []
  const displayRoomTypes = getFilteredDisplayRoomTypes()

  displayRoomTypes.forEach((roomType) => {
    const roomTypeRecords = priceData.value.filter((record) => record.roomTypeId === roomType.id)
    const headerDates: Record<string, { rooms: number }> = {}

    dateColumns.value.forEach((dateCol) => {
      const priceRecords = roomTypeRecords.filter((record) => record.priceDate === dateCol.dateStr)

      let availableRooms = roomType.totalRooms || 0
      if (priceRecords.length > 0) {
        availableRooms =
          priceRecords[0].availableRooms !== undefined &&
          priceRecords[0].availableRooms !== null
            ? priceRecords[0].availableRooms
            : roomType.totalRooms || 0
      }

      headerDates[dateCol.dateStr] = { rooms: availableRooms }
    })

    rows.push({
      id: `roomtype-header-${roomType.id}`,
      roomTypeId: roomType.id,
      roomTypeName: roomType.name,
      editable: false,
      channelRefs: [],
      isRoomHeader: true,
      dates: headerDates,
    })

    const roomTypePlanRows = new Map<
      string,
      {
        key: string
        id?: number
        name: string
        bindingState: string
        editable: boolean
        uneditableReason?: string
        channelCount: number
        channelRefs: ChannelRefDTO[]
      }
    >()

    roomTypeRecords.forEach((record) => {
      if (typeof record.pricePlanId === 'number') {
        const key = `plan-${record.pricePlanId}`
        const channelRefs = normalizeChannelRefs(record.channelRefs)
        if (!roomTypePlanRows.has(key)) {
          let planName = record.pricePlanName
          if (!planName) {
            planName = t('accommodation.roomPrice.unnamedPricePlan')
          }

          roomTypePlanRows.set(key, {
            key,
            id: record.pricePlanId,
            name: planName,
            bindingState: record.bindingState || BINDING_STATE_BOUND,
            editable: record.editable !== false,
            uneditableReason: record.uneditableReason,
            channelCount: getChannelCount(record),
            channelRefs,
          })
        } else {
          const existingPlan = roomTypePlanRows.get(key)
          if (existingPlan) {
            existingPlan.channelCount = Math.max(existingPlan.channelCount, getChannelCount(record))
            mergeChannelRefs(existingPlan.channelRefs, channelRefs)
          }
        }
        return
      }

      const key = `unbound-${roomType.id}`

      if (!roomTypePlanRows.has(key)) {
        roomTypePlanRows.set(key, {
          key,
          id: undefined,
          name: t('accommodation.roomPrice.unboundPricePlanName'),
          bindingState: record.bindingState || BINDING_STATE_UNBOUND,
          editable: false,
          uneditableReason:
            record.uneditableReason || t('accommodation.roomPrice.messages.noPricePlanBound'),
          channelCount: 0,
          channelRefs: [],
        })
      }
    })

    roomTypePlanRows.forEach((plan) => {
      const dates: PriceTableRow['dates'] = {}

      dateColumns.value.forEach((dateCol) => {
        const priceRecord = roomTypeRecords.find((record) => {
          if (record.priceDate !== dateCol.dateStr) {
            return false
          }

          if (plan.bindingState === BINDING_STATE_UNBOUND) {
            return typeof record.pricePlanId !== 'number'
          }

          return record.pricePlanId === plan.id
        })

        dates[dateCol.dateStr] = {
          price: priceRecord?.price ?? 0,
          rooms: priceRecord?.availableRooms ?? 0,
          minStay: priceRecord?.minStay ?? 1,
          priceSource: priceRecord?.priceSource,
          priceLabsBasePrice: priceRecord?.priceLabsBasePrice,
          priceLabsUpdatedAt: priceRecord?.priceLabsUpdatedAt,
          manualOverride: priceRecord?.manualOverride,
          manualOverrideUntil: priceRecord?.manualOverrideUntil,
        }
      })

      rows.push({
        id: `roomtype-plan-${roomType.id}-${plan.key}`,
        roomTypeId: roomType.id,
        roomTypeName: roomType.name,
        planKey: plan.key,
        pricePlanId: plan.id,
        pricePlanName: plan.name,
        bindingState: plan.bindingState,
        editable: plan.editable,
        uneditableReason: plan.uneditableReason,
        channelCount: plan.channelCount,
        channelRefs: plan.channelRefs,
        isRoomHeader: false,
        dates,
      })
    })
  })

  return rows
})

const formatDisplayPrice = (price: number | undefined): string => {
  if (price === undefined || price === null) return '¥-'
  return `¥${price.toLocaleString()}`
}

const showChannels = (row: PriceTableRow) => {
  if (!hasChannelSummary(row)) {
    ElMessage.info(t('accommodation.roomPrice.messages.noChannelSummary'))
    return
  }

  const channelNames = row.channelRefs
    .map((channelRef) => {
      if (channelRef.channelName) {
        return channelRef.channelName
      }
      if (channelRef.channelCode) {
        return channelRef.channelCode
      }
      return t('accommodation.roomPrice.unnamedChannel')
    })
    .join(', ')

  ElMessageBox.alert(
    t('accommodation.roomPrice.messages.channelSummary', {
      name: row.pricePlanName,
      channels: channelNames,
    }),
    t('accommodation.roomPrice.channelSummaryTitle'),
    {
      confirmButtonText: t('accommodation.common.confirm'),
    },
  )
}

const formatPrice = (price: number | undefined): string => {
  if (price === undefined || price === null) return '濠?'
  return `濠?{price.toLocaleString()}`
}

const headerCellStyle = {
  background: '#eeeeee',
  color: '#252525',
  textAlign: 'center' as const,
  fontSize: '12px',
  fontWeight: '600',
  padding: '0',
}

const cellStyle = ({
  row,
  columnIndex,
}: {
  row: PriceTableRow
  columnIndex: number
}) => {
  const baseStyle = {
    textAlign: 'center' as const,
    padding: '0',
    fontSize: '12px',
  }

  if (row.isRoomHeader) {
    return baseStyle
  }

  if (columnIndex === 0) {
    return {
      ...baseStyle,
      backgroundColor: '#f7f7f7',
    }
  }

  const dateStr = dateColumns.value[columnIndex - 1]?.dateStr

  return {
    ...baseStyle,
    backgroundColor: dateStr && isRoomsSoldOut(row, dateStr) ? '#ffe9e9' : '#f7f7f7',
  }
}

const getRowClassName = ({ row }: { row: PriceTableRow }): string => {
  return row.isRoomHeader ? 'room-type-header-row' : 'price-plan-row'
}

const getCellClassName = ({
  row,
  columnIndex,
}: {
  row: PriceTableRow
  columnIndex: number
}): string => {
  if (row.isRoomHeader) {
    return ''
  }

  if (columnIndex === 0) {
    return 'price-plan-label-td'
  }

  const dateIndex = columnIndex - 1
  if (dateIndex < 0 || dateIndex >= dateColumns.value.length) {
    return ''
  }

  const dateStr = dateColumns.value[dateIndex]?.dateStr
  if (!dateStr) {
    return ''
  }

  const classNames = ['price-plan-cell-td']
  if (isRoomsSoldOut(row, dateStr)) {
    classNames.push('sold-out-td')
  }
  if (!isEditablePriceRow(row)) {
    classNames.push('uneditable-td')
  }

  return classNames.join(' ')
}

const previousDay = () => {
  selectedDate.value = addDaysToYmd(selectedDate.value, -1)
}

const nextDay = () => {
  selectedDate.value = addDaysToYmd(selectedDate.value, 1)
}

const previousWeek = () => {
  selectedDate.value = addDaysToYmd(selectedDate.value, -7)
}

const nextWeek = () => {
  selectedDate.value = addDaysToYmd(selectedDate.value, 7)
}

const loadRoomTypes = async () => {
  try {
    const response = await getAllRoomTypes()
    if (response.success && response.data) {
      roomTypes.value = response.data
    }
  } catch (error) {
    console.error('Failed to load room types:', error)
    ElMessage.error(t('accommodation.roomPrice.messages.loadRoomTypesFailed'))
  }
}

const loadRoomGroups = async () => {
  try {
    const [groupsResponse, roomsResponse] = await Promise.all([getAllRoomGroups(), getRooms()])

    if (!groupsResponse.success || !Array.isArray(groupsResponse.data)) {
      roomGroupOptions.value = []
      roomGroupRoomTypeIdsMap.value = {}
      return
    }

    roomGroupOptions.value = groupsResponse.data.filter(
      (group): group is RoomGroupDTO & { id: number } => typeof group.id === 'number',
    )

    const roomTypeIdByRoomId = new Map<number, number>()
    if (roomsResponse.success && Array.isArray(roomsResponse.data)) {
      roomsResponse.data.forEach((room: RoomDTO) => {
        if (typeof room.id === 'number' && typeof room.roomType?.id === 'number') {
          roomTypeIdByRoomId.set(room.id, room.roomType.id)
        }
      })
    }

    const nextMap: Record<number, number[]> = {}
    const memberPairs = await Promise.all(
      roomGroupOptions.value.map(async (group) => {
        try {
          const membersResponse = await getGroupMembers(group.id)
          return {
            groupId: group.id,
            members:
              membersResponse.success && Array.isArray(membersResponse.data)
                ? membersResponse.data
                : ([] as RoomGroupMemberDTO[]),
          }
        } catch (error) {
          console.error(`Failed to load group members for ${group.id}:`, error)
          return {
            groupId: group.id,
            members: [] as RoomGroupMemberDTO[],
          }
        }
      }),
    )

    memberPairs.forEach(({ groupId, members }) => {
      const roomTypeIds = Array.from(
        new Set(
          members
            .map((member) => roomTypeIdByRoomId.get(member.roomId))
            .filter((roomTypeId): roomTypeId is number => typeof roomTypeId === 'number'),
        ),
      )
      nextMap[groupId] = roomTypeIds
    })

    roomGroupRoomTypeIdsMap.value = nextMap
  } catch (error) {
    console.error('Failed to load room groups:', error)
    roomGroupOptions.value = []
    roomGroupRoomTypeIdsMap.value = {}
  }
}

const loadPriceData = async () => {
  try {
    loading.value = true

    const startDate = selectedDate.value
    const endDateStr = getCalendarEndDateString(startDate)
    const response = await getRoomPriceManagementData(
      startDate,
      endDateStr,
      selectedRoomTypeId.value || undefined,
    )

    if (response.success && response.data) {
      priceData.value = response.data
    }
  } catch (error) {
    console.error('Failed to load room price data:', error)
    ElMessage.error(t('accommodation.roomPrice.messages.loadPriceDataFailed'))
  } finally {
    loading.value = false
  }
}

const openPriceEditDialog = (row: PriceTableRow, date: { dateStr: string; label: string }) => {
  if (row.isRoomHeader) return
  if (!isEditablePriceRow(row)) {
    ElMessage.warning(getRowUneditableReason(row))
    return
  }

  const cellData = row.dates[date.dateStr]
  const record = priceData.value.find(
    (priceRecord) =>
      priceRecord.roomTypeId === row.roomTypeId &&
      priceRecord.pricePlanId === row.pricePlanId &&
      priceRecord.priceDate === date.dateStr,
  )
  const roomType = roomTypes.value.find((item) => item.id === row.roomTypeId)

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
    availableRooms: cellData?.rooms || 0,
    minStay: record?.minStay != null ? String(record.minStay) : '',
    maxStay: record?.maxStay != null ? String(record.maxStay) : '',
    closeRoomStatus: 'off',
    currentCloseRoom: record?.closeRoom ?? false,
    currentCta: record?.cta ?? false,
    currentCtd: record?.ctd ?? false,
  }

  showPriceEditDialog.value = true
}

watch(
  () => editForm.value.settingType,
  (type) => {
    if (type === 'closeRoom') {
      editForm.value.closeRoomStatus = editForm.value.currentCloseRoom ? 'on' : 'off'
    } else if (type === 'cta') {
      editForm.value.closeRoomStatus = editForm.value.currentCta ? 'on' : 'off'
    } else if (type === 'ctd') {
      editForm.value.closeRoomStatus = editForm.value.currentCtd ? 'on' : 'off'
    }
  },
)

watch(
  () => editForm.value.closeRoomStatus,
  (value) => {
    const on = value === 'on'
    if (editForm.value.settingType === 'closeRoom') {
      editForm.value.currentCloseRoom = on
    } else if (editForm.value.settingType === 'cta') {
      editForm.value.currentCta = on
    } else if (editForm.value.settingType === 'ctd') {
      editForm.value.currentCtd = on
    }
  },
)

const addMoreSegments = () => {
  ElMessage.info(t('accommodation.roomPrice.addMoreSegmentsPending'))
}

const handleWeekdayChange = (values: number[]) => {
  if (values.includes(0) && !previousEditFormWeekdays.includes(0)) {
    editForm.value.weekdays = [0, 1, 2, 3, 4, 5, 6, 7]
  } else if (!values.includes(0) && previousEditFormWeekdays.includes(0)) {
    editForm.value.weekdays = []
  } else if (
    values.includes(0) &&
    previousEditFormWeekdays.includes(0) &&
    values.length < previousEditFormWeekdays.length
  ) {
    editForm.value.weekdays = values.filter((value) => value !== 0)
  } else if (
    !values.includes(0) &&
    values.length === 7 &&
    [1, 2, 3, 4, 5, 6, 7].every((value) => values.includes(value))
  ) {
    editForm.value.weekdays = [0, 1, 2, 3, 4, 5, 6, 7]
  }

  setTimeout(() => {
    previousEditFormWeekdays = [...editForm.value.weekdays]
  }, 0)
}

const invertEditWeekdays = () => {
  const explicitSelected = editForm.value.weekdays.filter((value) => value !== 0)
  const inverted = [1, 2, 3, 4, 5, 6, 7].filter((value) => !explicitSelected.includes(value))
  editForm.value.weekdays =
    inverted.length === 7 ? [0, 1, 2, 3, 4, 5, 6, 7] : inverted
  previousEditFormWeekdays = [...editForm.value.weekdays]
}

const closePriceEditDialog = () => {
  showPriceEditDialog.value = false
  previousEditFormWeekdays = []
}

const parseYmdToLocalDate = (ymd: string): Date => parseYmdAsUtcDate(ymd)

const getWeekdayValue = (ymd: string): number => {
  const day = getYmdWeekdayIndex(ymd)
  return day === 0 ? 7 : day
}

const hasMatchedWeekdayInRange = (
  startYmd: string,
  endYmd: string,
  weekdays: number[],
): boolean => {
  if (!weekdays.length || weekdays.includes(0)) return true

  const weekdaySet = new Set(weekdays)
  const start = parseYmdToLocalDate(startYmd)
  const end = parseYmdToLocalDate(endYmd)
  const cursor = new Date(start)

  while (cursor <= end) {
    const day = cursor.getUTCDay()
    const weekdayValue = day === 0 ? 7 : day
    if (weekdaySet.has(weekdayValue)) return true
    cursor.setUTCDate(cursor.getUTCDate() + 1)
  }

  return false
}

const savePriceEdit = async () => {
  try {
    saving.value = true
    const operator =
      userStore.currentUser?.nickname ||
      userStore.currentUser?.email ||
      t('accommodation.priceHistory.operatorOptions.system')
    if (!editForm.value.startDate || !editForm.value.endDate) {
      ElMessage.warning(t('accommodation.roomPrice.messages.incompleteDateRange'))
      return
    }

    if (!editForm.value.roomTypeId || !editForm.value.pricePlanId) {
      ElMessage.warning(t('accommodation.roomPrice.messages.noPricePlanBound'))
      return
    }

    const normalizedStartDate =
      editForm.value.startDate <= editForm.value.endDate
        ? editForm.value.startDate
        : editForm.value.endDate
    const normalizedEndDate =
      editForm.value.startDate <= editForm.value.endDate
        ? editForm.value.endDate
        : editForm.value.startDate
    const normalizedWeekdays =
      editForm.value.weekdays.length > 0 ? [...editForm.value.weekdays] : undefined
    const isSingleDayRange = normalizedStartDate === normalizedEndDate
    const isMultiDayRange = !isSingleDayRange
    const weekdaysContainAll = Boolean(normalizedWeekdays?.includes(0))
    const payloadWeekdays =
      isMultiDayRange && weekdaysContainAll ? undefined : normalizedWeekdays
    const hasExplicitWeekdaySelection = Boolean(
      normalizedWeekdays && normalizedWeekdays.length > 0,
    )
    const applyWeekdaysInRange = hasExplicitWeekdaySelection ? !isSingleDayRange : true

    if (
      applyWeekdaysInRange &&
      normalizedWeekdays &&
      normalizedWeekdays.length > 0 &&
      !hasMatchedWeekdayInRange(normalizedStartDate, normalizedEndDate, normalizedWeekdays)
    ) {
      ElMessage.warning(t('accommodation.roomPrice.messages.noMatchedWeekdays'))
      return
    }

    if (
      isSingleDayRange &&
      normalizedWeekdays &&
      normalizedWeekdays.length > 0 &&
      !normalizedWeekdays.includes(0)
    ) {
      const currentWeekday = getWeekdayValue(normalizedStartDate)
      if (!normalizedWeekdays.includes(currentWeekday)) {
        ElMessage.info(t('accommodation.roomPrice.messages.singleDayWeekTemplateApplied'))
      }
    }

    const requestData: any = {
      roomTypeId: editForm.value.roomTypeId,
      pricePlanId: editForm.value.pricePlanId,
      startDate: normalizedStartDate,
      endDate: normalizedEndDate,
      weekdays: payloadWeekdays,
      applyWeekdaysInRange,
    }

    if (editForm.value.settingType === 'price') {
      const normalizedPrice = Number(editForm.value.price)
      if (!Number.isFinite(normalizedPrice) || normalizedPrice < 0) {
        ElMessage.warning(t('accommodation.roomPrice.messages.invalidPrice'))
        return
      }
      requestData.price = normalizedPrice
      requestData.availableRooms = editForm.value.availableRooms ?? undefined
    } else if (editForm.value.settingType === 'minStay') {
      requestData.minStay = Number(editForm.value.minStay)
    } else if (editForm.value.settingType === 'maxStay') {
      requestData.maxStay = Number(editForm.value.maxStay)
    } else if (editForm.value.settingType === 'closeRoom') {
      requestData.closeRoom = editForm.value.closeRoomStatus === 'on'
    } else if (editForm.value.settingType === 'cta') {
      requestData.cta = editForm.value.closeRoomStatus === 'on'
    } else if (editForm.value.settingType === 'ctd') {
      requestData.ctd = editForm.value.closeRoomStatus === 'on'
    }

    const response = await updatePriceByPlan(requestData, operator)
    if (response.success) {
      const successMsg =
        editForm.value.settingType === 'price'
          ? t('accommodation.roomPrice.messages.saveSuccess.price')
          : editForm.value.settingType === 'minStay'
            ? t('accommodation.roomPrice.messages.saveSuccess.minStay')
            : editForm.value.settingType === 'maxStay'
              ? t('accommodation.roomPrice.messages.saveSuccess.maxStay')
              : editForm.value.settingType === 'closeRoom'
                ? t('accommodation.roomPrice.messages.saveSuccess.closeRoom')
                : editForm.value.settingType === 'cta'
                  ? t('accommodation.roomPrice.messages.saveSuccess.cta')
                  : editForm.value.settingType === 'ctd'
                    ? t('accommodation.roomPrice.messages.saveSuccess.ctd')
                    : t('accommodation.roomPrice.messages.saveSuccess.default')

      ElMessage.success(successMsg)
      closePriceEditDialog()
      await loadPriceData()
    } else {
      ElMessage.error(response.message || t('accommodation.roomPrice.messages.saveFailed'))
    }
  } catch (error) {
    console.error('Failed to save room price:', error)
    ElMessage.error(t('accommodation.roomPrice.messages.savePriceFailed'))
  } finally {
    saving.value = false
  }
}

const goToBulkUpdate = () => {
  router.push('/accommodation/room-price-bulk-update')
}

watch(selectedDate, () => {
  loadPriceData()
})

watch(selectedRoomTypeId, () => {
  loadPriceData()
})

onMounted(() => {
  loadRoomTypes()
  loadRoomGroups()
  loadPriceData()
})
</script>

<style scoped>
.price-management-page {
  min-height: 100%;
  padding: 4px 0 16px;
  background: #f5f5f5;
}

.price-management-surface {
  display: flex;
  min-height: calc(100vh - 154px);
  flex-direction: column;
  margin: 0 24px;
  background: #ffffff;
  border: 1px solid #e3e3e3;
  overflow: hidden;
}

.top-controls {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: center;
  column-gap: 20px;
  row-gap: 12px;
  padding: 18px 20px 16px;
  border-bottom: 1px solid #e2e2e2;
  background: #ffffff;
}

.toolbar-left {
  display: flex;
  align-items: center;
  min-width: 0;
}

.toolbar-right {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  flex-wrap: nowrap;
  gap: 12px 16px;
  min-width: 0;
  white-space: nowrap;
}

.date-navigation {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: nowrap;
  min-width: 0;
}

.date-nav-group {
  display: inline-flex;
  align-items: center;
}

.date-nav-group .nav-button + .nav-button {
  margin-left: -1px;
}

.date-nav-group .nav-button {
  position: relative;
}

.date-nav-group .nav-button:hover,
.date-nav-group .nav-button:focus-visible {
  z-index: 1;
}

.nav-button {
  --el-button-size: 34px;
  --el-component-size: 34px;
  --el-button-horizontal-padding: 8px;
  height: 36px;
  min-height: 36px;
  min-width: 56px;
  padding: 0 8px !important;
  border-radius: 4px;
  border-color: #dddddd;
  background: #ffffff;
  color: #3b3b3b;
}

.nav-button :deep(.el-icon) {
  font-size: 13px;
}

.nav-button:hover {
  border-color: #bfbfbf;
  color: #111111;
}

.nav-button--wide {
  min-width: 64px;
}

.date-nav-group:not(.date-nav-group--right) .nav-button:first-child {
  border-top-right-radius: 0;
  border-bottom-right-radius: 0;
}

.date-nav-group:not(.date-nav-group--right) .nav-button:last-child {
  border-top-left-radius: 0;
  border-bottom-left-radius: 0;
}

.date-nav-group--right .nav-button:first-child {
  border-top-right-radius: 0;
  border-bottom-right-radius: 0;
}

.date-nav-group--right .nav-button:last-child {
  border-top-left-radius: 0;
  border-bottom-left-radius: 0;
}

.nav-button--compact {
  --el-button-horizontal-padding: 6px;
  min-width: 44px;
  padding: 0 6px !important;
}

.nav-button :deep(span) {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  white-space: nowrap;
}

.date-picker {
  flex: 0 0 242px;
  width: 242px;
  --el-component-size: 36px;
  height: 36px;
}

:deep(.date-picker.el-date-editor),
:deep(.date-picker.el-date-editor.el-input),
:deep(.date-picker.el-input) {
  height: 36px;
  min-height: 36px;
  width: 242px;
  flex: 0 0 242px;
}

.date-picker :deep(.el-input__wrapper) {
  height: 36px;
  min-height: 36px;
  padding: 0 9px;
  border-radius: 4px;
  box-sizing: border-box;
  box-shadow: 0 0 0 1px #dddddd inset;
}

.date-picker :deep(.el-input__inner) {
  height: 34px;
  line-height: 34px;
}

.date-picker :deep(.el-input__wrapper:hover),
.date-picker :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1px #bdbdbd inset;
}

.filter-field {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.filter-label {
  flex: 0 0 auto;
  color: #5a5a5a;
  font-size: 14px;
  font-weight: 600;
  line-height: 1;
  white-space: nowrap;
}

.filter-select {
  width: 160px;
  flex: 0 0 160px;
}

.filter-select :deep(.el-select__wrapper) {
  min-height: 36px;
  border-radius: 4px;
  box-shadow: 0 0 0 1px #dddddd inset;
}

.filter-select :deep(.el-select__wrapper:hover),
.filter-select :deep(.el-select__wrapper.is-focused) {
  box-shadow: 0 0 0 1px #bdbdbd inset;
}

.bulk-update-button {
  height: 36px;
  padding: 0 18px;
  flex: 0 0 auto;
  border-radius: 4px;
  border-color: #2196f3;
  background: #2196f3;
  color: #ffffff;
  font-weight: 600;
}

.price-table-container {
  flex: 1 1 auto;
  min-height: 0;
  padding: 0;
  overflow: hidden;
  background: #ffffff;
}

.price-table {
  width: 100%;
  --el-table-border-color: #dfdfdf;
  --el-table-header-bg-color: #eeeeee;
  --el-table-row-hover-bg-color: #fafafa;
  color: #303030;
  font-size: 14px;
}

:deep(.price-table .el-table__inner-wrapper::before),
:deep(.price-table .el-table__border-left-patch) {
  display: none;
}

:deep(.price-table .el-table__header-wrapper) {
  z-index: 6;
}

:deep(.price-table .el-table__fixed-header-wrapper) {
  z-index: 8;
}

:deep(.price-table .el-table__header th.el-table__cell) {
  height: 66px;
  border-color: #d4d4d4;
  border-top: 2px solid #d4d4d4;
  border-bottom: 2px solid #d4d4d4;
  border-right: 2px solid #d4d4d4;
}

:deep(.price-table .el-table__header tr th.el-table__cell:first-child) {
  border-left: 2px solid #d4d4d4;
}

:deep(.price-table .el-table__fixed-left th.el-table__cell:last-child) {
  box-shadow: inset -2px 0 0 #d4d4d4;
}

:deep(.price-table .el-table__header th.el-table__cell .cell) {
  padding: 0 6px;
}

:deep(.price-table .el-table__body td > .cell) {
  padding: 0 !important;
}

:deep(.price-table .el-table__body td.el-table__cell) {
  height: 54px;
  border-color: #e5e5e5;
  background: #ffffff;
}

:deep(.price-table .el-table__fixed-left td.el-table__cell:last-child) {
  box-shadow: inset -1px 0 0 #e5e5e5;
}

:deep(.price-table tr.price-plan-row td.el-table__cell) {
  background: #f7f7f7 !important;
}

:deep(.price-table tr.price-plan-row td.el-table__cell > .cell) {
  background: #f7f7f7 !important;
}

:deep(.price-table tr.price-plan-row td.price-plan-label-td .plan-name-cell),
:deep(.price-table tr.price-plan-row td.price-plan-cell-td .price-cell) {
  background: #f7f7f7;
}

.room-name-cell,
.plan-name-cell {
  min-height: 54px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 8px 10px;
  text-align: center;
}

.room-type-name {
  font-size: 13px;
  font-weight: 600;
  line-height: 1.35;
  color: #2b2b2b;
  white-space: normal;
  word-break: break-word;
}

.plan-name-cell {
  flex-direction: column;
  gap: 5px;
}

.plan-name {
  color: #303030;
  font-size: 12px;
  font-weight: 600;
  line-height: 1.25;
}

.plan-uneditable-reason {
  max-width: 112px;
  color: #8a5a00;
  font-size: 11px;
  font-weight: 500;
  line-height: 1.25;
  word-break: break-word;
}

.channel-link {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 11px;
  font-weight: 500;
}

.date-header {
  text-align: center;
  line-height: 1.28;
}

.date-day {
  font-size: 12px;
  font-weight: 600;
  color: #252525;
}

.date-weekday {
  margin-top: 2px;
  font-size: 11px;
  color: #545454;
}

.room-count-cell,
.price-cell {
  width: 100%;
  min-height: 54px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 8px 6px;
}

.price-cell {
  border: none;
  background: transparent;
  cursor: pointer;
  transition: background-color 0.15s ease;
}

.price-cell.uneditable-cell {
  cursor: not-allowed;
}

:deep(.price-table tr.price-plan-row td.price-plan-cell-td .price-cell:hover) {
  background-color: #f1f1f1;
}

:deep(.price-table tr.price-plan-row td.uneditable-td),
:deep(.price-table tr.price-plan-row td.uneditable-td > .cell),
:deep(.price-table tr.price-plan-row td.uneditable-td .price-cell) {
  background-color: #fff8e7 !important;
}

:deep(.price-table tr.price-plan-row td.uneditable-td .price-cell:hover) {
  background-color: #fff2cf !important;
}

:deep(.price-table tr.price-plan-row:hover > td.el-table__cell) {
  background: #f7f7f7 !important;
}

:deep(.price-table tr.price-plan-row:hover > td.el-table__cell > .cell) {
  background: #f7f7f7 !important;
}

:deep(.price-table tr.price-plan-row:hover td.price-plan-label-td .plan-name-cell) {
  background: #f7f7f7;
}

:deep(.price-table td.sold-out-td),
:deep(.price-table td.sold-out-td > .cell),
:deep(.price-table td.sold-out-td .price-cell) {
  background-color: #ffe9e9 !important;
}

:deep(.price-table tr.price-plan-row td.sold-out-td:hover),
:deep(.price-table tr.price-plan-row td.sold-out-td:hover > .cell),
:deep(.price-table tr.price-plan-row td.sold-out-td .price-cell:hover) {
  background-color: #ffdede !important;
}

.sold-out-cell {
  background-color: #ffe9e9 !important;
}

.price-cell.sold-out-cell:hover {
  background-color: #ffdede !important;
}

.price-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}

.price-value {
  font-size: 13px;
  font-weight: 600;
  line-height: 1.1;
  color: #333333;
}

.rooms-count {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 11px;
  font-weight: 500;
  line-height: 1;
  color: #7fca53;
}

.rooms-count .el-icon {
  font-size: 12px;
}

.room-count-value {
  font-size: 13px;
  font-weight: 600;
  color: #303030;
}

.sold-out-cell .price-value,
.sold-out-cell .rooms-count {
  color: #de5f5f;
}

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
  flex-shrink: 0;
  color: #606266;
  font-size: 14px;
}

.weekday-group {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.weekday-selector {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
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
  font-weight: 400;
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

@media (max-width: 1200px) {
  .price-management-surface {
    margin: 0 20px;
  }

  .top-controls {
    grid-template-columns: 1fr;
    gap: 12px;
  }

  .toolbar-left,
  .toolbar-right {
    width: 100%;
    flex-basis: auto;
  }

  .toolbar-right {
    justify-content: flex-start;
    flex-wrap: wrap;
    white-space: normal;
  }

  .date-navigation {
    justify-content: flex-start;
    flex-wrap: wrap;
  }
}

@media (max-width: 768px) {
  .price-management-page {
    padding-bottom: 20px;
  }

  .price-management-surface {
    margin: 0 12px;
  }

  .date-navigation,
  .toolbar-right {
    align-items: stretch;
  }

  .date-nav-group {
    width: 100%;
  }

  .date-nav-group .nav-button {
    flex: 1 1 0;
  }

  .date-picker {
    width: 100%;
  }

  .filter-field {
    width: 100%;
    flex-direction: column;
    align-items: flex-start;
    min-width: 0;
  }

  .filter-select {
    width: 100%;
  }
}
</style>
