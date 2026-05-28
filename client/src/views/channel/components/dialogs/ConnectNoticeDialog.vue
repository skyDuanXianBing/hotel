<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'

const props = defineProps<{
  modelValue: boolean
  channel: { name: string; logoText?: string; logoClass?: string } | null
}>()

defineEmits<{
  'update:modelValue': [value: boolean]
  confirm: []
}>()

const { t } = useI18n()
const channelName = computed(() => props.channel?.name || '')
</script>

<template>
  <el-dialog
    :model-value="modelValue"
    :title="t('channel.dialogs.connectNotice.title', { name: channelName })"
    width="700px"
    :close-on-click-modal="false"
    @update:model-value="$emit('update:modelValue', $event)"
  >
    <div class="connect-dialog-content">
      <div class="connect-header">
        <p class="connect-description">
          {{ t('channel.dialogs.connectNotice.description', { name: channelName }) }}
        </p>
        <div v-if="channel?.logoClass" class="channel-logo-large" :class="channel.logoClass">
          <span class="logo-text-large">{{ channel?.logoText }}</span>
        </div>
      </div>

      <!-- 直连须知内容 -->
      <div class="notice-section">
        <h3 class="notice-title">
          {{ t('channel.dialogs.connectNotice.noticeTitle', { name: channelName }) }}
        </h3>
        <div class="notice-content">
          <div class="notice-text">
            <p>{{ t('channel.dialogs.connectNotice.paragraph1', { name: channelName }) }}</p>
            <p>{{ t('channel.dialogs.connectNotice.paragraph2', { name: channelName }) }}</p>

            <h4>{{ t('channel.dialogs.connectNotice.featureTitle') }}</h4>
            <p>{{ t('channel.dialogs.connectNotice.feature1', { name: channelName }) }}</p>
            <p>{{ t('channel.dialogs.connectNotice.feature2', { name: channelName }) }}</p>
            <p>{{ t('channel.dialogs.connectNotice.feature3', { name: channelName }) }}</p>

            <h4>{{ t('channel.dialogs.connectNotice.changeTitle') }}</h4>
            <p>{{ t('channel.dialogs.connectNotice.changeText') }}</p>

            <h4>{{ t('channel.dialogs.connectNotice.disclaimerTitle') }}</h4>
            <p>{{ t('channel.dialogs.connectNotice.disclaimerText') }}</p>

            <h4>{{ t('channel.dialogs.connectNotice.lawTitle') }}</h4>
            <p>{{ t('channel.dialogs.connectNotice.lawText') }}</p>
          </div>
        </div>
      </div>
    </div>

    <template #footer>
      <div class="dialog-footer">
        <el-button @click="$emit('update:modelValue', false)">{{ t('channel.dialogs.common.cancel') }}</el-button>
        <el-button type="primary" @click="$emit('confirm')">
          {{ t('channel.dialogs.connectNotice.agree') }}
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<style scoped>
.connect-dialog-content {
  padding: 0;
}

.connect-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 20px;
  padding: 0 20px 20px;
  border-bottom: 1px solid #f0f0f0;
}

.connect-description {
  flex: 1;
  font-size: 14px;
  color: #666;
  line-height: 1.6;
  margin: 0;
}

.channel-logo-large {
  width: 80px;
  height: 80px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.logo-text-large {
  font-size: 18px;
  font-weight: bold;
  color: white;
}

.notice-section {
  padding: 0 20px;
}

.notice-title {
  font-size: 16px;
  font-weight: 500;
  color: #333;
  margin: 20px 0 16px;
  text-align: center;
}

.notice-content {
  max-height: 400px;
  overflow-y: auto;
  padding-right: 8px;
}

.notice-content::-webkit-scrollbar {
  width: 6px;
}

.notice-content::-webkit-scrollbar-track {
  background: #f1f1f1;
  border-radius: 3px;
}

.notice-content::-webkit-scrollbar-thumb {
  background: #c1c1c1;
  border-radius: 3px;
}

.notice-content::-webkit-scrollbar-thumb:hover {
  background: #a8a8a8;
}

.notice-text {
  font-size: 13px;
  line-height: 1.8;
  color: #333;
}

.notice-text p {
  margin: 0 0 16px 0;
  text-indent: 0;
  text-align: justify;
}

.notice-text h4 {
  font-size: 14px;
  font-weight: 500;
  color: #333;
  margin: 24px 0 12px 0;
  line-height: 1.6;
}

.notice-text h4:first-of-type {
  margin-top: 0;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}
</style>
