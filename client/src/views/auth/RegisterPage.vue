<template>
  <div class="register-container">
    <div class="register-layout">
      <div class="left-section">
        <img
          :src="registerIllustration"
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
            <h2 class="form-title">{{ t('auth.register.title') }}</h2>
            <p class="form-subtitle">{{ t('auth.register.subtitle') }}</p>

            <el-form
              ref="registerFormRef"
              :model="registerForm"
              :rules="registerRules"
              class="register-form"
            >
              <div class="form-item">
                <label class="form-label">{{ t('common.email') }}</label>
                <el-form-item prop="email">
                  <el-input
                    v-model="registerForm.email"
                    :placeholder="t('auth.register.emailPlaceholder')"
                    size="large"
                    :prefix-icon="Message"
                  />
                </el-form-item>
              </div>

              <div class="form-item">
                <label class="form-label">{{ t('common.verificationCode') }}</label>
                <el-form-item prop="verificationCode">
                  <div class="verification-code-field">
                    <el-input
                      v-model="registerForm.verificationCode"
                      class="verification-code-input"
                      :placeholder="t('auth.register.codePlaceholder')"
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

              <div class="form-item">
                <label class="form-label">{{ t('auth.register.passwordLabel') }}</label>
                <el-form-item prop="password">
                  <el-input
                    v-model="registerForm.password"
                    type="password"
                    :placeholder="t('auth.register.passwordPlaceholder')"
                    size="large"
                    :prefix-icon="Lock"
                    show-password
                  />
                </el-form-item>
              </div>

              <div class="form-item">
                <label class="form-label">{{ t('auth.register.confirmPasswordLabel') }}</label>
                <el-form-item prop="confirmPassword">
                  <el-input
                    v-model="registerForm.confirmPassword"
                    type="password"
                    :placeholder="t('auth.register.confirmPasswordPlaceholder')"
                    size="large"
                    :prefix-icon="Lock"
                    show-password
                  />
                </el-form-item>
              </div>

              <div class="agreement-row">
                <el-checkbox v-model="registerForm.agreeToTerms">
                  {{ t('auth.register.agreementPrefix') }}
                  <el-link
                    type="primary"
                    :underline="false"
                    @click.stop.prevent="goToTermsOfService"
                  >
                    {{ t('common.termsOfService') }}
                  </el-link>
                  {{ t('auth.register.agreementComma') }}
                  <el-link
                    type="primary"
                    :underline="false"
                    @click.stop.prevent="goToPrivacyPolicy"
                  >
                    {{ t('common.privacyPolicy') }}
                  </el-link>
                  {{ t('auth.register.agreementJoiner') }}
                  <el-link type="primary" :underline="false" @click.stop.prevent>
                    {{ t('common.memberTerms') }}
                  </el-link>
                </el-checkbox>
              </div>

              <el-button
                type="primary"
                size="large"
                class="register-button"
                :loading="loading"
                @click="handleRegister"
              >
                {{ t('auth.register.submit') }}
              </el-button>
            </el-form>

            <div class="form-footer">
              <div class="login-link">
                {{ t('auth.register.loginPrefix') }}
                <el-link type="primary" :underline="false" @click="goToLogin">
                  {{ t('auth.register.loginAction') }}
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
import registerIllustration from '@/assets/auth/register-illustration.svg'
import {
  sendVerificationCode as sendVerificationCodeAPI,
  register as registerAPI,
} from '@/api/auth'

const router = useRouter()
const { t } = useI18n()

const countdown = ref(0)
const loading = ref(false)
const registerFormRef = ref<FormInstance>()

const registerForm = reactive({
  email: '',
  verificationCode: '',
  password: '',
  confirmPassword: '',
  agreeToTerms: false,
})

const validateConfirmPassword = (rule: any, value: any, callback: any) => {
  if (value === '') {
    callback(new Error(t('auth.register.validation.confirmPasswordRequired')))
  } else if (value !== registerForm.password) {
    callback(new Error(t('auth.register.validation.confirmPasswordMismatch')))
  } else {
    callback()
  }
}

