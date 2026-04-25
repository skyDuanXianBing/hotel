<template>
  <SettingsCrudPage
    :back-href="ROUTE_PATHS.settingsCleaningSettings"
    title="易耗品"
    hero-eyebrow="保洁设置"
    hero-title="易耗品"
    toolbar-action-label="新增"
    section-title="易耗品列表"
    :loading="loading"
    :modal-open="editorOpen"
    :modal-title="editingSupplyId ? '编辑易耗品' : '新增易耗品'"
    @toolbar-action="handleCreateSupply"
    @dismiss-editor="handleDismissEditor"
  >
    <div v-if="supplies.length > 0" class="mobile-list settings-minimal-list">
      <article v-for="supply in supplies" :key="supply.id" class="settings-minimal-card">
        <div class="settings-minimal-card__header">
          <div class="settings-minimal-card__title-group">
            <strong>{{ supply.roomType }}</strong>
            <p class="settings-minimal-card__summary settings-minimal-card__summary--clamp-two">
              {{ supply.supplies || '当前为空' }}
            </p>
          </div>
          <span
            class="settings-minimal-card__badge"
            :class="supply.supplies ? 'settings-minimal-card__badge--success' : 'settings-minimal-card__badge--warning'"
          >
            {{ supply.supplies ? '已配置' : '未配置' }}
          </span>
        </div>

        <div class="settings-minimal-card__actions">
          <ion-button size="small" fill="outline" @click="handleEditSupply(supply)">编辑</ion-button>
          <ion-button size="small" fill="outline" @click="handleClearSupply(supply)">清空</ion-button>
          <ion-button size="small" color="danger" fill="clear" @click="handleDeleteSupply(supply)">删除</ion-button>
        </div>
      </article>
    </div>

    <p v-else-if="!loading" class="mobile-note">当前暂无易耗品配置。</p>

    <template #modalContent>
      <div class="settings-form-grid">
        <label class="settings-form-field">
          <span>房型</span>
          <ion-input v-model="supplyForm.roomType" fill="outline" placeholder="请输入房型名称" />
        </label>

        <label class="settings-form-field settings-form-field--full">
          <span>易耗品内容</span>
          <ion-textarea v-model="supplyForm.supplies" :rows="5" fill="outline" placeholder="请输入易耗品内容" />
        </label>
      </div>
    </template>

    <template #modalActions>
      <ion-button fill="outline" @click="handleDismissEditor">取消</ion-button>
      <ion-button :disabled="submitting" @click="handleSaveSupply">
        {{ submitting ? '提交中...' : '保存易耗品' }}
      </ion-button>
    </template>
  </SettingsCrudPage>
</template>

<script setup lang="ts">
import {
  alertController,
  IonButton,
  IonInput,
  IonTextarea,
  onIonViewWillEnter,
} from '@ionic/vue'
import { ref } from 'vue'
import {
  clearCleaningSupply,
  createCleaningSupply,
  deleteCleaningSupply,
  getAllCleaningSupplies,
  updateCleaningSupply,
} from '@/api/cleaning'
import SettingsCrudPage from '@/components/settings/families/SettingsCrudPage.vue'
import { ROUTE_PATHS } from '@/router/guards'
import type { CleaningSupplyDTO } from '@/types/settings'
import { showSuccessToast, showWarningToast } from '@/utils/notify'
import { isHandledRequestError } from '@/utils/request'

const loading = ref(false)
const submitting = ref(false)
const editorOpen = ref(false)
const editingSupplyId = ref<number | null>(null)
const supplies = ref<CleaningSupplyDTO[]>([])
const supplyForm = ref({ roomType: '', supplies: '' })

function resolveWarningMessage(error: unknown, fallbackMessage: string) {
  if (error instanceof Error && error.message) {
    return error.message
  }
  return fallbackMessage
}

async function confirmDelete(name: string, actionText: string) {
  const alert = await alertController.create({
    header: `${actionText}易耗品`,
    message: `确认${actionText} ${name} 吗？`,
    buttons: [
      { text: '取消', role: 'cancel' },
      { text: '确认', role: 'destructive' },
    ],
  })
  await alert.present()
  const result = await alert.onDidDismiss()
  return result.role === 'destructive'
}

async function loadPageData() {
  loading.value = true
  try {
    const response = await getAllCleaningSupplies()
    if (!response.success || !response.data) {
      throw new Error(response.message || '加载易耗品失败')
    }
    supplies.value = response.data
  } catch (error) {
    if (!isHandledRequestError(error)) {
      showWarningToast(resolveWarningMessage(error, '加载易耗品失败'))
    }
  } finally {
    loading.value = false
  }
}

function handleCreateSupply() {
  editingSupplyId.value = null
  supplyForm.value = { roomType: '', supplies: '' }
  editorOpen.value = true
}

function handleEditSupply(supply: CleaningSupplyDTO) {
  editingSupplyId.value = supply.id
  supplyForm.value = { roomType: supply.roomType, supplies: supply.supplies }
  editorOpen.value = true
}

function handleDismissEditor() {
  editorOpen.value = false
  editingSupplyId.value = null
  supplyForm.value = { roomType: '', supplies: '' }
}

async function handleSaveSupply() {
  if (!supplyForm.value.roomType.trim()) {
    showWarningToast('请输入房型名称')
    return
  }

  submitting.value = true
  try {
    if (editingSupplyId.value) {
      const response = await updateCleaningSupply(editingSupplyId.value, {
        roomType: supplyForm.value.roomType.trim(),
        supplies: supplyForm.value.supplies.trim(),
      })
      if (!response.success) {
        throw new Error(response.message || '更新易耗品失败')
      }
    } else {
      const response = await createCleaningSupply({
        roomType: supplyForm.value.roomType.trim(),
        supplies: supplyForm.value.supplies.trim(),
      })
      if (!response.success) {
        throw new Error(response.message || '创建易耗品失败')
      }
    }

    showSuccessToast('易耗品已保存')
    handleDismissEditor()
    await loadPageData()
  } catch (error) {
    if (!isHandledRequestError(error)) {
      showWarningToast(resolveWarningMessage(error, '保存易耗品失败'))
    }
  } finally {
    submitting.value = false
  }
}

async function handleClearSupply(supply: CleaningSupplyDTO) {
  const confirmed = await confirmDelete(supply.roomType, '清空')
  if (!confirmed) {
    return
  }
  try {
    const response = await clearCleaningSupply(supply.id)
    if (!response.success) {
      throw new Error(response.message || '清空易耗品失败')
    }
    showSuccessToast('易耗品已清空')
    await loadPageData()
  } catch (error) {
    if (!isHandledRequestError(error)) {
      showWarningToast(resolveWarningMessage(error, '清空易耗品失败'))
    }
  }
}

async function handleDeleteSupply(supply: CleaningSupplyDTO) {
  const confirmed = await confirmDelete(supply.roomType, '删除')
  if (!confirmed) {
    return
  }
  try {
    const response = await deleteCleaningSupply(supply.id)
    if (!response.success) {
      throw new Error(response.message || '删除易耗品失败')
    }
    showSuccessToast('易耗品已删除')
    await loadPageData()
  } catch (error) {
    if (!isHandledRequestError(error)) {
      showWarningToast(resolveWarningMessage(error, '删除易耗品失败'))
    }
  }
}

onIonViewWillEnter(async () => {
  await loadPageData()
})
</script>
