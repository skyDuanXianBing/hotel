<template>
  <div class="room-table">
    <div class="room-table-header">
      <div class="room-table-header-spacer" aria-hidden="true"></div>

      <div class="room-table-tabs" aria-label="Room table tabs">
        <button
          v-for="tab in tabs"
          :key="tab.name"
          type="button"
          class="room-table-tab"
          :class="{
            'is-active': activeTab === tab.name,
            'is-disabled': tab.disabled,
          }"
          :disabled="tab.disabled"
          @click="switchTab(tab.name)"
        >
          {{ t(tab.labelKey) }}
        </button>
      </div>

      <!-- 顶部工具栏 -->
      <div v-if="activeTab === 'daily'" class="toolbar">
        <el-date-picker
          v-model="selectedDate"
          type="date"
          :placeholder="
            activeTab === 'daily'
              ? t('accommodation.roomTable.selectDate')
              : t('accommodation.roomTable.selectStartDate')
          "
          format="YYYY-MM-DD"
          value-format="YYYY-MM-DD"
          @change="onDateChange"
          class="date-picker"
        />
        <el-button type="primary" :icon="Download" @click="exportData">
          {{ t('accommodation.common.exportDetails') }}
        </el-button>
      </div>
      <div v-else class="room-table-header-spacer room-table-header-spacer--right" aria-hidden="true"></div>
    </div>

    <!-- 单日房情统计表格 -->
    <div v-if="activeTab === 'daily'" class="table-container" v-loading="loading">
      <el-table
        :data="tableData"
        border
        class="room-statistics-table"
        :header-cell-style="dailyHeaderCellStyle"
        :row-class-name="getDailyRowClassName"
      >
        <el-table-column
          prop="roomTypeName"
          :label="formatHeaderLabel(t('accommodation.roomTable.columns.roomTypeName'))"
          min-width="120"
          align="center"
        >
          <template #header>
            <DailyHeaderLabel :label="t('accommodation.roomTable.columns.roomTypeName')" />
          </template>
        </el-table-column>
        <el-table-column
          prop="totalRooms"
          :label="formatHeaderLabel(t('accommodation.roomTable.columns.totalRooms'))"
          width="88"
          align="center"
        >
          <template #header>
            <DailyHeaderLabel :label="t('accommodation.roomTable.columns.totalRooms')" />
          </template>
        </el-table-column>

        <el-table-column
          :label="formatHeaderLabel(t('accommodation.roomTable.columns.availableForSale'))"
          width="84"
          align="center"
        >
          <template #header>
            <DailyHeaderLabel :label="t('accommodation.roomTable.columns.availableForSale')" />
            <el-tooltip
              :content="t('accommodation.roomTable.tooltips.availableForSale')"
              placement="top"
            >
              <el-icon class="info-icon"><InfoFilled /></el-icon>
            </el-tooltip>
          </template>
          <template #default="{ row }">
            {{ row.availableForSale }}
          </template>
        </el-table-column>

        <el-table-column
          :label="formatHeaderLabel(t('accommodation.roomTable.columns.availableRooms'))"
          width="84"
          align="center"
        >
          <template #header>
            <DailyHeaderLabel :label="t('accommodation.roomTable.columns.availableRooms')" />
            <el-tooltip
              :content="t('accommodation.roomTable.tooltips.availableRooms')"
              placement="top"
            >
              <el-icon class="info-icon"><InfoFilled /></el-icon>
            </el-tooltip>
          </template>
          <template #default="{ row }">
            {{ row.availableRooms }}
          </template>
        </el-table-column>

        <el-table-column
          :label="formatHeaderLabel(t('accommodation.roomTable.columns.occupiedRooms'))"
          width="82"
          align="center"
        >
          <template #header>
            <DailyHeaderLabel :label="t('accommodation.roomTable.columns.occupiedRooms')" />
            <el-tooltip
              :content="t('accommodation.roomTable.tooltips.occupiedRooms')"
              placement="top"
            >
              <el-icon class="info-icon"><InfoFilled /></el-icon>
            </el-tooltip>
          </template>
          <template #default="{ row }">
            {{ row.occupiedRooms }}
          </template>
        </el-table-column>

        <el-table-column
          :label="formatHeaderLabel(t('accommodation.roomTable.columns.occupiedWithoutDeparture'))"
          min-width="128"
          align="center"
        >
          <template #header>
            <DailyHeaderLabel
              :label="t('accommodation.roomTable.columns.occupiedWithoutDeparture')"
            />
            <el-tooltip
              :content="t('accommodation.roomTable.tooltips.occupiedWithoutDeparture')"
              placement="top"
            >
              <el-icon class="info-icon"><InfoFilled /></el-icon>
            </el-tooltip>
          </template>
          <template #default="{ row }">
            {{ row.occupiedWithoutDeparture }}
          </template>
        </el-table-column>

        <el-table-column
          :label="formatHeaderLabel(t('accommodation.roomTable.columns.scheduledDeparture'))"
          width="90"
          align="center"
        >
          <template #header>
            <DailyHeaderLabel :label="t('accommodation.roomTable.columns.scheduledDeparture')" />
            <el-tooltip
              :content="t('accommodation.roomTable.tooltips.scheduledDeparture')"
              placement="top"
            >
              <el-icon class="info-icon"><InfoFilled /></el-icon>
            </el-tooltip>
          </template>
          <template #default="{ row }">
            {{ row.scheduledDeparture }}
          </template>
        </el-table-column>

        <el-table-column
          prop="scheduledArrival"
          :label="formatHeaderLabel(t('accommodation.roomTable.columns.scheduledArrival'))"
          width="82"
          align="center"
        >
          <template #header>
            <DailyHeaderLabel :label="t('accommodation.roomTable.columns.scheduledArrival')" />
          </template>
        </el-table-column>
        <el-table-column
          prop="reservedRooms"
          :label="formatHeaderLabel(t('accommodation.roomTable.columns.reservedRooms'))"
          min-width="118"
          align="center"
        >
          <template #header>
            <DailyHeaderLabel :label="t('accommodation.roomTable.columns.reservedRooms')" />
          </template>
        </el-table-column>
        <el-table-column
          prop="maintenanceRooms"
          :label="formatHeaderLabel(t('accommodation.roomTable.columns.maintenanceRooms'))"
          min-width="126"
          align="center"
        >
          <template #header>
            <DailyHeaderLabel :label="t('accommodation.roomTable.columns.maintenanceRooms')" />
          </template>
        </el-table-column>
        <el-table-column
          prop="outOfOrderRooms"
          :label="formatHeaderLabel(t('accommodation.roomTable.columns.outOfOrderRooms'))"
          min-width="122"
          align="center"
        >
          <template #header>
            <DailyHeaderLabel :label="t('accommodation.roomTable.columns.outOfOrderRooms')" />
          </template>
        </el-table-column>
        <el-table-column
          prop="linkedClosedRooms"
          :label="formatHeaderLabel(t('accommodation.roomTable.columns.linkedClosedRooms'))"
          min-width="120"
          align="center"
        >
          <template #header>
            <DailyHeaderLabel :label="t('accommodation.roomTable.columns.linkedClosedRooms')" />
          </template>
        </el-table-column>
        <el-table-column
          prop="cleanRooms"
          :label="formatHeaderLabel(t('accommodation.roomTable.columns.cleanRooms'))"
          width="76"
          align="center"
        >
          <template #header>
            <DailyHeaderLabel :label="t('accommodation.roomTable.columns.cleanRooms')" />
          </template>
        </el-table-column>
        <el-table-column
          prop="dirtyRooms"
          :label="formatHeaderLabel(t('accommodation.roomTable.columns.dirtyRooms'))"
          width="76"
          align="center"
        >
          <template #header>
            <DailyHeaderLabel :label="t('accommodation.roomTable.columns.dirtyRooms')" />
          </template>
        </el-table-column>

        <el-table-column
          :label="formatHeaderLabel(t('accommodation.roomTable.columns.expectedOccupancyRate'))"
          min-width="124"
          align="center"
        >
          <template #header>
            <DailyHeaderLabel :label="t('accommodation.roomTable.columns.expectedOccupancyRate')" />
            <el-tooltip
              :content="t('accommodation.roomTable.tooltips.expectedOccupancyRate')"
              placement="top"
            >
              <el-icon class="info-icon"><InfoFilled /></el-icon>
            </el-tooltip>
          </template>
          <template #default="{ row }"> {{ row.expectedOccupancyRate }}% </template>
        </el-table-column>

        <el-table-column
          :label="formatHeaderLabel(t('accommodation.roomTable.columns.dailyCancelledRooms'))"
          min-width="118"
          align="center"
        >
          <template #header>
            <DailyHeaderLabel :label="t('accommodation.roomTable.columns.dailyCancelledRooms')" />
            <el-tooltip
              :content="t('accommodation.roomTable.tooltips.dailyCancelledRooms')"
              placement="top"
            >
              <el-icon class="info-icon"><InfoFilled /></el-icon>
            </el-tooltip>
          </template>
          <template #default="{ row }">
            {{ row.dailyCancelledRooms }}
          </template>
        </el-table-column>
      </el-table>
    </div>

    <div v-else-if="activeTab === 'monthly'" class="monthly-room-container" v-loading="loading">
      <div class="monthly-toolbar">
        <label class="monthly-filter-field">
          <span class="monthly-filter-label">{{ monthlyText.roomTypePlaceholder }}</span>
          <el-select
            v-model="monthlyRoomTypeFilter"
            class="monthly-filter monthly-filter--room-type"
            :placeholder="monthlyText.all"
          >
            <el-option :label="monthlyText.all" value="" />
            <el-option
              v-for="option in monthlyRoomTypeOptions"
              :key="option"
              :label="option"
              :value="option"
            />
          </el-select>
        </label>

        <label class="monthly-filter-field">
          <span class="monthly-filter-label">{{ monthlyText.statusPlaceholder }}</span>
          <el-select
            v-model="monthlyStatusFilter"
            class="monthly-filter monthly-filter--status"
            :placeholder="monthlyText.statusPlaceholder"
          >
            <el-option
              v-for="option in monthlyStatusOptions"
              :key="option.value"
              :label="option.label"
              :value="option.value"
            />
          </el-select>
        </label>

        <div class="monthly-month-switcher" :aria-label="monthlyText.monthSwitcher">
          <button type="button" class="future-nav-button" @click="previousMonth">
            <el-icon><ArrowLeft /></el-icon>
          </button>
          <button type="button" class="monthly-month-button" @click="goToCurrentMonth">
            {{ selectedMonthLabel }}
          </button>
          <button
            type="button"
            class="future-nav-button future-nav-button--next"
            @click="nextMonth"
          >
            <el-icon><ArrowRight /></el-icon>
          </button>
          <el-button class="monthly-current-button" @click="goToCurrentMonth">
            {{ monthlyText.currentMonth }}
          </el-button>
        </div>

        <el-input
          v-model="monthlySearchKeyword"
          class="monthly-search"
          clearable
          :placeholder="monthlyText.searchPlaceholder"
        />

        <el-button type="primary" :icon="Download" @click="exportData">
          {{ t('accommodation.common.exportDetails') }}
        </el-button>
      </div>

      <div v-if="filteredMonthlyRoomCards.length" class="monthly-room-grid">
        <article
          v-for="room in filteredMonthlyRoomCards"
          :key="room.roomId"
          class="monthly-room-card"
        >
          <header class="monthly-room-card-header">
            <div class="monthly-room-type">{{ room.roomType }}</div>
            <div class="monthly-room-number">{{ room.roomNumber }}</div>
          </header>

          <div class="monthly-weekdays">
            <span v-for="weekday in monthlyWeekdayLabels" :key="weekday">{{ weekday }}</span>
          </div>

          <div class="monthly-days">
            <span
              v-for="(cell, index) in room.cells"
              :key="cell ? cell.date : `blank-${room.roomId}-${index}`"
              class="monthly-day"
              :class="
                cell ? [`monthly-day--${cell.kind}`, `monthly-day--${cell.segment}`] : 'monthly-day--blank'
              "
              :title="cell?.tooltip || ''"
            >
              {{ cell?.dayLabel || '' }}
            </span>
          </div>
        </article>
      </div>

      <el-empty v-else class="monthly-empty" :description="monthlyText.empty" />
    </div>

    <!-- 远期房情表 -->
    <div v-else-if="activeTab === 'future'" class="future-room-container">
      <!-- 日期导航栏 -->
      <div class="future-top-bar">
        <div class="date-navigation">
          <button type="button" class="future-nav-button" @click="previousPeriod">
            <el-icon><ArrowLeft /></el-icon>
          </button>
          <div class="future-date-picker-shell">
            <el-date-picker
              v-model="selectedDate"
              type="date"
              :placeholder="t('accommodation.roomTable.selectStartDate')"
              format="YYYY/MM/DD"
              value-format="YYYY-MM-DD"
              @change="onFutureDateChange"
              class="future-date-picker future-date-picker--primary"
            />
            <span v-if="futureDateDisplayText" class="future-date-picker-display">
              {{ futureDateDisplayText }}
            </span>
          </div>
          <button
            type="button"
            class="future-nav-button future-nav-button--next"
            @click="nextPeriod"
          >
            <el-icon><ArrowRight /></el-icon>
          </button>
        </div>

        <div class="toolbar toolbar--future">
          <el-date-picker
            v-model="selectedDate"
            type="date"
            :placeholder="t('accommodation.roomTable.selectStartDate')"
            format="YYYY-MM-DD"
            value-format="YYYY-MM-DD"
            @change="onFutureDateChange"
            class="date-picker"
          />
          <el-button type="primary" :icon="Download" @click="exportData">
            {{ t('accommodation.common.exportDetails') }}
          </el-button>
        </div>
      </div>

      <!-- 远期房情表格 -->
      <div class="future-table-container" v-loading="loading">
        <div class="table-scroll-wrapper" v-if="futureRoomTableData">
          <table class="future-table-main">
            <thead>
              <!-- 第一行：日期标题 -->
              <tr class="date-header-row">
                <th rowspan="2" class="fixed-header room-type-col">
                  {{ t('accommodation.roomTable.future.roomType') }}
                </th>
                <th rowspan="2" class="fixed-header total-rooms-col">
                  {{ t('accommodation.roomTable.future.totalRooms') }}
                </th>
                <th
                  v-for="date in futureRoomTableData.total.dates"
                  :key="date.date"
                  colspan="3"
                  class="date-header"
                >
                  {{ formatDateHeader(date.date, date.dayOfWeek) }}
                </th>
              </tr>
              <!-- 第二行：子列标题 -->
              <tr class="sub-header-row">
                <template v-for="date in futureRoomTableData.total.dates" :key="`sub-${date.date}`">
                  <th class="sub-header">{{ t('accommodation.roomTable.future.available') }}</th>
                  <th class="sub-header">{{ t('accommodation.roomTable.future.occupied') }}</th>
                  <th class="sub-header">{{ t('accommodation.roomTable.future.unavailable') }}</th>
                </template>
              </tr>
            </thead>
            <tbody>
              <!-- 房型数据行 -->
              <tr
                v-for="roomType in futureRoomTableData.roomTypes"
                :key="roomType.roomTypeName"
                class="room-type-row"
              >
                <td class="fixed-cell room-type-cell">{{ roomType.roomTypeName }}</td>
                <td class="fixed-cell total-rooms-cell">{{ roomType.totalRooms }}</td>
                <template
                  v-for="date in roomType.dates"
                  :key="`${roomType.roomTypeName}-${date.date}`"
                >
                  <td class="data-cell" :class="{ available: date.available > 0 }">
                    {{ date.available }}
                  </td>
                  <td class="data-cell" :class="{ occupied: date.occupied > 0 }">
                    {{ date.occupied }}
                  </td>
                  <td class="data-cell">{{ date.unavailable }}</td>
                </template>
              </tr>
              <!-- 占总房数的比例行 -->
              <tr class="percentage-row">
                <td class="fixed-cell room-type-cell">
                  {{ t('accommodation.roomTable.future.percentageOfTotalRooms') }}
                </td>
                <td class="fixed-cell total-rooms-cell">-</td>
                <template
                  v-for="date in futureRoomTableData.total.dates"
                  :key="`percentage-${date.date}`"
                >
                  <td class="data-cell percentage" :class="{ available: date.availableRate > 0 }">
                    {{ date.availableRate }}%
                  </td>
                  <td class="data-cell percentage" :class="{ occupied: date.occupiedRate > 0 }">
                    {{ date.occupiedRate }}%
                  </td>
                  <td class="data-cell percentage">{{ date.unavailableRate }}%</td>
                </template>
              </tr>
              <!-- 合计行 -->
              <tr class="total-row">
                <td class="fixed-cell room-type-cell">
                  {{ t('accommodation.roomTable.future.total') }}
                </td>
                <td class="fixed-cell total-rooms-cell">
                  {{ futureRoomTableData.total.totalRooms }}
                </td>
                <template
                  v-for="date in futureRoomTableData.total.dates"
                  :key="`total-${date.date}`"
                >
                  <td class="data-cell" :class="{ available: date.available > 0 }">
                    {{ date.available }}
                  </td>
                  <td class="data-cell" :class="{ occupied: date.occupied > 0 }">
                    {{ date.occupied }}
                  </td>
                  <td class="data-cell">{{ date.unavailable }}</td>
                </template>
              </tr>
            </tbody>
          </table>

          <!-- 底部统计区域 -->
          <table class="future-statistics-table">
            <tbody>
              <tr class="future-stats-date-row">
                <th class="future-stats-label-cell"></th>
                <td
                  v-for="statistic in futureRoomTableData.statistics"
                  :key="statistic.date"
                  class="future-stats-date-cell"
                >
                  {{ formatStatisticDateHeader(statistic.date) }}
                </td>
              </tr>
              <tr v-for="row in futureStatisticRows" :key="row.key" class="future-stats-row">
                <th class="future-stats-label-cell">
                  <span class="stat-label">
                    {{ row.label }}
                    <el-tooltip :content="row.tooltip" placement="top">
                      <el-icon class="info-icon"><InfoFilled /></el-icon>
                    </el-tooltip>
                  </span>
                </th>
                <td
                  v-for="statistic in futureRoomTableData.statistics"
                  :key="`${row.key}-${statistic.date}`"
                  class="future-stat-value"
                >
                  {{ row.getValue(statistic) }}
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed, defineComponent, h } from 'vue'
import { ElMessage } from 'element-plus'
import { Download, InfoFilled, ArrowLeft, ArrowRight } from '@element-plus/icons-vue'
import { useI18n } from 'vue-i18n'
import { request } from '@/utils/request'
import {
  getMonthlyRoomTableData,
  type MonthlyDailyStatusDTO,
  type RoomTableMonthlyResponse,
} from '@/api/roomTable'
import type {
  RoomTableData,
  RoomStatistics,
  ApiResponse,
  FutureRoomTableData,
  FutureRoomStatistics,
} from '@/types/room'
import { getFutureRoomTableData } from '@/api/futureRoomTable'
import { useAccommodationI18n } from '@/composables/useAccommodationI18n'
import {
  addCalendarMonthsToYmd,
  addDaysToYmd,
  formatYmdMonthDay,
  getYmdMonthStart,
  getYmdRange,
  parseYmdAsUtcDate,
  getStoreTodayYmd,
  getYmdWeekdayIndex,
} from '@/utils/storeDateTime'

