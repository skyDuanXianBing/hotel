<template>
  <div class="auth-register-form auth-register-form--single">
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

      <div class="auth-field">
        <p class="auth-field-label">{{ t('auth.field.emailCode') }}</p>
        <div class="auth-code-row">
          <ion-item
            :class="[
              'auth-item',
              { 'auth-item--focused': focusedField === 'verificationCode' },
            ]"
          >
            <ion-input
              v-model="form.verificationCode"
              autocomplete="one-time-code"
              inputmode="numeric"
              :maxlength="VERIFICATION_CODE_LENGTH"
              :placeholder="t('auth.placeholder.emailCode')"
              type="text"
              @ionBlur="handleInputBlur('verificationCode')"
              @ionFocus="handleInputFocus('verificationCode')"
            />
          </ion-item>

          <ion-button
            fill="outline"
            class="auth-code-action"
            :disabled="isSendCodeButtonDisabled"
            @click="handleSendRegisterCode"
          >
            <ion-spinner v-if="sendingRegisterCode" name="crescent" />
            <span v-else>{{ sendCodeButtonLabel }}</span>
          </ion-button>
        </div>
      </div>

      <div class="auth-field">
        <p class="auth-field-label">{{ t('auth.field.password') }}</p>
        <ion-item :class="['auth-item', { 'auth-item--focused': focusedField === 'password' }]">
          <ion-input
            v-model="form.password"
            autocomplete="new-password"
            :placeholder="t('auth.placeholder.registerPassword')"
            type="password"
            @ionBlur="handleInputBlur('password')"
            @ionFocus="handleInputFocus('password')"
          />
        </ion-item>
      </div>

      <div class="auth-field">
        <p class="auth-field-label">{{ t('auth.field.confirmPassword') }}</p>
        <ion-item
          :class="['auth-item', { 'auth-item--focused': focusedField === 'confirmPassword' }]"
        >
          <ion-input
            v-model="form.confirmPassword"
            autocomplete="new-password"
            :placeholder="t('auth.placeholder.confirmPassword')"
            type="password"
            @ionBlur="handleInputBlur('confirmPassword')"
            @ionFocus="handleInputFocus('confirmPassword')"
          />
        </ion-item>
      </div>

      <label class="auth-agreement-row">
        <input v-model="form.agreeToTerms" class="auth-agreement-row__native" type="checkbox" />
        <span class="auth-agreement-row__box">
          <span class="auth-agreement-row__check">✓</span>
        </span>
        <span class="auth-agreement-row__text">{{ t('auth.register.agreement') }}</span>
      </label>

      <ion-button
        expand="block"
        class="auth-primary-button"
        :disabled="submitting"
        @click="handleSubmitRegister"
      >
        <ion-spinner v-if="submitting" name="crescent" />
        <span v-else>{{ t('auth.action.register') }}</span>
      </ion-button>
    </ion-list>
  </div>
</template>

