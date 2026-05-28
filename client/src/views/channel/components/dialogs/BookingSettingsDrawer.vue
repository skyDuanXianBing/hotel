<script setup lang="ts">
import { ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { QuestionFilled } from '@element-plus/icons-vue'
import type { BookingSettings } from '../../types'

const props = defineProps<{
  modelValue: boolean
  settings: BookingSettings
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  save: [data: BookingSettings]
}>()

const { t } = useI18n()

/** 本地编辑副本，避免直接修改 prop */
const localForm = ref<BookingSettings>({ ...props.settings })

watch(
  () => props.settings,
  (val) => {
    localForm.value = { ...val }
  },
)

/** 重置并关闭 */
function handleClose() {
  localForm.value = { ...props.settings }
  emit('update:modelValue', false)
}

function handleSave() {
  emit('save', { ...localForm.value })
}
</script>

<template>
  <el-drawer
    :model-value="modelValue"
    :title="t('channel.dialogs.bookingSettings.title')"
    direction="rtl"
    size="400px"
    :before-close="handleClose"
    @update:model-value="$emit('update:modelValue', $event)"
  >
    <div class="booking-settings-form">
      <!-- 提前预订量 -->
      <div class="form-item">
        <div class="form-label">
          <span>{{ t('channel.dialogs.bookingSettings.advanceBooking') }}</span>
          <el-icon class="help-icon"><QuestionFilled /></el-icon>
        </div>
        <div class="form-desc">{{ t('channel.dialogs.bookingSettings.advanceBookingDesc') }}</div>
        <div class="form-control">
          <el-select v-model="localForm.advanceBookingHours" :placeholder="t('channel.dialogs.bookingSettings.select')">
            <el-option
              v-for="i in [1, 2, 3, 6, 12, 24, 48]"
              :key="i"
              :label="t('channel.dialogs.bookingSettings.hours', { count: i })"
              :value="i"
            />
          </el-select>
          <span class="control-unit">{{ t('channel.dialogs.bookingSettings.hourUnit') }}</span>
        </div>
      </div>

      <!-- 未在邀请时间完成 -->
      <div class="form-item">
        <div class="form-desc">{{ t('channel.dialogs.bookingSettings.approvalDesc') }}</div>
        <el-radio-group v-model="localForm.requireApproval">
          <el-radio :value="true">{{ t('channel.dialogs.bookingSettings.yes') }}</el-radio>
          <el-radio :value="false">{{ t('channel.dialogs.bookingSettings.no') }}</el-radio>
        </el-radio-group>
      </div>

      <!-- 准备时间 -->
      <div class="form-item">
        <div class="form-label">
          <span>{{ t('channel.dialogs.bookingSettings.preparationTime') }}</span>
        </div>
        <div class="form-desc">{{ t('channel.dialogs.bookingSettings.preparationDesc') }}</div>
        <div class="form-control">
          <el-select v-model="localForm.preparationNights" :placeholder="t('channel.dialogs.bookingSettings.select')">
            <el-option :label="t('channel.dialogs.bookingSettings.none')" :value="0" />
            <el-option
              v-for="i in [1, 2, 3, 5, 7]"
              :key="i"
              :label="t('channel.dialogs.bookingSettings.nights', { count: i })"
              :value="i"
            />
          </el-select>
          <span class="control-unit">{{ t('channel.dialogs.bookingSettings.nightUnit') }}</span>
        </div>
      </div>

      <!-- 预订开放期 -->
      <div class="form-item">
        <div class="form-label">
          <span>{{ t('channel.dialogs.bookingSettings.bookingWindow') }}</span>
          <el-icon class="help-icon"><QuestionFilled /></el-icon>
        </div>
        <div class="form-desc">{{ t('channel.dialogs.bookingSettings.bookingWindowDesc') }}</div>
        <div class="form-control">
          <el-select v-model="localForm.bookingWindowDays" :placeholder="t('channel.dialogs.bookingSettings.select')">
            <el-option
              v-for="i in [30, 60, 90, 180, 365, 730]"
              :key="i"
              :label="t('channel.dialogs.bookingSettings.days', { count: i })"
              :value="i"
            />
          </el-select>
          <span class="control-unit">{{ t('channel.dialogs.bookingSettings.dayUnit') }}</span>
        </div>
      </div>

      <!-- 入住窗口 -->
      <div class="form-item">
        <div class="form-label">
          <span>{{ t('channel.dialogs.bookingSettings.checkInWindow') }}</span>
        </div>

        <!-- 入住开始时间 -->
        <div class="form-subitem">
          <div class="form-desc">{{ t('channel.dialogs.bookingSettings.checkInStart') }}</div>
          <el-select v-model="localForm.checkInStartTime" :placeholder="t('channel.dialogs.bookingSettings.select')">
            <el-option
              v-for="hour in 24"
              :key="hour"
              :label="`${String(hour - 1).padStart(2, '0')}:00`"
              :value="`${String(hour - 1).padStart(2, '0')}:00`"
            />
          </el-select>
        </div>

        <!-- 入住结束时间 -->
        <div class="form-subitem">
          <div class="form-desc">{{ t('channel.dialogs.bookingSettings.checkInEnd') }}</div>
          <el-select v-model="localForm.checkInEndTime" :placeholder="t('channel.dialogs.bookingSettings.select')">
            <el-option :label="t('channel.dialogs.bookingSettings.unlimited')" value="" />
            <el-option
              v-for="hour in 24"
              :key="hour"
              :label="`${String(hour - 1).padStart(2, '0')}:00`"
              :value="`${String(hour - 1).padStart(2, '0')}:00`"
            />
          </el-select>
        </div>
      </div>

      <!-- 离店时间 -->
      <div class="form-item">
        <div class="form-label">
          <span>{{ t('channel.dialogs.bookingSettings.checkOutTime') }}</span>
        </div>
        <div class="form-subitem">
          <div class="form-desc">{{ t('channel.dialogs.bookingSettings.beforeCheckOut') }}</div>
          <el-select v-model="localForm.checkOutTime" :placeholder="t('channel.dialogs.bookingSettings.select')">
            <el-option
              v-for="hour in 24"
              :key="hour"
              :label="`${String(hour - 1).padStart(2, '0')}:00`"
              :value="`${String(hour - 1).padStart(2, '0')}:00`"
            />
          </el-select>
        </div>
      </div>
    </div>

    <!-- 抽屉底部按钮 -->
    <template #footer>
      <div class="drawer-footer">
        <el-button @click="handleClose">{{ t('channel.dialogs.common.cancel') }}</el-button>
        <el-button type="primary" @click="handleSave">{{ t('channel.dialogs.common.save') }}</el-button>
      </div>
    </template>
  </el-drawer>
</template>

<style scoped>
.booking-settings-form {
  padding: 0 20px;
}

.form-item {
  margin-bottom: 28px;
}

.form-label {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 15px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 8px;
}

.help-icon {
  font-size: 16px;
  color: #909399;
  cursor: help;
}

.form-desc {
  font-size: 13px;
  color: #606266;
  margin-bottom: 12px;
  line-height: 1.5;
}

.form-control {
  display: flex;
  align-items: center;
  gap: 8px;
}

.form-control .el-select {
  flex: 1;
}

.control-unit {
  font-size: 14px;
  color: #606266;
  white-space: nowrap;
}

.form-subitem {
  margin-top: 16px;
}

.form-subitem:first-child {
  margin-top: 0;
}

.form-subitem .form-desc {
  margin-bottom: 8px;
  font-size: 13px;
}

.form-subitem .el-select {
  width: 100%;
}

.drawer-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding: 12px 20px;
  border-top: 1px solid #e4e7ed;
}
</style>
