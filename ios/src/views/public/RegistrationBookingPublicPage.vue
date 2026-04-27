<template>
  <ion-page>
    <ion-header translucent>
      <ion-toolbar class="app-page-header__toolbar">
        <ion-title class="app-page-header__title">公开登记入口</ion-title>
      </ion-toolbar>
    </ion-header>

    <ion-content fullscreen class="mobile-page public-booking-page">
      <section v-if="!selectedLanguage" class="public-language-screen">
        <div class="public-language-card">
          <p class="mobile-note public-language-eyebrow">Public Registration</p>
          <h1 class="mobile-title public-language-title">Select Language</h1>
          <p class="mobile-subtitle">请选择语言后继续查看预订信息。</p>
          <div class="public-language-grid">
            <button
              v-for="language in languageOptions"
              :key="language.value"
              class="public-language-button"
              type="button"
              @click="handleSelectLanguage(language.value)"
            >
              {{ language.label }}
            </button>
          </div>
        </div>
      </section>

      <template v-else>
        <section class="mobile-hero public-hero">
          <div class="public-hero__top">
            <div>
              <p class="mobile-note public-hero__eyebrow">{{ t('heroEyebrow') }}</p>
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
          <div class="mobile-chip-row public-chip-row">
            <span class="mobile-chip">{{ t('bookingNumber') }}: {{ booking?.bookingKey || '-' }}</span>
            <span class="mobile-chip">{{ t('stay') }}: {{ stayText }}</span>
          </div>
        </section>

        <div class="mobile-stack">
          <section class="mobile-card">
            <h2 class="mobile-section-title">{{ t('importantNotice') }}</h2>
            <p class="mobile-note">{{ t('importantNoticeText') }}</p>
          </section>

          <section v-if="loading" class="mobile-card public-state-card">
            <ion-spinner name="crescent" />
            <p class="mobile-note">{{ t('loading') }}</p>
          </section>

          <section v-else-if="errorMessage" class="mobile-card public-state-card public-state-card--error">
            <h2 class="mobile-section-title">{{ t('loadFailed') }}</h2>
            <p class="mobile-note">{{ errorMessage }}</p>
            <ion-button @click="loadBooking">{{ t('retry') }}</ion-button>
          </section>

          <template v-else-if="booking">
            <section class="mobile-card">
              <h2 class="mobile-section-title">{{ t('bookingOverview') }}</h2>
              <div class="public-info-grid">
                <article class="public-info-item">
                  <span class="public-info-item__label">{{ t('guestName') }}</span>
                  <strong>{{ booking.guestName || '-' }}</strong>
                </article>
                <article class="public-info-item">
                  <span class="public-info-item__label">{{ t('stay') }}</span>
                  <strong>{{ stayText }}</strong>
                </article>
                <article class="public-info-item">
                  <span class="public-info-item__label">{{ t('rooms') }}</span>
                  <strong>{{ booking.rooms.length }}</strong>
                </article>
              </div>
            </section>

            <section
              v-for="room in booking.rooms"
              :key="room.orderNumber"
              class="mobile-card public-room-card"
            >
              <div class="public-room-card__header">
                <div>
                  <h2 class="mobile-section-title public-room-card__title">
                    {{ room.storeName || '-' }} / {{ room.roomNumber || '-' }}
                  </h2>
                  <p class="mobile-note">{{ room.roomTypeName || '-' }}</p>
                </div>
                <ion-chip :color="resolveRegistrationStatusColor(room.status)">
                  <ion-label>{{ t(statusKey(room.status)) }}</ion-label>
                </ion-chip>
              </div>

              <div class="public-info-grid public-info-grid--compact">
                <article class="public-info-item">
                  <span class="public-info-item__label">{{ t('stay') }}</span>
                  <strong>{{ formatPublicDate(room.checkInDate) }} - {{ formatPublicDate(room.checkOutDate) }}</strong>
                </article>
                <article class="public-info-item">
                  <span class="public-info-item__label">{{ t('maxGuests') }}</span>
                  <strong>{{ room.maxGuests }}</strong>
                </article>
                <article class="public-info-item">
                  <span class="public-info-item__label">{{ t('lastSavedAt') }}</span>
                  <strong>{{ formatPublicDateTime(room.lastSavedAt) }}</strong>
                </article>
              </div>

              <label class="public-field-label" :for="`guest-count-${room.orderNumber}`">
                {{ t('guestCount') }}
              </label>
              <select
                :id="`guest-count-${room.orderNumber}`"
                v-model="guestCountByOrder[room.orderNumber]"
                class="public-select"
                :disabled="savingOrderNumber === room.orderNumber"
              >
                <option v-for="count in buildGuestCountOptions(room.maxGuests)" :key="count" :value="count">
                  {{ count }}
                </option>
              </select>

              <ion-button
                expand="block"
                class="public-room-card__button"
                :disabled="savingOrderNumber === room.orderNumber"
                @click="handleContinue(room.orderNumber)"
              >
                <ion-spinner v-if="savingOrderNumber === room.orderNumber" name="crescent" />
                <span v-else>{{ t('continue') }}</span>
              </ion-button>
            </section>
          </template>
        </div>
      </template>
    </ion-content>
  </ion-page>
