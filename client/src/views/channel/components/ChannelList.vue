<template>
  <div class="channel-list-panel">
    <div class="channel-grid">
      <div
        v-for="channel in channels"
        :key="channel.id"
        class="channel-card"
        role="button"
        tabindex="0"
        @click="handleCardClick(channel)"
        @keydown.enter.prevent="handleCardClick(channel)"
        @keydown.space.prevent="handleCardClick(channel)"
      >
        <div class="channel-info">
          <h3 class="channel-name">{{ channel.name }}</h3>
          <div class="channel-status">
            <span class="status-dot" :class="channel.connected ? 'connected' : 'disconnected'" />
            <span class="status-text">
              {{ channel.connected ? t('channel.list.connected') : t('channel.list.disconnected') }}
            </span>
          </div>
          <el-button
            type="primary"
            size="small"
            class="config-btn"
            @click.stop="handleConnect(channel)"
          >
            {{ channel.connected ? t('channel.list.reconnect') : t('channel.list.configure') }}
          </el-button>
        </div>

        <div class="channel-logo-wrapper">
          <img :src="channel.logoUrl" :alt="channel.name" class="channel-logo-img" />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import type { ChannelItem } from '../types'

defineProps<{
  channels: ChannelItem[]
}>()

const emit = defineEmits<{
  connect: [channel: ChannelItem]
  manage: [channel: ChannelItem]
}>()

const { t } = useI18n()

function handleCardClick(channel: ChannelItem) {
  if (channel.connected) {
    emit('manage', channel)
    return
  }

  emit('connect', channel)
}

function handleConnect(channel: ChannelItem) {
  emit('connect', channel)
}
</script>

<style scoped>
.channel-list-panel {
  height: 100%;
  overflow: auto;
  background: #ffffff;
  padding: 46px 96px;
}

.channel-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(320px, 1fr));
  gap: 24px;
  max-width: 1160px;
  margin: 0 auto;
}

.channel-card {
  min-height: 166px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
  padding: 26px 10px 20px 32px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  background: #ffffff;
  cursor: pointer;
  transition:
    border-color 0.2s ease,
    box-shadow 0.2s ease,
    transform 0.2s ease;
}

.channel-card:hover,
.channel-card:focus-visible {
  border-color: #409eff;
  box-shadow: 0 8px 20px rgba(64, 158, 255, 0.12);
  outline: none;
  transform: translateY(-1px);
}

.channel-info {
  min-width: 0;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 13px;
}

.channel-name {
  max-width: 260px;
  margin: 0;
  color: #303133;
  font-size: 34px;
  font-weight: 700;
  line-height: 1.18;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.channel-status {
  display: flex;
  align-items: center;
  gap: 10px;
  color: #303133;
  font-size: 16px;
  line-height: 1;
}

.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
}

.status-dot.connected {
  background: #67c23a;
}

.status-dot.disconnected {
  background: #d9d9d9;
}

.config-btn {
  min-width: 98px;
  height: 28px;
  margin: 0;
  border-radius: 4px;
  font-size: 14px;
}

.channel-logo-wrapper {
  width: 176px;
  height: 120px;
  flex: 0 0 176px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 18px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  background: #ffffff;
}

.channel-logo-img {
  max-width: 100%;
  max-height: 100%;
  object-fit: contain;
}

@media (max-width: 1180px) {
  .channel-list-panel {
    padding: 32px;
  }

  .channel-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .channel-list-panel {
    padding: 16px;
  }

  .channel-card {
    min-height: auto;
    flex-direction: column;
    align-items: flex-start;
    gap: 18px;
    padding: 22px;
  }

  .channel-logo-wrapper {
    width: 100%;
    max-width: 180px;
    height: 90px;
    flex: 0 0 auto;
  }

  .channel-name {
    max-width: 180px;
    font-size: 26px;
  }
}
</style>
