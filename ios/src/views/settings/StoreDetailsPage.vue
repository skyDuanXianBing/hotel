<template>
  <ion-page>
    <ion-header translucent>
      <ion-toolbar>
        <ion-buttons slot="start">
          <ion-back-button :default-href="ROUTE_PATHS.settings" />
        </ion-buttons>
        <ion-title>门店详情</ion-title>
      </ion-toolbar>
    </ion-header>

    <ion-content fullscreen class="mobile-page settings-detail-page">
      <ion-refresher slot="fixed" @ionRefresh="handleRefresh">
        <ion-refresher-content pulling-text="下拉刷新门店详情" refreshing-spinner="crescent" />
      </ion-refresher>

      <section class="mobile-hero settings-detail-hero">
        <p class="mobile-note settings-detail-hero__eyebrow">门店设置</p>
        <h1 class="mobile-title">{{ pageTitle }}</h1>
        <p class="mobile-subtitle">补齐设施、照片、地图占位、入住退房时间与门店政策等关键字段。</p>
        <div class="mobile-chip-row">
          <span class="mobile-chip">设施 {{ selectedFacilityLabels.length }}</span>
          <span class="mobile-chip">照片 {{ photoCount }}</span>
          <span class="mobile-chip">{{ locationLabel }}</span>
        </div>
      </section>

      <div class="mobile-stack">
        <section class="mobile-card">
          <div class="mobile-inline-row settings-detail-page__section-header">
            <div>
              <h2 class="mobile-section-title">门店详情</h2>
              <p class="mobile-note">门店基础详情、地区语言、照片链接与地图说明统一在此维护。</p>
            </div>
            <ion-spinner v-if="loading" name="crescent" />
          </div>

          <div class="settings-form-grid">
            <label class="settings-form-field">
              <span>邮箱地址</span>
              <ion-input v-model="form.email" fill="outline" placeholder="请输入邮箱地址" />
            </label>

            <label class="settings-form-field">
              <span>语言</span>
              <ion-select v-model="form.language" fill="outline" interface="modal">
                <ion-select-option v-for="option in LANGUAGE_OPTIONS" :key="option.value" :value="option.value">
                  {{ option.label }}
                </ion-select-option>
              </ion-select>
            </label>

            <label class="settings-form-field settings-form-field--full">
              <span>详细地址</span>
              <ion-input v-model="form.address" fill="outline" placeholder="请输入详细地址" />
            </label>

            <label class="settings-form-field">
              <span>城市</span>
              <ion-input v-model="form.city" fill="outline" placeholder="请输入城市" />
            </label>

            <label class="settings-form-field">
              <span>州 / 省</span>
              <ion-input v-model="form.state" fill="outline" placeholder="请输入州 / 省" />
            </label>

            <label class="settings-form-field">
              <span>国家</span>
              <ion-select v-model="form.country" fill="outline" interface="modal">
                <ion-select-option v-for="option in COUNTRY_OPTIONS" :key="option.value" :value="option.value">
                  {{ option.label }}
                </ion-select-option>
              </ion-select>
            </label>

            <label class="settings-form-field">
              <span>时区</span>
              <ion-select v-model="form.timezone" fill="outline" interface="modal">
                <ion-select-option v-for="option in TIMEZONE_OPTIONS" :key="option.value" :value="option.value">
                  {{ option.label }}
                </ion-select-option>
              </ion-select>
            </label>

            <label class="settings-form-field">
              <span>币种</span>
              <ion-select v-model="form.currency" fill="outline" interface="modal">
                <ion-select-option v-for="option in CURRENCY_OPTIONS" :key="option.value" :value="option.value">
                  {{ option.label }}
                </ion-select-option>
              </ion-select>
            </label>

            <label class="settings-form-field">
              <span>入住时间</span>
              <ion-select v-model="policyForm.checkinTime" fill="outline" interface="modal">
                <ion-select-option v-for="option in STORE_TIME_OPTIONS" :key="option.value" :value="option.value">
                  {{ option.label }}
                </ion-select-option>
              </ion-select>
            </label>

            <label class="settings-form-field">
              <span>退房时间</span>
              <ion-select v-model="policyForm.checkoutTime" fill="outline" interface="modal">
                <ion-select-option v-for="option in STORE_TIME_OPTIONS" :key="option.value" :value="option.value">
                  {{ option.label }}
                </ion-select-option>
              </ion-select>
            </label>

            <label class="settings-form-field settings-form-field--full">
              <span>门店设施</span>
              <ion-select v-model="selectedFacilityLabels" fill="outline" interface="modal" multiple>
                <ion-select-option v-for="item in STORE_FACILITY_OPTIONS" :key="item.label" :value="item.label">
                  {{ item.label }}
                </ion-select-option>
              </ion-select>
            </label>

            <label class="settings-form-field settings-form-field--full">
              <span>门店描述</span>
              <ion-textarea v-model="form.description" :rows="4" fill="outline" placeholder="请输入门店描述" />
            </label>

            <label class="settings-form-field settings-form-field--full">
              <span>桌面照片链接</span>
              <ion-textarea
                v-model="form.desktopPhotoUrlsText"
                :rows="4"
                fill="outline"
                placeholder="每行一个照片链接"
              />
            </label>

            <label class="settings-form-field settings-form-field--full">
              <span>移动照片链接</span>
              <ion-textarea
                v-model="form.mobilePhotoUrlsText"
                :rows="4"
                fill="outline"
                placeholder="每行一个照片链接"
              />
            </label>
          </div>
        </section>

        <section class="mobile-card">
          <h2 class="mobile-section-title">门店政策</h2>
          <div class="settings-form-grid">
            <label class="settings-form-field settings-form-field--full">
              <span>儿童政策</span>
              <ion-textarea v-model="policyForm.childPolicy" :rows="3" fill="outline" placeholder="请输入儿童政策" />
            </label>

            <label class="settings-form-field settings-form-field--full">
              <span>吸烟政策</span>
              <ion-textarea v-model="policyForm.smokingPolicy" :rows="3" fill="outline" placeholder="请输入吸烟政策" />
            </label>

            <label class="settings-form-field settings-form-field--full">
              <span>宠物政策</span>
              <ion-textarea v-model="policyForm.petPolicy" :rows="3" fill="outline" placeholder="请输入宠物政策" />
            </label>

            <label class="settings-form-field settings-form-field--full">
              <span>附加规则</span>
              <ion-textarea
                v-model="policyForm.additionalRules"
                :rows="4"
                fill="outline"
                placeholder="请输入附加规则"
              />
            </label>

            <label class="settings-form-field settings-form-field--full">
              <span>酒店条款</span>
              <ion-textarea v-model="policyForm.hotelTerms" :rows="4" fill="outline" placeholder="请输入酒店条款" />
            </label>
          </div>
        </section>

        <section class="mobile-card">
          <h2 class="mobile-section-title">地图占位</h2>
          <div class="settings-map-placeholder">
            <strong>{{ form.address || '未填写地址' }}</strong>
            <p>移动端当前只展示地图占位与地址信息，后续若接入地图 SDK，将直接复用此地址字段。</p>
          </div>
        </section>

        <section class="mobile-card settings-detail-page__actions-card">
          <ion-button fill="outline" :disabled="loading || saving" @click="loadPageData">重置</ion-button>
          <ion-button :disabled="loading || saving" @click="handleSave">
            {{ saving ? '保存中...' : '保存门店详情' }}
          </ion-button>
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
  IonSelect,
  IonSelectOption,
  IonSpinner,
  IonTextarea,
  IonTitle,
  IonToolbar,
  onIonViewWillEnter,
} from '@ionic/vue'
import { computed, ref } from 'vue'
import { getStoreById, getStorePolicy, updateStore, updateStorePolicy } from '@/api/store'
import { STORE_FACILITY_OPTIONS } from '@/constants/settings'
import {
  COUNTRY_OPTIONS,
  CURRENCY_OPTIONS,
  LANGUAGE_OPTIONS,
  STORE_TIME_OPTIONS,
  TIMEZONE_OPTIONS,
} from '@/constants/store'
import { ROUTE_PATHS } from '@/router/guards'
import { useStoreStore } from '@/stores/store'
import type { StoreDTO, StoreRequest } from '@/types/store'
import type { StorePolicyDTO } from '@/types/settings'
import { showSuccessToast, showWarningToast } from '@/utils/notify'
import { isHandledRequestError } from '@/utils/request'
import { formatTextList, normalizeTextList } from '@/utils/settings'

