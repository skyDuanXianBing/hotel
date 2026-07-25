<template>
  <ion-page class="registration-review-links-route">
    <ion-header translucent>
      <ion-toolbar class="app-page-header__toolbar">
        <ion-buttons slot="start">
          <ion-button
            fill="clear"
            class="registration-review-links-page__back-button app-page-header__text-btn"
            @click="handleBack"
          >
            {{ $t('roomStatus.cancelReservation.back') }}
          </ion-button>
        </ion-buttons>
        <ion-title class="app-page-header__title">{{ $t('routes.RegistrationReviewLinks') }}</ion-title>
      </ion-toolbar>
    </ion-header>

    <ion-content fullscreen class="mobile-page registration-review-links-page">
      <section class="mobile-hero registration-review-links-page__hero">
        <div class="registration-review-links-page__hero-header">
          <div>
            <h1 class="mobile-title">{{ $t('routes.RegistrationReviewLinks') }}</h1>
          </div>
          <ion-button fill="outline" size="small" class="registration-review-links-page__hero-action" @click="handleReload">
            {{ $t('accommodation.cleaning.refresh') }}
          </ion-button>
        </div>
        <div class="mobile-chip-row registration-review-links-page__hero-metrics">
          <span class="mobile-chip">{{ $t('stage5DynamicUi.95') }} {{ reviewStore.linkEntries.length }} {{ $t('stage5DynamicUi.125') }}</span>
        </div>
      </section>

      <div class="mobile-stack">
        <section class="mobile-card">
          <div class="mobile-inline-row registration-review-links-page__results-header">
            <div>
              <h2 class="mobile-section-title">{{ $t('stage5SourceText.225') }}</h2>
              <p class="mobile-note">{{ $t('stage5DynamicUi.95') }} {{ reviewStore.linkEntries.length }} {{ $t('stage5DynamicUi.128') }}</p>
            </div>
          </div>

          <div
            v-if="reviewStore.isLinkLoading"
            class="registration-review-links-page__state-block registration-review-links-page__state-block--loading"
          >
            <div class="registration-review-links-page__loading-header">
              <ion-spinner name="crescent" />
              <p class="mobile-note">{{ $t('stage5SourceText.158') }}</p>
            </div>
            <div class="registration-review-links-page__skeleton-list" aria-hidden="true">
              <div
                v-for="index in 3"
                :key="index"
                class="registration-review-links-page__skeleton-card"
              >
                <div class="registration-review-links-page__skeleton-line registration-review-links-page__skeleton-line--title"></div>
                <div class="registration-review-links-page__skeleton-grid">
                  <div class="registration-review-links-page__skeleton-line"></div>
                  <div class="registration-review-links-page__skeleton-line"></div>
                  <div class="registration-review-links-page__skeleton-line registration-review-links-page__skeleton-line--wide"></div>
                </div>
                <div class="registration-review-links-page__skeleton-line registration-review-links-page__skeleton-line--box"></div>
                <div class="registration-review-links-page__skeleton-action"></div>
              </div>
            </div>
          </div>

          <div v-else-if="reviewStore.linkLoadError" class="registration-review-links-page__state-block">
            <p class="mobile-note">{{ reviewStore.linkLoadError }}</p>
            <ion-button fill="outline" size="small" @click="handleReload">{{ $t('storeSelection.reload') }}</ion-button>
          </div>

          <div v-else-if="reviewStore.linkEntries.length > 0" class="mobile-list pms-list registration-review-links-page__list">
            <article v-for="entry in reviewStore.linkEntries" :key="entry.id" class="registration-review-links-page__item">
              <div class="mobile-inline-row registration-review-links-page__item-header">
                <div>
                  <strong class="registration-review-links-page__guest-name">{{ entry.guestName }}</strong>
                  <p class="mobile-note">{{ $t('stage5DynamicUi.99') }}{{ entry.createdAt }}</p>
                </div>
                <span class="mobile-chip">{{ entry.roomCount }} {{ $t('settingsStage4.common.unitRooms') }}</span>
              </div>

              <div class="registration-review-links-page__meta-grid">
                <span>{{ $t('roomStatus.action.checkIn') }} {{ entry.checkInDate }}</span>
                <span>{{ $t('roomStatus.hoverCard.checkOutDate') }} {{ entry.checkOutDate }}</span>
                <span>BookingKey {{ entry.bookingKey }}</span>
              </div>

              <div class="registration-review-links-page__link-box">
                {{ entry.linkUrl || $t('stage5DynamicUi.44') }}
              </div>

              <div class="registration-review-links-page__actions">
                <ion-button fill="outline" size="small" :disabled="!entry.linkUrl" @click="handleCopy(entry.linkUrl)">
                  {{ $t('stage5VisibleText.151') }}
                </ion-button>
              </div>
            </article>
          </div>

          <p v-else class="mobile-note">{{ $t('stage5SourceText.90') }}</p>
        </section>
      </div>
    </ion-content>
  </ion-page>
