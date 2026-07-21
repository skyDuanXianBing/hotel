<template>
  <ion-page>
    <ion-content fullscreen class="auth-page">
      <div class="auth-shell">
        <AuthBrandHeader
          show-tabs
          show-language-switcher
          :active-tab="accountTab"
          @select-login="handleAccountTabChange('login')"
          @select-register="handleAccountTabChange('register')"
        />

        <main class="auth-panel">
          <section v-show="accountTab === 'login'" class="auth-account-panel">
            <p v-if="routePreferredTarget" class="auth-workspace-intent">
              {{ routePreferredTarget === 'CLEANER' ? t('auth.login.cleanerIntent') : t('auth.login.pmsIntent') }}
            </p>

            <ion-list lines="none" class="auth-list">
              <div class="auth-field">
                <p class="auth-field-label">{{ t('auth.field.email') }}</p>
                <ion-item :class="['auth-item', { 'auth-item--focused': focusedField === 'email' }]">
                  <ion-input
                    v-model="form.email"
                    autocomplete="email"
                    inputmode="email"
                    :placeholder="t('auth.placeholder.email')"
                    type="email"
                    @ionBlur="handleInputBlur('email')"
                    @ionFocus="handleInputFocus('email')"
                  />
                </ion-item>
              </div>

              <template v-if="loginMode === 'password'">
                <div class="auth-field">
                  <p class="auth-field-label">{{ t('auth.field.password') }}</p>
                  <ion-item :class="['auth-item', { 'auth-item--focused': focusedField === 'password' }]">
                    <ion-input
                      v-model="form.password"
                      autocomplete="current-password"
                      :placeholder="t('auth.placeholder.password')"
                      type="password"
                      @ionBlur="handleInputBlur('password')"
                      @ionFocus="handleInputFocus('password')"
                    />
                  </ion-item>
                </div>

                <div class="auth-secondary-link">
                  <button class="auth-text-button" type="button" @click="handleGoToForgotPassword">
                    {{ t('auth.action.forgotPassword') }}
                  </button>
                </div>
              </template>

              <template v-else>
                <p class="auth-code-hint">{{ t('auth.login.codeHint') }}</p>
              </template>
            </ion-list>

            <label class="auth-agreement-row auth-login-option-row">
              <input
                v-model="form.rememberMe"
                class="auth-agreement-row__native"
                type="checkbox"
              />
              <span class="auth-agreement-row__box" aria-hidden="true">
                <span class="auth-agreement-row__check">✓</span>
              </span>
              <span class="auth-agreement-row__text">{{ rememberMeLabel }}</span>
            </label>
            <label class="auth-agreement-row auth-login-option-row">
              <input
                v-model="form.agreeToTerms"
                class="auth-agreement-row__native"
                type="checkbox"
              />
              <span class="auth-agreement-row__box" aria-hidden="true">
                <span class="auth-agreement-row__check">✓</span>
              </span>
              <span class="auth-agreement-row__text">
                {{ t('auth.login.agreement') }}
              </span>
            </label>

            <ion-button
              v-if="loginMode === 'password'"
              expand="block"
              class="auth-primary-button"
              :disabled="submitting"
              @click="handleLogin"
            >
              <ion-spinner v-if="submitting" name="crescent" />
              <span v-else>{{ t('auth.action.login') }}</span>
            </ion-button>

            <ion-button
              v-else
              expand="block"
              class="auth-primary-button"
              :disabled="isSendCodeButtonDisabled"
              @click="handleSendVerificationCode"
            >
              <ion-spinner v-if="isSendingCode" name="crescent" />
              <span v-else>{{ t('auth.action.sendCode') }}</span>
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
                <ion-label>{{ t('auth.action.codeLogin') }}</ion-label>
              </ion-segment-button>
              <ion-segment-button v-else value="password">
                <ion-label>{{ t('auth.action.passwordLogin') }}</ion-label>
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

      <WorkspaceSelectionModal
        :open="workspaceSelectionOpen"
        :targets="pendingLoginTargets"
        :loading="submitting"
        @dismiss="handleWorkspaceDismiss"
        @select="handlePasswordWorkspaceSelect"
      />
    </ion-content>
  </ion-page>
