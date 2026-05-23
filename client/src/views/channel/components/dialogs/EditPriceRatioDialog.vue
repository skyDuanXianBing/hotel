<script setup lang="ts">
import { ref, watch } from 'vue'
import type { PriceRatioEdit } from '../../types'

const props = defineProps<{
  modelValue: boolean
  editData: PriceRatioEdit | null
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  save: [data: PriceRatioEdit]
}>()

/** 本地编辑副本，避免直接修改 prop */
const localData = ref<PriceRatioEdit | null>(null)

watch(
  () => props.editData,
  (val) => {
    localData.value = val ? { ...val } : null
  },
  { immediate: true },
)

function handleSave() {
  if (localData.value) {
    emit('save', { ...localData.value })
  }
}
</script>

<template>
  <el-dialog
    :model-value="modelValue"
    title="编辑价格比例"
    width="500px"
    @update:model-value="$emit('update:modelValue', $event)"
  >
    <div v-if="localData" class="edit-ratio-form">
      <div class="form-item">
        <div class="form-label">OTA名称</div>
        <div class="form-value">{{ localData.channel }}</div>
      </div>
      <div class="form-item">
        <div class="form-label">价格调整</div>
        <div class="form-value adjustment-row">
          <el-select v-model="localData.adjustmentType" class="adjustment-type-select">
            <el-option label="更便宜" value="cheaper" />
            <el-option label="更贵" value="expensive" />
          </el-select>
          <el-input-number
            v-model="localData.adjustmentValue"
            :min="0"
            :max="localData.adjustmentUnit === '%' ? 100 : 999999"
            :controls="false"
            class="adjustment-value-input"
            placeholder="数值"
          />
          <el-select v-model="localData.adjustmentUnit" class="adjustment-unit-select">
            <el-option label="%" value="%" />
            <el-option label="¥" value="¥" />
          </el-select>
        </div>
      </div>
    </div>
    <template #footer>
      <el-button @click="$emit('update:modelValue', false)">取消</el-button>
      <el-button type="primary" @click="handleSave">保存</el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.edit-ratio-form {
  padding: 20px;
}

.edit-ratio-form .form-item {
  margin-bottom: 20px;
}

.edit-ratio-form .form-item:last-child {
  margin-bottom: 0;
}

.edit-ratio-form .form-label {
  font-size: 14px;
  color: #606266;
  margin-bottom: 8px;
}

.edit-ratio-form .form-value {
  font-size: 14px;
  color: #303133;
}

.edit-ratio-form .adjustment-row {
  display: flex;
  align-items: center;
  gap: 12px;
}

.edit-ratio-form .adjustment-type-select {
  width: 120px;
}

.edit-ratio-form .adjustment-value-input {
  width: 100px;
}

.edit-ratio-form .adjustment-value-input :deep(.el-input__inner) {
  text-align: center;
}

.edit-ratio-form .adjustment-unit-select {
  width: 80px;
}
</style>
