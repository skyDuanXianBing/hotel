<template>
  <ion-page>
    <ion-header translucent>
      <ion-toolbar>
        <ion-buttons slot="start">
          <ion-back-button :default-href="ROUTE_PATHS.rooms" />
        </ion-buttons>
        <ion-title>房价管理</ion-title>
        <ion-buttons slot="end">
          <ion-button @click="handleOpenHistory">改价记录</ion-button>
        </ion-buttons>
      </ion-toolbar>
    </ion-header>

    <ion-content fullscreen class="mobile-page room-price-page">
      <ion-refresher slot="fixed" @ionRefresh="handleRefresh">
        <ion-refresher-content pulling-text="下拉刷新房价" refreshing-spinner="crescent" />
      </ion-refresher>

      <section class="mobile-hero room-price-page__hero">
        <p class="mobile-note room-price-page__eyebrow">住宿运营</p>
        <h1 class="mobile-title">房价管理</h1>
        <p class="mobile-subtitle">移动端提供长周期运营矩阵、批量更新、导出与单元格编辑，补齐房价管理核心闭环。</p>
        <div class="mobile-chip-row">
          <span class="mobile-chip">起始日 {{ formatDate(selectedDate) }}</span>
          <span class="mobile-chip">连续 {{ windowDays }} 天</span>
        </div>
      </section>

      <div class="mobile-stack">
        <section class="mobile-card room-price-page__toolbar-card">
          <div class="room-price-page__toolbar-row">
            <ion-button fill="outline" size="small" @click="handleShiftDate(-7)">上周</ion-button>
            <ion-button fill="outline" size="small" @click="handleShiftDate(-1)">前一天</ion-button>
            <ion-button fill="outline" size="small" @click="handleGoToday">今天</ion-button>
            <ion-button fill="outline" size="small" @click="handleShiftDate(1)">后一天</ion-button>
            <ion-button fill="outline" size="small" @click="handleShiftDate(7)">下周</ion-button>
          </div>

          <div class="room-price-page__toolbar-row room-price-page__toolbar-row--compact">
            <button
              v-for="preset in windowDayPresets"
              :key="preset"
              class="room-price-page__preset-chip"
              :class="{ 'is-active': windowDays === preset }"
              type="button"
              @click="handleChangeWindowDays(preset)"
            >
              {{ preset }} 天
            </button>
          </div>

          <div class="room-price-page__filter-grid">
            <label class="room-price-page__field">
              <span>起始日期</span>
              <input :value="selectedDate" type="date" @input="handleDateInput" />
            </label>

            <label class="room-price-page__field">
              <span>房型筛选</span>
              <select :value="roomTypeSelectValue" @change="handleRoomTypeChange">
                <option value="">全部房型</option>
                <option v-for="roomType in roomTypes" :key="roomType.id" :value="String(roomType.id)">
                  {{ roomType.name }}
                </option>
              </select>
            </label>

            <label class="room-price-page__field">
              <span>分组筛选</span>
              <select :value="roomGroupSelectValue" @change="handleRoomGroupChange">
                <option value="">全部分组</option>
                <option v-for="group in roomGroups" :key="group.id" :value="String(group.id)">
                  {{ group.name }}
                </option>
              </select>
            </label>
          </div>

          <div class="room-price-page__toolbar-row room-price-page__toolbar-row--actions">
            <ion-button fill="outline" @click="handleExportCurrentView">导出 CSV</ion-button>
            <ion-button @click="handleOpenBulkUpdate">批量更新</ion-button>
          </div>

          <p v-if="errorMessage" class="mobile-note room-price-page__error">{{ errorMessage }}</p>
        </section>

        <section class="mobile-card">
          <div class="mobile-inline-row room-price-page__section-header">
            <div>
              <h2 class="mobile-section-title">运营矩阵</h2>
              <p class="mobile-note">横向查看 {{ windowDays }} 天价格，点击任意单元格可进入区间编辑。</p>
            </div>
            <ion-spinner v-if="loading" name="crescent" />
          </div>

          <div class="mobile-chip-row room-price-page__summary-row">
            <span class="mobile-chip">房型 {{ summaryMetrics.roomTypeCount }} 个</span>
            <span class="mobile-chip">价格计划 {{ summaryMetrics.planCount }} 个</span>
            <span class="mobile-chip">售罄/关房 {{ summaryMetrics.unavailableCellCount }} 天</span>
          </div>

          <div v-if="matrixRows.length > 0" class="room-price-page__matrix-shell">
            <div class="room-price-page__matrix-scroll">
              <div class="room-price-page__matrix-row room-price-page__matrix-row--header" :style="matrixGridStyle">
                <div class="room-price-page__matrix-leading room-price-page__matrix-leading--header">
                  <strong>房型 / 价格计划</strong>
                  <span>点击单元格编辑</span>
                </div>
                <div
                  v-for="day in dateWindow"
                  :key="`matrix-header-${day.date}`"
                  class="room-price-page__matrix-header-cell"
                  :class="{ 'is-today': day.isToday, 'is-weekend': day.isWeekend }"
                >
                  <strong>{{ day.shortLabel }}</strong>
                  <span>{{ day.weekday }}</span>
                </div>
              </div>

              <div
                v-for="row in matrixRows"
                :key="row.key"
                class="room-price-page__matrix-row"
                :style="matrixGridStyle"
              >
                <div class="room-price-page__matrix-leading room-price-page__matrix-leading--body">
                  <strong>{{ row.roomTypeName }}</strong>
                  <span>{{ row.pricePlanName }}</span>
                  <small>{{ row.roomTypeCode || '未设置编码' }}</small>
                </div>

                <button
                  v-for="cell in row.cells"
                  :key="`${row.key}-${cell.date}`"
                  class="room-price-page__matrix-cell"
                  :class="getMatrixCellClass(cell.record)"
                  type="button"
                  @click="handleOpenEditor(row, cell)"
                >
                  <strong>{{ getDisplayPriceText(cell.record) }}</strong>
                  <span>{{ getAvailabilityCompactText(cell.record) }}</span>
                  <small>{{ getMatrixStatusText(cell.record) }}</small>
                </button>
              </div>
            </div>
          </div>

          <p v-else-if="!loading" class="mobile-note">当前日期范围暂无可展示的运营房价数据。</p>
        </section>

        <section class="mobile-card">
          <h2 class="mobile-section-title">移动端操作提示</h2>
          <ul class="mobile-bullet-list">
            <li>支持 7 / 14 / 30 天矩阵浏览，长周期通过横向滚动查看。</li>
            <li>支持批量更新多个房型、多个日期段与星期模板。</li>
            <li>支持导出当前筛选窗口的 CSV 明细，便于移动端快速下发。</li>
          </ul>
        </section>
      </div>

      <ion-modal :is-open="editorOpen" @didDismiss="handleCloseEditor">
        <ion-header>
          <ion-toolbar>
            <ion-title>编辑房价</ion-title>
            <ion-buttons slot="end">
              <ion-button @click="handleCloseEditor">关闭</ion-button>
            </ion-buttons>
          </ion-toolbar>
        </ion-header>

        <ion-content class="mobile-page room-price-page__modal-page">
          <section class="mobile-card room-price-page__editor-card">
            <div class="room-price-page__editor-title">
              <strong>{{ editorForm.roomTypeName }}</strong>
              <span>{{ editorForm.pricePlanName }}</span>
            </div>

            <div v-if="editorRecord" class="room-price-page__editor-current">
              <div class="room-price-page__editor-current-header">
                <strong>{{ getDisplayPriceText(editorRecord) }}</strong>
                <span>{{ getAvailabilityText(editorRecord) }}</span>
              </div>
              <div class="room-price-page__tag-row">
                <span v-for="tag in getStatusTags(editorRecord)" :key="`editor-${tag}`">{{ tag }}</span>
              </div>
              <p class="mobile-note">
                {{ getPriceSourceText(editorRecord) }} · {{ getStayRestrictionText(editorRecord) }}
              </p>
              <p v-if="editorRecord.priceLabsBasePrice !== undefined && editorRecord.priceLabsBasePrice !== null" class="mobile-note">
                PriceLabs 基价 {{ formatCurrency(editorRecord.priceLabsBasePrice) }}，最近同步 {{ formatDateTime(editorRecord.priceLabsUpdatedAt) }}
              </p>
              <p v-if="editorRecord.manualOverrideUntil" class="mobile-note">
                手动覆盖有效至 {{ formatDate(editorRecord.manualOverrideUntil) }}
              </p>
            </div>

            <div class="room-price-page__filter-grid">
              <label class="room-price-page__field">
                <span>开始日期</span>
                <input v-model="editorForm.startDate" type="date" />
              </label>

              <label class="room-price-page__field">
                <span>结束日期</span>
                <input v-model="editorForm.endDate" type="date" />
              </label>

              <label class="room-price-page__field room-price-page__field--full">
                <span>编辑项</span>
                <select v-model="editorForm.settingType">
                  <option value="price">价格</option>
                  <option value="minStay">最小入住天数</option>
                  <option value="maxStay">最大入住天数</option>
                  <option value="closeRoom">关房</option>
                  <option value="cta">CTA</option>
                  <option value="ctd">CTD</option>
                </select>
              </label>
            </div>

            <div class="room-price-page__weekday-section">
              <div class="room-price-page__weekday-header">
                <span>适用星期</span>
                <div class="room-price-page__weekday-actions">
                  <ion-button fill="clear" size="small" @click="handleSelectAllWeekdays">全选</ion-button>
                  <ion-button fill="clear" size="small" @click="handleInvertWeekdays">反选</ion-button>
                </div>
              </div>
              <div class="room-price-page__weekday-grid">
                <button
                  v-for="item in weekdayOptions"
                  :key="item.value"
                  class="room-price-page__weekday-button"
                  :class="{ 'is-active': isWeekdaySelected(item.value) }"
                  type="button"
                  @click="handleToggleWeekday(item.value)"
                >
                  {{ item.label }}
                </button>
              </div>
              <p class="mobile-note room-price-page__weekday-hint">{{ editorWeekdayHint }}</p>
            </div>

            <label v-if="editorForm.settingType === 'price'" class="room-price-page__field room-price-page__field--full">
              <span>价格</span>
              <input v-model="editorForm.price" inputmode="decimal" placeholder="请输入价格" type="number" />
            </label>

            <label v-if="editorForm.settingType === 'minStay'" class="room-price-page__field room-price-page__field--full">
              <span>最小入住天数</span>
              <input v-model="editorForm.minStay" inputmode="numeric" placeholder="请输入最小入住天数" type="number" />
            </label>

            <label v-if="editorForm.settingType === 'maxStay'" class="room-price-page__field room-price-page__field--full">
              <span>最大入住天数</span>
              <input v-model="editorForm.maxStay" inputmode="numeric" placeholder="请输入最大入住天数" type="number" />
            </label>

            <div v-if="editorForm.settingType === 'closeRoom'" class="room-price-page__toggle-row">
              <div>
                <strong>关房</strong>
                <p>开启后该日期范围不可售。</p>
              </div>
              <ion-toggle v-model="editorForm.closeRoom" />
            </div>

            <div v-if="editorForm.settingType === 'cta'" class="room-price-page__toggle-row">
              <div>
                <strong>CTA</strong>
                <p>控制该价格是否禁止到店入住。</p>
              </div>
              <ion-toggle v-model="editorForm.cta" />
            </div>

            <div v-if="editorForm.settingType === 'ctd'" class="room-price-page__toggle-row">
              <div>
                <strong>CTD</strong>
                <p>控制该价格是否禁止离店。</p>
              </div>
              <ion-toggle v-model="editorForm.ctd" />
            </div>

            <div class="room-price-page__editor-actions">
              <ion-button fill="outline" @click="handleCloseEditor">取消</ion-button>
              <ion-button :disabled="saving" @click="handleSaveEditor">
                {{ saving ? '保存中...' : '保存修改' }}
              </ion-button>
            </div>
          </section>
        </ion-content>
      </ion-modal>

      <ion-modal :is-open="bulkUpdateOpen" @didDismiss="handleCloseBulkUpdate">
        <ion-header>
          <ion-toolbar>
            <ion-title>批量更新</ion-title>
            <ion-buttons slot="end">
              <ion-button @click="handleCloseBulkUpdate">关闭</ion-button>
            </ion-buttons>
          </ion-toolbar>
        </ion-header>

        <ion-content class="mobile-page room-price-page__modal-page">
          <section class="mobile-card room-price-page__editor-card">
            <div class="mobile-chip-row">
              <span class="mobile-chip">房型 {{ bulkForm.roomTypeIds.length }} 个</span>
              <span class="mobile-chip">日期段 {{ bulkForm.dateRanges.length }} 个</span>
              <span class="mobile-chip">星期 {{ bulkSelectedWeekdayCountLabel }}</span>
            </div>

            <div class="room-price-page__bulk-section">
              <div class="room-price-page__bulk-section-header">
                <strong>1. 选择房型</strong>
                <div class="room-price-page__weekday-actions">
                  <ion-button fill="clear" size="small" @click="handleSelectAllBulkRoomTypes">全选</ion-button>
                  <ion-button fill="clear" size="small" @click="handleClearBulkRoomTypes">清空</ion-button>
                </div>
              </div>

              <div class="room-price-page__selection-grid">
                <button
                  v-for="roomType in bulkSelectableRoomTypes"
                  :key="`bulk-room-type-${roomType.id}`"
                  class="room-price-page__selection-chip"
                  :class="{ 'is-active': bulkForm.roomTypeIds.includes(roomType.id) }"
                  type="button"
                  @click="handleToggleBulkRoomType(roomType.id)"
                >
                  {{ roomType.name }}
                </button>
              </div>
            </div>

            <div class="room-price-page__bulk-section">
              <div class="room-price-page__bulk-section-header">
                <strong>2. 选择日期段</strong>
                <ion-button fill="clear" size="small" @click="handleAddBulkDateRange">添加日期段</ion-button>
              </div>

              <div class="room-price-page__bulk-range-list">
                <div
                  v-for="(range, index) in bulkForm.dateRanges"
                  :key="`bulk-range-${index}`"
                  class="room-price-page__bulk-range-item"
                >
                  <label class="room-price-page__field">
                    <span>开始日期</span>
                    <input v-model="range.startDate" type="date" />
                  </label>
                  <label class="room-price-page__field">
                    <span>结束日期</span>
                    <input v-model="range.endDate" type="date" />
                  </label>
                  <ion-button
                    v-if="bulkForm.dateRanges.length > 1"
                    fill="clear"
                    color="danger"
                    @click="handleRemoveBulkDateRange(index)"
                  >
                    删除
                  </ion-button>
                </div>
              </div>
            </div>

            <div class="room-price-page__bulk-section">
              <div class="room-price-page__bulk-section-header">
                <strong>3. 适用星期</strong>
                <div class="room-price-page__weekday-actions">
                  <ion-button fill="clear" size="small" @click="handleSelectAllBulkWeekdays">全选</ion-button>
                  <ion-button fill="clear" size="small" @click="handleClearBulkWeekdays">清空</ion-button>
                </div>
              </div>

              <div class="room-price-page__weekday-grid">
                <button
                  v-for="item in bulkWeekdayOptions"
                  :key="`bulk-weekday-${item.value}`"
                  class="room-price-page__weekday-button"
                  :class="{ 'is-active': bulkForm.weekdays.includes(item.value) }"
                  type="button"
                  @click="handleToggleBulkWeekday(item.value)"
                >
                  {{ item.label }}
                </button>
              </div>
            </div>

            <div class="room-price-page__bulk-section">
              <div class="room-price-page__toggle-row">
                <div>
                  <strong>区分平日 / 周末</strong>
                  <p>开启后可分别设置平日价与周末价。</p>
                </div>
                <ion-toggle v-model="bulkForm.weekendDifferentiation" />
              </div>

              <div class="room-price-page__filter-grid">
                <label class="room-price-page__field">
                  <span>平日价</span>
                  <input v-model="bulkForm.weekdayPrice" inputmode="decimal" placeholder="请输入平日价" type="number" />
                </label>

                <label v-if="bulkForm.weekendDifferentiation" class="room-price-page__field">
                  <span>周末价</span>
                  <input v-model="bulkForm.weekendPrice" inputmode="decimal" placeholder="请输入周末价" type="number" />
                </label>

                <label class="room-price-page__field room-price-page__field--full">
                  <span>备注</span>
                  <textarea v-model="bulkForm.notes" rows="3" placeholder="可选，写入本次批量更新备注"></textarea>
                </label>
              </div>
            </div>

            <div class="room-price-page__editor-actions">
              <ion-button fill="outline" @click="handleCloseBulkUpdate">取消</ion-button>
              <ion-button :disabled="bulkSubmitting" @click="handleSubmitBulkUpdate">
                {{ bulkSubmitting ? '提交中...' : '应用批量更新' }}
              </ion-button>
            </div>
          </section>
        </ion-content>
      </ion-modal>
    </ion-content>
  </ion-page>
