<template>
  <ion-modal :is-open="isOpen" @didDismiss="$emit('dismiss')">
    <ion-header translucent>
      <ion-toolbar>
        <ion-title>{{ $t('stage5VisibleText.194') }}</ion-title>
        <ion-buttons slot="end">
          <ion-button @click="$emit('dismiss')">{{ $t('home.section.close') }}</ion-button>
        </ion-buttons>
      </ion-toolbar>
    </ion-header>

    <ion-content>
      <ion-list inset>
        <ion-item>
          <ion-input v-model="form.item" :label="$t('roomStatus.common.item')" label-placement="stacked" :placeholder="$t('stage5UiAttributes.26')" />
        </ion-item>
        <ion-item>
          <ion-input v-model.number="form.quantity" type="number" min="1" :label="$t('roomStatus.consumption.quantity')" label-placement="stacked" />
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
          <ion-button :disabled="submitting" @click="handleSubmit">{{ $t('stage5VisibleText.126') }}</ion-button>
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
  IonTextarea,
  IonTitle,
  IonToolbar,
} from '@ionic/vue'
import { ref, watch } from 'vue'
import { getStoreTodayDate } from '@/utils/storeBusinessDate'

export interface ConsumptionFormSubmitPayload {
  item: string
  quantity: number
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
  submit: [payload: ConsumptionFormSubmitPayload]
}>()

const form = ref<ConsumptionFormSubmitPayload>({
  item: '',
  quantity: 1,
  amount: 0,
  date: getStoreTodayDate(),
  remark: '',
})

function resetForm() {
  form.value = {
    item: '',
    quantity: 1,
    amount: 0,
    date: getStoreTodayDate(),
    remark: '',
  }
}

function handleSubmit() {
  emit('submit', {
    item: form.value.item.trim(),
    quantity: Number(form.value.quantity || 1),
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