type RoomTableTab = 'daily' | 'future' | 'monthly'
type MonthlyStatusFilter = 'all' | 'full' | 'available' | 'availableMany'
type MonthlyDayKind = 'full' | 'available' | 'available-many'
type MonthlyDaySegment = 'single' | 'start' | 'middle' | 'end'

interface MonthlyRoomDay {
  date: string
  dayLabel: string
  kind: MonthlyDayKind
  segment: MonthlyDaySegment
  tooltip: string
}

interface MonthlyRoomCard {
  roomId: number
  roomNumber: string
  roomType: string
  cells: Array<MonthlyRoomDay | null>
  hasFull: boolean
  hasAvailable: boolean
  hasAvailableMany: boolean
}

// 响应式数据
const { t, locale } = useI18n()
const { weekdayFullMap } = useAccommodationI18n()
const loading = ref(false)
const activeTab = ref<RoomTableTab>('daily')
const selectedDate = ref<string>(getStoreTodayYmd())
const selectedMonthDate = ref<string>(getYmdMonthStart(getStoreTodayYmd()))
const roomTableData = ref<RoomTableData | null>(null)
const futureRoomTableData = ref<FutureRoomTableData | null>(null)
const monthlyRoomTableData = ref<RoomTableMonthlyResponse | null>(null)
const monthlyRoomTypeFilter = ref('')
const monthlyStatusFilter = ref<MonthlyStatusFilter>('all')
const monthlySearchKeyword = ref('')