</template>

<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import {
  IonButton,
  IonButtons,
  IonContent,
  IonHeader,
  IonPage,
  IonSpinner,
  IonTitle,
  IonToolbar,
  onIonViewWillEnter,
} from '@ionic/vue'
import { useRouter } from 'vue-router'
import { useReviewStore } from '@/stores/reviews'
import { ROUTE_PATHS } from '@/router/guards'
import { showSuccessToast } from '@/utils/notify'
import { showUnhandledRequestWarning } from '@/utils/requestError'
import { copyTextToClipboard } from '@/utils/file'

const { t } = useI18n()

const router = useRouter()
const reviewStore = useReviewStore()

onIonViewWillEnter(async () => {
  await reviewStore.refreshLinks()
})

async function handleBack() {
  if (window.history.length > 1) {
    await router.back()
    return
  }

  await router.replace(ROUTE_PATHS.reviews)
}

async function handleReload() {
  await reviewStore.refreshLinks()
}

async function handleCopy(linkUrl: string) {
  try {
    await copyTextToClipboard(linkUrl)
    showSuccessToast(t('stage5Pattern.operationCompleted'))
  } catch (error) {
    showUnhandledRequestWarning(error, t('stage5Final.review.copyLinkFailed'))
  }
}
</script>

<style scoped>
.registration-review-links-route {
  background:
    radial-gradient(circle at top right, rgba(37, 99, 235, 0.08), transparent 34%),
    linear-gradient(180deg, #f4f7fc 0%, #eef3fb 100%);
}

.registration-review-links-page {
  --background:
    radial-gradient(circle at top right, rgba(37, 99, 235, 0.08), transparent 34%),
    linear-gradient(180deg, #f4f7fc 0%, #eef3fb 100%);
  background: var(--background);
}

.registration-review-links-page__back-button {
  --color: var(--ios-pms-header-control-color);
  font-weight: var(--ios-pms-weight-bold);
}

.registration-review-links-page__hero {
  display: grid;
  gap: var(--ios-pms-space-2);
  padding-bottom: var(--ios-pms-space-3);
  margin-bottom: var(--ios-pms-space-4);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.99), rgba(249, 251, 255, 0.98));
  box-shadow: 0 10px 24px rgba(77, 98, 145, 0.045);
}

.registration-review-links-page__hero::before {
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.28), rgba(255, 255, 255, 0));
}

.registration-review-links-page__hero-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--ios-pms-space-3);
  min-width: 0;
}

.registration-review-links-page__hero-header > div,
.registration-review-links-page__results-header > div,
.registration-review-links-page__item-header > div {
  min-width: 0;
}

.registration-review-links-page__hero :deep(.mobile-title) {
  color: var(--ios-pms-text-primary);
  margin: 0;
}

.registration-review-links-page__hero-action {
  flex-shrink: 0;
}

.registration-review-links-page__hero-metrics {
  margin-top: 0;
  padding-top: 0;
}