</template>

<script setup lang="ts">
import {
  IonBackButton,
  IonButton,
  IonButtons,
  IonContent,
  IonHeader,
  IonModal,
  IonPage,
  IonRefresher,
  IonRefresherContent,
  IonSpinner,
  IonTitle,
  IonToggle,
  IonToolbar,
} from '@ionic/vue'
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { onIonViewWillEnter } from '@ionic/vue'
import {
  bulkPriceChange,
  getRoomPriceManagementData,
  updatePriceByPlan,
  type BulkPriceChangeRequest,
  type RoomPriceManagementDTO,
  type UpdatePriceByPlanRequest,
} from '@/api/roomPrice'
import { getAllRoomGroups, getGroupMembers } from '@/api/roomGroup'
import { getAllRoomTypes, getAllRoomTypesWithRooms, type RoomTypeDTO } from '@/api/roomType'
import { ROUTE_PATHS } from '@/router/guards'
import { useUserStore } from '@/stores/user'
import type { RoomGroupDTO } from '@/types/settings'
import {
  buildDateWindow,
  formatCurrency,
  formatDate,
  formatDateTime,
  getTodayDate,
  resolveUserLabel,
  shiftDate,
} from '@/utils/accommodation'
import { showSuccessToast, showWarningToast } from '@/utils/notify'
import { isHandledRequestError } from '@/utils/request'