const tabs: Array<{
  name: RoomTableTab
  labelKey: string
  disabled?: boolean
}> = [
  { name: 'daily', labelKey: 'accommodation.roomTable.dailyTab' },
  { name: 'monthly', labelKey: 'accommodation.roomTable.monthlyTab' },
  { name: 'future', labelKey: 'accommodation.roomTable.futureTab' },
]

const dailyHeaderCellStyle = {
  background: '#eeeeee',
  color: '#252525',
  fontWeight: '600',
}

const splitHeaderLabel = (label: string) => {
  const match = label.match(/^(.*?)([（(].*)$/)

  if (!match) {
    return {
      main: label,
      note: '',
    }
  }

  return {
    main: match[1].trimEnd(),
    note: match[2].trim(),
  }
}

const formatHeaderLabel = (label: string) => {
  const { main, note } = splitHeaderLabel(label)
  return note ? `${main}\n${note}` : main
}

const DailyHeaderLabel = defineComponent({
  name: 'DailyHeaderLabel',
  props: {
    label: {
      type: String,
      required: true,
    },
  },
  setup(props) {
    return () => {
      const { main, note } = splitHeaderLabel(props.label)
      const children = [h('span', { class: 'daily-header-main' }, main)]

      if (note) {
        children.push(h('span', { class: 'daily-header-note' }, note))
      }

      return h('span', { class: 'daily-header-text' }, children)
    }
  },
})

// 计算属性 - 表格数据（包含合计行）
const tableData = computed(() => {
  if (!roomTableData.value) return []

  const data = [...roomTableData.value.statistics]
  // 添加合计行
  data.push({
    ...roomTableData.value.total,
    roomTypeName: t('accommodation.roomTable.future.total'),
  })

  return data
})

const monthlyText = computed(() => ({
  roomTypePlaceholder: t('accommodation.common.roomType'),
  statusPlaceholder: t('accommodation.common.status'),
  monthSwitcher: t('accommodation.roomTable.monthly.monthSwitcher'),
  currentMonth: t('accommodation.roomTable.monthly.currentMonth'),
  searchPlaceholder: t('accommodation.roomTable.monthly.searchPlaceholder'),
  all: t('accommodation.common.all'),
  full: t('accommodation.roomTable.monthly.full'),
  available: t('accommodation.roomTable.monthly.available'),
  availableMany: t('accommodation.roomTable.monthly.availableMany'),
  empty: t('accommodation.roomTable.monthly.empty'),
}))

const monthlyStatusOptions = computed<Array<{ label: string; value: MonthlyStatusFilter }>>(() => [
  { label: monthlyText.value.all, value: 'all' },
  { label: monthlyText.value.full, value: 'full' },
  { label: monthlyText.value.available, value: 'available' },
  { label: monthlyText.value.availableMany, value: 'availableMany' },
])

const selectedMonthStart = computed(() => getYmdMonthStart(selectedMonthDate.value))

const selectedMonthEnd = computed(() => {
  const nextMonthStart = getYmdMonthStart(addCalendarMonthsToYmd(selectedMonthStart.value, 1))
  return addDaysToYmd(nextMonthStart, -1)
})

const selectedMonthDates = computed(() => getYmdRange(selectedMonthStart.value, selectedMonthEnd.value))

const roomTableDateLocale = computed(() => {
  switch (locale.value) {
    case 'en':
      return 'en-US'
    case 'zh-TW':
      return 'zh-TW'
    case 'ja':
      return 'ja-JP'
    default:
      return 'zh-CN'
  }
})

const selectedMonthLabel = computed(() => {
  const date = parseYmdAsUtcDate(selectedMonthStart.value)
  return new Intl.DateTimeFormat(roomTableDateLocale.value, {
    year: 'numeric',
    month: 'long',
    timeZone: 'UTC',
  }).format(date)
})

const monthlyWeekdayLabels = computed(() =>
  [0, 1, 2, 3, 4, 5, 6].map((day) => t(`accommodation.roomTable.monthly.weekdays.${day}`)),
)

const monthlyRoomTypeOptions = computed(() => {
  const roomTypes = new Set<string>()
  monthlyRoomTableData.value?.rooms?.forEach((room) => {
    if (room.roomType) {
      roomTypes.add(room.roomType)
    }
  })
  return Array.from(roomTypes)
})

const getMonthlyDayKind = (dailyStatus: MonthlyDailyStatusDTO): MonthlyDayKind => {
  if (dailyStatus.displayStatus === 'AVAILABLE_MANY') {
    return 'available-many'
  }

  if (dailyStatus.displayStatus === 'AVAILABLE') {
    return 'available'
  }

  return 'full'
}

const getMonthlyDayTooltip = (
  dailyStatus: MonthlyDailyStatusDTO,
  kind: MonthlyDayKind,
) => {
  if (dailyStatus.closed) {
    return dailyStatus.closeRemark
      ? `${monthlyText.value.full}: ${dailyStatus.closeRemark}`
      : monthlyText.value.full
  }

  if (dailyStatus.reservation) {
    return `${monthlyText.value.full}: ${dailyStatus.reservation.guestName || ''}`.trim()
  }

  if (kind === 'available-many') {
    const availableCount = dailyStatus.roomTypeAvailableRooms || 0
    return `${monthlyText.value.availableMany}: ${availableCount}`
  }

  return kind === 'available' ? monthlyText.value.available : monthlyText.value.full
}

const monthlyRoomCards = computed<MonthlyRoomCard[]>(() => {
  const firstWeekday = getYmdWeekdayIndex(selectedMonthStart.value)
  const blankCells = Array.from({ length: firstWeekday }, () => null)

  return (monthlyRoomTableData.value?.rooms || []).map((room) => {
    let hasFull = false
    let hasAvailable = false
    let hasAvailableMany = false

    const dailyStatusByDate = new Map(room.dailyStatus.map((dailyStatus) => [dailyStatus.date, dailyStatus]))
    const days = selectedMonthDates.value.map<MonthlyRoomDay>((date) => {
      const dailyStatus = dailyStatusByDate.get(date)
      if (!dailyStatus) {
        hasFull = true
        return {
          date,
          dayLabel: String(parseYmdAsUtcDate(date).getUTCDate()),
          kind: 'full' as MonthlyDayKind,
          segment: 'single',
          tooltip: monthlyText.value.full,
        }
      }

      const kind = getMonthlyDayKind(dailyStatus)
      hasFull = hasFull || kind === 'full'
      hasAvailable = hasAvailable || kind === 'available'
      hasAvailableMany = hasAvailableMany || kind === 'available-many'

      return {
        date,
        dayLabel: String(parseYmdAsUtcDate(date).getUTCDate()),
        kind,
        segment: 'single',
        tooltip: getMonthlyDayTooltip(dailyStatus, kind),
      }
    })
    const segmentedDays = days.map<MonthlyRoomDay>((day, index) => {
      const weekday = getYmdWeekdayIndex(day.date)
      const previous = index > 0 ? days[index - 1] : null
      const next = index < days.length - 1 ? days[index + 1] : null
      const connectsPrevious = Boolean(previous && previous.kind === day.kind && weekday !== 0)
      const connectsNext = Boolean(next && next.kind === day.kind && weekday !== 6)

      let segment: MonthlyDaySegment = 'single'
      if (connectsPrevious && connectsNext) {
        segment = 'middle'
      } else if (connectsPrevious) {
        segment = 'end'
      } else if (connectsNext) {
        segment = 'start'
      }

      return {
        ...day,
        segment,
      }
    })

    return {
      roomId: room.roomId,
      roomNumber: room.roomNumber,
      roomType: room.roomType,
      cells: [...blankCells, ...segmentedDays],
      hasFull,
      hasAvailable,
      hasAvailableMany,
    }
  })
})

const filteredMonthlyRoomCards = computed(() => {
  const keyword = monthlySearchKeyword.value.trim().toLowerCase()

  return monthlyRoomCards.value.filter((room) => {
    if (monthlyRoomTypeFilter.value && room.roomType !== monthlyRoomTypeFilter.value) {
      return false
    }

    if (keyword) {
      const haystack = `${room.roomType} ${room.roomNumber}`.toLowerCase()
      if (!haystack.includes(keyword)) {
        return false
      }
    }

    if (monthlyStatusFilter.value === 'full') {
      return room.hasFull
    }

    if (monthlyStatusFilter.value === 'available') {
      return room.hasAvailable
    }

    if (monthlyStatusFilter.value === 'availableMany') {
      return room.hasAvailableMany
    }

    return true
  })
})

const fetchMonthlyRoomTableData = async () => {
  try {
    loading.value = true

    const response = await getMonthlyRoomTableData(selectedMonthStart.value, selectedMonthEnd.value)

    if (response.success) {
      monthlyRoomTableData.value = response.data
    } else {
      monthlyRoomTableData.value = null
      ElMessage.error(response.message || t('accommodation.roomTable.messages.loadMonthlyFailed'))
    }
  } catch (error) {
    console.error('获取月度房情表数据失败:', error)
    monthlyRoomTableData.value = null
    ElMessage.error(t('accommodation.roomTable.messages.loadMonthlyFailed'))
  } finally {
    loading.value = false
  }
}

// 获取房情表数据
const fetchRoomTableData = async (date: string) => {
  try {
    loading.value = true

    const response: ApiResponse<RoomTableData> = await request.get(
      `/room-table/statistics?date=${date}`,
    )

    if (response.success) {
      roomTableData.value = response.data
    } else {
      ElMessage.error(response.message || t('accommodation.roomTable.messages.loadDailyFailed'))
    }
  } catch (error) {
    console.error('获取房情表数据失败:', error)
    ElMessage.error(t('accommodation.roomTable.messages.loadDailyFailed'))

    // 添加模拟数据用于展示
    roomTableData.value = {
      date: date,
      statistics: [
        {
          roomTypeName: t('stage6.components.addRoomDialog.demoRoomTypes.queen'),
          totalRooms: 1,
          availableForSale: 1,
          availableRooms: 1,
          occupiedRooms: 0,
          occupiedWithoutDeparture: 0,
          scheduledDeparture: 0,
          scheduledArrival: 0,
          reservedRooms: 0,
          maintenanceRooms: 0,
          outOfOrderRooms: 0,
          linkedClosedRooms: 0,
          cleanRooms: 0,
          dirtyRooms: 1,
          expectedOccupancyRate: 0,
          dailyCancelledRooms: 0,
        },
      ],
      total: {
        roomTypeName: t('accommodation.roomTable.future.total'),
        totalRooms: 1,
        availableForSale: 1,
        availableRooms: 1,
        occupiedRooms: 0,
        occupiedWithoutDeparture: 0,
        scheduledDeparture: 0,
        scheduledArrival: 0,
        reservedRooms: 0,
        maintenanceRooms: 0,
        outOfOrderRooms: 0,
        linkedClosedRooms: 0,
        cleanRooms: 0,
        dirtyRooms: 1,
        expectedOccupancyRate: 0,
        dailyCancelledRooms: 0,
      },
    }
  } finally {
    loading.value = false
  }
}

// 日期变化处理
const onDateChange = (date: string) => {
  if (date) {
    fetchRoomTableData(date)
  }
}

// 标签页切换
const switchTab = (tab: RoomTableTab) => {
  activeTab.value = tab
  if (tab === 'daily') {
    fetchRoomTableData(selectedDate.value)
  } else if (tab === 'monthly') {
    fetchMonthlyRoomTableData()
  } else {
    fetchFutureRoomTableData(selectedDate.value, 7)
  }
}

const getDailyRowClassName = ({ rowIndex }: { rowIndex: number }) => {
  return rowIndex === tableData.value.length - 1 ? 'daily-total-row' : ''
}

const futureStatisticRows = computed(() => [
  {
    key: 'effective',
    label: t('accommodation.roomTable.stats.effectiveRooms'),
    tooltip: t('accommodation.roomTable.tooltips.effectiveRooms'),
    getValue: (statistic: FutureRoomStatistics) => statistic.effectiveRooms,
  },
  {
    key: 'occupancy',
    label: t('accommodation.roomTable.stats.expectedOccupancy'),
    tooltip: t('accommodation.roomTable.tooltips.expectedOccupancy'),
    getValue: (statistic: FutureRoomStatistics) => `${statistic.expectedOccupancyRate}%`,
  },
  {
    key: 'revenue',
    label: t('accommodation.roomTable.stats.expectedRoomRevenue'),
    tooltip: t('accommodation.roomTable.tooltips.expectedRoomRevenue'),
    getValue: (statistic: FutureRoomStatistics) => statistic.expectedRoomRevenue,
  },
  {
    key: 'totalFee',
    label: t('accommodation.roomTable.stats.expectedTotalRoomFee'),
    tooltip: t('accommodation.roomTable.tooltips.expectedTotalRoomFee'),
    getValue: (statistic: FutureRoomStatistics) => statistic.expectedTotalRoomFee,
  },
  {
    key: 'averageRevenue',
    label: t('accommodation.roomTable.stats.averageRoomRevenue'),
    tooltip: t('accommodation.roomTable.tooltips.averageRoomRevenue'),
    getValue: (statistic: FutureRoomStatistics) => statistic.averageRoomRevenue,
  },
])

const futureDateDisplayText = computed(() => {
  if (!selectedDate.value) {
    return ''
  }

  return `${selectedDate.value.replace(/-/g, '/')} ${futureWeekdayLabel.value}`
})

const futureWeekdayLabel = computed(() => {
  if (!selectedDate.value) {
    return ''
  }

  return weekdayFullMap.value[getYmdWeekdayIndex(selectedDate.value)]
})

// 获取远期房情表数据
const fetchFutureRoomTableData = async (startDate: string, days: number = 7) => {
  try {
    loading.value = true

    const response = await getFutureRoomTableData(startDate, days)

    if (response.success) {
      futureRoomTableData.value = response.data
    } else {
      ElMessage.error(response.message || t('accommodation.roomTable.messages.loadFutureFailed'))
    }
  } catch (error) {
    console.error('获取远期房情表数据失败:', error)
    ElMessage.error(t('accommodation.roomTable.messages.loadFutureFailed'))
  } finally {
    loading.value = false
  }
}

// 远期房情日期变化处理
const onFutureDateChange = (date: string) => {
  if (date) {
    fetchFutureRoomTableData(date, 7)
  }
}

const previousMonth = () => {
  selectedMonthDate.value = getYmdMonthStart(addCalendarMonthsToYmd(selectedMonthDate.value, -1))
  fetchMonthlyRoomTableData()
}

const nextMonth = () => {
  selectedMonthDate.value = getYmdMonthStart(addCalendarMonthsToYmd(selectedMonthDate.value, 1))
  fetchMonthlyRoomTableData()
}

const goToCurrentMonth = () => {
  selectedMonthDate.value = getYmdMonthStart(getStoreTodayYmd())
  fetchMonthlyRoomTableData()
}

// 上一个时间段
const previousPeriod = () => {
  selectedDate.value = addDaysToYmd(selectedDate.value, -7)
  fetchFutureRoomTableData(selectedDate.value, 7)
}

// 下一个时间段
const nextPeriod = () => {
  selectedDate.value = addDaysToYmd(selectedDate.value, 7)
  fetchFutureRoomTableData(selectedDate.value, 7)
}

// 导出数据
const exportData = () => {
  if (activeTab.value === 'daily') {
    exportDailyRoomTable()
    return
  }
  if (activeTab.value === 'monthly') {
    exportMonthlyRoomTable()
    return
  }
  exportFutureRoomTable()
}

const escapeHtml = (value: string | number) => {
  return String(value ?? '')
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;')
}

const buildHtmlTable = (
  title: string,
  headers: Array<string | number>,
  rows: Array<Array<string | number>>,
) => {
  const headerHtml = headers.map((header) => `<th>${escapeHtml(header)}</th>`).join('')
  const rowHtml = rows
    .map((row) => `<tr>${row.map((cell) => `<td>${escapeHtml(cell)}</td>`).join('')}</tr>`)
    .join('')

  return `
    <div class="sheet-section">
      <h3>${escapeHtml(title)}</h3>
      <table>
        <thead><tr>${headerHtml}</tr></thead>
        <tbody>${rowHtml}</tbody>
      </table>
    </div>
  `
}

const downloadExcelFile = (fileName: string, sections: string[]) => {
  const html = `
    <!DOCTYPE html>
    <html>
      <head>
        <meta charset="UTF-8" />
        <style>
          body { font-family: Arial, Helvetica, sans-serif; font-size: 12px; padding: 16px; }
          h3 { margin: 0 0 10px 0; font-size: 14px; }
          .sheet-section { margin-bottom: 20px; }
          table { border-collapse: collapse; width: 100%; margin-bottom: 8px; }
          th, td { border: 1px solid #dcdfe6; padding: 6px 8px; text-align: center; white-space: nowrap; }
          th { background: #f5f7fa; font-weight: 600; }
        </style>
      </head>
      <body>
        ${sections.join('')}
      </body>
    </html>
  `

  const blob = new Blob([`\uFEFF${html}`], {
    type: 'application/vnd.ms-excel;charset=utf-8;',
  })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = fileName
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  URL.revokeObjectURL(url)
}

const exportDailyRoomTable = () => {
  if (!roomTableData.value || tableData.value.length === 0) {
    ElMessage.warning(t('accommodation.roomTable.messages.noDailyExportData'))
    return
  }

  const headers = [
    t('accommodation.roomTable.columns.roomTypeName'),
    t('accommodation.roomTable.columns.totalRooms'),
    t('accommodation.roomTable.columns.availableForSale'),
    t('accommodation.roomTable.columns.availableRooms'),
    t('accommodation.roomTable.columns.occupiedRooms'),
    t('accommodation.roomTable.columns.occupiedWithoutDeparture'),
    t('accommodation.roomTable.columns.scheduledDeparture'),
    t('accommodation.roomTable.columns.scheduledArrival'),
    t('accommodation.roomTable.columns.reservedRooms'),
    t('accommodation.roomTable.columns.maintenanceRooms'),
    t('accommodation.roomTable.columns.outOfOrderRooms'),
    t('accommodation.roomTable.columns.linkedClosedRooms'),
    t('accommodation.roomTable.columns.cleanRooms'),
    t('accommodation.roomTable.columns.dirtyRooms'),
    t('accommodation.roomTable.columns.expectedOccupancyRate'),
    t('accommodation.roomTable.columns.dailyCancelledRooms'),
  ]

  const rows = tableData.value.map((item: RoomStatistics) => [
    item.roomTypeName,
    item.totalRooms,
    item.availableForSale,
    item.availableRooms,
    item.occupiedRooms,
    item.occupiedWithoutDeparture,
    item.scheduledDeparture,
    item.scheduledArrival,
    item.reservedRooms,
    item.maintenanceRooms,
    item.outOfOrderRooms,
    item.linkedClosedRooms,
    item.cleanRooms,
    item.dirtyRooms,
    `${item.expectedOccupancyRate}%`,
    item.dailyCancelledRooms,
  ])

  const section = buildHtmlTable(
    t('accommodation.roomTable.export.dailyTitle', { date: selectedDate.value }),
    headers,
    rows,
  )
  downloadExcelFile(
    t('accommodation.roomTable.export.dailyFileName', { date: selectedDate.value }),
    [section],
  )
  ElMessage.success(t('accommodation.roomTable.messages.dailyExportSuccess'))
}

const exportMonthlyRoomTable = () => {
  if (filteredMonthlyRoomCards.value.length === 0) {
    ElMessage.warning(t('accommodation.roomTable.messages.noMonthlyExportData'))
    return
  }

  const headers: Array<string | number> = [
    t('accommodation.common.roomType'),
    t('accommodation.common.roomNumber'),
    ...selectedMonthDates.value,
  ]

  const rows = filteredMonthlyRoomCards.value.map((room) => {
    const dayStatusByDate = new Map(
      room.cells
        .filter((cell): cell is MonthlyRoomDay => !!cell)
        .map((cell) => [cell.date, cell.kind]),
    )

    return [
      room.roomType,
      room.roomNumber,
      ...selectedMonthDates.value.map((date) => {
        const kind = dayStatusByDate.get(date)
        if (kind === 'available-many') {
          return monthlyText.value.availableMany
        }
        if (kind === 'available') {
          return monthlyText.value.available
        }
        return monthlyText.value.full
      }),
    ]
  })

  const section = buildHtmlTable(
    t('accommodation.roomTable.export.monthlyTitle', { month: selectedMonthLabel.value }),
    headers,
    rows,
  )
  downloadExcelFile(
    t('accommodation.roomTable.export.monthlyFileName', { month: selectedMonthStart.value.slice(0, 7) }),
    [section],
  )
  ElMessage.success(t('accommodation.roomTable.messages.monthlyExportSuccess'))
}

const exportFutureRoomTable = () => {
  if (!futureRoomTableData.value) {
    ElMessage.warning(t('accommodation.roomTable.messages.noFutureExportData'))
    return
  }

  const futureData = futureRoomTableData.value
  const mainHeaders: Array<string | number> = [
    t('accommodation.roomTable.future.roomType'),
    t('accommodation.roomTable.future.totalRooms'),
  ]
  futureData.total.dates.forEach((date) => {
    mainHeaders.push(
      `${date.date} ${t('accommodation.roomTable.future.available')}`,
      `${date.date} ${t('accommodation.roomTable.future.occupied')}`,
      `${date.date} ${t('accommodation.roomTable.future.unavailable')}`,
    )
  })

  const mainRows: Array<Array<string | number>> = futureData.roomTypes.map((roomType) => {
    const row: Array<string | number> = [roomType.roomTypeName, roomType.totalRooms]
    roomType.dates.forEach((date) => {
      row.push(date.available, date.occupied, date.unavailable)
    })
    return row
  })

  const percentageRow: Array<string | number> = [
    t('accommodation.roomTable.future.percentageOfTotalRooms'),
    '-',
  ]
  futureData.total.dates.forEach((date) => {
    percentageRow.push(
      `${date.availableRate}%`,
      `${date.occupiedRate}%`,
      `${date.unavailableRate}%`,
    )
  })
  mainRows.push(percentageRow)

  const totalRow: Array<string | number> = [
    t('accommodation.roomTable.future.total'),
    futureData.total.totalRooms,
  ]
  futureData.total.dates.forEach((date) => {
    totalRow.push(date.available, date.occupied, date.unavailable)
  })
  mainRows.push(totalRow)

  const statsHeaders: Array<string | number> = [t('accommodation.common.item')]
  futureData.statistics.forEach((stat) => {
    statsHeaders.push(stat.date)
  })

  const statsRows: Array<Array<string | number>> = [
    [
      t('accommodation.roomTable.stats.effectiveRooms'),
      ...futureData.statistics.map((stat) => stat.effectiveRooms),
    ],
    [
      t('accommodation.roomTable.stats.expectedOccupancy'),
      ...futureData.statistics.map((stat) => `${stat.expectedOccupancyRate}%`),
    ],
    [
      t('accommodation.roomTable.stats.expectedRoomRevenue'),
      ...futureData.statistics.map((stat) => stat.expectedRoomRevenue),
    ],
    [
      t('accommodation.roomTable.stats.expectedTotalRoomFee'),
      ...futureData.statistics.map((stat) => stat.expectedTotalRoomFee),
    ],
    [
      t('accommodation.roomTable.stats.averageRoomRevenue'),
      ...futureData.statistics.map((stat) => stat.averageRoomRevenue),
    ],
  ]

  const mainSection = buildHtmlTable(
    t('accommodation.roomTable.export.futureMainTitle', {
      startDate: futureData.startDate,
      endDate: futureData.endDate,
    }),
    mainHeaders,
    mainRows,
  )
  const statsSection = buildHtmlTable(
    t('accommodation.roomTable.export.futureStatsTitle'),
    statsHeaders,
    statsRows,
  )
  downloadExcelFile(
    t('accommodation.roomTable.export.futureFileName', {
      startDate: futureData.startDate,
      endDate: futureData.endDate,
    }),
    [mainSection, statsSection],
  )
  ElMessage.success(t('accommodation.roomTable.messages.futureExportSuccess'))
}

// 获取格式化的日期和星期
// 格式化表格表头日期
const formatDateHeader = (dateStr: string, dayOfWeek: string): string => {
  const today = getStoreTodayYmd()
  const isToday = dateStr === today
  const { month, day } = formatYmdMonthDay(dateStr)

  return isToday
    ? `${month}-${day} ${t('accommodation.common.today')}`
    : `${month}-${day} ${dayOfWeek}`
}

// 格式化统计区域的日期标题
const formatStatisticDateHeader = (dateStr: string): string => {
  const today = getStoreTodayYmd()
  const isToday = dateStr === today
  const { month, day } = formatYmdMonthDay(dateStr)

  // 获取星期几
  const weekday = weekdayFullMap.value[getYmdWeekdayIndex(dateStr)]

  return isToday
    ? `${month}-${day} ${t('accommodation.common.today')}`
    : `${month}-${day} ${weekday}`
}

// 组件挂载时获取数据
onMounted(() => {
  fetchRoomTableData(selectedDate.value)
})
</script>

<style scoped>
.room-table {
  --room-table-gap: 18px;
  --room-table-left-min-width: 336px;
  --room-table-center-safe-width: 500px;
  --room-table-right-min-width: 346px;
  --room-table-center-shift: calc(-56px + ((var(--sidebar-width, 84px) - 84px) / 6) + 7px);
  --room-table-right-shift: -28px;
  --room-table-min-width: calc(
    var(--room-table-left-min-width) + var(--room-table-center-safe-width) +
      var(--room-table-right-min-width) + (var(--room-table-gap) * 2)
  );
  padding: 4px 0 24px 0;
  background: #f5f5f5;
  min-height: 100vh;
}

.room-table-header {
  box-sizing: border-box;
  display: grid;
  grid-template-columns:
    minmax(var(--room-table-left-min-width), 1fr)
    auto
    minmax(var(--room-table-right-min-width), 1fr);
  align-items: center;
  gap: var(--room-table-gap);
  width: max(calc(100% + 24px), calc(var(--room-table-min-width) + 64px));
  min-width: calc(var(--room-table-min-width) + 64px);
  margin: 0 0 16px;
  padding: 0 32px;
}

.room-table-header-spacer {
  min-width: var(--room-table-left-min-width);
}

.room-table-header-spacer--right {
  min-width: var(--room-table-right-min-width);
}

.room-table-tabs {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  justify-self: center;
  gap: 2px;
  padding: 2px;
  border-radius: 999px;
  background: #ffffff;
  box-shadow: 0 1px 8px rgba(30, 30, 30, 0.04);
  transform: translateX(var(--room-table-center-shift));
  transition: transform 0.24s ease;
}

.room-table-tab {
  min-width: 78px;
  height: 24px;
  padding: 0 11px;
  border: none;
  border-radius: 999px;
  background: transparent;
  color: #252525;
  cursor: pointer;
  font-size: 12px;
  font-weight: 600;
  line-height: 1;
  white-space: nowrap;
  transition:
    background-color 0.2s ease,
    color 0.2s ease,
    opacity 0.2s ease;
}

.room-table-tab:hover:not(:disabled) {
  background: #f2f2f2;
}

.room-table-tab.is-active,
.room-table-tab.is-active:hover {
  background: #111111;
  color: #ffffff;
}

.room-table-tab.is-disabled {
  color: #9b9b9b;
  cursor: not-allowed;
  opacity: 0.72;
}

.toolbar {
  display: flex;
  justify-self: end;
  justify-content: flex-end;
  align-items: center;
  gap: 8px;
  min-width: var(--room-table-right-min-width);
  background: transparent;
  transform: translateX(var(--room-table-right-shift));
  transition: transform 0.24s ease;
}

.date-picker {
  width: 146px;
}

.toolbar :deep(.el-input__wrapper) {
  height: 30px;
  border-radius: 3px;
  background: #ffffff;
  box-shadow: 0 0 0 1px #dcdcdc inset;
}

.toolbar :deep(.el-input__wrapper:hover),
.toolbar :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1px #c8c8c8 inset;
}

