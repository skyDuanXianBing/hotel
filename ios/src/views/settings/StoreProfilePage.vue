<template>
  <SettingsDetailFormShell
    :back-href="ROUTE_PATHS.settings"
    title="门店资料"
    hero-eyebrow="当前门店"
    :hero-title="pageTitle"
    toolbar-action-label="门店详情"
    :show-refresher="true"
    refresher-pulling-text="下拉刷新门店资料"
    @refresh="handleRefresh"
    @toolbar-action="handleOpenStoreDetails"
  >
    <template #heroExtra>
      <div class="settings-detail-hero__summary">
        <div class="settings-detail-hero__meta">
          <div class="settings-detail-hero__meta-item">
            <span class="settings-detail-hero__meta-label">地区</span>
            <strong>{{ locationLabel }}</strong>
          </div>
          <div class="settings-detail-hero__meta-item">
            <span class="settings-detail-hero__meta-label">币种</span>
            <strong>{{ currencyLabel }}</strong>
          </div>
          <div class="settings-detail-hero__meta-item">
            <span class="settings-detail-hero__meta-label">门店类型</span>
            <strong>{{ storeTypeLabel }}</strong>
          </div>
          <div class="settings-detail-hero__meta-item">
            <span class="settings-detail-hero__meta-label">时区</span>
            <strong>{{ timezoneLabel }}</strong>
          </div>
        </div>
      </div>
    </template>

    <SettingsSectionCard
      title="基础资料"
      :loading="loading"
      header-class="settings-detail-page__section-header"
    >
      <div class="settings-form-grid settings-form-grid--top">
        <label class="settings-form-field">
          <span>门店名称</span>
          <ion-input v-model="form.name" fill="outline" placeholder="请输入门店名称" />
        </label>

        <label class="settings-form-field">
          <span>联系电话</span>
          <ion-input v-model="form.phone" fill="outline" placeholder="请输入联系电话" />
        </label>

        <label class="settings-form-field">
          <span>门店类型</span>
          <ion-input v-model="form.type" fill="outline" placeholder="请输入门店类型" />
        </label>

        <label class="settings-form-field">
          <span>时区</span>
          <ion-input v-model="form.timezone" fill="outline" placeholder="请输入时区" />
        </label>

        <label class="settings-form-field">
          <span>负责人</span>
          <ion-input v-model="form.manager" fill="outline" placeholder="请输入负责人姓名" />
        </label>

        <label class="settings-form-field">
          <span>负责人邮箱</span>
          <ion-input v-model="form.ownerEmail" fill="outline" placeholder="请输入负责人邮箱" />
        </label>

        <label class="settings-form-field">
          <span>国家</span>
          <ion-input v-model="form.country" fill="outline" placeholder="请输入国家" />
        </label>

        <label class="settings-form-field">
          <span>城市</span>
          <ion-input v-model="form.city" fill="outline" placeholder="请输入城市" />
        </label>

        <label class="settings-form-field">
          <span>地址</span>
          <ion-input v-model="form.address" fill="outline" placeholder="请输入地址" />
        </label>

        <label class="settings-form-field">
          <span>币种</span>
          <ion-input v-model="form.currency" fill="outline" placeholder="请输入币种" />
        </label>

        <label class="settings-form-field settings-form-field--full">
          <span>简介</span>
          <ion-textarea v-model="form.description" :rows="5" fill="outline" placeholder="请输入门店简介" />
        </label>
      </div>

    </SettingsSectionCard>

    <template #bottomActions>
      <div class="settings-page-actions">
        <ion-button fill="outline" :disabled="loading || saving" @click="loadStoreDetail">重置</ion-button>
        <ion-button :disabled="loading || saving" @click="handleSave">
          {{ saving ? '保存中...' : '保存资料' }}
        </ion-button>
      </div>
    </template>
  </SettingsDetailFormShell>
</template>

<script setup lang="ts">
import { IonButton, IonInput, IonTextarea, onIonViewWillEnter } from '@ionic/vue'
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getStoreById, updateStore } from '@/api/store'
import SettingsSectionCard from '@/components/settings/base/SettingsSectionCard.vue'
import SettingsDetailFormShell from '@/components/settings/families/SettingsDetailFormShell.vue'
import { ROUTE_PATHS } from '@/router/guards'
import { useStoreStore } from '@/stores/store'
import type { StoreDTO, StoreRequest } from '@/types/store'
import { showSuccessToast, showWarningToast } from '@/utils/notify'
import { isHandledRequestError } from '@/utils/request'

