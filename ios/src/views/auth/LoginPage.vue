<template>
  <ion-page>
    <ion-content fullscreen class="mobile-page auth-page">
      <section class="mobile-hero auth-hero">
        <p class="mobile-note auth-hero__eyebrow">THE HOST HUB</p>
        <h1 class="mobile-title">酒店业务管理平台</h1>
        <p class="mobile-subtitle">
          提升运营效率，随时随地管理您的门店业务和订单。
        </p>
      </section>

      <div class="mobile-stack">
        <section class="mobile-card auth-card">
          <div class="auth-card__header">
            <h2 class="mobile-section-title">{{ currentModeTitle }}</h2>
            <p class="mobile-note">{{ currentModeDescription }}</p>
          </div>

          <ion-segment :value="loginMode" @ionChange="handleLoginModeChange">
            <ion-segment-button value="password">
              <ion-label>密码登录</ion-label>
            </ion-segment-button>
            <ion-segment-button value="code">
              <ion-label>验证码登录</ion-label>
            </ion-segment-button>
          </ion-segment>

          <ion-list lines="none" class="auth-list">
            <ion-item class="auth-item">
              <ion-input
                v-model="form.email"
                autocomplete="email"
                inputmode="email"
                label="邮箱"
                label-placement="stacked"
                placeholder="请输入登录邮箱"
                type="email"
              />
            </ion-item>

            <ion-item v-if="loginMode === 'password'" class="auth-item">
              <ion-input
                v-model="form.password"
                autocomplete="current-password"
                label="密码"
                label-placement="stacked"
                placeholder="请输入密码"
                type="password"
              />
            </ion-item>

            <div v-if="loginMode === 'password'" class="auth-secondary-link">
              <button class="auth-text-button" type="button" @click="handleGoToForgotPassword">
                忘记密码？
              </button>
            </div>

            <template v-else>
              <ion-item class="auth-item">
                <ion-input
                  v-model="form.verificationCode"
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
            </template>
          </ion-list>

          <div class="auth-checkbox-row">
            <ion-checkbox :checked="form.rememberMe" @ionChange="handleRememberMeChange" />
            <span>{{ rememberMeLabel }}</span>
          </div>

          <div class="auth-checkbox-row auth-checkbox-row--top">
            <ion-checkbox :checked="form.agreeToTerms" @ionChange="handleAgreeToTermsChange" />
            <span>我已阅读并同意《用户服务协议》与《隐私政策》</span>
          </div>

          <ion-button expand="block" class="auth-submit-button" :disabled="submitting" @click="handleLogin">
            <ion-spinner v-if="submitting" name="crescent" />
            <span v-else>登录并进入门店选择</span>
          </ion-button>

          <div class="auth-footer-link">
            <span>还没有账户？</span>
            <button class="auth-text-button" type="button" @click="handleGoToRegister">立即注册</button>
          </div>
        </section>


      </div>
    </ion-content>
  </ion-page>
</template>