.toolbar :deep(.el-button) {
  height: 30px;
  padding: 0 12px;
  border-radius: 3px;
  border-color: #1d94f3;
  background: #1d94f3;
  color: #ffffff;
  font-size: 12px;
  font-weight: 600;
}

.table-container {
  margin: 0 24px;
  background: #ffffff;
  border: 1px solid #cfcfcf;
  border-radius: 0;
  overflow: hidden;
  box-shadow: none;
}

.room-statistics-table {
  width: 100%;
  color: #303030;
  font-size: 14px;
}

.room-statistics-table :deep(.el-table) {
  border-radius: 0;
}

.room-statistics-table :deep(.el-table__inner-wrapper::before),
.room-statistics-table :deep(.el-table__border-left-patch) {
  display: none;
}

.room-statistics-table :deep(.el-table__header th.el-table__cell) {
  height: 58px;
  border-color: #cfcfcf;
}

.room-statistics-table :deep(.el-table__header th .cell) {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 3px;
  padding: 0 6px;
  color: #252525;
  line-height: 1.28;
  white-space: normal;
}

.daily-header-text {
  display: inline-flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  line-height: 1.28;
  white-space: normal;
}

.daily-header-text :deep(.daily-header-main),
.daily-header-text :deep(.daily-header-note) {
  display: block;
  max-width: 100%;
  overflow: visible;
  word-break: keep-all;
  overflow-wrap: normal;
  hyphens: none;
}

