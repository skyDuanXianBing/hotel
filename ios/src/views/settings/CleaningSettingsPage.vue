<template>
  <SettingsPageShell
    :back-href="ROUTE_PATHS.settings"
    title="保洁设置"
    hero-eyebrow="保洁设置"
    hero-title="保洁配置"
    :chips="[
      { label: `保洁员 ${cleaners.length}` },
      { label: configForm.enabled ? '配置已启用' : '配置已停用' },
    ]"
    content-class="settings-page-block"
    hero-class="settings-page-block__hero"
    eyebrow-class="settings-page-block__eyebrow"
  >
    <SettingsSectionCard
      title="门店级配置"
      :loading="loading"
      header-class="settings-page-block__section-header"
    >
      <div class="settings-toggle-field">
        <div>
          <strong>启用保洁配置</strong>
        </div>
        <ion-toggle v-model="configForm.enabled" />
      </div>

      <div class="settings-form-grid settings-form-grid--top">
        <label class="settings-form-field">
          <span>住中开始</span>
          <ion-input v-model="configForm.stayStartTime" fill="outline" placeholder="10:00" />
        </label>
        <label class="settings-form-field">
          <span>住中结束</span>
          <ion-input v-model="configForm.stayEndTime" fill="outline" placeholder="15:00" />
        </label>
        <label class="settings-form-field">
          <span>退房开始</span>
          <ion-input v-model="configForm.checkoutStartTime" fill="outline" placeholder="11:00" />
        </label>
        <label class="settings-form-field">
          <span>退房结束</span>
          <ion-input v-model="configForm.checkoutEndTime" fill="outline" placeholder="17:00" />
        </label>
      </div>

      <div class="settings-toggle-field settings-toggle-field--top">
        <div>
          <strong>自动生成住中任务</strong>
        </div>
        <ion-toggle v-model="configForm.autoStayTask" />
      </div>

      <div class="settings-toggle-field settings-toggle-field--top">
        <div>
          <strong>自动生成退房任务</strong>
        </div>
        <ion-toggle v-model="configForm.autoCheckoutTask" />
      </div>

      <div class="settings-form-actions settings-form-actions--section">
        <ion-button fill="outline" :disabled="loading || savingConfig" @click="loadPageData">重置</ion-button>
        <ion-button :disabled="loading || savingConfig" @click="handleSaveConfig">
          {{ savingConfig ? '保存中...' : '保存保洁配置' }}
        </ion-button>
        <ion-button fill="outline" @click="handleOpenSupplies">易耗品</ion-button>
      </div>
    </SettingsSectionCard>

    <SettingsSectionCard
      title="保洁员"
      header-class="settings-page-block__section-header"
    >
      <template #headerActions>
        <ion-button size="small" @click="handleCreateCleaner">新增保洁员</ion-button>
      </template>

      <div v-if="cleaners.length > 0" class="mobile-list settings-card-list">
        <article v-for="cleaner in cleaners" :key="cleaner.id" class="settings-card-item">
          <div>
            <strong>{{ cleaner.name }}</strong>
            <p>{{ cleaner.email }}</p>
          </div>
          <div class="settings-card-item__actions">
            <ion-button size="small" fill="outline" @click="handleEditCleaner(cleaner)">编辑</ion-button>
            <ion-button size="small" color="danger" fill="clear" @click="handleDeleteCleaner(cleaner)">删除</ion-button>
          </div>
        </article>
      </div>

      <p v-else-if="!loading" class="mobile-note">当前暂无保洁员。</p>
    </SettingsSectionCard>

    <SettingsEditorModal
      :is-open="editorOpen"
      :title="editingCleanerId ? '编辑保洁员' : '新增保洁员'"
      @close="handleDismissEditor"
      @didDismiss="handleDismissEditor"
    >
      <div class="settings-form-grid">
        <label class="settings-form-field">
          <span>姓名</span>
          <ion-input v-model="cleanerForm.name" fill="outline" placeholder="请输入保洁员姓名" />
        </label>
        <label class="settings-form-field">
          <span>邮箱</span>
          <ion-input v-model="cleanerForm.email" fill="outline" placeholder="请输入邮箱地址" />
        </label>
      </div>

      <template #actions>
        <ion-button fill="outline" @click="handleDismissEditor">取消</ion-button>
        <ion-button :disabled="submittingCleaner" @click="handleSaveCleaner">
          {{ submittingCleaner ? '提交中...' : '保存保洁员' }}
        </ion-button>
      </template>
    </SettingsEditorModal>
  </SettingsPageShell>
</template>

<script setup lang="ts">
import { alertController, IonButton, IonInput, IonToggle, onIonViewWillEnter } from '@ionic/vue'
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import {
  createCleaner,
  deleteCleaner,
  getCleaners,
  getOrCreateCleaningConfig,
  updateCleaner,
  updateCleaningConfig,
} from '@/api/cleaning'
import SettingsEditorModal from '@/components/settings/base/SettingsEditorModal.vue'
import SettingsPageShell from '@/components/settings/base/SettingsPageShell.vue'
import SettingsSectionCard from '@/components/settings/base/SettingsSectionCard.vue'
import { ROUTE_PATHS } from '@/router/guards'
import { useStoreStore } from '@/stores/store'
import { useUserStore } from '@/stores/user'
import type { CleanerDTO, CleaningConfigRequest, CleanerRequest } from '@/types/settings'
import { showSuccessToast, showWarningToast } from '@/utils/notify'
import { isHandledRequestError } from '@/utils/request'

const router = useRouter()
const storeStore = useStoreStore()
const userStore = useUserStore()

