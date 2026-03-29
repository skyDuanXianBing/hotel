<template>
  <ion-page>
    <ion-header translucent>
      <ion-toolbar>
        <ion-buttons slot="start">
          <ion-back-button :default-href="ROUTE_PATHS.login" />
        </ion-buttons>
        <ion-title class="mobile-toolbar-title">忘记密码</ion-title>
      </ion-toolbar>
    </ion-header>

    <ion-content fullscreen class="mobile-page auth-page">
      <section class="mobile-hero auth-hero">
        <p class="mobile-note auth-hero__eyebrow">THE HOST HUB</p>
        <h1 class="mobile-title">重置您的密码</h1>
        <p class="mobile-subtitle">
          请输入邮箱、验证码和新密码，完成校验后即可返回登录页重新登录。
        </p>
      </section>

      <div class="mobile-stack">
        <section class="mobile-card auth-card">
          <div class="auth-card__header">
            <h2 class="mobile-section-title">找回账户访问权限</h2>
            <p class="mobile-note">验证码会发送到您的注册邮箱，请注意查收。</p>
          </div>

          <ion-list lines="none" class="auth-list">
            <ion-item class="auth-item">
              <ion-input
                v-model="form.email"
                autocomplete="email"
                inputmode="email"
                label="邮箱"
                label-placement="stacked"
                placeholder="请输入注册邮箱"
                type="email"
              />
            </ion-item>

            <ion-item class="auth-item">
              <ion-input
                v-model="form.verificationCode"
                autocomplete="one-time-code"
                inputmode="numeric"
                label="验证码"
                label-placement="stacked"
                :maxlength="VERIFICATION_CODE_LENGTH"
                placeholder="请输入 6 位验证码"
                type="text"
              />
            </ion-item>

            <ion-button
              class="auth-code-button"
              expand="block"
              fill="outline"
              :disabled="isSendingCode || countdown > 0"
              @click="handleSendVerificationCode"
            >
              <ion-spinner v-if="isSendingCode" name="crescent" />
              <span v-else>{{ sendCodeButtonLabel }}</span>
            </ion-button>

            <ion-item class="auth-item">
              <ion-input
                v-model="form.newPassword"
                autocomplete="new-password"
                label="新密码"
                label-placement="stacked"
                placeholder="请设置新密码（6-20 位）"
                type="password"
              />
            </ion-item>

            <ion-item class="auth-item">
              <ion-input
                v-model="form.confirmPassword"
                autocomplete="new-password"
                label="确认密码"
                label-placement="stacked"
                placeholder="请再次输入新密码"
                type="password"
              />
            </ion-item>
          </ion-list>

          <ion-button
            expand="block"
            class="auth-submit-button"
            :disabled="submitting"
            @click="handleResetPassword"
          >
            <ion-spinner v-if="submitting" name="crescent" />
            <span v-else>重置密码</span>
          </ion-button>

          <div class="auth-footer-link">
            <span>想起密码了？</span>
            <button class="auth-text-button" type="button" @click="handleGoToLogin">返回登录</button>
          </div>
        </section>
      </div>
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
  IonItem,
  IonList,
  IonPage,
  IonSpinner,
  IonTitle,
  IonToolbar,
} from '@ionic/vue'
import { computed, onBeforeUnmount, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { resetPassword, sendVerificationCode } from '@/api/auth'
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
    return `${countdown.value}s 后可重新发送`
  }

  return '发送重置验证码'
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
  const newPassword = form.newPassword.trim()
  const confirmPassword = form.confirmPassword.trim()

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

const handleGoToLogin = async () => {
  await router.replace(buildLoginRouteLocation())
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

<style scoped>
.auth-page {
  display: block;
}

.auth-hero {
  margin-top: 20px;
  margin-bottom: 24px;
  text-align: center;
}

.auth-hero__eyebrow {
  color: var(--ion-color-primary);
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  font-size: 13px;
  margin-bottom: 8px;
}

.mobile-title {
  font-size: 30px;
  font-weight: 800;
  margin-bottom: 12px;
  color: var(--app-heading);
}

.mobile-subtitle {
  font-size: 15px;
  color: var(--app-muted);
  line-height: 1.6;
}

.auth-card {
  padding: 24px 20px;
  border-radius: 24px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.04);
  background: #ffffff;
  border: 1px solid rgba(0, 0, 0, 0.02);
}

.auth-card__header {
  margin-bottom: 20px;
}

.auth-list {
  margin: 16px 0 8px;
  background: transparent;
}

.auth-item {
  --background: #f8fafc;
  --border-radius: 14px;
  --padding-start: 16px;
  --padding-end: 16px;
  --inner-padding-end: 0;
  margin-bottom: 16px;
  border: 1px solid #e2e8f0;
  border-radius: 14px;
  transition: all 0.2s ease;
}

.auth-item.item-has-focus {
  border-color: var(--ion-color-primary);
}

.auth-code-button {
  margin-top: 8px;
  margin-bottom: 12px;
  --border-radius: 14px;
  min-height: 48px;
}

.auth-submit-button {
  margin-top: 24px;
  min-height: 52px;
  font-weight: 600;
  font-size: 16px;
  --border-radius: 14px;
  --box-shadow: 0 4px 12px rgba(var(--ion-color-primary-rgb), 0.3);
}

.auth-footer-link {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  margin-top: 16px;
  color: var(--app-muted);
  font-size: 14px;
}

.auth-text-button {
  padding: 0;
  border: 0;
  background: transparent;
  color: var(--ion-color-primary);
  font: inherit;
  font-weight: 600;
}

:deep(.auth-item ion-input) {
  --padding-top: 10px;
  --padding-bottom: 10px;
}

:deep(.auth-item .label-text-wrapper) {
  margin-bottom: 8px;
  color: var(--app-heading);
  font-weight: 600;
}
</style>