type SettingType = 'price' | 'minStay' | 'maxStay' | 'closeRoom' | 'cta' | 'ctd'

interface PriceCellView {
  date: string
  shortLabel: string
  weekday: string
  record: RoomPriceManagementDTO | null
}

interface PricePlanView {
  key: string
  pricePlanId: number
  pricePlanName: string
  cells: PriceCellView[]
}

interface RoomPriceGroupView {
  roomTypeId: number
  roomTypeName: string
  roomTypeCode: string
  plans: PricePlanView[]
}

interface MatrixRowView {
  key: string
  roomTypeId: number
  roomTypeName: string
  roomTypeCode: string
  pricePlanId: number
  pricePlanName: string
  cells: PriceCellView[]
}

interface RoomGroupOption extends RoomGroupDTO {
  id: number
}

interface SummaryMetrics {
  roomTypeCount: number
  planCount: number
  unavailableCellCount: number
}

interface EditorState {
  roomTypeId: number
  pricePlanId: number
  roomTypeName: string
  pricePlanName: string
  startDate: string
  endDate: string
  weekdays: number[]
  settingType: SettingType
  price: string
  minStay: string
  maxStay: string
  closeRoom: boolean
  cta: boolean
  ctd: boolean
}

interface BulkDateRangeItem {
  startDate: string
  endDate: string
}

interface BulkFormState {
  roomTypeIds: number[]
  dateRanges: BulkDateRangeItem[]
  weekdays: number[]
  weekendDifferentiation: boolean
  weekdayPrice: string
  weekendPrice: string
  notes: string
}

const DEFAULT_WINDOW_DAYS = 30
const WINDOW_DAY_PRESETS = [7, 14, 30]
const FULL_WEEKDAY_VALUES = [1, 2, 3, 4, 5, 6, 7]
const ALL_WEEKDAY_VALUES = [0, ...FULL_WEEKDAY_VALUES]
const BULK_WEEKDAY_VALUES = [1, 2, 3, 4, 5, 6, 0]
const MAX_STAY_LIMIT = 99
const MAX_BULK_DATE_RANGES = 10

const router = useRouter()
const userStore = useUserStore()

const selectedDate = ref(getTodayDate())
const windowDays = ref(DEFAULT_WINDOW_DAYS)
const selectedRoomTypeId = ref<number | null>(null)
const selectedRoomGroupId = ref<number | null>(null)
const roomTypes = ref<RoomTypeDTO[]>([])
const roomGroups = ref<RoomGroupOption[]>([])
const roomGroupRoomTypeIdsMap = ref<Record<number, number[]>>({})
const records = ref<RoomPriceManagementDTO[]>([])
const loading = ref(false)
const saving = ref(false)
const editorOpen = ref(false)
const bulkUpdateOpen = ref(false)
const bulkSubmitting = ref(false)
const errorMessage = ref('')
const editorRecord = ref<RoomPriceManagementDTO | null>(null)
let hasLoadedReferenceData = false

const weekdayOptions = [
  { label: '全部', value: 0 },
  { label: '周一', value: 1 },
  { label: '周二', value: 2 },
  { label: '周三', value: 3 },
  { label: '周四', value: 4 },
  { label: '周五', value: 5 },
  { label: '周六', value: 6 },
  { label: '周日', value: 7 },
]

const bulkWeekdayOptions = [
  { label: '周一', value: 1 },
  { label: '周二', value: 2 },
  { label: '周三', value: 3 },
  { label: '周四', value: 4 },
  { label: '周五', value: 5 },
  { label: '周六', value: 6 },
  { label: '周日', value: 0 },
]

const editorForm = ref<EditorState>({
  roomTypeId: 0,
  pricePlanId: 0,
  roomTypeName: '',
  pricePlanName: '',
  startDate: getTodayDate(),
  endDate: getTodayDate(),
  weekdays: [],
  settingType: 'price',
  price: '0',
  minStay: '1',
  maxStay: '',
  closeRoom: false,
  cta: false,
  ctd: false,
})

const createDefaultBulkForm = (): BulkFormState => ({
  roomTypeIds: [],
  dateRanges: [
    {
      startDate: selectedDate.value,
      endDate: selectedDate.value,
    },
  ],
  weekdays: [...BULK_WEEKDAY_VALUES],
  weekendDifferentiation: false,
  weekdayPrice: '',
  weekendPrice: '',
  notes: '',
})

const bulkForm = ref<BulkFormState>(createDefaultBulkForm())

const dateWindow = computed(() => {
  return buildDateWindow(selectedDate.value, windowDays.value)
})

const windowDayPresets = WINDOW_DAY_PRESETS

const roomTypeSelectValue = computed(() => {
  if (!selectedRoomTypeId.value) {
    return ''
  }
  return String(selectedRoomTypeId.value)
})

