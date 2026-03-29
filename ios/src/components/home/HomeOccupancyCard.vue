<template>
  <section class="occ-section">
    <div class="occ-section__header">
      <h2 class="occ-section__title">近 7 天入住率</h2>
      <span class="occ-section__range">{{ dateRangeLabel }}</span>
    </div>

    <div v-if="loading" class="occ-skeleton">
      <div v-for="index in 7" :key="index" class="occ-skeleton__bar">
        <ion-skeleton-text animated style="width: 100%; height: 100%" />
      </div>
    </div>

    <template v-else-if="items.length > 0">
      <!-- Summary chips -->
      <div class="occ-chips">
        <div class="occ-chip">
          <span class="occ-chip__label">平均</span>
          <strong class="occ-chip__value">{{ averageRateLabel }}</strong>
        </div>
        <div class="occ-chip">
          <span class="occ-chip__label">峰值</span>
          <strong class="occ-chip__value">{{ peakRateLabel }}</strong>
        </div>
        <div class="occ-chip">
          <span class="occ-chip__label">今日</span>
          <strong class="occ-chip__value">{{ latestRateLabel }}</strong>
        </div>
      </div>

      <!-- Bar chart -->
      <div class="occ-chart">
        <div v-for="item in normalizedItems" :key="item.date" class="occ-bar">
          <div class="occ-bar__track">
            <div class="occ-bar__fill" :style="{ height: `${resolveBarHeight(item.rate)}%` }" />
          </div>
          <span class="occ-bar__rate">{{ formatRate(item.rate) }}</span>
          <span class="occ-bar__date">{{ formatShortDate(item.date) }}</span>
        </div>
      </div>
    </template>

    <p v-else class="occ-empty">暂无入住率数据</p>
  </section>
</template>

<script setup lang="ts">
import { IonSkeletonText } from '@ionic/vue'
import { computed } from 'vue'
import type { DailyOccupancyDTO } from '@/api/home'

interface Props {
  items: DailyOccupancyDTO[]
  loading: boolean
}

const props = defineProps<Props>()

const normalizeRate = (rate: number) => {
  if (Number.isNaN(rate)) {
    return 0
  }
  if (rate < 0) {
    return 0
  }
  if (rate > 100) {
    return 100
  }
  return Number(rate.toFixed(1))
}

const normalizedItems = computed(() => {
  const nextItems: DailyOccupancyDTO[] = []
  for (const item of props.items) {
    nextItems.push({
      ...item,
      rate: normalizeRate(Number(item.rate)),
    })
  }
  return nextItems
})

const averageRateLabel = computed(() => {
  if (normalizedItems.value.length === 0) {
    return '--'
  }
  let total = 0
  for (const item of normalizedItems.value) {
    total += item.rate
  }
  return formatRate(total / normalizedItems.value.length)
})

const peakRateLabel = computed(() => {
  if (normalizedItems.value.length === 0) {
    return '--'
  }
  let peakItem = normalizedItems.value[0]
  for (const item of normalizedItems.value) {
    if (item.rate > peakItem.rate) {
      peakItem = item
    }
  }
  return formatRate(peakItem.rate)
})

const latestRateLabel = computed(() => {
  if (normalizedItems.value.length === 0) {
    return '--'
  }
  const latestItem = normalizedItems.value[normalizedItems.value.length - 1]
  return formatRate(latestItem.rate)
})

const dateRangeLabel = computed(() => {
  if (normalizedItems.value.length === 0) {
    return '近 7 天'
  }
  const start = normalizedItems.value[0]?.date
  const end = normalizedItems.value[normalizedItems.value.length - 1]?.date
  if (!start || !end) {
    return '近 7 天'
  }
  return `${formatShortDate(start)} – ${formatShortDate(end)}`
})

const formatRate = (rate: number) => {
  return `${normalizeRate(rate).toFixed(0)}%`
}

const formatShortDate = (rawDate: string) => {
  const date = new Date(rawDate)
  if (Number.isNaN(date.getTime())) {
    return rawDate
  }
  return `${date.getMonth() + 1}/${date.getDate()}`
}

const resolveBarHeight = (rate: number) => {
  const normalizedValue = normalizeRate(rate)
  if (normalizedValue < 12) {
    return 12
  }
  return normalizedValue
}
</script>

<style scoped>
.occ-section {
  padding: 18px;
  border: 1px solid var(--app-border);
  border-radius: 20px;
  background: var(--app-surface);
  box-shadow: var(--app-shadow);
}

.occ-section__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 14px;
}

.occ-section__title {
  margin: 0;
  color: var(--app-heading);
  font-size: 16px;
  font-weight: 700;
}

.occ-section__range {
  color: var(--app-muted);
  font-size: 12px;
  font-weight: 500;
}

.occ-chips {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
  margin-bottom: 16px;
}

.occ-chip {
  padding: 10px;
  border: 1px solid var(--app-border);
  border-radius: 14px;
  background: var(--app-surface-muted);
  text-align: center;
}

.occ-chip__label {
  display: block;
  color: var(--app-muted);
  font-size: 11px;
  font-weight: 500;
}

.occ-chip__value {
  display: block;
  margin-top: 4px;
  color: var(--ion-color-primary);
  font-size: 18px;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
}

.occ-chart {
  display: grid;
  grid-template-columns: repeat(7, minmax(0, 1fr));
  align-items: end;
  gap: 6px;
}

.occ-bar {
  display: grid;
  justify-items: center;
  gap: 6px;
}

.occ-bar__track {
  display: flex;
  align-items: flex-end;
  width: 100%;
  min-height: 90px;
  padding: 4px;
  border-radius: 12px;
  background: var(--app-surface-muted);
}

.occ-bar__fill {
  width: 100%;
  border-radius: 10px;
  background: linear-gradient(180deg, #60a5fa 0%, var(--ion-color-primary) 100%);
  transition: height 0.3s ease;
}

.occ-bar__rate {
  color: var(--app-heading);
  font-size: 11px;
  font-weight: 600;
  font-variant-numeric: tabular-nums;
}

.occ-bar__date {
  color: var(--app-muted);
  font-size: 10px;
}

.occ-empty {
  margin: 0;
  padding: 20px 0;
  color: var(--app-muted);
  font-size: 13px;
  text-align: center;
}

.occ-skeleton {
  display: grid;
  grid-template-columns: repeat(7, minmax(0, 1fr));
  gap: 6px;
  min-height: 100px;
}

.occ-skeleton__bar {
  border-radius: 12px;
  overflow: hidden;
}
</style>
