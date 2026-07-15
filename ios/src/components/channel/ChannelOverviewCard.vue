<template>
  <ion-card class="channel-overview-card" :style="brandVars">
    <ion-card-content class="channel-overview-card__content">
      <button type="button" class="channel-overview-card__main" @click="$emit('openDetail')">
        <div class="channel-overview-card__logo-wrap">
          <img :src="item.logoUrl" :alt="item.name" class="channel-overview-card__logo" />
        </div>

        <div class="channel-overview-card__body">
          <div class="channel-overview-card__headline">
            <h3>{{ item.name }}</h3>
            <span v-if="showState" class="channel-overview-card__state" :class="stateClass">
              {{ compactStatusLabel }}
            </span>
          </div>

          <p v-if="showSummary" class="channel-overview-card__summary">{{ summaryText }}</p>

          <div v-if="metaChips.length > 0" class="channel-overview-card__chips">
            <span v-for="chip in metaChips" :key="chip" class="channel-overview-card__chip">{{ chip }}</span>
          </div>
        </div>
      </button>

      <ion-button
        class="channel-overview-card__action"
        :class="{ 'channel-overview-card__action--soft': item.isConnected }"
        size="small"
        @click.stop="$emit('openConnect')"
      >
        {{ actionText }}
      </ion-button>
    </ion-card-content>
  </ion-card>
</template>

<script setup lang="ts">
import { IonButton, IonCard, IonCardContent } from '@ionic/vue'
import { computed } from 'vue'
import { normalizeChannelCode, type ChannelViewModel } from '@/components/channel/channelUtils'

const props = defineProps<{
  item: ChannelViewModel
}>()

defineEmits<{
  openDetail: []
  openConnect: []
}>()

type BrandPalette = {
  accent: string
  soft: string
  border: string
  buttonText: string
}

const DEFAULT_BRAND_PALETTE: BrandPalette = {
  accent: '#3f7cff',
  soft: 'rgba(63, 124, 255, 0.12)',
  border: 'rgba(63, 124, 255, 0.16)',
  buttonText: '#ffffff',
}

function resolveBrandPalette(item: ChannelViewModel): BrandPalette {
  const code = normalizeChannelCode(item.code)
  const name = item.name.toLowerCase()

  if (code.includes('BOOKING') || name.includes('booking')) {
    return {
      accent: '#0758ad',
      soft: '#e7eef8',
      border: '#dce5f1',
      buttonText: '#ffffff',
    }
  }

  if (code.includes('AIRBNB') || name.includes('airbnb')) {
    return {
      accent: '#ff5a5f',
      soft: '#ffe4e4',
      border: '#ffd8d9',
      buttonText: '#ffffff',
    }
  }

  if (code.includes('AGODA') || name.includes('agoda')) {
    return {
      accent: '#c247b2',
      soft: '#f4d8ef',
      border: '#edcbe7',
      buttonText: '#ffffff',
    }
  }

  if (code.includes('TRIP') || name.includes('trip.com') || name.includes('ctrip')) {
    return {
      accent: '#246cfa',
      soft: 'rgba(36, 108, 250, 0.12)',
      border: 'rgba(36, 108, 250, 0.16)',
      buttonText: '#ffffff',
    }
  }

  if (code.includes('MEITUAN') || name.includes('美团')) {
    return {
      accent: '#ffc928',
      soft: 'rgba(255, 201, 40, 0.18)',
      border: 'rgba(255, 201, 40, 0.24)',
      buttonText: '#382600',
    }
  }

  if (code.includes('TUJIA') || name.includes('途家')) {
    return {
      accent: '#ff8a29',
      soft: 'rgba(255, 138, 41, 0.15)',
      border: 'rgba(255, 138, 41, 0.2)',
      buttonText: '#ffffff',
    }
  }

  if (code.includes('XIAOZHU') || name.includes('小猪')) {
    return {
      accent: '#ff5f8d',
      soft: 'rgba(255, 95, 141, 0.13)',
      border: 'rgba(255, 95, 141, 0.18)',
      buttonText: '#ffffff',
    }
  }

  if (name.includes('木鸟') || code.includes('MUNIAO')) {
    return {
      accent: '#ff7a4b',
      soft: 'rgba(255, 122, 75, 0.13)',
      border: 'rgba(255, 122, 75, 0.18)',
      buttonText: '#ffffff',
    }
  }

  return DEFAULT_BRAND_PALETTE
}

const palette = computed(() => resolveBrandPalette(props.item))

const brandVars = computed(() => ({
  '--channel-brand-accent': palette.value.accent,
  '--channel-brand-soft': palette.value.soft,
  '--channel-brand-border': palette.value.border,
  '--channel-brand-button-text': palette.value.buttonText,
}))

const showState = computed(() => props.item.isConnected)

const compactStatusLabel = computed(() => {
  if (!props.item.isConnected) {
    return ''
  }

  if (props.item.mappingReady) {
    return '已设置'
  }

  if (props.item.mappingStatus && props.item.suChannelId) {
    return '待补齐'
  }

  return '已连接'
})

const stateClass = computed(() => {
  if (props.item.mappingReady) {
    return 'channel-overview-card__state--ready'
  }

  if (!props.item.isConnected) {
    return 'channel-overview-card__state--idle'
  }

  return 'channel-overview-card__state--partial'
})