const registerRules = computed<FormRules>(() => ({
  email: [
    { required: true, message: t('auth.register.validation.emailRequired'), trigger: 'blur' },
    { type: 'email', message: t('auth.register.validation.emailInvalid'), trigger: 'blur' },
  ],
  verificationCode: [
    { required: true, message: t('auth.register.validation.codeRequired'), trigger: 'blur' },
    { len: 6, message: t('auth.register.validation.codeLength'), trigger: 'blur' },
  ],
  password: [
    { required: true, message: t('auth.register.validation.passwordRequired'), trigger: 'blur' },
    { min: 6, max: 20, message: t('auth.register.validation.passwordLength'), trigger: 'blur' },
  ],
  confirmPassword: [
    {
      required: true,
      message: t('auth.register.validation.confirmPasswordRequired'),
      trigger: 'blur',
    },
    { validator: validateConfirmPassword, trigger: 'blur' },
  ],
}))

const sendVerificationCode = async () => {
  try {
    await registerFormRef.value?.validateField('email')
  } catch {
    return
  }

  try {
    await sendVerificationCodeAPI({
      email: registerForm.email,
      type: 'register',
    })

    ElMessage.success(t('auth.register.codeSent'))

    countdown.value = 60
    const timer = setInterval(() => {
      countdown.value--
      if (countdown.value <= 0) {
        clearInterval(timer)
      }
    }, 1000)
  } catch (error: any) {
    const message = error.response?.data?.message || error.message || t('auth.register.failed')
    ElMessage.error(message)
  }
}

const handleRegister = async () => {
  if (!registerForm.agreeToTerms) {
    ElMessage.warning(t('auth.register.agreementRequired'))
    return
  }

  try {
    await registerFormRef.value?.validate()

    loading.value = true

    await registerAPI({
      email: registerForm.email,
      verificationCode: registerForm.verificationCode,
      password: registerForm.password,
    })

    ElMessage.success(t('auth.register.success'))
    router.push('/login')
    loading.value = false
  } catch (error: any) {
    loading.value = false
    const message = error.response?.data?.message || error.message || t('auth.register.failed')
    ElMessage.error(message)
  }
}

