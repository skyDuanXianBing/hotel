<template>
  <ion-modal :is-open="isOpen" @didDismiss="$emit('dismiss')">
    <ion-header translucent>
      <ion-toolbar>
        <ion-title>筛选订单</ion-title>
        <ion-buttons slot="end">
          <ion-button @click="$emit('dismiss')">关闭</ion-button>
        </ion-buttons>
      </ion-toolbar>
    </ion-header>

    <ion-content>
      <section class="filter-modal__hero">
        <strong>{{ activeTabLabel }}</strong>
      </section>

      <ion-list inset>
        <ion-item>
          <ion-select
            v-model="form.channel"
            interface="action-sheet"
            label="渠道"
            label-placement="stacked"
            placeholder="全部渠道"
          >
            <ion-select-option value="">全部渠道</ion-select-option>
            <ion-select-option v-for="item in channels" :key="item.value" :value="item.value">
              {{ item.label }}
            </ion-select-option>
          </ion-select>
        </ion-item>

        <ion-item>
          <ion-select
            v-model="form.roomType"
            interface="action-sheet"
            label="房型"
            label-placement="stacked"
            placeholder="全部房型"
          >
            <ion-select-option value="">全部房型</ion-select-option>
            <ion-select-option v-for="item in roomTypes" :key="item.value" :value="item.value">
              {{ item.label }}
            </ion-select-option>
          </ion-select>
        </ion-item>

        <ion-item>
          <ion-select
            v-model="form.checkinType"
            interface="action-sheet"
            label="入住类型"
            label-placement="stacked"
            placeholder="全部类型"
          >
            <ion-select-option value="">全部类型</ion-select-option>
            <ion-select-option value="normal">正常入住</ion-select-option>
            <ion-select-option value="early">提前入住</ion-select-option>
            <ion-select-option value="late">延迟入住</ion-select-option>
          </ion-select>
        </ion-item>

        <ion-item>
          <ion-select
            v-model="form.status"
            interface="action-sheet"
            label="入住状态"
            label-placement="stacked"
            placeholder="全部状态"
          >
            <ion-select-option value="">全部状态</ion-select-option>
            <ion-select-option value="checked-in">已入住</ion-select-option>
            <ion-select-option value="not-checked-in">未入住</ion-select-option>
            <ion-select-option value="checked-out">已退房</ion-select-option>
          </ion-select>
        </ion-item>

        <ion-item>
          <ion-select
            v-model="form.paymentStatus"
            interface="action-sheet"
            label="结账状态"
            label-placement="stacked"
            placeholder="全部状态"
          >
            <ion-select-option value="">全部状态</ion-select-option>
            <ion-select-option value="paid">已结账</ion-select-option>
            <ion-select-option value="unpaid">未结账 / 部分结账</ion-select-option>
          </ion-select>
        </ion-item>

        <ion-item>
          <ion-input
            v-model="form.startDate"
            type="date"
            label="创建开始日期"
            label-placement="stacked"
          />
        </ion-item>

        <ion-item>
          <ion-input
            v-model="form.endDate"
            type="date"
            label="创建结束日期"
            label-placement="stacked"
          />
        </ion-item>
      </ion-list>
    </ion-content>

    <ion-footer>
      <ion-toolbar>
        <div class="filter-modal__footer">
          <ion-button fill="outline" @click="handleReset">重置</ion-button>
          <ion-button @click="handleApply">应用筛选</ion-button>
        </div>
      </ion-toolbar>
    </ion-footer>
  </ion-modal>
</template>

<script setup lang="ts">
import {
  IonButton,
  IonButtons,
  IonContent,
  IonFooter,
  IonHeader,
  IonInput,
  IonItem,
  IonList,
  IonModal,
  IonSelect,
  IonSelectOption,
  IonTitle,
  IonToolbar,
} from '@ionic/vue'
import { computed, ref, watch } from 'vue'
import {
  createDefaultOrderFilters,
  getOrderTabLabel,
  type OrderFilterForm,
  type OrderOptionItem,
  type OrderTabValue,
} from '@/components/order/orderUtils'

const props = defineProps<{
  isOpen: boolean
  activeTab: OrderTabValue
  channels: OrderOptionItem[]
  roomTypes: OrderOptionItem[]
  initialFilters: OrderFilterForm
}>()

const emit = defineEmits<{
  dismiss: []
  apply: [filters: OrderFilterForm]
  reset: []
}>()

const form = ref<OrderFilterForm>(createDefaultOrderFilters())

const activeTabLabel = computed(() => {
  return `当前视图：${getOrderTabLabel(props.activeTab)}`
})

function resetForm() {
  form.value = {
    channel: props.initialFilters.channel,
    roomType: props.initialFilters.roomType,
    checkinType: props.initialFilters.checkinType,
    status: props.initialFilters.status,
    paymentStatus: props.initialFilters.paymentStatus,
    startDate: props.initialFilters.startDate,
    endDate: props.initialFilters.endDate,
  }
}

function handleApply() {
  emit('apply', {
    channel: form.value.channel,
    roomType: form.value.roomType,
    checkinType: form.value.checkinType,
    status: form.value.status,
    paymentStatus: form.value.paymentStatus,
    startDate: form.value.startDate,
    endDate: form.value.endDate,
  })
}

function handleReset() {
  form.value = createDefaultOrderFilters()
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
</script>

<style scoped>
.filter-modal__hero {
  margin: 16px;
  padding: 16px;
  border-radius: 20px;
  background: var(--app-primary-soft);
}

.filter-modal__hero p {
  margin: 6px 0 0;
  color: var(--app-muted);
  font-size: 13px;
}

.filter-modal__footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding: 0 16px;
}
</style>
