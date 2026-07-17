<template>
  <ion-page>
    <ion-content fullscreen class="auth-page">
      <div class="auth-shell">
        <AuthBrandHeader back-label="找回密码" @select-back="handleBackToLogin" />

        <main class="auth-panel auth-panel--forgot">
          <ion-list lines="none" class="auth-list">
            <div class="auth-field">
              <p class="auth-field-label">邮箱</p>
              <ion-item class="auth-item">
                <ion-input
                  v-model="form.email"
                  autocomplete="email"
                  inputmode="email"
                  placeholder="请输入邮箱"
                  type="email"
                />
              </ion-item>
            </div>

            <div class="auth-field">
              <p class="auth-field-label">邮箱验证码</p>
              <div class="auth-code-row">
                <ion-item class="auth-item auth-item--code">
                  <ion-input
                    v-model="form.verificationCode"
                    autocomplete="one-time-code"
                    inputmode="numeric"
                    :maxlength="VERIFICATION_CODE_LENGTH"
                    placeholder="请输入验证码"
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
              <p class="auth-field-label">新密码</p>
              <ion-item class="auth-item">
                <ion-input
                  v-model="form.newPassword"
                  autocomplete="new-password"
                  placeholder="请输入 6-20 位新密码"
                  type="password"
                />
              </ion-item>
            </div>

            <div class="auth-field auth-field--last">
              <p class="auth-field-label">确认密码</p>
              <ion-item class="auth-item">
                <ion-input
                  v-model="form.confirmPassword"
                  autocomplete="new-password"
                  placeholder="再次输入新密码"
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
            <span v-else>确认</span>
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
    return `${countdown.value}s 后可重发`
  }

  return '发送验证码'
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
    showWarningToast('请输入邮箱')
    return ''
  }

  if (!EMAIL_PATTERN.test(email)) {
    showWarningToast('请输入正确的邮箱格式')
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
    showWarningToast('请输入验证码')
    return null
  }

  if (verificationCode.length !== VERIFICATION_CODE_LENGTH) {
    showWarningToast(`验证码需为 ${VERIFICATION_CODE_LENGTH} 位`)
    return null
  }

  if (!newPassword) {
    showWarningToast('请输入新密码')
    return null
  }

  if (newPassword.length < PASSWORD_MIN_LENGTH) {
    showWarningToast(`密码长度至少为 ${PASSWORD_MIN_LENGTH} 位`)
    return null
  }

  if (newPassword.length > PASSWORD_MAX_LENGTH) {
    showWarningToast(`密码长度不能超过 ${PASSWORD_MAX_LENGTH} 位`)
    return null
  }

  if (!confirmPassword) {
    showWarningToast('请再次输入新密码')
    return null
  }

  if (newPassword !== confirmPassword) {
    showWarningToast('两次输入的密码不一致')
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
      showErrorToast(response.message || '验证码发送失败')
      return
    }

    showSuccessToast('验证码已发送，请查收邮箱')
    startCountdown()
  } catch (error) {
    if (!isHandledRequestError(error)) {
      showErrorToast(resolveErrorMessage(error, '验证码发送失败'))
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
      showErrorToast(response.message || '密码重置失败')
      return
    }

    showSuccessToast('密码重置成功，请使用新密码登录')
    await router.replace(buildLoginRouteLocation())
  } catch (error) {
    if (!isHandledRequestError(error)) {
      showErrorToast(resolveErrorMessage(error, '密码重置失败'))
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