const roomGroupSelectValue = computed(() => {
  if (!selectedRoomGroupId.value) {
    return ''
  }
  return String(selectedRoomGroupId.value)
})

const matrixGridStyle = computed(() => {
  return {
    gridTemplateColumns: `168px repeat(${dateWindow.value.length}, minmax(88px, 1fr))`,
  }
})

const filteredRecords = computed(() => {
  if (!selectedRoomGroupId.value) {
    return records.value
  }

  const roomTypeIds = roomGroupRoomTypeIdsMap.value[selectedRoomGroupId.value] || []
  if (roomTypeIds.length === 0) {
    return []
  }

  const allowedRoomTypeIds = new Set(roomTypeIds)
  return records.value.filter((record) => allowedRoomTypeIds.has(record.roomTypeId))
})

const filteredRecordMap = computed(() => {
  const map = new Map<string, RoomPriceManagementDTO>()

  for (const record of filteredRecords.value) {
    const recordPricePlanId = record.pricePlanId ?? 0
    map.set(`${record.roomTypeId}-${recordPricePlanId}-${record.priceDate}`, record)
  }

  return map
})

const groupedRecords = computed<RoomPriceGroupView[]>(() => {
  const roomMap = new Map<number, RoomPriceGroupView>()

  for (const record of filteredRecords.value) {
    const roomTypeId = record.roomTypeId
    const pricePlanId = record.pricePlanId ?? 0
    const pricePlanName = record.pricePlanName || '未命名价格计划'
    const roomKey = roomTypeId
    const planKey = `${roomTypeId}-${pricePlanId}`

    if (!roomMap.has(roomKey)) {
      roomMap.set(roomKey, {
        roomTypeId,
        roomTypeName: record.roomTypeName,
        roomTypeCode: record.roomTypeCode,
        plans: [],
      })
    }

    const roomGroup = roomMap.get(roomKey)
    if (!roomGroup) {
      continue
    }

    let planGroup = roomGroup.plans.find((item) => item.key === planKey)
    if (!planGroup) {
      planGroup = {
        key: planKey,
        pricePlanId,
        pricePlanName,
        cells: [],
      }
      roomGroup.plans.push(planGroup)
    }
  }

  const groups = Array.from(roomMap.values())

  for (const group of groups) {
    for (const plan of group.plans) {
      const cells: PriceCellView[] = []
      for (const day of dateWindow.value) {
        const matchedRecord = filteredRecordMap.value.get(
          `${group.roomTypeId}-${plan.pricePlanId}-${day.date}`,
        )

        cells.push({
          date: day.date,
          shortLabel: day.shortLabel,
          weekday: day.weekday,
          record: matchedRecord || null,
        })
      }
      plan.cells = cells
    }
  }

  groups.sort((left, right) => left.roomTypeName.localeCompare(right.roomTypeName, 'zh-CN'))
  return groups
})

const matrixRows = computed<MatrixRowView[]>(() => {
  const rows: MatrixRowView[] = []

  for (const group of groupedRecords.value) {
    for (const plan of group.plans) {
      rows.push({
        key: plan.key,
        roomTypeId: group.roomTypeId,
        roomTypeName: group.roomTypeName,
        roomTypeCode: group.roomTypeCode,
        pricePlanId: plan.pricePlanId,
        pricePlanName: plan.pricePlanName,
        cells: plan.cells,
      })
    }
  }

  return rows
})

const bulkSelectableRoomTypes = computed(() => {
  const groupRoomTypeIds = selectedRoomGroupId.value
    ? roomGroupRoomTypeIdsMap.value[selectedRoomGroupId.value] || []
    : []
  const allowedByGroup = selectedRoomGroupId.value ? new Set(groupRoomTypeIds) : null

  return roomTypes.value
    .filter((roomType) => {
      if (selectedRoomTypeId.value && roomType.id !== selectedRoomTypeId.value) {
        return false
      }

      if (allowedByGroup && !allowedByGroup.has(roomType.id)) {
        return false
      }

      return true
    })
    .sort((left, right) => left.name.localeCompare(right.name, 'zh-CN'))
})

const bulkSelectedWeekdayCountLabel = computed(() => {
  if (bulkForm.value.weekdays.length === BULK_WEEKDAY_VALUES.length) {
    return '全部'
  }

  return `${bulkForm.value.weekdays.length} 天`
})

const summaryMetrics = computed<SummaryMetrics>(() => {
  let planCount = 0
  let unavailableCellCount = 0

  for (const group of groupedRecords.value) {
    planCount += group.plans.length
    for (const plan of group.plans) {
      for (const cell of plan.cells) {
        if (isUnavailableRecord(cell.record)) {
          unavailableCellCount += 1
        }
      }
    }
  }

  return {
    roomTypeCount: groupedRecords.value.length,
    planCount,
    unavailableCellCount,
  }
})

const editorWeekdayHint = computed(() => {
  if (!editorForm.value.startDate || !editorForm.value.endDate) {
    return '多天范围会按日期逐天生效，已选星期只过滤范围内命中的日期。'
  }

  const normalizedRange = normalizeDateRange(editorForm.value.startDate, editorForm.value.endDate)
  if (normalizedRange.startDate === normalizedRange.endDate) {
    return '单天范围选择星期时，将按星期模板兼容保存；只改当天可不选星期。'
  }

  return '多天范围会按日期逐天生效，已选星期只过滤范围内命中的日期。'
})

function resolveWarningMessage(error: unknown, fallbackMessage: string) {
  if (error instanceof Error && error.message) {
    return error.message
  }
  return fallbackMessage
}

function normalizeDateRange(startDate: string, endDate: string) {
  if (startDate <= endDate) {
    return { startDate, endDate }
  }

  return {
    startDate: endDate,
    endDate: startDate,
  }
}

function parseYmdToLocalDate(value: string) {
  const [year, month, day] = value.split('-').map(Number)
  return new Date(year, (month || 1) - 1, day || 1)
}

function getWeekdayValue(date: string) {
  const weekday = parseYmdToLocalDate(date).getDay()
  if (weekday === 0) {
    return 7
  }
  return weekday
}

function buildWeekdaySelection(values: number[]) {
  const nextValues = values.filter((value) => FULL_WEEKDAY_VALUES.includes(value))
  nextValues.sort((left, right) => left - right)

  const hasFullWeek = FULL_WEEKDAY_VALUES.every((value) => nextValues.includes(value))
  if (hasFullWeek) {
    return [...ALL_WEEKDAY_VALUES]
  }

  return nextValues
}

function isUnavailableRecord(record: RoomPriceManagementDTO | null) {
  if (!record) {
    return false
  }

  if (record.closeRoom) {
    return true
  }

  if (typeof record.availableRooms === 'number' && record.availableRooms <= 0) {
    return true
  }

  return false
}

function getDisplayPriceText(record: RoomPriceManagementDTO | null) {
  if (!record) {
    return '未配置'
  }

  return formatCurrency(record.price)
}

function getAvailabilityText(record: RoomPriceManagementDTO | null) {
  if (!record) {
    return '暂无房量信息'
  }

  if (record.closeRoom) {
    return '已关房'
  }

  if (typeof record.availableRooms === 'number') {
    if (record.availableRooms <= 0) {
      return '已售罄'
    }
    return `可售 ${record.availableRooms} 间`
  }

  return '房量待确认'
}

function getAvailabilityCompactText(record: RoomPriceManagementDTO | null) {
  if (!record) {
    return '未配'
  }

  if (record.closeRoom) {
    return '关房'
  }

  if (typeof record.availableRooms === 'number') {
    if (record.availableRooms <= 0) {
      return '售罄'
    }
    return `余${record.availableRooms}`
  }

  return '待定'
}

function getStayRestrictionText(record: RoomPriceManagementDTO | null) {
  if (!record) {
    return '暂无入住限制'
  }

  const minStay = record.minStay ?? 1
  if (record.maxStay) {
    return `入住 ${minStay}-${record.maxStay} 晚`
  }

  return `最少入住 ${minStay} 晚`
}

