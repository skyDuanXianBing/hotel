<template>
  <section class="mobile-card">
    <div class="mobile-inline-row room-type-summary__header">
      <div>
        <h2 class="mobile-section-title">房型视角</h2>
        <p class="mobile-note">勾选需要保留的房型，点击确定后再应用到房态列表。</p>
      </div>
      <ion-button fill="clear" size="small" @click="$emit('reset')">重置筛选</ion-button>
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
          <span>{{ item.totalRooms }} 间</span>
        </div>
        <div class="room-type-summary__metrics">
          <span>可售 {{ item.availableRooms }}</span>
          <span>占用 {{ item.occupiedRooms }}</span>
          <span>关房 {{ item.closedRooms }}</span>
          <span>脏房 {{ item.dirtyRooms }}</span>
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
.room-type-summary__list {
  display: grid;
  gap: 10px;
}

.room-type-summary__item {
  appearance: none;
  border: 1px solid var(--app-border);
  background: rgba(255, 255, 255, 0.82);
  border-radius: 18px;
  padding: 14px;
  text-align: left;
  color: inherit;
}

.room-type-summary__item--selected {
  border-color: var(--app-border-strong);
  background: var(--app-primary-soft-strong);
}

.room-type-summary__title-main {
  display: inline-flex;
  align-items: center;
  gap: 10px;
}

.room-type-summary__checkbox {
  width: 22px;
  height: 22px;
  border-radius: 8px;
  border: 1px solid var(--app-border-strong);
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
  background: var(--ion-color-primary);
  border-color: var(--ion-color-primary);
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
  margin-top: 8px;
  font-size: 12px;
  color: var(--app-muted);
}
</style>
