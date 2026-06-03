<template>
  <div class="room-group-statistic-scope-filter">
    <span class="scope-label">{{ t('stage5.dataCenter.overview.roomGroupDimension') }}</span>
    <el-select
      :model-value="modelValue"
      class="room-group-select"
      multiple
      collapse-tags
      collapse-tags-tooltip
      clearable
      filterable
      :loading="loading"
      :disabled="!loading && roomGroups.length === 0"
      :placeholder="t('stage5.dataCenter.overview.roomGroupPlaceholder')"
      :empty-text="t('stage5.dataCenter.overview.roomGroupEmpty')"
      @update:model-value="handleModelValueUpdate"
    >
      <el-option
        v-for="group in roomGroups"
        :key="group.id"
        :label="group.name"
        :value="group.id"
      />
    </el-select>
  </div>
</template>

<script setup lang="ts">
import { useI18n } from 'vue-i18n'

interface RoomGroupOption {
  id: number
  name: string
}

defineProps<{
  modelValue: number[]
  roomGroups: RoomGroupOption[]
  loading?: boolean
}>()

const emit = defineEmits<{
  (event: 'update:modelValue', value: number[]): void
}>()

const { t } = useI18n()

const handleModelValueUpdate = (value: unknown) => {
  if (!Array.isArray(value)) {
    emit('update:modelValue', [])
    return
  }

  emit(
    'update:modelValue',
    value
      .map((item) => Number(item))
      .filter((item) => Number.isFinite(item)),
  )
}
</script>

<style scoped>
.room-group-statistic-scope-filter {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.scope-label {
  flex-shrink: 0;
  font-size: 14px;
  color: #606266;
}

.room-group-select {
  width: 260px;
}

@media (max-width: 768px) {
  .room-group-statistic-scope-filter {
    width: 100%;
    flex-wrap: wrap;
  }

  .room-group-select {
    flex: 1 1 180px;
    width: auto;
  }
}
</style>