function getPriceSourceText(record: RoomPriceManagementDTO | null) {
  if (!record?.priceSource) {
    return '来源：默认周价'
  }

  if (record.priceSource === 'MANUAL') {
    return '来源：手动覆盖'
  }

  if (record.priceSource === 'PRICELABS') {
    return '来源：PriceLabs'
  }

  if (record.priceSource === 'SYSTEM') {
    return '来源：系统周价'
  }

  return `来源：${record.priceSource}`
}

function getStatusTags(record: RoomPriceManagementDTO | null) {
  const tags: string[] = []

  if (!record) {
    return tags
  }

  if (typeof record.availableRooms === 'number') {
    if (record.availableRooms <= 0) {
      tags.push('售罄')
    } else {
      tags.push(`余房 ${record.availableRooms}`)
    }
  }

  if (record.closeRoom) {
    tags.push('关房')
  }
  if (record.cta) {
    tags.push('CTA')
  }
  if (record.ctd) {
    tags.push('CTD')
  }
  if (record.manualOverride) {
    tags.push('手动覆盖')
  }
  if (record.priceLabsBasePrice !== undefined && record.priceLabsBasePrice !== null) {
    tags.push('PriceLabs')
  }

  return tags
}

function getMatrixStatusText(record: RoomPriceManagementDTO | null) {
  if (!record) {
    return '未配置'
  }

  const tags = getStatusTags(record).filter((tag) => tag !== '手动覆盖' && tag !== 'PriceLabs')
  if (tags.length > 0) {
    return tags.slice(0, 2).join(' · ')
  }

  if (record.manualOverride) {
    return '手动覆盖'
  }

  return record.minStay && record.minStay > 1 ? `最少 ${record.minStay} 晚` : '可编辑'
}

function getDateCardClass(record: RoomPriceManagementDTO | null) {
  return {
    'is-unavailable': isUnavailableRecord(record),
    'is-closed': Boolean(record?.closeRoom),
    'is-manual': Boolean(record?.manualOverride),
  }
}

function getMatrixCellClass(record: RoomPriceManagementDTO | null) {
  return {
    'is-unavailable': isUnavailableRecord(record),
    'is-closed': Boolean(record?.closeRoom),
    'is-manual': Boolean(record?.manualOverride),
    'is-empty': !record,
  }
}

