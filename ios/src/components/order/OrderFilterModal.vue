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
            <p class="filter-modal__subtitle">{{ $t('order.mobile.filterDescription') }}</p>
          </div>
          <button type="button" class="filter-modal__close" @click="$emit('dismiss')">
            {{ $t('iosStage5.roomStatus.close') }}
          </button>
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

        <section v-if="activePanel === 'date' && isDateRangeTab" class="filter-modal__section">
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
              <span>{{ $t('accommodation.common.startDate') }}</span>
              <ion-input v-model="form.startDate" type="date" fill="outline" />
            </label>
            <label class="filter-modal__date-field">
              <span>{{ $t('accommodation.common.endDate') }}</span>
              <ion-input v-model="form.endDate" type="date" fill="outline" />
            </label>
          </div>
        </section>
        <section v-else-if="activePanel === 'date' && isOperationDateTab" class="filter-modal__section">
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
              <span>{{ operationDateLabel }}</span>
              <ion-input v-model="form.operationDate" type="date" fill="outline" />
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
              {{ $t('order.mobile.allRooms') }}
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
              {{ $t('order.mobile.allChannels') }}
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
            {{ $t('accommodation.common.reset') }}
          </ion-button>
          <ion-button expand="block" class="filter-modal__footer-btn filter-modal__footer-btn--primary" @click="handleApply">
            {{ $t('common.confirm') }}
          </ion-button>
        </footer>
      </div>
    </ion-content>
  </ion-modal>
</template>

<script setup lang="ts">
import { IonButton, IonContent, IonInput, IonModal } from '@ionic/vue'
import { computed, nextTick, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import {
  createDefaultOrderFilters,
  getOrderTabLabel,
  type OrderFilterForm,
  type OrderOptionItem,
  type OrderTabValue,
} from '@/components/order/orderUtils'
import { getStoreTodayDate, shiftBusinessDate } from '@/utils/storeBusinessDate'

type FilterPanelKey = 'date' | 'roomType' | 'channel' | 'status' | 'paymentStatus' | 'checkinType'
type DateShortcutKey = 'yesterday' | 'today' | 'tomorrow' | 'recent7' | 'recent30'
type MultiSelectField = 'roomType' | 'channel'

const props = defineProps<{
  isOpen: boolean
  activeTab: OrderTabValue
  channels: OrderOptionItem[]
  roomTypes: OrderOptionItem[]
  initialFilters: OrderFilterForm
  initialPanel?: FilterPanelKey
}>()
const { t } = useI18n()

const emit = defineEmits<{
  dismiss: []
  apply: [filters: OrderFilterForm]
  reset: []
}>()

const form = ref<OrderFilterForm>(createDefaultOrderFilters())
const activePanel = ref<FilterPanelKey>('date')
const activeDateShortcut = ref<DateShortcutKey | ''>('')
let isApplyingDateShortcut = false

const isDateRangeTab = computed(() => props.activeTab === 'all')
const isOperationDateTab = computed(() => {
  return ['today-checkin', 'today-checkout', 'today-new'].includes(props.activeTab)
})
const showDatePanel = computed(() => isDateRangeTab.value || isOperationDateTab.value)

const operationDateLabel = computed(() => {
  if (props.activeTab === 'today-checkin') {
    return t('order.filters.todayCheckinDate')
  }
  if (props.activeTab === 'today-checkout') {
    return t('order.filters.todayCheckoutDate')
  }
  if (props.activeTab === 'today-new') {
    return t('order.filters.todayNewDate')
  }
  return t('order.filters.operationDate')
})

const panelTabs = computed<Array<{ key: FilterPanelKey; label: string }>>(() => [
  ...(showDatePanel.value
    ? [{
        key: 'date' as FilterPanelKey,
        label: isDateRangeTab.value ? t('order.filters.createdAt') : operationDateLabel.value,
      }]
    : []),
  { key: 'roomType', label: t('order.filters.roomType') },
  { key: 'channel', label: t('order.filters.channel') },
  { key: 'status', label: t('order.filters.stayStatus') },
  { key: 'paymentStatus', label: t('order.table.roomFee') },
  { key: 'checkinType', label: t('order.filters.checkinType') },
])

const dateShortcutOptions = computed<Array<{ label: string; value: DateShortcutKey }>>(() => [
  { label: t('order.mobile.yesterday'), value: 'yesterday' },
  { label: t('order.mobile.today'), value: 'today' },
  { label: t('order.mobile.tomorrow'), value: 'tomorrow' },
  ...(isDateRangeTab.value
    ? [
        { label: t('order.mobile.recent7Days'), value: 'recent7' as DateShortcutKey },
        { label: t('order.mobile.recent30Days'), value: 'recent30' as DateShortcutKey },
      ]
    : []),
])

const statusOptions = computed(() => [
  { label: t('order.mobile.allStatus'), value: '' },
  { label: t('order.options.checkedIn'), value: 'checked-in' },
  { label: t('order.options.notCheckedIn'), value: 'not-checked-in' },
  { label: t('order.options.checkedOut'), value: 'checked-out' },
])

const paymentStatusOptions = computed(() => [
  { label: t('order.mobile.allRoomFees'), value: '' },
  { label: t('order.options.paid'), value: 'paid' },
  { label: t('order.options.unpaid'), value: 'unpaid' },
])

const checkinTypeOptions = computed(() => [
  { label: t('order.mobile.allStayTypes'), value: '' },
  { label: t('order.options.normalCheckin'), value: 'normal' },
  { label: t('order.options.earlyCheckin'), value: 'early' },
  { label: t('order.options.lateCheckin'), value: 'late' },
])

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
    operationDate: filters.operationDate,
  }
}

function resolvePanel(panel?: FilterPanelKey) {
  if (panel === 'date' && !showDatePanel.value) {
    return 'roomType'
  }
  return panel || (showDatePanel.value ? 'date' : 'roomType')
}

function resetForm() {
  form.value = cloneFilters(props.initialFilters)
  activeDateShortcut.value = ''
  activePanel.value = resolvePanel(props.initialPanel)
}

function applyDateShortcut(type: DateShortcutKey) {
  isApplyingDateShortcut = true
  const today = getStoreTodayDate()
  let startDate = today
  let endDate = today

  if (type === 'yesterday') {
    startDate = shiftBusinessDate(today, -1)
    endDate = startDate
  } else if (type === 'tomorrow') {
    startDate = shiftBusinessDate(today, 1)
    endDate = startDate
  } else if (type === 'recent7') {
    startDate = shiftBusinessDate(today, -6)
  } else if (type === 'recent30') {
    startDate = shiftBusinessDate(today, -29)
  }

  if (isOperationDateTab.value) {
    form.value.operationDate = startDate
  } else {
    form.value.startDate = startDate
    form.value.endDate = endDate
  }

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
  if (isDateRangeTab.value) {
    if (form.value.startDate && !form.value.endDate) {
      form.value.endDate = form.value.startDate
    } else if (!form.value.startDate && form.value.endDate) {
      form.value.startDate = form.value.endDate
    }
  } else if (isOperationDateTab.value) {
    form.value.startDate = ''
    form.value.endDate = ''
  }

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
    activePanel.value = resolvePanel(panel)
  },
)

watch(
  () => [form.value.startDate, form.value.endDate, form.value.operationDate],
  () => {
    if (isApplyingDateShortcut) {
      return
    }
    activeDateShortcut.value = ''
  },
)

watch(
  () => props.activeTab,
  () => {
    activePanel.value = resolvePanel(activePanel.value)
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
