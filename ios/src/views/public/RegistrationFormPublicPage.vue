<template>
  <ion-page>
    <ion-header translucent>
      <ion-toolbar class="app-page-header__toolbar">
        <ion-title class="app-page-header__title public-page-header__title">
          {{ t('routeTitle') }}
        </ion-title>
      </ion-toolbar>
    </ion-header>

    <ion-content fullscreen class="mobile-page public-form-page">
      <section class="mobile-hero public-form-hero">
        <div class="public-form-hero__top">
          <div>
            <p class="mobile-note public-form-hero__eyebrow">{{ t('heroEyebrow') }}</p>
            <h1 class="mobile-title">{{ t('title') }}</h1>
            <p class="mobile-subtitle">{{ t('subtitle') }}</p>
          </div>
          <div class="public-language-switcher">
            <button
              v-for="language in languageOptions"
              :key="language.value"
              class="public-language-chip"
              :class="{ 'public-language-chip--active': selectedLanguage === language.value }"
              type="button"
              @click="handleChangeLanguage(language.value)"
            >
              {{ language.shortLabel }}
            </button>
          </div>
        </div>

        <div class="public-form-summary">
          <span class="mobile-chip">{{ t('bookingNumber') }}: {{ registration?.bookingKey || '-' }}</span>
          <span class="mobile-chip">{{ t('stay') }}: {{ stayText }}</span>
          <span class="mobile-chip">{{ t('status') }}: {{ statusLabel(registration?.status) }}</span>
        </div>
      </section>

      <div class="mobile-stack">
        <section v-if="loading" class="mobile-card public-state-card">
          <ion-spinner name="crescent" />
          <p class="mobile-note">{{ t('loading') }}</p>
        </section>

        <section v-else-if="errorMessage" class="mobile-card public-state-card public-state-card--error">
          <h2 class="mobile-section-title">{{ t('loadFailed') }}</h2>
          <p class="mobile-note">{{ errorMessage }}</p>
          <ion-button @click="loadRegistration">{{ t('retry') }}</ion-button>
        </section>

        <template v-else-if="registration">
          <section class="mobile-card">
            <div class="public-info-grid">
              <article class="public-info-item">
                <span class="public-info-item__label">{{ t('guestName') }}</span>
                <strong>{{ registration.guestName || '-' }}</strong>
              </article>
              <article class="public-info-item">
                <span class="public-info-item__label">{{ t('guestCount') }}</span>
                <strong>{{ registration.guestCount }}</strong>
              </article>
              <article class="public-info-item">
                <span class="public-info-item__label">{{ t('lastSavedAt') }}</span>
                <strong>{{ formatPublicDateTime(registration.lastSavedAt) }}</strong>
              </article>
            </div>
          </section>

          <section class="mobile-card public-step-card">
            <div class="public-step-list">
              <button
                v-for="(guest, index) in guestForms"
                :key="guest.id || index"
                class="public-step-button"
                :class="{ 'public-step-button--active': activeStepIndex === index }"
                type="button"
                @click="handleSelectStep(index)"
              >
                {{ t('guest') }} {{ index + 1 }}
              </button>
              <button
                class="public-step-button"
                :class="{ 'public-step-button--active': isReviewStep }"
                type="button"
                @click="handleSelectStep(guestForms.length)"
              >
                {{ t('reviewAndSubmit') }}
              </button>
            </div>
          </section>

          <section v-if="currentGuest" class="mobile-card public-guest-card">
            <div class="public-guest-card__header">
              <div>
                <h2 class="mobile-section-title">{{ t('guest') }} {{ activeStepIndex + 1 }}</h2>
                <p class="mobile-note">{{ t('guestSubtitle') }}</p>
              </div>
              <ion-chip :color="resolveRegistrationStatusColor(registration.status)">
                <ion-label>{{ statusLabel(registration.status) }}</ion-label>
              </ion-chip>
            </div>

            <div class="public-field-grid public-field-grid--half">
              <label class="public-field">
                <span class="public-field__label">{{ t('residenceType') }}</span>
                <select v-model="currentGuest.residenceType" class="public-input">
                  <option value="JAPAN">{{ t('residenceJapan') }}</option>
                  <option value="OTHER">{{ t('residenceOther') }}</option>
                </select>
              </label>
              <label class="public-field">
                <span class="public-field__label">{{ t('birthday') }}</span>
                <input v-model="currentGuest.birthday" class="public-input" type="date" />
              </label>
            </div>

            <div class="public-field-grid public-field-grid--half">
              <label class="public-field">
                <span class="public-field__label">{{ t('firstName') }}</span>
                <input v-model="currentGuest.firstName" class="public-input" type="text" />
              </label>
              <label class="public-field">
                <span class="public-field__label">{{ t('lastName') }}</span>
                <input v-model="currentGuest.lastName" class="public-input" type="text" />
              </label>
            </div>

            <label class="public-field">
              <span class="public-field__label">{{ t('phone') }}</span>
              <input v-model="currentGuest.phone" class="public-input" type="tel" />
            </label>

            <template v-if="currentGuest.residenceType === 'JAPAN'">
              <label class="public-field">
                <span class="public-field__label">{{ t('address') }}</span>
                <textarea v-model="currentGuest.address" class="public-input public-input--textarea"></textarea>
              </label>
            </template>

            <template v-else>
              <div class="public-field-grid public-field-grid--half">
                <label class="public-field">
                  <span class="public-field__label">{{ t('passportNumber') }}</span>
                  <input v-model="currentGuest.passportNumber" class="public-input" type="text" />
                </label>
                <label class="public-field">
                  <span class="public-field__label">{{ t('nationality') }}</span>
                  <input v-model="currentGuest.nationality" class="public-input" type="text" />
                </label>
              </div>

              <label class="public-field">
                <span class="public-field__label">{{ t('address1') }}</span>
                <input v-model="currentGuest.address1" class="public-input" type="text" />
              </label>

              <label class="public-field">
                <span class="public-field__label">{{ t('address2') }}</span>
                <input v-model="currentGuest.address2" class="public-input" type="text" />
              </label>

              <div class="public-field-grid public-field-grid--half">
                <label class="public-field">
                  <span class="public-field__label">{{ t('city') }}</span>
                  <input v-model="currentGuest.city" class="public-input" type="text" />
                </label>
                <label class="public-field">
                  <span class="public-field__label">{{ t('state') }}</span>
                  <input v-model="currentGuest.state" class="public-input" type="text" />
                </label>
              </div>

              <div class="public-field-grid public-field-grid--half">
                <label class="public-field">
                  <span class="public-field__label">{{ t('country') }}</span>
                  <input v-model="currentGuest.country" class="public-input" type="text" />
                </label>
                <label class="public-field">
                  <span class="public-field__label">{{ t('priorStay') }}</span>
                  <input v-model="currentGuest.priorStay" class="public-input" type="text" />
                </label>
              </div>

              <label class="public-field">
                <span class="public-field__label">{{ t('nextDestination') }}</span>
                <input v-model="currentGuest.nextDestination" class="public-input" type="text" />
              </label>

              <label class="public-field">
                <span class="public-field__label">{{ t('passportPhoto') }}</span>
                <input class="public-input public-input--file" type="file" accept="image/*" @change="handlePassportFileChange" />
              </label>

              <div v-if="activeGuestAttachment" class="public-passport-card">
                <img
                  class="public-passport-card__image"
                  :src="buildAttachmentPreviewUrl(activeGuestAttachment.id)"
                  :alt="t('passportImageAlt')"
                />
                <div class="public-passport-card__actions">
                  <ion-button fill="outline" size="small" @click="handleOpenPassportPreview">{{ t('preview') }}</ion-button>
                  <ion-button fill="outline" size="small" @click="handleDownloadPassport">{{ t('download') }}</ion-button>
                </div>
              </div>
            </template>

            <div class="public-action-row">
              <ion-button fill="outline" :disabled="activeStepIndex <= 0" @click="handlePreviousStep">
                {{ t('previous') }}
              </ion-button>
              <ion-button fill="outline" :disabled="saving" @click="handleSaveDraft">
                <ion-spinner v-if="saving" name="crescent" />
                <span v-else>{{ t('saveDraft') }}</span>
              </ion-button>
              <ion-button @click="handleNextStep">{{ t('next') }}</ion-button>
            </div>
          </section>

          <section v-else class="mobile-card public-review-card">
            <div class="public-review-card__header">
              <div>
                <h2 class="mobile-section-title">{{ t('reviewAndSubmit') }}</h2>
                <p class="mobile-note">{{ t('reviewNotice') }}</p>
              </div>
              <ion-chip :color="resolveRegistrationStatusColor(registration.status)">
                <ion-label>{{ statusLabel(registration.status) }}</ion-label>
              </ion-chip>
            </div>

            <article v-for="(guest, index) in guestForms" :key="guest.id || index" class="public-review-guest">
              <h3>{{ t('guest') }} {{ index + 1 }}</h3>
              <div class="public-review-grid">
                <div>{{ t('firstName') }}: {{ guest.firstName || '-' }}</div>
                <div>{{ t('lastName') }}: {{ guest.lastName || '-' }}</div>
                <div>{{ t('birthday') }}: {{ guest.birthday || '-' }}</div>
                <div>{{ t('phone') }}: {{ guest.phone || '-' }}</div>
                <div>{{ t('residenceType') }}: {{ guest.residenceType === 'OTHER' ? t('residenceOther') : t('residenceJapan') }}</div>
                <div v-if="guest.residenceType === 'JAPAN'">{{ t('address') }}: {{ guest.address || '-' }}</div>
                <div v-else>{{ t('passportNumber') }}: {{ guest.passportNumber || '-' }}</div>
                <div v-if="guest.residenceType === 'OTHER'">{{ t('country') }}: {{ guest.country || '-' }}</div>
              </div>
            </article>

            <div v-if="registration.status === 'APPROVED' && normalizedGuideLink" class="public-guide-card">
              <h3>{{ t('approvedTitle') }}</h3>
              <p class="mobile-note">{{ t('guideNotice') }}</p>
              <ion-button fill="outline" @click="handleOpenGuide">{{ t('openGuide') }}</ion-button>
            </div>

            <div class="public-action-row">
              <ion-button fill="outline" @click="handlePreviousStep">{{ t('previous') }}</ion-button>
              <ion-button fill="outline" :disabled="saving" @click="handleSaveDraft">
                <ion-spinner v-if="saving" name="crescent" />
                <span v-else>{{ t('saveDraft') }}</span>
              </ion-button>
              <ion-button
                color="success"
                :disabled="submitting || registration.status === 'SUBMITTED' || registration.status === 'APPROVED'"
                @click="handleSubmit"
              >
                <ion-spinner v-if="submitting" name="crescent" />
                <span v-else>{{ t('submit') }}</span>
              </ion-button>
            </div>
          </section>
        </template>
      </div>

      <ion-modal :is-open="passportPreviewOpen" @didDismiss="handleClosePassportPreview">
        <ion-content class="public-passport-modal">
          <div class="public-passport-modal__body">
            <img
              v-if="passportPreviewUrl"
              :src="passportPreviewUrl"
              :alt="t('passportPreviewAlt')"
            />
          </div>
        </ion-content>
      </ion-modal>
    </ion-content>
  </ion-page>