interface StoreFormState {
  name: string
  phone: string
  type: string
  timezone: string
  manager: string
  ownerEmail: string
  country: string
  city: string
  address: string
  currency: string
  description: string
}

const storeStore = useStoreStore()
const router = useRouter()

const loading = ref(false)
const saving = ref(false)
const form = ref<StoreFormState>(createEmptyForm())

const pageTitle = computed(() => {
  return form.value.name || storeStore.currentStore?.name || '门店资料'
})

const locationLabel = computed(() => {
  const parts = [form.value.city, form.value.country].filter(Boolean)
  if (parts.length === 0) {
    return '未设置地区'
  }
  return parts.join(' · ')
})

const currencyLabel = computed(() => {
  return form.value.currency || '未设置币种'
})

const storeTypeLabel = computed(() => {
  return form.value.type || '未设置类型'
})

const timezoneLabel = computed(() => {
  return form.value.timezone || '未设置时区'
})

function createEmptyForm(): StoreFormState {
  return {
    name: '',
    phone: '',
    type: '',
    timezone: '',
    manager: '',
    ownerEmail: '',
    country: '',
    city: '',
    address: '',
    currency: '',
    description: '',
  }
}

function fillForm(store: StoreDTO) {
  form.value = {
    name: store.name || '',
    phone: store.phone || '',
    type: store.type || '',
    timezone: store.timezone || '',
    manager: store.manager || '',
    ownerEmail: store.ownerEmail || '',
    country: store.country || '',
    city: store.city || '',
    address: store.address || '',
    currency: store.currency || '',
    description: store.description || '',
  }
}

function buildRequestData(): StoreRequest {
  return {
    name: form.value.name.trim(),
    phone: form.value.phone.trim(),
    type: form.value.type.trim(),
    timezone: form.value.timezone.trim(),
    manager: form.value.manager.trim(),
    ownerEmail: form.value.ownerEmail.trim(),
    country: form.value.country.trim(),
    city: form.value.city.trim(),
    address: form.value.address.trim(),
    currency: form.value.currency.trim(),
    description: form.value.description.trim(),
  }
}

function resolveWarningMessage(error: unknown, fallbackMessage: string) {
  if (error instanceof Error && error.message) {
    return error.message
  }
  return fallbackMessage
}

async function loadStoreDetail() {
  const storeId = storeStore.currentStore?.id
  if (!storeId) {
    showWarningToast('请先选择门店')
    return
  }

  loading.value = true
  try {
    const response = await getStoreById(storeId)
    if (!response.success || !response.data) {
      throw new Error(response.message || '加载门店资料失败')
    }

    fillForm(response.data)
    storeStore.setCurrentStore(response.data)
  } catch (error) {
    if (!isHandledRequestError(error)) {
      showWarningToast(resolveWarningMessage(error, '加载门店资料失败'))
    }
  } finally {
    loading.value = false
  }
}

async function handleSave() {
  const storeId = storeStore.currentStore?.id
  if (!storeId) {
    showWarningToast('请先选择门店')
    return
  }

  if (!form.value.name.trim()) {
    showWarningToast('请输入门店名称')
    return
  }

  if (!form.value.country.trim()) {
    showWarningToast('请输入国家')
    return
  }

  saving.value = true
  try {
    const response = await updateStore(storeId, buildRequestData())
    if (!response.success || !response.data) {
      throw new Error(response.message || '保存门店资料失败')
    }

    fillForm(response.data)
    storeStore.setCurrentStore(response.data)
    showSuccessToast('门店资料已保存')
  } catch (error) {
    if (!isHandledRequestError(error)) {
      showWarningToast(resolveWarningMessage(error, '保存门店资料失败'))
    }
  } finally {
    saving.value = false
  }
}

async function handleRefresh(event: CustomEvent) {
  await loadStoreDetail()
  event.detail.complete()
}

async function handleOpenStoreDetails() {
  await router.push(ROUTE_PATHS.settingsStoreDetails)
}

onIonViewWillEnter(async () => {
  await loadStoreDetail()
})
</script>