</template>

<script setup lang="ts">
import { IonButton, IonChip, IonContent, IonHeader, IonLabel, IonPage, IonSpinner, IonTitle, IonToolbar } from '@ionic/vue'
import { computed, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getPublicRegistrationBooking, setPublicRegistrationRoomGuestCount } from '@/api/publicRegistrationBooking'
import { buildPublicRegistrationFormPath } from '@/router/guards'
import type {
  PublicRegistrationBookingResponse,
  PublicRegistrationLanguage,
  RegistrationFormStatus,
} from '@/types/publicRegistration'
import { resolveRegistrationStatusColor, formatPublicDate, formatPublicDateTime, readPublicRegistrationLanguage, writePublicRegistrationLanguage } from '@/utils/publicRegistration'
import { showWarningToast } from '@/utils/notify'

const route = useRoute()
const router = useRouter()

const languageOptions: Array<{ value: PublicRegistrationLanguage; label: string; shortLabel: string }> = [
  { value: 'en', label: 'English', shortLabel: 'EN' },
  { value: 'ja', label: '日本語', shortLabel: 'JA' },
  { value: 'zh', label: '中文', shortLabel: '中文' },
  { value: 'ko', label: '한국어', shortLabel: 'KO' },
]

const translations = {
  en: {
    heroEyebrow: 'Guest Self Check-in',
    title: 'Guest Registration',
    subtitle: 'Review your booking and confirm guest count before filling the form.',
    bookingNumber: 'Booking',
    stay: 'Stay',
    importantNotice: 'Important Notice',
    importantNoticeText:
      'Under Japanese law, overseas travelers must submit personal information and passport images for all guests.',
    loading: 'Loading booking...',
    loadFailed: 'Failed to load booking',
    retry: 'Retry',
    bookingOverview: 'Booking Overview',
    guestName: 'Guest',
    rooms: 'Rooms',
    maxGuests: 'Max guests',
    lastSavedAt: 'Last saved',
    guestCount: 'Number of guests',
    continue: 'Continue',
    draft: 'Draft',
    submitted: 'Submitted',
    approved: 'Approved',
    rejected: 'Rejected',
  },
  ja: {
    heroEyebrow: 'Guest Self Check-in',
    title: '宿泊者名簿',
    subtitle: '予約内容を確認し、宿泊人数を選択してから入力を進めてください。',
    bookingNumber: '予約番号',
    stay: '宿泊期間',
    importantNotice: '重要なお知らせ',
    importantNoticeText: '日本の法律により、海外からの宿泊者は全員分の個人情報と旅券画像の提出が必要です。',
    loading: '予約情報を読み込み中...',
    loadFailed: '予約情報の取得に失敗しました',
    retry: '再試行',
    bookingOverview: '予約概要',
    guestName: '宿泊者',
    rooms: '部屋数',
    maxGuests: '最大人数',
    lastSavedAt: '最終保存',
    guestCount: '宿泊人数',
    continue: '続ける',
    draft: '未提出',
    submitted: '提出済み',
    approved: '承認済み',
    rejected: '要再提出',
  },
  zh: {
    heroEyebrow: '住客自助登记',
    title: '入住登记',
    subtitle: '请先确认预订信息与入住人数，再进入房间登记表。',
    bookingNumber: '预订号',
    stay: '入住期间',
    importantNotice: '重要提示',
    importantNoticeText: '根据日本法律，海外旅客需提交所有入住人的个人信息及护照照片。',
    loading: '正在加载预订信息...',
    loadFailed: '预订信息加载失败',
    retry: '重试',
    bookingOverview: '预订概览',
    guestName: '客人',
    rooms: '房间数',
    maxGuests: '最多人数',
    lastSavedAt: '最后保存',
    guestCount: '入住人数',
    continue: '继续填写',
    draft: '未提交',
    submitted: '已提交',
    approved: '已通过',
    rejected: '需重填',
  },
  ko: {
    heroEyebrow: 'Guest Self Check-in',
    title: '투숙자 등록',
    subtitle: '예약 정보와 숙박 인원을 확인한 뒤 등록 폼으로 이동하세요.',
    bookingNumber: '예약 번호',
    stay: '숙박 기간',
    importantNotice: '중요 공지',
    importantNoticeText: '일본 법에 따라 해외 투숙객은 모든 투숙객의 개인정보와 여권 이미지를 제출해야 합니다.',
    loading: '예약 정보를 불러오는 중...',
    loadFailed: '예약 정보를 불러오지 못했습니다',
    retry: '다시 시도',
    bookingOverview: '예약 개요',
    guestName: '투숙객',
    rooms: '객실 수',
    maxGuests: '최대 인원',
    lastSavedAt: '마지막 저장',
    guestCount: '숙박 인원',
    continue: '계속하기',
    draft: '미제출',
    submitted: '제출됨',
    approved: '승인됨',
    rejected: '재작성 필요',
  },
}

