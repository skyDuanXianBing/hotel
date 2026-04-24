<template>
  <SettingsCrudPage
    :back-href="ROUTE_PATHS.settings"
    title="收款方式"
    hero-eyebrow="财务设置"
    hero-title="收款方式"
    toolbar-action-label="新增"
    :show-refresher="true"
    refresher-pulling-text="下拉刷新收款方式"
    section-title="方式列表"
    :loading="loading"
    :modal-open="editorOpen"
    :modal-title="editingMethodId ? '编辑收款方式' : '新增收款方式'"
    @toolbar-action="handleCreateMethod"
    @refresh="handleRefresh"
    @dismiss-editor="handleDismissEditor"
  >
    <div v-if="methods.length > 0" class="mobile-list settings-minimal-list">
      <article v-for="(method, index) in methods" :key="method.id" class="settings-minimal-card">
        <div class="settings-minimal-card__header">
          <div class="settings-minimal-card__title-group">
            <strong>{{ method.name }}</strong>
            <p class="settings-minimal-card__summary">
              顺序 {{ method.displayOrder }} · {{ method.enabled ? '已启用' : '已停用' }}
            </p>
          </div>
          <span
            class="settings-minimal-card__badge"
            :class="method.enabled ? 'settings-minimal-card__badge--success' : 'settings-minimal-card__badge--warning'"
          >
            {{ method.enabled ? '启用中' : '已停用' }}
          </span>
        </div>

        <div class="settings-minimal-card__actions settings-minimal-card__actions--dense">
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

    <template #sectionFooter>
      <div class="settings-form-actions settings-form-actions--section">
        <ion-button fill="outline" :disabled="loading || savingOrder" @click="loadPageData">重置</ion-button>
        <ion-button :disabled="loading || savingOrder || methods.length === 0" @click="handleSaveOrder">
          {{ savingOrder ? '保存中...' : '保存顺序' }}
        </ion-button>
      </div>
    </template>

    <template #modalContent>
      <div class="settings-form-grid">
        <label class="settings-form-field">
          <span>名称</span>
          <ion-input v-model="methodForm.name" fill="outline" placeholder="请输入收款方式名称" />
        </label>

        <div class="settings-toggle-field">
          <div>
            <strong>启用状态</strong>
          </div>
          <ion-toggle v-model="methodForm.enabled" />
        </div>
      </div>
    </template>

    <template #modalActions>
      <ion-button fill="outline" @click="handleDismissEditor">取消</ion-button>
      <ion-button :disabled="submitting" @click="handleSaveMethod">
        {{ submitting ? '提交中...' : '保存方式' }}
      </ion-button>
    </template>
  </SettingsCrudPage>
</template>

<script setup lang="ts">
import {
  alertController,
  IonButton,
  IonInput,
  IonToggle,
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
import SettingsCrudPage from '@/components/settings/families/SettingsCrudPage.vue'
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
