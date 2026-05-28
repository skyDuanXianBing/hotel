<template>
  <div class="store-details-container">
    <div class="config-page">
      <div class="page-header">
        <h2 class="page-title">{{ t('settingsStage4.storeDetails.title') }}</h2>
      </div>

      <el-tabs v-model="activeTab" class="store-tabs">
        <el-tab-pane :label="t('settingsStage4.storeDetails.tabs.details')" name="details">
          <div class="details-content">
            <div class="store-header">
              <h2 class="store-title">{{ storeDetails.name || '-' }}</h2>
              <el-button type="primary" :loading="loading" @click="handleEdit">{{ t('settings.common.edit') }}</el-button>
            </div>

            <div class="main-content">
              <div v-if="false" class="image-section">
                <div class="store-image">
                  <img
                    v-if="storeDetails.logo"
                    :src="storeDetails.logo"
                    :alt="t('settingsStage4.storeDetails.logoAlt')"
                    class="store-logo-image"
                  />
                  <template v-else>
                    <el-icon class="upload-icon"><UploadFilled /></el-icon>
                    <p class="upload-text">{{ t('settingsStage4.storeDetails.uploadImage') }}</p>
                  </template>
                </div>
              </div>

              <div class="info-section">
                <div class="info-grid">
                  <div class="info-item">
                    <label class="info-label">{{ t('settingsStage4.storeDetails.fields.phone') }}</label>
                    <span class="info-value">{{ storeDetails.phone || '-' }}</span>
                  </div>
                  <div class="info-item">
                    <label class="info-label">{{ t('settingsStage4.storeDetails.fields.email') }}</label>
                    <span class="info-value">{{ storeDetails.email || '-' }}</span>
                  </div>
                  <div class="info-item">
                    <label class="info-label">{{ t('settingsStage4.storeDetails.fields.propertyType') }}</label>
                    <span class="info-value">{{ formatPropertyType(storeDetails.type) }}</span>
                  </div>
                  <div class="info-item full-width">
                    <label class="info-label">{{ t('settingsStage4.storeDetails.fields.address') }}</label>
                    <span class="info-value">{{ storeDetails.address || '-' }}</span>
                  </div>
                  <div class="info-item">
                    <label class="info-label">{{ t('settingsStage4.storeBasic.fields.city') }}</label>
                    <span class="info-value">{{ storeDetails.city || '-' }}</span>
                  </div>
                  <div class="info-item">
                    <label class="info-label">{{ t('settingsStage4.storeDetails.fields.state') }}</label>
                    <span class="info-value">{{ storeDetails.state || '-' }}</span>
                  </div>
                  <div class="info-item">
                    <label class="info-label">{{ t('settingsStage4.storeBasic.fields.country') }}</label>
                    <span class="info-value">{{ formatCountry(storeDetails.country) }}</span>
                  </div>
                  <div class="info-item">
                    <label class="info-label">{{ t('settingsStage4.storeDetails.fields.language') }}</label>
                    <span class="info-value">{{ formatLanguage(storeDetails.language) }}</span>
                  </div>
                  <div class="info-item">
                    <label class="info-label">{{ t('settingsStage4.storeBasic.fields.timezone') }}</label>
                    <span class="info-value">{{ storeDetails.timezone || '-' }}</span>
                  </div>
                  <div class="info-item">
                    <label class="info-label">{{ t('settingsStage4.storeBasic.fields.currency') }}</label>
                    <span class="info-value">{{ storeDetails.currency || '-' }}</span>
                  </div>
                  <div class="info-item">
                    <label class="info-label">{{ t('settingsStage4.storeDetails.fields.checkinTime') }}</label>
                    <span class="info-value">{{ storeDetails.checkinTime || '-' }}</span>
                  </div>
                  <div class="info-item">
                    <label class="info-label">{{ t('settingsStage4.storeDetails.fields.checkoutTime') }}</label>
                    <span class="info-value">{{ storeDetails.checkoutTime || '-' }}</span>
                  </div>
                  <div class="info-item full-width">
                    <label class="info-label">{{ t('settingsStage4.storeDetails.fields.description') }}</label>
                    <span class="info-value">{{ storeDetails.description || '-' }}</span>
                  </div>
                </div>
              </div>
            </div>

            <div class="map-section">
              <div class="map-header">
                <el-radio-group v-model="mapType" size="small">
                  <el-radio-button label="map">{{ t('settingsStage4.storeDetails.mapTypes.map') }}</el-radio-button>
                  <el-radio-button label="satellite">{{ t('settingsStage4.storeDetails.mapTypes.satellite') }}</el-radio-button>
                </el-radio-group>
              </div>
              <div class="map-container">
                <div class="map-placeholder">
                  <p>{{ t('settingsStage4.storeDetails.mapPlaceholder') }}</p>
                </div>
              </div>
            </div>
          </div>
        </el-tab-pane>

        <el-tab-pane :label="t('settingsStage4.storeDetails.tabs.facilities')" name="facilities">
          <div class="facilities-content">
            <div class="facilities-header">
              <h2 class="facilities-title">{{ storeDetails.name || '-' }}</h2>
              <el-button type="primary" :loading="loading" @click="handleEditFacilities">{{ t('settings.common.edit') }}</el-button>
            </div>

            <div class="facilities-main">
              <div v-if="false" class="facilities-image-section">
                <div class="facilities-image">
                  <img
                    v-if="storeDetails.logo"
                    :src="storeDetails.logo"
                    :alt="t('settingsStage4.storeDetails.logoAlt')"
                    class="store-logo-image"
                  />
                  <template v-else>
                    <el-icon class="upload-icon"><UploadFilled /></el-icon>
                    <p class="upload-text">{{ t('settingsStage4.storeDetails.uploadImage') }}</p>
                  </template>
                </div>
              </div>

              <div class="facilities-info-section">
                <div class="facilities-grid">
                  <div v-for="facility in selectedFacilitiesList" :key="facility" class="facility-tag">
                    {{ facility }}
                  </div>
                  <div v-if="selectedFacilitiesList.length === 0" class="no-facilities">{{ t('settingsStage4.storeDetails.empty.facilities') }}</div>
                </div>
              </div>
            </div>
          </div>
        </el-tab-pane>

        <el-tab-pane :label="t('settingsStage4.storeDetails.tabs.photos')" name="photos">
          <div class="photos-content">
            <div class="photos-header">
              <h2 class="photos-title">{{ storeDetails.name || '-' }}</h2>
              <el-button type="primary" :loading="loading" @click="handleEditPhotos">{{ t('settings.common.edit') }}</el-button>
            </div>

            <div class="photo-section">
              <h3 class="photo-section-title">{{ t('settingsStage4.storeDetails.tabs.photos') }}</h3>
              <div class="photos-grid">
                <div v-for="photo in photos" :key="photo.uid || photo.url" class="photo-item">
                  <img v-if="photo.url" :src="photo.url" :alt="t('settingsStage4.storeDetails.photoAlt')" class="photo-img" />
                </div>
                <div v-if="photos.length === 0" class="no-photos">{{ t('settingsStage4.storeDetails.empty.photos') }}</div>
              </div>
            </div>
          </div>
        </el-tab-pane>
      </el-tabs>
    </div>

    <el-dialog
      v-model="editDialogVisible"
      :title="t('settings.common.edit')"
      width="800px"
      :close-on-click-modal="false"
    >
      <el-form ref="formRef" :model="editForm" :rules="formRules" label-width="100px">
        <el-form-item :label="t('settingsStage4.storeBasic.fields.name')" prop="name">
          <div style="width: 100%">
            <div class="form-hint" style="margin-bottom: 8px">{{ t('settingsStage4.storeDetails.hints.guestFacingName') }}</div>
            <el-input v-model="editForm.name" :placeholder="t('settingsStage4.storeDetails.placeholders.name')" />
          </div>
        </el-form-item>

        <el-form-item :label="t('settingsStage4.storeDetails.fields.logo')">
          <el-upload v-if="false"
            :show-file-list="false"
            :before-upload="beforeImageUpload"
            :http-request="handlePhotoRequest"
          >
            <div class="upload-area-dialog">
              <img v-if="editForm.logo" :src="editForm.logo" alt="logo" class="store-logo-image" />
              <template v-else>
                <el-icon class="upload-icon"><UploadFilled /></el-icon>
                <p class="upload-text">{{ t('settingsStage4.storeDetails.upload') }}</p>
              </template>
            </div>
          </el-upload>
        </el-form-item>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item :label="t('settingsStage4.storeDetails.fields.phone')" prop="phone">
              <el-input v-model="editForm.phone" :placeholder="t('settingsStage4.storeDetails.placeholders.phone')" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item :label="t('settingsStage4.storeDetails.fields.email')" prop="email">
              <el-input v-model="editForm.email" :placeholder="t('settingsStage4.storeBasic.placeholders.ownerEmail')" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item :label="t('settingsStage4.storeDetails.fields.propertyType')" prop="type">
              <el-select v-model="editForm.type" :placeholder="t('settingsStage4.storeDetails.placeholders.propertyType')" style="width: 100%">
                <el-option
                  v-for="option in PROPERTY_TYPE_OPTIONS"
                  :key="option.value"
                  :label="option.labelKey ? t(option.labelKey) : option.label"
                  :value="option.value"
                />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item :label="t('settingsStage4.storeDetails.fields.address')" prop="address">
          <el-input v-model="editForm.address" :placeholder="t('settingsStage4.storeDetails.placeholders.address')" />
        </el-form-item>

        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item :label="t('settingsStage4.storeBasic.fields.city')" prop="city">
              <el-input v-model="editForm.city" :placeholder="t('settingsStage4.storeBasic.placeholders.city')" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item :label="t('settingsStage4.storeDetails.fields.state')">
              <el-input v-model="editForm.state" :placeholder="t('settingsStage4.storeDetails.placeholders.state')" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item :label="t('settingsStage4.storeBasic.fields.country')" prop="country">
              <el-select v-model="editForm.country" :placeholder="t('settingsStage4.storeBasic.placeholders.country')" style="width: 100%">
                <el-option
                  v-for="option in COUNTRY_OPTIONS"
                  :key="option.value"
                  :label="option.labelKey ? t(option.labelKey) : option.label"
                  :value="option.value"
                />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item :label="t('settingsStage4.storeBasic.fields.timezone')" prop="timezone">
              <el-select v-model="editForm.timezone" :placeholder="t('settingsStage4.storeBasic.placeholders.timezone')" style="width: 100%">
                <el-option
                  v-for="option in TIMEZONE_OPTIONS"
                  :key="option.value"
                  :label="option.label"
                  :value="option.value"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item :label="t('settingsStage4.storeBasic.fields.currency')" prop="currency">
              <el-select v-model="editForm.currency" :placeholder="t('settingsStage4.storeBasic.placeholders.currency')" style="width: 100%">
                <el-option
                  v-for="option in CURRENCY_OPTIONS"
                  :key="option.value"
                  :label="option.labelKey ? t(option.labelKey) : option.label"
                  :value="option.value"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item :label="t('settingsStage4.storeDetails.fields.language')" prop="language">
              <el-select v-model="editForm.language" :placeholder="t('settingsStage4.storeDetails.placeholders.language')" style="width: 100%">
                <el-option
                  v-for="option in LANGUAGE_OPTIONS"
                  :key="option.value"
                  :label="option.labelKey ? t(option.labelKey) : option.label"
                  :value="option.value"
                />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item :label="t('settingsStage4.storeDetails.fields.checkinTime')" prop="checkinTime">
              <el-time-select
                v-model="editForm.checkinTime"
                start="00:00"
                step="00:30"
                end="23:30"
                :placeholder="t('settingsStage4.storeDetails.placeholders.checkinTime')"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item :label="t('settingsStage4.storeDetails.fields.checkoutTime')" prop="checkoutTime">
              <el-time-select
                v-model="editForm.checkoutTime"
                start="00:00"
                step="00:30"
                end="23:30"
                :placeholder="t('settingsStage4.storeDetails.placeholders.checkoutTime')"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item :label="t('settingsStage4.storeDetails.fields.description')">
          <el-input
            v-model="editForm.description"
            type="textarea"
            :rows="4"
            :placeholder="t('settingsStage4.storeDetails.placeholders.description')"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="handleCancelEdit">{{ t('settings.common.cancel') }}</el-button>
          <el-button type="primary" :loading="loading" @click="handleSaveEdit">{{ t('settings.common.save') }}</el-button>
        </div>
      </template>
    </el-dialog>

    <el-dialog
      v-model="editFacilitiesDialogVisible"
      :title="t('settings.common.edit')"
      width="900px"
      :close-on-click-modal="false"
    >
      <div class="facilities-edit-grid">
        <el-checkbox
          v-for="facility in STORE_FACILITY_OPTIONS"
          :key="facility.payload.name"
          v-model="selectedFacilities"
          :label="facility.payload.name"
          :value="facility.payload.name"
        >
          {{ t(facility.labelKey) }}
        </el-checkbox>
      </div>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="handleCancelEditFacilities">{{ t('settings.common.cancel') }}</el-button>
          <el-button type="primary" :loading="loading" @click="handleSaveEditFacilities">{{ t('settings.common.save') }}</el-button>
        </div>
      </template>
    </el-dialog>

    <el-dialog
      v-model="editPhotosDialogVisible"
      :title="t('settings.common.edit')"
      width="900px"
      :close-on-click-modal="false"
    >
      <div class="photos-edit-container">
        <div class="upload-tip">
          <h4 class="upload-tip-title">{{ t('settingsStage4.storeDetails.photos.uploadTitle') }}</h4>
          <p class="upload-tip-text">
            {{ t('settingsStage4.storeDetails.photos.syncTip') }}
          </p>
        </div>

        <div class="upload-section">
          <h4 class="upload-section-title">{{ t('settingsStage4.storeDetails.tabs.photos') }}</h4>
          <div class="upload-info">
            <p>{{ t('settingsStage4.storeDetails.photos.maxCount') }}</p>
            <p>{{ t('settingsStage4.storeDetails.photos.maxSize') }}</p>
            <p>{{ t('settingsStage4.storeDetails.photos.recommendedSize') }}</p>
          </div>
          <el-upload
            v-model:file-list="photos"
            class="upload-area-large"
            list-type="picture-card"
            :before-upload="beforeImageUpload"
            :http-request="handlePhotoRequest"
          >
            <el-icon class="upload-icon-large"><UploadFilled /></el-icon>
            <p class="upload-text-large">{{ t('settingsStage4.storeDetails.upload') }}</p>
          </el-upload>
        </div>
      </div>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="handleCancelEditPhotos">{{ t('settings.common.cancel') }}</el-button>
          <el-button type="primary" :loading="loading" @click="handleSaveEditPhotos">{{ t('settings.common.save') }}</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { UploadFilled } from '@element-plus/icons-vue'