.registration-review-links-page__results-header {
  margin-bottom: var(--ios-pms-space-2);
  min-width: 0;
}

.registration-review-links-page__state-block {
  display: grid;
  justify-items: center;
  gap: var(--ios-pms-space-2);
  padding: 24px 0;
}

.registration-review-links-page__state-block--loading {
  justify-items: stretch;
  gap: var(--ios-pms-space-4);
}

.registration-review-links-page__loading-header {
  display: grid;
  justify-items: center;
  gap: var(--ios-pms-space-2);
}

.registration-review-links-page__skeleton-list {
  display: grid;
  gap: var(--ios-pms-space-3);
}

.registration-review-links-page__skeleton-card {
  display: grid;
  gap: var(--ios-pms-space-3);
  padding: var(--ios-pms-space-4);
  border: 1px solid rgba(164, 181, 216, 0.18);
  border-radius: var(--ios-pms-radius-card-sm);
  background: rgba(255, 255, 255, 0.84);
  box-shadow: 0 10px 24px rgba(77, 98, 145, 0.04);
}

.registration-review-links-page__skeleton-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: var(--ios-pms-space-2) var(--ios-pms-space-3);
}

.registration-review-links-page__skeleton-line,
.registration-review-links-page__skeleton-action {
  border-radius: 999px;
  background: linear-gradient(
    90deg,
    rgba(222, 230, 244, 0.7),
    rgba(244, 247, 252, 0.96),
    rgba(222, 230, 244, 0.7)
  );
  background-size: 200% 100%;
  animation: registration-review-links-page-skeleton 1.15s ease-in-out infinite;
}

.registration-review-links-page__skeleton-line {
  height: 12px;
}

.registration-review-links-page__skeleton-line--title {
  width: min(180px, 68%);
  height: 18px;
}

.registration-review-links-page__skeleton-line--wide {
  grid-column: 1 / -1;
  width: 72%;
}

.registration-review-links-page__skeleton-line--box {
  width: 100%;
  height: 44px;
  border-radius: var(--ios-pms-radius-input);
}

.registration-review-links-page__skeleton-action {
  justify-self: end;
  width: 98px;
  height: 32px;
}

.registration-review-links-page__item {
  padding: var(--ios-pms-space-4) 0;
  border-top: 1px solid var(--ios-pms-divider);
}

.registration-review-links-page__item:first-child {
  border-top: none;
}

.registration-review-links-page__item-header {
  align-items: flex-start;
  min-width: 0;
}

.registration-review-links-page__guest-name {
  display: block;
  color: var(--ios-pms-text-primary);
  font-size: var(--ios-pms-font-title-sm-size);
  font-weight: var(--ios-pms-weight-heavy);
  line-height: 1.3;
}

.registration-review-links-page__meta-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: var(--ios-pms-space-2) var(--ios-pms-space-3);
  margin-top: var(--ios-pms-space-3);
  color: var(--ios-pms-text-soft);
  font-size: var(--ios-pms-font-body-sm-size);
}

.registration-review-links-page__link-box {
  margin-top: var(--ios-pms-space-3);
  padding: 0;
  background: transparent;
  color: var(--ios-pms-text-muted);
  font-size: var(--ios-pms-font-body-sm-size);
  line-height: 1.6;
  word-break: break-all;
}

.registration-review-links-page__actions {
  display: flex;
  justify-content: flex-end;
  margin-top: var(--ios-pms-space-3);
}

.registration-review-links-page__actions :deep(ion-button) {
  flex-shrink: 0;
}

@keyframes registration-review-links-page-skeleton {
  0% {
    background-position: 200% 0;
  }

  100% {
    background-position: -200% 0;
  }
}

@media (max-width: 360px) {
  .registration-review-links-page__hero-header,
  .registration-review-links-page__results-header {
    display: grid;
  }

  .registration-review-links-page__meta-grid,
  .registration-review-links-page__skeleton-grid {
    grid-template-columns: minmax(0, 1fr);
  }
}
</style>
