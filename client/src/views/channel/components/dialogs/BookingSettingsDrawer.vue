<script setup lang="ts">
import { ref, watch } from 'vue'
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
    title="预定设置"
    direction="rtl"
    size="400px"
    :before-close="handleClose"
    @update:model-value="$emit('update:modelValue', $event)"
  >
    <div class="booking-settings-form">
      <!-- 提前预订量 -->
      <div class="form-item">
        <div class="form-label">
          <span>提前预订量</span>
          <el-icon class="help-icon"><QuestionFilled /></el-icon>
        </div>
        <div class="form-desc">你需要客人提前多少小时预订?</div>
        <div class="form-control">
          <el-select v-model="localForm.advanceBookingHours" placeholder="请选择">
            <el-option
              v-for="i in [1, 2, 3, 6, 12, 24, 48]"
              :key="i"
              :label="`${i} 小时`"
              :value="i"
            />
          </el-select>
          <span class="control-unit">小时</span>
        </div>
      </div>

      <!-- 未在邀请时间完成 -->
      <div class="form-item">
        <div class="form-desc">未在您的邀请时间段完成的预订将变为"预订申请"。</div>
        <el-radio-group v-model="localForm.requireApproval">
          <el-radio :value="true">是</el-radio>
          <el-radio :value="false">否</el-radio>
        </el-radio-group>
      </div>

      <!-- 准备时间 -->
      <div class="form-item">
        <div class="form-label">
          <span>准备时间</span>
        </div>
        <div class="form-desc">预订日期和入住日之间隔最大天数。</div>
        <div class="form-control">
          <el-select v-model="localForm.preparationNights" placeholder="请选择">
            <el-option label="无" :value="0" />
            <el-option
              v-for="i in [1, 2, 3, 5, 7]"
              :key="i"
              :label="`${i} 晚`"
              :value="i"
            />
          </el-select>
          <span class="control-unit">晚</span>
        </div>
      </div>

      <!-- 预订开放期 -->
      <div class="form-item">
        <div class="form-label">
          <span>预订开放期</span>
          <el-icon class="help-icon"><QuestionFilled /></el-icon>
        </div>
        <div class="form-desc">预订日期和入住日之间隔最大天数。</div>
        <div class="form-control">
          <el-select v-model="localForm.bookingWindowDays" placeholder="请选择">
            <el-option
              v-for="i in [30, 60, 90, 180, 365, 730]"
              :key="i"
              :label="`${i} 天`"
              :value="i"
            />
          </el-select>
          <span class="control-unit">天</span>
        </div>
      </div>

      <!-- 入住窗口 -->
      <div class="form-item">
        <div class="form-label">
          <span>入住窗口</span>
        </div>

        <!-- 入住开始时间 -->
        <div class="form-subitem">
          <div class="form-desc">入住开始时间</div>
          <el-select v-model="localForm.checkInStartTime" placeholder="请选择">
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
          <div class="form-desc">入住结束时间</div>
          <el-select v-model="localForm.checkInEndTime" placeholder="请选择">
            <el-option label="不限" value="" />
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
          <span>离店时间</span>
        </div>
        <div class="form-subitem">
          <div class="form-desc">之前离店</div>
          <el-select v-model="localForm.checkOutTime" placeholder="请选择">
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
        <el-button @click="handleClose">取消</el-button>
        <el-button type="primary" @click="handleSave">保存</el-button>
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
