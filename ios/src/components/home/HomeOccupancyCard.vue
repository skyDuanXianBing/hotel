<template>
  <section class="occ-section mobile-dashboard-surface">
    <div class="occ-section__header">
      <h2 class="occ-section__title">近7天入住率</h2>
      <span class="occ-section__range">{{ dateRangeLabel }}</span>
    </div>

    <div v-if="loading" class="occ-skeleton">
      <div class="occ-skeleton__summary">
        <ion-skeleton-text animated class="occ-skeleton__lead" />
        <div class="occ-skeleton__chips">
          <ion-skeleton-text animated class="occ-skeleton__chip" />
          <ion-skeleton-text animated class="occ-skeleton__chip" />
        </div>
      </div>

      <div class="occ-skeleton__chart">
        <div v-for="index in 7" :key="index" class="occ-skeleton__bar">
          <ion-skeleton-text animated style="width: 100%; height: 100%" />
        </div>
      </div>
    </div>

    <template v-else-if="items.length > 0">
      <div class="occ-overview">
        <div class="occ-lead">
          <span class="occ-lead__label">今日</span>
          <strong class="occ-lead__value">{{ latestRateLabel }}</strong>
        </div>

        <div class="occ-chips">
          <div class="occ-chip">
            <span class="occ-chip__label">平均</span>
            <strong class="occ-chip__value">{{ averageRateLabel }}</strong>
          </div>
          <div class="occ-chip">
            <span class="occ-chip__label">峰值</span>
            <strong class="occ-chip__value">{{ peakRateLabel }}</strong>
          </div>
        </div>
      </div>

      <div class="occ-chart-shell">
        <div class="occ-chart">
          <div v-for="item in normalizedItems" :key="item.date" class="occ-bar">
            <div class="occ-bar__track">
              <span class="occ-bar__rate">{{ formatRate(item.rate) }}</span>
              <div class="occ-bar__fill" :style="{ height: `${resolveBarHeight(item.rate)}%` }" />
            </div>
            <span class="occ-bar__date">{{ formatShortDate(item.date) }}</span>
          </div>
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
import { formatBusinessDateLabel } from '@/utils/storeBusinessDate'

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
  return `${formatShortDate(start)} - ${formatShortDate(end)}`
})

const formatRate = (rate: number) => {
  return `${normalizeRate(rate).toFixed(0)}%`
}

const formatShortDate = (rawDate: string) => {
  const fallbackDate = rawDate.trim()
  const label = formatBusinessDateLabel(fallbackDate, 'month-day', fallbackDate)
  if (label === fallbackDate) {
    return rawDate
  }
  return label.replace('-', '/')
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
  padding: 18px 16px 20px;
  border-radius: var(--ios-pms-radius-card);
}

.occ-section__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 14px;
}

.occ-section__title {
  margin: 0;
  color: var(--ios-pms-text-primary);
  font-size: 20px;
  font-weight: var(--ios-pms-weight-medium);
  letter-spacing: 0;
}

.occ-section__range {
  display: inline-flex;
  align-items: center;
  flex-shrink: 0;
  padding: 0;
  border: none;
  background: transparent;
  color: var(--ios-pms-text-muted);
  box-shadow: none;
  font-size: 13px;
  font-weight: 400;
  white-space: nowrap;
}

.occ-overview {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
  margin-bottom: 18px;
}