</template>

<script setup lang="ts">
import {
  IonButton,
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
import { useI18n } from 'vue-i18n'
import { useRoute, useRouter } from 'vue-router'
import { loginByPassword, sendVerificationCode } from '@/api/auth'
import AuthBrandHeader from '@/components/auth/AuthBrandHeader.vue'
import { AUTH_LOGIN_FAILURE_STATUSES } from '@/constants/auth'
import { ROUTE_PATHS } from '@/router/guards'
import { useAuthStore } from '@/stores/auth'
import { useStoreStore } from '@/stores/store'
import { useUserStore } from '@/stores/user'
import type { LoginByPasswordRequest, LoginResponse, LoginTarget } from '@/types/auth'
import { clearAutoLoginCredentials, saveAutoLoginCredentials } from '@/utils/autoLogin'
import {
  applyUnifiedLoginResponse,
  normalizeAvailableLoginTargets,
  normalizePreferredLoginTarget,
} from '@/utils/loginSessionResolver'
import { showErrorToast, showSuccessToast, showWarningToast } from '@/utils/notify'
import { getRequestErrorStatus, isHandledRequestError } from '@/utils/request'
import RegisterPage from './RegisterPage.vue'
import WorkspaceSelectionModal from './WorkspaceSelectionModal.vue'

const PASSWORD_MIN_LENGTH = 6
const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
const { t } = useI18n()

type LoginMode = 'password' | 'code'
type AccountTab = 'login' | 'register'
type FocusedField = 'email' | 'password' | null

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
const registerEmail = ref('')
const workspaceSelectionOpen = ref(false)
const pendingLoginTargets = ref<LoginTarget[]>([])
const pendingPasswordPayload = ref<LoginByPasswordRequest | null>(null)

const form = reactive({
  email: '',
  password: '',
  rememberMe: false,
  agreeToTerms: false,
})

const rememberMeLabel = computed(() => {
  if (loginMode.value === 'password') {
    return t('auth.login.rememberPassword')
  }

  return t('auth.login.rememberCode')
})

const routePreferredTarget = computed(() => {
  return normalizePreferredLoginTarget(route.query.workspace)
})

const hasEmailInput = computed(() => normalizeEmail().length > 0)
const isSendCodeButtonDisabled = computed(() => isSendingCode.value || !hasEmailInput.value)

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
    showWarningToast(t('auth.validation.emailRequired'))
    return ''
  }

  if (!EMAIL_PATTERN.test(email)) {
    showWarningToast(t('auth.validation.emailInvalid'))
    return ''
  }

  return email
}

const validatePasswordLoginForm = (): LoginByPasswordRequest | null => {
  const email = validateEmail()
  const password = form.password

  if (!email) {
    return null
  }

  if (!password) {
    showWarningToast(t('auth.validation.passwordRequired'))
    return null
  }

  if (password.length < PASSWORD_MIN_LENGTH) {
    showWarningToast(t('auth.validation.passwordMin', { min: PASSWORD_MIN_LENGTH }))
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

const requestPasswordLogin = async (payload: LoginByPasswordRequest) => {
  try {
    const response = await loginByPassword(payload)

    if (!response.success || !response.data) {
      return null
    }

    return response.data
  } catch (error) {
    if (isLoginFailureError(error)) {
      return null
    }

    throw error
  }
}

const finishPasswordLogin = async (
  responseData: LoginResponse,
  payload: LoginByPasswordRequest,
) => {
  const sessionResult = applyUnifiedLoginResponse(responseData, {
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
        token: responseData.token,
        preferredLoginTarget: responseData.loginTarget,
      })
    } catch {
      clearAutoLoginCredentials()
    }
  }

  if (sessionResult.target === 'CLEANER') {
    showSuccessToast(t('auth.message.loginSuccess'))
  } else {
    showSuccessToast(t('auth.message.loginSelectStore'))
  }

  await router.replace(sessionResult.redirectPath)
}

