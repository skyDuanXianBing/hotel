<template>
  <ion-page>
    <ion-header translucent>
      <ion-toolbar class="app-page-header__toolbar">
        <ion-title class="app-page-header__title">公开入住登记</ion-title>
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
                  alt="passport"
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
            <img v-if="passportPreviewUrl" :src="passportPreviewUrl" alt="passport preview" />
          </div>
        </ion-content>
      </ion-modal>
    </ion-content>
  </ion-page>
</template>

<script setup lang="ts">
import { IonButton, IonChip, IonContent, IonHeader, IonLabel, IonModal, IonPage, IonSpinner, IonTitle, IonToolbar } from '@ionic/vue'
import { computed, ref } from 'vue'
import { useRoute } from 'vue-router'
import {
  buildPublicRegistrationAttachmentUrl,
  downloadPublicRegistrationAttachment,
  getPublicRegistration,
  savePublicRegistration,
  submitPublicRegistration,
  uploadPublicRegistrationPassport,
} from '@/api/publicRegistration'
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
  formatPublicDate,
  formatPublicDateTime,
  normalizeExternalLink,
  resolvePublicRegistrationLanguage,
  resolveRegistrationStatusColor,
  writePublicRegistrationLanguage,
} from '@/utils/publicRegistration'
import { showSuccessToast, showWarningToast } from '@/utils/notify'

const route = useRoute()

const languageOptions: Array<{ value: PublicRegistrationLanguage; shortLabel: string }> = [
  { value: 'en', shortLabel: 'EN' },
  { value: 'ja', shortLabel: 'JA' },
  { value: 'zh', shortLabel: '中文' },
  { value: 'ko', shortLabel: 'KO' },
]

const translations = {
  en: {
    heroEyebrow: 'Guest Form',
    title: 'Registration Form',
    subtitle: 'Fill in each guest information and submit for review.',
    bookingNumber: 'Booking',
    stay: 'Stay',
    status: 'Status',
    loading: 'Loading form...',
    loadFailed: 'Failed to load form',
    retry: 'Retry',
    guestName: 'Guest',
    guestCount: 'Guests',
    lastSavedAt: 'Last saved',
    guest: 'Guest',
    guestSubtitle: 'Please complete the required fields carefully.',
    residenceType: 'Residence',
    residenceJapan: 'Japan',
    residenceOther: 'Other than Japan',
    birthday: 'Birthday',
    firstName: 'First name',
    lastName: 'Last name',
    phone: 'Phone',
    address: 'Address',
    passportNumber: 'Passport number',
    nationality: 'Nationality',
    address1: 'Address 1',
    address2: 'Address 2',
    city: 'City',
    state: 'State',
    country: 'Country',
    priorStay: 'Previous location',
    nextDestination: 'Next destination',
    passportPhoto: 'Passport photo',
    preview: 'Preview',
    download: 'Download',
    previous: 'Previous',
    saveDraft: 'Save draft',
    next: 'Next',
    reviewAndSubmit: 'Review & Submit',
    reviewNotice: 'Please confirm all guest data before sending.',
    submit: 'Submit',
    approvedTitle: 'Approved',
    guideNotice: 'The check-in guide is ready.',
    openGuide: 'Open guide',
    draft: 'Draft',
    submitted: 'Submitted',
    approved: 'Approved',
    rejected: 'Rejected',
  },
  ja: {
    heroEyebrow: 'Guest Form',
    title: '宿泊者名簿',
    subtitle: '各宿泊者情報を入力し、確認後に送信してください。',
    bookingNumber: '予約番号',
    stay: '宿泊期間',
    status: '状態',
    loading: 'フォームを読み込み中...',
    loadFailed: 'フォームの読み込みに失敗しました',
    retry: '再試行',
    guestName: '宿泊者',
    guestCount: '宿泊人数',
    lastSavedAt: '最終保存',
    guest: '宿泊者',
    guestSubtitle: '必須項目を確認しながら入力してください。',
    residenceType: '居住地',
    residenceJapan: '日本',
    residenceOther: '海外',
    birthday: '生年月日',
    firstName: '名',
    lastName: '姓',
    phone: '電話番号',
    address: '住所',
    passportNumber: '旅券番号',
    nationality: '国籍',
    address1: '住所1',
    address2: '住所2',
    city: '市区町村',
    state: '都道府県',
    country: '国',
    priorStay: '前泊地',
    nextDestination: '行先',
    passportPhoto: '旅券画像',
    preview: 'プレビュー',
    download: '保存',
    previous: '戻る',
    saveDraft: '下書き保存',
    next: '次へ',
    reviewAndSubmit: '確認して送信',
    reviewNotice: '送信前に宿泊者情報をご確認ください。',
    submit: '送信',
    approvedTitle: '承認済み',
    guideNotice: 'チェックインガイドを確認できます。',
    openGuide: 'ガイドを開く',
    draft: '未提出',
    submitted: '提出済み',
    approved: '承認済み',
    rejected: '要再提出',
  },
  zh: {
    heroEyebrow: '住客登记表',
    title: '入住登记表',
    subtitle: '请逐位填写入住人信息，并在确认后提交审查。',
    bookingNumber: '预订号',
    stay: '入住期间',
    status: '状态',
    loading: '正在加载登记表...',
    loadFailed: '登记表加载失败',
    retry: '重试',
    guestName: '客人',
    guestCount: '入住人数',
    lastSavedAt: '最后保存',
    guest: '入住人',
    guestSubtitle: '请认真填写必填信息。',
    residenceType: '居住地',
    residenceJapan: '日本',
    residenceOther: '海外',
    birthday: '出生日期',
    firstName: '名',
    lastName: '姓',
    phone: '电话',
    address: '住址',
    passportNumber: '护照号',
    nationality: '国籍',
    address1: '地址1',
    address2: '地址2',
    city: '城市',
    state: '州',
    country: '国家',
    priorStay: '前泊地',
    nextDestination: '行先',
    passportPhoto: '护照照片',
    preview: '预览',
    download: '下载',
    previous: '上一步',
    saveDraft: '保存草稿',
    next: '下一步',
    reviewAndSubmit: '确认并提交',
    reviewNotice: '提交前请再次确认所有入住人信息。',
    submit: '提交',
    approvedTitle: '已通过审查',
    guideNotice: '现在可以查看入住指南。',
    openGuide: '打开指南',
    draft: '未提交',
    submitted: '已提交',
    approved: '已通过',
    rejected: '需重填',
  },
  ko: {
    heroEyebrow: 'Guest Form',
    title: '투숙자 등록 폼',
    subtitle: '각 투숙객 정보를 입력하고 확인 후 제출하세요.',
    bookingNumber: '예약 번호',
    stay: '숙박 기간',
    status: '상태',
    loading: '폼을 불러오는 중...',
    loadFailed: '폼을 불러오지 못했습니다',
    retry: '다시 시도',
    guestName: '투숙객',
    guestCount: '숙박 인원',
    lastSavedAt: '마지막 저장',
    guest: '투숙객',
    guestSubtitle: '필수 정보를 정확히 입력해 주세요.',
    residenceType: '거주지',
    residenceJapan: '일본',
    residenceOther: '해외',
    birthday: '생년월일',
    firstName: '이름',
    lastName: '성',
    phone: '전화번호',
    address: '주소',
    passportNumber: '여권번호',
    nationality: '국적',
    address1: '주소1',
    address2: '주소2',
    city: '도시',
    state: '주',
    country: '국가',
    priorStay: '이전 숙박지',
    nextDestination: '다음 목적지',
    passportPhoto: '여권 사진',
    preview: '미리보기',
    download: '다운로드',
    previous: '이전',
    saveDraft: '임시 저장',
    next: '다음',
    reviewAndSubmit: '확인 후 제출',
    reviewNotice: '제출 전에 투숙객 정보를 다시 확인하세요.',
    submit: '제출',
    approvedTitle: '승인됨',
    guideNotice: '체크인 가이드를 확인할 수 있습니다.',
    openGuide: '가이드 열기',
    draft: '미제출',
    submitted: '제출됨',
    approved: '승인됨',
    rejected: '재작성 필요',
  },
}