const summaryText = computed(() => {
  if (!props.item.isConnected) {
    return ''
  }

  if (props.item.mappingReady) {
    return '已完成授权与核心映射。'
  }

  if (props.item.mappingStatus && props.item.suChannelId) {
    return '已授权，继续补齐映射。'
  }

  return '已授权，可进入详情继续管理。'
})

const showSummary = computed(() => Boolean(summaryText.value))

const metaChips = computed(() => {
  if (!props.item.mappingStatus) {
    return []
  }

  const chips: string[] = []
  const roomCount = props.item.mappingStatus.mappedRoomIdCount
  const rateCount = props.item.mappingStatus.activeRatePlanCount

  if (roomCount > 0) {
    chips.push(`房型 ${roomCount}`)
  }

  if (rateCount > 0) {
    chips.push(`价盘 ${rateCount}`)
  }

  return chips
})

const actionText = computed(() => {
  if (!props.item.isConnected) {
    return '授权'
  }

  if (props.item.mappingReady) {
    return '管理'
  }

  return '继续'
})
</script>

<style scoped>
.channel-overview-card {
  margin: 0;
  border: 1px solid rgba(217, 226, 239, 0.28);
  border-radius: 5px;
  box-shadow: none;
  background: #ffffff;
  overflow: hidden;
}

.channel-overview-card__content {
  --channel-logo-width: clamp(124px, 40vw, 152px);
  --channel-column-gap: clamp(18px, 8.4vw, 34px);
  display: grid;
  grid-template-columns: var(--channel-logo-width) minmax(0, 1fr);
  grid-template-rows: minmax(0, 1fr) 24px;
  column-gap: var(--channel-column-gap);
  row-gap: 9px;
  min-height: 102px;
  padding: 18px 14px 20px;
}

.channel-overview-card__main {
  grid-column: 1 / -1;
  grid-row: 1 / -1;
  display: grid;
  grid-template-columns: var(--channel-logo-width) minmax(0, 1fr);
  align-items: start;
  column-gap: var(--channel-column-gap);
  min-width: 0;
  width: 100%;
  padding: 0;
  border: 0;
  background: transparent;
  color: inherit;
  text-align: left;
}

.channel-overview-card__body {
  display: grid;
  align-content: start;
  min-width: 0;
  padding-top: 4px;
}

.channel-overview-card__headline {
  display: block;
  align-items: center;
  min-width: 0;
}

.channel-overview-card__headline h3,
.channel-overview-card__summary {
  margin: 0;
}

.channel-overview-card__headline h3 {
  min-width: 0;
  color: #303030;
  font-size: 20px;
  font-weight: 700;
  letter-spacing: 0;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  line-height: 1.25;
  text-overflow: initial;
}

.channel-overview-card__summary {
  display: none;
}

.channel-overview-card__logo-wrap {
  width: var(--channel-logo-width);
  aspect-ratio: 3 / 2;
  border: 1px solid #d9d9d9;
  border-radius: 5px;
  background: #ffffff;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  flex-shrink: 0;
}

.channel-overview-card__logo {
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.channel-overview-card__chips {
  display: flex;
  flex-wrap: nowrap;
  gap: 14px;
  margin-top: 8px;
}

.channel-overview-card__chip {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 0;
  min-height: 20px;
  padding: 0 10px;
  border-radius: 4px;
  background: var(--channel-brand-soft);
  color: var(--channel-brand-accent, var(--ion-color-primary));
  font-size: 12px;
  font-weight: 400;
  line-height: 20px;
  white-space: nowrap;
}

.channel-overview-card__state {
  display: none;
}

.channel-overview-card__state--ready {
  background: rgba(23, 166, 115, 0.12);
  color: #16835f;
}

.channel-overview-card__state--partial {
  background: rgba(255, 186, 56, 0.18);
  color: #94620d;
}

.channel-overview-card__action {
  grid-column: 2;
  grid-row: 2;
  z-index: 2;
  width: 100%;
  height: 24px;
  min-width: 0;
  min-height: 24px;
  margin: 0;
  --background: var(--channel-brand-accent, var(--ion-color-primary));
  --color: var(--channel-brand-button-text, #ffffff);
  --box-shadow: none;
  --border-radius: 4px;
  --padding-start: 8px;
  --padding-end: 8px;
  font-size: 13px;
  font-weight: 400;
  letter-spacing: 0;
}

.channel-overview-card__action--soft {
  --background: var(--channel-brand-accent, var(--ion-color-primary));
  --color: var(--channel-brand-button-text, #ffffff);
}

.channel-overview-card__main:active {
  opacity: 0.88;
}

@media (max-width: 360px) {
  .channel-overview-card__content {
    --channel-logo-width: clamp(118px, 40vw, 136px);
    --channel-column-gap: 18px;
    padding-right: 12px;
    padding-left: 12px;
  }

  .channel-overview-card__headline h3 {
    font-size: 18px;
  }

  .channel-overview-card__chips {
    gap: 6px;
  }

  .channel-overview-card__chip {
    padding-right: 7px;
    padding-left: 7px;
    font-size: 11px;
  }
}
</style>