</template>

<script setup lang="ts">
import { IonButton, IonChip, IonContent, IonHeader, IonLabel, IonModal, IonPage, IonSpinner, IonTitle, IonToolbar } from '@ionic/vue'
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  buildPublicRegistrationAttachmentUrl,
  downloadPublicRegistrationAttachment,
  getPublicRegistration,
  savePublicRegistration,
  submitPublicRegistration,
  uploadPublicRegistrationPassport,
} from '@/api/publicRegistration'
import { usePublicRegistrationI18n } from '@/composables/usePublicRegistrationI18n'
import type {
  PublicRegistrationAttachment,
  PublicRegistrationGuest,
  PublicRegistrationLanguage,
  PublicRegistrationResponse,
  RegistrationFormStatus,
} from '@/types/publicRegistration'
import {
  compressImageIfNeeded,
  downloadBlobFile,
  findLatestPassportAttachment,
  formatPublicDate as formatPublicDateValue,
  formatPublicDateTime as formatPublicDateTimeValue,
  normalizeExternalLink,
  resolvePublicRegistrationErrorKey,
  resolveRegistrationStatusColor,
} from '@/utils/publicRegistration'
import { showSuccessToast, showWarningToast } from '@/utils/notify'
import { isHandledPublicRequestError } from '@/utils/publicRequest'