const selectedLanguage = ref<PublicRegistrationLanguage>(resolvePublicRegistrationLanguage(route.query.lang))
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

const t = (key: keyof (typeof translations)['zh']) => {
  return translations[selectedLanguage.value][key]
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
    errorMessage.value = '缺少 orderNumber 或 t 参数'
    return
  }

  loading.value = true
  errorMessage.value = ''

  try {
    const response = await getPublicRegistration(orderNumber.value, token.value)
    if (!response.success || !response.data) {
      errorMessage.value = response.message || t('loadFailed')
      return
    }

    applyRegistration(response.data)
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : t('loadFailed')
  } finally {
    loading.value = false
  }
}

const validateGuest = (guest: PublicRegistrationGuest) => {
  const errors: string[] = []

  if (!String(guest.firstName || '').trim()) {
    errors.push(`${t('firstName')}不能为空`)
  }

  if (!String(guest.lastName || '').trim()) {
    errors.push(`${t('lastName')}不能为空`)
  }

  if (!String(guest.phone || '').trim()) {
    errors.push(`${t('phone')}不能为空`)
  }

  if (!String(guest.birthday || '').trim()) {
    errors.push(`${t('birthday')}不能为空`)
  }

  if (guest.residenceType === 'OTHER') {
    if (!String(guest.passportNumber || '').trim()) {
      errors.push(`${t('passportNumber')}不能为空`)
    }

    if (!String(guest.nationality || '').trim()) {
      errors.push(`${t('nationality')}不能为空`)
    }

    if (!String(guest.address1 || '').trim()) {
      errors.push(`${t('address1')}不能为空`)
    }

    if (!String(guest.city || '').trim()) {
      errors.push(`${t('city')}不能为空`)
    }

    if (!String(guest.country || '').trim()) {
      errors.push(`${t('country')}不能为空`)
    }

    if (!findLatestPassportAttachment(registration.value?.attachments, guest.id)) {
      errors.push(`${t('passportPhoto')}不能为空`)
    }
  } else if (!String(guest.address || '').trim()) {
    errors.push(`${t('address')}不能为空`)
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
      showWarningToast(response.message || '保存失败')
      return
    }

    applyRegistration(response.data)
    showSuccessToast('草稿已保存')
  } catch {
    return
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
      showWarningToast(saveResponse.message || '保存失败')
      return
    }

    const submitResponse = await submitPublicRegistration(orderNumber.value, token.value)
    if (!submitResponse.success || !submitResponse.data) {
      showWarningToast(submitResponse.message || '提交失败')
      return
    }

    applyRegistration(submitResponse.data)
    showSuccessToast('登记表已提交')
  } catch {
    return
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
      showWarningToast(response.message || '护照上传失败')
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

    showSuccessToast('护照上传成功')
  } catch {
    return
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
  selectedLanguage.value = language
  writePublicRegistrationLanguage(language)
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
