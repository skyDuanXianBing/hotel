<template>
  <ion-page>
    <ion-content fullscreen class="auth-page">
      <div class="auth-shell">
        <AuthBrandHeader />

        <main class="auth-panel auth-panel--standalone">
          <div class="auth-panel__heading">
            <button class="auth-back-title" type="button" @click="handleBack">
              <ion-icon :icon="chevronBackOutline" aria-hidden="true" />
              {{ t('auth.verify.title') }}
            </button>
            <p class="auth-panel__subtitle">
              {{ t('auth.verify.description', { email }) }}
            </p>
          </div>

          <div class="verify-code-row" @click="focusInput(activeIndex)">
            <input
              v-for="(_, index) in verificationDigits"
              :key="index"
              :ref="(element) => setDigitInputRef(element, index)"
              v-model="verificationDigits[index]"
              class="verify-code-input"
              :class="{
                'verify-code-input--active': activeIndex === index,
                'verify-code-input--filled': verificationDigits[index],
              }"
              autocomplete="one-time-code"
              inputmode="numeric"
              maxlength="1"
              type="text"
              @focus="handleDigitFocus(index)"
              @blur="handleDigitBlur(index)"
              @input="handleDigitInput(index, $event)"
              @keydown="handleDigitKeydown(index, $event)"
              @paste="handleDigitPaste(index, $event)"
            />
          </div>

          <ion-button
            expand="block"
            class="resend-button"
            :class="{ 'resend-button--disabled': !canResend || resending }"
            :disabled="!canResend || resending"
            @click="handleResendCode"
          >
            <ion-spinner v-if="resending" name="crescent" />
            <span v-else>{{ resendButtonText }}</span>
          </ion-button>
        </main>
      </div>

      <WorkspaceSelectionModal
        :open="workspaceSelectionOpen"
        :targets="pendingLoginTargets"
        :loading="submitting"
        @dismiss="handleWorkspaceDismiss"
        @select="handleWorkspaceSelect"
      />
    </ion-content>
  </ion-page>
</template>

<script setup lang="ts">
import { IonButton, IonContent, IonIcon, IonPage, IonSpinner, onIonViewDidEnter } from '@ionic/vue'
import { chevronBackOutline } from 'ionicons/icons'
import { computed, nextTick, onBeforeUnmount, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute, useRouter } from 'vue-router'
import { loginByCode, sendVerificationCode } from '@/api/auth'
import AuthBrandHeader from '@/components/auth/AuthBrandHeader.vue'
import { ROUTE_PATHS } from '@/router/guards'
import { useAuthStore } from '@/stores/auth'
import { useStoreStore } from '@/stores/store'
import { useUserStore } from '@/stores/user'
import type { LoginResponse, LoginTarget } from '@/types/auth'
import { clearAutoLoginCredentials } from '@/utils/autoLogin'
import {
  applyUnifiedLoginResponse,
  normalizeAvailableLoginTargets,
  normalizePreferredLoginTarget,
  selectLoginTargetFromAuthorizedResponse,
} from '@/utils/loginSessionResolver'
import { showErrorToast, showSuccessToast, showWarningToast } from '@/utils/notify'
import { isHandledRequestError } from '@/utils/request'
import WorkspaceSelectionModal from './WorkspaceSelectionModal.vue'

const CODE_LENGTH = 6
const RESEND_SECONDS = 60
const { t } = useI18n()

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()
const userStore = useUserStore()
const storeStore = useStoreStore()

const email = computed(() => String(route.query.email ?? '').trim())
const rememberMe = computed(() => route.query.rememberMe === '1')
const routePreferredTarget = computed(() => {
  return normalizePreferredLoginTarget(route.query.workspace)
})
const verificationDigits = ref<string[]>(Array.from({ length: CODE_LENGTH }, () => ''))
const digitInputRefs = ref<Array<HTMLInputElement | null>>(Array.from({ length: CODE_LENGTH }, () => null))
const activeIndex = ref(0)
const submitting = ref(false)
const resending = ref(false)
const resendCountdown = ref(RESEND_SECONDS)
const workspaceSelectionOpen = ref(false)
const pendingLoginTargets = ref<LoginTarget[]>([])
const pendingLoginResponse = ref<LoginResponse | null>(null)

