<template>
  <ion-page>
    <ion-header translucent>
      <ion-toolbar class="app-page-header__toolbar">
        <ion-title class="app-page-header__title public-page-header__title">
          {{ t('routeTitle') }}
        </ion-title>
      </ion-toolbar>
    </ion-header>

    <ion-content fullscreen class="mobile-page public-booking-page">
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
                @click="handleContinue(room.orderNumber, room.roomRegistrationLink)"
              >
                <ion-spinner v-if="savingOrderNumber === room.orderNumber" name="crescent" />
                <span v-else>{{ t('continue') }}</span>
              </ion-button>
            </section>
          </template>
      </div>
    </ion-content>
  </ion-page>
</template>

<script setup lang="ts">
import { IonButton, IonChip, IonContent, IonHeader, IonLabel, IonPage, IonSpinner, IonTitle, IonToolbar } from '@ionic/vue'
import { computed, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getPublicRegistrationBooking, setPublicRegistrationRoomGuestCount } from '@/api/publicRegistrationBooking'
import { usePublicRegistrationI18n } from '@/composables/usePublicRegistrationI18n'
import { buildPublicRegistrationFormPath } from '@/router/guards'
import type {
  PublicRegistrationBookingResponse,
  PublicRegistrationLanguage,
  RegistrationFormStatus,
} from '@/types/publicRegistration'
import {
  formatPublicDate as formatPublicDateValue,
  formatPublicDateTime as formatPublicDateTimeValue,
  resolvePublicRegistrationErrorKey,
  resolveRegistrationStatusColor,
} from '@/utils/publicRegistration'
import { showWarningToast } from '@/utils/notify'
import { isHandledPublicRequestError } from '@/utils/publicRequest'

const route = useRoute()
const router = useRouter()

const {
  t,
  locale: selectedLanguage,
  languageOptions,
  setLocale,
} = usePublicRegistrationI18n('booking', () => route.query.lang)

const booking = ref<PublicRegistrationBookingResponse | null>(null)
const loading = ref(false)
const errorMessage = ref('')
const savingOrderNumber = ref('')
const guestCountByOrder = reactive<Record<string, number>>({})

const bookingKey = computed(() => String(route.params.bookingKey || ''))
const token = computed(() => String(route.query.t || ''))
const formatPublicDate = (value?: string | null) => {
  return formatPublicDateValue(value, selectedLanguage.value)
}
const formatPublicDateTime = (value?: string | null) => {
  return formatPublicDateTimeValue(value, selectedLanguage.value)
}
const stayText = computed(() => {
  if (!booking.value) {
    return '-'
  }

  return `${formatPublicDate(booking.value.checkInDate)} - ${formatPublicDate(booking.value.checkOutDate)}`
})

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

const buildRegistrationFormLocation = (orderNumber: string, roomRegistrationLink: string) => {
  const language = selectedLanguage.value
  const link = String(roomRegistrationLink || '').trim()
  if (!link) {
    return null
  }

  try {
    const url = new URL(link, window.location.origin)
    url.searchParams.set('lang', language)
    if (!url.searchParams.get('t')) {
      return null
    }

    return {
      path: buildPublicRegistrationFormPath(orderNumber),
      query: Object.fromEntries(url.searchParams.entries()),
    }
  } catch {
    return null
  }
}

const loadBooking = async () => {
  if (!bookingKey.value || !token.value) {
    errorMessage.value = t('missingParams')
    return
  }

  loading.value = true
  errorMessage.value = ''

  try {
    const response = await getPublicRegistrationBooking(bookingKey.value, token.value)
    if (!response.success || !response.data) {
      errorMessage.value = t(resolvePublicRegistrationErrorKey(response.message))
      return
    }

    applyBooking(response.data)
  } catch (error) {
    errorMessage.value = t(
      resolvePublicRegistrationErrorKey(error instanceof Error ? error.message : null),
    )
  } finally {
    loading.value = false
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

const handleContinue = async (orderNumber: string, roomRegistrationLink: string) => {
  const guestCount = guestCountByOrder[orderNumber]
  if (!guestCount || guestCount < 1) {
    showWarningToast(t('selectGuestCount'))
    return
  }

  const formLocation = buildRegistrationFormLocation(orderNumber, roomRegistrationLink)
  if (!formLocation) {
    showWarningToast(t('invalidRegistrationLink'))
    return
  }

  savingOrderNumber.value = orderNumber

  try {
    await setPublicRegistrationRoomGuestCount(bookingKey.value, orderNumber, token.value, guestCount)
    await router.push(formLocation)
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
    savingOrderNumber.value = ''
  }
}

void loadBooking()
</script>

<style scoped>
.public-booking-page {
  display: block;
}

.public-language-chip {
  border: 0;
  cursor: pointer;
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
