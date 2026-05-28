<template>
  <div class="channel-list-panel">
    <div class="page-header">
      <h2>{{ t('channel.list.title') }}</h2>
      <div class="header-tabs">
        <el-tabs v-model="activeTab">
          <el-tab-pane :label="t('channel.list.otaTab')" name="ota" />
        </el-tabs>
      </div>
    </div>

    <!-- OTA渠道内容 -->
    <div v-if="activeTab === 'ota'" class="channel-content">
      <div class="channel-section">
        <h3 class="section-title">{{ t('channel.list.otaSection') }}</h3>
        <div class="channel-grid">
          <div
            v-for="channel in channels"
            :key="channel.id"
            class="channel-card"
          >
            <div class="channel-logo-wrapper">
              <img :src="channel.logoUrl" :alt="channel.name" class="channel-logo-img" />
            </div>
            <h4 class="channel-name">{{ channel.name }}</h4>
            <div class="channel-status">
              <span
                class="status-dot"
                :class="channel.connected ? 'connected' : 'disconnected'"
              />
              <span class="status-text">
                {{ channel.connected ? t('channel.list.connected') : t('channel.list.disconnected') }}
              </span>
            </div>
            <div class="channel-card-actions">
              <el-button
                v-if="channel.connected"
                type="primary"
                size="small"
                class="manage-btn"
                @click="$emit('manage', channel)"
              >
                {{ t('channel.list.manage') }}
              </el-button>
              <el-button
                :type="channel.connected ? 'default' : 'primary'"
                size="small"
                class="config-btn"
                @click="$emit('connect', channel)"
              >
                {{ channel.connected ? t('channel.list.reconnect') : t('channel.list.configure') }}
              </el-button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 查看介绍内容 -->
    <div v-if="activeTab === 'intro'" class="intro-content">
      <div class="intro-section">
        <h3>{{ t('channel.list.introTitle') }}</h3>
        <p>{{ t('channel.list.introLead') }}</p>
        <ul>
          <li>{{ t('channel.list.introItems.connect') }}</li>
          <li>{{ t('channel.list.introItems.status') }}</li>
          <li>{{ t('channel.list.introItems.pricing') }}</li>
          <li>{{ t('channel.list.introItems.orders') }}</li>
        </ul>
      </div>
      <div class="intro-section">
        <h3>{{ t('channel.list.supportedTitle') }}</h3>
        <div class="channel-category-list">
          <div v-for="category in categories" :key="category.name" class="category">
            <h4>{{ category.name }}</h4>
            <div class="channel-items">
              <span v-for="item in category.items" :key="item" class="channel-item">
                {{ item }}
              </span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import type { ChannelItem } from '../types'
import { CHANNEL_CATEGORIES } from '../constants'

defineProps<{
  channels: ChannelItem[]
}>()

defineEmits<{
  connect: [channel: ChannelItem]
  manage: [channel: ChannelItem]
}>()

const activeTab = ref('ota')
const { t } = useI18n()
const categoryNameMap: Record<string, string> = {
  domesticOta: 'channel.categories.domesticOta',
  homestay: 'channel.categories.homestay',
  internationalOta: 'channel.categories.internationalOta',
}
const categories = computed(() =>
  CHANNEL_CATEGORIES.map((category) => ({
    ...category,
    name: t(categoryNameMap[category.key] || category.name),
  })),
)
</script>

<style scoped>
.channel-list-panel {
  height: 100%;
  overflow: auto;
  background: #f5f5f5;
}

.page-header {
  background: white;
  padding: 20px 24px;
  border-bottom: 1px solid #e8e8e8;
}

.page-header h2 {
  margin: 0 0 16px 0;
  font-size: 20px;
  font-weight: 600;
  color: #1d2129;
}

.header-tabs :deep(.el-tabs__header) {
  margin: 0;
}

/* 渠道内容区域 */
.channel-content {
  padding: 24px;
}

.channel-section {
  margin-bottom: 40px;
}

.section-title {
  font-size: 15px;
  font-weight: 500;
  color: #606266;
  margin: 0 0 20px 0;
  padding-bottom: 12px;
  border-bottom: 1px solid #ebeef5;
}

/* 渠道网格 — 统一卡片尺寸 */
.channel-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 20px;
}

.channel-card {
  background: white;
  border: 1px solid #e8e8e8;
  border-radius: 8px;
  padding: 24px 20px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  transition: all 0.3s ease;
}

.channel-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
  border-color: #409eff;
}

.channel-logo-wrapper {
  width: 120px;
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #fff;
  border-radius: 4px;
  padding: 8px;
}

.channel-logo-img {
  max-width: 100%;
  max-height: 100%;
  object-fit: contain;
}

.channel-name {
  font-size: 14px;
  font-weight: 500;
  color: #303133;
  margin: 4px 0;
  text-align: center;
}

.channel-status {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: #909399;
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

.channel-card-actions {
  width: 100%;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 4px;
}

.channel-card-actions .el-button {
  flex: 1;
  min-width: 0;
  margin: 0;
  white-space: normal;
}

/* 介绍内容 */
.intro-content {
  padding: 24px;
  background: white;
  margin: 24px;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.intro-section {
  margin-bottom: 32px;
}

.intro-section:last-child {
  margin-bottom: 0;
}

.intro-section h3 {
  font-size: 18px;
  font-weight: 500;
  margin: 0 0 16px 0;
  color: #1d2129;
}

.intro-section p {
  font-size: 14px;
  color: #606266;
  margin: 0 0 12px 0;
}

.intro-section ul {
  margin: 0;
  padding-left: 20px;
}

.intro-section li {
  font-size: 14px;
  color: #606266;
  margin-bottom: 8px;
}

.channel-category-list {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.category h4 {
  font-size: 16px;
  font-weight: 500;
  margin: 0 0 12px 0;
  color: #1d2129;
}

.channel-items {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.channel-item {
  background: #f0f7ff;
  color: #1890ff;
  padding: 4px 12px;
  border-radius: 16px;
  font-size: 12px;
}

/* 响应式 */
@media (max-width: 768px) {
  .channel-grid {
    grid-template-columns: 1fr;
  }

  .channel-content {
    padding: 16px;
  }
}
</style>