.daily-header-text :deep(.daily-header-main) {
  white-space: normal;
}

.daily-header-text :deep(.daily-header-note) {
  white-space: nowrap;
}

.room-statistics-table :deep(.el-table__body td.el-table__cell) {
  height: 56px;
  border-color: #e1e1e1;
  background: #ffffff;
}

.room-statistics-table :deep(.el-table__body tr.el-table__row:nth-child(even) td.el-table__cell) {
  background: #f8f8f8;
}

.room-statistics-table :deep(.el-table__body td .cell) {
  padding: 0 8px;
  line-height: 1.35;
  color: #303030;
  white-space: normal;
}

.room-statistics-table :deep(.el-table__row:hover > td.el-table__cell) {
  background: #fafafa;
}

.room-statistics-table :deep(.daily-total-row td.el-table__cell) {
  background: #f3f3f3 !important;
  font-weight: 700;
}

.room-statistics-table :deep(.daily-total-row td .cell) {
  color: #202020;
}

.info-icon {
  margin-left: 2px;
  color: #858585;
  cursor: help;
  font-size: 11px;
  vertical-align: -1px;
}

.info-icon:hover {
  color: #333333;
}

.monthly-room-container {
  margin: 0 24px;
  min-height: 520px;
}

.monthly-toolbar {
  display: flex;
  align-items: center;
  gap: 10px;
  min-height: 50px;
  margin-bottom: 16px;
  padding: 10px 14px;
  background: #ffffff;
  border: 1px solid #eeeeee;
}