import { useI18n } from 'vue-i18n'
import {
  ElMessage,
  type FormInstance,
  type FormRules,
  type UploadRawFile,
  type UploadRequestOptions,
  type UploadUserFile,
} from 'element-plus'
import {
  getStoreById,
  updateStore,
  type FacilityDTO,
  type StoreRequest,
} from '@/api/store'
import { uploadMedia } from '@/api/media'
import { STORE_FACILITY_OPTIONS } from '@/constants/suFacilities'
import {
  COUNTRY_OPTIONS,
  CURRENCY_OPTIONS,
  LANGUAGE_OPTIONS,
  PROPERTY_TYPE_OPTIONS,
  TIMEZONE_OPTIONS,
  getStoreOptionLabel,
} from '@/constants/storeOptions'
import { useStoreStore } from '@/stores/store'

type UploadErrorParam = Parameters<NonNullable<UploadRequestOptions['onError']>>[0]

interface StoreDetailsForm extends StoreRequest {
  language: string
  phoneTechType: string
  checkinTime: string
  checkoutTime: string
}

const storeStore = useStoreStore()
const { t } = useI18n()
const currentStoreId = computed(() => storeStore.currentStore?.id)

const activeTab = ref('details')
const editDialogVisible = ref(false)
const editFacilitiesDialogVisible = ref(false)
const editPhotosDialogVisible = ref(false)
const formRef = ref<FormInstance>()
const mapType = ref('map')
const selectedFacilities = ref<string[]>([])
const photos = ref<UploadUserFile[]>([])
const loading = ref(false)

