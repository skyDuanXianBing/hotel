<template>
  <div class="auth-register-form auth-register-form--single">
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

      <div class="auth-field">
        <p class="auth-field-label">品牌名或姓名</p>
        <ion-item :class="['auth-item', { 'auth-item--focused': focusedField === 'displayName' }]">
          <ion-input
            v-model="form.displayName"
            autocomplete="name"
            placeholder="请输入品牌名或姓名"
            type="text"
            @ionBlur="handleInputBlur('displayName')"
            @ionFocus="handleInputFocus('displayName')"
          />
        </ion-item>
      </div>

      <div class="auth-field">
        <p class="auth-field-label">邮箱验证码</p>
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
              placeholder="请输入邮箱验证码"
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
        <p class="auth-field-label">密码</p>
        <ion-item :class="['auth-item', { 'auth-item--focused': focusedField === 'password' }]">
          <ion-input
            v-model="form.password"
            autocomplete="new-password"
            placeholder="字母或数字，8~16位"
            type="password"
            @ionBlur="handleInputBlur('password')"
            @ionFocus="handleInputFocus('password')"
          />
        </ion-item>
      </div>

      <div class="auth-field">
        <p class="auth-field-label">确认密码</p>
        <ion-item
          :class="['auth-item', { 'auth-item--focused': focusedField === 'confirmPassword' }]"
        >
          <ion-input
            v-model="form.confirmPassword"
            autocomplete="new-password"
            placeholder="请再次输入密码"
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
        <span class="auth-agreement-row__text">我已阅读并同意用户协议和隐私协议</span>
      </label>

      <ion-button
        expand="block"
        class="auth-primary-button"
        :disabled="submitting"
        @click="handleSubmitRegister"
      >
        <ion-spinner v-if="submitting" name="crescent" />
        <span v-else>注册</span>
      </ion-button>
    </ion-list>
  </div>
</template>

<script setup lang="ts">
import { IonButton, IonInput, IonItem, IonList, IonSpinner } from '@ionic/vue'
import { computed, onBeforeUnmount, reactive, ref, watch } from 'vue'
import { register, sendVerificationCode } from '@/api/auth'
import type { RegisterRequest } from '@/types/auth'
import { showErrorToast, showSuccessToast, showWarningToast } from '@/utils/notify'
import { isHandledRequestError } from '@/utils/request'

const PASSWORD_MIN_LENGTH = 8
const PASSWORD_MAX_LENGTH = 16
const VERIFICATION_CODE_LENGTH = 6
const VERIFICATION_CODE_SECONDS = 60
const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

type FocusedField =
  | 'email'
  | 'displayName'
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
  displayName: '',
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
    return `${registerCodeCountdown.value}s 后可重发`
  }

  return '发送验证码'
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
    showWarningToast('请输入邮箱')
    return ''
  }

  if (!EMAIL_PATTERN.test(email)) {
    showWarningToast('请输入正确的邮箱格式')
    return ''
  }

  return email
}

const validateRegisterPayload = (): RegisterRequest | null => {
  const email = validateEmail(form.email)
  const verificationCode = form.verificationCode.trim()
  const password = form.password.trim()
  const confirmPassword = form.confirmPassword.trim()

  if (!email) {
    return null
  }

  if (!form.displayName.trim()) {
    showWarningToast('请输入品牌名或姓名')
    return null
  }

  if (verificationCode.length !== VERIFICATION_CODE_LENGTH) {
    showWarningToast('请输入 6 位邮箱验证码')
    return null
  }

  if (password.length < PASSWORD_MIN_LENGTH || password.length > PASSWORD_MAX_LENGTH) {
    showWarningToast(`密码需为 ${PASSWORD_MIN_LENGTH}-${PASSWORD_MAX_LENGTH} 位`)
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

  if (!form.agreeToTerms) {
    showWarningToast('请先阅读并同意用户协议和隐私协议')
    return null
  }

  // TODO(auth-registration): Send displayName after the backend register DTO can persist it.
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
      showErrorToast(response.message || '验证码发送失败')
      return
    }

    showSuccessToast('验证码已发送，请查收邮箱')
    startRegisterCodeCountdown()
  } catch (error) {
    if (!isHandledRequestError(error)) {
      showErrorToast(error instanceof Error ? error.message : '验证码发送失败')
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
      showErrorToast(response.message || '注册失败')
      return
    }

    showSuccessToast('注册成功，请登录')
    emit('registered', payload.email)

    form.displayName = ''
    form.verificationCode = ''
    form.password = ''
    form.confirmPassword = ''
    form.agreeToTerms = false
  } catch (error) {
    if (!isHandledRequestError(error)) {
      showErrorToast(error instanceof Error ? error.message : '注册失败')
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
