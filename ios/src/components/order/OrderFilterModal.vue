<template>
  <ion-modal
    :is-open="isOpen"
    :initial-breakpoint="0.88"
    :breakpoints="[0, 0.88]"
    class="order-filter-modal"
    @didDismiss="$emit('dismiss')"
  >
    <ion-content class="filter-modal">
      <div class="filter-modal__sheet">

        <section class="filter-modal__header">
          <div class="filter-modal__context">
            <p class="filter-modal__title">{{ activeTabLabel }}</p>
            <p class="filter-modal__subtitle">按条件组合筛选当前订单列表</p>
          </div>
          <button type="button" class="filter-modal__close" @click="$emit('dismiss')">关闭</button>
        </section>

        <section class="filter-modal__anchors">
          <button
            v-for="item in panelTabs"
            :key="item.key"
            type="button"
            class="filter-modal__anchor"
            :class="{ 'is-active': activePanel === item.key }"
            @click="activePanel = item.key"
          >
            {{ item.label }}
          </button>
        </section>

        <section v-if="activePanel === 'date'" class="filter-modal__section">
          <div class="filter-modal__grid filter-modal__grid--tight">
            <button
              v-for="item in dateTypeOptions"
              :key="item.value"
              type="button"
              class="filter-modal__pill"
              :class="{ 'is-active': dateType === item.value }"
              @click="dateType = item.value"
            >
              {{ item.label }}
            </button>
          </div>

          <div class="filter-modal__grid filter-modal__grid--tight filter-modal__grid--stacked">
            <button
              v-for="item in dateShortcutOptions"
              :key="item.value"
              type="button"
              class="filter-modal__pill"
              :class="{ 'is-active': activeDateShortcut === item.value }"
              @click="applyDateShortcut(item.value)"
            >
              {{ item.label }}
            </button>
          </div>

          <div class="filter-modal__date-inputs">
            <label class="filter-modal__date-field">
              <span>开始日期</span>
              <ion-input v-model="form.startDate" type="date" fill="outline" />
            </label>
            <label class="filter-modal__date-field">
              <span>结束日期</span>
              <ion-input v-model="form.endDate" type="date" fill="outline" />
            </label>
          </div>
        </section>

        <section v-else-if="activePanel === 'roomType'" class="filter-modal__section">
          <div class="filter-modal__grid">
            <button
              type="button"
              class="filter-modal__pill"
              :class="{ 'is-active': form.roomType.length === 0 }"
              @click="form.roomType = []"
            >
              全部房间
            </button>
            <button
              v-for="item in roomTypes"
              :key="item.value"
              type="button"
              class="filter-modal__pill"
              :class="{ 'is-active': form.roomType.includes(item.value) }"
              @click="toggleMultiSelectValue('roomType', item.value)"
            >
              {{ item.label }}
            </button>
          </div>
        </section>

        <section v-else-if="activePanel === 'channel'" class="filter-modal__section">
          <div class="filter-modal__grid">
            <button
              type="button"
              class="filter-modal__pill"
              :class="{ 'is-active': form.channel.length === 0 }"
              @click="form.channel = []"
            >
              全部渠道
            </button>
            <button
              v-for="item in channels"
              :key="item.value"
              type="button"
              class="filter-modal__pill"
              :class="{ 'is-active': form.channel.includes(item.value) }"
              @click="toggleMultiSelectValue('channel', item.value)"
            >
              {{ item.label }}
            </button>
          </div>
        </section>

        <section v-else-if="activePanel === 'status'" class="filter-modal__section">
          <div class="filter-modal__grid filter-modal__grid--tight">
            <button
              v-for="item in statusOptions"
              :key="item.value"
              type="button"
              class="filter-modal__pill"
              :class="{ 'is-active': form.status === item.value }"
              @click="form.status = item.value"
            >
              {{ item.label }}
            </button>
          </div>
        </section>

        <section v-else-if="activePanel === 'paymentStatus'" class="filter-modal__section">
          <div class="filter-modal__grid filter-modal__grid--tight">
            <button
              v-for="item in paymentStatusOptions"
              :key="item.value"
              type="button"
              class="filter-modal__pill"
              :class="{ 'is-active': form.paymentStatus === item.value }"
              @click="form.paymentStatus = item.value"
            >
              {{ item.label }}
            </button>
          </div>
        </section>

        <section v-else class="filter-modal__section">
          <div class="filter-modal__grid filter-modal__grid--tight">
            <button
              v-for="item in checkinTypeOptions"
              :key="item.value"
              type="button"
              class="filter-modal__pill"
              :class="{ 'is-active': form.checkinType === item.value }"
              @click="form.checkinType = item.value"
            >
              {{ item.label }}
            </button>
          </div>
        </section>

        <footer class="filter-modal__footer">
          <ion-button expand="block" fill="outline" class="filter-modal__footer-btn" @click="handleReset">
            重置
          </ion-button>
          <ion-button expand="block" class="filter-modal__footer-btn filter-modal__footer-btn--primary" @click="handleApply">
            确认
          </ion-button>
        </footer>
      </div>
    </ion-content>
  </ion-modal>