const createEmptyStore = (): StoreDetailsForm => ({
  language: 'en',
  phoneTechType: '5',
  checkinTime: '15:00',
  checkoutTime: '11:00',
  name: '',
  description: '',
  logo: '',
  phone: '',
  email: '',
  address: '',
  city: '',
  state: '',
  country: '',
  type: '1',
  timezone: '',
  manager: '',
  currency: '',
  suHotelId: '',
  ownerEmail: '',
  facilities: [],
  desktopPhotoUrls: [],
  mobilePhotoUrls: [],
})

const storeDetails = reactive<StoreDetailsForm>(createEmptyStore())
const editForm = reactive<StoreDetailsForm>(createEmptyStore())

const formRules = computed<FormRules>(() => ({
  name: [{ required: true, message: t('settingsStage4.storeBasic.validation.nameRequired'), trigger: 'blur' }],
  phone: [{ required: true, message: t('settingsStage4.storeDetails.validation.phoneRequired'), trigger: 'blur' }],
  address: [{ required: true, message: t('settingsStage4.storeDetails.validation.addressRequired'), trigger: 'blur' }],
  city: [{ required: true, message: t('settingsStage4.storeBasic.validation.cityRequired'), trigger: 'blur' }],
  country: [{ required: true, message: t('settingsStage4.storeDetails.validation.countryRequired'), trigger: 'blur' }],
  type: [{ required: true, message: t('settingsStage4.storeDetails.validation.propertyTypeRequired'), trigger: 'change' }],
  timezone: [{ required: true, message: t('settingsStage4.storeBasic.validation.timezoneRequired'), trigger: 'change' }],
  currency: [{ required: true, message: t('settingsStage4.storeBasic.validation.currencyRequired'), trigger: 'change' }],
  language: [{ required: true, message: t('settingsStage4.storeDetails.validation.languageRequired'), trigger: 'change' }],
}))