const loading = ref(false)
const savingConfig = ref(false)
const submittingCleaner = ref(false)
const cleaningConfigId = ref<number | null>(null)
const cleaners = ref<CleanerDTO[]>([])
const editorOpen = ref(false)
const editingCleanerId = ref<number | null>(null)
const cleanerForm = ref<{ name: string; email: string }>({ name: '', email: '' })
const configForm = ref<CleaningConfigRequest>({
  enabled: true,
  stayStartTime: '10:00',
  stayEndTime: '15:00',
  checkoutStartTime: '11:00',
  checkoutEndTime: '17:00',
  autoStayTask: false,
  autoCheckoutTask: true,
})

function resolveWarningMessage(error: unknown, fallbackMessage: string) {
  if (error instanceof Error && error.message) {
    return error.message
  }
  return fallbackMessage
}

async function confirmDelete(name: string) {
  const alert = await alertController.create({
    header: '删除保洁员',
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
  const userId = userStore.currentUser?.id
  const storeId = storeStore.currentStore?.id
  if (!userId || !storeId) {
    showWarningToast('请先恢复用户与门店信息')
    return
  }

  loading.value = true
  try {
    const [configResponse, cleanerResponse] = await Promise.all([getOrCreateCleaningConfig(userId, storeId), getCleaners()])
    if (!configResponse.success || !configResponse.data) {
      throw new Error(configResponse.message || '加载保洁配置失败')
    }
    if (!cleanerResponse.success || !cleanerResponse.data) {
      throw new Error(cleanerResponse.message || '加载保洁员失败')
    }

    cleaningConfigId.value = configResponse.data.id
    configForm.value = {
      enabled: configResponse.data.enabled,
      stayStartTime: configResponse.data.stayStartTime,
      stayEndTime: configResponse.data.stayEndTime,
      checkoutStartTime: configResponse.data.checkoutStartTime,
      checkoutEndTime: configResponse.data.checkoutEndTime,
      autoStayTask: configResponse.data.autoStayTask,
      autoCheckoutTask: configResponse.data.autoCheckoutTask,
    }
    cleaners.value = cleanerResponse.data
  } catch (error) {
    if (!isHandledRequestError(error)) {
      showWarningToast(resolveWarningMessage(error, '加载保洁设置失败'))
    }
  } finally {
    loading.value = false
  }
}

async function handleSaveConfig() {
  if (!cleaningConfigId.value) {
    showWarningToast('未获取到保洁配置')
    return
  }

  savingConfig.value = true
  try {
    const response = await updateCleaningConfig(cleaningConfigId.value, configForm.value)
    if (!response.success || !response.data) {
      throw new Error(response.message || '保存保洁配置失败')
    }
    showSuccessToast('保洁配置已保存')
    await loadPageData()
  } catch (error) {
    if (!isHandledRequestError(error)) {
      showWarningToast(resolveWarningMessage(error, '保存保洁配置失败'))
    }
  } finally {
    savingConfig.value = false
  }
}

function handleCreateCleaner() {
  editingCleanerId.value = null
  cleanerForm.value = { name: '', email: '' }
  editorOpen.value = true
}

function handleEditCleaner(cleaner: CleanerDTO) {
  editingCleanerId.value = cleaner.id
  cleanerForm.value = { name: cleaner.name, email: cleaner.email }
  editorOpen.value = true
}

function handleDismissEditor() {
  editorOpen.value = false
  editingCleanerId.value = null
  cleanerForm.value = { name: '', email: '' }
}

async function handleSaveCleaner() {
  const userId = userStore.currentUser?.id
  const storeId = storeStore.currentStore?.id
  if (!userId || !storeId) {
    showWarningToast('请先恢复用户与门店信息')
    return
  }
  if (!cleanerForm.value.name.trim()) {
    showWarningToast('请输入保洁员姓名')
    return
  }
  if (!cleanerForm.value.email.trim()) {
    showWarningToast('请输入保洁员邮箱')
    return
  }

  submittingCleaner.value = true
  try {
    const payload: CleanerRequest = {
      userId,
      storeId,
      name: cleanerForm.value.name.trim(),
      email: cleanerForm.value.email.trim(),
    }

    if (editingCleanerId.value) {
      const response = await updateCleaner(editingCleanerId.value, payload)
      if (!response.success) {
        throw new Error(response.message || '更新保洁员失败')
      }
    } else {
      const response = await createCleaner(payload)
      if (!response.success) {
        throw new Error(response.message || '创建保洁员失败')
      }
    }

    showSuccessToast('保洁员已保存')
    handleDismissEditor()
    await loadPageData()
  } catch (error) {
    if (!isHandledRequestError(error)) {
      showWarningToast(resolveWarningMessage(error, '保存保洁员失败'))
    }
  } finally {
    submittingCleaner.value = false
  }
}

async function handleDeleteCleaner(cleaner: CleanerDTO) {
  const confirmed = await confirmDelete(cleaner.name)
  if (!confirmed) {
    return
  }

  try {
    const response = await deleteCleaner(cleaner.id)
    if (!response.success) {
      throw new Error(response.message || '删除保洁员失败')
    }
    showSuccessToast('保洁员已删除')
    await loadPageData()
  } catch (error) {
    if (!isHandledRequestError(error)) {
      showWarningToast(resolveWarningMessage(error, '删除保洁员失败'))
    }
  }
}

async function handleOpenSupplies() {
  await router.push(ROUTE_PATHS.settingsCleaningSupplies)
}

onIonViewWillEnter(async () => {
  await loadPageData()
})
</script>
