<template>
  <ion-page>
    <ion-header translucent>
      <ion-toolbar>
        <ion-buttons slot="start">
          <ion-back-button :default-href="ROUTE_PATHS.settingsCleaningSettings" />
        </ion-buttons>
        <ion-title>易耗品</ion-title>
        <ion-buttons slot="end">
          <ion-button @click="handleCreateSupply">新增</ion-button>
        </ion-buttons>
      </ion-toolbar>
    </ion-header>

    <ion-content fullscreen class="mobile-page settings-page-block">
      <section class="mobile-hero settings-page-block__hero">
        <p class="mobile-note settings-page-block__eyebrow">保洁设置</p>
        <h1 class="mobile-title">易耗品</h1>
        <p class="mobile-subtitle">按房型维护保洁补给项，支持清空内容与删除。</p>
      </section>

      <div class="mobile-stack">
        <section class="mobile-card">
          <div class="mobile-inline-row settings-page-block__section-header">
            <div>
              <h2 class="mobile-section-title">易耗品列表</h2>
              <p class="mobile-note">建议将一类房型常用补给品维护为一条记录。</p>
            </div>
            <ion-spinner v-if="loading" name="crescent" />
          </div>

          <div v-if="supplies.length > 0" class="mobile-list settings-card-list">
            <article v-for="supply in supplies" :key="supply.id" class="settings-card-item">
              <div>
                <strong>{{ supply.roomType }}</strong>
                <p>{{ supply.supplies || '当前为空' }}</p>
              </div>
              <div class="settings-card-item__actions">
                <ion-button size="small" fill="outline" @click="handleEditSupply(supply)">编辑</ion-button>
                <ion-button size="small" fill="outline" @click="handleClearSupply(supply)">清空</ion-button>
                <ion-button size="small" color="danger" fill="clear" @click="handleDeleteSupply(supply)">删除</ion-button>
              </div>
            </article>
          </div>

          <p v-else-if="!loading" class="mobile-note">当前暂无易耗品配置。</p>
        </section>
      </div>

      <ion-modal :is-open="editorOpen" @didDismiss="handleDismissEditor">
        <ion-header>
          <ion-toolbar>
            <ion-title>{{ editingSupplyId ? '编辑易耗品' : '新增易耗品' }}</ion-title>
            <ion-buttons slot="end">
              <ion-button @click="handleDismissEditor">关闭</ion-button>
            </ion-buttons>
          </ion-toolbar>
        </ion-header>

        <ion-content class="mobile-page settings-modal-page">
          <section class="mobile-card">
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

            <div class="settings-form-actions">
              <ion-button fill="outline" @click="handleDismissEditor">取消</ion-button>
              <ion-button :disabled="submitting" @click="handleSaveSupply">
                {{ submitting ? '提交中...' : '保存易耗品' }}
              </ion-button>
            </div>
          </section>
        </ion-content>
      </ion-modal>
    </ion-content>
  </ion-page>
</template>

<script setup lang="ts">
import {
  alertController,
  IonBackButton,
  IonButton,
  IonButtons,
  IonContent,
  IonHeader,
  IonInput,
  IonModal,
  IonPage,
  IonSpinner,
  IonTextarea,
  IonTitle,
  IonToolbar,
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

<style scoped>
.settings-page-block {
  display: block;
}

.settings-page-block__hero {
  margin-top: 4px;
}

.settings-page-block__eyebrow {
  color: var(--ion-color-primary);
  font-weight: 700;
}

.settings-page-block__section-header {
  align-items: flex-start;
}

.settings-card-list {
  margin-top: 16px;
}

.settings-card-item {
  padding: 14px;
  border-radius: 18px;
  border: 1px solid var(--app-border);
  background: rgba(255, 255, 255, 0.82);
}

.settings-card-item strong,
.settings-card-item p {
  margin: 0;
}

.settings-card-item p {
  margin-top: 6px;
  color: var(--app-muted);
  font-size: 13px;
}

.settings-card-item__actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-top: 12px;
}

.settings-modal-page {
  --padding-top: 16px;
  --padding-bottom: 24px;
  --padding-start: 16px;
  --padding-end: 16px;
}

.settings-form-grid {
  display: grid;
  gap: 14px;
}

.settings-form-field {
  display: grid;
  gap: 8px;
}

.settings-form-field span {
  color: var(--app-heading);
  font-size: 13px;
  font-weight: 600;
}

.settings-form-field--full {
  grid-column: 1 / -1;
}

.settings-form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  flex-wrap: wrap;
  margin-top: 18px;
}
</style>