.occ-lead {
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  min-height: 80px;
  padding: 13px 14px 12px;
  border: none;
  border-radius: 7px;
  background: linear-gradient(127deg, #007cfe 0%, #6eb5ff 100%);
}

.occ-lead__label {
  color: rgba(255, 255, 255, 0.9);
  font-size: 14px;
  font-weight: 400;
}

.occ-lead__value {
  color: #ffffff;
  font-size: 23px;
  font-weight: var(--ios-pms-weight-medium);
  line-height: 1;
  letter-spacing: 0;
  font-variant-numeric: tabular-nums;
}

.occ-chips {
  display: contents;
}

.occ-chip {
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  min-height: 80px;
  padding: 13px 14px 12px;
  border: none;
  border-radius: 7px;
  background: #f3f8fd;
  box-shadow: none;
}

.occ-chip__label {
  display: block;
  color: var(--ios-pms-text-muted);
  font-size: 14px;
  font-weight: 400;
}

.occ-chip__value {
  display: block;
  margin-top: 6px;
  color: var(--ios-pms-text-primary);
  font-size: 23px;
  font-weight: var(--ios-pms-weight-medium);
  line-height: 1;
  font-variant-numeric: tabular-nums;
}

.occ-chart-shell {
  padding: 0;
  border: 1px dashed #d9e1ee;
  border-radius: 4px;
  background:
    linear-gradient(to bottom, transparent 49.5%, rgba(205, 215, 231, 0.62) 50%, transparent 50.5%),
    #fbfcff;
  overflow: hidden;
}

.occ-chart {
  display: grid;
  grid-template-columns: repeat(7, minmax(0, 1fr));
  align-items: stretch;
  gap: 7px;
  height: 164px;
  padding: 0 7px;
}

.occ-bar {
  display: grid;
  grid-template-rows: minmax(0, 1fr) 24px;
  justify-items: center;
  gap: 0;
  min-width: 0;
}

.occ-bar__track {
  display: flex;
  flex-direction: column;
  justify-content: flex-end;
  align-items: flex-end;
  justify-self: stretch;
  width: 100%;
  min-height: 0;
  padding: 7px 4px 0;
  border-radius: 0 0 18px 18px;
  background: rgba(239, 244, 252, 0.72);
  box-sizing: border-box;
}

.occ-bar__fill {
  align-self: stretch;
  width: 100%;
  max-height: calc(100% - 19px);
  border-radius: 20px;
  background: linear-gradient(180deg, #cce6ff 0%, #80baf2 48%, #5d9fe8 100%);
  box-shadow: none;
  transition: height 0.3s ease;
}

.occ-bar__rate {
  align-self: center;
  flex-shrink: 0;
  min-width: 0;
  margin-bottom: 3px;
  color: #697384;
  font-size: 10px;
  font-weight: 400;
  line-height: 1.2;
  font-variant-numeric: tabular-nums;
  white-space: nowrap;
}

.occ-bar__date {
  align-self: center;
  color: #5f6570;
  font-size: 11px;
  line-height: 24px;
  white-space: nowrap;
}

.occ-empty {
  margin: 0;
  padding: 28px 0 8px;
  color: #8b99b4;
  font-size: 13px;
  text-align: center;
}

.occ-skeleton {
  display: grid;
  gap: 16px;
}

.occ-skeleton__summary {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
}

.occ-skeleton__lead,
.occ-skeleton__chip {
  margin: 0;
  border-radius: 7px;
}

.occ-skeleton__lead {
  height: 80px;
}

.occ-skeleton__chips {
  display: contents;
}

.occ-skeleton__chip {
  height: 80px;
}

.occ-skeleton__chart {
  display: grid;
  grid-template-columns: repeat(7, minmax(0, 1fr));
  gap: 7px;
  min-height: 164px;
}

.occ-skeleton__bar {
  border-radius: 0 0 18px 18px;
  overflow: hidden;
}

@media (max-width: 374px) {
  .occ-section {
    padding-left: 14px;
    padding-right: 14px;
  }

  .occ-lead,
  .occ-chip {
    min-height: 76px;
    padding-left: 11px;
    padding-right: 11px;
  }

  .occ-lead__label,
  .occ-chip__label {
    font-size: 13px;
  }

  .occ-lead__value,
  .occ-chip__value {
    font-size: 21px;
  }

  .occ-chart {
    gap: 5px;
    height: 156px;
    padding-left: 5px;
    padding-right: 5px;
  }

  .occ-bar__track {
    padding-left: 3px;
    padding-right: 3px;
  }

  .occ-bar__rate {
    font-size: 9px;
  }

  .occ-bar__date {
    font-size: 10px;
  }
}

@media (prefers-reduced-motion: reduce) {
  .occ-bar__fill {
    transition: none;
  }
}
</style>