.monthly-filter-field {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.monthly-filter-label {
  flex: 0 0 auto;
  color: #3f4347;
  font-size: 13px;
  font-weight: 600;
  white-space: nowrap;
}

.monthly-filter--room-type {
  width: 178px;
}

.monthly-filter--status {
  width: 120px;
}

.monthly-search {
  width: 330px;
  margin-left: auto;
}

.monthly-toolbar :deep(.el-input__wrapper),
.monthly-toolbar :deep(.el-select__wrapper) {
  min-height: 32px;
  border-radius: 4px;
  box-shadow: 0 0 0 1px #dedede inset;
}

.monthly-toolbar :deep(.el-button) {
  height: 32px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 600;
}

.monthly-month-switcher {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  min-width: 264px;
  margin-left: 2px;
}

.monthly-month-button {
  min-width: 108px;
  height: 32px;
  padding: 0 16px;
  border: 1px solid #dcdcdc;
  border-radius: 4px;
  background: #f8f8f8;
  color: #303030;
  cursor: pointer;
  font-size: 13px;
  font-weight: 600;
  white-space: nowrap;
}

.monthly-month-button:hover {
  background: #f1f1f1;
}

.monthly-current-button {
  padding: 0 12px;
  color: #303030;
  border-color: #d9d9d9;
  background: #ffffff;
}

.monthly-room-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 14px;
}