<script setup lang="ts">
import {
  IonButton,
  IonCheckbox,
  IonContent,
  IonInput,
  IonItem,
  IonLabel,
  IonList,
  IonPage,
  IonSegment,
  IonSegmentButton,
  IonSpinner,
} from '@ionic/vue'
import { computed, onBeforeUnmount, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { loginByCode, loginByPassword, sendVerificationCode } from '@/api/auth'
import { ROUTE_PATHS } from '@/router/guards'
import { useAuthStore } from '@/stores/auth'
import { useStoreStore } from '@/stores/store'
import { useUserStore } from '@/stores/user'
import type { LoginByCodeRequest, LoginByPasswordRequest, LoginResponse } from '@/types/auth'
import { showErrorToast, showSuccessToast, showWarningToast } from '@/utils/notify'
import { isHandledRequestError } from '@/utils/request'

const PASSWORD_MIN_LENGTH = 6
const VERIFICATION_CODE_LENGTH = 6
const VERIFICATION_CODE_SECONDS = 60
const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

type LoginMode = 'password' | 'code'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()
const userStore = useUserStore()
const storeStore = useStoreStore()

const loginMode = ref<LoginMode>('password')
const countdown = ref(0)
const submitting = ref(false)
const isSendingCode = ref(false)
const form = reactive({
  email: '',
  password: '',
  verificationCode: '',
  rememberMe: true,
  agreeToTerms: true,
})

let countdownTimer: number | null = null

const currentModeTitle = computed(() => {
  if (loginMode.value === 'password') {
    return '欢迎回来'
  }

  return '验证码登录'
})

const currentModeDescription = computed(() => {
  if (loginMode.value === 'password') {
    return '输入邮箱和密码，完成登录后继续确认当前门店。'
  }

  return '输入邮箱并获取验证码，适合临时设备快速登录。'
})

const rememberMeLabel = computed(() => {
  if (loginMode.value === 'password') {
    return '记住登录状态'
  }

  return '保持当前设备登录'
})

const sendCodeButtonLabel = computed(() => {
  if (countdown.value > 0) {
    return `${countdown.value}s 后可重新发送`
  }

  return '发送登录验证码'
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

const validatePasswordLoginForm = (): LoginByPasswordRequest | null => {
  const email = validateEmail()
  const password = form.password.trim()

  if (!email) {
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

  return {
    email,
    password,
    rememberMe: form.rememberMe,
  }
}

const validateCodeLoginForm = (): LoginByCodeRequest | null => {
  const email = validateEmail()
  const verificationCode = form.verificationCode.trim()

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

  return {
    email,
    verificationCode,
    rememberMe: form.rememberMe,
  }
}

const persistLoginSession = (payload: LoginResponse) => {
  authStore.setToken(payload.token)
  userStore.setUser(payload.user)
  storeStore.setStores(payload.stores ?? [])
  storeStore.setCurrentStore(null)
}

const resetModeSpecificFields = () => {
  form.password = ''
  form.verificationCode = ''
  countdown.value = 0
  clearCountdownTimer()
}

const handleLoginModeChange = (event: CustomEvent) => {
  const nextMode = String(event.detail.value ?? '')

  if (nextMode !== 'password' && nextMode !== 'code') {
    return
  }

  if (loginMode.value === nextMode) {
    return
  }

  loginMode.value = nextMode
  resetModeSpecificFields()
}

const handleRememberMeChange = (event: CustomEvent) => {
  form.rememberMe = Boolean(event.detail.checked)
}

const handleAgreeToTermsChange = (event: CustomEvent) => {
  form.agreeToTerms = Boolean(event.detail.checked)
}

const handleGoToRegister = async () => {
  const email = normalizeEmail()

  if (!email) {
    await router.push(ROUTE_PATHS.register)
    return
  }

  await router.push({
    path: ROUTE_PATHS.register,
    query: {
      email,
    },
  })
}

const handleGoToForgotPassword = async () => {
  const email = normalizeEmail()

  if (!email) {
    await router.push(ROUTE_PATHS.forgotPassword)
    return
  }

  await router.push({
    path: ROUTE_PATHS.forgotPassword,
    query: {
      email,
    },
  })
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
      type: 'login',
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

const handleLogin = async () => {
  if (submitting.value) {
    return
  }

  if (!form.agreeToTerms) {
    showWarningToast('请先阅读并同意用户服务协议与隐私政策')
    return
  }

  submitting.value = true

  try {
    let response

    if (loginMode.value === 'password') {
      const payload = validatePasswordLoginForm()

      if (!payload) {
        return
      }

      response = await loginByPassword(payload)
    } else {
      const payload = validateCodeLoginForm()

      if (!payload) {
        return
      }

      response = await loginByCode(payload)
    }

    if (!response.success || !response.data) {
      showErrorToast(response.message || '登录失败')
      return
    }

    persistLoginSession(response.data)
    showSuccessToast('登录成功，请选择门店')
    await router.replace(ROUTE_PATHS.storeSelection)
  } catch (error) {
    if (!isHandledRequestError(error)) {
      showErrorToast(resolveErrorMessage(error, '登录失败'))
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

<style scoped>
.auth-page {
  display: block;
}

.auth-hero {
  margin-top: 24px;
  margin-bottom: 18px;
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
  font-weight: 700;
  margin-bottom: 10px;
  color: var(--app-heading);
}

.mobile-subtitle {
  font-size: 15px;
  color: var(--app-muted);
  line-height: 1.6;
}

.auth-card {
  max-width: 480px;
  margin: 0 auto;
  padding: 22px 18px;
  border-radius: 22px;
  box-shadow: var(--app-shadow);
  background: var(--app-surface-strong);
  border: 1px solid var(--app-border);
}

.auth-card__header {
  margin-bottom: 20px;
}

.auth-list {
  margin: 16px 0 8px;
  background: transparent;
}

.auth-item {
  --background: var(--app-surface-muted);
  --border-radius: 16px;
  --padding-start: 16px;
  --padding-end: 16px;
  --inner-padding-end: 0;
  margin-bottom: 16px;
  border: 1px solid var(--app-border);
  border-radius: 16px;
  transition: all 0.2s ease;
}

.auth-item.item-has-focus {
  border-color: rgba(var(--ion-color-primary-rgb), 0.28);
  box-shadow: 0 0 0 4px rgba(var(--ion-color-primary-rgb), 0.08);
}

.auth-code-button {
  margin-top: 8px;
  margin-bottom: 12px;
  --border-radius: 16px;
  min-height: 48px;
}

.auth-secondary-link {
  display: flex;
  justify-content: flex-end;
  margin: -4px 0 12px;
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
  --border-radius: 16px;
  --box-shadow: var(--app-button-shadow);
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

@media (min-width: 768px) {
  .auth-page {
    --padding-top: calc(32px + var(--app-safe-top));
  }
}
</style>