const selectedFacilitiesList = computed(() =>
  selectedFacilities.value.map((name) => {
    const option = STORE_FACILITY_OPTIONS.find((item) => item.payload.name === name)
    return option ? t(option.labelKey) : name
  }),
)
const formatCountry = (value?: string) => getStoreOptionLabel(COUNTRY_OPTIONS, value, t)
const formatPropertyType = (value?: string) => getStoreOptionLabel(PROPERTY_TYPE_OPTIONS, value, t)
const formatLanguage = (value?: string) => getStoreOptionLabel(LANGUAGE_OPTIONS, value, t)

const buildUploadAjaxError = (error: unknown): UploadErrorParam => {
  const normalizedError = error instanceof Error ? error : new Error(String(error))
  return {
    name: normalizedError.name || 'UploadError',
    message: normalizedError.message,
    status: 0,
    method: 'POST',
    url: '',
    stack: normalizedError.stack,
  } as UploadErrorParam
}

const toUploadFiles = (urls: string[]): UploadUserFile[] =>
  urls.map((url, index) => ({
    name: `image-${index + 1}`,
    url,
  }))

const mergePhotoUrls = (desktopUrls: string[] = [], mobileUrls: string[] = []): string[] => [
  ...new Set([...desktopUrls, ...mobileUrls].filter(Boolean)),
]

