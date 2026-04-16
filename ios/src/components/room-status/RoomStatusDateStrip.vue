<template>
  <section class="mobile-card date-strip-card">
    <div class="mobile-inline-row date-strip-header">
      <div>
        <h2 class="mobile-section-title">日期浏览</h2>
        <p class="mobile-note">保留 5 天时间线，切换窗口后自动同步房态、摘要与房型汇总。</p>
      </div>
      <ion-button fill="clear" size="small" @click="$emit('today')">今天</ion-button>
    </div>

    <div class="date-strip-actions">
      <ion-button fill="outline" size="small" @click="$emit('previous')">前 {{ days.length }} 天</ion-button>
      <ion-button fill="outline" size="small" @click="$emit('next')">后 {{ days.length }} 天</ion-button>
    </div>

    <div class="date-strip-grid">
      <button
        v-for="item in days"
        :key="item.date"
        class="date-pill"
        :class="{
          'date-pill--selected': item.isSelected,
          'date-pill--today': item.isToday,
        }"
        type="button"
        @click="$emit('select', item.date)"
      >
        <span class="date-pill__top">{{ item.weekday }}</span>
        <strong>{{ item.label }}</strong>
        <span class="date-pill__bottom">剩 {{ item.availableRooms }}</span>
      </button>
    </div>
  </section>
</template>

<script setup lang="ts">
import { IonButton } from '@ionic/vue'
import type { RoomStatusDateItem } from '@/stores/roomStatus'

defineProps<{
  days: RoomStatusDateItem[]
}>()

defineEmits<{
  select: [date: string]
  previous: []
  next: []
  today: []
}>()
</script>

<style scoped>
.date-strip-card {
  padding-bottom: 16px;
}

.date-strip-header {
  align-items: flex-start;
}

.date-strip-actions {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
  margin-bottom: 14px;
}

.date-strip-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 10px;
}

.date-pill {
  appearance: none;
  border: 1px solid var(--app-border);
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.82);
  color: inherit;
  padding: 12px 8px;
  display: grid;
  gap: 4px;
  text-align: center;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.6);
}

.date-pill--selected {
  border-color: var(--app-border-strong);
  background: var(--app-primary-soft-strong);
  color: var(--ion-color-primary);
}

.date-pill--today {
  box-shadow: 0 10px 24px var(--app-primary-soft-strong);
}

.date-pill__top,
.date-pill__bottom {
  font-size: 11px;
  color: var(--app-muted);
}

@media (max-width: 420px) {
  .date-strip-grid {
    grid-template-columns: repeat(5, minmax(58px, 1fr));
    overflow-x: auto;
  }
}
</style>