.monthly-room-card {
  min-height: 284px;
  padding: 12px 14px 16px;
  background: #ffffff;
  border: 1px solid #f0f0f0;
  border-radius: 6px;
  box-shadow: 0 6px 14px rgba(30, 30, 30, 0.04);
}

.monthly-room-card-header {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 42px;
  margin-bottom: 8px;
  color: #3c3c3c;
  line-height: 1.35;
}

.monthly-room-type {
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 13px;
  font-weight: 700;
}

.monthly-room-number {
  color: #707070;
  font-size: 12px;
  font-weight: 600;
}

.monthly-weekdays {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 6px;
  margin-bottom: 10px;
  padding: 4px 0 10px;
  border-bottom: 1px solid #e6e6e6;
  color: #7a7f85;
  font-size: 12px;
  text-align: center;
}

.monthly-days {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 7px 0;
}

.monthly-day {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 0;
  height: 27px;
  border-radius: 999px;
  color: #5f6267;
  font-size: 12px;
  font-weight: 700;
  line-height: 1;
  margin: 0 2px;
  box-sizing: border-box;
}

.monthly-day--single {
  border-radius: 999px;
}

.monthly-day--start {
  margin-right: 0;
  border-radius: 999px 0 0 999px;
}

.monthly-day--middle {
  margin-right: 0;
  margin-left: 0;
  border-radius: 0;
}

.monthly-day--end {
  margin-left: 0;
  border-radius: 0 999px 999px 0;
}

.monthly-day--full {
  background: #ff7f85;
  color: #6f262a;
}

.monthly-day--available {
  background: #bff2a4;
  color: #34702b;
}

.monthly-day--available-many {
  background: #ffe27a;
  color: #75621b;
}

.monthly-day--blank {
  margin: 0;
  background: transparent;
}

.monthly-empty {
  min-height: 360px;
  background: #ffffff;
  border: 1px solid #eeeeee;
}

/* 远期房情表样式 */
.future-room-container {
  --future-room-type-width: 118px;
  --future-total-width: 74px;
  --future-day-width: 154px;
  --future-sub-col-width: calc(var(--future-day-width) / 3);
  margin: 0 24px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.future-top-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
}

.date-navigation {
  display: flex;
  align-items: center;
  gap: 10px;
  min-height: 36px;
  background: transparent;
  padding: 0;
}

.future-date-picker-shell {
  position: relative;
  display: inline-flex;
  align-items: center;
  width: 190px;
  height: 32px;
}

.future-date-picker-shell :deep(.future-date-picker) {
  width: 190px !important;
}

.future-date-picker-display {
  position: absolute;
  top: 1px;
  right: 1px;
  bottom: 1px;
  left: 51px;
  display: inline-flex;
  align-items: center;
  justify-content: flex-end;
  min-width: 0;
  padding: 0 10px 0 12px;
  border-radius: 0 3px 3px 0;
  background: #f3f3f3;
  color: #2f2f2f;
  font-size: 14px;
  font-weight: 600;
  line-height: 1;
  letter-spacing: 0.1px;
  pointer-events: none;
  white-space: nowrap;
}

.future-nav-button {
  width: 24px;
  height: 24px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 0;
  border: 1px solid #d8ebf4;
  border-radius: 999px;
  background: #ffffff;
  color: #9aa8ad;
  cursor: pointer;
  font-size: 13px;
  transition:
    border-color 0.2s ease,
    color 0.2s ease,
    background-color 0.2s ease;
}

.future-nav-button :deep(svg) {
  width: 30px;
  height: 30px;
}

.future-nav-button:hover {
  border-color: #67b6d9;
  color: #55add4;
  background: #f6fcff;
}

.future-nav-button--next {
  border-color: #8cc7df;
  color: #5baed1;
}

.toolbar--future {
  min-width: auto;
  transform: none;
}

.toolbar--future :deep(.el-input__wrapper) {
  height: 32px;
}

