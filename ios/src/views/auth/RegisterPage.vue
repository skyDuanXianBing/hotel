<template>
  <ion-page>
    <ion-header translucent>
      <ion-toolbar>
        <ion-buttons slot="start">
          <ion-back-button :default-href="ROUTE_PATHS.login" />
        </ion-buttons>
        <ion-title class="mobile-toolbar-title">注册</ion-title>
      </ion-toolbar>
    </ion-header>

    <ion-content fullscreen class="mobile-page auth-page">
      <section class="mobile-hero auth-hero">
        <p class="mobile-note auth-hero__eyebrow">THE HOST HUB</p>
        <h1 class="mobile-title">创建您的账户</h1>
        <p class="mobile-subtitle">
          加入 THE HOST HUB，提升运营效率，轻松管理您的门店。
        </p>
      </section>

      <div class="mobile-stack">
        <section class="mobile-card auth-card">
          <div class="auth-card__header">
            <h2 class="mobile-section-title">立即注册</h2>
            <p class="mobile-note">请填写邮箱、验证码和密码，提交前需确认相关协议。</p>
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
                v-model="form.password"
                autocomplete="new-password"
                label="密码"
                label-placement="stacked"
                placeholder="请设置密码（6-20 位）"
                type="password"
              />
            </ion-item>

            <ion-item class="auth-item">
              <ion-input
                v-model="form.confirmPassword"
                autocomplete="new-password"
                label="确认密码"
                label-placement="stacked"
                placeholder="请再次输入密码"
                type="password"
              />
            </ion-item>
          </ion-list>

          <div class="auth-checkbox-row auth-checkbox-row--top">
            <ion-checkbox :checked="form.agreeToTerms" @ionChange="handleAgreeToTermsChange" />
            <span>我已阅读并同意《用户服务协议》《隐私政策》和《会员服务条款》</span>
          </div>

          <ion-button expand="block" class="auth-submit-button" :disabled="submitting" @click="handleRegister">
            <ion-spinner v-if="submitting" name="crescent" />
            <span v-else>提交注册</span>
          </ion-button>

          <div class="auth-footer-link">
            <span>已有账户？</span>
            <button class="auth-text-button" type="button" @click="handleGoToLogin">立即登录</button>
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
  IonCheckbox,
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
import { register, sendVerificationCode } from '@/api/auth'
import { ROUTE_PATHS } from '@/router/guards'
import type { RegisterRequest } from '@/types/auth'
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
  password: '',
  confirmPassword: '',
  agreeToTerms: false,
})

let countdownTimer: number | null = null

const sendCodeButtonLabel = computed(() => {
  if (countdown.value > 0) {
    return `${countdown.value}s 后可重新发送`
  }

  return '发送注册验证码'
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

const validateRegisterForm = (): RegisterRequest | null => {
  const email = validateEmail()
  const verificationCode = form.verificationCode.trim()
  const password = form.password.trim()
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

  if (!password) {
    showWarningToast('请输入密码')
    return null
  }

  if (password.length < PASSWORD_MIN_LENGTH) {
    showWarningToast(`密码长度至少为 ${PASSWORD_MIN_LENGTH} 位`)
    return null
  }

  if (password.length > PASSWORD_MAX_LENGTH) {
    showWarningToast(`密码长度不能超过 ${PASSWORD_MAX_LENGTH} 位`)
    return null
  }

  if (!confirmPassword) {
    showWarningToast('请再次输入密码')
    return null
  }

  if (password !== confirmPassword) {
    showWarningToast('两次输入的密码不一致')
    return null
  }

  return {
    email,
    verificationCode,
    password,
  }
}

const handleAgreeToTermsChange = (event: CustomEvent) => {
  form.agreeToTerms = Boolean(event.detail.checked)
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
      type: 'register',
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

const handleRegister = async () => {
  if (submitting.value) {
    return
  }

  if (!form.agreeToTerms) {
    showWarningToast('请先阅读并同意用户服务协议、隐私政策和会员服务条款')
    return
  }

  const payload = validateRegisterForm()
  if (!payload) {
    return
  }

  submitting.value = true

  try {
    const response = await register(payload)

    if (!response.success) {
      showErrorToast(response.message || '注册失败')
      return
    }

    showSuccessToast('注册成功，请登录')
    await router.replace(buildLoginRouteLocation())
  } catch (error) {
    if (!isHandledRequestError(error)) {
      showErrorToast(resolveErrorMessage(error, '注册失败'))
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

.auth-checkbox-row {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  color: var(--app-muted);
  font-size: 13px;
  line-height: 1.5;
}

.auth-checkbox-row--top {
  margin-top: 14px;
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

:deep(.auth-checkbox-row ion-checkbox) {
  margin-top: 1px;
}
</style>
