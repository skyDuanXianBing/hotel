<template>
  <ion-page>
    <ion-header translucent>
      <ion-toolbar class="app-page-header__toolbar">
        <ion-buttons slot="start">
          <ion-back-button class="app-page-header__back-btn" :default-href="ROUTE_PATHS.settings" />
        </ion-buttons>
        <ion-title class="app-page-header__title">{{ $t('routes.ProfileCenter') }}</ion-title>
      </ion-toolbar>
    </ion-header>

    <ion-content fullscreen class="mobile-page mobile-page--dashboard profile-page">
      <ion-refresher slot="fixed" @ionRefresh="handleRefresh">
        <ion-refresher-content :pulling-text="t('profile.pullToRefresh')" refreshing-spinner="crescent" />
      </ion-refresher>

      <div class="profile-shell mobile-stack">
        <section class="mobile-dashboard-surface profile-hero">
          <div class="profile-hero__content">
            <h1 class="profile-hero__title">{{ greetingText }}</h1>
            <p class="profile-hero__email">{{ userStore.currentUser?.email || t('profile.unavailableEmail') }}</p>
          </div>
        </section>

        <section class="mobile-dashboard-surface profile-form-card">
          <div class="mobile-inline-row profile-form-card__header">
            <h2 class="profile-section-title">{{ t('profile.details') }}</h2>
            <ion-spinner v-if="loading || saving" class="profile-form-card__spinner" name="crescent" />
          </div>

          <div class="profile-form-grid">
            <label class="profile-form-field">
              <span>{{ t('profile.nickname') }}</span>
              <ion-input v-model="form.nickname" :placeholder="t('profile.nicknamePlaceholder')" />
            </label>

            <label class="profile-form-field">
              <span>{{ t('profile.gender') }}</span>
              <ion-select v-model="form.gender" interface="action-sheet">
                <ion-select-option value="male">{{ t('profile.genderMale') }}</ion-select-option>
                <ion-select-option value="female">{{ t('profile.genderFemale') }}</ion-select-option>
                <ion-select-option value="private">{{ t('profile.genderPrivate') }}</ion-select-option>
              </ion-select>
            </label>

            <label class="profile-form-field profile-form-field--full">
              <span>{{ t('profile.avatar') }}</span>
              <ion-input v-model="form.avatar" :placeholder="t('profile.avatarPlaceholder')" />
            </label>
          </div>

          <div class="profile-form-actions">
            <ion-button fill="outline" :disabled="saving" @click="resetForm">{{ t('profile.reset') }}</ion-button>
            <ion-button :disabled="saving" @click="handleSaveProfile">
              {{ saving ? t('profile.saving') : t('profile.save') }}
            </ion-button>
          </div>
        </section>

        <section class="mobile-dashboard-surface profile-security-card">
          <div class="profile-security-card__copy">
            <h2 class="profile-section-title profile-security-card__title">{{ t('profile.security') }}</h2>
            <p class="profile-security-card__note">{{ t('profile.securityDescription') }}</p>
          </div>
          <ion-button fill="outline" class="profile-security-card__action" @click="passwordModalOpen = true">
            {{ t('profile.changePassword') }}
          </ion-button>
        </section>
      </div>

      <ion-modal :is-open="passwordModalOpen" @didDismiss="handleDismissPasswordModal">
        <ion-header>
          <ion-toolbar>
            <ion-title>{{ t('profile.changePassword') }}</ion-title>
            <ion-buttons slot="end">
              <ion-button @click="handleDismissPasswordModal">{{ t('profile.close') }}</ion-button>
            </ion-buttons>
          </ion-toolbar>
        </ion-header>

        <ion-content class="mobile-page profile-modal-page">
          <section class="mobile-card profile-form-card profile-form-card--modal">
            <div class="profile-form-grid">
              <label class="profile-form-field">
                <span>{{ t('profile.currentPassword') }}</span>
                <ion-input
                  v-model="passwordForm.currentPassword"
                  type="password"
                  :placeholder="t('profile.currentPasswordPlaceholder')"
                />
              </label>

              <label class="profile-form-field">
                <span>{{ t('profile.newPassword') }}</span>
                <ion-input
                  v-model="passwordForm.newPassword"
                  type="password"
                  :placeholder="t('profile.newPasswordPlaceholder')"
                />
              </label>

              <label class="profile-form-field">
                <span>{{ t('profile.confirmPassword') }}</span>
                <ion-input
                  v-model="passwordForm.confirmPassword"
                  type="password"
                  :placeholder="t('profile.confirmPasswordPlaceholder')"
                />
              </label>
            </div>

            <div class="profile-form-actions profile-form-actions--modal">
              <ion-button fill="outline" :disabled="changingPassword" @click="handleDismissPasswordModal">
                {{ t('profile.cancel') }}
              </ion-button>
              <ion-button :disabled="changingPassword" @click="handleChangePassword">
                {{ changingPassword ? t('profile.submitting') : t('profile.confirmChange') }}
              </ion-button>
            </div>
          </section>
        </ion-content>
      </ion-modal>
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
  IonModal,
  IonPage,
  IonRefresher,
  IonRefresherContent,
  IonSelect,
  IonSelectOption,
  IonSpinner,
  IonTitle,
  IonToolbar,
  onIonViewWillEnter,
} from '@ionic/vue'
import { computed, onBeforeUnmount, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import { changePassword, updateProfile } from '@/api/auth'
import { ROUTE_PATHS } from '@/router/guards'
import { useUserStore } from '@/stores/user'
import { isHandledRequestError } from '@/utils/request'
import { showSuccessToast, showWarningToast } from '@/utils/notify'

const router = useRouter()
const userStore = useUserStore()
const { t } = useI18n()

const loading = ref(false)
const saving = ref(false)
const changingPassword = ref(false)
const passwordModalOpen = ref(false)
const currentHour = ref(new Date().getHours())

const form = reactive({
  nickname: '',
  gender: 'private' as 'male' | 'female' | 'private',
  avatar: '',
})

const passwordForm = reactive({
  currentPassword: '',
  newPassword: '',
  confirmPassword: '',
})

const displayName = computed(() => {
  if (form.nickname.trim()) {
    return form.nickname.trim()
  }

  if (userStore.currentUser?.email) {
    return userStore.currentUser.email.split('@')[0]
  }

  return t('profile.title')
})

const greetingPeriod = computed(() => {
  if (currentHour.value < 12) {
    return 'morning'
  }

  if (currentHour.value < 18) {
    return 'afternoon'
  }

  return 'evening'
})

const greetingText = computed(() => {
  return t(`profile.greeting.${greetingPeriod.value}`, { name: displayName.value })
})

function refreshCurrentHour() {
  currentHour.value = new Date().getHours()
}

const greetingTimer = window.setInterval(refreshCurrentHour, 60 * 1000)

function normalizeGender(value?: string | null) {
  if (value === 'male' || value === 'female') {
    return value
  }
  return 'private'
}

function resolveWarningMessage(error: unknown, fallbackMessage: string) {
  if (error instanceof Error && error.message) {
    return error.message
  }

  return fallbackMessage
}

function syncForm() {
  form.nickname = userStore.currentUser?.nickname || userStore.currentUser?.email?.split('@')[0] || ''
  form.gender = normalizeGender(userStore.currentUser?.gender)
  form.avatar = userStore.currentUser?.avatar || ''
}

function resetPasswordForm() {
  passwordForm.currentPassword = ''
  passwordForm.newPassword = ''
  passwordForm.confirmPassword = ''
}

async function loadProfile(force = false) {
  loading.value = true
  try {
    await userStore.fetchCurrentUser(force)
    syncForm()
  } catch (error) {
    if (!isHandledRequestError(error)) {
      showWarningToast(resolveWarningMessage(error, t('profile.loadFailed')))
    }
  } finally {
    loading.value = false
  }
}

function resetForm() {
  syncForm()
}

async function handleSaveProfile() {
  if (!form.nickname.trim()) {
    showWarningToast(t('profile.nicknameRequired'))
    return
  }

  saving.value = true
  try {
    const response = await updateProfile({
      nickname: form.nickname.trim(),
      gender: form.gender,
      avatar: form.avatar.trim() || undefined,
    })
    if (!response.success || !response.data) {
      throw new Error(response.message || t('profile.updateFailed'))
    }

    userStore.setUser(response.data)
    syncForm()
    showSuccessToast(t('profile.updated'))
  } catch (error) {
    if (!isHandledRequestError(error)) {
      showWarningToast(resolveWarningMessage(error, t('profile.updateFailed')))
    }
  } finally {
    saving.value = false
  }
}

function handleDismissPasswordModal() {
  passwordModalOpen.value = false
  resetPasswordForm()
}

async function handleChangePassword() {
  if (!passwordForm.currentPassword.trim()) {
    showWarningToast(t('profile.currentPasswordRequired'))
    return
  }

  if (passwordForm.newPassword.trim().length < 6) {
    showWarningToast(t('profile.passwordMin', { min: 6 }))
    return
  }

  if (passwordForm.newPassword !== passwordForm.confirmPassword) {
    showWarningToast(t('profile.passwordMismatch'))
    return
  }

  changingPassword.value = true
  try {
    const response = await changePassword({
      currentPassword: passwordForm.currentPassword,
      newPassword: passwordForm.newPassword,
      confirmPassword: passwordForm.confirmPassword,
    })
    if (!response.success) {
      throw new Error(response.message || t('profile.passwordChangeFailed'))
    }

    showSuccessToast(t('profile.passwordChanged'))
    handleDismissPasswordModal()
    await userStore.logout()
    await router.replace(ROUTE_PATHS.login)
  } catch (error) {
    if (!isHandledRequestError(error)) {
      showWarningToast(resolveWarningMessage(error, t('profile.passwordChangeFailed')))
    }
  } finally {
    changingPassword.value = false
  }
}

async function handleRefresh(event: CustomEvent) {
  try {
    await loadProfile(true)
  } finally {
    event.detail.complete()
  }
}

onIonViewWillEnter(async () => {
  refreshCurrentHour()
  await loadProfile(false)
})

onBeforeUnmount(() => {
  window.clearInterval(greetingTimer)
})
</script>

<style scoped>
.profile-page {
  display: block;
  --background: var(--ios-pms-dashboard-page-background);
  --padding-top: 12px;
  --padding-bottom: calc(20px + var(--app-safe-bottom));
  --padding-start: 14px;
  --padding-end: 14px;
}

.profile-shell {
  gap: 12px;
  padding-bottom: calc(4px + var(--app-safe-bottom));
}

.profile-hero,
.profile-form-card,
.profile-security-card {
  display: grid;
  gap: 12px;
  overflow: hidden;
  border-radius: var(--ios-pms-radius-card-sm);
}

.profile-hero {
  padding: 16px 16px 20px;
}

.profile-hero__content {
  min-width: 0;
}

.profile-hero__title {
  margin: 0;
  color: var(--ios-pms-text-primary);
  font-size: 18px;
  font-weight: var(--ios-pms-weight-medium);
  line-height: 1.25;
  letter-spacing: 0;
}

.profile-hero__email {
  margin: 6px 0 0;
  color: var(--ios-pms-text-secondary);
  font-size: 14px;
  line-height: 1.45;
  word-break: break-word;
}

.profile-form-card {
  padding: 16px 16px 20px;
}

.profile-form-card__header {
  align-items: center;
}

.profile-section-title {
  margin: 0;
  color: var(--ios-pms-text-primary);
  font-size: 17px;
  font-weight: var(--ios-pms-weight-medium);
  line-height: 1.25;
  letter-spacing: -0.02em;
}

.profile-form-card__spinner {
  flex-shrink: 0;
  color: var(--ios-pms-primary);
}

.profile-form-grid {
  display: grid;
  gap: 11px;
}

.profile-form-field {
  display: grid;
  gap: 6px;
}

.profile-form-field span {
  color: var(--ios-pms-text-secondary);
  font-size: 14px;
  font-weight: 400;
  line-height: 1.4;
}

.profile-form-field--full {
  grid-column: 1 / -1;
}

.profile-form-field :deep(ion-input),
.profile-form-field :deep(ion-select) {
  --background: var(--ios-pms-dashboard-card-background);
  --highlight-color-focused: #d8d8dc;
  --highlight-color-valid: #d8d8dc;
  --color: var(--ios-pms-text-primary);
  --placeholder-color: rgba(115, 130, 157, 0.78);
  --placeholder-opacity: 1;
  --border-radius: 11px;
  --padding-start: 12px;
  --padding-end: 12px;
  --padding-top: 0;
  --padding-bottom: 0;
  box-sizing: border-box;
  display: block;
  width: 100%;
  min-height: 44px;
  border: 1px solid #d8d8dc;
  border-radius: 11px;
  background: var(--ios-pms-dashboard-card-background);
  box-shadow: none;
  overflow: visible;
  font-size: 14px;
  font-weight: 400;
}

.profile-form-actions {
  display: grid;
  grid-template-columns: minmax(0, 0.92fr) minmax(0, 1.08fr);
  gap: 12px;
  margin-top: 2px;
}

.profile-form-actions ion-button {
  margin: 0;
  min-height: 30px;
  height: 30px;
  font-size: 13px;
  font-weight: 500;
  --border-radius: 6px;
  --padding-top: 0;
  --padding-bottom: 0;
  --padding-start: 10px;
  --padding-end: 10px;
}

.profile-form-actions ion-button:first-child {
  --background: rgba(255, 255, 255, 0.96);
  --border-color: rgba(193, 204, 220, 0.95);
  --border-width: 1px;
  --box-shadow: none;
  --color: #8a96a8;
}

.profile-form-actions ion-button:last-child {
  --background: linear-gradient(180deg, #3191ff 0%, #2687f7 100%);
  --box-shadow: 0 4px 10px rgba(52, 116, 246, 0.1);
  --color: #ffffff;
}

.profile-form-actions ion-button:last-child::part(native) {
  border: none;
}

.profile-security-card {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  grid-template-areas:
    'title action'
    'note note';
  align-items: start;
  column-gap: 12px;
  row-gap: 8px;
  padding: 16px 16px 20px;
}

.profile-security-card__copy {
  display: contents;
}

.profile-security-card__title {
  grid-area: title;
  margin-bottom: 0;
}

.profile-security-card__note {
  grid-area: note;
  width: 100%;
  margin: 0;
  color: var(--ios-pms-text-muted);
  font-size: 14px;
  line-height: 1.6;
}

.profile-security-card__action {
  grid-area: action;
  justify-self: end;
  align-self: start;
  margin: 0;
  min-height: 30px;
  height: 30px;
  --padding-start: 14px;
  --padding-end: 14px;
  --border-radius: 6px;
  --border-color: rgba(73, 136, 249, 0.28);
  --border-width: 1px;
  --background: rgba(255, 255, 255, 0.96);
  --box-shadow: none;
  --color: var(--ios-pms-primary);
  font-size: 13px;
  font-weight: 500;
}

.profile-modal-page {
  --padding-top: 12px;
  --padding-bottom: 20px;
  --padding-start: 16px;
  --padding-end: 16px;
}

.profile-form-card--modal {
  padding: 20px;
  box-shadow: none;
}

.profile-form-actions--modal {
  margin-top: 0;
}
</style>
