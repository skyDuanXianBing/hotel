<template>
  <ion-page>
    <ion-content fullscreen class="mobile-page auth-page">
      <section class="mobile-hero auth-hero">
        <p class="mobile-note auth-hero__eyebrow">THE HOST HUB</p>
        <h1 class="mobile-title">酒店业务管理平台</h1>
        <p class="mobile-subtitle">提升运营效率，随时随地管理您的门店业务和订单。</p>
      </section>

      <div class="mobile-stack">
        <section class="mobile-card auth-card">
          <div class="auth-card__header">
            <h2 class="mobile-section-title">{{ currentModeTitle }}</h2>
            <p class="mobile-note">{{ currentModeDescription }}</p>
          </div>

          <ion-list lines="none" class="auth-list">
            <p class="auth-field-label">邮箱</p>
            <ion-item :class="['auth-item', { 'auth-item--focused': focusedField === 'email' }]">
              <ion-input
                v-model="form.email"
                autocomplete="email"
                inputmode="email"
                placeholder="请输入邮箱"
                type="email"
                @ionBlur="handleInputBlur('email')"
                @ionFocus="handleInputFocus('email')"
              />
            </ion-item>

            <template v-if="loginMode === 'password'">
              <p class="auth-field-label">密码</p>
              <ion-item :class="['auth-item', { 'auth-item--focused': focusedField === 'password' }]">
                <ion-input
                  v-model="form.password"
                  autocomplete="current-password"
                  placeholder="请输入密码"
                  type="password"
                  @ionBlur="handleInputBlur('password')"
                  @ionFocus="handleInputFocus('password')"
                />
              </ion-item>

              <div class="auth-secondary-link">
                <button class="auth-text-button" type="button" @click="handleGoToForgotPassword">
                  忘记密码？
                </button>
              </div>
            </template>

            <template v-else>
              <p class="auth-field-label">图形验证码</p>
              <div class="auth-captcha-row">
                <ion-item
                  :class="['auth-item', 'auth-item--captcha', { 'auth-item--focused': focusedField === 'captcha' }]"
                >
                  <ion-input
                    v-model="form.graphicCaptcha"
                    :maxlength="GRAPHIC_CAPTCHA_LENGTH"
                    autocomplete="off"
                    autocapitalize="characters"
                    placeholder="请输入图形验证码"
                    type="text"
                    @ionBlur="handleInputBlur('captcha')"
                    @ionFocus="handleInputFocus('captcha')"
                  />
                </ion-item>

                <button class="auth-captcha-preview" type="button" @click="refreshGraphicCaptcha">
                  <span class="auth-captcha-preview__bg" />
                  <span
                    v-for="line in captchaLines"
                    :key="line.id"
                    class="auth-captcha-line"
                    :style="line.style"
                  />
                  <span
                    v-for="dot in captchaDots"
                    :key="dot.id"
                    class="auth-captcha-dot"
                    :style="dot.style"
                  />
                  <span class="auth-captcha-chars">
                    <span
                      v-for="(char, index) in graphicCaptchaChars"
                      :key="`${char}-${index}`"
                      class="auth-captcha-char"
                      :style="getCaptchaCharStyle(index)"
                    >
                      {{ char }}
                    </span>
                  </span>
                </button>
              </div>

              <ion-button
                expand="block"
                :class="[
                  'auth-submit-button',
                  'auth-submit-button--code',
                  { 'auth-submit-button--code-disabled': isSendCodeButtonEmpty },
                ]"
                :disabled="isSendCodeButtonDisabled"
                @click="handleSendVerificationCode"
              >
                <ion-spinner v-if="isSendingCode" name="crescent" />
                <span v-else>发送验证码</span>
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

          <ion-button
            v-if="loginMode === 'password'"
            expand="block"
            class="auth-submit-button"
            :disabled="submitting"
            @click="handleLogin"
          >
            <ion-spinner v-if="submitting" name="crescent" />
            <span v-else>登录</span>
          </ion-button>

          <ion-segment class="auth-mode-switch" :value="loginMode" @ionChange="handleLoginModeChange">
            <ion-segment-button v-if="loginMode === 'password'" value="code">
              <ion-label>使用验证码登录</ion-label>
            </ion-segment-button>
            <ion-segment-button v-else value="password">
              <ion-label>使用密码登录</ion-label>
            </ion-segment-button>
          </ion-segment>

          <div :class="['auth-footer-link', { 'auth-footer-link--password': loginMode === 'password' }]">
            <button class="auth-text-button" type="button" @click="handleGoToRegister">注册新账号</button>
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
import { computed, reactive, ref, watch } from 'vue'
import type { CSSProperties } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { loginByPassword, sendVerificationCode } from '@/api/auth'
import { cleanerLoginByPassword } from '@/api/cleanerAuth'
import { AUTH_LOGIN_FAILURE_STATUSES, LOGIN_FAILURE_MESSAGE } from '@/constants/auth'
import { ROUTE_PATHS } from '@/router/guards'
import { useAuthStore } from '@/stores/auth'
import { useStoreStore } from '@/stores/store'
import { useUserStore } from '@/stores/user'
import type { CleanerDTO, CleanerSessionUser, LoginByPasswordRequest, LoginResponse } from '@/types/auth'
import { saveCleanerSession } from '@/utils/cleanerSession'
import { showErrorToast, showSuccessToast, showWarningToast } from '@/utils/notify'
import { getRequestErrorStatus, isHandledRequestError } from '@/utils/request'