<script setup lang="ts">
import { IonButton, IonInput, IonItem, IonList, IonSpinner } from '@ionic/vue'
import { computed, onBeforeUnmount, reactive, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { register, sendVerificationCode } from '@/api/auth'
import type { RegisterRequest } from '@/types/auth'
import { showErrorToast, showSuccessToast, showWarningToast } from '@/utils/notify'
import { isHandledRequestError } from '@/utils/request'

const PASSWORD_MIN_LENGTH = 6
const PASSWORD_MAX_LENGTH = 20
const VERIFICATION_CODE_LENGTH = 6
const VERIFICATION_CODE_SECONDS = 60
const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
const { t } = useI18n()

type FocusedField =
  | 'email'
  | 'verificationCode'
  | 'password'
  | 'confirmPassword'
  | null

const props = withDefaults(
  defineProps<{
    initialEmail?: string
  }>(),
  {
    initialEmail: '',
  },
)

const emit = defineEmits<{
  (event: 'email-change', email: string): void
  (event: 'registered', email: string): void
}>()

const focusedField = ref<FocusedField>(null)
const sendingRegisterCode = ref(false)
const registerCodeCountdown = ref(0)
const submitting = ref(false)
let registerCodeCountdownTimer: ReturnType<typeof setInterval> | null = null

const form = reactive({
  email: '',
  verificationCode: '',
  password: '',
  confirmPassword: '',
  agreeToTerms: false,
})

const normalizeEmail = (value: string) => value.trim()

const hasEmailInput = computed(() => normalizeEmail(form.email).length > 0)
const isSendCodeButtonDisabled = computed(
  () => sendingRegisterCode.value || registerCodeCountdown.value > 0 || !hasEmailInput.value,
)
const sendCodeButtonLabel = computed(() => {
  if (registerCodeCountdown.value > 0) {
    return t('auth.action.resendAfter', { seconds: registerCodeCountdown.value })
  }

  return t('auth.action.sendCode')
})

const clearRegisterCodeCountdown = () => {
  if (registerCodeCountdownTimer === null) {
    return
  }

  clearInterval(registerCodeCountdownTimer)
  registerCodeCountdownTimer = null
}

const startRegisterCodeCountdown = () => {
  clearRegisterCodeCountdown()
  registerCodeCountdown.value = VERIFICATION_CODE_SECONDS

  registerCodeCountdownTimer = setInterval(() => {
    if (registerCodeCountdown.value <= 1) {
      registerCodeCountdown.value = 0
      clearRegisterCodeCountdown()
      return
    }

    registerCodeCountdown.value -= 1
  }, 1000)
}

const handleInputFocus = (field: Exclude<FocusedField, null>) => {
  focusedField.value = field
}

const handleInputBlur = (field: Exclude<FocusedField, null>) => {
  if (focusedField.value === field) {
    focusedField.value = null
  }
}

const applyEmailPrefill = (emailQuery: unknown) => {
  if (typeof emailQuery !== 'string') {
    return
  }

  const email = normalizeEmail(emailQuery)
  if (!email) {
    return
  }

  form.email = email
}

const validateEmail = (emailValue: string) => {
  const email = normalizeEmail(emailValue)

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

const validateRegisterPayload = (): RegisterRequest | null => {
  const email = validateEmail(form.email)
  const verificationCode = form.verificationCode.trim()
  const password = form.password
  const confirmPassword = form.confirmPassword

  if (!email) {
    return null
  }

  if (verificationCode.length !== VERIFICATION_CODE_LENGTH) {
    showWarningToast(t('auth.validation.emailCodeLength', { length: VERIFICATION_CODE_LENGTH }))
    return null
  }

  if (password.length < PASSWORD_MIN_LENGTH || password.length > PASSWORD_MAX_LENGTH) {
    showWarningToast(t('auth.validation.passwordRange', { min: PASSWORD_MIN_LENGTH, max: PASSWORD_MAX_LENGTH }))
    return null
  }

  if (!confirmPassword) {
    showWarningToast(t('auth.validation.confirmPasswordRequired'))
    return null
  }

  if (password !== confirmPassword) {
    showWarningToast(t('auth.validation.passwordMismatch'))
    return null
  }

  if (!form.agreeToTerms) {
    showWarningToast(t('auth.validation.registerAgreementRequired'))
    return null
  }

  return {
    email,
    verificationCode,
    password,
  }
}

const handleSendRegisterCode = async () => {
  if (sendingRegisterCode.value || registerCodeCountdown.value > 0) {
    return
  }

  const email = validateEmail(form.email)
  if (!email) {
    return
  }

  sendingRegisterCode.value = true

  try {
    const response = await sendVerificationCode({
      email,
      type: 'register',
    })

    if (!response.success) {
      showErrorToast(response.message || t('auth.message.codeSendFailed'))
      return
    }

    showSuccessToast(t('auth.message.codeSent'))
    startRegisterCodeCountdown()
  } catch (error) {
    if (!isHandledRequestError(error)) {
      showErrorToast(error instanceof Error ? error.message : t('auth.message.codeSendFailed'))
    }
  } finally {
    sendingRegisterCode.value = false
  }
}

const handleSubmitRegister = async () => {
  if (submitting.value) {
    return
  }

  const payload = validateRegisterPayload()
  if (!payload) {
    return
  }

  submitting.value = true

  try {
    const response = await register(payload)

    if (!response.success) {
      showErrorToast(response.message || t('auth.message.registerFailed'))
      return
    }

    showSuccessToast(t('auth.message.registerSuccess'))
    emit('registered', payload.email)

    form.verificationCode = ''
    form.password = ''
    form.confirmPassword = ''
    form.agreeToTerms = false
  } catch (error) {
    if (!isHandledRequestError(error)) {
      showErrorToast(error instanceof Error ? error.message : t('auth.message.registerFailed'))
    }
  } finally {
    submitting.value = false
  }
}

watch(
  () => props.initialEmail,
  (email) => {
    applyEmailPrefill(email)
  },
  {
    immediate: true,
  },
)

watch(
  () => form.email,
  (email) => {
    emit('email-change', email)
  },
)

onBeforeUnmount(() => {
  clearRegisterCodeCountdown()
})
</script>