const extractPhotoUrls = (files: UploadUserFile[]): string[] =>
  files
    .map((file) => file.url)
    .filter((url): url is string => Boolean(url))

const buildFacilities = (): FacilityDTO[] =>
  selectedFacilities.value
    .map((name) => STORE_FACILITY_OPTIONS.find((item) => item.payload.name === name)?.payload)
    .filter((facility): facility is FacilityDTO => Boolean(facility))

const applyFacilities = (facilities: FacilityDTO[] = []) => {
  selectedFacilities.value = STORE_FACILITY_OPTIONS.filter((option) =>
    facilities.some(
      (facility) =>
        facility.name === option.payload.name &&
        (facility.group || 'Common') === (option.payload.group || 'Common')
    )
  ).map((option) => option.payload.name)
}

const buildStorePayload = (source: StoreDetailsForm): StoreRequest => ({
  name: source.name,
  phone: source.phone,
  phoneTechType: '5',
  type: source.type,
  timezone: source.timezone,
  manager: source.manager,
  country: source.country,
  city: source.city,
  state: source.state,
  address: source.address,
  currency: source.currency,
  suHotelId: source.suHotelId,
  ownerEmail: source.ownerEmail,
  language: source.language,
  description: source.description,
  logo: source.logo,
  email: source.email,
  facilities: buildFacilities(),
  desktopPhotoUrls: extractPhotoUrls(photos.value),
  mobilePhotoUrls: [],
  checkinTime: source.checkinTime,
  checkoutTime: source.checkoutTime,
})

