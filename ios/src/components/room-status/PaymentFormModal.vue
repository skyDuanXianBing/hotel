<template>
  <ion-modal :is-open="isOpen" @didDismiss="$emit('dismiss')">
    <ion-header translucent>
      <ion-toolbar>
        <ion-title>新增收款</ion-title>
        <ion-buttons slot="end">
          <ion-button @click="$emit('dismiss')">关闭</ion-button>
        </ion-buttons>
      </ion-toolbar>
    </ion-header>

    <ion-content>
      <ion-list inset>
        <ion-item>
          <ion-select v-model="form.type" label="类型" label-placement="stacked" interface="action-sheet">
            <ion-select-option value="收款">收款</ion-select-option>
            <ion-select-option value="收押金">收押金</ion-select-option>
            <ion-select-option value="其他">其他</ion-select-option>
          </ion-select>
        </ion-item>
        <ion-item>
          <ion-select
            v-model="form.paymentMethod"
            label="支付方式"
            label-placement="stacked"
            interface="action-sheet"
          >
            <ion-select-option value="微信">微信</ion-select-option>
            <ion-select-option value="支付宝">支付宝</ion-select-option>
            <ion-select-option value="现金">现金</ion-select-option>
            <ion-select-option value="银行卡">银行卡</ion-select-option>
          </ion-select>
        </ion-item>
        <ion-item>
          <ion-input v-model.number="form.amount" type="number" min="0" label="金额" label-placement="stacked" />
        </ion-item>
        <ion-item>
          <ion-input v-model="form.date" type="date" label="日期" label-placement="stacked" />
        </ion-item>
        <ion-item lines="none">
          <ion-textarea v-model="form.remark" auto-grow label="备注" label-placement="stacked" placeholder="可选" />
        </ion-item>
      </ion-list>
    </ion-content>

    <ion-footer>
      <ion-toolbar>
        <div class="modal-footer-actions">
          <ion-button fill="outline" @click="$emit('dismiss')">取消</ion-button>
          <ion-button :disabled="submitting" @click="handleSubmit">保存收款</ion-button>
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
  type: '收款',
  paymentMethod: '微信',
  amount: 0,
  date: new Date().toISOString().split('T')[0],
  remark: '',
})

function resetForm() {
  form.value = {
    type: '收款',
    paymentMethod: '微信',
    amount: 0,
    date: new Date().toISOString().split('T')[0],
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