interface StoreDetailsFormState {
  email: string
  address: string
  city: string
  state: string
  country: string
  language: string
  timezone: string
  currency: string
  description: string
  desktopPhotoUrlsText: string
  mobilePhotoUrlsText: string
}

const storeStore = useStoreStore()

const loading = ref(false)
const saving = ref(false)
const form = ref<StoreDetailsFormState>(createEmptyForm())
const policyForm = ref<StorePolicyDTO>(createEmptyPolicyForm())
const selectedFacilityLabels = ref<string[]>([])

const pageTitle = computed(() => storeStore.currentStore?.name || '门店详情')
const locationLabel = computed(() => {
  const values = [form.value.city, form.value.state, form.value.country].filter(Boolean)
  if (values.length === 0) {
    return '未设置地区'
  }
  return values.join(' · ')
})
const photoCount = computed(() => {
  return normalizeTextList(form.value.desktopPhotoUrlsText).length + normalizeTextList(form.value.mobilePhotoUrlsText).length
})

function createEmptyForm(): StoreDetailsFormState {
  return {
    email: '',
    address: '',
    city: '',
    state: '',
    country: 'China',
    language: 'en',
    timezone: 'Asia/Shanghai',
    currency: 'CNY',
    description: '',
    desktopPhotoUrlsText: '',
    mobilePhotoUrlsText: '',
  }
}