</template>

<script setup lang="ts">
import { IonButton, IonContent, IonInput, IonModal } from '@ionic/vue'
import { computed, nextTick, ref, watch } from 'vue'
import {
  createDefaultOrderFilters,
  getOrderTabLabel,
  type OrderFilterForm,
  type OrderOptionItem,
  type OrderTabValue,
} from '@/components/order/orderUtils'

type FilterPanelKey = 'date' | 'roomType' | 'channel' | 'status' | 'paymentStatus' | 'checkinType'
type DateShortcutKey = 'yesterday' | 'today' | 'tomorrow' | 'recent7' | 'recent30'
type DateTypeKey = 'checkin' | 'checkout' | 'created'
type MultiSelectField = 'roomType' | 'channel'

const props = defineProps<{
  isOpen: boolean
  activeTab: OrderTabValue
  channels: OrderOptionItem[]
  roomTypes: OrderOptionItem[]
  initialFilters: OrderFilterForm
  initialPanel?: FilterPanelKey
}>()

const emit = defineEmits<{
  dismiss: []
  apply: [filters: OrderFilterForm]
  reset: []
}>()

const form = ref<OrderFilterForm>(createDefaultOrderFilters())
const activePanel = ref<FilterPanelKey>('date')
const dateType = ref<DateTypeKey>('checkout')
const activeDateShortcut = ref<DateShortcutKey | ''>('')
let isApplyingDateShortcut = false

const panelTabs: Array<{ key: FilterPanelKey; label: string }> = [
  { key: 'date', label: '时间' },
  { key: 'roomType', label: '房间' },
  { key: 'channel', label: '渠道' },
  { key: 'status', label: '入住状态' },
  { key: 'paymentStatus', label: '房费' },
  { key: 'checkinType', label: '入住类型' },
]

const dateTypeOptions: Array<{ label: string; value: DateTypeKey }> = [
  { label: '入住时间', value: 'checkin' },
  { label: '退房时间', value: 'checkout' },
  { label: '下单时间', value: 'created' },
]

const dateShortcutOptions: Array<{ label: string; value: DateShortcutKey }> = [
  { label: '昨日', value: 'yesterday' },
  { label: '今天', value: 'today' },
  { label: '明日', value: 'tomorrow' },
  { label: '最近7天', value: 'recent7' },
  { label: '最近30天', value: 'recent30' },
]

const statusOptions = [
  { label: '全部状态', value: '' },
  { label: '已入住', value: 'checked-in' },
  { label: '未入住', value: 'not-checked-in' },
  { label: '已退房', value: 'checked-out' },
]

const paymentStatusOptions = [
  { label: '全部房费', value: '' },
  { label: '已结清', value: 'paid' },
  { label: '未结清', value: 'unpaid' },
]

const checkinTypeOptions = [
  { label: '全部类型', value: '' },
  { label: '正常入住', value: 'normal' },
  { label: '提前入住', value: 'early' },
  { label: '延迟入住', value: 'late' },
]

const activeTabLabel = computed(() => getOrderTabLabel(props.activeTab))

function cloneFilters(filters: OrderFilterForm): OrderFilterForm {
  return {
    channel: [...filters.channel],
    roomType: [...filters.roomType],
    checkinType: filters.checkinType,
    status: filters.status,
    paymentStatus: filters.paymentStatus,
    startDate: filters.startDate,
    endDate: filters.endDate,
  }
}

function resetForm() {
  form.value = cloneFilters(props.initialFilters)
  activeDateShortcut.value = ''
  activePanel.value = props.initialPanel || 'date'
}

function formatDate(date: Date) {
  const year = date.getFullYear()
  const month = `${date.getMonth() + 1}`.padStart(2, '0')
  const day = `${date.getDate()}`.padStart(2, '0')
  return `${year}-${month}-${day}`
}

