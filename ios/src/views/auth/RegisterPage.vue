<template>
  <ion-page>
    <ion-content fullscreen class="mobile-page auth-page">
      <div class="mobile-stack">
        <section class="mobile-card auth-card">
          <button v-if="step === 'details'" class="auth-back-button" type="button" @click="handleBackToEmailStep">
            <span class="auth-back-button__icon">‹</span>
          </button>

          <div class="auth-card__header" :class="{ 'auth-card__header--details': step === 'details' }">
            <h2 class="mobile-section-title">{{ currentTitle }}</h2>
            <p class="mobile-note auth-card__subtitle">{{ currentSubtitle }}</p>
          </div>

          <ion-list lines="none" class="auth-list">
            <template v-if="step === 'email'">
              <p class="auth-field-label">邮箱</p>
              <ion-item :class="['auth-item', { 'auth-item--focused': focusedField === 'email' }]">
                <ion-input
                  v-model="emailStepForm.email"
                  autocomplete="email"
                  inputmode="email"
                  placeholder="邮箱"
                  type="email"
                  @ionBlur="handleInputBlur('email')"
                  @ionFocus="handleInputFocus('email')"
                />
              </ion-item>

              <ion-button
                expand="block"
                :class="[
                  'auth-submit-button',
                  'auth-submit-button--email-step',
                  { 'auth-submit-button--disabled': sendingRegisterCode },
                ]"
                :disabled="sendingRegisterCode"
                @click="handleContinueRegister"
              >
                <ion-spinner v-if="sendingRegisterCode" name="crescent" />
                <span v-else>继续</span>
              </ion-button>
            </template>

            <template v-else>
              <p class="auth-field-label">邮箱</p>
              <ion-item class="auth-item auth-item--readonly">
                <ion-input :model-value="detailsForm.email" readonly type="email" />
              </ion-item>

              <p class="auth-field-label">品牌名或姓名</p>
              <ion-item :class="['auth-item', { 'auth-item--focused': focusedField === 'displayName' }]">
                <ion-input
                  v-model="detailsForm.displayName"
                  placeholder="输入名称"
                  type="text"
                  @ionBlur="handleInputBlur('displayName')"
                  @ionFocus="handleInputFocus('displayName')"
                />
              </ion-item>

              <p class="auth-field-label">邮箱验证码</p>
              <ion-item :class="['auth-item', { 'auth-item--focused': focusedField === 'verificationCode' }]">
                <ion-input
                  v-model="detailsForm.verificationCode"
                  autocomplete="one-time-code"
                  inputmode="numeric"
                  :maxlength="VERIFICATION_CODE_LENGTH"
                  placeholder="请输入邮箱验证码"
                  type="text"
                  @ionBlur="handleInputBlur('verificationCode')"
                  @ionFocus="handleInputFocus('verificationCode')"
                />
              </ion-item>

              <p class="auth-field-label">密码</p>
              <ion-item :class="['auth-item', { 'auth-item--focused': focusedField === 'password' }]">
                <ion-input
                  v-model="detailsForm.password"
                  autocomplete="new-password"
                  placeholder="字母或数字，8~16位"
                  type="password"
                  @ionBlur="handleInputBlur('password')"
                  @ionFocus="handleInputFocus('password')"
                />
              </ion-item>

              <label class="auth-agreement-row">
                <input v-model="detailsForm.agreeToTerms" class="auth-agreement-row__native" type="checkbox" />
                <span class="auth-agreement-row__box">
                  <span class="auth-agreement-row__check">✓</span>
                </span>
                <span class="auth-agreement-row__text">我已阅读并同意用户协议和隐私协议</span>
              </label>

              <ion-button
                expand="block"
                :class="['auth-submit-button', { 'auth-submit-button--disabled': !canSubmitRegister || submitting }]"
                :disabled="!canSubmitRegister || submitting"
                @click="handleSubmitRegister"
              >
                <ion-spinner v-if="submitting" name="crescent" />
                <span v-else>注册</span>
              </ion-button>
            </template>
          </ion-list>

          <div v-if="step === 'email'" class="auth-footer-link">
            <button class="auth-text-button auth-footer-link__button" type="button" @click="handleGoToLogin">
              已有账号？登录
            </button>
          </div>

        </section>
      </div>
    </ion-content>
  </ion-page>
