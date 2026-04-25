<template>
  <ion-page>
    <ion-content fullscreen class="mobile-page auth-page">
      <div class="mobile-stack">
        <section class="mobile-card auth-card">
          <button class="auth-back-button" type="button" @click="handleBack">
            <span class="auth-back-button__icon">‹</span>
          </button>

          <div class="auth-card__header">
            <h2 class="mobile-section-title">验证邮箱</h2>
            <p class="mobile-note auth-card__subtitle">
              请输入发送至
              <strong>{{ email }}</strong>
              的9位验证码，有效期10分钟，如未收到，请尝试重新获取验证码
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
        </section>
      </div>
    </ion-content>
  </ion-page>
</template>

<script setup lang="ts">
import { IonButton, IonContent, IonPage, IonSpinner, onIonViewDidEnter } from '@ionic/vue'
import { computed, nextTick, onBeforeUnmount, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { loginByCode, sendVerificationCode } from '@/api/auth'
import { ROUTE_PATHS } from '@/router/guards'
import { useAuthStore } from '@/stores/auth'
import { useStoreStore } from '@/stores/store'
import { useUserStore } from '@/stores/user'
import type { LoginResponse } from '@/types/auth'
import { showErrorToast, showSuccessToast, showWarningToast } from '@/utils/notify'
import { isHandledRequestError } from '@/utils/request'

const CODE_LENGTH = 9
const RESEND_SECONDS = 60

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()
const userStore = useUserStore()
const storeStore = useStoreStore()

const email = computed(() => String(route.query.email ?? '').trim())
const verificationDigits = ref<string[]>(Array.from({ length: CODE_LENGTH }, () => ''))
const digitInputRefs = ref<Array<HTMLInputElement | null>>(Array.from({ length: CODE_LENGTH }, () => null))
const activeIndex = ref(0)
const submitting = ref(false)
const resending = ref(false)
const resendCountdown = ref(RESEND_SECONDS)

let countdownTimer: ReturnType<typeof setInterval> | null = null

const verificationCode = computed(() => verificationDigits.value.join(''))
const canResend = computed(() => resendCountdown.value === 0)
const resendButtonText = computed(() => {
  if (canResend.value) {
    return '重新获取'
  }

  return `重新获取 (${resendCountdown.value}s)`
})

const persistLoginSession = (payload: LoginResponse) => {
  authStore.setToken(payload.token)
  userStore.setUser(payload.user)
  storeStore.setStores(payload.stores ?? [])
  storeStore.setCurrentStore(null)
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
    showWarningToast('邮箱不能为空')
    return false
  }

  return true
}

const handleBack = async () => {
  if (window.history.length > 1) {
    await router.back()
    return
  }

  await router.replace({
    path: ROUTE_PATHS.login,
    query: email.value ? { email: email.value } : undefined,
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
      rememberMe: true,
    })

    if (!response.success || !response.data) {
      showErrorToast(response.message || '登录失败')
      clearVerificationDigits()
      await focusInput(0)
      return
    }

    persistLoginSession(response.data)
    showSuccessToast('登录成功，请选择门店')
    await router.replace(ROUTE_PATHS.storeSelection)
  } catch (error) {
    clearVerificationDigits()
    await focusInput(0)

    if (!isHandledRequestError(error)) {
      showErrorToast(error instanceof Error ? error.message : '登录失败')
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
      showErrorToast(response.message || '验证码发送失败')
      return
    }

    clearVerificationDigits()
    startResendCountdown()
    showSuccessToast('验证码已重新发送，请查收邮箱')
    await focusInput(0)
  } catch (error) {
    if (!isHandledRequestError(error)) {
      showErrorToast(error instanceof Error ? error.message : '验证码发送失败')
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

<style scoped>
.auth-page {
  --auth-control-radius: 13px;
  --background: #fcfcfc;
  --padding-top: calc(38px + var(--app-safe-top));
  --padding-bottom: calc(28px + var(--app-safe-bottom));
  --padding-start: 24px;
  --padding-end: 24px;
  color: #1f2128;
}

.mobile-stack {
  min-height: calc(100vh - var(--app-safe-top) - var(--app-safe-bottom) - 72px);
  overflow: hidden;
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
  position: relative;
  overflow: hidden;
}

.auth-back-button {
  position: absolute;
  top: 4px;
  left: -2px;
  width: 36px;
  height: 36px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border: 0;
  padding: 0;
  background: transparent;
  color: #1f2128;
}

.auth-back-button__icon {
  font-size: 48px;
  line-height: 1;
  transform: translateY(-2px);
}

.auth-card__header {
  margin: 82px 0 56px;
}

.mobile-section-title {
  margin: 0;
  color: #1f2128;
  font-size: clamp(35px, 11.2vw, 55px);
  font-weight: 600;
  line-height: 1.08;
  letter-spacing: -0.03em;
}

.auth-card__subtitle {
  margin: 30px 0 0;
  color: #8f949e;
  font-size: clamp(19px, 5.6vw, 24px);
  line-height: 1.5;
}

.auth-card__subtitle strong {
  color: #1f2128;
  font-weight: 700;
}

.verify-code-row {
  display: flex;
  align-items: stretch;
  justify-content: space-between;
  gap: 10px;
  margin-top: 8px;
}

.verify-code-input {
  flex: 1 1 0;
  width: 0;
  min-width: 0;
  aspect-ratio: 1 / 1;
  min-height: 48px;
  border: 2px solid #eef2f5;
  border-radius: 13px;
  background: #ffffff;
  box-shadow: 0 2px 8px rgba(31, 33, 40, 0.02);
  color: #1f2128;
  text-align: center;
  font-size: clamp(20px, 6.8vw, 30px);
  font-weight: 600;
  line-height: 1;
  padding: 0;
  outline: none;
  transition: border-color 0.2s ease, box-shadow 0.2s ease, color 0.2s ease;
  caret-color: transparent;
}

.verify-code-input--active {
  border-color: #3484ea;
  box-shadow: none;
}

.verify-code-input--filled {
  color: #1f2128;
}

.resend-button {
  margin-top: 46px;
  min-height: clamp(54px, 13.5vw, 64px);
  font-size: clamp(21px, 5.8vw, 24px);
  font-weight: 400;
  letter-spacing: 0;
  --border-radius: 13px;
  --background: #3484ea;
  --background-activated: #2e77d4;
  --background-focused: #2e77d4;
  --background-hover: #2e77d4;
  --box-shadow: none;
  --color: #ffffff;
}

.resend-button--disabled {
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

:deep(.resend-button--disabled.button-disabled) {
  opacity: 1;
}

:deep(.resend-button--disabled.button-disabled .button-native) {
  opacity: 1;
  color: #c6c9d1;
  background: #f9f9f9;
  border-color: #dde5e8;
}

@media (min-width: 768px) {
  .auth-page {
    --padding-top: calc(78px + var(--app-safe-top));
    --padding-start: 56px;
    --padding-end: 56px;
  }

  .auth-card {
    margin: 0 auto;
  }

  .verify-code-row {
    gap: 14px;
  }
}

@media (max-width: 480px) {
  .auth-card {
    padding-top: 52px;
  }

  .auth-card__header {
    margin-top: 62px;
    margin-bottom: 42px;
  }

  .auth-card__subtitle {
    margin-top: 22px;
  }

  .verify-code-row {
    gap: 8px;
  }

  .resend-button {
    margin-top: 40px;
  }
}
</style>
