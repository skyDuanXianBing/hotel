<template>
  <ion-modal :is-open="isOpen" @didDismiss="$emit('dismiss')">
    <ion-header translucent>
      <ion-toolbar>
        <ion-title>关房设置</ion-title>
        <ion-buttons slot="end">
          <ion-button @click="$emit('dismiss')">关闭</ion-button>
        </ion-buttons>
      </ion-toolbar>
    </ion-header>

    <ion-content>
      <section class="close-room-modal__hero">
        <strong>{{ roomLabel }}</strong>
      </section>

      <ion-list inset>
        <ion-item>
          <ion-input v-model="form.startDate" type="date" label="开始日期" label-placement="stacked" />
        </ion-item>
        <ion-item>
          <ion-input v-model="form.endDate" type="date" label="结束日期" label-placement="stacked" />
        </ion-item>
        <ion-item>
          <ion-select v-model="form.type" label="关房类型" label-placement="stacked" interface="action-sheet">
            <ion-select-option value="stop">停用房</ion-select-option>
            <ion-select-option value="maintenance">维修房</ion-select-option>
            <ion-select-option value="retain">保留房</ion-select-option>
          </ion-select>
        </ion-item>
        <ion-item lines="none">
          <ion-textarea v-model="form.remark" auto-grow label="备注" label-placement="stacked" placeholder="例如设备维修、房间保留" />
        </ion-item>
      </ion-list>
    </ion-content>

    <ion-footer>
      <ion-toolbar>
        <div class="close-room-modal__footer">
          <ion-button fill="outline" @click="$emit('dismiss')">取消</ion-button>
          <ion-button :disabled="submitting" @click="handleSubmit">保存</ion-button>
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
import { computed, ref, watch } from 'vue'
import type { RoomStatusRoomItem } from '@/stores/roomStatus'
import { getStoreTodayDate } from '@/utils/storeBusinessDate'

export interface CloseRoomSubmitPayload {
  startDate: string
  endDate: string
  type: 'stop' | 'maintenance' | 'retain'
  remark: string
}

const props = defineProps<{
  isOpen: boolean
  room: RoomStatusRoomItem | null
  submitting: boolean
}>()

const emit = defineEmits<{
  dismiss: []
  submit: [payload: CloseRoomSubmitPayload]
}>()

const form = ref<CloseRoomSubmitPayload>({
  startDate: '',
  endDate: '',
  type: 'stop',
  remark: '',
})

const roomLabel = computed(() => {
  if (!props.room) {
    return '未选择房间'
  }
  return `${props.room.roomType} · ${props.room.roomNumber}`
})

function resetForm() {
  const fallbackDate = getStoreTodayDate()
  const focusedDate = props.room?.focusedDate || fallbackDate
  form.value = {
    startDate: focusedDate,
    endDate: focusedDate,
    type: 'stop',
    remark: props.room?.closeRemark || '',
  }
}

function handleSubmit() {
  emit('submit', {
    startDate: form.value.startDate,
    endDate: form.value.endDate,
    type: form.value.type,
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
.close-room-modal__hero {
  margin: 16px;
  padding: 16px;
  border-radius: 18px;
  background: rgba(220, 38, 38, 0.08);
}

.close-room-modal__hero p {
  margin: 6px 0 0;
  color: var(--app-muted);
}

.close-room-modal__footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding: 0 16px;
}
</style>