const loadStoreDetails = async () => {
  if (!currentStoreId.value) {
    ElMessage.warning(t('settingsStage4.storeBasic.messages.selectStore'))
    return
  }

  try {
    loading.value = true
    const response = await getStoreById(currentStoreId.value)
    if (!response.success || !response.data) {
      ElMessage.error(response.message || t('settingsStage4.storeDetails.messages.loadFailed'))
      return
    }

    const data = response.data
    Object.assign(storeDetails, createEmptyStore(), {
      language: data.language || 'en',
      phoneTechType: data.phoneTechType || '5',
      checkinTime: data.checkinTime || '15:00',
      checkoutTime: data.checkoutTime || '11:00',
      name: data.name || '',
      description: data.description || '',
      logo: data.logo || '',
      phone: data.phone || '',
      email: data.email || '',
      address: data.address || '',
      city: data.city || '',
      state: data.state || '',
      country: data.country || '',
      type: data.type || '',
      timezone: data.timezone || '',
      manager: data.manager || '',
      currency: data.currency || '',
      suHotelId: data.suHotelId || '',
      ownerEmail: data.ownerEmail || '',
      facilities: data.facilities || [],
      desktopPhotoUrls: mergePhotoUrls(data.desktopPhotoUrls || [], data.mobilePhotoUrls || []),
      mobilePhotoUrls: [],
    })

    photos.value = toUploadFiles(mergePhotoUrls(data.desktopPhotoUrls || [], data.mobilePhotoUrls || []))
    applyFacilities(data.facilities || [])
  } catch (error) {
    console.error('加载门店详情失败:', error)
    ElMessage.error(t('settingsStage4.storeDetails.messages.loadFailed'))
  } finally {
    loading.value = false
  }
}

const saveStore = async (payload: StoreRequest, successMessage: string) => {
  if (!currentStoreId.value) {
    ElMessage.warning(t('settingsStage4.storeBasic.messages.selectStore'))
    return false
  }

  const response = await updateStore(currentStoreId.value, payload)
  if (!response.success) {
    ElMessage.error(response.message || t('settingsStage4.storeBasic.messages.saveFailed'))
    return false
  }

  ElMessage.success(successMessage)
  await loadStoreDetails()
  return true
}

const handleEdit = () => {
  Object.assign(editForm, storeDetails)
  editDialogVisible.value = true
}

const handleCancelEdit = () => {
  editDialogVisible.value = false
}

const handleSaveEdit = async () => {
  try {
    const valid = await formRef.value?.validate()
    if (!valid) {
      return
    }
    loading.value = true
    const saved = await saveStore(
      buildStorePayload(editForm),
      t('settingsStage4.storeBasic.messages.saveSuccess')
    )
    if (saved) {
      editDialogVisible.value = false
    }
  } catch (error) {
    console.error('保存门店失败:', error)
    ElMessage.error(t('settingsStage4.storeDetails.messages.saveStoreFailed'))
  } finally {
    loading.value = false
  }
}

const handleEditFacilities = () => {
  editFacilitiesDialogVisible.value = true
}

const handleCancelEditFacilities = () => {
  editFacilitiesDialogVisible.value = false
}

