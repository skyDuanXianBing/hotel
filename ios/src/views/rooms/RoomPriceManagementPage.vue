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
                      type="button"
                      @click="handleOpenEditor(roomGroup, plan, cell)"
                    >
                      <strong>{{ cell.shortLabel }}</strong>
                      <span>{{ cell.weekday }}</span>
                      <b>{{ formatCurrency(cell.record?.price) }}</b>
                      <small>最小入住 {{ cell.record?.minStay ?? 1 }}</small>
                      <small>{{ cell.record?.priceSource || '默认来源' }}</small>
                      <small v-if="cell.record?.manualOverride">手动覆盖</small>
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
              <span>适用星期</span>
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
import { getRoomPriceManagementData, updatePriceByPlan, type RoomPriceManagementDTO } from '@/api/roomPrice'
import { getAllRoomTypes, type RoomTypeDTO } from '@/api/roomType'
import { ROUTE_PATHS } from '@/router/guards'
import { useUserStore } from '@/stores/user'
import { buildDateWindow, formatCurrency, formatDate, getTodayDate, resolveUserLabel, shiftDate } from '@/utils/accommodation'
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

const router = useRouter()
const userStore = useUserStore()

const selectedDate = ref(getTodayDate())
const selectedRoomTypeId = ref<number | null>(null)
const roomTypes = ref<RoomTypeDTO[]>([])
const records = ref<RoomPriceManagementDTO[]>([])
const loading = ref(false)
const saving = ref(false)
const editorOpen = ref(false)
const errorMessage = ref('')

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

const groupedRecords = computed<RoomPriceGroupView[]>(() => {
  const roomMap = new Map<number, RoomPriceGroupView>()

  for (const record of records.value) {
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
        const matchedRecord = records.value.find((item) => {
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

function resolveWarningMessage(error: unknown, fallbackMessage: string) {
  if (error instanceof Error && error.message) {
    return error.message
  }
  return fallbackMessage
}

async function loadRoomTypes() {
  const response = await getAllRoomTypes()
  if (!response.success || !response.data) {
    throw new Error(response.message || '加载房型失败')
  }
  roomTypes.value = response.data
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

async function loadPageData() {
  loading.value = true
  errorMessage.value = ''

  try {
    await Promise.all([loadRoomTypes(), loadPriceData()])
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
}

function isWeekdaySelected(value: number) {
  return editorForm.value.weekdays.includes(value)
}

function handleToggleWeekday(value: number) {
  const selectedValues = [...editorForm.value.weekdays]
  const isSelected = selectedValues.includes(value)

  if (value === 0) {
    if (isSelected) {
      editorForm.value.weekdays = []
      return
    }

    editorForm.value.weekdays = [0, 1, 2, 3, 4, 5, 6, 7]
    return
  }

  const nextValues = selectedValues.filter((item) => item !== 0 && item !== value)
  if (!isSelected) {
    nextValues.push(value)
  }

  const hasFullWeek = [1, 2, 3, 4, 5, 6, 7].every((item) => nextValues.includes(item))
  if (hasFullWeek) {
    editorForm.value.weekdays = [0, 1, 2, 3, 4, 5, 6, 7]
    return
  }

  nextValues.sort((left, right) => left - right)
  editorForm.value.weekdays = nextValues
}

function handleOpenEditor(roomGroup: RoomPriceGroupView, plan: PricePlanView, cell: PriceCellView) {
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

async function handleSaveEditor() {
  if (!editorForm.value.roomTypeId || !editorForm.value.pricePlanId) {
    showWarningToast('当前价格计划信息不完整，无法保存')
    return
  }

  if (!editorForm.value.startDate || !editorForm.value.endDate) {
    showWarningToast('请选择完整日期范围')
    return
  }

  const requestData = {
    roomTypeId: editorForm.value.roomTypeId,
    pricePlanId: editorForm.value.pricePlanId,
    startDate: editorForm.value.startDate,
    endDate: editorForm.value.endDate,
    weekdays: editorForm.value.weekdays.length > 0 ? editorForm.value.weekdays : undefined,
  } as {
    roomTypeId: number
    pricePlanId: number
    startDate: string
    endDate: string
    weekdays?: number[]
    price?: number
    minStay?: number
    maxStay?: number
    closeRoom?: boolean
    cta?: boolean
    ctd?: boolean
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
    if (!Number.isFinite(minStay) || minStay <= 0) {
      showWarningToast('请输入有效的最小入住天数')
      return
    }
    requestData.minStay = minStay
  }

  if (editorForm.value.settingType === 'maxStay') {
    const maxStay = Number(editorForm.value.maxStay)
    if (!Number.isFinite(maxStay) || maxStay <= 0) {
      showWarningToast('请输入有效的最大入住天数')
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

    showSuccessToast('房价设置已更新')
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

async function handleRefresh(event: CustomEvent) {
  await loadPageData()
  event.detail.complete()
}

async function handleOpenHistory() {
  await router.push(ROUTE_PATHS.roomsPricingHistory)
}

onIonViewWillEnter(async () => {
  await loadPageData()
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
  gap: 4px;
  padding: 12px;
  border: none;
  border-radius: 16px;
  background: rgba(16, 35, 63, 0.04);
  color: var(--app-muted);
  text-align: left;
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

.room-price-page__weekday-section {
  display: grid;
  gap: 10px;
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
