<template>
  <SettingsDetailFormShell
    :back-href="ROUTE_PATHS.settings"
    :title="$t('settingsStage4.storeDetails.title')"
    :hero-eyebrow="$t('common.currentStore')"
    :hero-title="pageTitle"
    :show-refresher="true"
    :refresher-pulling-text="$t('stage5UiAttributes.19')"
    @refresh="handleRefresh"
  >
    <template #heroExtra>
      <div class="settings-detail-hero__summary">
        <div class="settings-detail-hero__meta">
          <div class="settings-detail-hero__meta-item">
            <span class="settings-detail-hero__meta-label">{{ $t('stage5SourceText.39') }}</span>
            <strong>{{ locationLabel }}</strong>
          </div>
          <div class="settings-detail-hero__meta-item">
            <span class="settings-detail-hero__meta-label">{{ $t('settingsStage4.storeDetails.fields.language') }}</span>
            <strong>{{ languageLabel }}</strong>
          </div>
          <div class="settings-detail-hero__meta-item">
            <span class="settings-detail-hero__meta-label">{{ $t('settingsStage4.storeBasic.fields.timezone') }}</span>
            <strong>{{ timezoneLabel }}</strong>
          </div>
          <div class="settings-detail-hero__meta-item">
            <span class="settings-detail-hero__meta-label">{{ $t('stage5SourceText.53') }}</span>
            <strong>{{ currencyLabel }}</strong>
          </div>
        </div>
      </div>
    </template>

    <SettingsSectionCard
      :title="$t('settingsStage4.storeDetails.title')"
      :loading="loading"
      header-class="settings-detail-page__section-header"
    >
      <div class="settings-form-grid settings-form-grid--top">
        <label class="settings-form-field">
          <span>{{ $t('settingsStage4.storeDetails.fields.email') }}</span>
          <ion-input v-model="form.email" fill="outline" :placeholder="$t('settingsStage4.accountList.placeholders.email')" />
        </label>

        <label class="settings-form-field">
          <span>{{ $t('settingsStage4.storeDetails.fields.language') }}</span>
          <ion-select v-model="form.language" fill="outline" interface="modal">
            <ion-select-option v-for="option in LANGUAGE_OPTIONS" :key="option.value" :value="option.value">
              {{ option.label }}
            </ion-select-option>
          </ion-select>
        </label>

        <label class="settings-form-field settings-form-field--full">
          <span>{{ $t('storeSelection.field.address') }}</span>
          <ion-input v-model="form.address" fill="outline" :placeholder="$t('settingsStage4.storeDetails.placeholders.address')" />
        </label>

        <label class="settings-form-field">
          <span>{{ $t('settingsStage4.storeBasic.fields.city') }}</span>
          <ion-input v-model="form.city" fill="outline" :placeholder="$t('settingsStage4.storeBasic.placeholders.city')" />
        </label>

        <label class="settings-form-field">
          <span>{{ $t('stage5SourceText.51') }}</span>
          <ion-input v-model="form.state" fill="outline" :placeholder="$t('stage5UiAttributes.73')" />
        </label>

        <label class="settings-form-field">
          <span>{{ $t('stage5.publicRegistration.form.country') }}</span>
          <ion-select v-model="form.country" fill="outline" interface="modal">
            <ion-select-option v-for="option in countryOptions" :key="option.value" :value="option.value">
              {{ option.label }}
            </ion-select-option>
          </ion-select>
        </label>

        <label class="settings-form-field">
          <span>{{ $t('settingsStage4.storeBasic.fields.timezone') }}</span>
          <ion-select v-model="form.timezone" fill="outline" interface="modal">
            <ion-select-option v-for="option in TIMEZONE_OPTIONS" :key="option.value" :value="option.value">
              {{ option.label }}
            </ion-select-option>
          </ion-select>
        </label>

        <label class="settings-form-field">
          <span>{{ $t('stage5SourceText.53') }}</span>
          <ion-select v-model="form.currency" fill="outline" interface="modal">
            <ion-select-option v-for="option in currencyOptions" :key="option.value" :value="option.value">
              {{ option.label }}
            </ion-select-option>
          </ion-select>
        </label>

        <label class="settings-form-field">
          <span>{{ $t('order.table.checkInTime') }}</span>
          <ion-select v-model="policyForm.checkinTime" fill="outline" interface="modal">
            <ion-select-option v-for="option in STORE_TIME_OPTIONS" :key="option.value" :value="option.value">
              {{ option.label }}
            </ion-select-option>
          </ion-select>
        </label>

        <label class="settings-form-field">
          <span>{{ $t('settingsStage4.storeDetails.fields.checkoutTime') }}</span>
          <ion-select v-model="policyForm.checkoutTime" fill="outline" interface="modal">
            <ion-select-option v-for="option in STORE_TIME_OPTIONS" :key="option.value" :value="option.value">
              {{ option.label }}
            </ion-select-option>
          </ion-select>
        </label>

        <label class="settings-form-field settings-form-field--full">
          <span>{{ $t('stage5SourceText.226') }}</span>
          <ion-select v-model="selectedFacilityKeys" fill="outline" interface="modal" multiple>
            <ion-select-option v-for="item in STORE_FACILITY_OPTIONS" :key="item.key" :value="item.key">
              {{ t(item.labelKey) }}
            </ion-select-option>
          </ion-select>
        </label>

        <label class="settings-form-field settings-form-field--full">
          <span>{{ $t('settingsStage4.storeDetails.fields.description') }}</span>
          <ion-textarea v-model="form.description" :rows="4" fill="outline" :placeholder="$t('stage5UiAttributes.99')" />
        </label>

        <label class="settings-form-field settings-form-field--full">
          <span>{{ $t('stage5SourceText.150') }}</span>
          <ion-textarea
            v-model="form.desktopPhotoUrlsText"
            :rows="4"
            fill="outline"
            :placeholder="$t('stage5UiAttributes.52')"
          />
        </label>

        <label class="settings-form-field settings-form-field--full">
          <span>{{ $t('stage5SourceText.171') }}</span>
          <ion-textarea
            v-model="form.mobilePhotoUrlsText"
            :rows="4"
            fill="outline"
            :placeholder="$t('stage5UiAttributes.52')"
          />
        </label>
      </div>
    </SettingsSectionCard>

    <SettingsSectionCard :title="$t('stage5UiAttributes.110')">
      <div class="settings-form-grid settings-form-grid--top">
        <label class="settings-form-field settings-form-field--full">
          <span>{{ $t('stage5SourceText.11') }}</span>
          <ion-textarea v-model="policyForm.childPolicy" :rows="3" fill="outline" :placeholder="$t('stage5UiAttributes.62')" />
        </label>

        <label class="settings-form-field settings-form-field--full">
          <span>{{ $t('stage5SourceText.36') }}</span>
          <ion-textarea v-model="policyForm.smokingPolicy" :rows="3" fill="outline" :placeholder="$t('stage5UiAttributes.67')" />
        </label>

        <label class="settings-form-field settings-form-field--full">
          <span>{{ $t('stage5SourceText.42') }}</span>
          <ion-textarea v-model="policyForm.petPolicy" :rows="3" fill="outline" :placeholder="$t('stage5UiAttributes.72')" />
        </label>

        <label class="settings-form-field settings-form-field--full">
          <span>{{ $t('stage5SourceText.228') }}</span>
          <ion-textarea
            v-model="policyForm.additionalRules"
            :rows="4"
            fill="outline"
            :placeholder="$t('stage5UiAttributes.102')"
          />
        </label>

        <label class="settings-form-field settings-form-field--full">
          <span>{{ $t('stage5SourceText.224') }}</span>
          <ion-textarea v-model="policyForm.hotelTerms" :rows="4" fill="outline" :placeholder="$t('stage5UiAttributes.98')" />
        </label>
      </div>
    </SettingsSectionCard>

    <SettingsSectionCard :title="$t('stage5UiAttributes.37')">
      <div class="settings-map-placeholder">
        <strong>{{ form.address || $t('stage5DynamicUi.48') }}</strong>
      </div>
    </SettingsSectionCard>

    <template #bottomActions>
      <div class="settings-page-actions">
        <ion-button fill="outline" :disabled="loading || saving" @click="loadPageData">{{ $t('accommodation.common.reset') }}</ion-button>
        <ion-button :disabled="loading || saving" @click="handleSave">
          {{ saving ? $t('channel.mobile.common.saving') : $t('stage5DynamicUi.22') }}
        </ion-button>
      </div>
    </template>
  </SettingsDetailFormShell>