const booking = ref<PublicRegistrationBookingResponse | null>(null)
const loading = ref(false)
const errorMessage = ref('')
const savingOrderNumber = ref('')
const guestCountByOrder = reactive<Record<string, number>>({})

const initialLanguage = () => {
  const queryLanguage = route.query.lang
  const storedLanguage = readPublicRegistrationLanguage()
  if (typeof queryLanguage === 'string' && ['en', 'ja', 'zh', 'ko'].includes(queryLanguage)) {
    return queryLanguage as PublicRegistrationLanguage
  }

  if (storedLanguage) {
    return storedLanguage as PublicRegistrationLanguage
  }

  return ''
}

const selectedLanguage = ref<PublicRegistrationLanguage | ''>(initialLanguage())

const bookingKey = computed(() => String(route.params.bookingKey || ''))
const token = computed(() => String(route.query.t || ''))
const stayText = computed(() => {
  if (!booking.value) {
    return '-'
  }

  return `${formatPublicDate(booking.value.checkInDate)} - ${formatPublicDate(booking.value.checkOutDate)}`
})

const t = (key: keyof (typeof translations)['zh']) => {
  const language = selectedLanguage.value || 'zh'
  return translations[language][key]
}

const statusKey = (status: RegistrationFormStatus) => {
  return status.toLowerCase() as 'draft' | 'submitted' | 'approved' | 'rejected'
}

const buildGuestCountOptions = (maxGuests: number) => {
  const safeMaxGuests = Math.max(1, Number(maxGuests || 1))
  return Array.from({ length: safeMaxGuests }, (_, index) => index + 1)
}

