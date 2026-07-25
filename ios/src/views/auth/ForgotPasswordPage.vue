<template>
  <ion-page>
    <ion-content fullscreen class="auth-page">
      <div class="auth-shell">
        <AuthBrandHeader :back-label="t('auth.forgot.title')" @select-back="handleBackToLogin" />

        <main class="auth-panel auth-panel--forgot">
          <ion-list lines="none" class="auth-list">
            <div class="auth-field">
              <p class="auth-field-label">{{ t('auth.field.email') }}</p>
              <ion-item class="auth-item">
                <ion-input
                  v-model="form.email"
                  autocomplete="email"
                  inputmode="email"
                  :placeholder="t('auth.placeholder.email')"
                  type="email"
                />
              </ion-item>
            </div>

            <div class="auth-field">
              <p class="auth-field-label">{{ t('auth.field.emailCode') }}</p>
              <div class="auth-code-row">
                <ion-item class="auth-item auth-item--code">
                  <ion-input
                    v-model="form.verificationCode"
                    autocomplete="one-time-code"
                    inputmode="numeric"
                    :maxlength="VERIFICATION_CODE_LENGTH"
                    :placeholder="t('auth.placeholder.code')"
                    type="text"
                  />
                </ion-item>

                <ion-button
                  class="auth-code-action"
                  fill="outline"
                  :disabled="isSendingCode || countdown > 0"
                  @click="handleSendVerificationCode"
                >
                  <ion-spinner v-if="isSendingCode" name="crescent" />
                  <span v-else>{{ sendCodeButtonLabel }}</span>
                </ion-button>
              </div>
            </div>

            <div class="auth-field">
              <p class="auth-field-label">{{ t('auth.field.newPassword') }}</p>
              <ion-item class="auth-item">
                <ion-input
                  v-model="form.newPassword"
                  autocomplete="new-password"
                  :placeholder="t('auth.placeholder.newPassword')"
                  type="password"
                />
              </ion-item>
            </div>

            <div class="auth-field auth-field--last">
              <p class="auth-field-label">{{ t('auth.field.confirmPassword') }}</p>
              <ion-item class="auth-item">
                <ion-input
                  v-model="form.confirmPassword"
                  autocomplete="new-password"
                  :placeholder="t('auth.placeholder.confirmNewPassword')"
                  type="password"
                />
              </ion-item>
            </div>
          </ion-list>

          <ion-button
            expand="block"
            class="auth-primary-button"
            :disabled="submitting"
            @click="handleResetPassword"
          >
            <ion-spinner v-if="submitting" name="crescent" />
            <span v-else>{{ t('auth.action.confirm') }}</span>
          </ion-button>
        </main>
      </div>
    </ion-content>
  </ion-page>
</template>

<script setup lang="ts">
import {
  IonButton,
  IonContent,
  IonInput,
  IonItem,
  IonList,
  IonPage,
  IonSpinner,
} from '@ionic/vue'
import { computed, onBeforeUnmount, reactive, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute, useRouter } from 'vue-router'
import { resetPassword, sendVerificationCode } from '@/api/auth'
import AuthBrandHeader from '@/components/auth/AuthBrandHeader.vue'
import { ROUTE_PATHS } from '@/router/guards'
import type { ResetPasswordRequest } from '@/types/auth'
import { showErrorToast, showSuccessToast, showWarningToast } from '@/utils/notify'
import { isHandledRequestError } from '@/utils/request'

const PASSWORD_MIN_LENGTH = 6
const PASSWORD_MAX_LENGTH = 20
const VERIFICATION_CODE_LENGTH = 6
const VERIFICATION_CODE_SECONDS = 60
const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
const { t } = useI18n()

const router = useRouter()
const route = useRoute()

const countdown = ref(0)
const submitting = ref(false)
const isSendingCode = ref(false)
const form = reactive({
  email: '',
  verificationCode: '',
  newPassword: '',
  confirmPassword: '',
})

let countdownTimer: number | null = null

const sendCodeButtonLabel = computed(() => {
  if (countdown.value > 0) {
    return t('auth.action.resendAfter', { seconds: countdown.value })
  }

  return t('auth.action.sendCode')
})

const resolveErrorMessage = (error: unknown, fallbackMessage: string) => {
  if (error instanceof Error && error.message) {
    return error.message
  }

  return fallbackMessage
}

const clearCountdownTimer = () => {
  if (countdownTimer === null) {
    return
  }

  window.clearInterval(countdownTimer)
  countdownTimer = null
}

