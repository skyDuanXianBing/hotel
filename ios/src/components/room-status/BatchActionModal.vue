<template>
  <ion-modal :is-open="isOpen" @didDismiss="$emit('dismiss')">
    <ion-header translucent>
      <ion-toolbar>
        <ion-title>{{ modalTitle }}</ion-title>
        <ion-buttons slot="end">
          <ion-button @click="$emit('dismiss')">{{ $t('home.section.close') }}</ion-button>
        </ion-buttons>
      </ion-toolbar>
    </ion-header>

    <ion-content>
      <section class="batch-modal__toolbar">
        <ion-searchbar v-model="searchKeyword" :placeholder="$t('stage5UiAttributes.47')" />
        <ion-button fill="clear" size="small" @click="toggleAllRooms">{{ toggleAllText }}</ion-button>
      </section>

      <ion-list inset v-if="needsDateRange">
        <ion-item>
          <ion-input v-model="form.startDate" type="date" :label="$t('accommodation.common.startDate')" label-placement="stacked" />
        </ion-item>
        <ion-item>
          <ion-input v-model="form.endDate" type="date" :label="$t('accommodation.common.endDate')" label-placement="stacked" />
        </ion-item>
        <ion-item>
          <ion-select v-model="form.weekMode" :label="$t('stage5UiAttributes.108')" label-placement="stacked" interface="action-sheet">
            <ion-select-option value="all">{{ $t('roomStatus.closeRoom.allDates') }}</ion-select-option>
            <ion-select-option value="weekday">{{ $t('stage5VisibleText.113') }}</ion-select-option>
            <ion-select-option value="weekend">{{ $t('stage5VisibleText.112') }}</ion-select-option>
          </ion-select>
        </ion-item>
        <ion-item v-if="mode === 'close'">
          <ion-select v-model="form.type" :label="$t('roomStatus.closeRoom.typeLabel')" label-placement="stacked" interface="action-sheet">
            <ion-select-option value="stop">{{ $t('roomStatus.closeRoom.type.stop') }}</ion-select-option>
            <ion-select-option value="maintenance">{{ $t('roomStatus.closeRoom.type.maintenance') }}</ion-select-option>
            <ion-select-option value="retain">{{ $t('roomStatus.closeRoom.type.retain') }}</ion-select-option>
          </ion-select>
        </ion-item>
        <ion-item v-if="mode === 'close'" lines="none">
          <ion-textarea v-model="form.remark" auto-grow :label="$t('accommodation.common.remarks')" label-placement="stacked" :placeholder="$t('settingsStage4.autoCheckin.status.optional')" />
        </ion-item>
      </ion-list>

      <ion-list inset>
        <ion-item v-for="room in filteredRooms" :key="room.roomId" button @click="toggleRoom(room.roomId)">
          <ion-checkbox slot="start" :checked="selectedRoomIds.includes(room.roomId)" />
          <ion-label>
            <h3>{{ room.roomNumber }}</h3>
            <p>{{ room.roomType }}</p>
          </ion-label>
        </ion-item>
      </ion-list>
    </ion-content>

    <ion-footer>
      <ion-toolbar>
        <div class="modal-footer-actions">
          <ion-button fill="outline" @click="$emit('dismiss')">{{ $t('accommodation.common.cancel') }}</ion-button>
          <ion-button :disabled="submitting || selectedRoomIds.length === 0" @click="handleSubmit">
            {{ $t('roomStatus.roomLock.actions.confirmExecute') }}
          </ion-button>
        </div>
      </ion-toolbar>
    </ion-footer>
  </ion-modal>
</template>

<script setup lang="ts">
import {
  IonButton,
  IonButtons,
  IonCheckbox,
  IonContent,
  IonFooter,
  IonHeader,
  IonInput,
  IonItem,
  IonLabel,
  IonList,
  IonModal,
  IonSearchbar,
  IonSelect,
  IonSelectOption,
  IonTextarea,
  IonTitle,
  IonToolbar,
} from '@ionic/vue'
import { computed, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import type { BatchWeekMode } from '@/stores/roomStatus'
import { getStoreTodayDate } from '@/utils/storeBusinessDate'

export type BatchActionMode = 'dirty' | 'clean' | 'open' | 'close'

export interface BatchRoomOption {
  roomId: number
  roomNumber: string
  roomType: string
}

export interface BatchActionSubmitPayload {
  roomIds: number[]
  startDate: string
  endDate: string
  weekMode: BatchWeekMode
  type: 'stop' | 'maintenance' | 'retain'
  remark: string
}

const props = defineProps<{
  isOpen: boolean
  mode: BatchActionMode
  rooms: BatchRoomOption[]
  submitting: boolean
}>()

const emit = defineEmits<{
  dismiss: []
  submit: [payload: BatchActionSubmitPayload]
}>()

const { t } = useI18n()
const searchKeyword = ref('')
const selectedRoomIds = ref<number[]>([])
const form = ref({
  startDate: getStoreTodayDate(),
  endDate: getStoreTodayDate(),
  weekMode: 'all' as BatchWeekMode,
  type: 'stop' as 'stop' | 'maintenance' | 'retain',
  remark: '',
})

const modalTitle = computed(() => {
  if (props.mode === 'dirty') {
    return t('roomStatus.batch.title.dirty')
  }
  if (props.mode === 'clean') {
    return t('roomStatus.batch.title.clean')
  }
  if (props.mode === 'open') {
    return t('roomStatus.batch.title.open')
  }
  return t('roomStatus.batch.title.close')
})

const needsDateRange = computed(() => props.mode === 'open' || props.mode === 'close')

const filteredRooms = computed(() => {
  const keyword = searchKeyword.value.trim().toLowerCase()
  if (!keyword) {
    return props.rooms
  }

  return props.rooms.filter((room) => {
    return room.roomNumber.toLowerCase().includes(keyword) || room.roomType.toLowerCase().includes(keyword)
  })
})

const toggleAllText = computed(() => {
  if (selectedRoomIds.value.length === props.rooms.length) {
    return t('roomStatus.batch.clearAll')
  }
  return t('roomStatus.batch.selectAll')
})

function resetForm() {
  const today = getStoreTodayDate()
  searchKeyword.value = ''
  selectedRoomIds.value = []
  form.value = {
    startDate: today,
    endDate: today,
    weekMode: 'all',
    type: 'stop',
    remark: '',
  }
}

function toggleRoom(roomId: number) {
  if (selectedRoomIds.value.includes(roomId)) {
    selectedRoomIds.value = selectedRoomIds.value.filter((item) => item !== roomId)
    return
  }

  selectedRoomIds.value = [...selectedRoomIds.value, roomId]
}

function toggleAllRooms() {
  if (selectedRoomIds.value.length === props.rooms.length) {
    selectedRoomIds.value = []
    return
  }
  selectedRoomIds.value = props.rooms.map((room) => room.roomId)
}

function handleSubmit() {
  emit('submit', {
    roomIds: selectedRoomIds.value,
    startDate: form.value.startDate,
    endDate: form.value.endDate,
    weekMode: form.value.weekMode,
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
.batch-modal__toolbar {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  padding: 12px 12px 0;
}

.batch-modal__toolbar ion-searchbar {
  min-width: 0;
  flex: 1 1 220px;
}

.modal-footer-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 10px;
  padding: 0 16px;
}

.modal-footer-actions ion-button {
  min-width: 0;
  white-space: normal;
}
</style>