const PASSWORD_MIN_LENGTH = 6
const GRAPHIC_CAPTCHA_LENGTH = 4
const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
const GRAPHIC_CAPTCHA_POOL = 'ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789'

type LoginMode = 'password' | 'code'
type FocusedField = 'email' | 'password' | 'captcha' | null

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()
const userStore = useUserStore()
const storeStore = useStoreStore()

const loginMode = ref<LoginMode>('password')
const focusedField = ref<FocusedField>(null)
const submitting = ref(false)
const isSendingCode = ref(false)
const graphicCaptchaSeed = ref(Date.now())

const form = reactive({
  email: '',
  password: '',
  verificationCode: '',
  graphicCaptcha: '',
  rememberMe: true,
  agreeToTerms: true,
})

const currentModeTitle = computed(() => '欢迎回来')

const currentModeDescription = computed(() => {
  if (loginMode.value === 'password') {
    return '输入邮箱和密码，完成登录后继续确认当前门店。'
  }

  return '输入邮箱和图形验证码，获取邮箱登录验证码。'
})

const rememberMeLabel = computed(() => {
  if (loginMode.value === 'password') {
    return '记住登录状态'
  }

  return '保持当前设备登录'
})

const hasEmailInput = computed(() => normalizeEmail().length > 0)
const isSendCodeButtonEmpty = computed(() => !hasEmailInput.value && !isSendingCode.value)
const isSendCodeButtonDisabled = computed(() => isSendingCode.value || !hasEmailInput.value)

const graphicCaptchaValue = computed(() => {
  const chars: string[] = []
  let seed = graphicCaptchaSeed.value

  for (let index = 0; index < GRAPHIC_CAPTCHA_LENGTH; index += 1) {
    seed = (seed * 9301 + 49297) % 233280
    chars.push(GRAPHIC_CAPTCHA_POOL[Math.floor((seed / 233280) * GRAPHIC_CAPTCHA_POOL.length)])
  }

  return chars.join('')
})

const graphicCaptchaChars = computed(() => graphicCaptchaValue.value.split(''))

const captchaLines = computed(() => [
  {
    id: 'line-1',
    style: {
      left: '6%',
      top: '30%',
      width: '78%',
      transform: 'rotate(14deg)',
      background: 'rgba(121, 92, 188, 0.32)',
    },
  },
  {
    id: 'line-2',
    style: {
      left: '18%',
      top: '64%',
      width: '62%',
      transform: 'rotate(-12deg)',
      background: 'rgba(74, 126, 215, 0.28)',
    },
  },
  {
    id: 'line-3',
    style: {
      left: '40%',
      top: '18%',
      width: '28%',
      transform: 'rotate(-36deg)',
      background: 'rgba(94, 186, 164, 0.22)',
    },
  },
])

const captchaDots = computed(() =>
  Array.from({ length: 18 }, (_, index) => ({
    id: `dot-${index}`,
    style: {
      left: `${8 + ((index * 11) % 84)}%`,
      top: `${10 + ((index * 17) % 72)}%`,
      background:
        index % 3 === 0
          ? 'rgba(255, 255, 255, 0.82)'
          : index % 3 === 1
            ? 'rgba(218, 196, 255, 0.66)'
            : 'rgba(184, 216, 255, 0.62)',
    },
  })),
)

const resolveErrorMessage = (error: unknown, fallbackMessage: string) => {
  if (error instanceof Error && error.message) {
    return error.message
  }

  return fallbackMessage
}

const handleInputFocus = (field: Exclude<FocusedField, null>) => {
  focusedField.value = field
}

