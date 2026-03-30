<template>
  <ion-page>
    <ion-header translucent>
      <ion-toolbar>
        <ion-buttons slot="start">
          <ion-back-button :default-href="ROUTE_PATHS.settings" />
        </ion-buttons>
        <ion-title>门店资料</ion-title>
        <ion-buttons slot="end">
          <ion-button @click="handleOpenStoreDetails">门店详情</ion-button>
        </ion-buttons>
      </ion-toolbar>
    </ion-header>

    <ion-content fullscreen class="mobile-page settings-detail-page">
      <ion-refresher slot="fixed" @ionRefresh="handleRefresh">
        <ion-refresher-content pulling-text="下拉刷新门店资料" refreshing-spinner="crescent" />
      </ion-refresher>

      <section class="mobile-hero settings-detail-hero">
        <p class="mobile-note settings-detail-hero__eyebrow">当前门店</p>
        <h1 class="mobile-title">{{ pageTitle }}</h1>
        <p class="mobile-subtitle">首版先覆盖门店基础资料、联系方式与描述保存。</p>
        <div class="mobile-chip-row">
          <span class="mobile-chip">{{ locationLabel }}</span>
          <span class="mobile-chip">{{ currencyLabel }}</span>
        </div>
      </section>

      <div class="mobile-stack">
        <section class="mobile-card">
          <div class="mobile-inline-row settings-detail-page__section-header">
            <div>
              <h2 class="mobile-section-title">基础资料</h2>
              <p class="mobile-note">可在这里查看并更新当前门店资料。</p>
            </div>
            <ion-spinner v-if="loading" name="crescent" />
          </div>

          <div class="settings-form-grid">
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
              <ion-input v-model="form.type" fill="outline" placeholder="例如 Hotel / Apartment" />
            </label>

            <label class="settings-form-field">
              <span>时区</span>
              <ion-input v-model="form.timezone" fill="outline" placeholder="例如 Asia/Shanghai" />
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
              <ion-input v-model="form.currency" fill="outline" placeholder="例如 CNY / USD" />
            </label>

            <label class="settings-form-field settings-form-field--full">
              <span>简介</span>
              <ion-textarea v-model="form.description" :rows="5" fill="outline" placeholder="补充门店介绍、交通或其他说明" />
            </label>
          </div>

          <div class="settings-form-actions">
            <ion-button fill="outline" :disabled="loading || saving" @click="loadStoreDetail">重置</ion-button>
            <ion-button :disabled="loading || saving" @click="handleSave">
              {{ saving ? '保存中...' : '保存资料' }}
            </ion-button>
          </div>
        </section>

        <section class="mobile-card">
          <h2 class="mobile-section-title">首版说明</h2>
          <ul class="mobile-bullet-list">
            <li>已覆盖门店基础资料、联系方式与描述保存。</li>
            <li>设施、照片、入住退房政策等更深配置将在后续任务拆为独立子页。</li>
          </ul>
        </section>
      </div>
    </ion-content>
  </ion-page>
</template>

<script setup lang="ts">
import {
  IonBackButton,
  IonButton,
  IonButtons,
  IonContent,
  IonHeader,
  IonInput,
  IonPage,
  IonRefresher,
  IonRefresherContent,
  IonSpinner,
  IonTextarea,
  IonTitle,
  IonToolbar,
  onIonViewWillEnter,
} from '@ionic/vue'
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getStoreById, updateStore } from '@/api/store'
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

<style scoped>
.settings-detail-page {
  display: block;
}

.settings-detail-hero {
  margin-top: 4px;
}

.settings-detail-hero__eyebrow {
  color: var(--ion-color-primary);
  font-weight: 700;
}

.settings-detail-page__section-header {
  align-items: flex-start;
}

.settings-form-grid {
  display: grid;
  gap: 14px;
  margin-top: 16px;
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
  margin-top: 18px;
  flex-wrap: wrap;
}
</style>