const handleSaveEditFacilities = async () => {
  try {
    loading.value = true
    const saved = await saveStore(
      buildStorePayload(storeDetails),
      t('settingsStage4.storeDetails.messages.facilitiesSaveSuccess')
    )
    if (saved) {
      editFacilitiesDialogVisible.value = false
    }
  } catch (error) {
    console.error('保存设施失败:', error)
    ElMessage.error(t('settingsStage4.storeDetails.messages.facilitiesSaveFailed'))
  } finally {
    loading.value = false
  }
}

const handleEditPhotos = () => {
  editPhotosDialogVisible.value = true
}

const handleCancelEditPhotos = () => {
  editPhotosDialogVisible.value = false
}

const handleSaveEditPhotos = async () => {
  try {
    loading.value = true
    const saved = await saveStore(
      buildStorePayload(storeDetails),
      t('settingsStage4.storeDetails.messages.photosSaveSuccess')
    )
    if (saved) {
      editPhotosDialogVisible.value = false
    }
  } catch (error) {
    console.error('保存照片失败:', error)
    ElMessage.error(t('settingsStage4.storeDetails.messages.photosSaveFailed'))
  } finally {
    loading.value = false
  }
}

const beforeImageUpload = (file: UploadRawFile) => {
  const isImage = file.type.startsWith('image/')
  const isLt5M = file.size / 1024 / 1024 < 5

  if (!isImage) {
    ElMessage.error(t('settingsStage4.storeDetails.messages.onlyImages'))
    return false
  }

  if (!isLt5M) {
    ElMessage.error(t('settingsStage4.storeDetails.messages.imageTooLarge'))
    return false
  }

  return true
}

const handleLogoUpload = async (options: UploadRequestOptions) => {
  try {
    const response = await uploadMedia('store-logo', options.file as File)
    if (!response.success || !response.data) {
      throw new Error(response.message || t('settingsStage4.storeDetails.messages.uploadFailed'))
    }

    editForm.logo = response.data.url
    options.onSuccess?.(response)
    ElMessage.success(t('settingsStage4.storeDetails.messages.logoUploadSuccess'))
  } catch (error) {
    console.error('Logo 上传失败:', error)
    ElMessage.error(t('settingsStage4.storeDetails.messages.logoUploadFailed'))
    options.onError?.(buildUploadAjaxError(error))
  }
}

const handlePhotoUpload = async (options: UploadRequestOptions) => {
  try {
    const response = await uploadMedia('store-desktop', options.file as File)
    if (!response.success || !response.data) {
      throw new Error(response.message || t('settingsStage4.storeDetails.messages.uploadFailed'))
    }

    const matchedFile = photos.value.find((file) => file.uid === options.file.uid)

    if (matchedFile) {
      matchedFile.name = options.file.name
      matchedFile.url = response.data.url
      matchedFile.status = 'success'
    } else {
      photos.value.push({
        uid: options.file.uid,
        name: options.file.name,
        url: response.data.url,
        status: 'success',
      })
    }

    options.onSuccess?.(response)
    ElMessage.success(t('settingsStage4.storeDetails.messages.uploadSuccess'))
  } catch (error) {
    console.error('上传失败:', error)
    ElMessage.error(t('settingsStage4.storeDetails.messages.uploadFailed'))
    options.onError?.(buildUploadAjaxError(error))
  }
}

const handlePhotoRequest = (options: UploadRequestOptions) => handlePhotoUpload(options)

onMounted(() => {
  loadStoreDetails()
})
</script>

<style scoped>
.store-details-container {
  padding: 20px;
  background: #f5f7fa;
  min-height: calc(100vh - 100px);
}

.config-page {
  background: #fff;
  border-radius: 8px;
  padding: 24px;
  min-height: calc(100vh - 140px);
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 16px;
  border-bottom: 1px solid #ebeef5;
}

.page-title {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
  margin: 0;
}

.store-tabs {
  margin-top: 20px;
}

.details-content,
.facilities-content,
.photos-content {
  padding: 20px 0;
}

.store-header,
.facilities-header,
.photos-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  padding-bottom: 16px;
  border-bottom: 1px solid #ebeef5;
}

.store-title,
.facilities-title,
.photos-title {
  font-size: 24px;
  font-weight: 600;
  color: #303133;
  margin: 0;
}

.main-content,
.facilities-main {
  display: flex;
  gap: 32px;
  margin-bottom: 32px;
}