function createEmptyPolicyForm(): StorePolicyDTO {
  return {
    checkinTime: '15:00',
    checkoutTime: '11:00',
    childPolicy: '',
    smokingPolicy: '',
    petPolicy: '',
    additionalRules: '',
    hotelTerms: '',
  }
}

function resolveWarningMessage(error: unknown, fallbackMessage: string) {
  if (error instanceof Error && error.message) {
    return error.message
  }
  return fallbackMessage
}

function fillStoreForm(store: StoreDTO) {
  form.value = {
    email: store.email || '',
    address: store.address || '',
    city: store.city || '',
    state: store.state || '',
    country: store.country || 'China',
    language: store.language || 'en',
    timezone: store.timezone || 'Asia/Shanghai',
    currency: store.currency || 'CNY',
    description: store.description || '',
    desktopPhotoUrlsText: formatTextList(store.desktopPhotoUrls),
    mobilePhotoUrlsText: formatTextList(store.mobilePhotoUrls),
  }

  const facilityLabels: string[] = []
  for (const selectedFacility of store.facilities || []) {
    const matched = STORE_FACILITY_OPTIONS.find((item) => {
      return item.payload.name === selectedFacility.name && (item.payload.group || '') === (selectedFacility.group || '')
    })
    if (matched) {
      facilityLabels.push(matched.label)
    }
  }
  selectedFacilityLabels.value = facilityLabels
}

function buildStorePayload(): Partial<StoreRequest> {
  const facilities = STORE_FACILITY_OPTIONS.filter((item) => selectedFacilityLabels.value.includes(item.label)).map(
    (item) => item.payload,
  )

  return {
    email: form.value.email.trim(),
    address: form.value.address.trim(),
    city: form.value.city.trim(),
    state: form.value.state.trim(),
    country: form.value.country,
    language: form.value.language,
    timezone: form.value.timezone,
    currency: form.value.currency,
    description: form.value.description.trim(),
    facilities,
    desktopPhotoUrls: normalizeTextList(form.value.desktopPhotoUrlsText),
    mobilePhotoUrls: normalizeTextList(form.value.mobilePhotoUrlsText),
  }
}

