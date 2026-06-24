<template>
  <div class="login-container">
    <div class="login-layout">
      <div class="left-section">
        <img
          :src="loginIllustration"
          alt=""
          aria-hidden="true"
          class="left-illustration"
        />
      </div>

      <div class="right-section">
        <div class="language-selector">
          <LanguageSwitcher variant="auth" />
        </div>

        <div class="form-shell">
          <div class="form-container">
            <h2 class="form-title">
              {{ loginMode === 'password' ? t('auth.login.title.password') : t('auth.login.title.code') }}
            </h2>
            <p class="form-subtitle">
              {{
                loginMode === 'password'
                  ? t('auth.login.subtitle.password')
                  : t('auth.login.subtitle.code')
              }}
            </p>

            <el-form ref="loginFormRef" :model="loginForm" :rules="loginRules" class="login-form">
              <div class="form-item">
                <label class="form-label">{{ t('common.email') }}</label>
                <el-form-item prop="email">
                  <el-input
                    v-model="loginForm.email"
                    :placeholder="t('auth.login.emailPlaceholder')"
                    size="large"
                    :prefix-icon="Message"
                  />
                </el-form-item>
              </div>

              <template v-if="loginMode === 'password'">
                <div class="form-item">
                  <div class="label-row">
                    <label class="form-label">{{ t('common.password') }}</label>
                    <el-link
                      type="primary"
                      :underline="false"
                      @click="goToForgotPassword"
                      class="forgot-link"
                    >
                      {{ t('auth.login.forgotPassword') }}
                    </el-link>
                  </div>
                  <el-form-item prop="password">
                    <el-input
                      v-model="loginForm.password"
                      type="password"
                      :placeholder="t('auth.login.passwordPlaceholder')"
                      size="large"
                      :prefix-icon="Lock"
                      show-password
                    />
                  </el-form-item>
                </div>

                <div class="options-row">
                  <el-checkbox v-model="loginForm.rememberMe">{{ t('auth.login.rememberPassword') }}</el-checkbox>
                  <el-link
                    type="primary"
                    :underline="false"
                    @click="switchLoginMode"
                    class="switch-mode-link"
                  >
                    {{ t('auth.login.switchToCode') }}
                  </el-link>
                </div>
              </template>

              <template v-else>
                <div class="form-item">
                  <label class="form-label">{{ t('common.verificationCode') }}</label>
                  <el-form-item prop="verificationCode">
                    <div class="verification-code-field">
                      <el-input
                        v-model="loginForm.verificationCode"
                        class="verification-code-input"
                        :placeholder="t('auth.login.codePlaceholder')"
                        size="large"
                        :prefix-icon="Key"
                      />
                      <el-button
                        class="send-code-button"
                        :disabled="countdown > 0"
                        @click="sendVerificationCode"
                      >
                        {{ countdown > 0 ? `${countdown}s` : t('auth.actions.sendCode') }}
                      </el-button>
                    </div>
                  </el-form-item>
                </div>

                <div class="options-row">
                  <el-checkbox v-model="loginForm.rememberMe">{{ t('auth.login.rememberSession') }}</el-checkbox>
                  <el-link
                    type="primary"
                    :underline="false"
                    @click="switchLoginMode"
                    class="switch-mode-link"
                  >
                    {{ t('auth.login.switchToPassword') }}
                  </el-link>
                </div>
              </template>

              <div class="agreement-row">
                <el-checkbox v-model="loginForm.agreeToTerms">
                  {{ t('auth.login.agreementPrefix') }}
                  <el-link
                    type="primary"
                    :underline="false"
                    @click.stop.prevent="goToTermsOfService"
                  >
                    {{ t('common.termsOfService') }}
                  </el-link>
                  {{ t('auth.login.agreementJoiner') }}
                  <el-link type="primary" :underline="false" @click.stop.prevent="goToPrivacyPolicy">
                    {{ t('common.privacyPolicy') }}
                  </el-link>
                </el-checkbox>
              </div>

              <el-button
                type="primary"
                size="large"
                class="login-button"
                :loading="loading"
                @click="handleLogin"
              >
                {{ t('auth.login.submit') }}
              </el-button>
            </el-form>

            <div class="form-footer">
              <div class="register-link">
                {{ t('auth.login.registerPrefix') }}
                <el-link type="primary" :underline="false" @click="goToRegister">
                  {{ t('auth.login.registerAction') }}
                </el-link>
              </div>
              <div class="support-link-row">
                <el-link type="primary" :underline="false" @click="goToTechnicalSupport">
                  {{ t('common.supportSite') }}
                </el-link>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Message, Lock, Key } from '@element-plus/icons-vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { useI18n } from 'vue-i18n'