function applyDateShortcut(type: DateShortcutKey) {
  isApplyingDateShortcut = true
  const today = new Date()
  const start = new Date(today)
  const end = new Date(today)

  if (type === 'yesterday') {
    start.setDate(today.getDate() - 1)
    end.setDate(today.getDate() - 1)
  } else if (type === 'tomorrow') {
    start.setDate(today.getDate() + 1)
    end.setDate(today.getDate() + 1)
  } else if (type === 'recent7') {
    start.setDate(today.getDate() - 6)
  } else if (type === 'recent30') {
    start.setDate(today.getDate() - 29)
  }

  form.value.startDate = formatDate(start)
  form.value.endDate = formatDate(end)
  activeDateShortcut.value = type
  void nextTick(() => {
    isApplyingDateShortcut = false
  })
}

function toggleMultiSelectValue(field: MultiSelectField, value: string) {
  const nextValues = [...form.value[field]]
  const matchedIndex = nextValues.indexOf(value)
  if (matchedIndex >= 0) {
    nextValues.splice(matchedIndex, 1)
  } else {
    nextValues.push(value)
  }
  form.value[field] = nextValues
}

function handleApply() {
  emit('apply', cloneFilters(form.value))
}

function handleReset() {
  form.value = createDefaultOrderFilters()
  activeDateShortcut.value = ''
  emit('reset')
}

watch(
  () => props.isOpen,
  (isOpen) => {
    if (isOpen) {
      resetForm()
    }
  },
)

watch(
  () => props.initialPanel,
  (panel) => {
    if (panel) {
      activePanel.value = panel
    }
  },
)

watch(
  () => [form.value.startDate, form.value.endDate],
  () => {
    if (isApplyingDateShortcut) {
      return
    }
    activeDateShortcut.value = ''
  },
)
</script>

<style scoped>
.order-filter-modal {
  --border-radius: 26px 26px 0 0;
}

.filter-modal {
  --background: transparent;
}

.filter-modal__sheet {
  min-height: 100%;
  padding: 10px 18px calc(24px + env(safe-area-inset-bottom));
  background: #ffffff;
}

.filter-modal__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.filter-modal__context {
  min-width: 0;
}

.filter-modal__title {
  margin: 0;
  color: #161922;
  font-size: 17px;
  font-weight: 600;
}

.filter-modal__subtitle {
  margin: 6px 0 0;
  color: #9aa1ad;
  font-size: 13px;
}

.filter-modal__close {
  border: 0;
  background: transparent;
  color: #8a90a0;
  font: inherit;
  font-size: 15px;
}

.filter-modal__anchors {
  display: flex;
  gap: 10px;
  overflow-x: auto;
  margin: 18px -2px 6px;
  padding: 0 2px;
  scrollbar-width: none;
}

.filter-modal__anchors::-webkit-scrollbar {
  display: none;
}

.filter-modal__anchor {
  flex: 0 0 auto;
  min-height: 40px;
  padding: 0 16px;
  border: 1px solid #eceff4;
  border-radius: 16px;
  background: #ffffff;
  color: #646b79;
  font: inherit;
  font-size: 15px;
  font-weight: 500;
}

.filter-modal__anchor.is-active {
  border-color: #55cfaf;
  color: #37b393;
  box-shadow: inset 0 0 0 1px rgba(85, 207, 175, 0.24);
}

.filter-modal__section {
  padding-top: 16px;
}

.filter-modal__grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.filter-modal__grid--tight {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.filter-modal__grid--stacked {
  margin-top: 12px;
}

.filter-modal__pill {
  min-height: 44px;
  padding: 0 14px;
  border: 1px solid #eceff4;
  border-radius: 16px;
  background: #ffffff;
  color: #646b79;
  font: inherit;
  font-size: 15px;
  font-weight: 500;
  text-align: center;
}

.filter-modal__pill.is-active {
  border-color: #4a98ff;
  color: #4a98ff;
  box-shadow: inset 0 0 0 1px rgba(74, 152, 255, 0.18);
}

.filter-modal__date-inputs {
  display: grid;
  gap: 12px;
  margin-top: 14px;
}

.filter-modal__date-field {
  display: grid;
  gap: 8px;
  color: #7a8190;
  font-size: 13px;
}

.filter-modal__date-field ion-input {
  --background: #ffffff;
  --border-radius: 18px;
  --padding-start: 14px;
  --padding-end: 14px;
  min-height: 48px;
  border-radius: 18px;
}

.filter-modal__footer {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14px;
  margin-top: 28px;
}

.filter-modal__footer-btn {
  --border-radius: 18px;
  min-height: 52px;
  font-weight: 500;
}

.filter-modal__footer-btn--primary {
  --background: #2f80ed;
  --background-activated: #2b75d7;
  --background-hover: #2b75d7;
}
</style>
