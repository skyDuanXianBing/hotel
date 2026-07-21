<template>
  <ion-modal :is-open="isOpen" @didDismiss="$emit('dismiss')">
    <ion-header translucent>
      <ion-toolbar>
        <ion-title>{{ $t('stage5VisibleText.193') }}</ion-title>
        <ion-buttons slot="end">
          <ion-button @click="$emit('dismiss')">{{ $t('home.section.close') }}</ion-button>
        </ion-buttons>
      </ion-toolbar>
    </ion-header>

    <ion-content>
      <ion-list inset>
        <ion-item>
          <ion-select v-model="form.type" :label="$t('roomStatus.sampleLogs.labels.type')" label-placement="stacked" interface="action-sheet">
            <ion-select-option :value="PAYMENT_TYPE_RECEIPT">{{ $t('roomStatus.payment.tabs.payment') }}</ion-select-option>
            <ion-select-option :value="PAYMENT_TYPE_DEPOSIT">{{ $t('roomStatus.payment.typeOptions.deposit') }}</ion-select-option>
            <ion-select-option :value="PAYMENT_TYPE_OTHER">{{ $t('roomStatus.payment.bookingTypeOptions.other') }}</ion-select-option>
          </ion-select>
        </ion-item>
        <ion-item>
          <ion-select
            v-model="form.paymentMethod"
            :label="$t('roomStatus.common.paymentMethod')"
            label-placement="stacked"
            interface="action-sheet"
          >
            <ion-select-option :value="PAYMENT_METHOD_WECHAT">{{ $t('accommodation.paymentMethods.wechat') }}</ion-select-option>
            <ion-select-option :value="PAYMENT_METHOD_ALIPAY">{{ $t('accommodation.paymentMethods.alipay') }}</ion-select-option>
            <ion-select-option :value="PAYMENT_METHOD_CASH">{{ $t('accommodation.paymentMethods.cash') }}</ion-select-option>
            <ion-select-option :value="PAYMENT_METHOD_BANK_CARD">{{ $t('roomStatus.payment.methodOptions.bankCard') }}</ion-select-option>
          </ion-select>
        </ion-item>
        <ion-item>
          <ion-input v-model.number="form.amount" type="number" min="0" :label="$t('roomStatus.common.amount')" label-placement="stacked" />
        </ion-item>
        <ion-item>
          <ion-input v-model="form.date" type="date" :label="$t('accommodation.common.date')" label-placement="stacked" />
        </ion-item>
        <ion-item lines="none">
          <ion-textarea v-model="form.remark" auto-grow :label="$t('accommodation.common.remarks')" label-placement="stacked" :placeholder="$t('settingsStage4.autoCheckin.status.optional')" />
        </ion-item>
      </ion-list>
    </ion-content>

    <ion-footer>
      <ion-toolbar>
        <div class="modal-footer-actions">
          <ion-button fill="outline" @click="$emit('dismiss')">{{ $t('accommodation.common.cancel') }}</ion-button>
          <ion-button :disabled="submitting" @click="handleSubmit">{{ $t('stage5VisibleText.124') }}</ion-button>
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
  IonTextarea,
  IonTitle,
  IonToolbar,
} from '@ionic/vue'
import { ref, watch } from 'vue'
import { getStoreTodayDate } from '@/utils/storeBusinessDate'

const PAYMENT_TYPE_RECEIPT = '收款'
const PAYMENT_TYPE_DEPOSIT = '收押金'
const PAYMENT_TYPE_OTHER = '其他'
const PAYMENT_METHOD_WECHAT = '微信'
const PAYMENT_METHOD_ALIPAY = '支付宝'
const PAYMENT_METHOD_CASH = '现金'
const PAYMENT_METHOD_BANK_CARD = '银行卡'

export interface PaymentFormSubmitPayload {
  type: string
  paymentMethod: string
  amount: number
  date: string
  remark: string
}

const props = defineProps<{
  isOpen: boolean
  submitting: boolean
}>()

const emit = defineEmits<{
  dismiss: []
  submit: [payload: PaymentFormSubmitPayload]
}>()

const form = ref<PaymentFormSubmitPayload>({
  type: PAYMENT_TYPE_RECEIPT,
  paymentMethod: PAYMENT_METHOD_WECHAT,
  amount: 0,
  date: getStoreTodayDate(),
  remark: '',
})

function resetForm() {
  form.value = {
    type: PAYMENT_TYPE_RECEIPT,
    paymentMethod: PAYMENT_METHOD_WECHAT,
    amount: 0,
    date: getStoreTodayDate(),
    remark: '',
  }
}

function handleSubmit() {
  emit('submit', {
    type: form.value.type,
    paymentMethod: form.value.paymentMethod,
    amount: Number(form.value.amount || 0),
    date: form.value.date,
    remark: form.value.remark.trim(),
  })
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
.modal-footer-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding: 0 16px;
}
</style>
