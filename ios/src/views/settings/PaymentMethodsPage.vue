<template>
  <ion-page>
    <ion-header translucent>
      <ion-toolbar>
        <ion-buttons slot="start">
          <ion-back-button :default-href="ROUTE_PATHS.settings" />
        </ion-buttons>
        <ion-title>收款方式</ion-title>
        <ion-buttons slot="end">
          <ion-button @click="handleCreateMethod">新增</ion-button>
        </ion-buttons>
      </ion-toolbar>
    </ion-header>

    <ion-content fullscreen class="mobile-page settings-page-block">
      <ion-refresher slot="fixed" @ionRefresh="handleRefresh">
        <ion-refresher-content pulling-text="下拉刷新收款方式" refreshing-spinner="crescent" />
      </ion-refresher>

      <section class="mobile-hero settings-page-block__hero">
        <p class="mobile-note settings-page-block__eyebrow">财务设置</p>
        <h1 class="mobile-title">收款方式</h1>
        <p class="mobile-subtitle">支持新增、编辑、启停与显示顺序调整。</p>
      </section>

      <div class="mobile-stack">
        <section class="mobile-card">
          <div class="mobile-inline-row settings-page-block__section-header">
            <div>
              <h2 class="mobile-section-title">方式列表</h2>
              <p class="mobile-note">上移 / 下移后点击保存顺序即可生效。</p>
            </div>
            <ion-spinner v-if="loading" name="crescent" />
          </div>

          <div v-if="methods.length > 0" class="mobile-list settings-card-list">
            <article v-for="(method, index) in methods" :key="method.id" class="settings-card-item">
              <div>
                <strong>{{ method.name }}</strong>
                <p>顺序 {{ method.displayOrder }} · {{ method.enabled ? '已启用' : '已停用' }}</p>
              </div>

              <div class="settings-card-item__actions">
                <ion-button size="small" fill="outline" @click="handleEditMethod(method)">编辑</ion-button>
                <ion-button size="small" fill="outline" @click="handleToggleMethod(method)">
                  {{ method.enabled ? '停用' : '启用' }}
                </ion-button>
                <ion-button size="small" fill="outline" :disabled="index === 0" @click="handleMove(index, -1)">上移</ion-button>
                <ion-button size="small" fill="outline" :disabled="index === methods.length - 1" @click="handleMove(index, 1)">下移</ion-button>
                <ion-button size="small" color="danger" fill="clear" @click="handleDeleteMethod(method)">删除</ion-button>
              </div>
            </article>
          </div>

          <p v-else-if="!loading" class="mobile-note">当前暂无收款方式。</p>

          <div class="settings-form-actions settings-form-actions--section">
            <ion-button fill="outline" :disabled="loading || savingOrder" @click="loadPageData">重置</ion-button>
            <ion-button :disabled="loading || savingOrder || methods.length === 0" @click="handleSaveOrder">
              {{ savingOrder ? '保存中...' : '保存顺序' }}
            </ion-button>
          </div>
        </section>
      </div>

      <ion-modal :is-open="editorOpen" @didDismiss="handleDismissEditor">
        <ion-header>
          <ion-toolbar>
            <ion-title>{{ editingMethodId ? '编辑收款方式' : '新增收款方式' }}</ion-title>
            <ion-buttons slot="end">
              <ion-button @click="handleDismissEditor">关闭</ion-button>
            </ion-buttons>
          </ion-toolbar>
        </ion-header>

        <ion-content class="mobile-page settings-modal-page">
          <section class="mobile-card">
            <div class="settings-form-grid">
              <label class="settings-form-field">
                <span>名称</span>
                <ion-input v-model="methodForm.name" fill="outline" placeholder="请输入收款方式名称" />
              </label>

              <div class="settings-toggle-field">
                <div>
                  <strong>启用状态</strong>
                  <p>停用后不会出现在可选收款方式中。</p>
                </div>
                <ion-toggle v-model="methodForm.enabled" />
              </div>
            </div>

            <div class="settings-form-actions">
              <ion-button fill="outline" @click="handleDismissEditor">取消</ion-button>
              <ion-button :disabled="submitting" @click="handleSaveMethod">
                {{ submitting ? '提交中...' : '保存方式' }}
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
  IonRefresher,
  IonRefresherContent,
  IonSpinner,
  IonTitle,
  IonToggle,
  IonToolbar,
  onIonViewWillEnter,
} from '@ionic/vue'
import { ref } from 'vue'
import {
  createPaymentMethod,
  deletePaymentMethod,
  getAllPaymentMethods,
  updatePaymentMethod,
  updatePaymentMethodEnabled,
  updatePaymentMethodsOrder,
} from '@/api/paymentMethod'
import { ROUTE_PATHS } from '@/router/guards'
import type { PaymentMethodDTO } from '@/types/settings'
import { showSuccessToast, showWarningToast } from '@/utils/notify'
import { isHandledRequestError } from '@/utils/request'
import { moveArrayItem } from '@/utils/settings'

interface PaymentMethodFormState {
  name: string
  enabled: boolean
}