let countdownTimer: ReturnType<typeof setInterval> | null = null

const verificationCode = computed(() => verificationDigits.value.join(''))
const canResend = computed(() => resendCountdown.value === 0)
const resendButtonText = computed(() => {
  if (canResend.value) {
    return t('auth.action.resend')
  }

  return t('auth.action.resendCountdown', { seconds: resendCountdown.value })
})

const hydrateRuntimeStores = () => {
  authStore.hydrate()
  userStore.hydrate()
  storeStore.hydrate()
}

const setDigitInputRef = (element: unknown, index: number) => {
  digitInputRefs.value[index] = element instanceof HTMLInputElement ? element : null
}

const focusInput = async (index: number) => {
  const nextIndex = Math.max(0, Math.min(index, CODE_LENGTH - 1))
  activeIndex.value = nextIndex

  await nextTick()
  digitInputRefs.value[nextIndex]?.focus()
  digitInputRefs.value[nextIndex]?.select()
}

const focusFirstEmptyInput = async () => {
  const emptyIndex = verificationDigits.value.findIndex((digit) => !digit)
  await focusInput(emptyIndex === -1 ? CODE_LENGTH - 1 : emptyIndex)
}

const clearVerificationDigits = () => {
  verificationDigits.value = Array.from({ length: CODE_LENGTH }, () => '')
}

const clearPendingWorkspaceSelection = () => {
  workspaceSelectionOpen.value = false
  pendingLoginTargets.value = []
  pendingLoginResponse.value = null
}

const startResendCountdown = () => {
  if (countdownTimer) {
    clearInterval(countdownTimer)
  }

  resendCountdown.value = RESEND_SECONDS
  countdownTimer = setInterval(() => {
    if (resendCountdown.value <= 1) {
      resendCountdown.value = 0
      if (countdownTimer) {
        clearInterval(countdownTimer)
        countdownTimer = null
      }
      return
    }

    resendCountdown.value -= 1
  }, 1000)
}

const stopResendCountdown = () => {
  if (countdownTimer) {
    clearInterval(countdownTimer)
    countdownTimer = null
  }
}

const validateEmail = () => {
  if (!email.value) {
    showWarningToast(t('auth.validation.emailEmpty'))
    return false
  }

  return true
}

const finishCodeLogin = async (responseData: LoginResponse) => {
  clearAutoLoginCredentials()
  const sessionResult = applyUnifiedLoginResponse(responseData, {
    resetPmsCurrentStore: true,
  })
  hydrateRuntimeStores()
  clearPendingWorkspaceSelection()

  if (sessionResult.target === 'CLEANER') {
    showSuccessToast(t('auth.message.loginSuccess'))
  } else {
    showSuccessToast(t('auth.message.loginSelectStore'))
  }

  await router.replace(sessionResult.redirectPath)
}

const handleWorkspaceDismiss = () => {
  if (submitting.value) {
    return
  }

  clearPendingWorkspaceSelection()
}

const handleWorkspaceSelect = async (target: LoginTarget) => {
  const responseData = pendingLoginResponse.value

  if (!responseData || submitting.value) {
    return
  }

  submitting.value = true

  try {
    const selectedResponse = selectLoginTargetFromAuthorizedResponse(responseData, target)
    await finishCodeLogin(selectedResponse)
  } catch (error) {
    if (!isHandledRequestError(error)) {
      showErrorToast(error instanceof Error ? error.message : t('auth.message.loginFailed'))
    }
  } finally {
    submitting.value = false
  }
}

const handleBack = async () => {
  if (window.history.length > 1) {
    await router.back()
    return
  }

  await router.replace({
    path: ROUTE_PATHS.login,
    query: {
      ...(email.value ? { email: email.value } : {}),
      ...(routePreferredTarget.value ? { workspace: routePreferredTarget.value } : {}),
    },
  })
}

const handleDigitFocus = (index: number) => {
  activeIndex.value = index
}

const handleDigitBlur = (index: number) => {
  if (activeIndex.value === index) {
    activeIndex.value = verificationDigits.value.findIndex((digit) => !digit)
    if (activeIndex.value === -1) {
      activeIndex.value = CODE_LENGTH - 1
    }
  }
}

