<template>
  <ion-page>
    <ion-content fullscreen class="auth-page">
      <div class="auth-shell">
        <AuthBrandHeader
          show-tabs
          :active-tab="accountTab"
          @select-login="handleAccountTabChange('login')"
          @select-register="handleAccountTabChange('register')"
        />

        <main class="auth-panel">
          <section v-show="accountTab === 'login'" class="auth-account-panel">
            <ion-list lines="none" class="auth-list">
              <div class="auth-field">
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
              </div>

              <template v-if="loginMode === 'password'">
                <div class="auth-field">
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
                </div>

                <div class="auth-secondary-link">
                  <button class="auth-text-button" type="button" @click="handleGoToForgotPassword">
                    忘记密码？
                  </button>
                </div>
              </template>

              <template v-else>
                <div class="auth-field">
                  <p class="auth-field-label">验证码</p>
                  <div class="auth-captcha-row">
                    <div class="auth-captcha-input">
                      <ion-item
                        :class="[
                          'auth-item',
                          'auth-item--captcha',
                          { 'auth-item--focused': focusedField === 'captcha' },
                        ]"
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

                      <button
                        class="auth-captcha-preview"
                        type="button"
                        aria-label="刷新图形验证码"
                        @click="refreshGraphicCaptcha"
                      >
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
                      fill="outline"
                      :class="[
                        'auth-code-action',
                        { 'auth-code-action--disabled': isSendCodeButtonEmpty },
                      ]"
                      :disabled="isSendCodeButtonDisabled"
                      @click="handleSendVerificationCode"
                    >
                      <ion-spinner v-if="isSendingCode" name="crescent" />
                      <span v-else>发送验证码</span>
                    </ion-button>
                  </div>
                </div>
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
              class="auth-primary-button"
              :disabled="submitting"
              @click="handleLogin"
            >
              <ion-spinner v-if="submitting" name="crescent" />
              <span v-else>登录</span>
            </ion-button>

            <ion-segment
              :class="[
                'auth-mode-switch',
                { 'auth-mode-switch--code': loginMode === 'code' },
              ]"
              :value="loginMode"
              @ionChange="handleLoginModeChange"
            >
              <ion-segment-button v-if="loginMode === 'password'" value="code">
                <ion-label>使用验证码登录</ion-label>
              </ion-segment-button>
              <ion-segment-button v-else value="password">
                <ion-label>使用密码登录</ion-label>
              </ion-segment-button>
            </ion-segment>
          </section>

          <RegisterPage
            v-show="accountTab === 'register'"
            :initial-email="registerEmail"
            @email-change="handleRegisterEmailChange"
            @registered="handleRegistrationComplete"
          />
        </main>
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
import AuthBrandHeader from '@/components/auth/AuthBrandHeader.vue'
import { AUTH_LOGIN_FAILURE_STATUSES, LOGIN_FAILURE_MESSAGE } from '@/constants/auth'
import { ROUTE_PATHS } from '@/router/guards'
import { useAuthStore } from '@/stores/auth'
import { useStoreStore } from '@/stores/store'
import { useUserStore } from '@/stores/user'
import type { LoginByPasswordRequest } from '@/types/auth'
import { clearAutoLoginCredentials, saveAutoLoginCredentials } from '@/utils/autoLogin'
import { applyUnifiedLoginResponse } from '@/utils/loginSessionResolver'
import { showErrorToast, showSuccessToast, showWarningToast } from '@/utils/notify'
import { getRequestErrorStatus, isHandledRequestError } from '@/utils/request'
import RegisterPage from './RegisterPage.vue'

const PASSWORD_MIN_LENGTH = 6
const GRAPHIC_CAPTCHA_LENGTH = 4
const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
const GRAPHIC_CAPTCHA_POOL = 'ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789'

type LoginMode = 'password' | 'code'
type AccountTab = 'login' | 'register'
type FocusedField = 'email' | 'password' | 'captcha' | null

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()
const userStore = useUserStore()
const storeStore = useStoreStore()

const accountTab = ref<AccountTab>('login')
const loginMode = ref<LoginMode>('password')
const focusedField = ref<FocusedField>(null)
const submitting = ref(false)
const isSendingCode = ref(false)
const graphicCaptchaSeed = ref(Date.now())
const registerEmail = ref('')

const form = reactive({
  email: '',
  password: '',
  verificationCode: '',
  graphicCaptcha: '',
  rememberMe: true,
  agreeToTerms: true,
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
  registerEmail.value = email
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

const hydrateRuntimeStores = () => {
  authStore.hydrate()
  userStore.hydrate()
  storeStore.hydrate()
}

const isLoginFailureError = (error: unknown) => {
  const status = getRequestErrorStatus(error)

  if (status === null) {
    return false
  }

  return AUTH_LOGIN_FAILURE_STATUSES.some((failureStatus) => failureStatus === status)
}

const loginWithPassword = async (payload: LoginByPasswordRequest) => {
  try {
    const response = await loginByPassword(payload)

    if (!response.success || !response.data) {
      return false
    }

    const sessionResult = applyUnifiedLoginResponse(response.data, {
      resetPmsCurrentStore: true,
    })
    hydrateRuntimeStores()

    if (!payload.rememberMe) {
      clearAutoLoginCredentials()
    } else {
      try {
        await saveAutoLoginCredentials({
          email: payload.email,
          password: payload.password,
          token: response.data.token,
        })
      } catch {
        clearAutoLoginCredentials()
      }
    }

    if (sessionResult.target === 'CLEANER') {
      showSuccessToast('登录成功')
    } else {
      showSuccessToast('登录成功，请选择门店')
    }

    await router.replace(sessionResult.redirectPath)
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

const buildAccountTabQuery = (tab: AccountTab, email: string) => {
  const query = { ...route.query }

  delete query.email
  delete query.tab

  if (email) {
    query.email = email
  }

  if (tab === 'register') {
    query.tab = 'register'
  }

  return query
}

const handleAccountTabChange = async (nextTab: AccountTab) => {
  const loginEmail = normalizeEmail()
  const currentRegisterEmail = registerEmail.value.trim()
  const email =
    nextTab === 'register'
      ? loginEmail || currentRegisterEmail
      : currentRegisterEmail || loginEmail

  if (nextTab === 'register' && email) {
    registerEmail.value = email
  }

  if (nextTab === 'login' && email) {
    form.email = email
  }

  accountTab.value = nextTab
  focusedField.value = null

  await router.replace({
    path: ROUTE_PATHS.login,
    query: buildAccountTabQuery(nextTab, email),
  })
}

const handleRegisterEmailChange = (email: string) => {
  registerEmail.value = email
}

const handleRegistrationComplete = async (email: string) => {
  const normalizedEmail = email.trim()

  form.email = normalizedEmail
  registerEmail.value = normalizedEmail
  accountTab.value = 'login'

  await router.replace({
    path: ROUTE_PATHS.login,
    query: buildAccountTabQuery('login', normalizedEmail),
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
    const loggedIn = await loginWithPassword(payload)
    if (loggedIn) {
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

watch(
  () => route.query.tab,
  (tabQuery) => {
    accountTab.value = tabQuery === 'register' ? 'register' : 'login'
  },
  {
    immediate: true,
  },
)
</script>
