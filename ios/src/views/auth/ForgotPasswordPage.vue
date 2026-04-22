<template>
  <ion-page>
    <ion-header translucent class="forgot-password-header">
      <ion-toolbar class="forgot-password-toolbar">
        <ion-buttons slot="start">
          <ion-back-button class="forgot-password-back" :default-href="ROUTE_PATHS.login" text="" />
        </ion-buttons>
        <ion-title class="mobile-toolbar-title forgot-password-toolbar__title">忘记密码</ion-title>
      </ion-toolbar>
    </ion-header>

    <ion-content :scroll-y="false" fullscreen class="mobile-page auth-page forgot-password-page">
      <section class="forgot-password-layout">
        <div class="forgot-password-heading">
          <h1 class="forgot-password-title">找回密码</h1>
        </div>

        <ion-list lines="none" class="auth-list forgot-password-form">
          <div class="auth-field">
            <p class="auth-field-label">邮箱</p>
            <ion-item class="auth-item">
              <ion-input
                v-model="form.email"
                autocomplete="email"
                inputmode="email"
                placeholder="邮箱"
                type="email"
              />
            </ion-item>
          </div>

          <div class="auth-field">
            <p class="auth-field-label">验证码</p>
            <div class="auth-captcha-row">
              <ion-item class="auth-item auth-item--captcha">
                <ion-input autocomplete="off" placeholder="图片验证码" type="text" />
              </ion-item>

              <button class="auth-captcha-preview auth-captcha-preview--static" type="button" aria-label="图形验证码">
                <span class="auth-captcha-preview__bg" />
                <span class="auth-captcha-line auth-captcha-line--one" />
                <span class="auth-captcha-line auth-captcha-line--two" />
                <span class="auth-captcha-dot auth-captcha-dot--one" />
                <span class="auth-captcha-dot auth-captcha-dot--two" />
                <span class="auth-captcha-dot auth-captcha-dot--three" />
                <span class="auth-captcha-chars">
                  <span class="auth-captcha-char auth-captcha-char--m">m</span>
                  <span class="auth-captcha-char auth-captcha-char--a">a</span>
                  <span class="auth-captcha-char auth-captcha-char--s">s</span>
                  <span class="auth-captcha-char auth-captcha-char--h">H</span>
                </span>
              </button>
            </div>
          </div>

          <div class="auth-field">
            <p class="auth-field-label">收到的验证码</p>
            <div class="auth-code-row">
              <ion-item class="auth-item auth-item--code">
                <ion-input
                  v-model="form.verificationCode"
                  autocomplete="one-time-code"
                  inputmode="numeric"
                  :maxlength="VERIFICATION_CODE_LENGTH"
                  placeholder="输入验证码"
                  type="text"
                />
              </ion-item>

              <ion-button
                class="auth-code-button"
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
                placeholder="新密码（8--16位字母和数字组合）"
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
          class="auth-submit-button"
          :disabled="submitting"
          @click="handleResetPassword"
        >
          <ion-spinner v-if="submitting" name="crescent" />
          <span v-else>确认</span>
        </ion-button>
      </section>
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
.forgot-password-header,
.forgot-password-toolbar {
  --background: #ffffff;
  --border-color: transparent;
  --min-height: 52px;
  box-shadow: none;
  background: #ffffff;
}

.forgot-password-toolbar__title {
  opacity: 0;
}

.forgot-password-back {
  --color: #1f2128;
  --icon-font-size: 28px;
  --padding-start: 0;
  --padding-end: 0;
  margin-left: 10px;
}

.auth-page {
  --background: #ffffff;
}

.forgot-password-page {
  --padding-top: calc(4px + var(--app-safe-top));
  --padding-bottom: calc(20px + var(--app-safe-bottom));
  --padding-start: 24px;
  --padding-end: 24px;
  overflow: hidden;
}

.forgot-password-layout {
  min-height: 100%;
  display: flex;
  flex-direction: column;
}

.forgot-password-heading {
  padding-top: 18px;
  margin-bottom: 28px;
}

.forgot-password-title {
  margin: 0;
  color: #1f2128;
  font-size: clamp(34px, 10vw, 54px);
  font-weight: 400;
  line-height: 1.08;
  letter-spacing: 0;
}

.auth-list {
  margin: 0;
  background: transparent;
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.auth-field {
  margin-bottom: 0;
}

.auth-field-label {
  margin: 0 0 10px 2px;
  color: #666b74;
  font-size: clamp(16px, 4.9vw, 19px);
  font-weight: 400;
  line-height: 1.32;
}

.auth-item {
  --auth-input-height: clamp(54px, 13.5vw, 64px);
  --background: #fcfcfc;
  --border-radius: 18px;
  --padding-start: 20px;
  --padding-end: 20px;
  --inner-padding-end: 0;
  --inner-padding-top: 0;
  --inner-padding-bottom: 5px;
  --highlight-height: 0;
  --min-height: var(--auth-input-height);
  height: var(--auth-input-height);
  min-height: var(--auth-input-height);
  margin: 0;
  border: 2px solid #dde1e6;
  border-radius: 18px;
  box-shadow: none;
}

.auth-item::part(native) {
  min-height: var(--auth-input-height);
  height: var(--auth-input-height);
  padding-top: 0;
  padding-bottom: 0;
  display: flex;
  align-items: center;
}

.auth-captcha-row,
.auth-code-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 126px;
  gap: 14px;
  align-items: stretch;
}

.auth-item--captcha,
.auth-item--code {
  min-width: 0;
}