const trySubmitLogin = async () => {
  if (submitting.value || verificationCode.value.length !== CODE_LENGTH) {
    return
  }

  if (!validateEmail()) {
    return
  }

  submitting.value = true

  try {
    const response = await loginByCode({
      email: email.value,
      verificationCode: verificationCode.value,
      rememberMe: rememberMe.value,
      preferredLoginTarget: routePreferredTarget.value,
    })

    if (!response.success || !response.data) {
      showErrorToast(response.message || t('auth.message.loginFailed'))
      clearVerificationDigits()
      await focusInput(0)
      return
    }

    const availableTargets = normalizeAvailableLoginTargets(response.data)

    if (!routePreferredTarget.value && availableTargets.length > 1) {
      pendingLoginTargets.value = availableTargets
      pendingLoginResponse.value = response.data
      workspaceSelectionOpen.value = true
      return
    }

    await finishCodeLogin(response.data)
  } catch (error) {
    clearVerificationDigits()
    await focusInput(0)

    if (!isHandledRequestError(error)) {
      showErrorToast(error instanceof Error ? error.message : t('auth.message.loginFailed'))
    }
  } finally {
    submitting.value = false
  }
}

const handleDigitInput = async (index: number, event: Event) => {
  const input = event.target as HTMLInputElement | null
  const rawValue = input?.value ?? verificationDigits.value[index] ?? ''
  const normalized = rawValue.replace(/\D/g, '')

  if (!normalized) {
    verificationDigits.value[index] = ''
    return
  }

  const digits = normalized.slice(0, CODE_LENGTH - index).split('')

  digits.forEach((digit, offset) => {
    verificationDigits.value[index + offset] = digit
  })

  const nextIndex = index + digits.length

  if (nextIndex < CODE_LENGTH) {
    await focusInput(nextIndex)
    return
  }

  await focusInput(CODE_LENGTH - 1)
  await trySubmitLogin()
}

const handleDigitKeydown = async (index: number, event: KeyboardEvent) => {
  if (event.key === 'Backspace') {
    if (verificationDigits.value[index]) {
      verificationDigits.value[index] = ''
      return
    }

    if (index > 0) {
      verificationDigits.value[index - 1] = ''
      event.preventDefault()
      await focusInput(index - 1)
    }
    return
  }

  if (event.key === 'ArrowLeft' && index > 0) {
    event.preventDefault()
    await focusInput(index - 1)
    return
  }

  if (event.key === 'ArrowRight' && index < CODE_LENGTH - 1) {
    event.preventDefault()
    await focusInput(index + 1)
    return
  }

  if (event.key.length === 1 && !/\d/.test(event.key)) {
    event.preventDefault()
  }
}

const handleDigitPaste = async (index: number, event: ClipboardEvent) => {
  event.preventDefault()
  const pastedText = event.clipboardData?.getData('text') ?? ''
  const digits = pastedText.replace(/\D/g, '').slice(0, CODE_LENGTH - index).split('')

  if (!digits.length) {
    return
  }

  digits.forEach((digit, offset) => {
    verificationDigits.value[index + offset] = digit
  })

  const nextIndex = Math.min(index + digits.length, CODE_LENGTH - 1)
  await focusInput(nextIndex)

  if (verificationCode.value.length === CODE_LENGTH) {
    await trySubmitLogin()
  }
}

const handleResendCode = async () => {
  if (resending.value || !canResend.value) {
    return
  }

  if (!validateEmail()) {
    return
  }

  resending.value = true

  try {
    const response = await sendVerificationCode({
      email: email.value,
      type: 'login',
    })

    if (!response.success) {
      showErrorToast(response.message || t('auth.message.codeSendFailed'))
      return
    }

    clearVerificationDigits()
    startResendCountdown()
    showSuccessToast(t('auth.message.codeResent'))
    await focusInput(0)
  } catch (error) {
    if (!isHandledRequestError(error)) {
      showErrorToast(error instanceof Error ? error.message : t('auth.message.codeSendFailed'))
    }
  } finally {
    resending.value = false
  }
}

onIonViewDidEnter(async () => {
  startResendCountdown()
  await focusFirstEmptyInput()
})

onBeforeUnmount(() => {
  stopResendCountdown()
})
</script>