const route = useRoute()
const router = useRouter()

const {
  t,
  locale: selectedLanguage,
  languageOptions,
  setLocale,
} = usePublicRegistrationI18n('form', () => route.query.lang)
const registration = ref<PublicRegistrationResponse | null>(null)
const guestForms = ref<PublicRegistrationGuest[]>([])
const loading = ref(false)
const saving = ref(false)
const submitting = ref(false)
const errorMessage = ref('')
const activeStepIndex = ref(0)
const passportPreviewOpen = ref(false)
const passportPreviewUrl = ref('')

const orderNumber = computed(() => String(route.params.orderNumber || ''))
const token = computed(() => String(route.query.t || ''))
const formatPublicDate = (value?: string | null) => {
  return formatPublicDateValue(value, selectedLanguage.value)
}
const formatPublicDateTime = (value?: string | null) => {
  return formatPublicDateTimeValue(value, selectedLanguage.value)
}
const currentGuest = computed(() => {
  if (activeStepIndex.value >= guestForms.value.length) {
    return null
  }

  return guestForms.value[activeStepIndex.value]
})
const isReviewStep = computed(() => activeStepIndex.value >= guestForms.value.length)
const stayText = computed(() => {
  if (!registration.value) {
    return '-'
  }

  return `${formatPublicDate(registration.value.checkInDate)} - ${formatPublicDate(registration.value.checkOutDate)}`
})
const activeGuestAttachment = computed<PublicRegistrationAttachment | null>(() => {
  if (!registration.value || !currentGuest.value?.id) {
    return null
  }

  return findLatestPassportAttachment(registration.value.attachments, currentGuest.value.id)
})
const normalizedGuideLink = computed(() => normalizeExternalLink(registration.value?.checkInGuideLink))

