<template>
  <section class="mobile-card">
    <div class="mobile-inline-row room-type-summary__header">
      <div>
        <h2 class="mobile-section-title">{{ $t('stage5VisibleText.175') }}</h2>
      </div>
      <ion-button fill="clear" size="small" @click="$emit('reset')">{{ $t('roomStatus.roomsPage.empty.resetAction') }}</ion-button>
    </div>

    <div class="room-type-summary__list">
      <button
        v-for="item in items"
        :key="item.roomType"
        class="room-type-summary__item"
        :class="{ 'room-type-summary__item--selected': selectedRoomTypeSet.has(item.roomType) }"
        type="button"
        :aria-pressed="selectedRoomTypeSet.has(item.roomType)"
        @click="$emit('toggle', item.roomType)"
      >
        <div class="room-type-summary__title-row">
          <div class="room-type-summary__title-main">
            <span
              class="room-type-summary__checkbox"
              :class="{
                'room-type-summary__checkbox--selected': selectedRoomTypeSet.has(item.roomType),
              }"
              aria-hidden="true"
            >
              {{ selectedRoomTypeSet.has(item.roomType) ? '✓' : '' }}
            </span>
            <strong>{{ item.roomType }}</strong>
          </div>
          <span>{{ item.totalRooms }} {{ $t('settingsStage4.common.unitRooms') }}</span>
        </div>
        <div class="room-type-summary__metrics">
          <span>{{ $t('accommodation.roomTable.future.available') }} {{ item.availableRooms }}</span>
          <span>{{ $t('accommodation.roomTable.future.occupied') }} {{ item.occupiedRooms }}</span>
          <span>{{ $t('accommodation.roomPrice.closeRoom') }} {{ item.closedRooms }}</span>
          <span>{{ $t('accommodation.roomTable.columns.dirtyRooms') }} {{ item.dirtyRooms }}</span>
        </div>
      </button>
    </div>
  </section>
</template>

<script setup lang="ts">
import { IonButton } from '@ionic/vue'
import { computed } from 'vue'
import type { RoomTypeSummaryItem } from '@/stores/roomStatus'

const props = defineProps<{
  items: RoomTypeSummaryItem[]
  selectedRoomTypes: string[]
}>()

defineEmits<{
  toggle: [roomType: string]
  reset: []
}>()

const selectedRoomTypeSet = computed(() => new Set(props.selectedRoomTypes))
</script>

<style scoped>
.room-type-summary__header {
  margin-bottom: 14px;
}

.room-type-summary__header h2 {
  margin: 0;
}

.room-type-summary__header ion-button {
  --color: #6f7f99;
}

.room-type-summary__list {
  display: grid;
  gap: 10px;
}

.room-type-summary__item {
  appearance: none;
  border: 1px solid rgba(112, 138, 187, 0.12);
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.94), rgba(247, 250, 255, 0.9));
  border-radius: 22px;
  padding: 16px;
  text-align: left;
  color: inherit;
  box-shadow: 0 14px 28px rgba(83, 107, 152, 0.05);
}

.room-type-summary__item--selected {
  border-color: rgba(63, 124, 255, 0.18);
  background:
    linear-gradient(180deg, rgba(235, 243, 255, 0.96), rgba(229, 238, 255, 0.92));
  box-shadow:
    0 16px 30px rgba(83, 107, 152, 0.06),
    0 0 0 1px rgba(63, 124, 255, 0.1);
}

.room-type-summary__title-main {
  display: inline-flex;
  align-items: center;
  gap: 10px;
}

.room-type-summary__checkbox {
  width: 22px;
  height: 22px;
  border-radius: 10px;
  border: 1px solid rgba(63, 124, 255, 0.18);
  background: rgba(255, 255, 255, 0.96);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  font-weight: 700;
  color: transparent;
  flex-shrink: 0;
}

.room-type-summary__checkbox--selected {
  background: linear-gradient(180deg, #4b86ff 0%, #2f6df2 100%);
  border-color: #2f6df2;
  color: #ffffff;
}

.room-type-summary__title-row,
.room-type-summary__metrics {
  display: flex;
  justify-content: space-between;
  gap: 8px;
  flex-wrap: wrap;
}

.room-type-summary__metrics {
  margin-top: 10px;
  font-size: 12px;
  color: var(--app-muted);
}

.room-type-summary__title-row strong {
  color: #1d2942;
  font-size: 15px;
}

.room-type-summary__title-row > span {
  color: #6f7f99;
  font-size: 12px;
  font-weight: 600;
}

.room-type-summary__metrics span {
  display: inline-flex;
  align-items: center;
  min-height: 28px;
  padding: 0 10px;
  border-radius: 999px;
  background: rgba(240, 244, 255, 0.78);
}
</style>