.future-date-picker-shell :deep(.future-date-picker--primary .el-input__wrapper) {
  height: 32px;
  min-height: 32px;
  padding: 0 2px 0 0;
  border-radius: 4px;
  background: #ffffff;
  box-shadow:
    0 0 0 1px #d8d8d8 inset,
    0 1px 2px rgba(20, 20, 20, 0.04);
}

.future-date-picker-shell :deep(.future-date-picker--primary .el-input__wrapper:hover),
.future-date-picker-shell :deep(.future-date-picker--primary .el-input__wrapper.is-focus) {
  box-shadow:
    0 0 0 1px #cdd4da inset,
    0 1px 2px rgba(20, 20, 20, 0.05);
}

.future-date-picker-shell :deep(.future-date-picker--primary .el-input__prefix) {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 54px;
  margin-right: 0;
  color: #a8afb7;
}

.future-date-picker-shell :deep(.future-date-picker--primary .el-input__prefix-inner) {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 100%;
}

.future-date-picker-shell :deep(.future-date-picker--primary .el-input__icon) {
  font-size: 16px;
}

.future-date-picker-shell :deep(.future-date-picker--primary .el-input__suffix) {
  display: none;
}

.future-date-picker-shell :deep(.future-date-picker--primary .el-input__inner) {
  height: 28px;
  margin: 2px 0;
  padding: 0 12px 0 52px;
  border-radius: 0;
  background: transparent;
  color: transparent !important;
  -webkit-text-fill-color: transparent !important;
  caret-color: transparent;
  opacity: 0 !important;
  text-shadow: none;
  font-size: 14px;
  font-weight: 600;
  text-align: left;
  letter-spacing: 0.1px;
  cursor: pointer;
}

.future-date-picker-shell :deep(.future-date-picker--primary .el-input__inner::placeholder) {
  color: transparent !important;
  -webkit-text-fill-color: transparent !important;
  text-align: right;
}

.table-scroll-wrapper {
  overflow-x: auto;
  overflow-y: visible;
  width: 100%;
}

.future-table-container {
  background: transparent;
  border: none;
  border-radius: 0;
  overflow: visible;
  box-shadow: none;
}

.table-scroll-wrapper::-webkit-scrollbar {
  height: 6px;
}

.table-scroll-wrapper::-webkit-scrollbar-track {
  background: #f2f2f2;
}

.table-scroll-wrapper::-webkit-scrollbar-thumb {
  background: #c9c9c9;
  border-radius: 999px;
}

.table-scroll-wrapper::-webkit-scrollbar-thumb:hover {
  background: #ababab;
}

.future-table-main {
  min-width: calc(
    var(--future-room-type-width) + var(--future-total-width) +
      (var(--future-day-width) * 7)
  );
  width: 100%;
  margin-bottom: 14px;
  border-collapse: separate;
  border-spacing: 0;
  color: #303030;
  font-size: 13px;
  position: relative;
  background: #ffffff;
  box-shadow:
    inset 1px 0 0 #c7d8e2,
    0 8px 14px rgba(38, 38, 38, 0.14);
}

.date-header-row th {
  height: 33px;
  padding: 0 8px;
  background: #cde5fb;
  color: #1890ff;
  font-weight: 600;
  text-align: center;
  border-top: 1px solid #c7d8e2;
  border-right: 1.5px solid #b7cbd7;
  border-bottom: 1px solid #c7d8e2;
  white-space: nowrap;
}

.sub-header-row th {
  height: 27px;
  padding: 0 6px;
  background: #f8f8f8;
  color: #303030;
  font-size: 12px;
  font-weight: 600;
  text-align: center;
  border-right: 1px solid #d9e2e8;
  border-bottom: 1px solid #d0d0d0;
  white-space: nowrap;
}

.fixed-header {
  position: sticky;
  z-index: 12;
  background: #cde5fb !important;
}

.room-type-col {
  width: var(--future-room-type-width);
  min-width: var(--future-room-type-width);
  left: 0;
}

.total-rooms-col {
  width: var(--future-total-width);
  min-width: var(--future-total-width);
  left: var(--future-room-type-width);
}

.fixed-cell {
  position: sticky;
  z-index: 9;
  background: #ffffff;
  border-right: 1px solid #dcdcdc;
  border-bottom: 1px solid #e5e5e5;
  padding: 0 10px;
}

.room-type-cell {
  left: 0;
  width: var(--future-room-type-width);
  min-width: var(--future-room-type-width);
  text-align: center;
  font-weight: 600;
  line-height: 1.25;
  white-space: normal;
}

.total-rooms-cell {
  left: var(--future-room-type-width);
  width: var(--future-total-width);
  min-width: var(--future-total-width);
  text-align: center;
}

.data-cell {
  width: var(--future-sub-col-width);
  min-width: var(--future-sub-col-width);
  height: 50px;
  padding: 0 8px;
  background: #ffffff;
  border-right: 1px solid #e5e5e5;
  border-bottom: 1px solid #e5e5e5;
  color: #303030;
  text-align: center;
  font-weight: 500;
  white-space: nowrap;
}

.date-header {
  width: var(--future-day-width);
  min-width: var(--future-day-width);
}

.sub-header {
  width: var(--future-sub-col-width);
  min-width: var(--future-sub-col-width);
}

.room-type-row:nth-child(even) .fixed-cell,
.room-type-row:nth-child(even) .data-cell {
  background: #f8f8f8;
}

.percentage-row .fixed-cell {
  background: #ffffff !important;
  color: #303030;
  font-weight: 600;
}

.percentage-row .data-cell {
  background: #ffffff;
  color: #303030;
}

.total-row .fixed-cell {
  background: #f8f8f8 !important;
  color: #303030;
  font-weight: 700;
}

.total-row .data-cell {
  background: #f8f8f8;
  color: #303030;
  font-weight: 700;
}

.future-table-main .data-cell.available {
  color: #16a34a;
}

.future-table-main .data-cell.occupied {
  color: #f5222d;
}

.future-statistics-table {
  min-width: calc(
    var(--future-room-type-width) + var(--future-total-width) +
      (var(--future-day-width) * 7)
  );
  width: 100%;
  margin-top: 0;
  border-collapse: separate;
  border-spacing: 0;
  color: #303030;
  font-size: 13px;
  background: #ffffff;
  box-shadow: 0 8px 14px rgba(38, 38, 38, 0.1);
}

.future-stats-label-cell {
  position: sticky;
  left: 0;
  z-index: 8;
  width: calc(var(--future-room-type-width) + var(--future-total-width));
  min-width: calc(var(--future-room-type-width) + var(--future-total-width));
  height: 40px;
  padding: 0 14px;
  background: #ffffff;
  border-right: 1px solid #e8e8e8;
  color: #303030;
  font-weight: 600;
  text-align: center;
}

.future-stats-date-cell,
.future-stat-value {
  width: var(--future-day-width);
  min-width: var(--future-day-width);
  height: 40px;
  padding: 0 12px;
  background: #ffffff;
  border-right: 1px solid #e8e8e8;
  text-align: center;
  white-space: nowrap;
}

.future-stats-date-cell {
  color: #1890ff;
  font-weight: 600;
}

.future-stats-date-row .future-stats-label-cell,
.future-stats-date-row .future-stats-date-cell {
  height: 54px;
  border-bottom: 1px solid #ececec;
}

.future-stat-value {
  font-weight: 500;
}

.future-table-main th:last-child,
.future-table-main td:last-child,
.future-statistics-table td:last-child {
  border-right: none;
}

.stat-label {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  line-height: 1.25;
}

@media (max-width: 1280px) {
  .room-table-header {
    width: max(calc(100% + 20px), calc(var(--room-table-min-width) + 44px));
    min-width: calc(var(--room-table-min-width) + 44px);
    padding: 0 20px 0 24px;
  }
}

@media (max-width: 960px) {
  .monthly-toolbar {
    align-items: flex-start;
    flex-wrap: wrap;
  }

  .monthly-filter-field {
    width: 100%;
  }

  .monthly-filter--room-type,
  .monthly-filter--status {
    flex: 1;
    width: auto;
  }

  .monthly-search {
    width: 100%;
    margin-left: 0;
  }

  .monthly-month-switcher {
    min-width: 0;
  }

  .future-top-bar {
    align-items: flex-start;
  }

  .toolbar--future {
    width: 100%;
    justify-content: flex-start;
  }
}
</style>
