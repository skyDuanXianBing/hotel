<template>
  <ion-card class="channel-overview-card">
    <ion-card-content>
      <div class="channel-overview-card__header">
        <div class="channel-overview-card__brand">
          <div class="channel-overview-card__logo-wrap">
            <img :src="item.logoUrl" :alt="item.name" class="channel-overview-card__logo" />
          </div>
          <div>
            <h3>{{ item.name }}</h3>
            <p>{{ item.code }}</p>
          </div>
        </div>
        <ion-badge :color="item.statusColor">{{ item.statusLabel }}</ion-badge>
      </div>

      <div class="channel-overview-card__meta">
        <p>{{ item.statusDescription }}</p>
        <p>最近状态更新：{{ item.lastStatusText }}</p>
      </div>

      <div class="channel-overview-card__chips" v-if="item.mappingStatus">
        <span class="channel-overview-card__chip">房型 {{ item.mappingStatus.mappedRoomIdCount }}</span>
        <span class="channel-overview-card__chip">价盘 {{ item.mappingStatus.activeRatePlanCount }}</span>
      </div>

      <div class="channel-overview-card__actions">
        <ion-button fill="outline" size="small" @click="$emit('openDetail')">查看详情</ion-button>
        <ion-button size="small" @click="$emit('openConnect')">{{ item.actionLabel }}</ion-button>
      </div>
    </ion-card-content>
  </ion-card>
</template>

<script setup lang="ts">
import { IonBadge, IonButton, IonCard, IonCardContent } from '@ionic/vue'
import type { ChannelViewModel } from '@/components/channel/channelUtils'

defineProps<{
  item: ChannelViewModel
}>()

defineEmits<{
  openDetail: []
  openConnect: []
}>()
</script>

<style scoped>
.channel-overview-card {
  margin: 0;
  border-radius: 24px;
  border: 1px solid var(--app-border);
  box-shadow: none;
  background: var(--app-surface);
}

.channel-overview-card__header,
.channel-overview-card__actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.channel-overview-card__brand {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
}

.channel-overview-card__brand h3,
.channel-overview-card__brand p,
.channel-overview-card__meta p {
  margin: 0;
}

.channel-overview-card__brand h3 {
  font-size: 16px;
  color: var(--app-heading);
}

.channel-overview-card__brand p,
.channel-overview-card__meta p {
  color: var(--app-muted);
  font-size: 13px;
  line-height: 1.5;
}

.channel-overview-card__logo-wrap {
  width: 54px;
  height: 54px;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.94);
  border: 1px solid var(--app-border);
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  flex-shrink: 0;
}

.channel-overview-card__logo {
  width: 40px;
  height: 40px;
  object-fit: contain;
}

.channel-overview-card__meta {
  display: grid;
  gap: 4px;
  margin-top: 14px;
}

.channel-overview-card__chips {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 14px;
}

.channel-overview-card__chip {
  padding: 6px 10px;
  border-radius: 999px;
  background: var(--app-primary-soft);
  color: var(--ion-color-primary);
  font-size: 12px;
  font-weight: 600;
}

.channel-overview-card__actions {
  margin-top: 16px;
}
</style>