.image-section,
.facilities-image-section {
  flex-shrink: 0;
  width: 200px;
}

.store-image,
.facilities-image {
  width: 200px;
  height: 200px;
  border: 1px dashed #dcdfe6;
  border-radius: 8px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.3s;
  background: #f5f7fa;
  overflow: hidden;
}

.store-image:hover,
.facilities-image:hover {
  border-color: #409eff;
  background: #ecf5ff;
}

.store-logo-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.upload-icon {
  font-size: 48px;
  color: #909399;
  margin-bottom: 8px;
}

.upload-text {
  font-size: 14px;
  color: #606266;
  margin: 0;
}

.info-section,
.facilities-info-section {
  flex: 1;
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 20px 32px;
}

.info-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.info-item.full-width {
  grid-column: 1 / -1;
}

.info-label {
  font-size: 12px;
  color: #909399;
  font-weight: 500;
}

.info-value {
  font-size: 14px;
  color: #303133;
  word-break: break-word;
}

.map-section {
  margin-top: 32px;
  padding-top: 32px;
  border-top: 1px solid #ebeef5;
}

.map-header {
  margin-bottom: 16px;
}

.map-container {
  width: 100%;
  height: 400px;
  border: 1px solid #dcdfe6;
  border-radius: 8px;
  overflow: hidden;
}

.map-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f7fa;
  color: #909399;
  font-size: 16px;
}

.facilities-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.facility-tag {
  padding: 8px 16px;
  background: #ecf5ff;
  color: #409eff;
  border-radius: 4px;
  font-size: 14px;
}

.no-facilities {
  color: #909399;
  font-size: 14px;
}

.photo-section {
  margin-bottom: 32px;
}

.photo-section-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  margin: 0 0 16px 0;
}

.photos-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 16px;
}

.photo-item {
  position: relative;
  width: 100%;
  aspect-ratio: 16 / 9;
  border-radius: 8px;
  overflow: hidden;
  border: 1px solid #dcdfe6;
}

.photo-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.no-photos {
  grid-column: 1 / -1;
  text-align: center;
  padding: 40px;
  color: #909399;
  font-size: 14px;
}

.upload-area-dialog {
  width: 120px;
  height: 120px;
  border: 1px dashed #dcdfe6;
  border-radius: 4px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.3s;
  overflow: hidden;
}

.upload-area-dialog:hover {
  border-color: #409eff;
}

.form-hint {
  font-size: 12px;
  color: #909399;
  line-height: 1.5;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.facilities-edit-grid {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 16px;
  padding: 20px 0;
}

.facilities-edit-grid :deep(.el-checkbox) {
  margin-right: 0;
}

.facilities-edit-grid :deep(.el-checkbox__label) {
  font-size: 14px;
}

.photos-edit-container {
  padding: 20px 0;
}

.upload-tip {
  margin-bottom: 24px;
  padding: 16px;
  background: #f5f7fa;
  border-radius: 4px;
}

.upload-tip-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  margin: 0 0 8px 0;
}

.upload-tip-text {
  font-size: 14px;
  color: #606266;
  line-height: 1.6;
  margin: 0;
}

.upload-section {
  margin-bottom: 32px;
}

.upload-section-title {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
  margin: 0 0 12px 0;
}

.upload-info {
  margin-bottom: 16px;
}

.upload-info p {
  font-size: 13px;
  color: #909399;
  margin: 4px 0;
}

.upload-area-large {
  display: block;
}

.upload-area-large :deep(.el-upload) {
  width: 100%;
}

.upload-area-large :deep(.el-upload--picture-card) {
  width: 200px;
  height: 200px;
  border: 2px dashed #dcdfe6;
  border-radius: 8px;
  background: #fafafa;
}

.upload-area-large :deep(.el-upload-list--picture-card) {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.upload-icon-large {
  font-size: 64px;
  color: #909399;
  margin-bottom: 12px;
}

.upload-text-large {
  font-size: 16px;
  color: #606266;
  margin: 0;
}

:deep(.el-tabs__nav-wrap::after) {
  display: none;
}

:deep(.el-textarea__inner) {
  font-family: inherit;
}
</style>
