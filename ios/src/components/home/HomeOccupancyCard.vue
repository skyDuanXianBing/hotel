<template>
  <section class="occ-section">
    <div class="occ-section__header">
      <h2 class="occ-section__title">近 7 天入住率</h2>
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
            <span class="occ-bar__rate">{{ formatRate(item.rate) }}</span>
            <div class="occ-bar__track">
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
  padding: 20px 18px;
  border: 1px solid rgba(97, 124, 177, 0.08);
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.96);
  box-shadow: 0 12px 30px rgba(77, 98, 145, 0.08);
}

.occ-section__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 16px;
}

.occ-section__title {
  margin: 0;
  color: #16233b;
  font-size: 18px;
  font-weight: 800;
  letter-spacing: -0.03em;
}

.occ-section__range {
  display: inline-flex;
  align-items: center;
  min-height: 28px;
  padding: 0 12px;
  border: 1px solid rgba(116, 163, 251, 0.1);
  border-radius: 999px;
  background: rgba(115, 164, 255, 0.08);
  color: #3f7cff;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.78);
  font-size: 11px;
  font-weight: 700;
  white-space: nowrap;
}

.occ-overview {
  display: grid;
  grid-template-columns: minmax(0, 1.15fr) minmax(0, 1.85fr);
  gap: 10px;
  margin-bottom: 16px;
}

.occ-lead {
  display: grid;
  gap: 8px;
  padding: 14px;
  border: 1px solid rgba(89, 125, 202, 0.1);
  border-radius: 20px;
  background: linear-gradient(180deg, rgba(239, 245, 255, 0.98), rgba(247, 250, 255, 0.94));
}

.occ-lead__label {
  color: #6f80a0;
  font-size: 12px;
  font-weight: 700;
}

.occ-lead__value {
  color: #16233b;
  font-size: 30px;
  font-weight: 800;
  line-height: 1;
  letter-spacing: -0.05em;
  font-variant-numeric: tabular-nums;
}

.occ-chips {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.occ-chip {
  padding: 12px;
  border: 1px solid rgba(115, 139, 188, 0.1);
  border-radius: 18px;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(247, 250, 255, 0.96));
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.82);
}

.occ-chip__label {
  display: block;
  color: #73829d;
  font-size: 11px;
  font-weight: 700;
}

.occ-chip__value {
  display: block;
  margin-top: 8px;
  color: #16233b;
  font-size: 24px;
  font-weight: 800;
  line-height: 1;
  font-variant-numeric: tabular-nums;
}

.occ-chart-shell {
  padding: 14px 12px 10px;
  border: 1px solid rgba(115, 139, 188, 0.1);
  border-radius: 22px;
  background: linear-gradient(180deg, rgba(247, 250, 255, 0.98), rgba(255, 255, 255, 0.96));
}

.occ-chart {
  display: grid;
  grid-template-columns: repeat(7, minmax(0, 1fr));
  align-items: end;
  gap: 8px;
}

.occ-bar {
  display: grid;
  justify-items: center;
  gap: 8px;
}

.occ-bar__track {
  display: flex;
  justify-content: center;
  align-items: flex-end;
  width: 100%;
  min-height: 124px;
  padding: 12px 0 8px;
  border-radius: 18px;
  background: linear-gradient(180deg, rgba(238, 244, 255, 0.96), rgba(248, 250, 255, 0.92));
}

.occ-bar__fill {
  width: 16px;
  border-radius: 999px;
  background: linear-gradient(180deg, #90cbff 0%, #4e82ff 100%);
  box-shadow: 0 10px 18px rgba(78, 130, 255, 0.2);
  transition: height 0.3s ease;
}

.occ-bar__rate {
  color: #16233b;
  font-size: 11px;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
}

.occ-bar__date {
  color: #7d8ca7;
  font-size: 10px;
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
  grid-template-columns: minmax(0, 1.15fr) minmax(0, 1.85fr);
  gap: 10px;
}

.occ-skeleton__lead,
.occ-skeleton__chip {
  margin: 0;
  border-radius: 18px;
}

.occ-skeleton__lead {
  height: 88px;
}

.occ-skeleton__chips {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.occ-skeleton__chip {
  height: 88px;
}

.occ-skeleton__chart {
  display: grid;
  grid-template-columns: repeat(7, minmax(0, 1fr));
  gap: 8px;
  min-height: 124px;
}

.occ-skeleton__bar {
  border-radius: 18px;
  overflow: hidden;
}

@media (max-width: 374px) {
  .occ-overview,
  .occ-skeleton__summary {
    grid-template-columns: 1fr;
  }

  .occ-chart-shell {
    padding-left: 10px;
    padding-right: 10px;
  }

  .occ-bar__track {
    min-height: 112px;
  }
}

@media (prefers-reduced-motion: reduce) {
  .occ-bar__fill {
    transition: none;
  }
}
</style>