function escapeCsvValue(value: string | number | boolean | null | undefined) {
  const text = value === null || value === undefined ? '' : String(value)
  const escapedText = text.replace(/"/g, '""')
  return `"${escapedText}"`
}

function downloadCsvFile(filename: string, content: string) {
  const blob = new Blob([`\uFEFF${content}`], { type: 'text/csv;charset=utf-8;' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = filename
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  URL.revokeObjectURL(url)
}

function handleExportCurrentView() {
  if (matrixRows.value.length === 0) {
    showWarningToast('当前没有可导出的房价数据')
    return
  }

  const header = [
    '房型',
    '房型编码',
    '价格计划',
    '日期',
    '价格',
    '可售房量',
    '最小入住',
    '最大入住',
    '关房',
    'CTA',
    'CTD',
    '价格来源',
    '备注',
  ]

  const rows: string[] = [header.map((item) => escapeCsvValue(item)).join(',')]

  for (const row of matrixRows.value) {
    for (const cell of row.cells) {
      rows.push(
        [
          row.roomTypeName,
          row.roomTypeCode || '',
          row.pricePlanName,
          cell.date,
          cell.record?.price ?? '',
          cell.record?.availableRooms ?? '',
          cell.record?.minStay ?? '',
          cell.record?.maxStay ?? '',
          cell.record?.closeRoom ? '是' : '否',
          cell.record?.cta ? '是' : '否',
          cell.record?.ctd ? '是' : '否',
          cell.record?.priceSource || '',
          cell.record?.notes || '',
        ]
          .map((item) => escapeCsvValue(item))
          .join(','),
      )
    }
  }

  downloadCsvFile(`room-price-${selectedDate.value}-${windowDays.value}d.csv`, rows.join('\n'))
  showSuccessToast('房价 CSV 已导出')
}

async function loadReferenceData() {
  const [roomTypeResponse, roomTypeWithRoomsResponse, roomGroupResponse] = await Promise.all([
    getAllRoomTypes(),
    getAllRoomTypesWithRooms(),
    getAllRoomGroups(),
  ])

  if (!roomTypeResponse.success || !roomTypeResponse.data) {
    throw new Error(roomTypeResponse.message || '加载房型失败')
  }

  roomTypes.value = roomTypeResponse.data

  if (!roomGroupResponse.success || !roomGroupResponse.data) {
    roomGroups.value = []
    roomGroupRoomTypeIdsMap.value = {}
    hasLoadedReferenceData = true
    return
  }

  roomGroups.value = roomGroupResponse.data.filter(
    (group): group is RoomGroupOption => typeof group.id === 'number',
  )

  if (!roomTypeWithRoomsResponse.success || !roomTypeWithRoomsResponse.data) {
    roomGroupRoomTypeIdsMap.value = {}
    hasLoadedReferenceData = true
    return
  }

  const roomIdToRoomTypeId = new Map<number, number>()
  for (const roomType of roomTypeWithRoomsResponse.data) {
    for (const room of roomType.rooms || []) {
      roomIdToRoomTypeId.set(room.id, roomType.id)
    }
  }

  const groupMemberResults = await Promise.all(
    roomGroups.value.map(async (group) => {
      try {
        const memberResponse = await getGroupMembers(group.id)
        if (!memberResponse.success || !memberResponse.data) {
          return { groupId: group.id, roomTypeIds: [] as number[] }
        }

        const roomTypeIds = Array.from(
          new Set(
            memberResponse.data
              .map((member) => roomIdToRoomTypeId.get(member.roomId))
              .filter((roomTypeId): roomTypeId is number => typeof roomTypeId === 'number'),
          ),
        )

        return { groupId: group.id, roomTypeIds }
      } catch {
        return { groupId: group.id, roomTypeIds: [] as number[] }
      }
    }),
  )

  const nextMap: Record<number, number[]> = {}
  for (const result of groupMemberResults) {
    nextMap[result.groupId] = result.roomTypeIds
  }

  roomGroupRoomTypeIdsMap.value = nextMap
  hasLoadedReferenceData = true
}

async function loadPriceData() {
  const endDate = shiftDate(selectedDate.value, windowDays.value - 1)
  const response = await getRoomPriceManagementData(
    selectedDate.value,
    endDate,
    selectedRoomTypeId.value || undefined,
  )

  if (!response.success || !response.data) {
    throw new Error(response.message || '加载房价失败')
  }

  records.value = response.data
}

async function loadPageData(forceReloadReferences = false) {
  loading.value = true
  errorMessage.value = ''

  try {
    const tasks = [loadPriceData()]
    if (forceReloadReferences || !hasLoadedReferenceData) {
      tasks.push(loadReferenceData())
    }
    await Promise.all(tasks)
  } catch (error) {
    const message = resolveWarningMessage(error, '加载房价失败')
    errorMessage.value = message
    if (!isHandledRequestError(error)) {
      showWarningToast(message)
    }
  } finally {
    loading.value = false
  }
}

function resetEditor() {
  editorForm.value = {
    roomTypeId: 0,
    pricePlanId: 0,
    roomTypeName: '',
    pricePlanName: '',
    startDate: getTodayDate(),
    endDate: getTodayDate(),
    weekdays: [],
    settingType: 'price',
    price: '0',
    minStay: '1',
    maxStay: '',
    closeRoom: false,
    cta: false,
    ctd: false,
  }
  editorRecord.value = null
}

function isWeekdaySelected(value: number) {
  return editorForm.value.weekdays.includes(value)
}

function handleToggleWeekday(value: number) {
  if (value === 0) {
    if (editorForm.value.weekdays.includes(0)) {
      editorForm.value.weekdays = []
      return
    }

    editorForm.value.weekdays = [...ALL_WEEKDAY_VALUES]
    return
  }

  const nextValues = editorForm.value.weekdays.filter((item) => item !== 0 && item !== value)
  if (!editorForm.value.weekdays.includes(value)) {
    nextValues.push(value)
  }

  editorForm.value.weekdays = buildWeekdaySelection(nextValues)
}

function handleSelectAllWeekdays() {
  editorForm.value.weekdays = [...ALL_WEEKDAY_VALUES]
}

function handleInvertWeekdays() {
  const explicitSelected = editorForm.value.weekdays.filter((value) => value !== 0)
  const invertedValues = FULL_WEEKDAY_VALUES.filter((value) => !explicitSelected.includes(value))
  editorForm.value.weekdays = buildWeekdaySelection(invertedValues)
}

function handleOpenEditor(row: MatrixRowView, cell: PriceCellView) {
  editorRecord.value = cell.record
  editorForm.value = {
    roomTypeId: row.roomTypeId,
    pricePlanId: row.pricePlanId,
    roomTypeName: row.roomTypeName,
    pricePlanName: row.pricePlanName,
    startDate: cell.date,
    endDate: cell.date,
    weekdays: [],
    settingType: 'price',
    price: cell.record?.price !== undefined ? String(cell.record.price) : '0',
    minStay: cell.record?.minStay !== undefined ? String(cell.record.minStay) : '1',
    maxStay: cell.record?.maxStay !== undefined ? String(cell.record.maxStay) : '',
    closeRoom: Boolean(cell.record?.closeRoom),
    cta: Boolean(cell.record?.cta),
    ctd: Boolean(cell.record?.ctd),
  }
  editorOpen.value = true
}

function handleCloseEditor() {
  editorOpen.value = false
  resetEditor()
}

function createBulkDateRange(startDate = selectedDate.value, endDate = selectedDate.value): BulkDateRangeItem {
  return {
    startDate,
    endDate,
  }
}

function syncBulkFormWithCurrentView() {
  const nextRoomTypeIds = bulkSelectableRoomTypes.value.map((roomType) => roomType.id)
  bulkForm.value = {
    ...createDefaultBulkForm(),
    roomTypeIds: nextRoomTypeIds,
    dateRanges: [createBulkDateRange(selectedDate.value, selectedDate.value)],
  }
}

function handleOpenBulkUpdate() {
  if (bulkSelectableRoomTypes.value.length === 0) {
    showWarningToast('当前筛选条件下没有可批量更新的房型')
    return
  }

  syncBulkFormWithCurrentView()
  bulkUpdateOpen.value = true
}

function handleCloseBulkUpdate() {
  bulkUpdateOpen.value = false
  bulkForm.value = createDefaultBulkForm()
}

function handleSelectAllBulkRoomTypes() {
  bulkForm.value.roomTypeIds = bulkSelectableRoomTypes.value.map((roomType) => roomType.id)
}

function handleClearBulkRoomTypes() {
  bulkForm.value.roomTypeIds = []
}

function handleToggleBulkRoomType(roomTypeId: number) {
  if (bulkForm.value.roomTypeIds.includes(roomTypeId)) {
    bulkForm.value.roomTypeIds = bulkForm.value.roomTypeIds.filter((item) => item !== roomTypeId)
    return
  }

  bulkForm.value.roomTypeIds = [...bulkForm.value.roomTypeIds, roomTypeId]
}

function handleAddBulkDateRange() {
  if (bulkForm.value.dateRanges.length >= MAX_BULK_DATE_RANGES) {
    showWarningToast(`最多只能添加 ${MAX_BULK_DATE_RANGES} 个日期段`)
    return
  }

  bulkForm.value.dateRanges = [...bulkForm.value.dateRanges, createBulkDateRange()]
}

function handleRemoveBulkDateRange(index: number) {
  bulkForm.value.dateRanges = bulkForm.value.dateRanges.filter((_, currentIndex) => currentIndex !== index)
}

function handleSelectAllBulkWeekdays() {
  bulkForm.value.weekdays = [...BULK_WEEKDAY_VALUES]
}

function handleClearBulkWeekdays() {
  bulkForm.value.weekdays = []
}

function handleToggleBulkWeekday(weekday: number) {
  if (bulkForm.value.weekdays.includes(weekday)) {
    bulkForm.value.weekdays = bulkForm.value.weekdays.filter((item) => item !== weekday)
    return
  }

  bulkForm.value.weekdays = [...bulkForm.value.weekdays, weekday].sort((left, right) => {
    return BULK_WEEKDAY_VALUES.indexOf(left) - BULK_WEEKDAY_VALUES.indexOf(right)
  })
}

async function handleSubmitBulkUpdate() {
  if (bulkForm.value.roomTypeIds.length === 0) {
    showWarningToast('请至少选择一个房型')
    return
  }

  const normalizedDateRanges = bulkForm.value.dateRanges
    .filter((range) => range.startDate && range.endDate)
    .map((range) => normalizeDateRange(range.startDate, range.endDate))

  if (normalizedDateRanges.length === 0) {
    showWarningToast('请至少填写一个完整日期段')
    return
  }

  if (bulkForm.value.weekdays.length === 0) {
    showWarningToast('请至少选择一个星期')
    return
  }

  const weekdayPrice = Number(bulkForm.value.weekdayPrice)
  if (!Number.isFinite(weekdayPrice) || weekdayPrice < 0) {
    showWarningToast('请输入有效的平日价')
    return
  }

  let weekendPrice: number | undefined
  if (bulkForm.value.weekendDifferentiation) {
    weekendPrice = Number(bulkForm.value.weekendPrice)
    if (!Number.isFinite(weekendPrice) || weekendPrice < 0) {
      showWarningToast('请输入有效的周末价')
      return
    }
  }

  const requestData: BulkPriceChangeRequest = {
    roomTypeIds: [...bulkForm.value.roomTypeIds],
    dateRanges: normalizedDateRanges,
    weekdays:
      bulkForm.value.weekdays.length === BULK_WEEKDAY_VALUES.length
        ? undefined
        : [...bulkForm.value.weekdays],
    weekendDifferentiation: bulkForm.value.weekendDifferentiation,
    weekdayPrice,
    weekendPrice,
    notes: bulkForm.value.notes.trim() || undefined,
  }

  bulkSubmitting.value = true
  try {
    const response = await bulkPriceChange(requestData)
    if (!response.success) {
      throw new Error(response.message || '批量更新失败')
    }

    showSuccessToast(response.message || '批量房价已更新')
    bulkUpdateOpen.value = false
    await loadPageData()
  } catch (error) {
    if (!isHandledRequestError(error)) {
      showWarningToast(resolveWarningMessage(error, '批量更新失败'))
    }
  } finally {
    bulkSubmitting.value = false
  }
}

function hasMatchedWeekdayInRange(startDate: string, endDate: string, weekdays: number[]) {
  if (weekdays.length === 0 || weekdays.includes(0)) {
    return true
  }

  const weekdaySet = new Set(weekdays)
  const start = parseYmdToLocalDate(startDate)
  const end = parseYmdToLocalDate(endDate)
  const cursor = new Date(start)

  while (cursor <= end) {
    const currentWeekday = cursor.getDay() === 0 ? 7 : cursor.getDay()
    if (weekdaySet.has(currentWeekday)) {
      return true
    }
    cursor.setDate(cursor.getDate() + 1)
  }

  return false
}

function buildSaveSuccessMessage(settingType: SettingType) {
  if (settingType === 'price') {
    return '房价设置已更新'
  }
  if (settingType === 'minStay') {
    return '最小入住天数已更新'
  }
  if (settingType === 'maxStay') {
    return '最大入住天数已更新'
  }
  if (settingType === 'closeRoom') {
    return '关房状态已更新'
  }
  if (settingType === 'cta') {
    return 'CTA 已更新'
  }
  return 'CTD 已更新'
}

async function handleSaveEditor() {
  if (!editorForm.value.roomTypeId || !editorForm.value.pricePlanId) {
    showWarningToast('当前价格计划信息不完整，无法保存')
    return
  }

  if (!editorForm.value.startDate || !editorForm.value.endDate) {
    showWarningToast('请选择完整日期范围')
    return
  }

  const normalizedRange = normalizeDateRange(editorForm.value.startDate, editorForm.value.endDate)
  const normalizedWeekdays = [...editorForm.value.weekdays]
  const isSingleDayRange = normalizedRange.startDate === normalizedRange.endDate
  const hasExplicitWeekdaySelection = normalizedWeekdays.length > 0
  const applyWeekdaysInRange = hasExplicitWeekdaySelection ? !isSingleDayRange : true

  let payloadWeekdays: number[] | undefined
  if (hasExplicitWeekdaySelection) {
    const containsAllWeekdays = normalizedWeekdays.includes(0)
    if (!(applyWeekdaysInRange && containsAllWeekdays)) {
      payloadWeekdays = normalizedWeekdays
    }
  }

  if (
    applyWeekdaysInRange &&
    payloadWeekdays &&
    !payloadWeekdays.includes(0) &&
    !hasMatchedWeekdayInRange(normalizedRange.startDate, normalizedRange.endDate, payloadWeekdays)
  ) {
    showWarningToast('所选日期范围内没有匹配的星期，请调整后再保存')
    return
  }

  const requestData: UpdatePriceByPlanRequest = {
    roomTypeId: editorForm.value.roomTypeId,
    pricePlanId: editorForm.value.pricePlanId,
    startDate: normalizedRange.startDate,
    endDate: normalizedRange.endDate,
    weekdays: payloadWeekdays,
    applyWeekdaysInRange,
  }

  if (editorForm.value.settingType === 'price') {
    const price = Number(editorForm.value.price)
    if (!Number.isFinite(price) || price < 0) {
      showWarningToast('请输入有效价格')
      return
    }
    requestData.price = price
  }

  if (editorForm.value.settingType === 'minStay') {
    const minStay = Number(editorForm.value.minStay)
    if (!Number.isFinite(minStay) || minStay <= 0 || minStay > MAX_STAY_LIMIT) {
      showWarningToast('请输入 1 到 99 之间的最小入住天数')
      return
    }
    if (typeof editorRecord.value?.maxStay === 'number' && minStay > editorRecord.value.maxStay) {
      showWarningToast('最小入住天数不能大于当前最大入住天数')
      return
    }
    requestData.minStay = minStay
  }

  if (editorForm.value.settingType === 'maxStay') {
    const maxStay = Number(editorForm.value.maxStay)
    if (!Number.isFinite(maxStay) || maxStay <= 0 || maxStay > MAX_STAY_LIMIT) {
      showWarningToast('请输入 1 到 99 之间的最大入住天数')
      return
    }
    const currentMinStay = editorRecord.value?.minStay ?? 1
    if (maxStay < currentMinStay) {
      showWarningToast('最大入住天数不能小于当前最小入住天数')
      return
    }
    requestData.maxStay = maxStay
  }

  if (editorForm.value.settingType === 'closeRoom') {
    requestData.closeRoom = editorForm.value.closeRoom
  }

  if (editorForm.value.settingType === 'cta') {
    requestData.cta = editorForm.value.cta
  }

  if (editorForm.value.settingType === 'ctd') {
    requestData.ctd = editorForm.value.ctd
  }

  const operator = resolveUserLabel(userStore.currentUser?.nickname, userStore.currentUser?.email)

  saving.value = true
  try {
    const response = await updatePriceByPlan(requestData, operator)
    if (!response.success) {
      throw new Error(response.message || '保存房价失败')
    }

    const successMessage = response.message || buildSaveSuccessMessage(editorForm.value.settingType)
    showSuccessToast(successMessage)
    handleCloseEditor()
    await loadPageData()
  } catch (error) {
    if (!isHandledRequestError(error)) {
      showWarningToast(resolveWarningMessage(error, '保存房价失败'))
    }
  } finally {
    saving.value = false
  }
}

async function handleShiftDate(offsetDays: number) {
  selectedDate.value = shiftDate(selectedDate.value, offsetDays)
  await loadPageData()
}

async function handleChangeWindowDays(days: number) {
  if (windowDays.value === days) {
    return
  }

  windowDays.value = days
  await loadPageData()
}

async function handleGoToday() {
  selectedDate.value = getTodayDate()
  await loadPageData()
}

async function handleDateInput(event: Event) {
  const target = event.target as HTMLInputElement | null
  if (!target?.value) {
    return
  }

  selectedDate.value = target.value
  await loadPageData()
}

async function handleRoomTypeChange(event: Event) {
  const target = event.target as HTMLSelectElement | null
  if (!target) {
    return
  }

  if (!target.value) {
    selectedRoomTypeId.value = null
  } else {
    selectedRoomTypeId.value = Number(target.value)
  }

  await loadPageData()
}

function handleRoomGroupChange(event: Event) {
  const target = event.target as HTMLSelectElement | null
  if (!target) {
    return
  }

  if (!target.value) {
    selectedRoomGroupId.value = null
    return
  }

  selectedRoomGroupId.value = Number(target.value)
}

async function handleRefresh(event: CustomEvent) {
  await loadPageData(true)
  event.detail.complete()
}

async function handleOpenHistory() {
  await router.push(ROUTE_PATHS.roomsPricingHistory)
}

onIonViewWillEnter(async () => {
  await loadPageData(true)
})
</script>

<style scoped>
.room-price-page {
  display: block;
}

.room-price-page__hero {
  margin-top: 4px;
}

.room-price-page__eyebrow {
  color: var(--ion-color-primary);
  font-weight: 700;
}

.room-price-page__toolbar-card,
.room-price-page__editor-card {
  display: grid;
  gap: 14px;
}

.room-price-page__toolbar-row {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.room-price-page__toolbar-row--compact,
.room-price-page__toolbar-row--actions {
  align-items: center;
}

.room-price-page__preset-chip {
  min-height: 38px;
  padding: 0 14px;
  border: 1px solid rgba(15, 23, 42, 0.12);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.92);
  color: var(--app-muted);
  font: inherit;
}

.room-price-page__preset-chip.is-active {
  border-color: var(--ion-color-primary);
  background: rgba(59, 130, 246, 0.14);
  color: var(--ion-color-primary);
  font-weight: 700;
}

.room-price-page__filter-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.room-price-page__field {
  display: grid;
  gap: 8px;
}

.room-price-page__field--full {
  grid-column: 1 / -1;
}

.room-price-page__field span,
.room-price-page__weekday-section span {
  color: var(--app-heading);
  font-size: 13px;
  font-weight: 600;
}

.room-price-page__field input,
.room-price-page__field select,
.room-price-page__field textarea {
  box-sizing: border-box;
  width: 100%;
  min-height: 44px;
  padding: 10px 8px;
  border: 1px solid rgba(15, 23, 42, 0.12);
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.92);
  color: var(--app-heading);
  font: inherit;
  text-align: center;
}

.room-price-page__field textarea {
  min-height: 96px;
  resize: vertical;
  text-align: left;
}

.room-price-page__error {
  color: var(--ion-color-danger);
}

.room-price-page__section-header {
  align-items: flex-start;
}

.room-price-page__summary-row {
  margin-top: 12px;
}

.room-price-page__matrix-shell {
  margin-top: 16px;
  border: 1px solid var(--app-border);
  border-radius: 20px;
  overflow: hidden;
  background: rgba(255, 255, 255, 0.88);
}

.room-price-page__matrix-scroll {
  overflow-x: auto;
  overflow-y: hidden;
}

.room-price-page__matrix-row {
  display: grid;
  min-width: max-content;
}

.room-price-page__matrix-row--header {
  position: sticky;
  top: 0;
  z-index: 4;
}

.room-price-page__matrix-leading,
.room-price-page__matrix-header-cell,
.room-price-page__matrix-cell {
  min-height: 88px;
  padding: 10px;
  border-right: 1px solid rgba(15, 23, 42, 0.08);
  border-bottom: 1px solid rgba(15, 23, 42, 0.08);
}

.room-price-page__matrix-leading {
  position: sticky;
  left: 0;
  z-index: 3;
  display: grid;
  align-content: center;
  gap: 6px;
  background: rgba(248, 250, 252, 0.98);
}

.room-price-page__matrix-leading--header {
  z-index: 5;
}

.room-price-page__matrix-leading strong,
.room-price-page__matrix-header-cell strong,
.room-price-page__matrix-cell strong {
  color: var(--app-heading);
  font-size: 13px;
}

.room-price-page__matrix-leading span,
.room-price-page__matrix-leading small,
.room-price-page__matrix-header-cell span,
.room-price-page__matrix-cell span,
.room-price-page__matrix-cell small {
  color: var(--app-muted);
  font-size: 11px;
  line-height: 1.4;
}

.room-price-page__matrix-header-cell {
  display: grid;
  align-content: center;
  justify-items: center;
  gap: 4px;
  background: rgba(248, 250, 252, 0.98);
}

.room-price-page__matrix-header-cell.is-today {
  background: rgba(59, 130, 246, 0.12);
}

.room-price-page__matrix-header-cell.is-weekend {
  color: var(--ion-color-primary);
}

.room-price-page__matrix-cell {
  appearance: none;
  border-top: none;
  border-left: none;
  border-bottom: 1px solid rgba(15, 23, 42, 0.08);
  border-right: 1px solid rgba(15, 23, 42, 0.08);
  background: rgba(255, 255, 255, 0.96);
  display: grid;
  align-content: center;
  gap: 6px;
  text-align: left;
}

.room-price-page__matrix-cell.is-empty {
  background: rgba(248, 250, 252, 0.74);
}

.room-price-page__matrix-cell.is-unavailable {
  background: rgba(244, 63, 94, 0.08);
}

.room-price-page__matrix-cell.is-closed {
  box-shadow: inset 0 0 0 1px rgba(244, 63, 94, 0.18);
}

.room-price-page__matrix-cell.is-manual {
  box-shadow: inset 0 0 0 1px rgba(37, 99, 235, 0.18);
}

.room-price-page__matrix-cell strong {
  color: var(--ion-color-primary);
  font-size: 14px;
}

.room-price-page__group-list {
  margin-top: 16px;
}

.room-price-page__group-card,
.room-price-page__plan-card {
  display: grid;
  gap: 12px;
  padding: 14px;
  border-radius: 18px;
  border: 1px solid var(--app-border);
  background: rgba(255, 255, 255, 0.82);
}

.room-price-page__group-header strong,
.room-price-page__group-header p,
.room-price-page__plan-header strong,
.room-price-page__plan-header p {
  margin: 0;
}

.room-price-page__group-header p,
.room-price-page__plan-header p {
  margin-top: 6px;
  color: var(--app-muted);
  font-size: 13px;
}

.room-price-page__date-strip {
  display: flex;
  gap: 10px;
  overflow-x: auto;
  padding-bottom: 4px;
}

.room-price-page__date-card {
  flex: 0 0 112px;
  display: grid;
  gap: 6px;
  padding: 12px;
  border: none;
  border-radius: 16px;
  background: rgba(16, 35, 63, 0.04);
  color: var(--app-muted);
  text-align: left;
}

.room-price-page__date-card.is-unavailable {
  background: rgba(244, 63, 94, 0.08);
}

.room-price-page__date-card.is-closed {
  border: 1px solid rgba(244, 63, 94, 0.18);
}

.room-price-page__date-card.is-manual {
  box-shadow: inset 0 0 0 1px rgba(37, 99, 235, 0.16);
}

.room-price-page__date-card strong,
.room-price-page__editor-title strong {
  color: var(--app-heading);
  font-size: 14px;
}

.room-price-page__date-card b {
  color: var(--ion-color-primary);
  font-size: 16px;
}

.room-price-page__date-card small {
  line-height: 1.5;
}

.room-price-page__modal-page {
  --padding-top: 16px;
  --padding-bottom: 24px;
  --padding-start: 16px;
  --padding-end: 16px;
}

.room-price-page__editor-title {
  display: grid;
  gap: 6px;
}

.room-price-page__editor-title span {
  color: var(--app-muted);
  font-size: 13px;
}

.room-price-page__editor-current {
  display: grid;
  gap: 8px;
  padding: 12px 14px;
  border-radius: 18px;
  background: rgba(16, 35, 63, 0.04);
}

.room-price-page__editor-current-header,
.room-price-page__weekday-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.room-price-page__editor-current-header span {
  color: var(--app-muted);
  font-size: 12px;
}

.room-price-page__bulk-section {
  display: grid;
  gap: 12px;
}

.room-price-page__bulk-section-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.room-price-page__selection-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.room-price-page__selection-chip {
  min-height: 40px;
  padding: 0 14px;
  border: 1px solid rgba(15, 23, 42, 0.12);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.96);
  color: var(--app-muted);
  font: inherit;
}

.room-price-page__selection-chip.is-active {
  border-color: var(--ion-color-primary);
  background: rgba(59, 130, 246, 0.14);
  color: var(--ion-color-primary);
  font-weight: 700;
}

.room-price-page__bulk-range-list {
  display: grid;
  gap: 12px;
}

.room-price-page__bulk-range-item {
  display: grid;
  gap: 10px;
  padding: 12px;
  border-radius: 18px;
  background: rgba(248, 250, 252, 0.82);
}

.room-price-page__weekday-section {
  display: grid;
  gap: 10px;
}

.room-price-page__weekday-actions,
.room-price-page__tag-row {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.room-price-page__tag-row span {
  padding: 2px 8px;
  border-radius: 999px;
  background: var(--app-primary-soft);
  color: var(--ion-color-primary);
  font-size: 11px;
  font-weight: 600;
}

.room-price-page__weekday-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.room-price-page__weekday-button {
  min-height: 40px;
  padding: 0 14px;
  border: 1px solid rgba(15, 23, 42, 0.12);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.92);
  color: var(--app-muted);
  font: inherit;
}

.room-price-page__weekday-button.is-active {
  border-color: var(--app-border-strong);
  background: var(--app-primary-soft-strong);
  color: var(--ion-color-primary);
  font-weight: 700;
}

.room-price-page__weekday-hint {
  margin: 0;
}

.room-price-page__toggle-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 12px 14px;
  border-radius: 18px;
  background: var(--app-primary-soft);
}

.room-price-page__toggle-row strong,
.room-price-page__toggle-row p {
  margin: 0;
}

.room-price-page__toggle-row p {
  margin-top: 6px;
  color: var(--app-muted);
  font-size: 12px;
}

.room-price-page__editor-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  flex-wrap: wrap;
}

@media (max-width: 520px) {
  .room-price-page__filter-grid {
    grid-template-columns: minmax(0, 1fr);
  }

  .room-price-page__matrix-leading,
  .room-price-page__matrix-header-cell,
  .room-price-page__matrix-cell {
    min-height: 84px;
    padding: 8px;
  }
}
</style>