import LanguageSwitcher from '@/components/LanguageSwitcher.vue'
import loginIllustration from '@/assets/auth/login-illustration.svg'
import {
  loginByPassword as loginByPasswordAPI,
  loginByCode as loginByCodeAPI,
  sendVerificationCode as sendVerificationCodeAPI,
} from '@/api/auth'
import { resolveLoginTarget } from '@/utils/loginTargetResolver'

const router = useRouter()
const { t } = useI18n()

const loginMode = ref<'password' | 'code'>('password')
const countdown = ref(0)
const loading = ref(false)
const loginFormRef = ref<FormInstance>()

const loginForm = reactive({
  email: '',
  password: '',
  verificationCode: '',
  rememberMe: false,
  agreeToTerms: false,
})

const loginRules = computed<FormRules>(() => ({
  email: [
    { required: true, message: t('auth.login.validation.emailRequired'), trigger: 'blur' },
    { type: 'email', message: t('auth.login.validation.emailInvalid'), trigger: 'blur' },
  ],
  password: [
    { required: true, message: t('auth.login.validation.passwordRequired'), trigger: 'blur' },
    { min: 6, message: t('auth.login.validation.passwordMin'), trigger: 'blur' },
  ],
  verificationCode: [
    { required: true, message: t('auth.login.validation.codeRequired'), trigger: 'blur' },
    { len: 6, message: t('auth.login.validation.codeLength'), trigger: 'blur' },
  ],
}))

const switchLoginMode = () => {
  loginMode.value = loginMode.value === 'password' ? 'code' : 'password'
  loginFormRef.value?.clearValidate()
}

const sendVerificationCode = async () => {
  try {
    await loginFormRef.value?.validateField('email')
  } catch {
    return
  }

  try {
    await sendVerificationCodeAPI({
      email: loginForm.email,
      type: 'login',
    })

    ElMessage.success(t('auth.login.codeSent'))

    countdown.value = 60
    const timer = setInterval(() => {
      countdown.value--
      if (countdown.value <= 0) {
        clearInterval(timer)
      }
    }, 1000)
  } catch (error: any) {
    const message = error.response?.data?.message || error.message || t('auth.login.failed')
    ElMessage.error(message)
  }
}

const handleLogin = async () => {
  if (!loginForm.agreeToTerms) {
    ElMessage.warning(t('auth.login.agreementRequired'))
    return
  }

  try {
    await loginFormRef.value?.validate()

    loading.value = true

    let response: any
    if (loginMode.value === 'password') {
      response = await loginByPasswordAPI({
        email: loginForm.email,
        password: loginForm.password,
        rememberMe: loginForm.rememberMe,
      })
    } else {
      response = await loginByCodeAPI({
        email: loginForm.email,
        verificationCode: loginForm.verificationCode,
        rememberMe: loginForm.rememberMe,
      })
    }

    if (!response.success || !response.data) {
      ElMessage.error(response.message || t('auth.login.failed'))
      return
    }

    await resolveLoginTarget(response.data, router)
    ElMessage.success(t('auth.login.success'))
  } catch (error: any) {
    const message = error.response?.data?.message || error.message || t('auth.login.failed')
    ElMessage.error(message)
  } finally {
    loading.value = false
  }
}

const goToForgotPassword = () => {
  router.push('/forgot-password')
}

const goToRegister = () => {
  router.push('/register')
}

const goToTermsOfService = () => {
  router.push('/legal/terms')
}

const goToPrivacyPolicy = () => {
  router.push('/legal/privacy')
}

const goToTechnicalSupport = () => {
  router.push('/legal/support')
}
</script>