const handleInputBlur = (field: Exclude<FocusedField, null>) => {
  if (focusedField.value === field) {
    focusedField.value = null
  }
}

const normalizeEmail = () => form.email.trim()

const refreshGraphicCaptcha = () => {
  graphicCaptchaSeed.value = Date.now() + Math.floor(Math.random() * 1000)
  form.graphicCaptcha = ''
  focusedField.value = null
}

const validateGraphicCaptcha = () => {
  const inputValue = form.graphicCaptcha.trim().toLowerCase()
  const expectedValue = graphicCaptchaValue.value.toLowerCase()

  if (!inputValue) {
    showWarningToast('请输入图形验证码')
    return false
  }

  if (inputValue !== expectedValue) {
    showWarningToast('图形验证码不正确，请重新输入')
    refreshGraphicCaptcha()
    return false
  }

  return true
}

const getCaptchaCharStyle = (index: number): CSSProperties => {
  const rotate = ((graphicCaptchaSeed.value + index * 37) % 18) - 9
  const offsetY = ((graphicCaptchaSeed.value + index * 19) % 10) - 5
  const colors = ['#5a48b5', '#315dbe', '#7a2f93', '#2b7381']

  return {
    color: colors[index % colors.length],
    transform: `translateY(${offsetY}px) rotate(${rotate}deg)`,
  }
}

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

const persistLoginSession = (payload: LoginResponse) => {
  authStore.setToken(payload.token)
  userStore.setUser(payload.user)
  storeStore.setStores(payload.stores ?? [])
  storeStore.setCurrentStore(null)
}

const buildCleanerSessionUser = (cleaner: CleanerDTO): CleanerSessionUser => {
  return {
    id: cleaner.id,
    email: cleaner.email,
    nickname: cleaner.name,
    gender: 'private',
    createdAt: cleaner.createdAt,
    updatedAt: cleaner.updatedAt,
    isCleaner: true,
  }
}

const isLoginFailureError = (error: unknown) => {
  const status = getRequestErrorStatus(error)

  if (status === null) {
    return false
  }

  return AUTH_LOGIN_FAILURE_STATUSES.some((failureStatus) => failureStatus === status)
}

const loginAsAdmin = async (payload: LoginByPasswordRequest) => {
  try {
    const response = await loginByPassword(payload)

    if (!response.success || !response.data) {
      return false
    }

    persistLoginSession(response.data)
    showSuccessToast('登录成功，请选择门店')
    await router.replace(ROUTE_PATHS.storeSelection)
    return true
  } catch (error) {
    if (isLoginFailureError(error)) {
      return false
    }

    throw error
  }
}

const loginAsCleaner = async (payload: LoginByPasswordRequest) => {
  try {
    const response = await cleanerLoginByPassword(payload)

    if (!response.success || !response.data?.token || !response.data.cleaner) {
      return false
    }

    const cleanerUser = buildCleanerSessionUser(response.data.cleaner)
    saveCleanerSession(response.data.token, cleanerUser, response.data.cleaner.storeId)
    showSuccessToast('登录成功，已进入保洁工作台')
    await router.replace(ROUTE_PATHS.cleanerDashboard)
    return true
  } catch (error) {
    if (isLoginFailureError(error)) {
      return false
    }

    throw error
  }
}

const resetModeSpecificFields = () => {
  form.password = ''
  form.verificationCode = ''
  form.graphicCaptcha = ''
  refreshGraphicCaptcha()
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
  focusedField.value = null
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
    query: { email },
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
    query: { email },
  })
}

const handleSendVerificationCode = async () => {
  if (isSendingCode.value) {
    return
  }

  if (!form.agreeToTerms) {
    showWarningToast('请先阅读并同意用户服务协议与隐私政策')
    return
  }

  const email = validateEmail()
  if (!email || !validateGraphicCaptcha()) {
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
      refreshGraphicCaptcha()
      return
    }

    showSuccessToast('验证码已发送，请查收邮箱')
    await router.push({
      path: ROUTE_PATHS.loginCodeVerify,
      query: { email },
    })
  } catch (error) {
    if (!isHandledRequestError(error)) {
      showErrorToast(resolveErrorMessage(error, '验证码发送失败'))
    }
    refreshGraphicCaptcha()
  } finally {
    isSendingCode.value = false
  }
}