const requiredMessage = (field: string) => {
  return t('required', { field })
}

const statusLabel = (status?: RegistrationFormStatus) => {
  if (!status) {
    return '-'
  }

  return t(status.toLowerCase() as 'draft' | 'submitted' | 'approved' | 'rejected')
}

const applyRegistration = (response: PublicRegistrationResponse) => {
  registration.value = response
  guestForms.value = response.guests.map((guest) => ({
    ...guest,
    residenceType: guest.residenceType || 'JAPAN',
  }))

  if (response.status === 'SUBMITTED' || response.status === 'APPROVED') {
    activeStepIndex.value = guestForms.value.length
    return
  }

  if (activeStepIndex.value >= guestForms.value.length) {
    activeStepIndex.value = Math.max(guestForms.value.length - 1, 0)
  }
}

const loadRegistration = async () => {
  if (!orderNumber.value || !token.value) {
    errorMessage.value = t('missingParams')
    return
  }

  loading.value = true
  errorMessage.value = ''

  try {
    const response = await getPublicRegistration(orderNumber.value, token.value)
    if (!response.success || !response.data) {
      errorMessage.value = t(resolvePublicRegistrationErrorKey(response.message))
      return
    }

    applyRegistration(response.data)
  } catch (error) {
    errorMessage.value = t(
      resolvePublicRegistrationErrorKey(error instanceof Error ? error.message : null),
    )
  } finally {
    loading.value = false
  }
}

const validateGuest = (guest: PublicRegistrationGuest) => {
  const errors: string[] = []

  if (!String(guest.firstName || '').trim()) {
    errors.push(requiredMessage(t('firstName')))
  }

  if (!String(guest.lastName || '').trim()) {
    errors.push(requiredMessage(t('lastName')))
  }

  if (!String(guest.phone || '').trim()) {
    errors.push(requiredMessage(t('phone')))
  }

  if (!String(guest.birthday || '').trim()) {
    errors.push(requiredMessage(t('birthday')))
  }

  if (guest.residenceType === 'OTHER') {
    if (!String(guest.passportNumber || '').trim()) {
      errors.push(requiredMessage(t('passportNumber')))
    }

    if (!String(guest.nationality || '').trim()) {
      errors.push(requiredMessage(t('nationality')))
    }

    if (!String(guest.address1 || '').trim()) {
      errors.push(requiredMessage(t('address1')))
    }

    if (!String(guest.city || '').trim()) {
      errors.push(requiredMessage(t('city')))
    }

    if (!String(guest.country || '').trim()) {
      errors.push(requiredMessage(t('country')))
    }

    if (!findLatestPassportAttachment(registration.value?.attachments, guest.id)) {
      errors.push(requiredMessage(t('passportPhoto')))
    }
  } else if (!String(guest.address || '').trim()) {
    errors.push(requiredMessage(t('address')))
  }

  return errors
}

const validateCurrentGuest = () => {
  if (!currentGuest.value) {
    return []
  }

  return validateGuest(currentGuest.value)
}

const buildSavePayload = () => {
  return {
    guestCount: registration.value?.guestCount || guestForms.value.length,
    guests: guestForms.value.map((guest, index) => ({
      ...guest,
      sortOrder: index + 1,
    })),
  }
}