<style scoped>
.login-container {
  width: 100%;
  min-height: 100vh;
  display: flex;
  background: #fff;
}

.login-layout {
  width: 100%;
  min-height: 100vh;
  display: grid;
  grid-template-columns: minmax(0, 55.8571428571%) minmax(0, 44.1428571429%);
}

.left-section {
  background: #eef3ff;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: clamp(44px, 5vh, 72px) clamp(32px, 3.6vw, 52px) clamp(34px, 4vh, 56px);
  overflow: hidden;
}

.left-illustration {
  width: clamp(590px, 44vw, 690px);
  max-width: 100%;
  height: auto;
  display: block;
  transform: translate(clamp(0px, 1vw, 14px), clamp(0px, 0.8vh, 8px));
}

.right-section {
  background: #fff;
  display: flex;
  flex-direction: column;
  padding:
    clamp(18px, 2.5vh, 24px)
    clamp(40px, 4.8vw, 96px)
    clamp(32px, 4.5vh, 48px)
    clamp(44px, 5vw, 104px);
}

.language-selector {
  display: flex;
  justify-content: flex-end;
}

.language-selector :deep(.language-trigger--auth) {
  color: #7b7b7b;
  font-size: 14px;
  line-height: 22px;
  gap: 8px;
}

.language-selector :deep(.language-trigger--auth .el-icon) {
  font-size: 14px;
}

.form-shell {
  flex: 1;
  display: flex;
  justify-content: center;
  min-height: 0;
  padding-top: clamp(24px, 5vh, 56px);
}

.form-container {
  width: 100%;
  max-width: clamp(465px, 74%, 560px);
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.form-title {
  margin: 0;
  color: #597ef7;
  font-size: 36px;
  font-weight: 600;
  line-height: 1.08;
}

.form-subtitle {
  margin: 14px 0 0;
  color: #ababab;
  font-size: 14px;
  line-height: 1.5;
}

.login-form {
  margin-top: clamp(28px, 4vh, 52px);
  display: flex;
  flex-direction: column;
}

.form-item {
  margin-bottom: 37px;
}

.form-label {
  display: block;
  margin-bottom: 11px;
  color: #111827;
  font-size: 14px;
  font-weight: 600;
  line-height: 1.2;
}

.label-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 11px;
}

.label-row .form-label {
  margin-bottom: 0;
}

.forgot-link {
  font-size: 14px;
  font-weight: 600;
  line-height: 1;
}

.options-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin: -2px 0 12px;
  gap: 16px;
}

.switch-mode-link {
  font-size: 14px;
  font-weight: 600;
}

.agreement-row {
  margin-top: 4px;
  color: #4b5563;
  font-size: 13px;
  line-height: 1.65;
}

.login-button {
  width: 314px;
  max-width: 100%;
  height: 32px;
  margin: clamp(32px, 5vh, 64px) auto 0;
  border-radius: 2px;
  border-color: #597ef7;
  background: #597ef7;
  font-size: 14px;
  font-weight: 600;
  line-height: 1;
  letter-spacing: 0;
  box-shadow: none;
}

.login-button:hover,
.login-button:focus-visible {
  border-color: #597ef7;
  background: #597ef7;
}

.login-button:active {
  border-color: #597ef7;
  background: #597ef7;
}

.form-footer {
  margin-top: auto;
  padding-top: clamp(28px, 4vh, 60px);
  padding-bottom: clamp(44px, 7vh, 96px);
}

.register-link {
  color: #4b5563;
  text-align: center;
  font-size: 14px;
  line-height: 1.6;
}

.support-link-row {
  margin-top: 14px;
  text-align: center;
}

.login-form :deep(.el-form-item) {
  margin-bottom: 0;
}

.login-form :deep(.el-form-item__content) {
  line-height: 1;
}

.login-form :deep(.el-input__wrapper) {
  min-height: 37px;
  padding: 0 12px;
  border-radius: 3px;
  background: #fff;
  box-shadow: inset 0 0 0 1px #d9dee8;
}

.login-form :deep(.el-input__wrapper:hover) {
  box-shadow: inset 0 0 0 1px #597ef7;
}

.login-form :deep(.el-input__wrapper.is-focus) {
  box-shadow: inset 0 0 0 1px #597ef7;
}

