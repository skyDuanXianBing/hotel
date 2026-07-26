<template>
  <ion-modal :is-open="isOpen" class="support-contact-modal" @didDismiss="handleDismiss">
    <ion-header translucent class="support-modal-header">
      <ion-toolbar class="support-modal-toolbar">
        <ion-buttons slot="start">
          <ion-button class="support-modal-back" @click="handleDismiss">
            <ion-icon :icon="chevronBackOutline" />
            <span>{{ t('tools.workbench.back') }}</span>
          </ion-button>
        </ion-buttons>
        <ion-title class="support-modal-title">{{ t('home.support.title') }}</ion-title>
      </ion-toolbar>
    </ion-header>

    <ion-content class="mobile-page support-modal-page">
      <div class="support-modal-shell">
        <section class="support-card" aria-labelledby="support-email-title">
          <article class="support-item">
            <div class="support-item__summary">
              <div class="support-item__icon" aria-hidden="true">
                <ion-icon :icon="mailOutline" />
              </div>
              <div class="support-item__body">
                <strong id="support-email-title">{{ t('home.support.email') }}</strong>
                <p>{{ supportContact.email }}</p>
              </div>
            </div>

            <div class="support-item__actions">
              <button type="button" class="support-btn" @click="handleCopyEmail">
                {{ t('home.support.copy') }}
              </button>
              <button type="button" class="support-btn support-btn--primary" @click="handleSendEmail">
                {{ t('home.support.sendEmail') }}
              </button>
            </div>
          </article>
        </section>

        <section class="support-card" aria-labelledby="support-phone-title">
          <article class="support-item">
            <div class="support-item__summary">
              <div class="support-item__icon" aria-hidden="true">
                <ion-icon :icon="callOutline" />
              </div>
              <div class="support-item__body">
                <strong id="support-phone-title">{{ t('home.support.phone') }}</strong>
                <p>{{ supportContact.phone }}</p>
                <span class="support-item__hours">
                  {{ t('home.support.serviceHours', { hours: t('home.support.serviceHoursValue') }) }}
                </span>
              </div>
            </div>

            <div class="support-item__actions">
              <button type="button" class="support-btn" @click="handleCopyPhone">
                {{ t('home.support.copy') }}
              </button>
              <button type="button" class="support-btn support-btn--primary" @click="handleCallPhone">
                {{ t('home.support.call') }}
              </button>
            </div>
          </article>
        </section>
      </div>
    </ion-content>
  </ion-modal>
</template>

<script setup lang="ts">
import {
  IonButton,
  IonButtons,
  IonContent,
  IonHeader,
  IonIcon,
  IonModal,
  IonTitle,
  IonToolbar,
} from '@ionic/vue'
import { callOutline, chevronBackOutline, mailOutline } from 'ionicons/icons'
import { useI18n } from 'vue-i18n'
import { supportContact } from '@/constants/support'
import { copyText } from '@/utils/clipboard'
import { showSuccessToast, showWarningToast } from '@/utils/notify'

interface Props {
  isOpen: boolean
}

defineProps<Props>()

const { t } = useI18n()

const emit = defineEmits<{
  dismiss: []
}>()

const handleDismiss = () => {
  emit('dismiss')
}

const handleCopyEmail = async () => {
  const copied = await copyText(supportContact.email)
  if (copied) {
    showSuccessToast(t('home.support.emailCopied'))
    return
  }

  showWarningToast(t('home.support.emailCopyUnavailable'))
}

const handleCopyPhone = async () => {
  const copied = await copyText(supportContact.phone)
  if (copied) {
    showSuccessToast(t('home.support.phoneCopied'))
    return
  }

  showWarningToast(t('home.support.phoneCopyUnavailable'))
}

const handleSendEmail = () => {
  if (typeof window === 'undefined') {
    return
  }

  window.location.href = `mailto:${supportContact.email}`
}

const handleCallPhone = () => {
  if (typeof window === 'undefined') {
    return
  }

  const phoneLink = supportContact.phone.replace(/\s+/g, '')
  window.location.href = `tel:${phoneLink}`
}
</script>

<style scoped>
:global(ion-modal.support-contact-modal) {
  --width: 100%;
  --height: 100%;
  --border-radius: 0;
}

