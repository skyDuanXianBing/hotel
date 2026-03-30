<template>
  <ion-modal :is-open="isOpen" @didDismiss="$emit('dismiss')">
    <ion-header translucent>
      <ion-toolbar>
        <ion-title>{{ modalTitle }}</ion-title>
        <ion-buttons slot="end">
          <ion-button @click="$emit('dismiss')">关闭</ion-button>
        </ion-buttons>
      </ion-toolbar>
    </ion-header>

    <ion-content>
      <section class="batch-modal__toolbar">
        <ion-searchbar v-model="searchKeyword" placeholder="搜索房号或房型" />
        <ion-button fill="clear" size="small" @click="toggleAllRooms">{{ toggleAllText }}</ion-button>
      </section>

      <ion-list inset v-if="needsDateRange">
        <ion-item>
          <ion-input v-model="form.startDate" type="date" label="开始日期" label-placement="stacked" />
        </ion-item>
        <ion-item>
          <ion-input v-model="form.endDate" type="date" label="结束日期" label-placement="stacked" />
        </ion-item>
        <ion-item>
          <ion-select v-model="form.weekMode" label="适用日期" label-placement="stacked" interface="action-sheet">
            <ion-select-option value="all">全部日期</ion-select-option>
            <ion-select-option value="weekday">仅工作日</ion-select-option>
            <ion-select-option value="weekend">仅周末</ion-select-option>
          </ion-select>
        </ion-item>
        <ion-item v-if="mode === 'close'">
          <ion-select v-model="form.type" label="关房类型" label-placement="stacked" interface="action-sheet">
            <ion-select-option value="stop">停用房</ion-select-option>
            <ion-select-option value="maintenance">维修房</ion-select-option>
            <ion-select-option value="retain">保留房</ion-select-option>
          </ion-select>
        </ion-item>
        <ion-item v-if="mode === 'close'" lines="none">
          <ion-textarea v-model="form.remark" auto-grow label="备注" label-placement="stacked" placeholder="可选" />
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
          <ion-button fill="outline" @click="$emit('dismiss')">取消</ion-button>
          <ion-button :disabled="submitting || selectedRoomIds.length === 0" @click="handleSubmit">
            确认执行
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
import type { BatchWeekMode } from '@/stores/roomStatus'

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

const searchKeyword = ref('')
const selectedRoomIds = ref<number[]>([])
const form = ref({
  startDate: new Date().toISOString().split('T')[0],
  endDate: new Date().toISOString().split('T')[0],
  weekMode: 'all' as BatchWeekMode,
  type: 'stop' as 'stop' | 'maintenance' | 'retain',
  remark: '',
})

const modalTitle = computed(() => {
  if (props.mode === 'dirty') {
    return '批量置脏'
  }
  if (props.mode === 'clean') {
    return '批量置净'
  }
  if (props.mode === 'open') {
    return '批量开房'
  }
  return '批量关房'
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
    return '取消全选'
  }
  return '全选'
})

function resetForm() {
  searchKeyword.value = ''
  selectedRoomIds.value = []
  form.value = {
    startDate: new Date().toISOString().split('T')[0],
    endDate: new Date().toISOString().split('T')[0],
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
  padding: 12px 12px 0;
}

.modal-footer-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding: 0 16px;
}
</style>