const loading = ref(false)
const submitting = ref(false)
const savingOrder = ref(false)
const editorOpen = ref(false)
const editingMethodId = ref<number | null>(null)
const methods = ref<PaymentMethodDTO[]>([])
const methodForm = ref<PaymentMethodFormState>(createEmptyForm())

function createEmptyForm(): PaymentMethodFormState {
  return {
    name: '',
    enabled: true,
  }
}

function resolveWarningMessage(error: unknown, fallbackMessage: string) {
  if (error instanceof Error && error.message) {
    return error.message
  }
  return fallbackMessage
}

async function confirmDelete(name: string) {
  const alert = await alertController.create({
    header: '删除收款方式',
    message: `确认删除 ${name} 吗？`,
    buttons: [
      { text: '取消', role: 'cancel' },
      { text: '确认删除', role: 'destructive' },
    ],
  })
  await alert.present()
  const result = await alert.onDidDismiss()
  return result.role === 'destructive'
}

async function loadPageData() {
  loading.value = true
  try {
    const response = await getAllPaymentMethods()
    if (!response.success || !response.data) {
      throw new Error(response.message || '加载收款方式失败')
    }
    methods.value = [...response.data].sort((left, right) => left.displayOrder - right.displayOrder)
  } catch (error) {
    if (!isHandledRequestError(error)) {
      showWarningToast(resolveWarningMessage(error, '加载收款方式失败'))
    }
  } finally {
    loading.value = false
  }
}

function handleCreateMethod() {
  editingMethodId.value = null
  methodForm.value = createEmptyForm()
  editorOpen.value = true
}

function handleEditMethod(method: PaymentMethodDTO) {
  editingMethodId.value = method.id
  methodForm.value = {
    name: method.name,
    enabled: method.enabled,
  }
  editorOpen.value = true
}

function handleDismissEditor() {
  editorOpen.value = false
  editingMethodId.value = null
  methodForm.value = createEmptyForm()
}

async function handleSaveMethod() {
  if (!methodForm.value.name.trim()) {
    showWarningToast('请输入收款方式名称')
    return
  }

  submitting.value = true
  try {
    if (editingMethodId.value) {
      const response = await updatePaymentMethod(editingMethodId.value, {
        name: methodForm.value.name.trim(),
        enabled: methodForm.value.enabled,
      })
      if (!response.success) {
        throw new Error(response.message || '更新收款方式失败')
      }
    } else {
      const response = await createPaymentMethod({
        name: methodForm.value.name.trim(),
        enabled: methodForm.value.enabled,
      })
      if (!response.success) {
        throw new Error(response.message || '创建收款方式失败')
      }
    }

    showSuccessToast('收款方式已保存')
    handleDismissEditor()
    await loadPageData()
  } catch (error) {
    if (!isHandledRequestError(error)) {
      showWarningToast(resolveWarningMessage(error, '保存收款方式失败'))
    }
  } finally {
    submitting.value = false
  }
}

async function handleToggleMethod(method: PaymentMethodDTO) {
  try {
    const response = await updatePaymentMethodEnabled(method.id, !method.enabled)
    if (!response.success) {
      throw new Error(response.message || '更新状态失败')
    }
    showSuccessToast(response.data.enabled ? '收款方式已启用' : '收款方式已停用')
    await loadPageData()
  } catch (error) {
    if (!isHandledRequestError(error)) {
      showWarningToast(resolveWarningMessage(error, '更新收款方式状态失败'))
    }
  }
}

function handleMove(index: number, delta: number) {
  methods.value = moveArrayItem(methods.value, index, index + delta)
}

async function handleSaveOrder() {
  savingOrder.value = true
  try {
    const payload = methods.value.map((item, index) => ({
      id: item.id,
      displayOrder: index + 1,
    }))
    const response = await updatePaymentMethodsOrder(payload)
    if (!response.success) {
      throw new Error(response.message || '保存顺序失败')
    }
    showSuccessToast('收款方式顺序已保存')
    await loadPageData()
  } catch (error) {
    if (!isHandledRequestError(error)) {
      showWarningToast(resolveWarningMessage(error, '保存顺序失败'))
    }
  } finally {
    savingOrder.value = false
  }
}

async function handleDeleteMethod(method: PaymentMethodDTO) {
  const confirmed = await confirmDelete(method.name)
  if (!confirmed) {
    return
  }

  try {
    const response = await deletePaymentMethod(method.id)
    if (!response.success) {
      throw new Error(response.message || '删除收款方式失败')
    }
    showSuccessToast('收款方式已删除')
    await loadPageData()
  } catch (error) {
    if (!isHandledRequestError(error)) {
      showWarningToast(resolveWarningMessage(error, '删除收款方式失败'))
    }
  }
}

async function handleRefresh(event: CustomEvent) {
  await loadPageData()
  event.detail.complete()
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

.settings-toggle-field {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 12px 14px;
  border-radius: 18px;
  background: var(--app-primary-soft);
}

.settings-toggle-field strong,
.settings-toggle-field p {
  margin: 0;
}

.settings-toggle-field p {
  margin-top: 6px;
  color: var(--app-muted);
  font-size: 12px;
}

.settings-form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  flex-wrap: wrap;
  margin-top: 18px;
}

.settings-form-actions--section {
  margin-top: 18px;
}
</style>
