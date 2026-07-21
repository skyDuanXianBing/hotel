<template>
  <ion-modal :is-open="isOpen" @didDismiss="$emit('dismiss')">
    <ion-header translucent>
      <ion-toolbar>
        <ion-title>{{ $t('stage5VisibleText.137') }}</ion-title>
        <ion-buttons slot="end">
          <ion-button @click="$emit('dismiss')">{{ $t('home.section.close') }}</ion-button>
        </ion-buttons>
      </ion-toolbar>
    </ion-header>

    <ion-content>
      <section class="close-room-modal__hero">
        <strong>{{ roomLabel }}</strong>
      </section>

      <ion-list inset>
        <ion-item>
          <ion-input v-model="form.startDate" type="date" :label="$t('accommodation.common.startDate')" label-placement="stacked" />
        </ion-item>
        <ion-item>
          <ion-input v-model="form.endDate" type="date" :label="$t('accommodation.common.endDate')" label-placement="stacked" />
        </ion-item>
        <ion-item>
          <ion-select v-model="form.type" :label="$t('roomStatus.closeRoom.typeLabel')" label-placement="stacked" interface="action-sheet">
            <ion-select-option value="stop">{{ $t('roomStatus.closeRoom.type.stop') }}</ion-select-option>
            <ion-select-option value="maintenance">{{ $t('roomStatus.closeRoom.type.maintenance') }}</ion-select-option>
            <ion-select-option value="retain">{{ $t('roomStatus.closeRoom.type.retain') }}</ion-select-option>
          </ion-select>
        </ion-item>
        <ion-item lines="none">
          <ion-textarea v-model="form.remark" auto-grow :label="$t('accommodation.common.remarks')" label-placement="stacked" :placeholder="$t('stage5UiAttributes.27')" />
        </ion-item>
      </ion-list>
    </ion-content>

    <ion-footer>
      <ion-toolbar>
        <div class="close-room-modal__footer">
          <ion-button fill="outline" @click="$emit('dismiss')">{{ $t('accommodation.common.cancel') }}</ion-button>
          <ion-button :disabled="submitting" @click="handleSubmit">{{ $t('home.manage.save') }}</ion-button>
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
import { useI18n } from 'vue-i18n'
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

const { t } = useI18n()

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
    return t('stage5Final.roomStatus.noRoomSelected')
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
