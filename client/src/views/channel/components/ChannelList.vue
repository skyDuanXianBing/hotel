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
  min-height: calc(100vh - 100px);
  height: 100%;
  overflow: auto;
  background: #ffffff;
  box-sizing: border-box;
  padding: 28px clamp(20px, 2.6vw, 36px);
}

.channel-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
  width: 100%;
  max-width: 1320px;
  margin: 0 auto;
}

.channel-card {
  min-height: 148px;
  display: flex;
  align-items: stretch;
  justify-content: space-between;
  gap: 14px;
  box-sizing: border-box;
  padding: 22px 14px 20px 22px;
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
  flex: 1 1 auto;
  min-width: 0;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  justify-content: space-between;
}

.channel-name {
  width: 100%;
  max-width: 170px;
  margin: 0;
  color: #303133;
  font-size: 24px;
  font-weight: 700;
  line-height: 1.25;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.channel-status {
  display: flex;
  align-items: center;
  gap: 9px;
  color: #303133;
  font-size: 15px;
  line-height: 20px;
}

.status-dot {
  width: 7px;
  height: 7px;
  flex: 0 0 7px;
  border-radius: 50%;
}

.status-dot.connected {
  background: #57d879;
}

.status-dot.disconnected {
  background: #d9d9d9;
}

.config-btn {
  min-width: 88px;
  height: 28px;
  margin: 0;
  border-radius: 4px;
  font-size: 14px;
}

.channel-logo-wrapper {
  width: clamp(136px, 11.4vw, 170px);
  height: 104px;
  flex: 0 0 clamp(136px, 11.4vw, 170px);
  display: flex;
  align-items: center;
  justify-content: center;
  align-self: center;
  box-sizing: border-box;
  padding: 14px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  background: #ffffff;
}

.channel-logo-img {
  max-width: 100%;
  max-height: 100%;
  object-fit: contain;
}

@media (max-width: 1060px) {
  .channel-list-panel {
    padding: 24px;
  }

  .channel-grid {
    grid-template-columns: repeat(2, minmax(300px, 1fr));
  }
}

@media (max-width: 980px) {
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
    max-width: 100%;
    font-size: 22px;
  }
}
</style>