</template>

<script setup lang="ts">
import { IonButton, IonInput, IonSelect, IonSelectOption, IonTextarea, onIonViewWillEnter } from '@ionic/vue'
import { computed, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { getStoreById, getStorePolicy, updateStore, updateStorePolicy } from '@/api/store'
import SettingsSectionCard from '@/components/settings/base/SettingsSectionCard.vue'
import SettingsDetailFormShell from '@/components/settings/families/SettingsDetailFormShell.vue'
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
import { DEFAULT_BUSINESS_TIME_ZONE, resolveBusinessTimeZone } from '@/utils/storeBusinessDate'

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
const { t } = useI18n()

const loading = ref(false)
const saving = ref(false)
const form = ref<StoreDetailsFormState>(createEmptyForm())
const policyForm = ref<StorePolicyDTO>(createEmptyPolicyForm())
const selectedFacilityKeys = ref<string[]>([])

const COUNTRY_OPTION_KEYS: Record<string, string> = {
  China: 'China',
  Japan: 'Japan',
  'South Korea': 'SouthKorea',
  'United Kingdom': 'UnitedKingdom',
  USA: 'USA',
}

const countryOptions = computed(() =>
  COUNTRY_OPTIONS.map((option) => ({
    ...option,
    label: t(`storeSelection.option.country.${COUNTRY_OPTION_KEYS[option.value] || option.value}`),
  })),
)

const currencyOptions = computed(() =>
  CURRENCY_OPTIONS.map((option) => ({
    ...option,
    label: t(`storeSelection.option.currency.${option.value}`),
  })),
)

const pageTitle = computed(() => storeStore.currentStore?.name || t('routes.SettingsStoreDetails'))
const locationLabel = computed(() => {
  const country = resolveOptionLabel(countryOptions.value, form.value.country, form.value.country)
  const values = [form.value.city, form.value.state, country].filter(Boolean)
  if (values.length === 0) {
    return t('settings.constants.storeDetails.unsetRegion')
  }
  return values.join(' · ')
})
const languageLabel = computed(() => {
  return resolveOptionLabel(LANGUAGE_OPTIONS, form.value.language, t('settings.constants.storeDetails.unsetLanguage'))
})
const timezoneLabel = computed(() => {
  return resolveOptionLabel(TIMEZONE_OPTIONS, form.value.timezone, t('settings.constants.storeDetails.unsetTimezone'))
})
const currencyLabel = computed(() => {
  return resolveOptionLabel(currencyOptions.value, form.value.currency, t('settings.constants.storeDetails.unsetCurrency'))
})

function createEmptyForm(): StoreDetailsFormState {
  return {
    email: '',
    address: '',
    city: '',
    state: '',
    country: 'China',
    language: 'en',
    timezone: DEFAULT_BUSINESS_TIME_ZONE,
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

function resolveOptionLabel(
  options: ReadonlyArray<{ value: string; label: string }>,
  value: string,
  fallback: string,
) {
  return options.find((option) => option.value === value)?.label || value || fallback
}

function fillStoreForm(store: StoreDTO) {
  form.value = {
    email: store.email || '',
    address: store.address || '',
    city: store.city || '',
    state: store.state || '',
    country: store.country || 'China',
    language: store.language || 'en',
    timezone: resolveBusinessTimeZone(store.timezone),
    currency: store.currency || 'CNY',
    description: store.description || '',
    desktopPhotoUrlsText: formatTextList(store.desktopPhotoUrls),
    mobilePhotoUrlsText: formatTextList(store.mobilePhotoUrls),
  }

  const facilityKeys: string[] = []
  for (const selectedFacility of store.facilities || []) {
    const matched = STORE_FACILITY_OPTIONS.find((item) => {
      return item.payload.name === selectedFacility.name && (item.payload.group || '') === (selectedFacility.group || '')
    })
    if (matched) {
      facilityKeys.push(matched.key)
    }
  }
  selectedFacilityKeys.value = facilityKeys
}

function buildStorePayload(): Partial<StoreRequest> {
  const facilities = STORE_FACILITY_OPTIONS.filter((item) => selectedFacilityKeys.value.includes(item.key)).map(
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
    showWarningToast(t('settingsStage4.roomGroup.messages.selectStore'))
    return
  }

  loading.value = true
  try {
    const [storeResponse, policyResponse] = await Promise.all([getStoreById(storeId), getStorePolicy(storeId)])
    if (!storeResponse.success || !storeResponse.data) {
      throw new Error(storeResponse.message || t('settingsStage4.storeDetails.messages.loadFailed'))
    }
    if (!policyResponse.success || !policyResponse.data) {
      throw new Error(policyResponse.message || t('stage5Pattern.loadFailed'))
    }

    fillStoreForm(storeResponse.data)
    policyForm.value = {
      ...createEmptyPolicyForm(),
      ...policyResponse.data,
    }
    storeStore.setCurrentStore(storeResponse.data)
  } catch (error) {
    if (!isHandledRequestError(error)) {
      showWarningToast(resolveWarningMessage(error, t('settingsStage4.storeDetails.messages.loadFailed')))
    }
  } finally {
    loading.value = false
  }
}

async function handleSave() {
  const currentStore = storeStore.currentStore
  if (!currentStore?.id) {
    showWarningToast(t('settingsStage4.roomGroup.messages.selectStore'))
    return
  }

  if (!form.value.country) {
    showWarningToast(t('settingsStage4.storeBasic.placeholders.country'))
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
      throw new Error(storeResponse.message || t('stage5Pattern.saveFailed'))
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
      throw new Error(policyResponse.message || t('stage5Pattern.saveFailed'))
    }

    storeStore.setCurrentStore(storeResponse.data)
    showSuccessToast(t('stage5Pattern.saveCompleted'))
    await loadPageData()
  } catch (error) {
    if (!isHandledRequestError(error)) {
      showWarningToast(resolveWarningMessage(error, t('stage5Pattern.saveFailed')))
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
</style>