</template>

<script setup lang="ts">
import { IonButton, IonContent, IonInput, IonItem, IonList, IonPage, IonSpinner } from '@ionic/vue'
import { computed, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { register, sendVerificationCode } from '@/api/auth'
import { ROUTE_PATHS } from '@/router/guards'
import type { RegisterRequest } from '@/types/auth'
import { showErrorToast, showSuccessToast, showWarningToast } from '@/utils/notify'
import { isHandledRequestError } from '@/utils/request'

const PASSWORD_MIN_LENGTH = 8
const PASSWORD_MAX_LENGTH = 16
const VERIFICATION_CODE_LENGTH = 6
const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

type RegisterStep = 'email' | 'details'
type FocusedField = 'email' | 'displayName' | 'verificationCode' | 'password' | null

const router = useRouter()
const route = useRoute()

const step = ref<RegisterStep>('email')
const focusedField = ref<FocusedField>(null)
const sendingRegisterCode = ref(false)
const submitting = ref(false)

const emailStepForm = reactive({
  email: '',
})

const detailsForm = reactive({
  email: '',
  displayName: '',
  verificationCode: '',
  password: '',
  agreeToTerms: false,
})

const currentTitle = computed(() => {
  if (step.value === 'email') {
    return 'Hi 你好！'
  }

  return '欢迎使用Hostex'
})

const currentSubtitle = computed(() => {
  if (step.value === 'email') {
    return '请输入邮箱'
  }

  return '正在创建Hostex账号'
})

const normalizeEmail = (value: string) => value.trim()

const canSubmitRegister = computed(() => {
  return (
    detailsForm.displayName.trim().length > 0 &&
    detailsForm.verificationCode.trim().length === VERIFICATION_CODE_LENGTH &&
    detailsForm.password.trim().length >= PASSWORD_MIN_LENGTH &&
    detailsForm.password.trim().length <= PASSWORD_MAX_LENGTH &&
    detailsForm.agreeToTerms
  )
})

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

  emailStepForm.email = email
  detailsForm.email = email
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
  const email = validateEmail(detailsForm.email)
  const verificationCode = detailsForm.verificationCode.trim()
  const password = detailsForm.password.trim()

  if (!email) {
    return null
  }

  if (!detailsForm.displayName.trim()) {
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

  if (!detailsForm.agreeToTerms) {
    showWarningToast('请先阅读并同意用户协议和隐私协议')
    return null
  }

  return {
    email,
    verificationCode,
    password,
  }
}

const handleContinueRegister = async () => {
  if (sendingRegisterCode.value) {
    return
  }

  const email = validateEmail(emailStepForm.email)
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

    detailsForm.email = email
    step.value = 'details'
    showSuccessToast('验证码已发送，请查收邮箱')
  } catch (error) {
    if (!isHandledRequestError(error)) {
      showErrorToast(error instanceof Error ? error.message : '验证码发送失败')
    }
  } finally {
    sendingRegisterCode.value = false
  }
}

const handleBackToEmailStep = () => {
  step.value = 'email'
}

const handleSubmitRegister = async () => {
  if (submitting.value || !canSubmitRegister.value) {
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
    await router.replace({
      path: ROUTE_PATHS.login,
      query: {
        email: payload.email,
      },
    })
  } catch (error) {
    if (!isHandledRequestError(error)) {
      showErrorToast(error instanceof Error ? error.message : '注册失败')
    }
  } finally {
    submitting.value = false
  }
}