const handleLogin = async () => {
  if (loginMode.value !== 'password' || submitting.value) {
    return
  }

  if (!form.agreeToTerms) {
    showWarningToast('请先阅读并同意用户服务协议与隐私政策')
    return
  }

  const payload = validatePasswordLoginForm()
  if (!payload) {
    return
  }

  submitting.value = true

  try {
    const adminLoggedIn = await loginAsAdmin(payload)
    if (adminLoggedIn) {
      return
    }

    const cleanerLoggedIn = await loginAsCleaner(payload)
    if (cleanerLoggedIn) {
      return
    }

    showErrorToast(LOGIN_FAILURE_MESSAGE)
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
</script>

<style scoped>
.auth-page {
  --auth-control-radius: 13px;
  --background: #fcfcfc;
  --padding-top: calc(44px + var(--app-safe-top));
  --padding-bottom: calc(28px + var(--app-safe-bottom));
  --padding-start: 24px;
  --padding-end: 24px;
  display: block;
  color: #1f2128;
}

.auth-hero {
  display: none;
}

.mobile-stack {
  min-height: calc(100vh - var(--app-safe-top) - var(--app-safe-bottom) - 72px);
}

.auth-card {
  width: 100%;
  max-width: 560px;
  margin: 0;
  padding: 74px 0 0;
  border-radius: 0;
  box-shadow: none;
  background: transparent;
  border: 0;
  min-height: calc(100vh - var(--app-safe-top) - var(--app-safe-bottom) - 74px);
  display: flex;
  flex-direction: column;
}

.auth-card__header {
  margin: 0 0 40px;
}

.auth-card__header .mobile-note {
  display: none;
}

.mobile-section-title {
  margin: 0;
  color: #1f2128;
  font-size: clamp(29px, 7.8vw, 35px);
  font-weight: 400;
  line-height: 1.18;
  letter-spacing: 0;
}

.mobile-section-title::before {
  content: '👋 ';
}

.auth-list {
  margin: 0;
  background: transparent;
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
  --border-radius: var(--auth-control-radius);
  --padding-start: 20px;
  --padding-end: 20px;
  --inner-padding-end: 0;
  --inner-padding-top: 0;
  --inner-padding-bottom: 0;
  --highlight-height: 0;
  margin-bottom: 24px;
  --min-height: var(--auth-input-height);
  height: var(--auth-input-height);
  min-height: var(--auth-input-height);
  border: 2px solid #d6d7dc;
  border-radius: var(--auth-control-radius);
  box-shadow: none;
  transition: border-color 0.2s ease;
  position: relative;
}

.auth-item::part(native) {
  min-height: var(--auth-input-height);
  height: var(--auth-input-height);
  padding-top: 0;
  padding-bottom: 0;
  display: flex;
  align-items: center;
}

.auth-list > .auth-item:first-child::after {
  content: '✉';
  position: absolute;
  right: 18px;
  top: 50%;
  transform: translateY(-50%);
  color: #1f2128;
  font-size: clamp(18px, 4.6vw, 22px);
  line-height: 1;
  pointer-events: none;
}

.auth-item.item-has-focus,
.auth-item--focused {
  border-color: #3484ea;
  box-shadow: none;
}

.auth-secondary-link {
  display: flex;
  justify-content: flex-start;
  margin: -8px 0 34px;
}

.auth-captcha-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 132px;
  gap: 12px;
  align-items: stretch;
  margin-bottom: 18px;
}

.auth-item--captcha {
  margin-bottom: 0;
}

.auth-captcha-preview {
  position: relative;
  overflow: hidden;
  border: 0;
  border-radius: var(--auth-control-radius);
  background: linear-gradient(135deg, #efe7ff 0%, #e3edff 54%, #f5ebff 100%);
  min-height: var(--auth-input-height, 56px);
  padding: 0 10px;
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
  font-size: 29px;
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

.auth-captcha-dot {
  width: 5px;
  height: 5px;
  border-radius: 50%;
}

.auth-checkbox-row,
.auth-checkbox-row--top {
  display: none;
}

.auth-submit-button {
  margin-top: 0;
  min-height: clamp(54px, 13.5vw, 64px);
  font-size: clamp(21px, 5.8vw, 24px);
  font-weight: 400;
  letter-spacing: 0;
  --border-radius: var(--auth-control-radius);
  --background: #3484ea;
  --background-activated: #2e77d4;
  --background-focused: #2e77d4;
  --background-hover: #2e77d4;
  --box-shadow: none;
  --color: #ffffff;
}

.auth-submit-button--code {
  margin-top: 30px;
}

.auth-submit-button--code-disabled {
  --background: #f9f9f9;
  --background-activated: #f9f9f9;
  --background-focused: #f9f9f9;
  --background-hover: #f9f9f9;
  --color: #c6c9d1;
  --border-color: #dde5e8;
  --border-style: solid;
  --border-width: 2px;
  opacity: 1;
}

:deep(.auth-submit-button--code-disabled.button-disabled) {
  opacity: 1;
}

:deep(.auth-submit-button--code-disabled.button-disabled .button-native) {
  opacity: 1;
  color: #c6c9d1;
  background: #f9f9f9;
  border-color: #dde5e8;
}

.auth-mode-switch {
  margin: 26px 0 0;
  background: transparent;
  --background: transparent;
  --indicator-color: transparent;
  display: flex;
  justify-content: center;
  overflow: visible;
}

.auth-footer-link {
  margin-top: auto;
  margin-bottom: 2px;
  padding-top: 64px;
  display: flex;
  align-items: center;
  justify-content: center;
}

:deep(ion-segment-button) {
  flex: 0;
  min-height: auto;
  min-width: max-content;
  max-width: none;
  width: auto;
  padding: 0;
  --background: transparent;
  --background-checked: transparent;
  --indicator-color: transparent;
  --color: #9a9fac;
  --color-checked: #9a9fac;
}

:deep(ion-segment-button ion-label) {
  margin: 0;
  white-space: nowrap;
  overflow: visible;
  text-overflow: clip;
  max-width: none;
  font-size: clamp(16px, 4.3vw, 18px);
  font-weight: 400;
  line-height: 1.3;
  letter-spacing: 0;
  text-transform: none;
}

:deep(.auth-item ion-input) {
  width: 100%;
  height: 100%;
  min-height: 100%;
  display: flex;
  align-items: center;
  --padding-top: 0;
  --padding-bottom: 0;
  --placeholder-color: #bdbfc4;
  --placeholder-opacity: 1;
}

:deep(.auth-item ion-input::part(native)) {
  height: auto;
  min-height: 0;
  line-height: 1.2;
  padding-top: 0;
  padding-bottom: 0;
}

:deep(.auth-item .input-wrapper),
:deep(.auth-item .native-wrapper) {
  height: 100%;
  min-height: 100%;
  display: flex;
  align-items: center;
}

:deep(.auth-list > .auth-item:first-child ion-input) {
  --padding-end: 22px;
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

:deep(.auth-item .native-input::placeholder),
:deep(.auth-item input::placeholder) {
  color: #bdbfc4;
}

.auth-secondary-link .auth-text-button {
  color: #6f7480;
  font-size: clamp(15px, 4.6vw, 18px);
  font-weight: 400;
  line-height: 1.28;
}

:deep(.auth-submit-button .button-native) {
  padding: 0 14px;
}

.auth-footer-link .auth-text-button {
  color: #5d626b;
  font-size: clamp(15px, 4.2vw, 18px);
  font-weight: 400;
  line-height: 1.24;
}

.auth-footer-link--password .auth-text-button {
  color: #3484ea;
}

.auth-text-button {
  padding: 0;
  border: 0;
  background: transparent;
  font: inherit;
}

@media (min-width: 768px) {
  .auth-page {
    --padding-top: calc(78px + var(--app-safe-top));
    --padding-start: 56px;
    --padding-end: 56px;
  }

  .auth-card {
    margin: 0 auto;
    min-height: calc(100vh - var(--app-safe-top) - var(--app-safe-bottom) - 102px);
  }

  .mobile-section-title {
    font-size: 35px;
  }

  .auth-field-label,
  :deep(.auth-item .native-input),
  :deep(.auth-item input) {
    font-size: 20px;
  }
}

@media (max-width: 480px) {
  .auth-page {
    --padding-top: calc(38px + var(--app-safe-top));
    --padding-start: 24px;
    --padding-end: 24px;
  }

  .auth-card__header {
    margin-bottom: 34px;
  }

  .auth-card {
    padding-top: 70px;
  }

  .mobile-section-title {
    font-size: clamp(29px, 9vw, 35px);
  }

  .auth-secondary-link {
    margin: -6px 0 30px;
  }

  .auth-captcha-row {
    grid-template-columns: minmax(0, 1fr) 122px;
    gap: 10px;
  }

  .auth-captcha-chars {
    font-size: 26px;
  }

  .auth-footer-link {
    padding-top: 56px;
  }
}

@media (max-width: 360px) {
  .auth-page {
    --padding-start: 20px;
    --padding-end: 20px;
  }

  .auth-item {
    --padding-start: 16px;
    --padding-end: 16px;
  }

  .auth-captcha-row {
    grid-template-columns: minmax(0, 1fr) 112px;
  }

  .auth-submit-button {
    font-size: clamp(20px, 5.6vw, 22px);
  }
}
</style>