const clearPendingWorkspaceSelection = () => {
  workspaceSelectionOpen.value = false
  pendingLoginTargets.value = []
  pendingPasswordPayload.value = null
}

const handleWorkspaceDismiss = () => {
  if (submitting.value) {
    return
  }

  clearPendingWorkspaceSelection()
}

const handlePasswordWorkspaceSelect = async (target: LoginTarget) => {
  const pendingPayload = pendingPasswordPayload.value

  if (!pendingPayload || submitting.value) {
    return
  }

  submitting.value = true

  try {
    const requestPayload = {
      ...pendingPayload,
      preferredLoginTarget: target,
    }
    const responseData = await requestPasswordLogin(requestPayload)

    if (!responseData) {
      showErrorToast(t('auth.message.loginFailed'))
      return
    }

    clearPendingWorkspaceSelection()
    await finishPasswordLogin(responseData, requestPayload)
  } catch (error) {
    if (!isHandledRequestError(error)) {
      showErrorToast(resolveErrorMessage(error, t('auth.message.loginFailed')))
    }
  } finally {
    submitting.value = false
  }
}

const resetModeSpecificFields = () => {
  form.password = ''
  clearPendingWorkspaceSelection()
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
    showWarningToast(t('auth.validation.agreementRequired'))
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
      showErrorToast(response.message || t('auth.message.codeSendFailed'))
      return
    }

    showSuccessToast(t('auth.message.codeSent'))
    const query: Record<string, string> = {
      email,
      rememberMe: form.rememberMe ? '1' : '0',
    }

    if (routePreferredTarget.value) {
      query.workspace = routePreferredTarget.value
    }

    await router.push({
      path: ROUTE_PATHS.loginCodeVerify,
      query,
    })
  } catch (error) {
    if (!isHandledRequestError(error)) {
      showErrorToast(resolveErrorMessage(error, t('auth.message.codeSendFailed')))
    }
  } finally {
    isSendingCode.value = false
  }
}

const handleLogin = async () => {
  if (loginMode.value !== 'password' || submitting.value) {
    return
  }

  if (!form.agreeToTerms) {
    showWarningToast(t('auth.validation.agreementRequired'))
    return
  }

  const payload = validatePasswordLoginForm()
  if (!payload) {
    return
  }

  submitting.value = true

  try {
    const preferredLoginTarget = routePreferredTarget.value
    const requestPayload = preferredLoginTarget
      ? {
          ...payload,
          preferredLoginTarget,
        }
      : payload
    const responseData = await requestPasswordLogin(requestPayload)

    if (!responseData) {
      showErrorToast(t('auth.message.loginFailed'))
      return
    }

    const availableTargets = normalizeAvailableLoginTargets(responseData)

    if (!preferredLoginTarget && availableTargets.length > 1) {
      pendingPasswordPayload.value = payload
      pendingLoginTargets.value = availableTargets
      workspaceSelectionOpen.value = true
      return
    }

    await finishPasswordLogin(responseData, requestPayload)
  } catch (error) {
    if (!isHandledRequestError(error)) {
      showErrorToast(resolveErrorMessage(error, t('auth.message.loginFailed')))
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

<style scoped>
.auth-code-hint {
  margin: -4px 0 22px;
  color: #74777e;
  font-size: 14px;
  line-height: 1.5;
}

.auth-workspace-intent {
  margin: 0 0 18px;
  padding: 10px 12px;
  border-radius: 8px;
  background: #f3f6ff;
  color: #315dbe;
  font-size: 13px;
  line-height: 1.45;
}

.auth-login-option-row {
  margin-bottom: 12px;
}

.auth-login-option-row .auth-agreement-row__native:focus-visible + .auth-agreement-row__box {
  outline: 3px solid rgba(7, 143, 244, 0.2);
  outline-offset: 2px;
}
</style>