.support-modal-header {
  box-shadow: none;
}

.support-modal-toolbar {
  --background: rgba(255, 255, 255, 0.94);
  --border-color: transparent;
  --border-width: 0;
  --min-height: 58px;
  --padding-start: 6px;
  --padding-end: 6px;
}

.support-modal-back {
  --color: var(--ios-pms-header-control-color);
  --padding-start: 2px;
  --padding-end: 8px;
  min-width: 74px;
  margin: 0;
  font-size: 15px;
  font-weight: 400;
}

.support-modal-back ion-icon {
  margin-right: 1px;
  font-size: 23px;
}

.support-modal-title {
  color: var(--ios-pms-header-title-color);
  font-size: var(--ios-pms-font-title-xl-size);
  font-weight: 500;
  letter-spacing: 0;
}

.support-modal-page {
  --background: var(--ios-pms-dashboard-page-background);
  --padding-top: 10px;
  --padding-bottom: calc(28px + var(--app-safe-bottom));
  --padding-start: 16px;
  --padding-end: 16px;
  background: var(--ios-pms-dashboard-page-background);
}

.support-modal-shell {
  display: grid;
  gap: var(--ios-pms-space-3);
}

.support-card {
  overflow: hidden;
  border: 1px solid var(--ios-pms-dashboard-card-border);
  border-radius: var(--ios-pms-radius-card);
  background: var(--app-surface-strong);
  box-shadow: var(--ios-pms-dashboard-card-shadow);
}

.support-item {
  display: grid;
  gap: var(--ios-pms-space-3);
  padding: 16px;
}

.support-item__summary {
  display: grid;
  grid-template-columns: 48px minmax(0, 1fr);
  align-items: center;
  gap: var(--ios-pms-space-3);
}

.support-item__icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 48px;
  height: 48px;
  border: 1px solid var(--ios-pms-border-soft);
  border-radius: var(--ios-pms-radius-icon);
  background: rgba(238, 242, 248, 0.92);
  color: var(--ios-pms-text-soft);
  font-size: 25px;
}

.support-item__body {
  min-width: 0;
}

.support-item__body strong {
  display: block;
  color: var(--ios-pms-text-primary);
  font-size: var(--ios-pms-font-title-md-size);
  font-weight: var(--ios-pms-weight-medium);
  line-height: 1.25;
}

.support-item__body p {
  margin: 4px 0 0;
  overflow: hidden;
  color: var(--ios-pms-text-muted);
  font-size: 14px;
  font-weight: 400;
  line-height: 1.35;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.support-item__hours {
  display: block;
  margin-top: 3px;
  overflow: hidden;
  color: var(--ios-pms-text-soft);
  font-size: var(--ios-pms-font-note-size);
  line-height: 1.35;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.support-item__actions {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 36px;
}

.support-btn {
  box-sizing: border-box;
  min-width: 0;
  min-height: 34px;
  padding: 3px 12px;
  border: 1px solid rgba(183, 194, 213, 0.54);
  border-radius: 7px;
  background: rgba(255, 255, 255, 0.72);
  color: var(--ios-pms-text-muted);
  font: inherit;
  font-size: 14px;
  font-weight: 400;
  white-space: nowrap;
  transition:
    background-color 0.15s ease,
    opacity 0.15s ease;
}

.support-btn--primary {
  border-color: #1890ff;
  background: #1890ff;
  color: #ffffff;
}

.support-btn:active {
  background: var(--app-primary-soft);
}

.support-btn--primary:active {
  background: var(--ion-color-primary-shade);
}

@media (max-width: 374px) {
  .support-modal-page {
    --padding-start: 14px;
    --padding-end: 14px;
  }

  .support-item {
    padding: 14px;
  }

  .support-item__summary {
    grid-template-columns: 44px minmax(0, 1fr);
  }

  .support-item__icon {
    width: 44px;
    height: 44px;
    font-size: 23px;
  }

  .support-item__body strong {
    font-size: 16px;
  }

  .support-item__body p {
    font-size: 13px;
  }

  .support-item__actions {
    gap: 20px;
  }
}

@media (prefers-reduced-motion: reduce) {
  .support-btn {
    transition: none;
  }
}
</style>
