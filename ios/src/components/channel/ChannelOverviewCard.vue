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
      accent: '#2948a8',
      soft: 'rgba(41, 72, 168, 0.12)',
      border: 'rgba(41, 72, 168, 0.16)',
      buttonText: '#ffffff',
    }
  }

  if (code.includes('AIRBNB') || name.includes('airbnb')) {
    return {
      accent: '#ff5a5f',
      soft: 'rgba(255, 90, 95, 0.12)',
      border: 'rgba(255, 90, 95, 0.16)',
      buttonText: '#ffffff',
    }
  }

  if (code.includes('AGODA') || name.includes('agoda')) {
    return {
      accent: '#7d4bff',
      soft: 'rgba(125, 75, 255, 0.11)',
      border: 'rgba(125, 75, 255, 0.15)',
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
  border-radius: 28px;
  border: 1px solid var(--channel-brand-border, var(--app-border));
  box-shadow: none;
  background: rgba(255, 255, 255, 0.96);
}

.channel-overview-card__content {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: center;
  gap: 14px;
  padding: 18px 18px 18px 16px;
}

.channel-overview-card__main {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  align-items: center;
  gap: 14px;
  min-width: 0;
  padding: 0;
  border: 0;
  background: transparent;
  color: inherit;
  text-align: left;
}

.channel-overview-card__body {
  display: grid;
  align-content: center;
  min-width: 0;
}

.channel-overview-card__headline {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.channel-overview-card__headline h3,
.channel-overview-card__summary {
  margin: 0;
}

.channel-overview-card__headline h3 {
  min-width: 0;
  font-size: 18px;
  font-weight: 800;
  letter-spacing: -0.02em;
  color: var(--app-heading);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  line-height: 1.18;
  text-overflow: initial;
}

.channel-overview-card__summary {
  margin-top: 6px;
  color: var(--app-muted);
  font-size: 13px;
  line-height: 1.5;
}

.channel-overview-card__logo-wrap {
  width: 60px;
  height: 60px;
  border-radius: 20px;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.96), var(--channel-brand-soft));
  border: 1px solid var(--channel-brand-border, var(--app-border));
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  flex-shrink: 0;
}

.channel-overview-card__logo {
  width: 44px;
  height: 44px;
  object-fit: contain;
}

.channel-overview-card__chips {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 10px;
}

.channel-overview-card__chip {
  padding: 4px 9px;
  border-radius: 999px;
  background: var(--channel-brand-soft);
  color: var(--channel-brand-accent, var(--ion-color-primary));
  font-size: 11px;
  font-weight: 600;
}

.channel-overview-card__state {
  flex-shrink: 0;
  padding: 4px 8px;
  border-radius: 999px;
  font-size: 11px;
  font-weight: 700;
  line-height: 1;
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
  flex-shrink: 0;
  min-width: 72px;
  --background: var(--channel-brand-accent, var(--ion-color-primary));
  --color: var(--channel-brand-button-text, #ffffff);
  --box-shadow: none;
  --border-radius: 18px;
  --padding-start: 14px;
  --padding-end: 14px;
  font-size: 13px;
  font-weight: 700;
}

.channel-overview-card__action--soft {
  --background: var(--channel-brand-soft);
  --color: var(--channel-brand-accent, var(--ion-color-primary));
}

.channel-overview-card__main:active .channel-overview-card__logo-wrap {
  transform: scale(0.98);
}

@media (max-width: 360px) {
  .channel-overview-card__content {
    gap: 10px;
    padding-right: 14px;
  }

  .channel-overview-card__logo-wrap {
    width: 56px;
    height: 56px;
  }

  .channel-overview-card__headline {
    gap: 6px;
  }

  .channel-overview-card__headline h3 {
    font-size: 17px;
  }

  .channel-overview-card__action {
    min-width: 64px;
    --padding-start: 12px;
    --padding-end: 12px;
  }
}
</style>