const handleSaveDraft = async () => {
  if (!registration.value) {
    return
  }

  saving.value = true

  try {
    const response = await savePublicRegistration(orderNumber.value, token.value, buildSavePayload())
    if (!response.success || !response.data) {
      showWarningToast(t(resolvePublicRegistrationErrorKey(response.message, 'saveFailed')))
      return
    }

    applyRegistration(response.data)
    showSuccessToast(t('draftSaved'))
  } catch (error) {
    if (!isHandledPublicRequestError(error)) {
      showWarningToast(
        t(
          resolvePublicRegistrationErrorKey(
            error instanceof Error ? error.message : null,
            'saveFailed',
          ),
        ),
      )
    }
  } finally {
    saving.value = false
  }
}

const handleNextStep = () => {
  const validationErrors = validateCurrentGuest()
  if (validationErrors.length > 0) {
    showWarningToast(validationErrors[0])
    return
  }

  const nextStepIndex = Math.min(activeStepIndex.value + 1, guestForms.value.length)
  activeStepIndex.value = nextStepIndex
}

const handlePreviousStep = () => {
  activeStepIndex.value = Math.max(activeStepIndex.value - 1, 0)
}

const handleSelectStep = (index: number) => {
  activeStepIndex.value = index
}

const handleSubmit = async () => {
  if (!registration.value) {
    return
  }

  for (let index = 0; index < guestForms.value.length; index += 1) {
    const validationErrors = validateGuest(guestForms.value[index])
    if (validationErrors.length > 0) {
      activeStepIndex.value = index
      showWarningToast(validationErrors[0])
      return
    }
  }

  submitting.value = true

  try {
    const saveResponse = await savePublicRegistration(orderNumber.value, token.value, buildSavePayload())
    if (!saveResponse.success || !saveResponse.data) {
      showWarningToast(t(resolvePublicRegistrationErrorKey(saveResponse.message, 'saveFailed')))
      return
    }

    const submitResponse = await submitPublicRegistration(orderNumber.value, token.value)
    if (!submitResponse.success || !submitResponse.data) {
      showWarningToast(t(resolvePublicRegistrationErrorKey(submitResponse.message, 'submitFailed')))
      return
    }

    applyRegistration(submitResponse.data)
    showSuccessToast(t('submittedSuccess'))
  } catch (error) {
    if (!isHandledPublicRequestError(error)) {
      showWarningToast(
        t(
          resolvePublicRegistrationErrorKey(
            error instanceof Error ? error.message : null,
            'submitFailed',
          ),
        ),
      )
    }
  } finally {
    submitting.value = false
  }
}

const handlePassportFileChange = async (event: Event) => {
  const inputElement = event.target as HTMLInputElement
  const selectedFile = inputElement.files?.[0]
  if (!selectedFile || !currentGuest.value?.id) {
    return
  }

  try {
    const compressedFile = await compressImageIfNeeded(selectedFile)
    const response = await uploadPublicRegistrationPassport(
      orderNumber.value,
      token.value,
      currentGuest.value.id,
      compressedFile,
    )

    if (!response.success || !response.data || !registration.value) {
      showWarningToast(
        t(resolvePublicRegistrationErrorKey(response.message, 'passportUploadFailed')),
      )
      return
    }

    const nextAttachments = Array.isArray(registration.value.attachments)
      ? [...registration.value.attachments]
      : []

    const filteredAttachments = nextAttachments.filter((attachment) => {
      if (attachment.type !== 'PASSPORT') {
        return true
      }

      return attachment.guestId !== currentGuest.value?.id
    })

    filteredAttachments.push(response.data)
    registration.value = {
      ...registration.value,
      attachments: filteredAttachments,
    }

    showSuccessToast(t('passportUploaded'))
  } catch (error) {
    if (!isHandledPublicRequestError(error)) {
      showWarningToast(
        t(
          resolvePublicRegistrationErrorKey(
            error instanceof Error ? error.message : null,
            'passportUploadFailed',
          ),
        ),
      )
    }
  } finally {
    inputElement.value = ''
  }
}

const buildAttachmentPreviewUrl = (attachmentId: number) => {
  return buildPublicRegistrationAttachmentUrl(orderNumber.value, attachmentId, token.value, true)
}

const handleOpenPassportPreview = () => {
  if (!activeGuestAttachment.value) {
    return
  }

  passportPreviewUrl.value = buildAttachmentPreviewUrl(activeGuestAttachment.value.id)
  passportPreviewOpen.value = true
}

const handleClosePassportPreview = () => {
  passportPreviewOpen.value = false
  passportPreviewUrl.value = ''
}