const goToLogin = () => {
  router.push('/login')
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
.register-container {
  width: 100%;
  min-height: 100vh;
  display: flex;
  background: #fff;
}

.register-layout {
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
  width: clamp(560px, 42vw, 660px);
  max-width: 100%;
  height: auto;
  display: block;
  transform: translate(clamp(-6px, 0.5vw, 8px), clamp(0px, 0.6vh, 6px));
}

.right-section {
  background: #fff;
  display: flex;
  flex-direction: column;
  padding:
    clamp(18px, 2.5vh, 24px)
    clamp(40px, 4.8vw, 96px)
    clamp(28px, 4vh, 44px)
    clamp(44px, 5vw, 104px);
  overflow-y: auto;
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
  padding-top: clamp(18px, 3.4vh, 36px);
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

.register-form {
  margin-top: clamp(22px, 3vh, 34px);
  display: flex;
  flex-direction: column;
}

.form-item {
  margin-bottom: 24px;
}

.form-label {
  display: block;
  margin-bottom: 11px;
  color: #111827;
  font-size: 14px;
  font-weight: 600;
  line-height: 1.2;
}

.agreement-row {
  margin-top: 14px;
  color: #4b5563;
  font-size: 13px;
  line-height: 1.65;
}

.register-button {
  width: 314px;
  max-width: 100%;
  height: 32px;
  margin: clamp(28px, 3.8vh, 40px) auto 0;
  border-radius: 2px;
  border-color: #597ef7;
  background: #597ef7;
  font-size: 14px;
  font-weight: 600;
  line-height: 1;
  letter-spacing: 0;
  box-shadow: none;
}

.register-button:hover,
.register-button:focus-visible,
.register-button:active {
  border-color: #597ef7;
  background: #597ef7;
}

.form-footer {
  margin-top: auto;
  padding-top: clamp(18px, 2.4vh, 26px);
  padding-bottom: clamp(44px, 7vh, 96px);
}

.login-link {
  color: #4b5563;
  text-align: center;
  font-size: 14px;
  line-height: 1.6;
}

.support-link-row {
  margin-top: 14px;
  text-align: center;
}

.register-form :deep(.el-form-item) {
  margin-bottom: 0;
}

.register-form :deep(.el-form-item__content) {
  line-height: 1;
}

.register-form :deep(.el-input__wrapper) {
  min-height: 37px;
  padding: 0 12px;
  border-radius: 3px;
  background: #fff;
  box-shadow: inset 0 0 0 1px #d9dee8;
}

.register-form :deep(.el-input__wrapper:hover) {
  box-shadow: inset 0 0 0 1px #597ef7;
}

.register-form :deep(.el-input__wrapper.is-focus) {
  box-shadow: inset 0 0 0 1px #597ef7;
}

.register-form :deep(.el-input__inner) {
  height: 100%;
  font-size: 13px;
  color: #111827;
}

.register-form :deep(.el-input__inner::placeholder) {
  color: #b7bdc9;
}

.register-form :deep(.el-input__prefix-inner),
.register-form :deep(.el-input__suffix-inner) {
  color: #4b5563;
  font-size: 14px;
}

.register-form :deep(.el-input__prefix-inner) {
  margin-right: 12px;
}

.register-form :deep(.el-input__suffix-inner) {
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
.verification-code-field :deep(.send-code-button:active),
.verification-code-field :deep(.send-code-button.is-disabled),
.verification-code-field :deep(.send-code-button.is-disabled:hover),
.verification-code-field :deep(.send-code-button.is-disabled:focus-visible),
.verification-code-field :deep(.send-code-button.is-disabled:active) {
  background: #f1f1f1;
  color: #b3b3b3;
  opacity: 1;
}

.register-form :deep(.el-checkbox) {
  align-items: flex-start;
  height: auto;
}

.register-form :deep(.el-checkbox__input) {
  margin-top: 2px;
}

.register-form :deep(.el-checkbox__inner) {
  width: 13px;
  height: 13px;
  border-radius: 2px;
  border-color: #d7dce8;
}

.register-form :deep(.el-checkbox__input.is-checked .el-checkbox__inner),
.register-form :deep(.el-checkbox__input.is-indeterminate .el-checkbox__inner) {
  border-color: #597ef7;
  background: #597ef7;
}

.register-form :deep(.el-checkbox__label) {
  padding-left: 8px;
  color: #4b5563;
  font-size: 13px;
  line-height: 1.65;
  white-space: normal;
}

.register-form :deep(.el-link),
.login-link :deep(.el-link),
.support-link-row :deep(.el-link) {
  color: #597ef7;
}

.login-link :deep(.el-link),
.support-link-row :deep(.el-link) {
  font-weight: 600;
}

@media (max-width: 1100px) {
  .register-layout {
    grid-template-columns: 1fr;
  }

  .left-section {
    display: none;
  }

  .right-section {
    padding: 18px clamp(24px, 8vw, 72px) 36px;
  }

  .form-shell {
    padding-top: 28px;
  }

  .form-container {
    margin: 0 auto;
  }
}

@media (max-width: 640px) {
  .right-section {
    padding: 18px 20px 28px;
  }

  .form-shell {
    padding-top: 22px;
  }

  .form-title {
    font-size: 32px;
  }

  .register-form {
    margin-top: 26px;
  }

  .register-button {
    width: 100%;
    margin-top: 32px;
  }

  .form-footer {
    padding-top: 22px;
  }
}
</style>