const applyBooking = (response: PublicRegistrationBookingResponse) => {
  booking.value = response

  response.rooms.forEach((room) => {
    guestCountByOrder[room.orderNumber] = Number(room.guestCount || 1)
  })
}

const loadBooking = async () => {
  if (!selectedLanguage.value) {
    return
  }

  if (!bookingKey.value || !token.value) {
    errorMessage.value = '缺少 bookingKey 或 t 参数'
    return
  }

  loading.value = true
  errorMessage.value = ''

  try {
    const response = await getPublicRegistrationBooking(bookingKey.value, token.value)
    if (!response.success || !response.data) {
      errorMessage.value = response.message || t('loadFailed')
      return
    }

    applyBooking(response.data)
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : t('loadFailed')
  } finally {
    loading.value = false
  }
}

const handleSelectLanguage = async (language: PublicRegistrationLanguage) => {
  selectedLanguage.value = language
  writePublicRegistrationLanguage(language)
  await loadBooking()
}

const handleChangeLanguage = (language: PublicRegistrationLanguage) => {
  selectedLanguage.value = language
  writePublicRegistrationLanguage(language)
}

const handleContinue = async (orderNumber: string) => {
  const guestCount = guestCountByOrder[orderNumber]
  if (!guestCount || guestCount < 1) {
    showWarningToast('请选择入住人数')
    return
  }

  savingOrderNumber.value = orderNumber

  try {
    await setPublicRegistrationRoomGuestCount(bookingKey.value, orderNumber, token.value, guestCount)
    await router.push({
      path: buildPublicRegistrationFormPath(orderNumber),
      query: {
        t: token.value,
        lang: selectedLanguage.value,
      },
    })
  } catch {
    return
  } finally {
    savingOrderNumber.value = ''
  }
}

if (selectedLanguage.value) {
  void loadBooking()
}
</script>

<style scoped>
.public-booking-page {
  display: block;
}

.public-language-screen {
  min-height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.public-language-card {
  width: min(100%, 520px);
  padding: 32px 24px;
  border-radius: 28px;
  background: var(--app-surface-strong);
  border: 1px solid var(--app-border);
  box-shadow: var(--app-shadow);
}

.public-language-eyebrow {
  color: var(--ion-color-primary);
  font-weight: 700;
}

.public-language-title {
  margin-top: 8px;
}

.public-language-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  margin-top: 24px;
}

.public-language-button,
.public-language-chip {
  border: 0;
  cursor: pointer;
}

.public-language-button {
  min-height: 52px;
  border-radius: 18px;
  background: var(--app-primary-soft);
  color: var(--ion-color-primary);
  font-size: 16px;
  font-weight: 600;
}

.public-hero {
  margin-top: 4px;
}

.public-hero__top {
  display: flex;
  justify-content: space-between;
  gap: 12px;
}

.public-hero__eyebrow {
  color: var(--ion-color-primary);
  font-weight: 700;
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

.public-chip-row {
  margin-top: 16px;
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

.public-info-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.public-info-grid--compact {
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

.public-room-card {
  display: grid;
  gap: 14px;
}

.public-room-card__header {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: flex-start;
}

.public-room-card__title {
  margin-bottom: 4px;
}

.public-field-label {
  color: var(--app-heading);
  font-size: 13px;
  font-weight: 600;
}

.public-select {
  width: 100%;
  min-height: 48px;
  padding: 0 14px;
  border-radius: 16px;
  border: 1px solid var(--app-border);
  background: rgba(255, 255, 255, 0.86);
  color: var(--app-heading);
  font: inherit;
}

.public-room-card__button {
  margin-top: 4px;
}

@media (max-width: 768px) {
  .public-language-grid,
  .public-info-grid,
  .public-info-grid--compact {
    grid-template-columns: 1fr;
  }

  .public-hero__top,
  .public-room-card__header {
    flex-direction: column;
  }

  .public-language-switcher {
    justify-content: flex-start;
  }
}
</style>