const handleDownloadPassport = async () => {
  if (!activeGuestAttachment.value) {
    return
  }

  try {
    const blob = await downloadPublicRegistrationAttachment(
      orderNumber.value,
      activeGuestAttachment.value.id,
      token.value,
    )

    const fileName = activeGuestAttachment.value.originalName || `passport-${activeGuestAttachment.value.id}.jpg`
    downloadBlobFile(blob, fileName)
  } catch {
    return
  }
}

const handleChangeLanguage = (language: PublicRegistrationLanguage) => {
  setLocale(language)
  void router.replace({
    query: {
      ...route.query,
      lang: language,
    },
  })
}

const handleOpenGuide = () => {
  if (!normalizedGuideLink.value) {
    return
  }

  window.open(normalizedGuideLink.value, '_blank', 'noopener,noreferrer')
}

void loadRegistration()
</script>

<style scoped>
.public-form-page {
  display: block;
}

.public-form-hero {
  margin-top: 4px;
}

.public-form-hero__top {
  display: flex;
  justify-content: space-between;
  gap: 12px;
}

.public-form-hero__eyebrow {
  color: var(--ion-color-primary);
  font-weight: 700;
}

.public-form-summary {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 14px;
}

.public-language-switcher {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  justify-content: flex-end;
}

.public-language-chip {
  min-width: 46px;
  padding: 8px 12px;
  border: 0;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.72);
  color: var(--app-heading);
  font-size: 12px;
  font-weight: 700;
}

.public-language-chip--active {
  background: var(--ion-color-primary);
  color: #ffffff;
}

.public-state-card {
  display: grid;
  justify-items: center;
  gap: 12px;
  text-align: center;
}

.public-state-card--error {
  align-items: center;
}

.public-info-grid,
.public-field-grid,
.public-review-grid {
  display: grid;
  gap: 12px;
}

.public-info-grid,
.public-review-grid {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.public-field-grid--half {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.public-info-item {
  display: grid;
  gap: 6px;
  padding: 14px;
  border-radius: 18px;
  background: rgba(16, 35, 63, 0.04);
}

.public-info-item__label {
  color: var(--app-muted);
  font-size: 12px;
}

.public-step-list {
  display: flex;
  gap: 8px;
  overflow-x: auto;
}

.public-step-button {
  flex: 0 0 auto;
  min-height: 38px;
  padding: 0 14px;
  border: 0;
  border-radius: 999px;
  background: var(--app-primary-soft);
  color: var(--ion-color-primary);
  font: inherit;
  font-weight: 600;
}

.public-step-button--active {
  background: var(--ion-color-primary);
  color: #ffffff;
}

.public-guest-card,
.public-review-card {
  display: grid;
  gap: 14px;
}

.public-guest-card__header,
.public-review-card__header {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: flex-start;
}

.public-field {
  display: grid;
  gap: 8px;
}

.public-field__label {
  color: var(--app-heading);
  font-size: 13px;
  font-weight: 600;
}

.public-input {
  width: 100%;
  min-height: 48px;
  padding: 0 14px;
  border: 1px solid var(--app-border);
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.86);
  color: var(--app-heading);
  font: inherit;
}

.public-input--textarea {
  min-height: 108px;
  padding: 14px;
  resize: vertical;
}

.public-input--file {
  padding-top: 12px;
}

.public-passport-card {
  display: grid;
  gap: 10px;
  padding: 12px;
  border-radius: 18px;
  background: rgba(16, 35, 63, 0.04);
}

.public-passport-card__image {
  width: 100%;
  max-height: 240px;
  border-radius: 14px;
  object-fit: contain;
  background: var(--app-surface);
}

.public-passport-card__actions,
.public-action-row {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.public-review-guest,
.public-guide-card {
  display: grid;
  gap: 12px;
  padding: 14px;
  border-radius: 18px;
  background: rgba(16, 35, 63, 0.04);
}

.public-review-guest h3,
.public-guide-card h3 {
  margin: 0;
  color: var(--app-heading);
}

.public-passport-modal__body {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100%;
  padding: 16px;
}

.public-passport-modal__body img {
  max-width: 100%;
  max-height: 100%;
  object-fit: contain;
}

@media (max-width: 768px) {
  .public-form-hero__top,
  .public-guest-card__header,
  .public-review-card__header {
    flex-direction: column;
  }

  .public-language-switcher {
    justify-content: flex-start;
  }

  .public-info-grid,
  .public-review-grid,
  .public-field-grid--half {
    grid-template-columns: 1fr;
  }
}
</style>