const handleGoToLogin = async () => {
  const email = normalizeEmail(emailStepForm.email)

  await router.push({
    path: ROUTE_PATHS.login,
    query: email ? { email } : undefined,
  })
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
  --padding-top: calc(38px + var(--app-safe-top));
  --padding-bottom: calc(28px + var(--app-safe-bottom));
  --padding-start: 24px;
  --padding-end: 24px;
  display: block;
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
  height: calc(100vh - var(--app-safe-top) - var(--app-safe-bottom) - 74px);
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
  margin: 0 0 28px;
}

.auth-card__header--details {
  margin-top: 8px;
}

.mobile-section-title {
  margin: 0;
  color: #1f2128;
  font-size: clamp(29px, 9vw, 35px);
  font-weight: 400;
  line-height: 1.18;
}

.mobile-section-title::before {
  content: '👋 ';
}

.auth-card__subtitle {
  margin: 12px 0 0;
  color: #8f949e;
  font-size: clamp(16px, 4.8vw, 18px);
  line-height: 1.4;
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
  margin-bottom: 18px;
  --min-height: var(--auth-input-height);
  height: var(--auth-input-height);
  min-height: var(--auth-input-height);
  border: 2px solid #dde5e8;
  border-radius: var(--auth-control-radius);
  box-shadow: none;
  transition: border-color 0.2s ease;
  position: relative;
}

.auth-item--readonly {
  background: #f7f8fa;
}

.auth-item::part(native) {
  min-height: var(--auth-input-height);
  height: var(--auth-input-height);
  padding-top: 0;
  padding-bottom: 0;
  display: flex;
  align-items: center;
}

.auth-item.item-has-focus,
.auth-item--focused {
  border-color: #3484ea;
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

:deep(.auth-item--readonly .native-input),
:deep(.auth-item--readonly input) {
  color: #b8bcc4;
}

.auth-agreement-row {
  display: flex;
  align-items: center;
  gap: 14px;
  margin: 2px 0 20px;
  cursor: pointer;
}

.auth-agreement-row__native {
  position: absolute;
  opacity: 0;
  pointer-events: none;
}

.auth-agreement-row__box {
  width: 30px;
  height: 30px;
  border-radius: 50%;
  border: 2px solid #d6dbe2;
  background: #ffffff;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  transition: all 0.2s ease;
}

.auth-agreement-row__check {
  color: #ffffff;
  font-size: 18px;
  line-height: 1;
  opacity: 0;
  transform: scale(0.8);
  transition: all 0.2s ease;
}

.auth-agreement-row__native:checked + .auth-agreement-row__box {
  border-color: #43d38d;
  background: #43d38d;
}

.auth-agreement-row__native:checked + .auth-agreement-row__box .auth-agreement-row__check {
  opacity: 1;
  transform: scale(1);
}

.auth-agreement-row__text {
  color: #1f2128;
  font-size: clamp(16px, 4.8vw, 18px);
  line-height: 1.4;
}

.auth-submit-button {
  margin-top: 10px;
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

.auth-submit-button--email-step {
  margin-top: 30px;
}

.auth-submit-button--disabled {
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

:deep(.auth-submit-button--disabled.button-disabled) {
  opacity: 1;
}

:deep(.auth-submit-button--disabled.button-disabled .button-native) {
  opacity: 1;
  color: #c6c9d1;
  background: #f9f9f9;
  border-color: #dde5e8;
}

.auth-footer-link {
  margin-top: auto;
  margin-bottom: 2px;
  padding-top: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.auth-footer-link__button {
  color: #3484ea;
  font-size: clamp(16px, 4.8vw, 18px);
  font-weight: 400;
  line-height: 1.24;
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
}

@media (max-width: 480px) {
  .auth-card {
    padding-top: 52px;
  }

  .auth-back-button {
    top: 0;
  }

  .auth-card__header--details {
    margin-top: 4px;
  }

  .auth-footer-link {
    padding-top: 20px;
  }

}
</style>
