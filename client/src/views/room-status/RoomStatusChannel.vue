<template>
  <div class="room-status-channel">
    <div class="header-notice">
      <el-alert
        :title="t('pages.roomStatusChannel.notice')"
        type="info"
        :closable="false"
        show-icon
      />
    </div>

    <div class="toolbar">
      <div class="date-section">
        <label>{{ t('pages.roomStatusChannel.date') }}</label>
        <el-date-picker
          v-model="selectedDate"
          type="date"
          :placeholder="t('pages.roomStatusChannel.datePlaceholder')"
          format="YYYY-MM-DD"
          value-format="YYYY-MM-DD"
          disabled
        />
      </div>

      <div class="room-type-filter">
        <el-select
          v-model="selectedRoomType"
          :placeholder="t('pages.roomStatusChannel.localRoomTypePlaceholder')"
          disabled
        >
          <el-option :label="t('pages.roomStatusChannel.all')" value="" />
        </el-select>
      </div>

      <div class="actions">
        <el-button type="primary" disabled :title="emptyStateDescription">
          {{ t('pages.roomStatusChannel.statusLog') }}
        </el-button>
        <el-button type="primary" disabled :title="emptyStateDescription">
          {{ t('pages.roomStatusChannel.bulkEdit') }}
        </el-button>
      </div>
    </div>

    <div class="channel-content">
      <div class="channel-placeholder">
        <div class="empty-icon">
          <el-icon size="48"><Box /></el-icon>
        </div>
        <h3>{{ t('pages.roomStatusChannel.unavailableTitle') }}</h3>
        <p>{{ t('pages.roomStatusChannel.unavailableDescription') }}</p>
        <p class="empty-tip">{{ emptyStateDescription }}</p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { Box } from '@element-plus/icons-vue'

const { t } = useI18n()

const selectedDate = ref<string>(new Date().toISOString().split('T')[0])
const selectedRoomType = ref<string>('')
const emptyStateDescription = computed(
  () => t('pages.roomStatusChannel.unavailableActionTip'),
)
</script>

<style scoped>
.room-status-channel {
  padding: 20px;
  background: #f5f5f5;
  min-height: 100vh;
}

.header-notice {
  margin-bottom: 20px;
}

.toolbar {
  display: flex;
  align-items: center;
  gap: 20px;
  margin-bottom: 20px;
  padding: 15px;
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.date-section {
  display: flex;
  align-items: center;
  gap: 10px;
}

.date-section label {
  font-weight: bold;
  color: #333;
}

.room-type-filter {
  flex: 1;
}

.actions {
  display: flex;
  gap: 10px;
}

.channel-content {
  background: white;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.channel-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 360px;
  padding: 48px 24px;
  color: #999;
  text-align: center;
}

.channel-placeholder h3 {
  margin: 0 0 10px;
  color: #303133;
  font-size: 18px;
}

.channel-placeholder p {
  margin: 0;
  max-width: 640px;
  color: #606266;
  line-height: 1.7;
}

.empty-tip {
  margin-top: 12px;
  font-size: 13px;
  color: #909399;
}
</style>