const startCountdown = () => {
  clearCountdownTimer()
  countdown.value = VERIFICATION_CODE_SECONDS

  countdownTimer = window.setInterval(() => {
    if (countdown.value <= 1) {
      countdown.value = 0
      clearCountdownTimer()
      return
    }

    countdown.value -= 1
  }, 1000)
}

const normalizeEmail = () => form.email.trim()

const applyEmailPrefill = (emailQuery: unknown) => {
  if (typeof emailQuery !== 'string') {
    return
  }

  const email = emailQuery.trim()
  if (!email) {
    return
  }

  form.email = email
}

const buildLoginRouteLocation = () => {
  const email = normalizeEmail()

  if (!email) {
    return { path: ROUTE_PATHS.login }
  }

  return {
    path: ROUTE_PATHS.login,
    query: {
      email,
    },
  }
}

const handleBackToLogin = async () => {
  if (window.history.length > 1) {
    await router.back()
    return
  }

  await router.replace(buildLoginRouteLocation())
}

const validateEmail = () => {
  const email = normalizeEmail()

  if (!email) {
    showWarningToast(t('auth.validation.emailRequired'))
    return ''
  }

  if (!EMAIL_PATTERN.test(email)) {
    showWarningToast(t('auth.validation.emailInvalid'))
    return ''
  }

  return email
}

const validateResetPasswordForm = (): ResetPasswordRequest | null => {
  const email = validateEmail()
  const verificationCode = form.verificationCode.trim()
  const newPassword = form.newPassword
  const confirmPassword = form.confirmPassword

  if (!email) {
    return null
  }

  if (!verificationCode) {
    showWarningToast(t('auth.validation.codeRequired'))
    return null
  }

  if (verificationCode.length !== VERIFICATION_CODE_LENGTH) {
    showWarningToast(t('auth.validation.codeLength', { length: VERIFICATION_CODE_LENGTH }))
    return null
  }

  if (!newPassword) {
    showWarningToast(t('auth.validation.newPasswordRequired'))
    return null
  }

  if (newPassword.length < PASSWORD_MIN_LENGTH) {
    showWarningToast(t('auth.validation.passwordMin', { min: PASSWORD_MIN_LENGTH }))
    return null
  }

  if (newPassword.length > PASSWORD_MAX_LENGTH) {
    showWarningToast(t('auth.validation.passwordMax', { max: PASSWORD_MAX_LENGTH }))
    return null
  }

  if (!confirmPassword) {
    showWarningToast(t('auth.validation.confirmNewPasswordRequired'))
    return null
  }

  if (newPassword !== confirmPassword) {
    showWarningToast(t('auth.validation.passwordMismatch'))
    return null
  }

  return {
    email,
    verificationCode,
    newPassword,
  }
}

const handleSendVerificationCode = async () => {
  if (isSendingCode.value || countdown.value > 0) {
    return
  }

  const email = validateEmail()
  if (!email) {
    return
  }

  isSendingCode.value = true

  try {
    const response = await sendVerificationCode({
      email,
      type: 'reset_password',
    })

    if (!response.success) {
      showErrorToast(response.message || t('auth.message.codeSendFailed'))
      return
    }

    showSuccessToast(t('auth.message.codeSent'))
    startCountdown()
  } catch (error) {
    if (!isHandledRequestError(error)) {
      showErrorToast(resolveErrorMessage(error, t('auth.message.codeSendFailed')))
    }
  } finally {
    isSendingCode.value = false
  }
}

const handleResetPassword = async () => {
  if (submitting.value) {
    return
  }

  const payload = validateResetPasswordForm()
  if (!payload) {
    return
  }

  submitting.value = true

  try {
    const response = await resetPassword(payload)

    if (!response.success) {
      showErrorToast(response.message || t('auth.message.resetFailed'))
      return
    }

    showSuccessToast(t('auth.message.resetSuccess'))
    await router.replace(buildLoginRouteLocation())
  } catch (error) {
    if (!isHandledRequestError(error)) {
      showErrorToast(resolveErrorMessage(error, t('auth.message.resetFailed')))
    }
  } finally {
    submitting.value = false
  }
}

watch(
  () => route.query.email,
  (emailQuery) => {
    applyEmailPrefill(emailQuery)
  },
  {
    immediate: true,
  },
)

onBeforeUnmount(() => {
  clearCountdownTimer()
})
</script>
