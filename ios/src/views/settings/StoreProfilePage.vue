<template>
  <SettingsDetailFormShell
    :back-href="ROUTE_PATHS.settings"
    :title="$t('settings.entries.storeProfile.0')"
    :hero-eyebrow="$t('common.currentStore')"
    :hero-title="pageTitle"
    :toolbar-action-label="$t('settingsStage4.storeDetails.title')"
    :show-refresher="true"
    :refresher-pulling-text="$t('stage5UiAttributes.20')"
    @refresh="handleRefresh"
    @toolbar-action="handleOpenStoreDetails"
  >
    <template #heroExtra>
      <div class="settings-detail-hero__summary">
        <div class="settings-detail-hero__meta">
          <div class="settings-detail-hero__meta-item">
            <span class="settings-detail-hero__meta-label">{{ $t('stage5SourceText.39') }}</span>
            <strong>{{ locationLabel }}</strong>
          </div>
          <div class="settings-detail-hero__meta-item">
            <span class="settings-detail-hero__meta-label">{{ $t('stage5SourceText.53') }}</span>
            <strong>{{ currencyLabel }}</strong>
          </div>
          <div class="settings-detail-hero__meta-item">
            <span class="settings-detail-hero__meta-label">{{ $t('settingsStage4.storeBasic.fields.type') }}</span>
            <strong>{{ storeTypeLabel }}</strong>
          </div>
          <div class="settings-detail-hero__meta-item">
            <span class="settings-detail-hero__meta-label">{{ $t('settingsStage4.storeBasic.fields.timezone') }}</span>
            <strong>{{ timezoneLabel }}</strong>
          </div>
        </div>
      </div>
    </template>

    <SettingsSectionCard
      :title="$t('stage5UiAttributes.38')"
      :loading="loading"
      header-class="settings-detail-page__section-header"
    >
      <div class="settings-form-grid settings-form-grid--top">
        <label class="settings-form-field">
          <span>{{ $t('settingsStage4.storeBasic.fields.name') }}</span>
          <ion-input v-model="form.name" fill="outline" :placeholder="$t('settingsStage4.storeBasic.placeholders.name')" />
        </label>

        <label class="settings-form-field">
          <span>{{ $t('settingsStage4.storeDetails.fields.phone') }}</span>
          <ion-input v-model="form.phone" fill="outline" :placeholder="$t('settingsStage4.storeDetails.placeholders.phone')" />
        </label>

        <label class="settings-form-field">
          <span>{{ $t('settingsStage4.storeBasic.fields.type') }}</span>
          <ion-input v-model="form.type" fill="outline" :placeholder="$t('stage5UiAttributes.101')" />
        </label>

        <label class="settings-form-field">
          <span>{{ $t('settingsStage4.storeBasic.fields.timezone') }}</span>
          <ion-input v-model="form.timezone" fill="outline" :placeholder="$t('stage5UiAttributes.82')" />
        </label>

        <label class="settings-form-field">
          <span>{{ $t('settingsStage4.storeBasic.fields.manager') }}</span>
          <ion-input v-model="form.manager" fill="outline" :placeholder="$t('settingsStage4.storeBasic.placeholders.manager')" />
        </label>

        <label class="settings-form-field">
          <span>{{ $t('settingsStage4.storeBasic.fields.ownerEmail') }}</span>
          <ion-input v-model="form.ownerEmail" fill="outline" :placeholder="$t('stage5UiAttributes.97')" />
        </label>

        <label class="settings-form-field">
          <span>{{ $t('stage5.publicRegistration.form.country') }}</span>
          <ion-input v-model="form.country" fill="outline" :placeholder="$t('stage5UiAttributes.70')" />
        </label>

        <label class="settings-form-field">
          <span>{{ $t('settingsStage4.storeBasic.fields.city') }}</span>
          <ion-input v-model="form.city" fill="outline" :placeholder="$t('settingsStage4.storeBasic.placeholders.city')" />
        </label>

        <label class="settings-form-field">
          <span>{{ $t('settingsStage4.storeDetails.fields.address') }}</span>
          <ion-input v-model="form.address" fill="outline" :placeholder="$t('stage5UiAttributes.71')" />
        </label>

        <label class="settings-form-field">
          <span>{{ $t('stage5SourceText.53') }}</span>
          <ion-input v-model="form.currency" fill="outline" :placeholder="$t('stage5UiAttributes.74')" />
        </label>

        <label class="settings-form-field settings-form-field--full">
          <span>{{ $t('stage5SourceText.175') }}</span>
          <ion-textarea v-model="form.description" :rows="5" fill="outline" :placeholder="$t('stage5UiAttributes.100')" />
        </label>
      </div>

    </SettingsSectionCard>

    <template #bottomActions>
      <div class="settings-page-actions">
        <ion-button fill="outline" :disabled="loading || saving" @click="loadStoreDetail">{{ $t('accommodation.common.reset') }}</ion-button>
        <ion-button :disabled="loading || saving" @click="handleSave">
          {{ saving ? $t('channel.mobile.common.saving') : $t('profile.save') }}
        </ion-button>
      </div>
    </template>
  </SettingsDetailFormShell>
</template>

<script setup lang="ts">
import { useI18n } from 'vue-i18n'
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

const { t } = useI18n()

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
  return form.value.name || storeStore.currentStore?.name || t('settings.entries.storeProfile.0')
})

const locationLabel = computed(() => {
  const parts = [form.value.city, form.value.country].filter(Boolean)
  if (parts.length === 0) {
    return t('settings.constants.storeDetails.unsetRegion')
  }
  return parts.join(' · ')
})

const currencyLabel = computed(() => {
  return form.value.currency || t('settings.constants.storeDetails.unsetCurrency')
})

const storeTypeLabel = computed(() => {
  return form.value.type || t('settingsResidual.common.notConfigured')
})

const timezoneLabel = computed(() => {
  return form.value.timezone || t('settings.constants.storeDetails.unsetTimezone')
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
    showWarningToast(t('settingsStage4.roomGroup.messages.selectStore'))
    return
  }

  loading.value = true
  try {
    const response = await getStoreById(storeId)
    if (!response.success || !response.data) {
      throw new Error(response.message || t('stage5Pattern.loadFailed'))
    }

    fillForm(response.data)
    storeStore.setCurrentStore(response.data)
  } catch (error) {
    if (!isHandledRequestError(error)) {
      showWarningToast(resolveWarningMessage(error, t('stage5Pattern.loadFailed')))
    }
  } finally {
    loading.value = false
  }
}

async function handleSave() {
  const storeId = storeStore.currentStore?.id
  if (!storeId) {
    showWarningToast(t('settingsStage4.roomGroup.messages.selectStore'))
    return
  }

  if (!form.value.name.trim()) {
    showWarningToast(t('settingsStage4.storeBasic.placeholders.name'))
    return
  }

  if (!form.value.country.trim()) {
    showWarningToast(t('stage5UiAttributes.70'))
    return
  }

  saving.value = true
  try {
    const response = await updateStore(storeId, buildRequestData())
    if (!response.success || !response.data) {
      throw new Error(response.message || t('stage5Pattern.saveFailed'))
    }

    fillForm(response.data)
    storeStore.setCurrentStore(response.data)
    showSuccessToast(t('stage5Pattern.saveCompleted'))
  } catch (error) {
    if (!isHandledRequestError(error)) {
      showWarningToast(resolveWarningMessage(error, t('stage5Pattern.saveFailed')))
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
