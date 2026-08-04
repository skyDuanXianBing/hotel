<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  createManagedOperationProperty,
  deleteManagedOperationProperty,
  listManagedOperationProperties,
  type ManagedOperationPropertySummary,
} from '@/api/managedOperationSettlement'
import PropertyGrid from './managed-operation/PropertyGrid.vue'
import PropertyDetail from './managed-operation/PropertyDetail.vue'

const { t } = useI18n()

const properties = ref<ManagedOperationPropertySummary[]>([])
const loadingList = ref(false)
const selectedId = ref<number | null>(null)

const createDialogVisible = ref(false)
const createName = ref('')
const creating = ref(false)

const loadProperties = async () => {
  loadingList.value = true
  try {
    properties.value = await listManagedOperationProperties()
  } catch (error) {
    console.error('Failed to load managed operation properties:', error)
    ElMessage.error(t('managedOperation.grid.loadFailed'))
  } finally {
    loadingList.value = false
  }
}

const openCreateDialog = () => {
  createName.value = ''
  createDialogVisible.value = true
}

const handleCreate = async () => {
  const name = createName.value.trim()
  if (!name || creating.value) return
  creating.value = true
  try {
    const response = await createManagedOperationProperty(name)
    createDialogVisible.value = false
    if (response.settings.id != null) {
      selectedId.value = response.settings.id
    }
  } catch (error) {
    console.error('Failed to create managed operation property:', error)
    ElMessage.error(t('managedOperation.grid.createFailed'))
  } finally {
    creating.value = false
  }
}

const handleDelete = async (property: ManagedOperationPropertySummary) => {
  try {
    await ElMessageBox.confirm(
      t('managedOperation.grid.deleteConfirm', { name: property.propertyName }),
      t('managedOperation.grid.deleteTitle'),
      {
        confirmButtonText: t('common.delete'),
        cancelButtonText: t('common.cancel'),
        type: 'warning',
      },
    )
  } catch {
    return
  }
  try {
    await deleteManagedOperationProperty(property.id)
    ElMessage.success(t('managedOperation.grid.deleteSuccess'))
    await loadProperties()
  } catch (error) {
    console.error('Failed to delete managed operation property:', error)
    ElMessage.error(t('managedOperation.grid.deleteFailed'))
  }
}

const handleBack = () => {
  selectedId.value = null
  loadProperties()
}

onMounted(loadProperties)
</script>

<template>
  <div class="managed-operation-page">
    <header class="page-header">
      <div>
        <div class="eyebrow">{{ t('managedOperation.eyebrow') }}</div>
        <h1>{{ t('managedOperation.title') }}</h1>
        <p>{{ t('managedOperation.description') }}</p>
      </div>
    </header>

    <PropertyGrid
      v-if="selectedId == null"
      :properties="properties"
      :loading="loadingList"
      @select="(id) => (selectedId = id)"
      @create="openCreateDialog"
      @delete="handleDelete"
    />
    <PropertyDetail v-else :settings-id="selectedId" @back="handleBack" />

    <el-dialog
      v-model="createDialogVisible"
      :title="t('managedOperation.create.title')"
      width="440px"
    >
      <p class="create-hint">{{ t('managedOperation.create.hint') }}</p>
      <el-input
        v-model="createName"
        maxlength="100"
        show-word-limit
        :placeholder="t('managedOperation.create.placeholder')"
        @keyup.enter="handleCreate"
      />
      <template #footer>
        <el-button @click="createDialogVisible = false">{{ t('common.cancel') }}</el-button>
        <el-button
          type="primary"
          :loading="creating"
          :disabled="!createName.trim()"
          @click="handleCreate"
        >
          {{ t('managedOperation.create.confirm') }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.managed-operation-page {
  min-height: 100%;
  padding: 4px 24px 40px;
  color: #1f2937;
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
  padding: 20px 0 24px;
}

.page-header h1 {
  margin: 0;
  color: #111827;
  font-size: 25px;
  line-height: 1.3;
}

.page-header p {
  margin: 6px 0 0;
  color: #6b7280;
  font-size: 13px;
  line-height: 1.6;
}

.eyebrow {
  margin-bottom: 5px;
  color: #2563eb;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.14em;
  text-transform: uppercase;
}

.create-hint {
  margin: 0 0 12px;
  color: #6b7280;
  font-size: 13px;
  line-height: 1.6;
}

@media (max-width: 760px) {
  .managed-operation-page {
    padding: 4px 12px 28px;
  }

  .page-header {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