.login-form :deep(.el-input__inner) {
  height: 100%;
  font-size: 13px;
  color: #111827;
}

.login-form :deep(.el-input__inner::placeholder) {
  color: #b7bdc9;
}

.login-form :deep(.el-input__inner:-webkit-autofill),
.login-form :deep(.el-input__inner:-webkit-autofill:hover),
.login-form :deep(.el-input__inner:-webkit-autofill:focus),
.login-form :deep(.el-input__inner:-webkit-autofill:active) {
  -webkit-text-fill-color: #111827;
  caret-color: #111827;
  background-color: transparent;
  -webkit-background-clip: text;
  transition: background-color 9999s ease-out 0s;
}

.login-form :deep(.el-input__prefix-inner),
.login-form :deep(.el-input__suffix-inner) {
  color: #4b5563;
  font-size: 14px;
}

.login-form :deep(.el-input__prefix-inner) {
  margin-right: 12px;
}

.login-form :deep(.el-input__suffix-inner) {
  margin-left: 12px;
}

.verification-code-field {
  position: relative;
  width: 100%;
}

.verification-code-field :deep(.verification-code-input) {
  width: 100%;
}

.verification-code-field :deep(.verification-code-input .el-input__wrapper) {
  padding-right: 175px;
}

.verification-code-field :deep(.send-code-button) {
  position: absolute;
  top: 1px;
  right: 35px;
  z-index: 2;
  width: auto;
  min-width: 84px;
  height: 35px;
  min-height: 35px;
  padding: 0 5px;
  border: 0;
  border-radius: 0;
  background: #f1f1f1;
  color: #b3b3b3;
  display: inline-flex;
  flex: 0 0 auto;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  font-weight: 500;
  white-space: nowrap;
  opacity: 1;
  box-shadow: none;
}

.verification-code-field :deep(.send-code-button:hover),
.verification-code-field :deep(.send-code-button:focus-visible),
.verification-code-field :deep(.send-code-button:active) {
  background: #f1f1f1;
  color: #b3b3b3;
}

.verification-code-field :deep(.send-code-button.is-disabled),
.verification-code-field :deep(.send-code-button.is-disabled:hover),
.verification-code-field :deep(.send-code-button.is-disabled:focus-visible),
.verification-code-field :deep(.send-code-button.is-disabled:active) {
  background: #f1f1f1;
  color: #b3b3b3;
  opacity: 1;
}

.login-form :deep(.el-checkbox) {
  align-items: flex-start;
  height: auto;
}

.login-form :deep(.el-checkbox__input) {
  margin-top: 2px;
}

.login-form :deep(.el-checkbox__inner) {
  width: 13px;
  height: 13px;
  border-radius: 2px;
  border-color: #d7dce8;
}

.login-form :deep(.el-checkbox__input.is-checked .el-checkbox__inner),
.login-form :deep(.el-checkbox__input.is-indeterminate .el-checkbox__inner) {
  border-color: #597ef7;
  background: #597ef7;
}

.login-form :deep(.el-checkbox__label) {
  padding-left: 8px;
  color: #4b5563;
  font-size: 13px;
  line-height: 1.65;
  white-space: normal;
}

.login-form :deep(.el-link),
.register-link :deep(.el-link),
.support-link-row :deep(.el-link) {
  color: #597ef7;
}

.register-link :deep(.el-link),
.support-link-row :deep(.el-link) {
  font-weight: 600;
}

@media (max-width: 1100px) {
  .login-layout {
    grid-template-columns: 1fr;
  }

  .left-section {
    display: none;
  }

  .right-section {
    padding: 18px clamp(24px, 8vw, 72px) 42px;
  }

  .form-shell {
    padding-top: 32px;
  }

  .form-container {
    margin: 0 auto;
  }
}

@media (max-width: 640px) {
  .right-section {
    padding: 18px 20px 32px;
  }

  .form-shell {
    padding-top: 24px;
  }

  .form-title {
    font-size: 32px;
  }

  .login-form {
    margin-top: 32px;
  }

  .login-button {
    width: 100%;
    margin-top: 40px;
  }

  .form-footer {
    padding-top: 28px;
  }
}
</style>
