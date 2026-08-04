<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import { Delete, OfficeBuilding, Plus } from '@element-plus/icons-vue'
import type { ManagedOperationPropertySummary } from '@/api/managedOperationSettlement'

defineProps<{
  properties: ManagedOperationPropertySummary[]
  loading: boolean
}>()

const emit = defineEmits<{
  (e: 'select', id: number): void
  (e: 'create'): void
  (e: 'delete', property: ManagedOperationPropertySummary): void
}>()

const { t, locale } = useI18n()

const formatUpdatedAt = (value: string) => {
  if (!value) return '—'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  return date.toLocaleString(locale.value, {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  })
}
</script>

<template>
  <div v-loading="loading" class="property-grid">
    <button
      v-for="property in properties"
      :key="property.id"
      class="property-card"
      type="button"
      @click="emit('select', property.id)"
    >
      <div class="property-card__top">
        <span class="property-card__icon">
          <el-icon><OfficeBuilding /></el-icon>
        </span>
        <el-button
          class="property-card__delete"
          text
          type="danger"
          :icon="Delete"
          :aria-label="t('common.delete')"
          @click.stop="emit('delete', property)"
        />
      </div>
      <div class="property-card__name" :title="property.propertyName">
        {{ property.propertyName }}
      </div>
      <div class="property-card__meta">
        <span>{{ t('managedOperation.grid.roomCount', { count: property.roomCount }) }}</span>
        <el-tag v-if="property.hasStamp" size="small" type="success" effect="plain">
          {{ t('managedOperation.grid.stampReady') }}
        </el-tag>
      </div>
      <div class="property-card__updated">
        {{ t('managedOperation.grid.updatedAt', { time: formatUpdatedAt(property.updatedAt) }) }}
      </div>
    </button>

    <button class="property-card property-card--new" type="button" @click="emit('create')">
      <el-icon class="property-card__plus"><Plus /></el-icon>
      <span>{{ t('managedOperation.grid.newProperty') }}</span>
    </button>
  </div>
</template>

<style scoped>
.property-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 16px;
}

.property-card {
  display: flex;
  flex-direction: column;
  gap: 10px;
  min-height: 150px;
  padding: 16px 18px;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  background: #fff;
  text-align: left;
  cursor: pointer;
  transition:
    border-color 0.15s ease,
    box-shadow 0.15s ease;
}

.property-card:hover {
  border-color: #2563eb;
  box-shadow: 0 6px 18px rgba(37, 99, 235, 0.1);
}

.property-card__top {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.property-card__icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border-radius: 10px;
  background: #eff6ff;
  color: #2563eb;
  font-size: 20px;
}

.property-card__delete {
  opacity: 0.55;
}

.property-card:hover .property-card__delete,
.property-card__delete:focus-visible {
  opacity: 1;
}

.property-card__name {
  overflow: hidden;
  color: #111827;
  font-size: 16px;
  font-weight: 700;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.property-card__meta {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #6b7280;
  font-size: 12px;
}

.property-card__updated {
  color: #9ca3af;
  font-size: 12px;
}

.property-card--new {
  align-items: center;
  justify-content: center;
  gap: 8px;
  border-style: dashed;
  color: #2563eb;
  font-size: 14px;
  font-weight: 600;
}

.property-card--new:hover {
  background: #eff6ff;
}

.property-card__plus {
  font-size: 24px;
}
</style>