async function loadPageData() {
  const storeId = storeStore.currentStore?.id
  if (!storeId) {
    showWarningToast('请先选择门店')
    return
  }

  loading.value = true
  try {
    const [storeResponse, policyResponse] = await Promise.all([getStoreById(storeId), getStorePolicy(storeId)])
    if (!storeResponse.success || !storeResponse.data) {
      throw new Error(storeResponse.message || '加载门店详情失败')
    }
    if (!policyResponse.success || !policyResponse.data) {
      throw new Error(policyResponse.message || '加载门店政策失败')
    }

    fillStoreForm(storeResponse.data)
    policyForm.value = {
      ...createEmptyPolicyForm(),
      ...policyResponse.data,
    }
    storeStore.setCurrentStore(storeResponse.data)
  } catch (error) {
    if (!isHandledRequestError(error)) {
      showWarningToast(resolveWarningMessage(error, '加载门店详情失败'))
    }
  } finally {
    loading.value = false
  }
}

async function handleSave() {
  const currentStore = storeStore.currentStore
  if (!currentStore?.id) {
    showWarningToast('请先选择门店')
    return
  }

  if (!form.value.country) {
    showWarningToast('请选择国家')
    return
  }

  saving.value = true
  try {
    const storePayload = {
      name: currentStore.name,
      phone: currentStore.phone,
      phoneTechType: currentStore.phoneTechType,
      type: currentStore.type,
      timezone: currentStore.timezone,
      manager: currentStore.manager,
      country: currentStore.country,
      city: currentStore.city,
      state: currentStore.state,
      address: currentStore.address,
      currency: currentStore.currency,
      suHotelId: currentStore.suHotelId,
      ownerEmail: currentStore.ownerEmail,
      language: currentStore.language,
      description: currentStore.description,
      logo: currentStore.logo,
      email: currentStore.email,
      wechat: currentStore.wechat,
      whatsapp: currentStore.whatsapp,
      line: currentStore.line,
      facilities: currentStore.facilities,
      desktopPhotoUrls: currentStore.desktopPhotoUrls,
      mobilePhotoUrls: currentStore.mobilePhotoUrls,
      localizedContent: currentStore.localizedContent,
      ...buildStorePayload(),
      checkinTime: policyForm.value.checkinTime,
      checkoutTime: policyForm.value.checkoutTime,
    }

    const storeResponse = await updateStore(currentStore.id, storePayload as StoreRequest)
    if (!storeResponse.success || !storeResponse.data) {
      throw new Error(storeResponse.message || '保存门店详情失败')
    }

    const policyResponse = await updateStorePolicy(currentStore.id, {
      ...policyForm.value,
      checkinTime: policyForm.value.checkinTime,
      checkoutTime: policyForm.value.checkoutTime,
      childPolicy: policyForm.value.childPolicy?.trim(),
      smokingPolicy: policyForm.value.smokingPolicy?.trim(),
      petPolicy: policyForm.value.petPolicy?.trim(),
      additionalRules: policyForm.value.additionalRules?.trim(),
      hotelTerms: policyForm.value.hotelTerms?.trim(),
    })

    if (!policyResponse.success || !policyResponse.data) {
      throw new Error(policyResponse.message || '保存门店政策失败')
    }

    storeStore.setCurrentStore(storeResponse.data)
    showSuccessToast('门店详情已保存')
    await loadPageData()
  } catch (error) {
    if (!isHandledRequestError(error)) {
      showWarningToast(resolveWarningMessage(error, '保存门店详情失败'))
    }
  } finally {
    saving.value = false
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

.settings-map-placeholder {
  display: grid;
  gap: 8px;
  padding: 16px;
  border-radius: 18px;
  background: var(--app-primary-soft);
}

.settings-map-placeholder strong,
.settings-map-placeholder p {
  margin: 0;
}

.settings-map-placeholder p {
  color: var(--app-muted);
  line-height: 1.6;
}

.settings-detail-page__actions-card {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  flex-wrap: wrap;
}
</style>