.auth-captcha-preview {
  position: relative;
  overflow: hidden;
  min-height: clamp(54px, 13.5vw, 64px);
  border: 0;
  border-radius: 18px;
  background: linear-gradient(135deg, #efe7ff 0%, #e3edff 54%, #f5ebff 100%);
  padding: 0 12px;
}

.auth-captcha-preview__bg {
  position: absolute;
  inset: 0;
  background:
    radial-gradient(circle at 16% 22%, rgba(255, 255, 255, 0.95) 0, rgba(255, 255, 255, 0) 18%),
    radial-gradient(circle at 84% 26%, rgba(206, 192, 255, 0.58) 0, rgba(206, 192, 255, 0) 28%),
    radial-gradient(circle at 66% 78%, rgba(190, 228, 255, 0.5) 0, rgba(190, 228, 255, 0) 24%);
}

.auth-captcha-chars {
  position: relative;
  z-index: 2;
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 100%;
  font-size: 28px;
  font-weight: 600;
  letter-spacing: 2px;
}

.auth-captcha-char {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 18px;
  text-shadow: 0 1px 0 rgba(255, 255, 255, 0.45);
}

.auth-captcha-char--m {
  color: #b01778;
  transform: rotate(-12deg) translateY(1px);
}

.auth-captcha-char--a {
  color: #cd4c42;
  transform: rotate(-6deg) translateY(3px);
}

.auth-captcha-char--s {
  color: #94403e;
  transform: rotate(8deg) translateY(5px);
}

.auth-captcha-char--h {
  color: #4f3c33;
  transform: rotate(13deg) translateY(-1px);
}

.auth-captcha-line,
.auth-captcha-dot {
  position: absolute;
  z-index: 1;
  pointer-events: none;
}

.auth-captcha-line {
  height: 2px;
  border-radius: 999px;
}

.auth-captcha-line--one {
  left: 8%;
  top: 32%;
  width: 78%;
  transform: rotate(14deg);
  background: rgba(121, 92, 188, 0.32);
}

.auth-captcha-line--two {
  left: 20%;
  top: 64%;
  width: 64%;
  transform: rotate(-18deg);
  background: rgba(74, 126, 215, 0.28);
}

.auth-captcha-dot {
  width: 5px;
  height: 5px;
  border-radius: 50%;
}

.auth-captcha-dot--one {
  left: 14%;
  top: 70%;
  background: rgba(230, 152, 207, 0.62);
}

.auth-captcha-dot--two {
  left: 48%;
  top: 20%;
  background: rgba(255, 220, 126, 0.7);
}

.auth-captcha-dot--three {
  right: 14%;
  top: 74%;
  background: rgba(153, 208, 255, 0.72);
}

.auth-code-button {
  margin: 0;
  min-height: clamp(54px, 13.5vw, 64px);
  font-size: clamp(14px, 4vw, 16px);
  font-weight: 400;
  letter-spacing: 0;
  white-space: nowrap;
  --border-radius: 18px;
  --border-color: #dde1e6;
  --border-style: solid;
  --border-width: 2px;
  --background: #ffffff;
  --background-activated: #ffffff;
  --background-hover: #ffffff;
  --background-focused: #ffffff;
  --color: #1f2128;
  --box-shadow: none;
}

.auth-submit-button {
  margin-top: 36px;
  min-height: clamp(58px, 14.5vw, 68px);
  font-size: clamp(22px, 6vw, 26px);
  font-weight: 400;
  letter-spacing: 0;
  --border-radius: 20px;
  --background: #3484ea;
  --background-activated: #2e77d4;
  --background-focused: #2e77d4;
  --background-hover: #2e77d4;
  --box-shadow: 0 8px 18px rgba(52, 132, 234, 0.2);
  --color: #ffffff;
}

:deep(.auth-item ion-input) {
  width: 100%;
  height: 100%;
  min-height: 100%;
  display: flex;
  align-items: center;
  --padding-top: 0;
  --padding-bottom: 0;
  --placeholder-color: #c4c8cf;
  --placeholder-opacity: 1;
  --placeholder-font-size: clamp(14px, 4.2vw, 17px);
}

:deep(.auth-item ion-input::part(native)) {
  height: auto;
  min-height: 0;
  line-height: 1.2;
}

:deep(.auth-item .input-wrapper),
:deep(.auth-item .native-wrapper) {
  height: 100%;
  min-height: 100%;
  display: flex;
  align-items: center;
}

:deep(.auth-item .native-input),
:deep(.auth-item input) {
  color: #1f2128;
  font-size: clamp(16px, 5.1vw, 20px);
  font-weight: 400;
  height: auto;
  min-height: 0;
  box-sizing: border-box;
  margin: 0;
  padding-top: 0;
  padding-bottom: 0;
  line-height: 1.2;
}

:deep(.auth-item input::placeholder),
:deep(.auth-item .native-input::placeholder) {
  font-size: clamp(14px, 4.2vw, 17px);
}

:deep(.auth-code-button.button-disabled) {
  opacity: 1;
}

:deep(.auth-code-button.button-disabled .button-native) {
  opacity: 1;
  color: #b7bcc6;
  background: #ffffff;
  border-color: #dde1e6;
}

:deep(.auth-submit-button .button-native),
:deep(.auth-code-button .button-native) {
  border-radius: inherit;
}

:deep(.auth-code-button .button-native) {
  white-space: nowrap;
  font-size: clamp(14px, 4vw, 16px);
}

@media (min-width: 768px) {
  .forgot-password-page {
    --padding-start: 40px;
    --padding-end: 40px;
  }

  .forgot-password-layout {
    max-width: 640px;
    margin: 0 auto;
  }
}
</style>
