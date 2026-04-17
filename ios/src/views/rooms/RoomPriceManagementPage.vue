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
        <p class="mobile-subtitle">设置页继续负责价格计划主数据，这里聚焦日期价格、入住限制与运营态控制。</p>
        <div class="mobile-chip-row">
          <span class="mobile-chip">起始日 {{ formatDate(selectedDate) }}</span>
          <span class="mobile-chip">连续 7 天</span>
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

          <p v-if="errorMessage" class="mobile-note room-price-page__error">{{ errorMessage }}</p>
        </section>

        <section class="mobile-card">
          <div class="mobile-inline-row room-price-page__section-header">
            <div>
              <h2 class="mobile-section-title">房型价格卡</h2>
              <p class="mobile-note">按房型聚合价格计划，点击任意日期卡即可打开编辑面板。</p>
            </div>
            <ion-spinner v-if="loading" name="crescent" />
          </div>

          <div class="mobile-chip-row room-price-page__summary-row">
            <span class="mobile-chip">房型 {{ summaryMetrics.roomTypeCount }} 个</span>
            <span class="mobile-chip">价格计划 {{ summaryMetrics.planCount }} 个</span>
            <span class="mobile-chip">售罄/关房 {{ summaryMetrics.unavailableCellCount }} 天</span>
          </div>

          <div v-if="groupedRecords.length > 0" class="mobile-list room-price-page__group-list">
            <article v-for="roomGroup in groupedRecords" :key="roomGroup.roomTypeId" class="room-price-page__group-card">
              <div class="room-price-page__group-header">
                <div>
                  <strong>{{ roomGroup.roomTypeName }}</strong>
                  <p>{{ roomGroup.roomTypeCode || '未设置编码' }} · 价格计划 {{ roomGroup.plans.length }} 个</p>
                </div>
              </div>

              <div class="mobile-list room-price-page__plan-list">
                <article v-for="plan in roomGroup.plans" :key="plan.key" class="room-price-page__plan-card">
                  <div class="room-price-page__plan-header">
                    <div>
                      <strong>{{ plan.pricePlanName }}</strong>
                      <p>已按所选日期窗口生成移动端 7 日价格卡。</p>
                    </div>
                  </div>

                  <div class="room-price-page__date-strip">
                    <button
                      v-for="cell in plan.cells"
                      :key="`${plan.key}-${cell.date}`"
                      class="room-price-page__date-card"
                      :class="getDateCardClass(cell.record)"
                      type="button"
                      @click="handleOpenEditor(roomGroup, plan, cell)"
                    >
                      <strong>{{ cell.shortLabel }}</strong>
                      <span>{{ cell.weekday }}</span>
                      <b>{{ getDisplayPriceText(cell.record) }}</b>
                      <small>{{ getAvailabilityText(cell.record) }}</small>
                      <small>{{ getStayRestrictionText(cell.record) }}</small>
                      <div v-if="getStatusTags(cell.record).length > 0" class="room-price-page__tag-row">
                        <span v-for="tag in getStatusTags(cell.record)" :key="`${cell.date}-${tag}`">{{ tag }}</span>
                      </div>
                      <small>{{ getPriceSourceText(cell.record) }}</small>
                      <small v-if="cell.record?.manualOverrideUntil">覆盖至 {{ formatDate(cell.record.manualOverrideUntil) }}</small>
                      <small v-if="cell.record?.priceLabsBasePrice !== undefined && cell.record?.priceLabsBasePrice !== null">
                        PriceLabs 基价 {{ formatCurrency(cell.record.priceLabsBasePrice) }}
                      </small>
                      <small v-if="cell.record?.priceLabsUpdatedAt">
                        PriceLabs {{ formatDateTime(cell.record.priceLabsUpdatedAt) }}
                      </small>
                    </button>
                  </div>
                </article>
              </div>
            </article>
          </div>

          <p v-else-if="!loading" class="mobile-note">当前日期范围暂无可展示的运营房价数据。</p>
        </section>

        <section class="mobile-card">
          <h2 class="mobile-section-title">首版说明</h2>
          <ul class="mobile-bullet-list">
            <li>移动端先聚焦单元格改价闭环，不搬运桌面 17 列密集矩阵。</li>
            <li>批量更新与导出能力暂不首发，后续可在当前结构上继续扩展。</li>
            <li>价格计划本体与房型周价维护仍保留在设置页。</li>
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
  getRoomPriceManagementData,
  updatePriceByPlan,
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

const DATE_WINDOW_DAYS = 7
const FULL_WEEKDAY_VALUES = [1, 2, 3, 4, 5, 6, 7]
const ALL_WEEKDAY_VALUES = [0, ...FULL_WEEKDAY_VALUES]
const MAX_STAY_LIMIT = 99

const router = useRouter()
const userStore = useUserStore()

const selectedDate = ref(getTodayDate())
const selectedRoomTypeId = ref<number | null>(null)
const selectedRoomGroupId = ref<number | null>(null)
const roomTypes = ref<RoomTypeDTO[]>([])
const roomGroups = ref<RoomGroupOption[]>([])
const roomGroupRoomTypeIdsMap = ref<Record<number, number[]>>({})
const records = ref<RoomPriceManagementDTO[]>([])
const loading = ref(false)
const saving = ref(false)
const editorOpen = ref(false)
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

const dateWindow = computed(() => {
  return buildDateWindow(selectedDate.value, DATE_WINDOW_DAYS)
})

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
        const matchedRecord = filteredRecords.value.find((item) => {
          const itemPricePlanId = item.pricePlanId ?? 0
          return item.roomTypeId === group.roomTypeId && itemPricePlanId === plan.pricePlanId && item.priceDate === day.date
        })

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

function getDateCardClass(record: RoomPriceManagementDTO | null) {
  return {
    'is-unavailable': isUnavailableRecord(record),
    'is-closed': Boolean(record?.closeRoom),
    'is-manual': Boolean(record?.manualOverride),
  }
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
  const endDate = shiftDate(selectedDate.value, DATE_WINDOW_DAYS - 1)
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

function handleOpenEditor(roomGroup: RoomPriceGroupView, plan: PricePlanView, cell: PriceCellView) {
  editorRecord.value = cell.record
  editorForm.value = {
    roomTypeId: roomGroup.roomTypeId,
    pricePlanId: plan.pricePlanId,
    roomTypeName: roomGroup.roomTypeName,
    pricePlanName: plan.pricePlanName,
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
.room-price-page__field select {
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

.room-price-page__error {
  color: var(--ion-color-danger);
}

.room-price-page__section-header {
  align-items: flex-start;
}

.room-price-page__summary-row {
  margin-top: 12px;
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
}
</style>
